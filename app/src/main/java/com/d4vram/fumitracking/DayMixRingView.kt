package com.d4vram.fumitracking

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.min

/**
 * Mini anillo del calendario: un único anillo grueso repartido en proporción
 * CBD → weed → polen, sin huecos, con el total de tomas en el centro.
 * A ~30 dp no caben trozos por toma; la proporción se lee mejor (ver RingsView para el detalle).
 */
class DayMixRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var mix = DayMix(0, 0, 0)

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT  // bordes rectos: los tramos se tocan sin solaparse
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val oval = RectF()

    fun setMix(mix: DayMix) {
        this.mix = mix
        contentDescription = "${mix.total}: CBD ${mix.cbd}, weed ${mix.weed}, polen ${mix.polen}"
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        val stroke = size * 0.22f
        val r = size / 2f - stroke / 2f
        val c = size / 2f
        oval.set(c - r, c - r, c + r, c + r)
        ringPaint.strokeWidth = stroke

        // Pista de fondo
        ringPaint.color = ContextCompat.getColor(context, R.color.ring_track)
        canvas.drawArc(oval, 0f, 360f, false, ringPaint)

        // Tramos en proporción, empezando arriba (-90°) en sentido horario
        val total = mix.total
        if (total > 0) {
            var start = -90f
            for ((count, color) in listOf(
                mix.cbd to R.color.ring_seg_cbd,
                mix.weed to R.color.ring_seg_weed,
                mix.polen to R.color.ring_seg_polen
            )) {
                if (count == 0) continue
                val sweep = 360f * count / total
                ringPaint.color = ContextCompat.getColor(context, color)
                canvas.drawArc(oval, start, sweep, false, ringPaint)
                start += sweep
            }
        }

        // Total en el centro; con dos cifras, letra algo menor para que quepa
        val label = total.toString()
        textPaint.textSize = size * if (label.length > 1) 0.28f else 0.34f
        textPaint.color = ContextCompat.getColor(context, R.color.text_primary)
        val y = c - (textPaint.descent() + textPaint.ascent()) / 2f  // centrado vertical real
        canvas.drawText(label, c, y, textPaint)
    }
}
