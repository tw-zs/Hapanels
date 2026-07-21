# Hapanels - Plan produkcji

## 🎯 Kierunek produktu

Hapanels to pojedyncza natywna aplikacja Android dla panelowych urządzeń Home Assistant. Głównym celem jest sprzęt **Shelly Wall Display**, z regularnymi tabletami Android jako lekką alternatywą.

**Zasady:**

- ❌ **Nie** używać ShellyElevate WebView jako głównego doświadczenia
- ✅ **Tak** zachować natywny fundament klienta Home Assistant
- ✅ **Tak** traktować ShellyElevate jako referencję dla dostępu do sprzętu i zachowania panelu appliance

## 🚫 Non-Negotiables (Nienegocjowalne)

- ✅ **Jedna aplikacja** – nie Hapanels + oddzielny ShellyElevate sidecar
- ✅ **Natywny interfejs Home Assistant jako pierwszy**
- ✅ **Pierwszorzędne wsparcie dla Shelly Wall Display**
- ✅ **Tryb tabletu Android musi działać**, gdy sprzęt Shelly jest niedostępny
- ✅ **Fizyczne przyciski mogą kontrolować** zarówno lokalny sprzęt Shelly, jak i encje/usługi Home Assistant
- ✅ **MQTT discovery obecne od pierwszego milestону sprzętowego**
- ✅ **Proximity wake, auto-brightness i screensaver** są częścią pierwszego toru produkcyjnego

## 🏗️ Architektura

### App Shell

- Zachować infrastrukturę autoryzacji HA, przechowywania tokenów, REST, WebSocket i wywołań usług Hapanels
- Przerobić flow uruchomienia w kierunku dashboardów tablet/panel zamiast małego ekranu card stack
- Zachować card stack i szybkie powierzchnie tylko tam, gdzie pozostają użyteczne na większych ekranach

### Abstrakcja sprzętu

Utworzyć granicę sprzętową przed portowaniem kodu ShellyElevate:

```kotlin
interface PanelHardware {
    val capabilities: StateFlow<PanelCapabilities>
    val events: Flow<PanelHardwareEvent>
    suspend fun setRelay(id: Int, on: Boolean)
    suspend fun setScreenBrightness(percent: Int)
    suspend fun wakeScreen(reason: WakeReason)
    suspend fun start()
    suspend fun stop()
}
```

**Implementacje:**

- `AndroidTabletHardware`: ogólny fallback, brak przekaźników, brak sprzętowych przycisków poza normalnymi zdarzeniami klawiszy Android, opcjonalne czujniki Android
- `ShellyWallDisplayHardware`: implementacja specyficzna dla Shelly używająca wybranego kodu ShellyElevate (native i Java/Kotlin)

### Moduły Shelly do portowania

Selektywnie portować z ShellyElevate:

- ✅ `InputMonitor` i `shellyinput.cpp` dla zdarzeń wejścia fizycznego
- ✅ `ButtonPressDetector` dla klasyfikacji short/long/double/triple press
- ✅ Wykrywanie modelu urządzenia potrzebne do mapowania liczby przycisków i przekaźników
- ✅ Menadżer czujników dla temperatury, wilgotności, światła i zbliżeniowego
- ✅ Ścieżka kontroli przekaźników
- ✅ Zachowanie menadżera ekranu dla wake, dim, zbliżeniowego i screensaverów ekranu-off
- ✅ Builder discovery MQTT i model publikowania, zaadaptowany do ustawień i lifecycle Hapanels

**Unikać portowania jako głównego UI:**

- ❌ HA WebView wrapper
- ❌ JavaScript dashboard bridge
- ❌ Cały ekran ustawień ShellyElevate

## 📅 Kamienie milowe

### Milestone 0: Inicjalizacja repozytorium

**Status:** ✅ Ukończony.

**Dostarczone:**
- Publiczne repozytorium GitHub
- Nazwa i ID aplikacji Hapanels
- Natywne budowanie aplikacji Android jako Hapanels
- Dodanie planu produkcji i notatki

**Weryfikacja:**
- `./gradlew :app:assembleGithubDebug` ✅
- `./gradlew :app:assembleGithubRelease` ✅

---

### Milestone 1: Powłoka produktu Tablet/Panel ścienny

**Cel:** Sprawić, aby aplikacja czuła się jak panel ścienny, a nie klient na małe ekrany.

**Status:** ✅ Zakończony dla runtime product shell.

**Zadania:**
- Utworzyć domyślny dashboard dla tabletów
- Sprawić, aby dashboard był domyślną powierzchnią uruchomienia
- Dodać ustawienia trybu panelu: panel ścienny, tablet, development
- Dodać opcje gęstości układu dla ekranów 7-10 cali
- Dodać nawigację persystentną odpowiednią dla landscape i portrait tabletów
- Zminimalizować dziedziczone sformułowania związane z kołem w ustawieniach i README

**Weryfikacja:**
- Uruchamia się na normalnym tablecie Android ✅
- Domyślny ekran jest użyteczny bez interakcji opartych na card stack ✅
- Istniejąca funkcjonalność logowania HA i encji/usług wciąż działa ✅
- Nazewnictwo release/update używa tagów i assetów Hapanels ✅

---

### Milestone 2: Warstwa abstrakcji sprzętu

**Cel:** Wprowadzić czystą granicę przed lądowaniem kodu specyficznego dla Shelly.

**Status:** ✅ Zakończony dla fundamentu HAL.

**Zadania:**
- Dodać `PanelHardware`, `PanelCapabilities` i modele zdarzeń
- Dodać implementację fallback `AndroidTabletHardware`
- Dodać integrację lifecycle w `AppGraph` lub równoważne okablowanie zależności
- Dodać ekran diagnostyki pokazujący provider sprzętu, capability i ostatnie zdarzenia
- Dodać ustawienia trybu providera sprzętu: auto, ogólny tablet, Shelly

**Weryfikacja:**
- Ogólny build tabletu działa bez obecnego native library Shelly ✅
- Diagnostyka pokazuje fallback hardware provider ✅
- Strumień zdarzeń sprzętowych jest testowalny z fake providerem ✅
- Providery Shelly i Android-tablet są wybierane przez `PanelHardwareController` i wystawiane w Ustawieniach/Diagnostyce ✅

---

### Milestone 3: Fizyczne przyciski Shelly

**Cel:** Fizyczne przyciski Shelly działają wewnątrz Hapanels.

**Status:** ✅ Zakończony dla obecnego sprzętu Shelly Wall Display.

**Zadania:**
- Port `InputMonitor` i native `shellyinput.cpp` z integracją CMake
- Port/zaadaptować `ButtonPressDetector`
- Wykryć liczbę fizycznych przycisków dla każdego obsługiwanego modelu Shelly
- Konwertować niskopoziomowe zdarzenia klawiszy do `PanelButtonEvent(buttonId, pressType)`
- Dodać ustawienia mapowania akcji przycisków
- Dodać skonfigurowane cele akcji dla lokalnej kontroli przekaźników, wywołań usług HA i publikacji MQTT

**Typy naciśnięć przycisków:**
- short
- long
- double
- triple

**Początkowe cele akcji:**
- lokalny toggle przekaźnika
- wywołanie usługi HA
- trigger sceny/skryptu HA
- toggle bieżącej encji
- nawigacja do dashboard/wyszukiwania/assist/ustawień
- wake/sleep ekranu

**Weryfikacja:**
- Test na rzeczywistym Shelly Wall Display ✅
- Potwierdzić wykrywanie short/long/double/triple press ✅
- Potwierdzić brak crasha, gdy native input jest niedostępny ✅
- Rzeczywiste naciśnięcia przycisków Shelly i tematy zdarzeń zostały przetestowane przez MQTT discovery Home Assistant ✅
- Rozwiązywanie skonfigurowanych akcji przycisków jest pokryte dla celów relay, usługi HA i publikacji MQTT ✅

---

### Milestone 4: Lokalne przekaźniki i czujniki

**Cel:** Sprzęt Shelly pojawia się jako first-class lokalny stan panelu.

**Status:** ✅ Zakończony dla relay 1, światła otoczenia, jasności ekranu i kafla kontroli panelu opartego na capability; temperatura/wilgotność/zbliżeniowy pozostają zablokowane na niezawodne dane sprzętowe.

**Zadania:**
- Port kontroli przekaźników
- Port odczytów czujników temperatury, wilgotności, światła i zbliżeniowego
- Utworzyć lokalne storage stanu sprzętu Shelly
- Dodać natywne karty UI dla lokalnych przekaźników i czujników
- Sprawić, aby kontrola przekaźników działała nawet, gdy HA jest odłączony
- Dodać kafelki kontroli panelu oparte na capability do selektora ulubionych i stacku kart

**Weryfikacja:**
- Przekaźnik może być toggle'owany lokalnie z UI i fizycznych przycisków ✅
- Czujniki aktualizują się w diagnostyce i dashboardzie ✅
- Aplikacja pozostaje użyteczna bez połączenia HA dla lokalnych funkcji sprzętowych ✅
- Relay 1 i jasność ekranu zostały przetestowane na rzeczywistym sprzęcie Shelly Wall Display ✅
- Światło otoczenia jest wystawiane, gdy niezawodne; zbliżeniowy, temperatura i wilgotność nie są wystawiane jako fałszywe czujniki ✅
- Tablety nie-Shelly widzą tylko lokalne kafelki panelu wsparte przez ich zgłoszone capability ✅

---

### Milestone 5: MQTT Discovery

**Cel:** Home Assistant może wykrywać i kontrolować sprzęt panelu.

**Status:** ✅ Zakończony i przetestowany przeciw brokerowi MQTT użytkownika.

**Zadania:**
- Dodać ustawienia MQTT: host, port, TLS, username, password, base topic, discovery prefix
- Dodać menadżer połączeń MQTT
- Publikować dostępność
- Publikować konfiguracje discovery dla:
  - przekaźników jako `switch`
  - fizycznych przycisków jako device triggers/zdarzenia
  - czujników temperatury/wilgotności/światła
  - binary sensor zbliżeniowego
  - stanu ekranu / jasności, jeśli użyteczne
- Subskrybować tematy poleceń przekaźników
- Re-publikować discovery na boot/zmianę ustawień

**Weryfikacja:**
- HA MQTT discovery tworzy jedno urządzenie na panel ✅
- HA może toggle'ować przekaźniki Shelly przez MQTT ✅
- HA otrzymuje zdarzenia przycisków ✅
- HA otrzymuje aktualizacje czujników ✅
- Zmiany dostępności na start/stop/utratę sieci ✅
- HA może kontrolować jasność ekranu i przełącznik auto-jasności przez MQTT ✅
- HA otrzymuje diagnostykę app/version, hardware-provider, dashboard, screen-mode, target-brightness, applied-brightness ✅
- HA otrzymuje status połączenia MQTT + ostatnie błędy connect, publish, subscribe ✅
- Hapanels publikuje retained tematy stanu/meta dashboardu i akceptuje polecenia importu/patcha dashboardu ✅

---

### Milestone 6: Zbliżeniowy, Jasność, Screensaver

**Cel:** Zachowanie panelu appliance odpowiednie dla montażu ściennego.

**Status:** Fundament rozpoczęty; diagnostyka jasności/screen managera jest użyteczna, pełny renderer screensaver/AOD pozostaje w toku.

**Zadania:**
- Port/zaadaptować zachowanie proximity wake
- Dodać tryby screensaver:
  - czarny/off
  - zegar
  - zegar + data
  - karta podsumowania HA
- Dodać auto-jasność z czujnika światła
- Dodać ustawienia krzywej jasności: min, max, smoothing, tryb nocny
- Dodać timeout bezczynności i powody wake
- Oceń `j-a-n/lovelace-wallpanel` przed finalizacją UX AOD/screensaver. Sprawdź, które koncepcje powinny zostać portowane jako natywne zachowanie Hapanels: timer bezczynności, fullscreen/chrome hiding, wake lock, motion/wake triggers, źródła pokazów slajdów zdjęć/wideo i overlay cards/widgety statusu. Traktować jako inspirację projektową/konfiguracyjną, nie jako zależność Lovelace/WebView.

**Weryfikacja:**
- Ekran budzi się na zbliżenie ✅
- Screensaver aktywuje się po timeout ✅
- Jasność zmienia się płynnie z światłem otoczenia ✅
- Regularne tablety bez zbliżeniowego wciąż działają z timeout/touch wake ✅
- Kontrole MQTT ręcznej i auto-jasności zostały przetestowane na Shelly Wall Display ✅
- AOD ma placeholder konfiguracji dashboard (`always_on_display`) dla przyszłego natywnego renderera ✅

---

### Milestone 7: Utwardzanie produkcji

**Cel:** Sprawić, aby Hapanels był instalowalny i utrzymywalny.

**Zadania:**
- Odbiorca boot/autostart
- Opcje trybu kiosku
- Backup/restore ustawień Hapanels
- Pakiet eksportu crash i diagnostyki
- Macierz kompatybilności sprzętu
- Workflow release i obsługa podpisanych APK
- Lista kontrolna testów ręcznych dla Shelly Wall Display

**Weryfikacja:**
- Świeża instalacja setup działa
- Uaktualnienie zachowuje ustawienia
- Reboot autostartuje, gdy włączony
- Pakiet debug dostarcza wystarczająco danych do troubleshootingu problemów sprzętu/MQTT/HA

---

### Milestone 8: Natywny dashboard panelu i synchronizacja konfiguracji HA

**Cel:** Pozwolić Home Assistant zarządzać konfiguracją dashboardu Hapanels, podczas gdy Hapanels renderuje dopracowany natywny dashboard Compose.

**Status:** Rozpoczęty.

**Zadania:**
- Zdefiniować natywny model konfiguracji dashboardu dla sekcji, kafla, ludzi, układu i ustawień AOD
- Inicjować i cache'ować lokalny dashboard JSON na panelu
- Renderować natywny ciemny dashboard z live binding encji
- Publikować retained konfigurację i metadane dashboardu przez MQTT
- Akceptować pełny import konfiguracji i polecenia patcha z sprawdzaniem rewizji przez MQTT
- Zbudować integrację HACS, aby HA mogła wystawić UI zarządzania/konfiguracji dashboardu

**Weryfikacja:**
- Natywna trasa dashboardu renderuje się na panelu
- Konfiguracja dashboardu przetrzymuje restart aplikacji przez lokalny cache
- HA/MQTT widzi ID dashboardu, rewizję i diagnostykę updated-by
- Polecenia patch odrzucają przestarzałe wartości `base_revision` zamiast nadpisywać nowszą konfigurację panelu

---

### Milestone 9: Obsługa kamer

**Cel:** Przenieść przeglądanie kamer do natywnego doświadczenia panelu w sposób zbliżony do UX Phylax (camera-first), wciąż używając natywnych powierzchni Compose Hapanels.

**Status:** Zaplanowany.

**Zadania:**
- Dodać natywną przeglądarkę kamer z trybami list/grid i live polling snapshotów
- Dodać fullscreen overlay/detail kamer z dostrojonym szybkim odświeżaniem
- Rozszerzyć mockup dashboardu o kafelki i szybkie akcje zorientowane na kamery
- Wsparcie przyjaznych dla kamer domyślnych odświeżeń HA i graceful fallback, gdy kamery są niedostępne
- Użyć Phylax jako inspirację dla przeglądania kamer, prezentacji live statusu i touch-friendly camera detail flows

**Weryfikacja:**
- Encje kamer z HA pojawiają się w natywnej przeglądarce kamer
- Widoki grid i fullscreen kamer pollują snapshoty bez blokowania reszty panelu
- Mockup dashboardu pokazuje dedykowany kafel/sekcję kamer
- Przeglądanie kamer pozostaje użyteczne na tabletach i panelach ściennych

---

## ⚠️ Główne ryzyka

- Kod sprzętu Shelly może zależeć od root lub ścieżek specyficznych dla urządzenia
- Dziedziczone UI card stack jest zorientowane na małe ekrany i potrzebuje prawdziwej pracy UX dla tabletów
- MQTT discovery musi unikać zduplikowanych ID urządzenia/encji na wielu panelach
- Różnice w target SDK między Hapanels a ShellyElevate mogą wpływać na uprawnienia i dostęp do sprzętu
- Testowanie fizycznych przycisków wymaga rzeczywistego sprzętu Shelly Wall Display

## 🎯 Zalecenie pierwszej implementacji

Rozpocząć od Milestone 2 i 3 przed przepisywaniem dużych powierzchni UI. Fizyczne przyciski są najcenniejszym differentiatorem i zweryfikują, czy native input Shelly może żyć wewnątrz Hapanels czysto.
