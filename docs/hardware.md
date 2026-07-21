# Sprzęt panelu

Hapanels obsługuje dwa główne tryby pracy, dostosowane do różnych typów urządzeń:

## 📱 Obsługiwane platformy

| Platforma | Opis | Status |
|-----------|------|--------|
| **Shelly Wall Display** | Dedykowane panele ścienne od Shelly z wbudowanymi przyciskami i czujnikami | ✅ Pełna obsługa |
| **Tablet z Androidem** | Dowolny tablet z systemem Android jako uniwersalna alternatywa | ✅ Pełna obsługa |

## 🔧 Shelly Wall Display

Panele Shelly Wall Display oferują pełną integrację z Hapanels, wykorzystując ich unikalne możliwości sprzętowe.

### Obsługiwane funkcje

| Funkcjonalność | Opis | Status |
|----------------|------|--------|
| **Przekaźnik (Relay 1)** | Sterowanie wbudowanym przekaźnikiem | ✅ Aktywny |
| **Fizyczne przyciski** | Obsługa wbudowanych przycisków Shelly | ✅ Aktywny |
| **Jasność ekranu** | Regulacja podświetlenia ekranu | ✅ Aktywny |
| **Czujnik światła** | Pomiar natężenia oświetlenia otoczenia | ✅ Aktywny |
| **Czujnik zbliżeniowy** | Wykrywanie obecności osoby przed panelem | ✅ Aktywny |
| **AOD (Always On Display)** | Energooszczędny zegar i statusy w trybie uśpienia | ✅ Aktywny |
| **Proximity Wake** | Automatyczne wybudzanie ekranu przy zbliżeniu | ✅ Aktywny |

### Modelowe różnice

| Model | Liczba przycisków | Liczba przekaźników | Czujnik światła | Czujnik zbliżeniowy |
|-------|------------------|---------------------|-----------------|---------------------|
| Shelly Wall Display | 1 | 1 | ✅ | ✅ |
| Shelly Wall Display XL | 5 | 1 | ✅ | ✅ |

## 📋 MQTT Discovery

Hapanels automatycznie publikuje encje do Home Assistant przez **MQTT Discovery**, co umożliwia:

- Automatyczne wykrywanie panelu jako urządzenia w Home Assistant
- Tworzenie encji dla wszystkich obsługiwanych funkcji
- Zdalne sterowanie przez MQTT

### Główne tematy MQTT

| Temat | Opis | Kierunek |
|-------|------|----------|
| `hapanels/<device>/relay/<id>/set` | Sterowanie przekaźnikiem | ✉️ Subscribe |
| `hapanels/<device>/screen/brightness/set` | Ustawienie jasności ekranu | ✉️ Subscribe |
| `hapanels/<device>/screen/auto_brightness/set` | Włącz/wyłącz auto-jasność | ✉️ Subscribe |
| `hapanels/<device>/dashboard/config/set` | Pełna konfiguracja dashboardu | ✉️ Subscribe |
| `hapanels/<device>/dashboard/config/patch/set` | Częściowa aktualizacja (patch) | ✉️ Subscribe |

### Publikowane stany

Panel regularnie publikuje aktualne stany na tematach:

- `hapanels/<device>/relay/<id>/state` – Stan przekaźnika
- `hapanels/<device>/screen/brightness/state` – Aktualna jasność ekranu
- `hapanels/<device>/sensor/light/state` – Odczyt czujnika światła
- `hapanels/<device>/binary_sensor/proximity/state` – Stan czujnika zbliżeniowego

## ⚠️ Filozofia: Bez fikcyjnych sensorów

Hapanels stosuje zasadę **uczciwości danych**:

> Jeśli sprzęt nie dostarcza wiarygodnych wartości, Hapanels ich nie wystawia.

### Przykłady

- ✅ **Czujnik światła na Shelly**: Wystawiany jako sensor, ponieważ dostarcza wiarygodne dane
- ❌ **Czujnik zbliżeniowy na Shelly Blake/XL**: Traktowany jako **binary sensor** (obecność/nieobecność), nie jako precyzyjny pomiar odległości
- ❌ **Temperatura/humidity**: Nie są wystawiane, dopóki sprzęt nie zapewni wiarygodnych odczytów

## 💡 Porady dotyczące sprzętu

### Shelly Wall Display

1. **Optymalne ustawienia jasności**:
   - Auto-jasność: 30-70% dla komfortowego widoku
   - Jasność minimalna (AOD): 5-10% dla oszczędności energii
   
2. **Czujnik zbliżeniowy**:
   - Ustaw próg na 10-15 cm dla najlepszych rezultatów
   - Pamiętaj, że czujnik reaguje na ruch, nie na statyczną obecność

3. **Przyciski fizyczne**:
   - Każdy przycisk może być skonfigurowany indywidualnie
   - Obsługiwane typy naciśnięć: krótkie, długie, podwójne, potrójne

### Tablet z Androidem

1. **Wymagania minimalne**:
   - Android 8.0 (Oreo) lub nowszy
   - 2 GB RAM (rekomendowane 4 GB)
   - Ekran co najmniej 7 cali

2. **Optymalizacje**:
   - Wyłącz animacje systemowe dla płynniejszego działania
   - Ustaw tryb kiosku dla ciągłej pracy jako panel ścienny
   - Rozważ użycie trybu oszczędzania energii dla dłuższej pracy na baterii

3. **Ograniczenia**:
   - Brak fizycznych przycisków (można użyć przycisków ekranowych)
   - Czujniki zależą od modelu tabletu
   - Brak wbudowanego przekaźnika

## 🔄 Porównanie platform

| Funkcjonalność | Shelly Wall Display | Tablet Android |
|----------------|---------------------|----------------|
| Fizyczne przyciski | ✅ 1-5 przycisków | ❌ Nie dostępne |
| Wbudowany przekaźnik | ✅ 1 przekaźnik | ❌ Nie dostępne |
| Czujnik światła | ✅ Wbudowany | ⚠️ Zależy od modelu |
| Czujnik zbliżeniowy | ✅ Wbudowany | ⚠️ Zależy od modelu |
| Auto-jasność | ✅ Pełna obsługa | ✅ Pełna obsługa |
| AOD (Always On Display) | ✅ Optymalizowany | ✅ Dostępny |
| Tryb kiosku | ✅ Wbudowany | ✅ Konfigurowalny |
| Zużycie energii | ⭐⭐⭐⭐⭐ Niskie | ⭐⭐⭐ Średnie |

---

## 📖 Zobacz także

- [Instrukcja instalacji dla Shelly](installation-shelly.md)
- [Instrukcja instalacji dla tabletów](installation-tablet.md)
- [Integracja Home Assistant](home-assistant-integration.md)
