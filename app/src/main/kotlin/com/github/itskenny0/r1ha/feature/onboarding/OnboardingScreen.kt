package com.github.itskenny0.r1ha.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.itskenny0.r1ha.core.ha.ConnectionState
import com.github.itskenny0.r1ha.core.ha.HaRepository
import com.github.itskenny0.r1ha.core.prefs.AppSettings
import com.github.itskenny0.r1ha.core.prefs.OnboardingStage
import com.github.itskenny0.r1ha.core.prefs.SettingsRepository
import com.github.itskenny0.r1ha.core.prefs.StartView
import com.github.itskenny0.r1ha.core.prefs.TokenStore
import com.github.itskenny0.r1ha.feature.panelgrid.HapanelsDashboardConfig
import com.github.itskenny0.r1ha.feature.panelgrid.HapanelsDashboardConfigSource
import com.github.itskenny0.r1ha.feature.panelgrid.HapanelsDashboardPatch
import com.github.itskenny0.r1ha.feature.panelgrid.HapanelsDashboardPatchResult
import com.github.itskenny0.r1ha.feature.panelgrid.HapanelsThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

private enum class VisualPage(val index: Int) {
    WELCOME(0), CONNECTION(1), AUTH(2), PANEL_NAME(3), APPEARANCE(4), STUDIO(5), MQTT(6), CHECKLIST(7), LAUNCHING(8),
}

@Composable
fun OnboardingScreen(
    settings: SettingsRepository,
    tokens: TokenStore,
    haRepository: HaRepository,
    dashboardConfigSource: HapanelsDashboardConfigSource,
    onComplete: (StartView) -> Unit,
    onOpenLongLivedToken: ((String) -> Unit)? = null,
    http: OkHttpClient,
) {
    val vm: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.factory(http, settings, tokens),
    )
    val authState by vm.state.collectAsStateWithLifecycle()
    val appSettings by settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val connection by haRepository.connection.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var credentialsReady by remember { mutableStateOf<Boolean?>(null) }
    var url by rememberSaveable { mutableStateOf("") }
    var tabletName by rememberSaveable { mutableStateOf("") }
    var startViewName by rememberSaveable { mutableStateOf(StartView.PANEL_GRID.name) }
    var darkMode by rememberSaveable { mutableStateOf(true) }
    var studioInfoOpen by rememberSaveable { mutableStateOf(false) }
    var dashboardConfig by remember { mutableStateOf<HapanelsDashboardConfig?>(null) }
    var appearanceLoading by remember { mutableStateOf(true) }
    var appearanceError by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(appSettings.onboardingStage, authState) {
        credentialsReady = appSettings.server != null && !tokens.load()?.accessToken.isNullOrBlank()
        if (authState is OnboardingViewModel.State.Done) haRepository.reconnectNow()
    }
    LaunchedEffect(appSettings.tabletFriendlyName) {
        if (tabletName.isBlank()) {
            tabletName = appSettings.tabletFriendlyName.ifBlank {
                android.os.Build.MODEL?.takeIf(String::isNotBlank) ?: "Panel Hapanels"
            }
        }
    }
    LaunchedEffect(appSettings.behavior.startView) {
        startViewName = appSettings.behavior.startView.name
    }
    LaunchedEffect(dashboardConfigSource) {
        appearanceLoading = true
        runCatching { dashboardConfigSource.loadOrSeed() }
            .onSuccess {
                dashboardConfig = it
                darkMode = it.theme.mode != HapanelsThemeMode.LIGHT
                appearanceError = null
            }
            .onFailure { appearanceError = it.message ?: "Nie udało się wczytać ustawień wyglądu." }
        appearanceLoading = false
    }

    val persistedStage = appSettings.onboardingStage
    val stage = resolvedOnboardingStage(persistedStage, credentialsReady)
    val page = when {
        stage == OnboardingStage.LAUNCHING -> VisualPage.LAUNCHING
        authState is OnboardingViewModel.State.ReadyToAuth || authState is OnboardingViewModel.State.Exchanging -> VisualPage.AUTH
        stage == OnboardingStage.WELCOME -> VisualPage.WELCOME
        stage == OnboardingStage.CONNECTION -> VisualPage.CONNECTION
        stage == OnboardingStage.PANEL_NAME -> VisualPage.PANEL_NAME
        stage == OnboardingStage.APPEARANCE -> VisualPage.APPEARANCE
        stage == OnboardingStage.STUDIO -> VisualPage.STUDIO
        stage == OnboardingStage.MQTT -> VisualPage.MQTT
        else -> VisualPage.CHECKLIST
    }
    val startView = runCatching { StartView.valueOf(startViewName) }.getOrDefault(StartView.PANEL_GRID)

    val edgeProgress = remember { Animatable(0f) }
    val successGlow = remember { Animatable(0f) }
    var previousPageIndex by remember { mutableStateOf(page.index) }
    LaunchedEffect(page.index) {
        if (page != VisualPage.LAUNCHING) {
            val forward = page.index > previousPageIndex
            edgeProgress.animateTo(
                (page.index / 7f).coerceIn(0f, 1f),
                tween(1_200, easing = CubicBezierEasing(0.55f, 0f, 0.10f, 1f)),
            )
            if (forward) {
                successGlow.snapTo(1f)
                successGlow.animateTo(0f, tween(850, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)))
            }
            previousPageIndex = page.index
        }
    }

    val launchProgress = remember { Animatable(0f) }
    var screenActive by remember { mutableStateOf(true) }
    DisposableEffect(Unit) {
        screenActive = true
        onDispose { screenActive = false }
    }
    LaunchedEffect(stage) {
        if (stage == OnboardingStage.LAUNCHING) {
            launchProgress.snapTo(0.001f)
            val startedAt = withFrameNanos { it }
            do {
                val elapsed = withFrameNanos { it } - startedAt
                val progress = (elapsed / 5_600_000_000f).coerceIn(0f, 1f)
                launchProgress.snapTo(progress)
            } while (progress < 1f)
            withContext(NonCancellable) {
                settings.update { it.copy(onboardingStage = OnboardingStage.COMPLETED) }
                check(settings.settings.first().onboardingStage == OnboardingStage.COMPLETED)
                if (screenActive) onComplete(startView)
            }
        }
    }

    BackHandler(enabled = page != VisualPage.WELCOME && page != VisualPage.LAUNCHING) {
        when (page) {
            VisualPage.AUTH -> vm.resetError()
            VisualPage.CONNECTION -> {
                vm.resetError()
                scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.WELCOME) } }
            }
            VisualPage.PANEL_NAME -> scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.CONNECTION) } }
            VisualPage.APPEARANCE -> scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.PANEL_NAME) } }
            VisualPage.STUDIO -> scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.APPEARANCE) } }
            VisualPage.MQTT -> scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.STUDIO) } }
            VisualPage.CHECKLIST -> scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.MQTT) } }
            else -> Unit
        }
    }

    Box(Modifier.fillMaxSize().background(OnboardingBg)) {
        OnboardingBackdrop(showPhoto = page == VisualPage.WELCOME)
        if (page == VisualPage.LAUNCHING) {
            LaunchSequence(launchProgress.value, tabletName, startView)
        } else {
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val direction = if (targetState.index >= initialState.index) 1 else -1
                    (slideIntoContainer(
                        if (direction > 0) AnimatedContentTransitionScope.SlideDirection.Left
                        else AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(360, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
                    ) + fadeIn(tween(180))) togetherWith
                        (slideOutOfContainer(
                            if (direction > 0) AnimatedContentTransitionScope.SlideDirection.Left
                            else AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(360, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
                        ) + fadeOut(tween(180)))
                },
                modifier = Modifier.fillMaxSize(),
                label = "onboarding-page",
            ) { target ->
                when (target) {
                    VisualPage.WELCOME -> WelcomePage {
                        scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.CONNECTION) } }
                    }
                    VisualPage.CONNECTION -> ConnectionPage(
                        url = url,
                        onUrlChange = {
                            url = it
                            if (authState is OnboardingViewModel.State.Error) vm.resetError()
                        },
                        detectedServer = appSettings.server?.url,
                        probing = authState is OnboardingViewModel.State.Probing,
                        error = (authState as? OnboardingViewModel.State.Error)?.message,
                        onBack = {
                            vm.resetError()
                            scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.WELCOME) } }
                        },
                        onOAuth = { vm.probe(url.ifBlank { appSettings.server?.url.orEmpty() }) },
                        onLlat = { onOpenLongLivedToken?.invoke(url.ifBlank { appSettings.server?.url.orEmpty() }) },
                    )
                    VisualPage.AUTH -> AuthPage(
                        title = if (authState is OnboardingViewModel.State.Exchanging) "Łączenie z Home Assistant" else "Zaloguj się do Home Assistant",
                        description = "Logowanie odbywa się na bezpiecznej stronie Twojej instancji Home Assistant.",
                        onBack = vm::resetError,
                    ) {
                        when (val current = authState) {
                            is OnboardingViewModel.State.ReadyToAuth -> OAuthWebView(
                                authorizeUrl = current.authorizeUrl,
                                onCodeCaptured = { vm.exchangeCode(it, current.baseUrl) },
                                onMissingCode = { vm.failOnboarding("Logowanie anulowane.") },
                                modifier = Modifier.offset(240.dp, 230.dp).width(800.dp).height(430.dp),
                            )
                            else -> {
                                Box(Modifier.offset(240.dp, 250.dp).size(800.dp, 300.dp).background(OnboardingSurface), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = OnboardingOrange)
                                    Text("Wymiana tokenów…", color = OnboardingSoft, fontSize = 18.sp, modifier = Modifier.offset(y = 50.dp))
                                }
                            }
                        }
                    }
                    VisualPage.PANEL_NAME -> PanelNamePage(
                        value = tabletName,
                        onValueChange = { tabletName = it },
                        onBack = { scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.CONNECTION) } } },
                        onContinue = {
                            scope.launch {
                                settings.update {
                                    it.copy(
                                        tabletFriendlyName = tabletName.trim(),
                                        onboardingStage = OnboardingStage.APPEARANCE,
                                    )
                                }
                            }
                        },
                    )
                    VisualPage.APPEARANCE -> AppearancePage(
                        dark = darkMode,
                        startView = startView,
                        loading = appearanceLoading,
                        error = appearanceError,
                        onDarkChange = { darkMode = it },
                        onStartViewChange = { startViewName = it.name },
                        onBack = { scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.PANEL_NAME) } } },
                        onContinue = {
                            scope.launch {
                                try {
                                    dashboardConfig?.let { current ->
                                        when (
                                            val result = dashboardConfigSource.applyPatch(
                                                HapanelsDashboardPatch(
                                                    baseRevision = current.revision,
                                                    updatedBy = "hapanels:onboarding",
                                                    theme = current.theme.copy(
                                                        mode = if (darkMode) HapanelsThemeMode.DARK else HapanelsThemeMode.LIGHT,
                                                    ),
                                                ),
                                            )
                                        ) {
                                            is HapanelsDashboardPatchResult.Applied -> dashboardConfig = result.config
                                            is HapanelsDashboardPatchResult.Conflict -> {
                                                dashboardConfig = result.currentConfig
                                                appearanceError = "Motyw zmienił się w innym miejscu. Spróbuj ponownie."
                                                return@launch
                                            }
                                        }
                                    }
                                    settings.update {
                                        it.copy(
                                            behavior = it.behavior.copy(startView = startView),
                                            onboardingStage = OnboardingStage.STUDIO,
                                        )
                                    }
                                } catch (t: CancellationException) {
                                    throw t
                                } catch (t: Throwable) {
                                    appearanceError = t.message
                                }
                            }
                        },
                    )
                    VisualPage.STUDIO -> StudioPage(
                        serverName = appSettings.server?.url ?: "Home Assistant",
                        tabletName = tabletName,
                        infoOpen = studioInfoOpen,
                        onInfoChange = { studioInfoOpen = it },
                        onBack = { scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.APPEARANCE) } } },
                        onSkip = { scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.MQTT) } } },
                    )
                    VisualPage.MQTT -> MqttPage(
                        onBack = { scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.STUDIO) } } },
                        onSkip = {
                            scope.launch {
                                settings.update { it.copy(onboardingStage = OnboardingStage.CHECKLIST) }
                                haRepository.reconnectNow()
                            }
                        },
                    )
                    VisualPage.CHECKLIST -> ChecklistPage(
                        haConnected = connection is ConnectionState.Connected,
                        dark = darkMode,
                        startView = startView,
                        onBack = { scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.MQTT) } } },
                        onRetry = { scope.launch { haRepository.reconnectNow() } },
                        onLaunch = { scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.LAUNCHING) } } },
                    )
                    VisualPage.LAUNCHING -> Unit
                }
            }
            ProgressEdge(edgeProgress.value, successGlow.value)
        }
    }
}

internal fun resolvedOnboardingStage(
    persistedStage: OnboardingStage,
    credentialsReady: Boolean?,
): OnboardingStage = when {
    persistedStage == OnboardingStage.LEGACY && credentialsReady == true -> OnboardingStage.COMPLETED
    persistedStage == OnboardingStage.LEGACY -> OnboardingStage.WELCOME
    credentialsReady == false && persistedStage !in setOf(OnboardingStage.WELCOME, OnboardingStage.CONNECTION) ->
        OnboardingStage.CONNECTION
    else -> persistedStage
}
