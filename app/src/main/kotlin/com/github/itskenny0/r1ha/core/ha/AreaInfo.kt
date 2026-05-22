package com.github.itskenny0.r1ha.core.ha

/**
 * One entry from HA's `config/area_registry/list` reply. The registry tracks
 * the user's logical areas (kitchen, bedroom, garage, etc.) and is the
 * source-of-truth for entity-area assignment.
 *
 * HA's payload includes more fields (floor_id, aliases, icon, picture); we
 * carry only the two the picker UI actually uses. Future expansion would
 * append fields without breaking the existing call sites.
 */
data class AreaInfo(
    /** Stable server-assigned id, e.g. "kitchen". */
    val areaId: String,
    /** Human-friendly label, e.g. "Kitchen". */
    val name: String,
)
