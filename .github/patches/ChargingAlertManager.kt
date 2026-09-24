package com.chargeanim.pro.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri

object ChargingAlertManager {
    private const val PREFS = "chargeflow_settings"
    private const val KEY_TONE = "plug_tone_uri"

    fun selectedTone(context: Context): Uri? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TONE, null)?.let(Uri::parse)

    fun saveTone(context: Context, uri: Uri?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_TONE, uri?.toString()).apply()
    }

    fun play(context: Context) {
        val uri = selectedTone(context) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        runCatching {
            val ringtone: Ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone.play()
        }
    }
}
