package com.github.itskenny0.r1ha.ui.i18n

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

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
            "Use a long-lived token instead" to "Użyj długoterminowego tokena dostępu",
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

    @Test
    fun `translates reviewer highlighted messages`() {
        assertThat(translateUiText("No data for light.kitchen", language = "pl"))
            .isEqualTo("Brak danych dla light.kitchen")
        assertThat(translateUiText("Entity light.kitchen is unavailable", language = "pl"))
            .isEqualTo("Encja light.kitchen jest niedostępna")
    }

    @Test
    fun `static compose text literals have polish translations`() {
        val untranslatedNames = setOf(
            "Hapanels",
            "HAPANELS STUDIO",
            "HOME ASSISTANT",
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
            .filter { (_, text) -> translateUiText(text, language = "pl") == text }
            .map { (file, text) -> "${file.relativeTo(File("src/main/kotlin"))}: $text" }
            .toList()

        assertThat(missing).isEmpty()
    }

    @Test
    fun `xml string resource keys match between default and pl`() {
        val rootDir = File("src/main/res")
        val enXml = File(rootDir, "values/strings.xml")
        val plXml = File(rootDir, "values-pl/strings.xml")

        assertThat(enXml.exists()).isTrue()
        assertThat(plXml.exists()).isTrue()

        val dbf = DocumentBuilderFactory.newInstance()
        val enDoc = dbf.newDocumentBuilder().parse(enXml)
        val plDoc = dbf.newDocumentBuilder().parse(plXml)

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

        val missingInPl = enKeys.keys - plKeys.keys
        assertThat(missingInPl).isEmpty()

        val extraInPl = plKeys.keys - enKeys.keys
        assertThat(extraInPl).isEmpty()

        // Check placeholders match (%1$s, %2$d, etc.)
        val placeholderRegex = Regex("%[0-9]+\\$[a-z]")
        enKeys.forEach { (key, enText) ->
            val plText = plKeys[key] ?: ""
            val enPlaceholders = placeholderRegex.findAll(enText).map { it.value }.toList()
            val plPlaceholders = placeholderRegex.findAll(plText).map { it.value }.toList()
            com.google.common.truth.Truth.assertWithMessage("Placeholders for key '$key'").that(plPlaceholders).isEqualTo(enPlaceholders)
        }
    }
}
