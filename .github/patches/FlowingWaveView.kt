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
    private var animator: ValueAnimator? = null

    init {
        isClickable = false
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()
        val w = width.toFloat()
        val y = h * flow

        paint.shader = LinearGradient(
            0f, y - h * 0.18f, 0f, y + h * 0.18f,
            intArrayOf(0x001F8BFF, 0xAA8A5CFF.toInt(), 0x001F8BFF),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE

        val path = android.graphics.Path()
        val amplitude = w * 0.035f
        val cycles = 3.5f
        path.moveTo(0f, y)
        for (x in 0..w.toInt()) {
            val xx = x.toFloat()
            val yy = y + kotlin.math.sin((xx / w) * cycles * Math.PI * 2.0).toFloat() * amplitude
            path.lineTo(xx, yy)
        }
        canvas.drawPath(path, paint)
        paint.shader = null
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
