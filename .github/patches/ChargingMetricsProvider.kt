package com.chargeanim.pro.telemetry

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/** Process-wide charging metrics source shared by service, dashboard, overlay and skins. */
object ChargingMetricsProvider {
    private var manager: ChargingMetricsManager? = null
    private var users = 0

    @Synchronized
    fun acquire(context: Context): StateFlow<ChargingMetrics> {
        if (manager == null) manager = ChargingMetricsManager(context.applicationContext)
        users += 1
        manager!!.start()
        return manager!!.state
    }

    @Synchronized
    fun release() {
        if (users > 0) users -= 1
        if (users == 0) {
            manager?.stop()
            manager = null
        }
    }

    @Synchronized
    fun current(): ChargingMetrics = manager?.state?.value ?: ChargingMetrics()
}
