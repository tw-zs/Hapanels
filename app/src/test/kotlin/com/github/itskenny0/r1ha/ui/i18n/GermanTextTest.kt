package com.github.itskenny0.r1ha.ui.i18n

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.jupiter.api.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

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
            assertWithMessage("Source: '$source'").that(translateUiText(source, language = "de")).isEqualTo(translation)
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

    @Test
    fun `translates reviewer highlighted messages to german`() {
        assertThat(translateUiText("No data for light.kitchen", language = "de"))
            .isEqualTo("Keine Daten für light.kitchen")
        assertThat(translateUiText("Entity light.kitchen is unavailable", language = "de"))
            .isEqualTo("Entität light.kitchen ist nicht verfügbar")
    }

    @Test
    fun `translates settings root groups to german`() {
        assertThat(translateUiText("Connection", language = "de")).isEqualTo("Verbindung")
        assertThat(translateUiText("Theme, card UI, dashboard", language = "de"))
            .isEqualTo("Design, Kartenoberfläche, Dashboard")
        assertThat(translateUiText("Search, Assist, scenes and tools", language = "de"))
            .isEqualTo("Suche, Assist, Szenen und Werkzeuge")
    }

    @Test
    fun `no regression in polish translations`() {
        assertThat(translateUiText("Welcome to Hapanels!", language = "pl")).isEqualTo("Witaj w Hapanels!")
        assertThat(translateUiText("IDLE", language = "pl")).isEqualTo("BEZCZYNNE")
    }

    @Test
    fun `static compose text literals have german translations`() {
        val untranslatedNames = setOf(
            "Hapanels",
            "HAPANELS STUDIO",
            "HOME ASSISTANT",
            "PAUSE",
            "PING",
            "Port",
            "SSL / 8883",
            "TLS",
            "tw-zs.github.io/Hapanels/",
            "+",
            "−",
            "·",
            "×",
            "✓",
            "⋯",
        )
        val literal = Regex("""\b(?:Text|MockText|EmptyText)\(\s*"([^"$]+)"""")
        val missing = File("src/main/kotlin").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                literal.findAll(file.readText()).map { match -> file to match.groupValues[1] }
            }
            .filter { (_, text) -> text !in untranslatedNames }
            .filter { (_, text) -> translateUiText(text, language = "de") == text }
            .map { (file, text) -> "${file.relativeTo(File("src/main/kotlin"))}: $text" }
            .toList()

        assertThat(missing).isEmpty()
    }

    @Test
    fun `xml string resource keys match between default, pl, and de`() {
        val rootDir = File("src/main/res")
        val enXml = File(rootDir, "values/strings.xml")
        val plXml = File(rootDir, "values-pl/strings.xml")
        val deXml = File(rootDir, "values-de/strings.xml")

        assertThat(enXml.exists()).isTrue()
        assertThat(plXml.exists()).isTrue()
        assertThat(deXml.exists()).isTrue()

        val dbf = DocumentBuilderFactory.newInstance()
        val enDoc = dbf.newDocumentBuilder().parse(enXml)
        val plDoc = dbf.newDocumentBuilder().parse(plXml)
        val deDoc = dbf.newDocumentBuilder().parse(deXml)

        fun parseKeys(doc: org.w3c.dom.Document): Map<String, String> {
            val map = mutableMapOf<String, String>()
            val nodes = doc.getElementsByTagName("string")
            for (i in 0 until nodes.length) {
                val elem = nodes.item(i) as org.w3c.dom.Element
                val name = elem.getAttribute("name")
                val translatable = elem.getAttribute("translatable")
                if (translatable != "false") {
                    map[name] = elem.textContent
                }
            }
            return map
        }

        val enKeys = parseKeys(enDoc)
        val plKeys = parseKeys(plDoc)
        val deKeys = parseKeys(deDoc)

        val missingInDe = enKeys.keys - deKeys.keys
        assertWithMessage("Keys missing in German XML").that(missingInDe).isEmpty()

        val extraInDe = deKeys.keys - enKeys.keys
        assertWithMessage("Extra keys in German XML").that(extraInDe).isEmpty()

        // Check placeholders match across EN, PL, and DE (%1$s, %2$d, etc.)
        val placeholderRegex = Regex("%[0-9]+\\$[a-z]")
        enKeys.forEach { (key, enText) ->
            val deText = deKeys[key] ?: ""
            val enPlaceholders = placeholderRegex.findAll(enText).map { it.value }.toList()
            val dePlaceholders = placeholderRegex.findAll(deText).map { it.value }.toList()
            assertWithMessage("German placeholders for key '$key'").that(dePlaceholders).isEqualTo(enPlaceholders)
        }
    }
}
