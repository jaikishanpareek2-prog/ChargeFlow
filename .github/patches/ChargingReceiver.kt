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
            Intent.ACTION_POWER_CONNECTED ->
                startWatcher(context, ChargingService.ACTION_PLUGGED_IN)
            Intent.ACTION_POWER_DISCONNECTED ->
                startWatcher(context, ChargingService.ACTION_UNPLUGGED)
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_USER_PRESENT ->
                startWatcher(context, ChargingService.ACTION_MONITOR)
        }
    }

    private fun startWatcher(context: Context, action: String) {
        DiagnosticLog.add(context, "Broadcast received: $action")
        val serviceIntent = Intent(context, ChargingService::class.java).setAction(action)
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
            DiagnosticLog.add(context, "startForegroundService() returned normally")
        } catch (e: RuntimeException) {
            Log.e(TAG, "Unable to start charging service", e)
            DiagnosticLog.add(
                context,
                "startForegroundService FAILED: ${e::class.simpleName}: ${e.message}"
            )
        }
    }
}
