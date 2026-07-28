package com.github.itskenny0.r1ha.feature.onboarding

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.itskenny0.r1ha.R
import com.github.itskenny0.r1ha.core.ha.ConnectionState
import com.github.itskenny0.r1ha.core.ha.HaRepository
import com.github.itskenny0.r1ha.core.prefs.AppSettings
import com.github.itskenny0.r1ha.core.prefs.OnboardingStage
import com.github.itskenny0.r1ha.core.prefs.SettingsRepository
import com.github.itskenny0.r1ha.core.prefs.StartView
import com.github.itskenny0.r1ha.core.prefs.TokenStore
import com.github.itskenny0.r1ha.core.util.R1Log
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
    WELCOME(0), CONNECTION(1), AUTH(2), PANEL_NAME(3), MQTT(4), STUDIO(5), APPEARANCE(6), CHECKLIST(7), LAUNCHING(8),
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
    val context = LocalContext.current
    val vm: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.factory(http, settings, tokens, context.resources),
    )
    val authState by vm.state.collectAsStateWithLifecycle()
    val appSettings by settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val connection by haRepository.connection.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var credentialsReady by remember { mutableStateOf<Boolean?>(null) }
    var discoveredServers by remember { mutableStateOf(emptyList<String>()) }
    var discoveryAttempt by rememberSaveable { mutableStateOf(0) }
    var discoveryRunning by remember { mutableStateOf(false) }
    var discoveryError by remember { mutableStateOf(false) }
    var url by rememberSaveable { mutableStateOf("") }
    var tabletName by rememberSaveable { mutableStateOf("") }
    var startViewName by rememberSaveable { mutableStateOf(StartView.PANEL_GRID.name) }
    var darkMode by rememberSaveable { mutableStateOf(true) }
    var mqttHost by rememberSaveable { mutableStateOf("") }
    var mqttPort by rememberSaveable { mutableStateOf("1883") }
    var mqttUsername by rememberSaveable { mutableStateOf("") }
    var mqttPassword by rememberSaveable { mutableStateOf("") }
    var mqttUseTls by rememberSaveable { mutableStateOf(false) }
    val haBrokerHost = remember(appSettings.server?.url) {
        runCatching { java.net.URI(appSettings.server?.url.orEmpty()).host }.getOrNull().orEmpty()
    }
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
                android.os.Build.MODEL?.takeIf(String::isNotBlank)
                    ?: context.getString(R.string.onboarding_panel_name_placeholder)
            }
        }
    }
    LaunchedEffect(appSettings.behavior.startView) {
        startViewName = appSettings.behavior.startView.name
    }
    LaunchedEffect(appSettings.advanced) {
        mqttHost = appSettings.advanced.mqttHost
        mqttPort = appSettings.advanced.mqttPort.toString()
        mqttUsername = appSettings.advanced.mqttUsername
        mqttPassword = appSettings.advanced.mqttPassword
        mqttUseTls = appSettings.advanced.mqttUseTls
    }
    DisposableEffect(context, discoveryAttempt) {
        val nsd = context.applicationContext.getSystemService(Context.NSD_SERVICE) as? NsdManager
        val listener = nsd?.let { manager ->
            HaNsdDiscovery(
                nsd = manager,
                onStarted = {
                    Handler(Looper.getMainLooper()).post {
                        discoveryRunning = true
                        discoveryError = false
                    }
                },
                onFailed = {
                    Handler(Looper.getMainLooper()).post {
                        discoveryRunning = false
                        discoveryError = true
                    }
                },
                onResolved = { endpoint ->
                    Handler(Looper.getMainLooper()).post {
                        discoveredServers = (discoveredServers + endpoint).distinct().sorted()
                    }
                },
            )
        }
        if (listener != null) {
            runCatching { nsd.discoverServices("_home-assistant._tcp.", NsdManager.PROTOCOL_DNS_SD, listener) }
        }
        onDispose { if (listener != null) runCatching { nsd.stopServiceDiscovery(listener) } }
    }
    LaunchedEffect(dashboardConfigSource) {
        appearanceLoading = true
        runCatching { dashboardConfigSource.loadOrSeed() }
            .onSuccess {
                dashboardConfig = it
                darkMode = it.theme.mode != HapanelsThemeMode.LIGHT
                appearanceError = null
            }
            .onFailure { appearanceError = it.message ?: context.getString(R.string.onboarding_appearance_load_error) }
        appearanceLoading = false
    }

    val persistedStage = appSettings.onboardingStage
    val availableServers = (listOfNotNull(appSettings.server?.url) + discoveredServers).distinct()
    val selectedServer = url.ifBlank { availableServers.firstOrNull().orEmpty() }
    val stage = resolvedOnboardingStage(persistedStage, credentialsReady)
    val page = when {
        stage == OnboardingStage.LAUNCHING -> VisualPage.LAUNCHING
        authState is OnboardingViewModel.State.ReadyToAuth || authState is OnboardingViewModel.State.Exchanging -> VisualPage.AUTH
        stage == OnboardingStage.WELCOME -> VisualPage.WELCOME
        stage == OnboardingStage.CONNECTION -> VisualPage.CONNECTION
        stage == OnboardingStage.PANEL_NAME -> VisualPage.PANEL_NAME
        stage == OnboardingStage.MQTT -> VisualPage.MQTT
        stage == OnboardingStage.STUDIO -> VisualPage.STUDIO
        stage == OnboardingStage.APPEARANCE -> VisualPage.APPEARANCE
        else -> VisualPage.CHECKLIST
    }
    val startView = runCatching { StartView.valueOf(startViewName) }.getOrDefault(StartView.PANEL_GRID)
    val authTopPadding = if (WindowInsets.ime.getBottom(LocalDensity.current) > 0) 0.dp else 230.dp

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
    var launchNavigationStarted by remember { mutableStateOf(false) }
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
                if (screenActive) {
                    launchNavigationStarted = true
                    onComplete(startView)
                }
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
            VisualPage.APPEARANCE -> scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.STUDIO) } }
            VisualPage.STUDIO -> scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.MQTT) } }
            VisualPage.MQTT -> scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.PANEL_NAME) } }
            VisualPage.CHECKLIST -> scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.APPEARANCE) } }
            else -> Unit
        }
    }

    Box(Modifier.fillMaxSize().background(OnboardingBg)) {
        OnboardingBackdrop(showPhoto = page == VisualPage.WELCOME)
        if (page == VisualPage.LAUNCHING ||
            (stage == OnboardingStage.COMPLETED && !launchNavigationStarted)
        ) {
            LaunchSequence(launchProgress.value)
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
                        detectedServers = availableServers,
                        probing = authState is OnboardingViewModel.State.Probing,
                        error = (authState as? OnboardingViewModel.State.Error)?.message,
                        discoveryRunning = discoveryRunning,
                        discoveryError = discoveryError,
                        onRetryDiscovery = {
                            discoveredServers = emptyList()
                            discoveryAttempt += 1
                        },
                        onDetectedServer = { server ->
                            url = server
                            if (authState is OnboardingViewModel.State.Error) vm.resetError()
                        },
                        onBack = {
                            vm.resetError()
                            scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.WELCOME) } }
                        },
                        onOAuth = { vm.probe(selectedServer) },
                        onLlat = { onOpenLongLivedToken?.invoke(selectedServer) },
                    )
                    VisualPage.AUTH -> AuthPage(
                        title = stringResource(if (authState is OnboardingViewModel.State.Exchanging) R.string.onboarding_connecting_title else R.string.onboarding_sign_in_title),
                        description = stringResource(R.string.onboarding_sign_in_description),
                        onBack = vm::resetError,
                    ) {
                        when (val current = authState) {
                            is OnboardingViewModel.State.ReadyToAuth -> OAuthWebView(
                                authorizeUrl = current.authorizeUrl,
                                onCodeCaptured = { vm.exchangeCode(it, current.baseUrl) },
                                onMissingCode = { vm.failOnboarding(context.getString(R.string.onboarding_sign_in_canceled)) },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 240.dp, top = authTopPadding, end = 240.dp, bottom = 16.dp),
                            )
                            else -> {
                                Box(Modifier.offset(240.dp, 250.dp).size(800.dp, 300.dp).background(OnboardingSurface), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = OnboardingOrange)
                                    Text(stringResource(R.string.onboarding_completing_sign_in), color = OnboardingSoft, fontSize = 18.sp, modifier = Modifier.offset(y = 50.dp))
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
                                        onboardingStage = OnboardingStage.MQTT,
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
                                                appearanceError = context.getString(R.string.onboarding_theme_conflict)
                                                return@launch
                                            }
                                        }
                                    }
                                    settings.update {
                                        it.copy(
                                            behavior = it.behavior.copy(startView = startView),
                                            onboardingStage = OnboardingStage.CHECKLIST,
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
                        mqttConfigured = appSettings.advanced.mqttHost.isNotBlank(),
                        infoOpen = studioInfoOpen,
                        onInfoChange = { studioInfoOpen = it },
                        onBack = { scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.MQTT) } } },
                        onSkip = { scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.APPEARANCE) } } },
                    )
                    VisualPage.MQTT -> MqttPage(
                        host = mqttHost,
                        port = mqttPort,
                        username = mqttUsername,
                        password = mqttPassword,
                        useTls = mqttUseTls,
                        hostError = mqttValidationError(
                            mqttHost,
                            mqttPort,
                            stringResource(R.string.onboarding_mqtt_host_error),
                            stringResource(R.string.onboarding_mqtt_port_error),
                        ),
                        onHostChange = { mqttHost = it.trim() },
                        onUseHaHost = { mqttHost = haBrokerHost },
                        onPortChange = { mqttPort = it.filter(Char::isDigit).take(5) },
                        onUsernameChange = { mqttUsername = it },
                        onPasswordChange = { mqttPassword = it },
                        onTlsChange = { mqttUseTls = !mqttUseTls },
                        onBack = { scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.PANEL_NAME) } } },
                        onSkip = {
                            scope.launch {
                                settings.update { it.copy(onboardingStage = OnboardingStage.STUDIO) }
                                haRepository.reconnectNow()
                            }
                        },
                        onSave = {
                            scope.launch {
                                settings.update {
                                    it.copy(
                                        advanced = it.advanced.copy(
                                            mqttHost = mqttHost.trim(),
                                            mqttPort = mqttPort.toInt(),
                                            mqttUsername = mqttUsername.trim(),
                                            mqttPassword = mqttPassword,
                                            mqttUseTls = mqttUseTls,
                                        ),
                                        onboardingStage = OnboardingStage.STUDIO,
                                    )
                                }
                                haRepository.reconnectNow()
                            }
                        },
                    )
                    VisualPage.CHECKLIST -> ChecklistPage(
                        haConnected = connection is ConnectionState.Connected,
                        mqttConfigured = appSettings.advanced.mqttHost.isNotBlank(),
                        dark = darkMode,
                        startView = startView,
                        onBack = { scope.launch { settings.update { it.copy(onboardingStage = OnboardingStage.APPEARANCE) } } },
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

internal fun mqttValidationError(
    host: String,
    port: String,
    hostError: String = "Enter the MQTT broker address or skip this step.",
    portError: String = "Port must be between 1 and 65535.",
): String? = when {
    host.isBlank() -> hostError
    port.toIntOrNull() !in 1..65535 -> portError
    else -> null
}

private class HaNsdDiscovery(
    private val nsd: NsdManager,
    private val onStarted: () -> Unit,
    private val onFailed: () -> Unit,
    private val onResolved: (String) -> Unit,
) : NsdManager.DiscoveryListener {
    private val pending = ArrayDeque<NsdServiceInfo>()
    private val seen = mutableSetOf<String>()
    private var resolving = false

    override fun onDiscoveryStarted(serviceType: String) {
        R1Log.i("Onboarding.NSD", "discovery started type=$serviceType")
        onStarted()
    }

    @Synchronized
    override fun onServiceFound(serviceInfo: NsdServiceInfo) {
        if (!seen.add(serviceInfo.serviceName)) return
        R1Log.i("Onboarding.NSD", "service found name=${serviceInfo.serviceName}")
        pending.addLast(serviceInfo)
        resolveNext()
    }

    override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
    override fun onDiscoveryStopped(serviceType: String) {
        R1Log.i("Onboarding.NSD", "discovery stopped type=$serviceType")
    }

    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
        R1Log.w("Onboarding.NSD", "start failed type=$serviceType code=$errorCode")
        onFailed()
        runCatching { nsd.stopServiceDiscovery(this) }
    }

    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit

    @Synchronized
    private fun resolveNext() {
        if (resolving) return
        val service = pending.removeFirstOrNull() ?: return
        resolving = true
        runCatching {
            nsd.resolveService(service, object : NsdManager.ResolveListener {
                override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = resolved()

                override fun onServiceResolved(info: NsdServiceInfo) {
                    val address = info.host?.hostAddress
                    if (address != null) {
                        val host = if (':' in address) "[$address]" else address
                        val endpoint = "http://$host:${info.port}"
                        R1Log.i("Onboarding.NSD", "service resolved name=${info.serviceName} endpoint=$endpoint")
                        onResolved(endpoint)
                    }
                    resolved()
                }
            })
        }.onFailure { resolved() }
    }

    @Synchronized
    private fun resolved() {
        resolving = false
        resolveNext()
    }
}
