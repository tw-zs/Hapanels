package com.github.itskenny0.r1ha.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.itskenny0.r1ha.core.prefs.AppSettings
import com.github.itskenny0.r1ha.core.ha.HaRepository
import com.github.itskenny0.r1ha.core.prefs.SettingsRepository
import com.github.itskenny0.r1ha.core.prefs.StartView
import com.github.itskenny0.r1ha.core.prefs.TokenStore
import com.github.itskenny0.r1ha.core.theme.R1
import com.github.itskenny0.r1ha.feature.panelgrid.HapanelsDashboardConfig
import com.github.itskenny0.r1ha.feature.panelgrid.HapanelsDashboardConfigSource
import com.github.itskenny0.r1ha.feature.panelgrid.HapanelsDashboardPatch
import com.github.itskenny0.r1ha.feature.panelgrid.HapanelsDashboardPatchResult
import com.github.itskenny0.r1ha.feature.panelgrid.HapanelsThemePreset
import com.github.itskenny0.r1ha.feature.panelgrid.hapanelsThemeDefinitions
import com.github.itskenny0.r1ha.ui.components.R1Button
import com.github.itskenny0.r1ha.ui.components.R1ButtonVariant
import com.github.itskenny0.r1ha.ui.components.R1TextField
import com.github.itskenny0.r1ha.ui.components.r1Pressable
import com.github.itskenny0.r1ha.ui.i18n.Text
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

@Composable
fun OnboardingScreen(
    settings: SettingsRepository,
    tokens: TokenStore,
    haRepository: HaRepository,
    dashboardConfigSource: HapanelsDashboardConfigSource,
    onComplete: (StartView) -> Unit,
    /** Optional escape hatch — when set, the URL entry form shows a
     *  small "Use long-lived token instead" link below CONNECT that
     *  jumps to the LLAT setup screen. Lets kiosk-style installs skip
     *  OAuth entirely without first having to OAuth in to reach the
     *  Settings → LLAT screen (chicken-and-egg). Null disables. */
    onOpenLongLivedToken: (() -> Unit)? = null,
    http: OkHttpClient = remember { OkHttpClient() },
) {
    val vm: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.factory(http = http, settings = settings, tokens = tokens),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val appSettings by settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    var step by rememberSaveable { mutableStateOf(0) }
    var credentialsReady by remember { mutableStateOf<Boolean?>(null) }

    // Authentication is only the middle of onboarding. Keep the user here long enough to
    // name the panel and choose its initial presentation before entering the app.
    LaunchedEffect(state) {
        if (state is OnboardingViewModel.State.Done) {
            haRepository.reconnectNow()
            step = 2
        }
    }

    LaunchedEffect(step, state) {
        if (step == 2) {
            val persistedSettings = settings.settings.first()
            val persistedTokens = tokens.load()
            val ready = persistedSettings.server != null && !persistedTokens?.accessToken.isNullOrBlank()
            credentialsReady = ready
            if (!ready) step = 1
        } else {
            credentialsReady = null
        }
    }

    // The LLAT escape hatch saves directly to TokenStore without
    // running the OAuth state machine, so OnboardingViewModel never
    // transitions to Done. Observe the activity lifecycle and re-check
    // token presence on ON_RESUME — when the user comes back from the
    // LLAT screen with a freshly-saved token, fire onComplete so they
    // land on the card stack instead of being stranded on the URL form.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val resumeScope = androidx.compose.runtime.rememberCoroutineScope()
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                resumeScope.launch {
                    if (!tokens.load()?.accessToken.isNullOrBlank() &&
                        settings.settings.first().server != null
                    ) {
                        step = 2
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when (step) {
        0 -> {
            WelcomeScreen(onStart = { step = 1 })
            return
        }
        2 -> {
            if (credentialsReady != true) {
                Box(modifier = Modifier.fillMaxSize().background(R1.Bg))
                return
            }
            PersonalizeScreen(
                initialSettings = appSettings,
                settings = settings,
                dashboardConfigSource = dashboardConfigSource,
                onComplete = onComplete,
            )
            return
        }
    }

    when (val s = state) {
        is OnboardingViewModel.State.ReadyToAuth -> {
            // Back press inside the OAuth WebView should drop the user back to the URL
            // entry form instead of exiting the app.
            BackHandler { vm.resetError() }
            OAuthWebView(
                authorizeUrl = s.authorizeUrl,
                // Use the baseUrl the user originally probed so path-prefixed HA setups
                // (e.g. https://example.com/ha) keep their prefix on /auth/token.
                onCodeCaptured = { code -> vm.exchangeCode(code, s.baseUrl) },
                // If HA redirects without a `code` query parameter — typically because the
                // user tapped "Deny" — drop them back to the URL entry form with the HA
                // error surfaced as a visible message rather than leaving the WebView pinned
                // on HA's error page with no clear next step.
                onMissingCode = { errorMessage ->
                    vm.failOnboarding(errorMessage?.let { "Login was cancelled or rejected ($it)" }
                        ?: "Login didn't complete. Please try again.")
                },
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
            )
        }

        is OnboardingViewModel.State.Exchanging -> {
            // Labelled progress state — bare CircularProgressIndicator on a black screen
            // doesn't communicate that the app is doing anything specific; users sometimes
            // think it's hung. The screen sequence (01 LINK → 02 AUTHORISE → 03 READY) makes
            // the onboarding feel like a guided flow with a known end.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(com.github.itskenny0.r1ha.core.theme.R1.Bg)
                    .systemBarsPadding()
                    .padding(horizontal = 22.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "02 · AUTHORISE",
                    style = com.github.itskenny0.r1ha.core.theme.R1.labelMicro,
                    color = com.github.itskenny0.r1ha.core.theme.R1.AccentWarm,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Exchanging tokens…",
                    style = com.github.itskenny0.r1ha.core.theme.R1.screenTitle,
                    color = com.github.itskenny0.r1ha.core.theme.R1.Ink,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Swapping the authorisation code for an access token. This\nis a one-time round-trip; it usually takes a second.",
                    style = com.github.itskenny0.r1ha.core.theme.R1.body,
                    color = com.github.itskenny0.r1ha.core.theme.R1.InkMuted,
                )
                Spacer(Modifier.height(28.dp))
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                    color = com.github.itskenny0.r1ha.core.theme.R1.AccentWarm,
                )
            }
        }

        is OnboardingViewModel.State.Done -> {
            // LaunchedEffect above handles navigation; show nothing while it fires.
            Box(Modifier.fillMaxSize())
        }

        else -> {
            // Idle / Probing / Error all show the URL entry form.
            if (s !is OnboardingViewModel.State.Probing) {
                BackHandler {
                    vm.resetError()
                    step = 0
                }
            }
            UrlEntryForm(
                isProbing = s is OnboardingViewModel.State.Probing,
                error = (s as? OnboardingViewModel.State.Error)?.message,
                onProbe = { vm.probe(it) },
                onErrorDismiss = { vm.resetError() },
                onUseLongLivedToken = onOpenLongLivedToken,
                onBack = { step = 0 },
            )
        }
    }
}

@Composable
private fun WelcomeScreen(onStart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(R1.Bg)
            .systemBarsPadding()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxHeight()
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(com.github.itskenny0.r1ha.R.drawable.hapanels_logo),
                contentDescription = "Hapanels",
                modifier = Modifier.width(190.dp).height(190.dp),
            )
            Text(
                text = "Welcome to Hapanels!",
                style = R1.screenTitle.copy(fontSize = 40.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold),
                color = R1.Ink,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Your home. One simple panel.",
                style = R1.body.copy(fontSize = 21.sp),
                color = R1.InkSoft,
            )
            Spacer(Modifier.height(34.dp))
            Text(
                text = "Control lights, temperature, blinds, and other devices from one screen.",
                style = R1.body.copy(fontSize = 17.sp),
                color = R1.Ink,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Next, connect this panel to your home and choose what it should display.",
                style = R1.body.copy(fontSize = 16.sp),
                color = R1.InkSoft,
            )
            Spacer(Modifier.height(38.dp))
            R1Button(
                text = "START SETUP",
                onClick = onStart,
                modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PersonalizeScreen(
    initialSettings: AppSettings,
    settings: SettingsRepository,
    dashboardConfigSource: HapanelsDashboardConfigSource,
    onComplete: (StartView) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var tabletName by rememberSaveable {
        mutableStateOf(
            initialSettings.tabletFriendlyName.ifBlank {
                android.os.Build.MODEL?.takeIf(String::isNotBlank) ?: "Hapanels tablet"
            },
        )
    }
    var dashboardConfig by remember { mutableStateOf<HapanelsDashboardConfig?>(null) }
    var selectedPresetName by rememberSaveable { mutableStateOf<String?>(null) }
    var startViewName by rememberSaveable { mutableStateOf(initialSettings.behavior.startView.name) }
    var personalizePage by rememberSaveable { mutableStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPreset = selectedPresetName?.let { name ->
        runCatching { HapanelsThemePreset.valueOf(name) }.getOrNull()
    }
    val startView = runCatching { StartView.valueOf(startViewName) }
        .getOrDefault(StartView.PANEL_GRID)

    BackHandler(enabled = personalizePage == 1) { personalizePage = 0 }

    LaunchedEffect(dashboardConfigSource) {
        try {
            val loaded = dashboardConfigSource.loadOrSeed()
            dashboardConfig = loaded
            if (selectedPresetName == null) selectedPresetName = loaded.theme.preset.name
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            error = t.message ?: "Could not load panel theme."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(R1.Bg)
            .systemBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxHeight()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            if (personalizePage == 0) {
                Text(text = "03 · PANEL NAME", style = R1.labelMicro.copy(fontSize = 13.sp), color = R1.AccentWarm)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "What should this tablet be called?",
                    style = R1.screenTitle.copy(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
                    color = R1.Ink,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "This name identifies the panel in Home Assistant, MQTT, and Hapanels Studio.",
                    style = R1.body.copy(fontSize = 16.sp),
                    color = R1.InkSoft,
                )
                Spacer(Modifier.height(72.dp))
                Text(text = "DEVICE NAME", style = R1.labelMicro.copy(fontSize = 12.sp), color = R1.InkSoft)
                Spacer(Modifier.height(8.dp))
                R1TextField(
                    value = tabletName,
                    onValueChange = { tabletName = it },
                    placeholder = "Kitchen panel",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Android identified this device as: ${android.os.Build.MODEL}",
                    style = R1.body.copy(fontSize = 14.sp),
                    color = R1.InkMuted,
                )
                Spacer(Modifier.height(72.dp))
                R1Button(
                    text = "SAVE NAME AND CONTINUE",
                    enabled = tabletName.isNotBlank(),
                    onClick = { personalizePage = 1 },
                    modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth().align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "You can change the name later in Settings.",
                    style = R1.body,
                    color = R1.InkMuted,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            } else {
                PrototypeBackButton(onClick = { personalizePage = 0 })
                Spacer(Modifier.height(18.dp))
                Text(text = "04 · APPEARANCE", style = R1.labelMicro.copy(fontSize = 13.sp), color = R1.AccentWarm)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Make the panel yours",
                    style = R1.screenTitle.copy(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
                    color = R1.Ink,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Nothing is permanent. You can change every choice later in Settings.",
                    style = R1.body.copy(fontSize = 16.sp),
                    color = R1.InkSoft,
                )
                Spacer(Modifier.height(32.dp))
                Text(text = "DEFAULT APP VIEW", style = R1.labelMicro.copy(fontSize = 12.sp), color = R1.InkSoft)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChoiceRow(
                        title = "Hapanels Grid",
                        selected = startView == StartView.PANEL_GRID,
                        onClick = { startViewName = StartView.PANEL_GRID.name },
                        modifier = Modifier.weight(1f),
                    )
                    ChoiceRow(
                        title = "Cards",
                        selected = startView == StartView.CARDS,
                        onClick = { startViewName = StartView.CARDS.name },
                        modifier = Modifier.weight(1f),
                    )
                }

                if (startView == StartView.PANEL_GRID) {
                    Spacer(Modifier.height(28.dp))
                    Text(text = "PANEL GRID THEME", style = R1.labelMicro.copy(fontSize = 12.sp), color = R1.InkSoft)
                    Spacer(Modifier.height(8.dp))
                    hapanelsThemeDefinitions.forEach { definition ->
                        ChoiceRow(
                            title = definition.name,
                            selected = selectedPreset == definition.preset,
                            onClick = { selectedPresetName = definition.preset.name },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                if (error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(text = error.orEmpty(), style = R1.body, color = R1.StatusRed)
                }
                Spacer(Modifier.height(28.dp))
                R1Button(
                    text = if (saving) "SAVING..." else "SAVE AND CONTINUE",
                    enabled = !saving && tabletName.isNotBlank() &&
                        (startView != StartView.PANEL_GRID || selectedPreset != null && dashboardConfig != null),
                    onClick = {
                        saving = true
                        error = null
                        scope.launch {
                            try {
                                if (startView == StartView.PANEL_GRID) {
                                    val current = requireNotNull(dashboardConfig)
                                    when (
                                        val patchResult = dashboardConfigSource.applyPatch(
                                            HapanelsDashboardPatch(
                                                baseRevision = current.revision,
                                                updatedBy = "hapanels:onboarding",
                                                theme = current.theme.copy(preset = requireNotNull(selectedPreset)),
                                            ),
                                        )
                                    ) {
                                        is HapanelsDashboardPatchResult.Applied -> dashboardConfig = patchResult.config
                                        is HapanelsDashboardPatchResult.Conflict -> {
                                            dashboardConfig = patchResult.currentConfig
                                            error = "Panel theme changed elsewhere. Retry to apply your selection."
                                            saving = false
                                            return@launch
                                        }
                                    }
                                }
                                val expectedTabletName = tabletName.trim()
                                settings.update { current ->
                                    current.copy(
                                        tabletFriendlyName = expectedTabletName,
                                        behavior = current.behavior.copy(startView = startView),
                                    )
                                }
                                val persisted = settings.settings.first()
                                check(
                                    persisted.tabletFriendlyName == expectedTabletName &&
                                        persisted.behavior.startView == startView
                                ) { "Personalization verification failed after save" }
                                onComplete(startView)
                            } catch (t: CancellationException) {
                                throw t
                            } catch (t: Throwable) {
                                error = t.message ?: "Could not save setup."
                                saving = false
                            }
                        }
                    },
                    modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth().align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(R1.ShapeM)
            .background(if (selected) R1.AccentWarm.copy(alpha = 0.14f) else R1.SurfaceMuted)
            .border(1.dp, if (selected) R1.AccentWarm else R1.Hairline, R1.ShapeM)
            .semantics {
                this.selected = selected
                role = Role.RadioButton
            }
            .r1Pressable(onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(14.dp)
                .clip(R1.ShapeS)
                .background(if (selected) R1.AccentWarm else R1.Bg)
                .border(1.dp, if (selected) R1.AccentWarm else R1.InkMuted, R1.ShapeS),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title.uppercase(),
            style = R1.labelMicro,
            color = if (selected) R1.AccentWarm else R1.InkSoft,
        )
    }
}

@Composable
private fun PrototypeBackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(128.dp)
            .height(54.dp)
            .clip(R1.ShapeM)
            .background(R1.Surface)
            .border(1.dp, R1.Hairline, R1.ShapeM)
            .r1Pressable(onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "‹  BACK",
            style = R1.bodyEmph.copy(fontSize = 16.sp),
            color = R1.Ink,
        )
    }
}

@Composable
private fun UrlEntryForm(
    isProbing: Boolean,
    error: String?,
    onProbe: (String) -> Unit,
    onErrorDismiss: () -> Unit,
    onUseLongLivedToken: (() -> Unit)? = null,
    onBack: () -> Unit,
) {
    // Start empty so the placeholder ("http://homeassistant.local:8123") is what the
    // user sees first — they can type a bare host like "192.168.1.10" and let the
    // normaliser pick the protocol + port, or paste a full URL if they prefer.
    var urlText by rememberSaveable { mutableStateOf("") }
    val scrollState = androidx.compose.foundation.rememberScrollState()
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(imeBottom) {
        if (imeBottom > 0) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.github.itskenny0.r1ha.core.theme.R1.Bg)
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
    Column(
        modifier = Modifier
            .widthIn(max = 800.dp)
            .fillMaxHeight()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        PrototypeBackButton(onClick = onBack)
        Spacer(Modifier.height(18.dp))
        // ── Headline ───────────────────────────────────────────────────────────────
        // Tiny callout above the screen title — "SECTION/01 · LINK".
        Text(
            text = "01 · CONNECTION",
            style = R1.labelMicro.copy(fontSize = 13.sp),
            color = com.github.itskenny0.r1ha.core.theme.R1.AccentWarm,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Connect the panel to Home Assistant",
            style = R1.screenTitle.copy(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
            color = com.github.itskenny0.r1ha.core.theme.R1.Ink,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Enter the server address. Protocol and port are optional.",
            style = R1.body.copy(fontSize = 16.sp),
            color = com.github.itskenny0.r1ha.core.theme.R1.InkMuted,
        )
        Spacer(Modifier.height(42.dp))

        // ── Field ──────────────────────────────────────────────────────────────────
        Text(
            text = "URL",
            style = R1.labelMicro.copy(fontSize = 12.sp),
            color = com.github.itskenny0.r1ha.core.theme.R1.InkMuted,
        )
        Spacer(Modifier.height(8.dp))
        com.github.itskenny0.r1ha.ui.components.R1TextField(
            value = urlText,
            onValueChange = {
                if (error != null) onErrorDismiss()
                urlText = it
            },
            placeholder = "http://homeassistant.local:8123",
            isError = error != null,
            enabled = !isProbing,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go,
            ),
            keyboardActions = KeyboardActions(onGo = { onProbe(urlText) }),
            modifier = Modifier.fillMaxWidth(),
        )

        // Live preview of what the normaliser will turn the typed URL into.
        // Surfaces the protocol-inference and default-port heuristic before the
        // user taps CONNECT so 'why is it adding :8123?' has an immediate answer
        // and a typo'd host doesn't probe for two seconds before failing. Only
        // shown when the preview differs from the raw input (i.e. the normaliser
        // actually did something).
        val normalised = remember(urlText) {
            com.github.itskenny0.r1ha.feature.onboarding.normalizeServerUrl(urlText)
        }
        if (normalised.isNotBlank() && normalised != urlText.trim().trimEnd('/')) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Will probe: $normalised",
                style = com.github.itskenny0.r1ha.core.theme.R1.labelMicro,
                color = com.github.itskenny0.r1ha.core.theme.R1.InkSoft,
            )
        }
        // OPEN IN BROWSER chip: lets the user sanity-check the normalised URL in the
        // system browser before committing to the OAuth round-trip. Common pre-onboarding
        // diagnostic for "is HA even reachable on this LAN" questions.
        if (normalised.isNotBlank() && normalised.startsWith("http", ignoreCase = true)) {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            Spacer(Modifier.height(4.dp))
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .clip(com.github.itskenny0.r1ha.core.theme.R1.ShapeS)
                    .background(com.github.itskenny0.r1ha.core.theme.R1.SurfaceMuted)
                    .border(
                        1.dp,
                        com.github.itskenny0.r1ha.core.theme.R1.Hairline,
                        com.github.itskenny0.r1ha.core.theme.R1.ShapeS,
                    )
                    .r1Pressable(onClick = {
                        runCatching {
                            ctx.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(normalised),
                                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    })
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "OPEN IN BROWSER",
                    style = com.github.itskenny0.r1ha.core.theme.R1.labelMicro,
                    color = com.github.itskenny0.r1ha.core.theme.R1.InkSoft,
                )
            }
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = error,
                style = com.github.itskenny0.r1ha.core.theme.R1.body,
                color = com.github.itskenny0.r1ha.core.theme.R1.StatusRed,
            )
        }

        Spacer(Modifier.height(24.dp))

        com.github.itskenny0.r1ha.ui.components.R1Button(
            text = if (isProbing) "PROBING…" else "CONNECT WITH HOME ASSISTANT",
            onClick = { onProbe(urlText) },
            enabled = !isProbing,
            modifier = Modifier.fillMaxWidth(),
            leadingContent = if (isProbing) {
                {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(14.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = com.github.itskenny0.r1ha.core.theme.R1.Bg,
                    )
                }
            } else null,
        )

        // ── LLAT escape hatch ──────────────────────────────────────────────────────
        // Without this link the user has to OAuth first to reach the
        // Settings → LLAT screen — pointless if they specifically don't
        // want OAuth in the first place. The link is muted so it doesn't
        // compete with CONNECT as the primary action.
        if (onUseLongLivedToken != null) {
            Spacer(Modifier.height(12.dp))
            R1Button(
                text = "CONNECT WITH LONG-LIVED TOKEN",
                onClick = onUseLongLivedToken,
                modifier = Modifier.fillMaxWidth(),
                variant = R1ButtonVariant.Outlined,
            )
        }
    }
    } // centering Box
}
