package com.chargeanim.pro.settings

import android.content.Context
import android.media.AudioManager
import android.os.BatteryManager
import android.os.PowerManager
import java.util.Calendar

object ChargeFlowSettings {
    private const val PREFS = "chargeflow_settings"
    private const val KEY_TEMP_WARNING = "temp_warning_c"
    private const val KEY_QUIET_START = "quiet_start_min"
    private const val KEY_QUIET_END = "quiet_end_min"
    private const val KEY_SPEED = "animation_speed"
    private const val KEY_PROFILE = "charging_profile"
    private const val KEY_CONSERVATION = "conservation_limit"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun temperatureWarningC(context: Context) = prefs(context).getFloat(KEY_TEMP_WARNING, 45f)
    fun quietStart(context: Context) = prefs(context).getInt(KEY_QUIET_START, -1)
    fun quietEnd(context: Context) = prefs(context).getInt(KEY_QUIET_END, -1)
    fun animationSpeed(context: Context) = prefs(context).getFloat(KEY_SPEED, 1f).coerceIn(.25f, 2f)
    fun chargingProfile(context: Context) = prefs(context).getString(KEY_PROFILE, "Balanced") ?: "Balanced"
    fun conservationLimit(context: Context) = prefs(context).getInt(KEY_CONSERVATION, 100).coerceIn(80, 100)

    fun isQuiet(context: Context): Boolean {
        val audio = context.getSystemService(AudioManager::class.java)
        if (audio?.ringerMode == AudioManager.RINGER_MODE_SILENT) return true
        val start = quietStart(context)
        val end = quietEnd(context)
        if (start !in 0..1439 || end !in 0..1439 || start == end) return false
        val now = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60 + Calendar.getInstance().get(Calendar.MINUTE)
        return if (start < end) now in start until end else now >= start || now < end
    }

    fun isPowerSave(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true

    fun animationDurationMs(context: Context): Long {
        val profile = chargingProfile(context)
        val base = when (profile) { "Eco" -> 3200L; "Turbo" -> 1100L; else -> 2000L }
        return (base / animationSpeed(context)).toLong().coerceIn(500L, 8000L)
    }

    fun temperatureC(intent: android.content.Intent): Float? =
        if (intent.hasExtra(BatteryManager.EXTRA_TEMPERATURE))
            intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f else null

    fun health(intent: android.content.Intent): Int =
        intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)

    fun level(intent: android.content.Intent): Int =
        intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)

    fun shouldBlockOverlay(context: Context, level: Int, temperatureC: Float?): Boolean =
        isQuiet(context) || level >= conservationLimit(context) || (temperatureC != null && temperatureC >= temperatureWarningC(context))

    fun conservationReached(context: Context, level: Int): Boolean =
        conservationLimit(context) < 100 && level >= conservationLimit(context)
}
