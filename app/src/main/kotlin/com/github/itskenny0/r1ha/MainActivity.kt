package com.github.itskenny0.r1ha

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.FileObserver
import android.provider.Settings
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import com.github.itskenny0.r1ha.ui.components.ToastHost
import com.github.itskenny0.r1ha.ui.components.r1Pressable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import com.github.itskenny0.r1ha.core.input.WheelEvent
import com.github.itskenny0.r1ha.core.hardware.PanelScreenMode
import com.github.itskenny0.r1ha.core.hardware.PanelScreenManager
import com.github.itskenny0.r1ha.core.hardware.WakeReason
import com.github.itskenny0.r1ha.core.ha.Domain
import com.github.itskenny0.r1ha.core.ha.EntityId
import com.github.itskenny0.r1ha.core.ha.EntityState
import com.github.itskenny0.r1ha.core.ha.HaRepository
import com.github.itskenny0.r1ha.core.prefs.AppSettings
import com.github.itskenny0.r1ha.core.prefs.WheelKeySource
import com.github.itskenny0.r1ha.core.theme.LocalUiOptions
import com.github.itskenny0.r1ha.core.theme.R1
import com.github.itskenny0.r1ha.core.theme.R1ThemeHost
import com.github.itskenny0.r1ha.core.util.R1Log
import com.github.itskenny0.r1ha.core.util.Toaster
import com.github.itskenny0.r1ha.feature.panelgrid.HapanelsDashboardConfigSource
import com.github.itskenny0.r1ha.feature.panelgrid.HapanelsTileAccent
import com.github.itskenny0.r1ha.feature.panelgrid.HapanelsTileConfig
import com.github.itskenny0.r1ha.feature.panelgrid.HapanelsTileKind
import com.github.itskenny0.r1ha.feature.panelgrid.HapanelsTileSize
import com.github.itskenny0.r1ha.feature.panelgrid.PanelIcons
import com.github.itskenny0.r1ha.nav.AppNavGraph
import com.github.itskenny0.r1ha.nav.Routes
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var graph: AppGraph
    private var lastTouchActivityAtMillis = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        R1Log.i("MainActivity.onCreate", "data=${intent?.data}")

        graph = (application as App).graph
        requestWriteSettingsPermissionIfShelly()

        // Tell the window manager we support all orientations BEFORE setContent / setContentView
        // so the system sizes the window correctly from frame 0. If we wait until a LaunchedEffect
        // fires (after the first frame), AOSP 12+ and derivative ROMs (LineageOS, crDroid) have
        // already applied their large-screen phone-compat letterbox policy and the window is stuck
        // at phone dimensions. FULL_USER means "all 4 orientations, respect the user's rotation
        // lock" — the most permissive option that still honours the system rotation setting.
        // The PORTRAIT_ONLY user preference overrides this in the LaunchedEffect below.
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_USER

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        handleOAuthCallback(intent)

        setContent {
            // Load the FIRST settings value synchronously (suspending) before we render the
            // NavHost. Otherwise we'd mount Onboarding briefly (initialValue.server is null)
            // and then jarringly switch to CardStack once the Flow emitted. produceState
            // returns null until the coroutine assigns the first value.
            val initialSettings by produceState<AppSettings?>(initialValue = null) {
                value = graph.settings.settings.first()
            }
            val settings by graph.settings.settings.collectAsStateWithLifecycle(
                initialValue = initialSettings ?: AppSettings(),
            )
            DisposableEffect(Unit) {
                graph.panelScreenManager.attachWindow(window)
                onDispose { graph.panelScreenManager.detachWindow(window) }
            }

            val initial = initialSettings
            if (initial == null) {
                // Splashscreen API keeps the system-level splash up until the activity is
                // ready to draw; we additionally render a blank surface to avoid any flash
                // until the first settings emission is in hand.
                Box(modifier = Modifier.fillMaxSize())
                return@setContent
            }
            // Cold-start app-shortcut delivery — if the user launched
            // us via a launcher long-press shortcut, the route to push
            // is sitting in the original intent's extras. Forward it
            // to the ShortcutBus so AppNavGraph picks it up on its
            // first compose tick. (onNewIntent handles subsequent
            // shortcut taps while the app is already running.)
            androidx.compose.runtime.LaunchedEffect(Unit) {
                intent.getStringExtra(EXTRA_INITIAL_ROUTE)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { route ->
                        R1Log.i("MainActivity.setContent", "cold-start shortcut route: $route")
                        com.github.itskenny0.r1ha.core.util.ShortcutBus.request(route)
                    }
            }

            // Lock the start destination to the FIRST loaded value so theme changes, server
            // changes, etc. don't re-graph the NavHost mid-session. Two paths:
            //   - server == null         → ONBOARDING
            //   - server + startOnDashboard → DASHBOARD (wall-mounted / kiosk panel path)
            //   - server + default        → CARD_STACK (manual control path)
            val startDestination = remember(initial) {
                when {
                    initial.server == null -> Routes.ONBOARDING
                    initial.behavior.startOnDashboard -> Routes.DASHBOARD
                    else -> Routes.CARD_STACK
                }
            }
            val navController = rememberNavController()
            R1Log.d("MainActivity.setContent", "startDestination=$startDestination server=${initial.server?.url ?: "null"}")

            // Live setting changes. PORTRAIT_ONLY overrides the FULL_USER set in onCreate;
            // FOLLOW_DEVICE reinstates FULL_USER. Both are immediate — no restart needed.
            androidx.compose.runtime.LaunchedEffect(settings.behavior.orientationMode) {
                requestedOrientation = when (settings.behavior.orientationMode) {
                    com.github.itskenny0.r1ha.core.prefs.OrientationMode.PORTRAIT_ONLY ->
                        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    com.github.itskenny0.r1ha.core.prefs.OrientationMode.FOLLOW_DEVICE ->
                        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_USER
                }
            }

            // Honour the user's "Hide status bar" toggle live — flipping it in Settings
            // applies immediately without an activity restart. WindowInsetsController is
            // the recommended API since SDK 30; we already require min 30 so no fallback
            // path is needed.
            androidx.compose.runtime.LaunchedEffect(settings.behavior.hideStatusBar) {
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                if (settings.behavior.hideStatusBar) {
                    controller.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                    // Make the user-swipe-to-show transient (auto-hides after a beat) so
                    // peeking the bar to check the time doesn't permanently break the
                    // hidden state.
                    controller.systemBarsBehavior =
                        androidx.core.view.WindowInsetsControllerCompat
                            .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    controller.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                }
            }

            // Apply the toast-log level setting whenever it changes. R1Toast is a
            // process-scope object so we just update its flags; the bus host
            // composable reads them at push time. Off → toast UI is silent;
            // ERROR/WARN/INFO/DEBUG raise the threshold progressively.
            androidx.compose.runtime.LaunchedEffect(settings.behavior.toastLogLevel) {
                val level = settings.behavior.toastLogLevel
                com.github.itskenny0.r1ha.core.util.R1Toast.enabled =
                    level != com.github.itskenny0.r1ha.core.prefs.ToastLogLevel.OFF
                com.github.itskenny0.r1ha.core.util.R1Toast.minLevel = when (level) {
                    com.github.itskenny0.r1ha.core.prefs.ToastLogLevel.OFF,
                    com.github.itskenny0.r1ha.core.prefs.ToastLogLevel.ERROR ->
                        com.github.itskenny0.r1ha.core.util.R1Toast.Level.ERROR
                    com.github.itskenny0.r1ha.core.prefs.ToastLogLevel.WARN ->
                        com.github.itskenny0.r1ha.core.util.R1Toast.Level.WARN
                    com.github.itskenny0.r1ha.core.prefs.ToastLogLevel.INFO ->
                        com.github.itskenny0.r1ha.core.util.R1Toast.Level.INFO
                    com.github.itskenny0.r1ha.core.prefs.ToastLogLevel.DEBUG ->
                        com.github.itskenny0.r1ha.core.util.R1Toast.Level.DEBUG
                }
            }
            // Track the current HA access token so deep image-fetch composables
            // (album art on media_player cards, primarily) can authenticate against
            // HA's media-proxy endpoints. Key produceState on the Connected-state's
            // haVersion so the token re-loads once per successful WS reconnect —
            // which is also when TokenRefresher has just rotated the access token —
            // without thrashing the Keystore on the rapid Connecting / Authenticating
            // / Disconnected bounces that come from a flaky network. haVersion is null
            // outside Connected, so transitions away from Connected and back fire
            // exactly one re-read.
            val connection by graph.haRepository.connection
                .collectAsStateWithLifecycle(initialValue = graph.haRepository.connection.value)
            val connectedHaVersion = (connection as? com.github.itskenny0.r1ha.core.ha.ConnectionState.Connected)?.haVersion
            val bearerToken by produceState<String?>(initialValue = null, connectedHaVersion) {
                value = runCatching { graph.tokens.load()?.accessToken }.getOrNull()
            }
            // Effective theme — auto-mode swaps to the night theme between the
            // configured night hours. produceState ticks every minute so the
            // crossover at 22:00 / 06:00 happens without waiting for the next
            // settings emission. The auto flag short-circuits the tick when off
            // (keepers of constant-theme installs don't pay for a recompose-
            // per-minute they don't need).
            val themeNow by androidx.compose.runtime.produceState(
                initialValue = settings.theme,
                settings.theme, settings.nightTheme, settings.autoThemeEnabled,
                settings.nightStartHour, settings.nightEndHour,
            ) {
                while (true) {
                    val now = java.time.LocalTime.now()
                    val hour = now.hour
                    val night = if (!settings.autoThemeEnabled) false
                    else if (settings.nightStartHour == settings.nightEndHour) false
                    else if (settings.nightStartHour < settings.nightEndHour) {
                        hour in settings.nightStartHour until settings.nightEndHour
                    } else {
                        // Wrap-around window — e.g. 22 → 06 — night is "outside
                        // the day window."
                        hour >= settings.nightStartHour || hour < settings.nightEndHour
                    }
                    value = if (night) settings.nightTheme else settings.theme
                    // Sleep until the top of the next minute so the crossover
                    // happens precisely at the configured boundary instead of
                    // up-to-60-seconds late.
                    val msUntilMinute = 60_000L - (System.currentTimeMillis() % 60_000L)
                    kotlinx.coroutines.delay(msUntilMinute.coerceAtLeast(1_000L))
                }
            }
            R1ThemeHost(themeId = themeNow) {
                val baseDensity = LocalDensity.current
                val panelDensity = remember(baseDensity) {
                    Density(
                        density = baseDensity.density * PANEL_UI_SCALE,
                        fontScale = baseDensity.fontScale,
                    )
                }
                CompositionLocalProvider(
                    LocalUiOptions provides settings.ui,
                    com.github.itskenny0.r1ha.core.theme.LocalHaBearerToken provides bearerToken,
                    LocalDensity provides panelDensity,
                ) {
                    // Wrap the nav graph in a Box so the in-app ToastHost can
                    // overlay every navigated screen. The toast bus is process-
                    // scoped (see R1Toast); the host just renders whatever event
                    // it last received as long as the toast feature is enabled.
                    //
                    // On the R1 (≤ 360 dp wide) the ResponsiveColumn wrapper is
                    // a passthrough — every screen renders bit-for-bit
                    // identical to before. On larger displays (phones,
                    // tablets) the wrapper paints the bezel area with the
                    // theme background and centres each screen inside a
                    // bounded column so the layout stays legible instead of
                    // stretching widgets across a 1200 dp panel. Per-screen
                    // exceptions can opt out by reading currentWidthTier()
                    // directly (Cameras GRID does this to keep its grid
                    // column count adaptive without losing the centering).
                    Box(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxSize()
                            .background(com.github.itskenny0.r1ha.core.theme.R1.Bg),
                    ) {
                        com.github.itskenny0.r1ha.ui.layout.ResponsiveColumn {
                            AppNavGraph(
                                navController = navController,
                                startDestination = startDestination,
                                haRepository = graph.haRepository,
                                settings = graph.settings,
                                tokens = graph.tokens,
                                wheelInput = graph.wheelInput,
                                panelHardware = graph.panelHardware,
                                panelScreenManager = graph.panelScreenManager,
                            )
                        }
                        PanelScreensaverOverlay(
                            mode = graph.panelScreenManager.state
                                .collectAsStateWithLifecycle()
                                .value
                                .mode,
                            haRepository = graph.haRepository,
                            dashboardConfigSource = graph.dashboardConfigSource,
                            panelScreenManager = graph.panelScreenManager,
                            onWake = { graph.panelScreenManager.reportUserActivity(WakeReason.USER) },
                        )
                        // Toast host sits OUTSIDE the responsive column so
                        // toasts always pop at the device's true screen
                        // edges, not the centred column's edges.
                        ToastHost()
                    }
                }
            }
        }
    }

    /**
     * If Android delivers the OAuth redirect to us as a deep-link intent (instead of being
     * intercepted by the WebView's `shouldOverrideUrlLoading`), surface it visibly so we can
     * debug. The WebView's interception is the primary path; this is a safety net.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        R1Log.i("MainActivity.onNewIntent", "data=${intent.data}")
        setIntent(intent) // so subsequent intent.getStringExtra reads see the new intent
        handleOAuthCallback(intent)
        // App-shortcut deep-link — fire the requested route through
        // the in-memory ShortcutBus so the nav-graph can pop it onto
        // its back stack. We don't navigate from here directly because
        // the NavController lives inside the Compose tree.
        intent.getStringExtra(EXTRA_INITIAL_ROUTE)?.takeIf { it.isNotBlank() }?.let { route ->
            R1Log.i("MainActivity.onNewIntent", "shortcut routed: $route")
            com.github.itskenny0.r1ha.core.util.ShortcutBus.request(route)
        }
    }

    /**
     * Coming back to the foreground after the app was backgrounded — kick a
     * reconnect if we're not currently connected. Backgrounded apps on R1
     * (and Android in general) frequently have their WS torn down by the
     * OS power saver; without an explicit nudge here the user would tap
     * back in, see stale data, and wonder why nothing updates until our
     * backoff timer fires. Cheap to call when already connected: the repo
     * short-circuits on the existing connection.
     */
    override fun onResume() {
        super.onResume()
        if (!::graph.isInitialized) return
        val conn = graph.haRepository.connection.value
        // Resume-time reconnect kicks ONLY out of Disconnected / Idle. AuthLost has its
        // own refresh + reconnect loop owned by the repository; piling onResume on top
        // would produce a visible flicker (try → 401 → try → 401) until the loop's
        // refresh path runs to completion, since the token is the same one that just
        // got rejected.
        val needsKick = conn is com.github.itskenny0.r1ha.core.ha.ConnectionState.Disconnected ||
            conn is com.github.itskenny0.r1ha.core.ha.ConnectionState.Idle
        if (needsKick) {
            R1Log.i("MainActivity.onResume", "kicking reconnect; conn=$conn")
            graph.haRepository.reconnectNow()
        }
        // Engage NFC reader mode while the activity is foregrounded — the
        // NfcReader checks the per-feature toggle internally before firing
        // HA events, so calling bind() with the toggle off is a cheap no-op.
        com.github.itskenny0.r1ha.feature.nfc.NfcReader.bind(this)
    }

    override fun onPause() {
        super.onPause()
        // Release reader mode; without this another foreground app would have
        // to wait for our adapter to time out before its own NFC features
        // could engage. Safe to call when bind() was a no-op.
        com.github.itskenny0.r1ha.feature.nfc.NfcReader.unbind(this)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked
        val shouldReportTouch = action == MotionEvent.ACTION_DOWN ||
            action == MotionEvent.ACTION_UP ||
            action == MotionEvent.ACTION_CANCEL ||
            ev.eventTime - lastTouchActivityAtMillis >= 1_000L
        if (::graph.isInitialized && shouldReportTouch) {
            lastTouchActivityAtMillis = ev.eventTime
            graph.panelScreenManager.reportUserActivity(WakeReason.USER)
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun handleOAuthCallback(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "r1ha" || data.host != "auth-callback") return
        val code = data.getQueryParameter("code")
        val error = data.getQueryParameter("error")
        if (!code.isNullOrBlank()) {
            R1Log.i("MainActivity.handleOAuth", "deep-link delivered code (len=${code.length})")
            Toaster.show("Deep-link delivered OAuth code (WebView should have caught this)", long = true)
        } else {
            R1Log.w("MainActivity.handleOAuth", "deep-link with no code; error=$error")
            Toaster.error("Deep-link with no code: error=$error")
        }
    }

    private fun requestWriteSettingsPermissionIfShelly() {
        val looksLikeShelly = listOf(Build.MANUFACTURER, Build.BRAND, Build.MODEL, Build.DEVICE, Build.PRODUCT)
            .any { it.contains("shelly", ignoreCase = true) || it.contains("blake", ignoreCase = true) }
        if (!looksLikeShelly || Settings.System.canWrite(this)) return
        runCatching {
            startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
            )
        }.onFailure { t ->
            R1Log.w("MainActivity", "WRITE_SETTINGS permission request failed: ${t.message}", t)
        }
    }

    /** Wall-clock of the last VOLUME-driven wheel emit, per direction.
     *  Lets us throttle the framework's ~30 Hz auto-repeat down to a
     *  more sensible cadence while still letting a held button drive
     *  continuous motion on phones / tablets (legacy physical wheel
     *  emits each detent as a discrete ACTION_DOWN so this only kicks
     *  in for VOLUME keycodes). */
    private var lastVolumeRepeatUp: Long = 0L
    private var lastVolumeRepeatDown: Long = 0L

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (::graph.isInitialized && event.action == KeyEvent.ACTION_DOWN) {
            graph.panelScreenManager.reportUserActivity(WakeReason.USER)
        }
        // Honour the user's "Key source" setting (AUTO = both, DPAD = only D-pad keys, VOLUME =
        // only volume keys). Filtered-out keycodes fall through to super so the system can act
        // on them normally (e.g. volume keys actually change media volume when the user has
        // explicitly chosen DPAD only).
        val src = graph.latestKeySource
        val acceptsDpad = src == WheelKeySource.AUTO || src == WheelKeySource.DPAD
        val acceptsVolume = src == WheelKeySource.AUTO || src == WheelKeySource.VOLUME
        val isDown = event.action == KeyEvent.ACTION_DOWN
        // For physical VOLUME buttons, the framework synthesises auto-repeat events at ~30 Hz
        // when the user holds the button. Firing on every repeat would run brightness/volume
        // away in milliseconds; ignoring every repeat (the original behaviour) forced the user
        // to tap the button N times to make a meaningful adjustment on a phone/tablet. We
        // compromise: emit on every initial press (repeatCount == 0) AND throttle the
        // auto-repeat stream to ~8 Hz so a held button gives smooth, controllable motion.
        //
        // Legacy physical wheels map to DPAD keycodes and emit each detent as a separate
        // ACTION_DOWN with repeatCount=0 — those bypass the throttle entirely so a fast spin
        // never loses an event.
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> if (acceptsDpad) {
                if (isDown) graph.wheelInput.emit(WheelEvent.Direction.UP)
                true
            } else super.dispatchKeyEvent(event)
            KeyEvent.KEYCODE_VOLUME_UP -> if (acceptsVolume) {
                if (isDown && shouldEmitVolumeRepeat(event, isUp = true)) {
                    graph.wheelInput.emit(WheelEvent.Direction.UP)
                }
                true
            } else super.dispatchKeyEvent(event)
            KeyEvent.KEYCODE_DPAD_DOWN -> if (acceptsDpad) {
                if (isDown) graph.wheelInput.emit(WheelEvent.Direction.DOWN)
                true
            } else super.dispatchKeyEvent(event)
            KeyEvent.KEYCODE_VOLUME_DOWN -> if (acceptsVolume) {
                if (isDown && shouldEmitVolumeRepeat(event, isUp = false)) {
                    graph.wheelInput.emit(WheelEvent.Direction.DOWN)
                }
                true
            } else super.dispatchKeyEvent(event)
            else -> super.dispatchKeyEvent(event)
        }
    }

    /** Decide whether this VOLUME ACTION_DOWN should produce a wheel
     *  emit. The first press (repeatCount == 0) always fires. Subsequent
     *  framework-synthesised repeats are accepted at most every
     *  [VOLUME_REPEAT_MIN_MS] ms — calibrated to ~8 Hz which feels like
     *  a controllable manual dial spin rather than a runaway. */
    private fun shouldEmitVolumeRepeat(event: KeyEvent, isUp: Boolean): Boolean {
        if (event.repeatCount == 0) {
            // Reset the throttle so the first press is always honoured
            // AND the next auto-repeat measures its delta from this
            // moment instead of any stale previous-burst timestamp.
            if (isUp) lastVolumeRepeatUp = event.eventTime
            else lastVolumeRepeatDown = event.eventTime
            return true
        }
        val last = if (isUp) lastVolumeRepeatUp else lastVolumeRepeatDown
        val delta = event.eventTime - last
        if (delta < VOLUME_REPEAT_MIN_MS) return false
        if (isUp) lastVolumeRepeatUp = event.eventTime
        else lastVolumeRepeatDown = event.eventTime
        return true
    }

    companion object {
        private const val PANEL_UI_SCALE = 1.25f

        /** Minimum gap between successive wheel emits when a VOLUME
         *  button is held. ~130 ms ≈ 7.7 Hz — the same cadence a
         *  practised thumb on a physical wheel can manage, so
         *  the held-volume-button feel matches a manual spin. */
        private const val VOLUME_REPEAT_MIN_MS = 130L

        /** Intent extra used by the app-shortcut definitions (see
         *  res/xml/shortcuts.xml) to ask MainActivity to deep-link
         *  to a specific top-level route on launch. The value is a
         *  bare route name (e.g. "search", "assist") that AppNavGraph
         *  resolves via Routes constants. */
        const val EXTRA_INITIAL_ROUTE = "initial_route"
    }
}

@androidx.compose.runtime.Composable
private fun PanelScreensaverOverlay(
    mode: PanelScreenMode,
    haRepository: HaRepository,
    dashboardConfigSource: HapanelsDashboardConfigSource,
    panelScreenManager: PanelScreenManager,
    onWake: () -> Unit,
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = mode == PanelScreenMode.SCREENSAVER,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 900,
                easing = androidx.compose.animation.core.FastOutSlowInEasing,
            ),
        ),
        exit = androidx.compose.animation.fadeOut(
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 180,
                easing = androidx.compose.animation.core.LinearEasing,
            ),
        ),
    ) {
        val now by produceState(initialValue = java.time.LocalTime.now()) {
            while (true) {
                value = java.time.LocalTime.now()
                kotlinx.coroutines.delay(1_000L)
            }
        }
        val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
        val config by produceState(
            initialValue = null as com.github.itskenny0.r1ha.feature.panelgrid.HapanelsAlwaysOnDisplayConfig?,
            key1 = mode,
            key2 = dashboardConfigSource,
        ) {
            if (mode != PanelScreenMode.SCREENSAVER) {
                null
            } else {
                val changes = Channel<Unit>(Channel.CONFLATED)
                val realObserver = object : FileObserver(
                    context.filesDir.absolutePath,
                    CLOSE_WRITE or CREATE or MOVED_TO,
                ) {
                    override fun onEvent(event: Int, path: String?) {
                        if (path == "hapanels_dashboard_config.json") changes.trySend(Unit)
                    }
                }
                realObserver.startWatching()
                try {
                    value = runCatching { dashboardConfigSource.loadOrSeed().alwaysOnDisplay }.getOrNull()
                    for (ignored in changes) {
                        value = runCatching { dashboardConfigSource.loadOrSeed().alwaysOnDisplay }.getOrNull()
                    }
                } finally {
                    realObserver.stopWatching()
                    changes.close()
                }
            }
        }
        val aod = config
        val tiles = aod?.tiles.orEmpty().sortedBy { it.order }
        val observedEntityIds = remember(tiles) { tiles.mapNotNull { it.entityId.toAodEntityIdOrNull() }.toSet() }
        val liveEntities by produceState<Map<EntityId, EntityState>>(
            initialValue = emptyMap(),
            key1 = haRepository,
            key2 = observedEntityIds,
        ) {
            if (observedEntityIds.isEmpty()) {
                value = emptyMap()
            } else {
                haRepository.observe(observedEntityIds).collect { value = it }
            }
        }
        val background = aod?.background?.toComposeColor() ?: Color.Black
        val brightnessPercent = aod?.brightnessPercent ?: 100
        val contentAlpha = (0.35f + (brightnessPercent.coerceIn(0, 100) / 100f) * 0.65f)
            .coerceIn(0.35f, 1f)
        androidx.compose.runtime.LaunchedEffect(mode, aod?.brightnessPercent, panelScreenManager) {
            if (mode == PanelScreenMode.SCREENSAVER && aod != null) {
                panelScreenManager.setAodBrightnessOverride(aod.brightnessPercent)
            }
        }
        DisposableEffect(mode, panelScreenManager) {
            onDispose {
                if (mode == PanelScreenMode.SCREENSAVER) {
                    panelScreenManager.setAodBrightnessOverride(null)
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .r1Pressable(onWake),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            if (tiles.isEmpty()) {
                AodClock(now = now, alpha = contentAlpha, modifier = Modifier.padding(24.dp))
            } else {
                AodGrid(
                    tiles = tiles,
                    liveEntities = liveEntities,
                    now = now,
                    columns = aod?.gridLayout?.columnsLandscape?.coerceIn(1, 4) ?: 3,
                    alpha = contentAlpha,
                    modifier = Modifier.padding(18.dp),
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun AodGrid(
    tiles: List<HapanelsTileConfig>,
    liveEntities: Map<EntityId, EntityState>,
    now: LocalTime,
    columns: Int,
    alpha: Float,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        tiles.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { tile ->
                    AodTile(
                        tile = tile,
                        liveState = tile.entityId.toAodEntityIdOrNull()?.let(liveEntities::get),
                        now = now,
                        alpha = alpha,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun AodTile(
    tile: HapanelsTileConfig,
    liveState: EntityState?,
    now: LocalTime,
    alpha: Float,
    modifier: Modifier,
) {
    if (tile.kind == HapanelsTileKind.CLOCK) {
        AodClock(now = now, alpha = alpha, modifier = modifier.height(128.dp))
        return
    }
    Column(
        modifier = modifier
            .height(if (tile.size == HapanelsTileSize.LARGE) 128.dp else 96.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.06f * alpha))
            .padding(12.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PanelIcons.Icon(tile.icon, tint = tile.accent.aodColor(alpha), modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(8.dp))
        com.github.itskenny0.r1ha.ui.i18n.Text(
            text = tile.shortLabel?.takeIf { it.isNotBlank() } ?: tile.label,
            style = R1.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        tile.aodLiveLabel(liveState)?.let {
            com.github.itskenny0.r1ha.ui.i18n.Text(
                text = it,
                style = R1.labelMicro,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun AodClock(now: LocalTime, alpha: Float, modifier: Modifier) {
    val today = LocalDate.now()
    val dateText = buildString {
        append(today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("pl", "PL")))
        append(", ")
        append(today.format(DateTimeFormatter.ofPattern("dd MMMM", Locale("pl", "PL"))))
    }
    Column(
        modifier = modifier,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        com.github.itskenny0.r1ha.ui.i18n.Text(
            text = now.format(DateTimeFormatter.ofPattern("HH:mm")),
            style = R1.numeralXl.copy(letterSpacing = 0.sp),
            color = Color.White,
        )
        com.github.itskenny0.r1ha.ui.i18n.Text(
            text = dateText,
            style = R1.body.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun HapanelsTileAccent.aodColor(alpha: Float): Color = when (this) {
    HapanelsTileAccent.ORANGE -> Color(0xFFE99900)
    HapanelsTileAccent.RED -> Color(0xFFFF5338)
    HapanelsTileAccent.WHITE -> Color.White
}.copy(alpha = 0.86f * alpha)

private fun String?.toAodEntityIdOrNull(): EntityId? =
    this?.takeIf { it.isNotBlank() }?.let { runCatching { EntityId(it) }.getOrNull() }

private fun HapanelsTileConfig.aodLiveLabel(liveState: EntityState?): String? {
    if (entityId.isNullOrBlank()) return null
    val entity = entityId.toAodEntityIdOrNull() ?: return "niewspierane"
    val state = liveState ?: return entity.objectId
    if (!state.isAvailable) return "niedostępne"
    return when (entity.domain) {
        Domain.LIGHT,
        Domain.SWITCH,
        Domain.INPUT_BOOLEAN,
        Domain.AUTOMATION,
        -> if (state.isOn) "włączone" else "wyłączone"
        Domain.COVER,
        Domain.VALVE,
        -> state.percent?.let { "$it%" } ?: state.rawState.aodUnknown()
        Domain.CLIMATE,
        Domain.WATER_HEATER,
        -> state.climateCurrentTemperature?.let { "${it.aodNumber()}°" }
            ?: state.climateTargetTemperature?.let { "${it.aodNumber()}°" }
            ?: state.climateHvacMode.aodUnknown()
        Domain.SENSOR,
        Domain.NUMBER,
        Domain.INPUT_NUMBER,
        -> listOfNotNull(state.rawState, state.unit).joinToString(" ").ifBlank { "nieznane" }
        Domain.BINARY_SENSOR -> if (state.isOn) "wykryto" else "brak"
        else -> state.rawState.aodUnknown()
    }
}

private fun String?.aodUnknown(): String = this?.takeIf { it.isNotBlank() } ?: "nieznane"

private fun Double.aodNumber(): String =
    if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.1f", this)

private fun String.toComposeColor(): Color = runCatching {
    val hex = removePrefix("#")
    val value = hex.toLong(16)
    when (hex.length) {
        6 -> Color((0xFF000000 or value).toInt())
        8 -> Color(value.toInt())
        else -> Color.Black
    }
}.getOrDefault(Color.Black)
