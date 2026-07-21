# Hapanels - Plan rozwoju

## 📌 Aktualny stan

Hapanels to obecnie natywna aplikacja panelowa Home Assistant na Androida z brandingiem Hapanels, publicznym repozytorium GitHub, białym ekranem powitalnym, zorientowaną na panele diagnostyką sprzętu oraz natywnym mockupem dashboardu dla tabletów.

## 🎯 Kamienie milowe

### Milestone 1: Powłoka produktu

**Cel:** Sprawić, aby aplikacja wyglądała jak panel ścienny/tablet, a nie klient na małe ekrany.

**Status:** ✅ Zakończony dla runtime product shell.

**Zrealizowane:**
- Branding, ikona, README i punkty wejścia diagnostyki panelu
- Domyślny dashboard dla tabletów dla świeżych instalacji
- Ustawienia trybu sprzętu/panełu
- Zastąpienie dziedziczonych sformułowań na rzecz terminologii Hapanels
- Dostrojenie powłoki dla landscape/portrait przez responsywne breakpointy i układy dashboardu
- Nazewnictwo release/update używa tagów, assetów i identyfikatorów Hapanels
- Komentarze nie związane z runtime zostały usunięte

**Następne:**
- Opcjonalne porządki: migracja dziedziczonych nazw pakietów/typów wewnętrznych, jeśli koszt jest uzasadniony

---

### Milestone 2: Warstwa abstrakcji sprzętu (HAL)

**Cel:** Wprowadzić czystą granicę, która może działać na zwykłych tabletach Android i Shelly Wall Display.

**Status:** ✅ Zakończony dla fundamentu HAL.

**Zrealizowane:**
- Interfejs `PanelHardware`
- `AndroidTabletHardware` jako fallback provider
- `ShellyWallDisplayHardware` jako safe stub provider
- `PanelHardwareController` z trybami `AUTO / TABLET / SHELLY`
- Status runtime w Ustawieniach i diagnostyce `PANEL HARDWARE`
- Odczyty czujników Android (światło otoczenia, zbliżeniowy, jasność ekranu)
- Testy wokół persystencji trybu providera i przełączania controller provider
- Lifecycle controllera jest idempotentny, `stop()` anuluje forwarding ustawień/providera
- Aktualizacje stanu runtime i capability aktywnego providera są przekazywane przez controller
- Natywny stan przycisków i przekaźników Shelly płynie przez tę samą granicę HAL

**Następne:**
- Opcjonalne szlifowanie: lokalizacja pozostałych etykiet diagnostyki sprzętu
- Opcjonalne szlifowanie: bogatsza diagnostyka wokół błędów start/stop providera

---

### Milestone 3: Fizyczne przyciski Shelly

**Cel:** Fizyczne przyciski Shelly generują niezawodne zdarzenia przycisków i mogą uruchamiać lokalne lub akcje HA.

**Status:** ✅ Zakończony dla obecnego sprzętu Shelly Wall Display.

**Zrealizowane:**
- Wspólny model zdarzeń przycisków
- Pole runtime state `pressedButtonIds`
- `PanelButtonPressDetector` dla short, long, double i triple press
- `InputMonitor` / `shellyinput.cpp` połączenie JNI
- Mapowanie niskopoziomowych kluczy Shelly do zdarzeń `PanelHardwareEvent.Button`
- Aktualizacje `pressedButtonIds` w `PanelHardwareRuntimeState`
- Mapowanie akcji przycisków dla przycisków 1-5
- Wiersze akcji przycisków dla press, release, short click, long press, double click, triple click
- Lokalne akcje przekaźników dla skonfigurowanych mapowań przycisków: none, toggle relay, relay on, relay off
- Nielokalne skonfigurowane akcje przycisków dla wywołań usług HA i publikacji MQTT
- Pola UI dla HA service domain/name/data JSON i MQTT topic/payload/retain
- Short click domyślnie toggle relay 1, gdy relay 1 istnieje
- Short click uruchamia się natychmiast, gdy ten sam przycisk nie ma skonfigurowanego double/triple click
- Long press nie opóźnia zachowania short-click relay
- `PanelMqttBridge` publikuje stan pressed button i tematy zdarzeń click dla discovery/state HA

**Następne:**
- Opcjonalne szlifowanie: pozwolić wierszom akcji przycisków wybierać relay id, gdy obsługiwany jest więcej niż jeden relay
- Opcjonalne szlifowanie: dodać wbudowane cele nawigacji/screen action, jeśli okażą się przydatne na zamontowanym panelu

---

### Milestone 4: Przekaźniki i czujniki Shelly

**Cel:** Stan przekaźników i czujników Shelly działa lokalnie i pojawia się w diagnostyce/UI Hapanels.

**Status:** ✅ Zakończony dla relay 1, rzeczywistej ekspozycji światła otoczenia i kafli kontroli panelu opartej na capability.

**Zrealizowane:**
- Helper odczytu/zapisu stanu sysfs relay 1 z pokryciem testów jednostkowych
- `ShellyWallDisplayHardware` utrzymuje stan relay 1 w `PanelHardwareRuntimeState` po lokalnych zapisach
- Odczyty runtime światła otoczenia i zbliżeniowego są sanityzowane, więc nieprawidłowe wartości czujników są traktowane jako brakujące
- MQTT discovery wystawia światło otoczenia tylko, gdy provider raportuje niezawodny czujnik; zbliżeniowy celowo nie jest wystawiany na Shelly, dopóki Android nie dostarczy użytecznych zdarzeń
- Temperatura/wilgotność celowo nie są wystawiane, dopóki sprzęt nie dostarcza niezawodnych odczytów
- Odczyt/zapis relay 1 został przetestowany na rzeczywistym sprzęcie Shelly Wall Display przez MQTT Home Assistant
- Zapisy jasności ekranu Shelly używają rzeczywistej ścieżki sysfs backlight, podczas gdy HA/MQTT utrzymuje stabilny kontrakt 0-100%
- Selektor ulubionych ma sekcję `Kontrola panelu` dla lokalnych kafli, filtrowaną przez bieżące capability sprzętu
- Lokalne kafle panelu mogą renderować relay 1, jasność ekranu, auto-jasność, światło otoczenia i stan panelu bez fałszywych danych czujników

**Następne:**
- Dodać temperaturę/wilgotność tylko, gdy dostępne jest niezawodne źródło sprzętowe lub integracyjne

---

### Milestone 5: MQTT Discovery

**Cel:** Home Assistant wykrywa panel jako urządzenie z przekaźnikami, przyciskami, czujnikami i dostępnością.

**Status:** ✅ Zakończony i przetestowany przeciw brokerowi MQTT użytkownika.

**Zrealizowane:**
- Ustawienia MQTT dla hosta, portu, TLS, username, password i client id
- Lekka sesja MQTT v3.1.1 z publish, subscribe, ping i disconnect
- Publisher konfiguracji discovery dla przekaźników, stanu pressed button, zdarzeń click button i jasności ekranu
- Publikowanie dostępności na temacie stanu panelu
- Publikowanie stanu relay
- Publikowanie stanu pressed button i button event
- Publikowanie stanu światła otoczenia, zbliżeniowego i jasności ekranu, gdzie runtime state dostarcza wartości
- Konfiguracje discovery światła otoczenia i zbliżeniowego, gdy aktywny provider wystawia te czujniki
- Subskrypcje poleceń relay przez `hapanels/<device>/relay/<id>/set`
- Subskrypcja poleceń jasności ekranu przez `hapanels/<device>/screen/brightness/set`
- Discovery i subskrypcja przełącznika auto-jasności ekranu przez `hapanels/<device>/screen/auto_brightness/set`
- Retained tematy stanu/meta dashboardu + tematy poleceń importu i patcha dashboardu
- Diagnostyka: app online, app version, hardware provider, metadata dashboardu, screen mode, target brightness, applied brightness
- Status połączenia MQTT + ostatnie błędy connect, publish i subscribe
- Triggery urządzenia Home Assistant dla fizycznych zdarzeń przycisków
- Pokrycie testów jednostkowych dla parsowania poleceń MQTT
- Rzeczywiste testy smoke dla relay 1, brightness, auto-brightness, availability, diagnostyki i metadata dashboardu

**Następne:**
- Dodać udoskonalenia metadanych urządzenia Home Assistant, jeśli UI HA potrzebuje polerowania

---

### Milestone 6: Zbliżeniowy, Jasność, AOD

**Cel:** Sprawić, aby Hapanels był użyteczny jako stale zamontowany panel ścienny.

**Status:** ✅ Zakończony dla praktycznego zbliżeniowego, jasności i natywnego fundamentu AOD.

**Zrealizowane:**
- Lifecycle `PanelScreenManager` podłączony od startu aplikacji
- Kontrola ręcznej jasności ekranu działa przez HA/MQTT i Shelly sysfs, z diagnostyką applied brightness
- Ustawienia auto-jasności, smoothing, histereza i kontrola przełącznika HA/MQTT
- Screen mode, target brightness i applied brightness publikowane jako diagnostyka MQTT
- `WRITE_SETTINGS` jest request/allowed dla Shelly, więc Android nie nadpisuje zapisów jasności sprzętu
- Ustawienia wake zbliżeniowego i obsługa progu przez `PanelScreenManager`
- Timeout screensaver/AOD, stan trybu, user activity wake i ostatnie powody wake/sleep są śledzone
- Natywny renderer AOD obsługuje tryb clock-only i AOD tile mode przez model konfiguracji dashboardu
- Wybór stylu zegara AOD jest persystowany jako `always_on_display.clock_style` i może być patchowany przez MQTT/Studio
- Pakiet stylów zegara AOD rozbudowany do 13 unikalnych stylów, w pełni zintegrowany z aplikacją kliencką i Hapanels Studio: default, modern, Warsaw Zakład, Cyberpunk Korpo, Zew Puszczy, popart, Fabryka Koloru, Italic Editorial, Szeroki, wide bold, Neon Baltic (gradient i streaks), Electric Stained Glass (stained glass i multi-colored numbers), i Poznan Goats (typografia Amber z clock-offset i zoptymalizowaną grafiką Poznan goats)

**Następne:**
- Opcjonalne szlifowanie: dostrojenie zachowania zbliżeniowego i idle na rzeczywistym zamontowanym sprzęcie Shelly po dłuższym użyciu
- Opcjonalne szlifowanie: dodać bogatsze źródła AOD później, takie jak pokaz slajdów zdjęć/wideo lub wybrane natywne widgety statusu, bez czynienia Hapanels zależnym od Lovelace/WebView

---

### Milestone 7: Utwardzanie produkcji

**Cel:** Dostarczyć utrzymywalny panel appliance.

**Następne:**
- Naprawić wyścig Shelly/Android system shade: po opuszczeniu Hapanels Studio, szybkie dotknięcie w pobliżu górnego-lewego hamburgera może otworzyć Android `NotificationShade` i wyglądać jak czarny ekran, zanim widok app/AOD powróci. Obecne dowody: app pozostaje foreground, no crash, no FavoritesPicker navigation, `mExpandedPanel=NotificationShade`. Oceń odpowiednie immersive/kiosk handling lub solidny top-gesture guard zamiast polegać na layout padding
- Boot/autostart
- Opcje trybu kiosku
- Pływający przycisk powrotu do aplikacji, gdy Hapanels jest w tle lub ukryty
- Eksport diagnostyki
- Macierz kompatybilności sprzętu
- Utwardzanie workflow release i obsługa podpisanych APK

---

### Milestone 8: Natywny dashboard panelu i synchronizacja konfiguracji HA

**Cel:** Pozwolić Home Assistant zarządzać konfiguracją dashboardu Hapanels, podczas gdy Hapanels renderuje dopracowany natywny dashboard Compose.

**Status:** Rozpoczęty.

**Zrealizowane:**
- Natywny ciemny mockup trasy grid panelu w Compose
- Zorientowany na tablety grid z zegarem, ludźmi, kafelkami akcji, dużymi kafelkami pomieszczeń i zwartymi kafelkami statusu
- Lokalne zasoby czcionek Nunito dla nowego dashboardu panelu
- Model konfiguracji dashboardu z typami tile/person/layout i przykładowym JSON
- Lokalne źródło konfiguracji dashboardu, które inicjuje i cache'uje `hapanels_dashboard_config.json`
- Jawne lokalne kontrole eksportu/importu/reset dashboardu JSON w ustawieniach Wyglądu
- Live binding encji dla obsługiwanych wartości `entity_id` kafla dashboardu przez `HaRepository.observe`
- Retained tematy stanu/meta dashboardu MQTT + wsparcie importu `dashboard/config/set`
- Lokalne patch'e edycji dashboardu z wykrywaniem konfliktów `base_revision` i wsparciem `dashboard/config/patch/set` MQTT
- Punkty wejścia z stacku kart, ekranu dashboardu i nawigacji aplikacji
- Hapanels Studio może edytować kafelki/AOD, pokazywać przybliżony podgląd HTML, zmieniać rozmiar kafla z podglądu, stosować presety AOD i rozstrzygać podstawowe konflikty konfiguracji
- Edytor kafla Hapanels Studio używa leniwego `ha-entity-picker` Home Assistant zamiast renderować każdą encję jako `<option>`
- Natywne dotknięcia kafla dashboardu wysyłają akcje Home Assistant przez istniejąca ścieżkę `ServiceCall.tapAction` świadomą domeny
- Presety motywów dashboardu są persystowane w konfiguracji dashboardu i renderowane przez natywny Compose panel
- Hapanels Studio zawiera zakładkę Wygląd dla presety motywów panelu i wybór trybu jasnego/ciemnego
- Hapanels Studio może wybierać style zegara AOD i stosować je przez polecenia patcha dashboardu

**Następne:**
- Dodać panele drilldown: `panel_id` otwiera natywny panel, wsparcie przez persystowany schemat panelu/kartu w konfiguracji dashboardu
- Kontynuować polerowanie układu Studio po rebuildzie edytora, zwłaszcza rozmiar ikon picker i mobile wrapping
- Poprawić wierność podglądu Studio: dopasować renderer Compose tablet bardziej ściśle dla geometrii kafla, sekcji zegara/osoby, kafla kamery/akcji, spacing, typografii i zachowania responsywnego. Obecny podgląd HTML jest użyteczny do edycji, ale nie pixel-perfect
- Dodać pełniejsze rozstrzyganie konfliktów: porównać konfigurację tablet/bieżącą vs Studio pending patch, pokazać zmienione pola i wsparcie per-tile/per-field merge zamiast tylko "Studio wins" / "tablet wins"
- Utrzymać Hapanels jako natywny renderer i unikać zależności WebView/Lovelace, chyba że konkretna karta tego wymaga

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

**Może później:**
- Wsparcie ESPHome Bluetooth proxy dla pobliskich urządzeń BLE

---

### Milestone 10: Pierwsze uruchomienie i onboarding

**Cel:** Sprawić, aby pierwsze uruchomienie wyglądało jak prawdziwy proces onboardingu urządzenia, a nie surowy start aplikacji.

**Status:** ✅ Zakończony dla fundamentu onboardingu produkcji.

**Zrealizowane:**
- Przewodnik first-run: powitanie, połączenie Home Assistant, autoryzacja, ekrany personalizacji
- Logowanie OAuth Home Assistant z sondowaniem serwera i wymianą tokenów
- Ustawienie long-lived access token pozostaje dostępne jako alternatywa onboardingowa do OAuth
- Nazwa tabletu persystuje do ustawień aplikacji i tożsamości panelu HA/MQTT
- Wybór presetu motywu Panel Grid patch'uje persystowaną konfigurację dashboardu bez zastępowania jego trybu jasnego/ciemnego, konfiguracji AOD lub kafla
- Wybór startowy ograniczony do `GRID` i `CARDS`, persystuje przez restart i zastępuje dziedziczone preferencje startowe Today/dashboard i linki launchera

**Weryfikacja:**
- Start pozostaje w onboardingu, dopóki serwer i non-blank access token nie są obecne
- Ścieżki OAuth i long-lived token mogą ukończyć setup
- Nazwa tabletu, preset motywu Panel Grid i `GRID` / `CARDS` widok startowy persystują przez restart
- Dziedziczone ustawienia startowe i backupy migrują kompatybilnie; startup i launcher shortcuts nigdy nie otwierają Today
- Stringi onboardingowe mają skupione pokrycie lokalizacji polskiej

---

### Milestone 11: Bezpieczny MQTT i onboarding Studio

**Cel:** Uwzględnić rzeczywiste setup MQTT i Hapanels Studio w first-run onboarding bez przechowywania poświadczeń niebezpiecznie lub pokazywania symulowanych stanów połączenia.

**Status:** Zaplanowany.

**Zadania:**
- Przenieść poświadczenia MQTT z regularnego DataStore do encrypted storage
- Zmigrować istniejące poświadczenia MQTT i usunąć wartości plaintext po udanej migracji
- Dodać onboarding MQTT dla hosta, portu, TLS, username, password, testu połączenia i opcjonalnego skip
- Raportować rzeczywisty status połączenia brokera i actionable błędy połączenia
- Dodać setup Hapanels Studio oparty na rzeczywistej dostępności MQTT/config-sync
- Wykrywać i wyświetlać rzeczywistą gotowość Studio zamiast symulowanego stanu połączonego
- Dodać wyniki MQTT i Studio do finalnej listy kontrolnej onboardingowej

**Weryfikacja:**
- Hasło MQTT nigdy nie persystuje w plaintext settings
- Prawidłowe poświadczenia brokera ustanawiają rzeczywiste połączenie
- Nieprawidłowe poświadczenia i nieosiągalni brokerzy pokazują użyteczne błędy
- Status Studio odzwierciedla rzeczywistą dostępność synchronizacji konfiguracji
- Oba kroki mogą zostać pominięte bez blokowania onboardingowego

---

### Milestone 12: Adaptacyjna jasność wyświetlacza

**Cel:** Utrzymać AOD czytelnym w ciągu dnia i komfortowym w nocy, zarówno dla AOD, jak i aktywnego panelu.

**Status:** Zaplanowany.

**Zadania:**
- Zastąpić oddzielne zachowania jasności AOD i aktywnego panelu jednym kontrolerem światła otoczenia
- Dodać niezależnie dostrajalne krzywe jasności AOD i aktywnego panelu z skalibrowanymi poziomami minimalnymi i maksymalnymi
- Wygładzać hałaśliwe odczyty luksów i używać histerezy, dwell time i stopniowych przejść, aby zapobiec widocznym skokom jasności
- Zapobiegać feedback, gdzie zmiany jasności ekranu zmieniają własny odczyt światła otoczenia panelu
- Zachować ręczną jasność jako jawne nadpisanie z klarowną ścieżką powrotu do trybu adaptacyjnego
- Wystawić kalibrację i diagnostykę w Hapanels Studio, w tym luks, filtered luks, target brightness, applied brightness i aktywny override source
- Dostroić zachowanie dzienne, wieczorne i nocne na zamontowanym sprzęcie Shelly Wall Display

**Weryfikacja:**
- AOD pozostaje czytelny w jasnym pomieszczeniu bez niepotrzebnego pełnego jasności
- AOD i aktywny panel nie oślepiają w ciemnym pomieszczeniu
- Chodzenie w kierunku panelu i opuszczanie AOD nie powoduje flash, dip lub oscylacji
- Szybkie lub indukowane przez ekran zmiany luksów nie powodują powtarzanych zapisów jasności
- Zachowanie pozostaje przewidywalne z auto-jasnością wyłączoną lub czujnikiem światła otoczenia niedostępnym
