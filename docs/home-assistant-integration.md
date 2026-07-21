# Integracja Home Assistant

Hapanels ma własną integrację `custom_components/hapanels`. To pierwszy krok pod Hapanels Studio: panel w Home Assistant ma czytać stan tabletów, wykrywać konflikty konfiguracji i wysyłać zmiany dashboardu/AOD przez MQTT.

## Status MVP

Obecnie integracja robi minimum potrzebne przed budową edytora:

- dodaje config flow `Hapanels`,
- dodaje panel `Hapanels` w sidebarze Home Assistant,
- subskrybuje retained MQTT topic `hapanels/+/dashboard/config/sync/state`,
- tworzy sensor `Dashboard sync` dla każdego wykrytego panelu,
- pokazuje wykryte panele i status synchronizacji w prostym widoku web,
- udostępnia usługi do wysyłania pełnego configu i patchy.

## Instalacja developerska

Skopiuj katalog:

```text
custom_components/hapanels
```

do katalogu Home Assistant:

```text
/config/custom_components/hapanels
```

Następnie zrestartuj Home Assistant i dodaj integrację z UI:

```text
Ustawienia -> Urządzenia i usługi -> Dodaj integrację -> Hapanels
```

Domyślny base topic to:

```text
hapanels
```

## MQTT Sync State

Tablet publikuje retained status synchronizacji:

```text
hapanels/<device>/dashboard/config/sync/state
```

Przykład `synced`:

```json
{
  "status": "synced",
  "dashboard_id": "home-panel-main",
  "revision": 44,
  "updated_by": "homeassistant:hapanels_studio"
}
```

Przykład `conflict`:

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

Integracja zamienia ten payload na sensor z atrybutami `revision`, `dashboard_id`, `updated_by`, `current_revision` i `attempted_base_revision`.

## Panel Hapanels

Po dodaniu integracji w sidebarze pojawia się panel:

```text
Hapanels
```

Pierwsza wersja pokazuje:

- listę wykrytych paneli,
- status `synced/conflict/invalid/unknown`,
- aktualną revision,
- ostatniego autora zmiany,
- revision konfliktu, jeśli patch był oparty o starą wersję.

To jeszcze nie jest pełny edytor. To fundament pod Hapanels Studio.

## Usługi

### `hapanels.set_dashboard_config`

Publikuje pełny dashboard config do:

```text
hapanels/<device>/dashboard/config/set
```

Pola:

- `device`: nazwa urządzenia z topicu MQTT, np. `Blake`, `shelly_wall_display`.
- `config`: pełny obiekt dashboard config.

### `hapanels.patch_dashboard_config`

Publikuje patch do:

```text
hapanels/<device>/dashboard/config/patch/set
```

Pola:

- `device`: nazwa urządzenia z topicu MQTT.
- `patch`: obiekt patcha z `base_revision`, `updated_by`, `surface` i `tile_updates`.

Przykład patcha AOD:

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

## Następny Krok

Następny etap to frontend panel `Hapanels Studio` w sidebarze Home Assistant:

- podgląd `Dashboard` i `AOD`,
- edycja kafli,
- wysyłanie patchy,
- obsługa `conflict` z `dashboard/config/sync/state`.
