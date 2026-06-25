package com.d4vram.cbdcounter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifica el parser de sustancia del Bug 1: el botón "-1" debe deshacer la última
 * toma real (CBD vs THC) según el icono de la nota, no el modo activo.
 */
class IntakeControllerTest {

    @Test
    fun cbd_estandar_es_CBD() {
        assertEquals(IntakeController.Substance.CBD, IntakeController.substanceFromEntryLine("🔹 12:30"))
    }

    @Test
    fun thc_estandar_es_THC() {
        assertEquals(IntakeController.Substance.THC, IntakeController.substanceFromEntryLine("🟢 12:30"))
    }

    @Test
    fun infusion_weed_es_THC() {
        assertEquals(IntakeController.Substance.THC, IntakeController.substanceFromEntryLine("🌿 23:50 (aliñado con weed)"))
    }

    @Test
    fun infusion_polen_es_THC() {
        assertEquals(IntakeController.Substance.THC, IntakeController.substanceFromEntryLine("🍫 23:50 (aliñado con polen)"))
    }

    @Test
    fun linea_sin_icono_es_null() {
        assertNull(IntakeController.substanceFromEntryLine("nota cualquiera sin icono"))
    }
}
