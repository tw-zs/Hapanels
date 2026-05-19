package com.github.itskenny0.r1ha.core.util

/**
 * Process-scoped one-shot for asking SettingsScreen to focus a specific section
 * on its next composition. Lets ModifiedSettingsScreen ("which values differ
 * from default?") send the user back to Settings with the relevant section
 * already expanded and scrolled to top, without making the diff panel itself
 * an editor (which would duplicate every section composable).
 *
 * Section names are the same uppercase strings SettingsScreen uses as keys for
 * its expand/collapse map (e.g. "SERVER", "CARD UI", "BEHAVIOUR").
 *
 * Why a @Volatile field instead of a SharedFlow: this is a strict one-shot. The
 * value is staged by the diff-row tap, then SettingsScreen's LaunchedEffect
 * reads-and-clears it on its first composition after the back-pop. If we used
 * a replay=0 SharedFlow we'd race the subscribe-vs-emit window across the nav
 * pop; a replay=1 flow would re-fire on every later Settings re-entry (the
 * user navigates away, then back, and the section silently re-expands). Read-
 * and-clear semantics avoid both failure modes and stay obvious to read.
 */
object SettingsFocusBus {
    /** Staged section name, or null if nothing is pending. SettingsScreen
     *  reads-and-clears this on its first composition after a back-pop. */
    @Volatile
    var pendingSection: String? = null
        private set

    /** Stage [sectionName] as the next-rendered SettingsScreen's focus
     *  target. Caller is expected to navigate back to Settings immediately;
     *  the consumer there clears the field as soon as it reads it, so a
     *  subsequent unrelated Settings re-entry doesn't re-fire the focus. */
    fun request(sectionName: String) {
        if (sectionName.isBlank()) return
        pendingSection = sectionName
    }

    /** Read-and-clear. Returns the staged section name (and nulls the field)
     *  or null if nothing was staged. Called from SettingsScreen's first
     *  composition. */
    fun consume(): String? {
        val value = pendingSection
        pendingSection = null
        return value
    }
}
