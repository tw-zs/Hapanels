package com.github.itskenny0.r1ha.core.hardware

import android.os.Build
import com.github.itskenny0.r1ha.BuildConfig
import com.github.itskenny0.r1ha.core.mqtt.MqttSession
import com.github.itskenny0.r1ha.core.prefs.AppSettings
import com.github.itskenny0.r1ha.core.prefs.SettingsRepository
import com.github.itskenny0.r1ha.core.util.R1Log
import com.github.itskenny0.r1ha.feature.panelgrid.HapanelsDashboardConfig
import com.github.itskenny0.r1ha.feature.panelgrid.HapanelsDashboardConfigSource
import com.github.itskenny0.r1ha.feature.panelgrid.HapanelsDashboardPatchResult
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
    private val dashboardConfigSource: HapanelsDashboardConfigSource,
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
                        publishPanelDiagnostics(capabilities)
                        publishDashboardConfig()
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
        discovery(
            component = "binary_sensor",
            entityId = "app_online",
            payload = """
                {
                  "name":"App online",
                  "unique_id":"${objectId}_app_online",
                  "state_topic":"$baseTopic/status",
                  "payload_on":"online",
                  "payload_off":"offline",
                  "device_class":"connectivity",
                  "device":${deviceJson()}
                }
            """.compactJson(),
        )
        discovery(
            component = "sensor",
            entityId = "app_version",
            payload = """
                {
                  "name":"App version",
                  "unique_id":"${objectId}_app_version",
                  "state_topic":"$baseTopic/app/version/state",
                  "icon":"mdi:cellphone-cog",
                  "availability_topic":"$baseTopic/status",
                  "device":${deviceJson()}
                }
            """.compactJson(),
        )
        discovery(
            component = "sensor",
            entityId = "hardware_provider",
            payload = """
                {
                  "name":"Hardware provider",
                  "unique_id":"${objectId}_hardware_provider",
                  "state_topic":"$baseTopic/hardware/provider/state",
                  "icon":"mdi:chip",
                  "availability_topic":"$baseTopic/status",
                  "device":${deviceJson()}
                }
            """.compactJson(),
        )
        discovery(
            component = "sensor",
            entityId = "dashboard_revision",
            payload = """
                {
                  "name":"Dashboard revision",
                  "unique_id":"${objectId}_dashboard_revision",
                  "state_topic":"$baseTopic/dashboard/revision/state",
                  "state_class":"measurement",
                  "icon":"mdi:counter",
                  "availability_topic":"$baseTopic/status",
                  "device":${deviceJson()}
                }
            """.compactJson(),
        )
        discovery(
            component = "sensor",
            entityId = "dashboard_id",
            payload = """
                {
                  "name":"Dashboard id",
                  "unique_id":"${objectId}_dashboard_id",
                  "state_topic":"$baseTopic/dashboard/id/state",
                  "icon":"mdi:view-dashboard",
                  "availability_topic":"$baseTopic/status",
                  "device":${deviceJson()}
                }
            """.compactJson(),
        )
        discovery(
            component = "sensor",
            entityId = "dashboard_updated_by",
            payload = """
                {
                  "name":"Dashboard updated by",
                  "unique_id":"${objectId}_dashboard_updated_by",
                  "state_topic":"$baseTopic/dashboard/updated_by/state",
                  "icon":"mdi:account-edit",
                  "availability_topic":"$baseTopic/status",
                  "device":${deviceJson()}
                }
            """.compactJson(),
        )
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
            PanelButtonPressType.entries.forEach { pressType ->
                discovery(
                    component = "device_automation",
                    entityId = "button_${buttonId}_${pressType.name.lowercase()}",
                    payload = """
                        {
                          "automation_type":"trigger",
                          "topic":"$baseTopic/button/$buttonId/event",
                          "payload":"${pressType.name.lowercase()}",
                          "type":"action",
                          "subtype":"button_${buttonId}_${pressType.name.lowercase()}",
                          "device":${deviceJson()}
                        }
                    """.compactJson(),
                )
            }
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
        if (capabilities.hasAmbientLightSensor) {
            discovery(
                component = "sensor",
                entityId = "ambient_light",
                payload = """
                    {
                      "name":"Ambient light",
                      "unique_id":"${objectId}_ambient_light",
                      "state_topic":"$baseTopic/sensor/ambient_light/state",
                      "device_class":"illuminance",
                      "state_class":"measurement",
                      "unit_of_measurement":"lx",
                      "availability_topic":"$baseTopic/status",
                      "device":${deviceJson()}
                    }
                """.compactJson(),
            )
        } else {
            clearDiscovery(component = "sensor", entityId = "ambient_light")
        }
        if (capabilities.hasProximitySensor) {
            discovery(
                component = "sensor",
                entityId = "proximity_distance",
                payload = """
                    {
                      "name":"Proximity distance",
                      "unique_id":"${objectId}_proximity_distance",
                      "state_topic":"$baseTopic/sensor/proximity/state",
                      "state_class":"measurement",
                      "unit_of_measurement":"cm",
                      "icon":"mdi:arrow-expand-horizontal",
                      "availability_topic":"$baseTopic/status",
                      "device":${deviceJson()}
                    }
                """.compactJson(),
            )
        } else {
            clearDiscovery(component = "sensor", entityId = "proximity_distance")
        }
    }

    private suspend fun discovery(component: String, entityId: String, payload: String) {
        publish("homeassistant/$component/$objectId/$entityId/config", payload, retain = true)
    }

    private suspend fun clearDiscovery(component: String, entityId: String) {
        publish("homeassistant/$component/$objectId/$entityId/config", "", retain = true)
    }

    private suspend fun publishPanelDiagnostics(capabilities: PanelCapabilities) {
        publish("$baseTopic/app/version/state", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", retain = true)
        publish("$baseTopic/hardware/provider/state", capabilities.providerLabel, retain = true)
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
                publishPanelDiagnostics(hardware.capabilities.value)
                publishDashboardConfig()
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
        session?.subscribe("$baseTopic/dashboard/config/set")
        session?.subscribe("$baseTopic/dashboard/config/patch/set")
    }

    private suspend fun handleCommand(topic: String, payload: ByteArray) {
        when (val command = PanelMqttCommand.parse(baseTopic, topic, payload.toString(Charsets.UTF_8))) {
            is PanelMqttCommand.SetRelay -> hardware.setRelay(command.relayId, command.on)
            is PanelMqttCommand.SetBrightness -> hardware.setScreenBrightness(command.percent)
            is PanelMqttCommand.SetDashboardConfig -> importDashboardConfig(command.rawJson)
            is PanelMqttCommand.PatchDashboardConfig -> patchDashboardConfig(command.rawJson)
            null -> R1Log.w("PanelMqttBridge", "ignored command topic=$topic payload=${payload.toString(Charsets.UTF_8).trim()}")
        }
    }

    private suspend fun publishDashboardConfig() {
        runCatching {
            val config = dashboardConfigSource.loadOrSeed()
            val raw = dashboardConfigSource.exportRaw()
            publish("$baseTopic/dashboard/config/state", raw, retain = true)
            publish("$baseTopic/dashboard/config/meta", config.dashboardMetaJson(), retain = true)
            publishDashboardMetadata(config)
        }.onFailure { t ->
            R1Log.w("PanelMqttBridge", "dashboard config publish failed: ${t.message}")
        }
    }

    private suspend fun importDashboardConfig(rawJson: String) {
        runCatching { dashboardConfigSource.importRaw(rawJson) }
            .onSuccess { config ->
                publish("$baseTopic/dashboard/config/state", dashboardConfigSource.exportRaw(), retain = true)
                publish("$baseTopic/dashboard/config/meta", config.dashboardMetaJson(), retain = true)
                publishDashboardMetadata(config)
            }
            .onFailure { t ->
                R1Log.w("PanelMqttBridge", "dashboard config import failed: ${t.message}")
            }
    }

    private suspend fun patchDashboardConfig(rawJson: String) {
        runCatching { dashboardConfigSource.applyPatchRaw(rawJson) }
            .onSuccess { result ->
                when (result) {
                    is HapanelsDashboardPatchResult.Applied -> {
                        publish("$baseTopic/dashboard/config/state", dashboardConfigSource.exportRaw(), retain = true)
                        publish("$baseTopic/dashboard/config/meta", result.config.dashboardMetaJson(), retain = true)
                        publishDashboardMetadata(result.config)
                    }
                    is HapanelsDashboardPatchResult.Conflict -> {
                        publish("$baseTopic/dashboard/config/conflict", result.conflictJson(), retain = false)
                    }
                }
            }
            .onFailure { t ->
                R1Log.w("PanelMqttBridge", "dashboard config patch failed: ${t.message}")
            }
    }

    private suspend fun publishDashboardMetadata(config: HapanelsDashboardConfig) {
        publish("$baseTopic/dashboard/revision/state", config.revision.toString(), retain = true)
        publish("$baseTopic/dashboard/id/state", config.dashboardId, retain = true)
        publish("$baseTopic/dashboard/updated_by/state", config.updatedBy, retain = true)
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
    data class SetDashboardConfig(val rawJson: String) : PanelMqttCommand
    data class PatchDashboardConfig(val rawJson: String) : PanelMqttCommand

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
            if (topic == "$baseTopic/dashboard/config/set") {
                return SetDashboardConfig(payload.trim())
            }
            if (topic == "$baseTopic/dashboard/config/patch/set") {
                return PatchDashboardConfig(payload.trim())
            }
            return null
        }
    }
}

private fun HapanelsDashboardConfig.dashboardMetaJson(): String = """
    {
      "version":$version,
      "dashboard_id":"${dashboardId.escapeJson()}",
      "revision":$revision,
      "updated_by":"${updatedBy.escapeJson()}"
    }
""".compactJson()

private fun HapanelsDashboardPatchResult.Conflict.conflictJson(): String = """
    {
      "current_revision":$currentRevision,
      "attempted_base_revision":$attemptedBaseRevision,
      "dashboard_id":"${currentConfig.dashboardId.escapeJson()}"
    }
""".compactJson()

private fun String.escapeJson(): String = buildString(length) {
    for (char in this@escapeJson) {
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(char)
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
