package com.chargeanim.pro.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.chargeanim.pro.service.ChargingService
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chargeanim.pro.data.PreferencesRepository
import com.chargeanim.pro.telemetry.BatteryStatusData
import com.chargeanim.pro.telemetry.BatteryTelemetryManager
import com.chargeanim.pro.ui.theme.ThemeId
import com.chargeanim.pro.ui.theme.ThemeVisual
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private enum class DashTab(val label: String) {
    SKINS("Skins"), VIBES("Vibes"), CONFIG("Config"), MONITOR("Monitor"), TELEMETRY("Telemetry")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainTabDashboard(prefs: PreferencesRepository, telemetry: BatteryTelemetryManager? = null, onLaunchOverlay: () -> Unit = {}) {
    val pagerState = rememberPagerState(pageCount = { DashTab.entries.size })
    val scope = rememberCoroutineScope()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text("ChargeFlow", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text("Premium charging experience", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                DashTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge) }
                    )
                }
            }
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (DashTab.entries[page]) {
                    DashTab.SKINS -> SkinsTab(prefs)
                    DashTab.VIBES -> VibesTab(prefs)
                    DashTab.CONFIG -> ConfigTab(prefs)
                    DashTab.MONITOR -> MonitorTab(telemetry)
                    DashTab.TELEMETRY -> TelemetryTab(telemetry)
                }
            }
        }
    }
}

@Composable
private fun SkinsTab(prefs: PreferencesRepository) {
    val selected by prefs.theme.collectAsStateWithLifecycle(ThemeId.FUTURISTIC)
    val scope = rememberCoroutineScope()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(ThemeId.entries.toList(), key = { it.name }) { id ->
            val isSelected = id == selected
            ElevatedCard(
                onClick = { scope.launch { prefs.setTheme(id) } },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isSelected) 6.dp else 1.dp),
                modifier = Modifier.fillMaxWidth().aspectRatio(0.85f)
            ) {
                Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)
                    ) {
                        ThemeVisual(themeId = id, modifier = Modifier.fillMaxSize()) {}
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(id.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (isSelected) Text("Selected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

private enum class VibeId(val label: String, val accent: Color) {
    MIDNIGHT_GARDEN("Midnight Garden", Color(0xFF69F0AE)),
    CELESTIAL_SPARKLE("Celestial Sparkle", Color(0xFFFFD740)),
    ENCHANTED_FOREST("Enchanted Forest", Color(0xFF00C853)),
    OCEAN_ABYSS("Ocean Abyss", Color(0xFF00B8D4))
}

@Composable
private fun VibesTab(@Suppress("UNUSED_PARAMETER") prefs: PreferencesRepository) {
    var selected by remember { mutableStateOf(VibeId.MIDNIGHT_GARDEN) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(VibeId.entries.toList(), key = { it.name }) { vibe ->
            val isSelected = vibe == selected
            ElevatedCard(
                onClick = { selected = vibe },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth().height(140.dp)
            ) {
                Box(
                    Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(vibe.accent.copy(alpha = 0.25f), Color.Transparent))).padding(16.dp)
                ) {
                    Column(Modifier.align(Alignment.BottomStart)) {
                        Text(vibe.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        if (isSelected) Text("Active", style = MaterialTheme.typography.labelSmall, color = vibe.accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigTab(prefs: PreferencesRepository) {
    val enabled by prefs.enabled.collectAsStateWithLifecycle(true)
    val amoled by prefs.amoledMode.collectAsStateWithLifecycle(true)
    val autoHide by prefs.autoHide.collectAsStateWithLifecycle(true)
    val sound by prefs.soundEnabled.collectAsStateWithLifecycle(false)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Animation", style = MaterialTheme.typography.titleMedium)
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Charging overlay", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (Settings.canDrawOverlays(context))
                        "Overlay access is enabled. ChargeFlow can display the full screen charging experience."
                    else
                        "Allow display over other apps so ChargeFlow can show the charging animation while the device is locked.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = {
                    if (Settings.canDrawOverlays(context)) {
                        ContextCompat.startForegroundService(
                            context,
                            Intent(context, ChargingService::class.java).setAction(ChargingService.ACTION_MONITOR)
                        )
                    } else {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }) {
                    Text(if (Settings.canDrawOverlays(context)) "Start charging monitor" else "Grant overlay access")
                }
            }
        }
        ElevatedCard(shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mode", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = autoHide, onClick = { scope.launch { prefs.setAutoHide(true) } }, label = { Text("Temporary") })
                    FilterChip(selected = !autoHide, onClick = { scope.launch { prefs.setAutoHide(false) } }, label = { Text("Always On") })
                }
                Text(
                    if (autoHide) "Shows while locked and charging. Hides after unlock." else "Stays visible while charging, including after unlock.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        SettingsSwitchRow("Charging animation", enabled) { scope.launch { prefs.setEnabled(it) } }
        SettingsSwitchRow("AMOLED background", amoled) { scope.launch { prefs.setAmoledMode(it) } }
        SettingsSwitchRow("Charging sound", sound) { scope.launch { prefs.setSoundEnabled(it) } }
    }
}

@Composable
private fun SettingsSwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        ListItem(
            headlineContent = { Text(title) },
            trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun MonitorTab(telemetry: BatteryTelemetryManager?) {
    val fallback = remember { MutableStateFlow(BatteryStatusData()) }
    val status by (telemetry?.state ?: fallback).collectAsStateWithLifecycle(BatteryStatusData())
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${status.percent}%", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            when { !status.isCharging -> "Not charging"; status.isFastCharging -> "Fast charging"; else -> "Charging" },
            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Power", status.wattage?.let { "%.1f W".format(it) } ?: "—", Modifier.weight(1f))
            MetricCard("Voltage", status.voltage?.let { "%.2f V".format(it) } ?: "—", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Current", status.currentMa?.let { "%.2f A".format(it / 1000f) } ?: "—", Modifier.weight(1f))
            MetricCard("Temp", status.temperatureC?.let { "%.0f°C".format(it) } ?: "—", Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun TelemetryTab(telemetry: BatteryTelemetryManager?) {
    val fallback = remember { MutableStateFlow(BatteryStatusData()) }
    val status by (telemetry?.state ?: fallback).collectAsStateWithLifecycle(BatteryStatusData())
    val stateLabel = when { status.isCharging && status.percent >= 100 -> "FULL"; status.isCharging -> "ACTIVE"; else -> "NOT CHARGING" }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(
            onClick = {},
            label = { Text(stateLabel) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = when (stateLabel) {
                    "ACTIVE" -> MaterialTheme.colorScheme.primaryContainer
                    "FULL" -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                }
            )
        )
        TelemetryRow("Battery", "${status.percent}%")
        TelemetryRow("Mode", status.chargeMode.name)
        TelemetryRow("Voltage", if (status.isCharging) status.voltage?.let { "%.3f V".format(it) } ?: "—" else "—")
        TelemetryRow("Current", if (status.isCharging) status.currentMa?.let { "${it} mA" } ?: "—" else "—")
        TelemetryRow("Power", if (status.isCharging) status.wattage?.let { "%.2f W".format(it) } ?: "—" else "—")
        TelemetryRow("Temperature", status.temperatureC?.let { "%.1f °C".format(it) } ?: "—")
        TelemetryRow("Fast charge", if (status.isFastCharging) "Yes" else "No")
        TelemetryRow("Session", status.sessionStartPercent?.let { "from $it%" } ?: "—")
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        trailingContent = { Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
    )
}
