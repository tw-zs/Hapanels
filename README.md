# Hapanels

[![CI](https://github.com/tw-zs/Hapanels/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/tw-zs/Hapanels/actions/workflows/ci.yml)

<p align="center">
  <img src="docs/assets/hapanels_icon_no_text.svg" alt="Hapanels logo" width="260">
</p>

Hapanels is a native Android Home Assistant wall-panel app for Shelly Wall Display devices and larger Android tablets.

The project starts from the native HA client foundation in [R1HA](https://github.com/itskenny0/R1HA) and is intended to absorb the useful hardware layer ideas from [ShellyElevate](https://github.com/RapierXbox/ShellyElevate) without using ShellyElevate's Home Assistant WebView dashboard as the primary interface.

## Goals

- Native Home Assistant UI for wall panels and tablets, not a Lovelace WebView wrapper.
- One app for Shelly Wall Display and regular Android tablets.
- First-class physical button support on Shelly hardware.
- Local Shelly relay control and Home Assistant service/scene/script control from the same button mapping layer.
- MQTT discovery from the first hardware milestone so Home Assistant can see panel buttons, relays, sensors, and availability.
- Proximity wake, auto-brightness, and screensavers for always-mounted panels.

## Current State

This repository is currently the initial Hapanels seed:

- Based on R1HA's native Home Assistant client code.
- Application identity changed to `Hapanels` / `com.github.twzs.hapanels`.
- Polish localization work from the R1HA experiment is included.
- Shelly hardware integration is planned but not yet ported.

GitHub Actions runs the debug APK build and unit tests on pushes to `main` and pull requests targeting `main`.

See `docs/PRODUCTION_PLAN.md` for the implementation roadmap.

## Development Build

Prerequisites are inherited from R1HA: JDK 17+ and Android SDK with platform/build tools for API 35.

```bash
./gradlew :app:assembleGithubDebug
```

For a smaller installable APK:

```bash
./gradlew :app:assembleGithubRelease
```

## Attribution

- Native Home Assistant client base: [R1HA](https://github.com/itskenny0/R1HA), Unlicense.
- Planned Shelly hardware integration reference: [ShellyElevate](https://github.com/RapierXbox/ShellyElevate), Apache-2.0.

See `NOTICE.md` for details.
