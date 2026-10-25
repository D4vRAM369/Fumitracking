package com.d4vram.fumitracking

import android.content.Context

object IntakeController {
    data class Snapshot(val cbdCount: Int, val thcCount: Int)

    enum class Substance { CBD, THC }

    // Iconos de las líneas de toma. Fuente única para writer (MainActivity, widget) y
    // parser (substanceFromEntryLine), para que no deriven y rompan undoLast.
    const val ICON_CBD = "🔹"
    const val ICON_THC = "🟢"
    const val ICON_WEED = "🌿"
    const val ICON_POLEN = "🍫"

    /**
     * Determina la sustancia de una línea de toma por su icono inicial. Función pura
     * (sin Context) para poder testearla. Las infusiones (weed, polen) y el THC
     * estándar cuentan como THC; el CBD estándar es el único CBD.
     */
    fun substanceFromEntryLine(line: String): Substance? {
        val t = line.trimStart()
        return when {
            t.startsWith(ICON_CBD) -> Substance.CBD
            t.startsWith(ICON_THC) || t.startsWith(ICON_WEED) || t.startsWith(ICON_POLEN) -> Substance.THC
            else -> null
        }
    }

    /**
     * Decide de qué contador restar al deshacer. Pura y testeable (sin Context):
     * si el icono de la nota lo dice, gana; si no, resta del único contador con datos,
     * y en empate del modo activo. [detected] es lo que dio el parser sobre la última línea.
     */
    fun resolveUndoSubstance(
        detected: Substance?,
        cbdCount: Int,
        thcCount: Int,
        activeIsThc: Boolean
    ): Substance = when (detected) {
        Substance.THC -> Substance.THC
        Substance.CBD -> Substance.CBD
        null -> when {
            cbdCount > 0 && thcCount == 0 -> Substance.CBD
            thcCount > 0 && cbdCount == 0 -> Substance.THC
            else -> if (activeIsThc) Substance.THC else Substance.CBD
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
     * true, borra esa misma línea. Así contador y notas quedan alineados. Devuelve null
     * si no hay nada que restar.
     */
    fun undoLast(context: Context, date: String, removeNote: Boolean): UndoResult? {
        val note = Prefs.getNote(context, date)
        val lastLine = note?.split("\n")?.lastOrNull { it.isNotBlank() }
        val detected = lastLine?.let { substanceFromEntryLine(it) }

        val substance = resolveUndoSubstance(
            detected,
            Prefs.getCbdCount(context, date),
            Prefs.getThcCount(context, date),
            Prefs.getSubstanceType(context) == "THC"
        )
        val isThc = substance == Substance.THC

        val current = if (isThc) Prefs.getThcCount(context, date) else Prefs.getCbdCount(context, date)
        if (current <= 0) return null

        if (isThc) Prefs.setThcCount(context, date, current - 1)
        else Prefs.setCbdCount(context, date, current - 1)

        var removed = false
        if (removeNote && detected != null) {
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
        // Borrar la última línea NO vacía: la misma que detecta undoLast. Saltar vacías
        // evita desincronizar contador y nota cuando la nota termina en '\n'.
        val idx = lines.indexOfLast { it.isNotBlank() }
        if (idx < 0) return
        lines.removeAt(idx)

        Prefs.setNote(context, date, lines.takeIf { list -> list.any { it.isNotBlank() } }?.joinToString("\n"))
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
