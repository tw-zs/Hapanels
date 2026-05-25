# Hapanels Production Plan

## Product Direction

Hapanels is a single native Android app for Home Assistant wall panels. The primary target is Shelly Wall Display hardware, with regular Android tablets supported as a hardware-light fallback.

The app should not use ShellyElevate's Home Assistant WebView as the main experience. R1HA's native Home Assistant client remains the foundation, while ShellyElevate is used as the reference for hardware access and panel appliance behavior.

## Non-Negotiables

- One application, not R1HA plus a separate ShellyElevate sidecar.
- Native Home Assistant UI first.
- Shelly Wall Display hardware support first-class.
- Regular Android tablet mode must still work when Shelly-specific hardware is unavailable.
- Physical buttons can control both local Shelly hardware and Home Assistant entities/services.
- MQTT discovery is present from the first hardware milestone.
- Proximity wake, auto-brightness, and screensaver functionality are part of the first production track.

## Architecture

### App Shell

- Keep R1HA's Home Assistant auth, token storage, REST, WebSocket, and service call infrastructure.
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
- R1HA base builds as Hapanels.
- Production plan and notices added.

Verification:

- `./gradlew :app:assembleGithubDebug`
- `./gradlew :app:assembleGithubRelease`

### Milestone 1: Tablet/Wall-Panel Product Shell

Goal: make the app feel like a wall panel, not a Rabbit R1 client.

Tasks:

- Create a tablet-first home dashboard route.
- Make dashboard the default launch surface.
- Add panel mode settings: wall panel, tablet, development.
- Add layout density options for 7-10 inch screens.
- Add persistent navigation suitable for landscape and portrait tablets.
- De-emphasize R1 wheel-specific wording in settings and README.

Verification:

- Launches on a normal Android tablet.
- Default screen is useful without R1-style card-stack interaction.
- Existing HA login and entity/service functionality still works.

### Milestone 2: Hardware Abstraction Layer

Goal: introduce a clean boundary before Shelly-specific code lands.

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

### Milestone 3: Shelly Physical Buttons

Goal: physical Shelly buttons work inside Hapanels.

Tasks:

- Port `InputMonitor` and native `shellyinput.cpp` with CMake integration.
- Port/adapt `ButtonPressDetector`.
- Detect physical button count per supported Shelly model.
- Convert low-level key events to `PanelButtonEvent(buttonId, pressType)`.
- Add button action mapping settings.

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

### Milestone 4: Local Relays And Sensors

Goal: Shelly hardware appears as first-class local panel state.

Tasks:

- Port relay control.
- Port temperature, humidity, light, and proximity sensor reads.
- Create local state store for Shelly hardware state.
- Add native UI cards for local relays and sensors.
- Make relay control work even when HA is disconnected.

Verification:

- Relay can be toggled locally from UI and physical buttons.
- Sensors update in diagnostics and dashboard.
- App remains usable without HA connection for local hardware functions.

### Milestone 5: MQTT Discovery

Goal: Home Assistant can discover and control panel hardware.

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

### Milestone 6: Proximity, Brightness, Screensaver

Goal: panel appliance behavior suitable for wall mounting.

Tasks:

- Port/adapt proximity wake behavior.
- Add screensaver modes:
  - black/off
  - clock
  - clock + date
  - HA summary card
- Add auto-brightness from light sensor.
- Add brightness curve settings: min, max, smoothing, night mode.
- Add inactivity timeout and wake reasons.

Verification:

- Screen wakes on proximity.
- Screensaver engages after timeout.
- Brightness changes smoothly with ambient light.
- Regular tablets without proximity still work with timeout/touch wake.

### Milestone 7: Production Hardening

Goal: make Hapanels installable and maintainable.

Tasks:

- Boot receiver/autostart option.
- Kiosk mode options.
- Backup/restore Hapanels settings.
- Crash and diagnostic bundle export.
- Hardware compatibility matrix.
- Release workflow and signed APK handling.
- Manual test checklist for Shelly Wall Display.

Verification:

- Fresh install setup works.
- Upgrade preserves settings.
- Reboot autostarts when enabled.
- Debug bundle gives enough data to troubleshoot hardware/MQTT/HA issues.

## Major Risks

- Shelly hardware code may depend on root or device-specific file paths.
- R1HA's current UI is small-screen/card-stack oriented and needs real tablet UX work.
- MQTT discovery must avoid duplicate device/entity IDs across multiple panels.
- Target SDK differences between R1HA and ShellyElevate may affect permissions and hardware access.
- Physical button testing requires real Shelly Wall Display hardware.

## First Implementation Recommendation

Start with Milestone 2 and 3 before rewriting large UI surfaces. Physical buttons are the highest-value differentiator and will validate whether Shelly native input can live inside the R1HA-derived app cleanly.
