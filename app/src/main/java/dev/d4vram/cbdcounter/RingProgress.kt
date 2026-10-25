package dev.d4vram.cbdcounter

/** Estado de un anillo respecto a su meta diaria (las metas son máximos, no objetivos). */
enum class RingState { NO_GOAL, UNDER, AT_LIMIT, OVER }

/**
 * Lógica pura de los anillos: sin Context ni Android, para poder testearla con JUnit.
 * Meta <= 0 significa "sin meta" y nunca se divide entre ella.
 */
object RingProgress {

    fun state(count: Int, goal: Int): RingState {
        if (goal <= 0) return RingState.NO_GOAL
        val c = count.coerceAtLeast(0)
        return when {
            c < goal -> RingState.UNDER
            c == goal -> RingState.AT_LIMIT
            else -> RingState.OVER
        }
    }

    /** Relleno de la primera vuelta, de 0f a 1f. */
    fun fraction(count: Int, goal: Int): Float {
        if (goal <= 0) return 0f
        return (count.coerceAtLeast(0).toFloat() / goal).coerceIn(0f, 1f)
    }

    /** Segunda vuelta fina cuando te pasas de la meta, de 0f a 1f (tope: una vuelta). */
    fun overflowFraction(count: Int, goal: Int): Float {
        if (state(count, goal) != RingState.OVER) return 0f
        return ((count - goal).toFloat() / goal).coerceIn(0f, 1f)
    }

    /** Cuántas tomas te pasas de la meta (0 si no te pasas). */
    fun overBy(count: Int, goal: Int): Int =
        if (state(count, goal) == RingState.OVER) count - goal else 0
}
