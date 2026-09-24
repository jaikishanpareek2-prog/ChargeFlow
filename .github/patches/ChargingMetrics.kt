package com.chargeanim.pro.telemetry

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

data class ChargingMetrics(
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val isFull: Boolean = false,
    val voltageVolts: Double = 0.0,
    val currentAmps: Double = 0.0,
    val powerWatts: Double = 0.0,
    val temperatureCelsius: Double = 0.0,
    val chargeRatePercentPerHour: Double = 0.0,
    val timeToFullMinutes: Int? = null,
    val chargingProfile: ChargingProfile = ChargingProfile.BALANCED,
    val sessionSeconds: Long = 0L,
    val plugType: Int = 0,
    val updatedAtMillis: Long = 0L
)

enum class ChargingProfile { ECO, BALANCED, TURBO }

/** Shared live charging state for every ChargeFlow skin, Monitor and Telemetry. */
class ChargingMetricsManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val _state = MutableStateFlow(ChargingMetrics())
    val state: StateFlow<ChargingMetrics> = _state.asStateFlow()
    private var pollJob: Job? = null
    private var sessionStart = 0L
    private var lastPercent = -1
    private var lastSampleTime = 0L
    private var rateEma = 0.0

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) update(intent)
        }
    }

    fun start() {
        if (pollJob != null) return
        val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (sticky != null) update(sticky)
        pollJob = scope.launch {
            while (isActive) {
                readBatteryChanged()
                delay(2000L)
            }
        }
    }

    fun stop() {
        pollJob?.cancel()
        pollJob = null
        runCatching { context.unregisterReceiver(receiver) }
    }

    private fun readBatteryChanged() {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (intent != null) update(intent)
    }

    private fun update(intent: Intent) {
        val now = System.currentTimeMillis()
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val percent = ((level.toDouble() / scale) * 100.0).roundToInt().coerceIn(0, 100)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val full = status == BatteryManager.BATTERY_STATUS_FULL || percent >= 100
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000.0
        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0
        val plug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val currentMicroAmps = readCurrentMicroAmps()
        val currentAmps = (currentMicroAmps / 1_000_000.0).coerceAtLeast(0.0)
        val power = (voltage * currentAmps).coerceAtLeast(0.0)

        if (charging && sessionStart == 0L) sessionStart = now
        if (!charging) sessionStart = 0L

        if (charging && lastPercent >= 0 && lastSampleTime > 0L && percent > lastPercent) {
            val hours = (now - lastSampleTime).toDouble() / 3_600_000.0
            if (hours > 0.0) {
                val instantRate = (percent - lastPercent) / hours
                rateEma = if (rateEma == 0.0) instantRate else rateEma * 0.75 + instantRate * 0.25
            }
        } else if (!charging) {
            rateEma *= 0.9
        }
        lastPercent = percent
        lastSampleTime = now

        val remaining = if (charging && !full && rateEma > 0.05) {
            max(1, ((100 - percent) / rateEma * 60.0).roundToInt())
        } else null

        _state.value = ChargingMetrics(
            batteryPercent = percent,
            isCharging = charging,
            isFull = full,
            voltageVolts = voltage,
            currentAmps = currentAmps,
            powerWatts = power,
            temperatureCelsius = temperature,
            chargeRatePercentPerHour = rateEma,
            timeToFullMinutes = remaining,
            chargingProfile = when {
                power >= 15.0 -> ChargingProfile.TURBO
                power >= 5.0 -> ChargingProfile.BALANCED
                else -> ChargingProfile.ECO
            },
            sessionSeconds = if (sessionStart > 0L) (now - sessionStart) / 1000L else 0L,
            plugType = plug,
            updatedAtMillis = now
        )
    }

    private fun readCurrentMicroAmps(): Long {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val bm = context.getSystemService(BatteryManager::class.java)
            val now = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)?.toLong() ?: 0L
            if (now != 0L && now != Int.MIN_VALUE.toLong()) return now
            val avg = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)?.toLong() ?: 0L
            if (avg != 0L && avg != Int.MIN_VALUE.toLong()) return avg
        }
        return 0L
    }
}
