package com.d4vram.cbdcounter

import android.content.Context

object IntakeController {
    data class Snapshot(val cbdCount: Int, val thcCount: Int)

    enum class Substance { CBD, THC }

    /**
     * Determina la sustancia de una línea de toma por su icono inicial. Función pura
     * (sin Context) para poder testearla. Las infusiones (🌿 weed, 🍫 polen) y el THC
     * estándar (🟢) cuentan como THC; el CBD estándar es 🔹.
     */
    fun substanceFromEntryLine(line: String): Substance? {
        val t = line.trimStart()
        return when {
            t.startsWith("🔹") -> Substance.CBD
            t.startsWith("🟢") || t.startsWith("🌿") || t.startsWith("🍫") -> Substance.THC
            else -> null
        }
    }

    data class UndoResult(
        val cbdCount: Int,
        val thcCount: Int,
        val wasThc: Boolean,
        val noteRemoved: Boolean
    )

    /**
     * Deshace la ÚLTIMA toma del día (no la del modo activo): detecta CBD/THC por el
     * icono de la última línea de nota, decrementa ESE contador y, si [removeNote] es
     * true, borra esa línea. Así contador y notas quedan alineados. Devuelve null si
     * no hay nada que restar.
     */
    fun undoLast(context: Context, date: String, removeNote: Boolean): UndoResult? {
        val note = Prefs.getNote(context, date)
        val lastLine = note?.split("\n")?.lastOrNull { it.isNotBlank() }
        val substance = lastLine?.let { substanceFromEntryLine(it) }

        val isThc = when (substance) {
            Substance.THC -> true
            Substance.CBD -> false
            null -> {
                // Sin pista en la nota: resta del contador que tenga algo, prefiere el activo
                val thc = Prefs.getThcCount(context, date)
                val cbd = Prefs.getCbdCount(context, date)
                when {
                    cbd > 0 && thc == 0 -> false
                    thc > 0 && cbd == 0 -> true
                    else -> Prefs.getSubstanceType(context) == "THC"
                }
            }
        }

        val current = if (isThc) Prefs.getThcCount(context, date) else Prefs.getCbdCount(context, date)
        if (current <= 0) return null

        if (isThc) Prefs.setThcCount(context, date, current - 1)
        else Prefs.setCbdCount(context, date, current - 1)

        var removed = false
        if (removeNote && substance != null) {
            removeLastEntry(context, date)
            removed = true
        }

        return UndoResult(
            cbdCount = Prefs.getCbdCount(context, date),
            thcCount = Prefs.getThcCount(context, date),
            wasThc = isThc,
            noteRemoved = removed
        )
    }

    fun registerActive(context: Context, date: String, entry: String): Snapshot {
        Prefs.incrementActiveCount(context, date)
        appendEntry(context, date, entry)
        return snapshot(context, date)
    }

    fun registerThc(context: Context, date: String, entry: String): Snapshot {
        Prefs.setThcCount(context, date, Prefs.getThcCount(context, date) + 1)
        appendEntry(context, date, entry)
        return snapshot(context, date)
    }

    fun resetActive(context: Context, date: String): Snapshot {
        if (Prefs.getSubstanceType(context) == "THC") {
            Prefs.setThcCount(context, date, 0)
        } else {
            Prefs.setCbdCount(context, date, 0)
        }
        return snapshot(context, date)
    }

    fun removeLastEntry(context: Context, date: String) {
        val currentNote = Prefs.getNote(context, date)
        if (currentNote.isNullOrBlank()) return

        val lines = currentNote.split("\n").toMutableList()
        if (lines.isNotEmpty()) lines.removeAt(lines.lastIndex)

        Prefs.setNote(context, date, lines.takeIf { it.isNotEmpty() }?.joinToString("\n"))
    }

    private fun appendEntry(context: Context, date: String, entry: String) {
        val currentNote = Prefs.getNote(context, date)
        val updatedNote = if (currentNote.isNullOrBlank()) {
            entry
        } else {
            buildString {
                append(currentNote)
                if (!currentNote.endsWith("\n")) append("\n")
                append(entry)
            }
        }

        Prefs.setNote(context, date, updatedNote)
    }

    private fun snapshot(context: Context, date: String): Snapshot {
        return Snapshot(
            cbdCount = Prefs.getCbdCount(context, date),
            thcCount = Prefs.getThcCount(context, date)
        )
    }
}
