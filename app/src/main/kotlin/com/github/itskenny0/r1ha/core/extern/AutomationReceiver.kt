package com.github.itskenny0.r1ha.core.extern

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.itskenny0.r1ha.App
import com.github.itskenny0.r1ha.core.ha.Domain
import com.github.itskenny0.r1ha.core.ha.EntityId
import com.github.itskenny0.r1ha.core.ha.ServiceCall
import com.github.itskenny0.r1ha.core.util.R1Log
import com.github.itskenny0.r1ha.core.util.Toaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Broadcast receiver that lets third-party automation apps (Tasker, MacroDroid,
 * Automate, IFTTT-via-shortcut, etc.) fire Home Assistant service calls through
 * this app's existing WS connection + auth.
 *
 * Why this exists: the alternative path for "Tasker turns my hallway light on
 * at sunset" is for each automation app to embed its own HA client (URL, token,
 * TLS pinning, OAuth refresh, ...). This receiver lets them piggyback on our
 * already-configured client by sending one parameterised intent.
 *
 * Intent contract:
 *  - action: `com.github.itskenny0.r1ha.action.HA_SERVICE_CALL`
 *  - `ha_domain`: required string, e.g. "light", "script", "homeassistant"
 *  - `ha_service`: required string, e.g. "turn_on", "toggle", "press"
 *  - `ha_entity_id`: optional string, e.g. "light.kitchen" — services that
 *    don't target a single entity (homeassistant.restart, automation.reload)
 *    omit this.
 *  - `ha_data_json`: optional string with a JSON object payload for the
 *    service_data field (e.g. `{"brightness_pct":50}`).
 *
 * Safety: gated by the
 * [com.github.itskenny0.r1ha.core.prefs.AdvancedSettings.externalAutomationEnabled]
 * toggle. When off (the default), every intent is silently ignored — so a
 * fresh install can't be hijacked by an installed app firing the action.
 * The toggle lives in Advanced rather than the main Settings tree because
 * this is a power-user feature; surfacing it on the main path would
 * confuse the casual user with a dangerous-sounding option.
 */
class AutomationReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_HA_SERVICE_CALL) return
        val app = context.applicationContext as? App ?: run {
            R1Log.w("AutomationReceiver", "context is not App; ignoring")
            return
        }
        // Settings flow is suspend-only; use a one-shot read via the
        // already-running graph. Pending-result keeps the receiver alive long
        // enough for the IO dispatch to land.
        val pending = goAsync()
        scope.launch {
            try {
                val current = app.graph.settings.settings.first()
                if (!current.advanced.externalAutomationEnabled) {
                    R1Log.w(
                        "AutomationReceiver",
                        "ignoring HA_SERVICE_CALL — externalAutomationEnabled is off",
                    )
                    return@launch
                }
                val domain = intent.getStringExtra(EXTRA_DOMAIN).orEmpty()
                val service = intent.getStringExtra(EXTRA_SERVICE).orEmpty()
                val entityId = intent.getStringExtra(EXTRA_ENTITY_ID).orEmpty()
                val dataJson = intent.getStringExtra(EXTRA_DATA_JSON).orEmpty()
                if (domain.isBlank() || service.isBlank()) {
                    R1Log.w(
                        "AutomationReceiver",
                        "missing ha_domain or ha_service; ignoring",
                    )
                    Toaster.error("External automation intent missing ha_domain/ha_service")
                    return@launch
                }
                val data = if (dataJson.isBlank()) JsonObject(emptyMap()) else {
                    runCatching { Json.parseToJsonElement(dataJson) as JsonObject }
                        .getOrElse {
                            R1Log.w(
                                "AutomationReceiver",
                                "ha_data_json doesn't parse: ${it.message}",
                            )
                            Toaster.error("External automation: ha_data_json invalid")
                            return@launch
                        }
                }
                val result = if (entityId.isBlank()) {
                    app.graph.haRepository.callRawService(domain, service, data)
                        .map { Unit }
                } else {
                    // EntityId's primary constructor throws on malformed input;
                    // wrap in runCatching so bad ids fail fast with a clear log
                    // line rather than crashing the receiver thread.
                    val id = runCatching { EntityId(entityId) }.getOrNull() ?: run {
                        R1Log.w("AutomationReceiver", "ha_entity_id malformed: $entityId")
                        Toaster.error("External automation: ha_entity_id malformed")
                        return@launch
                    }
                    // The ServiceCall.tapAction-style helpers know about
                    // brightness/percent for specific domains; the raw
                    // (domain, service, data) constructor is the right level
                    // here because the caller has already picked the service.
                    app.graph.haRepository.call(
                        ServiceCall(target = id, service = service, data = data),
                    )
                }
                result.fold(
                    onSuccess = {
                        R1Log.i(
                            "AutomationReceiver",
                            "fired $domain.$service entity=$entityId",
                        )
                    },
                    onFailure = { t ->
                        R1Log.w(
                            "AutomationReceiver",
                            "$domain.$service entity=$entityId failed: ${t.message}",
                        )
                    },
                )
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_HA_SERVICE_CALL = "com.github.itskenny0.r1ha.action.HA_SERVICE_CALL"
        const val EXTRA_DOMAIN = "ha_domain"
        const val EXTRA_SERVICE = "ha_service"
        const val EXTRA_ENTITY_ID = "ha_entity_id"
        const val EXTRA_DATA_JSON = "ha_data_json"
    }
}
