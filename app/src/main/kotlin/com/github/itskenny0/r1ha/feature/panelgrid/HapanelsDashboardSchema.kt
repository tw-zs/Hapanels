package com.github.itskenny0.r1ha.feature.panelgrid

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class HapanelsDashboardValidationException(
    val errors: List<String>,
) : IllegalArgumentException(errors.joinToString("; "))

internal fun HapanelsDashboardConfig.migrateToCurrentSchema(): HapanelsDashboardConfig {
    if (version == HAPANELS_DASHBOARD_SCHEMA_VERSION) return this
    require(version == 1) { "Unsupported dashboard schema version: $version" }

    val panelIds = tiles.mapNotNull { it.panelId?.takeIf(String::isNotBlank) }.distinct()
    val panelOpeners = tiles
        .filter { it.kind in panelOpenerKinds && !it.panelId.isNullOrBlank() }
        .associateBy { it.panelId!! }
    val migratedPanels = panelIds.map { panelId ->
        val opener = panelOpeners[panelId]
        HapanelsPanelConfig(
            id = panelId,
            title = opener?.label?.takeIf(String::isNotBlank) ?: panelId,
            layout = layout.copy(columns = layout.columns ?: 12, rows = layout.rows ?: 9),
            tiles = tiles
                .filter { it.panelId == panelId && it.kind !in panelOpenerKinds }
                .map { it.migrateLegacyTile(ownerPanelId = panelId) },
        )
    }
    val report = migrationReport.toMutableList()
    val migratedRootTiles = tiles
        .filter { it.panelId.isNullOrBlank() || it.kind in panelOpenerKinds }
        .map { it.migrateLegacyTile(report = report) }
    val migratedAodTiles = alwaysOnDisplay.tiles.map { it.migrateLegacyTile(report = report, interactive = false) }.toMutableList()
    alwaysOnDisplay.entityIds.forEachIndexed { index, entityId ->
        if (migratedAodTiles.none { it.entityId == entityId }) {
            migratedAodTiles += HapanelsTileConfig(
                id = uniqueMigratedTileId("aod_${entityId.toStableId()}", migratedAodTiles),
                kind = HapanelsTileKind.ENTITY,
                size = HapanelsTileSize.SMALL,
                label = entityId.substringAfter('.').replace('_', ' '),
                entityId = entityId,
                icon = "mdi:cog",
                accent = HapanelsTileAccent.ORANGE,
                order = migratedAodTiles.size + index,
                tapAction = HapanelsTileAction(type = "none"),
                presentation = defaultPresentation(HapanelsTileKind.ENTITY),
            )
        }
    }
    if (alwaysOnDisplay.entityIds.isNotEmpty()) report += "Przeniesiono legacy AOD entity_ids do tiles"
    val migratedExtensions = if (legacyLayoutEditor == null) {
        extensions
    } else {
        buildJsonObject {
            extensions.forEach { (key, value) -> put(key, value) }
            put("legacy_layout_editor", legacyLayoutEditor)
        }
    }
    return copy(
        version = HAPANELS_DASHBOARD_SCHEMA_VERSION,
        layout = layout.copy(columns = layout.columns ?: 12, rows = layout.rows ?: 9),
        tiles = migratedRootTiles,
        panels = migratedPanels,
        alwaysOnDisplay = alwaysOnDisplay.copy(entityIds = emptyList(), tiles = migratedAodTiles),
        extensions = migratedExtensions,
        migrationReport = report.distinct(),
        legacyLayoutEditor = null,
    )
}

internal fun HapanelsDashboardConfig.validateCurrentSchema() {
    val errors = mutableListOf<String>()
    if (version != HAPANELS_DASHBOARD_SCHEMA_VERSION) errors += "version: expected $HAPANELS_DASHBOARD_SCHEMA_VERSION"
    if (dashboardId.isBlank()) errors += "dashboard_id: must not be blank"
    if (revision < 0) errors += "revision: must be >= 0"
    if (updatedBy.isBlank()) errors += "updated_by: must not be blank"
    validateLayout("layout", layout, errors)

    val panelIds = panels.map { it.id }
    duplicateValues(panelIds).forEach { errors += "panels: duplicate id '$it'" }
    panels.forEachIndexed { index, panel ->
        if (panel.id.isBlank()) errors += "panels[$index].id: must not be blank"
        if (panel.title.isBlank()) errors += "panels[$index].title: must not be blank"
        validateLayout("panels[$index].layout", panel.layout, errors)
    }

    val allTiles = buildList {
        tiles.forEach { add("tiles" to it) }
        panels.forEachIndexed { panelIndex, panel -> panel.tiles.forEach { add("panels[$panelIndex].tiles" to it) } }
        alwaysOnDisplay.tiles.forEach { add("always_on_display.tiles" to it) }
    }
    duplicateValues(allTiles.map { it.second.id }).forEach { errors += "tiles: duplicate id '$it'" }
    validateTileCollection("tiles", tiles, layout, panelIds.toSet(), false, errors)
    panels.forEachIndexed { index, panel ->
        validateTileCollection("panels[$index].tiles", panel.tiles, panel.layout, panelIds.toSet(), false, errors)
    }
    validateTileCollection("always_on_display.tiles", alwaysOnDisplay.tiles, alwaysOnDisplay.gridLayout, emptySet(), true, errors)
    if (alwaysOnDisplay.entityIds.isNotEmpty()) errors += "always_on_display.entity_ids: unsupported in schema v2"
    val aodLimit = if (alwaysOnDisplay.layout == HapanelsAlwaysOnDisplayLayout.STATUS_STRIP) 4 else 6
    if (alwaysOnDisplay.tiles.size > aodLimit) errors += "always_on_display.tiles: maximum $aodLimit items"
    if (alwaysOnDisplay.timeoutSec !in 5..86_400) errors += "always_on_display.timeout_sec: must be 5..86400"
    if (alwaysOnDisplay.brightnessPercent !in 1..100) errors += "always_on_display.brightness_percent: must be 1..100"
    if (alwaysOnDisplay.wakeFadeMillis !in 0..2_000) errors += "always_on_display.wake_fade_ms: must be 0..2000"
    if (legacyLayoutEditor != null) errors += "layout_editor: unsupported in schema v2; use extensions"

    validatePanelGraph(panelIds.toSet(), errors)
    if (errors.isNotEmpty()) throw HapanelsDashboardValidationException(errors)
}

private fun HapanelsDashboardConfig.validatePanelGraph(panelIds: Set<String>, errors: MutableList<String>) {
    fun targets(sourceTiles: List<HapanelsTileConfig>): List<String> = sourceTiles
        .filter { it.kind in panelOpenerKinds }
        .mapNotNull { it.panelId?.takeIf(String::isNotBlank) }

    (targets(tiles) + panels.flatMap { targets(it.tiles) }).filterNot(panelIds::contains).distinct().forEach {
        errors += "panel_id: target '$it' does not exist"
    }
    val edges = panels.associate { panel -> panel.id to targets(panel.tiles).filter(panelIds::contains) }
    fun visit(id: String, path: List<String>) {
        if (id in path) {
            errors += "panels: cycle ${ (path + id).joinToString(" -> ") }"
            return
        }
        if (path.size >= 3) {
            errors += "panels: depth exceeds 3 at '$id'"
            return
        }
        edges[id].orEmpty().forEach { visit(it, path + id) }
    }
    targets(tiles).filter(panelIds::contains).forEach { visit(it, emptyList()) }
}

private fun validateTileCollection(
    path: String,
    tiles: List<HapanelsTileConfig>,
    layout: HapanelsDashboardLayout,
    panelIds: Set<String>,
    aod: Boolean,
    errors: MutableList<String>,
) {
    tiles.forEachIndexed { index, tile -> validateTile("$path[$index]", tile, panelIds, aod, errors) }
    val columns = layout.columns ?: 12
    val rows = layout.rows ?: 9
    val placed = tiles.mapIndexedNotNull { index, tile ->
        if (tile.col == null && tile.row == null) return@mapIndexedNotNull null
        if (tile.col == null || tile.row == null) {
            errors += "$path[$index]: col and row must be set together"
            return@mapIndexedNotNull null
        }
        val colSpan = tile.colSpan ?: 1
        val rowSpan = tile.rowSpan ?: 1
        if (tile.col < 1 || tile.row < 1 || colSpan < 1 || rowSpan < 1 || tile.col + colSpan - 1 > columns || tile.row + rowSpan - 1 > rows) {
            errors += "$path[$index]: tile is outside ${columns}x$rows grid"
            return@mapIndexedNotNull null
        }
        TileArea(tile.id, tile.col, tile.row, colSpan, rowSpan)
    }
    placed.forEachIndexed { index, area ->
        placed.drop(index + 1).firstOrNull(area::overlaps)?.let { errors += "$path: tiles '${area.id}' and '${it.id}' overlap" }
    }
}

private fun validateTile(path: String, tile: HapanelsTileConfig, panelIds: Set<String>, aod: Boolean, errors: MutableList<String>) {
    if (tile.id.isBlank()) errors += "$path.id: must not be blank"
    if (tile.kind != HapanelsTileKind.SPACER && tile.kind != HapanelsTileKind.TEXT && tile.label.isBlank()) errors += "$path.label: must not be blank"
    if (tile.kind != HapanelsTileKind.SPACER && tile.kind != HapanelsTileKind.TEXT && tile.kind != HapanelsTileKind.CLOCK && tile.icon.isBlank()) errors += "$path.icon: must not be blank"
    if (!tile.entityId.isNullOrBlank() && !entityIdRegex.matches(tile.entityId)) errors += "$path.entity_id: expected domain.object_id"
    if (tile.kind in entityKinds && tile.entityId.isNullOrBlank()) errors += "$path.entity_id: required for ${tile.kind.name.lowercase()}"
    if (tile.kind == HapanelsTileKind.TEXT && tile.content.isNullOrBlank()) errors += "$path.content: required for text"
    if (tile.kind == HapanelsTileKind.SPACER) {
        if (tile.presentation != null) errors += "$path.presentation: spacer must not define presentation"
        if (tile.tapAction != null || tile.holdAction != null) errors += "$path: spacer must not define actions"
        if (tile.entityId != null) errors += "$path.entity_id: spacer must not define entity"
    } else {
        validatePresentation(path, tile, errors)
    }
    if (tile.kind in strictPanelOpenerKinds && tile.panelId.isNullOrBlank()) errors += "$path.panel_id: required for ${tile.kind.name.lowercase()}"
    if (tile.kind == HapanelsTileKind.CATEGORY && tile.panelId.isNullOrBlank() && !tile.legacyAction) errors += "$path.panel_id: required for category"
    if (!tile.panelId.isNullOrBlank() && tile.kind in panelOpenerKinds && tile.panelId !in panelIds) errors += "$path.panel_id: target '${tile.panelId}' does not exist"
    validateAction("$path.tap_action", tile.tapAction, errors)
    validateAction("$path.hold_action", tile.holdAction, errors)
    listOfNotNull(tile.tapAction, tile.holdAction).forEach { action ->
        if (action.type == "navigate" && !action.panelId.isNullOrBlank() && action.panelId !in panelIds) errors += "$path: action panel_id '${action.panelId}' does not exist"
    }
    if (tile.holdAction != null) errors += "$path.hold_action: unsupported by this tablet version"
    if (tile.kind == HapanelsTileKind.ACTION && tile.tapAction == null) errors += "$path.tap_action: required for action tile"
    if (tile.legacyShowIcon != null || tile.legacyShowTitle != null || tile.legacyShowSubtitle != null) errors += "$path: legacy showIcon/showTitle/showSubtitle are invalid in schema v2"
    if (tile.accent == HapanelsTileAccent.RED && !tile.hasUnsafeConfirmedAction()) errors += "$path.accent: red requires unsafe confirmed action"
    if (aod) {
        if (tile.kind !in aodKinds) errors += "$path.kind: ${tile.kind.name.lowercase()} is not allowed in AOD"
        if (tile.tapAction?.type !in setOf(null, "none") || tile.holdAction != null) errors += "$path: AOD tiles must be read-only"
    }
}

private fun validatePresentation(path: String, tile: HapanelsTileConfig, errors: MutableList<String>) {
    val presentation = tile.presentation ?: return
    if (presentation.background !in setOf("surface", "transparent")) errors += "$path.presentation.background: unknown value"
    if (presentation.border !in setOf("default", "none")) errors += "$path.presentation.border: unknown value"
    if (presentation.contentAlignment !in setOf("start", "center", "end")) errors += "$path.presentation.content_alignment: unknown value"
    val guaranteedVisible = (presentation.showLabel && tile.label.isNotBlank()) ||
        (presentation.showIcon && tile.icon.isNotBlank()) ||
        (presentation.showValue && (tile.kind in entityKinds || tile.kind == HapanelsTileKind.CLOCK || !tile.content.isNullOrBlank())) ||
        (presentation.showSecondary && !tile.secondary.isNullOrBlank())
    if ((tile.tapAction?.type !in setOf(null, "none") || tile.holdAction != null) && !guaranteedVisible) errors += "$path.presentation: interactive tile has no visible channel"
}

private fun validateAction(path: String, action: HapanelsTileAction?, errors: MutableList<String>) {
    if (action == null) return
    if (action.type !in actionTypes) {
        errors += "$path.type: unknown action '${action.type}'"
        return
    }
    when (action.type) {
        "entity_default" -> {
            if (action.entityId.isNullOrBlank() || !entityIdRegex.matches(action.entityId)) errors += "$path.entity_id: required"
            else if (action.entityId.substringBefore('.') !in safeDefaultDomains) errors += "$path.entity_id: domain does not support safe default action"
        }
        "navigate" -> {
            if (action.destination.isNullOrBlank() == action.panelId.isNullOrBlank()) errors += "$path: provide exactly one destination or panel_id"
            if (!action.destination.isNullOrBlank() && action.destination !in supportedDestinations) errors += "$path.destination: unsupported destination '${action.destination}'"
        }
        "local_panel" -> if (action.action !in supportedLocalActions) errors += "$path.action: unsupported local action '${action.action}'"
        "call_service" -> {
            if (action.domain.isNullOrBlank()) errors += "$path.domain: required"
            if (action.service.isNullOrBlank()) errors += "$path.service: required"
            if (action.target == null || action.target.isEmpty()) errors += "$path.target: required"
        }
    }
    action.confirmation?.let { confirmation ->
        if (confirmation.kind !in confirmationKinds) errors += "$path.confirmation.kind: unknown value '${confirmation.kind}'"
        if (confirmation.kind == "custom" && (confirmation.negativeLabel.isNullOrBlank() || confirmation.positiveLabel.isNullOrBlank())) {
            errors += "$path.confirmation: custom requires negative_label and positive_label"
        }
    }
}

private fun HapanelsTileConfig.migrateLegacyTile(
    ownerPanelId: String? = null,
    report: MutableList<String> = mutableListOf(),
    interactive: Boolean = true,
): HapanelsTileConfig {
    val migratedPresentation = presentation ?: defaultPresentation(kind).copy(
        showIcon = legacyShowIcon ?: defaultPresentation(kind).showIcon,
        showLabel = legacyShowTitle ?: defaultPresentation(kind).showLabel,
        showSecondary = legacyShowSubtitle ?: defaultPresentation(kind).showSecondary,
    )
    val migratedAction = when {
        !interactive -> HapanelsTileAction(type = "none")
        tapAction != null -> tapAction
        kind in panelOpenerKinds || kind == HapanelsTileKind.CLOCK || kind == HapanelsTileKind.SPACER || kind == HapanelsTileKind.TEXT -> null
        kind == HapanelsTileKind.CATEGORY && panelId.isNullOrBlank() -> null
        entityId.isNullOrBlank() -> if (kind == HapanelsTileKind.ACTION) HapanelsTileAction(type = "none") else null
        entityId.substringBefore('.') in safeDefaultDomains -> HapanelsTileAction(type = "entity_default", entityId = entityId)
        entityId.substringBefore('.') in readOnlyDomains -> HapanelsTileAction(type = "none")
        else -> HapanelsTileAction(type = "none")
    }
    val keepRed = listOfNotNull(migratedAction, holdAction).any { it.confirmation?.required == true && it.confirmation.kind in unsafeConfirmationKinds }
    val migratedAccent = if (accent == HapanelsTileAccent.RED && !keepRed) {
        report += "Dekoracyjny czerwony akcent zmieniono na neutralny"
        HapanelsTileAccent.WHITE
    } else {
        accent
    }
    return copy(
        panelId = if (ownerPanelId == null || kind in panelOpenerKinds) panelId else null,
        accent = migratedAccent,
        tapAction = migratedAction,
        presentation = if (kind == HapanelsTileKind.SPACER) null else migratedPresentation,
        legacyAction = kind == HapanelsTileKind.CATEGORY && panelId.isNullOrBlank() && !entityId.isNullOrBlank(),
        legacyShowIcon = null,
        legacyShowTitle = null,
        legacyShowSubtitle = null,
    )
}

internal fun defaultPresentation(kind: HapanelsTileKind): HapanelsTilePresentation = when (kind) {
    HapanelsTileKind.CLOCK -> HapanelsTilePresentation(showIcon = false, showLabel = false)
    HapanelsTileKind.ACTION, HapanelsTileKind.FOLDER, HapanelsTileKind.POPUP -> HapanelsTilePresentation(showValue = false, showSecondary = false)
    HapanelsTileKind.TEXT -> HapanelsTilePresentation(showIcon = false, showLabel = false, showSecondary = false)
    HapanelsTileKind.SPACER -> HapanelsTilePresentation(showIcon = false, showLabel = false, showValue = false, showSecondary = false, background = "transparent", border = "none")
    else -> HapanelsTilePresentation()
}

private fun validateLayout(path: String, layout: HapanelsDashboardLayout, errors: MutableList<String>) {
    if (layout.type != "fixed_grid") errors += "$path.type: expected fixed_grid"
    if (layout.columnsLandscape < 1 || layout.columnsPortrait < 1) errors += "$path: columns must be >= 1"
    if (layout.gap !in setOf("small", "medium", "large")) errors += "$path.gap: unknown value '${layout.gap}'"
    if (layout.columns != null && layout.columns < 1) errors += "$path.columns: must be >= 1"
    if (layout.rows != null && layout.rows < 1) errors += "$path.rows: must be >= 1"
}

private fun HapanelsTileConfig.hasUnsafeConfirmedAction(): Boolean = listOfNotNull(tapAction, holdAction).any {
    it.confirmation?.required == true && it.confirmation.kind in unsafeConfirmationKinds
}

private fun String.toStableId(): String = lowercase().map { if (it.isLetterOrDigit()) it else '_' }.joinToString("").trim('_')

private fun uniqueMigratedTileId(base: String, tiles: List<HapanelsTileConfig>): String {
    val used = tiles.mapTo(mutableSetOf(), HapanelsTileConfig::id)
    if (base !in used) return base
    var suffix = 2
    while ("${base}_$suffix" in used) suffix++
    return "${base}_$suffix"
}

private fun <T> duplicateValues(values: List<T>): Set<T> = values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys

private data class TileArea(val id: String, val col: Int, val row: Int, val colSpan: Int, val rowSpan: Int) {
    fun overlaps(other: TileArea): Boolean = col < other.col + other.colSpan && col + colSpan > other.col && row < other.row + other.rowSpan && row + rowSpan > other.row
}

private val panelOpenerKinds = setOf(HapanelsTileKind.FOLDER, HapanelsTileKind.POPUP, HapanelsTileKind.CATEGORY)
private val strictPanelOpenerKinds = setOf(HapanelsTileKind.FOLDER, HapanelsTileKind.POPUP)
private val entityKinds = setOf(HapanelsTileKind.ENTITY, HapanelsTileKind.COVER, HapanelsTileKind.CAMERA)
private val aodKinds = setOf(HapanelsTileKind.CLOCK, HapanelsTileKind.ENTITY, HapanelsTileKind.TEXT)
private val actionTypes = setOf("none", "entity_default", "navigate", "local_panel")
private val confirmationKinds = setOf("unlock", "cover_move", "delete_tile", "delete_panel", "clear_tray", "disarm_alarm", "custom")
private val unsafeConfirmationKinds = setOf("unlock", "cover_move", "delete_tile", "delete_panel", "clear_tray", "disarm_alarm", "custom")
private val safeDefaultDomains = setOf("light", "switch", "input_boolean", "automation", "fan", "scene", "script", "button", "input_button")
private val readOnlyDomains = setOf("sensor", "binary_sensor", "person", "device_tracker")
private val supportedDestinations = setOf("settings", "settings/appearance", "settings/behaviour", "settings/integrations", "panel_diagnostics")
private val supportedLocalActions = setOf("screen.aod_now", "connection.reconnect_home_assistant")
private val entityIdRegex = Regex("^[a-z0-9_]+\\.[a-z0-9_]+$")
