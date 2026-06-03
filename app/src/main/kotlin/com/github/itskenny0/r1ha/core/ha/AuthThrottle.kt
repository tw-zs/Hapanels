package com.github.itskenny0.r1ha.core.ha

/**
 * Rolling-window circuit breaker for REST auth failures. When HA returns 401s in a
 * burst, the breaker opens and later API calls fail fast locally instead of
 * generating more failed-login requests that can trigger Home Assistant IP bans.
 */
class AuthThrottle(
    private val windowMillis: Long = 60_000L,
    private val failureThreshold: Int = 1,
    private val baseBackoffMillis: Long = 15_000L,
    private val maxBackoffMillis: Long = 900_000L,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private enum class State { CLOSED, OPEN, HALF_OPEN }

    private val lock = Any()
    private val failures = ArrayDeque<Long>()
    private var state = State.CLOSED
    private var openUntil = 0L
    private var consecutiveOpens = 0

    fun shouldShortCircuit(): Boolean = synchronized(lock) {
        when (state) {
            State.CLOSED -> false
            State.HALF_OPEN -> true
            State.OPEN -> {
                if (clock() >= openUntil) {
                    state = State.HALF_OPEN
                    false
                } else {
                    true
                }
            }
        }
    }

    fun isOpenNow(): Boolean = synchronized(lock) {
        state == State.OPEN && clock() < openUntil
    }

    fun recordAuthFailure() = synchronized(lock) {
        if (state == State.HALF_OPEN) {
            reopen()
            return@synchronized
        }
        val now = clock()
        failures.addLast(now)
        while (failures.isNotEmpty() && now - failures.first() > windowMillis) {
            failures.removeFirst()
        }
        if (state == State.CLOSED && failures.size >= failureThreshold.coerceAtLeast(1)) {
            reopen()
        }
    }

    fun recordSuccess() = synchronized(lock) {
        state = State.CLOSED
        failures.clear()
        consecutiveOpens = 0
        openUntil = 0L
    }

    fun reset() = recordSuccess()

    private fun reopen() {
        val shift = consecutiveOpens.coerceAtMost(20)
        val grown = baseBackoffMillis shl shift
        val backoff = if (grown <= 0L) maxBackoffMillis else grown.coerceAtMost(maxBackoffMillis)
        state = State.OPEN
        openUntil = clock() + backoff
        failures.clear()
        consecutiveOpens++
    }
}
