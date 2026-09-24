package com.chargeanim.pro.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chargeanim.pro.ui.theme.ThemeType

@Composable
fun VibesTab() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(ThemeType.entries.toList()) { type -> VibeCard(type) }
    }
}

@Composable
private fun VibeCard(type: ThemeType) {
    Column(
        Modifier.fillMaxWidth().background(Color(0xFF080B14), RoundedCornerShape(16.dp)).padding(10.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(130.dp).background(Color.Black, RoundedCornerShape(10.dp))) {
            VibeVisual(type, Modifier.fillMaxSize())
            androidx.compose.material3.Text("72%", color = Color(0xFFEAF4FF), fontSize = 28.sp, modifier = Modifier.align(Alignment.Center))
        }
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.Text(type.label, color = Color(0xFFEAF4FF), style = MaterialTheme.typography.bodyMedium)
        androidx.compose.material3.Text("Soft ambient vibe", color = Color(0xFF8793A8), style = MaterialTheme.typography.labelSmall)
    }
}

private val ThemeType.label: String
    get() = when (this) {
        ThemeType.MIDNIGHT_GARDEN -> "Midnight Garden"
        ThemeType.CELESTIAL_SPARKLE -> "Celestial Sparkle"
        ThemeType.ENCHANTED_FOREST -> "Enchanted Forest"
        ThemeType.OCEAN_ABYSS -> "Ocean Abyss"
    }

@Composable
private fun VibeVisual(type: ThemeType, modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "vibe")
    val pulse by transition.animateFloat(
        0.65f, 1f,
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val accent = when (type) {
        ThemeType.MIDNIGHT_GARDEN -> Color(0xFFFF70C8)
        ThemeType.CELESTIAL_SPARKLE -> Color(0xFF9EA8FF)
        ThemeType.ENCHANTED_FOREST -> Color(0xFF72FF9A)
        ThemeType.OCEAN_ABYSS -> Color(0xFF48D9FF)
    }

    Canvas(modifier.background(
        when (type) {
            ThemeType.MIDNIGHT_GARDEN -> Brush.verticalGradient(listOf(Color(0xFF160A20), Color(0xFF05060D)))
            ThemeType.CELESTIAL_SPARKLE -> Brush.verticalGradient(listOf(Color(0xFF0B1230), Color(0xFF03050E)))
            ThemeType.ENCHANTED_FOREST -> Brush.verticalGradient(listOf(Color(0xFF0B241A), Color(0xFF030A07)))
            ThemeType.OCEAN_ABYSS -> Brush.verticalGradient(listOf(Color(0xFF06253A), Color(0xFF020910)))
        }
    )) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension * 0.28f
        drawCircle(accent.copy(alpha = 0.12f * pulse), r * 1.9f, center)
        drawCircle(accent.copy(alpha = 0.28f * pulse), r, center, style = androidx.compose.ui.graphics.drawscope.Stroke(3f))

        when (type) {
            ThemeType.MIDNIGHT_GARDEN -> repeat(10) { i ->
                val a = i * (2f * Math.PI.toFloat() / 10f)
                drawCircle(Color(0xFFFF8EDB).copy(alpha = 0.35f + 0.3f * pulse), 4f,
                    Offset(center.x + kotlin.math.cos(a) * r * 1.45f, center.y + kotlin.math.sin(a) * r * 1.45f))
            }
            ThemeType.CELESTIAL_SPARKLE -> {
                repeat(24) { i ->
                    drawCircle(Color.White.copy(alpha = 0.35f + 0.45f * pulse), if (i % 5 == 0) 2.2f else 1f,
                        Offset((i * 37 % size.width.toInt()).toFloat(), (i * 19 % size.height.toInt()).toFloat()))
                }
                drawCircle(Color(0xFFF3E8FF).copy(alpha = 0.85f), r * 0.55f, Offset(center.x + r * 0.55f, center.y - r * 0.55f))
                drawCircle(Color(0xFF0B1230), r * 0.55f, Offset(center.x + r * 0.8f, center.y - r * 0.7f))
            }
            ThemeType.ENCHANTED_FOREST -> {
                repeat(14) { i ->
                    drawCircle(Color(0xFF9CFFB5).copy(alpha = 0.25f + 0.45f * pulse), 1.5f + (i % 3),
                        Offset((i * 29 % size.width.toInt()).toFloat(), (i * 47 % size.height.toInt()).toFloat()))
                }
                drawCircle(Color(0xFF75FF9A).copy(alpha = 0.18f * pulse), r * 1.45f, center)
            }
            ThemeType.OCEAN_ABYSS -> {
                repeat(3) { i ->
                    drawCircle(Color(0xFF55D9FF).copy(alpha = (0.2f - i * 0.04f) * pulse), r * (1.1f + i * 0.28f), center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                }
                drawCircle(Color(0xFF7DEBFF).copy(alpha = 0.7f * pulse), 3f, Offset(center.x - r * 0.9f, center.y + r * 0.3f))
            }
        }
    }
}
