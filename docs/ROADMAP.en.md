# Hapanels Roadmap

Hapanels is a native Android panel for Home Assistant. It targets Shelly Wall Display first, while keeping regular Android tablets as a supported fallback.

This is the project's single plan. It combines product direction, current status, and implementation priorities.

## Product Principles

- Native Android and Compose UI first.
- Home Assistant REST/WebSocket for the dashboard.
- MQTT for panel discovery, hardware controls, and dashboard synchronization.
- Shelly hardware support without a separate ShellyElevate sidecar.
- No fake sensor data: expose hardware only when readings are reliable.
- Tablet mode must remain useful when Shelly-specific hardware is unavailable.

## Current Status

### Shipped

- Tablet-first Hapanels product shell and native dashboard.
- Hardware abstraction for Android tablets and Shelly Wall Display.
- Shelly physical buttons with short, long, double, and triple press actions.
- Shelly relay 1, ambient light, brightness, and panel-control tiles.
- MQTT discovery, availability, diagnostics, button events, and relay control.
- Proximity wake, touch wake, auto-brightness, screensaver, and native AOD foundation.
- OAuth and long-lived-token onboarding.
- Hapanels Studio foundation for dashboard/AOD editing and MQTT config sync.
- English, Polish, and German onboarding screens and documentation support.

### In Progress

- Native dashboard drilldown panels.
- Hapanels Studio preview fidelity, layout polish, and conflict merging.
- Production hardening for wall-mounted devices, including the Android system-shade race.

## Next Priorities

### 1. Production Hardening

Make installation and long-term use reliable:

- boot/autostart and kiosk options;
- backup and diagnostics export;
- hardware compatibility matrix;
- signed release workflow;
- real-device regression checklist for Shelly Wall Display.

### 2. Dashboard And Studio Completion

- Open persisted `panel_id` drilldown panels.
- Improve Studio preview geometry, typography, spacing, and mobile layout.
- Show field-level differences when dashboard revisions conflict.
- Keep Hapanels' native renderer independent from Lovelace/WebView.

### 3. Secure MQTT And Onboarding

- Move MQTT credentials to encrypted storage.
- Add guided MQTT setup with connection testing and useful errors.
- Base Studio readiness on real MQTT/config-sync state.
- Keep MQTT and Studio setup optional during first launch.

### 4. Camera Support

- Native camera browser with list and grid views.
- Fullscreen camera view with efficient snapshot refresh.
- Camera tiles and quick actions in the native dashboard.
- Graceful behavior when no cameras are available.

### 5. Adaptive Brightness

- One ambient-light controller for AOD and active panel.
- Calibrated brightness curves with smoothing and hysteresis.
- Manual override with a clear return to adaptive mode.
- Diagnostics for lux, target brightness, applied brightness, and override source.

## Later

- Richer AOD sources such as selected widgets or media slideshows.
- Additional relay and button-action polish.
- ESPHome Bluetooth proxy support for nearby BLE devices.
- More hardware providers when there is a tested use case.

## Verification

Every milestone should be checked at the right level:

- unit tests for state, parsing, migration, and hardware logic;
- Android tablet smoke tests for onboarding, dashboard, AOD, and restart behavior;
- real Shelly Wall Display tests for buttons, relay, brightness, and proximity;
- Home Assistant MQTT tests for discovery, commands, availability, and config sync;
- upgrade tests to confirm settings and dashboard configuration survive updates.

## Constraints And Risks

- Shelly hardware paths can depend on root access and device-specific files.
- Physical hardware behavior cannot be validated fully on an emulator.
- MQTT discovery must avoid duplicate devices and entity IDs.
- Sensor values stay hidden until the hardware provides reliable readings.
- The inherited card-stack UI remains useful, but is not the primary wall-panel experience.

## Contributor Notes

Use this page when changing project priorities or milestone status. Keep implementation details close to the code and update this document only with user-visible outcomes, active priorities, and verification expectations.
