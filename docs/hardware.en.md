# Panel Hardware

Hapanels features two hardware modes:

- **Shelly Wall Display** for Shelly panels.
- **Android tablet** as a safe fallback.

## Shelly

Currently supported or detected functions:

- relay 1,
- physical Shelly buttons,
- screen brightness,
- ambient light sensor,
- proximity presence as a binary sensor,
- AOD/screensaver and proximity wake.

## MQTT Discovery

The panel publishes entities and diagnostics to Home Assistant via MQTT discovery. Example command topics:

```text
hapanels/<device>/relay/<id>/set
hapanels/<device>/screen/brightness/set
hapanels/<device>/screen/auto_brightness/set
hapanels/<device>/dashboard/config/set
hapanels/<device>/dashboard/config/patch/set
```
