from __future__ import annotations

import json
from pathlib import Path
from typing import Any

import voluptuous as vol

from homeassistant.components import mqtt, panel_custom, websocket_api
from homeassistant.config_entries import ConfigEntry
from homeassistant.core import HomeAssistant, ServiceCall, callback
from homeassistant.helpers.typing import ConfigType

from .const import (
    ATTR_CONFIG,
    ATTR_DEVICE,
    ATTR_PATCH,
    CONF_BASE_TOPIC,
    DATA_CONFIGS,
    DATA_PANELS,
    DATA_UNSUB,
    DEFAULT_BASE_TOPIC,
    DOMAIN,
    FRONTEND_VERSION,
    PANEL_ELEMENT,
    PANEL_URL_PATH,
    PLATFORMS,
    SERVICE_PATCH_DASHBOARD_CONFIG,
    SERVICE_SET_DASHBOARD_CONFIG,
    STATIC_URL_PATH,
)


async def async_setup(hass: HomeAssistant, config: ConfigType) -> bool:
    hass.data.setdefault(DOMAIN, {})
    return True


async def async_setup_entry(hass: HomeAssistant, entry: ConfigEntry) -> bool:
    hass.data.setdefault(DOMAIN, {})[entry.entry_id] = {
        DATA_PANELS: {},
        DATA_CONFIGS: {},
        DATA_UNSUB: [],
    }

    await hass.config_entries.async_forward_entry_setups(entry, PLATFORMS)
    _register_services(hass, entry)
    _register_websocket(hass)
    await _register_panel(hass)
    return True


async def async_unload_entry(hass: HomeAssistant, entry: ConfigEntry) -> bool:
    unload_ok = await hass.config_entries.async_unload_platforms(entry, PLATFORMS)
    for unsub in hass.data[DOMAIN].get(entry.entry_id, {}).get(DATA_UNSUB, []):
        unsub()
    hass.data[DOMAIN].pop(entry.entry_id, None)
    return unload_ok


def _register_services(hass: HomeAssistant, entry: ConfigEntry) -> None:
    if hass.services.has_service(DOMAIN, SERVICE_SET_DASHBOARD_CONFIG):
        return

    async def set_dashboard_config(call: ServiceCall) -> None:
        await _publish_json(
            hass,
            entry,
            call.data[ATTR_DEVICE],
            "dashboard/config/set",
            call.data[ATTR_CONFIG],
        )

    async def patch_dashboard_config(call: ServiceCall) -> None:
        await _publish_json(
            hass,
            entry,
            call.data[ATTR_DEVICE],
            "dashboard/config/patch/set",
            call.data[ATTR_PATCH],
        )

    hass.services.async_register(DOMAIN, SERVICE_SET_DASHBOARD_CONFIG, set_dashboard_config)
    hass.services.async_register(DOMAIN, SERVICE_PATCH_DASHBOARD_CONFIG, patch_dashboard_config)


def _register_websocket(hass: HomeAssistant) -> None:
    websocket_api.async_register_command(hass, websocket_list_panels)
    websocket_api.async_register_command(hass, websocket_get_dashboard_config)


@callback
@websocket_api.websocket_command({vol.Required("type"): "hapanels/list_panels"})
@websocket_api.async_response
async def websocket_list_panels(hass: HomeAssistant, connection, msg) -> None:
    panels = []
    for entry_data in hass.data.get(DOMAIN, {}).values():
        if not isinstance(entry_data, dict):
            continue
        for entity in entry_data.get(DATA_PANELS, {}).values():
            as_dict = getattr(entity, "as_dict", None)
            if as_dict is not None:
                panels.append(as_dict())
    connection.send_result(msg["id"], {"panels": panels})


@callback
@websocket_api.websocket_command({
    vol.Required("type"): "hapanels/get_dashboard_config",
    vol.Required("device"): str,
})
@websocket_api.async_response
async def websocket_get_dashboard_config(hass: HomeAssistant, connection, msg) -> None:
    device = msg["device"]
    for entry_data in hass.data.get(DOMAIN, {}).values():
        if isinstance(entry_data, dict) and device in entry_data.get(DATA_CONFIGS, {}):
            connection.send_result(msg["id"], {"device": device, "config": entry_data[DATA_CONFIGS][device]})
            return
    connection.send_result(msg["id"], {"device": device, "config": None})


async def _register_panel(hass: HomeAssistant) -> None:
    frontend_dir = Path(__file__).parent / "frontend"
    if hasattr(hass.http, "async_register_static_paths"):
        from homeassistant.components.http import StaticPathConfig

        await hass.http.async_register_static_paths([
            StaticPathConfig(STATIC_URL_PATH, str(frontend_dir), cache_headers=False),
        ])
    else:
        hass.http.async_register_static_path(STATIC_URL_PATH, str(frontend_dir), False)
    await panel_custom.async_register_panel(
        hass,
        webcomponent_name=PANEL_ELEMENT,
        frontend_url_path=PANEL_URL_PATH,
        module_url=f"{STATIC_URL_PATH}/hapanels-panel.js?v={FRONTEND_VERSION}",
        sidebar_title="Hapanels",
        sidebar_icon="mdi:tablet-dashboard",
        require_admin=False,
        config={},
    )


async def _publish_json(
    hass: HomeAssistant,
    entry: ConfigEntry,
    device: str,
    suffix: str,
    payload: Any,
) -> None:
    base_topic = entry.data.get(CONF_BASE_TOPIC, DEFAULT_BASE_TOPIC).strip("/")
    topic = f"{base_topic}/{device}/{suffix}"
    text = payload if isinstance(payload, str) else json.dumps(payload, separators=(",", ":"))
    await mqtt.async_publish(hass, topic, text, qos=0, retain=False)
