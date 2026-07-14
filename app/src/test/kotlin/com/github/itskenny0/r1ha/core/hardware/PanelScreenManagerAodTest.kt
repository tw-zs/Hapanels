package com.github.itskenny0.r1ha.core.hardware

import androidx.test.core.app.ApplicationProvider
import com.github.itskenny0.r1ha.core.prefs.SettingsRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PanelScreenManagerAodTest {
    @Test
    fun aodBrightness100DoesNotOverrideOrRestoreHardwareBrightness() = runTest {
        val hardware = RecordingPanelHardware(initialBrightnessPercent = 74)
        val manager = manager(hardware)

        manager.setAodBrightnessOverride(100)
        runCurrent()
        manager.setAodBrightnessOverride(null)
        runCurrent()

        assertThat(hardware.brightnessRequests).isEmpty()
    }

    @Test
    fun switchingLowerAodOverrideTo100RestoresNormalBrightnessOnceWithoutFade() = runTest {
        val hardware = RecordingPanelHardware(initialBrightnessPercent = 74)
        val manager = manager(hardware)

        manager.setAodBrightnessOverride(5)
        runCurrent()
        manager.setAodBrightnessOverride(100)
        runCurrent()
        advanceTimeBy(2_000L)
        runCurrent()

        assertThat(hardware.brightnessRequests).containsExactly(5, 74).inOrder()
        assertThat(manager.state.value.appliedBrightnessPercent).isEqualTo(74)
    }

    private fun TestScope.manager(hardware: PanelHardware): PanelScreenManager =
        PanelScreenManager(
            settingsRepository = SettingsRepository.forTesting(
                context = ApplicationProvider.getApplicationContext(),
                datastoreName = "panel_screen_${System.nanoTime()}",
            ),
            hardware = hardware,
            scope = backgroundScope,
        )
}

private class RecordingPanelHardware(initialBrightnessPercent: Int) : PanelHardware {
    override val provider = PanelHardwareProvider.SHELLY_WALL_DISPLAY
    override val capabilities = MutableStateFlow(
        PanelCapabilities(
            providerLabel = provider.label,
            hardwareModel = "test",
            supportsScreenBrightness = true,
        ),
    )
    override val status = MutableStateFlow(
        PanelHardwareStatus(
            providerLabel = provider.label,
            modeLabel = "test",
            running = true,
            detail = "test",
        ),
    )
    override val runtimeState = MutableStateFlow(
        PanelHardwareRuntimeState(screenBrightnessPercent = initialBrightnessPercent),
    )
    override val events = MutableSharedFlow<PanelHardwareEvent>()
    val brightnessRequests = mutableListOf<Int>()

    override suspend fun start() = Unit
    override suspend fun stop() = Unit
    override suspend fun setRelay(id: Int, on: Boolean) = Unit
    override suspend fun setScreenBrightness(percent: Int) {
        brightnessRequests += percent
    }
    override suspend fun wakeScreen(reason: WakeReason) = Unit
}
