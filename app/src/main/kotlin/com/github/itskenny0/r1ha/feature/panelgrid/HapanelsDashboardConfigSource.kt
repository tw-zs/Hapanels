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
) {
    suspend fun loadOrSeed(): HapanelsDashboardConfig = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, DASHBOARD_CACHE_FILE)
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
}
