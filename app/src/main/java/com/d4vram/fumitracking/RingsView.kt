package com.d4vram.fumitracking

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import kotlin.math.min

/**
 * Dos anillos concéntricos: CBD fuera, THC dentro. Cada toma es un segmento con hueco,
 * coloreado por tipo (CBD azul, weed verde, polen marrón) y en orden de hora.
 * Solo dibuja; las reglas (segmentos, vueltas, estado) viven en [RingProgress].
 * Los colores se leen del tema en cada dibujo, así el cambio claro/oscuro sale gratis.
 */
class RingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var cbdSegments: List<RingSegment> = emptyList()
    private var thcSegments: List<RingSegment> = emptyList()

    // Los Paint se crean una vez: crear objetos dentro de onDraw castiga el rendimiento.
    // BUTT (borde recto) y no ROUND: con borde redondo las puntas se comen el hueco entre tomas.
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    private val oval = RectF()

    // Cuánto del anillo se ve (0..1). La animación lo lleva de 0 a 1: los segmentos "se dibujan".
    private var reveal = 1f
    private var animator: ValueAnimator? = null

    /**
     * [note] es la nota del día: de ella salen el tipo y el orden de cada toma.
     * [animate] = true redibuja los anillos girando en 400 ms. Con false pinta directo.
     * Si el sistema tiene las animaciones desactivadas, ValueAnimator salta al final solo.
     */
    fun setProgress(cbd: Int, thc: Int, cbdGoal: Int, thcGoal: Int, note: String?, animate: Boolean = false) {
        val kinds = RingProgress.kindsFromNote(note)
        cbdSegments = RingProgress.segments(kinds.filter { it == IntakeKind.CBD }, cbd, cbdGoal, IntakeKind.CBD)
        thcSegments = RingProgress.segments(kinds.filter { it != IntakeKind.CBD }, thc, thcGoal, IntakeKind.WEED)

        animator?.cancel()
        if (!animate || !isLaidOut) {
            reveal = 1f
            invalidate()
            return
        }
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                reveal = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()  // evita que la animación siga viva con la vista ya destruida
        super.onDetachedFromWindow()
    }

    /** Color del número según el estado (NO_GOAL y UNDER usan el color propio). */
    fun colorFor(isThc: Boolean, state: RingState): Int = ContextCompat.getColor(
        context,
        when (state) {
            RingState.AT_LIMIT -> R.color.ring_limit
            RingState.OVER -> R.color.ring_over
            else -> if (isThc) R.color.ring_thc else R.color.ring_cbd
        }
    )

    private fun colorOf(kind: IntakeKind): Int = ContextCompat.getColor(
        context,
        when (kind) {
            IntakeKind.CBD -> R.color.ring_seg_cbd
            IntakeKind.WEED -> R.color.ring_seg_weed
            IntakeKind.POLEN -> R.color.ring_seg_polen
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

        drawRing(canvas, center, outerRadius, stroke, cbdSegments)
        drawRing(canvas, center, innerRadius, stroke, thcSegments)
    }

    private fun drawRing(canvas: Canvas, center: Float, radius: Float, stroke: Float, segments: List<RingSegment>) {
        oval.set(center - radius, center - radius, center + radius, center + radius)

        // 1) Pista: siempre visible, es lo único que se ve sin tomas.
        paint.strokeWidth = stroke
        paint.color = ContextCompat.getColor(context, R.color.ring_track)
        canvas.drawArc(oval, 0f, 360f, false, paint)

        // 2) Un arco por toma: arranca arriba (-90°) y avanza en sentido horario.
        //    La segunda vuelta (te pasaste) va más fina, encima de la primera.
        for (seg in segments) {
            val gapDeg = min(GAP_DEG, seg.sweep * 360f * 0.5f)  // con muchas tomas el hueco no se come el arco
            val start = seg.start * 360f + gapDeg / 2f
            val visibleEnd = reveal * 360f  // durante la animación se corta lo que aún no "ha llegado"
            if (start >= visibleEnd) continue
            val sweep = min(seg.sweep * 360f - gapDeg, visibleEnd - start)
            paint.strokeWidth = if (seg.overflow) stroke * 0.4f else stroke
            paint.color = colorOf(seg.kind)
            canvas.drawArc(oval, -90f + start, sweep, false, paint)
        }
    }

    private companion object {
        const val GAP_DEG = 4f  // hueco entre tomas, en grados
    }
}
