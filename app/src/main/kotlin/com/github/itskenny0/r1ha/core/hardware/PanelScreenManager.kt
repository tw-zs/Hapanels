package com.github.itskenny0.r1ha.core.hardware

import android.view.Window
import android.view.WindowManager
import com.github.itskenny0.r1ha.core.prefs.AppSettings
import com.github.itskenny0.r1ha.core.prefs.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ln
import kotlin.math.roundToInt

enum class PanelScreenMode { ACTIVE, DIMMED, SCREENSAVER }

enum class SleepReason { IDLE_TIMEOUT }

data class PanelScreenSettings(
    val proximityWakeEnabled: Boolean = false,
    val proximityNearThresholdCm: Float = 5f,
    val autoBrightnessEnabled: Boolean = false,
    val minBrightnessPercent: Int = 10,
    val maxBrightnessPercent: Int = 100,
    val screensaverEnabled: Boolean = false,
    val screensaverTimeoutMillis: Long = 300_000L,
) {
    companion object {
        fun from(settings: AppSettings): PanelScreenSettings {
            val advanced = settings.advanced
            return PanelScreenSettings(
                proximityWakeEnabled = advanced.proximityWakeEnabled,
                proximityNearThresholdCm = advanced.proximityNearThresholdCm.coerceIn(0.1f, 20f),
                autoBrightnessEnabled = advanced.autoBrightnessEnabled,
                minBrightnessPercent = advanced.autoBrightnessMinPercent.coerceIn(1, 100),
                maxBrightnessPercent = advanced.autoBrightnessMaxPercent.coerceIn(1, 100),
                screensaverEnabled = advanced.screensaverEnabled,
                screensaverTimeoutMillis = advanced.screensaverTimeoutSec.coerceIn(15, 86_400) * 1_000L,
            )
        }
    }
}

data class PanelScreenState(
    val mode: PanelScreenMode = PanelScreenMode.ACTIVE,
    val targetBrightnessPercent: Int? = null,
    val appliedBrightnessPercent: Int? = null,
    val lastWakeReason: WakeReason? = null,
    val lastSleepReason: SleepReason? = null,
    val lastWakeAtMillis: Long? = null,
    val lastSleepAtMillis: Long? = null,
    val lastUserActivityAtMillis: Long = 0L,
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

object PanelBrightnessCurve {
    fun percentForLux(lux: Float?, minPercent: Int, maxPercent: Int): Int {
        val low = minPercent.coerceIn(1, 100)
        val high = maxPercent.coerceIn(low, 100)
        if (lux == null) return high
        val clampedLux = lux.coerceIn(0f, 1_000f)
        val ratio = (ln(clampedLux + 1f) / ln(1_001f)).coerceIn(0f, 1f)
        return (low + ((high - low) * ratio)).roundToInt().coerceIn(low, high)
    }
}

class PanelScreenEngine(
    initialNowMillis: Long = System.currentTimeMillis(),
    initialSettings: PanelScreenSettings = PanelScreenSettings(),
) {
    private var settings = initialSettings
    private var lastProximityWakeAtMillis = -PROXIMITY_WAKE_COOLDOWN_MS
    var state: PanelScreenState = PanelScreenState(
        targetBrightnessPercent = brightnessFor(null, initialSettings),
        lastUserActivityAtMillis = initialNowMillis,
        updatedAtMillis = initialNowMillis,
    )
        private set

    fun updateSettings(next: PanelScreenSettings, nowMillis: Long = System.currentTimeMillis()): PanelScreenState {
        settings = next
        val brightness = brightnessFor(null, next)
        state = state.copy(
            targetBrightnessPercent = brightness,
            mode = if (!next.screensaverEnabled && state.mode == PanelScreenMode.SCREENSAVER) {
                PanelScreenMode.ACTIVE
            } else {
                state.mode
            },
            updatedAtMillis = nowMillis,
        )
        return state
    }

    fun onRuntimeState(runtime: PanelHardwareRuntimeState, nowMillis: Long = System.currentTimeMillis()): PanelScreenState {
        val brightness = brightnessFor(runtime.ambientLightLux, settings)
        val isNear = runtime.proximityDistanceCm?.let { it <= settings.proximityNearThresholdCm } == true
        val next = if (settings.proximityWakeEnabled && isNear && nowMillis - lastProximityWakeAtMillis >= PROXIMITY_WAKE_COOLDOWN_MS) {
            lastProximityWakeAtMillis = nowMillis
            wake(WakeReason.PROXIMITY, nowMillis, brightness)
        } else {
            state.copy(targetBrightnessPercent = brightness, updatedAtMillis = nowMillis)
        }
        state = next
        return state
    }

    fun onUserActivity(reason: WakeReason = WakeReason.USER, nowMillis: Long = System.currentTimeMillis()): PanelScreenState =
        wake(reason, nowMillis, state.targetBrightnessPercent)

    fun onTick(nowMillis: Long = System.currentTimeMillis()): PanelScreenState {
        if (!settings.screensaverEnabled) return state.copy(updatedAtMillis = nowMillis).also { state = it }
        val idleFor = nowMillis - state.lastUserActivityAtMillis
        if (state.mode == PanelScreenMode.SCREENSAVER || idleFor < settings.screensaverTimeoutMillis) {
            return state.copy(updatedAtMillis = nowMillis).also { state = it }
        }
        state = state.copy(
            mode = PanelScreenMode.SCREENSAVER,
            lastSleepReason = SleepReason.IDLE_TIMEOUT,
            lastSleepAtMillis = nowMillis,
            updatedAtMillis = nowMillis,
        )
        return state
    }

    fun markAppliedBrightness(percent: Int?, nowMillis: Long = System.currentTimeMillis()): PanelScreenState {
        state = state.copy(appliedBrightnessPercent = percent, updatedAtMillis = nowMillis)
        return state
    }

    private fun wake(reason: WakeReason, nowMillis: Long, brightness: Int?): PanelScreenState =
        state.copy(
            mode = PanelScreenMode.ACTIVE,
            targetBrightnessPercent = brightness,
            lastWakeReason = reason,
            lastWakeAtMillis = nowMillis,
            lastUserActivityAtMillis = nowMillis,
            updatedAtMillis = nowMillis,
        )

    private fun brightnessFor(lux: Float?, settings: PanelScreenSettings): Int? =
        if (settings.autoBrightnessEnabled) {
            PanelBrightnessCurve.percentForLux(lux, settings.minBrightnessPercent, settings.maxBrightnessPercent)
        } else {
            null
        }

    companion object {
        const val PROXIMITY_WAKE_COOLDOWN_MS: Long = 2_000L
    }
}

class PanelScreenManager(
    private val settingsRepository: SettingsRepository,
    private val hardware: PanelHardware,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val engine = PanelScreenEngine()
    private val mutableState = MutableStateFlow(engine.state)
    val state: StateFlow<PanelScreenState> = mutableState
    private var startJob: Job? = null
    @Volatile
    private var window: Window? = null

    fun attachWindow(window: Window) {
        this.window = window
        applyBrightness(mutableState.value.targetBrightnessPercent)
    }

    fun detachWindow(window: Window) {
        if (this.window === window) this.window = null
    }

    fun start() {
        if (startJob != null) return
        startJob = scope.launch {
            combine(
                settingsRepository.settings,
                hardware.runtimeState,
            ) { settings, runtime -> PanelScreenSettings.from(settings) to runtime }
                .collect { (settings, runtime) ->
                    publish(engine.updateSettings(settings))
                    publish(engine.onRuntimeState(runtime))
                }
        }
        scope.launch {
            hardware.events.collect { event ->
                if (event is PanelHardwareEvent.Button) reportUserActivity(WakeReason.BUTTON)
            }
        }
        scope.launch {
            while (true) {
                delay(1_000L)
                publish(engine.onTick())
            }
        }
        scope.launch {
            state
                .distinctUntilChanged { a, b -> a.targetBrightnessPercent == b.targetBrightnessPercent }
                .collect { applyBrightness(it.targetBrightnessPercent) }
        }
    }

    fun reportUserActivity(reason: WakeReason = WakeReason.USER) {
        scope.launch { publish(engine.onUserActivity(reason)) }
    }

    private suspend fun publish(next: PanelScreenState) {
        mutableState.value = next
    }

    private fun applyBrightness(percent: Int?) {
        scope.launch {
            if (percent != null && hardware.capabilities.value.supportsScreenBrightness) {
                hardware.setScreenBrightness(percent)
                mutableState.value = engine.markAppliedBrightness(percent)
                return@launch
            }
            val targetWindow = window ?: run {
                mutableState.value = engine.markAppliedBrightness(percent)
                return@launch
            }
            withContext(Dispatchers.Main.immediate) {
                val attrs = targetWindow.attributes
                attrs.screenBrightness = percent?.let { it.coerceIn(1, 100) / 100f }
                    ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                targetWindow.attributes = attrs
                mutableState.value = engine.markAppliedBrightness(percent)
            }
        }
    }
}
