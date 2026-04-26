package com.d4vram.cbdcounter

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EvolutionActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChartView
    private lateinit var rangeChipGroup: ChipGroup
    private lateinit var evolutionTitle: TextView
    private val dateKeyFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val labelFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

    private var rangeDays = 7
    private var offsetDays = 0 // 0 = up to today

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_evolution)

        window.statusBarColor = getColor(R.color.gradient_start)

        val toolbar = findViewById<MaterialToolbar>(R.id.evolutionToolbar)
        toolbar.setNavigationOnClickListener { finish() }

        lineChart = findViewById(R.id.lineChart)
        rangeChipGroup = findViewById(R.id.rangeChipGroup)
        evolutionTitle = findViewById(R.id.evolutionTitle)
        val btnPrev = findViewById<ImageButton>(R.id.btnPrev)
        val btnNext = findViewById<ImageButton>(R.id.btnNext)

        // Arrow listeners
        btnPrev.setOnClickListener {
            offsetDays += rangeDays
            loadData(rangeDays, offsetDays)
        }
        btnNext.setOnClickListener {
            offsetDays = (offsetDays - rangeDays).coerceAtLeast(0)
            loadData(rangeDays, offsetDays)
        }

        rangeChipGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chip7Days -> rangeDays = 7
                R.id.chip14Days -> rangeDays = 14
                R.id.chip30Days -> rangeDays = 30
                R.id.chip60Days -> rangeDays = 60
            }
            offsetDays = 0 // reset to today when range changes
            loadData(rangeDays, offsetDays)
        }

        // Default load
        loadData(rangeDays, offsetDays)
    }

    private fun loadData(days: Int, offset: Int) {
        evolutionTitle.text = if (offset == 0)
            getString(R.string.evolution_last_n_days, days)
        else
            getString(R.string.evolution_ago_range, offset, offset + days)
        val dataPoints = mutableListOf<Pair<String, Int>>()
        val calendar = Calendar.getInstance()
        // Move back 'offset' days from today
        calendar.add(Calendar.DAY_OF_YEAR, -offset)
        // Then go back (days-1) more to get start date
        calendar.add(Calendar.DAY_OF_YEAR, -(days - 1))

        for (i in 0 until days) {
            val dateKey = dateKeyFormat.format(calendar.time)
            val label = labelFormat.format(calendar.time)
            val count = Prefs.getTotalCount(this, dateKey)
            dataPoints.add(Pair(label, count))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        lineChart.setData(dataPoints)
    }
}
