# Instalacja na Shelly Wall Display

Kompletna instrukcja przygotowania i instalacji Hapanels na urządzeniu Shelly Wall Display.

---

### **Krok 0. Przygotowanie Shelly Wall Display**
Musisz najpierw opuścić domyślną aplikację producenta (tryb kiosku). 

Szczegółowa instrukcja odblokowania urządzenia oraz wejścia do systemu Android opisana jest w przewodniku **[ShellyElevate Wiki](https://github.com/RapierXbox/ShellyElevate/wiki/Installation)**. Cały proces odblokowania oraz konfiguracji można przeprowadzić za pomocą połączenia kablowego (ADB USB) lub bezprzewodowego (Wireless ADB).

---

### **Krok 0a. Zmiana domyślnego launchera**
Fabryczne oprogramowanie Shelly (`Stargate`) działa jako domyślny launcher i będzie automatycznie przejmować kontrolę nad ekranem. Aby temu zapobiec, należy zainstalować lekki, zastępczy launcher i wyłączyć fabryczną nakładkę:

1. Pobierz **[Ultra Small Launcher](https://blakadder.com/assets/files/ultra-small-launcher.apk)**.
2. Zainstaluj go oraz wyłącz fabryczny launcher komendami ADB:
```bash
adb install ultra-small-launcher.apk
adb shell pm disable cloud.shelly.stargate
```
3. Wciśnij fizyczny lub wirtualny przycisk Home na urządzeniu. System Android zapyta o wybór domyślnego ekranu startowego — wskaż zainstalowany **Ultra Small Launcher** i zatwierdź jako zawsze domyślny.

---

### **Krok 1. Pobierz i zainstaluj aplikację**
Pobierz plik `.apk` bezpośrednio z sekcji **[GitHub Releases](https://github.com/tw-zs/Hapanels/releases)** i zainstaluj go na urządzeniu.

```bash
adb install -r -d app-github-debug.apk
```

---

### **Krok 2. Uprawnienie jasności**
Aby panel mógł automatycznie kontrolować jasność ekranu Shelly, nadaj mu wymagane uprawnienie systemowe za pomocą komendy ADB:

```bash
adb shell appops set com.github.twzs.hapanels android:write_settings allow
```

---

### **Krok 3. Połącz z Home Assistant**
1. **Zaloguj się:** Otwórz aplikację na urządzeniu i połącz się ze swoją instancją Home Assistant przez OAuth (adres powrotny logowania to `r1ha://auth-callback`).
2. **Włącz MQTT (Opcjonalnie):** Jeśli masz uruchomiony broker MQTT w Home Assistant, Hapanels automatycznie wykryje panel i doda jego fizyczny przekaźnik oraz czujniki do Twojego systemu.
