package com.github.itskenny0.r1ha.feature.paneldiagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.github.itskenny0.r1ha.core.hardware.PanelCapabilities
import com.github.itskenny0.r1ha.core.hardware.PanelHardware
import com.github.itskenny0.r1ha.core.hardware.PanelHardwareEvent
import com.github.itskenny0.r1ha.core.hardware.PanelHardwareRuntimeState
import com.github.itskenny0.r1ha.core.hardware.PanelHardwareStatus
import com.github.itskenny0.r1ha.core.hardware.PanelScreenManager
import com.github.itskenny0.r1ha.core.hardware.PanelScreenState
import com.github.itskenny0.r1ha.core.theme.R1
import com.github.itskenny0.r1ha.ui.components.R1TopBar
import com.github.itskenny0.r1ha.ui.i18n.Text
import java.time.Instant

@Composable
fun PanelDiagnosticsScreen(
    hardware: PanelHardware,
    screenManager: PanelScreenManager,
    onBack: () -> Unit,
) {
    val capabilities by hardware.capabilities.collectAsState()
    val status by hardware.status.collectAsState()
    val runtime by hardware.runtimeState.collectAsState()
    val screenState by screenManager.state.collectAsState()
    val recentEvents = remember { mutableStateListOf<PanelHardwareEvent>() }
    LaunchedEffect(hardware) {
        hardware.events.collect { event ->
            recentEvents.add(0, event)
            while (recentEvents.size > 24) recentEvents.removeAt(recentEvents.lastIndex)
        }
    }
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(R1.Bg)
            .systemBarsPadding(),
    ) {
        R1TopBar(title = "PANEL HARDWARE", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "PROVIDER", style = R1.labelMicro, color = R1.InkSoft)
            CapabilityPanel(capabilities, status)
            Text(text = "LIVE STATE", style = R1.labelMicro, color = R1.InkSoft)
            RuntimePanel(runtime)
            Text(text = "SCREEN MANAGER", style = R1.labelMicro, color = R1.InkSoft)
            ScreenManagerPanel(screenState)
            Text(text = "RECENT EVENTS", style = R1.labelMicro, color = R1.InkSoft)
            if (recentEvents.isEmpty()) {
                Text(
                    text = "No hardware events yet. The generic tablet provider emits lifecycle events at app start.",
                    style = R1.body,
                    color = R1.InkMuted,
                )
            } else {
                recentEvents.forEach { EventRow(it) }
            }
            Spacer(Modifier.size(24.dp))
        }
    }
}

@Composable
private fun ScreenManagerPanel(state: PanelScreenState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(R1.ShapeS)
            .background(R1.SurfaceMuted)
            .border(1.dp, R1.Hairline, R1.ShapeS)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Pair("Mode", state.mode.name.lowercase()).render()
        Pair("Target brightness", state.targetBrightnessPercent?.let { "$it%" } ?: "system default").render()
        Pair("Applied brightness", state.appliedBrightnessPercent?.let { "$it%" } ?: "system default").render()
        Pair("Last wake reason", state.lastWakeReason?.name?.lowercase() ?: "none").render()
        Pair("Last sleep reason", state.lastSleepReason?.name?.lowercase() ?: "none").render()
        Pair("Last wake", state.lastWakeAtMillis?.let { Instant.ofEpochMilli(it).toString() } ?: "never").render()
        Pair("Last sleep", state.lastSleepAtMillis?.let { Instant.ofEpochMilli(it).toString() } ?: "never").render()
    }
}

@Composable
private fun RuntimePanel(runtime: PanelHardwareRuntimeState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(R1.ShapeS)
            .background(R1.SurfaceMuted)
            .border(1.dp, R1.Hairline, R1.ShapeS)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Pair("Pressed buttons", runtime.pressedButtonIds.size.toString()).render()
        Pair("Pressed button ids", runtime.pressedButtonIds.sorted().joinToString(", ").ifBlank { "none" }).render()
        Pair("Relays", relayStateText(runtime.relayStates)).render()
        Pair("Proximity distance", runtime.proximityDistanceCm?.let { "${formatFloat(it)} cm" } ?: "unknown").render()
        Pair("Ambient light", runtime.ambientLightLux?.let { "${formatFloat(it)} lx" } ?: "unknown").render()
        Pair("Jasność ekranu", runtime.screenBrightnessPercent?.let { "$it%" } ?: "unknown").render()
        runtime.note?.takeIf { it.isNotBlank() }?.let { Pair("Note", it).render() }
        Pair("Updated", Instant.ofEpochMilli(runtime.updatedAtMillis).toString()).render()
    }
}

@Composable
private fun CapabilityPanel(capabilities: PanelCapabilities, status: PanelHardwareStatus) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(R1.ShapeS)
            .background(R1.SurfaceMuted)
            .border(1.dp, R1.Hairline, R1.ShapeS)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Pair("State", if (status.running) "active" else "stopped").render()
        Pair("Mode", status.modeLabel).render()
        Pair("Provider", status.providerLabel.ifBlank { capabilities.providerLabel }).render()
        Pair("Detail", status.detail).render()
        Pair("Model", capabilities.hardwareModel).render()
        Pair("Relays", capabilities.relayCount.toString()).render()
        Pair("Physical buttons", capabilities.physicalButtonCount.toString()).render()
        Pair("Ambient light", yesNo(capabilities.hasAmbientLightSensor)).render()
        Pair("Proximity", yesNo(capabilities.hasProximitySensor)).render()
        Pair("Screen brightness", yesNo(capabilities.supportsScreenBrightness)).render()
        Pair("Wake control", yesNo(capabilities.supportsWake)).render()
    }
}

@Composable
private fun EventRow(event: PanelHardwareEvent) {
    val (kind, detail) = when (event) {
        is PanelHardwareEvent.Lifecycle -> "Lifecycle" to event.message
        is PanelHardwareEvent.UnsupportedAction -> event.action to event.detail
        is PanelHardwareEvent.Button -> "Button ${event.buttonId}" to event.type.name.lowercase()
        is PanelHardwareEvent.Relay -> "Relay ${event.relayId}" to if (event.on) "on" else "off"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(R1.ShapeS)
            .background(R1.SurfaceMuted)
            .border(1.dp, R1.Hairline, R1.ShapeS)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = kind.uppercase(), style = R1.labelMicro, color = R1.InkSoft)
            Text(text = detail, style = R1.body, color = R1.Ink)
            Text(text = Instant.ofEpochMilli(event.timestampMillis).toString(), style = R1.labelMicro, color = R1.InkMuted)
        }
    }
}

@Composable
private fun Pair<String, String>.render() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = first.uppercase(), style = R1.labelMicro, color = R1.InkMuted)
        Text(text = second, style = R1.body, color = R1.Ink)
    }
}

private fun yesNo(value: Boolean): String = if (value) "yes" else "no"

private fun relayStateText(states: Map<Int, Boolean?>): String {
    if (states.isEmpty()) return "unknown / not wired"
    return states.entries.sortedBy { it.key }.joinToString(" · ") { (id, on) ->
        "R$id=${when (on) { true -> "on"; false -> "off"; null -> "unknown" }}"
    }
}

private fun formatFloat(value: Float): String = if (value % 1f == 0f) {
    value.toInt().toString()
} else {
    String.format(java.util.Locale.US, "%.1f", value)
}
