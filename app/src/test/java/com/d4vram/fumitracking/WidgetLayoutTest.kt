package com.d4vram.fumitracking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetLayoutTest {

    @Test
    fun `ancho 0 o ausente es compacto`() {
        assertFalse(WidgetLayout.isWide(0))
    }

    @Test
    fun `justo bajo el umbral es compacto`() {
        assertFalse(WidgetLayout.isWide(249))
    }

    @Test
    fun `en el umbral es ancho`() {
        assertTrue(WidgetLayout.isWide(250))
    }

    @Test
    fun `ancho muy grande es ancho`() {
        assertTrue(WidgetLayout.isWide(600))
    }
}
