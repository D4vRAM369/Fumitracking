package com.d4vram.cbdcounter

import org.junit.Assert.assertEquals
import org.junit.Test

class HeatmapUtilsTest {

    @Test
    fun `count 0 returns darkest green`() {
        assertEquals(0xFF1a472a.toInt(), HeatmapUtils.colorForCount(0))
    }

    @Test
    fun `count 2 returns medium green`() {
        assertEquals(0xFF2d6a4f.toInt(), HeatmapUtils.colorForCount(2))
    }

    @Test
    fun `count 5 returns orange`() {
        assertEquals(0xFFf4a261.toInt(), HeatmapUtils.colorForCount(5))
    }

    @Test
    fun `count 11 plus returns darkest red`() {
        assertEquals(0xFF6d1a36.toInt(), HeatmapUtils.colorForCount(11))
        assertEquals(0xFF6d1a36.toInt(), HeatmapUtils.colorForCount(20))
    }
}
