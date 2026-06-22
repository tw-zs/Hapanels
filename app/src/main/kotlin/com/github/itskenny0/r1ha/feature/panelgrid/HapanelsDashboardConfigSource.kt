package com.github.itskenny0.r1ha.feature.panelgrid

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private const val DASHBOARD_CACHE_FILE = "hapanels_dashboard_config.json"

private val configJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    prettyPrint = true
}

/**
 * Local config source for the panel-grid mockup.
 *
 * This deliberately mirrors the future HA/MQTT flow: render from the last known
 * full config, not from hardcoded UI state. For now the cache is seeded with the
 * sample JSON; later MQTT can replace the cached file atomically on update.
 */
class HapanelsDashboardConfigSource(
    private val context: Context,
    private val cacheFileName: String = DASHBOARD_CACHE_FILE,
) {
    suspend fun loadOrSeed(): HapanelsDashboardConfig = withContext(Dispatchers.IO) {
        val file = dashboardFile()
        val raw = if (file.exists()) {
            file.readText()
        } else {
            file.writeText(SAMPLE_HAPANELS_DASHBOARD_JSON.trimIndent())
            SAMPLE_HAPANELS_DASHBOARD_JSON
        }
        runCatching { configJson.decodeFromString<HapanelsDashboardConfig>(raw) }
            .getOrElse {
                val fallback = sampleHapanelsDashboardConfig()
                file.writeText(configJson.encodeToString(fallback))
                fallback
            }
    }

    suspend fun exportRaw(): String = withContext(Dispatchers.IO) {
        val config = loadOrSeed()
        configJson.encodeToString(config)
    }

    suspend fun importRaw(raw: String): HapanelsDashboardConfig {
        val config = configJson.decodeFromString<HapanelsDashboardConfig>(raw)
        return withContext(Dispatchers.IO) {
            dashboardFile().writeText(configJson.encodeToString(config), Charsets.UTF_8)
            config
        }
    }

    suspend fun applyPatch(patch: HapanelsDashboardPatch): HapanelsDashboardPatchResult = withContext(Dispatchers.IO) {
        val current = loadOrSeed()
        if (current.revision != patch.baseRevision) {
            return@withContext HapanelsDashboardPatchResult.Conflict(
                currentRevision = current.revision,
                attemptedBaseRevision = patch.baseRevision,
                currentConfig = current,
            )
        }
        val updatesById = patch.tileUpdates.associateBy { it.id }
        val next = when (patch.surface) {
            HapanelsDashboardSurface.DASHBOARD -> current.copy(
                revision = current.revision + 1,
                updatedBy = patch.updatedBy,
                tiles = current.tiles.applyPatches(updatesById),
            )
            HapanelsDashboardSurface.AOD -> current.copy(
                revision = current.revision + 1,
                updatedBy = patch.updatedBy,
                alwaysOnDisplay = current.alwaysOnDisplay.copy(
                    tiles = current.alwaysOnDisplay.tiles.applyPatches(updatesById),
                ),
            )
        }
        dashboardFile().writeText(configJson.encodeToString(next), Charsets.UTF_8)
        HapanelsDashboardPatchResult.Applied(next)
    }

    suspend fun applyPatchRaw(raw: String): HapanelsDashboardPatchResult {
        val patch = configJson.decodeFromString<HapanelsDashboardPatch>(raw)
        return applyPatch(patch)
    }

    suspend fun resetToSample(): HapanelsDashboardConfig = withContext(Dispatchers.IO) {
        val config = sampleHapanelsDashboardConfig()
        dashboardFile().writeText(configJson.encodeToString(config), Charsets.UTF_8)
        config
    }

    private fun dashboardFile(): File = File(context.filesDir, cacheFileName)
}

private fun HapanelsTileConfig.applyPatch(patch: HapanelsTilePatch): HapanelsTileConfig = copy(
    label = patch.label ?: label,
    shortLabel = patch.shortLabel ?: shortLabel,
    entityId = patch.entityId ?: entityId,
    icon = patch.icon ?: icon,
    accent = patch.accent ?: accent,
    order = patch.order ?: order,
)

private fun List<HapanelsTileConfig>.applyPatches(
    updatesById: Map<String, HapanelsTilePatch>,
): List<HapanelsTileConfig> = map { tile ->
    updatesById[tile.id]?.let(tile::applyPatch) ?: tile
}
