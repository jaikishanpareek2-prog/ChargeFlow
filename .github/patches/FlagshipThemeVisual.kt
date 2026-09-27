package com.chargeanim.pro.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import com.chargeflow.theme.FlagshipTheme
import com.chargeflow.theme.FlagshipThemes
import com.chargeflow.theme.ThemeId
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FlagshipThemeVisual(
    themeId: ThemeId,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    content: @Composable () -> Unit = {}
) {
    val theme = FlagshipThemes.get(themeId)
    val transition = rememberInfiniteTransition(label = "flagship-\${theme.id.name}")
    val phase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween((9000f / theme.pulseSpeed.coerceAtLeast(0.1f)).toInt(), easing = LinearEasing)),
        label = "phase"
    )
    val pulse by transition.animateFloat(
        0.78f, 1f,
        infiniteRepeatable(tween((1400f / theme.pulseSpeed.coerceAtLeast(0.1f)).toInt(), easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val particles = remember(theme.id) {
        val r = Random(theme.id.ordinal * 7919 + 17)
        List(theme.particleCount.coerceAtMost(180)) {
            ParticleSeed(r.nextFloat(), r.nextFloat(), r.nextFloat(), r.nextFloat(), r.nextInt(0, theme.particleColors.size))
        }
    }
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.45f
            val glow = theme.glowIntensity.coerceIn(0f, 1f) * if (active) pulse else 0.35f
            drawRect(theme.background)
            drawCircle(Brush.radialGradient(listOf(theme.primary.copy(alpha = 0.28f * glow), theme.secondary.copy(alpha = 0.08f * glow), Color.Transparent), center, radius * 1.15f), radius * 1.15f, center)

            particles.forEach { p ->
                val t = (phase * theme.particleSpeed.start + p.phase) % 1f
                val x = if (theme.hasMatrix) p.x * size.width else (p.x + sin((t + p.y) * 6.283f) * 0.04f) * size.width
                val baseY = if (theme.upwardBias) 1f - t else t
                val y = if (theme.gravity > 0f) (p.y + t * (0.35f + theme.gravity * 0.08f)) % 1f * size.height else (baseY + p.y * 0.15f) % 1f * size.height
                val color = theme.particleColors[p.colorIndex.coerceIn(theme.particleColors.indices)]
                val sizePx = theme.particleSize.start + (theme.particleSize.endInclusive - theme.particleSize.start) * p.size
                val alpha = (0.35f + 0.65f * (1f - p.size)) * if (active) 1f else 0.35f
                if (theme.hasMatrix) drawRect(color.copy(alpha = alpha), Offset(x, y), androidx.compose.ui.geometry.Size(sizePx, sizePx * 2.4f))
                else drawCircle(color.copy(alpha = alpha), sizePx, Offset(x, y))
            }

            if (theme.hasRibbon) {
                repeat(3) { band ->
                    val path = Path()
                    for (i in 0..40) {
                        val x = i / 40f * size.width
                        val y = center.y + (band - 1) * radius * 0.34f + sin(i / 40f * 6.283f + phase * 6.283f + band) * radius * 0.13f
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, theme.ringColors[band % theme.ringColors.size].copy(alpha = 0.72f), style = Stroke(width = theme.ringStroke))
                }
            } else {
                val rotation = phase * 360f
                val segments = theme.ringSegments.coerceIn(8, 96)
                for (ring in 0..1) {
                    val rr = radius * (0.72f + ring * 0.16f)
                    val color = theme.ringColors[ring % theme.ringColors.size]
                    for (i in 0 until segments) if (i % 3 != 2) {
                        drawArc(color=color.copy(alpha=0.72f*glow), startAngle=rotation+i*(360f/segments), sweepAngle=(360f/segments)*0.62f, useCenter=false, topLeft=Offset(center.x-rr,center.y-rr), size=androidx.compose.ui.geometry.Size(rr*2f,rr*2f), style=Stroke(width=theme.ringStroke))
                    }
                }
            }

            if (theme.hasRipple) repeat(4) { i ->
                val rr = radius * (0.35f + ((phase + i * 0.22f) % 1f) * 0.75f)
                drawCircle(theme.accent.copy(alpha=0.28f*glow), rr, center, style=Stroke(theme.ringStroke))
            }

            if (theme.hasLightning) {
                val path=Path()
                path.moveTo(center.x-radius*0.35f,center.y-radius*0.8f)
                path.lineTo(center.x+radius*0.05f,center.y-radius*0.12f)
                path.lineTo(center.x-radius*0.08f,center.y-radius*0.12f)
                path.lineTo(center.x+radius*0.4f,center.y+radius*0.78f)
                drawPath(path,theme.accent.copy(alpha=0.85f*glow),style=Stroke(width=theme.ringStroke*0.9f))
            }

            if (theme.hasCrystals) repeat(6) { i ->
                val a=i*60f+phase*40f
                val rr=radius*0.78f
                val c=Offset(center.x+cos(Math.toRadians(a.toDouble())).toFloat()*rr,center.y+sin(Math.toRadians(a.toDouble())).toFloat()*rr)
                val path=Path().apply{moveTo(c.x,c.y-10);lineTo(c.x+8,c.y);lineTo(c.x,c.y+14);lineTo(c.x-8,c.y);close()}
                drawPath(path,theme.accent.copy(alpha=0.7f),style=Stroke(2f))
            }
        }
        content()
    }
}
private data class ParticleSeed(val x: Float,val y: Float,val size: Float,val phase: Float,val colorIndex: Int)
