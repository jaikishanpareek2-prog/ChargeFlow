package com.chargeanim.pro.ui.main

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chargeanim.pro.data.AnimationMode
import com.chargeanim.pro.data.MediaType
import com.chargeanim.pro.data.PreferencesRepository
import com.chargeanim.pro.diagnostics.DiagnosticLog
import com.chargeanim.pro.telemetry.BatteryStatusData
import com.chargeanim.pro.telemetry.ChargingMetrics
import com.chargeanim.pro.telemetry.ChargingMetricsManager
import com.chargeanim.pro.ui.overlay.ChargingOverlayScreen
import com.chargeanim.pro.ui.theme.ThemeCatalog
import com.chargeanim.pro.ui.theme.ThemeId
import com.chargeanim.pro.ui.theme.ThemeVisual
import kotlinx.coroutines.launch

private enum class DashboardTab(val label: String, val icon: @Composable () -> Unit) {
    THEMES("Skins", { Icon(Icons.Default.Palette, null) }), VIBES("Vibes", { Icon(Icons.Default.Waves, null) }), SETTINGS("Config", { Icon(Icons.Default.Settings, null) }), PREVIEW("Monitor", { Icon(Icons.Default.PlayArrow, null) }), DIAGNOSTICS("Telemetry", { Icon(Icons.Default.GraphicEq, null) })
}

@Composable
fun MainTabDashboard(prefs: PreferencesRepository, onLaunchOverlay: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        runCatching {
            val intent = Intent(context, com.chargeanim.pro.service.ChargingService::class.java).apply {
                action = com.chargeanim.pro.service.ChargingService.ACTION_MONITOR
            }
            ContextCompat.startForegroundService(context, intent)
            DiagnosticLog.add(context, "Charging monitor start requested from visible app")
        }.onFailure {
            DiagnosticLog.add(context, "Charging monitor start FAILED: ${it::class.simpleName}: ${it.message}")
        }
    }
    var tab by remember { mutableStateOf(DashboardTab.THEMES) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(20.dp, 20.dp, 20.dp, 8.dp)) {
            Text("ChargeFlow", style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        }
        ScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = 12.dp) {
            DashboardTab.entries.forEach { t -> Tab(selected = tab == t, onClick = { tab = t }, icon = t.icon, text = { Text(t.label, maxLines = 1) }) }
        }
        when (tab) {
            DashboardTab.THEMES -> ThemesTab(prefs)
            DashboardTab.VIBES -> VibesTab()
            DashboardTab.SETTINGS -> SettingsTab(prefs)
            DashboardTab.PREVIEW -> PreviewTab(prefs, onLaunchOverlay)
            DashboardTab.DIAGNOSTICS -> DiagnosticsTab()
        }
    }
}

@Composable
private fun ThemesTab(prefs: PreferencesRepository) {
    val scope = rememberCoroutineScope()
    val selected by prefs.theme.collectAsStateWithLifecycle(initialValue = ThemeId.FUTURISTIC)
    LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        items(ThemeId.entries.toList()) { themeId ->
            ThemeCard(themeId, themeId == selected) { scope.launch { prefs.setTheme(themeId) } }
        }
    }
}

@Composable
private fun ThemeCard(themeId: ThemeId, isSelected: Boolean, onClick: () -> Unit) {
    val style = ThemeCatalog.getValue(themeId)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) style.accentPrimary.copy(alpha = 0.13f)
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, style.accentPrimary.copy(alpha = .65f)) else null
    ) {
        Column(Modifier.padding(10.dp)) {
            Box(Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF05070C))) {
                ThemeVisual(themeId = themeId, modifier = Modifier.fillMaxSize()) {
                    Text("72%", color = Color(0xFFEAF4FF), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
                if (isSelected) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = style.accentPrimary
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = Color.Black, modifier = Modifier.padding(5.dp).size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(themeId.label, style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, maxLines = 1)
            Text(if (isSelected) "Active skin" else "Tap to apply", style = MaterialTheme.typography.labelSmall, color = if (isSelected) style.accentPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsTab(prefs: PreferencesRepository) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val enabled by prefs.enabled.collectAsStateWithLifecycle(initialValue = true)
    val animationMode by prefs.animationMode.collectAsStateWithLifecycle(initialValue = AnimationMode.TEMPORARY)
    val amoled by prefs.amoledMode.collectAsStateWithLifecycle(initialValue = true)
    val autoHide by prefs.autoHide.collectAsStateWithLifecycle(initialValue = true)
    val soundEnabled by prefs.soundEnabled.collectAsStateWithLifecycle(initialValue = false)
    val overlayGranted = remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) overlayGranted.value = Settings.canDrawOverlays(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val normalMediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            scope.launch { prefs.setNormalMedia(uri.toString(), guessType(context, uri)) }
        }
    }
    val fastMediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            scope.launch { prefs.setFastMedia(uri.toString(), guessType(context, uri)) }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Charging experience", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Text("Control how ChargeFlow behaves while connected.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SettingSwitch(Icons.Default.Bolt, "Show animation when charging", "Enable the charging experience", enabled) { scope.launch { prefs.setEnabled(it) } }
                Text("Animation mode", style = MaterialTheme.typography.titleSmall)
                Text(if (animationMode == AnimationMode.TEMPORARY) "Shows on charge and while the lock screen is active; hides after unlock." else "Stays visible continuously while charging; hides after disconnect.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = animationMode == AnimationMode.TEMPORARY, onClick = { scope.launch { prefs.setAnimationMode(AnimationMode.TEMPORARY) } }, label = { Text("Temporary") })
                    FilterChip(selected = animationMode == AnimationMode.ALWAYS_ON, onClick = { scope.launch { prefs.setAnimationMode(AnimationMode.ALWAYS_ON) } }, label = { Text("Always On") })
                }
                Text("System overlay", style = MaterialTheme.typography.titleSmall)
                Text(if (overlayGranted.value) "Enabled: ChargeFlow can appear automatically when charging." else "Required for automatic charging animation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName))) }) { Text(if (overlayGranted.value) "Open overlay permission" else "Enable system overlay") }
                SettingSwitch(Icons.Default.Waves, "AMOLED black background", "Use a deeper black background", amoled) { scope.launch { prefs.setAmoledMode(it) } }
                SettingSwitch(Icons.Default.GraphicEq, "Hide automatically when unplugged", "Dismiss the overlay after disconnect", autoHide) { scope.launch { prefs.setAutoHide(it) } }
                SettingSwitch(Icons.Default.VolumeUp, "Charging start sound", "Play the selected sound", soundEnabled) { scope.launch { prefs.setSoundEnabled(it) } }
            }
        }
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(16.dp)) {
                Text("Custom media", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Text("Optional media can replace the theme visual.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = { normalMediaPicker.launch("*/*") }) { Text("Normal charging") }
                    OutlinedButton(onClick = { fastMediaPicker.launch("*/*") }) { Text("Fast charging") }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsTab() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lines by remember { mutableStateOf(DiagnosticLog.readAll(context)) }
    val metricsManager = remember { ChargingMetricsManager(context.applicationContext) }
    val metrics by metricsManager.state.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner) {
        metricsManager.start()
        onDispose { metricsManager.stop() }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("POWER STATE", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        MetricsPanel(metrics)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Event log", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { lines = DiagnosticLog.readAll(context) }) { Text("Refresh") }
                OutlinedButton(onClick = { DiagnosticLog.clear(context); lines = emptyList() }) { Text("Clear") }
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
            lazyItems(lines) { line ->
                Text(line, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(8.dp))
            }
        }
    }
}

@Composable
private fun MetricsPanel(metrics: ChargingMetrics) {
    val powerState = when {
        metrics.isFull -> "FULL"
        metrics.isCharging -> "CHARGING • ${metrics.chargingProfile.name}"
        else -> "NOT CHARGING"
    }
    val timeToFull = metrics.timeToFullMinutes?.let { "${it} min" } ?: "—"
    val rate = if (metrics.chargeRatePercentPerHour > 0.05) String.format(java.util.Locale.US, "%.1f %%/h", metrics.chargeRatePercentPerHour) else "—"
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0E1A), RoundedCornerShape(14.dp)).padding(14.dp)) {
        Text(powerState, color = Color(0xFF55E6FF), style = MaterialTheme.typography.titleSmall)
        Text("BATTERY %   ${metrics.batteryPercent}%")
        Text(String.format(java.util.Locale.US, "VOLTAGE   %.2f V", metrics.voltageVolts))
        Text(String.format(java.util.Locale.US, "CURRENT   %.2f A", metrics.currentAmps))
        Text(String.format(java.util.Locale.US, "POWER     %.2f W", metrics.powerWatts))
        Text(String.format(java.util.Locale.US, "TEMPERATURE   %.1f °C", metrics.temperatureCelsius))
        Text("CHARGE RATE   $rate")
        Text("TIME TO FULL   $timeToFull")
        Text("SESSION   ${metrics.sessionSeconds / 60} min")
    }
}

@Composable
private fun PreviewTab(prefs: PreferencesRepository, onLaunchOverlay: () -> Unit) {
    val theme by prefs.theme.collectAsStateWithLifecycle(initialValue = ThemeId.FUTURISTIC)
    val style = ThemeCatalog.getValue(theme)
    val fakeStatus = remember { BatteryStatusData(percent = 72, isCharging = true, isFastCharging = true, chargeMode = com.chargeanim.pro.telemetry.ChargeMode.USB, voltage = 5.02f, currentMa = 3670, wattage = 18.4f, temperatureC = 32f, elapsedChargingMs = 11 * 60_000L, sessionStartPercent = 60) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Live preview", style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        Text(theme.label, style = MaterialTheme.typography.bodyMedium, color = style.accentPrimary)
                    }
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = style.accentPrimary)
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().aspectRatio(0.58f).clip(RoundedCornerShape(24.dp)).background(Color.Black)) {
                    ChargingOverlayScreen(status = fakeStatus, theme = theme, media = com.chargeanim.pro.data.MediaSelection(null, MediaType.NONE))
                }
            }
        }
        Text("Preview uses sample charging data. Real battery telemetry is used by the charging overlay.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onLaunchOverlay, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text("Open full-screen")
        }
    }
}

@Composable
private fun SettingSwitch(
    icon: @Composable () -> Unit,
    label: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    ListItem(
        leadingContent = {
            Box(Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) { icon() }
        },
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = { Text(description, maxLines = 2) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

private fun guessType(context: android.content.Context, uri: android.net.Uri): MediaType {
    val mime = context.contentResolver.getType(uri) ?: ""
    val path = uri.toString().lowercase()
    return when {
        mime.contains("json") || path.endsWith(".json") -> MediaType.LOTTIE
        mime.contains("gif") || path.endsWith(".gif") -> MediaType.GIF
        mime.startsWith("video") || path.endsWith(".mp4") -> MediaType.MP4
        else -> MediaType.NONE
    }
}