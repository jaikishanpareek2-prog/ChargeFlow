package com.chargeanim.pro.history

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ChargingSession(
    val startTime: Long,
    val endTime: Long,
    val finalLevel: Int
)

object ChargingHistoryStore {
    private const val PREFS = "chargeflow_history"
    private const val KEY = "sessions"
    private const val MAX = 100

    fun add(context: Context, session: ChargingSession) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val a = JSONArray(p.getString(KEY, "[]"))
        a.put(JSONObject().apply {
            put("startTime", session.startTime)
            put("endTime", session.endTime)
            put("finalLevel", session.finalLevel)
        })
        while (a.length() > MAX) a.remove(0)
        p.edit().putString(KEY, a.toString()).apply()
    }

    fun all(context: Context): List<ChargingSession> {
        val a = JSONArray(prefs(context).getString(KEY, "[]"))
        return buildList {
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                add(ChargingSession(o.getLong("startTime"), o.getLong("endTime"), o.getInt("finalLevel")))
            }
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
