package com.github.itskenny0.r1ha.core.hardware

import android.content.Context
import android.os.Build
import com.github.itskenny0.r1ha.core.ha.HaRepository
import com.github.itskenny0.r1ha.core.prefs.HardwareProviderMode
import com.github.itskenny0.r1ha.core.prefs.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PanelHardwareController(
    context: Context,
    private val settings: SettingsRepository,
    private val haRepository: HaRepository? = null,
    private val androidFactory: (Context) -> PanelHardware = { AndroidTabletHardware(it) },
    private val shellyFactory: (Context) -> PanelHardware = { ShellyWallDisplayHardware(it, settings, haRepository) },
    private val shellyDetector: () -> Boolean = { looksLikeShellyDevice() },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : PanelHardware {
    private val appContext = context.applicationContext
    private val eventBus = MutableSharedFlow<PanelHardwareEvent>(
        replay = 32,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private var activeHardware: PanelHardware? = null
    private var activeMode: HardwareProviderMode? = null
    private var settingsJob: Job? = null
    private var capabilitiesForwardJob: Job? = null
    private var eventForwardJob: Job? = null
    private var runtimeForwardJob: Job? = null

    override val provider: PanelHardwareProvider
        get() = activeHardware?.provider ?: PanelHardwareProvider.ANDROID_TABLET
    override val capabilities = MutableStateFlow(AndroidTabletHardware(appContext).capabilities.value)
    override val status = MutableStateFlow(
        PanelHardwareStatus(
            providerLabel = "None",
            modeLabel = "AUTO",
            running = false,
            detail = "Waiting for settings",
        ),
    )
    override val runtimeState = MutableStateFlow(
        PanelHardwareRuntimeState(note = "Waiting for active hardware provider"),
    )
    override val events: Flow<PanelHardwareEvent> = eventBus

    override suspend fun start() {
        if (settingsJob?.isActive == true) return
        settingsJob = scope.launch {
            settings.settings
                .map { it.behavior.hardwareProviderMode }
                .distinctUntilChanged()
                .collectLatest { mode -> switchTo(mode) }
        }
    }

    override suspend fun stop() {
        settingsJob?.cancel()
        activeHardware?.stop()
        capabilitiesForwardJob?.cancel()
        eventForwardJob?.cancel()
        runtimeForwardJob?.cancel()
        settingsJob = null
        capabilitiesForwardJob = null
        eventForwardJob = null
        runtimeForwardJob = null
        activeHardware = null
        activeMode = null
        status.value = status.value.copy(
            running = false,
            detail = "Panel hardware controller stopped",
            updatedAtMillis = System.currentTimeMillis(),
        )
        eventBus.emit(PanelHardwareEvent.Lifecycle("Panel hardware controller stopped"))
    }

    override suspend fun setRelay(id: Int, on: Boolean) {
        activeHardware?.setRelay(id, on)
    }

    override suspend fun setScreenBrightness(percent: Int) {
        activeHardware?.setScreenBrightness(percent)
    }

    override suspend fun wakeScreen(reason: WakeReason) {
        activeHardware?.wakeScreen(reason)
    }

    private suspend fun switchTo(mode: HardwareProviderMode) {
        val targetProvider = when (mode) {
            HardwareProviderMode.AUTO -> if (shellyDetector()) {
                PanelHardwareProvider.SHELLY_WALL_DISPLAY
            } else {
                PanelHardwareProvider.ANDROID_TABLET
            }
            HardwareProviderMode.ANDROID_TABLET -> PanelHardwareProvider.ANDROID_TABLET
            HardwareProviderMode.SHELLY_WALL_DISPLAY -> PanelHardwareProvider.SHELLY_WALL_DISPLAY
        }
        val previous = activeHardware
        val providerChanged = previous?.provider != targetProvider
        val next = if (providerChanged) {
            when (targetProvider) {
                PanelHardwareProvider.ANDROID_TABLET -> androidFactory(appContext)
                PanelHardwareProvider.SHELLY_WALL_DISPLAY -> shellyFactory(appContext)
            }
        } else {
            previous
        }
        if (providerChanged) {
            previous?.stop()
            capabilitiesForwardJob?.cancel()
            eventForwardJob?.cancel()
            runtimeForwardJob?.cancel()
            activeHardware = next
            capabilities.value = next.capabilities.value
            runtimeState.value = next.runtimeState.value
            capabilitiesForwardJob = scope.launch {
                next.capabilities.collect { capabilities.value = it }
            }
            eventForwardJob = scope.launch {
                next.events.collect { eventBus.emit(it) }
            }
            runtimeForwardJob = scope.launch {
                next.runtimeState.collect { runtimeState.value = it }
            }
            next.start()
        }
        status.value = PanelHardwareStatus(
            providerLabel = next.provider.label,
            modeLabel = mode.name,
            running = true,
            detail = if (mode == HardwareProviderMode.AUTO) {
                "Auto selected ${next.provider.label}"
            } else {
                "Manually selected ${next.provider.label}"
            },
        )
        if (providerChanged || activeMode != mode) {
            eventBus.emit(PanelHardwareEvent.Lifecycle("Hardware mode ${mode.name} selected ${next.provider.label}"))
        }
        activeMode = mode
    }

    companion object {
        private fun looksLikeShellyDevice(): Boolean {
            val parts = listOf(Build.MANUFACTURER, Build.BRAND, Build.MODEL, Build.DEVICE, Build.PRODUCT)
            return parts.any { it.contains("shelly", ignoreCase = true) || it.contains("blake", ignoreCase = true) }
        }
    }
}
