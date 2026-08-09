# Install Hapanels Integration

This page covers only the **Hapanels custom integration for Home Assistant**.

## Requirements

- Home Assistant with the MQTT integration configured.
- Access to your Home Assistant `/config` directory.
- The Hapanels repository or a downloaded copy of its `custom_components/hapanels` folder.

## 1. Copy the Integration

Copy the complete `hapanels` folder from:

```text
custom_components/hapanels
```

to this directory in Home Assistant:

```text
/config/custom_components/hapanels
```

The final path must contain the integration files directly, for example:

```text
/config/custom_components/hapanels/manifest.json
/config/custom_components/hapanels/__init__.py
/config/custom_components/hapanels/config_flow.py
```

Do not create an extra nested folder such as:

```text
/config/custom_components/hapanels/custom_components/hapanels
```

## 2. Restart Home Assistant

Restart Home Assistant after copying the files. The integration is loaded during startup.

## 3. Add the Integration

In Home Assistant, open:

```text
Settings -> Devices & services -> Add integration
```

Search for **Hapanels** and open the integration.

Enter the MQTT base topic used by your panels. The default is:

```text
hapanels
```

Use the same topic in the Hapanels app. Finish the setup flow.

## 4. Confirm Installation

After setup, **Hapanels** should appear in the Home Assistant sidebar.

When a panel publishes its MQTT state, the integration creates a `Dashboard sync` sensor and shows the panel's synchronization status.

## Troubleshooting

- **Hapanels is missing from Add integration:** check that `manifest.json` is directly inside `/config/custom_components/hapanels`, then restart Home Assistant.
- **No panel appears:** confirm that MQTT is connected and the panel uses the same base topic, normally `hapanels`.
- **Integration does not load:** check **Settings -> System -> Logs** for `hapanels` errors.
- **Files changed but nothing updated:** restart Home Assistant again; custom integrations are loaded at startup.

The Android app is installed separately. Use the [tablet installation guide](installation-tablet.md) or [Shelly Wall Display installation guide](installation-shelly.md).
