package com.github.itskenny0.r1ha.ui.i18n

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class GermanTextTest {
    @Test
    fun `translates onboarding welcome and personalization to german`() {
        val expected = mapOf(
            "Welcome to Hapanels!" to "Willkommen bei Hapanels!",
            "Your home. One simple panel." to "Dein Zuhause. Ein einfaches Panel.",
            "Your home.\nOne simple panel." to "Dein Zuhause.\nEin einfaches Panel.",
            "Connect Home Assistant, name this tablet, and choose what opens first." to
                "Verbinde Home Assistant, benenne das Tablet und wähle den Startbildschirm.",
            "START SETUP" to "EINRICHTUNG STARTEN",
            "‹  BACK" to "‹  ZURÜCK",
            "01 · CONNECTION" to "01 · VERBINDUNG",
            "01 · LINK" to "01 · VERBINDUNG",
            "02 · AUTHORISE" to "02 · AUTORISIERUNG",
            "03 · PERSONALISE" to "03 · PERSONALISIERUNG",
            "03 · PANEL NAME" to "03 · PANEL-NAME",
            "04 · APPEARANCE" to "04 · ERSCHEINUNGSBILD",
            "Make it yours." to "Pass dein Panel an.",
            "TABLET NAME" to "TABLET-NAME",
            "START VIEW" to "STARTANSICHT",
            "GRID" to "RASTER",
            "CARDS" to "KARTEN",
            "OPEN HAPANELS" to "HAPANELS ÖFFNEN",
            "SAVE AND CONTINUE" to "SPEICHERN UND WEITER",
            "Use a long-lived token instead" to "Stattdessen langlebigen Zugriffstoken verwenden",
        )

        expected.forEach { (source, translation) ->
            assertThat(translateUiText(source, language = "de")).isEqualTo(translation)
        }
    }

    @Test
    fun `translates media card controls and state labels to german`() {
        assertThat(translateUiText("IDLE", language = "de")).isEqualTo("INAKTIV")
        assertThat(translateUiText("PAUSE", language = "de")).isEqualTo("PAUSE")
        assertThat(translateUiText("SHUFFLE", language = "de")).isEqualTo("ZUFALLSWIEDERGABE")
        assertThat(translateUiText("REPEAT OFF", language = "de")).isEqualTo("WIEDERHOLUNG AUS")
        assertThat(translateUiText("PAUSE 2 MEDIA", language = "de")).isEqualTo("MEDIA PAUSIEREN: 2")
    }
}
