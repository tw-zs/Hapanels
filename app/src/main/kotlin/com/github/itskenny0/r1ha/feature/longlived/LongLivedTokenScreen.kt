package com.github.itskenny0.r1ha.feature.longlived

import android.content.ClipboardManager
import android.content.Context
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.github.itskenny0.r1ha.ui.components.r1Pressable
import com.github.itskenny0.r1ha.feature.onboarding.OnboardingInk
import com.github.itskenny0.r1ha.feature.onboarding.OnboardingOrange
import com.github.itskenny0.r1ha.feature.onboarding.OnboardingRed
import com.github.itskenny0.r1ha.feature.onboarding.OnboardingRule
import com.github.itskenny0.r1ha.feature.onboarding.OnboardingSoft
import com.github.itskenny0.r1ha.feature.onboarding.OnboardingSurface
import com.github.itskenny0.r1ha.feature.onboarding.usePortraitOnboardingLayout
import com.github.itskenny0.r1ha.core.ha.HaRepository
import com.github.itskenny0.r1ha.core.prefs.OnboardingStage
import com.github.itskenny0.r1ha.core.prefs.ServerConfig
import com.github.itskenny0.r1ha.core.prefs.SettingsRepository
import com.github.itskenny0.r1ha.core.prefs.TokenStore
import com.github.itskenny0.r1ha.core.prefs.Tokens
import com.github.itskenny0.r1ha.core.util.R1Log
import com.github.itskenny0.r1ha.core.util.Toaster
import com.github.itskenny0.r1ha.feature.onboarding.AuthPage
import com.github.itskenny0.r1ha.feature.onboarding.MockButton
import com.github.itskenny0.r1ha.feature.onboarding.MockField
import com.github.itskenny0.r1ha.feature.onboarding.MockText
import com.github.itskenny0.r1ha.feature.onboarding.OnboardingBg
import com.github.itskenny0.r1ha.feature.onboarding.ProgressEdge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

@Composable
fun LongLivedTokenScreen(
    settings: SettingsRepository,
    tokens: TokenStore,
    haRepository: HaRepository,
    @Suppress("UNUSED_PARAMETER") wheelInput: com.github.itskenny0.r1ha.core.input.WheelInput,
    http: OkHttpClient,
    initialUrl: String = "",
    onboardingMode: Boolean = false,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val current by settings.settings.collectAsState(initial = null)
    var url by remember { mutableStateOf(initialUrl) }
    var token by remember { mutableStateOf("") }
    var revealed by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? android.app.Activity)?.window
        window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    LaunchedEffect(current) {
        if (url.isBlank()) url = current?.server?.url.orEmpty()
    }

    val save: () -> Unit = {
        if (!saving) {
            saving = true
            error = null
            scope.launch {
                try {
                    val normalisedUrl = com.github.itskenny0.r1ha.feature.onboarding.normalizeServerUrl(url)
                    require(normalisedUrl.isNotBlank()) { "Enter Home Assistant URL." }
                    val cleanedToken = token.filterNot(Char::isWhitespace)
                    require(cleanedToken.isNotBlank()) { "Paste long-lived access token." }
                    validateLongLivedToken(http, normalisedUrl, cleanedToken)

                    val previousSettings = settings.settings.first()
                    val previousTokens = tokens.load()
                    val savedTokens = Tokens(cleanedToken, "", Long.MAX_VALUE)
                    try {
                        tokens.save(savedTokens)
                        check(tokens.load() == savedTokens) { "Failed to verify saved token." }
                        settings.update {
                            it.copy(
                                server = ServerConfig(normalisedUrl),
                                onboardingStage = if (onboardingMode) OnboardingStage.PANEL_NAME else it.onboardingStage,
                            )
                        }
                        check(
                            settings.settings.first().let {
                                it.server?.url == normalisedUrl &&
                                    (!onboardingMode || it.onboardingStage == OnboardingStage.PANEL_NAME)
                            },
                        ) { "Failed to verify saved server." }
                    } catch (t: Throwable) {
                        withContext(NonCancellable) {
                            runCatching {
                                if (previousTokens == null) tokens.clear() else tokens.save(previousTokens)
                                check(tokens.load() == previousTokens) { "Failed to rollback token." }
                            }.onFailure { R1Log.e("LLAT", "token rollback failed", it) }
                            runCatching {
                                settings.update {
                                    it.copy(
                                        server = previousSettings.server,
                                        onboardingStage = previousSettings.onboardingStage,
                                    )
                                }
                                check(
                                    settings.settings.first().let {
                                        it.server == previousSettings.server &&
                                            it.onboardingStage == previousSettings.onboardingStage
                                    },
                                ) { "Failed to rollback settings." }
                            }.onFailure { R1Log.e("LLAT", "settings rollback failed", it) }
                        }
                        throw t
                    }
                    haRepository.reconnectNow(force = true)
                    Toaster.show("Token saved · connecting…")
                    onBack()
                } catch (t: CancellationException) {
                    throw t
                } catch (t: Throwable) {
                    R1Log.w("LLAT", "validation or save failed: ${t::class.simpleName}")
                    error = t.message ?: "Failed to connect."
                } finally {
                    saving = false
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(OnboardingBg)) {
        AuthPage(
            title = "Enter long-lived access token",
            description = "Token will be checked by Home Assistant before saving.",
            onBack = onBack,
        ) {
        if (usePortraitOnboardingLayout()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 180.dp)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!onboardingMode || initialUrl.isBlank()) {
                    Text("HOME ASSISTANT URL", color = OnboardingSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .background(OnboardingSurface)
                            .border(1.dp, OnboardingOrange)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        BasicTextField(
                            value = url,
                            onValueChange = { url = it; error = null },
                            singleLine = true,
                            textStyle = TextStyle(color = OnboardingInk, fontSize = 16.sp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Text("LONG-LIVED ACCESS TOKEN", color = OnboardingSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp)
                        .background(OnboardingSurface)
                        .border(1.dp, if (error != null) OnboardingRed else OnboardingOrange)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.TopStart,
                ) {
                    BasicTextField(
                        value = token,
                        onValueChange = { token = it; error = null },
                        textStyle = TextStyle(color = OnboardingInk, fontSize = 15.sp),
                        visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(OnboardingSurface)
                            .border(1.dp, OnboardingOrange)
                            .r1Pressable(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                token = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                            }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("PASTE", color = OnboardingOrange, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(OnboardingSurface)
                            .border(1.dp, OnboardingRule)
                            .r1Pressable(onClick = { revealed = !revealed }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (revealed) "HIDE TOKEN" else "REVEAL TOKEN", color = OnboardingSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                error?.let { Text(it, color = OnboardingRed, fontSize = 14.sp) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(if (!saving && url.isNotBlank() && token.isNotBlank()) OnboardingOrange else OnboardingSurface)
                        .then(if (!saving && url.isNotBlank() && token.isNotBlank()) Modifier.r1Pressable(save) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (saving) "CHECKING…" else "CONTINUE",
                        color = if (!saving && url.isNotBlank() && token.isNotBlank()) com.github.itskenny0.r1ha.feature.onboarding.OnboardingBg else OnboardingSoft,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    "Token will not appear in logs and remains encrypted on device.",
                    color = OnboardingSoft,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(16.dp))
            }
        } else {
            val tokenTop = if (onboardingMode && initialUrl.isNotBlank()) 278.dp else 356.dp
            if (!onboardingMode || initialUrl.isBlank()) {
                MockText("HOME ASSISTANT URL", 240.dp, 238.dp, 800.dp, 15, com.github.itskenny0.r1ha.feature.onboarding.OnboardingSoft, bold = true)
                MockField(url, { url = it; error = null }, "http://homeassistant.local:8123", 240.dp, 255.dp, 800.dp, 58.dp)
            }
            MockText("LONG-LIVED ACCESS TOKEN", 240.dp, tokenTop - 31.dp, 800.dp, 15, com.github.itskenny0.r1ha.feature.onboarding.OnboardingSoft, bold = true)
            MockField(
                value = token,
                onValueChange = { token = it; error = null },
                placeholder = "Paste token from Home Assistant profile",
                x = 240.dp,
                y = tokenTop,
                width = 620.dp,
                height = 128.dp,
                error = error != null,
                visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
            )
            MockButton(
                text = "PASTE",
                x = 880.dp,
                y = tokenTop,
                width = 160.dp,
                height = 128.dp,
                outlined = true,
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    token = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                },
            )
            MockButton(
                text = if (revealed) "HIDE TOKEN" else "REVEAL TOKEN",
                x = 240.dp,
                y = tokenTop + 142.dp,
                width = 190.dp,
                height = 44.dp,
                outlined = true,
                mutedOutline = true,
                onClick = { revealed = !revealed },
            )
            error?.let { MockText(it, 450.dp, tokenTop + 149.dp, 590.dp, 14, com.github.itskenny0.r1ha.feature.onboarding.OnboardingRed) }
            MockButton(
                text = if (saving) "CHECKING…" else "CONTINUE",
                x = 420.dp,
                y = 556.dp,
                width = 440.dp,
                height = 68.dp,
                enabled = !saving && url.isNotBlank() && token.isNotBlank(),
                onClick = save,
            )
            MockText(
                "Token will not appear in logs and remains encrypted on device.",
                0.dp,
                674.dp,
                1280.dp,
                15,
                com.github.itskenny0.r1ha.feature.onboarding.OnboardingSoft,
                align = androidx.compose.ui.Alignment.CenterHorizontally,
            )
        }
        }
        if (onboardingMode) ProgressEdge(2f / 7f, 0f)
    }
}
