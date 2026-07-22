package com.github.itskenny0.r1ha.core.hardware

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.github.itskenny0.r1ha.BuildConfig
import com.github.itskenny0.r1ha.core.mqtt.MqttSession
import com.github.itskenny0.r1ha.core.prefs.AppSettings
import com.github.itskenny0.r1ha.core.prefs.AdvancedSettings
import com.github.itskenny0.r1ha.core.prefs.SettingsRepository
import com.github.itskenny0.r1ha.core.util.R1Log
import com.github.itskenny0.r1ha.feature.panelgrid.HapanelsDashboardConfig
import com.github.itskenny0.r1ha.feature.panelgrid.HapanelsDashboardConfigSource
import com.github.itskenny0.r1ha.feature.panelgrid.HapanelsDashboardPatchResult
import com.github.itskenny0.r1ha.feature.panelgrid.HAPANELS_DASHBOARD_SCHEMA_VERSION
import com.github.itskenny0.r1ha.feature.panelgrid.syncStateJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PanelMqttBridge(
    private val context: Context,
    private val settings: SettingsRepository,
    private val hardware: PanelHardware,
    private val dashboardConfigSource: HapanelsDashboardConfigSource,
    private val screenManager: PanelScreenManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var panelDeviceId: String = "panel"
    private var tabletFriendlyName: String = ""
    private val objectId: String get() = "hapanels_$panelDeviceId"
    private val baseTopic: String get() = "hapanels/$panelDeviceId"
    private var discoverySignature: PanelDiscoverySignature? = null
    private var session: MqttSession? = null
    private var settingsJob: Job? = null
    private var sessionJob: Job? = null
    private var mqttConnectionState: String = "disabled"
    private var lastConnectError: String = "none"
    private var lastPublishError: String = "none"
    private var lastSubscribeError: String = "none"

    fun start() {
        settingsJob?.cancel()
        settingsJob = scope.launch {
            val initialSettings = settings.settings.first()
            panelDeviceId = panelDeviceId(context, initialSettings)
            tabletFriendlyName = initialSettings.tabletFriendlyName
            launch {
                settings.settings
                    .map { resolveMqtt(it) }
                    .distinctUntilChanged()
                    .collect { mqtt ->
                        sessionJob?.cancel()
                        disconnectSession(publishOffline = true)
                        if (mqtt != null) {
                            mqttConnectionState = "connecting"
                            sessionJob = scope.launch { maintainSession(mqtt) }
                        } else {
                            mqttConnectionState = "disabled"
                        }
                    }
            }
            launch {
                hardware.capabilities
                    .collect { capabilities ->
                        val signature = PanelDiscoverySignature.from(capabilities)
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
            launch {
                hardware.runtimeState.collect { runtime ->
                    runtime.relayStates.forEach { (id, on) ->
                        if (on != null) publish("$baseTopic/relay/$id/state", if (on) "ON" else "OFF", retain = true)
                    }
                    (1..hardware.capabilities.value.physicalButtonCount).forEach { id ->
                        publish("$baseTopic/button/$id/state", if (id in runtime.pressedButtonIds) "ON" else "OFF", retain = false)
                    }
                    runtime.screenBrightnessPercent?.let { publish("$baseTopic/screen/brightness/state", it.toString(), retain = true) }
                    runtime.screenBrightnessPercent?.let { publish("$baseTopic/screen/applied_brightness/state", it.toString(), retain = true) }
                    runtime.ambientLightLux?.let { publish("$baseTopic/sensor/ambient_light/state", it.toString(), retain = true) }
                    runtime.proximityDistanceCm?.let {
                        publish("$baseTopic/binary_sensor/proximity_presence/state", if (it <= 5f) "ON" else "OFF", retain = true)
                    }
                }
            }
            launch {
                screenManager.state.collect { screen -> publishScreenDiagnostics(screen) }
            }
            launch {
                settings.settings
                    .map { it.advanced.autoBrightnessEnabled }
                    .distinctUntilChanged()
                    .collect { enabled -> publishAutoBrightnessState(enabled) }
            }
            launch {
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
            entityId = "mqtt_connection",
            payload = """
                {
                  "name":"MQTT connection",
                  "unique_id":"${objectId}_mqtt_connection",
                  "state_topic":"$baseTopic/mqtt/connection/state",
                  "icon":"mdi:lan-connect",
                  "availability_topic":"$baseTopic/status",
                  "device":${deviceJson()}
                }
            """.compactJson(),
        )
        discovery(
            component = "sensor",
            entityId = "mqtt_last_connect_error",
            payload = """
                {
                  "name":"MQTT last connect error",
                  "unique_id":"${objectId}_mqtt_last_connect_error",
                  "state_topic":"$baseTopic/mqtt/last_connect_error/state",
                  "icon":"mdi:lan-disconnect",
                  "availability_topic":"$baseTopic/status",
                  "device":${deviceJson()}
                }
            """.compactJson(),
        )
        discovery(
            component = "sensor",
            entityId = "mqtt_last_publish_error",
            payload = """
                {
                  "name":"MQTT last publish error",
                  "unique_id":"${objectId}_mqtt_last_publish_error",
                  "state_topic":"$baseTopic/mqtt/last_publish_error/state",
                  "icon":"mdi:publish-off",
                  "availability_topic":"$baseTopic/status",
                  "device":${deviceJson()}
                }
            """.compactJson(),
        )
        discovery(
            component = "sensor",
            entityId = "mqtt_last_subscribe_error",
            payload = """
                {
                  "name":"MQTT last subscribe error",
                  "unique_id":"${objectId}_mqtt_last_subscribe_error",
                  "state_topic":"$baseTopic/mqtt/last_subscribe_error/state",
                  "icon":"mdi:message-badge-outline",
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
        discovery(
            component = "switch",
            entityId = "screen_auto_brightness",
            payload = """
                {
                  "name":"Screen auto brightness",
                  "unique_id":"${objectId}_screen_auto_brightness",
                  "state_topic":"$baseTopic/screen/auto_brightness/state",
                  "command_topic":"$baseTopic/screen/auto_brightness/set",
                  "payload_on":"ON",
                  "payload_off":"OFF",
                  "icon":"mdi:brightness-auto",
                  "availability_topic":"$baseTopic/status",
                  "device":${deviceJson()}
                }
            """.compactJson(),
        )
        discovery(
            component = "sensor",
            entityId = "screen_mode",
            payload = """
                {
                  "name":"Screen mode",
                  "unique_id":"${objectId}_screen_mode",
                  "state_topic":"$baseTopic/screen/mode/state",
                  "icon":"mdi:monitor-eye",
                  "availability_topic":"$baseTopic/status",
                  "device":${deviceJson()}
                }
            """.compactJson(),
        )
        discovery(
            component = "sensor",
            entityId = "screen_resolution",
            payload = """
                {
                  "name":"Screen resolution",
                  "unique_id":"${objectId}_screen_resolution",
                  "state_topic":"$baseTopic/screen/resolution/state",
                  "icon":"mdi:monitor-screenshot",
                  "availability_topic":"$baseTopic/status",
                  "device":${deviceJson()}
                }
            """.compactJson(),
        )
        discovery(
            component = "sensor",
            entityId = "screen_target_brightness",
            payload = """
                {
                  "name":"Screen target brightness",
                  "unique_id":"${objectId}_screen_target_brightness",
                  "state_topic":"$baseTopic/screen/target_brightness/state",
                  "state_class":"measurement",
                  "unit_of_measurement":"%",
                  "icon":"mdi:brightness-auto",
                  "availability_topic":"$baseTopic/status",
                  "device":${deviceJson()}
                }
            """.compactJson(),
        )
        discovery(
            component = "sensor",
            entityId = "screen_applied_brightness",
            payload = """
                {
                  "name":"Screen applied brightness",
                  "unique_id":"${objectId}_screen_applied_brightness",
                  "state_topic":"$baseTopic/screen/applied_brightness/state",
                  "state_class":"measurement",
                  "unit_of_measurement":"%",
                  "icon":"mdi:brightness-percent",
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
                component = "binary_sensor",
                entityId = "proximity_presence",
                payload = """
                    {
                      "name":"Obecność przy panelu",
                      "unique_id":"${objectId}_proximity_presence",
                      "state_topic":"$baseTopic/binary_sensor/proximity_presence/state",
                      "payload_on":"ON",
                      "payload_off":"OFF",
                      "device_class":"occupancy",
                      "availability_topic":"$baseTopic/status",
                      "device":${deviceJson()}
                    }
                """.compactJson(),
            )
            clearDiscovery(component = "sensor", entityId = "proximity_distance")
        } else {
            clearDiscovery(component = "binary_sensor", entityId = "proximity_presence")
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
        publish("$baseTopic/screen/resolution/state", screenResolutionState(capabilities), retain = true)
        publishMqttDiagnostics()
    }

    private fun screenResolutionState(capabilities: PanelCapabilities): String =
        if (capabilities.screenWidthPx != null && capabilities.screenHeightPx != null) {
            "${capabilities.screenWidthPx}x${capabilities.screenHeightPx}"
        } else {
            "unknown"
        }

    private suspend fun publishMqttDiagnostics() {
        publish("$baseTopic/mqtt/connection/state", mqttConnectionState, retain = true)
        publish("$baseTopic/mqtt/last_connect_error/state", lastConnectError, retain = true)
        publish("$baseTopic/mqtt/last_publish_error/state", lastPublishError, retain = true)
        publish("$baseTopic/mqtt/last_subscribe_error/state", lastSubscribeError, retain = true)
    }

    private suspend fun publishScreenDiagnostics(screen: PanelScreenState) {
        val appliedBrightness = screen.appliedBrightnessPercent ?: hardware.runtimeState.value.screenBrightnessPercent
        publish("$baseTopic/screen/mode/state", screen.mode.name.lowercase(), retain = true)
        publish("$baseTopic/screen/target_brightness/state", screen.targetBrightnessPercent?.toString() ?: "unknown", retain = true)
        publish("$baseTopic/screen/applied_brightness/state", appliedBrightness?.toString() ?: "unknown", retain = true)
    }

    private suspend fun publishAutoBrightnessState(enabled: Boolean) {
        publish("$baseTopic/screen/auto_brightness/state", if (enabled) "ON" else "OFF", retain = true)
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
            lastPublishError = mqttDiagnosticError(t.message)
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
                willTopic = "$baseTopic/status",
                willPayload = "offline".toByteArray(Charsets.UTF_8),
                willRetain = true,
                onMessage = ::handleCommand,
            )
            val connectResult = next.connect()
            if (connectResult.isSuccess) {
                session = next
                mqttConnectionState = "connected"
                lastConnectError = "none"
                subscribeCommands()
                publishDiscovery(hardware.capabilities.value)
                publishPanelDiagnostics(hardware.capabilities.value)
                publishScreenDiagnostics(screenManager.state.value)
                publishAutoBrightnessState(settings.settings.first().advanced.autoBrightnessEnabled)
                publishDashboardConfig()
                publishAvailability(true)
                hardware.runtimeState.value.relayStates.forEach { (id, on) ->
                    if (on != null) publish("$baseTopic/relay/$id/state", if (on) "ON" else "OFF", retain = true)
                }
                while (scope.isActive && session == next && next.isConnected) delay(1_000)
                if (session == next) {
                    mqttConnectionState = "disconnected"
                    session = null
                }
            } else {
                mqttConnectionState = "connect_failed"
                lastConnectError = mqttDiagnosticError(connectResult.exceptionOrNull()?.message)
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
            subscribe("$baseTopic/relay/$relayId/set")
        }
        subscribe("$baseTopic/screen/brightness/set")
        subscribe("$baseTopic/screen/auto_brightness/set")
        subscribe("$baseTopic/dashboard/config/set")
        subscribe("$baseTopic/dashboard/config/patch/set")
        publishMqttDiagnostics()
    }

    private suspend fun subscribe(topic: String) {
        val result = session?.subscribe(topic) ?: return
        result.onSuccess {
            lastSubscribeError = "none"
        }.onFailure { t ->
            lastSubscribeError = mqttDiagnosticError(t.message)
        }
    }

    private suspend fun handleCommand(topic: String, payload: ByteArray) {
        when (val command = PanelMqttCommand.parse(baseTopic, topic, payload.toString(Charsets.UTF_8))) {
            is PanelMqttCommand.SetRelay -> hardware.setRelay(command.relayId, command.on)
            is PanelMqttCommand.SetBrightness -> hardware.setScreenBrightness(command.percent)
            is PanelMqttCommand.SetAutoBrightness -> setAutoBrightness(command.enabled)
            is PanelMqttCommand.SetDashboardConfig -> importDashboardConfig(command.rawJson)
            is PanelMqttCommand.PatchDashboardConfig -> patchDashboardConfig(command.rawJson)
            null -> R1Log.w("PanelMqttBridge", "ignored command topic=$topic payload=${payload.toString(Charsets.UTF_8).trim()}")
        }
    }

    private suspend fun publishDashboardConfig() {
        runCatching {
            val config = dashboardConfigSource.loadOrSeed()
            val raw = dashboardConfigSource.exportRaw()
            syncAodSettings(config)
            publish("$baseTopic/dashboard/config/state", raw, retain = true)
            publish("$baseTopic/dashboard/config/meta", config.dashboardMetaJson(), retain = true)
            publish("$baseTopic/dashboard/config/sync/state", config.syncStateJson("synced", panelName = panelDisplayName()), retain = true)
            publishDashboardMetadata(config)
        }.onFailure { t ->
            R1Log.w("PanelMqttBridge", "dashboard config publish failed: ${t.message}")
        }
    }

    private suspend fun setAutoBrightness(enabled: Boolean) {
        settings.update { current ->
            current.copy(advanced = current.advanced.copy(autoBrightnessEnabled = enabled))
        }
        publishAutoBrightnessState(enabled)
    }

    private suspend fun importDashboardConfig(rawJson: String) {
        runCatching { dashboardConfigSource.importRaw(rawJson) }
            .onSuccess { config ->
                syncAodSettings(config)
                publish("$baseTopic/dashboard/config/state", dashboardConfigSource.exportRaw(), retain = true)
                publish("$baseTopic/dashboard/config/meta", config.dashboardMetaJson(), retain = true)
                publish("$baseTopic/dashboard/config/sync/state", config.syncStateJson("synced", panelName = panelDisplayName()), retain = true)
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
                publish("$baseTopic/dashboard/config/sync/state", result.config.syncStateJson("synced", panelName = panelDisplayName()), retain = true)
                        syncAodSettings(result.config)
                        publishDashboardMetadata(result.config)
                    }
                    is HapanelsDashboardPatchResult.Conflict -> {
                        publish("$baseTopic/dashboard/config/sync/state", result.currentConfig.syncStateJson(
                            status = "conflict",
                            panelName = panelDisplayName(),
                            attemptedBaseRevision = result.attemptedBaseRevision,
                            currentRevision = result.currentRevision,
                        ), retain = true)
                        publish("$baseTopic/dashboard/config/conflict", result.conflictJson(), retain = false)
                    }
                }
            }
            .onFailure { t ->
                R1Log.w("PanelMqttBridge", "dashboard config patch failed: ${t.message}")
            }
    }

    private suspend fun syncAodSettings(config: HapanelsDashboardConfig) {
        settings.update { current ->
            val defaults = AdvancedSettings()
            val advanced = current.advanced
            current.copy(
                advanced = current.advanced.copy(
                    screensaverEnabled = if (advanced.screensaverEnabled == defaults.screensaverEnabled) {
                        config.alwaysOnDisplay.enabled
                    } else {
                        advanced.screensaverEnabled
                    },
                    screensaverTimeoutSec = if (advanced.screensaverTimeoutSec == defaults.screensaverTimeoutSec) {
                        config.alwaysOnDisplay.timeoutSec.coerceIn(5, 900)
                    } else {
                        advanced.screensaverTimeoutSec
                    },
                ),
            )
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
          "name":"${panelDisplayName().escapeJson()}",
          "manufacturer":"tw-zs",
          "model":"${Build.MANUFACTURER} ${Build.MODEL}" 
        }
    """.compactJson()

    private fun panelDisplayName(): String = tabletFriendlyName.ifBlank { "Hapanels $panelDeviceId" }

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
    data class SetAutoBrightness(val enabled: Boolean) : PanelMqttCommand
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
            if (topic == "$baseTopic/screen/auto_brightness/set") {
                val enabled = when (text.uppercase()) {
                    "ON", "1", "TRUE" -> true
                    "OFF", "0", "FALSE" -> false
                    else -> return null
                }
                return SetAutoBrightness(enabled)
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
      "schema_version":$HAPANELS_DASHBOARD_SCHEMA_VERSION,
      "schema_capabilities":["panels","presentation","text","spacer","tap_action","technical_actions"],
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

private fun panelDeviceId(context: Context, settings: AppSettings): String = resolvePanelDeviceId(
    panelDeviceId = settings.mqttPanelDeviceId,
    mqttClientId = settings.advanced.mqttClientId,
    androidId = stableAndroidId(context),
    buildDevice = Build.DEVICE,
    buildModel = Build.MODEL,
    buildProduct = Build.PRODUCT,
)

internal fun resolvePanelDeviceId(
    panelDeviceId: String?,
    mqttClientId: String?,
    androidId: String?,
    buildDevice: String,
    buildModel: String,
    buildProduct: String,
): String = listOf(panelDeviceId, mqttClientId, androidId, buildDevice, buildModel, buildProduct)
    .firstNotNullOfOrNull { it?.trim()?.takeIf { value -> value.isNotBlank() } }
    ?.safeId()
    ?: "panel"

private fun stableAndroidId(context: Context): String? = runCatching {
    Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
}.getOrNull()?.takeIf { it.isNotBlank() }

internal fun mqttDiagnosticError(message: String?): String = message
    ?.trim()
    ?.replace(Regex("\\s+"), " ")
    ?.take(240)
    ?.ifBlank { null }
    ?: "unknown"

private fun String.compactJson(): String = lineSequence()
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .joinToString("")

private data class PanelDiscoverySignature(
    val relayCount: Int,
    val physicalButtonCount: Int,
    val hasAmbientLightSensor: Boolean,
    val hasProximitySensor: Boolean,
    val supportsScreenBrightness: Boolean,
) {
    companion object {
        fun from(capabilities: PanelCapabilities): PanelDiscoverySignature = PanelDiscoverySignature(
            relayCount = capabilities.relayCount,
            physicalButtonCount = capabilities.physicalButtonCount,
            hasAmbientLightSensor = capabilities.hasAmbientLightSensor,
            hasProximitySensor = capabilities.hasProximitySensor,
            supportsScreenBrightness = capabilities.supportsScreenBrightness,
        )
    }
}
