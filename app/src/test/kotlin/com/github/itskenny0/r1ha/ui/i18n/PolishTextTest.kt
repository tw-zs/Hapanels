package com.github.itskenny0.r1ha.ui.i18n

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class PolishTextTest {
    @Test
    fun `translates media card controls and state labels`() {
        assertThat(translateUiText("IDLE", language = "pl")).isEqualTo("BEZCZYNNE")
        assertThat(translateUiText("PAUSE", language = "pl")).isEqualTo("PAUZA")
        assertThat(translateUiText("SHUFFLE", language = "pl")).isEqualTo("LOSOWO")
        assertThat(translateUiText("REPEAT OFF", language = "pl")).isEqualTo("POWTARZANIE WYŁ.")
        assertThat(translateUiText("PAUSE 2 MEDIA", language = "pl")).isEqualTo("WSTRZYMAJ MEDIA: 2")
    }
}
