package com.github.itskenny0.r1ha.core.hardware

import androidx.test.core.app.ApplicationProvider
import com.github.itskenny0.r1ha.core.prefs.HardwareProviderMode
import com.github.itskenny0.r1ha.core.prefs.SettingsRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PanelHardwareControllerTest {

    private fun newRepo(): SettingsRepository =
        SettingsRepository.forTesting(
            ApplicationProvider.getApplicationContext(),
            datastoreName = "panel_hardware_${System.nanoTime()}",
        )

    @Test fun sanitizePanelSensorReadingDropsInvalidValues() {
        assertThat(sanitizePanelSensorReading(null)).isNull()
        assertThat(sanitizePanelSensorReading(Float.NaN)).isNull()
        assertThat(sanitizePanelSensorReading(Float.POSITIVE_INFINITY)).isNull()
        assertThat(sanitizePanelSensorReading(-1f)).isNull()
        assertThat(sanitizePanelSensorReading(42.5f)).isEqualTo(42.5f)
    }

    @Test fun autoToManualTabletUpdatesModeWithoutRestartingSameProvider() = runTest {
        val repo = newRepo()
        val tablet = FakePanelHardware(PanelHardwareProvider.ANDROID_TABLET)
        val shelly = FakePanelHardware(PanelHardwareProvider.SHELLY_WALL_DISPLAY)
        val controller = controller(repo, tablet, shelly, shellyDetected = false)

        controller.start()
        waitUntil { controller.status.value.modeLabel == HardwareProviderMode.AUTO.name }

        repo.update { it.copy(behavior = it.behavior.copy(hardwareProviderMode = HardwareProviderMode.ANDROID_TABLET)) }
        waitUntil { controller.status.value.modeLabel == HardwareProviderMode.ANDROID_TABLET.name }

        assertThat(controller.provider).isEqualTo(PanelHardwareProvider.ANDROID_TABLET)
        assertThat(controller.status.value.providerLabel).isEqualTo(PanelHardwareProvider.ANDROID_TABLET.label)
        assertThat(tablet.startCount).isEqualTo(1)
        assertThat(tablet.stopCount).isEqualTo(0)
        assertThat(shelly.startCount).isEqualTo(0)
    }

    @Test fun changingProviderStopsPreviousAndStartsNext() = runTest {
        val repo = newRepo()
        val tablet = FakePanelHardware(PanelHardwareProvider.ANDROID_TABLET)
        val shelly = FakePanelHardware(PanelHardwareProvider.SHELLY_WALL_DISPLAY)
        val controller = controller(repo, tablet, shelly, shellyDetected = false)

        controller.start()
        waitUntil { controller.provider == PanelHardwareProvider.ANDROID_TABLET }

        repo.update { it.copy(behavior = it.behavior.copy(hardwareProviderMode = HardwareProviderMode.SHELLY_WALL_DISPLAY)) }
        waitUntil { controller.provider == PanelHardwareProvider.SHELLY_WALL_DISPLAY }

        assertThat(controller.status.value.modeLabel).isEqualTo(HardwareProviderMode.SHELLY_WALL_DISPLAY.name)
        assertThat(controller.status.value.detail).contains("Manually selected")
        assertThat(tablet.stopCount).isEqualTo(1)
        assertThat(shelly.startCount).isEqualTo(1)
    }

    @Test fun autoSelectsShellyWhenDetectorMatches() = runTest {
        val repo = newRepo()
        val tablet = FakePanelHardware(PanelHardwareProvider.ANDROID_TABLET)
        val shelly = FakePanelHardware(PanelHardwareProvider.SHELLY_WALL_DISPLAY)
        val controller = controller(repo, tablet, shelly, shellyDetected = true)

        controller.start()
        waitUntil { controller.provider == PanelHardwareProvider.SHELLY_WALL_DISPLAY }

        assertThat(controller.status.value.modeLabel).isEqualTo(HardwareProviderMode.AUTO.name)
        assertThat(controller.status.value.detail).contains("Auto selected")
        assertThat(tablet.startCount).isEqualTo(0)
        assertThat(shelly.startCount).isEqualTo(1)
    }

    @Test fun stopCancelsSettingsCollection() = runTest {
        val repo = newRepo()
        val tablet = FakePanelHardware(PanelHardwareProvider.ANDROID_TABLET)
        val shelly = FakePanelHardware(PanelHardwareProvider.SHELLY_WALL_DISPLAY)
        val controller = controller(repo, tablet, shelly, shellyDetected = false)

        controller.start()
        waitUntil { controller.provider == PanelHardwareProvider.ANDROID_TABLET }
        controller.stop()
        repo.update { it.copy(behavior = it.behavior.copy(hardwareProviderMode = HardwareProviderMode.SHELLY_WALL_DISPLAY)) }
        delay(100)

        assertThat(controller.status.value.running).isFalse()
        assertThat(shelly.startCount).isEqualTo(0)
    }

    @Test fun forwardsCapabilityUpdatesFromActiveProvider() = runTest {
        val repo = newRepo()
        val tablet = FakePanelHardware(PanelHardwareProvider.ANDROID_TABLET)
        val shelly = FakePanelHardware(PanelHardwareProvider.SHELLY_WALL_DISPLAY)
        val controller = controller(repo, tablet, shelly, shellyDetected = false)

        controller.start()
        waitUntil { controller.provider == PanelHardwareProvider.ANDROID_TABLET }
        tablet.capabilities.value = tablet.capabilities.value.copy(relayCount = 2)

        waitUntil { controller.capabilities.value.relayCount == 2 }
    }

    private fun TestScope.controller(
        repo: SettingsRepository,
        tablet: FakePanelHardware,
        shelly: FakePanelHardware,
        shellyDetected: Boolean,
    ): PanelHardwareController = PanelHardwareController(
        context = ApplicationProvider.getApplicationContext(),
        settings = repo,
        androidFactory = { tablet },
        shellyFactory = { shelly },
        shellyDetector = { shellyDetected },
        scope = backgroundScope,
    )

    private suspend fun waitUntil(predicate: () -> Boolean) {
        withTimeout(5_000) {
            while (!predicate()) delay(10)
        }
    }
}

private class FakePanelHardware(
    override val provider: PanelHardwareProvider,
) : PanelHardware {
    override val capabilities = MutableStateFlow(
        PanelCapabilities(
            providerLabel = provider.label,
            hardwareModel = "fake-${provider.name.lowercase()}",
        ),
    )
    override val status = MutableStateFlow(
        PanelHardwareStatus(
            providerLabel = provider.label,
            modeLabel = provider.name,
            running = false,
            detail = "Fake provider not started",
        ),
    )
    override val runtimeState = MutableStateFlow(PanelHardwareRuntimeState(note = "fake"))
    override val events = MutableSharedFlow<PanelHardwareEvent>(replay = 16)
    var startCount = 0
        private set
    var stopCount = 0
        private set

    override suspend fun start() {
        startCount += 1
        status.value = status.value.copy(running = true, detail = "Fake provider started")
        events.emit(PanelHardwareEvent.Lifecycle("${provider.label} fake started"))
    }

    override suspend fun stop() {
        stopCount += 1
        status.value = status.value.copy(running = false, detail = "Fake provider stopped")
        events.emit(PanelHardwareEvent.Lifecycle("${provider.label} fake stopped"))
    }

    override suspend fun setRelay(id: Int, on: Boolean) = Unit
    override suspend fun setScreenBrightness(percent: Int) = Unit
    override suspend fun wakeScreen(reason: WakeReason) = Unit
}
