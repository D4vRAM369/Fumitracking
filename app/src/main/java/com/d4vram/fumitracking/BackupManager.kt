package com.d4vram.fumitracking

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupManager {

    private const val TAG = "BackupManager"
    private const val PREFS_NAME = "CBDCounter"
    private const val EMOJI_PREFS_NAME = "emoji_prefs"
    private const val JSON_FILE_NAME = "data.json"

    // Cifrado: [MAGIC][salt 16][iv 12][AES-256-GCM(zip)]. La clave sale de la contraseña (PBKDF2).
    private val MAGIC = "CBDENC1".toByteArray()
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val KDF_ITERATIONS = 120_000

    // Estructura del JSON de backup
    // {
    //   "version": 1,
    //   "timestamp": 1234567890,
    //   "prefs": { ... },
    //   "emoji_prefs": { ... }
    // }

    fun createBackup(context: Context, password: CharArray? = null): File? {
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
            val ext = if (password != null) "cbdenc" else "zip"
            val zipFileName = "cbd_backup_$timeStamp.$ext"
            val zipFile = File(context.getExternalFilesDir(null), "backups/$zipFileName")
            zipFile.parentFile?.mkdirs()

            // 4. Escribir ZIP (incluyendo JSON y carpeta de audios)
            val raw = BufferedOutputStream(FileOutputStream(zipFile))
            val sink = if (password != null) encryptingStream(raw, password) else raw
            ZipOutputStream(sink).use { zos ->
                // Agregar JSON
                addToZip(zos, jsonFile, JSON_FILE_NAME)

                // Agregar notas de voz canónicas y archivos legacy.
                VoiceNoteRepository.exportEntries(context).forEach { (audioFile, entryName) ->
                    addToZip(zos, audioFile, entryName)
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

    /** true si el archivo es un backup cifrado por esta app (empieza por MAGIC). */
    fun isEncrypted(context: Context, uri: Uri): Boolean =
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { hasMagic(it) } ?: false
        }.getOrDefault(false)

    internal fun hasMagic(input: InputStream): Boolean {
        val head = ByteArray(MAGIC.size)
        return runCatching { DataInputStream(input).readFully(head); head.contentEquals(MAGIC) }
            .getOrDefault(false)
    }

    internal fun encryptingStream(out: OutputStream, password: CharArray): OutputStream {
        val rnd = SecureRandom()
        val salt = ByteArray(SALT_LEN).also { rnd.nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { rnd.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
        out.write(MAGIC); out.write(salt); out.write(iv)
        return CipherOutputStream(out, cipher)
    }

    internal fun decryptingStream(input: InputStream, password: CharArray): InputStream {
        val din = DataInputStream(input)
        val magic = ByteArray(MAGIC.size).also { din.readFully(it) }
        require(magic.contentEquals(MAGIC)) { "No es un backup cifrado" }
        val salt = ByteArray(SALT_LEN).also { din.readFully(it) }
        val iv = ByteArray(IV_LEN).also { din.readFully(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
        return CipherInputStream(din, cipher)
    }

    // ponytail: PBKDF2WithHmacSHA1 porque SHA256 pide API 26 y minSdk es 24; subir a SHA256 si sube el minSdk.
    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, KDF_ITERATIONS, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        return SecretKeySpec(bytes, "AES")
    }

    fun restoreBackup(context: Context, uri: Uri, password: CharArray? = null): Boolean {
        val tempDir = File(context.cacheDir, "restore_temp")
        if (tempDir.exists()) tempDir.deleteRecursively()
        tempDir.mkdirs()

        try {
            // 1. Descomprimir ZIP
            context.contentResolver.openInputStream(uri)?.use { raw ->
                val src = BufferedInputStream(raw)
                val plain = if (password != null) decryptingStream(src, password) else src
                val zis = ZipInputStream(plain)
                var entry = zis.nextEntry
                while (entry != null) {
                    val file = safeZipTarget(tempDir, entry.name)
                        ?: throw IllegalArgumentException("Entrada ZIP no segura: ${entry.name}")
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        file.outputStream().use { fos -> zis.copyTo(fos) }
                    }
                    entry = zis.nextEntry
                }
                // Leer hasta el final: GCM solo verifica contraseña/integridad al terminar.
                val sink = ByteArray(8192)
                while (plain.read(sink) != -1) { /* drenar */ }
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
            restoreAudioDirectory(tempDir, context, "audios")
            restoreAudioDirectory(tempDir, context, "voice_notes")

            return true

        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup", e)
            return false
        } finally {
            tempDir.deleteRecursively()
        }
    }

    internal fun safeZipTarget(tempDir: File, entryName: String): File? {
        val root = tempDir.canonicalFile
        val target = File(root, entryName).canonicalFile
        val rootPath = root.path
        val targetPath = target.path

        return if (targetPath == rootPath || targetPath.startsWith(rootPath + File.separator)) {
            target
        } else {
            null
        }
    }

    private fun restoreAudioDirectory(tempDir: File, context: Context, name: String) {
        val sourceDir = File(tempDir, name)
        if (!sourceDir.exists() || !sourceDir.isDirectory) return

        val targetDir = File(context.filesDir, name)
        targetDir.mkdirs()
        sourceDir.walkTopDown()
            .filter { it.isFile }
            .forEach { audioFile ->
                val relativePath = audioFile.relativeTo(sourceDir)
                val targetFile = File(targetDir, relativePath.path)
                targetFile.parentFile?.mkdirs()
                audioFile.copyTo(targetFile, overwrite = true)
            }
    }
}
