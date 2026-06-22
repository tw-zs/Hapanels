from __future__ import annotations

import voluptuous as vol

from homeassistant import config_entries

from .const import CONF_BASE_TOPIC, DEFAULT_BASE_TOPIC, DOMAIN


class HapanelsConfigFlow(config_entries.ConfigFlow, domain=DOMAIN):
    VERSION = 1

    async def async_step_user(self, user_input=None):
        errors = {}
        if user_input is not None:
            await self.async_set_unique_id(DOMAIN)
            self._abort_if_unique_id_configured()
            return self.async_create_entry(title="Hapanels", data=user_input)

        return self.async_show_form(
            step_id="user",
            data_schema=vol.Schema({vol.Optional(CONF_BASE_TOPIC, default=DEFAULT_BASE_TOPIC): str}),
            errors=errors,
        )
