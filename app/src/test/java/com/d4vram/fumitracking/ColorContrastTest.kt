package com.d4vram.fumitracking

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.pow

/**
 * Lee los colors.xml reales (claro y oscuro) y comprueba que los roles de los anillos
 * se ven bien sobre el fondo en AMBOS temas (contraste WCAG para elementos gráficos: 3:1).
 * El directorio de trabajo de los tests de Gradle es app/.
 */
class ColorContrastTest {

    private val light = File("src/main/res/values/colors.xml")
    private val night = File("src/main/res/values-night/colors.xml")

    private val ringRoles = listOf("ring_cbd", "ring_thc", "ring_limit", "ring_over")
    private val allRoles = listOf("main_bg", "ring_track", "ring_on") + ringRoles

    private fun colors(file: File): Map<String, Int> =
        Regex("""<color name="(\w+)">#([0-9A-Fa-f]{6})</color>""")
            .findAll(file.readText())
            .associate { it.groupValues[1] to it.groupValues[2].toInt(16) }

    private fun luminance(rgb: Int): Double {
        fun ch(shift: Int): Double {
            val v = ((rgb shr shift) and 0xFF) / 255.0
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * ch(16) + 0.7152 * ch(8) + 0.0722 * ch(0)
    }

    private fun contrast(a: Int, b: Int): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    @Test
    fun `every ring role exists in both themes`() {
        for (file in listOf(light, night)) {
            val c = colors(file)
            for (role in allRoles) {
                assertTrue("falta $role en ${file.path}", c.containsKey(role))
            }
        }
    }

    @Test
    fun `ring colors reach 3 to 1 against background in both themes`() {
        for (file in listOf(light, night)) {
            val c = colors(file)
            for (role in ringRoles) {
                val ratio = contrast(c.getValue(role), c.getValue("main_bg"))
                assertTrue("$role en ${file.path} tiene contraste $ratio (< 3.0)", ratio >= 3.0)
            }
        }
    }

    @Test
    fun `track is visible but subtle`() {
        for (file in listOf(light, night)) {
            val c = colors(file)
            val ratio = contrast(c.getValue("ring_track"), c.getValue("main_bg"))
            assertTrue("ring_track en ${file.path} tiene contraste $ratio", ratio in 1.1..2.0)
        }
    }

    @Test
    fun `button text ring_on is readable on CBD and THC buttons in both themes`() {
        for (file in listOf(light, night)) {
            val c = colors(file)
            for (bg in listOf("ring_cbd", "ring_thc")) {
                val ratio = contrast(c.getValue("ring_on"), c.getValue(bg))
                assertTrue("ring_on sobre $bg en ${file.path} tiene contraste $ratio (< 4.5)", ratio >= 4.5)
            }
        }
    }

    private fun blend(fg: Int, bg: Int, alpha: Double): Int {
        fun ch(shift: Int) = ((((fg shr shift) and 0xFF) * alpha + ((bg shr shift) and 0xFF) * (1 - alpha))).toInt() shl shift
        return ch(16) or ch(8) or ch(0)
    }

    @Test
    fun `inactive counter number is still readable in both themes`() {
        val alpha = MainActivity.INACTIVE_COUNTER_ALPHA.toDouble()
        for (file in listOf(light, night)) {
            val c = colors(file)
            for (role in ringRoles) {
                val shown = blend(c.getValue(role), c.getValue("main_bg"), alpha)
                val ratio = contrast(shown, c.getValue("main_bg"))
                assertTrue("$role atenuado en ${file.path} tiene contraste $ratio (< 3.0)", ratio >= 3.0)
            }
        }
    }
}
