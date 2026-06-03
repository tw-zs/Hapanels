package com.github.itskenny0.r1ha.core.hardware

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ShellyRelayStateStoreTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun missingRelayFileMeansNoLocalRelay() {
        val relayFile = temp.root.resolve("missing-relay")

        assertThat(ShellyRelayStateStore.readRelayStates(relayFile)).isEmpty()
    }

    @Test fun readsRelayOneStateFromSysfsValue() {
        val relayFile = temp.newFile("relay1")

        relayFile.writeText("1\n")
        assertThat(ShellyRelayStateStore.readRelayStates(relayFile)).containsExactly(1, true)

        relayFile.writeText("0\n")
        assertThat(ShellyRelayStateStore.readRelayStates(relayFile)).containsExactly(1, false)
    }

    @Test fun writesRelayOneStateAsSysfsValue() {
        val relayFile = temp.newFile("relay1")

        ShellyRelayStateStore.writeRelay(1, true, relayFile)
        assertThat(relayFile.readText()).isEqualTo("1")

        ShellyRelayStateStore.writeRelay(1, false, relayFile)
        assertThat(relayFile.readText()).isEqualTo("0")
    }
}
