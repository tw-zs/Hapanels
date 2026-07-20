# Instalacja na tablecie z Androidem

Instrukcja instalacji i konfiguracji aplikacji Hapanels na tradycyjnym tablecie ściennym z systemem Android (wymagany Android 9 lub nowszy).

---

### **Krok 1. Pobierz i zainstaluj aplikację**
Pobierz plik `.apk` bezpośrednio z sekcji **[GitHub Releases](https://github.com/tw-zs/Hapanels/releases)** i zainstaluj go na tablecie.

Możesz to zrobić bezpośrednio przez przeglądarkę na tablecie lub za pomocą komputera i ADB:
```bash
adb install -r -d app-github-debug.apk
```

---

### **Krok 2. Uprawnienie jasności (Opcjonalnie)**
Jeśli Twój tablet wspiera automatyczną kontrolę jasności za pośrednictwem systemowych ustawień Androida i chcesz, aby Hapanels nią sterował, nadaj mu uprawnienie komendą ADB:

```bash
adb shell appops set com.github.twzs.hapanels android:write_settings allow
```

---

### **Krok 3. Połącz z Home Assistant**
1. **Zaloguj się:** Otwórz aplikację na tablecie i połącz się ze swoją instancją Home Assistant przez OAuth (adres powrotny logowania to `r1ha://auth-callback`).
2. **Włącz MQTT (Opcjonalnie):** Jeśli masz uruchomiony broker MQTT w Home Assistant, Hapanels automatycznie wykryje panel i doda jego podstawowe stany oraz parametry ekranu do Twojego systemu.
