package com.github.itskenny0.r1ha.feature.labels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.github.itskenny0.r1ha.core.ha.HaRepository
import com.github.itskenny0.r1ha.core.util.R1Log
import com.github.itskenny0.r1ha.core.util.Toaster
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Labels registry surface. Mirrors AreasViewModel's shape — labels are a
 * post-2024 HA primitive that lets users tag entities with arbitrary
 * categories ("daily routine", "needs batteries", "rec room AV") that cut
 * across the area axis. Surfacing them lets the user browse by label the
 * same way they browse by area.
 *
 * Driven via the `/api/template` endpoint:
 *
 *     {% for label in labels() %}
 *       label_name(label) → human label
 *       label_entities(label) → entities tagged with this label
 *
 * No WebSocket protocol additions needed.
 */
class LabelsViewModel(
    private val haRepository: HaRepository,
) : ViewModel() {

    @androidx.compose.runtime.Stable
    data class Label(
        val name: String,
        val entityIds: List<String>,
    )

    enum class Sort { ALPHA, COUNT }

    @androidx.compose.runtime.Stable
    data class UiState(
        val loading: Boolean = true,
        val labels: List<Label> = emptyList(),
        val error: String? = null,
        val sort: Sort = Sort.ALPHA,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    val sortedLabels: StateFlow<List<Label>> = _ui
        .map { s ->
            when (s.sort) {
                Sort.ALPHA -> s.labels.sortedBy { it.name.lowercase() }
                Sort.COUNT -> s.labels.sortedByDescending { it.entityIds.size }
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyList())

    fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            val tpl = """
                {%- set out = namespace(items=[]) -%}
                {%- for label in labels() -%}
                  {%- set _ = out.items.append({"id": label, "name": label_name(label), "entities": label_entities(label)}) -%}
                {%- endfor -%}
                {{ out.items | tojson }}
            """.trimIndent()
            haRepository.renderTemplate(tpl).fold(
                onSuccess = { rendered ->
                    runCatching {
                        val root = Json.parseToJsonElement(rendered)
                        val arr = root as? JsonArray
                            ?: error("Unexpected template response shape. Not an array")
                        val list = arr.mapNotNull { el ->
                            val obj = el as? JsonObject ?: return@mapNotNull null
                            val name = (obj["name"] as? JsonPrimitive)?.content
                                ?: (obj["id"] as? JsonPrimitive)?.content
                                ?: return@mapNotNull null
                            val entitiesArr = obj["entities"] as? JsonArray
                            val entities = entitiesArr?.mapNotNull {
                                (it as? JsonPrimitive)?.content
                            }.orEmpty()
                            Label(name = name, entityIds = entities)
                        }
                        R1Log.i("Labels", "loaded ${list.size}")
                        _ui.value = _ui.value.copy(loading = false, labels = list, error = null)
                    }.onFailure { t ->
                        R1Log.w("Labels", "parse failed: ${t.message}")
                        Toaster.error("Labels parse failed. Try Templates to debug")
                        _ui.value = _ui.value.copy(loading = false, error = t.message)
                    }
                },
                onFailure = { t ->
                    R1Log.w("Labels", "fetch failed: ${t.message}")
                    Toaster.error("Labels load failed: ${t.message ?: "unknown"}")
                    _ui.value = _ui.value.copy(loading = false, error = t.message)
                },
            )
        }
    }

    fun setSort(s: Sort) {
        if (_ui.value.sort == s) return
        _ui.value = _ui.value.copy(sort = s)
    }

    companion object {
        fun factory(haRepository: HaRepository) = viewModelFactory {
            initializer { LabelsViewModel(haRepository) }
        }
    }
}
