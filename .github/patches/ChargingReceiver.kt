package com.chargeanim.pro.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.chargeanim.pro.diagnostics.DiagnosticLog

class ChargingReceiver : BroadcastReceiver() {
    companion object { private const val TAG = "ChargeFlowReceiver" }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                DiagnosticLog.add(context, "Broadcast received: ACTION_POWER_CONNECTED")
                val serviceIntent = Intent(context, ChargingService::class.java).apply {
                    action = ChargingService.ACTION_PLUGGED_IN
                }
                try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                    DiagnosticLog.add(context, "startForegroundService() call returned normally")
                } catch (e: RuntimeException) {
                    Log.e(TAG, "Unable to start charging service", e)
                    DiagnosticLog.add(context, "startForegroundService FAILED: ${e::class.simpleName}: ${e.message}")
                }
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                DiagnosticLog.add(context, "Broadcast received: ACTION_POWER_DISCONNECTED")
                val serviceIntent = Intent(context, ChargingService::class.java).apply {
                    action = ChargingService.ACTION_UNPLUGGED
                }
                try {
                    context.startService(serviceIntent)
                    DiagnosticLog.add(context, "stopService call returned normally")
                } catch (e: RuntimeException) {
                    Log.e(TAG, "Unable to stop charging service", e)
                    DiagnosticLog.add(context, "stopService call FAILED: ${e::class.simpleName}: ${e.message}")
                }
            }
        }
    }
}