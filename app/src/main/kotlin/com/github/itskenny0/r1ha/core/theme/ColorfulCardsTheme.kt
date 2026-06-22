package com.github.itskenny0.r1ha.core.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import com.github.itskenny0.r1ha.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.itskenny0.r1ha.core.prefs.DisplayMode
import com.github.itskenny0.r1ha.core.prefs.ThemeId

/**
 * "Colourful Cards" — a per-entity gradient sky behind the same Mission-Control
 * layout. Each card gets a stable palette hashed from its entity_id so two lights
 * named "kitchen" and "lounge" always read distinctly. Reuses the shared
 * [PragmaticHybridTheme] building blocks ([BigReadout], [LightControlsRow],
 * [MediaControlsRow], [VerticalTapeMeter]) so the theme is fully featured rather than
 * just a pretty wrapper; the distinct identity here is the gradient backdrop and the
 * always-white ink/accent.
 */
object ColorfulCardsTheme : R1Theme {
    override val id = ThemeId.COLORFUL_CARDS
    override val displayName = "Colourful Cards"
    override val systemBars = SystemBarColors(status = Color.Black, nav = Color.Black)
    override val baseline = sharedDarkBaseline

    private val palette = listOf(
        listOf(Color(0xFFFFB347), Color(0xFFFF6B1A), Color(0xFFC7338A)), // warm
        listOf(Color(0xFF41BDF5), Color(0xFF1B7BB8), Color(0xFF0D3B66)), // cool
        listOf(Color(0xFF52C77F), Color(0xFF2C8B5A), Color(0xFF154A35)), // green
        listOf(Color(0xFF9B6BD8), Color(0xFF5B3B9E), Color(0xFF2E2057)), // violet
    )
    private val gradientEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    private val presetGlowEasing = CubicBezierEasing(0.34f, 1.18f, 0.64f, 1f)

    private fun paletteFor(id: String): List<Color> =
        palette[(id.hashCode().rem(palette.size) + palette.size) % palette.size]

    private fun climatePalette(model: CardRenderModel): List<Color>? {
        if (model.domainGlyph != CardRenderModel.Glyph.CLIMATE) return null
        val mode = (model.entityState?.climateHvacMode ?: model.entityState?.rawState).orEmpty()
            .lowercase()
            .replace('-', '_')
        return when (mode) {
            "heat", "heating" -> listOf(Color(0xFFFFB347), Color(0xFFFF6B1A), Color(0xFFD73372))
            "cool", "cooling" -> listOf(Color(0xFF41C7F5), Color(0xFF1B8FD1), Color(0xFF0D3B66))
            "dry", "drying" -> listOf(Color(0xFFD7B56D), Color(0xFFB8843A), Color(0xFF6F4A2A))
            "auto" -> listOf(Color(0xFF6EE7F2), Color(0xFF7C8CF8), Color(0xFFC084FC))
            "heat_cool" -> listOf(Color(0xFF35C7F3), Color(0xFF8A7CFF), Color(0xFFFF7A2F))
            "off" -> listOf(Color(0xFF343A40), Color(0xFF1F2933), Color(0xFF0B0F14))
            else -> null
        }
    }

    private fun climatePresetOverlay(model: CardRenderModel): Color? {
        if (model.domainGlyph != CardRenderModel.Glyph.CLIMATE) return null
        val preset = model.entityState?.climatePresetMode.orEmpty()
            .lowercase()
            .replace('-', '_')
        return when {
            preset.isBlank() || preset == "none" -> null
            "eco" in preset -> Color(0xFF35D07F)
            "comfort" in preset -> Color(0xFFFFE08A)
            "turbo" in preset || "boost" in preset -> Color(0xFFFF3B30)
            "sleep" in preset || "sen" in preset -> Color(0xFF5B6CFF)
            "away" in preset || "poza" in preset -> Color(0xFF8B949E)
            else -> null
        }
    }

    private fun domainLabel(glyph: CardRenderModel.Glyph): String = when (glyph) {
        CardRenderModel.Glyph.LIGHT -> "LIGHT"
        CardRenderModel.Glyph.FAN -> "FAN"
        CardRenderModel.Glyph.COVER -> "COVER"
        CardRenderModel.Glyph.MEDIA_PLAYER -> "MEDIA"
        CardRenderModel.Glyph.SWITCH -> "SWITCH"
        CardRenderModel.Glyph.LOCK -> "LOCK"
        CardRenderModel.Glyph.HUMIDIFIER -> "HUMIDIFIER"
        CardRenderModel.Glyph.CLIMATE -> "CLIMATE"
        CardRenderModel.Glyph.NUMBER -> "NUMBER"
        CardRenderModel.Glyph.VALVE -> "VALVE"
        CardRenderModel.Glyph.VACUUM -> "VACUUM"
        CardRenderModel.Glyph.WATER_HEATER -> "WATER HEATER"
        CardRenderModel.Glyph.LAWN_MOWER -> "MOWER"
    }

    @Composable
    override fun Card(model: CardRenderModel, modifier: Modifier, onTapToggle: () -> Unit) {
        val pal = climatePalette(model) ?: paletteFor(model.entityIdText)
        val climatePresetColor = climatePresetOverlay(model)
        val animatedPalette = pal.mapIndexed { index, color ->
            animateColorAsState(
                targetValue = color,
                animationSpec = tween(durationMillis = 680, easing = gradientEasing),
                label = "colorful-card-gradient-$index",
            ).value
        }
        val animatedPresetColor = animateColorAsState(
            targetValue = climatePresetColor ?: Color.Transparent,
            animationSpec = tween(durationMillis = 520, easing = gradientEasing),
            label = "colorful-card-preset-color",
        ).value
        val presetGlowAlpha = animateFloatAsState(
            targetValue = if (climatePresetColor != null) 1f else 0f,
            animationSpec = tween(durationMillis = 520, easing = presetGlowEasing),
            label = "colorful-card-preset-alpha",
        ).value
        val presetGlowScale = animateFloatAsState(
            targetValue = if (climatePresetColor != null) 1f else 0.72f,
            animationSpec = tween(durationMillis = 620, easing = presetGlowEasing),
            label = "colorful-card-preset-scale",
        ).value
        val ui = LocalUiOptions.current
        // Accent is white for body text + slider, with a per-card override (from
        // EntityOverride.accentColor) winning when set. The gradient backdrop already
        // carries the colour identity so we don't need a coloured accent on top.
        // Per-card override wins, then the global accent picker, then white
        // (the readable default on every gradient backdrop).
        val accent = model.accentOverride
            ?: LocalThemeAccentOverride.current
            ?: Color.White

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Brush.linearGradient(animatedPalette))
                .then(
                    if (presetGlowAlpha > 0.01f) {
                        Modifier.drawBehind {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        animatedPresetColor.copy(alpha = 0.58f * presetGlowAlpha),
                                        animatedPresetColor.copy(alpha = 0.22f * presetGlowAlpha),
                                        Color.Transparent,
                                    ),
                                    center = Offset(size.width, size.height),
                                    radius = size.minDimension * 0.72f * presetGlowScale,
                                ),
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 22.dp, top = 18.dp, bottom = 18.dp, end = 18.dp),
            ) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(width = 14.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.9f)),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = domainLabel(model.domainGlyph),
                            style = R1.labelMicro,
                            color = Color.White,
                        )
                        if (ui.showAreaLabel && !model.area.isNullOrBlank()) {
                            Spacer(Modifier.width(8.dp))
                            Text("·", style = R1.labelMicro, color = Color.White.copy(alpha = 0.7f))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = model.area.replace('_', ' ').uppercase(),
                                style = R1.labelMicro,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = model.friendlyName,
                        style = R1.titleCard,
                        color = Color.White,
                        maxLines = 2,
                    )
                    // 'Last changed' relative-time label — parity with
                    // PragmaticHybridTheme. Localised composable so the ticker
                    // doesn't recompose the whole card on every interval.
                    if (model.lastChangedAt != null) {
                        Spacer(Modifier.height(2.dp))
                        com.github.itskenny0.r1ha.ui.components.RelativeTimeLabel(
                            at = model.lastChangedAt,
                            color = Color.White.copy(alpha = 0.65f),
                            style = R1.labelMicro,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    // Hide the giant percent readout on media_player cards with
                    // active now-playing — same parity rule as PragmaticHybridTheme
                    // and MinimalDarkTheme. The cover + title strip already
                    // dominate the card; '100 %' on top of them just compressed
                    // the now-playing into a sliver.
                    val hideBigReadoutForMedia = model.domainGlyph ==
                        CardRenderModel.Glyph.MEDIA_PLAYER &&
                        (!model.mediaTitle.isNullOrBlank() || !model.mediaPicture.isNullOrBlank())
                    if (!hideBigReadoutForMedia) {
                        BigReadout(
                            percent = model.percent,
                            showPercentSuffix = ui.displayMode == DisplayMode.PERCENT,
                            accent = accent,
                            overrideText = model.displayValue,
                            overrideUnit = model.displayUnit,
                            textSizeSp = model.textSizeSp,
                            lightEntityId = if (model.lightWheelMode != null) com.github.itskenny0.r1ha.core.ha.EntityId(model.entityIdText) else null,
                            lightWheelMode = model.lightWheelMode,
                        )
                    }
                    if (model.domainGlyph == CardRenderModel.Glyph.LIGHT) {
                        Spacer(Modifier.height(8.dp))
                        LightControlsRow(
                            entityId = com.github.itskenny0.r1ha.core.ha.EntityId(model.entityIdText),
                            currentMode = model.lightWheelMode
                                ?: com.github.itskenny0.r1ha.core.ha.LightWheelMode.BRIGHTNESS,
                            availableModes = model.lightAvailableModes,
                            currentEffect = model.lightEffect,
                            effectList = model.lightEffectList,
                            accent = accent,
                            hidden = model.lightButtonsHidden,
                            isOn = model.isOn,
                            onTapToggle = onTapToggle,
                        )
                    }
                    if (model.domainGlyph == CardRenderModel.Glyph.MEDIA_PLAYER) {
                        // Always render the now-playing block — the previous
                        // title-or-picture conditional left the block missing
                        // for the same entity on bigger screens where it
                        // rendered on R1. MediaNowPlayingCompact's internal
                        // null/blank skip keeps the empty-state clean.
                        Spacer(Modifier.height(10.dp))
                        com.github.itskenny0.r1ha.ui.components.MediaNowPlayingCompact(
                            title = model.mediaTitle,
                            artist = model.mediaArtist,
                            album = model.mediaAlbumName,
                            picture = model.mediaPicture,
                            durationSec = model.mediaDurationSec,
                            positionSec = model.mediaPositionSec,
                            positionUpdatedAt = model.mediaPositionUpdatedAt,
                            isPlaying = model.mediaIsPlaying,
                            accent = accent,
                        )
                        Spacer(Modifier.height(8.dp))
                        MediaControlsRow(
                            entityId = com.github.itskenny0.r1ha.core.ha.EntityId(model.entityIdText),
                            isPlaying = model.isOn,
                            accent = accent,
                            isMuted = model.mediaIsMuted,
                            supportedFeatures = model.mediaSupportedFeatures,
                        )
                        if (model.entityState != null) {
                            Spacer(Modifier.height(8.dp))
                            com.github.itskenny0.r1ha.ui.components.MediaExtrasPanel(
                                state = model.entityState,
                                accent = accent,
                            )
                        }
                    }
                    if (model.entityState != null) {
                        when (model.domainGlyph) {
                            CardRenderModel.Glyph.CLIMATE -> {
                                Spacer(Modifier.height(10.dp))
                                com.github.itskenny0.r1ha.ui.components.ClimatePanel(
                                    state = model.entityState,
                                    accent = accent,
                                )
                            }
                            CardRenderModel.Glyph.WATER_HEATER -> {
                                Spacer(Modifier.height(10.dp))
                                com.github.itskenny0.r1ha.ui.components.WaterHeaterPanel(
                                    state = model.entityState,
                                    accent = accent,
                                )
                            }
                            CardRenderModel.Glyph.VALVE -> {
                                Spacer(Modifier.height(10.dp))
                                com.github.itskenny0.r1ha.ui.components.ValvePanel(
                                    state = model.entityState,
                                    accent = accent,
                                )
                            }
                            else -> Unit
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (ui.showOnOffPill) {
                        Box(
                            modifier = Modifier
                                .clip(R1.ShapeRound)
                                .background(Color.Black.copy(alpha = 0.22f))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = if (model.isOn) "● ON" else "○ OFF",
                                style = R1.labelMicro,
                                color = Color.White,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(20.dp))
                val modelEntityId = com.github.itskenny0.r1ha.core.ha.EntityId(model.entityIdText)
                if (model.domainGlyph == CardRenderModel.Glyph.COVER) {
                    CoverPositionSlider(
                        entityId = modelEntityId,
                        percent = model.percent,
                        accent = accent,
                    )
                } else {
                    // Shared meter — touch-drag + clickable ticks. The accent passed here is
                    // white, which reads cleanly against the gradient backdrop. Climate /
                    // number cards still show their domain-native range via meterLabels; light
                    // HUE mode still renders the rainbow track.
                    VerticalTapeMeter(
                        entityId = modelEntityId,
                        percent = model.percent,
                        accent = accent,
                        tickLabels = model.meterLabels,
                        rainbow = model.lightWheelMode == com.github.itskenny0.r1ha.core.ha.LightWheelMode.HUE,
                        // Default tick colour (R1.InkMuted) is invisible against the
                        // colourful gradient; force a soft-white that reads on every
                        // palette in the theme.
                        tickLabelColor = Color.White.copy(alpha = 0.78f),
                    )
                }
            }
        }
    }
}
