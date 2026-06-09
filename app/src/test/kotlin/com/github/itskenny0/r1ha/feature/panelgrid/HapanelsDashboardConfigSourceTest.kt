package com.github.itskenny0.r1ha.feature.panelgrid

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
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
    }

    @Test fun importValidJsonReplacesCachedConfig() = runTest {
        val source = newSource()
        val raw = SAMPLE_HAPANELS_DASHBOARD_JSON.replace("Oświetlenie", "Test Lights")

        val imported = source.importRaw(raw)
        val loaded = source.loadOrSeed()

        assertThat(imported.tiles.first { it.id == "lights" }.label).isEqualTo("Test Lights")
        assertThat(loaded.tiles.first { it.id == "lights" }.label).isEqualTo("Test Lights")
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

    @Test fun applyPatchReportsConflictAndLeavesCacheUnchanged() = runTest {
        val source = newSource()
        val current = source.loadOrSeed()

        val result = source.applyPatch(
            HapanelsDashboardPatch(
                baseRevision = current.revision - 1,
                updatedBy = "hapanels:stale_editor",
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
        assertThat(loaded.revision).isEqualTo(current.revision)
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

    private fun newSource(): HapanelsDashboardConfigSource = HapanelsDashboardConfigSource(
        ApplicationProvider.getApplicationContext(),
        cacheFileName = "hapanels_dashboard_test_${System.nanoTime()}.json",
    )
}
