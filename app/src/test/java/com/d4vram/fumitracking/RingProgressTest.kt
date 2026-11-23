package com.d4vram.fumitracking

import org.junit.Assert.assertEquals
import org.junit.Test

class RingProgressTest {

    private val delta = 0.0001f

    @Test
    fun `goal 0 is NO_GOAL with zero fraction`() {
        assertEquals(RingState.NO_GOAL, RingProgress.state(3, 0))
        assertEquals(0f, RingProgress.fraction(3, 0), delta)
        assertEquals(0f, RingProgress.overflowFraction(3, 0), delta)
        assertEquals(0, RingProgress.overBy(3, 0))
    }

    @Test
    fun `negative goal is NO_GOAL`() {
        assertEquals(RingState.NO_GOAL, RingProgress.state(2, -1))
    }

    @Test
    fun `under goal`() {
        assertEquals(RingState.UNDER, RingProgress.state(3, 5))
        assertEquals(0.6f, RingProgress.fraction(3, 5), delta)
    }

    @Test
    fun `zero count with goal is UNDER with zero fraction`() {
        assertEquals(RingState.UNDER, RingProgress.state(0, 5))
        assertEquals(0f, RingProgress.fraction(0, 5), delta)
    }

    @Test
    fun `count equals goal is AT_LIMIT`() {
        assertEquals(RingState.AT_LIMIT, RingProgress.state(5, 5))
        assertEquals(1f, RingProgress.fraction(5, 5), delta)
        assertEquals(0, RingProgress.overBy(5, 5))
    }

    @Test
    fun `over goal`() {
        assertEquals(RingState.OVER, RingProgress.state(4, 3))
        assertEquals(1f, RingProgress.fraction(4, 3), delta)
        assertEquals(1, RingProgress.overBy(4, 3))
        assertEquals(1 / 3f, RingProgress.overflowFraction(4, 3), delta)
    }

    @Test
    fun `huge overflow is clamped to one lap`() {
        assertEquals(1f, RingProgress.overflowFraction(50, 3), delta)
        assertEquals(47, RingProgress.overBy(50, 3))
    }

    @Test
    fun `negative count is treated as zero`() {
        assertEquals(RingState.UNDER, RingProgress.state(-1, 5))
        assertEquals(0f, RingProgress.fraction(-1, 5), delta)
    }

    // --- Anillo fragmentado: un segmento por toma, coloreado por tipo ---

    @Test
    fun `kindsFromNote maps icons in order`() {
        val note = "🔹 10:00\n🌿 11:00 (aliñado con weed)\n🍫 12:00\n🟢 13:00"
        assertEquals(
            listOf(IntakeKind.CBD, IntakeKind.WEED, IntakeKind.POLEN, IntakeKind.WEED),
            RingProgress.kindsFromNote(note)
        )
    }

    @Test
    fun `kindsFromNote ignores non-intake lines`() {
        assertEquals(listOf(IntakeKind.CBD), RingProgress.kindsFromNote("hola\n\n🔹 10:00"))
        assertEquals(emptyList<IntakeKind>(), RingProgress.kindsFromNote(null))
    }

    @Test
    fun `segments one per intake with goal`() {
        val s = RingProgress.segments(listOf(IntakeKind.WEED, IntakeKind.POLEN), 2, 4, IntakeKind.WEED)
        assertEquals(2, s.size)
        assertEquals(IntakeKind.WEED, s[0].kind)
        assertEquals(0f, s[0].start, delta)
        assertEquals(0.25f, s[0].sweep, delta)
        assertEquals(false, s[0].overflow)
        assertEquals(IntakeKind.POLEN, s[1].kind)
        assertEquals(0.25f, s[1].start, delta)
        assertEquals(0.25f, s[1].sweep, delta)
    }

    @Test
    fun `segments fills missing kinds with default`() {
        val s = RingProgress.segments(listOf(IntakeKind.POLEN), 3, 4, IntakeKind.WEED)
        assertEquals(listOf(IntakeKind.POLEN, IntakeKind.WEED, IntakeKind.WEED), s.map { it.kind })
    }

    @Test
    fun `segments empty when count is zero`() {
        assertEquals(0, RingProgress.segments(listOf(IntakeKind.CBD), 0, 5, IntakeKind.CBD).size)
    }

    @Test
    fun `segments overflow goes to second lap`() {
        val s = RingProgress.segments(emptyList(), 5, 4, IntakeKind.CBD)
        assertEquals(5, s.size)
        assertEquals(false, s[3].overflow)
        assertEquals(true, s[4].overflow)
        assertEquals(0f, s[4].start, delta)
    }

    @Test
    fun `segments without goal use tenth of lap`() {
        val s = RingProgress.segments(emptyList(), 2, 0, IntakeKind.CBD)
        assertEquals(0.1f, s[0].sweep, delta)
        assertEquals(0.1f, s[1].start, delta)
    }

    @Test
    fun `segments cap at two laps`() {
        assertEquals(20, RingProgress.segments(emptyList(), 25, 0, IntakeKind.CBD).size)
    }
}
