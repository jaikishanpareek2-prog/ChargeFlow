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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chargeanim.pro.ui.theme.ThemeType
import com.chargeanim.pro.ui.theme.VibesThemeManager

@Composable
fun VibesTab() {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(VibesThemeManager.getSelectedTheme(context)) }

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

@Composable
private fun VibeCard(type: ThemeType, isSelected: Boolean, onClick: () -> Unit) {
    val accent = VibesThemeManager.accent(type)
    Column(
        Modifier.fillMaxWidth()
            .background(if (isSelected) accent.copy(alpha = 0.12f) else Color(0xFF080B14), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(130.dp).background(Color.Black, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            if (type != ThemeType.NONE) {
                Box(Modifier.size(76.dp).background(accent.copy(alpha = 0.16f), RoundedCornerShape(38.dp)))
                Box(Modifier.size(52.dp).background(accent.copy(alpha = 0.10f), RoundedCornerShape(26.dp)))
                androidx.compose.material3.Text("72%", color = Color(0xFFEAF4FF), fontSize = 28.sp)
            } else {
                androidx.compose.material3.Text("Theme", color = Color(0xFF8793A8), fontSize = 22.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.Text(VibesThemeManager.label(type), color = Color(0xFFEAF4FF), style = MaterialTheme.typography.bodyMedium)
        androidx.compose.material3.Text(
            if (isSelected) "Selected" else if (type == ThemeType.NONE) "Use normal theme visual" else "Soft ambient vibe",
            color = if (isSelected) accent else Color(0xFF8793A8),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
