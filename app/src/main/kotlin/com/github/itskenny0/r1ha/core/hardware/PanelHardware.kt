package com.github.itskenny0.r1ha.core.hardware

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PanelHardware {
    val provider: PanelHardwareProvider
    val capabilities: StateFlow<PanelCapabilities>
    val status: StateFlow<PanelHardwareStatus>
    val runtimeState: StateFlow<PanelHardwareRuntimeState>
    val events: Flow<PanelHardwareEvent>

    suspend fun start()
    suspend fun stop()
    suspend fun setRelay(id: Int, on: Boolean)
    suspend fun setScreenBrightness(percent: Int)
    suspend fun wakeScreen(reason: WakeReason)
}

enum class PanelHardwareProvider(val label: String) {
    ANDROID_TABLET("Generic Android tablet"),
    SHELLY_WALL_DISPLAY("Shelly Wall Display"),
}

data class PanelCapabilities(
    val providerLabel: String,
    val hardwareModel: String,
    val relayCount: Int = 0,
    val physicalButtonCount: Int = 0,
    val hasAmbientLightSensor: Boolean = false,
    val hasProximitySensor: Boolean = false,
    val supportsScreenBrightness: Boolean = false,
    val supportsWake: Boolean = false,
)

data class PanelHardwareStatus(
    val providerLabel: String,
    val modeLabel: String,
    val running: Boolean,
    val detail: String,
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

data class PanelHardwareRuntimeState(
    val pressedButtonIds: Set<Int> = emptySet(),
    val relayStates: Map<Int, Boolean?> = emptyMap(),
    val proximityDistanceCm: Float? = null,
    val ambientLightLux: Float? = null,
    val screenBrightnessPercent: Int? = null,
    val note: String? = null,
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

enum class PanelButtonPressType { DOWN, UP, SHORT, LONG, DOUBLE, TRIPLE }

sealed interface PanelHardwareEvent {
    val timestampMillis: Long

    data class Lifecycle(
        val message: String,
        override val timestampMillis: Long = System.currentTimeMillis(),
    ) : PanelHardwareEvent

    data class UnsupportedAction(
        val action: String,
        val detail: String,
        override val timestampMillis: Long = System.currentTimeMillis(),
    ) : PanelHardwareEvent

    data class Button(
        val buttonId: Int,
        val type: PanelButtonPressType,
        override val timestampMillis: Long = System.currentTimeMillis(),
    ) : PanelHardwareEvent

    data class Relay(
        val relayId: Int,
        val on: Boolean,
        override val timestampMillis: Long = System.currentTimeMillis(),
    ) : PanelHardwareEvent
}

enum class WakeReason {
    USER,
    PROXIMITY,
    BUTTON,
    AUTOMATION,
}
