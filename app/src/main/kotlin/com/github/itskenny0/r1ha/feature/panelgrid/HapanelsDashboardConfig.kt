package com.github.itskenny0.r1ha.feature.panelgrid

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class HapanelsDashboardConfig(
    val version: Int,
    @SerialName("dashboard_id") val dashboardId: String,
    val revision: Int,
    @SerialName("updated_by") val updatedBy: String,
    val title: String,
    val layout: HapanelsDashboardLayout,
    @SerialName("always_on_display") val alwaysOnDisplay: HapanelsAlwaysOnDisplayConfig = HapanelsAlwaysOnDisplayConfig(),
    val people: List<HapanelsPersonConfig> = emptyList(),
    val tiles: List<HapanelsTileConfig> = emptyList(),
    @SerialName("camera_actions") val cameraActions: List<String> = emptyList(),
)

@Serializable
data class HapanelsDashboardLayout(
    val type: String,
    @SerialName("columns_landscape") val columnsLandscape: Int,
    @SerialName("columns_portrait") val columnsPortrait: Int,
    val gap: String = "medium",
)

@Serializable
data class HapanelsAlwaysOnDisplayConfig(
    val enabled: Boolean = false,
    val layout: HapanelsAlwaysOnDisplayLayout = HapanelsAlwaysOnDisplayLayout.MINIMAL_CLOCK,
    @SerialName("entity_ids") val entityIds: List<String> = emptyList(),
)

@Serializable
enum class HapanelsAlwaysOnDisplayLayout {
    @SerialName("minimal_clock") MINIMAL_CLOCK,
    @SerialName("status_strip") STATUS_STRIP,
}

@Serializable
data class HapanelsPersonConfig(
    val id: String,
    val name: String,
    val state: String,
    val status: HapanelsPersonStatus,
)

@Serializable
enum class HapanelsPersonStatus {
    @SerialName("home") HOME,
    @SerialName("away") AWAY,
    @SerialName("unknown") UNKNOWN,
}

@Serializable
data class HapanelsTileConfig(
    val id: String,
    val kind: HapanelsTileKind,
    val size: HapanelsTileSize,
    val label: String,
    @SerialName("short_label") val shortLabel: String? = null,
    @SerialName("entity_id") val entityId: String? = null,
    val icon: HapanelsPanelIcon,
    val accent: HapanelsTileAccent = HapanelsTileAccent.ORANGE,
    val order: Int,
)

@Serializable
data class HapanelsDashboardPatch(
    @SerialName("base_revision") val baseRevision: Int,
    @SerialName("updated_by") val updatedBy: String,
    @SerialName("tile_updates") val tileUpdates: List<HapanelsTilePatch> = emptyList(),
)

@Serializable
data class HapanelsTilePatch(
    val id: String,
    val label: String? = null,
    @SerialName("short_label") val shortLabel: String? = null,
    @SerialName("entity_id") val entityId: String? = null,
    val icon: HapanelsPanelIcon? = null,
    val accent: HapanelsTileAccent? = null,
    val order: Int? = null,
)

sealed interface HapanelsDashboardPatchResult {
    data class Applied(val config: HapanelsDashboardConfig) : HapanelsDashboardPatchResult
    data class Conflict(
        val currentRevision: Int,
        val attemptedBaseRevision: Int,
        val currentConfig: HapanelsDashboardConfig,
    ) : HapanelsDashboardPatchResult
}

@Serializable
enum class HapanelsTileKind {
    @SerialName("category") CATEGORY,
    @SerialName("action") ACTION,
    @SerialName("entity") ENTITY,
}

@Serializable
enum class HapanelsTileSize {
    @SerialName("large") LARGE,
    @SerialName("small") SMALL,
    @SerialName("action") ACTION,
}

@Serializable
enum class HapanelsTileAccent {
    @SerialName("orange") ORANGE,
    @SerialName("red") RED,
    @SerialName("white") WHITE,
}

@Serializable
enum class HapanelsPanelIcon {
    @SerialName("lightbulb") LIGHTBULB,
    @SerialName("lightbulb_off") LIGHTBULB_OFF,
    @SerialName("shield_lock") SHIELD_LOCK,
    @SerialName("blinds") BLINDS,
    @SerialName("home_thermometer") HOME_THERMOMETER,
    @SerialName("cctv") CCTV,
    @SerialName("gate") GATE,
    @SerialName("home_lightning") HOME_LIGHTNING,
    @SerialName("motion_sensor") MOTION_SENSOR,
    @SerialName("sprinkler") SPRINKLER,
    @SerialName("cog") COG,
}

private val dashboardJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

fun sampleHapanelsDashboardConfig(): HapanelsDashboardConfig =
    dashboardJson.decodeFromString(SAMPLE_HAPANELS_DASHBOARD_JSON)

const val SAMPLE_HAPANELS_DASHBOARD_JSON = """
{
  "version": 1,
  "dashboard_id": "home-panel-main",
  "revision": 42,
  "updated_by": "homeassistant:hapanels_mock_editor",
  "title": "Panel domowy",
  "layout": {
    "type": "fixed_grid",
    "columns_landscape": 3,
    "columns_portrait": 2,
    "gap": "medium"
  },
  "always_on_display": {
    "enabled": false,
    "layout": "minimal_clock",
    "entity_ids": []
  },
  "people": [
    { "id": "person.michal", "name": "Michał", "state": "poza domem", "status": "away" },
    { "id": "person.bartek", "name": "Bartek", "state": "poza domem", "status": "away" },
    { "id": "person.marcin", "name": "Marcin", "state": "nieznany", "status": "unknown" },
    { "id": "person.tomek", "name": "Tomek", "state": "w domu", "status": "home" }
  ],
  "tiles": [
    { "id": "all_lights_off", "kind": "action", "size": "action", "label": "Zgaś wszystko", "icon": "lightbulb_off", "accent": "white", "order": 0 },
    { "id": "alarm_settings", "kind": "action", "size": "action", "label": "Ustawienia alarmu", "short_label": "Alarm", "icon": "shield_lock", "accent": "white", "order": 1 },
    { "id": "lights", "kind": "category", "size": "large", "label": "Oświetlenie", "entity_id": "group.all_lights", "icon": "lightbulb", "order": 10 },
    { "id": "covers", "kind": "category", "size": "large", "label": "Rolety i żaluzje", "short_label": "Rolety", "entity_id": "cover.all_covers", "icon": "blinds", "order": 11 },
    { "id": "climate", "kind": "category", "size": "large", "label": "Klimat", "entity_id": "climate.home", "icon": "home_thermometer", "order": 12 },
    { "id": "cameras", "kind": "category", "size": "large", "label": "Monitoring", "entity_id": "camera.front", "icon": "cctv", "order": 13 },
    { "id": "gate", "kind": "category", "size": "large", "label": "Brama", "entity_id": "cover.gate", "icon": "gate", "order": 14 },
    { "id": "energy", "kind": "entity", "size": "small", "label": "Energia", "entity_id": "sensor.home_power", "icon": "home_lightning", "accent": "red", "order": 20 },
    { "id": "presence", "kind": "entity", "size": "small", "label": "Obecność w domu", "short_label": "Obecność", "entity_id": "binary_sensor.presence_home", "icon": "motion_sensor", "order": 21 },
    { "id": "watering", "kind": "entity", "size": "small", "label": "Podlewanie", "entity_id": "switch.garden_watering", "icon": "sprinkler", "order": 22 },
    { "id": "settings", "kind": "action", "size": "small", "label": "Konfiguracja", "short_label": "Config", "icon": "cog", "order": 23 }
  ],
  "camera_actions": ["Wyłącz kamery", "Włącz kamery"]
}
"""
