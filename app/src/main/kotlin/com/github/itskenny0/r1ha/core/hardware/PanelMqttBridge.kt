package com.github.itskenny0.r1ha.core.hardware

import android.os.Build
import com.github.itskenny0.r1ha.core.mqtt.MqttSession
import com.github.itskenny0.r1ha.core.prefs.AppSettings
import com.github.itskenny0.r1ha.core.prefs.SettingsRepository
import com.github.itskenny0.r1ha.core.util.R1Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PanelMqttBridge(
    private val settings: SettingsRepository,
    private val hardware: PanelHardware,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val objectId = "hapanels_${Build.DEVICE.ifBlank { "panel" }.safeId()}"
    private val baseTopic = "hapanels/${Build.DEVICE.ifBlank { "panel" }.safeId()}"
    private var discoverySignature: Pair<Int, Int>? = null
    private var session: MqttSession? = null
    private var settingsJob: Job? = null
    private var sessionJob: Job? = null

    fun start() {
        settingsJob?.cancel()
        settingsJob = scope.launch {
            settings.settings
                .map { resolveMqtt(it) }
                .distinctUntilChanged()
                .collect { mqtt ->
                    sessionJob?.cancel()
                    disconnectSession(publishOffline = true)
                    if (mqtt != null) {
                        sessionJob = scope.launch { maintainSession(mqtt) }
                    }
                }
        }
        scope.launch {
            hardware.capabilities
                .collect { capabilities ->
                    val signature = capabilities.relayCount to capabilities.physicalButtonCount
                    if (signature != discoverySignature) {
                        discoverySignature = signature
                        publishDiscovery(capabilities)
                        subscribeCommands()
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

    fun stop() {
        settingsJob?.cancel()
        settingsJob = null
        sessionJob?.cancel()
        sessionJob = null
        scope.launch { disconnectSession(publishOffline = true) }
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
        (session ?: return).publish(
            topic = topic,
            payload = payload.toByteArray(Charsets.UTF_8),
            retain = retain,
        ).onFailure { t ->
            R1Log.w("PanelMqttBridge", "publish failed topic=$topic: ${t.message}")
        }
    }

    private suspend fun maintainSession(mqtt: MqttConfig) {
        while (scope.isActive) {
            val next = MqttSession(
                host = mqtt.host,
                port = mqtt.port,
                clientId = mqtt.clientId,
                username = mqtt.username,
                password = mqtt.password,
                useTls = mqtt.useTls,
                onMessage = ::handleCommand,
            )
            if (next.connect().isSuccess) {
                session = next
                subscribeCommands()
                publishDiscovery(hardware.capabilities.value)
                publishAvailability(true)
                hardware.runtimeState.value.relayStates.forEach { (id, on) ->
                    if (on != null) publish("$baseTopic/relay/$id/state", if (on) "ON" else "OFF", retain = true)
                }
                while (scope.isActive && session == next && next.isConnected) delay(1_000)
                if (session == next) session = null
            }
            delay(15_000)
        }
    }

    private suspend fun disconnectSession(publishOffline: Boolean) {
        val current = session ?: return
        if (publishOffline) publishAvailability(false)
        current.disconnect()
        session = null
    }

    private suspend fun subscribeCommands() {
        val relayCount = hardware.capabilities.value.relayCount
        for (relayId in 1..relayCount) {
            session?.subscribe("$baseTopic/relay/$relayId/set")
        }
        session?.subscribe("$baseTopic/screen/brightness/set")
    }

    private suspend fun handleCommand(topic: String, payload: ByteArray) {
        when (val command = PanelMqttCommand.parse(baseTopic, topic, payload.toString(Charsets.UTF_8))) {
            is PanelMqttCommand.SetRelay -> hardware.setRelay(command.relayId, command.on)
            is PanelMqttCommand.SetBrightness -> hardware.setScreenBrightness(command.percent)
            null -> R1Log.w("PanelMqttBridge", "ignored command topic=$topic payload=${payload.toString(Charsets.UTF_8).trim()}")
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

internal sealed interface PanelMqttCommand {
    data class SetRelay(val relayId: Int, val on: Boolean) : PanelMqttCommand
    data class SetBrightness(val percent: Int) : PanelMqttCommand

    companion object {
        fun parse(baseTopic: String, topic: String, payload: String): PanelMqttCommand? {
            val text = payload.trim()
            val relayMatch = Regex("^${Regex.escape(baseTopic)}/relay/(\\d+)/set$").matchEntire(topic)
            if (relayMatch != null) {
                val relayId = relayMatch.groupValues[1].toIntOrNull() ?: return null
                val on = when (text.uppercase()) {
                    "ON", "1", "TRUE" -> true
                    "OFF", "0", "FALSE" -> false
                    else -> return null
                }
                return SetRelay(relayId, on)
            }
            if (topic == "$baseTopic/screen/brightness/set") {
                return SetBrightness(text.toIntOrNull()?.coerceIn(0, 100) ?: return null)
            }
            return null
        }
    }
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
