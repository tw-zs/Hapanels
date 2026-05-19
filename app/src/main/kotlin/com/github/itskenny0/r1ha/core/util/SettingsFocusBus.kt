package com.github.itskenny0.r1ha.core.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Process-scoped channel for asking SettingsScreen to focus a specific section
 * on its next composition. Lets ModifiedSettingsScreen ("which values differ
 * from default?") send the user back to Settings with the relevant section
 * already expanded and scrolled to top — without making the diff panel itself
 * an editor (which would duplicate every section composable).
 *
 * Section names are the same uppercase strings SettingsScreen uses as keys for
 * its expand/collapse map (e.g. "SERVER", "CARD UI", "BEHAVIOUR").
 *
 * Shape mirrors [AssistDraftBus]: SharedFlow with capacity 1 + DROP_OLDEST.
 * A focus request pushed before SettingsScreen subscribes stays buffered for
 * the next subscriber; back-to-back pushes don't queue stale targets.
 */
object SettingsFocusBus {
    private val _requests = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val requests: SharedFlow<String> = _requests.asSharedFlow()

    /** Stage [sectionName] as the next-rendered SettingsScreen's focus target.
     *  Caller is expected to navigate back to Settings immediately; the
     *  collector there fires on first composition. */
    fun request(sectionName: String) {
        if (sectionName.isBlank()) return
        _requests.tryEmit(sectionName)
    }
}
