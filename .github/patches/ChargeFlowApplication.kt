package com.chargeanim.pro

import android.app.Application

class ChargeFlowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashCapture.install(this)
    }
}
