package com.github.itskenny0.r1ha.feature.panelgrid

import com.github.itskenny0.r1ha.core.ha.EntityId
import com.github.itskenny0.r1ha.core.ha.EntityState
import com.github.itskenny0.r1ha.core.ha.ServiceCall
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class PanelOptimisticStateTest {
    @Test fun `tap and hue calls update panel state immediately`() {
        val id = EntityId("light.kitchen")
        val state = EntityState(
            id = id,
            friendlyName = "Kitchen",
            area = null,
            isOn = true,
            percent = 80,
            raw = 204,
            lastChanged = Instant.EPOCH,
            isAvailable = true,
            rawState = "on",
        )

        val off = state.optimisticFor(ServiceCall.tapAction(id, isOn = true))
        assertThat(off?.isOn).isFalse()
        assertThat(off?.rawState).isEqualTo("off")

        val blue = state.optimisticFor(ServiceCall.setLightHue(id, hueDegrees = 240.0, brightnessPct = 80))
        assertThat(blue?.isOn).isTrue()
        assertThat(blue?.hue).isEqualTo(240.0)
        assertThat(blue?.lastUpdated).isGreaterThan(Instant.EPOCH)
    }
}
