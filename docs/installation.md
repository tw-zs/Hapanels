# Instalacja

## APK

Najprostsza ścieżka to pobranie APK z [GitHub Releases](https://github.com/tw-zs/Hapanels/releases).

```bash
adb install -r -d app-github-debug.apk
```

## Shelly Wall Display

Na Shelly Wall Display nadaj uprawnienie `WRITE_SETTINGS`, jeśli ma działać sterowanie jasnością ekranu:

```bash
adb shell appops set com.github.twzs.hapanels android:write_settings allow
```

## Home Assistant

1. Skonfiguruj połączenie z Home Assistant w aplikacji.
2. Włącz MQTT w Home Assistant, jeśli chcesz auto-discovery panelu.
3. Po starcie Hapanels opublikuje encje panelu jako MQTT discovery.

!!! note
    OAuth redirect URI pozostaje `r1ha://auth-callback`, żeby nie rozbijać istniejącego flow logowania.
