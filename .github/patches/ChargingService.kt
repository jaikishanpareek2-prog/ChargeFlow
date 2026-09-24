package com.chargeanim.pro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.chargeanim.pro.data.MediaSelection
import com.chargeanim.pro.data.MediaType
import com.chargeanim.pro.data.PreferencesRepository
import com.chargeanim.pro.diagnostics.DiagnosticLog
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

class ChargingService : Service() {
    companion object {
        const val ACTION_MONITOR = "com.chargeanim.pro.action.MONITOR"
        const val ACTION_PLUGGED_IN = "com.chargeanim.pro.action.PLUGGED_IN"
        const val ACTION_UNPLUGGED = "com.chargeanim.pro.action.UNPLUGGED"
        private const val CHANNEL_ID = "charging_service_channel"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "ChargeFlowService"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var telemetry: BatteryTelemetryManager
    private lateinit var prefsRepo: PreferencesRepository
    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var overlayLifecycleOwner: OverlayLifecycleOwner? = null
    private var stateJob: Job? = null
    private val statusState = mutableStateOf(BatteryStatusData())
    private val themeState = mutableStateOf(ThemeId.FUTURISTIC)
    private val mediaState = mutableStateOf(MediaSelection(null, MediaType.NONE))
    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> { DiagnosticLog.add(context, "Runtime power receiver: CONNECTED"); showOverlayIfAllowed() }
                Intent.ACTION_POWER_DISCONNECTED -> { DiagnosticLog.add(context, "Runtime power receiver: DISCONNECTED"); removeOverlay() }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        DiagnosticLog.add(this, "Service onCreate()")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        telemetry = BatteryTelemetryManager(applicationContext)
        prefsRepo = PreferencesRepository(applicationContext)
        telemetry.start()
        DiagnosticLog.add(this, "Battery telemetry started")
        registerReceiver(powerReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        })
        DiagnosticLog.add(this, "Runtime power receiver registered")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        DiagnosticLog.add(this, "Service onStartCommand: action=${intent?.action}")
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
            DiagnosticLog.add(this, "startForeground() succeeded")
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
            DiagnosticLog.add(this, "startForeground FAILED: ${e::class.simpleName}: ${e.message}")
            stopSelf(startId)
            return START_NOT_STICKY
        }
        when (intent?.action) {
            ACTION_UNPLUGGED -> removeOverlay()
            ACTION_PLUGGED_IN, ACTION_MONITOR, null -> checkCurrentChargingState()
        }
        return START_STICKY
    }

    private fun checkCurrentChargingState() {
        val battery = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        DiagnosticLog.add(this, "Current battery state: charging=$charging")
        if (charging) showOverlayIfAllowed() else removeOverlay()
    }

    private fun showOverlayIfAllowed() {
        val allowed = Settings.canDrawOverlays(this)
        DiagnosticLog.add(this, "Overlay permission: $allowed")
        if (allowed) showOverlay() else DiagnosticLog.add(this, "Overlay permission missing; animation cannot be shown")
    }

    private fun showOverlay() {
        if (overlayView != null) {
            DiagnosticLog.add(this, "showOverlay skipped: overlay already exists")
            return
        }

        DiagnosticLog.add(this, "Creating Compose overlay lifecycle owner")
        val owner = OverlayLifecycleOwner().apply {
            performRestore()
            handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            handleLifecycleEvent(Lifecycle.Event.ON_START)
            handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        overlayLifecycleOwner = owner

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else {
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
            DiagnosticLog.add(this, "Calling WindowManager.addView()")
            windowManager.addView(composeView, params)
            overlayView = composeView
            DiagnosticLog.add(this, "Overlay window added successfully")
            startStateCollection()
            DiagnosticLog.add(this, "Overlay state collection started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Overlay addView failed: SecurityException", e)
            DiagnosticLog.add(this, "Overlay addView FAILED: SecurityException: ${e.message}")
            destroyOverlayOwner()
            stopSelf()
        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "Overlay addView failed: BadTokenException", e)
            DiagnosticLog.add(this, "Overlay addView FAILED: BadTokenException: ${e.message}")
            destroyOverlayOwner()
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Overlay addView failed: ${e::class.simpleName}: ${e.message}", e)
            DiagnosticLog.add(this, "Overlay addView FAILED: ${e::class.simpleName}: ${e.message}")
            destroyOverlayOwner()
            stopSelf()
        }
    }

    private fun startStateCollection() {
        stateJob?.cancel()
        stateJob = serviceScope.launch {
            combine(telemetry.state, prefsRepo.theme, prefsRepo.normalMedia, prefsRepo.fastMedia) { status, theme, normalMedia, fastMedia ->
                OverlayState(status, theme, if (status.isFastCharging) fastMedia else normalMedia)
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
        overlayView?.let {
            try {
                windowManager.removeView(it)
                DiagnosticLog.add(this, "Overlay window removed")
            } catch (_: IllegalArgumentException) {
                DiagnosticLog.add(this, "Overlay window was already removed")
            }
        }
        overlayView = null
        destroyOverlayOwner()
    }

    private fun destroyOverlayOwner() {
        overlayLifecycleOwner?.let {
            it.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            it.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            it.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            it.viewModelStore.clear()
        }
        overlayLifecycleOwner = null
    }

    override fun onDestroy() {
        DiagnosticLog.add(this, "Service onDestroy()")
        removeOverlay()
        runCatching { unregisterReceiver(powerReceiver) }
        telemetry.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Charging animation", NotificationManager.IMPORTANCE_MIN).apply {
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val settingsIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName))
        val pendingIntent = PendingIntent.getActivity(this, 1002, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ChargeFlow is active")
            .setContentText("Charging animation is running")
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(false)
            .setContentIntent(pendingIntent)
            .build()
    }

    private data class OverlayState(val status: BatteryStatusData, val theme: ThemeId, val media: MediaSelection)
}

private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val viewModelStore = ViewModelStore()
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    fun performRestore() = savedStateRegistryController.performRestore(null)
    fun handleLifecycleEvent(event: Lifecycle.Event) = lifecycleRegistry.handleLifecycleEvent(event)
}