package com.d4vram.cbdcounter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowInsetsController
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
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
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    // Views principales
    private lateinit var counterText: TextView
    private lateinit var dateText: TextView
    private lateinit var emojiText: TextView
    private lateinit var addButton: Button
    private lateinit var addInfusedButton: MaterialButton
    private lateinit var statsButton: Chip
    private lateinit var subtractButton: Button
    private lateinit var resetButton: Button
    private lateinit var exportButton: ImageButton
    private lateinit var importButton: ImageButton
    private lateinit var settingsButton: ImageButton

    // Botón switch para cambiar el tema
    private lateinit var themeSwitch: SwitchMaterial

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
    private var currentCount = 0
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
        @StringRes val labelRes: Int,
        @StringRes val feedbackRes: Int,
        val icon: String
    ) {
        WEED(R.string.weed_option, R.string.cbd_infused_added_weed, "🌿"),
        POLEM(R.string.polem_option, R.string.cbd_infused_added_polem, "🍫")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Aplicar tema ANTES de super.onCreate para evitar flickering
        initSharedPreferences()
        applyStoredTheme()
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Hacer que el contenido se dibuje detrás de la barra de estado
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.setSystemBarsAppearance(
                0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }

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
        
        // Registrar receptor para cambios de fecha/hora
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        registerReceiver(dateChangeReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        // Desregistrar receptor para evitar memory leaks
        try {
            unregisterReceiver(dateChangeReceiver)
        } catch (e: IllegalArgumentException) {
            // Receptor ya desregistrado, ignorar
        }
    }

    private fun initViews() {
        // Views principales
        counterText = findViewById(R.id.counterText)
        dateText = findViewById(R.id.dateText)
        emojiText = findViewById(R.id.emojiText)
        addButton = findViewById(R.id.addButton)
        addInfusedButton = findViewById(R.id.addInfusedButton)
        statsButton = findViewById(R.id.statsButton)
        subtractButton = findViewById(R.id.subtractButton)
        resetButton = findViewById(R.id.resetButton)
        exportButton = findViewById(R.id.exportButton)
        importButton = findViewById(R.id.importButton)
        settingsButton = findViewById(R.id.settingsButton)
        themeSwitch = findViewById(R.id.themeSwitch)    

        // Estado inicial del switch según preferencias o sistema
        val isNight = isDarkModeEnabled()
        themeSwitch.setOnCheckedChangeListener(null)
        themeSwitch.isChecked = isNight
        
        themeSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked != isDarkModeEnabled()) {
                setDarkMode(checked)
            }
        }

        // Historial
        historyRecyclerView = findViewById(R.id.historyRecyclerView)
        tabLayout = findViewById(R.id.tabLayout)
        statsContainer = findViewById(R.id.statsContainer)
        avgText = findViewById(R.id.avgText)
        totalText = findViewById(R.id.totalText)
        streakText = findViewById(R.id.streakText)
        searchButton = findViewById(R.id.searchButton)

        // Adapter con callback para abrir el modal de notas
        historyAdapter = ImprovedHistoryAdapter(displayedHistoryData) { date ->
            DayModalFragment.newInstance(date).show(supportFragmentManager, "day_modal")
        }
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
        tabLayout.addTab(tabLayout.newTab().setText("Semana"))
        tabLayout.addTab(tabLayout.newTab().setText("Mes"))
        tabLayout.addTab(tabLayout.newTab().setText("Todo"))

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
        currentCount = sharedPrefs.getInt("count_$today", 0)
    }

    private fun loadAllHistoryData() {
        allHistoryData.clear()
        val allEntries = sharedPrefs.all
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        allEntries.forEach { (key, value) ->
            if (key.startsWith("count_") && value is Int) {
                val dateString = key.removePrefix("count_")
                try {
                    val date = dateFormat.parse(dateString)
                    if (date != null) {
                        allHistoryData.add(HistoryItem(dateString, value, date))
                    }
                } catch (_: Exception) {}
            }
        }
        allHistoryData.sortByDescending { it.dateObject }
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
            avgText.text = "Promedio: 0"
            totalText.text = "Total: 0"
            streakText.text = "Racha: 0 días"
            return
        }
        val average = displayedHistoryData.map { it.count }.average()
        avgText.text = "Promedio: %.1f".format(average)
        val total = displayedHistoryData.sumOf { it.count }
        totalText.text = "Total: $total"
        val streak = calculateCleanStreak()
        streakText.text = "Racha limpia: $streak días"
    }

    private fun calculateCleanStreak(): Int {
        var streak = 0
        val sortedData = allHistoryData.sortedByDescending { it.dateObject }
        for (item in sortedData) {
            if (item.count == 0) streak++ else break
        }
        return streak
    }

    private fun saveData() {
        val today = getCurrentDateKey()
        sharedPrefs.edit().putInt("count_$today", currentCount).apply()
        loadAllHistoryData()
        updateHistoryView()
        updateStats()
        CBDWidgetProvider.updateAllWidgets(this)
    }

    private fun isDarkModeEnabled(): Boolean {
        // Si no hay preferencia guardada, usamos el estado actual del delegado o el sistema
        return sharedPrefs.getBoolean(Prefs.KEY_DARK_MODE, 
            AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES ||
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        )
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
        counterText.text = currentCount.toString()
        dateText.text = getCurrentDateDisplay()
        val newEmoji = getEmoji(currentCount)
        
        if (animate && emojiText.text != newEmoji && emojiText.text.isNotEmpty()) {
            emojiText.animate().alpha(0f).setDuration(150).withEndAction {
                emojiText.text = newEmoji
                emojiText.animate().alpha(1f).setDuration(150).start()
            }.start()
        } else {
            emojiText.alpha = 1f
            emojiText.text = newEmoji
        }

        val color = when {
            currentCount == 0 -> R.color.green_safe
            currentCount <= 3 -> R.color.yellow_warning
            currentCount <= 6 -> R.color.orange_danger
            else -> R.color.red_critical
        }
        counterText.setTextColor(ContextCompat.getColor(this, color))
    }

    private fun getEmoji(count: Int): String = EmojiUtils.emojiForCount(count, this)

    private fun setupClickListeners() {
        addButton.setOnClickListener { registerStandardIntake() }
        addInfusedButton.setOnClickListener { showInfusionDialog() }
        statsButton.setOnClickListener { openStatsCalendar() }
        settingsButton.setOnClickListener {
            // Abrir pantalla de ajustes generales
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        subtractButton.setOnClickListener {
        if (currentCount > 0) {
            // Inflar layout personalizado
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_delete, null)
            
            // Crear el diálogo
            val dialog = MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            // Configurar chips
            dialogView.findViewById<View>(R.id.chip_confirm).setOnClickListener {
                currentCount--
                updateDisplay()
                removeLastEntryFromTodayNote()  // 🎯 Borrar último timestamp
                saveData()
                animateCounter(0.9f)
                showFeedback(getString(R.string.cbd_subtracted), true)
                dialog.dismiss()
            }

            dialogView.findViewById<View>(R.id.chip_keep_note).setOnClickListener {
                currentCount--
                updateDisplay()
                // NO borramos la nota, solo restamos el contador
                saveData()
                animateCounter(0.9f)
                showFeedback("Restado -1 (nota mantenida)", true)
                dialog.dismiss()
            }

            dialogView.findViewById<View>(R.id.chip_cancel).setOnClickListener {
                dialog.dismiss()
            }

            // Mostrar con fondo transparente para que se vea bien el card (opcional, pero recomendado si el root es CardView)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()
        }
    }
        resetButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reiniciar contador")
                .setMessage("¿Estás seguro de que quieres reiniciar el contador de hoy?")
                .setPositiveButton("Sí") { _, _ ->
                    currentCount = 0
                    updateDisplay()
                    saveData()
                    showFeedback("¡Día reiniciado! 💪", true)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        exportButton.setOnClickListener { exportCsv() }
        importButton.setOnClickListener {
            // Mostrar diálogo de confirmación antes de importar
            MaterialAlertDialogBuilder(this)
                .setTitle("⚠️ Importar datos")
                .setMessage("Esto BORRARÁ todos tus datos actuales (historial, notas y emojis personalizados) y los reemplazará con los del archivo CSV.\n\n¿Estás seguro de continuar?")
                .setPositiveButton("Sí, importar") { _, _ ->
                    importCsvLauncher.launch(importMimeTypes)
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
        val prefsMap = sharedPrefs.all
        if (prefsMap.isEmpty()) return ""

        val dates = mutableSetOf<String>()
        prefsMap.keys.forEach { key ->
            when {
                key.startsWith("count_") -> dates.add(key.removePrefix("count_"))
                key.startsWith("NOTE_") -> dates.add(key.removePrefix("NOTE_"))
            }
        }
        if (dates.isEmpty()) return ""

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sortedDates = dates.mapNotNull { dateString ->
            runCatching { dateFormat.parse(dateString) }.getOrNull()?.let { parsed ->
                dateString to parsed
            }
        }.sortedBy { it.second }

        val builder = StringBuilder("date,count,note\n")
        sortedDates.forEach { (dateString, _) ->
            val count = sharedPrefs.getInt("count_$dateString", 0)
            val note = Prefs.getNote(this, dateString) ?: ""
            builder.append(dateString)
                .append(',')
                .append(count)
                .append(',')
                .append(escapeCsvField(note))
                .append('\n')
        }
        return builder.toString()
    }

    private fun handleImportCsv(uri: Uri) {
        val result = runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    val lines = reader.readLines()
                    if (lines.isEmpty()) throw IllegalArgumentException("Archivo vacío")

                    val editor = sharedPrefs.edit()
                    sharedPrefs.all.keys.filter {
                        it.startsWith("count_") || it.startsWith("NOTE_")
                    }.forEach { key -> editor.remove(key) }

                    lines.drop(1).forEach { line ->
                        if (line.isBlank()) return@forEach
                        val columns = splitCsvLine(line)
                        if (columns.size < 2) return@forEach

                        val date = columns[0]
                        val count = columns[1].toIntOrNull() ?: return@forEach
                        editor.putInt("count_$date", count)

                        val rawNote = if (columns.size >= 3) columns[2] else ""
                        val note = unescapeCsvField(rawNote)
                        if (note.isNotEmpty()) {
                            editor.putString("NOTE_$date", note)
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

    private fun splitCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var escape = false
        line.forEach { char ->
            when {
                escape -> {
                    current.append(char)
                    escape = false
                }
                char == '\\' -> {
                    current.append(char)
                    escape = true
                }
                char == ',' -> {
                    fields.add(current.toString())
                    current.setLength(0)
                }
                else -> current.append(char)
            }
        }
        fields.add(current.toString())
        return fields
    }

    private fun escapeCsvField(value: String): String {
        if (value.isEmpty()) return ""
        
        // RFC 4180: Si el campo contiene comas, saltos de línea o comillas,
        // debe ir entre comillas dobles. Las comillas dobles se duplican.
        val needsQuotes = value.contains(',') || value.contains('\n') || value.contains('"') || value.contains('\r')
        
        return if (needsQuotes) {
            // Duplicar comillas dobles y envolver todo en comillas
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun unescapeCsvField(value: String): String {
        if (value.isEmpty()) return ""
        
        // RFC 4180: Si el campo está entre comillas, removerlas y desduplicar comillas internas
        if (value.startsWith("\"") && value.endsWith("\"") && value.length >= 2) {
            return value.substring(1, value.length - 1).replace("\"\"", "\"")
        }
        
        // Backward compatibility: Manejar formato antiguo con backslash
        val builder = StringBuilder()
        var escape = false
        value.forEach { char ->
            if (escape) {
                builder.append(
                    when (char) {
                        'n' -> '\n'
                        '\\' -> '\\'
                        ',' -> ','
                        else -> char
                    }
                )
                escape = false
            } else if (char == '\\') {
                escape = true
            } else {
                builder.append(char)
            }
        }
        if (escape) builder.append('\\')
        return builder.toString()
    }

    private fun registerStandardIntake() {
        val entry = "🔹 ${getCurrentTimestamp()}"
        registerIntake(entry, getString(R.string.cbd_added))
    }

    private fun showInfusionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_infusion_choice, null)
        val weedButton = dialogView.findViewById<MaterialButton>(R.id.weedButton)
        val polemButton = dialogView.findViewById<MaterialButton>(R.id.polemButton)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        weedButton.text = "${InfusionType.WEED.icon} ${getString(InfusionType.WEED.labelRes)}"
        polemButton.text = "${InfusionType.POLEM.icon} ${getString(InfusionType.POLEM.labelRes)}"

        weedButton.setOnClickListener {
            handleInfusionSelection(InfusionType.WEED)
            dialog.dismiss()
        }
        polemButton.setOnClickListener {
            handleInfusionSelection(InfusionType.POLEM)
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun handleInfusionSelection(type: InfusionType) {
        val label = getString(type.labelRes)
        val suffix = getString(R.string.infusion_note_suffix, label)
        val entry = "${type.icon} ${getCurrentTimestamp()}$suffix"
        registerIntake(entry, getString(type.feedbackRes))
    }

    private fun registerIntake(entry: String, feedbackMessage: String) {
        currentCount++
        updateDisplay()
        appendEntryToTodayNote(entry)
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
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.disclaimer_title)
                .setMessage(R.string.disclaimer_message)
                .setPositiveButton(R.string.disclaimer_accept) { _, _ ->
                    sharedPrefs.edit().putBoolean("disclaimer_accepted", true).apply()
                }
                .setNegativeButton(R.string.disclaimer_decline) { _, _ ->
                    // Si el usuario no acepta, cerrar la app
                    Toast.makeText(
                        this,
                        "Debes aceptar el aviso para usar la aplicación",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                .setCancelable(false) // No puede cancelar con el botón atrás
                .show()
        }
    }

    private fun appendEntryToTodayNote(entry: String) {
        val today = getCurrentDateKey()
        val currentNote = Prefs.getNote(this, today)

        val updatedNote = if (currentNote.isNullOrBlank()) {
            entry
        } else {
            buildString {
                append(currentNote)
                if (!currentNote.endsWith("\n")) append("\n")
                append(entry)
            }
        }

        Prefs.setNote(this, today, updatedNote)
    }

    private fun removeLastEntryFromTodayNote() {
        val today = getCurrentDateKey()
        val currentNote = Prefs.getNote(this, today)

        // Si no hay nota o está vacía, no hay nada que borrar
        if (currentNote.isNullOrBlank()) return

        // Dividir la nota en líneas
        val lines = currentNote.split("\n").toMutableList()

        // Eliminar la última línea
        if (lines.isNotEmpty()) {
            lines.removeAt(lines.lastIndex)
        }

        // Si quedan líneas, unirlas de nuevo; si no, guardar null
        val updatedNote = if (lines.isNotEmpty()) {
            lines.joinToString("\n")
        } else {
            null
        }

        Prefs.setNote(this, today, updatedNote)
    }

    private fun openStatsCalendar() {
        startActivity(Intent(this, StatsActivity::class.java))
    }
}

// Data class
data class HistoryItem(val date: String, val count: Int, val dateObject: Date)

// Adapter
class ImprovedHistoryAdapter(
    private val historyList: List<HistoryItem>,
    private val onDayClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
    }

    private val groupedData = mutableListOf<Any>()

    init { groupData() }

    private fun groupData() {
        groupedData.clear()
        if (historyList.isEmpty()) return
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
        var lastMonth = ""
        historyList.forEach { item ->
            val monthYear = dateFormat.format(item.dateObject)
                .replaceFirstChar { it.uppercase() }
            if (monthYear != lastMonth) {
                groupedData.add(monthYear)
                lastMonth = monthYear
            }
            groupedData.add(item)
        }
    }

    override fun getItemViewType(position: Int) =
        if (groupedData[position] is String) TYPE_HEADER else TYPE_ITEM

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.historyDate)
        val countText: TextView = itemView.findViewById(R.id.historyCount)
        val emojiText: TextView = itemView.findViewById(R.id.historyEmoji)
        val progressBar: View = itemView.findViewById(R.id.progressBar)
        val noteBadge: TextView? = itemView.findViewById(R.id.noteBadge)
        val audioBadge: TextView? = itemView.findViewById(R.id.audioBadge)
    }
    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val headerText: TextView = itemView.findViewById(R.id.headerText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        if (viewType == TYPE_HEADER) {
            HeaderViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.history_header, parent, false))
        } else {
            ItemViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.history_item, parent, false))
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.headerText.text = groupedData[position] as String
            is ItemViewHolder -> {
                val item = groupedData[position] as HistoryItem
                val dayFormat = SimpleDateFormat("EEEE dd", Locale("es", "ES"))
                holder.dateText.text = dayFormat.format(item.dateObject)
                    .replaceFirstChar { it.uppercase() }
                holder.countText.text = "${item.count} CBD"

                holder.emojiText.text = EmojiUtils.emojiForCount(item.count, holder.itemView.context)

                // Barra de progreso (como ya tenías)
                val maxWidth = holder.itemView.width
                val progress = minOf(item.count / 10f, 1f)
                val layoutParams = holder.progressBar.layoutParams
                layoutParams.width = (maxWidth * progress).toInt()
                holder.progressBar.layoutParams = layoutParams
                val color = when {
                    item.count == 0 -> R.color.green_safe
                    item.count <= 3 -> R.color.yellow_warning
                    item.count <= 6 -> R.color.orange_danger
                    else -> R.color.red_critical
                }
                holder.progressBar.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, color)
                )

                // --- NUEVO: badge de nota visible si existe nota para ese día
                val ctx = holder.itemView.context
                holder.noteBadge?.visibility =
                    if (Prefs.hasNote(ctx, item.date)) View.VISIBLE else View.GONE

                // --- NUEVO: badge de audio visible si existe audio para ese día
                holder.audioBadge?.visibility =
                    if (Prefs.hasAudio(ctx, item.date)) View.VISIBLE else View.GONE

                // --- NUEVO: clicks para abrir el modal
                holder.itemView.setOnClickListener { onDayClick(item.date) }
                holder.noteBadge?.setOnClickListener { onDayClick(item.date) }
                holder.audioBadge?.setOnClickListener { onDayClick(item.date) }
            }
        }
    }

    override fun getItemCount() = groupedData.size

    fun refresh() {
        groupData()
        notifyDataSetChanged()
    }
}


// Decoración
class HistoryItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: android.graphics.Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.bottom = spacing
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = spacing
        }
    }
}
