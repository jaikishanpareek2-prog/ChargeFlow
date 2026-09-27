package com.chargeanim.pro.ui.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.LinearInterpolator

class FlowingWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var flow = 0f
    private var accentStart = 0xFF7C4DFF.toInt()
    private var accentEnd = 0xFF00E5FF.toInt()
    private var animator: ValueAnimator? = null

    init {
        isClickable = false
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()
        val w = width.toFloat().coerceAtLeast(1f)
        val y = h * flow

        paint.shader = LinearGradient(
            0f, 0f, w, 0f,
            intArrayOf(0x001F8BFF, accentStart, accentEnd, 0x001F8BFF),
            floatArrayOf(0f, 0.28f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.strokeWidth = 3.5f
        paint.style = Paint.Style.STROKE

        val path = android.graphics.Path()
        val amplitude = w * 0.028f
        val cycles = 2.4f
        path.moveTo(0f, y)
        for (x in 0..w.toInt()) {
            val xx = x.toFloat()
            val yy = y + kotlin.math.sin((xx / w) * cycles * Math.PI * 2.0 + flow * Math.PI * 2.0).toFloat() * amplitude
            path.lineTo(xx, yy)
        }
        canvas.drawPath(path, paint)

        paint.shader = null
        paint.style = Paint.Style.FILL
        val pulseX = (flow * w).coerceIn(0f, w)
        paint.color = accentEnd
        paint.alpha = 210
        canvas.drawCircle(pulseX, y, 4.5f, paint)
        paint.alpha = 255
    }

    fun setAccentColors(primary: Int, secondary: Int) {
        accentStart = primary
        accentEnd = secondary
        invalidate()
    }
    fun startFlow(durationMs: Long = 2000L) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(1f, 0f).apply {
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
