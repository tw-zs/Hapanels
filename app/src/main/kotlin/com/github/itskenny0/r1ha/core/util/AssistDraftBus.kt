package com.github.itskenny0.r1ha.core.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Process-scoped channel for handing a pre-filled draft prompt to the Assist screen
 * from elsewhere in the app. Designed for the case where the user's intent is
 * already captured as a string in another surface, and routing them to Assist
 * with that string pre-typed saves them from re-entering it.
 *
 * Current call sites:
 *   - [com.github.itskenny0.r1ha.feature.search.SearchScreen] empty-state →
 *     "Ask Assist about '<query>'" CTA. The Search query goes here; AssistScreen
 *     picks it up on first composition and seeds its draft field.
 *
 * Shape rationale mirrors [ShortcutBus]: SharedFlow with `extraBufferCapacity = 1`
 * and DROP_OLDEST so a cold start before AssistScreen subscribes keeps the single
 * pending draft buffered, and rapid double-pushes don't queue stale prompts.
 *
 * Deliberately NOT a replay buffer: AssistScreen's collector re-subscribes on
 * recomposition and a replay would re-seed the draft on every recomposition,
 * which would clobber what the user types after the first seed.
 */
object AssistDraftBus {
    private val _drafts = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val drafts: SharedFlow<String> = _drafts.asSharedFlow()

    /** Stage [text] as the next Assist screen's pre-filled draft. The caller is
     *  expected to navigate to Routes.ASSIST immediately after; AssistScreen's
     *  collector picks it up on first composition. */
    fun push(text: String) {
        if (text.isBlank()) return
        _drafts.tryEmit(text)
    }
}
