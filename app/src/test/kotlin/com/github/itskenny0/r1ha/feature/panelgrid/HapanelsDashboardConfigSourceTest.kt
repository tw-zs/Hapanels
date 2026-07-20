package com.github.itskenny0.r1ha.feature.panelgrid

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HapanelsDashboardConfigSourceTest {
    @Test fun loadSeedsAndExportsSampleConfig() = runTest {
        val source = newSource()

        val config = source.loadOrSeed()
        val raw = source.exportRaw()

        assertThat(config.dashboardId).isEqualTo("home-panel-main")
        assertThat(raw).contains("home-panel-main")
        assertThat(raw).contains("Oświetlenie")
        assertThat(config.alwaysOnDisplay.tiles.first().kind).isEqualTo(HapanelsTileKind.CLOCK)
    }

    @Test fun importValidJsonReplacesCachedConfig() = runTest {
        val source = newSource()
        val raw = SAMPLE_HAPANELS_DASHBOARD_JSON.replace("Oświetlenie", "Test Lights")

        val imported = source.importRaw(raw)
        val loaded = source.loadOrSeed()

        assertThat(imported.tiles.first { it.id == "lights" }.label).isEqualTo("Test Lights")
        assertThat(loaded.tiles.first { it.id == "lights" }.label).isEqualTo("Test Lights")
    }

    @Test fun importPreservesLocalPanelAction() = runTest {
        val source = newSource()

        val tile = source.importRaw(SAMPLE_HAPANELS_DASHBOARD_JSON).tiles.first { it.id == "settings" }

        assertThat(tile.tapAction?.type).isEqualTo("navigate")
        assertThat(tile.tapAction?.destination).isEqualTo("settings")
    }

    @Test fun importInvalidJsonDoesNotReplaceCachedConfig() = runTest {
        val source = newSource()
        source.loadOrSeed()

        val result = runCatching { source.importRaw("not json") }
        val loaded = source.loadOrSeed()

        assertThat(result.isFailure).isTrue()
        assertThat(loaded.dashboardId).isEqualTo("home-panel-main")
    }

    @Test fun resetRestoresSampleConfig() = runTest {
        val source = newSource()
        source.importRaw(SAMPLE_HAPANELS_DASHBOARD_JSON.replace("Oświetlenie", "Test Lights"))

        val reset = source.resetToSample()

        assertThat(reset.tiles.first { it.id == "lights" }.label).isEqualTo("Oświetlenie")
    }

    @Test fun applyPatchUpdatesTilesAndIncrementsRevision() = runTest {
        val source = newSource()
        val current = source.loadOrSeed()

        val result = source.applyPatch(
            HapanelsDashboardPatch(
                baseRevision = current.revision,
                updatedBy = "hapanels:local_editor",
                tileUpdates = listOf(
                    HapanelsTilePatch(
                        id = "lights",
                        label = "Światła parter",
                        shortLabel = "Parter",
                        accent = HapanelsTileAccent.RED,
                    ),
                ),
            ),
        )
        val loaded = source.loadOrSeed()

        assertThat(result).isInstanceOf(HapanelsDashboardPatchResult.Applied::class.java)
        assertThat(loaded.revision).isEqualTo(current.revision + 1)
        assertThat(loaded.updatedBy).isEqualTo("hapanels:local_editor")
        assertThat(loaded.tiles.first { it.id == "lights" }.label).isEqualTo("Światła parter")
        assertThat(loaded.tiles.first { it.id == "lights" }.shortLabel).isEqualTo("Parter")
        assertThat(loaded.tiles.first { it.id == "lights" }.accent).isEqualTo(HapanelsTileAccent.RED)
    }

    @Test fun applyPatchPersistsPresetAndPreservesModeAodAndTiles() = runTest {
        val cacheFileName = "hapanels_dashboard_test_${System.nanoTime()}.json"
        val source = newSource(cacheFileName)
        val current = source.loadOrSeed()
        source.importRaw(
            configJsonForTest(
                current.copy(
                    theme = HapanelsThemeConfig(
                        preset = HapanelsThemePreset.DEFAULT,
                        mode = HapanelsThemeMode.SYSTEM,
                    ),
                ),
            ),
        )
        val before = source.loadOrSeed()

        val result = source.applyPatch(
            HapanelsDashboardPatch(
                baseRevision = before.revision,
                updatedBy = "hapanels:onboarding",
                theme = before.theme.copy(preset = HapanelsThemePreset.FOREST_LEAVES),
            ),
        )
        val loaded = newSource(cacheFileName).loadOrSeed()

        assertThat(result).isInstanceOf(HapanelsDashboardPatchResult.Applied::class.java)
        assertThat(loaded.theme.preset).isEqualTo(HapanelsThemePreset.FOREST_LEAVES)
        assertThat(loaded.theme.mode).isEqualTo(HapanelsThemeMode.SYSTEM)
        assertThat(loaded.alwaysOnDisplay).isEqualTo(before.alwaysOnDisplay)
        assertThat(loaded.tiles).isEqualTo(before.tiles)
        assertThat(loaded.revision).isEqualTo(before.revision + 1)
        assertThat(loaded.updatedBy).isEqualTo("hapanels:onboarding")
    }

    @Test fun applyPatchReportsConflictAndLeavesCacheUnchanged() = runTest {
        val source = newSource()
        val current = source.loadOrSeed()

        val result = source.applyPatch(
            HapanelsDashboardPatch(
                baseRevision = current.revision - 1,
                updatedBy = "hapanels:stale_editor",
                theme = current.theme.copy(preset = HapanelsThemePreset.NEON_NOIR),
                tileUpdates = listOf(HapanelsTilePatch(id = "lights", label = "Stale Lights")),
            ),
        )
        val loaded = source.loadOrSeed()

        assertThat(result).isEqualTo(
            HapanelsDashboardPatchResult.Conflict(
                currentRevision = current.revision,
                attemptedBaseRevision = current.revision - 1,
                currentConfig = current,
            ),
        )
        assertThat(loaded.tiles.first { it.id == "lights" }.label).isEqualTo("Oświetlenie")
        assertThat(loaded.theme).isEqualTo(current.theme)
        assertThat(loaded.revision).isEqualTo(current.revision)
    }

    @Test fun concurrentPatchesAllowOneAppliedAndOneConflict() = runTest {
        val cacheFileName = "hapanels_dashboard_test_${System.nanoTime()}.json"
        val source = newSource(cacheFileName)
        val otherSource = newSource(cacheFileName)
        val current = source.loadOrSeed()
        val results = listOf(
            async(Dispatchers.IO) {
                otherSource.applyPatch(
                    HapanelsDashboardPatch(
                        baseRevision = current.revision,
                        updatedBy = "hapanels:onboarding",
                        tileUpdates = listOf(HapanelsTilePatch(id = "lights", label = "Onboarding lights")),
                    ),
                )
            },
            async(Dispatchers.IO) {
                source.applyPatch(
                    HapanelsDashboardPatch(
                        baseRevision = current.revision,
                        updatedBy = "homeassistant:mqtt",
                        tileUpdates = listOf(HapanelsTilePatch(id = "energy", label = "MQTT energy")),
                    ),
                )
            },
        ).awaitAll()
        val applied = results.filterIsInstance<HapanelsDashboardPatchResult.Applied>().single()
        val conflict = results.filterIsInstance<HapanelsDashboardPatchResult.Conflict>().single()
        val loaded = source.loadOrSeed()

        assertThat(loaded).isEqualTo(applied.config)
        assertThat(conflict.currentConfig).isEqualTo(applied.config)
        assertThat(loaded.revision).isEqualTo(current.revision + 1)
        assertThat(
            (loaded.tiles.first { it.id == "lights" }.label == "Onboarding lights") xor
                (loaded.tiles.first { it.id == "energy" }.label == "MQTT energy"),
        ).isTrue()
    }

    @Test fun concurrentFullImportAndPatchAreSerializedAcrossSources() = runTest {
        val cacheFileName = "hapanels_dashboard_test_${System.nanoTime()}.json"
        val importSource = newSource(cacheFileName)
        val patchSource = newSource(cacheFileName)
        val current = importSource.loadOrSeed()
        val imported = current.copy(
            revision = current.revision + 10,
            updatedBy = "settings:full_import",
            tiles = current.tiles.map { tile ->
                if (tile.id == "lights") tile.copy(label = "Imported ".repeat(50_000)) else tile
            },
        )
        val start = CompletableDeferred<Unit>()

        val importedResult = async(Dispatchers.IO) {
            start.await()
            importSource.importRaw(configJsonForTest(imported))
        }
        val patchResult = async(Dispatchers.IO) {
            start.await()
            patchSource.applyPatch(
                HapanelsDashboardPatch(
                    baseRevision = current.revision,
                    updatedBy = "homeassistant:mqtt",
                    tileUpdates = listOf(HapanelsTilePatch(id = "energy", label = "Patched energy")),
                ),
            )
        }
        start.complete(Unit)

        assertThat(importedResult.await()).isEqualTo(imported)
        val patch = patchResult.await()
        val loaded = newSource(cacheFileName).loadOrSeed()
        assertThat(loaded).isEqualTo(imported)
        if (patch is HapanelsDashboardPatchResult.Conflict) {
            assertThat(patch.currentConfig).isEqualTo(imported)
        } else {
            assertThat(patch).isInstanceOf(HapanelsDashboardPatchResult.Applied::class.java)
        }
    }

    @Test fun concurrentResetAndPatchAreSerializedAcrossSources() = runTest {
        val cacheFileName = "hapanels_dashboard_test_${System.nanoTime()}.json"
        val resetSource = newSource(cacheFileName)
        val patchSource = newSource(cacheFileName)
        val seeded = resetSource.loadOrSeed()
        val current = seeded.copy(
            revision = seeded.revision + 10,
            tiles = seeded.tiles.map { tile ->
                if (tile.id == "lights") tile.copy(label = "Before reset ".repeat(50_000)) else tile
            },
        )
        resetSource.importRaw(configJsonForTest(current))
        val start = CompletableDeferred<Unit>()

        val resetResult = async(Dispatchers.IO) {
            start.await()
            resetSource.resetToSample()
        }
        val patchResult = async(Dispatchers.IO) {
            start.await()
            patchSource.applyPatch(
                HapanelsDashboardPatch(
                    baseRevision = current.revision,
                    updatedBy = "homeassistant:mqtt",
                    tileUpdates = listOf(HapanelsTilePatch(id = "energy", label = "Patched energy")),
                ),
            )
        }
        start.complete(Unit)

        val sample = resetResult.await()
        val patch = patchResult.await()
        val loaded = newSource(cacheFileName).loadOrSeed()
        assertThat(loaded).isEqualTo(sample)
        if (patch is HapanelsDashboardPatchResult.Conflict) {
            assertThat(patch.currentConfig).isEqualTo(sample)
        } else {
            assertThat(patch).isInstanceOf(HapanelsDashboardPatchResult.Applied::class.java)
        }
    }

    @Test fun applyPatchRawDecodesJsonPatch() = runTest {
        val source = newSource()
        val current = source.loadOrSeed()

        val result = source.applyPatchRaw(
            """
                {
                  "base_revision": ${current.revision},
                  "updated_by": "homeassistant:test_patch",
                  "tile_updates": [
                    { "id": "energy", "label": "Pobór", "order": 99 }
                  ]
                }
            """.trimIndent(),
        )
        val loaded = source.loadOrSeed()

        assertThat(result).isInstanceOf(HapanelsDashboardPatchResult.Applied::class.java)
        assertThat(loaded.tiles.first { it.id == "energy" }.label).isEqualTo("Pobór")
        assertThat(loaded.tiles.first { it.id == "energy" }.order).isEqualTo(99)
    }

    @Test fun syncStateJsonReportsSyncedAndConflict() = runTest {
        val source = newSource()
        val current = source.loadOrSeed()

        val synced = current.syncStateJson("synced")
        val conflict = current.syncStateJson("conflict", attemptedBaseRevision = current.revision - 1, currentRevision = current.revision)

        assertThat(synced).contains("\"status\":\"synced\"")
        assertThat(synced).contains("\"revision\":${current.revision}")
        assertThat(conflict).contains("\"status\":\"conflict\"")
        assertThat(conflict).contains("\"attempted_base_revision\":${current.revision - 1}")
        assertThat(conflict).contains("\"current_revision\":${current.revision}")
    }

    @Test fun applyPatchCanTargetAodTiles() = runTest {
        val source = newSource()
        val current = source.loadOrSeed()

        val result = source.applyPatch(
            HapanelsDashboardPatch(
                baseRevision = current.revision,
                updatedBy = "homeassistant:aod_editor",
                surface = HapanelsDashboardSurface.AOD,
                tileUpdates = listOf(
                    HapanelsTilePatch(
                        id = "aod_temperature",
                        label = "Na zewnątrz",
                        order = 9,
                    ),
                ),
            ),
        )
        val loaded = source.loadOrSeed()

        assertThat(result).isInstanceOf(HapanelsDashboardPatchResult.Applied::class.java)
        assertThat(loaded.revision).isEqualTo(current.revision + 1)
        assertThat(loaded.updatedBy).isEqualTo("homeassistant:aod_editor")
        assertThat(loaded.alwaysOnDisplay.tiles.first { it.id == "aod_temperature" }.label).isEqualTo("Na zewnątrz")
        assertThat(loaded.alwaysOnDisplay.tiles.first { it.id == "aod_temperature" }.order).isEqualTo(9)
        assertThat(loaded.tiles.first { it.id == "energy" }.label).isEqualTo("Energia")
    }

    @Test fun oldAodLayoutStringStillLoads() = runTest {
        val source = newSource()
        val oldRaw = SAMPLE_HAPANELS_DASHBOARD_JSON
            .replace("\"layout\": \"grid\",", "\"layout\": \"minimal_clock\",")
            .replace(Regex("\\n    \\\"grid_layout\\\": \\{[\\s\\S]*?\\n    \\},"), "")

        val imported = source.importRaw(oldRaw)

        assertThat(imported.alwaysOnDisplay.layout).isEqualTo(HapanelsAlwaysOnDisplayLayout.MINIMAL_CLOCK)
        assertThat(imported.alwaysOnDisplay.gridLayout.columnsLandscape).isEqualTo(3)
    }

    private fun newSource(
        cacheFileName: String = "hapanels_dashboard_test_${System.nanoTime()}.json",
    ): HapanelsDashboardConfigSource = HapanelsDashboardConfigSource(
        ApplicationProvider.getApplicationContext(),
        cacheFileName = cacheFileName,
    )

    private fun configJsonForTest(config: HapanelsDashboardConfig): String =
        kotlinx.serialization.json.Json.encodeToString(HapanelsDashboardConfig.serializer(), config)
}
