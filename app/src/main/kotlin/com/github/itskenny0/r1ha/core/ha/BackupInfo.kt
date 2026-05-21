package com.github.itskenny0.r1ha.core.ha

/**
 * One entry from HA's `backup/info` WS reply. HA 2024.4+ exposes a structured backup
 * list via the WS protocol; the same blob serves both Core (no supervisor) and HAOS
 * installs, with extra fields surfacing only when the supervisor is present.
 *
 * The full payload includes more (mainfest_id, location addons, paths, encryption
 * details). This data class captures what the R1's narrow viewport needs: a label,
 * a timestamp, a size, and an indicator of whether the backup is protected by a
 * passphrase. The full blob stays in the repository response so a future detail
 * surface can crack it open further.
 */
data class BackupInfo(
    /** Stable slug HA uses to identify the backup across delete / restore calls. */
    val backupId: String,
    /** Human-friendly name set when the backup was created. */
    val name: String,
    /** ISO-8601 creation timestamp. */
    val createdAt: String?,
    /** Backup size in bytes. Null when HA didn't include it (some older releases). */
    val sizeBytes: Long?,
    /** When true, restoring requires the passphrase set at creation time. */
    val protected: Boolean,
    /** Backup type — "manual" (user-initiated) or "automatic" (scheduled by the supervisor). */
    val type: String?,
)
