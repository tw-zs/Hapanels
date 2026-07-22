package com.github.itskenny0.r1ha.feature.panelgrid

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap

private const val DASHBOARD_CACHE_FILE = "hapanels_dashboard_config.json"

private val configJson = Json {
    ignoreUnknownKeys = false
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
    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changes = _changes.asSharedFlow()
    private val fileMutex by lazy {
        fileMutexes.computeIfAbsent(dashboardFile().canonicalPath) { Mutex() }
    }

    suspend fun loadOrSeed(): HapanelsDashboardConfig = fileMutex.withLock {
        withContext(Dispatchers.IO) { loadOrSeedLocked() }
    }

    private fun loadOrSeedLocked(): HapanelsDashboardConfig {
        val file = dashboardFile()
        val raw = if (file.exists()) file.readText() else SAMPLE_HAPANELS_DASHBOARD_JSON
        return runCatching { decodeCurrentConfig(raw) }
            .onSuccess { config ->
                if (!file.exists() || config.version != configJson.decodeFromString<HapanelsDashboardConfig>(raw).version) {
                    writeConfig(file, config)
                }
            }
            .getOrElse {
                val fallback = sampleHapanelsDashboardConfig().migrateToCurrentSchema().also(HapanelsDashboardConfig::validateCurrentSchema)
                writeConfig(file, fallback)
                fallback
            }
    }

    suspend fun exportRaw(): String = withContext(Dispatchers.IO) {
        val config = loadOrSeed()
        configJson.encodeToString(config)
    }

    suspend fun importRaw(raw: String): HapanelsDashboardConfig {
        val config = decodeCurrentConfig(raw)
        return fileMutex.withLock {
            withContext(Dispatchers.IO) {
                writeConfig(dashboardFile(), config)
                _changes.tryEmit(Unit)
                config
            }
        }
    }

    suspend fun applyPatch(patch: HapanelsDashboardPatch): HapanelsDashboardPatchResult = fileMutex.withLock {
        withContext(Dispatchers.IO) {
            val current = loadOrSeedLocked()
            if (current.revision != patch.baseRevision) {
                return@withContext HapanelsDashboardPatchResult.Conflict(
                    currentRevision = current.revision,
                    attemptedBaseRevision = patch.baseRevision,
                    currentConfig = current,
                )
            }
            val updatesById = patch.tileUpdates.associateBy { it.id }
            val patchedTheme = patch.theme ?: current.theme
            val next = when (patch.surface) {
                HapanelsDashboardSurface.DASHBOARD -> current.copy(
                    revision = current.revision + 1,
                    updatedBy = patch.updatedBy,
                    theme = patchedTheme,
                    tiles = current.tiles.applyPatches(updatesById),
                    panels = current.panels.map { panel -> panel.copy(tiles = panel.tiles.applyPatches(updatesById)) },
                )
                HapanelsDashboardSurface.AOD -> current.copy(
                    revision = current.revision + 1,
                    updatedBy = patch.updatedBy,
                    theme = patchedTheme,
                    alwaysOnDisplay = current.alwaysOnDisplay.copy(
                        clockStyle = patch.aodClockStyle ?: current.alwaysOnDisplay.clockStyle,
                        tiles = current.alwaysOnDisplay.tiles.applyPatches(updatesById),
                    ),
                )
            }
            next.validateCurrentSchema()
            writeConfig(dashboardFile(), next)
            _changes.tryEmit(Unit)
            HapanelsDashboardPatchResult.Applied(next)
        }
    }

    suspend fun applyPatchRaw(raw: String): HapanelsDashboardPatchResult {
        val patch = configJson.decodeFromString<HapanelsDashboardPatch>(raw)
        return applyPatch(patch)
    }

    suspend fun resetToSample(): HapanelsDashboardConfig = fileMutex.withLock {
        withContext(Dispatchers.IO) {
            val config = sampleHapanelsDashboardConfig().migrateToCurrentSchema().also(HapanelsDashboardConfig::validateCurrentSchema)
            writeConfig(dashboardFile(), config)
            _changes.tryEmit(Unit)
            config
        }
    }

    private fun dashboardFile(): File = File(context.filesDir, cacheFileName)

    private fun decodeCurrentConfig(raw: String): HapanelsDashboardConfig =
        configJson.decodeFromString<HapanelsDashboardConfig>(raw)
            .migrateToCurrentSchema()
            .also(HapanelsDashboardConfig::validateCurrentSchema)

    private fun writeConfig(file: File, config: HapanelsDashboardConfig) {
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(configJson.encodeToString(config), Charsets.UTF_8)
        Files.move(
            temporary.toPath(),
            file.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING,
        )
    }

    private companion object {
        val fileMutexes = ConcurrentHashMap<String, Mutex>()
    }
}

private fun HapanelsTileConfig.applyPatch(patch: HapanelsTilePatch): HapanelsTileConfig = copy(
    kind = patch.kind ?: kind,
    size = patch.size ?: size,
    label = patch.label ?: label,
    shortLabel = patch.shortLabel ?: shortLabel,
    entityId = patch.entityId ?: entityId,
    panelId = patch.panelId ?: panelId,
    icon = patch.icon ?: icon,
    accent = patch.accent ?: accent,
    order = patch.order ?: order,
    col = patch.col ?: col,
    row = patch.row ?: row,
    colSpan = patch.colSpan ?: colSpan,
    rowSpan = patch.rowSpan ?: rowSpan,
    clockStyle = patch.clockStyle ?: clockStyle,
    coverVisual = patch.coverVisual ?: coverVisual,
    coverDirection = patch.coverDirection ?: coverDirection,
    tapAction = patch.tapAction ?: tapAction,
    holdAction = patch.holdAction ?: holdAction,
    content = patch.content ?: content,
    summary = patch.summary ?: summary,
    secondary = patch.secondary ?: secondary,
    presentation = patch.presentation ?: presentation,
)

private fun List<HapanelsTileConfig>.applyPatches(
    updatesById: Map<String, HapanelsTilePatch>,
): List<HapanelsTileConfig> = map { tile ->
    updatesById[tile.id]?.let(tile::applyPatch) ?: tile
}
