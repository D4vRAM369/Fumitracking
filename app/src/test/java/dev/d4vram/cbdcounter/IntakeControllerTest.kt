package dev.d4vram.cbdcounter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Cubre las dos funciones puras del botón "-1": el parser de icono y la decisión de
 * qué contador restar. La parte con Context (undoLast/removeLastEntry) se verifica a mano
 * (no hay Robolectric); aquí blindamos la lógica donde realmente vivían los bugs.
 */
class IntakeControllerTest {

    // ---- substanceFromEntryLine ----

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

    @Test
    fun parser_usa_las_constantes_compartidas() {
        // El writer (MainActivity/widget) y el parser comparten estas constantes (FM3).
        assertEquals(IntakeController.Substance.CBD, IntakeController.substanceFromEntryLine("${IntakeController.ICON_CBD} x"))
        assertEquals(IntakeController.Substance.THC, IntakeController.substanceFromEntryLine("${IntakeController.ICON_THC} x"))
    }

    // ---- resolveUndoSubstance ----

    @Test
    fun resolver_respeta_el_icono_detectado() {
        // Aunque el modo activo sea CBD, si la última toma fue THC se resta THC.
        assertEquals(
            IntakeController.Substance.THC,
            IntakeController.resolveUndoSubstance(IntakeController.Substance.THC, cbdCount = 5, thcCount = 1, activeIsThc = false)
        )
    }

    @Test
    fun resolver_sin_icono_resta_del_unico_con_datos() {
        assertEquals(
            IntakeController.Substance.CBD,
            IntakeController.resolveUndoSubstance(null, cbdCount = 3, thcCount = 0, activeIsThc = true)
        )
        assertEquals(
            IntakeController.Substance.THC,
            IntakeController.resolveUndoSubstance(null, cbdCount = 0, thcCount = 2, activeIsThc = false)
        )
    }

    @Test
    fun resolver_sin_icono_y_ambos_con_datos_usa_el_modo_activo() {
        assertEquals(
            IntakeController.Substance.THC,
            IntakeController.resolveUndoSubstance(null, cbdCount = 2, thcCount = 2, activeIsThc = true)
        )
        assertEquals(
            IntakeController.Substance.CBD,
            IntakeController.resolveUndoSubstance(null, cbdCount = 2, thcCount = 2, activeIsThc = false)
        )
    }
}
