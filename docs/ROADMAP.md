# Roadmap Hapanels

Hapanels to natywne panele Android dla Home Assistant. Głównym celem są urządzenia Shelly Wall Display, ale zwykłe tablety z Androidem pozostają wspieranym wariantem.

To jedyny plan projektu. Łączy kierunek produktu, aktualny status i najbliższe priorytety.

## Zasady produktu

- Najpierw natywny Android i interfejs Compose.
- REST/WebSocket Home Assistant dla dashboardu.
- MQTT dla autodetekcji panelu, sterowania sprzętem i synchronizacji konfiguracji.
- Obsługa Shelly bez osobnego dodatku ShellyElevate.
- Brak sztucznych danych: czujnik pokazujemy tylko przy wiarygodnym odczycie.
- Tryb tabletowy musi działać także bez sprzętu specyficznego dla Shelly.

## Aktualny status

### Gotowe

- Shell Hapanels zoptymalizowany pod tablet i natywny dashboard.
- Warstwa sprzętowa dla tabletów Android i Shelly Wall Display.
- Fizyczne przyciski Shelly: krótkie, długie, podwójne i potrójne naciśnięcie.
- Przekaźnik 1 Shelly, światło otoczenia, jasność i kafelki sterowania panelem.
- MQTT discovery, dostępność, diagnostyka, zdarzenia przycisków i sterowanie przekaźnikiem.
- Wybudzanie zbliżeniem i dotykiem, autojasność, wygaszacz oraz baza natywnego AOD.
- Onboarding przez OAuth lub token długoterminowy.
- Integracja Home Assistant z panelem bocznym i sensorem synchronizacji.
- Podstawa Hapanels Studio do edycji dashboardu/AOD i synchronizacji konfiguracji MQTT.
- Onboarding i dokumentacja po polsku, angielsku i niemiecku.

### W toku

- Natywne panele szczegółowe otwierane z dashboardu.
- Wierność podglądu Studio, dopracowanie layoutu i scalanie konfliktów.
- Stabilizacja pracy na ścianie, w tym problem z paskiem systemowym Androida.

## Najbliższe priorytety

### 1. Stabilizacja produkcyjna

Ułatwić instalację i długotrwałe używanie:

- autostart i opcje trybu kiosk;
- eksport kopii zapasowych i diagnostyki;
- macierz kompatybilności sprzętu;
- podpisywanie i publikacja wydań;
- checklista testów na prawdziwym Shelly Wall Display.

### 2. Dashboard i Studio

- Otwieranie zapisanych paneli przez `panel_id`.
- Lepsze odwzorowanie geometrii, typografii, odstępów i widoku mobilnego w Studio.
- Pokazywanie różnic pól przy konflikcie rewizji dashboardu.
- Zachowanie natywnego renderera Hapanels bez zależności od Lovelace/WebView.

### 3. Bezpieczne MQTT i onboarding

- Przeniesienie danych MQTT do szyfrowanego magazynu.
- Konfiguracja MQTT z testem połączenia i zrozumiałymi błędami.
- Wyznaczanie gotowości Studio na podstawie prawdziwego stanu MQTT/config sync.
- Opcjonalne MQTT i Studio podczas pierwszego uruchomienia.

### 4. Kamery

- Natywna przeglądarka kamer z widokiem listy i siatki.
- Widok pełnoekranowy z wydajnym odświeżaniem snapshotów.
- Kafelki kamer i szybkie akcje w natywnym dashboardzie.
- Poprawne działanie, gdy kamer nie ma.

### 5. Adaptacyjna jasność

- Jeden kontroler światła otoczenia dla AOD i aktywnego panelu.
- Kalibrowane krzywe jasności z wygładzaniem i histerezą.
- Ręczne nadpisanie z jasnym powrotem do trybu adaptacyjnego.
- Diagnostyka lux, jasności docelowej, zastosowanej i źródła nadpisania.

## Później

- Bogatsze źródła AOD, np. wybrane widgety lub pokazy multimediów.
- Dalsze usprawnienia przekaźników i akcji przycisków.
- Wsparcie proxy Bluetooth ESPHome dla pobliskich urządzeń BLE.
- Kolejni dostawcy sprzętu, gdy pojawi się przetestowany przypadek użycia.

## Weryfikacja

Każdy etap sprawdzamy na właściwym poziomie:

- testy jednostkowe stanu, parserów, migracji i logiki sprzętowej;
- smoke testy Androida: onboarding, dashboard, AOD i restart;
- testy prawdziwego Shelly: przyciski, przekaźnik, jasność i zbliżenie;
- testy Home Assistant MQTT: discovery, komendy, dostępność i synchronizacja;
- testy aktualizacji, aby ustawienia i konfiguracja dashboardu przetrwały upgrade.

## Ograniczenia i ryzyka

- Ścieżki sprzętowe Shelly mogą wymagać roota i plików zależnych od modelu.
- Pełnego zachowania sprzętu nie da się zweryfikować na emulatorze.
- MQTT discovery nie może tworzyć duplikatów urządzeń ani entity ID.
- Dane czujników pozostają ukryte, dopóki sprzęt nie zapewni wiarygodnych odczytów.
- Dziedziczony interfejs kart pozostaje użyteczny, ale nie jest głównym doświadczeniem panelu ściennego.

## Dla kontrybutorów

Ten dokument jest źródłem prawdy dla priorytetów i statusu etapów. Szczegóły implementacji trzymaj przy kodzie; tutaj aktualizuj tylko efekty widoczne dla użytkownika, priorytety i oczekiwania dotyczące weryfikacji.
