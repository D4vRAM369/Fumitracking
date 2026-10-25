package dev.d4vram.cbdcounter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SettingsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EmojiRangeAdapter

    // SAF folder picker — must be registered before onCreate
    private val openFolderLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                // Guardar permiso persistente para poder escribir después de reinicios
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                saveFolderUri(uri.toString())
                updateFolderDisplay(uri.toString())
                Toast.makeText(this, getString(R.string.backup_folder_saved), Toast.LENGTH_SHORT).show()
            }
        }

    private val emojiRanges = listOf(
        EmojiRange(0,  "😌", R.color.green_safe,     "0"),
        EmojiRange(1,  "🙂", R.color.green_safe,     "1-2"),
        EmojiRange(3,  "😄", R.color.yellow_warning, "3-4"),
        EmojiRange(5,  "🫠", R.color.yellow_warning, "5"),
        EmojiRange(6,  "🤔", R.color.orange_danger,  "6"),
        EmojiRange(7,  "🙄", R.color.orange_danger,  "7"),
        EmojiRange(8,  "😶‍🌫️", R.color.orange_danger, "8"),
        EmojiRange(9,  "🫡", R.color.red_critical,   "9"),
        EmojiRange(10, "🫥", R.color.red_critical,   "10"),
        EmojiRange(11, "⛔️", R.color.red_critical,   "11"),
        EmojiRange(12, "💀", R.color.primary_purple, "12+")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        EdgeToEdgeUtils.setStatusBarColor(this, R.color.gradient_start)

        val toolbar = findViewById<MaterialToolbar>(R.id.settingsToolbar)
        toolbar.setNavigationOnClickListener { finish() }

        setupBackupSection()
        setupEmojiSection()
        setupExtrasSection()
    }

    // ============================================================
    // SECCIÓN: Backup
    // ============================================================
    private fun setupBackupSection() {
        val autoSwitch        = findViewById<SwitchMaterial>(R.id.switchBackupAuto)
        val layoutInterval    = findViewById<View>(R.id.layoutBackupInterval)
        val toggleInterval    = findViewById<MaterialButtonToggleGroup>(R.id.toggleBackupInterval)
        val btnChangeFolder   = findViewById<MaterialButton>(R.id.btnChangeFolder)
        val btnBackupCsv      = findViewById<MaterialButton>(R.id.btnBackupCsv)
        val btnExportAudios   = findViewById<MaterialButton>(R.id.btnExportAudios)

        val prefs = getSharedPreferences("CBDCounter", MODE_PRIVATE)

        // Restaurar estado guardado
        val autoEnabled = prefs.getBoolean("auto_backup", false)
        val intervalH   = prefs.getLong("backup_interval_hours", 24L)
        autoSwitch.isChecked      = autoEnabled
        layoutInterval.visibility = if (autoEnabled) View.VISIBLE else View.GONE
        if (intervalH == 12L) toggleInterval.check(R.id.btn12h)
        else toggleInterval.check(R.id.btn24h)

        // Mostrar carpeta guardada
        updateFolderDisplay(prefs.getString("backup_folder_uri", null))

        // Auto-backup switch
        autoSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_backup", isChecked).apply()
            layoutInterval.visibility = if (isChecked) View.VISIBLE else View.GONE
            val hours = prefs.getLong("backup_interval_hours", 24L)
            scheduleAutoBackup(isChecked, hours)
            Toast.makeText(
                this,
                "Backup automático ${if (isChecked) "activado" else "desactivado"}",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Selector de intervalo
        toggleInterval.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val hours = if (checkedId == R.id.btn12h) 12L else 24L
                prefs.edit().putLong("backup_interval_hours", hours).apply()
                if (autoSwitch.isChecked) scheduleAutoBackup(true, hours)
            }
        }

        // Elegir carpeta SAF
        btnChangeFolder.setOnClickListener {
            openFolderLauncher.launch(null)
        }

        btnBackupCsv.setOnClickListener    { exportCsvToSaf() }
        btnExportAudios.setOnClickListener { exportZipToSaf() }
    }

    // ============================================================
    // WorkManager scheduling
    // ============================================================
    private fun scheduleAutoBackup(enabled: Boolean, intervalHours: Long) {
        val wm = WorkManager.getInstance(this)
        if (enabled) {
            val request = PeriodicWorkRequestBuilder<BackupWorker>(
                intervalHours, TimeUnit.HOURS
            ).build()
            wm.enqueueUniquePeriodicWork(
                "auto_backup",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        } else {
            wm.cancelUniqueWork("auto_backup")
        }
    }

    // ============================================================
    // SAF helpers
    // ============================================================
    private fun saveFolderUri(uriString: String) {
        getSharedPreferences("CBDCounter", MODE_PRIVATE)
            .edit().putString("backup_folder_uri", uriString).apply()
    }

    private fun updateFolderDisplay(uriString: String?) {
        val tvFolder = findViewById<TextView>(R.id.tvBackupFolder)
        if (uriString == null) {
            tvFolder.text = getString(R.string.backup_folder_none)
            return
        }
        val uri  = Uri.parse(uriString)
        val name = DocumentFile.fromTreeUri(this, uri)?.name ?: uriString
        tvFolder.text = name
    }

    private fun getSafFolderDoc(): DocumentFile? {
        val uri = getSharedPreferences("CBDCounter", MODE_PRIVATE)
            .getString("backup_folder_uri", null) ?: return null
        return DocumentFile.fromTreeUri(this, Uri.parse(uri))
    }

    // ============================================================
    // Exportar CSV — primero intenta SAF, fallback a FileProvider
    // ============================================================
    private fun exportCsvToSaf() {
        val csvContent = buildCsvContent()
        if (csvContent.isBlank()) {
            Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show()
            return
        }

        val folderDoc = getSafFolderDoc()
        if (folderDoc != null) {
            // Escribe directamente en la carpeta elegida — sin FileProvider
            try {
                val stamp    = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                val fileName = "cbdcounter_backup_$stamp.csv"
                val csvDoc   = folderDoc.createFile("text/csv", fileName)
                if (csvDoc != null) {
                    contentResolver.openOutputStream(csvDoc.uri)?.use { os ->
                        os.write(csvContent.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(this, getString(R.string.backup_csv_saved), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al crear el archivo en la carpeta", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Fallback — carpeta exports/ (declarada en file_paths.xml)
            val exportDir = File(cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val stamp     = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val file      = File(exportDir, "cbdcounter_backup_$stamp.csv")
            try {
                file.writeText(csvContent, Charsets.UTF_8)
                shareFile(file, "text/csv", "Compartir Backup CSV")
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ============================================================
    // Exportar ZIP de audios — mismo patrón SAF/fallback
    // ============================================================
    private fun exportZipToSaf() {
        val audioEntries = VoiceNoteRepository.exportEntries(this)
        if (audioEntries.isEmpty()) {
            Toast.makeText(this, "No hay audios para exportar", Toast.LENGTH_SHORT).show()
            return
        }

        val folderDoc = getSafFolderDoc()
        if (folderDoc != null) {
            try {
                val stamp    = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                val fileName = "cbdcounter_audios_$stamp.zip"
                val zipDoc   = folderDoc.createFile("application/zip", fileName)
                if (zipDoc != null) {
                    contentResolver.openOutputStream(zipDoc.uri)?.use { os ->
                        ZipOutputStream(os).use { zos ->
                            audioEntries.forEach { (f, entryName) ->
                                FileInputStream(f).use { fis ->
                                    zos.putNextEntry(ZipEntry(entryName))
                                    fis.copyTo(zos)
                                    zos.closeEntry()
                                }
                            }
                        }
                    }
                    Toast.makeText(this, getString(R.string.backup_zip_saved), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al crear el archivo en la carpeta", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Fallback — carpeta exports/ (declarada en file_paths.xml)
            val exportDir = File(cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val zipFile = File(exportDir, "audios_export.zip")
            try {
                ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                    audioEntries.forEach { (f, entryName) ->
                        FileInputStream(f).use { fis ->
                            zos.putNextEntry(ZipEntry(entryName))
                            fis.copyTo(zos)
                            zos.closeEntry()
                        }
                    }
                }
                shareFile(zipFile, "application/zip", "Compartir ZIP de audios")
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareFile(file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, title))
    }

    private fun buildCsvContent(): String {
        val allDates = Prefs.getAllDatesWithData(this)
        if (allDates.isEmpty()) return ""

        val dateFormat  = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sortedDates = allDates.mapNotNull { ds ->
            runCatching { dateFormat.parse(ds) }.getOrNull()?.let { ds to it }
        }.sortedBy { it.second }

        val sb = StringBuilder("${CsvCodec.HEADER}\n")
        sortedDates.forEach { (ds, _) ->
            CsvCodec.appendRow(
                sb,
                ds,
                Prefs.getCbdCount(this, ds),
                Prefs.getThcCount(this, ds),
                Prefs.getNote(this, ds) ?: ""
            )
        }
        return sb.toString()
    }

    // ============================================================
    // SECCIÓN: Personalizar Emojis
    // ============================================================
    private fun setupEmojiSection() {
        recyclerView = findViewById(R.id.emojiRangesRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val currentEmojis = loadCustomEmojis()
        adapter = EmojiRangeAdapter(emojiRanges, currentEmojis) { range, newEmoji ->
            saveCustomEmoji(range.count, newEmoji)
        }
        recyclerView.adapter = adapter

        findViewById<MaterialButton>(R.id.resetEmojiButton).setOnClickListener {
            showResetConfirmationDialog()
        }
    }

    private fun loadCustomEmojis(): Map<Int, String> {
        val prefs = getSharedPreferences("emoji_prefs", MODE_PRIVATE)
        val map   = mutableMapOf<Int, String>()
        for (range in emojiRanges) {
            prefs.getString("emoji_${range.count}", null)?.let { map[range.count] = it }
        }
        return map
    }

    private fun saveCustomEmoji(count: Int, emoji: String) {
        getSharedPreferences("emoji_prefs", MODE_PRIVATE)
            .edit().putString("emoji_$count", emoji).apply()
    }

    private fun resetToDefaults() {
        getSharedPreferences("emoji_prefs", MODE_PRIVATE).edit().clear().apply()
        adapter.resetToDefaults()
        Toast.makeText(this, "Emojis restaurados", Toast.LENGTH_SHORT).show()
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Restaurar emojis")
            .setMessage("¿Restaurar todos los emojis a sus valores por defecto?")
            .setPositiveButton("Sí") { _, _ -> resetToDefaults() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEmojiPicker(currentEmoji: String, onEmojiSelected: (String) -> Unit) {
        val emojis = listOf(
            "😌", "🙂", "😊", "😀", "😃", "😄", "😁", "😆", "😅", "🤣",
            "😂", "🙃", "😉", "😇", "🤩", "☺️", "🥲", "😋", "😛", "😜", "🤪", "😝",
            "🤔", "🤨", "😐", "😑", "😶", "🙄", "😣", "😥", "😮", "😯", "😪", "😫", "🥱", "😴", "🤤",
            "🫠", "😵", "😵‍💫", "🤯", "🥴", "😲",
            "🫡", "😬", "🫨", "🫥",
            "😞", "😔", "😟", "😕", "🙁", "☹️", "😰", "😨", "😧", "😦", "😈",
            "👿", "💀", "☠️", "👻", "👽", "👾",
            "👍", "👎", "🤞", "✌️", "👌", "🤌", "🤏", "✋", "🤚",
            "🌿", "🍀", "🌱", "🌾", "🪴", "🍃",
            "⚠️", "🚫", "⛔️", "🔞", "📵", "🔕", "❌", "⭕️", "❗️", "❓",
            "🟢", "🟡", "🟠", "🔴", "🟣", "🔵", "🟤", "⚫️", "⚪️",
            "💚", "💛", "🧡", "❤️", "💜", "💙", "🖤", "🤍", "🤎", "💯",
            "💥", "💫", "⭐️", "🌟", "✨", "⚡️", "🔥"
        )

        val emojiArray = emojis.toTypedArray()
        var selectedIndex = emojis.indexOf(currentEmoji).takeIf { it >= 0 } ?: 0

        AlertDialog.Builder(this)
            .setTitle("Selecciona un emoji")
            .setSingleChoiceItems(emojiArray, selectedIndex) { _, which -> selectedIndex = which }
            .setPositiveButton("Aceptar") { _, _ -> onEmojiSelected(emojis[selectedIndex]) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupExtrasSection() {
        findViewById<MaterialButton>(R.id.btnGoals).setOnClickListener {
            startActivity(Intent(this, GoalsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnInsights).setOnClickListener {
            startActivity(Intent(this, InsightsActivity::class.java))
        }
    }

    // ============================================================
    // Data classes & Adapter
    // ============================================================
    data class EmojiRange(
        val count: Int,
        val defaultEmoji: String,
        val colorRes: Int,
        val rangeText: String
    )

    inner class EmojiRangeAdapter(
        private val ranges: List<EmojiRange>,
        private var customEmojis: Map<Int, String>,
        private val onEmojiChanged: (EmojiRange, String) -> Unit
    ) : RecyclerView.Adapter<EmojiRangeAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val colorIndicator: View     = view.findViewById(R.id.colorIndicator)
            val rangeText: TextView      = view.findViewById(R.id.rangeText)
            val emojiText: TextView      = view.findViewById(R.id.emojiText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_emoji_range, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val range        = ranges[position]
            val currentEmoji = customEmojis[range.count] ?: range.defaultEmoji

            holder.rangeText.text = range.rangeText
            holder.emojiText.text = currentEmoji
            holder.colorIndicator.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, range.colorRes)
            )

            holder.emojiText.setOnClickListener {
                showEmojiPicker(currentEmoji) { newEmoji ->
                    val updated = customEmojis.toMutableMap()
                    updated[range.count] = newEmoji
                    customEmojis = updated
                    notifyItemChanged(position)
                    onEmojiChanged(range, newEmoji)
                }
            }
        }

        override fun getItemCount() = ranges.size

        fun resetToDefaults() {
            customEmojis = emptyMap()
            notifyDataSetChanged()
        }
    }
}
