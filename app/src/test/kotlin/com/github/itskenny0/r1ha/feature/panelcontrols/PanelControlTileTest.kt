package com.github.itskenny0.r1ha.feature.panelcontrols

import com.github.itskenny0.r1ha.core.hardware.PanelCapabilities
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PanelControlTileTest {
    @Test fun availableTilesFollowCapabilities() {
        val shellyLike = PanelCapabilities(
            providerLabel = "Shelly",
            hardwareModel = "Shelly Wall Display",
            relayCount = 1,
            hasAmbientLightSensor = true,
            supportsScreenBrightness = true,
        )

        assertThat(availablePanelControlTiles(shellyLike)).containsExactly(
            PanelControlTile.Relay1,
            PanelControlTile.ScreenBrightness,
            PanelControlTile.AutoBrightness,
            PanelControlTile.AmbientLight,
            PanelControlTile.PanelStatus,
        ).inOrder()
    }

    @Test fun genericPanelKeepsOnlyAlwaysAvailableStatusTile() {
        val generic = PanelCapabilities(
            providerLabel = "Android tablet",
            hardwareModel = "Generic",
            relayCount = 0,
            hasAmbientLightSensor = false,
            supportsScreenBrightness = false,
        )

        assertThat(availablePanelControlTiles(generic)).containsExactly(PanelControlTile.PanelStatus)
    }
}
