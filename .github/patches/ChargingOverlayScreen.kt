package com.chargeanim.pro.ui.overlay

import androidx.compose.animation.core.LinearEasing
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.chargeanim.pro.data.MediaSelection
import com.chargeanim.pro.data.PreferencesRepository
import com.chargeanim.pro.media.MediaRenderer
import com.chargeanim.pro.telemetry.BatteryStatusData
import com.chargeanim.pro.ui.theme.ThemeCatalog
import com.chargeanim.pro.ui.theme.ThemeId
import com.chargeanim.pro.ui.theme.ThemeVisual
import com.chargeanim.pro.ui.theme.VibeVisual
import com.chargeanim.pro.ui.theme.ThemeType
import com.chargeanim.pro.ui.theme.VibesThemeManager

@Composable
fun ChargingOverlayScreen(status: BatteryStatusData, theme: ThemeId, media: MediaSelection, showTelemetry: Boolean = true) {
    val context = LocalContext.current
    val prefs = remember { PreferencesRepository(context.applicationContext) }
    val amoledMode by prefs.amoledMode.collectAsStateWithLifecycle(initialValue = true)
    var vibe by remember { mutableStateOf(VibesThemeManager.getSelectedTheme(context)) }
    DisposableEffect(context) {
        val preferences = context.applicationContext.getSharedPreferences("chargeflow_vibes", android.content.Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "selected_vibe") {
                vibe = VibesThemeManager.getSelectedTheme(context)
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val accent = if (vibe != ThemeType.NONE) VibesThemeManager.accent(vibe) else ThemeCatalog.getValue(theme).accentPrimary
    Box(Modifier.fillMaxSize().background(if (amoledMode) Color.Black else Color(0xFF0A0E1A))) {
        Row(Modifier.align(Alignment.TopCenter).padding(top = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.Bolt, null, tint = accent, modifier = Modifier.size(16.dp))
            Text("${status.percent}%", fontSize = 15.sp, color = Color(0xFFEAF4FF))
            Text("•", color = Color(0xFF44506A), fontSize = 13.sp)
            Text(speedLabel(status), fontSize = 13.sp, color = accent)
            if (status.wattage != null) {
                Text("•", color = Color(0xFF44506A), fontSize = 13.sp)
                Text("%.1f W".format(status.wattage), fontSize = 13.sp, color = Color(0xFF8B9AB0))
            }
        }
        Box(Modifier.align(Alignment.Center).size(260.dp), contentAlignment = Alignment.Center) {
            if (media.uri != null) {
                MediaRenderer(media, Modifier.fillMaxSize()) {}
            } else if (vibe != ThemeType.NONE) {
                VibeVisual(vibe, Modifier.fillMaxSize())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${status.percent}%", fontSize = 44.sp, color = Color(0xFFEAF4FF))
                    Text(speedLabel(status), fontSize = 14.sp, color = accent)
                }
            } else {
                ThemeVisual(theme, Modifier.fillMaxSize()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${status.percent}%", fontSize = 44.sp, color = Color(0xFFEAF4FF))
                        Text(speedLabel(status), fontSize = 14.sp, color = accent)
                    }
                }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp).fillMaxWidth(0.86f), horizontalAlignment = Alignment.CenterHorizontally) {
            if (showTelemetry) {
                TelemetryCard(status)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatusBanner(status)
                    TimeBadge(status)
                }
                Spacer(Modifier.height(20.dp))
            }
            AndroidView(
                factory = { context -> FlowingWaveView(context) },
                update = { it.startFlow() },
                modifier = Modifier.fillMaxWidth().height(70.dp)
            )
            PortGlow(accent)
        }
    }
}

@Composable private fun TelemetryCard(status: BatteryStatusData) {
    Row(Modifier.fillMaxWidth().background(Color(0xFF0A0E1A), androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        MetricCell("Voltage", status.voltage?.let { "%.2f V".format(it) })
        MetricCell("Current", status.currentMa?.let { "%.2f A".format(it / 1000f) })
        MetricCell("Power", status.wattage?.let { "%.1f W".format(it) })
    }
}
@Composable private fun MetricCell(label: String, value: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value ?: "—", fontSize = 15.sp, color = Color(0xFFEAF4FF))
        Text(label, fontSize = 10.sp, color = Color(0xFF6B7788))
    }
}
@Composable private fun StatusBanner(status: BatteryStatusData) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(Icons.Filled.Thermostat, null, tint = Color(0xFF6B7788), modifier = Modifier.size(14.dp))
        Text(status.temperatureC?.let { "%.0f°C".format(it) } ?: "—", fontSize = 12.sp, color = Color(0xFF9AA8BA))
    }
}
@Composable private fun TimeBadge(status: BatteryStatusData) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(Icons.Filled.Schedule, null, tint = Color(0xFF6B7788), modifier = Modifier.size(14.dp))
        Text(timeToFullLabel(status), fontSize = 12.sp, color = Color(0xFF9AA8BA))
    }
}
@Composable private fun PortGlow(accent: Color) {
    val infinite = rememberInfiniteTransition(label = "port")
    val pulse by infinite.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse), label = "pulse")
    Canvas(Modifier.fillMaxWidth().height(36.dp)) {
        val center = Offset(size.width / 2f, size.height)
        drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.5f * pulse), Color.Transparent), center, size.width * 0.18f), size.width * 0.18f, center)
        drawCircle(accent.copy(alpha = 0.9f), 4f, Offset(size.width / 2f, size.height - 6f))
    }
}
private fun speedLabel(status: BatteryStatusData) = when {
    !status.isCharging -> "Not charging"
    status.isFastCharging -> "Fast Charging"
    else -> "Charging"
}
private fun timeToFullLabel(status: BatteryStatusData): String {
    if (!status.isCharging) return "—"
    if (status.percent >= 100) return "Full"
    val start = status.sessionStartPercent ?: return "Estimating…"
    val elapsedMin = status.elapsedChargingMs / 60000f
    val gained = status.percent - start
    if (elapsedMin < 1f || gained <= 0) return "Estimating…"
    val ratePerMin = gained / elapsedMin
    val minutesLeft = ((100 - status.percent) / ratePerMin).toInt()
    return if (minutesLeft in 0..600) "$minutesLeft min to full" else "Estimating…"
}
