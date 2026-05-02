package com.d4vram.cbdcounter

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Locale

object HistoryRepository {
    fun loadAll(context: Context): List<HistoryItem> {
        return Prefs.getAllDatesWithData(context)
            .mapNotNull { dateString ->
                parseDate(dateString)?.let { date ->
                    HistoryItem(
                        date = dateString,
                        cbdCount = Prefs.getCbdCount(context, dateString),
                        thcCount = Prefs.getThcCount(context, dateString),
                        dateObject = date
                    )
                }
            }
            .sortedByDescending { it.dateObject }
    }

    internal fun parseDate(dateString: String) = runCatching {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            isLenient = false
        }.parse(dateString)
    }.getOrNull()
}
