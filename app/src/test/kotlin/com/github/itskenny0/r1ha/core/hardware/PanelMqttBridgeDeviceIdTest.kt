package com.github.itskenny0.r1ha.core.hardware

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class PanelMqttBridgeDeviceIdTest {
    @Test
    fun `prefers explicit panel device id`() {
        assertThat(
            resolvePanelDeviceId(
                panelDeviceId = "Panel One",
                mqttClientId = "client-x",
                androidId = "android-123",
                buildDevice = "device",
                buildModel = "model",
                buildProduct = "product",
            ),
        ).isEqualTo("panel_one")
    }

    @Test
    fun `falls back to mqtt client id before android id`() {
        assertThat(
            resolvePanelDeviceId(
                panelDeviceId = "",
                mqttClientId = "client-x",
                androidId = "android-123",
                buildDevice = "device",
                buildModel = "model",
                buildProduct = "product",
            ),
        ).isEqualTo("client_x")
    }

    @Test
    fun `falls back to android id before build ids`() {
        assertThat(
            resolvePanelDeviceId(
                panelDeviceId = null,
                mqttClientId = null,
                androidId = "android-123",
                buildDevice = "device",
                buildModel = "model",
                buildProduct = "product",
            ),
        ).isEqualTo("android_123")
    }

    @Test
    fun `falls back to build ids and default panel name`() {
        assertThat(
            resolvePanelDeviceId(
                panelDeviceId = null,
                mqttClientId = null,
                androidId = null,
                buildDevice = "",
                buildModel = "  ",
                buildProduct = "",
            ),
        ).isEqualTo("panel")
    }
}
