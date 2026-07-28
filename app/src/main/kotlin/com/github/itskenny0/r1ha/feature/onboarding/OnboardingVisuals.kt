package com.github.itskenny0.r1ha.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.res.stringResource
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
        modifier = modifier.fillMaxSize().imePadding(),
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
        MockText(stringResource(R.string.onboarding_welcome_title), 0.dp, 277.dp, 1280.dp, 46, bold = true, align = Alignment.CenterHorizontally)
        MockText(stringResource(R.string.onboarding_welcome_tagline), 0.dp, 341.dp, 1280.dp, 24, OnboardingSoft, align = Alignment.CenterHorizontally)
        MockText(
            stringResource(R.string.onboarding_welcome_body),
            0.dp,
            411.dp,
            1280.dp,
            19,
            align = Alignment.CenterHorizontally,
        )
        MockText(
            stringResource(R.string.onboarding_welcome_hint),
            0.dp,
            447.dp,
            1280.dp,
            18,
            OnboardingSoft,
            align = Alignment.CenterHorizontally,
        )
        MockButton(stringResource(R.string.onboarding_start_setup), 420.dp, 580.dp, 440.dp, 68.dp, onClick = onStart)
    }
}

@Composable
internal fun ConnectionPage(
    url: String,
    onUrlChange: (String) -> Unit,
    detectedServers: List<String>,
    probing: Boolean,
    error: String?,
    discoveryRunning: Boolean,
    discoveryError: Boolean,
    onRetryDiscovery: () -> Unit,
    onDetectedServer: (String) -> Unit,
    onBack: () -> Unit,
    onOAuth: () -> Unit,
    onLlat: () -> Unit,
) {
    StandardPage(
        step = stringResource(R.string.onboarding_step_connection),
        title = stringResource(R.string.onboarding_connection_title),
        description = stringResource(R.string.onboarding_connection_description),
        onBack = onBack,
    ) {
        MockText(stringResource(R.string.onboarding_discovered_servers), 240.dp, 228.dp, 800.dp, 15, OnboardingSoft, bold = true, letterSpacing = 1.2f)
        if (discoveryRunning) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.offset(815.dp, 222.dp).size(18.dp),
                strokeWidth = 2.dp,
                color = OnboardingOrange,
            )
        }
        MockButton(stringResource(R.string.onboarding_search_again), 845.dp, 212.dp, 195.dp, 36.dp, outlined = true, mutedOutline = true, onClick = onRetryDiscovery)
        if (detectedServers.isEmpty()) {
            MockCard(240.dp, 260.dp, 800.dp, 58.dp)
            MockText(
                stringResource(if (discoveryError) R.string.onboarding_search_failed else R.string.onboarding_searching),
                265.dp,
                277.dp,
                750.dp,
                18,
                if (discoveryError) OnboardingRed else OnboardingSoft,
            )
        } else {
            detectedServers.take(6).forEachIndexed { index, server ->
                val col = index % 2
                val row = index / 2
                val x = (240 + col * 410).dp
                val y = (250 + row * 48).dp
                val detectedSelected = url == server || (url.isBlank() && index == 0)
                MockCard(x, y, 390.dp, 42.dp, selected = detectedSelected, onClick = { onDetectedServer(server) })
                MockText(server, x + 16.dp, y + 11.dp, 340.dp, 14, if (detectedSelected) OnboardingInk else OnboardingSoft, bold = detectedSelected)
                if (detectedSelected) MockText("✓", x + 350.dp, y + 10.dp, 24.dp, 15, OnboardingOrange, bold = true)
            }
        }
        MockText(stringResource(R.string.onboarding_manual_address), 240.dp, 410.dp, 800.dp, 15, OnboardingSoft, bold = true, letterSpacing = 1.2f)
        MockField(
            value = url,
            onValueChange = onUrlChange,
            placeholder = stringResource(R.string.onboarding_url_placeholder),
            x = 240.dp,
            y = 442.dp,
            width = 800.dp,
            height = 58.dp,
            error = error != null,
        )
        MockButton(
            stringResource(if (probing) R.string.onboarding_connecting else R.string.onboarding_sign_in_ha),
            240.dp,
            510.dp,
            800.dp,
            58.dp,
            enabled = !probing && (url.isNotBlank() || detectedServers.isNotEmpty()),
            onClick = onOAuth,
        )
        MockButton(
            stringResource(R.string.onboarding_long_lived_token),
            240.dp,
            580.dp,
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
    StandardPage(stringResource(R.string.onboarding_step_sign_in), title, description, onBack, content)
}

@Composable
internal fun PanelNamePage(
    value: String,
    onValueChange: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    StandardPage(
        stringResource(R.string.onboarding_step_panel_name),
        stringResource(R.string.onboarding_panel_name_title),
        stringResource(R.string.onboarding_panel_name_description),
        onBack,
    ) {
        MockText(stringResource(R.string.onboarding_device_name), 240.dp, 300.dp, 800.dp, 15, OnboardingSoft, bold = true, letterSpacing = 1.2f)
        MockField(value, onValueChange, stringResource(R.string.onboarding_panel_name_placeholder), 240.dp, 332.dp, 800.dp, 74.dp, selected = true)
        MockText(stringResource(R.string.onboarding_device_model, android.os.Build.MODEL), 240.dp, 429.dp, 800.dp, 16, OnboardingSoft)
        MockButton(stringResource(R.string.onboarding_save_name), 420.dp, 580.dp, 440.dp, 68.dp, enabled = value.isNotBlank(), onClick = onContinue)
        MockText(stringResource(R.string.onboarding_name_later), 0.dp, 674.dp, 1280.dp, 15, OnboardingSoft, align = Alignment.CenterHorizontally)
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
        stringResource(R.string.onboarding_step_appearance),
        stringResource(R.string.onboarding_appearance_title),
        stringResource(R.string.onboarding_appearance_description),
        onBack,
    ) {
        MockText(stringResource(R.string.onboarding_color_mode), 120.dp, 226.dp, 600.dp, 15, OnboardingSoft, bold = true, letterSpacing = 1.2f)
        ColorChoice(stringResource(R.string.onboarding_light), false, dark, 120.dp) { onDarkChange(false) }
        ColorChoice(stringResource(R.string.onboarding_dark), true, dark, 400.dp) { onDarkChange(true) }
        MockText(stringResource(R.string.onboarding_start_screen), 120.dp, 367.dp, 900.dp, 15, OnboardingSoft, bold = true, letterSpacing = 1.2f)
        ViewChoice(StartView.PANEL_GRID, startView, 120.dp, onStartViewChange)
        ViewChoice(StartView.CARDS, startView, 640.dp, onStartViewChange)
        if (error != null) {
            MockText(error, 120.dp, 548.dp, 1040.dp, 14, OnboardingRed, align = Alignment.CenterHorizontally)
        }
        MockButton(
            stringResource(if (error != null) R.string.onboarding_continue_without_theme else R.string.onboarding_save_continue),
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
    mqttConfigured: Boolean,
    infoOpen: Boolean,
    onInfoChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
) {
    StandardPage(
        stringResource(R.string.onboarding_step_studio),
        stringResource(R.string.onboarding_studio_title),
        stringResource(R.string.onboarding_studio_description),
        onBack,
    ) {
        MockCard(240.dp, 264.dp, 800.dp, 134.dp)
        MockText("HOME ASSISTANT", 270.dp, 285.dp, 700.dp, 14, OnboardingSoft, bold = true, letterSpacing = 1.2f)
        MockText("$serverName · $tabletName", 270.dp, 313.dp, 700.dp, 23, bold = true)
        MockText(
            stringResource(if (mqttConfigured) R.string.onboarding_mqtt_connected else R.string.onboarding_mqtt_required),
            270.dp,
            354.dp,
            700.dp,
            16,
            if (mqttConfigured) OnboardingGreen else OnboardingSoft,
        )
        MockText(if (mqttConfigured) stringResource(R.string.onboarding_online) else "MQTT", 915.dp, 313.dp, 93.dp, 18, if (mqttConfigured) OnboardingGreen else OnboardingOrange, bold = true, align = Alignment.End)
        MockText(
            stringResource(R.string.onboarding_studio_available),
            0.dp,
            435.dp,
            1280.dp,
            17,
            OnboardingSoft,
            align = Alignment.CenterHorizontally,
        )
        MockButton(
            stringResource(if (mqttConfigured) R.string.onboarding_studio_ready else R.string.onboarding_mqtt_required),
            240.dp,
            525.dp,
            800.dp,
            64.dp,
            enabled = false,
            onClick = {},
        )
        MockButton(stringResource(R.string.onboarding_continue), 240.dp, 615.dp, 385.dp, 56.dp, outlined = true, mutedOutline = true, onClick = onSkip)
        MockButton(stringResource(R.string.onboarding_learn_more), 655.dp, 615.dp, 385.dp, 56.dp, outlined = true, onClick = { onInfoChange(true) })
        MockText(stringResource(R.string.onboarding_studio_later), 0.dp, 681.dp, 1280.dp, 15, OnboardingSoft, align = Alignment.CenterHorizontally)
        if (infoOpen) StudioInfoPopup { onInfoChange(false) }
    }
}

@Composable
internal fun MqttPage(
    host: String,
    port: String,
    username: String,
    password: String,
    useTls: Boolean,
    hostError: String?,
    onHostChange: (String) -> Unit,
    onUseHaHost: () -> Unit,
    onPortChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTlsChange: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onSave: () -> Unit,
) {
    StandardPage(
        stringResource(R.string.onboarding_step_mqtt),
        stringResource(R.string.onboarding_mqtt_title),
        stringResource(R.string.onboarding_mqtt_description),
        onBack,
    ) {
        MockText(stringResource(R.string.onboarding_broker_address), 180.dp, 233.dp, 430.dp, 14, OnboardingSoft, bold = true)
        MockField(host, onHostChange, "192.168.1.10", 180.dp, 263.dp, 430.dp, 58.dp, error = hostError != null, selected = true)
        MockButton(stringResource(R.string.onboarding_use_ha_host), 625.dp, 263.dp, 155.dp, 58.dp, outlined = true, onClick = onUseHaHost)
        MockText(stringResource(R.string.onboarding_port), 810.dp, 233.dp, 290.dp, 14, OnboardingSoft, bold = true)
        MockField(port, onPortChange, "1883", 810.dp, 263.dp, 290.dp, 58.dp, selected = true)
        MockText(stringResource(R.string.onboarding_username), 180.dp, 357.dp, 440.dp, 14, OnboardingSoft, bold = true)
        MockField(username, onUsernameChange, stringResource(R.string.onboarding_optional), 180.dp, 387.dp, 440.dp, 58.dp)
        MockText(stringResource(R.string.onboarding_password), 650.dp, 357.dp, 450.dp, 14, OnboardingSoft, bold = true)
        MockField(password, onPasswordChange, stringResource(R.string.onboarding_optional), 650.dp, 387.dp, 450.dp, 58.dp, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
        MockButton(stringResource(if (useTls) R.string.onboarding_tls_on else R.string.onboarding_tls_off), 180.dp, 482.dp, 230.dp, 54.dp, outlined = true, onClick = onTlsChange)
        if (hostError != null) MockText(hostError, 180.dp, 548.dp, 920.dp, 14, OnboardingRed)
        MockButton(stringResource(R.string.onboarding_save_continue), 420.dp, 580.dp, 440.dp, 68.dp, enabled = hostError == null, onClick = onSave)
        MockText(stringResource(R.string.onboarding_mqtt_skip_hint), 0.dp, 681.dp, 1280.dp, 15, OnboardingSoft, align = Alignment.CenterHorizontally)
        MockButton(stringResource(R.string.onboarding_skip), 240.dp, 615.dp, 120.dp, 56.dp, outlined = true, mutedOutline = true, onClick = onSkip)
    }
}

@Composable
internal fun ChecklistPage(
    haConnected: Boolean,
    mqttConfigured: Boolean,
    dark: Boolean,
    startView: StartView,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onLaunch: () -> Unit,
) {
    StandardPage(
        stringResource(R.string.onboarding_step_ready),
        stringResource(R.string.onboarding_ready_title),
        stringResource(R.string.onboarding_ready_description),
        onBack,
    ) {
        ChecklistRow("HOME ASSISTANT", stringResource(if (haConnected) R.string.onboarding_connected else R.string.onboarding_waiting_connection), if (haConnected) OnboardingGreen else OnboardingOrange, 248.dp)
        ChecklistRow(
            "HAPANELS STUDIO",
            stringResource(if (mqttConfigured) R.string.onboarding_configured_mqtt else R.string.onboarding_skipped_device),
            if (mqttConfigured) OnboardingGreen else OnboardingSoft,
            326.dp,
        )
        ChecklistRow(stringResource(R.string.onboarding_appearance), "${stringResource(if (dark) R.string.onboarding_dark else R.string.onboarding_light)} · ${if (startView == StartView.PANEL_GRID) "Hapanels Grid" else stringResource(R.string.onboarding_cards)}", OnboardingGreen, 404.dp)
        ChecklistRow(
            "MQTT",
            stringResource(if (mqttConfigured) R.string.onboarding_broker_configured else R.string.onboarding_skipped),
            if (mqttConfigured) OnboardingGreen else OnboardingSoft,
            482.dp,
        )
        MockButton(
            stringResource(if (haConnected) R.string.onboarding_start_hapanels else R.string.onboarding_try_again),
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
internal fun LaunchSequence(progress: Float) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ScaledOnboardingPage {
            val logoAlpha = windowAlpha(progress, 0.15f, 0.36f, 0.76f, 0.84f)
            Image(
                painter = painterResource(R.drawable.hapanels_logo),
                contentDescription = null,
                modifier = Modifier.offset(510.dp, 134.dp).size(260.dp).alpha(logoAlpha),
            )
            MockText(
                stringResource(R.string.onboarding_setup_complete),
                0.dp,
                427.dp,
                1280.dp,
                34,
                OnboardingInk.copy(alpha = windowAlpha(progress, 0.34f, 0.43f, 0.50f, 0.56f)),
                bold = true,
                align = Alignment.CenterHorizontally,
            )
            MockText(
                stringResource(R.string.onboarding_welcome_home),
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
private fun StandardPage(
    step: String,
    title: String,
    description: String,
    onBack: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    ScaledOnboardingPage {
        Box(Modifier.fillMaxSize().background(OnboardingBg))
        MockButton(stringResource(R.string.onboarding_back), 48.dp, 38.dp, 128.dp, 54.dp, outlined = true, mutedOutline = true, onClick = onBack)
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
    MockText(stringResource(R.string.onboarding_studio_manage), 210.dp, 165.dp, 650.dp, 31, bold = true)
    MockText(stringResource(R.string.onboarding_studio_popup_description), 210.dp, 221.dp, 650.dp, 18, OnboardingSoft)
    MockText(stringResource(R.string.onboarding_studio_bullet_screens), 210.dp, 282.dp, 560.dp, 18)
    MockText(stringResource(R.string.onboarding_studio_bullet_tablets), 210.dp, 321.dp, 560.dp, 18)
    MockText(stringResource(R.string.onboarding_studio_bullet_deploy), 210.dp, 358.dp, 560.dp, 18)
    MockText(stringResource(R.string.onboarding_documentation), 210.dp, 426.dp, 500.dp, 16, OnboardingSoft, bold = true)
    MockText("tw-zs.github.io/Hapanels/", 210.dp, 456.dp, 500.dp, 20, OnboardingOrange, bold = true)
    Box(Modifier.offset(792.dp, 216.dp).size(272.dp).background(Color.White))
    Image(painterResource(R.drawable.hapanels_docs_qr), null, Modifier.offset(808.dp, 232.dp).size(240.dp))
    MockText(stringResource(R.string.onboarding_scan_qr), 808.dp, 512.dp, 240.dp, 14, OnboardingSoft, bold = true, align = Alignment.CenterHorizontally)
    MockButton(stringResource(R.string.onboarding_close), 420.dp, 576.dp, 440.dp, 60.dp, onClick = onClose)
    MockText("×", 1042.dp, 117.dp, 50.dp, 34, OnboardingSoft, align = Alignment.CenterHorizontally, onClick = onClose)
}

@Composable
private fun BoxScope.ColorChoice(label: String, value: Boolean, selectedValue: Boolean, x: Dp, onClick: () -> Unit) {
    val selected = value == selectedValue
    MockCard(x, 258.dp, 250.dp, 72.dp, selected, onClick)
    Box(Modifier.offset(x + 16.dp, 274.dp).size(48.dp, 40.dp).background(if (value) Color(0xFF101010) else Color(0xFFEFEFEF)))
    MockText(if (value) "●" else "☀", x + 16.dp, 280.dp, 48.dp, 16, if (value) OnboardingInk else OnboardingOrange, bold = true, align = Alignment.CenterHorizontally)
    MockText(label, x + 82.dp, 273.dp, 140.dp, 19, bold = true)
    if (selected) MockText(stringResource(R.string.onboarding_selected), x + 82.dp, 303.dp, 140.dp, 12, OnboardingOrange, bold = true)
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
    MockText(if (value == StartView.PANEL_GRID) "Hapanels Grid" else stringResource(R.string.onboarding_cards), x + 194.dp, 433.dp, 250.dp, 22, bold = true)
    MockText(stringResource(if (value == StartView.PANEL_GRID) R.string.onboarding_grid_description else R.string.onboarding_cards_description), x + 194.dp, 476.dp, 260.dp, 16, OnboardingSoft)
    MockText(stringResource(if (selected) R.string.onboarding_selected else R.string.onboarding_select), x + 194.dp, 511.dp, 200.dp, 13, if (selected) OnboardingOrange else OnboardingSoft, bold = true)
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
