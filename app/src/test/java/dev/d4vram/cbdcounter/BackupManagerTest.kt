package dev.d4vram.cbdcounter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.nio.file.Files

class BackupManagerTest {

    @Test
    fun `safe zip target allows files inside restore directory`() {
        val tempDir = Files.createTempDirectory("restore-safe").toFile()

        try {
            val target = BackupManager.safeZipTarget(tempDir, "audios/note.m4a")

            assertEquals(File(tempDir, "audios/note.m4a").canonicalFile, target)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `safe zip target rejects parent traversal`() {
        val tempDir = Files.createTempDirectory("restore-safe").toFile()

        try {
            assertNull(BackupManager.safeZipTarget(tempDir, "../evil.txt"))
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
