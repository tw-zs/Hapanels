# Milestone 6 Agent Brief: Proximity, Brightness, Screensaver

## Project Links

- GitHub repo: https://github.com/tw-zs/Hapanels
- Main branch compare page for PRs: https://github.com/tw-zs/Hapanels/compare
- Roadmap: https://github.com/tw-zs/Hapanels/blob/main/docs/ROADMAP.md
- Android SensorManager docs: https://developer.android.com/reference/android/hardware/SensorManager
- Android Sensor docs: https://developer.android.com/reference/android/hardware/Sensor
- Android Settings.System docs: https://developer.android.com/reference/android/provider/Settings.System
- Android WindowManager.LayoutParams docs: https://developer.android.com/reference/android/view/WindowManager.LayoutParams
- Home Assistant MQTT Discovery docs: https://www.home-assistant.io/integrations/mqtt/#mqtt-discovery
- Shelly Wall Display product page: https://www.shelly.com/products/shelly-wall-display

## Assignment

Implement Milestone 6: Proximity, Brightness, Screensaver.

Goal: make Hapanels useful as an always-mounted wall panel by adding practical screen management driven by proximity, ambient light, and user-configurable screensaver behavior.

Expected result: a clean PR that adds the screen manager foundation, settings, runtime state, and tests without destabilizing the existing Shelly hardware/button work.

## Current Baseline

Hapanels is an Android/Kotlin/Compose native Home Assistant panel app.

Important current files:
- `app/src/main/kotlin/com/github/itskenny0/r1ha/core/hardware/PanelHardware.kt`
- `app/src/main/kotlin/com/github/itskenny0/r1ha/core/hardware/AndroidTabletHardware.kt`
- `app/src/main/kotlin/com/github/itskenny0/r1ha/core/hardware/ShellyWallDisplayHardware.kt`
- `app/src/main/kotlin/com/github/itskenny0/r1ha/core/hardware/PanelHardwareController.kt`
- `app/src/main/kotlin/com/github/itskenny0/r1ha/core/hardware/PanelMqttBridge.kt`
- `app/src/main/kotlin/com/github/itskenny0/r1ha/core/prefs/AppSettings.kt`
- `app/src/main/kotlin/com/github/itskenny0/r1ha/core/prefs/SettingsRepository.kt`
- `app/src/main/kotlin/com/github/itskenny0/r1ha/feature/settings/SettingsScreen.kt`
- `app/src/main/kotlin/com/github/itskenny0/r1ha/ui/i18n/PolishText.kt`

Existing hardware state already includes:
- `PanelHardwareRuntimeState.proximityDistanceCm`
- `PanelHardwareRuntimeState.ambientLightLux`
- `PanelHardwareRuntimeState.screenBrightnessPercent`
- `WakeReason.PROXIMITY`
- `WakeReason.BUTTON`
- `WakeReason.AUTOMATION`

Existing provider behavior:
- `AndroidTabletHardware` registers Android light and proximity sensors when present.
- `ShellyWallDisplayHardware` also registers Android light and proximity sensors when present.
- `setScreenBrightness(percent)` and `wakeScreen(reason)` currently emit unsupported-action events.
- Current screen brightness is read from `Settings.System.SCREEN_BRIGHTNESS` but not actively controlled.

## Scope

Build a minimal but production-shaped panel screen manager.

Required features:
- Proximity wake when the panel detects near presence.
- Auto-brightness curve from ambient lux to screen brightness percent.
- Screensaver mode after idle timeout.
- Wake/sleep reason tracking in runtime state or a dedicated screen state model.
- Settings UI for enabling/disabling these features and tuning basic thresholds.
- Diagnostics visibility for current lux, proximity, brightness, screen mode, and last wake/sleep reason.

Preferred shape:
- Add a small `PanelScreenManager` or similarly named class under `core/hardware` or a nearby core package.
- Keep `PanelHardware` focused on hardware capabilities and low-level actions.
- Let the manager observe `PanelHardware.runtimeState`, app settings, and app lifecycle as needed.
- Use Android APIs for brightness/wake in one place, not scattered through screens.
- Add unit tests for the brightness curve and proximity/sleep state transitions.

## Non-Goals

- Do not rewrite the hardware abstraction layer.
- Do not change package name `com.github.itskenny0.r1ha`.
- Do not introduce a WebView screensaver.
- Do not implement dashboard config sync in this PR.
- Do not block Milestone 3 button behavior or relay behavior.
- Do not require Shelly-only APIs for generic Android tablet operation.

## Implementation Guidance

Brightness:
- Start with a deterministic curve function, for example `lux -> percent` with configurable min/max brightness.
- Clamp percent to `0..100`.
- Prefer per-window brightness through `WindowManager.LayoutParams.screenBrightness` where possible.
- Avoid requiring `WRITE_SETTINGS` for the MVP unless absolutely necessary.
- Keep a clear fallback if brightness cannot be changed.

Proximity wake:
- Treat a small proximity distance as near presence.
- Add a debounce/cooldown so noisy sensors do not rapidly wake/sleep.
- Call the screen manager wake path with `WakeReason.PROXIMITY`.
- Button events should also be able to wake with `WakeReason.BUTTON` if the app is in screensaver/dimmed mode.

Screensaver:
- Add an idle timeout setting.
- MVP screensaver can be dim/black overlay or a simple clock-first panel overlay.
- It must be reversible by proximity, touch, or button event.
- Keep UI native Compose.

Settings:
- Add settings under the existing Advanced/Panel hardware area.
- User-facing strings should be Polish or routed through `PolishText.kt` when practical.
- Suggested settings:
  - Enable proximity wake.
  - Enable auto brightness.
  - Minimum brightness percent.
  - Maximum brightness percent.
  - Screensaver enabled.
  - Screensaver timeout seconds/minutes.

Runtime/diagnostics:
- Show current screen manager mode, for example active, dimmed, screensaver.
- Show last wake reason and last sleep reason.
- Preserve existing hardware diagnostics.

## Acceptance Criteria

- App compiles with JDK 17.
- Existing Shelly button actions still compile and tests still pass.
- Brightness curve has unit coverage.
- Screen manager state transitions have unit coverage, or are split so logic can be tested without Android framework dependencies.
- Settings persist through `SettingsRepository`.
- Screensaver can be enabled/disabled from settings.
- Proximity and ambient light sensor absence does not crash and leaves clear diagnostics/fallback state.
- PR description explains supported behavior on generic Android tablets and expected behavior on Shelly Wall Display.

## Verification Commands

Use JDK 17. The local known-good JDK path on the main development machine is `/home/tomek/Pobrane/jdk17`.

```bash
JAVA_HOME="/home/tomek/Pobrane/jdk17" ./gradlew --no-daemon -Dkotlin.compiler.execution.strategy=in-process :app:compileGithubDebugKotlin
JAVA_HOME="/home/tomek/Pobrane/jdk17" ./gradlew --no-daemon -Dkotlin.compiler.execution.strategy=in-process :app:testGithubDebugUnitTest
JAVA_HOME="/home/tomek/Pobrane/jdk17" ./gradlew --no-daemon -Dkotlin.compiler.execution.strategy=in-process :app:assembleGithubDebug
```

If Gradle gets stuck or a daemon/cache issue appears, stop daemons and retry:

```bash
JAVA_HOME="/home/tomek/Pobrane/jdk17" ./gradlew --stop
```

Optional device smoke test:

```bash
adb install -r -d app/build/outputs/apk/github/debug/app-github-debug.apk
adb shell am start -n com.github.twzs.hapanels/com.github.itskenny0.r1ha.MainActivity
adb shell pidof com.github.twzs.hapanels
adb logcat -d -t 200 AndroidRuntime:E '*:S'
```

## PR Instructions

1. Create a feature branch from latest `main`, for example `feat/milestone-6-screen-manager`.
2. Keep the PR focused on Milestone 6 only.
3. Include tests for pure logic.
4. Include screenshots or a short note for the settings/screensaver UI if possible.
5. Do not commit local SDK paths or generated build outputs.
6. Open a PR against `main` in https://github.com/tw-zs/Hapanels.

Suggested PR title:

```text
feat: add panel proximity brightness screensaver manager
```

Suggested PR summary:

```text
- Adds a panel screen manager for proximity wake, auto brightness, and screensaver state.
- Adds persisted settings and diagnostics for screen behavior.
- Adds tests for brightness curve and screen state transitions.
```

## Known Constraints

- `minSdk` is intentionally panel/Shelly-oriented and currently not being lowered.
- The app still uses inherited package paths under `com.github.itskenny0.r1ha`.
- The APK package/application id is `com.github.twzs.hapanels`.
- The current direct launch component is `com.github.twzs.hapanels/com.github.itskenny0.r1ha.MainActivity`.
- Avoid changing OAuth redirect behavior.
- Avoid broad rewrites of Settings unless needed for the specific screen manager settings.
