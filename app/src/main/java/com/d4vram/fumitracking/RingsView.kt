package com.d4vram.fumitracking

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.animation.ValueAnimator
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
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

    // Lo que se está dibujando ahora mismo (0..1). Durante la animación va del valor anterior al nuevo.
    private var shownCbd = 0f
    private var shownCbdOver = 0f
    private var shownThc = 0f
    private var shownThcOver = 0f
    private var animator: ValueAnimator? = null

    /**
     * [animate] = true interpola el llenado desde lo que se veía hasta lo nuevo (400 ms).
     * Con false (primer dibujo, volver a la app) pinta directo. Si el sistema tiene las
     * animaciones desactivadas, ValueAnimator ya salta al final por su cuenta.
     */
    fun setProgress(cbd: Int, thc: Int, cbdGoal: Int, thcGoal: Int, animate: Boolean = false) {
        this.cbd = cbd
        this.thc = thc
        this.cbdGoal = cbdGoal
        this.thcGoal = thcGoal

        val target = floatArrayOf(
            RingProgress.fraction(cbd, cbdGoal), RingProgress.overflowFraction(cbd, cbdGoal),
            RingProgress.fraction(thc, thcGoal), RingProgress.overflowFraction(thc, thcGoal)
        )
        animator?.cancel()
        if (!animate || !isLaidOut) {
            shownCbd = target[0]; shownCbdOver = target[1]
            shownThc = target[2]; shownThcOver = target[3]
            invalidate()
            return
        }
        val from = floatArrayOf(shownCbd, shownCbdOver, shownThc, shownThcOver)
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val t = it.animatedValue as Float
                shownCbd = from[0] + (target[0] - from[0]) * t
                shownCbdOver = from[1] + (target[1] - from[1]) * t
                shownThc = from[2] + (target[2] - from[2]) * t
                shownThcOver = from[3] + (target[3] - from[3]) * t
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()  // evita que la animación siga viva con la vista ya destruida
        super.onDetachedFromWindow()
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

        drawRing(canvas, center, outerRadius, stroke, cbd, cbdGoal, shownCbd, shownCbdOver, isThc = false)
        drawRing(canvas, center, innerRadius, stroke, thc, thcGoal, shownThc, shownThcOver, isThc = true)
    }

    private fun drawRing(
        canvas: Canvas, center: Float, radius: Float, stroke: Float,
        count: Int, goal: Int, fraction: Float, overflowFraction: Float, isThc: Boolean
    ) {
        oval.set(center - radius, center - radius, center + radius, center + radius)
        paint.strokeWidth = stroke

        // 1) Pista: siempre visible, es lo único que se ve si no hay meta.
        paint.color = ContextCompat.getColor(context, R.color.ring_track)
        canvas.drawArc(oval, 0f, 360f, false, paint)

        val state = RingProgress.state(count, goal)
        val sweep = 360f * fraction
        if (sweep <= 0f) return

        // 2) Primera vuelta: arranca arriba (-90°) y avanza en sentido horario.
        val color = colorFor(isThc, state)
        paint.color = color
        canvas.drawArc(oval, -90f, sweep, false, paint)

        // 3) Segunda vuelta fina y más clara, solo si te pasas de la meta.
        val overflow = 360f * overflowFraction
        if (overflow > 0f) {
            paint.strokeWidth = stroke * 0.4f
            paint.color = ColorUtils.blendARGB(color, Color.WHITE, 0.55f)
            canvas.drawArc(oval, -90f, overflow, false, paint)
        }
    }
}
