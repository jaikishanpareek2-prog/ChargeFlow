package com.chargeanim.pro.ui.main

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.chargeanim.pro.data.MediaType
import com.chargeanim.pro.data.PreferencesRepository
import com.chargeanim.pro.diagnostics.DiagnosticLog
import com.chargeanim.pro.telemetry.BatteryStatusData
import com.chargeanim.pro.ui.overlay.ChargingOverlayScreen
import com.chargeanim.pro.ui.theme.ThemeCatalog
import com.chargeanim.pro.ui.theme.ThemeId
import com.chargeanim.pro.ui.theme.ThemeVisual
import kotlinx.coroutines.launch

private enum class DashboardTab(val label: String) {
    THEMES("Themes"), SETTINGS("Settings"), PREVIEW("Preview"), DIAGNOSTICS("Diagnostics")
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
            Text("ChargeFlow", style = MaterialTheme.typography.headlineMedium)
            Text("No ads, no trackers, no root.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TabRow(selectedTabIndex = tab.ordinal) {
            DashboardTab.entries.forEach { t -> Tab(selected = tab == t, onClick = { tab = t }, text = { Text(t.label) }) }
        }
        when (tab) {
            DashboardTab.THEMES -> ThemesTab(prefs)
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
    Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF0A0E1A)).then(if (isSelected) Modifier.background(style.accentPrimary.copy(alpha = 0.08f)) else Modifier).clickable(onClick = onClick).padding(10.dp)) {
        Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(10.dp))) {
            ThemeVisual(themeId = themeId, modifier = Modifier.fillMaxSize()) { Text("72%", color = Color(0xFFEAF4FF)) }
        }
        Spacer(Modifier.height(8.dp))
        Text(themeId.label, color = Color(0xFFEAF4FF), style = MaterialTheme.typography.bodyMedium)
        if (isSelected) Text("Selected", color = style.accentPrimary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SettingsTab(prefs: PreferencesRepository) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val enabled by prefs.enabled.collectAsStateWithLifecycle(initialValue = true)
    val amoled by prefs.amoledMode.collectAsStateWithLifecycle(initialValue = true)
    val autoHide by prefs.autoHide.collectAsStateWithLifecycle(initialValue = true)
    val soundEnabled by prefs.soundEnabled.collectAsStateWithLifecycle(initialValue = false)
    val overlayGranted = remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) overlayGranted.value = Settings.canDrawOverlays(context) }
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

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        SettingSwitch("Show animation when charging", enabled) { scope.launch { prefs.setEnabled(it) } }
        Spacer(Modifier.height(12.dp))
        Text("System overlay", style = MaterialTheme.typography.titleSmall)
        Text(if (overlayGranted.value) "Enabled: ChargeFlow can appear automatically when charging." else "Required for the charging animation to appear automatically over the system.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName))) }) {
            Text(if (overlayGranted.value) "Open overlay permission" else "Enable system overlay")
        }
        SettingSwitch("AMOLED black background", amoled) { scope.launch { prefs.setAmoledMode(it) } }
        SettingSwitch("Hide automatically when unplugged", autoHide) { scope.launch { prefs.setAutoHide(it) } }
        SettingSwitch("Play a sound when charging starts", soundEnabled) { scope.launch { prefs.setSoundEnabled(it) } }
        Spacer(Modifier.height(20.dp))
        Text("Custom media (overrides the theme visual)", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { normalMediaPicker.launch("*/*") }) { Text("Normal charging…") }
            OutlinedButton(onClick = { fastMediaPicker.launch("*/*") }) { Text("Fast charging…") }
        }
    }
}

@Composable
private fun DiagnosticsTab() {
    val context = LocalContext.current
    var lines by remember { mutableStateOf(DiagnosticLog.readAll(context)) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Charging diagnostics", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { lines = DiagnosticLog.readAll(context) }) { Text("Refresh") }
                OutlinedButton(onClick = { DiagnosticLog.clear(context); lines = emptyList() }) { Text("Clear") }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Plug in the charger, then tap Refresh. The newest event is at the top.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
            lazyItems(lines) { line ->
                Text(line, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(8.dp))
            }
        }
    }
}

@Composable
private fun PreviewTab(prefs: PreferencesRepository, onLaunchOverlay: () -> Unit) {
    val theme by prefs.theme.collectAsStateWithLifecycle(initialValue = ThemeId.FUTURISTIC)
    val fakeStatus = remember { BatteryStatusData(percent = 72, isCharging = true, isFastCharging = true, chargeMode = com.chargeanim.pro.telemetry.ChargeMode.USB, voltage = 5.02f, currentMa = 3670, wattage = 18.4f, temperatureC = 32f, elapsedChargingMs = 11 * 60_000L, sessionStartPercent = 60) }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.padding(24.dp).aspectRatio(0.5f).clip(RoundedCornerShape(28.dp)).background(Color.Black)) {
            ChargingOverlayScreen(status = fakeStatus, theme = theme, media = com.chargeanim.pro.data.MediaSelection(null, MediaType.NONE))
        }
        Text("Preview with sample data — tap Settings to enable the system overlay before charging.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(12.dp))
        Button(onClick = onLaunchOverlay) { Text("Open full-screen now") }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
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