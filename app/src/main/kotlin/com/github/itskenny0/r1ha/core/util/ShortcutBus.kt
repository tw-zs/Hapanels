package com.github.itskenny0.r1ha.core.util

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * One-way channel from [com.github.itskenny0.r1ha.MainActivity]'s
 * intent layer to the Compose nav graph. App-shortcut taps (and any
 * future deep-link source) emit the requested route here; the nav
 * graph collects and pushes it onto the back stack.
 *
 * Process-scoped singleton because the route hand-off has to survive
 * across:
 *   - Cold start (MainActivity.onCreate → setContent → AppNavGraph
 *     first compose tick) and
 *   - Warm start (MainActivity.onNewIntent → AppNavGraph already in
 *     composition).
 *
 * A conflated channel retains the latest route before the nav graph
 * subscribes and delivers each route to one collector exactly once.
 * If the user mashes shortcuts before delivery, the latest wins.
 */
object ShortcutBus {
    private val channel = Channel<String>(Channel.CONFLATED)
    val requests: Flow<String> = channel.receiveAsFlow()

    /** Push a route name onto the bus. Called from
     *  [com.github.itskenny0.r1ha.MainActivity] on app-shortcut
     *  intent delivery. */
    fun request(route: String) {
        channel.trySend(route)
    }
}
