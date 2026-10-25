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
}
