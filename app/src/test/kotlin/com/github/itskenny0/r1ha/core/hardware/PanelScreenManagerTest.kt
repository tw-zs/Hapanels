package com.github.itskenny0.r1ha.core.hardware

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class PanelScreenManagerTest {

    @Test
    fun brightnessCurveClampsAndScalesLux() {
        assertThat(PanelBrightnessCurve.percentForLux(-10f, 10, 80)).isEqualTo(10)
        assertThat(PanelBrightnessCurve.percentForLux(0f, 10, 80)).isEqualTo(10)
        assertThat(PanelBrightnessCurve.percentForLux(1_000f, 10, 80)).isEqualTo(80)
        assertThat(PanelBrightnessCurve.percentForLux(null, 10, 80)).isEqualTo(80)

        val dimRoom = PanelBrightnessCurve.percentForLux(10f, 10, 80)
        val brightRoom = PanelBrightnessCurve.percentForLux(500f, 10, 80)
        assertThat(dimRoom).isGreaterThan(10)
        assertThat(brightRoom).isGreaterThan(dimRoom)
        assertThat(brightRoom).isLessThan(80)
    }

    @Test
    fun idleTimeoutMovesToScreensaverAndTouchWakes() {
        val engine = PanelScreenEngine(
            initialNowMillis = 1_000L,
            initialSettings = PanelScreenSettings(
                screensaverEnabled = true,
                screensaverTimeoutMillis = 30_000L,
            ),
        )

        assertThat(engine.onTick(30_999L).mode).isEqualTo(PanelScreenMode.ACTIVE)

        val asleep = engine.onTick(31_000L)
        assertThat(asleep.mode).isEqualTo(PanelScreenMode.SCREENSAVER)
        assertThat(asleep.lastSleepReason).isEqualTo(SleepReason.IDLE_TIMEOUT)

        val awake = engine.onUserActivity(WakeReason.USER, 32_000L)
        assertThat(awake.mode).isEqualTo(PanelScreenMode.ACTIVE)
        assertThat(awake.lastWakeReason).isEqualTo(WakeReason.USER)
        assertThat(awake.lastUserActivityAtMillis).isEqualTo(32_000L)
    }

    @Test
    fun proximityNearWakesFromScreensaverWithCooldown() {
        val engine = PanelScreenEngine(
            initialNowMillis = 0L,
            initialSettings = PanelScreenSettings(
                proximityWakeEnabled = true,
                proximityNearThresholdCm = 5f,
                screensaverEnabled = true,
                screensaverTimeoutMillis = 10_000L,
            ),
        )
        engine.onTick(10_000L)
        assertThat(engine.state.mode).isEqualTo(PanelScreenMode.SCREENSAVER)

        val near = PanelHardwareRuntimeState(proximityDistanceCm = 3f)
        val awake = engine.onRuntimeState(near, 12_000L)
        assertThat(awake.mode).isEqualTo(PanelScreenMode.ACTIVE)
        assertThat(awake.lastWakeReason).isEqualTo(WakeReason.PROXIMITY)

        val ignoredByCooldown = engine.onRuntimeState(near, 12_500L)
        assertThat(ignoredByCooldown.lastWakeAtMillis).isEqualTo(12_000L)
    }

    @Test
    fun ambientLuxUpdatesTargetBrightnessWhenEnabled() {
        val engine = PanelScreenEngine(
            initialNowMillis = 0L,
            initialSettings = PanelScreenSettings(
                autoBrightnessEnabled = true,
                minBrightnessPercent = 20,
                maxBrightnessPercent = 90,
            ),
        )

        val state = engine.onRuntimeState(PanelHardwareRuntimeState(ambientLightLux = 0f), 1_000L)
        assertThat(state.targetBrightnessPercent).isEqualTo(20)

        val bright = engine.onRuntimeState(PanelHardwareRuntimeState(ambientLightLux = 1_000f), 4_000L)
        assertThat(bright.targetBrightnessPercent).isEqualTo(90)
    }

    @Test
    fun ambientLuxChangesUseHysteresisAndMinimumStep() {
        val engine = PanelScreenEngine(
            initialNowMillis = 0L,
            initialSettings = PanelScreenSettings(
                autoBrightnessEnabled = true,
                minBrightnessPercent = 10,
                maxBrightnessPercent = 100,
            ),
        )

        val low = engine.onRuntimeState(PanelHardwareRuntimeState(ambientLightLux = 0f), 100L)
        assertThat(low.targetBrightnessPercent).isEqualTo(10)

        val heldByHysteresis = engine.onRuntimeState(PanelHardwareRuntimeState(ambientLightLux = 1_000f), 1_000L)
        assertThat(heldByHysteresis.targetBrightnessPercent).isEqualTo(10)

        val appliedAfterHysteresis = engine.onRuntimeState(PanelHardwareRuntimeState(ambientLightLux = 1_000f), 3_100L)
        assertThat(appliedAfterHysteresis.targetBrightnessPercent).isEqualTo(100)

        val heldBySmallStep = engine.onRuntimeState(PanelHardwareRuntimeState(ambientLightLux = 950f), 6_500L)
        assertThat(heldBySmallStep.targetBrightnessPercent).isEqualTo(100)
    }
}
