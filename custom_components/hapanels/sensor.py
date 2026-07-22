from __future__ import annotations

import json
from dataclasses import dataclass, field
from typing import Any

from homeassistant.components import mqtt
from homeassistant.components.sensor import SensorEntity
from homeassistant.config_entries import ConfigEntry
from homeassistant.core import HomeAssistant, callback
from homeassistant.helpers.entity_platform import AddEntitiesCallback

from . import clear_pending_if_synced
from .const import CONF_BASE_TOPIC, DATA_CONFIGS, DATA_PANELS, DATA_PENDING_PATCHES, DATA_UNSUB, DEFAULT_BASE_TOPIC, DOMAIN
from .schema import validate_dashboard_config


@dataclass
class PanelSyncState:
    device: str
    status: str = "unknown"
    revision: int | None = None
    dashboard_id: str | None = None
    updated_by: str | None = None
    panel_name: str | None = None
    current_revision: int | None = None
    attempted_base_revision: int | None = None
    screen_resolution: str | None = None
    screen_width_px: int | None = None
    screen_height_px: int | None = None
    extra: dict[str, Any] = field(default_factory=dict)


async def async_setup_entry(
    hass: HomeAssistant,
    entry: ConfigEntry,
    async_add_entities: AddEntitiesCallback,
) -> None:
    base_topic = entry.data.get(CONF_BASE_TOPIC, DEFAULT_BASE_TOPIC).strip("/")
    panels: dict[str, HapanelsSyncSensor] = hass.data[DOMAIN][entry.entry_id][DATA_PANELS]
    configs: dict[str, Any] = hass.data[DOMAIN][entry.entry_id][DATA_CONFIGS]

    @callback
    def handle_sync_state(msg) -> None:
        device = _device_from_topic(base_topic, msg.topic)
        if device is None:
            return
        state = _parse_state(device, msg.payload)
        entity = panels.get(device)
        if entity is None:
            entity = HapanelsSyncSensor(state)
            panels[device] = entity
            async_add_entities([entity])
        else:
            entity.update_state(state)
        clear_pending_if_synced(
            hass.data[DOMAIN][entry.entry_id][DATA_PENDING_PATCHES],
            device,
            state.status,
            state.revision,
        )

    unsub = await mqtt.async_subscribe(
        hass,
        f"{base_topic}/+/dashboard/config/sync/state",
        handle_sync_state,
        qos=0,
    )
    hass.data[DOMAIN][entry.entry_id][DATA_UNSUB].append(unsub)

    @callback
    def handle_config_state(msg) -> None:
        device = _device_from_topic(base_topic, msg.topic, "/dashboard/config/state")
        if device is None:
            return
        try:
            configs[device] = validate_dashboard_config(json.loads(msg.payload))
        except (json.JSONDecodeError, ValueError):
            return

    config_unsub = await mqtt.async_subscribe(
        hass,
        f"{base_topic}/+/dashboard/config/state",
        handle_config_state,
        qos=0,
    )
    hass.data[DOMAIN][entry.entry_id][DATA_UNSUB].append(config_unsub)

    @callback
    def handle_resolution_state(msg) -> None:
        device = _device_from_topic(base_topic, msg.topic, "/screen/resolution/state")
        if device is None:
            return
        entity = panels.get(device)
        if entity is None:
            entity = HapanelsSyncSensor(PanelSyncState(device=device))
            panels[device] = entity
            async_add_entities([entity])
        payload = msg.payload.decode("utf-8") if isinstance(msg.payload, bytes) else str(msg.payload)
        entity.update_resolution(payload)

    resolution_unsub = await mqtt.async_subscribe(
        hass,
        f"{base_topic}/+/screen/resolution/state",
        handle_resolution_state,
        qos=0,
    )
    hass.data[DOMAIN][entry.entry_id][DATA_UNSUB].append(resolution_unsub)


def _device_from_topic(base_topic: str, topic: str, suffix: str = "/dashboard/config/sync/state") -> str | None:
    prefix = f"{base_topic}/"
    if not topic.startswith(prefix) or not topic.endswith(suffix):
        return None
    device = topic[len(prefix) : -len(suffix)]
    return device or None


def _parse_state(device: str, payload: str) -> PanelSyncState:
    try:
        raw = json.loads(payload)
    except json.JSONDecodeError:
        return PanelSyncState(device=device, status="invalid", extra={"raw": payload})
    if not isinstance(raw, dict):
        return PanelSyncState(device=device, status="invalid", extra={"raw": payload})
    return PanelSyncState(
        device=device,
        status=str(raw.get("status", "unknown")),
        revision=_as_int(raw.get("revision")),
        dashboard_id=raw.get("dashboard_id"),
        updated_by=raw.get("updated_by"),
        panel_name=raw.get("panel_name"),
        current_revision=_as_int(raw.get("current_revision")),
        attempted_base_revision=_as_int(raw.get("attempted_base_revision")),
        extra=raw,
    )


def _as_int(value: Any) -> int | None:
    try:
        return int(value)
    except (TypeError, ValueError):
        return None


class HapanelsSyncSensor(SensorEntity):
    _attr_has_entity_name = True
    _attr_name = "Dashboard sync"
    _attr_icon = "mdi:sync-alert"

    def __init__(self, state: PanelSyncState) -> None:
        self._state = state
        self._attr_unique_id = f"hapanels_{state.device}_dashboard_sync"
        self._attr_device_info = {
            "identifiers": {(DOMAIN, state.device)},
            "name": state.panel_name or f"Hapanels {state.device}",
            "manufacturer": "tw-zs",
        }

    @property
    def native_value(self) -> str:
        return self._state.status

    @property
    def extra_state_attributes(self) -> dict[str, Any]:
        return {
            "device": self._state.device,
            "dashboard_id": self._state.dashboard_id,
            "revision": self._state.revision,
            "updated_by": self._state.updated_by,
            "panel_name": self._state.panel_name,
            "current_revision": self._state.current_revision,
            "attempted_base_revision": self._state.attempted_base_revision,
            "screen_resolution": self._state.screen_resolution,
            "screen_width_px": self._state.screen_width_px,
            "screen_height_px": self._state.screen_height_px,
            "schema_version": self._state.extra.get("schema_version"),
            "schema_capabilities": self._state.extra.get("schema_capabilities", []),
            "supported_tile_kinds": self._state.extra.get("supported_tile_kinds", []),
            "supported_action_types": self._state.extra.get("supported_action_types", []),
        }

    @callback
    def update_state(self, state: PanelSyncState) -> None:
        state.screen_resolution = self._state.screen_resolution
        state.screen_width_px = self._state.screen_width_px
        state.screen_height_px = self._state.screen_height_px
        self._state = state
        self._attr_device_info = {
            **self._attr_device_info,
            "name": state.panel_name or f"Hapanels {state.device}",
        }
        self.async_write_ha_state()

    @callback
    def update_resolution(self, resolution: str) -> None:
        width, height = _parse_resolution(resolution)
        self._state.screen_resolution = resolution if width and height else None
        self._state.screen_width_px = width
        self._state.screen_height_px = height
        self.async_write_ha_state()

    def as_dict(self) -> dict[str, Any]:
        return {
            "device": self._state.device,
            "status": self._state.status,
            "dashboard_id": self._state.dashboard_id,
            "revision": self._state.revision,
            "updated_by": self._state.updated_by,
            "panel_name": self._state.panel_name,
            "current_revision": self._state.current_revision,
            "attempted_base_revision": self._state.attempted_base_revision,
            "screen_resolution": self._state.screen_resolution,
            "screen_width_px": self._state.screen_width_px,
            "screen_height_px": self._state.screen_height_px,
            "schema_version": self._state.extra.get("schema_version"),
            "schema_capabilities": self._state.extra.get("schema_capabilities", []),
            "supported_tile_kinds": self._state.extra.get("supported_tile_kinds", []),
            "supported_action_types": self._state.extra.get("supported_action_types", []),
        }


def _parse_resolution(value: str) -> tuple[int | None, int | None]:
    parts = value.lower().replace("×", "x").split("x", 1)
    if len(parts) != 2:
        return None, None
    return _as_int(parts[0]), _as_int(parts[1])
