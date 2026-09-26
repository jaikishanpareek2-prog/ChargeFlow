package com.chargeanim.pro.diagnostics

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticLog {
    private const val PREFS = "chargeanim_diagnostics"
    private const val KEY = "log_lines"
    private const val MAX_LINES = 300
    private val formatter = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val lock = Any()


    fun add(context: Context, message: String) {
        synchronized(lock) {
            val line = "${formatter.format(Date())}  $message"
            android.util.Log.d("ChargeFlow", line)
            val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val existing = prefs.getString(KEY, "") ?: ""
            val lines = (existing.split("\n").filter { it.isNotBlank() } + line).takeLast(MAX_LINES)
            prefs.edit().putString(KEY, lines.joinToString("\n")).apply()
        }
    }

    fun readAll(context: Context): List<String> = synchronized(lock) {
        (context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: "")
            .split("\n").filter { it.isNotBlank() }.reversed()
    }

    fun clear(context: Context) {
        synchronized(lock) {
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        }
    }

}