package com.github.itskenny0.r1ha.ui.i18n

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class PolishTextTest {
    @Test
    fun `translates onboarding welcome and personalization`() {
        val expected = mapOf(
            "Welcome to Hapanels!" to "Witaj w Hapanels!",
            "Your home. One simple panel." to "Twój dom. Jeden prosty panel.",
            "Your home.\nOne simple panel." to "Twój dom.\nJeden prosty panel.",
            "Connect Home Assistant, name this tablet, and choose what opens first." to
                "Połącz Home Assistant, nazwij tablet i wybierz ekran startowy.",
            "START SETUP" to "ROZPOCZNIJ KONFIGURACJĘ",
            "‹  BACK" to "‹  COFNIJ",
            "01 · CONNECTION" to "01 · POŁĄCZENIE",
            "01 · LINK" to "01 · POŁĄCZENIE",
            "02 · AUTHORISE" to "02 · AUTORYZACJA",
            "03 · PERSONALISE" to "03 · PERSONALIZACJA",
            "03 · PANEL NAME" to "03 · NAZWA PANELU",
            "04 · APPEARANCE" to "04 · WYGLĄD",
            "Make it yours." to "Dostosuj panel.",
            "TABLET NAME" to "NAZWA TABLETU",
            "START VIEW" to "EKRAN STARTOWY",
            "GRID" to "SIATKA",
            "CARDS" to "KARTY",
            "OPEN HAPANELS" to "OTWÓRZ HAPANELS",
            "SAVE AND CONTINUE" to "ZAPISZ I KONTYNUUJ",
            "Use a long-lived token instead" to "Użyj tokena długoterminowego",
        )

        expected.forEach { (source, translation) ->
            assertThat(translateUiText(source, language = "pl")).isEqualTo(translation)
        }
    }

    @Test
    fun `translates media card controls and state labels`() {
        assertThat(translateUiText("IDLE", language = "pl")).isEqualTo("BEZCZYNNE")
        assertThat(translateUiText("PAUSE", language = "pl")).isEqualTo("PAUZA")
        assertThat(translateUiText("SHUFFLE", language = "pl")).isEqualTo("LOSOWO")
        assertThat(translateUiText("REPEAT OFF", language = "pl")).isEqualTo("POWTARZANIE WYŁ.")
        assertThat(translateUiText("PAUSE 2 MEDIA", language = "pl")).isEqualTo("WSTRZYMAJ MEDIA: 2")
    }
}
