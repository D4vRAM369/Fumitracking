package com.d4vram.cbdcounter

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BackupManager {

    private const val TAG = "BackupManager"
    private const val PREFS_NAME = "CBDCounter"
    private const val EMOJI_PREFS_NAME = "emoji_prefs"
    private const val JSON_FILE_NAME = "data.json"

    // Estructura del JSON de backup
    // {
    //   "version": 1,
    //   "timestamp": 1234567890,
    //   "prefs": { ... },
    //   "emoji_prefs": { ... }
    // }

    fun createBackup(context: Context, destinationUri: Uri? = null): File? {
        try {
            // 1. Recopilar datos
            val mainPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).all
            val emojiPrefs = context.getSharedPreferences(EMOJI_PREFS_NAME, Context.MODE_PRIVATE).all

            val backupJson = JSONObject()
            backupJson.put("version", 1)
            backupJson.put("timestamp", System.currentTimeMillis())
            
            // Convertir mapas a JSONObjects
            val prefsJson = JSONObject()
            mainPrefs.forEach { (k, v) -> prefsJson.put(k, v) }
            backupJson.put("prefs", prefsJson)

            val emojiJson = JSONObject()
            emojiPrefs.forEach { (k, v) -> emojiJson.put(k, v) }
            backupJson.put("emoji_prefs", emojiJson)

            // 2. Crear archivo temporal para el JSON
            val tempDir = File(context.cacheDir, "backup_temp")
            if (tempDir.exists()) tempDir.deleteRecursively()
            tempDir.mkdirs()

            val jsonFile = File(tempDir, JSON_FILE_NAME)
            jsonFile.writeText(backupJson.toString(2))

            // 3. Crear archivo ZIP de destino
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val zipFileName = "cbd_backup_$timeStamp.zip"
            val zipFile = File(context.getExternalFilesDir(null), "backups/$zipFileName")
            zipFile.parentFile?.mkdirs()

            // 4. Escribir ZIP (incluyendo JSON y carpeta de audios)
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                // Agregar JSON
                addToZip(zos, jsonFile, JSON_FILE_NAME)

                // Agregar Audios
                val audioDir = File(context.filesDir, "audios")
                if (audioDir.exists() && audioDir.isDirectory) {
                    audioDir.listFiles()?.forEach { audioFile ->
                        addToZip(zos, audioFile, "audios/${audioFile.name}")
                    }
                }
            }

            // Limpiar
            tempDir.deleteRecursively()

            return zipFile

        } catch (e: Exception) {
            Log.e(TAG, "Error creating backup", e)
            return null
        }
    }

    private fun addToZip(zos: ZipOutputStream, file: File, entryName: String) {
        if (!file.exists()) return
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        file.inputStream().use { it.copyTo(zos) }
        zos.closeEntry()
    }

    fun restoreBackup(context: Context, uri: Uri): Boolean {
        val tempDir = File(context.cacheDir, "restore_temp")
        if (tempDir.exists()) tempDir.deleteRecursively()
        tempDir.mkdirs()

        try {
            // 1. Descomprimir ZIP
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                java.util.zip.ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val file = File(tempDir, entry.name)
                        if (entry.isDirectory) {
                            file.mkdirs()
                        } else {
                            file.parentFile?.mkdirs()
                            file.outputStream().use { fos -> zis.copyTo(fos) }
                        }
                        entry = zis.nextEntry
                    }
                }
            }

            // 2. Leer y validar JSON
            val jsonFile = File(tempDir, JSON_FILE_NAME)
            if (!jsonFile.exists()) return false

            val jsonContent = jsonFile.readText()
            val backupJson = JSONObject(jsonContent)

            // 3. Restaurar Prefs (Main)
            val prefsJson = backupJson.optJSONObject("prefs")
            if (prefsJson != null) {
                val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                editor.clear() // Opcional: limpiar antes de restaurar
                for (key in prefsJson.keys()) {
                    val value = prefsJson.get(key)
                    when (value) {
                        is Boolean -> editor.putBoolean(key, value)
                        is Int -> editor.putInt(key, value)
                        is Long -> editor.putLong(key, value)
                        is Float -> editor.putFloat(key, value.toFloat())
                        is String -> editor.putString(key, value)
                    }
                }
                editor.apply()
            }

            // 4. Restaurar Prefs (Emojis)
            val emojiJson = backupJson.optJSONObject("emoji_prefs")
            if (emojiJson != null) {
                val editor = context.getSharedPreferences(EMOJI_PREFS_NAME, Context.MODE_PRIVATE).edit()
                editor.clear()
                for (key in emojiJson.keys()) {
                    val value = emojiJson.get(key)
                    if (value is String) editor.putString(key, value)
                }
                editor.apply()
            }

            // 5. Restaurar Audios
            val audiosDir = File(tempDir, "audios")
            if (audiosDir.exists() && audiosDir.isDirectory) {
                val targetDir = File(context.filesDir, "audios")
                targetDir.mkdirs()
                audiosDir.listFiles()?.forEach { audioFile ->
                    audioFile.copyTo(File(targetDir, audioFile.name), overwrite = true)
                }
            }

            return true

        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup", e)
            return false
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
