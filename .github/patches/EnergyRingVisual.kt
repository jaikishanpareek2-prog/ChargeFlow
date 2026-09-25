package com.chargeanim.pro.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.random.Random

@Composable
fun EnergyRingVisual(
    accent: Color,
    modifier: Modifier = Modifier,
    showStarfield: Boolean = true,
    bloomIntensity: Float = 1f,
    triangleShape: Boolean = false,
    content: @Composable () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "ring-rotation")
    val slowRotation by infinite.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "slow"
    )
    val fastRotation by infinite.animateFloat(
        360f, 0f,
        infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "fast"
    )
    val pulse by infinite.animateFloat(
        0.75f, 1f,
        infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val stars = remember {
        val rnd = Random(1)
        List(50) { Triple(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat() * 0.5f + 0.1f) }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f
            if (showStarfield) {
                stars.forEach { (fx, fy, alpha) ->
                    drawCircle(
                        Color.White.copy(alpha = alpha * 0.5f),
                        1.2f,
                        Offset(fx * size.width, fy * size.height)
                    )
                }
            }
            drawCircle(
                Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.35f * pulse * bloomIntensity), Color.Transparent),
                    center,
                    maxRadius * 1.1f
                ),
                maxRadius * 1.1f,
                center
            )
            drawCircle(
                Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.55f * pulse * bloomIntensity), Color.Transparent),
                    center,
                    maxRadius * 0.55f
                ),
                maxRadius * 0.55f,
                center
            )
            if (triangleShape) {
                withTransform({ rotate(slowRotation, center) }) {
                    val r = maxRadius * 0.92f
                    val path = Path().apply {
                        for (i in 0..3) {
                            val angle = Math.toRadians((-90 + i * 120).toDouble())
                            val x = center.x + r * kotlin.math.cos(angle).toFloat()
                            val y = center.y + r * kotlin.math.sin(angle).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                    }
                    drawPath(path, accent.copy(alpha = 0.8f), style = Stroke(width = 3f))
                }
            } else {
                withTransform({ rotate(slowRotation, center) }) {
                    for (i in 0 until 8) {
                        drawArc(
                            color = accent.copy(alpha = 0.7f),
                            startAngle = i * 45f,
                            sweepAngle = 28f,
                            useCenter = false,
                            topLeft = Offset(center.x - maxRadius * 0.92f, center.y - maxRadius * 0.92f),
                            size = Size(maxRadius * 1.84f, maxRadius * 1.84f),
                            style = Stroke(width = 3f)
                        )
                    }
                }
            }
            withTransform({ rotate(fastRotation, center) }) {
                drawCircle(
                    accent.copy(alpha = 0.9f),
                    maxRadius * 0.62f,
                    center,
                    style = Stroke(width = 2f)
                )
                drawArc(
                    color = Color.White.copy(alpha = 0.9f),
                    startAngle = 0f,
                    sweepAngle = 14f,
                    useCenter = false,
                    topLeft = Offset(center.x - maxRadius * 0.62f, center.y - maxRadius * 0.62f),
                    size = Size(maxRadius * 1.24f, maxRadius * 1.24f),
                    style = Stroke(width = 4f)
                )
            }
            drawCircle(
                accent.copy(alpha = 0.25f),
                maxRadius * 0.78f,
                center,
                style = Stroke(width = 1f)
            )
        }
        content()
    }
}
