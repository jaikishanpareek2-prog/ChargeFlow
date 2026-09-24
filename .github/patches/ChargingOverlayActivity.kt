package com.chargeanim.pro.ui.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chargeanim.pro.data.MediaSelection
import com.chargeanim.pro.data.MediaType
import com.chargeanim.pro.data.PreferencesRepository
import com.chargeanim.pro.media.AudioPlayerManager
import com.chargeanim.pro.telemetry.BatteryTelemetryManager
import com.chargeanim.pro.ui.theme.ThemeId

class ChargingOverlayActivity:ComponentActivity(){private lateinit var telemetry:BatteryTelemetryManager;private lateinit var prefsRepo:PreferencesRepository;private val unplugReceiver=object:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){if(intent.action==Intent.ACTION_POWER_DISCONNECTED)finish()}}
override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P){setShowWhenLocked(true);setTurnScreenOn(true)};telemetry=BatteryTelemetryManager(applicationContext);prefsRepo=PreferencesRepository(applicationContext);telemetry.start();registerReceiver(unplugReceiver,IntentFilter(Intent.ACTION_POWER_DISCONNECTED));setContent{val status by telemetry.state.collectAsStateWithLifecycle();val theme by prefsRepo.theme.collectAsStateWithLifecycle(initialValue=ThemeId.FUTURISTIC);val normalMedia by prefsRepo.normalMedia.collectAsStateWithLifecycle(initialValue=MediaSelection(null,MediaType.NONE));val fastMedia by prefsRepo.fastMedia.collectAsStateWithLifecycle(initialValue=MediaSelection(null,MediaType.NONE));val soundEnabled by prefsRepo.soundEnabled.collectAsStateWithLifecycle(initialValue=false);val customSound by prefsRepo.customSoundUri.collectAsStateWithLifecycle(initialValue=null);LaunchedEffect(Unit){if(soundEnabled)AudioPlayerManager(applicationContext).play(customSound)};Surface(color=Color.Black,modifier=Modifier.fillMaxSize()){ChargingOverlayScreen(status,theme,if(status.isFastCharging)fastMedia else normalMedia)}}}
override fun onDestroy(){super.onDestroy();telemetry.stop();try{unregisterReceiver(unplugReceiver)}catch(_:Exception){}}}