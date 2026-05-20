package com.github.itskenny0.r1ha.core.ha

import kotlin.math.min
import kotlin.random.Random

/** Exponential-backoff schedule with ±jitter. Pure function for easy testing. */
data class BackoffPolicy(
    val baseMillis: Long = 1_000,
    val capMillis: Long = 30_000,
    val jitter: Double = 0.25,
    val rng: Random = Random.Default,
) {
    fun delayForAttempt(attempt: Int): Long {
        // shl on Long overflows silently at attempt + log2(baseMillis) ≥ 63; with the
        // default baseMillis=1000 (~2^10) and the coerce(0,20) cap that's fine, but
        // larger baseMillis values would silently wrap to a negative delay. Coerce the
        // shift count down based on baseMillis so the result is always bounded.
        val safeShift = attempt.coerceIn(
            0,
            // 62 - leading-zero-count(baseMillis) is the largest shift that keeps the
            // result inside positive Long range. coerceAtLeast(0) covers the
            // baseMillis = 0 degenerate case.
            (62 - java.lang.Long.numberOfLeadingZeros(baseMillis.coerceAtLeast(1L))).coerceAtLeast(0),
        )
        val raw = baseMillis shl safeShift
        val capped = min(raw, capMillis)
        if (jitter == 0.0) return capped
        val window = (capped * jitter).toLong()
        val delta = if (window == 0L) 0L else rng.nextLong(-window, window + 1)
        return (capped + delta).coerceAtLeast(0)
    }
}
