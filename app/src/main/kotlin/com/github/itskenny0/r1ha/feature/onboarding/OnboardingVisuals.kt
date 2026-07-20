package com.github.itskenny0.r1ha.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.itskenny0.r1ha.R
import com.github.itskenny0.r1ha.core.prefs.StartView
import com.github.itskenny0.r1ha.ui.components.r1Pressable
import kotlin.math.min

internal val OnboardingBg = Color(0xFF0A0A0A)
internal val OnboardingSurface = Color(0xFF141414)
internal val OnboardingRule = Color(0xFF383838)
internal val OnboardingInk = Color(0xFFEDEDED)
internal val OnboardingSoft = Color(0xFFA8A8A8)
internal val OnboardingOrange = Color(0xFFF36F21)
internal val OnboardingGreen = Color(0xFF48D27A)
internal val OnboardingRed = Color(0xFFE5504A)

@Composable
internal fun OnboardingBackdrop(showPhoto: Boolean) {
    Box(Modifier.fillMaxSize().background(OnboardingBg)) {
        if (showPhoto) {
            Image(
                painter = painterResource(R.drawable.hapanels_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 120f / 255f)))
        }
    }
}

@Composable
internal fun ScaledOnboardingPage(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val scale = min(maxWidth.value / 1280f, maxHeight.value / 752f)
        Box(
            modifier = Modifier.size(1280.dp * scale, 752.dp * scale),
            contentAlignment = Alignment.TopStart,
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .requiredSize(1280.dp, 752.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                    },
                content = content,
            )
        }
    }
}

@Composable
internal fun WelcomePage(onStart: () -> Unit) {
    ScaledOnboardingPage {
        Image(
            painter = painterResource(R.drawable.hapanels_logo),
            contentDescription = "Hapanels",
            modifier = Modifier.offset(508.dp, 82.dp).size(264.dp),
        )
        MockText("Witaj w Hapanels!", 0.dp, 277.dp, 1280.dp, 46, bold = true, align = Alignment.CenterHorizontally)
        MockText("Twój dom. Jeden prosty panel.", 0.dp, 341.dp, 1280.dp, 24, OnboardingSoft, align = Alignment.CenterHorizontally)
        MockText(
            "Steruj światłem, temperaturą, roletami i innymi urządzeniami z jednego ekranu.",
            0.dp,
            411.dp,
            1280.dp,
            19,
            align = Alignment.CenterHorizontally,
        )
        MockText(
            "Za chwilę połączysz panel ze swoim domem i wybierzesz, co ma się na nim wyświetlać.",
            0.dp,
            447.dp,
            1280.dp,
            18,
            OnboardingSoft,
            align = Alignment.CenterHorizontally,
        )
        MockButton("Rozpocznij konfigurację", 420.dp, 580.dp, 440.dp, 68.dp, onClick = onStart)
    }
}

@Composable
internal fun ConnectionPage(
    url: String,
    onUrlChange: (String) -> Unit,
    detectedServer: String?,
    probing: Boolean,
    error: String?,
    onBack: () -> Unit,
    onOAuth: () -> Unit,
    onLlat: () -> Unit,
) {
    StandardPage(
        step = "01 · POŁĄCZENIE",
        title = "Połącz panel z Home Assistant",
        description = "Wybierz wykryty serwer albo wpisz jego adres ręcznie.",
        onBack = onBack,
    ) {
        MockText("WYKRYTE W SIECI LOKALNEJ", 240.dp, 228.dp, 800.dp, 15, OnboardingSoft, bold = true, letterSpacing = 1.2f)
        if (detectedServer == null) {
            MockCard(240.dp, 260.dp, 800.dp, 58.dp)
            MockText("Szukam Home Assistant w sieci lokalnej…", 265.dp, 277.dp, 750.dp, 18, OnboardingSoft)
        } else {
            MockCard(240.dp, 260.dp, 800.dp, 58.dp, selected = true)
            MockText("Zapisany Home Assistant", 258.dp, 269.dp, 700.dp, 17, bold = true)
            MockText(detectedServer, 258.dp, 294.dp, 700.dp, 13, OnboardingSoft)
            MockText("✓", 985.dp, 272.dp, 30.dp, 18, OnboardingOrange, bold = true)
        }
        MockText("LUB WPISZ ADRES RĘCZNIE", 240.dp, 390.dp, 800.dp, 15, OnboardingSoft, bold = true, letterSpacing = 1.2f)
        MockField(
            value = url,
            onValueChange = onUrlChange,
            placeholder = "http://homeassistant.local:8123",
            x = 240.dp,
            y = 422.dp,
            width = 800.dp,
            height = 58.dp,
            error = error != null,
        )
        MockButton(
            if (probing) "ŁĄCZENIE…" else "POŁĄCZ ZA POMOCĄ HASŁA",
            240.dp,
            500.dp,
            800.dp,
            58.dp,
            enabled = !probing && (url.isNotBlank() || detectedServer != null),
            onClick = onOAuth,
        )
        MockButton(
            "POŁĄCZ ZA POMOCĄ TOKENA DŁUGOTERMINOWEGO",
            240.dp,
            570.dp,
            800.dp,
            58.dp,
            outlined = true,
            onClick = onLlat,
        )
        if (error != null) MockText(error, 240.dp, 640.dp, 800.dp, 14, OnboardingRed, align = Alignment.CenterHorizontally)
    }
}

@Composable
internal fun AuthPage(
    title: String,
    description: String,
    onBack: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    StandardPage("02 · LOGOWANIE", title, description, onBack, content)
}

@Composable
internal fun PanelNamePage(
    value: String,
    onValueChange: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    StandardPage(
        "03 · NAZWA PANELU",
        "Jak nazwać ten tablet?",
        "Nazwa pomoże rozpoznać panel w Home Assistant, MQTT i Hapanels Studio.",
        onBack,
    ) {
        MockText("NAZWA URZĄDZENIA", 240.dp, 300.dp, 800.dp, 15, OnboardingSoft, bold = true, letterSpacing = 1.2f)
        MockField(value, onValueChange, "Panel Hapanels", 240.dp, 332.dp, 800.dp, 74.dp, selected = true)
        MockText("Android rozpoznał urządzenie jako: ${android.os.Build.MODEL}", 240.dp, 429.dp, 800.dp, 16, OnboardingSoft)
        MockButton("ZAPISZ NAZWĘ I KONTYNUUJ", 420.dp, 580.dp, 440.dp, 68.dp, enabled = value.isNotBlank(), onClick = onContinue)
        MockText("Nazwę można później zmienić w ustawieniach.", 0.dp, 674.dp, 1280.dp, 15, OnboardingSoft, align = Alignment.CenterHorizontally)
    }
}

@Composable
internal fun AppearancePage(
    dark: Boolean,
    startView: StartView,
    loading: Boolean,
    error: String?,
    onDarkChange: (Boolean) -> Unit,
    onStartViewChange: (StartView) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    StandardPage(
        "04 · WYGLĄD",
        "Dopasuj panel do siebie",
        "Nie musisz decydować na zawsze — wszystko zmienisz później w ustawieniach.",
        onBack,
    ) {
        MockText("TRYB KOLORÓW", 120.dp, 226.dp, 600.dp, 15, OnboardingSoft, bold = true, letterSpacing = 1.2f)
        ColorChoice("Jasny", false, dark, 120.dp) { onDarkChange(false) }
        ColorChoice("Ciemny", true, dark, 400.dp) { onDarkChange(true) }
        MockText("DOMYŚLNY WIDOK APLIKACJI", 120.dp, 367.dp, 900.dp, 15, OnboardingSoft, bold = true, letterSpacing = 1.2f)
        ViewChoice(StartView.PANEL_GRID, startView, 120.dp, onStartViewChange)
        ViewChoice(StartView.CARDS, startView, 640.dp, onStartViewChange)
        if (error != null) {
            MockText(error, 120.dp, 548.dp, 1040.dp, 14, OnboardingRed, align = Alignment.CenterHorizontally)
        }
        MockButton(
            if (error != null) "POMIŃ MOTYW I KONTYNUUJ" else "ZAPISZ I KONTYNUUJ",
            420.dp,
            580.dp,
            440.dp,
            68.dp,
            enabled = !loading,
            onClick = onContinue,
        )
    }
}

@Composable
internal fun StudioPage(
    serverName: String,
    tabletName: String,
    infoOpen: Boolean,
    onInfoChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
) {
    StandardPage(
        "05 · HAPANELS STUDIO",
        "Połącz panel z Hapanels Studio",
        "Studio pozwala układać widoki panelu bez ręcznego edytowania konfiguracji.",
        onBack,
    ) {
        MockCard(240.dp, 264.dp, 800.dp, 134.dp)
        MockText("HOME ASSISTANT", 270.dp, 285.dp, 700.dp, 14, OnboardingSoft, bold = true, letterSpacing = 1.2f)
        MockText("$serverName · $tabletName", 270.dp, 313.dp, 700.dp, 23, bold = true)
        MockText("Integracja Studio nie jest jeszcze skonfigurowana", 270.dp, 354.dp, 700.dp, 16, OnboardingSoft)
        MockText("STUDIO", 915.dp, 313.dp, 93.dp, 18, OnboardingOrange, bold = true, align = Alignment.End)
        MockText(
            "Po połączeniu ustawienia wyglądu i widoków będą dostępne bezpośrednio w Home Assistant.",
            0.dp,
            435.dp,
            1280.dp,
            17,
            OnboardingSoft,
            align = Alignment.CenterHorizontally,
        )
        MockButton("PLANOWANE", 240.dp, 525.dp, 800.dp, 64.dp, enabled = false, onClick = {})
        MockButton("POMIŃ NA RAZIE", 240.dp, 615.dp, 385.dp, 56.dp, outlined = true, mutedOutline = true, onClick = onSkip)
        MockButton("DOWIEDZ SIĘ WIĘCEJ", 655.dp, 615.dp, 385.dp, 56.dp, outlined = true, onClick = { onInfoChange(true) })
        MockText("Połączenie ze Studio można dodać później w ustawieniach.", 0.dp, 681.dp, 1280.dp, 15, OnboardingSoft, align = Alignment.CenterHorizontally)
        if (infoOpen) StudioInfoPopup { onInfoChange(false) }
    }
}

@Composable
internal fun MqttPage(onBack: () -> Unit, onSkip: () -> Unit) {
    StandardPage(
        "06 · MQTT · OPCJONALNIE",
        "Dodaj broker MQTT",
        "Hapanels może publikować stan panelu i odbierać polecenia przez MQTT.",
        onBack,
    ) {
        MockText("ADRES BROKERA", 180.dp, 233.dp, 430.dp, 14, OnboardingSoft, bold = true)
        MockField("", {}, "192.168.1.10", 180.dp, 263.dp, 430.dp, 58.dp, enabled = false)
        MockButton("UŻYJ IP HA", 625.dp, 263.dp, 155.dp, 58.dp, outlined = true, enabled = false, onClick = {})
        MockText("PORT", 810.dp, 233.dp, 290.dp, 14, OnboardingSoft, bold = true)
        MockField("", {}, "1883", 810.dp, 263.dp, 290.dp, 58.dp, enabled = false)
        MockText("UŻYTKOWNIK", 180.dp, 357.dp, 440.dp, 14, OnboardingSoft, bold = true)
        MockField("", {}, "Opcjonalnie", 180.dp, 387.dp, 440.dp, 58.dp, enabled = false)
        MockText("HASŁO", 650.dp, 357.dp, 450.dp, 14, OnboardingSoft, bold = true)
        MockField("", {}, "Nie jest zapisywane", 650.dp, 387.dp, 450.dp, 58.dp, enabled = false)
        MockButton("TLS WYŁĄCZONY", 180.dp, 482.dp, 230.dp, 54.dp, outlined = true, mutedOutline = true, enabled = false, onClick = {})
        MockButton("POMIŃ MQTT I KONTYNUUJ", 420.dp, 580.dp, 440.dp, 68.dp, onClick = onSkip)
        MockText("Konfigurację MQTT dodasz później, gdy bezpieczne przechowywanie będzie gotowe.", 0.dp, 681.dp, 1280.dp, 15, OnboardingSoft, align = Alignment.CenterHorizontally)
    }
}

@Composable
internal fun ChecklistPage(
    haConnected: Boolean,
    dark: Boolean,
    startView: StartView,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onLaunch: () -> Unit,
) {
    StandardPage(
        "07 · GOTOWE",
        "Sprawdźmy, czy wszystko działa",
        "Ostatnia kontrola przed uruchomieniem panelu.",
        onBack,
    ) {
        ChecklistRow("HOME ASSISTANT", if (haConnected) "Połączenie działa" else "Oczekiwanie na połączenie", if (haConnected) OnboardingGreen else OnboardingOrange, 248.dp)
        ChecklistRow("HAPANELS STUDIO", "Pominięto na tym urządzeniu", OnboardingSoft, 326.dp)
        ChecklistRow("WYGLĄD", "${if (dark) "Ciemny" else "Jasny"} · ${if (startView == StartView.PANEL_GRID) "Hapanels Grid" else "Karty"}", OnboardingGreen, 404.dp)
        ChecklistRow("MQTT", "Pominięto", OnboardingSoft, 482.dp)
        MockButton(
            if (haConnected) "URUCHOM HAPANELS" else "SPRAWDŹ PONOWNIE",
            420.dp,
            580.dp,
            440.dp,
            68.dp,
            onClick = if (haConnected) onLaunch else onRetry,
        )
    }
}

@Composable
internal fun ProgressEdge(progress: Float, successGlow: Float) {
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        if (progress <= 0f) return@Canvas
        val inset = 4.dp.toPx()
        val path = Path().apply {
            moveTo(size.width / 2f, inset)
            lineTo(size.width - inset, inset)
            lineTo(size.width - inset, size.height - inset)
            lineTo(inset, size.height - inset)
            lineTo(inset, inset)
            lineTo(size.width / 2f, inset)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val visible = Path()
        measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), visible, true)
        drawPath(
            visible,
            Color(
                red = OnboardingOrange.red + (OnboardingGreen.red - OnboardingOrange.red) * successGlow,
                green = OnboardingOrange.green + (OnboardingGreen.green - OnboardingOrange.green) * successGlow,
                blue = OnboardingOrange.blue + (OnboardingGreen.blue - OnboardingOrange.blue) * successGlow,
            ),
            style = Stroke(
                width = (5f + 3f * successGlow) * density,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

@Composable
internal fun LaunchSequence(progress: Float, tabletName: String, startView: StartView) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val panelAlpha = smoothStep(0.82f, 1f, progress)
        if (panelAlpha > 0f) LaunchPanelPreview(tabletName, startView, panelAlpha)
        ScaledOnboardingPage {
            val logoAlpha = windowAlpha(progress, 0.15f, 0.36f, 0.76f, 0.84f)
            Image(
                painter = painterResource(R.drawable.hapanels_logo),
                contentDescription = null,
                modifier = Modifier.offset(510.dp, 134.dp).size(260.dp).alpha(logoAlpha),
            )
            MockText(
                "Wszystko skonfigurowane!",
                0.dp,
                427.dp,
                1280.dp,
                34,
                OnboardingInk.copy(alpha = windowAlpha(progress, 0.34f, 0.43f, 0.50f, 0.56f)),
                bold = true,
                align = Alignment.CenterHorizontally,
            )
            MockText(
                "Witaj w swoim domu!",
                0.dp,
                427.dp,
                1280.dp,
                34,
                OnboardingInk.copy(alpha = windowAlpha(progress, 0.54f, 0.62f, 0.71f, 0.80f)),
                bold = true,
                align = Alignment.CenterHorizontally,
            )
        }
        if (progress < 0.21f) {
            val q = 1f - (1f - (progress / 0.18f).coerceIn(0f, 1f)).let { it * it * it }
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val left = 4.dp.toPx() + (size.width / 2f - 4.dp.toPx()) * q
                val top = 4.dp.toPx() + (size.height / 2f - 4.dp.toPx()) * q
                drawRect(
                    OnboardingOrange,
                    topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(size.width - 2f * left, size.height - 2f * top),
                    style = Stroke(6.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun BoxScope.LaunchPanelPreview(tabletName: String, startView: StartView, alpha: Float) {
    ScaledOnboardingPage(Modifier.alpha(alpha)) {
        Box(Modifier.fillMaxSize().background(OnboardingBg))
        MockText(tabletName, 48.dp, 18.dp, 300.dp, 21, bold = true)
        MockText("21:37", 1050.dp, 18.dp, 182.dp, 20, OnboardingSoft, bold = true, align = Alignment.End)
        Box(Modifier.offset(470.dp, 0.dp).size(340.dp, 42.dp).background(OnboardingOrange))
        MockText(
            "TRYB MOCKUP · DANE DEMONSTRACYJNE",
            470.dp,
            16.dp,
            340.dp,
            14,
            OnboardingBg,
            bold = true,
            align = Alignment.CenterHorizontally,
            letterSpacing = 0.8f,
        )
        Box(Modifier.offset(48.dp, 58.dp).size(1184.dp, 2.dp).background(OnboardingOrange))
        if (startView == StartView.PANEL_GRID) {
            MockText("TWÓJ DOM", 48.dp, 91.dp, 300.dp, 15, OnboardingSoft, bold = true, letterSpacing = 1.5f)
            val labels = listOf("OŚWIETLENIE", "TEMPERATURA", "ROLETY", "MEDIA", "ENERGIA", "OBECNOŚĆ")
            val values = listOf("7 włączonych", "21,5 °C", "3 otwarte", "Salon · Spotify", "1,24 kW", "2 osoby w domu")
            val accents = listOf(OnboardingOrange, Color(0xFF41BDF5), Color(0xFFAAAAAA), Color(0xFF659FFF), Color(0xFF52C77F), OnboardingOrange)
            repeat(6) { index ->
                val col = index % 3
                val row = index / 3
                val x = (48 + col * 400).dp
                val y = (136 + row * 276).dp
                MockCard(x, y, 368.dp, 244.dp)
                Box(Modifier.offset(x, y).size(5.dp, 244.dp).background(accents[index]))
                MockText(labels[index], x + 25.dp, y + 29.dp, 300.dp, 14, OnboardingSoft, bold = true, letterSpacing = 1.2f)
                MockText(values[index], x + 25.dp, y + 75.dp, 320.dp, 24, bold = true)
                MockText(if (index == 1) "KOMFORT" else "GOTOWE", x + 25.dp, y + 190.dp, 300.dp, 13, accents[index], bold = true, letterSpacing = 1f)
            }
        } else {
            MockText("KARTY · 1 / 8", 48.dp, 91.dp, 300.dp, 15, OnboardingSoft, bold = true, letterSpacing = 1.5f)
            MockCard(180.dp, 136.dp, 920.dp, 520.dp)
            Box(Modifier.offset(180.dp, 136.dp).size(7.dp, 520.dp).background(OnboardingOrange))
            MockText("SALON · OŚWIETLENIE", 225.dp, 174.dp, 700.dp, 15, OnboardingOrange, bold = true, letterSpacing = 1.5f)
            MockText("Lampa stojąca", 225.dp, 218.dp, 700.dp, 28, bold = true)
            MockText("72", 225.dp, 333.dp, 220.dp, 118, bold = true)
            MockText("%", 390.dp, 357.dp, 100.dp, 36, OnboardingSoft)
            Box(Modifier.offset(225.dp, 451.dp).size(705.dp, 13.dp).background(Color(0xFF373737)))
            Box(Modifier.offset(225.dp, 451.dp).size(510.dp, 13.dp).background(OnboardingOrange))
            MockButton("WYŁĄCZ", 790.dp, 506.dp, 245.dp, 78.dp, onClick = {})
        }
    }
}

@Composable
private fun StandardPage(
    step: String,
    title: String,
    description: String,
    onBack: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    ScaledOnboardingPage {
        Box(Modifier.fillMaxSize().background(OnboardingBg))
        MockButton("‹  Cofnij", 48.dp, 38.dp, 128.dp, 54.dp, outlined = true, mutedOutline = true, onClick = onBack)
        MockText(step, 240.dp, 96.dp, 800.dp, 16, OnboardingOrange, bold = true, letterSpacing = 1.5f)
        MockText(title, 240.dp, 123.dp, 850.dp, 36, bold = true)
        MockText(description, 240.dp, 178.dp, 900.dp, 17, OnboardingSoft)
        content()
    }
}

@Composable
private fun BoxScope.StudioInfoPopup(onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.84f)).r1Pressable(onClick = {}))
    Box(Modifier.offset(160.dp, 92.dp).size(960.dp, 584.dp).background(OnboardingSurface).border(2.dp, OnboardingOrange))
    MockText("HAPANELS STUDIO", 210.dp, 132.dp, 600.dp, 15, OnboardingOrange, bold = true, letterSpacing = 1.5f)
    MockText("Zarządzaj panelami z Home Assistant", 210.dp, 165.dp, 650.dp, 31, bold = true)
    MockText("Studio synchronizuje widoki, ustawienia i stan panelu przez MQTT.", 210.dp, 221.dp, 650.dp, 18, OnboardingSoft)
    MockText("• układaj ekrany bez edytowania plików", 210.dp, 282.dp, 560.dp, 18)
    MockText("• zarządzaj wieloma tabletami w jednym miejscu", 210.dp, 321.dp, 560.dp, 18)
    MockText("• wdrażaj zmiany bez dotykania każdego panelu", 210.dp, 358.dp, 560.dp, 18)
    MockText("Dokumentacja i instalacja:", 210.dp, 426.dp, 500.dp, 16, OnboardingSoft, bold = true)
    MockText("tw-zs.github.io/Hapanels/", 210.dp, 456.dp, 500.dp, 20, OnboardingOrange, bold = true)
    Box(Modifier.offset(792.dp, 216.dp).size(272.dp).background(Color.White))
    Image(painterResource(R.drawable.hapanels_docs_qr), null, Modifier.offset(808.dp, 232.dp).size(240.dp))
    MockText("ZESKANUJ KOD QR", 808.dp, 512.dp, 240.dp, 14, OnboardingSoft, bold = true, align = Alignment.CenterHorizontally)
    MockButton("ZAMKNIJ", 420.dp, 576.dp, 440.dp, 60.dp, onClick = onClose)
    MockText("×", 1042.dp, 117.dp, 50.dp, 34, OnboardingSoft, align = Alignment.CenterHorizontally, onClick = onClose)
}

@Composable
private fun BoxScope.ColorChoice(label: String, value: Boolean, selectedValue: Boolean, x: Dp, onClick: () -> Unit) {
    val selected = value == selectedValue
    MockCard(x, 258.dp, 250.dp, 72.dp, selected, onClick)
    Box(Modifier.offset(x + 16.dp, 274.dp).size(48.dp, 40.dp).background(if (value) Color(0xFF101010) else Color(0xFFEFEFEF)))
    MockText(if (value) "●" else "☀", x + 16.dp, 280.dp, 48.dp, 16, if (value) OnboardingInk else OnboardingOrange, bold = true, align = Alignment.CenterHorizontally)
    MockText(label, x + 82.dp, 273.dp, 140.dp, 19, bold = true)
    if (selected) MockText("WYBRANY", x + 82.dp, 303.dp, 140.dp, 12, OnboardingOrange, bold = true)
}

@Composable
private fun BoxScope.ViewChoice(value: StartView, selectedValue: StartView, x: Dp, onSelect: (StartView) -> Unit) {
    val selected = value == selectedValue
    MockCard(x, 400.dp, 480.dp, 148.dp, selected) { onSelect(value) }
    Box(Modifier.offset(x + 20.dp, 420.dp).size(150.dp, 108.dp).background(Color(0xFF090909)))
    if (value == StartView.PANEL_GRID) {
        repeat(4) { tile ->
            Box(
                Modifier
                    .offset(x + (32 + (tile % 2) * 68).dp, (432 + (tile / 2) * 46).dp)
                    .size(56.dp, 36.dp)
                    .background(if (tile == 0) OnboardingOrange else Color(0xFF303030)),
            )
        }
    } else {
        Box(Modifier.offset(x + 37.dp, 435.dp).size(116.dp, 78.dp).background(Color(0xFF1C1C1C)))
        Box(Modifier.offset(x + 51.dp, 451.dp).size(38.dp, 46.dp).background(OnboardingOrange))
        Box(Modifier.offset(x + 100.dp, 451.dp).size(38.dp, 16.dp).background(Color(0xFF464646)))
    }
    MockText(if (value == StartView.PANEL_GRID) "Hapanels Grid" else "Karty", x + 194.dp, 433.dp, 250.dp, 22, bold = true)
    MockText(if (value == StartView.PANEL_GRID) "Wszystko w jednym układzie" else "Jedno urządzenie na ekranie", x + 194.dp, 476.dp, 260.dp, 16, OnboardingSoft)
    MockText(if (selected) "WYBRANY" else "WYBIERZ", x + 194.dp, 511.dp, 200.dp, 13, if (selected) OnboardingOrange else OnboardingSoft, bold = true)
}

@Composable
private fun BoxScope.ChecklistRow(label: String, value: String, status: Color, y: Dp) {
    MockCard(240.dp, y, 800.dp, 62.dp)
    Box(Modifier.offset(240.dp, y).size(4.dp, 62.dp).background(status))
    MockText(label, 265.dp, y + 11.dp, 600.dp, 14, OnboardingSoft, bold = true)
    MockText(value, 265.dp, y + 33.dp, 690.dp, 17, bold = true)
    MockText(if (status == OnboardingGreen) "✓" else "•", 960.dp, y + 13.dp, 55.dp, 20, status, bold = true, align = Alignment.End)
}

@Composable
private fun BoxScope.MockCard(
    x: Dp,
    y: Dp,
    width: Dp,
    height: Dp,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Box(
        Modifier
            .offset(x, y)
            .size(width, height)
            .background(if (selected) Color(0xFF2B1F17) else OnboardingSurface)
            .border(if (selected) 2.dp else 1.dp, if (selected) OnboardingOrange else OnboardingRule, RectangleShape)
            .then(if (onClick != null) Modifier.r1Pressable(onClick) else Modifier),
    )
}

@Composable
internal fun BoxScope.MockField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    x: Dp,
    y: Dp,
    width: Dp,
    height: Dp,
    selected: Boolean = false,
    error: Boolean = false,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    Box(
        modifier = Modifier
            .offset(x, y)
            .size(width, height)
            .background(OnboardingSurface)
            .border(
                if (selected || error) 2.dp else 1.dp,
                when { error -> OnboardingRed; selected -> OnboardingOrange; else -> OnboardingRule },
                RectangleShape,
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(
                color = if (enabled) OnboardingInk else OnboardingSoft,
                fontSize = 19.sp,
                fontFamily = FontFamily.SansSerif,
            ),
            visualTransformation = visualTransformation,
            modifier = Modifier.fillMaxSize(),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, color = OnboardingSoft, fontSize = 19.sp)
                    inner()
                }
            },
        )
    }
}

@Composable
internal fun BoxScope.MockButton(
    text: String,
    x: Dp,
    y: Dp,
    width: Dp,
    height: Dp,
    outlined: Boolean = false,
    mutedOutline: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val foreground = when {
        !enabled -> OnboardingSoft
        outlined && mutedOutline -> OnboardingSoft
        outlined -> OnboardingOrange
        else -> OnboardingBg
    }
    Box(
        modifier = Modifier
            .offset(x, y)
            .size(width, height)
            .background(if (outlined || !enabled) OnboardingSurface else OnboardingOrange)
            .then(
                if (outlined || !enabled) Modifier.border(
                    if (outlined && !mutedOutline) 2.dp else 1.dp,
                    if (outlined && !mutedOutline) OnboardingOrange else OnboardingRule,
                    RectangleShape,
                ) else Modifier,
            )
            .alpha(if (enabled) 1f else 0.7f)
            .semantics { role = Role.Button }
            .then(if (enabled) Modifier.r1Pressable(onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = foreground, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
    }
}

@Composable
internal fun BoxScope.MockText(
    text: String,
    x: Dp,
    y: Dp,
    width: Dp,
    size: Int,
    color: Color = OnboardingInk,
    bold: Boolean = false,
    align: Alignment.Horizontal = Alignment.Start,
    letterSpacing: Float = 0f,
    onClick: (() -> Unit)? = null,
) {
    val boxAlignment = when (align) {
        Alignment.CenterHorizontally -> Alignment.Center
        Alignment.End -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }
    Box(
        modifier = Modifier
            .offset(x, y)
            .width(width)
            .then(if (onClick != null) Modifier.r1Pressable(onClick) else Modifier),
        contentAlignment = boxAlignment,
    ) {
        Text(
            text = text,
            color = color,
            fontSize = size.sp,
            lineHeight = (size * 1.18f).sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = letterSpacing.sp,
        )
    }
}

private fun smoothStep(start: Float, end: Float, value: Float): Float {
    val x = ((value - start) / (end - start)).coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

private fun windowAlpha(value: Float, fadeInStart: Float, fullStart: Float, fullEnd: Float, fadeOutEnd: Float): Float = when {
    value < fadeInStart || value > fadeOutEnd -> 0f
    value < fullStart -> smoothStep(fadeInStart, fullStart, value)
    value <= fullEnd -> 1f
    else -> 1f - smoothStep(fullEnd, fadeOutEnd, value)
}
