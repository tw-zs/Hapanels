package com.github.itskenny0.r1ha.feature.favoritespicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.github.itskenny0.r1ha.core.ha.Domain
import com.github.itskenny0.r1ha.core.ha.EntityState
import com.github.itskenny0.r1ha.core.ha.HaRepository
import com.github.itskenny0.r1ha.core.hardware.PanelHardware
import com.github.itskenny0.r1ha.core.prefs.SettingsRepository
import com.github.itskenny0.r1ha.core.util.R1Log
import com.github.itskenny0.r1ha.core.util.Toaster
import com.github.itskenny0.r1ha.feature.panelcontrols.PanelControlTile
import com.github.itskenny0.r1ha.feature.panelcontrols.availablePanelControlTiles
import com.github.itskenny0.r1ha.feature.panelcontrols.materializePanelControlTile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Filter chip — groups related domains into a single user-facing label. Picked so that the
 * chip row stays readable on a 240 px display (six or seven chips, not fifteen).
 */
enum class PickerFilter(val label: String, val matches: (Domain) -> Boolean) {
    ALL("WSZYSTKIE", { true }),
    FAVS("★ ULUBIONE", { true }),  // "isFavorite" filter applied outside `matches`; this entry is special-cased.
    PANEL("KONTROLA PANELU", { false }),
    LIGHTS("ŚWIATŁA", { it == Domain.LIGHT }),
    SWITCHES("PRZEŁĄCZNIKI", { it == Domain.SWITCH || it == Domain.INPUT_BOOLEAN || it == Domain.AUTOMATION }),
    COVERS("ROLETA/OSŁONY", { it == Domain.COVER }),
    // Valves get their own chip rather than living under COVERS — HA keeps the two
    // domains distinct (water valves vs window covers) and grouping them confused
    // discovery for users who knew they had a `valve.foo` entity but couldn't find it
    // by searching "valve".
    VALVES("ZAWORY", { it == Domain.VALVE }),
    CLIMATE("KLIMAT", { it == Domain.CLIMATE || it == Domain.HUMIDIFIER || it == Domain.FAN || it == Domain.WATER_HEATER }),
    LOCKS("ZAMKI", { it == Domain.LOCK }),
    MEDIA("MEDIA", { it == Domain.MEDIA_PLAYER }),
    // Action-only entities — scene/script/button/input_button. SCENES is the
    // human-friendly umbrella label even though it also covers scripts/buttons,
    // because that's the most-searched-for kind in this group.
    SCENES("SCENY", { it.isAction }),
    SENSORS("CZUJNIKI", { it.isSensor }),
    // Number / input_number — settable scalars common in MQTT integrations (pump
    // speeds, calibration knobs, manual setpoints). Previously hidden inside ALL
    // because no chip filtered for them.
    NUMBERS("LICZBY", { it == Domain.NUMBER || it == Domain.INPUT_NUMBER }),
    VACUUMS("ODKURZACZE", { it == Domain.VACUUM || it == Domain.LAWN_MOWER }),
    // Settable-enum entities — select / input_select. Useful for fan-mode selectors,
    // operating-mode pickers, room-target selectors for vacuums, etc.
    SELECTS("WYBORY", { it.isSelect }),
}

class FavoritesPickerViewModel(
    private val repo: HaRepository,
    private val settings: SettingsRepository,
    private val panelHardware: PanelHardware,
) : ViewModel() {

    @androidx.compose.runtime.Stable
    data class Row(
        val state: EntityState,
        val isFavorite: Boolean,
        val orderIndex: Int?,
        /** Display name after applying any client-side rename override; defaults to
         *  `state.friendlyName`. UI binds to this so the override appears live without
         *  the row composable needing to know about the override mechanism. */
        val displayName: String,
    )
    /**
     * Sort order applied within a filter tab. FAVS always sorts by orderIndex
     * regardless of this setting (the user is reasoning about card position
     * there, not alphabetical order). Other tabs default to ALPHA and the
     * user can flip into AREA (group physically) or DOMAIN (group by entity
     * type, useful when CONTROLLABLE / ALL chips are showing a heterogeneous
     * mix).
     */
    enum class SortOrder(val label: String) {
        ALPHA("A→Z"),
        AREA("WG POMIESZCZEŃ"),
        DOMAIN("WG TYPU"),
    }

    data class UiState(
        val loading: Boolean = true,
        val rows: List<Row> = emptyList(),
        val error: String? = null,
        val filter: PickerFilter = PickerFilter.ALL,
        /** Total counts per filter chip — surfaces a small number next to each chip so
         *  the user can see at a glance how many entities of each kind are available
         *  even before tapping the chip. */
        val countsByFilter: Map<PickerFilter, Int> = emptyMap(),
        /** Free-text search query — applied AFTER the filter chip. Case-insensitive
         *  substring match against display name + entity_id. */
        val query: String = "",
        /** Entity currently being renamed via the rename dialog, or null when no dialog
         *  is open. Picker observes this to show/hide the dialog overlay. */
        val editingEntityId: String? = null,
        /** Per-filter-tab sort selector. Survives navigation back to the picker
         *  but not process death — the picker is short-lived enough that DataStore
         *  persistence felt like over-engineering. FAVS tab ignores this. */
        val sortPerFilter: Map<PickerFilter, SortOrder> = emptyMap(),
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()
    /** Cached list of all controllable entities from the latest /api/states fetch. Toggling or
     *  reordering favourites doesn't change this list, so we can update [_ui] locally without
     *  re-fetching every time the user taps a checkbox. */
    private var entitiesCache: List<EntityState> = emptyList()

    init { refresh() }

    init {
        // Re-build rows whenever the rename-override map changes so the UI picks up a
        // freshly-saved rename even though we haven't refetched HA's entities. Same goes
        // for the favourites list — when the user un-favourites from CardStack, the
        // picker should reflect it. Subscribed for the VM lifetime; cheap because the
        // upstream Flow is distinctUntilChanged'd on the data we care about.
        viewModelScope.launch {
            // Subscribe to the active page's favourites — not the legacy global
            // [favorites]. The flat-union favourites field is still maintained but
            // a 'favourite' in picker terms means 'is in the currently-active page'.
            // Switching pages from the card stack will flow a new active-page id
            // here, the picker re-renders with that page's contents.
            combine(settings.settings, panelHardware.capabilities, panelHardware.runtimeState) { app, capabilities, runtime ->
                val active = app.pages.firstOrNull { p -> p.id == app.activePageId }
                val panelRows = availablePanelControlTiles(capabilities).mapNotNull { tile ->
                    materializePanelControlTile(tile, capabilities, runtime, app.advanced)
                }
                PanelPickerSnapshot(app.nameOverrides, active?.favorites.orEmpty(), panelRows)
            }
                .distinctUntilChanged()
                .collect { snapshot ->
                    if (entitiesCache.isNotEmpty()) {
                        val cur = _ui.value
                        _ui.value = cur.copy(
                            rows = buildRows(entitiesCache, snapshot.panelRows, snapshot.favs, cur.filter, cur.query, snapshot.overrides),
                            countsByFilter = countsByFilter(entitiesCache, snapshot.panelRows, snapshot.favs),
                        )
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            val snapshot = settings.settings.first()
            R1Log.i("FavoritesPicker.refresh", "server=${snapshot.server?.url ?: "null"} favoritesSoFar=${snapshot.favorites.size}")
            // 20s overall ceiling. OkHttp already enforces connect/read timeouts, but
            // a wedged DNS lookup or an HA that accepts the TCP connection and then
            // never responds can still pile multiple sub-timeouts on top of each
            // other. Capping the whole call guarantees the loading state can't
            // outlast the user's patience; on expiry we surface an error rather than
            // letting the picker sit on its spinner indefinitely.
            val all = kotlinx.coroutines.withTimeoutOrNull(20_000L) { repo.listAllEntities() }
                ?: Result.failure(java.util.concurrent.TimeoutException("Took longer than 20 s"))
            // Picker shows favourites of the ACTIVE page (other pages aren't visible
            // here; the user would switch pages on the card stack first). Falls back
            // to flat-union when there's no active page resolved (shouldn't happen
            // after migration, but defensive).
            val favs = snapshot.pages.firstOrNull { it.id == snapshot.activePageId }
                ?.favorites
                ?: snapshot.favorites
            val panelRows = currentPanelRows(snapshot)
            all.fold(
                onSuccess = { list ->
                    // Keep BOTH scalar-controllable and on/off-only entities — on/off ones
                    // render as a switch card on CardStack (wheel up/down flips them, tap
                    // toggles) rather than being hidden entirely.
                    entitiesCache = list
                    val cur = _ui.value
                    _ui.value = cur.copy(
                        loading = false,
                        rows = buildRows(list, panelRows, favs, cur.filter, cur.query, snapshot.nameOverrides),
                        countsByFilter = countsByFilter(list, panelRows, favs),
                    )
                    R1Log.i("FavoritesPicker.refresh", "fetched ${list.size} entities")
                },
                onFailure = {
                    R1Log.e("FavoritesPicker.refresh", "fetch failed", it)
                    Toaster.error("Fetch failed: ${it.message}")
                    _ui.value = UiState(loading = false, error = it.message)
                },
            )
        }
    }

    fun setQuery(q: String) {
        val cur = _ui.value
        if (cur.query == q) return
        // SYNC update of the query string so the search field's value parameter reflects
        // every keystroke immediately. Without this, the previous implementation hopped
        // through viewModelScope.launch → settings.first() before publishing the new
        // query, leaving BasicTextField recomposing with a one-step-old value. The IME's
        // composing region landed on a stale string and characters appeared transposed
        // ("testing" → "tetings"). Filtering work (which needs settings access) hops
        // async below; the visible text stays in lock-step with the user's keystrokes.
        _ui.value = cur.copy(query = q)
        viewModelScope.launch {
            val snapshot = settings.settings.first()
            val favs = snapshot.pages.firstOrNull { it.id == snapshot.activePageId }
                ?.favorites
                ?: snapshot.favorites
            val panelRows = currentPanelRows(snapshot)
            // Read the LATEST query and filter (not the captured `cur`) — by the time
            // this coroutine runs the user may have typed more characters, and we want
            // the result list to reflect that.
            val now = _ui.value
            _ui.value = now.copy(
                rows = buildRows(entitiesCache, panelRows, favs, now.filter, now.query, snapshot.nameOverrides),
            )
        }
    }

    fun startEditing(entityId: String) {
        _ui.value = _ui.value.copy(editingEntityId = entityId)
    }

    fun cancelEditing() {
        _ui.value = _ui.value.copy(editingEntityId = null)
    }

    /** Save the customize dialog — name + per-card override map. Blank [newName] removes
     *  the name override and restores HA's `friendly_name`; an override matching the
     *  default ([com.github.itskenny0.r1ha.core.prefs.EntityOverride.NONE]) is dropped
     *  from the map so a card the user "reset to defaults" doesn't keep an empty entry
     *  hanging around in preferences. */
    fun saveCustomize(
        entityId: String,
        newName: String,
        newOverride: com.github.itskenny0.r1ha.core.prefs.EntityOverride,
    ) {
        viewModelScope.launch {
            settings.update { cur ->
                val trimmed = newName.trim()
                val nextNames = cur.nameOverrides.toMutableMap()
                if (trimmed.isBlank()) nextNames.remove(entityId) else nextNames[entityId] = trimmed

                val nextOverrides = cur.entityOverrides.toMutableMap()
                if (newOverride == com.github.itskenny0.r1ha.core.prefs.EntityOverride.NONE) {
                    nextOverrides.remove(entityId)
                } else {
                    nextOverrides[entityId] = newOverride
                }

                cur.copy(nameOverrides = nextNames, entityOverrides = nextOverrides)
            }
            _ui.value = _ui.value.copy(editingEntityId = null)
        }
    }

    /** Switch the active filter chip. Re-evaluates [buildRows] against the cached entity
     *  set — no network refetch needed, just a local prune. */
    fun setFilter(filter: PickerFilter) {
        val cur = _ui.value
        if (cur.filter == filter) return
        viewModelScope.launch {
            val snapshot = settings.settings.first()
            val favs = snapshot.pages.firstOrNull { it.id == snapshot.activePageId }
                ?.favorites
                ?: snapshot.favorites
            val panelRows = currentPanelRows(snapshot)
            _ui.value = cur.copy(
                filter = filter,
                rows = buildRows(
                    entitiesCache, panelRows, favs, filter, cur.query, snapshot.nameOverrides,
                    sortOrder = cur.sortPerFilter[filter] ?: SortOrder.ALPHA,
                ),
            )
        }
    }

    /** Cycle the sort order for the active filter tab. Persists in [UiState]
     *  so re-entering this tab later in the same picker session restores it.
     *  FAVS ignores the setter — its sort is locked to orderIndex. */
    fun cycleSortOrder() {
        val cur = _ui.value
        if (cur.filter == PickerFilter.FAVS) return
        val now = cur.sortPerFilter[cur.filter] ?: SortOrder.ALPHA
        val next = when (now) {
            SortOrder.ALPHA -> SortOrder.AREA
            SortOrder.AREA -> SortOrder.DOMAIN
            SortOrder.DOMAIN -> SortOrder.ALPHA
        }
        viewModelScope.launch {
            val snapshot = settings.settings.first()
            val favs = snapshot.pages.firstOrNull { it.id == snapshot.activePageId }
                ?.favorites
                ?: snapshot.favorites
            val panelRows = currentPanelRows(snapshot)
            _ui.value = cur.copy(
                sortPerFilter = cur.sortPerFilter + (cur.filter to next),
                rows = buildRows(
                    entitiesCache, panelRows, favs, cur.filter, cur.query, snapshot.nameOverrides,
                    sortOrder = next,
                ),
            )
        }
    }

    /** Build the row list from cached entities + the current favourites list. Sorted by name
     *  rather than favourites-first; toggling a checkbox no longer reorders the list, which
     *  prevents the visible page from jumping when the user is selecting several entities
     *  back-to-back. The up/down arrows still mutate favourites order — visible in CardStack.
     *  Applies the active filter chip and (case-insensitive) the [query] substring match
     *  against both the *display* name (override or HA's friendly_name) and the entity_id —
     *  searching by entity_id is useful for HA users who know what they typed in their
     *  configuration but can't remember the friendly name. */
    private fun buildRows(
        entities: List<EntityState>,
        panelRows: List<EntityState>,
        favs: List<String>,
        filter: PickerFilter,
        query: String,
        overrides: Map<String, String>,
        sortOrder: SortOrder = SortOrder.ALPHA,
    ): List<Row> {
        val favOrder = favs.withIndex().associate { (idx, id) -> id to idx }
        val q = query.trim().lowercase()
        val haRows = entities
            .asSequence()
            .filter { ent ->
                when (filter) {
                    PickerFilter.ALL -> true
                    PickerFilter.FAVS -> ent.id.value in favOrder
                    PickerFilter.PANEL -> false
                    else -> filter.matches(ent.id.domain)
                }
            }
            .map { ent ->
                val display = overrides[ent.id.value] ?: ent.friendlyName
                Row(
                    state = ent,
                    isFavorite = ent.id.value in favOrder,
                    orderIndex = favOrder[ent.id.value],
                    displayName = display,
                )
            }
            .filter { row ->
                if (q.isEmpty()) true
                else row.displayName.lowercase().contains(q) ||
                    row.state.id.value.lowercase().contains(q)
            }
            .toList()
        val localRows = panelRows
            .asSequence()
            .mapNotNull { ent ->
                val tile = PanelControlTile.entries.firstOrNull { it.entityId == ent.id.value } ?: return@mapNotNull null
                val include = filter == PickerFilter.ALL ||
                    filter == PickerFilter.PANEL ||
                    (filter == PickerFilter.FAVS && tile.favoriteId in favOrder)
                if (!include) return@mapNotNull null
                Row(
                    state = ent,
                    isFavorite = tile.favoriteId in favOrder,
                    orderIndex = favOrder[tile.favoriteId],
                    displayName = ent.friendlyName,
                )
            }
            .filter { row ->
                if (q.isEmpty()) true
                else row.displayName.lowercase().contains(q) ||
                    row.state.id.value.lowercase().contains(q)
            }
            .toList()
        return (haRows + localRows)
            .let { rows ->
                // On the FAVS chip the user is reasoning about their card-stack order, so
                // sort by orderIndex — matches the order they'll see on the main screen
                // and is the order they're reordering via the move-up/down chevrons.
                // Every other view sorts alphabetically by display name (the usual case
                // when they're hunting for something to favourite).
                when {
                    filter == PickerFilter.FAVS ->
                        rows.sortedBy { it.orderIndex ?: Int.MAX_VALUE }
                    sortOrder == SortOrder.AREA ->
                        // Stable alphabetical-within-area: empty area sinks to the bottom.
                        rows.sortedWith(
                            compareBy<Row> { it.state.area?.lowercase() ?: "￿" }
                                .thenBy { it.displayName.lowercase() },
                        )
                    sortOrder == SortOrder.DOMAIN ->
                        rows.sortedWith(
                            compareBy<Row> { it.state.id.domain.name }
                                .thenBy { it.displayName.lowercase() },
                        )
                    else -> rows.sortedBy { it.displayName.lowercase() }
                }
            }
    }

    /** Tally how many entities match each filter — surfaces as a small badge on each chip
     *  so the user knows at a glance which filters are populated. Computed once per refresh
     *  rather than per-render so chip layout stays cheap. */
    private fun countsByFilter(entities: List<EntityState>, panelRows: List<EntityState>, favs: List<String>): Map<PickerFilter, Int> {
        val favSet = favs.toSet()
        return PickerFilter.entries.associateWith { f ->
            when (f) {
                PickerFilter.ALL -> entities.size + panelRows.size
                PickerFilter.FAVS -> entities.count { it.id.value in favSet } + panelRows.count { row ->
                    PanelControlTile.entries.any { it.entityId == row.id.value && it.favoriteId in favSet }
                }
                PickerFilter.PANEL -> panelRows.size
                else -> entities.count { f.matches(it.id.domain) }
            }
        }
    }

    fun toggle(entityId: String) {
        viewModelScope.launch {
            // Toggle the entity in the ACTIVE PAGE only — pre-tabs builds had a
            // single global favourites list; with tabs, add/remove operations are
            // scoped to whichever page the user has selected in the card stack.
            // updateActivePage handles the mutex + favourites-union recalculation.
            settings.updateActivePage { page ->
                val storedId = PanelControlTile.entries.firstOrNull { it.entityId == entityId }?.favoriteId ?: entityId
                val l = page.favorites.toMutableList()
                if (storedId in l) l.remove(storedId) else l.add(storedId)
                page.copy(favorites = l)
            }
            // Local re-render reads from the active page after the write completes.
            val snapshot = settings.settings.first()
            val newFavs = snapshot.pages.firstOrNull { it.id == snapshot.activePageId }?.favorites.orEmpty()
            val panelRows = currentPanelRows(snapshot)
            val cur = _ui.value
            _ui.value = cur.copy(
                rows = buildRows(entitiesCache, panelRows, newFavs, cur.filter, cur.query, snapshot.nameOverrides),
                countsByFilter = countsByFilter(entitiesCache, panelRows, newFavs),
            )
        }
    }

    fun moveUp(entityId: String) {
        viewModelScope.launch {
            settings.updateActivePage { page ->
                val storedId = PanelControlTile.entries.firstOrNull { it.entityId == entityId }?.favoriteId ?: entityId
                val l = page.favorites.toMutableList()
                val idx = l.indexOf(storedId)
                if (idx > 0) { l.removeAt(idx); l.add(idx - 1, storedId) }
                page.copy(favorites = l)
            }
            val snapshot = settings.settings.first()
            val newFavs = snapshot.pages.firstOrNull { it.id == snapshot.activePageId }?.favorites.orEmpty()
            val panelRows = currentPanelRows(snapshot)
            val cur = _ui.value
            _ui.value = cur.copy(rows = buildRows(entitiesCache, panelRows, newFavs, cur.filter, cur.query, snapshot.nameOverrides))
        }
    }

    /**
     * Absolute reorder — moves [entityId] from its current position to [toIndex].
     * Backs the drag-reorder gesture in the FAVS view; the up/down arrows still
     * use [moveUp] / [moveDown] for single-step nudges. Affects only the active
     * page's favourites list.
     */
    fun moveTo(entityId: String, toIndex: Int) {
        viewModelScope.launch {
            settings.updateActivePage { page ->
                val storedId = PanelControlTile.entries.firstOrNull { it.entityId == entityId }?.favoriteId ?: entityId
                val l = page.favorites.toMutableList()
                val fromIdx = l.indexOf(storedId)
                if (fromIdx < 0) return@updateActivePage page
                val clamped = toIndex.coerceIn(0, l.size - 1)
                if (fromIdx == clamped) return@updateActivePage page
                l.removeAt(fromIdx)
                l.add(clamped, storedId)
                page.copy(favorites = l)
            }
            val snapshot = settings.settings.first()
            val newFavs = snapshot.pages.firstOrNull { it.id == snapshot.activePageId }?.favorites.orEmpty()
            val panelRows = currentPanelRows(snapshot)
            val cur = _ui.value
            _ui.value = cur.copy(rows = buildRows(entitiesCache, panelRows, newFavs, cur.filter, cur.query, snapshot.nameOverrides))
        }
    }

    fun moveDown(entityId: String) {
        viewModelScope.launch {
            settings.updateActivePage { page ->
                val storedId = PanelControlTile.entries.firstOrNull { it.entityId == entityId }?.favoriteId ?: entityId
                val l = page.favorites.toMutableList()
                val idx = l.indexOf(storedId)
                if (idx in 0 until l.size - 1) { l.removeAt(idx); l.add(idx + 1, storedId) }
                page.copy(favorites = l)
            }
            val snapshot = settings.settings.first()
            val newFavs = snapshot.pages.firstOrNull { it.id == snapshot.activePageId }?.favorites.orEmpty()
            val panelRows = currentPanelRows(snapshot)
            val cur = _ui.value
            _ui.value = cur.copy(rows = buildRows(entitiesCache, panelRows, newFavs, cur.filter, cur.query, snapshot.nameOverrides))
        }
    }

    private fun currentPanelRows(settings: com.github.itskenny0.r1ha.core.prefs.AppSettings): List<EntityState> =
        availablePanelControlTiles(panelHardware.capabilities.value).mapNotNull { tile ->
            materializePanelControlTile(tile, panelHardware.capabilities.value, panelHardware.runtimeState.value, settings.advanced)
        }

    companion object {
        fun factory(
            repo: HaRepository,
            settings: SettingsRepository,
            panelHardware: PanelHardware,
        ) = viewModelFactory {
            initializer {
                FavoritesPickerViewModel(repo = repo, settings = settings, panelHardware = panelHardware)
            }
        }
    }
}

private data class PanelPickerSnapshot(
    val overrides: Map<String, String>,
    val favs: List<String>,
    val panelRows: List<EntityState>,
)
