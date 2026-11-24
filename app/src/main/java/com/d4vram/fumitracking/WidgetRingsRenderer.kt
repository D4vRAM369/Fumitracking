package com.d4vram.fumitracking

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import kotlin.math.min

/**
 * Dibuja los dos anillos del día (CBD fuera, THC dentro) en un Bitmap para el widget.
 * Un widget solo acepta RemoteViews, no vistas propias, así que los anillos viajan como imagen.
 * Igual que [RingsView]: un trozo por toma, con hueco, coloreado por tipo (sale de la nota del día).
 */
object WidgetRingsRenderer {

    private const val SIZE_DP = 176  // grande: el ImageView lo reduce según el alto real, y escalar hacia abajo no desenfoca
    private const val GAP_DEG = 4f   // hueco entre tomas, en grados (el mismo que RingsView)

    fun render(context: Context, cbd: Int, thc: Int, cbdGoal: Int, thcGoal: Int, note: String?): Bitmap {
        val side = (SIZE_DP * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val size = side.toFloat()
        val stroke = size * 0.085f
        val gap = stroke * 0.45f
        val center = size / 2f
        val outerRadius = center - stroke / 2f
        val innerRadius = outerRadius - stroke - gap

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.BUTT  // bordes rectos: con ROUND los huecos se taparían
        }
        val kinds = RingProgress.kindsFromNote(note)
        val cbdSegments = RingProgress.segments(kinds.filter { it == IntakeKind.CBD }, cbd, cbdGoal, IntakeKind.CBD)
        val thcSegments = RingProgress.segments(kinds.filter { it != IntakeKind.CBD }, thc, thcGoal, IntakeKind.WEED)
        drawRing(context, canvas, paint, center, outerRadius, stroke, cbdSegments)
        drawRing(context, canvas, paint, center, innerRadius, stroke, thcSegments)
        return bitmap
    }

    private fun drawRing(
        context: Context, canvas: Canvas, paint: Paint, center: Float, radius: Float,
        stroke: Float, segments: List<RingSegment>
    ) {
        val oval = RectF(center - radius, center - radius, center + radius, center + radius)

        // Pista: siempre visible; es lo único que se ve sin tomas.
        paint.strokeWidth = stroke
        paint.color = ContextCompat.getColor(context, R.color.widget_ring_track)
        canvas.drawArc(oval, 0f, 360f, false, paint)

        // Un arco por toma desde arriba (-90°) en sentido horario; la segunda vuelta (exceso) va fina.
        for (seg in segments) {
            val gapDeg = min(GAP_DEG, seg.sweep * 360f * 0.5f)  // con muchas tomas el hueco no se come el arco
            paint.strokeWidth = if (seg.overflow) stroke * 0.4f else stroke
            paint.color = ContextCompat.getColor(context, colorOf(seg.kind))
            canvas.drawArc(oval, -90f + seg.start * 360f + gapDeg / 2f, seg.sweep * 360f - gapDeg, false, paint)
        }
    }

    private fun colorOf(kind: IntakeKind) = when (kind) {
        IntakeKind.CBD -> R.color.widget_ring_seg_cbd
        IntakeKind.WEED -> R.color.widget_ring_seg_weed
        IntakeKind.POLEN -> R.color.widget_ring_seg_polen
    }
}
