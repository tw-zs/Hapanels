package com.github.itskenny0.r1ha.core.hardware

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class PanelMqttCommandTest {
    @Test
    fun `parses relay on payloads`() {
        assertThat(PanelMqttCommand.parse("hapanels/panel", "hapanels/panel/relay/1/set", "ON"))
            .isEqualTo(PanelMqttCommand.SetRelay(relayId = 1, on = true))
        assertThat(PanelMqttCommand.parse("hapanels/panel", "hapanels/panel/relay/2/set", "true"))
            .isEqualTo(PanelMqttCommand.SetRelay(relayId = 2, on = true))
        assertThat(PanelMqttCommand.parse("hapanels/panel", "hapanels/panel/relay/3/set", "1"))
            .isEqualTo(PanelMqttCommand.SetRelay(relayId = 3, on = true))
    }

    @Test
    fun `parses relay off payloads`() {
        assertThat(PanelMqttCommand.parse("hapanels/panel", "hapanels/panel/relay/1/set", "OFF"))
            .isEqualTo(PanelMqttCommand.SetRelay(relayId = 1, on = false))
        assertThat(PanelMqttCommand.parse("hapanels/panel", "hapanels/panel/relay/2/set", "false"))
            .isEqualTo(PanelMqttCommand.SetRelay(relayId = 2, on = false))
        assertThat(PanelMqttCommand.parse("hapanels/panel", "hapanels/panel/relay/3/set", "0"))
            .isEqualTo(PanelMqttCommand.SetRelay(relayId = 3, on = false))
    }

    @Test
    fun `parses and clamps brightness command`() {
        assertThat(PanelMqttCommand.parse("hapanels/panel", "hapanels/panel/screen/brightness/set", "42"))
            .isEqualTo(PanelMqttCommand.SetBrightness(42))
        assertThat(PanelMqttCommand.parse("hapanels/panel", "hapanels/panel/screen/brightness/set", "140"))
            .isEqualTo(PanelMqttCommand.SetBrightness(100))
        assertThat(PanelMqttCommand.parse("hapanels/panel", "hapanels/panel/screen/brightness/set", "-5"))
            .isEqualTo(PanelMqttCommand.SetBrightness(0))
    }

    @Test
    fun `parses auto brightness switch payloads`() {
        assertThat(PanelMqttCommand.parse("hapanels/panel", "hapanels/panel/screen/auto_brightness/set", "ON"))
            .isEqualTo(PanelMqttCommand.SetAutoBrightness(enabled = true))
        assertThat(PanelMqttCommand.parse("hapanels/panel", "hapanels/panel/screen/auto_brightness/set", "false"))
            .isEqualTo(PanelMqttCommand.SetAutoBrightness(enabled = false))
    }

    @Test
    fun `parses dashboard config command`() {
        val raw = """{"revision":43}"""

        assertThat(PanelMqttCommand.parse("hapanels/panel", "hapanels/panel/dashboard/config/set", raw))
            .isEqualTo(PanelMqttCommand.SetDashboardConfig(raw))
    }

    @Test
    fun `parses dashboard patch command`() {
        val raw = """{"base_revision":42,"tile_updates":[]}"""

        assertThat(PanelMqttCommand.parse("hapanels/panel", "hapanels/panel/dashboard/config/patch/set", raw))
            .isEqualTo(PanelMqttCommand.PatchDashboardConfig(raw))
    }

    @Test
    fun `rejects unknown command payloads and topics`() {
        assertThat(PanelMqttCommand.parse("hapanels/panel", "hapanels/panel/relay/1/set", "toggle"))
            .isNull()
        assertThat(PanelMqttCommand.parse("hapanels/panel", "hapanels/panel/screen/brightness/set", "bright"))
            .isNull()
        assertThat(PanelMqttCommand.parse("hapanels/panel", "hapanels/panel/screen/auto_brightness/set", "auto"))
            .isNull()
        assertThat(PanelMqttCommand.parse("hapanels/panel", "hapanels/other/relay/1/set", "ON"))
            .isNull()
    }

    @Test
    fun `normalizes mqtt diagnostic errors for HA sensor state`() {
        assertThat(mqttDiagnosticError("  refused\nreturn code=5  "))
            .isEqualTo("refused return code=5")
        assertThat(mqttDiagnosticError(""))
            .isEqualTo("unknown")
        assertThat(mqttDiagnosticError("x".repeat(260)))
            .hasLength(240)
    }
}
