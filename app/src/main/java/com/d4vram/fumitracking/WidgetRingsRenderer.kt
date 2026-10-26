package com.d4vram.fumitracking

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils

/**
 * Dibuja los dos anillos del día (CBD fuera, THC dentro) en un Bitmap para el widget.
 * Un widget solo acepta RemoteViews, no vistas propias, así que los anillos viajan como imagen.
 * Las reglas (estado, relleno, exceso) vienen de [RingProgress]; la geometría es la de [RingsView].
 */
object WidgetRingsRenderer {

    private const val SIZE_DP = 176  // grande: el ImageView lo reduce según el alto real, y escalar hacia abajo no desenfoca

    fun render(context: Context, cbd: Int, thc: Int, cbdGoal: Int, thcGoal: Int): Bitmap {
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
            strokeCap = Paint.Cap.ROUND
        }
        drawRing(context, canvas, paint, center, outerRadius, stroke, cbd, cbdGoal, R.color.widget_ring_cbd)
        drawRing(context, canvas, paint, center, innerRadius, stroke, thc, thcGoal, R.color.widget_ring_thc)
        return bitmap
    }

    private fun drawRing(
        context: Context, canvas: Canvas, paint: Paint, center: Float, radius: Float,
        stroke: Float, count: Int, goal: Int, ownColor: Int
    ) {
        val oval = RectF(center - radius, center - radius, center + radius, center + radius)
        paint.strokeWidth = stroke

        // Pista: siempre visible; es lo único que se ve si no hay meta.
        paint.color = ContextCompat.getColor(context, R.color.widget_ring_track)
        canvas.drawArc(oval, 0f, 360f, false, paint)

        val sweep = 360f * RingProgress.fraction(count, goal)
        if (sweep <= 0f) return

        val color = ContextCompat.getColor(
            context,
            when (RingProgress.state(count, goal)) {
                RingState.AT_LIMIT -> R.color.widget_ring_limit
                RingState.OVER -> R.color.widget_ring_over
                else -> ownColor
            }
        )
        // Primera vuelta: arranca arriba (-90°) en sentido horario.
        paint.color = color
        canvas.drawArc(oval, -90f, sweep, false, paint)

        // Segunda vuelta fina y más clara, solo si te pasas de la meta.
        val overflow = 360f * RingProgress.overflowFraction(count, goal)
        if (overflow > 0f) {
            paint.strokeWidth = stroke * 0.4f
            paint.color = ColorUtils.blendARGB(color, Color.WHITE, 0.55f)
            canvas.drawArc(oval, -90f, overflow, false, paint)
        }
    }
}
