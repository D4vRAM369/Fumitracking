package com.d4vram.cbdcounter

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarActivity : AppCompatActivity(), NoteBottomSheet.Listener {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var monthLabel: TextView
    private lateinit var prevMonthButton: ImageButton
    private lateinit var nextMonthButton: ImageButton
    private lateinit var calendarRecycler: RecyclerView
    private lateinit var sharedPrefs: SharedPreferences

    // Referencias a los TextViews de la leyenda
    private lateinit var legendEmoji0: TextView
    private lateinit var legendEmoji1: TextView
    private lateinit var legendEmoji3: TextView
    private lateinit var legendEmoji5: TextView
    private lateinit var legendEmoji6: TextView
    private lateinit var legendEmoji7: TextView
    private lateinit var legendEmoji8: TextView
    private lateinit var legendEmoji9: TextView
    private lateinit var legendEmoji10: TextView
    private lateinit var legendEmoji11: TextView
    private lateinit var legendEmoji12: TextView

    private val displayCalendar: Calendar = Calendar.getInstance()
    private val monthFormat = SimpleDateFormat("LLLL yyyy", Locale("es", "ES"))
    private val dateKeyFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val todayKey: String = dateKeyFormat.format(Date())
    private val calendarAdapter = CalendarAdapter(displayCalendar, dateKeyFormat) { date ->
        DayModalFragment.newInstance(date).show(supportFragmentManager, "day_modal")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        window.statusBarColor = getColor(R.color.gradient_start)

        toolbar = findViewById(R.id.statsToolbar)
        monthLabel = findViewById(R.id.monthLabel)
        prevMonthButton = findViewById(R.id.prevMonthButton)
        nextMonthButton = findViewById(R.id.nextMonthButton)
        calendarRecycler = findViewById(R.id.calendarRecycler)

        // Inicializar referencias a los TextViews de la leyenda
        legendEmoji0 = findViewById(R.id.legendEmoji0)
        legendEmoji1 = findViewById(R.id.legendEmoji1)
        legendEmoji3 = findViewById(R.id.legendEmoji3)
        legendEmoji5 = findViewById(R.id.legendEmoji5)
        legendEmoji6 = findViewById(R.id.legendEmoji6)
        legendEmoji7 = findViewById(R.id.legendEmoji7)
        legendEmoji8 = findViewById(R.id.legendEmoji8)
        legendEmoji9 = findViewById(R.id.legendEmoji9)
        legendEmoji10 = findViewById(R.id.legendEmoji10)
        legendEmoji11 = findViewById(R.id.legendEmoji11)
        legendEmoji12 = findViewById(R.id.legendEmoji12)

        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        sharedPrefs = getSharedPreferences("CBDCounter", Context.MODE_PRIVATE)

        calendarRecycler.layoutManager = GridLayoutManager(this, 7)
        calendarRecycler.adapter = calendarAdapter

        prevMonthButton.setOnClickListener {
            displayCalendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        nextMonthButton.setOnClickListener {
            displayCalendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        findViewById<com.google.android.material.chip.Chip>(R.id.btnEvolution).setOnClickListener {
            startActivity(android.content.Intent(this, EvolutionActivity::class.java))
        }

        updateCalendar()
        updateLegend() // Cargar emojis dinámicamente al iniciar
    }

    override fun onNoteChanged(date: String) {
        updateCalendar() // Refresh calendar after note change
    }

    /**
     * Se ejecuta cada vez que la Activity vuelve a primer plano
     * Esto incluye cuando vuelves de EmojiSettingsActivity
     */
    override fun onResume() {
        super.onResume()
        updateLegend() // Actualizar leyenda por si cambiaron los emojis
        updateCalendar() // Actualizar calendario también
    }

    private fun updateCalendar() {
        displayCalendar.set(Calendar.DAY_OF_MONTH, 1)
        val formattedMonth = monthFormat.format(displayCalendar.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        monthLabel.text = formattedMonth
        calendarAdapter.submit(buildCalendarCells())
    }

    /**
     * Actualiza la leyenda de emojis con los valores personalizados del usuario
     * Esta función lee desde SharedPreferences y actualiza cada TextView dinámicamente
     */
    private fun updateLegend() {
        // Definir los rangos con sus labels correspondientes
        val legendData = listOf(
            Triple(0, legendEmoji0, "0"),
            Triple(1, legendEmoji1, "1-2"),
            Triple(3, legendEmoji3, "3-4"),
            Triple(5, legendEmoji5, "5"),
            Triple(6, legendEmoji6, "6"),
            Triple(7, legendEmoji7, "7"),
            Triple(8, legendEmoji8, "8"),
            Triple(9, legendEmoji9, "9"),
            Triple(10, legendEmoji10, "10"),
            Triple(11, legendEmoji11, "11"),
            Triple(12, legendEmoji12, "12+")
        )

        // Actualizar cada TextView con el emoji correspondiente
        for ((count, textView, rangeText) in legendData) {
            val emoji = EmojiUtils.emojiForCount(count, this)
            textView.text = "$emoji  $rangeText"
        }
    }

    private fun buildCalendarCells(): List<CalendarCell> {
        val items = mutableListOf<CalendarCell>()
        val workingCalendar = displayCalendar.clone() as Calendar

        val firstDayOffset = mondayFirstDayOffset(workingCalendar.get(Calendar.DAY_OF_WEEK))
        repeat(firstDayOffset) { items.add(CalendarCell.Empty) }

        val daysInMonth = workingCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentMonth = workingCalendar.get(Calendar.MONTH)
        val currentYear = workingCalendar.get(Calendar.YEAR)
        val todayCalendar = Calendar.getInstance()

        for (day in 1..daysInMonth) {
            workingCalendar.set(Calendar.DAY_OF_MONTH, day)
            val dateKey = dateKeyFormat.format(workingCalendar.time)
            val count = Prefs.getTotalCount(this, dateKey)
            val hasData = count > 0
            val emoji = if (hasData) EmojiUtils.emojiForCount(count, this) else ""
            val isToday = dateKey == todayKey &&
                todayCalendar.get(Calendar.MONTH) == currentMonth &&
                todayCalendar.get(Calendar.YEAR) == currentYear

            items.add(CalendarCell.Day(day, emoji, hasData, isToday))
        }

        while (items.size % 7 != 0) {
            items.add(CalendarCell.Empty)
        }

        return items
    }

    private fun mondayFirstDayOffset(dayOfWeek: Int): Int {
        val adjusted = (dayOfWeek - Calendar.MONDAY)
        return if (adjusted < 0) adjusted + 7 else adjusted
    }

    private sealed class CalendarCell {
        object Empty : CalendarCell()
        data class Day(
            val number: Int,
            val emoji: String,
            val hasData: Boolean,
            val isToday: Boolean
        ) : CalendarCell()
    }

    private class CalendarAdapter(
        private val displayCalendar: java.util.Calendar,
        private val dateKeyFormat: java.text.SimpleDateFormat,
        private val onDayClick: (String) -> Unit
    ) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {
        private val items = mutableListOf<CalendarCell>()

        fun submit(data: List<CalendarCell>) {
            items.clear()
            items.addAll(data)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_calendar_day, parent, false)
            return CalendarViewHolder(view)
        }

        override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
            holder.bind(items[position], displayCalendar, dateKeyFormat, onDayClick)
        }

        override fun getItemCount(): Int = items.size

        class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val dayContainer: View = itemView.findViewById(R.id.dayContainer)
            private val dayNumber: TextView = itemView.findViewById(R.id.dayNumber)
            private val dayEmoji: TextView = itemView.findViewById(R.id.dayEmoji)

            fun bind(cell: CalendarCell, displayCalendar: java.util.Calendar, dateKeyFormat: java.text.SimpleDateFormat, onDayClick: (String) -> Unit) {
                itemView.setOnClickListener {
                    if (cell is CalendarCell.Day && cell.hasData) {
                        val calendar = java.util.Calendar.getInstance()
                        calendar.time = displayCalendar.time
                        calendar.set(java.util.Calendar.DAY_OF_MONTH, cell.number)
                        val dateKey = dateKeyFormat.format(calendar.time)
                        onDayClick(dateKey)
                    }
                }

                when (cell) {
                    is CalendarCell.Empty -> {
                        dayNumber.text = ""
                        dayNumber.alpha = 1f
                        dayEmoji.text = ""
                        dayEmoji.visibility = View.INVISIBLE
                        dayContainer.background = null
                    }
                    is CalendarCell.Day -> {
                        dayNumber.text = cell.number.toString()
                        dayEmoji.text = cell.emoji
                        dayNumber.alpha = when {
                            cell.isToday -> 1f
                            cell.hasData -> 1f
                            else -> 0.45f
                        }
                        dayEmoji.visibility = if (cell.hasData && cell.emoji.isNotBlank()) View.VISIBLE else View.INVISIBLE
                        dayContainer.background = if (cell.isToday) {
                            androidx.appcompat.content.res.AppCompatResources.getDrawable(
                                itemView.context,
                                R.drawable.bg_calendar_today
                            )
                        } else {
                            null
                        }
                    }
                }
            }
        }
    }
}
