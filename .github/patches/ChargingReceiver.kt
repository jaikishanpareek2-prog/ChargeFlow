package com.chargeanim.pro.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class ChargingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val serviceAction = when (action) {
            Intent.ACTION_POWER_CONNECTED -> ChargingService.ACTION_PLUGGED_IN
            Intent.ACTION_POWER_DISCONNECTED -> ChargingService.ACTION_UNPLUGGED
            else -> return
        }

        val serviceIntent = Intent(context, ChargingService::class.java).apply {
            this.action = serviceAction
        }

        if (serviceAction == ChargingService.ACTION_PLUGGED_IN) {
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
