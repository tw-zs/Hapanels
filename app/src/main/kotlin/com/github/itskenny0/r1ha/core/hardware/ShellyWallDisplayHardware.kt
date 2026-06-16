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
import com.github.itskenny0.r1ha.core.ha.HaRepository
import com.github.itskenny0.r1ha.core.mqtt.MqttPublisher
import com.github.itskenny0.r1ha.core.prefs.HardwareButtonActionMapping
import com.github.itskenny0.r1ha.core.prefs.HardwareButtonTriggerPhase
import com.github.itskenny0.r1ha.core.prefs.SettingsRepository
import com.github.itskenny0.r1ha.core.util.R1Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.io.File

class ShellyWallDisplayHardware(
    context: Context,
    private val settings: SettingsRepository? = null,
    private val haRepository: HaRepository? = null,
) : PanelHardware, SensorEventListener {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val proximitySensor = sensorManager?.getShellyProximitySensor()
    private val screenBrightnessFile = SHELLY_SCREEN_BRIGHTNESS_FILES.firstOrNull { it.exists() }
    private val supportsGpioProximity = listOf(Build.MODEL, Build.DEVICE, Build.PRODUCT)
        .any { value -> value.contains("blake", ignoreCase = true) || value.contains("xl", ignoreCase = true) }
    private val inputMonitor = ShellyInputMonitor()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var settingsJob: Job? = null
    @Volatile private var buttonActionMappings: List<HardwareButtonActionMapping> = emptyList()
    @Volatile private var gpioProximityConfirmed = false
    private val buttonFlushHandler = Handler(Looper.getMainLooper())
    private val buttonDetector = PanelButtonPressDetector { event ->
        eventBus.tryEmit(event)
        if (event.type != PanelButtonPressType.DOWN && event.type != PanelButtonPressType.UP) {
            scope.launch { executeButtonAction(event) }
        }
    }
    private val eventBus = MutableSharedFlow<PanelHardwareEvent>(
        replay = 32,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val provider: PanelHardwareProvider = PanelHardwareProvider.SHELLY_WALL_DISPLAY
    private val capabilitiesState = MutableStateFlow(
        PanelCapabilities(
            providerLabel = provider.label,
            hardwareModel = listOf(Build.MANUFACTURER, Build.MODEL)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { Build.DEVICE },
            relayCount = if (ShellyRelayStateStore.defaultRelayFile.exists()) 1 else 0,
            physicalButtonCount = 5,
            hasAmbientLightSensor = lightSensor != null,
            hasProximitySensor = proximitySensor != null,
            supportsScreenBrightness = screenBrightnessFile != null,
            supportsWake = false,
        ),
    )
    override val capabilities = capabilitiesState.asStateFlow()
    private val statusState = MutableStateFlow(
        PanelHardwareStatus(
            providerLabel = provider.label,
            modeLabel = "Shelly",
            running = false,
            detail = "Provider not started",
        ),
    )
    override val status = statusState.asStateFlow()
    private val runtimeStateState = MutableStateFlow(
        PanelHardwareRuntimeState(
            relayStates = initialRelayStates(),
            screenBrightnessPercent = readScreenBrightnessPercent(),
            note = "Shelly wall buttons are 1-4 from adc-keys. Button 5 is the power key. Relay detect is read-only until relay control is ported.",
        ),
    )
    override val runtimeState = runtimeStateState.asStateFlow()
    override val events = eventBus.asSharedFlow()

    override suspend fun start() {
        settingsJob?.cancel()
        settingsJob = settings?.let { repo ->
            scope.launch {
                repo.settings
                    .map { it.advanced.hardwareButtonActions }
                    .collect { buttonActionMappings = it }
            }
        }
        val lightRegistered = registerSensor(lightSensor)
        val proximityRegistered = registerSensor(proximitySensor, useMainHandler = false)
        R1Log.i(
            "ShellyWallDisplayHardware",
            "sensors light=${lightSensor?.name ?: "none"} registered=$lightRegistered proximity=${proximitySensor?.name ?: "none"} registered=$proximityRegistered",
        )
        val inputStarted = inputMonitor.start(object : ShellyInputMonitor.KeyCallback {
            override fun onHardwareKey(keyCode: Int, action: Int, repeatCount: Int) {
                handleHardwareKey(keyCode, action)
            }
        })
        R1Log.i("ShellyWallDisplayHardware", "native input started=$inputStarted")
        capabilitiesState.value = capabilitiesState.value.copy(
            hasAmbientLightSensor = lightRegistered,
            hasProximitySensor = proximityRegistered || (inputStarted && supportsGpioProximity),
        )
        updateRuntime {
            it.copy(
                relayStates = initialRelayStates(),
                screenBrightnessPercent = readScreenBrightnessPercent(),
                ambientLightLux = it.ambientLightLux.takeIf { lightRegistered },
                proximityDistanceCm = it.proximityDistanceCm.takeIf { proximityRegistered },
            )
        }
        statusState.value = statusState.value.copy(
            running = true,
            detail = if (inputStarted) {
                "Selected, native input monitor active; relay control ${if (ShellyRelayStateStore.defaultRelayFile.exists()) "active" else "unavailable"}"
            } else {
                "Selected, native input unavailable; relays/buttons pending"
            },
            updatedAtMillis = System.currentTimeMillis(),
        )
        eventBus.emit(
            PanelHardwareEvent.Lifecycle(
                "Shelly provider selected. Native input active; relay 1 ${if (ShellyRelayStateStore.defaultRelayFile.exists()) "available" else "unavailable"}.",
            ),
        )
    }

    override suspend fun stop() {
        sensorManager?.unregisterListener(this)
        buttonFlushHandler.removeCallbacksAndMessages(null)
        inputMonitor.stop()
        settingsJob?.cancel()
        settingsJob = null
        statusState.value = statusState.value.copy(
            running = false,
            detail = "Provider stopped",
            updatedAtMillis = System.currentTimeMillis(),
        )
        eventBus.emit(PanelHardwareEvent.Lifecycle("Shelly provider stopped"))
    }

    override suspend fun setRelay(id: Int, on: Boolean) {
        if (id != 1 || !ShellyRelayStateStore.defaultRelayFile.exists()) {
            eventBus.emit(
                PanelHardwareEvent.UnsupportedAction(
                    action = "setRelay($id, $on)",
                    detail = "This Shelly exposes only relay 1 at ${ShellyRelayStateStore.defaultRelayFile.path}.",
                ),
            )
            return
        }
        runCatching {
            ShellyRelayStateStore.writeRelay(1, on)
        }.onSuccess {
            updateRuntime { it.copy(relayStates = it.relayStates + (1 to on)) }
            eventBus.emit(PanelHardwareEvent.Relay(relayId = 1, on = on))
            R1Log.i("ShellyWallDisplayHardware", "relay 1 set to $on")
        }.onFailure { t ->
            eventBus.emit(
                PanelHardwareEvent.UnsupportedAction(
                    action = "setRelay($id, $on)",
                    detail = "Relay write failed: ${t.message ?: t::class.java.simpleName}",
                ),
            )
            R1Log.w("ShellyWallDisplayHardware", "relay 1 write failed: ${t.message}", t)
        }
    }

    override suspend fun setScreenBrightness(percent: Int) {
        val targetFile = screenBrightnessFile
        if (targetFile == null) {
            eventBus.emit(
                PanelHardwareEvent.UnsupportedAction(
                    action = "setScreenBrightness($percent)",
                    detail = "No Shelly backlight sysfs node found.",
                ),
            )
            return
        }
        val safePercent = percent.coerceIn(0, 100)
        val raw = ((safePercent / 100f) * 255).toInt().coerceIn(0, 255)
        runCatching {
            if (Settings.System.canWrite(appContext)) {
                Settings.System.putInt(
                    appContext.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                )
            }
            targetFile.writeText(raw.toString())
        }.onSuccess {
            updateRuntime { it.copy(screenBrightnessPercent = safePercent) }
            R1Log.i("ShellyWallDisplayHardware", "screen brightness set to $safePercent% ($raw)")
        }.onFailure { t ->
            eventBus.emit(
                PanelHardwareEvent.UnsupportedAction(
                    action = "setScreenBrightness($percent)",
                    detail = "Brightness write failed: ${t.message ?: t::class.java.simpleName}",
                ),
            )
            R1Log.w("ShellyWallDisplayHardware", "screen brightness write failed: ${t.message}", t)
        }
    }

    override suspend fun wakeScreen(reason: WakeReason) {
        eventBus.emit(
            PanelHardwareEvent.UnsupportedAction(
                action = "wakeScreen($reason)",
                detail = "Shelly wake handling will land with proximity and screensaver support.",
            ),
        )
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_LIGHT -> updateRuntime { it.copy(ambientLightLux = sanitizePanelSensorReading(event.values.firstOrNull())) }
            Sensor.TYPE_PROXIMITY -> {
                if (gpioProximityConfirmed) return
                val distance = sanitizePanelSensorReading(event.values.firstOrNull())
                R1Log.d("ShellyWallDisplayHardware", "proximity distance=${distance?.toString() ?: "invalid"}cm")
                updateRuntime { it.copy(proximityDistanceCm = distance) }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun registerSensor(sensor: Sensor?, useMainHandler: Boolean = true): Boolean = sensor?.let {
        if (useMainHandler) {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL, Handler(Looper.getMainLooper())) == true
        } else {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) == true
        }
    } ?: false

    private fun SensorManager.getShellyProximitySensor(): Sensor? =
        getDefaultSensor(Sensor.TYPE_PROXIMITY)
            ?: getDefaultSensor(Sensor.TYPE_PROXIMITY, true)
            ?: getDefaultSensor(Sensor.TYPE_PROXIMITY, false)

    private fun updateRuntime(transform: (PanelHardwareRuntimeState) -> PanelHardwareRuntimeState) {
        runtimeStateState.value = transform(runtimeStateState.value).copy(updatedAtMillis = System.currentTimeMillis())
    }

    private fun readScreenBrightnessPercent(): Int? = runCatching {
        val raw = screenBrightnessFile?.readText()?.trim()?.toIntOrNull()
            ?: Settings.System.getInt(appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        ((raw.coerceIn(0, 255) / 255f) * 100).toInt().coerceIn(0, 100)
    }.getOrNull()

    private fun handleHardwareKey(keyCode: Int, action: Int) {
        val proximityDistance = ShellyInputKeyMap.proximityDistanceFor(keyCode)
        if (proximityDistance != null) {
            if (action == ACTION_DOWN) {
                gpioProximityConfirmed = true
                capabilitiesState.value = capabilitiesState.value.copy(hasProximitySensor = true)
                R1Log.d("ShellyWallDisplayHardware", "gpio proximity distance=${proximityDistance}cm keyCode=$keyCode")
                updateRuntime { it.copy(proximityDistanceCm = proximityDistance) }
            }
            return
        }

        val relayId = ShellyInputKeyMap.relayIdFor(keyCode)
        if (relayId != null) {
            if (action == ACTION_DOWN || action == ACTION_UP) {
                updateRuntime {
                    it.copy(relayStates = it.relayStates + (relayId to (action == ACTION_DOWN)))
                }
            }
            return
        }

        val buttonId = ShellyInputKeyMap.buttonIdFor(keyCode) ?: return
        R1Log.i("ShellyWallDisplayHardware", "button id=$buttonId keyCode=$keyCode action=$action")
        when (action) {
            ACTION_DOWN -> {
                updateRuntime { it.copy(pressedButtonIds = it.pressedButtonIds + buttonId) }
                scope.launch { executeButtonPhaseAction(buttonId, HardwareButtonTriggerPhase.DOWN) }
                buttonDetector.onDown(buttonId)
            }
            ACTION_UP -> {
                updateRuntime { it.copy(pressedButtonIds = it.pressedButtonIds - buttonId) }
                scope.launch { executeButtonPhaseAction(buttonId, HardwareButtonTriggerPhase.UP) }
                val emitShortImmediately = PanelButtonActionDefaults.shouldEmitShortImmediately(
                    buttonId = buttonId,
                    relayCount = capabilitiesState.value.relayCount,
                    mappings = buttonActionMappings,
                )
                buttonDetector.onUp(buttonId, emitShortImmediately = emitShortImmediately)
                if (!emitShortImmediately) {
                    buttonFlushHandler.postDelayed({ buttonDetector.flush() }, BUTTON_MULTI_CLICK_TIMEOUT_MS + 25)
                }
            }
            ACTION_REPEAT -> Unit
        }
    }

    private suspend fun executeButtonPhaseAction(buttonId: Int, triggerPhase: HardwareButtonTriggerPhase) {
        executeAction(
            PanelButtonActionDefaults.phaseActionFor(
                buttonId = buttonId,
                triggerPhase = triggerPhase,
                relayCount = capabilitiesState.value.relayCount,
                mappings = buttonActionMappings,
            ),
        )
    }

    private suspend fun executeButtonAction(event: PanelHardwareEvent.Button) {
        executeAction(
            PanelButtonActionDefaults.actionFor(
                buttonId = event.buttonId,
                pressType = event.type,
                relayCount = capabilitiesState.value.relayCount,
                mappings = buttonActionMappings,
            ),
        )
    }

    private suspend fun executeAction(action: PanelButtonAction) {
        when (action) {
            PanelButtonAction.None -> Unit
            is PanelButtonAction.ToggleRelay -> {
                val current = runtimeStateState.value.relayStates[action.relayId] == true
                setRelay(action.relayId, !current)
            }
            is PanelButtonAction.SetRelay -> setRelay(action.relayId, action.on)
            is PanelButtonAction.CallHaService -> callHaService(action)
            is PanelButtonAction.PublishMqtt -> publishMqtt(action)
        }
    }

    private suspend fun callHaService(action: PanelButtonAction.CallHaService) {
        val repo = haRepository
        if (repo == null) {
            eventBus.emit(
                PanelHardwareEvent.UnsupportedAction(
                    action = "haService(${action.domain}.${action.service})",
                    detail = "Home Assistant repository is not available to the Shelly hardware provider.",
                ),
            )
            return
        }
        val data = runCatching { parseButtonServiceData(action.dataJson) }
            .onFailure { t ->
                eventBus.emit(
                    PanelHardwareEvent.UnsupportedAction(
                        action = "haService(${action.domain}.${action.service})",
                        detail = "Service data JSON is invalid: ${t.message ?: t::class.java.simpleName}",
                    ),
                )
            }
            .getOrNull() ?: return
        repo.callRawService(action.domain, action.service, data)
            .onSuccess { R1Log.i("ShellyWallDisplayHardware", "button fired HA service ${action.domain}.${action.service}") }
            .onFailure { t ->
                eventBus.emit(
                    PanelHardwareEvent.UnsupportedAction(
                        action = "haService(${action.domain}.${action.service})",
                        detail = "Service call failed: ${t.message ?: t::class.java.simpleName}",
                    ),
                )
                R1Log.w("ShellyWallDisplayHardware", "button HA service failed: ${t.message}", t)
            }
    }

    private suspend fun publishMqtt(action: PanelButtonAction.PublishMqtt) {
        val advanced = settings?.settings?.first()?.advanced
        val host = advanced?.mqttHost?.trim().orEmpty()
        if (host.isBlank()) {
            eventBus.emit(
                PanelHardwareEvent.UnsupportedAction(
                    action = "mqttPublish(${action.topic})",
                    detail = "MQTT host is empty in Advanced settings.",
                ),
            )
            return
        }
        MqttPublisher.publish(
            host = host,
            port = advanced?.mqttPort ?: 1883,
            topic = action.topic,
            payload = action.payload.toByteArray(Charsets.UTF_8),
            clientId = advanced?.mqttClientId?.ifBlank { null } ?: "hapanels-button-${Build.DEVICE}-${System.currentTimeMillis() and 0xFFFF}",
            username = advanced?.mqttUsername?.ifBlank { null },
            password = advanced?.mqttPassword?.ifBlank { null },
            useTls = advanced?.mqttUseTls == true,
            retain = action.retain,
        ).onSuccess {
            R1Log.i("ShellyWallDisplayHardware", "button published MQTT topic=${action.topic}")
        }.onFailure { t ->
            eventBus.emit(
                PanelHardwareEvent.UnsupportedAction(
                    action = "mqttPublish(${action.topic})",
                    detail = "MQTT publish failed: ${t.message ?: t::class.java.simpleName}",
                ),
            )
            R1Log.w("ShellyWallDisplayHardware", "button MQTT publish failed: ${t.message}", t)
        }
    }

    private fun initialRelayStates(): Map<Int, Boolean?> = ShellyRelayStateStore.readRelayStates()

    private companion object {
        const val ACTION_UP = 0
        const val ACTION_DOWN = 1
        const val ACTION_REPEAT = 2
        const val BUTTON_MULTI_CLICK_TIMEOUT_MS = 400L
    }
}

private fun parseButtonServiceData(raw: String): JsonObject {
    if (raw.isBlank()) return JsonObject(emptyMap())
    return Json.parseToJsonElement(raw) as? JsonObject
        ?: error("Service data must be a JSON object")
}

private val SHELLY_SCREEN_BRIGHTNESS_FILES = listOf(
    File("/sys/devices/platform/leds-mt65xx/leds/lcd-backlight/brightness"),
    File("/sys/devices/platform/sprd_backlight/backlight/sprd_backlight/brightness"),
    File("/sys/devices/platform/backlight/backlight/backlight/brightness"),
)

internal object ShellyRelayStateStore {
    val defaultRelayFile = File("/sys/class/strelay/relay1")

    fun readRelayStates(relay1File: File = defaultRelayFile): Map<Int, Boolean?> {
        if (!relay1File.exists()) return emptyMap()
        return mapOf(1 to (relay1File.readText().trim() == "1"))
    }

    fun writeRelay(relayId: Int, on: Boolean, relay1File: File = defaultRelayFile) {
        require(relayId == 1) { "Only relay 1 is supported" }
        relay1File.writeText(if (on) "1" else "0")
    }
}

internal object ShellyInputKeyMap {
    const val KEY_F1 = 59
    const val KEY_F2 = 60
    const val KEY_F3 = 61
    const val KEY_F4 = 62
    const val KEY_F5 = 63
    const val KEY_F6 = 64
    const val KEY_F10 = 68
    const val KEY_F11 = 87
    const val KEY_F12 = 88

    fun proximityDistanceFor(keyCode: Int): Float? = when (keyCode) {
        KEY_F5 -> 0f
        KEY_F6 -> 10f
        else -> null
    }

    fun buttonIdFor(keyCode: Int): Int? = when (keyCode) {
        KEY_F1 -> 1
        KEY_F2 -> 2
        KEY_F3 -> 3
        KEY_F4 -> 4
        KEY_F10 -> 5
        else -> null
    }

    fun relayIdFor(keyCode: Int): Int? = when (keyCode) {
        KEY_F11 -> 1
        else -> null
    }
}
