package com.github.itskenny0.r1ha.core.prefs

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppBackupStartViewTest {
    @Test fun backupRoundTripPreservesStartView() {
        val raw = encodeBackup(AppSettings(behavior = Behavior(startView = StartView.CARDS)).toBackup("now"))

        assertThat(decodeBackup(raw).applyOnto(AppSettings()).behavior.startView).isEqualTo(StartView.CARDS)
        assertThat(decodeBackup(raw).behaviorStartOnDashboard).isFalse()
    }

    @Test fun legacyBooleanBackupMigratesBothValues() {
        val oldTrue = """{"createdAt":"then","behaviorStartOnDashboard":true}"""
        val oldFalse = """{"createdAt":"then","behaviorStartOnDashboard":false}"""

        assertThat(decodeBackup(oldTrue).applyOnto(AppSettings()).behavior.startView).isEqualTo(StartView.PANEL_GRID)
        assertThat(decodeBackup(oldFalse).applyOnto(AppSettings()).behavior.startView).isEqualTo(StartView.CARDS)
    }

    @Test fun enumWinsOverConflictingLegacyBoolean() {
        val raw = """{"createdAt":"then","behaviorStartView":"CARDS","behaviorStartOnDashboard":true}"""

        assertThat(decodeBackup(raw).applyOnto(AppSettings()).behavior.startView).isEqualTo(StartView.CARDS)
    }

    @Test fun backupWithoutEitherStartSettingUsesPanelGrid() {
        val raw = """{"createdAt":"then","unknownFutureField":true}"""

        assertThat(decodeBackup(raw).applyOnto(AppSettings()).behavior.startView)
            .isEqualTo(StartView.PANEL_GRID)
    }
}
