package com.github.itskenny0.r1ha.core.hardware

import com.github.itskenny0.r1ha.core.prefs.HardwareButtonActionKind
import com.github.itskenny0.r1ha.core.prefs.HardwareButtonActionMapping
import com.github.itskenny0.r1ha.core.prefs.HardwareButtonTriggerPhase
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PanelButtonActionDefaultsTest {
    @Test fun defaultShortPressButtonOneTogglesRelayOne() {
        assertThat(
            PanelButtonActionDefaults.actionFor(
                buttonId = 1,
                pressType = PanelButtonPressType.SHORT,
                relayCount = 1,
            ),
        ).isEqualTo(PanelButtonAction.ToggleRelay(1))
    }

    @Test fun configuredActionOverridesDefault() {
        val mappings = listOf(
            HardwareButtonActionMapping(
                buttonId = 1,
                action = HardwareButtonActionKind.RELAY_OFF,
                relayId = 1,
            ),
        )

        assertThat(
            PanelButtonActionDefaults.actionFor(
                buttonId = 1,
                pressType = PanelButtonPressType.SHORT,
                relayCount = 1,
                mappings = mappings,
            ),
        ).isEqualTo(PanelButtonAction.SetRelay(1, false))
    }

    @Test fun configuredActionIsIgnoredWhenRelayDoesNotExist() {
        val mappings = listOf(
            HardwareButtonActionMapping(
                buttonId = 2,
                action = HardwareButtonActionKind.TOGGLE_RELAY,
                relayId = 1,
            ),
        )

        assertThat(
            PanelButtonActionDefaults.actionFor(
                buttonId = 2,
                pressType = PanelButtonPressType.SHORT,
                relayCount = 0,
                mappings = mappings,
            ),
        ).isEqualTo(PanelButtonAction.None)
    }

    @Test fun shortRelayActionCanEmitImmediatelyWhenNoOtherPressMappingExists() {
        val mappings = listOf(
            HardwareButtonActionMapping(
                buttonId = 2,
                action = HardwareButtonActionKind.TOGGLE_RELAY,
                relayId = 1,
            ),
        )

        assertThat(
            PanelButtonActionDefaults.shouldEmitShortImmediately(
                buttonId = 2,
                relayCount = 1,
                mappings = mappings,
            ),
        ).isTrue()
    }

    @Test fun shortRelayActionWaitsWhenSameButtonHasNonShortMapping() {
        val mappings = listOf(
            HardwareButtonActionMapping(
                buttonId = 2,
                pressType = PanelButtonPressType.SHORT.name,
                action = HardwareButtonActionKind.TOGGLE_RELAY,
                relayId = 1,
            ),
            HardwareButtonActionMapping(
                buttonId = 2,
                pressType = PanelButtonPressType.DOUBLE.name,
                action = HardwareButtonActionKind.RELAY_ON,
                relayId = 1,
            ),
        )

        assertThat(
            PanelButtonActionDefaults.shouldEmitShortImmediately(
                buttonId = 2,
                relayCount = 1,
                mappings = mappings,
            ),
        ).isFalse()
    }

    @Test fun longPressMappingDoesNotDelayShortClick() {
        val mappings = listOf(
            HardwareButtonActionMapping(
                buttonId = 2,
                pressType = PanelButtonPressType.SHORT.name,
                action = HardwareButtonActionKind.TOGGLE_RELAY,
                relayId = 1,
            ),
            HardwareButtonActionMapping(
                buttonId = 2,
                pressType = PanelButtonPressType.LONG.name,
                action = HardwareButtonActionKind.RELAY_ON,
                relayId = 1,
            ),
        )

        assertThat(
            PanelButtonActionDefaults.shouldEmitShortImmediately(
                buttonId = 2,
                relayCount = 1,
                mappings = mappings,
            ),
        ).isTrue()
    }

    @Test fun pressAndReleaseActionsResolveByTriggerPhase() {
        val mappings = listOf(
            HardwareButtonActionMapping(
                buttonId = 1,
                triggerPhase = HardwareButtonTriggerPhase.DOWN,
                action = HardwareButtonActionKind.RELAY_ON,
                relayId = 1,
            ),
            HardwareButtonActionMapping(
                buttonId = 1,
                triggerPhase = HardwareButtonTriggerPhase.UP,
                action = HardwareButtonActionKind.RELAY_OFF,
                relayId = 1,
            ),
        )

        assertThat(
            PanelButtonActionDefaults.phaseActionFor(
                buttonId = 1,
                triggerPhase = HardwareButtonTriggerPhase.DOWN,
                relayCount = 1,
                mappings = mappings,
            ),
        ).isEqualTo(PanelButtonAction.SetRelay(1, true))
        assertThat(
            PanelButtonActionDefaults.phaseActionFor(
                buttonId = 1,
                triggerPhase = HardwareButtonTriggerPhase.UP,
                relayCount = 1,
                mappings = mappings,
            ),
        ).isEqualTo(PanelButtonAction.SetRelay(1, false))
    }

    @Test fun pressAndReleaseMappingsDoNotDelayShortClick() {
        val mappings = listOf(
            HardwareButtonActionMapping(
                buttonId = 2,
                triggerPhase = HardwareButtonTriggerPhase.DOWN,
                action = HardwareButtonActionKind.RELAY_ON,
                relayId = 1,
            ),
            HardwareButtonActionMapping(
                buttonId = 2,
                triggerPhase = HardwareButtonTriggerPhase.CLICK,
                pressType = PanelButtonPressType.SHORT.name,
                action = HardwareButtonActionKind.TOGGLE_RELAY,
                relayId = 1,
            ),
        )

        assertThat(
            PanelButtonActionDefaults.shouldEmitShortImmediately(
                buttonId = 2,
                relayCount = 1,
                mappings = mappings,
            ),
        ).isTrue()
    }
}
