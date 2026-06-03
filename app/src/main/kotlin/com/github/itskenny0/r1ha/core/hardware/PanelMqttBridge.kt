package com.github.itskenny0.r1ha.core.hardware

import android.os.Build
import com.github.itskenny0.r1ha.core.mqtt.MqttPublisher
import com.github.itskenny0.r1ha.core.prefs.AppSettings
import com.github.itskenny0.r1ha.core.prefs.SettingsRepository
import com.github.itskenny0.r1ha.core.util.R1Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PanelMqttBridge(
    private val settings: SettingsRepository,
    private val hardware: PanelHardware,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val objectId = "hapanels_${Build.DEVICE.ifBlank { "panel" }.safeId()}"
    private val baseTopic = "hapanels/${Build.DEVICE.ifBlank { "panel" }.safeId()}"
    private var discoverySignature: Pair<Int, Int>? = null

    fun start() {
        scope.launch {
            hardware.capabilities
                .collect { capabilities ->
                    val signature = capabilities.relayCount to capabilities.physicalButtonCount
                    if (signature != discoverySignature) {
                        discoverySignature = signature
                        publishDiscovery(capabilities)
                        publishAvailability(true)
                    }
                }
        }
        scope.launch {
            hardware.runtimeState.collect { runtime ->
                runtime.relayStates.forEach { (id, on) ->
                    if (on != null) publish("$baseTopic/relay/$id/state", if (on) "ON" else "OFF", retain = true)
                }
                (1..hardware.capabilities.value.physicalButtonCount).forEach { id ->
                    publish("$baseTopic/button/$id/state", if (id in runtime.pressedButtonIds) "ON" else "OFF", retain = false)
                }
                runtime.screenBrightnessPercent?.let { publish("$baseTopic/screen/brightness/state", it.toString(), retain = true) }
                runtime.ambientLightLux?.let { publish("$baseTopic/sensor/ambient_light/state", it.toString(), retain = true) }
                runtime.proximityDistanceCm?.let { publish("$baseTopic/sensor/proximity/state", it.toString(), retain = true) }
            }
        }
        scope.launch {
            hardware.events.collect { event ->
                when (event) {
                    is PanelHardwareEvent.Button -> {
                        publish("$baseTopic/button/${event.buttonId}/event", event.type.name.lowercase(), retain = false)
                    }
                    is PanelHardwareEvent.Relay -> {
                        publish("$baseTopic/relay/${event.relayId}/state", if (event.on) "ON" else "OFF", retain = true)
                    }
                    is PanelHardwareEvent.Lifecycle,
                    is PanelHardwareEvent.UnsupportedAction -> Unit
                }
            }
        }
    }

    private suspend fun publishDiscovery(capabilities: PanelCapabilities) {
        publish("$baseTopic/status", "online", retain = true)
        for (relayId in 1..capabilities.relayCount) {
            discovery(
                component = "switch",
                entityId = "relay_$relayId",
                payload = """
                    {
                      "name":"Relay $relayId",
                      "unique_id":"${objectId}_relay_$relayId",
                      "state_topic":"$baseTopic/relay/$relayId/state",
                      "command_topic":"$baseTopic/relay/$relayId/set",
                      "payload_on":"ON",
                      "payload_off":"OFF",
                      "availability_topic":"$baseTopic/status",
                      "device":${deviceJson()}
                    }
                """.compactJson(),
            )
        }
        for (buttonId in 1..capabilities.physicalButtonCount) {
            discovery(
                component = "binary_sensor",
                entityId = "button_${buttonId}_pressed",
                payload = """
                    {
                      "name":"Button $buttonId pressed",
                      "unique_id":"${objectId}_button_${buttonId}_pressed",
                      "state_topic":"$baseTopic/button/$buttonId/state",
                      "payload_on":"ON",
                      "payload_off":"OFF",
                      "availability_topic":"$baseTopic/status",
                      "device":${deviceJson()}
                    }
                """.compactJson(),
            )
            discovery(
                component = "sensor",
                entityId = "button_${buttonId}_event",
                payload = """
                    {
                      "name":"Button $buttonId event",
                      "unique_id":"${objectId}_button_${buttonId}_event",
                      "state_topic":"$baseTopic/button/$buttonId/event",
                      "icon":"mdi:gesture-tap-button",
                      "availability_topic":"$baseTopic/status",
                      "device":${deviceJson()}
                    }
                """.compactJson(),
            )
        }
        discovery(
            component = "number",
            entityId = "screen_brightness",
            payload = """
                {
                  "name":"Screen brightness",
                  "unique_id":"${objectId}_screen_brightness",
                  "state_topic":"$baseTopic/screen/brightness/state",
                  "command_topic":"$baseTopic/screen/brightness/set",
                  "min":0,
                  "max":100,
                  "step":1,
                  "unit_of_measurement":"%",
                  "availability_topic":"$baseTopic/status",
                  "device":${deviceJson()}
                }
            """.compactJson(),
        )
    }

    private suspend fun discovery(component: String, entityId: String, payload: String) {
        publish("homeassistant/$component/$objectId/$entityId/config", payload, retain = true)
    }

    private suspend fun publishAvailability(online: Boolean) {
        publish("$baseTopic/status", if (online) "online" else "offline", retain = true)
    }

    private suspend fun publish(topic: String, payload: String, retain: Boolean) {
        val mqtt = resolveMqtt(settings.settings.first()) ?: return
        MqttPublisher.publish(
            host = mqtt.host,
            port = mqtt.port,
            topic = topic,
            payload = payload.toByteArray(Charsets.UTF_8),
            clientId = mqtt.clientId,
            username = mqtt.username,
            password = mqtt.password,
            useTls = mqtt.useTls,
            retain = retain,
        ).onFailure { t ->
            R1Log.w("PanelMqttBridge", "publish failed topic=$topic: ${t.message}")
        }
    }

    private fun resolveMqtt(settings: AppSettings): MqttConfig? {
        val advanced = settings.advanced
        val host = advanced.mqttHost
        if (host.isBlank()) return null
        return MqttConfig(
            host = host,
            port = advanced.mqttPort,
            username = advanced.mqttUsername.ifBlank { null },
            password = advanced.mqttPassword.ifBlank { null },
            useTls = advanced.mqttUseTls,
            clientId = advanced.mqttClientId.ifBlank { "$objectId-panel" },
        )
    }

    private fun deviceJson(): String = """
        {
          "identifiers":["$objectId"],
          "name":"Hapanels Shelly Wall Display",
          "manufacturer":"tw-zs",
          "model":"${Build.MANUFACTURER} ${Build.MODEL}" 
        }
    """.compactJson()

    private data class MqttConfig(
        val host: String,
        val port: Int,
        val username: String?,
        val password: String?,
        val useTls: Boolean,
        val clientId: String,
    )
}

private fun String.safeId(): String = lowercase()
    .map { if (it.isLetterOrDigit()) it else '_' }
    .joinToString("")
    .trim('_')
    .ifBlank { "panel" }

private fun String.compactJson(): String = lineSequence()
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .joinToString("")
