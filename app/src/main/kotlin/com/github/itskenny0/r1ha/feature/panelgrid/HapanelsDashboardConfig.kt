package com.github.itskenny0.r1ha.feature.panelgrid

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

const val HAPANELS_DASHBOARD_SCHEMA_VERSION = 2

@Serializable
data class HapanelsDashboardConfig(
    val version: Int,
    @SerialName("dashboard_id") val dashboardId: String,
    val revision: Int,
    @SerialName("updated_by") val updatedBy: String,
    val title: String,
    val layout: HapanelsDashboardLayout,
    val theme: HapanelsThemeConfig = defaultHapanelsThemeConfig,
    @SerialName("always_on_display") val alwaysOnDisplay: HapanelsAlwaysOnDisplayConfig = HapanelsAlwaysOnDisplayConfig(),
    val people: List<HapanelsPersonConfig> = emptyList(),
    val tiles: List<HapanelsTileConfig> = emptyList(),
    val panels: List<HapanelsPanelConfig> = emptyList(),
    @SerialName("camera_actions") val cameraActions: List<String> = emptyList(),
    val extensions: JsonObject = JsonObject(emptyMap()),
    @SerialName("migration_report") val migrationReport: List<String> = emptyList(),
    @SerialName("layout_editor") val legacyLayoutEditor: JsonObject? = null,
)

@Serializable
data class HapanelsDashboardLayout(
    val type: String,
    @SerialName("columns_landscape") val columnsLandscape: Int,
    @SerialName("columns_portrait") val columnsPortrait: Int,
    val gap: String = "medium",
    val columns: Int? = null,
    val rows: Int? = null,
)

@Serializable
data class HapanelsPanelConfig(
    val id: String,
    val title: String,
    val layout: HapanelsDashboardLayout,
    val tiles: List<HapanelsTileConfig> = emptyList(),
)

@Serializable
data class HapanelsAlwaysOnDisplayConfig(
    val enabled: Boolean = false,
    val layout: HapanelsAlwaysOnDisplayLayout = HapanelsAlwaysOnDisplayLayout.MINIMAL_CLOCK,
    @SerialName("clock_style") val clockStyle: HapanelsAodClockStyle = HapanelsAodClockStyle.DEFAULT,
    @SerialName("grid_layout") val gridLayout: HapanelsDashboardLayout = HapanelsDashboardLayout(
        type = "fixed_grid",
        columnsLandscape = 3,
        columnsPortrait = 2,
        gap = "small",
    ),
    @SerialName("timeout_sec") val timeoutSec: Int = 300,
    @SerialName("brightness_percent") val brightnessPercent: Int = 3,
    @SerialName("wake_fade_ms") val wakeFadeMillis: Int = 500,
    val background: String = "#000000",
    @SerialName("entity_ids") val entityIds: List<String> = emptyList(),
    val tiles: List<HapanelsTileConfig> = emptyList(),
)

@Serializable
enum class HapanelsAlwaysOnDisplayLayout {
    @SerialName("minimal_clock") MINIMAL_CLOCK,
    @SerialName("status_strip") STATUS_STRIP,
    @SerialName("grid") GRID,
}

@Serializable
enum class HapanelsAodClockStyle {
    @SerialName("default") DEFAULT,
    @SerialName("modern") MODERN,
    @SerialName("warsaw_zaklad") WARSAW_ZAKLAD,
    @SerialName("cyberpunk_korpo") CYBERPUNK_KORPO,
    @SerialName("zew_puszczy") ZEW_PUSZCZY,
    @SerialName("popart") POPART,
    @SerialName("popart_multiples") POPART_MULTIPLES,
    @SerialName("italic_editorial") ITALIC_EDITORIAL,
    @SerialName("fullscreen_bold") FULLSCREEN_BOLD,
    @SerialName("fullscreen_heavy") FULLSCREEN_HEAVY,
    @SerialName("neon_baltic") NEON_BALTIC,
    @SerialName("electric_stained_glass") ELECTRIC_STAINED_GLASS,
    @SerialName("poznan_goats") POZNAN_GOATS,
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
    @SerialName("panel_id") val panelId: String? = null,
    val icon: String,
    val accent: HapanelsTileAccent = HapanelsTileAccent.ORANGE,
    val order: Int,
    val col: Int? = null,
    val row: Int? = null,
    val colSpan: Int? = null,
    val rowSpan: Int? = null,
    @SerialName("clock_style") val clockStyle: String? = null,
    @SerialName("cover_visual") val coverVisual: String? = null,
    @SerialName("cover_direction") val coverDirection: String? = null,
    @SerialName("tap_action") val tapAction: HapanelsTileAction? = null,
    @SerialName("hold_action") val holdAction: HapanelsTileAction? = null,
    val content: String? = null,
    val summary: String? = null,
    val secondary: String? = null,
    val presentation: HapanelsTilePresentation? = null,
    @SerialName("legacy_action") val legacyAction: Boolean = false,
    @SerialName("showIcon") val legacyShowIcon: Boolean? = null,
    @SerialName("showTitle") val legacyShowTitle: Boolean? = null,
    @SerialName("showSubtitle") val legacyShowSubtitle: Boolean? = null,
)

@Serializable
data class HapanelsTilePresentation(
    @SerialName("show_icon") val showIcon: Boolean = true,
    @SerialName("show_label") val showLabel: Boolean = true,
    @SerialName("show_value") val showValue: Boolean = true,
    @SerialName("show_secondary") val showSecondary: Boolean = true,
    val background: String = "surface",
    val border: String = "default",
    @SerialName("content_alignment") val contentAlignment: String = "center",
)

@Serializable
data class HapanelsTileAction(
    val type: String,
    val destination: String? = null,
    val action: String? = null,
    @SerialName("entity_id") val entityId: String? = null,
    @SerialName("panel_id") val panelId: String? = null,
    val domain: String? = null,
    val service: String? = null,
    val target: JsonObject? = null,
    val data: JsonObject? = null,
    val confirmation: HapanelsActionConfirmation? = null,
)

@Serializable
data class HapanelsActionConfirmation(
    val required: Boolean = true,
    val kind: String,
    val title: String? = null,
    val body: String? = null,
    @SerialName("negative_label") val negativeLabel: String? = null,
    @SerialName("positive_label") val positiveLabel: String? = null,
)

@Serializable
data class HapanelsDashboardPatch(
    @SerialName("base_revision") val baseRevision: Int,
    @SerialName("updated_by") val updatedBy: String,
    val surface: HapanelsDashboardSurface = HapanelsDashboardSurface.DASHBOARD,
    val theme: HapanelsThemeConfig? = null,
    @SerialName("aod_clock_style") val aodClockStyle: HapanelsAodClockStyle? = null,
    @SerialName("tile_updates") val tileUpdates: List<HapanelsTilePatch> = emptyList(),
)

@Serializable
enum class HapanelsDashboardSurface {
    @SerialName("dashboard") DASHBOARD,
    @SerialName("aod") AOD,
}

@Serializable
data class HapanelsTilePatch(
    val id: String,
    val kind: HapanelsTileKind? = null,
    val size: HapanelsTileSize? = null,
    val label: String? = null,
    @SerialName("short_label") val shortLabel: String? = null,
    @SerialName("entity_id") val entityId: String? = null,
    @SerialName("panel_id") val panelId: String? = null,
    val icon: String? = null,
    val accent: HapanelsTileAccent? = null,
    val order: Int? = null,
    val col: Int? = null,
    val row: Int? = null,
    val colSpan: Int? = null,
    val rowSpan: Int? = null,
    @SerialName("clock_style") val clockStyle: String? = null,
    @SerialName("cover_visual") val coverVisual: String? = null,
    @SerialName("cover_direction") val coverDirection: String? = null,
    @SerialName("tap_action") val tapAction: HapanelsTileAction? = null,
    @SerialName("hold_action") val holdAction: HapanelsTileAction? = null,
    val content: String? = null,
    val summary: String? = null,
    val secondary: String? = null,
    val presentation: HapanelsTilePresentation? = null,
)

sealed interface HapanelsDashboardPatchResult {
    data class Applied(val config: HapanelsDashboardConfig) : HapanelsDashboardPatchResult
    data class Conflict(
        val currentRevision: Int,
        val attemptedBaseRevision: Int,
        val currentConfig: HapanelsDashboardConfig,
    ) : HapanelsDashboardPatchResult
}

internal fun HapanelsDashboardConfig.syncStateJson(
    status: String,
    panelName: String? = null,
    attemptedBaseRevision: Int? = null,
    currentRevision: Int? = null,
): String = buildString {
    append('{')
    append("\"status\":\"")
    append(status.escapeJson())
    append("\",")
    append("\"dashboard_id\":\"")
    append(dashboardId.escapeJson())
    append("\",")
    append("\"revision\":")
    append(revision)
    append(',')
    append("\"updated_by\":\"")
    append(updatedBy.escapeJson())
    append('"')
    append(",\"schema_version\":")
    append(HAPANELS_DASHBOARD_SCHEMA_VERSION)
    append(",\"schema_capabilities\":[\"panels\",\"presentation\",\"text\",\"spacer\",\"tap_action\",\"technical_actions\"]")
    append(",\"supported_tile_kinds\":[\"clock\",\"category\",\"action\",\"entity\",\"cover\",\"camera\",\"folder\",\"popup\",\"text\",\"spacer\"]")
    append(",\"supported_action_types\":[\"none\",\"entity_default\",\"navigate\",\"local_panel\"]")
    panelName?.takeIf { it.isNotBlank() }?.let {
        append(',')
        append("\"panel_name\":\"")
        append(it.escapeJson())
        append('"')
    }
    if (currentRevision != null) {
        append(',')
        append("\"current_revision\":")
        append(currentRevision)
    }
    if (attemptedBaseRevision != null) {
        append(',')
        append("\"attempted_base_revision\":")
        append(attemptedBaseRevision)
    }
    append('}')
}

@Serializable
enum class HapanelsTileKind {
    @SerialName("clock") CLOCK,
    @SerialName("category") CATEGORY,
    @SerialName("action") ACTION,
    @SerialName("entity") ENTITY,
    @SerialName("cover") COVER,
    @SerialName("camera") CAMERA,
    @SerialName("folder") FOLDER,
    @SerialName("popup") POPUP,
    @SerialName("text") TEXT,
    @SerialName("spacer") SPACER,
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

private val dashboardJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

private fun String.escapeJson(): String = buildString(length) {
    for (char in this@escapeJson) {
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(char)
        }
    }
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
    "layout": "grid",
    "timeout_sec": 300,
    "brightness_percent": 3,
    "wake_fade_ms": 500,
    "background": "#000000",
    "grid_layout": {
      "type": "fixed_grid",
      "columns_landscape": 3,
      "columns_portrait": 2,
      "gap": "small"
    },
    "entity_ids": [],
    "tiles": [
      { "id": "aod_clock", "kind": "clock", "size": "large", "label": "Zegar", "icon": "clock", "accent": "white", "order": 0 },
      { "id": "aod_presence", "kind": "entity", "size": "small", "label": "Obecność", "entity_id": "binary_sensor.presence_home", "icon": "motion_sensor", "order": 1 },
      { "id": "aod_temperature", "kind": "entity", "size": "small", "label": "Temperatura", "entity_id": "sensor.outside_temperature", "icon": "home_thermometer", "accent": "orange", "order": 2 }
    ]
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
    { "id": "covers", "kind": "cover", "size": "large", "label": "Rolety i żaluzje", "short_label": "Rolety", "entity_id": "cover.all_covers", "icon": "blinds", "order": 11, "cover_visual": "blind", "cover_direction": "top" },
    { "id": "climate", "kind": "category", "size": "large", "label": "Klimat", "entity_id": "climate.home", "icon": "home_thermometer", "order": 12 },
    { "id": "cameras", "kind": "camera", "size": "large", "label": "Monitoring", "entity_id": "camera.front", "icon": "cctv", "order": 13 },
    { "id": "gate", "kind": "category", "size": "large", "label": "Brama", "entity_id": "cover.gate", "icon": "gate", "order": 14 },
    { "id": "energy", "kind": "entity", "size": "small", "label": "Energia", "entity_id": "sensor.home_power", "icon": "home_lightning", "accent": "red", "order": 20 },
    { "id": "presence", "kind": "entity", "size": "small", "label": "Obecność w domu", "short_label": "Obecność", "entity_id": "binary_sensor.presence_home", "icon": "motion_sensor", "order": 21 },
    { "id": "watering", "kind": "entity", "size": "small", "label": "Podlewanie", "entity_id": "switch.garden_watering", "icon": "sprinkler", "order": 22 },
    { "id": "settings", "kind": "action", "size": "small", "label": "Otwórz ustawienia", "short_label": "Ustawienia", "icon": "cog", "order": 23, "tap_action": { "type": "navigate", "destination": "settings" } }
  ],
  "camera_actions": ["Lista kamer", "Pełny ekran"]
}
"""
