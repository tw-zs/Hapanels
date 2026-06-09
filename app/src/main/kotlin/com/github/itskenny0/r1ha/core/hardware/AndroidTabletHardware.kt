package com.github.itskenny0.r1ha.core.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidTabletHardware(context: Context) : PanelHardware, SensorEventListener {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val sensorHandler = Handler(Looper.getMainLooper())
    private val eventBus = MutableSharedFlow<PanelHardwareEvent>(
        replay = 32,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val provider: PanelHardwareProvider = PanelHardwareProvider.ANDROID_TABLET
    private val capabilitiesState = MutableStateFlow(detectCapabilities())
    override val capabilities = capabilitiesState.asStateFlow()
    private val statusState = MutableStateFlow(
        PanelHardwareStatus(
            providerLabel = provider.label,
            modeLabel = "Generic tablet",
            running = false,
            detail = "Provider not started",
        ),
    )
    override val status = statusState.asStateFlow()
    private val runtimeStateState = MutableStateFlow(
        PanelHardwareRuntimeState(
            screenBrightnessPercent = readScreenBrightnessPercent(),
            note = "Relays and physical buttons are not available on generic Android provider.",
        ),
    )
    override val runtimeState = runtimeStateState.asStateFlow()
    override val events = eventBus.asSharedFlow()

    override suspend fun start() {
        val lightRegistered = registerSensor(lightSensor)
        val proximityRegistered = registerSensor(proximitySensor)
        capabilitiesState.value = capabilitiesState.value.copy(
            hasAmbientLightSensor = lightRegistered,
            hasProximitySensor = proximityRegistered,
        )
        updateRuntime { it.copy(screenBrightnessPercent = readScreenBrightnessPercent()) }
        statusState.value = statusState.value.copy(
            running = true,
            detail = tabletDetail(),
            updatedAtMillis = System.currentTimeMillis(),
        )
        eventBus.emit(PanelHardwareEvent.Lifecycle("${provider.label} provider started"))
    }

    override suspend fun stop() {
        sensorManager?.unregisterListener(this)
        statusState.value = statusState.value.copy(
            running = false,
            detail = "Provider stopped",
            updatedAtMillis = System.currentTimeMillis(),
        )
        eventBus.emit(PanelHardwareEvent.Lifecycle("${provider.label} provider stopped"))
    }

    override suspend fun setRelay(id: Int, on: Boolean) {
        eventBus.emit(
            PanelHardwareEvent.UnsupportedAction(
                action = "setRelay($id, $on)",
                detail = "Generic Android tablets do not expose local relays.",
            ),
        )
    }

    override suspend fun setScreenBrightness(percent: Int) {
        eventBus.emit(
            PanelHardwareEvent.UnsupportedAction(
                action = "setScreenBrightness($percent)",
                detail = "Generic Android brightness uses per-window fallback from the panel screen manager.",
            ),
        )
    }

    override suspend fun wakeScreen(reason: WakeReason) {
        eventBus.emit(
            PanelHardwareEvent.UnsupportedAction(
                action = "wakeScreen($reason)",
                detail = "Wake handling is not wired until the panel screen manager lands.",
            ),
        )
    }

    private fun detectCapabilities(): PanelCapabilities {
        return PanelCapabilities(
            providerLabel = provider.label,
            hardwareModel = listOf(Build.MANUFACTURER, Build.MODEL)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { Build.DEVICE },
            hasAmbientLightSensor = lightSensor != null,
            hasProximitySensor = proximitySensor != null,
            supportsScreenBrightness = false,
            supportsWake = false,
        )
    }

    private fun registerSensor(sensor: Sensor?): Boolean = sensor?.let {
        sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL, sensorHandler) == true
    } ?: false

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_LIGHT -> updateRuntime { it.copy(ambientLightLux = sanitizePanelSensorReading(event.values.firstOrNull())) }
            Sensor.TYPE_PROXIMITY -> updateRuntime { it.copy(proximityDistanceCm = sanitizePanelSensorReading(event.values.firstOrNull())) }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    protected fun updateRuntime(transform: (PanelHardwareRuntimeState) -> PanelHardwareRuntimeState) {
        runtimeStateState.value = transform(runtimeStateState.value).copy(updatedAtMillis = System.currentTimeMillis())
    }

    private fun readScreenBrightnessPercent(): Int? = runCatching {
        val raw = Settings.System.getInt(appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        ((raw.coerceIn(0, 255) / 255f) * 100).toInt().coerceIn(0, 100)
    }.getOrNull()

    private fun tabletDetail(): String {
        val caps = capabilities.value
        val sensors = buildList {
            if (caps.hasAmbientLightSensor) add("light")
            if (caps.hasProximitySensor) add("proximity")
        }
        return if (sensors.isEmpty()) {
            "Active fallback provider, no panel sensors detected"
        } else {
            "Active fallback provider, sensors: ${sensors.joinToString(", ")}"
        }
    }
}
