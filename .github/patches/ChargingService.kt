package com.chargeanim.pro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import com.chargeanim.pro.data.MediaSelection
import com.chargeanim.pro.data.MediaType
import com.chargeanim.pro.data.PreferencesRepository
import com.chargeanim.pro.telemetry.BatteryStatusData
import com.chargeanim.pro.telemetry.BatteryTelemetryManager
import com.chargeanim.pro.ui.overlay.ChargingOverlayScreen
import com.chargeanim.pro.ui.theme.ThemeId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Owns the real system charging overlay. The existing Activity remains
 * available for manual preview, while this service uses TYPE_APPLICATION_OVERLAY
 * so the charging screen can appear when the app is not in the foreground.
 */
class ChargingService : Service() {

    companion object {
        const val ACTION_PLUGGED_IN = "com.chargeanim.pro.action.PLUGGED_IN"
        const val ACTION_UNPLUGGED = "com.chargeanim.pro.action.UNPLUGGED"
        private const val CHANNEL_ID = "charging_service_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var telemetry: BatteryTelemetryManager
    private lateinit var prefsRepo: PreferencesRepository
    private lateinit var windowManager: WindowManager

    private var overlayView: ComposeView? = null
    private var stateJob: Job? = null

    private val statusState = mutableStateOf(BatteryStatusData())
    private val themeState = mutableStateOf(ThemeId.FUTURISTIC)
    private val mediaState = mutableStateOf(MediaSelection(null, MediaType.NONE))

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        telemetry = BatteryTelemetryManager(applicationContext)
        prefsRepo = PreferencesRepository(applicationContext)
        telemetry.start()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLUGGED_IN -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                if (Settings.canDrawOverlays(this)) {
                    showOverlay()
                } else {
                    stopSelf(startId)
                }
            }

            ACTION_UNPLUGGED -> {
                removeOverlay()
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        if (overlayView != null) return

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                ChargingOverlayScreen(
                    status = statusState.value,
                    theme = themeState.value,
                    media = mediaState.value
                )
            }
        }

        val flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            screenBrightness = 1f
        }

        try {
            windowManager.addView(composeView, params)
            overlayView = composeView
            startStateCollection()
        } catch (_: SecurityException) {
            stopSelf()
        } catch (_: WindowManager.BadTokenException) {
            stopSelf()
        }
    }

    private fun startStateCollection() {
        stateJob?.cancel()
        stateJob = serviceScope.launch {
            combine(
                telemetry.state,
                prefsRepo.theme,
                prefsRepo.normalMedia,
                prefsRepo.fastMedia
            ) { status, theme, normalMedia, fastMedia ->
                OverlayState(
                    status = status,
                    theme = theme,
                    media = if (status.isFastCharging) fastMedia else normalMedia
                )
            }.collect { state ->
                statusState.value = state.status
                themeState.value = state.theme
                mediaState.value = state.media
            }
        }
    }

    private fun removeOverlay() {
        stateJob?.cancel()
        stateJob = null
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: IllegalArgumentException) {
            }
        }
        overlayView = null
    }

    override fun onDestroy() {
        removeOverlay()
        telemetry.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Charging animation",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val settingsIntent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + packageName)
        )
        val pendingIntent = PendingIntent.getActivity(
            this,
            1002,
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ChargeFlow is active")
            .setContentText("Charging animation is running")
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(false)
            .setContentIntent(pendingIntent)
            .build()
    }

    private data class OverlayState(
        val status: BatteryStatusData,
        val theme: ThemeId,
        val media: MediaSelection
    )
}
