package com.d4vram.cbdcounter

import android.content.Context

object IntakeController {
    data class Snapshot(val cbdCount: Int, val thcCount: Int)

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
