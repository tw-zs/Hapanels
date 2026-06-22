# Hapanels Roadmap

## Current Baseline

Hapanels is currently a native Home Assistant Android panel app with Hapanels branding, a public GitHub repo, a white splash screen, panel-oriented hardware diagnostics, and a native tablet dashboard mockup.

## Milestone 1: Product Shell

Goal: make the app feel like a wall panel/tablet app instead of a small-screen card-stack client.

Status: done for runtime product shell.

Done:
- Branding, icon, README, and panel diagnostics entry points.
- Tablet-first dashboard default for fresh installs.
- Panel hardware/provider mode settings.
- Visible runtime legacy wording replaced with Hapanels/panel copy on primary surfaces.
- Landscape/portrait shell tuning through responsive breakpoints and tablet dashboard layouts.
- Release/update naming uses Hapanels tags, assets, client ids, cache names, and runtime identifiers.
- Non-runtime comments were swept so product-shell documentation describes Hapanels as a panel app.

Next:
- Optional future cleanup: migrate inherited package/internal type names if the cost is justified.

## Milestone 2: Hardware Abstraction Layer

Goal: introduce a clean hardware boundary that can run on normal Android tablets and Shelly Wall Display.

Status: done for the HAL foundation.

Done:
- `PanelHardware` interface.
- `AndroidTabletHardware` fallback provider.
- `ShellyWallDisplayHardware` safe stub provider.
- `PanelHardwareController` with `AUTO / TABLET / SHELLY` mode.
- Runtime status in Settings and `PANEL HARDWARE` diagnostics.
- Live Android sensor reads for ambient light, proximity, and screen brightness where the OS exposes them.
- Tests around provider mode persistence and controller provider switching.
- Controller lifecycle is idempotent and `stop()` cancels settings/provider forwarding.
- Active provider capability and runtime-state updates are forwarded through the controller.
- Shelly-native button and relay runtime state now flows through the same HAL boundary.

Next:
- Optional polish: localize all remaining hardware diagnostics labels.
- Optional polish: add richer diagnostics around provider start/stop failures.

## Milestone 3: Shelly Physical Buttons

Goal: physical Shelly buttons produce reliable button events and can trigger local or HA actions.

Status: done for current Shelly Wall Display hardware.

Done:
- Shared button event model.
- Pressed-button runtime state field.
- `PanelButtonPressDetector` for short, long, double, and triple presses.
- `InputMonitor` / `shellyinput.cpp` JNI wiring.
- Low-level Shelly input key mapping to `PanelHardwareEvent.Button` events.
- Live `pressedButtonIds` updates in `PanelHardwareRuntimeState`.
- Button action mapping settings for buttons 1-5.
- Button action rows for press, release, short click, long press, double click, and triple click.
- Local relay actions for configured button mappings: none, toggle relay, relay on, and relay off.
- Non-local configured button actions for Home Assistant service calls and MQTT publishes.
- Settings UI fields for HA service domain/name/data JSON and MQTT topic/payload/retain.
- Button 1 short click defaults to relay 1 toggle when relay 1 exists.
- Short click fires immediately when the same button has no configured double/triple click mapping.
- Long press does not delay short-click relay behavior.
- `PanelMqttBridge` publishes button pressed state and click event topics for HA discovery/state surfaces.

Next:
- Optional future polish: let button action rows choose relay id once more than one relay is supported.
- Optional future polish: add built-in navigation/screen action targets if they prove useful on the mounted panel.

## Milestone 4: Shelly Relays And Sensors

Goal: Shelly relay and sensor state works locally and appears in Hapanels diagnostics/UI.

Status: done for relay 1, real ambient-light exposure, and capability-based panel control tiles.

Done:
- Relay 1 sysfs state read/write helper with unit coverage.
- `ShellyWallDisplayHardware` keeps relay 1 state in `PanelHardwareRuntimeState` after local writes.
- Ambient light and proximity runtime readings are sanitized so invalid sensor values are treated as missing.
- MQTT discovery exposes ambient light only when the provider reports a reliable sensor; proximity is intentionally not exposed on Shelly until Android delivers usable events.
- Temperature/humidity are intentionally not exposed until the hardware provides reliable readings.
- Relay 1 read/write was smoke-tested on real Shelly Wall Display hardware through Home Assistant MQTT.
- Shelly screen brightness writes use the real sysfs backlight path while HA/MQTT keeps a stable 0-100% contract.
- Favorites picker has a `Kontrola panelu` section for local panel tiles, filtered by current hardware capabilities.
- Local panel tiles can render relay 1, screen brightness, auto-brightness, ambient light, and panel status without fake sensor data.

Next:
- Add temperature/humidity only when a reliable hardware or integration source exists.

## Milestone 5: MQTT Discovery

Goal: Home Assistant discovers the panel as a device with relays, buttons, sensors, and availability.

Status: done and smoke-tested against the user's Home Assistant MQTT broker.

Done:
- MQTT settings for host, port, TLS, username, password, and client id.
- Lightweight MQTT v3.1.1 session with publish, subscribe, ping, and disconnect.
- Discovery config publisher for relays, button pressed state, button click event sensors, and screen brightness.
- Availability publishing on the panel status topic.
- Relay state publishing.
- Button pressed state and button event publishing.
- Ambient light, proximity, and screen brightness state publishing where runtime state provides values.
- Ambient light and proximity discovery configs when the active provider exposes those sensors.
- Relay command subscriptions via `hapanels/<device>/relay/<id>/set`.
- Screen brightness command subscription via `hapanels/<device>/screen/brightness/set`.
- Screen auto-brightness switch discovery and command subscription via `hapanels/<device>/screen/auto_brightness/set`.
- Dashboard config retained state/meta topics plus config import and patch command topics.
- App online, app version, hardware provider, dashboard metadata, screen mode, target brightness, and applied brightness diagnostics.
- MQTT connection status plus last connect, publish, and subscribe error diagnostics.
- Home Assistant device triggers for physical button events.
- Unit coverage for MQTT command parsing.
- Real Home Assistant smoke tests for relay 1, brightness, auto-brightness, availability, diagnostics, and dashboard metadata.

Next:
- Add Home Assistant device metadata refinements if HA UI naming needs polish.

## Milestone 6: Proximity, Brightness, Screensaver

Goal: make Hapanels useful as an always-mounted wall panel.

Status: foundation started; brightness and screen diagnostics are usable, full AOD/screensaver UX remains pending.

Done:
- `PanelScreenManager` lifecycle is wired from app startup.
- Manual screen brightness control works through HA/MQTT and Shelly sysfs, with diagnostics for applied brightness.
- Auto-brightness settings, smoothing, hysteresis, and HA/MQTT switch control are in place.
- Screen mode, target brightness, and applied brightness are published as MQTT diagnostics.
- `WRITE_SETTINGS` is requested/allowed for Shelly so Android does not override hardware brightness writes.
- AOD configuration placeholder exists in the dashboard config model as `always_on_display`.

Next:
- Proximity wake.
- Screensaver modes.
- Wake/sleep reason tracking.
- AOD/screensaver research: evaluate which ideas from `j-a-n/lovelace-wallpanel` can map to native Hapanels, especially idle timeout, fullscreen/chrome hiding, wake lock, motion/wake triggers, photo/video slideshow sources, and overlaying selected HA cards or status widgets. Reuse concepts/config shape where useful, but do not make Hapanels depend on Lovelace/WebView for the AOD renderer.

## Milestone 7: Production Hardening

Goal: ship a maintainable panel appliance.

Next:
- Boot/autostart.
- Kiosk mode options.
- Floating return-to-app button when Hapanels is backgrounded or hidden.
- Diagnostics export.
- Hardware compatibility matrix.
- Release workflow hardening and signed APK handling.

## Milestone 8: Native Panel Dashboard And HA Config Sync

Goal: let Home Assistant manage Hapanels dashboard configuration while Hapanels renders a polished native Compose wall-panel dashboard.

Status: started.

Done:
- Native dark panel-grid mockup route in Compose.
- Tablet-oriented grid with clock, people, action tiles, large room tiles, and compact status tiles.
- Local Nunito font resources for the new panel dashboard.
- Dashboard config model with tile/person/layout types and sample JSON.
- Local dashboard config source that seeds and caches `hapanels_dashboard_config.json`.
- Explicit local dashboard JSON export/import/reset controls in Appearance settings.
- Live entity binding for supported dashboard tile `entity_id` values via `HaRepository.observe`.
- Retained MQTT dashboard config state/meta topics plus inbound `dashboard/config/set` import support.
- Local dashboard edit patches with `base_revision` conflict detection and MQTT `dashboard/config/patch/set` support.
- Entry points from the card stack, dashboard screen, and app navigation.

Next:
- Build a HACS custom integration so HA can expose dashboard management/config UI.
- Keep Hapanels as the native renderer and avoid WebView/Lovelace dependence unless a specific card requires it.

## Milestone 9: Camera Support

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

Maybe later:
- ESPHome Bluetooth proxy support for nearby BLE devices.

## Milestone 10: First-Run Setup

Goal: make first launch feel like a real device onboarding flow instead of a raw app start.

Status: planned.

Tasks:
- First-run screen with a tablet name field.
- Initial setup hints for the most important panel settings.
- Save the chosen tablet name into app and HA-facing identity surfaces.
- Small UX polish for the first-launch flow so it feels guided and not technical.

Verification:
- Fresh install shows onboarding before the main dashboard.
- Tablet name can be entered and persists after restart.
- Onboarding completes cleanly into normal panel use.
