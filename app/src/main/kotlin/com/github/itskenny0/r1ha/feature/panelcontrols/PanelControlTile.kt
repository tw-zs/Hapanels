package com.github.itskenny0.r1ha.feature.panelcontrols

import com.github.itskenny0.r1ha.core.ha.EntityId
import com.github.itskenny0.r1ha.core.ha.EntityState
import com.github.itskenny0.r1ha.core.hardware.PanelCapabilities
import com.github.itskenny0.r1ha.core.hardware.PanelHardwareRuntimeState
import com.github.itskenny0.r1ha.core.prefs.AdvancedSettings
import java.time.Instant

enum class PanelControlTile(
    val favoriteId: String,
    val label: String,
    val entityId: String,
) {
    Relay1("hapanels://panel/relay_1", "Przekaźnik 1", "switch.hapanels_panel_relay_1"),
    ScreenBrightness("hapanels://panel/screen_brightness", "Jasność ekranu", "number.hapanels_panel_screen_brightness"),
    AutoBrightness("hapanels://panel/auto_brightness", "Auto-jasność", "switch.hapanels_panel_auto_brightness"),
    AmbientLight("hapanels://panel/ambient_light", "Światło otoczenia", "sensor.hapanels_panel_ambient_light"),
    PanelStatus("hapanels://panel/status", "Stan panelu", "binary_sensor.hapanels_panel_status"),
}

fun isPanelControlFavoriteId(id: String): Boolean = id.startsWith("hapanels://panel/")

fun panelControlTileForFavoriteId(id: String): PanelControlTile? = PanelControlTile.entries.firstOrNull { it.favoriteId == id }

fun panelControlTileForEntityId(id: String): PanelControlTile? = PanelControlTile.entries.firstOrNull { it.entityId == id }

fun availablePanelControlTiles(capabilities: PanelCapabilities): List<PanelControlTile> = buildList {
    if (capabilities.relayCount >= 1) add(PanelControlTile.Relay1)
    if (capabilities.supportsScreenBrightness) add(PanelControlTile.ScreenBrightness)
    if (capabilities.supportsScreenBrightness && capabilities.hasAmbientLightSensor) add(PanelControlTile.AutoBrightness)
    if (capabilities.hasAmbientLightSensor) add(PanelControlTile.AmbientLight)
    add(PanelControlTile.PanelStatus)
}

fun materializePanelControlTile(
    tile: PanelControlTile,
    capabilities: PanelCapabilities,
    runtime: PanelHardwareRuntimeState,
    advanced: AdvancedSettings,
): EntityState? {
    if (tile !in availablePanelControlTiles(capabilities)) return null
    val now = Instant.now()
    return when (tile) {
        PanelControlTile.Relay1 -> {
            val on = runtime.relayStates[1] == true
            EntityState(
                id = EntityId(tile.entityId),
                friendlyName = tile.label,
                area = "Kontrola panelu",
                isOn = on,
                percent = null,
                raw = null,
                lastChanged = now,
                isAvailable = runtime.relayStates.containsKey(1),
                supportsScalar = false,
                rawState = if (on) "on" else "off",
            )
        }
        PanelControlTile.ScreenBrightness -> {
            val pct = runtime.screenBrightnessPercent
            EntityState(
                id = EntityId(tile.entityId),
                friendlyName = tile.label,
                area = "Kontrola panelu",
                isOn = (pct ?: 0) > 0,
                percent = pct,
                raw = pct,
                lastChanged = now,
                isAvailable = pct != null,
                supportsScalar = true,
                rawState = pct?.toString() ?: "unknown",
                unit = "%",
                minRaw = 0.0,
                maxRaw = 100.0,
                step = 1.0,
            )
        }
        PanelControlTile.AutoBrightness -> {
            val enabled = advanced.autoBrightnessEnabled
            EntityState(
                id = EntityId(tile.entityId),
                friendlyName = tile.label,
                area = "Kontrola panelu",
                isOn = enabled,
                percent = null,
                raw = null,
                lastChanged = now,
                isAvailable = true,
                supportsScalar = false,
                rawState = if (enabled) "on" else "off",
            )
        }
        PanelControlTile.AmbientLight -> {
            val lux = runtime.ambientLightLux
            EntityState(
                id = EntityId(tile.entityId),
                friendlyName = tile.label,
                area = "Kontrola panelu",
                isOn = false,
                percent = null,
                raw = lux,
                lastChanged = now,
                isAvailable = lux != null,
                supportsScalar = false,
                rawState = lux?.let { "%.1f".format(it) } ?: "unknown",
                unit = "lx",
                deviceClass = "illuminance",
            )
        }
        PanelControlTile.PanelStatus -> EntityState(
            id = EntityId(tile.entityId),
            friendlyName = tile.label,
            area = "Kontrola panelu",
            isOn = true,
            percent = null,
            raw = null,
            lastChanged = now,
            isAvailable = true,
            supportsScalar = false,
            rawState = capabilities.providerLabel,
            deviceClass = "connectivity",
        )
    }
}
