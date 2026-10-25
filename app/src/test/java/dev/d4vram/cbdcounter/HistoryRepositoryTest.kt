package dev.d4vram.cbdcounter

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryRepositoryTest {

    @Test
    fun `parse date accepts app date keys`() {
        assertNotNull(HistoryRepository.parseDate("02/05/2026"))
    }

    @Test
    fun `parse date rejects malformed keys`() {
        assertNull(HistoryRepository.parseDate("2026-05-02"))
    }
}
