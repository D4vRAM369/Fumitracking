package com.d4vram.fumitracking

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

    // ponytail: sin meta, cada toma ocupa 1/10 de vuelta; ajustar si alguien suele pasar de 20 al día.
    private const val NO_GOAL_SLOTS = 10

    /**
     * Tipo de cada toma en orden de hora, leído del icono inicial de cada línea de la nota.
     * 🟢 (THC estándar) se agrupa con weed; las líneas sin icono de toma se ignoran.
     */
    fun kindsFromNote(note: String?): List<IntakeKind> =
        note.orEmpty().lines().mapNotNull { line ->
            val t = line.trimStart()
            when {
                t.startsWith(IntakeController.ICON_CBD) -> IntakeKind.CBD
                t.startsWith(IntakeController.ICON_POLEN) -> IntakeKind.POLEN
                t.startsWith(IntakeController.ICON_WEED) || t.startsWith(IntakeController.ICON_THC) -> IntakeKind.WEED
                else -> null
            }
        }

    /**
     * Un segmento por toma. Manda el contador ([count]); [kinds] solo pone el color.
     * Si la nota tiene menos líneas que tomas, las que faltan usan [default].
     * Máximo dos vueltas: la segunda (overflow) es la de "te pasaste".
     */
    fun segments(kinds: List<IntakeKind>, count: Int, goal: Int, default: IntakeKind): List<RingSegment> {
        val perLap = if (goal > 0) goal else NO_GOAL_SLOTS
        val slot = 1f / perLap
        val total = count.coerceIn(0, perLap * 2)
        return List(total) { i ->
            RingSegment(
                kind = kinds.getOrElse(i) { default },
                start = (i % perLap) * slot,
                sweep = slot,
                overflow = i >= perLap
            )
        }
    }

    /**
     * Cuántas tomas de cada tipo hubo en un día. Los contadores mandan: de la nota solo
     * se usan las primeras [thcCount] tomas de THC para saber cuántas fueron polen;
     * el resto de THC (sin línea en la nota) cuenta como weed.
     */
    fun dayMix(cbdCount: Int, thcCount: Int, kinds: List<IntakeKind>): DayMix {
        val thc = thcCount.coerceAtLeast(0)
        val polen = kinds.filter { it != IntakeKind.CBD }.take(thc).count { it == IntakeKind.POLEN }
        return DayMix(cbd = cbdCount.coerceAtLeast(0), weed = thc - polen, polen = polen)
    }
}

/** Reparto de un día por tipo de toma. */
data class DayMix(val cbd: Int, val weed: Int, val polen: Int) {
    val total: Int get() = cbd + weed + polen
}

/** Qué se tomó: decide el color del segmento. */
enum class IntakeKind { CBD, WEED, POLEN }

/** [start] y [sweep] son fracciones de vuelta (0..1) dentro de su propia vuelta. */
data class RingSegment(val kind: IntakeKind, val start: Float, val sweep: Float, val overflow: Boolean)
