package com.github.itskenny0.r1ha.core.hardware

import android.view.Window
import android.view.WindowManager
import com.github.itskenny0.r1ha.core.prefs.AppSettings
import com.github.itskenny0.r1ha.core.prefs.SettingsRepository
import com.github.itskenny0.r1ha.core.util.R1Log
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
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

enum class PanelScreenMode { ACTIVE, DIMMED, SCREENSAVER }

enum class SleepReason { IDLE_TIMEOUT, USER_ACTION }

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
                screensaverTimeoutMillis = advanced.screensaverTimeoutSec.coerceIn(5, 86_400) * 1_000L,
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

object PanelBrightnessFade {
    private const val STEP_MILLIS = 50

    fun values(fromPercent: Int, toPercent: Int, durationMillis: Int): List<Int> {
        val from = fromPercent.coerceIn(1, 100)
        val to = toPercent.coerceIn(1, 100)
        if (from == to || durationMillis <= 0) return listOf(to)
        val steps = (durationMillis / STEP_MILLIS).coerceIn(1, 20)
        return (1..steps)
            .map { step -> from + ((to - from) * step.toFloat() / steps).roundToInt() }
            .distinct()
    }

    fun normalTarget(latestPercent: Int?, preAodPercent: Int?): Int? = latestPercent ?: preAodPercent
}

class PanelScreenEngine(
    initialNowMillis: Long = System.currentTimeMillis(),
    initialSettings: PanelScreenSettings = PanelScreenSettings(),
) {
    private var settings = initialSettings
    private var lastProximityWakeAtMillis = -PROXIMITY_WAKE_COOLDOWN_MS
    private var lastBrightnessTargetAtMillis = initialNowMillis - BRIGHTNESS_HYSTERESIS_MS
    private var lastLoggedAmbientLightLux: Float? = null
    private var lastLoggedProximityDistanceCm: Float? = null
    private var lastLoggedTargetBrightness: Int? = null
    var state: PanelScreenState = PanelScreenState(
        targetBrightnessPercent = brightnessFor(null, initialSettings),
        lastUserActivityAtMillis = initialNowMillis,
        updatedAtMillis = initialNowMillis,
    )
        private set

    fun updateSettings(next: PanelScreenSettings, nowMillis: Long = System.currentTimeMillis()): PanelScreenState {
        val screensaverJustEnabled = !settings.screensaverEnabled && next.screensaverEnabled
        settings = next
        val brightness = if (next.autoBrightnessEnabled) {
            state.targetBrightnessPercent ?: brightnessFor(null, next)
        } else {
            null
        }
        state = state.copy(
            targetBrightnessPercent = brightness,
            mode = if (
                !next.screensaverEnabled &&
                state.mode == PanelScreenMode.SCREENSAVER &&
                state.lastSleepReason == SleepReason.IDLE_TIMEOUT
            ) {
                PanelScreenMode.ACTIVE
            } else {
                state.mode
            },
            lastUserActivityAtMillis = if (screensaverJustEnabled) nowMillis else state.lastUserActivityAtMillis,
            updatedAtMillis = nowMillis,
        )
        return state
    }

    fun onRuntimeState(runtime: PanelHardwareRuntimeState, nowMillis: Long = System.currentTimeMillis()): PanelScreenState {
        val brightness = throttledBrightness(brightnessFor(runtime.ambientLightLux, settings), nowMillis)
        val isNear = runtime.proximityDistanceCm?.let { it < settings.proximityNearThresholdCm } == true
        logRuntimeSample(runtime, brightness, isNear)
        val next = if (settings.proximityWakeEnabled && isNear) {
            if (state.mode != PanelScreenMode.ACTIVE && nowMillis - lastProximityWakeAtMillis >= PROXIMITY_WAKE_COOLDOWN_MS) {
                lastProximityWakeAtMillis = nowMillis
                wake(WakeReason.PROXIMITY, nowMillis, brightness)
            } else {
                state.copy(
                    targetBrightnessPercent = brightness,
                    lastUserActivityAtMillis = nowMillis,
                    updatedAtMillis = nowMillis,
                )
            }
        } else {
            state.copy(targetBrightnessPercent = brightness, updatedAtMillis = nowMillis)
        }
        state = next
        return state
    }

    fun onUserActivity(reason: WakeReason = WakeReason.USER, nowMillis: Long = System.currentTimeMillis()): PanelScreenState =
        if (state.mode == PanelScreenMode.ACTIVE) {
            state.copy(lastUserActivityAtMillis = nowMillis, updatedAtMillis = nowMillis).also { state = it }
        } else {
            wake(reason, nowMillis, state.targetBrightnessPercent).also { state = it }
        }

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

    fun showScreensaver(nowMillis: Long = System.currentTimeMillis()): PanelScreenState {
        state = state.copy(
            mode = PanelScreenMode.SCREENSAVER,
            lastSleepReason = SleepReason.USER_ACTION,
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
        run {
            R1Log.d(
                "PanelScreenManager",
                "wake reason=${reason.name.lowercase()} brightness=${brightness?.toString() ?: "system"}",
            )
            state.copy(
                mode = PanelScreenMode.ACTIVE,
                targetBrightnessPercent = brightness,
                lastWakeReason = reason,
                lastWakeAtMillis = nowMillis,
                lastUserActivityAtMillis = nowMillis,
                updatedAtMillis = nowMillis,
            )
        }

    private fun logRuntimeSample(
        runtime: PanelHardwareRuntimeState,
        brightness: Int?,
        isNear: Boolean,
    ) {
        val ambient = runtime.ambientLightLux
        val proximity = runtime.proximityDistanceCm
        val changed = ambient != lastLoggedAmbientLightLux || proximity != lastLoggedProximityDistanceCm || brightness != lastLoggedTargetBrightness
        if (!changed && !isNear) return
        lastLoggedAmbientLightLux = ambient
        lastLoggedProximityDistanceCm = proximity
        lastLoggedTargetBrightness = brightness
        R1Log.d(
            "PanelScreenManager",
            buildString {
                append("runtime lux=")
                append(ambient?.toString() ?: "null")
                append(" proximityCm=")
                append(proximity?.toString() ?: "null")
                append(" near=")
                append(isNear)
                append(" autoBrightness=")
                append(settings.autoBrightnessEnabled)
                append(" proximityWake=")
                append(settings.proximityWakeEnabled)
                append(" target=")
                append(brightness?.toString() ?: "system")
                append(" mode=")
                append(state.mode.name.lowercase())
            },
        )
    }

    private fun brightnessFor(lux: Float?, settings: PanelScreenSettings): Int? =
        if (settings.autoBrightnessEnabled) {
            PanelBrightnessCurve.percentForLux(lux, settings.minBrightnessPercent, settings.maxBrightnessPercent)
        } else {
            null
        }

    private fun throttledBrightness(desired: Int?, nowMillis: Long): Int? {
        if (desired == null) return null
        val current = state.targetBrightnessPercent ?: return desired.also {
            lastBrightnessTargetAtMillis = nowMillis
        }
        if (abs(desired - current) < BRIGHTNESS_MIN_STEP_PERCENT) return current
        if (nowMillis - lastBrightnessTargetAtMillis < BRIGHTNESS_HYSTERESIS_MS) return current
        lastBrightnessTargetAtMillis = nowMillis
        return desired
    }

    companion object {
        const val PROXIMITY_WAKE_COOLDOWN_MS: Long = 2_000L
        const val BRIGHTNESS_HYSTERESIS_MS: Long = 3_000L
        const val BRIGHTNESS_MIN_STEP_PERCENT: Int = 3
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
    @Volatile
    private var aodBrightnessPercent: Int? = null
    @Volatile
    private var aodWakeFadeMillis: Int = 500
    private var aodRestoreBrightnessPercent: Int? = null
    private val brightnessRequestId = AtomicLong()
    @Volatile
    private var brightnessFadeJob: Job? = null

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
                .collect {
                    if (aodBrightnessPercent == null && brightnessFadeJob?.isActive != true) {
                        applyBrightness(it.targetBrightnessPercent)
                    }
                }
        }
    }

    fun reportUserActivity(reason: WakeReason = WakeReason.USER) {
        mutableState.value = engine.onUserActivity(reason)
    }

    fun showScreensaverNow() {
        mutableState.value = engine.showScreensaver()
    }

    fun setAodBrightnessOverride(percent: Int?, wakeFadeMillis: Int = 500) {
        val safePercent = percent?.coerceIn(1, 100)
        if (safePercent == 100) {
            if (aodBrightnessPercent == null) return
            aodBrightnessPercent = null
            val preAodBrightness = aodRestoreBrightnessPercent
            aodRestoreBrightnessPercent = null
            applyBrightness(
                PanelBrightnessFade.normalTarget(mutableState.value.targetBrightnessPercent, preAodBrightness),
            )
            return
        }
        if (safePercent != null) {
            if (aodBrightnessPercent == null) {
                aodRestoreBrightnessPercent = mutableState.value.targetBrightnessPercent
                    ?: hardware.runtimeState.value.screenBrightnessPercent
            }
            aodBrightnessPercent = safePercent
            aodWakeFadeMillis = wakeFadeMillis.coerceIn(0, 2_000)
            applyBrightness(safePercent)
            return
        }
        val aodBrightness = aodBrightnessPercent ?: return
        aodBrightnessPercent = null
        val preAodBrightness = aodRestoreBrightnessPercent
        fadeBrightness(
            fromPercent = aodBrightness,
            toPercent = PanelBrightnessFade.normalTarget(mutableState.value.targetBrightnessPercent, preAodBrightness),
            preAodPercent = preAodBrightness,
            durationMillis = aodWakeFadeMillis,
        )
        aodRestoreBrightnessPercent = null
    }

    private suspend fun publish(next: PanelScreenState) {
        mutableState.value = next
    }

    private fun fadeBrightness(fromPercent: Int, toPercent: Int?, preAodPercent: Int?, durationMillis: Int) {
        val target = toPercent ?: return applyBrightness(null)
        val values = PanelBrightnessFade.values(fromPercent, target, durationMillis)
        brightnessFadeJob?.cancel()
        val requestId = brightnessRequestId.incrementAndGet()
        val stepDelayMillis = (durationMillis / values.size).toLong()
        val job = scope.launch {
            for (percent in values) {
                if (stepDelayMillis > 0) delay(stepDelayMillis)
                if (brightnessRequestId.get() != requestId) return@launch
                applyBrightnessNow(percent)
            }
        }
        brightnessFadeJob = job
        job.invokeOnCompletion { cause ->
            if (brightnessFadeJob !== job) return@invokeOnCompletion
            brightnessFadeJob = null
            if (
                cause == null &&
                aodBrightnessPercent == null &&
                brightnessRequestId.compareAndSet(requestId, requestId + 1)
            ) {
                val completionId = requestId + 1
                val latestTarget = PanelBrightnessFade.normalTarget(
                    mutableState.value.targetBrightnessPercent,
                    preAodPercent,
                )
                scope.launch {
                    if (brightnessRequestId.get() == completionId) applyBrightnessNow(latestTarget)
                }
            }
        }
    }

    private fun applyBrightness(percent: Int?) {
        val requestId = brightnessRequestId.incrementAndGet()
        val fadeJob = brightnessFadeJob
        brightnessFadeJob = null
        fadeJob?.cancel()
        scope.launch {
            if (brightnessRequestId.get() == requestId) applyBrightnessNow(percent)
        }
    }

    private suspend fun applyBrightnessNow(percent: Int?) {
        if (percent != null && hardware.capabilities.value.supportsScreenBrightness) {
            hardware.setScreenBrightness(percent)
            mutableState.value = engine.markAppliedBrightness(percent)
            return
        }
        val targetWindow = window ?: run {
            mutableState.value = engine.markAppliedBrightness(percent)
            return
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
