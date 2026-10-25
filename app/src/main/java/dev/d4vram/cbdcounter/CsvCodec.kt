package dev.d4vram.cbdcounter

object CsvCodec {
    const val HEADER = "date,count_cbd,count_thc,note"

    fun appendRow(
        builder: StringBuilder,
        date: String,
        cbdCount: Int,
        thcCount: Int,
        note: String
    ) {
        builder.append(escapeField(date))
            .append(',')
            .append(cbdCount)
            .append(',')
            .append(thcCount)
            .append(',')
            .append(escapeField(note))
            .append('\n')
    }

    fun escapeField(value: String): String {
        if (value.isEmpty()) return ""

        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    fun parse(text: String): List<List<String>> {
        if (text.isEmpty()) return emptyList()

        val records = mutableListOf<List<String>>()
        val currentRecord = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var fieldStartedWithQuote = false
        var index = 0

        fun endField() {
            currentRecord.add(currentField.toString())
            currentField.setLength(0)
            fieldStartedWithQuote = false
        }

        fun endRecord() {
            endField()
            records.add(currentRecord.toList())
            currentRecord.clear()
        }

        while (index < text.length) {
            val char = text[index]
            when {
                inQuotes && char == '"' && text.getOrNull(index + 1) == '"' -> {
                    currentField.append('"')
                    index++
                }
                char == '"' && !inQuotes && currentField.isEmpty() -> {
                    inQuotes = true
                    fieldStartedWithQuote = true
                }
                char == '"' && inQuotes -> inQuotes = false
                char == ',' && !inQuotes -> endField()
                (char == '\n' || char == '\r') && !inQuotes -> {
                    if (char == '\r' && text.getOrNull(index + 1) == '\n') index++
                    endRecord()
                }
                else -> {
                    if (!fieldStartedWithQuote || inQuotes || char != '"') {
                        currentField.append(char)
                    }
                }
            }
            index++
        }

        if (currentField.isNotEmpty() || currentRecord.isNotEmpty()) {
            endRecord()
        }

        return records
    }

    fun parseLegacyLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var escape = false

        line.forEach { char ->
            when {
                escape -> {
                    current.append(
                        when (char) {
                            'n' -> '\n'
                            '\\' -> '\\'
                            ',' -> ','
                            else -> char
                        }
                    )
                    escape = false
                }
                char == '\\' -> escape = true
                char == ',' -> {
                    fields.add(current.toString())
                    current.setLength(0)
                }
                else -> current.append(char)
            }
        }
        if (escape) current.append('\\')
        fields.add(current.toString())
        return fields
    }
}
