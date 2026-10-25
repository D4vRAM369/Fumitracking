package com.d4vram.cbdcounter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import kotlin.math.min

/**
 * Dos anillos concéntricos: CBD fuera, THC dentro. Solo dibuja; las reglas (estado,
 * relleno, exceso) viven en [RingProgress]. Los colores se leen del tema en cada dibujo,
 * así que el cambio claro/oscuro se refleja sin lógica extra.
 */
class RingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var cbd = 0
    private var thc = 0
    private var cbdGoal = 0
    private var thcGoal = 0

    // Los Paint se crean una vez: crear objetos dentro de onDraw castiga el rendimiento.
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val oval = RectF()

    /** [animate] se usa en la Task 7; de momento pinta directo. */
    fun setProgress(cbd: Int, thc: Int, cbdGoal: Int, thcGoal: Int, animate: Boolean = false) {
        this.cbd = cbd
        this.thc = thc
        this.cbdGoal = cbdGoal
        this.thcGoal = thcGoal
        invalidate()
    }

    /** Color del anillo o del número según el estado (NO_GOAL y UNDER usan el color propio). */
    fun colorFor(isThc: Boolean, state: RingState): Int = ContextCompat.getColor(
        context,
        when (state) {
            RingState.AT_LIMIT -> R.color.ring_limit
            RingState.OVER -> R.color.ring_over
            else -> if (isThc) R.color.ring_thc else R.color.ring_cbd
        }
    )

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Cuadrada: el menor de los dos lados disponibles.
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val side = when {
            MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED -> h
            MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED -> w
            else -> min(w, h)
        }
        setMeasuredDimension(side, side)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        val stroke = size * 0.085f
        val gap = stroke * 0.45f
        val center = size / 2f
        val outerRadius = center - stroke / 2f
        val innerRadius = outerRadius - stroke - gap

        drawRing(canvas, center, outerRadius, stroke, cbd, cbdGoal, isThc = false)
        drawRing(canvas, center, innerRadius, stroke, thc, thcGoal, isThc = true)
    }

    private fun drawRing(
        canvas: Canvas, center: Float, radius: Float, stroke: Float,
        count: Int, goal: Int, isThc: Boolean
    ) {
        oval.set(center - radius, center - radius, center + radius, center + radius)
        paint.strokeWidth = stroke

        // 1) Pista: siempre visible, es lo único que se ve si no hay meta.
        paint.color = ContextCompat.getColor(context, R.color.ring_track)
        canvas.drawArc(oval, 0f, 360f, false, paint)

        val state = RingProgress.state(count, goal)
        val sweep = 360f * RingProgress.fraction(count, goal)
        if (sweep <= 0f) return

        // 2) Primera vuelta: arranca arriba (-90°) y avanza en sentido horario.
        val color = colorFor(isThc, state)
        paint.color = color
        canvas.drawArc(oval, -90f, sweep, false, paint)

        // 3) Segunda vuelta fina y más clara, solo si te pasas de la meta.
        val overflow = 360f * RingProgress.overflowFraction(count, goal)
        if (overflow > 0f) {
            paint.strokeWidth = stroke * 0.4f
            paint.color = ColorUtils.blendARGB(color, Color.WHITE, 0.55f)
            canvas.drawArc(oval, -90f, overflow, false, paint)
        }
    }
}
