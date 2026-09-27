package com.chargeanim.pro.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chargeanim.pro.ui.theme.ThemeType
import com.chargeanim.pro.ui.theme.VibesThemeManager
import com.chargeanim.pro.ui.theme.VibeVisual
import com.chargeanim.pro.ui.theme.FlagshipThemeVisual
import com.chargeflow.theme.ThemeId as FlagshipThemeId

@Composable
fun VibesTab() {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(VibesThemeManager.getSelectedTheme(context)) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            androidx.compose.material3.Text("VIBES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            androidx.compose.material3.Text("Atmosphere, not just color.", style = MaterialTheme.typography.headlineSmall)
            androidx.compose.material3.Text("Ambient presets designed to make the charging moment feel alive.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items = ThemeType.entries, key = { it.name }) { type ->
            VibeCard(type, type == selected) {
                VibesThemeManager.setSelectedTheme(context, type)
                selected = type
            }
        }
        }
    }
}

@Composable
private fun VibeCard(type: ThemeType, isSelected: Boolean, onClick: () -> Unit) {
    val accent = VibesThemeManager.accent(type)
    Column(
        Modifier.fillMaxWidth()
            .background(if (isSelected) accent.copy(alpha = 0.13f) else MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(26.dp))
            .border(if (isSelected) 1.5.dp else 0.dp, if (isSelected) accent.copy(alpha = 0.7f) else Color.Transparent, RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(148.dp).background(Color.Black, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
            if (type != ThemeType.NONE) {
                val flagshipId = when (type) {
                    ThemeType.MIDNIGHT_GARDEN -> FlagshipThemeId.MIDNIGHT_GARDEN
                    ThemeType.CELESTIAL_SPARKLE -> FlagshipThemeId.CELESTIAL_SPARKLE
                    ThemeType.ENCHANTED_FOREST -> FlagshipThemeId.ENCHANTED_FOREST
                    ThemeType.OCEAN_ABYSS -> FlagshipThemeId.OCEAN_ABYSS
                    ThemeType.NONE -> FlagshipThemeId.FUTURISTIC
                }
                FlagshipThemeVisual(flagshipId, Modifier.fillMaxSize(), active = true)
                androidx.compose.material3.Text("72%", color = Color.White, fontSize = 28.sp)
            } else {
                androidx.compose.material3.Text("BASE", color = Color(0xFF8793A8), fontSize = 22.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        androidx.compose.material3.Text(VibesThemeManager.label(type), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
        androidx.compose.material3.Text(if (isSelected) "ACTIVE EXPERIENCE" else if (type == ThemeType.NONE) "Use the selected flagship theme" else "Ambient motion preset", color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 3.dp))
    }
}
