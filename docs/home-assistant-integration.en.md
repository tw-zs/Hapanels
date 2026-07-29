# Home Assistant Integration

Hapanels features a dedicated custom integration `custom_components/hapanels`. This is the first step toward Hapanels Studio: the Home Assistant panel reads tablet states, detects configuration conflicts, and pushes dashboard/AOD changes over MQTT.

## MVP Status

Currently, the integration performs the minimum required before building the visual editor:

- adds the `Hapanels` config flow,
- adds a `Hapanels` panel to the Home Assistant sidebar,
- subscribes to the retained MQTT topic `hapanels/+/dashboard/config/sync/state`,
- creates a `Dashboard sync` sensor for each discovered panel,
- displays discovered panels and synchronization status in a simple web view,
- provides services for publishing full configurations and patches.

## Developer Installation

Copy the directory:

```text
custom_components/hapanels
```

to your Home Assistant directory:

```text
/config/custom_components/hapanels
```

Then restart Home Assistant and add the integration via the UI:

```text
Settings -> Devices & Services -> Add Integration -> Hapanels
```

The default base topic is:

```text
hapanels
```

## MQTT Sync State

The tablet publishes a retained synchronization status:

```text
hapanels/<device>/dashboard/config/sync/state
```

Example `synced`:

```json
{
  "status": "synced",
  "dashboard_id": "home-panel-main",
  "revision": 44,
  "updated_by": "homeassistant:hapanels_studio"
}
```

Example `conflict`:

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

The integration maps this payload into a sensor with attributes `revision`, `dashboard_id`, `updated_by`, `current_revision`, and `attempted_base_revision`.

## Hapanels Panel

After adding the integration, a panel appears in the sidebar:

```text
Hapanels
```

The initial version displays:

- list of discovered panels,
- `synced/conflict/invalid/unknown` status,
- current revision,
- author of the last change,
- conflict revision if a patch was based on a stale revision.

This is not yet a full visual editor. It is the foundation for Hapanels Studio.

## Services

### `hapanels.set_dashboard_config`

Publishes a full dashboard config to:

```text
hapanels/<device>/dashboard/config/set
```

Fields:

- `device`: device name from the MQTT topic, e.g. `Blake`, `shelly_wall_display`.
- `config`: full dashboard config object.

### `hapanels.patch_dashboard_config`

Publishes a patch to:

```text
hapanels/<device>/dashboard/config/patch/set
```

Fields:

- `device`: device name from the MQTT topic.
- `patch`: patch object containing `base_revision`, `updated_by`, `surface`, and `tile_updates`.

Example AOD patch:

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

## Next Step

The next phase is the `Hapanels Studio` frontend panel in the Home Assistant sidebar:

- `Dashboard` and `AOD` preview,
- tile editing,
- patch publishing,
- `conflict` resolution from `dashboard/config/sync/state`.
