package com.github.itskenny0.r1ha.core.hardware

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShellyInputKeyMapTest {
    @Test fun mapsShellyFunctionKeysToButtons() {
        assertThat(ShellyInputKeyMap.buttonIdFor(ShellyInputKeyMap.KEY_F1)).isEqualTo(1)
        assertThat(ShellyInputKeyMap.buttonIdFor(ShellyInputKeyMap.KEY_F2)).isEqualTo(2)
        assertThat(ShellyInputKeyMap.buttonIdFor(ShellyInputKeyMap.KEY_F3)).isEqualTo(3)
        assertThat(ShellyInputKeyMap.buttonIdFor(ShellyInputKeyMap.KEY_F4)).isEqualTo(4)
        assertThat(ShellyInputKeyMap.buttonIdFor(ShellyInputKeyMap.KEY_F10)).isEqualTo(5)
    }

    @Test fun mapsRelayKeySeparatelyFromButtons() {
        assertThat(ShellyInputKeyMap.relayIdFor(ShellyInputKeyMap.KEY_F11)).isEqualTo(1)
        assertThat(ShellyInputKeyMap.buttonIdFor(ShellyInputKeyMap.KEY_F11)).isNull()
    }

    @Test fun ignoresUnknownAndReservedKeys() {
        assertThat(ShellyInputKeyMap.buttonIdFor(ShellyInputKeyMap.KEY_F12)).isNull()
        assertThat(ShellyInputKeyMap.relayIdFor(ShellyInputKeyMap.KEY_F12)).isNull()
        assertThat(ShellyInputKeyMap.buttonIdFor(0)).isNull()
        assertThat(ShellyInputKeyMap.relayIdFor(0)).isNull()
    }
}
