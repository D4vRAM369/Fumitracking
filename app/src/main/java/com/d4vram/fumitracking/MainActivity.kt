package com.d4vram.fumitracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowInsetsController
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), InfusionChoiceBottomSheet.Listener, VoiceNoteBottomSheet.Listener {

    companion object {
        /** Opacidad del número del contador inactivo. Un test exige que siga leyéndose (3:1). */
        const val INACTIVE_COUNTER_ALPHA = 0.8f
    }

    // Views principales
    private lateinit var counterText: TextView  // Oculto, para compatibilidad
    private lateinit var cbdCountText: TextView
    private lateinit var thcCountText: TextView

    private lateinit var dateText: TextView
    private lateinit var emojiText: TextView
    private lateinit var ringsView: RingsView
    private lateinit var ringsContainer: View
    private lateinit var cbdLabelText: TextView
    private lateinit var thcLabelText: TextView
    private lateinit var ringsHintText: TextView
    private lateinit var addButton: Button
    private lateinit var addInfusedButton: MaterialButton
    private lateinit var subtractButton: Button
    private lateinit var resetButton: Button
    private lateinit var moreButton: ImageButton

    // Iconos top-right

    // Chip estadísticas
    private lateinit var statsChip: com.google.android.material.chip.Chip

    // Botón toggle tema claro/oscuro

    // Views del historial mejorado
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var historyAdapter: ImprovedHistoryAdapter
    private lateinit var tabLayout: TabLayout
    private lateinit var statsContainer: View
    private lateinit var avgText: TextView
    private lateinit var totalText: TextView
    private lateinit var streakText: TextView
    private lateinit var searchButton: ImageButton

    // Data
    private lateinit var sharedPrefs: SharedPreferences
    private var cbdCount = 0
    private var thcCount = 0
    private var loadedDateKey = ""  // día cargado en pantalla; detecta cambio de medianoche
    private val currentCount: Int get() = cbdCount + thcCount  // Total para emoji y compatibilidad
    private val allHistoryData = ArrayList<HistoryItem>()
    private val displayedHistoryData = ArrayList<HistoryItem>()
    private var currentViewMode = ViewMode.WEEK

    private val importMimeTypes = arrayOf(
        "text/csv",
        "text/comma-separated-values",
        "application/csv",
        "application/vnd.ms-excel",
        "text/plain"
    )

    // Receptor para detectar cambio de día/hora mientras la app está abierta
    private val dateChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_DATE_CHANGED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED -> {
                    // Recargar datos del día actual
                    loadTodayData()
                    updateDisplay()
                    loadAllHistoryData()
                    updateHistoryView()
                    updateStats()
                }
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val midnightReloadRunnable = Runnable {
        loadTodayData()
        updateDisplay(animate = false)
        loadAllHistoryData()
        updateHistoryView()
        updateStats()
        historyAdapter.refresh()
        scheduleMidnightReload()
    }

    private val importCsvLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            runCatching {
                uri?.let { handleImportCsv(it) }
            }.onFailure { e ->
                showFeedback("Error inesperado al importar: ${e.message}", true)
            }
        }

    enum class ViewMode { WEEK, MONTH, ALL }
    private enum class InfusionType(
        @param:StringRes val labelRes: Int,
        @param:StringRes val feedbackRes: Int,
        val icon: String
    ) {
        WEED(R.string.weed_option, R.string.cbd_infused_added_weed, IntakeController.ICON_WEED),
        POLEM(R.string.polem_option, R.string.cbd_infused_added_polem, IntakeController.ICON_POLEN)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Aplicar tema ANTES de super.onCreate para evitar flickering
        initSharedPreferences()
        Prefs.migrateToV14IfNeeded(this)  // Migrar datos al nuevo formato si es necesario
        applyStoredTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        EdgeToEdgeUtils.enableEdgeToEdge(this, lightStatusBar = !isDarkModeEnabled())

        runCatching {
            // initSharedPreferences() -> Ya llamado arriba
            initViews()
            loadTodayData()
            loadAllHistoryData()
            setupTabLayout()
            setupClickListeners()
            updateDisplay(animate = false)
            updateHistoryView()
            updateStats()

            // BottomSheet behavior
            val bottomSheet = findViewById<LinearLayout>(R.id.bottomSheetContainer)
            val sheetBehavior = BottomSheetBehavior.from(bottomSheet)
            sheetBehavior.peekHeight = (260 * resources.displayMetrics.density).toInt()
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            sheetBehavior.isHideable = false
        }.onFailure { e ->
            showFeedback("Error al iniciar la app: ${e.message}", true)
            e.printStackTrace()
        }

        // Mostrar disclaimer médico en el primer uso
        showDisclaimerIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        // Actualizar emoji cuando se vuelve de EmojiSettingsActivity
        loadTodayData()
        updateDisplay(animate = false)
        historyAdapter.refresh()
        // Restaurar backup, importar CSV, cambiar metas o emojis alteran lo que muestra el widget
        // sin pasar por saveData(); al volver a la app lo ponemos al día.
        CBDWidgetProvider.updateAllWidgets(this)

        // Registrar receptor para cambios de fecha/hora
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        registerReceiver(dateChangeReceiver, filter)
        scheduleMidnightReload()
    }

    override fun onPause() {
        super.onPause()
        // Desregistrar receptor para evitar memory leaks
        try {
            handler.removeCallbacks(midnightReloadRunnable)
            unregisterReceiver(dateChangeReceiver)
        } catch (e: IllegalArgumentException) {
            // Receptor ya desregistrado, ignorar
        }
    }

    private fun scheduleMidnightReload() {
        handler.removeCallbacks(midnightReloadRunnable)
        val midnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 1)
            set(Calendar.MILLISECOND, 0)
        }
        val delay = midnight.timeInMillis - System.currentTimeMillis()
        handler.postDelayed(midnightReloadRunnable, delay)
    }

    private fun initViews() {
        // Views principales
        counterText = findViewById(R.id.counterText)  // Oculto, para compatibilidad
        cbdCountText = findViewById(R.id.cbdCountText)
        thcCountText = findViewById(R.id.thcCountText)

        dateText = findViewById(R.id.dateText)
        emojiText = findViewById(R.id.emojiText)
        ringsView = findViewById(R.id.ringsView)
        ringsContainer = findViewById(R.id.ringsContainer)
        cbdLabelText = findViewById(R.id.cbdLabelText)
        thcLabelText = findViewById(R.id.thcLabelText)
        ringsHintText = findViewById(R.id.ringsHintText)
        addButton = findViewById(R.id.addButton)
        addInfusedButton = findViewById(R.id.addInfusedButton)
        subtractButton = findViewById(R.id.subtractButton)
        resetButton = findViewById(R.id.resetButton)
        moreButton = findViewById(R.id.moreButton)
        statsChip = findViewById(R.id.statsChip)
        updateThemeIcon()

        // Historial
        historyRecyclerView = findViewById(R.id.historyRecyclerView)
        tabLayout = findViewById(R.id.tabLayout)
        statsContainer = findViewById(R.id.statsContainer)
        avgText = findViewById(R.id.avgText)
        totalText = findViewById(R.id.totalText)
        streakText = findViewById(R.id.streakText)
        searchButton = findViewById(R.id.searchButton)

        // Adapter con callback para abrir el modal de notas
        historyAdapter = ImprovedHistoryAdapter(
            historyList = displayedHistoryData,
            onDayClick = { date ->
                DayModalFragment.newInstance(date).show(supportFragmentManager, "day_modal")
            },
            fragmentManager = supportFragmentManager
        )
        historyRecyclerView.layoutManager = LinearLayoutManager(this)
        historyRecyclerView.adapter = historyAdapter

        // Espaciado entre ítems
        historyRecyclerView.addItemDecoration(HistoryItemDecoration(16))



        // Abrir diálogo de búsqueda de notas
        searchButton.setOnClickListener {
            SearchNotesDialog().show(supportFragmentManager, "search_notes")
        }
    } // <-- CIERRE de initViews() AQUÍ




    private fun initSharedPreferences() {
        sharedPrefs = getSharedPreferences("CBDCounter", Context.MODE_PRIVATE)
    }

    private fun setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_week)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_month)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_all)))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentViewMode = when (tab?.position) {
                    0 -> ViewMode.WEEK
                    1 -> ViewMode.MONTH
                    2 -> ViewMode.ALL
                    else -> ViewMode.WEEK
                }
                updateHistoryView()
                updateStats()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun getCurrentDateKey(): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun getCurrentDateDisplay(): String {
        val formatter = SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        return formatter.format(Date()).replaceFirstChar { it.uppercase() }
    }

    private fun loadTodayData() {
        val today = getCurrentDateKey()
        loadedDateKey = today
        cbdCount = Prefs.getCbdCount(this, today)
        thcCount = Prefs.getThcCount(this, today)
    }

    /**
     * Si el día cambió desde la última carga (típicamente al pasar medianoche con la app
     * en segundo plano), recarga todo y empuja el widget. Disparador fiable de reloj real:
     * se llama cada vez que la app recupera el foco, no depende de Handler/uptime.
     */
    private fun reloadIfDateChanged() {
        if (getCurrentDateKey() == loadedDateKey) return
        loadTodayData()
        updateDisplay(animate = false)
        loadAllHistoryData()
        updateHistoryView()
        updateStats()
        historyAdapter.refresh()
        CBDWidgetProvider.updateAllWidgets(this)
        scheduleMidnightReload()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) reloadIfDateChanged()
    }

    private fun loadAllHistoryData() {
        allHistoryData.clear()
        allHistoryData.addAll(HistoryRepository.loadAll(this))
    }

    private fun updateHistoryView() {
        displayedHistoryData.clear()
        val calendar = Calendar.getInstance()

        val filteredByTab = when (currentViewMode) {
            ViewMode.WEEK -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val weekAgo = calendar.time
                allHistoryData.filter { it.dateObject >= weekAgo }
            }
            ViewMode.MONTH -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                val monthAgo = calendar.time
                allHistoryData.filter { it.dateObject >= monthAgo }
            }
            ViewMode.ALL -> allHistoryData
        }

        displayedHistoryData.addAll(filteredByTab)

        historyAdapter.refresh()
        if (displayedHistoryData.isNotEmpty()) {
            historyRecyclerView.smoothScrollToPosition(0)
        }
    }



    private fun updateStats() {
        if (displayedHistoryData.isEmpty()) {
            avgText.text = getString(R.string.avg_placeholder)
            totalText.text = getString(R.string.total_placeholder)
            streakText.text = getString(R.string.streak_reset)
            return
        }
        val average = displayedHistoryData.map { it.totalCount }.average()
        avgText.text = getString(R.string.avg_format).format(average)
        val total = displayedHistoryData.sumOf { it.totalCount }
        totalText.text = getString(R.string.total_format, total)
        val streak = calculateCleanStreak()
        streakText.text = getString(R.string.streak_format, streak)
    }

    private fun calculateCleanStreak(): Int {
        var streak = 0
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val cal = Calendar.getInstance()
        // Retroceder día a día desde hoy; días sin registro = día limpio
        repeat(365) {
            val key = formatter.format(cal.time)
            val total = Prefs.getTotalCount(this, key)
            if (total > 0) return streak
            // Solo contar si hay al menos un registro anterior (evita inflar racha en app nueva)
            if (allHistoryData.isNotEmpty() || streak > 0) streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    private fun saveData() {
        val today = getCurrentDateKey()
        Prefs.setCbdCount(this, today, cbdCount)
        Prefs.setThcCount(this, today, thcCount)

        loadAllHistoryData()
        updateHistoryView()
        updateStats()
        CBDWidgetProvider.updateAllWidgets(this)
        GoalsActivity.notifyIfGoalExceeded(this, cbdCount, thcCount)
    }

    private fun isDarkModeEnabled(): Boolean {
        // Si no hay preferencia guardada, usamos el estado actual del delegado o el sistema
        return sharedPrefs.getBoolean(Prefs.KEY_DARK_MODE, 
            AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES ||
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        )
    }

    private fun updateThemeIcon() {
        val isDark = isDarkModeEnabled()
        // Ajustar iconos de barra de estado: claros en dark mode, oscuros en light mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                if (isDark) 0 else WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        }
    }

    private fun applyStoredTheme() {
        val enabled = isDarkModeEnabled()
        val targetMode = if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        
        // Solo aplicar si el modo actual es diferente al objetivo para evitar bucles
        if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
            AppCompatDelegate.setDefaultNightMode(targetMode)
        }
    }

    private fun setDarkMode(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(Prefs.KEY_DARK_MODE, enabled).apply()
        applyStoredTheme()
    }

    private fun updateDisplay(animate: Boolean = true) {
        // Actualizar contadores duales
        cbdCountText.text = cbdCount.toString()
        thcCountText.text = thcCount.toString()
        counterText.text = currentCount.toString()  // Total oculto para compatibilidad

        dateText.text = getCurrentDateDisplay()
        val newEmoji = getEmoji(currentCount)

        if (animate && emojiText.text != newEmoji && emojiText.text.isNotEmpty()) {
            emojiText.animate().alpha(0f).setDuration(150).withEndAction {
                emojiText.text = newEmoji
                emojiText.animate().alpha(1f).setDuration(150).start()
                animateEmoji()
            }.start()
        } else {
            emojiText.alpha = 1f
            emojiText.text = newEmoji
        }

        // Destacar el modo activo
        val isThc = Prefs.getSubstanceType(this) == "THC"
        highlightActiveCounter(isThc)

        // Botón principal: etiqueta y color según el modo activo (antes decía "+THC" aunque sumaba CBD)
        addButton.text = getString(if (isThc) R.string.add_thc_label else R.string.add_cbd_label)
        addButton.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, if (isThc) R.color.ring_thc else R.color.ring_cbd)
        )
        // -1 apagado si no hay nada que deshacer (misma condición que usa su listener)
        val hasIntakes = cbdCount + thcCount > 0
        subtractButton.isEnabled = hasIntakes
        subtractButton.alpha = if (hasIntakes) 1f else 0.4f

        // Anillos, colores y etiquetas según conteo y metas
        updateCounterColors(animate)
    }

    private fun highlightActiveCounter(isThcActive: Boolean) {
        // El contador activo se ve más grande/destacado
        val activeScale = 1.1f
        val inactiveScale = 0.9f
        val activeAlpha = 1.0f
        val inactiveAlpha = INACTIVE_COUNTER_ALPHA

        if (isThcActive) {
            thcCountText.scaleX = activeScale
            thcCountText.scaleY = activeScale
            thcCountText.alpha = activeAlpha
            cbdCountText.scaleX = inactiveScale
            cbdCountText.scaleY = inactiveScale
            cbdCountText.alpha = inactiveAlpha
        } else {
            cbdCountText.scaleX = activeScale
            cbdCountText.scaleY = activeScale
            cbdCountText.alpha = activeAlpha
            thcCountText.scaleX = inactiveScale
            thcCountText.scaleY = inactiveScale
            thcCountText.alpha = inactiveAlpha
        }
    }

    /** Anillos, colores de los números, etiquetas y toque suave a Metas (sin metas). */
    private fun updateCounterColors(animate: Boolean) {
        val goalPrefs = getSharedPreferences(GoalsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val cbdGoal = goalPrefs.getInt(GoalsActivity.KEY_CBD_MAX, 0)
        val thcGoal = goalPrefs.getInt(GoalsActivity.KEY_THC_MAX, 0)
        ringsView.setProgress(cbdCount, thcCount, cbdGoal, thcGoal, animate)

        val cbdState = RingProgress.state(cbdCount, cbdGoal)
        val thcState = RingProgress.state(thcCount, thcGoal)
        cbdCountText.setTextColor(ringsView.colorFor(false, cbdState))
        thcCountText.setTextColor(ringsView.colorFor(true, thcState))
        cbdLabelText.text = ringLabel("CBD", cbdCount, cbdGoal, cbdState)
        thcLabelText.text = ringLabel("THC", thcCount, thcGoal, thcState)

        // Toque suave: solo si falta alguna meta; nada de diálogos ni banners.
        val anyNoGoal = cbdGoal <= 0 || thcGoal <= 0
        ringsHintText.visibility = if (anyNoGoal) View.VISIBLE else View.GONE
        ringsContainer.isClickable = anyNoGoal

        ringsContainer.contentDescription = listOf(
            ringDescription("CBD", cbdCount, cbdGoal, cbdState),
            ringDescription("THC", thcCount, thcGoal, thcState)
        ).joinToString(". ")
    }

    private fun ringLabel(name: String, count: Int, goal: Int, state: RingState): String = when (state) {
        RingState.NO_GOAL -> name
        RingState.UNDER -> getString(R.string.ring_label_goal, name, goal)
        RingState.AT_LIMIT -> getString(R.string.ring_label_state, name, getString(R.string.ring_at_limit))
        RingState.OVER -> getString(
            R.string.ring_label_state, name, getString(R.string.ring_over_by, RingProgress.overBy(count, goal))
        )
    }

    // Texto para TalkBack: valores y metas, y el estado en palabras (no solo en color).
    private fun ringDescription(name: String, count: Int, goal: Int, state: RingState): String {
        val base = if (goal > 0) getString(R.string.ring_cd_goal, name, count, goal)
        else getString(R.string.ring_cd_no_goal, name, count)
        return when (state) {
            RingState.AT_LIMIT -> "$base, ${getString(R.string.ring_at_limit)}"
            RingState.OVER -> "$base, ${getString(R.string.ring_over_by, RingProgress.overBy(count, goal))}"
            else -> base
        }
    }

    /** Menú ⋯ de la barra superior: lo que antes eran cinco iconos sueltos. */
    private fun showMoreMenu() {
        val popup = PopupMenu(this, moreButton)
        popup.menuInflater.inflate(R.menu.main_more, popup.menu)
        // En oscuro ofrecemos pasar a claro, y al revés.
        popup.menu.findItem(R.id.action_theme).setTitle(
            if (isDarkModeEnabled()) R.string.menu_theme_to_light else R.string.menu_theme_to_dark
        )
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_calendar -> startActivity(Intent(this, CalendarActivity::class.java))
                R.id.action_export -> exportCsv()
                R.id.action_import -> importCsvLauncher.launch(importMimeTypes)
                R.id.action_theme -> setDarkMode(!isDarkModeEnabled())
                R.id.action_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
        popup.show()
    }

    private fun getEmoji(count: Int): String = EmojiUtils.emojiForCount(count, this)

    private fun setupClickListeners() {
        ringsContainer.setOnClickListener {
            startActivity(Intent(this, GoalsActivity::class.java))
        }
        moreButton.setOnClickListener { showMoreMenu() }
        statsChip.setOnClickListener { startActivity(Intent(this, DashboardActivity::class.java)) }

        addButton.setOnClickListener { registerStandardIntake() }
        addInfusedButton.setOnClickListener {
            InfusionChoiceBottomSheet.new()
                .show(supportFragmentManager, "infusion_choice")
        }
        subtractButton.setOnClickListener {
        // "-1" deshace la ÚLTIMA toma real (CBD o THC), no la del modo activo
        if (cbdCount + thcCount <= 0) {
            showFeedback("No hay nada que restar", false)
            return@setOnClickListener
        }

        val today = getCurrentDateKey()
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_delete, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<View>(R.id.chip_confirm).setOnClickListener {
            // "-1" siempre borra la línea: contador y notas quedan alineados (FM1).
            val result = IntakeController.undoLast(this, today, removeNote = true)
            if (result == null) {
                showFeedback("No hay nada que restar", false)
            } else {
                cbdCount = result.cbdCount
                thcCount = result.thcCount
                updateDisplay()
                saveData()
                animateCounter(0.9f)
                val sub = if (result.wasThc) "THC" else "CBD"
                showFeedback("Restado -1 ($sub)", true)
            }
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.chip_cancel).setOnClickListener {
            dialog.dismiss()
        }

        // Mostrar con fondo transparente para que se vea bien el card
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
        resetButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reiniciar contador")
                .setMessage("¿Estás seguro de que quieres reiniciar el contador de hoy? (CBD y THC)")
                .setPositiveButton("Sí") { _, _ ->
                    cbdCount = 0
                    thcCount = 0
                    updateDisplay()
                    saveData()
                    showFeedback("¡Día reiniciado! 💪", true)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

    }

    private fun showFeedback(message: String, isPositive: Boolean) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun animateCounter(scale: Float) {
        counterText.animate().scaleX(scale).scaleY(scale).setDuration(100).withEndAction {
            counterText.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }.start()
    }

    private fun exportCsv() {
        loadAllHistoryData()
        val csvContent = buildCsvContent()
        if (csvContent.isBlank()) {
            showFeedback("No hay datos para exportar", true)
            return
        }

        val exportDir = File(cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val fileName = "cbd_counter_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date()) + ".csv"
        val file = File(exportDir, fileName)

        val uriResult = runCatching {
            file.writeText(csvContent, Charsets.UTF_8)
            val authority = "$packageName.fileprovider"
            FileProvider.getUriForFile(this, authority, file)
        }

        uriResult.onSuccess { uri ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                startActivity(Intent.createChooser(shareIntent, "Compartir CSV"))
                showFeedback("CSV exportado", false)
            } catch (e: Exception) {
                showFeedback("No se encontró una app para compartir el CSV", true)
            }
        }.onFailure { e ->
            showFeedback("Error al exportar CSV: ${e.message}", true)
        }
    }

    private fun buildCsvContent(): String {
        val allDates = Prefs.getAllDatesWithData(this)
        if (allDates.isEmpty()) return ""

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sortedDates = allDates.mapNotNull { dateString ->
            runCatching { dateFormat.parse(dateString) }.getOrNull()?.let { parsed ->
                dateString to parsed
            }
        }.sortedBy { it.second }

        val builder = StringBuilder("${CsvCodec.HEADER}\n")
        sortedDates.forEach { (dateString, _) ->
            val cbdCount = Prefs.getCbdCount(this, dateString)
            val thcCount = Prefs.getThcCount(this, dateString)
            val note = Prefs.getNote(this, dateString) ?: ""

            CsvCodec.appendRow(builder, dateString, cbdCount, thcCount, note)
        }
        return builder.toString()
    }

    private fun handleImportCsv(uri: Uri) {
        val result = runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    val content = reader.readText()
                    val records = CsvCodec.parse(content)
                    if (records.isEmpty()) throw IllegalArgumentException("Archivo vacío")

                    val editor = sharedPrefs.edit()
                    // Limpiar datos existentes
                    sharedPrefs.all.keys.filter {
                        it.startsWith("count_") || it.startsWith("NOTE_")
                    }.forEach { key -> editor.remove(key) }

                    // Detectar formato por cabecera
                    val header = records.first().joinToString(",").lowercase()
                    val isNewFormat = header.contains("count_cbd")
                    val dataRows = if (isNewFormat) {
                        records.drop(1)
                    } else {
                        content.lineSequence()
                            .drop(1)
                            .filter { it.isNotBlank() }
                            .map { CsvCodec.parseLegacyLine(it) }
                            .toList()
                    }

                    dataRows.forEach { columns ->
                        if (columns.size < 2) return@forEach

                        val date = columns[0]

                        if (isNewFormat) {
                            // Nuevo formato: date,count_cbd,count_thc,note
                            val cbdCount = columns.getOrNull(1)?.toIntOrNull() ?: 0
                            val thcCount = columns.getOrNull(2)?.toIntOrNull() ?: 0
                            editor.putInt("${Prefs.KEY_COUNT_CBD_PREFIX}$date", cbdCount)
                            editor.putInt("${Prefs.KEY_COUNT_THC_PREFIX}$date", thcCount)

                            val rawNote = columns.getOrNull(3) ?: ""
                            val note = rawNote
                            if (note.isNotEmpty()) {
                                editor.putString("${Prefs.KEY_NOTE_PREFIX}$date", note)
                            }
                        } else {
                            // Formato legacy: date,count,note,substance
                            val count = columns[1].toIntOrNull() ?: return@forEach
                            val substance = if (columns.size >= 4) columns[3] else "CBD"

                            // Importar al contador correspondiente
                            if (substance == "THC") {
                                editor.putInt("${Prefs.KEY_COUNT_THC_PREFIX}$date", count)
                            } else {
                                editor.putInt("${Prefs.KEY_COUNT_CBD_PREFIX}$date", count)
                            }

                            val rawNote = if (columns.size >= 3) columns[2] else ""
                            val note = rawNote
                            if (note.isNotEmpty()) {
                                editor.putString("${Prefs.KEY_NOTE_PREFIX}$date", note)
                            }
                        }
                    }
                    editor.apply()
                }
            } ?: throw IllegalArgumentException("No se pudo abrir el archivo")
        }

        result.onSuccess {
            runCatching {
                loadTodayData()
                loadAllHistoryData()
                updateDisplay()
                updateHistoryView()
                updateStats()
            }
            showFeedback("Importación completada", false)
        }.onFailure { e ->
            showFeedback("Error al importar CSV: ${e.message}", true)
        }
    }

    private fun registerStandardIntake() {
        val isThc = Prefs.getSubstanceType(this) == "THC"
        val entry = if (isThc) "${IntakeController.ICON_THC} ${getCurrentTimestamp()}" else "${IntakeController.ICON_CBD} ${getCurrentTimestamp()}"
        val feedback = if (isThc) getString(R.string.thc_added) else getString(R.string.cbd_added)
        registerIntake(entry, feedback)
    }

    override fun onVoiceNoteChanged(date: String) {
        historyAdapter.refresh()
    }

    override fun onInfusionTypeSelected(type: String) {
        val infusionType = when (type) {
            "weed" -> InfusionType.WEED
            else -> InfusionType.POLEM
        }
        handleInfusionSelection(infusionType)
    }

    private fun handleInfusionSelection(type: InfusionType) {
        val label = getString(type.labelRes)
        val suffix = getString(R.string.infusion_note_suffix, label)
        val entry = "${type.icon} ${getCurrentTimestamp()}$suffix"
        // Infusión (weed/polen) SIEMPRE suma a THC
        registerThcIntake(entry, getString(type.feedbackRes))
    }

    /** Registra una toma que siempre va al contador THC (para infusiones) */
    private fun registerThcIntake(entry: String, feedbackMessage: String) {
        val snapshot = IntakeController.registerThc(this, getCurrentDateKey(), entry)
        cbdCount = snapshot.cbdCount
        thcCount = snapshot.thcCount
        updateDisplay()
        saveData()
        animateCounter(1.1f)
        showFeedback("$feedbackMessage (THC)", false)
    }

    private fun registerIntake(entry: String, feedbackMessage: String) {
        val snapshot = IntakeController.registerActive(this, getCurrentDateKey(), entry)
        cbdCount = snapshot.cbdCount
        thcCount = snapshot.thcCount
        updateDisplay()
        saveData()
        animateCounter(1.1f)
        showFeedback(feedbackMessage, false)
    }

    private fun getCurrentTimestamp(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    /**
     * Muestra el disclaimer médico la primera vez que se abre la app.
     * Requerido por las políticas de Google Play para apps relacionadas con sustancias.
     */
    private fun showDisclaimerIfNeeded() {
        val disclaimerAccepted = sharedPrefs.getBoolean("disclaimer_accepted", false)
        if (!disclaimerAccepted) {
            val v = LayoutInflater.from(this).inflate(R.layout.dialog_disclaimer, null)
            val dialog = MaterialAlertDialogBuilder(this)
                .setView(v)
                .setCancelable(false)
                .create()

            v.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDisclaimerAccept)
                .setOnClickListener {
                    sharedPrefs.edit().putBoolean("disclaimer_accepted", true).apply()
                    dialog.dismiss()
                }
            v.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDisclaimerDecline)
                .setOnClickListener {
                    Toast.makeText(this, "Debes aceptar el aviso para usar la aplicación", Toast.LENGTH_LONG).show()
                    finish()
                }

            dialog.show()
        }
    }

    private fun animateEmoji() {
        val emojiView = findViewById<TextView>(R.id.emojiText)
        SpringAnimation(emojiView, SpringAnimation.SCALE_X, 1f).apply {
            spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
            spring.stiffness = SpringForce.STIFFNESS_LOW
            setStartValue(0.7f)
            start()
        }
        SpringAnimation(emojiView, SpringAnimation.SCALE_Y, 1f).apply {
            spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
            spring.stiffness = SpringForce.STIFFNESS_LOW
            setStartValue(0.7f)
            start()
        }
    }

    private fun openStatsCalendar() {
        startActivity(Intent(this, DashboardActivity::class.java))
    }
}
