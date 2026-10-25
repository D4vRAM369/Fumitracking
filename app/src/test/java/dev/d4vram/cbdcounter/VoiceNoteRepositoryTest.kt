package dev.d4vram.cbdcounter

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceNoteRepositoryTest {

    @Test
    fun `safe file name removes date slashes`() {
        assertEquals("02-05-2026", VoiceNoteRepository.safeFileName("02/05/2026"))
    }
}
