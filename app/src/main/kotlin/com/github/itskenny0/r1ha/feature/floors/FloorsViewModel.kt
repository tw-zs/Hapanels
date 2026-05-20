package com.github.itskenny0.r1ha.feature.floors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.github.itskenny0.r1ha.core.ha.HaRepository
import com.github.itskenny0.r1ha.core.util.R1Log
import com.github.itskenny0.r1ha.core.util.Toaster
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Floors registry surface. HA's floor primitive groups areas — a "main
 * floor" might contain Kitchen / Living Room / Office; "basement" might
 * contain Garage / Laundry. Each floor lists its constituent areas with
 * the entity count rolled up per area so the user can see "which floor
 * has the most going on".
 *
 * Driven by the `/api/template` endpoint:
 *
 *     {% for floor in floors() %}
 *       floor_name(floor) → human label
 *       floor_areas(floor) → list of area_ids
 *       area_entities(area_id) → entities in each constituent area
 */
class FloorsViewModel(
    private val haRepository: HaRepository,
) : ViewModel() {

    @androidx.compose.runtime.Stable
    data class AreaInFloor(
        val name: String,
        val entityCount: Int,
    )

    @androidx.compose.runtime.Stable
    data class Floor(
        val name: String,
        val areas: List<AreaInFloor>,
    )

    @androidx.compose.runtime.Stable
    data class UiState(
        val loading: Boolean = true,
        val floors: List<Floor> = emptyList(),
        val error: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            // Single template pass: for each floor, name + per-area entity
            // count. Avoids the N+1 round trip we'd otherwise pay.
            val tpl = """
                {%- set out = namespace(items=[]) -%}
                {%- for floor in floors() -%}
                  {%- set areas_list = namespace(items=[]) -%}
                  {%- for a in floor_areas(floor) -%}
                    {%- set _ = areas_list.items.append({"name": area_name(a), "count": area_entities(a) | length}) -%}
                  {%- endfor -%}
                  {%- set _ = out.items.append({"id": floor, "name": floor_name(floor), "areas": areas_list.items}) -%}
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
                            val areasArr = obj["areas"] as? JsonArray
                            val areas = areasArr?.mapNotNull { a ->
                                val aObj = a as? JsonObject ?: return@mapNotNull null
                                val aName = (aObj["name"] as? JsonPrimitive)?.content ?: return@mapNotNull null
                                val cnt = (aObj["count"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                                AreaInFloor(name = aName, entityCount = cnt)
                            }.orEmpty()
                            Floor(name = name, areas = areas.sortedBy { it.name.lowercase() })
                        }.sortedBy { it.name.lowercase() }
                        R1Log.i("Floors", "loaded ${list.size}")
                        _ui.value = _ui.value.copy(loading = false, floors = list, error = null)
                    }.onFailure { t ->
                        R1Log.w("Floors", "parse failed: ${t.message}")
                        Toaster.error("Floors parse failed. Try Templates to debug")
                        _ui.value = _ui.value.copy(loading = false, error = t.message)
                    }
                },
                onFailure = { t ->
                    R1Log.w("Floors", "fetch failed: ${t.message}")
                    Toaster.error("Floors load failed: ${t.message ?: "unknown"}")
                    _ui.value = _ui.value.copy(loading = false, error = t.message)
                },
            )
        }
    }

    companion object {
        fun factory(haRepository: HaRepository) = viewModelFactory {
            initializer { FloorsViewModel(haRepository) }
        }
    }
}
