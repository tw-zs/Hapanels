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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

class ShellyWallDisplayHardware(
    context: Context,
    private val settings: SettingsRepository? = null,
) : PanelHardware, SensorEventListener {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val inputMonitor = ShellyInputMonitor()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var settingsJob: Job? = null
    @Volatile private var buttonActionMappings: List<HardwareButtonActionMapping> = emptyList()
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
            supportsScreenBrightness = true,
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
        lightSensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        proximitySensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        updateRuntime {
            it.copy(
                relayStates = initialRelayStates(),
                screenBrightnessPercent = readScreenBrightnessPercent(),
            )
        }
        val inputStarted = inputMonitor.start(object : ShellyInputMonitor.KeyCallback {
            override fun onHardwareKey(keyCode: Int, action: Int, repeatCount: Int) {
                handleHardwareKey(keyCode, action)
            }
        })
        R1Log.i("ShellyWallDisplayHardware", "native input started=$inputStarted")
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
        eventBus.emit(
            PanelHardwareEvent.UnsupportedAction(
                action = "setScreenBrightness($percent)",
                detail = "Shelly brightness control will land with the panel screen manager.",
            ),
        )
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
            Sensor.TYPE_LIGHT -> updateRuntime { it.copy(ambientLightLux = event.values.firstOrNull()) }
            Sensor.TYPE_PROXIMITY -> updateRuntime { it.copy(proximityDistanceCm = event.values.firstOrNull()) }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun updateRuntime(transform: (PanelHardwareRuntimeState) -> PanelHardwareRuntimeState) {
        runtimeStateState.value = transform(runtimeStateState.value).copy(updatedAtMillis = System.currentTimeMillis())
    }

    private fun readScreenBrightnessPercent(): Int? = runCatching {
        val raw = Settings.System.getInt(appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        ((raw.coerceIn(0, 255) / 255f) * 100).toInt().coerceIn(0, 100)
    }.getOrNull()

    private fun handleHardwareKey(keyCode: Int, action: Int) {
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
    const val KEY_F10 = 68
    const val KEY_F11 = 87
    const val KEY_F12 = 88

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
