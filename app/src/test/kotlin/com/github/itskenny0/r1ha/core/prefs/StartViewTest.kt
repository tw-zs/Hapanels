package com.github.itskenny0.r1ha.core.prefs

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StartViewTest {
    @Test fun persistedEnumWinsAndLegacyBooleanMigrates() {
        assertThat(persistedStartView(StartView.CARDS.name, true)).isEqualTo(StartView.CARDS)
        assertThat(persistedStartView(null, true)).isEqualTo(StartView.PANEL_GRID)
        assertThat(persistedStartView(null, false)).isEqualTo(StartView.CARDS)
        assertThat(persistedStartView(null, null)).isEqualTo(StartView.PANEL_GRID)
        assertThat(persistedStartView("STALE", null)).isEqualTo(StartView.PANEL_GRID)
    }
}
