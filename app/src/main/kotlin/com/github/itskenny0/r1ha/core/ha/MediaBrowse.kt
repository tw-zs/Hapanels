package com.github.itskenny0.r1ha.core.ha

/**
 * One entry returned by HA's `media_player/browse_media` WS command. The full
 * payload is a recursive structure — the root entry carries metadata for the
 * current folder and a [children] list of [MediaBrowseResult] entries one
 * level down. Tapping an entry whose [canExpand] is true drills deeper;
 * tapping one whose [canPlay] is true fires `media_player.play_media` against
 * the bound player.
 *
 * HA's payload includes more fields (thumbnail URL, child types, etc.) that
 * we don't surface in narrow panel viewports — the title + a play/folder
 * affordance is the at-a-glance shape the screen needs.
 */
data class MediaBrowseEntry(
    val title: String,
    val mediaClass: String?,
    val mediaContentId: String,
    val mediaContentType: String,
    val canPlay: Boolean,
    val canExpand: Boolean,
    val thumbnail: String?,
)

/** Result of one browse step: the breadcrumb for the current folder (title,
 *  ids) plus its child entries. */
data class MediaBrowseResult(
    val current: MediaBrowseEntry,
    val children: List<MediaBrowseEntry>,
)
