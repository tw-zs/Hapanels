package com.github.itskenny0.r1ha.core.hardware

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PanelButtonPressDetectorTest {
    @Test fun emitsDownUpAndShortOnSinglePressFlush() {
        val events = mutableListOf<PanelHardwareEvent.Button>()
        val detector = PanelButtonPressDetector(emit = { events.add(it) })

        detector.onDown(buttonId = 1, nowMs = 1_000)
        detector.onUp(buttonId = 1, nowMs = 1_050)
        detector.flush()

        assertThat(events.map { it.type })
            .containsExactly(
                PanelButtonPressType.DOWN,
                PanelButtonPressType.UP,
                PanelButtonPressType.SHORT,
            )
            .inOrder()
        assertThat(events.map { it.buttonId }).containsExactly(1, 1, 1).inOrder()
    }

    @Test fun emitsShortImmediatelyWhenCallerKnowsNoMultiClickActionExists() {
        val events = mutableListOf<PanelHardwareEvent.Button>()
        val detector = PanelButtonPressDetector(emit = { events.add(it) })

        detector.onDown(buttonId = 1, nowMs = 1_000)
        detector.onUp(buttonId = 1, nowMs = 1_050, emitShortImmediately = true)

        assertThat(events.map { it.type })
            .containsExactly(
                PanelButtonPressType.DOWN,
                PanelButtonPressType.UP,
                PanelButtonPressType.SHORT,
            )
            .inOrder()
    }

    @Test fun emitsLongInsteadOfShortWhenHeldPastThreshold() {
        val events = mutableListOf<PanelHardwareEvent.Button>()
        val detector = PanelButtonPressDetector(emit = { events.add(it) })

        detector.onDown(buttonId = 2, nowMs = 1_000)
        detector.onUp(buttonId = 2, nowMs = 2_100)
        detector.flush()

        assertThat(events.map { it.type })
            .containsExactly(
                PanelButtonPressType.DOWN,
                PanelButtonPressType.UP,
                PanelButtonPressType.LONG,
            )
            .inOrder()
    }

    @Test fun collapsesFastSequentialPressesIntoDoubleAndTriple() {
        val events = mutableListOf<PanelHardwareEvent.Button>()
        val detector = PanelButtonPressDetector(emit = { events.add(it) })

        detector.onDown(buttonId = 3, nowMs = 1_000)
        detector.onUp(buttonId = 3, nowMs = 1_050)
        detector.onDown(buttonId = 3, nowMs = 1_200)
        detector.onUp(buttonId = 3, nowMs = 1_250)
        detector.flush()

        assertThat(events.last().type).isEqualTo(PanelButtonPressType.DOUBLE)

        events.clear()
        detector.onDown(buttonId = 4, nowMs = 2_000)
        detector.onUp(buttonId = 4, nowMs = 2_050)
        detector.onDown(buttonId = 4, nowMs = 2_200)
        detector.onUp(buttonId = 4, nowMs = 2_250)
        detector.onDown(buttonId = 4, nowMs = 2_400)
        detector.onUp(buttonId = 4, nowMs = 2_450)
        detector.flush()

        assertThat(events.last().type).isEqualTo(PanelButtonPressType.TRIPLE)
    }
}
