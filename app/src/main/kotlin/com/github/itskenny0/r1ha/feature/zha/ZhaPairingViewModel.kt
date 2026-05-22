package com.github.itskenny0.r1ha.feature.zha

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.github.itskenny0.r1ha.core.ha.HaRepository
import com.github.itskenny0.r1ha.core.util.R1Log
import com.github.itskenny0.r1ha.core.util.Toaster
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Drives the Zigbee pairing surface. HA exposes pairing through whichever Zigbee
 * integration the user installed:
 *  - ZHA (built-in) — fires `zha.permit { duration: N }`
 *  - Zigbee2MQTT (community) — publishes `{ value: true, time: N }` to the
 *    `zigbee2mqtt/bridge/request/permit_join` MQTT topic via `mqtt.publish`
 *  - deCONZ — fires `deconz.configure { field: "/config/permitjoin", data: N, ...}`
 *
 * The viewmodel detects which integration is installed via HA's components list
 * and picks the right service. If multiple are present (some users run ZHA + Z2M
 * in parallel) we surface both as selectable.
 *
 * After firing permit, a countdown timer ticks down to zero. During the window
 * (plus a 60 s discovery grace after, because new devices can take time to enrol
 * after physical join), we poll /api/states and surface entity_ids that weren't
 * in the baseline — that's how newly-paired devices show up to the user.
 */
class ZhaPairingViewModel(
    private val haRepository: HaRepository,
) : ViewModel() {

    enum class Backend(val label: String, val component: String) {
        ZHA("ZHA", "zha"),
        ZIGBEE2MQTT("Zigbee2MQTT", "mqtt"),
        DECONZ("deCONZ", "deconz"),
    }

    @androidx.compose.runtime.Stable
    data class UiState(
        val loading: Boolean = true,
        /** Integrations HA has installed — the detected subset of [Backend]. */
        val available: List<Backend> = emptyList(),
        /** Currently selected backend; first available by default. */
        val selected: Backend? = null,
        /** Permit duration in seconds. HA allows 1..255; default 60 s. */
        val durationSec: Int = 60,
        /** Permit window remaining (seconds). 0 = closed. */
        val remainingSec: Int = 0,
        /** Entity ids present at the moment permit was fired — diffed against
         *  the latest /api/states to surface newly-added devices. */
        val baselineEntityIds: Set<String> = emptySet(),
        /** Entity ids that appeared after permit was fired. */
        val newEntityIds: List<String> = emptyList(),
        val error: String? = null,
        val info: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    private var countdownJob: Job? = null
    private var pollJob: Job? = null

    /** Detect which Zigbee integrations HA has installed. Falls through to
     *  showing the user a friendly "no Zigbee integration detected" empty
     *  state when none of the known backends are present. */
    fun detect() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null, info = null)
            haRepository.fetchHaConfig().fold(
                onSuccess = { cfg ->
                    val present = Backend.entries.filter { it.component in cfg.components }
                    _ui.value = _ui.value.copy(
                        loading = false,
                        available = present,
                        selected = present.firstOrNull(),
                    )
                    R1Log.i("ZhaPairing", "detected backends: ${present.map { it.label }}")
                },
                onFailure = { t ->
                    R1Log.w("ZhaPairing", "config fetch failed: ${t.message}")
                    _ui.value = _ui.value.copy(
                        loading = false,
                        error = "Couldn't reach HA: ${t.message ?: "unknown"}",
                    )
                },
            )
        }
    }

    fun setBackend(backend: Backend) {
        if (backend !in _ui.value.available) return
        _ui.value = _ui.value.copy(selected = backend)
    }

    fun setDuration(sec: Int) {
        _ui.value = _ui.value.copy(durationSec = sec.coerceIn(1, 254))
    }

    fun permitJoin() {
        val state = _ui.value
        val backend = state.selected ?: run {
            Toaster.error("No Zigbee integration available")
            return
        }
        if (state.remainingSec > 0) {
            Toaster.show("Permit window already open")
            return
        }
        viewModelScope.launch {
            // Snapshot the current entity registry as the baseline so we can
            // diff against it for the rest of the permit + discovery window.
            val baselineResult = haRepository.listAllEntities()
            val baselineIds = baselineResult.getOrNull()?.map { it.id.value }?.toSet().orEmpty()

            val outcome = when (backend) {
                Backend.ZHA -> haRepository.callRawService(
                    domain = "zha",
                    service = "permit",
                    data = buildJsonObject { put("duration", JsonPrimitive(state.durationSec)) },
                )
                Backend.ZIGBEE2MQTT -> haRepository.callRawService(
                    domain = "mqtt",
                    service = "publish",
                    data = buildJsonObject {
                        put("topic", JsonPrimitive("zigbee2mqtt/bridge/request/permit_join"))
                        put(
                            "payload",
                            JsonPrimitive(
                                """{"value":true,"time":${state.durationSec}}""",
                            ),
                        )
                    },
                )
                Backend.DECONZ -> haRepository.callRawService(
                    domain = "deconz",
                    service = "configure",
                    data = buildJsonObject {
                        put("field", JsonPrimitive("/config"))
                        put(
                            "data",
                            buildJsonObject {
                                put("permitjoin", JsonPrimitive(state.durationSec))
                            },
                        )
                    },
                )
            }

            outcome.fold(
                onSuccess = {
                    _ui.value = _ui.value.copy(
                        baselineEntityIds = baselineIds,
                        newEntityIds = emptyList(),
                        remainingSec = state.durationSec,
                        info = "Pairing mode is open. Power on your device now and put it in pairing mode.",
                        error = null,
                    )
                    Toaster.show("Pairing window open for ${state.durationSec} s")
                    startCountdown()
                    startNewDevicePoll(graceSec = 60)
                },
                onFailure = { t ->
                    R1Log.w("ZhaPairing", "permit failed: ${t.message}")
                    _ui.value = _ui.value.copy(error = t.message ?: "Permit failed")
                    Toaster.error(t.message ?: "Permit failed")
                },
            )
        }
    }

    /** Cancel the permit window early by firing duration=0. HA's ZHA permit
     *  treats 0 as "close now"; Z2M honours it via the same topic. Best-effort:
     *  if the cancel service errors we still tear down our local countdown. */
    fun cancelPermit() {
        val backend = _ui.value.selected ?: return
        viewModelScope.launch {
            runCatching {
                when (backend) {
                    Backend.ZHA -> haRepository.callRawService(
                        "zha", "permit",
                        buildJsonObject { put("duration", JsonPrimitive(0)) },
                    )
                    Backend.ZIGBEE2MQTT -> haRepository.callRawService(
                        "mqtt", "publish",
                        buildJsonObject {
                            put("topic", JsonPrimitive("zigbee2mqtt/bridge/request/permit_join"))
                            put("payload", JsonPrimitive("""{"value":false}"""))
                        },
                    )
                    Backend.DECONZ -> haRepository.callRawService(
                        "deconz", "configure",
                        buildJsonObject {
                            put("field", JsonPrimitive("/config"))
                            put("data", buildJsonObject { put("permitjoin", JsonPrimitive(0)) })
                        },
                    )
                }
            }
            countdownJob?.cancel()
            countdownJob = null
            _ui.value = _ui.value.copy(remainingSec = 0)
            Toaster.show("Pairing window closed")
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive && _ui.value.remainingSec > 0) {
                delay(1_000L)
                _ui.value = _ui.value.copy(remainingSec = (_ui.value.remainingSec - 1).coerceAtLeast(0))
            }
        }
    }

    /**
     * Poll /api/states every 4 s during the permit window + a grace period. Each
     * tick re-fetches the entity list and diffs against the baseline; any new
     * entity_ids show up in [UiState.newEntityIds]. Polls stop when the permit
     * window closes AND the grace period elapses.
     */
    private fun startNewDevicePoll(graceSec: Int) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            var graceRemaining = graceSec
            while (isActive) {
                val state = _ui.value
                if (state.remainingSec <= 0) {
                    if (graceRemaining <= 0) break
                    graceRemaining -= 4
                }
                val result = haRepository.listAllEntities().getOrNull() ?: emptyList()
                val current = result.map { it.id.value }.toSet()
                val added = (current - state.baselineEntityIds).toList().sorted()
                if (added != state.newEntityIds) {
                    _ui.value = _ui.value.copy(newEntityIds = added)
                    if (added.isNotEmpty()) {
                        R1Log.i("ZhaPairing", "spotted ${added.size} new entity ids")
                    }
                }
                delay(4_000L)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        pollJob?.cancel()
    }

    companion object {
        fun factory(haRepository: HaRepository) = viewModelFactory {
            initializer { ZhaPairingViewModel(haRepository) }
        }
    }
}
