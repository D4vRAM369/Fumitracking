package com.d4vram.fumitracking

/** Elige qué layout del widget usar. Lógica pura, sin Android, para poder testearla con JUnit. */
object WidgetLayout {
    // 4 celdas de launcher miden ~250 dp o más; 2 celdas, ~110–180 dp.
    const val WIDE_MIN_WIDTH_DP = 250

    fun isWide(widthDp: Int): Boolean = widthDp >= WIDE_MIN_WIDTH_DP
}
