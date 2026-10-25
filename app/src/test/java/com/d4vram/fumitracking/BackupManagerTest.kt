package com.d4vram.fumitracking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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

    private fun encrypt(data: ByteArray, pass: String): ByteArray {
        val out = ByteArrayOutputStream()
        BackupManager.encryptingStream(out, pass.toCharArray()).use { it.write(data) }
        return out.toByteArray()
    }

    @Test
    fun `cifrar y descifrar devuelve los mismos bytes`() {
        val data = ByteArray(100_000) { (it % 251).toByte() }
        val enc = encrypt(data, "clave-secreta")

        assertTrue(BackupManager.hasMagic(ByteArrayInputStream(enc)))
        val back = BackupManager.decryptingStream(ByteArrayInputStream(enc), "clave-secreta".toCharArray())
            .readBytes()
        assertArrayEquals(data, back)
    }

    @Test
    fun `contrasena incorrecta falla al leer`() {
        val enc = encrypt("hola".toByteArray(), "buena")
        try {
            BackupManager.decryptingStream(ByteArrayInputStream(enc), "mala".toCharArray()).readBytes()
            fail("debía lanzar excepción")
        } catch (_: java.io.IOException) {
        }
    }

    @Test
    fun `un zip normal no se detecta como cifrado`() {
        assertFalse(BackupManager.hasMagic(ByteArrayInputStream("PK\u0003\u0004zipzipzip".toByteArray())))
    }
}
