package com.chargeanim.pro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Intent
import android.app.KeyguardManager
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
import com.chargeanim.pro.alert.ChargingAlertManager
import com.chargeanim.pro.history.ChargingHistoryStore
import com.chargeanim.pro.history.ChargingSession
import com.chargeanim.pro.data.AnimationMode
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
import kotlinx.coroutines.flow.StateFlow
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
    private lateinit var chargingMetricsState: StateFlow<com.chargeanim.pro.telemetry.ChargingMetrics>
    private lateinit var prefsRepo: PreferencesRepository
    private lateinit var keyguardManager: KeyguardManager
    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var overlayLifecycleOwner: OverlayLifecycleOwner? = null
    private var stateJob: Job? = null
    private var metricsJob: Job? = null
    private var prefsJob: Job? = null
    private var soundJob: Job? = null
    private var animationMode = AnimationMode.TEMPORARY
    private var enabled = true
    private var soundEnabled = false
    private var userPresentSincePlugged = false
    private var lastCharging = false
    private var chargingSessionStart = 0L
    private val statusState = mutableStateOf(BatteryStatusData())
    private val themeState = mutableStateOf(ThemeId.FUTURISTIC)
    private val mediaState = mutableStateOf(MediaSelection(null, MediaType.NONE))
    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    DiagnosticLog.add(context, "Runtime event: POWER_CONNECTED")
                    userPresentSincePlugged = false
                    if (soundEnabled) ChargingAlertManager.play(context)
                    evaluateAnimationState("POWER_CONNECTED", true)
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    DiagnosticLog.add(context, "Runtime event: POWER_DISCONNECTED")
                    userPresentSincePlugged = false
                    removeOverlay("POWER_DISCONNECTED")
                    evaluateAnimationState("POWER_DISCONNECTED", false)
                    DiagnosticLog.add(context, "Runtime watcher stopping after power disconnect")
                    stopSelf()
                }
                Intent.ACTION_BATTERY_CHANGED -> evaluateAnimationState("BATTERY_CHANGED", false)
                Intent.ACTION_SCREEN_ON -> evaluateAnimationState("SCREEN_ON", false)
                Intent.ACTION_SCREEN_OFF -> evaluateAnimationState("SCREEN_OFF", false)
                Intent.ACTION_USER_PRESENT -> {
                    userPresentSincePlugged = true
                    evaluateAnimationState("USER_PRESENT", false)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        DiagnosticLog.add(this, "Service onCreate()")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        telemetry = BatteryTelemetryManager(applicationContext)
        chargingMetricsState = com.chargeanim.pro.telemetry.ChargingMetricsProvider.acquire(applicationContext)
        prefsRepo = PreferencesRepository(applicationContext)
        telemetry.start()
        DiagnosticLog.add(this, "Battery telemetry and shared charging metrics started")
        metricsJob = serviceScope.launch {
            chargingMetricsState.collect { metrics ->
                DiagnosticLog.add(
                    this@ChargingService,
                    "Metrics: ${metrics.batteryPercent}% ${String.format(java.util.Locale.US, "%.2fV", metrics.voltageVolts)} " +
                        "${String.format(java.util.Locale.US, "%.2fA", metrics.currentAmps)} " +
                        "${String.format(java.util.Locale.US, "%.2fW", metrics.powerWatts)} " +
                        "${String.format(java.util.Locale.US, "%.1fC", metrics.temperatureCelsius)} " +
                        "profile=${metrics.chargingProfile} ttf=${metrics.timeToFullMinutes ?: -1}m"
                )
            }
        }
        keyguardManager = getSystemService(KeyguardManager::class.java)
        registerReceiver(powerReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }, if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
            android.content.Context.RECEIVER_NOT_EXPORTED else 0)
        DiagnosticLog.add(this, "Runtime system receiver registered")

        prefsJob = serviceScope.launch {
            combine(
                prefsRepo.enabled,
                prefsRepo.animationMode,
                prefsRepo.theme,
                prefsRepo.normalMedia,
                prefsRepo.fastMedia
            ) { enabledValue, mode, theme, normalMedia, fastMedia ->
                PrefState(enabledValue, mode, theme, normalMedia, fastMedia)
            }.collect { state ->
                enabled = state.enabled
                animationMode = state.mode
                themeState.value = state.theme
                mediaState.value = if (statusState.value.isFastCharging) state.fastMedia else state.normalMedia
                evaluateAnimationState("PREFERENCES", false)
            }
        }
        soundJob = serviceScope.launch {
            prefsRepo.soundEnabled.collect { soundEnabled = it }
        }
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
            ACTION_UNPLUGGED -> {
                removeOverlay("ACTION_UNPLUGGED")
                DiagnosticLog.add(this, "Stopping watcher after active charging session ended")
                stopSelf(startId)
                return START_NOT_STICKY
            }
            ACTION_PLUGGED_IN, null -> {
                evaluateAnimationState("SERVICE_START", false)
                if (!isCurrentlyCharging()) {
                    DiagnosticLog.add(this, "Service start found no active charging; stopping watcher")
                    stopSelf(startId)
                    return START_NOT_STICKY
                }
            }
            ACTION_MONITOR -> {
                evaluateAnimationState("SERVICE_MONITOR", false)
                if (!isCurrentlyCharging()) {
                    DiagnosticLog.add(this, "Monitor request found no active charging; stopping watcher")
                    stopSelf(startId)
                    return START_NOT_STICKY
                }
            }
        }
        return START_STICKY
    }

    private fun isCurrentlyCharging(): Boolean {
        val battery = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        return (status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL) && plugged != 0
    }

    private fun evaluateAnimationState(reason: String, haptic: Boolean) {
        val battery = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val charging = (status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL) && plugged != 0
        val locked = keyguardManager.isKeyguardLocked

        if (charging != lastCharging) {
            if (charging) {
                chargingSessionStart = System.currentTimeMillis()
                DiagnosticLog.add(this, "Charging session started")
            } else if (chargingSessionStart > 0L) {
                ChargingHistoryStore.add(
                    this,
                    ChargingSession(
                        startTime = chargingSessionStart,
                        endTime = System.currentTimeMillis(),
                        finalLevel = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, statusState.value.percent) ?: statusState.value.percent
                    )
                )
                DiagnosticLog.add(this, "Charging session recorded")
                chargingSessionStart = 0L
            }
            lastCharging = charging
            userPresentSincePlugged = false
        }

        val shouldShow = enabled && charging && when (animationMode) {
            AnimationMode.ALWAYS_ON -> true
            AnimationMode.TEMPORARY -> !userPresentSincePlugged || locked
        }

        DiagnosticLog.add(this, "Evaluate: reason=$reason charging=$charging locked=$locked enabled=$enabled mode=$animationMode userPresent=$userPresentSincePlugged show=$shouldShow")

        if (shouldShow) showOverlayIfAllowed(haptic) else removeOverlay(reason)
    }

    private fun showOverlayIfAllowed(haptic: Boolean = false) {
        val allowed = Settings.canDrawOverlays(this)
        DiagnosticLog.add(this, "Overlay permission: $allowed")
        if (allowed) {
            showOverlay()
            if (haptic) {
                overlayView?.let { view ->
                    view.post { com.chargeanim.pro.ui.overlay.ChargerHaptics.trigger(view) }
                }
            }
        } else {
            DiagnosticLog.add(this, "Overlay permission missing; watcher remains alive")
        }
    }

    private fun showOverlay() {
        if (overlayView != null) {
            DiagnosticLog.add(this, "showOverlay skipped: overlay already exists")
            return
        }

        DiagnosticLog.add(this, "Creating Compose overlay lifecycle owner")
        val owner = OverlayLifecycleOwner().apply {
            handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            handleLifecycleEvent(Lifecycle.Event.ON_START)
            handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        overlayLifecycleOwner = owner

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
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
        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "Overlay addView failed: BadTokenException", e)
            DiagnosticLog.add(this, "Overlay addView FAILED: BadTokenException: ${e.message}")
            destroyOverlayOwner()
        } catch (e: Exception) {
            Log.e(TAG, "Overlay addView failed: ${e::class.simpleName}: ${e.message}", e)
            DiagnosticLog.add(this, "Overlay addView FAILED: ${e::class.simpleName}: ${e.message}")
            destroyOverlayOwner()
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

    private fun removeOverlay(reason: String = "UNSPECIFIED") {
        stateJob?.cancel()
        stateJob = null
        overlayView?.let {
            try {
                windowManager.removeViewImmediate(it)
                DiagnosticLog.add(this, "Overlay window removed: $reason")
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
        metricsJob?.cancel()
        metricsJob = null
        prefsJob?.cancel()
        prefsJob = null
        soundJob?.cancel()
        soundJob = null
        runCatching { unregisterReceiver(powerReceiver) }
        com.chargeanim.pro.telemetry.ChargingMetricsProvider.release()
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
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private data class OverlayState(val status: BatteryStatusData, val theme: ThemeId, val media: MediaSelection)
    private data class PrefState(val enabled: Boolean, val mode: AnimationMode, val theme: ThemeId, val normalMedia: MediaSelection, val fastMedia: MediaSelection)
}

private class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val viewModelStore = ViewModelStore()
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    fun handleLifecycleEvent(event: Lifecycle.Event) = lifecycleRegistry.handleLifecycleEvent(event)
}
