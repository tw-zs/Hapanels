# Integracja Home Assistant

Hapanels oferuje własną integrację `custom_components/hapanels`, która umożliwia dwukierunkową komunikację między Home Assistant a panelami Hapanels. To pierwszy krok w kierunku pełnego **Hapanels Studio** – narzędzia do zarządzania i konfigurowania panelu bezpośrednio z poziomu Home Assistant.

## 📋 Czym jest integracja Hapanels?

Integracja służy jako most między Home Assistant a Twoimi panelami. Dzięki niej:

- Home Assistant wykrywa wszystkie aktywne panele Hapanels
- Możesz monitorować status synchronizacji konfiguracji
- Możesz zdalnie aktualizować ustawienia panelu przez MQTT
- Otrzymujesz informacje o ewentualnych konfliktach konfiguracji

## 🎯 Obecne możliwości (MVP)

Aktualna wersja integracji oferuje podstawowe funkcjonalności niezbędne przed budową pełnego edytora:

| Funkcjonalność | Opis |
|----------------|------|
| **Config Flow** | Dodaje integrację Hapanels w interfejsie Home Assistant |
| **Panel w sidebarze** | Tworzy dedykowany panel Hapanels w bocznym menu |
| **Monitorowanie MQTT** | Subskrybuje tematy `hapanels/+/dashboard/config/sync/state` |
| **Sensory synchronizacji** | Tworzy sensory `Dashboard sync` dla każdego panelu |
| **Podgląd paneli** | Pokazuje wykryte panele i ich status synchronizacji |
| **Usługi MQTT** | Udostępnia usługi do wysyłania pełnej konfiguracji i patchy |

## 📥 Instalacja

### 1. Kopiowanie plików

Skopiuj katalog integracji:

```bash
custom_components/hapanels
```

Do katalogu konfiguracyjnego Home Assistant:

```bash
/config/custom_components/hapanels
```

### 2. Restart Home Assistant

Zrestartuj swoją instancję Home Assistant, aby załadować nową integrację.

### 3. Dodawanie integracji

Przejdź do:

```
Ustawienia → Urządzenia i usługi → Dodaj integrację → Hapanels
```

### 4. Konfiguracja

Domyślny base topic MQTT:

```
hapanels
```

Możesz go zmienić podczas konfiguracji, jeśli używasz innego prefiksu.

## 📡 Komunikacja MQTT

### Tematy synchronizacji

Panel publikuje status synchronizacji na temacie:

```
hapanels/<device>/dashboard/config/sync/state
```

#### Przykład: Status "synced"

```json
{
  "status": "synced",
  "dashboard_id": "home-panel-main",
  "revision": 44,
  "updated_by": "homeassistant:hapanels_studio"
}
```

#### Przykład: Konflikt

```json
{
  "status": "conflict",
  "dashboard_id": "home-panel-main",
  "revision": 44,
  "updated_by": "tablet:local_editor",
  "current_revision": 44,
  "attempted_base_revision": 43
}
```

Integracja konwertuje te dane na sensory z atrybutami takimi jak `revision`, `dashboard_id`, `updated_by`, itp.

## 🖥️ Panel Hapanels w Home Assistant

Po dodaniu integracji, w sidebarze pojawi się nowy panel **Hapanels**, który oferuje:

- **Listę wykrytych paneli** – wszystkie aktywne urządzenia Hapanels
- **Status synchronizacji** – `synced`, `conflict`, `invalid` lub `unknown`
- **Aktualna rewizja** – numer wersji konfiguracji
- **Ostatni autor zmiany** – kto ostatnio modyfikował konfigurację
- **Informacje o konflikcie** – jeśli patch był oparty o starą wersję

⚠️ **Uwaga**: To jeszcze nie jest pełny edytor. To fundament pod przyszłe **Hapanels Studio**.

## 🔧 Dostępne usługi

### `hapanels.set_dashboard_config`

Publikuje pełną konfigurację dashboardu na temacie:

```
hapanels/<device>/dashboard/config/set
```

**Parametry:**

| Parametr | Typ | Opis |
|----------|-----|------|
| `device` | string | Nazwa urządzenia z tematu MQTT (np. `Blake`, `shelly_wall_display`) |
| `config` | object | Pełny obiekt konfiguracji dashboardu |

### `hapanels.patch_dashboard_config`

Publikuje patch (częściową aktualizację) na temacie:

```
hapanels/<device>/dashboard/config/patch/set
```

**Parametry:**

| Parametr | Typ | Opis |
|----------|-----|------|
| `device` | string | Nazwa urządzenia z tematu MQTT |
| `patch` | object | Obiekt patcha z polami: `base_revision`, `updated_by`, `surface`, `tile_updates` |

#### Przykład patcha AOD

```json
{
  "base_revision": 44,
  "updated_by": "homeassistant:hapanels_studio",
  "surface": "aod",
  "tile_updates": [
    {
      "id": "aod_temperature",
      "label": "Na zewnątrz",
      "order": 2
    }
  ]
}
```

## 🚀 Następne kroki

Kolejnym etapem rozwoju jest **Hapanels Studio** – wizualny edytor zintegrowany z Home Assistant, który umożliwi:

- Podgląd dashboardu i AOD (Always On Display)
- Edycję kafelków w interfejsie graficznym
- Wysyłanie patchy przez MQTT
- Obsługę konfliktów synchronizacji
- Zarządzanie wieloma panelami

---

## 📖 Zobacz także

- [Instrukcja instalacji MQTT](installation.md)
- [Konfiguracja sprzętu](hardware.md)
- [Dokumentacja dla developerów](development.md)
