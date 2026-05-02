package com.d4vram.cbdcounter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

class InsightsActivity : AppCompatActivity() {

    private val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var weekOffset = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insights)
        supportActionBar?.title = "Insights"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Edge-to-edge: aplicar insets al root para respetar status bar y navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<MaterialButton>(R.id.btnPrevWeek).setOnClickListener {
            weekOffset--
            updateInsights()
        }
        findViewById<MaterialButton>(R.id.btnNextWeek).setOnClickListener {
            if (weekOffset < 0) { weekOffset++; updateInsights() }
        }
        updateInsights()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun updateInsights() {
        val (weekDates, weekLabel) = getWeekDates(weekOffset)
        val prevWeekDates = getWeekDates(weekOffset - 1).first

        val weekCounts = weekDates.map { Prefs.getCbdCount(this, it) + Prefs.getThcCount(this, it) }
        val prevCounts = prevWeekDates.map { Prefs.getCbdCount(this, it) + Prefs.getThcCount(this, it) }
        val weekAvg = weekCounts.average()
        val prevAvg = prevCounts.average()

        findViewById<TextView>(R.id.tvWeekLabel).text = weekLabel
        findViewById<MaterialButton>(R.id.btnNextWeek).isEnabled = weekOffset < 0
        renderBars(weekCounts)

        findViewById<TextView>(R.id.tvAvgLabel).text = "Promedio: %.1f CBD+THC/día".format(weekAvg)
        findViewById<TextView>(R.id.tvCompareLabel).text = if (prevAvg > 0) {
            val pct = ((weekAvg - prevAvg) / prevAvg * 100).roundToInt()
            "${if (pct >= 0) "▲" else "▼"} ${abs(pct)}% vs semana anterior"
        } else ""

        updateTip()
    }

    private fun renderBars(counts: List<Int>) {
        val container = findViewById<LinearLayout>(R.id.llBarsContainer)
        val labels = findViewById<LinearLayout>(R.id.llDayLabels)
        container.removeAllViews()
        labels.removeAllViews()

        val maxCount = counts.maxOrNull()?.takeIf { it > 0 } ?: 1
        val dayNames = listOf("L", "M", "X", "J", "V", "S", "D")
        val density = resources.displayMetrics.density
        val maxHeightPx = (140 * density).toInt()

        counts.forEachIndexed { i, count ->
            val barHeight = (maxHeightPx * count.toFloat() / maxCount).toInt().coerceAtLeast(if (count > 0) 4 else 0)

            val wrapper = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            }
            val bar = View(this).apply {
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, barHeight)
                lp.setMargins(4, 0, 4, 0)
                layoutParams = lp
                val r = 4 * density
                background = GradientDrawable().apply {
                    setColor(HeatmapUtils.colorForCount(count))
                    cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
                }
            }
            wrapper.addView(bar)
            container.addView(wrapper)

            labels.addView(TextView(this).apply {
                text = dayNames[i]
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setTextColor(Color.GRAY)
                textSize = 12f
            })
        }
    }

    private fun updateTip() {
        val cardTip = findViewById<CardView>(R.id.cardTip)
        val tvTip = findViewById<TextView>(R.id.tvTip)
        val allDates = getAllDatesWithData()
        if (allDates.size < 14) { cardTip.visibility = View.GONE; return }

        val cal = Calendar.getInstance()
        val dayAvgs = (0..6).map { dow ->
            val totals = allDates.mapNotNull { dateKey ->
                runCatching { fmt.parse(dateKey) }.getOrNull()?.let { date ->
                    cal.time = date
                    val d = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
                    if (d == dow) Prefs.getCbdCount(this, dateKey) + Prefs.getThcCount(this, dateKey) else null
                }
            }
            totals.takeIf { it.isNotEmpty() }?.average() ?: 0.0
        }

        val globalAvg = dayAvgs.filter { it > 0 }.average().takeIf { !it.isNaN() }
            ?: run { cardTip.visibility = View.GONE; return }
        val maxDayIdx = dayAvgs.indices.maxByOrNull { dayAvgs[it] }
            ?: run { cardTip.visibility = View.GONE; return }
        val pct = ((dayAvgs[maxDayIdx] - globalAvg) / globalAvg * 100).roundToInt()
        if (pct < 15) { cardTip.visibility = View.GONE; return }

        val dayNames = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábados", "domingos")
        tvTip.text = "💡 Los ${dayNames[maxDayIdx]} consumes un $pct% más de lo habitual."
        cardTip.visibility = View.VISIBLE
    }

    private fun getAllDatesWithData(): List<String> =
        getSharedPreferences("CBDCounter", MODE_PRIVATE).all.keys
            .filter { it.startsWith("count_cbd_") }
            .map { it.removePrefix("count_cbd_") }

    private fun getWeekDates(offset: Int): Pair<List<String>, String> {
        val cal = Calendar.getInstance()
        val dow = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        cal.add(Calendar.DAY_OF_YEAR, -dow + (offset * 7))
        val dates = (0..6).map { i ->
            val d = cal.clone() as Calendar
            d.add(Calendar.DAY_OF_YEAR, i)
            fmt.format(d.time)
        }
        val label = if (offset == 0) "Esta semana" else {
            val s = SimpleDateFormat("dd/MM", Locale.getDefault())
            val endCal = (cal.clone() as Calendar).also { it.add(Calendar.DAY_OF_YEAR, 6) }
            "${s.format(cal.time)} – ${s.format(endCal.time)}"
        }
        return Pair(dates, label)
    }
}
