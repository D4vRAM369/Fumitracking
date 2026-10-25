package dev.d4vram.cbdcounter

import android.content.Context
import java.io.File

object VoiceNoteRepository {
    private const val CANONICAL_DIR = "voice_notes"
    private const val LEGACY_DIR = "audios"

    fun safeFileName(date: String): String = date.replace("/", "-")

    fun outputFile(context: Context, date: String): File {
        val dir = File(context.filesDir, CANONICAL_DIR).also { it.mkdirs() }
        return File(dir, "${safeFileName(date)}.m4a")
    }

    fun existingFile(context: Context, date: String): File? {
        val canonical = outputFile(context, date)
        if (canonical.exists()) return canonical

        return legacyFiles(context, date).firstOrNull { it.exists() }
    }

    fun hasVoiceNote(context: Context, date: String): Boolean = existingFile(context, date) != null

    fun deleteAllForDate(context: Context, date: String) {
        outputFile(context, date).delete()
        legacyFiles(context, date).forEach { it.delete() }
    }

    fun exportEntries(context: Context): List<Pair<File, String>> {
        val entries = mutableListOf<Pair<File, String>>()
        addDirectoryEntries(File(context.filesDir, CANONICAL_DIR), CANONICAL_DIR, entries)
        addDirectoryEntries(File(context.filesDir, LEGACY_DIR), LEGACY_DIR, entries)
        return entries.distinctBy { it.second }
    }

    private fun legacyFiles(context: Context, date: String): List<File> {
        val dir = File(context.filesDir, LEGACY_DIR)
        val unsafeName = "audio_$date"
        val safeName = "audio_${safeFileName(date)}"
        return listOf(
            File(dir, "$unsafeName.3gp"),
            File(dir, "$unsafeName.mp3"),
            File(dir, "$unsafeName.m4a"),
            File(dir, "$safeName.3gp"),
            File(dir, "$safeName.mp3"),
            File(dir, "$safeName.m4a")
        )
    }

    private fun addDirectoryEntries(
        dir: File,
        entryDir: String,
        entries: MutableList<Pair<File, String>>
    ) {
        if (!dir.exists() || !dir.isDirectory) return

        dir.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val relativePath = file.relativeTo(dir).invariantSeparatorsPath
                entries.add(file to "$entryDir/$relativePath")
            }
    }
}
