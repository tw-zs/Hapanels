package com.github.itskenny0.r1ha.feature.search

import com.github.itskenny0.r1ha.core.ha.Domain
import com.github.itskenny0.r1ha.core.ha.EntityState

/**
 * Pure search filter + sort, extracted from SearchViewModel so it can be unit-tested
 * (and stress-tested) without spinning up a full ViewModel + repository graph.
 *
 *  - Substring match on friendlyName / entity_id / area (case-insensitive).
 *  - Bucket filter pinned to a caller-supplied (Domain → Bucket) mapper so this stays
 *    independent of the ViewModel's bucketOf instance method.
 *  - Sort by "prefix-match first, then friendlyName" so a query that exactly opens a
 *    name floats it to the top.
 *  - Caps the result list to `resultCap` so a thousand-match query returns predictably.
 */
internal object SearchRanker {
    fun filter(
        all: List<EntityState>,
        query: String,
        bucket: SearchViewModel.Bucket,
        bucketOf: (Domain) -> SearchViewModel.Bucket,
        resultCap: Int,
    ): List<EntityState> {
        val q = query.trim().lowercase()
        if (q.isBlank() && bucket == SearchViewModel.Bucket.ALL) return emptyList()
        return all.asSequence().filter { e ->
            val matchesQuery = if (q.isBlank()) true else (
                e.friendlyName.lowercase().contains(q) ||
                    e.id.value.lowercase().contains(q) ||
                    (e.area?.lowercase()?.contains(q) ?: false)
                )
            val matchesBucket = bucket == SearchViewModel.Bucket.ALL || bucketOf(e.id.domain) == bucket
            matchesQuery && matchesBucket
        }
            .sortedWith(
                compareByDescending<EntityState> {
                    q.isNotBlank() && (
                        it.friendlyName.lowercase().startsWith(q) ||
                            it.id.value.lowercase().substringAfter('.').startsWith(q)
                        )
                }.thenBy { it.friendlyName.lowercase() },
            )
            .take(resultCap)
            .toList()
    }
}
