# Hapanels Alpha: Known Issues

This list covers known limitations in current alpha builds. Report new issues at <https://github.com/tw-zs/Hapanels/issues> with the device model, Android version, app version, Home Assistant version, and steps to reproduce.

## Installation And Updates

- Alpha builds signed with the new Hapanels release key cannot update older debug-signed builds. Uninstall the older app first, then install the alpha APK. Export any dashboard or settings backup you need before uninstalling.
- Install the GitHub APK for alpha testing. The F-Droid flavor does not include the in-app updater.

## Supported Hardware

- Shelly Wall Display is the primary hardware target. Regular Android 9+ tablets are supported with fewer hardware features.
- Relay, physical-button, ambient-light, and proximity behavior depends on the active hardware provider and device capabilities.
- Temperature and humidity stay hidden unless the hardware provides reliable values.

## Dashboard And Studio

- Hapanels Studio preview is useful for editing but is not pixel-perfect compared with the native Android renderer.
- Drilldown panels and field-level dashboard conflict merging are still in progress.
- After leaving Studio on some Shelly/Android builds, a fast tap near the top-left corner can reveal the Android system shade. Reopen Hapanels if this happens.

## Network And Security

- Use HTTPS for Home Assistant whenever possible, especially outside a trusted local network.
- MQTT and Hapanels Studio require the same MQTT base topic, normally `hapanels`.
- MQTT credential encryption is planned work. Use a dedicated, least-privilege broker account for alpha testing.

## Feedback

- Include logs or exported diagnostics when possible.
- Do not include Home Assistant tokens, MQTT passwords, private URLs, or other secrets in public issues.
