package com.chargeanim.pro.ui.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.sin

class FlowingWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var flow = 0f
    private var accentStart = 0xFF00E5FF.toInt()
    private var accentEnd = 0xFF7C4DFF.toInt()
    private var animator: ValueAnimator? = null

    init {
        isClickable = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)
        val centerY = h * 0.52f
        val travel = flow * w
        val gradient = LinearGradient(
            0f, 0f, w, 0f,
            intArrayOf(0x00100000, accentStart, accentEnd, accentStart, 0x00100000),
            floatArrayOf(0f, .18f, .5f, .82f, 1f),
            Shader.TileMode.CLAMP
        )

        drawLayer(canvas, centerY, w, h, .00f, 1.0f, 3.0f, 150, gradient)
        drawLayer(canvas, centerY, w, h, .17f, .72f, 1.7f, 105, gradient)
        drawLayer(canvas, centerY, w, h, -.14f, .48f, 1.1f, 80, gradient)

        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = accentEnd
        paint.alpha = 235
        canvas.drawCircle(travel, centerY, 4.2f, paint)
        paint.alpha = 75
        canvas.drawCircle(travel, centerY, 10f, paint)

        val portX = w / 2f
        val convergence = Path()
        convergence.moveTo(portX - 28f, h - 2f)
        convergence.quadTo(portX, h * .72f, portX + 28f, h - 2f)
        paint.shader = gradient
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.4f
        paint.alpha = 120
        canvas.drawPath(convergence, paint)
        paint.shader = null
        paint.alpha = 220
        canvas.drawCircle(portX, h - 3f, 3.5f, paint)
        paint.alpha = 255
    }

    private fun drawLayer(
        canvas: Canvas,
        baseY: Float,
        width: Float,
        height: Float,
        offset: Float,
        amplitudeScale: Float,
        stroke: Float,
        alpha: Int,
        shader: Shader
    ) {
        paint.shader = shader
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.alpha = alpha
        val path = Path()
        val cycles = 1.65f
        for (x in 0..width.toInt()) {
            val xx = x.toFloat()
            val primary = sin((xx / width) * cycles * PI * 2.0 + flow * PI * 2.0 + offset).toFloat()
            val secondary = sin((xx / width) * cycles * PI * 4.0 - flow * PI * 3.0 + offset * 1.7).toFloat() * .28f
            val y = baseY + (primary + secondary) * height * .11f * amplitudeScale
            if (x == 0) path.moveTo(xx, y) else path.lineTo(xx, y)
        }
        canvas.drawPath(path, paint)
    }

    fun setAccentColors(primary: Int, secondary: Int) {
        accentStart = primary
        accentEnd = secondary
        invalidate()
    }

    fun startFlow(durationMs: Long = 2600L) {
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener {
                flow = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun stopFlow() {
        animator?.cancel()
        animator = null
    }

    override fun onDetachedFromWindow() {
        stopFlow()
        super.onDetachedFromWindow()
    }
}

object ChargerHaptics {
    fun trigger(view: View) {
        view.performHapticFeedback(
            HapticFeedbackConstants.KEYBOARD_TAP,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }
}
