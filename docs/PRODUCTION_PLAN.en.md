# Hapanels Production Plan

## Product Direction

Hapanels is a single native Android app for Home Assistant wall panels. The primary target is Shelly Wall Display hardware, with regular Android tablets supported as a hardware-light fallback.

The app should not use ShellyElevate's Home Assistant WebView as the main experience. Hapanels keeps a native Home Assistant client foundation, while ShellyElevate is used as the reference for hardware access and panel appliance behavior.

## Non-Negotiables

- One application, not Hapanels plus a separate ShellyElevate sidecar.
- Native Home Assistant UI first.
- Shelly Wall Display hardware support first-class.
- Regular Android tablet mode must still work when Shelly-specific hardware is unavailable.
- Physical buttons can control both local Shelly hardware and Home Assistant entities/services.
- MQTT discovery is present from the first hardware milestone.
- Proximity wake, auto-brightness, and screensaver functionality are part of the first production track.

## Architecture

### App Shell

- Keep Hapanels' Home Assistant auth, token storage, REST, WebSocket, and service call infrastructure.
- Rework launch flow toward tablet/wall-panel dashboards instead of small-screen card stack first.
- Keep card stack and quick surfaces only where they remain useful on larger screens.

### Hardware Abstraction

Create a hardware boundary before porting ShellyElevate code:

```kotlin
interface PanelHardware {
    val capabilities: StateFlow<PanelCapabilities>
    val events: Flow<PanelHardwareEvent>
    suspend fun setRelay(id: Int, on: Boolean)
    suspend fun setScreenBrightness(percent: Int)
    suspend fun wakeScreen(reason: WakeReason)
    suspend fun start()
    suspend fun stop()
}
```

Implementations:

- `AndroidTabletHardware`: generic fallback, no relays, no hardware buttons beyond normal Android key events, optional Android sensors.
- `ShellyWallDisplayHardware`: Shelly-specific implementation using selected ShellyElevate native and Java/Kotlin code.

### Shelly Modules To Port

Port selectively from ShellyElevate:

- `InputMonitor` and `shellyinput.cpp` for physical input events.
- `ButtonPressDetector` for short/long/double/triple press classification.
- Device model detection needed to map button and relay counts.
- Sensor manager for temperature, humidity, light, and proximity.
- Relay control path.
- Screen manager behavior for wake, dim, proximity, and screen-off screensavers.
- MQTT discovery builder and publishing model, adapted into Hapanels' settings and lifecycle.

Avoid porting as primary UI:

- HA WebView wrapper.
- JavaScript dashboard bridge.
- ShellyElevate settings screen wholesale.

## Milestones

### Milestone 0: Repository Seed

Status: initial repository setup.

Deliverables:

- Public GitHub repository.
- Hapanels application name and application ID.
- Native Android app builds as Hapanels.
- Production plan and notices added.

Verification:

- `./gradlew :app:assembleGithubDebug`
- `./gradlew :app:assembleGithubRelease`

### Milestone 1: Tablet/Wall-Panel Product Shell

Goal: make the app feel like a wall panel, not a small-screen card-stack client.

Status: done for runtime product shell.

Tasks:

- Create a tablet-first home dashboard route.
- Make dashboard the default launch surface.
- Add panel mode settings: wall panel, tablet, development.
- Add layout density options for 7-10 inch screens.
- Add persistent navigation suitable for landscape and portrait tablets.
- De-emphasize legacy wheel-specific wording in settings and README.

Verification:

- Launches on a normal Android tablet.
- Default screen is useful without card-stack-first interaction.
- Existing HA login and entity/service functionality still works.
- Release/update naming uses Hapanels tags and assets.

### Milestone 2: Hardware Abstraction Layer

Goal: introduce a clean boundary before Shelly-specific code lands.

Status: done for the HAL foundation.

Tasks:

- Add `PanelHardware`, `PanelCapabilities`, and event models.
- Add `AndroidTabletHardware` fallback implementation.
- Add lifecycle integration in `AppGraph` or equivalent dependency wiring.
- Add diagnostics screen showing hardware provider, capabilities, and recent events.
- Add settings for hardware provider mode: auto, generic tablet, Shelly.

Verification:

- Generic tablet build runs with no Shelly native library present.
- Diagnostics show fallback hardware provider.
- Hardware event stream is testable with fake provider.
- Shelly and Android-tablet providers are selected through `PanelHardwareController` and surfaced in Settings/Diagnostics.

### Milestone 3: Shelly Physical Buttons

Goal: physical Shelly buttons work inside Hapanels.

Status: done for current Shelly Wall Display hardware.

Tasks:

- Port `InputMonitor` and native `shellyinput.cpp` with CMake integration.
- Port/adapt `ButtonPressDetector`.
- Detect physical button count per supported Shelly model.
- Convert low-level key events to `PanelButtonEvent(buttonId, pressType)`.
- Add button action mapping settings.
- Add configured action targets for local relay control, HA service calls, and MQTT publishes.

Button press types:

- short
- long
- double
- triple

Initial action targets:

- local relay toggle
- HA service call
- HA scene/script trigger
- current entity toggle
- navigate to dashboard/search/assist/settings
- screen wake/sleep

Verification:

- Test on real Shelly Wall Display.
- Confirm short/long/double/triple press detection.
- Confirm no crash when native input is unavailable.
- Real Shelly button press and event topics have been smoke-tested through Home Assistant MQTT discovery.
- Configured button action resolution is covered for relay, HA service, and MQTT publish targets.

### Milestone 4: Local Relays And Sensors

Goal: Shelly hardware appears as first-class local panel state.

Status: done for relay 1, ambient light, screen brightness, and capability-based local panel tiles; temp/humidity/proximity remain gated on reliable hardware data.

Tasks:

- Port relay control.
- Port temperature, humidity, light, and proximity sensor reads.
- Create local state store for Shelly hardware state.
- Add native UI cards for local relays and sensors.
- Make relay control work even when HA is disconnected.
- Add capability-based `Panel controls` tiles to the favorites picker and card stack.

Verification:

- Relay can be toggled locally from UI and physical buttons.
- Sensors update in diagnostics and dashboard.
- App remains usable without HA connection for local hardware functions.
- Relay 1 and screen brightness were smoke-tested on real Shelly Wall Display hardware.
- Ambient light is exposed when reliable; proximity, temperature, and humidity are not exposed as fake sensors.
- Non-Shelly tablets only see local panel tiles backed by their reported capabilities.

### Milestone 5: MQTT Discovery

Goal: Home Assistant can discover and control panel hardware.

Status: done and smoke-tested against the user's Home Assistant MQTT broker.

Tasks:

- Add MQTT settings: host, port, TLS, username, password, base topic, discovery prefix.
- Add MQTT connection manager.
- Publish availability.
- Publish discovery configs for:
  - relays as `switch`
  - physical buttons as device triggers/events
  - temperature/humidity/light sensors
  - proximity binary sensor
  - screen state / brightness if useful
- Subscribe to relay command topics.
- Re-publish discovery on boot/settings change.

Verification:

- HA MQTT discovery creates one device per panel.
- HA can toggle Shelly relays through MQTT.
- HA receives button events.
- HA receives sensor updates.
- Availability changes on app start/stop/network loss.
- HA can control screen brightness and the auto-brightness switch through MQTT.
- HA receives app/version, hardware-provider, dashboard, screen-mode, target-brightness, and applied-brightness diagnostics.
- HA receives MQTT connection status plus last connect, publish, and subscribe error diagnostics.
- Hapanels publishes retained dashboard config state/meta and accepts dashboard config import/patch commands.

### Milestone 6: Proximity, Brightness, Screensaver

Goal: panel appliance behavior suitable for wall mounting.

Status: done for practical proximity, brightness, and native AOD foundation.

Tasks:

- `PanelScreenManager` lifecycle is wired from app startup.
- Manual screen brightness control works through HA/MQTT and Shelly sysfs, with diagnostics for applied brightness.
- Auto-brightness settings, smoothing, hysteresis, and HA/MQTT switch control are in place.
- Screen mode, target brightness, and applied brightness are published as MQTT diagnostics.
- `WRITE_SETTINGS` is requested/allowed for Shelly so Android does not override hardware brightness writes.
- Proximity wake settings and threshold handling are wired through `PanelScreenManager`.
- Screensaver/AOD timeout, mode state, user activity wake, and last wake/sleep reasons are tracked.
- Native AOD renderer supports clock-only mode and AOD tile mode through the dashboard config model.
- AOD clock style selection is persisted as `always_on_display.clock_style` and can be patched over MQTT/Studio.
- AOD clock style pack is expanded to 13 unique styles, fully integrated with both the client app and Hapanels Studio: default, modern, Warsaw Zaklad, Cyberpunk Korpo, Zew Puszczy, popart, Fabryka Koloru, Italic Editorial, Szeroki, wide bold, Neon Baltic (gradient and streaks), Electric Stained Glass (stained glass and multi-colored numbers), and Poznan Goats (Amber typography with clock-offset and optimized Poznan goats artwork).

Next:
- Optional polish: tune proximity and idle behavior on real mounted Shelly hardware after longer use.
- Optional polish: add richer AOD sources later, such as photo/video slideshow or selected native status widgets, without making Hapanels depend on Lovelace/WebView.

### Milestone 7: Production Hardening

Goal: ship a maintainable panel appliance.

Next:
- Fix Shelly/Android system shade race: after leaving Hapanels Studio, a fast tap near the top-left hamburger can open Android `NotificationShade` and look like a black screen before the app/AOD view returns. Current evidence: app stays foreground, no crash, no FavoritesPicker navigation, `mExpandedPanel=NotificationShade`. Evaluate proper immersive/kiosk handling or a robust top-gesture guard instead of relying on layout padding.
- Boot/autostart.
- Kiosk mode options.
- Floating return-to-app button when Hapanels is backgrounded or hidden.
- Diagnostics export.
- Hardware compatibility matrix.
- Release workflow hardening and signed APK handling.

### Milestone 8: Native Panel Dashboard And HA Config Sync

Goal: let Home Assistant manage Hapanels dashboard configuration while Hapanels renders a polished native Compose wall-panel dashboard.

Status: started.

Tasks:

- Define a native dashboard config model for sections, tiles, people, layout, and AOD settings.
- Seed and cache local dashboard JSON on the panel.
- Render a native dark dashboard with live entity bindings.
- Publish retained dashboard config and metadata over MQTT.
- Accept full config import and revision-checked patch commands over MQTT.
- Build a HACS custom integration so HA can expose dashboard management/config UI.

Verification:

- Native dashboard route renders on the panel.
- Dashboard config survives app restart through local cache.
- HA/MQTT sees dashboard id, revision, and updated-by diagnostics.
- Patch commands reject stale `base_revision` values instead of overwriting newer panel config.

### Milestone 9: Camera Support

Goal: bring camera viewing into the native panel experience in a way that feels closer to Phylax's camera-first UX, while still using Hapanels' native Compose surfaces.

Status: planned.

Tasks:

- Add a native camera browser with list/grid modes and live snapshot polling.
- Add fullscreen camera overlay/detail with fast refresh tuning.
- Extend the dashboard mockup with camera-focused tiles and quick actions.
- Support camera-friendly HA refresh defaults and graceful fallback when no cameras are available.
- Use Phylax as inspiration for camera browsing, live status presentation, and touch-friendly camera detail flows.

Verification:

- Camera entities from HA appear in the native camera browser.
- Grid and fullscreen camera views poll snapshots without stalling the rest of the panel.
- Dashboard mockup shows a dedicated camera tile/section.
- Camera browsing stays usable on both tablets and wall panels.

### Milestone 10: First-Run Setup

Goal: make first launch feel like a real device onboarding flow instead of a raw app start.

Status: done for the production onboarding foundation.

Done:
- Guided first-run welcome, Home Assistant connection, authorization, and personalization screens.
- Home Assistant OAuth sign-in with server probing and token exchange.
- Long-lived access token setup remains available as an onboarding alternative to OAuth.
- Tablet name persists into app settings and HA/MQTT-facing panel identity.
- Panel Grid theme preset selection patches the persisted dashboard config without replacing its light/dark mode, AOD configuration, or tiles.
- Startup choice is limited to `GRID` and `CARDS`, persists across restart, and replaces legacy Today/dashboard startup preferences and launcher links.

Verification:

- Startup stays in onboarding until both a server and non-blank access token are present.
- OAuth and long-lived token paths can complete setup.
- Tablet name, Panel Grid theme, and `GRID` / `CARDS` start view persist after restart.
- Legacy start-view settings and backups migrate compatibly; startup and launcher shortcuts never open Today.
- Onboarding strings have focused Polish localization coverage.

### Milestone 11: Secure MQTT And Studio Onboarding

Goal: include real MQTT and Hapanels Studio setup in first-run onboarding without storing credentials insecurely or showing simulated connection states.

Status: planned.

Tasks:

- Move MQTT credentials from regular DataStore into encrypted storage.
- Migrate existing MQTT credentials and remove plaintext values after successful migration.
- Add MQTT onboarding for host, port, TLS, username, password, connection test, and optional skip.
- Report real broker connection status and actionable connection errors.
- Add Hapanels Studio setup based on actual MQTT/config-sync availability.
- Detect and display real Studio readiness instead of a simulated connected state.
- Add MQTT and Studio results to the final onboarding checklist.

Verification:

- MQTT password never persists in plaintext settings.
- Valid broker credentials establish a real connection.
- Invalid credentials and unreachable brokers show useful errors.
- Studio status reflects actual configuration-sync availability.
- Both steps can be skipped without blocking onboarding.

### Milestone 12: Adaptive Display Brightness

Goal: keep AOD readable during the day and keep both AOD and the active panel comfortable at night.

Status: planned.

Tasks:

- Replace separate AOD and panel brightness behavior with one ambient-light controller.
- Add independently tunable AOD and active-panel brightness curves with calibrated minimum and maximum levels.
- Smooth noisy lux readings and use hysteresis, dwell time, and gradual transitions to prevent visible brightness jumps.
- Prevent feedback where screen brightness changes alter the panel's own ambient-light reading.
- Preserve manual brightness as an explicit override with a clear path back to adaptive mode.
- Expose calibration and diagnostics in Hapanels Studio, including lux, filtered lux, target brightness, applied brightness, and active override source.
- Tune day, evening, and night behavior on mounted Shelly Wall Display hardware.

Verification:

- AOD remains readable in a bright room without running at unnecessary full brightness.
- AOD and the active panel do not dazzle in a dark room.
- Walking toward the panel and leaving AOD produces no flash, dip, or oscillation.
- Rapid or screen-induced lux changes do not cause repeated brightness writes.
- Behavior remains predictable with auto-brightness disabled or the ambient-light sensor unavailable.

## Major Risks

- Shelly hardware code may depend on root or device-specific file paths.
- The inherited card-stack UI is small-screen oriented and needs real tablet UX work.
- MQTT discovery must avoid duplicate device/entity IDs across multiple panels.
- Target SDK differences between Hapanels and ShellyElevate may affect permissions and hardware access.
- Physical button testing requires real Shelly Wall Display hardware.

## First Implementation Recommendation

Start with Milestone 2 and 3 before rewriting large UI surfaces. Physical buttons are the highest-value differentiator and will validate whether Shelly native input can live inside Hapanels cleanly.
