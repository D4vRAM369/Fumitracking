package com.d4vram.cbdcounter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.max

class LineChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Colors
    private val colorPrimary = ContextCompat.getColor(context, R.color.green_safe)
    private val colorGrid = ContextCompat.getColor(context, R.color.divider_color)
    private val colorText = ContextCompat.getColor(context, R.color.text_secondary)
    private val colorFillStart = ContextCompat.getColor(context, R.color.green_safe)
    private val colorFillEnd = ContextCompat.getColor(context, R.color.transparent)

    private val paintLine = Paint().apply {
        color = colorPrimary
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val paintFill = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintDot = Paint().apply {
        color = colorPrimary
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintDotInner = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintText = Paint().apply {
        color = colorText
        textSize = 32f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private val paintGrid = Paint().apply {
        color = colorGrid
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private var dataPoints: List<Pair<String, Int>> = emptyList()
    private var maxValue = 10

    fun setData(data: List<Pair<String, Int>>) {
        dataPoints = data
        maxValue = max(data.maxOfOrNull { it.second } ?: 10, 10)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (dataPoints.isEmpty()) return

        val paddingStart = 80f
        val paddingEnd = 80f
        val paddingTop = 80f
        val paddingBottom = 100f
        
        val graphWidth = width - paddingStart - paddingEnd
        val graphHeight = height - paddingTop - paddingBottom
        
        // Draw Grid Lines (Horizontal)
        val steps = 5
        for (i in 0..steps) {
            val y = height - paddingBottom - (i * graphHeight / steps)
            canvas.drawLine(paddingStart, y, width - paddingEnd, y, paintGrid)
        }

        val xStep = graphWidth / (dataPoints.size - 1).coerceAtLeast(1)
        val yStep = graphHeight / maxValue.toFloat()

        // Prepare Paths
        val pathLine = Path()
        val pathFill = Path()

        // Calculate points
        val points = dataPoints.mapIndexed { index, point ->
            val x = paddingStart + index * xStep
            val y = height - paddingBottom - (point.second * yStep)
            Pair(x, y)
        }

        // Build Smooth Path (Catmull-Rom spline to Bezier)
        if (points.isNotEmpty()) {
            pathLine.moveTo(points[0].first, points[0].second)
            pathFill.moveTo(points[0].first, height - paddingBottom) // Start at bottom-left
            pathFill.lineTo(points[0].first, points[0].second)

            for (i in 0 until points.size - 1) {
                val p0 = points[max(0, i - 1)]
                val p1 = points[i]
                val p2 = points[i + 1]
                val p3 = points[minOf(points.size - 1, i + 2)]

                val cp1x = p1.first + (p2.first - p0.first) * 0.2f
                val cp1y = p1.second + (p2.second - p0.second) * 0.2f
                val cp2x = p2.first - (p3.first - p1.first) * 0.2f
                val cp2y = p2.second - (p3.second - p1.second) * 0.2f

                pathLine.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.first, p2.second)
                pathFill.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.first, p2.second)
            }
            
            pathFill.lineTo(points.last().first, height - paddingBottom) // End at bottom-right
            pathFill.close()
        }

        // Draw Fill Gradient
        paintFill.shader = LinearGradient(
            0f, paddingTop, 0f, height - paddingBottom,
            colorFillStart, colorFillEnd, Shader.TileMode.CLAMP
        )
        paintFill.alpha = 50 // Semi-transparent
        canvas.drawPath(pathFill, paintFill)

        // Draw Line
        canvas.drawPath(pathLine, paintLine)

        // Draw Dots and Labels
        val labelSkip = when {
            dataPoints.size > 30 -> 7
            dataPoints.size > 10 -> 5
            else -> 1
        }

        points.forEachIndexed { index, (x, y) ->
            // Draw Dot
            canvas.drawCircle(x, y, 12f, paintDot)
            canvas.drawCircle(x, y, 6f, paintDotInner)

            // Draw Value and X-Axis labels with same skip
            if (index % labelSkip == 0 || index == points.size - 1) {
                canvas.drawText(dataPoints[index].second.toString(), x, y - 25, paintText)
                canvas.drawText(dataPoints[index].first, x, height - paddingBottom + 50, paintText)
            }
        }
    }
}
