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

Status: mostly done for local relay actions; HA/MQTT action targets still pending.

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
- Button 1 short click defaults to relay 1 toggle when relay 1 exists.
- Short click fires immediately when the same button has no configured double/triple click mapping.
- Long press does not delay short-click relay behavior.
- `PanelMqttBridge` publishes button pressed state and click event topics for HA discovery/state surfaces.

Next:
- Add non-local action targets for button mappings, especially HA service calls and MQTT publish actions.
- Let button action rows choose relay id once more than one relay is supported.
- Validate physical timing on Shelly Wall Display hardware across all five buttons.
- Add or expand tests for configured long/double/triple mappings and immediate-short behavior.

## Milestone 4: Shelly Relays And Sensors

Goal: Shelly relay and sensor state works locally and appears in Hapanels diagnostics/UI.

Status: started.

Done:
- Relay 1 sysfs state read/write helper with unit coverage.
- `ShellyWallDisplayHardware` keeps relay 1 state in `PanelHardwareRuntimeState` after local writes.

Next:
- Validate relay read/write on real Shelly Wall Display hardware.
- Promote temperature/humidity/light/proximity into dashboard cards.

## Milestone 5: MQTT Discovery

Goal: Home Assistant discovers the panel as a device with relays, buttons, sensors, and availability.

Status: mostly done; needs real broker/device smoke validation.

Done:
- MQTT settings for host, port, TLS, username, password, and client id.
- Lightweight MQTT v3.1.1 session with publish, subscribe, ping, and disconnect.
- Discovery config publisher for relays, button pressed state, button click event sensors, and screen brightness.
- Availability publishing on the panel status topic.
- Relay state publishing.
- Button pressed state and button event publishing.
- Ambient light, proximity, and screen brightness state publishing where runtime state provides values.
- Relay command subscriptions via `hapanels/<device>/relay/<id>/set`.
- Screen brightness command subscription via `hapanels/<device>/screen/brightness/set`.
- Unit coverage for MQTT command parsing.

Next:
- Smoke-test discovery and command topics against the user's Home Assistant MQTT broker.
- Add richer MQTT diagnostics for connection status and last publish/subscribe error.
- Add Home Assistant device metadata refinements if HA UI naming needs polish.

## Milestone 6: Proximity, Brightness, Screensaver

Goal: make Hapanels useful as an always-mounted wall panel.

Next:
- Proximity wake.
- Auto-brightness curve.
- Screensaver modes.
- Wake/sleep reason tracking.

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
- Entry points from the card stack, dashboard screen, and app navigation.

Next:
- Add an explicit import/edit/override path for the JSON config.
- Add retained MQTT config topic support with config revision metadata.
- Add local edit patches with `base_revision` conflict handling.
- Build a HACS custom integration so HA can expose dashboard management/config UI.
- Keep Hapanels as the native renderer and avoid WebView/Lovelace dependence unless a specific card requires it.

Maybe later:
- ESPHome Bluetooth proxy support for nearby BLE devices.
