# Hapanels Roadmap

## Current Baseline

Hapanels is currently a rebranded R1HA-based native Home Assistant Android app with Hapanels branding, a public GitHub repo, a white splash screen, and a panel-oriented hardware diagnostics foundation.

## Milestone 1: Product Shell

Goal: make the app feel like a wall panel/tablet app instead of an R1-first client.

Status: mostly done for runtime product shell.

Done:
- Branding, icon, README, and panel diagnostics entry points.
- Tablet-first dashboard default for fresh installs.
- Panel hardware/provider mode settings.
- Visible runtime R1/R1HA wording replaced with Hapanels/panel copy on primary surfaces.

Next:
- Landscape/portrait wall-panel layout tuning.
- Sweep non-runtime comments and inherited release/update naming when the package/release pipeline is renamed.

## Milestone 2: Hardware Abstraction Layer

Goal: introduce a clean hardware boundary that can run on normal Android tablets and Shelly Wall Display.

Status: in progress.

Done:
- `PanelHardware` interface.
- `AndroidTabletHardware` fallback provider.
- `ShellyWallDisplayHardware` safe stub provider.
- `PanelHardwareController` with `AUTO / TABLET / SHELLY` mode.
- Runtime status in Settings and `PANEL HARDWARE` diagnostics.
- Live Android sensor reads for ambient light, proximity, and screen brightness where the OS exposes them.
- Tests around provider mode persistence and controller provider switching.

Next:
- Keep runtime state populated by Shelly-native buttons, relays, and sensors once native access lands.

## Milestone 3: Shelly Physical Buttons

Goal: physical Shelly buttons produce reliable button events and can trigger local or HA actions.

Status: in progress.

Done:
- Shared button event model.
- Pressed-button runtime state field.
- `PanelButtonPressDetector` for short, long, double, and triple presses.
- `InputMonitor` / `shellyinput.cpp` JNI wiring.
- Low-level Shelly input key mapping to `PanelHardwareEvent.Button` events.
- Live `pressedButtonIds` updates in `PanelHardwareRuntimeState`.
- Button action mapping settings for press, release, and short-click local relay actions.

Next:
- Extend button action mapping to long/double/triple and HA/MQTT actions.

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

Next:
- MQTT connection manager.
- Discovery config publisher.
- Availability publishing.
- Relay command subscriptions.
- Button event publishing.

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
- Release workflow cleanup from inherited R1HA naming.

Maybe later:
- Native Lovelace/dashboard renderers, ported selectively from upstream R1HA after the panel/Shelly core is stable.
- ESPHome Bluetooth proxy support for nearby BLE devices.
