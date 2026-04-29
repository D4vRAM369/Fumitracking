package com.d4vram.cbdcounter

object HeatmapUtils {

    fun colorForCount(count: Int): Int = when {
        count == 0  -> 0xFF1a472a.toInt()  // verde oscuro
        count <= 2  -> 0xFF2d6a4f.toInt()  // verde
        count <= 4  -> 0xFF52b788.toInt()  // verde claro
        count == 5  -> 0xFFf4a261.toInt()  // naranja
        count <= 7  -> 0xFFe76f51.toInt()  // naranja-rojo
        count <= 10 -> 0xFFc1121f.toInt()  // rojo
        else        -> 0xFF6d1a36.toInt()  // rojo oscuro
    }
}
