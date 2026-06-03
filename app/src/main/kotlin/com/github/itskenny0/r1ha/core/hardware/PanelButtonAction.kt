package com.github.itskenny0.r1ha.core.hardware

import com.github.itskenny0.r1ha.core.prefs.HardwareButtonActionKind
import com.github.itskenny0.r1ha.core.prefs.HardwareButtonActionMapping
import com.github.itskenny0.r1ha.core.prefs.HardwareButtonTriggerPhase

sealed interface PanelButtonAction {
    data object None : PanelButtonAction
    data class ToggleRelay(val relayId: Int) : PanelButtonAction
    data class SetRelay(val relayId: Int, val on: Boolean) : PanelButtonAction
}

object PanelButtonActionDefaults {
    fun shouldEmitShortImmediately(
        buttonId: Int,
        relayCount: Int,
        mappings: List<HardwareButtonActionMapping> = emptyList(),
    ): Boolean {
        val clickMappings = mappings.filter {
            it.buttonId == buttonId && it.triggerPhase == HardwareButtonTriggerPhase.CLICK
        }
        val hasNonShortMapping = clickMappings.any {
            it.pressType == PanelButtonPressType.DOUBLE.name ||
                it.pressType == PanelButtonPressType.TRIPLE.name
        }
        if (hasNonShortMapping) return false
        return actionFor(
            buttonId = buttonId,
            pressType = PanelButtonPressType.SHORT,
            relayCount = relayCount,
            mappings = mappings,
        ) != PanelButtonAction.None
    }

    fun actionFor(
        buttonId: Int,
        pressType: PanelButtonPressType,
        relayCount: Int,
        mappings: List<HardwareButtonActionMapping> = emptyList(),
    ): PanelButtonAction {
        val configured = mappings.firstOrNull {
            it.buttonId == buttonId &&
                it.triggerPhase == HardwareButtonTriggerPhase.CLICK &&
                it.pressType == pressType.name
        }
        if (configured != null) return configured.toAction(relayCount)
        if (pressType != PanelButtonPressType.SHORT) return PanelButtonAction.None
        return when (buttonId) {
            1 -> if (relayCount >= 1) PanelButtonAction.ToggleRelay(1) else PanelButtonAction.None
            // Button 5 is the power key and intentionally has no default action.
            else -> PanelButtonAction.None
        }
    }

    fun phaseActionFor(
        buttonId: Int,
        triggerPhase: HardwareButtonTriggerPhase,
        relayCount: Int,
        mappings: List<HardwareButtonActionMapping> = emptyList(),
    ): PanelButtonAction {
        val configured = mappings.firstOrNull {
            it.buttonId == buttonId && it.triggerPhase == triggerPhase
        } ?: return PanelButtonAction.None
        return configured.toAction(relayCount)
    }

    private fun HardwareButtonActionMapping.toAction(relayCount: Int): PanelButtonAction {
        if (relayId !in 1..relayCount) return PanelButtonAction.None
        return when (action) {
            HardwareButtonActionKind.NONE -> PanelButtonAction.None
            HardwareButtonActionKind.TOGGLE_RELAY -> PanelButtonAction.ToggleRelay(relayId)
            HardwareButtonActionKind.RELAY_ON -> PanelButtonAction.SetRelay(relayId, true)
            HardwareButtonActionKind.RELAY_OFF -> PanelButtonAction.SetRelay(relayId, false)
        }
    }
}
