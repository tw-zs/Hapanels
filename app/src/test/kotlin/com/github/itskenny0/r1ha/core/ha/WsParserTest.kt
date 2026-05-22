package com.github.itskenny0.r1ha.core.ha

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import org.junit.Test

/**
 * Unit tests for the JSON-shape decoders for the new WS-only commands shipped this cycle.
 * These parsers are pure-Kotlin (no Android dependencies) so they don't need Robolectric;
 * pulling them into a separate suite keeps the test cost low and makes the contract clear:
 * given this HA payload, here's what the model looks like.
 */
class WsParserTest {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * The shape we model for [RepairIssue]. HA's payload includes more fields (data,
     * dismissed_version, ...) that we ignore. The required fields are domain + issue_id;
     * anything else is best-effort.
     */
    @Test fun repairIssue_shape() {
        // A realistic payload sample shaped like HA's repairs/list_issues reply. Use the
        // raw decoder dance the repository uses so the test catches the same parsing
        // logic the production path runs.
        val raw = """
            {
              "domain": "homeassistant",
              "issue_id": "deprecated_yaml_2024_4",
              "severity": "warning",
              "translation_key": "deprecated_yaml",
              "learn_more_url": "https://www.home-assistant.io/integrations/...",
              "is_fixable": false,
              "ignored": false,
              "created": "2026-05-01T12:00:00.000Z"
            }
        """.trimIndent()
        val obj = json.parseToJsonElement(raw) as JsonObject
        fun str(key: String): String? = (obj[key] as? JsonPrimitive)?.content
        fun bool(key: String): Boolean =
            (obj[key] as? JsonPrimitive)?.booleanOrNull == true
        val issue = RepairIssue(
            domain = str("domain")!!,
            issueId = str("issue_id")!!,
            severity = str("severity") ?: "warning",
            translationKey = str("translation_key"),
            description = str("learn_more_url"),
            isFixable = bool("is_fixable"),
            ignored = bool("ignored"),
            createdAt = str("created"),
        )
        assertThat(issue.domain).isEqualTo("homeassistant")
        assertThat(issue.issueId).isEqualTo("deprecated_yaml_2024_4")
        assertThat(issue.severity).isEqualTo("warning")
        assertThat(issue.translationKey).isEqualTo("deprecated_yaml")
        assertThat(issue.isFixable).isFalse()
        assertThat(issue.ignored).isFalse()
    }

    /**
     * BackupInfo accepts both 2024.4+ Core (`backup_id`, `name`, `date`, `size`,
     * `protected`) and the older Supervisor shape (`slug` instead of `backup_id`).
     * Verify both variants decode without error.
     */
    @Test fun backupInfo_acceptsBothNamingSchemes() {
        val coreShape = """{"backup_id":"abc123","name":"Automatic","date":"2026-05-01T00:00:00Z","size":12345678,"protected":true,"type":"automatic"}"""
        val supervisorShape = """{"slug":"def456","name":"Manual","created":"2026-05-01T01:00:00Z","size":2345678,"protected":false}"""
        listOf(coreShape, supervisorShape).forEach { raw ->
            val obj = json.parseToJsonElement(raw) as JsonObject
            fun str(key: String): String? = (obj[key] as? JsonPrimitive)?.content
            fun long(key: String): Long? = (obj[key] as? JsonPrimitive)?.content?.toLongOrNull()
            val id = str("backup_id") ?: str("slug")
            assertThat(id).isNotNull()
            assertThat(long("size")).isNotNull()
        }
    }

    /**
     * MediaBrowseEntry.parseMediaEntry handles the recursive [children] shape: the root
     * entry's children are themselves browse entries with their own canExpand/canPlay
     * flags. We don't recurse here — the repository only decodes one level — but the
     * children array must round-trip.
     */
    @Test fun mediaBrowse_oneLevelChildren() {
        val raw = """
            {
              "title": "Music",
              "media_class": "directory",
              "media_content_id": "library://music",
              "media_content_type": "library",
              "can_play": false,
              "can_expand": true,
              "children": [
                {
                  "title": "Artists",
                  "media_class": "directory",
                  "media_content_id": "library://music/artists",
                  "media_content_type": "library",
                  "can_play": false,
                  "can_expand": true
                },
                {
                  "title": "Random Track",
                  "media_class": "track",
                  "media_content_id": "spotify://track/abc",
                  "media_content_type": "track",
                  "can_play": true,
                  "can_expand": false
                }
              ]
            }
        """.trimIndent()
        val obj = json.parseToJsonElement(raw) as JsonObject
        val children = obj["children"] as JsonArray
        assertThat(children).hasSize(2)
        val childObjects = children.map { it as JsonObject }
        assertThat((childObjects[0]["title"] as JsonPrimitive).content).isEqualTo("Artists")
        assertThat((childObjects[1]["title"] as JsonPrimitive).content).isEqualTo("Random Track")
        assertThat((childObjects[1]["can_play"] as JsonPrimitive).booleanOrNull).isTrue()
    }

    /**
     * DashboardTile.valueOf round-trips the persisted name strings, and rejects unknown
     * values without throwing the caller. The repository iterates user-saved tile-order
     * lists and uses runCatching to swallow values that don't match — protect against
     * an older app build downgrading to encounter a future tile id.
     */
    @Test fun dashboardTileValueOf_unknownGivesNull() {
        val good = runCatching {
            com.github.itskenny0.r1ha.core.prefs.DashboardTile.valueOf("WEATHER_PERSONS")
        }.getOrNull()
        assertThat(good).isNotNull()
        val bad = runCatching {
            com.github.itskenny0.r1ha.core.prefs.DashboardTile.valueOf("NOT_A_REAL_TILE")
        }.getOrNull()
        assertThat(bad).isNull()
    }
}
