package com.d4vram.cbdcounter

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.*

/**
 * WorkManager worker que ejecuta el auto-backup CSV a la carpeta SAF
 * que el usuario haya elegido en Ajustes. Si no hay carpeta guardada
 * el trabajo falla silenciosamente (Result.failure) sin molestar al usuario.
 */
class BackupWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext
            .getSharedPreferences("CBDCounter", Context.MODE_PRIVATE)
        val folderUriString = prefs.getString("backup_folder_uri", null)
            ?: return Result.failure()

        return try {
            val folderUri = Uri.parse(folderUriString)
            val folderDoc = DocumentFile.fromTreeUri(applicationContext, folderUri)
                ?: return Result.failure()

            val csvContent = buildCsvContent()
            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val fileName = "cbdcounter_autobackup_$stamp.csv"

            val csvDoc = folderDoc.createFile("text/csv", fileName)
                ?: return Result.failure()

            applicationContext.contentResolver.openOutputStream(csvDoc.uri)?.use { os ->
                os.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun buildCsvContent(): String {
        val allDates = Prefs.getAllDatesWithData(applicationContext)
        if (allDates.isEmpty()) return "date,count_cbd,count_thc,note\n"

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sorted = allDates.mapNotNull { ds ->
            runCatching { dateFormat.parse(ds) }.getOrNull()?.let { ds to it }
        }.sortedBy { it.second }

        val sb = StringBuilder("date,count_cbd,count_thc,note\n")
        sorted.forEach { (ds, _) ->
            val cbd  = Prefs.getCbdCount(applicationContext, ds)
            val thc  = Prefs.getThcCount(applicationContext, ds)
            val note = Prefs.getNote(applicationContext, ds) ?: ""
            sb.append(ds).append(',')
                .append(cbd).append(',')
                .append(thc).append(',')
                .append(escapeCsvField(note)).append('\n')
        }
        return sb.toString()
    }

    private fun escapeCsvField(value: String): String {
        if (value.isEmpty()) return ""
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace(",", "\\,")
    }
}
