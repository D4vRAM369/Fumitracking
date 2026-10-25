package com.d4vram.fumitracking

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvCodecTest {

    @Test
    fun `round trip preserves commas quotes and line breaks`() {
        val note = "hola, \"mundo\"\nlinea 2"
        val builder = StringBuilder("${CsvCodec.HEADER}\n")

        CsvCodec.appendRow(builder, "02/05/2026", 3, 1, note)

        val records = CsvCodec.parse(builder.toString())

        assertEquals(listOf("date", "count_cbd", "count_thc", "note"), records[0])
        assertEquals(listOf("02/05/2026", "3", "1", note), records[1])
    }

    @Test
    fun `empty note remains empty`() {
        val builder = StringBuilder("${CsvCodec.HEADER}\n")

        CsvCodec.appendRow(builder, "02/05/2026", 0, 0, "")

        assertEquals(listOf("02/05/2026", "0", "0", ""), CsvCodec.parse(builder.toString())[1])
    }

    @Test
    fun `legacy parser supports backslash escaped commas and newlines`() {
        val row = CsvCodec.parseLegacyLine("02/05/2026,4,nota\\, con coma\\ny salto,THC")

        assertEquals(listOf("02/05/2026", "4", "nota, con coma\ny salto", "THC"), row)
    }
}
