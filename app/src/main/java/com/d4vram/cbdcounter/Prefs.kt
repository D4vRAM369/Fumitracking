package com.d4vram.cbdcounter

import android.content.Context
import android.content.SharedPreferences
import java.io.File

object Prefs {
    private const val PREFS_NAME = "CBDCounter"

    // Constantes de claves para evitar errores tipográficos
    const val KEY_COUNT_PREFIX = "count_"  // Legacy, para migración
    const val KEY_COUNT_CBD_PREFIX = "count_cbd_"
    const val KEY_COUNT_THC_PREFIX = "count_thc_"
    const val KEY_NOTE_PREFIX = "NOTE_"
    const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_MIGRATION_V14_DONE = "migration_v1.4_done"

    // función privada para acceder a las SharedPreferences
    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---- Helpers para las NOTAS ----
    fun getNote(ctx: Context, date: String): String? =
        prefs(ctx).getString("${KEY_NOTE_PREFIX}$date", null)

    fun setNote(ctx: Context, date: String, note: String?) {
        prefs(ctx).edit().apply {
            if (note.isNullOrBlank()) {
                remove("${KEY_NOTE_PREFIX}$date")  // borra la nota si está vacía
            } else {
                putString("${KEY_NOTE_PREFIX}$date", note)  // guarda la nota
            }
        }.apply()
    }

    fun hasNote(ctx: Context, date: String): Boolean =
        prefs(ctx).contains("${KEY_NOTE_PREFIX}$date")

    fun hasAudio(ctx: Context, date: String): Boolean {
        val audioFile = File(ctx.filesDir, "audios/audio_$date.mp3")
        return audioFile.exists()
    }

    // ---- Sustancia (CBD vs THC) - Modo activo ----
    fun getSubstanceType(ctx: Context): String =
        prefs(ctx).getString("substance_type", "CBD") ?: "CBD"

    fun setSubstanceType(ctx: Context, type: String) {
        prefs(ctx).edit().putString("substance_type", type).apply()
    }

    // ---- Contadores separados CBD/THC ----
    fun getCbdCount(ctx: Context, date: String): Int =
        prefs(ctx).getInt("${KEY_COUNT_CBD_PREFIX}$date", 0)

    fun setCbdCount(ctx: Context, date: String, count: Int) {
        prefs(ctx).edit().putInt("${KEY_COUNT_CBD_PREFIX}$date", count).apply()
    }

    fun getThcCount(ctx: Context, date: String): Int =
        prefs(ctx).getInt("${KEY_COUNT_THC_PREFIX}$date", 0)

    fun setThcCount(ctx: Context, date: String, count: Int) {
        prefs(ctx).edit().putInt("${KEY_COUNT_THC_PREFIX}$date", count).apply()
    }

    fun getTotalCount(ctx: Context, date: String): Int =
        getCbdCount(ctx, date) + getThcCount(ctx, date)

    /** Incrementa el contador del modo activo y devuelve el nuevo valor */
    fun incrementActiveCount(ctx: Context, date: String): Int {
        val isThc = getSubstanceType(ctx) == "THC"
        return if (isThc) {
            val newCount = getThcCount(ctx, date) + 1
            setThcCount(ctx, date, newCount)
            newCount
        } else {
            val newCount = getCbdCount(ctx, date) + 1
            setCbdCount(ctx, date, newCount)
            newCount
        }
    }

    /** Decrementa el contador del modo activo (mínimo 0) y devuelve el nuevo valor */
    fun decrementActiveCount(ctx: Context, date: String): Int {
        val isThc = getSubstanceType(ctx) == "THC"
        return if (isThc) {
            val newCount = maxOf(0, getThcCount(ctx, date) - 1)
            setThcCount(ctx, date, newCount)
            newCount
        } else {
            val newCount = maxOf(0, getCbdCount(ctx, date) - 1)
            setCbdCount(ctx, date, newCount)
            newCount
        }
    }

    /** Obtiene el conteo del modo activo */
    fun getActiveCount(ctx: Context, date: String): Int {
        val isThc = getSubstanceType(ctx) == "THC"
        return if (isThc) getThcCount(ctx, date) else getCbdCount(ctx, date)
    }

    // ---- Migración v1.4 ----
    fun migrateToV14IfNeeded(ctx: Context) {
        val prefs = prefs(ctx)
        if (prefs.getBoolean(KEY_MIGRATION_V14_DONE, false)) return

        val editor = prefs.edit()
        val allEntries = prefs.all

        // Migrar count_* → count_cbd_*
        allEntries.keys
            .filter { it.startsWith(KEY_COUNT_PREFIX) && !it.startsWith(KEY_COUNT_CBD_PREFIX) && !it.startsWith(KEY_COUNT_THC_PREFIX) }
            .forEach { oldKey ->
                val date = oldKey.removePrefix(KEY_COUNT_PREFIX)
                val count = allEntries[oldKey] as? Int ?: 0
                editor.putInt("${KEY_COUNT_CBD_PREFIX}$date", count)
                editor.remove(oldKey)
            }

        // Eliminar claves substance_* obsoletas
        allEntries.keys
            .filter { it.startsWith("substance_") && it != "substance_type" }
            .forEach { editor.remove(it) }

        editor.putBoolean(KEY_MIGRATION_V14_DONE, true)
        editor.apply()
    }

    // ---- Helpers para NOTAS DE VOZ ----
    fun getVoiceNotePath(context: Context, date: String): String {
        val dir = java.io.File(context.filesDir, "voice_notes").also { it.mkdirs() }
        // Las fechas usan formato "dd/MM/yyyy" — las barras "/" se interpretarían como
        // separadores de directorio en File(), creando subdirectorios inexistentes.
        // Se sanitizan a "dd-MM-yyyy" para que quede como un nombre de archivo plano.
        val safeName = date.replace("/", "-")
        return java.io.File(dir, "$safeName.m4a").absolutePath
    }

    fun hasVoiceNote(context: Context, date: String): Boolean {
        return java.io.File(getVoiceNotePath(context, date)).exists()
    }

    /** Obtiene todas las fechas que tienen datos (para historial) */
    fun getAllDatesWithData(ctx: Context): Set<String> {
        val prefs = prefs(ctx)
        val dates = mutableSetOf<String>()

        prefs.all.keys.forEach { key ->
            when {
                key.startsWith(KEY_COUNT_CBD_PREFIX) -> dates.add(key.removePrefix(KEY_COUNT_CBD_PREFIX))
                key.startsWith(KEY_COUNT_THC_PREFIX) -> dates.add(key.removePrefix(KEY_COUNT_THC_PREFIX))
                key.startsWith(KEY_NOTE_PREFIX) -> dates.add(key.removePrefix(KEY_NOTE_PREFIX))
            }
        }
        return dates
    }
}
