# Home Assistant Integration

Hapanels connects wall panels and Android tablets to Home Assistant in two parts:

- **Hapanels app** runs on the panel and displays the native dashboard.
- **Hapanels integration** runs in Home Assistant and manages panel discovery and dashboard synchronization.

The app uses Home Assistant's native REST/WebSocket connection for the dashboard. MQTT is optional for the local dashboard, but required for panel hardware discovery and Hapanels Studio synchronization.

## What You Need

- Home Assistant. Add an MQTT broker if you want hardware discovery or dashboard synchronization.
- An Android 9+ tablet or Shelly Wall Display.
- The Hapanels APK from [GitHub Releases](https://github.com/tw-zs/Hapanels/releases).
- This repository, if installing the custom integration manually.

## Installation

### 1. Install the app on the panel

Choose the guide for your hardware:

- [Android tablet installation](installation-tablet.md)
- [Shelly Wall Display installation](installation-shelly.md)

Open Hapanels, connect it to Home Assistant, and complete onboarding. If you use MQTT, configure the broker in the app. The default MQTT base topic is:

```text
hapanels
```

### 2. Install the Home Assistant integration

Copy this directory from the repository:

```text
custom_components/hapanels
```

to your Home Assistant configuration directory:

```text
/config/custom_components/hapanels
```

Restart Home Assistant. Then open:

```text
Settings -> Devices & services -> Add integration -> Hapanels
```

Complete the config flow and keep the base topic as `hapanels` unless you changed it in the app.

## What Happens Next

When the panel publishes its MQTT discovery and state, Home Assistant receives:

- the panel's hardware entities when supported by the active hardware provider;
- availability and connection diagnostics;
- dashboard synchronization status from the custom integration;
- the current dashboard revision and last editor.

The Hapanels panel appears in the Home Assistant sidebar. It lists discovered panels and exposes whether each panel is `synced`, `conflict`, `invalid`, or `unknown`.

## Dashboard Synchronization

The app publishes retained synchronization state to:

```text
hapanels/<device>/dashboard/config/sync/state
```

Example:

```json
{
  "status": "synced",
  "dashboard_id": "home-panel-main",
  "revision": 44,
  "updated_by": "homeassistant:hapanels_studio"
}
```

If the app reports that a patch used an old revision, the integration exposes `conflict` instead of hiding that state.

## Hapanels Studio

Hapanels Studio is the Home Assistant panel for managing the native dashboard. It is used to:

- preview the dashboard and AOD screen;
- edit tiles and labels;
- publish full dashboard configurations or small patches;
- detect and resolve stale revisions.

This is a native Hapanels dashboard workflow, not a Lovelace WebView wrapper.

## Troubleshooting

If the panel does not appear:

1. Confirm that the panel is connected to Home Assistant.
2. If you use MQTT, confirm that the broker is running and configured in Hapanels.
3. Check that both sides use base topic `hapanels`.
4. Restart Home Assistant after copying or updating `custom_components/hapanels`.
5. Check MQTT logs for a topic containing `hapanels/<device>/dashboard/config/sync/state`.
