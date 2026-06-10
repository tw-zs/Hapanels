package com.github.itskenny0.r1ha.feature.panelgrid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.itskenny0.r1ha.R
import com.github.itskenny0.r1ha.core.ha.EntityId
import com.github.itskenny0.r1ha.core.ha.EntityState
import com.github.itskenny0.r1ha.core.ha.HaRepository
import com.github.itskenny0.r1ha.core.theme.R1
import com.github.itskenny0.r1ha.ui.components.ChevronBack
import com.github.itskenny0.r1ha.ui.components.r1Pressable
import com.github.itskenny0.r1ha.ui.i18n.Text
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val PanelBg = Color(0xFF090D10)
private val TileBg = Color(0xFF23242D)
private val TileStroke = Color.White.copy(alpha = 0.035f)
private val Orange = Color(0xFFE99900)
private val Red = Color(0xFFFF5338)
private val Green = Color(0xFF58C56A)
private val Muted = Color(0xFF888C96)
private val NunitoPanelFont = FontFamily(
    Font(R.font.nunito_regular, weight = FontWeight.Normal),
    Font(R.font.nunito_bold, weight = FontWeight.Bold),
)

@Composable
fun PanelGridMockupScreen(haRepository: HaRepository, onBack: () -> Unit) {
    val cfg = LocalConfiguration.current
    val compact = cfg.screenWidthDp < 820
    val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
    val today = LocalDate.now()
    val dateText = buildString {
        append(today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("pl", "PL")))
        append(", ")
        append(today.format(DateTimeFormatter.ofPattern("dd MMMM", Locale("pl", "PL"))))
    }
    val context = LocalContext.current.applicationContext
    val config by produceState<HapanelsDashboardConfig?>(initialValue = null, key1 = context) {
        value = HapanelsDashboardConfigSource(context).loadOrSeed()
    }
    val observedEntityIds = remember(config) { config?.observableEntityIds() ?: emptySet() }
    val liveEntities by produceState<Map<EntityId, EntityState>>(
        initialValue = emptyMap(),
        key1 = haRepository,
        key2 = observedEntityIds,
    ) {
        if (observedEntityIds.isEmpty()) {
            value = emptyMap()
        } else {
            haRepository.observe(observedEntityIds).collect { value = it }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PanelBg)
            .systemBarsPadding(),
    ) {
        val loadedConfig = config
        if (loadedConfig == null) {
            LoadingPanelConfig()
        } else if (compact) {
            CompactPanel(config = loadedConfig, liveEntities = liveEntities, now = now, dateText = dateText)
        } else {
            WidePanel(config = loadedConfig, liveEntities = liveEntities, now = now, dateText = dateText)
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp),
        ) {
            ChevronBack(onClick = onBack)
        }
    }
}

@Composable
private fun LoadingPanelConfig() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "Ładowanie panelu...",
            color = Color.White.copy(alpha = 0.78f),
            style = R1.body.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = NunitoPanelFont,
            ),
        )
    }
}

@Composable
private fun WidePanel(
    config: HapanelsDashboardConfig,
    liveEntities: Map<EntityId, EntityState>,
    now: String,
    dateText: String,
) {
    val actionTiles = remember(config) { config.tilesBySize(HapanelsTileSize.ACTION) }
    val largeTiles = remember(config) { config.tilesBySize(HapanelsTileSize.LARGE) }
    val smallTiles = remember(config) { config.tilesBySize(HapanelsTileSize.SMALL) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 14.dp, top = 18.dp, end = 14.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PanelClockBlock(now = now, dateText = dateText, modifier = Modifier.weight(1f))
            PeopleGrid(config.people, modifier = Modifier.weight(1f).fillMaxHeight())
            Row(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                actionTiles.forEach { action ->
                    PanelActionTile(action, liveState = action.liveState(liveEntities), modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PanelLargeTileOrCamera(
                    tile = largeTiles[0],
                    liveState = largeTiles[0].liveState(liveEntities),
                    cameraActions = config.cameraActions,
                    modifier = Modifier.weight(1f),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PanelLargeTileOrCamera(
                        tile = largeTiles[3],
                        liveState = largeTiles[3].liveState(liveEntities),
                        cameraActions = config.cameraActions,
                        modifier = Modifier.weight(1f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PanelTextAction(config.cameraActions.getOrElse(0) { "Wyłącz kamery" }, modifier = Modifier.weight(1f))
                        PanelTextAction(config.cameraActions.getOrElse(1) { "Włącz kamery" }, modifier = Modifier.weight(1f))
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PanelLargeTileOrCamera(
                    tile = largeTiles[1],
                    liveState = largeTiles[1].liveState(liveEntities),
                    cameraActions = config.cameraActions,
                    modifier = Modifier.weight(1f),
                )
                PanelLargeTileOrCamera(
                    tile = largeTiles[4],
                    liveState = largeTiles[4].liveState(liveEntities),
                    cameraActions = config.cameraActions,
                    modifier = Modifier.weight(1f),
                )
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PanelLargeTileOrCamera(
                    tile = largeTiles[2],
                    liveState = largeTiles[2].liveState(liveEntities),
                    cameraActions = config.cameraActions,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PanelSmallTile(smallTiles[0], liveState = smallTiles[0].liveState(liveEntities), modifier = Modifier.weight(1f))
                        PanelSmallTile(smallTiles[2], liveState = smallTiles[2].liveState(liveEntities), modifier = Modifier.weight(1f))
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PanelSmallTile(smallTiles[1], liveState = smallTiles[1].liveState(liveEntities), modifier = Modifier.weight(1f))
                        PanelSmallTile(smallTiles[3], liveState = smallTiles[3].liveState(liveEntities), modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactPanel(
    config: HapanelsDashboardConfig,
    liveEntities: Map<EntityId, EntityState>,
    now: String,
    dateText: String,
) {
    val actionTiles = remember(config) { config.tilesBySize(HapanelsTileSize.ACTION) }
    val largeTiles = remember(config) { config.tilesBySize(HapanelsTileSize.LARGE) }
    val smallTiles = remember(config) { config.tilesBySize(HapanelsTileSize.SMALL) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PanelClockBlock(now = now, dateText = dateText, modifier = Modifier.fillMaxWidth().height(132.dp))
        PeopleGrid(config.people, modifier = Modifier.fillMaxWidth().height(122.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            actionTiles.forEach { action ->
                PanelActionTile(action, liveState = action.liveState(liveEntities), modifier = Modifier.weight(1f).height(112.dp), iconSize = 54.dp)
            }
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                largeTiles.take(3).forEach { tile ->
                    PanelLargeTileOrCamera(
                        tile = tile,
                        liveState = tile.liveState(liveEntities),
                        cameraActions = config.cameraActions,
                        modifier = Modifier.weight(1f),
                        iconSize = 60.dp,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                largeTiles.drop(3).forEach { tile ->
                    PanelLargeTileOrCamera(
                        tile = tile,
                        liveState = tile.liveState(liveEntities),
                        cameraActions = config.cameraActions,
                        modifier = Modifier.weight(1f),
                        iconSize = 60.dp,
                    )
                }
                PanelSmallTile(smallTiles[3], liveState = smallTiles[3].liveState(liveEntities), modifier = Modifier.weight(1f), iconSize = 48.dp)
            }
        }
    }
}

@Composable
private fun PanelClockBlock(now: String, dateText: String, modifier: Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            now,
            color = Color(0xFFF4F4F6),
            fontSize = 80.sp,
            fontWeight = FontWeight.Black,
            fontFamily = NunitoPanelFont,
            lineHeight = 80.sp,
        )
        Text(
            dateText,
            color = Color(0xFFF4F4F6),
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = NunitoPanelFont,
        )
    }
}

@Composable
private fun PeopleGrid(people: List<HapanelsPersonConfig>, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PanelPersonChip(people[0], modifier = Modifier.weight(1f))
            PanelPersonChip(people[1], modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PanelPersonChip(people[2], modifier = Modifier.weight(1f))
            PanelPersonChip(people[3], modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PanelPersonChip(person: HapanelsPersonConfig, modifier: Modifier) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(TileBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(person.status.color()),
            )
        }
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                person.name,
                color = Color.White,
                style = R1.body.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = NunitoPanelFont),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            Text(
                person.state,
                color = Color.White.copy(alpha = 0.84f),
                style = R1.labelMicro.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = NunitoPanelFont),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PanelLargeTile(
    tile: HapanelsTileConfig,
    liveState: EntityState?,
    modifier: Modifier,
    iconSize: Dp = 92.dp,
) {
    PanelTileShell(modifier = modifier) {
        PanelIcons.Icon(tile.icon, tint = tile.accent.color(), modifier = Modifier.size(iconSize))
        Spacer(Modifier.height(14.dp))
        Text(
            tile.displayLabel(),
            color = Color.White,
            style = R1.body.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, fontFamily = NunitoPanelFont),
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
        )
        PanelLiveStatus(tile = tile, liveState = liveState, compact = false)
    }
}

@Composable
private fun PanelLargeTileOrCamera(
    tile: HapanelsTileConfig,
    liveState: EntityState?,
    cameraActions: List<String>,
    modifier: Modifier,
    iconSize: Dp = 92.dp,
) {
    if (tile.kind == HapanelsTileKind.CAMERA) {
        PanelCameraTile(tile = tile, liveState = liveState, cameraActions = cameraActions, modifier = modifier, iconSize = iconSize)
    } else {
        PanelLargeTile(tile = tile, liveState = liveState, modifier = modifier, iconSize = iconSize)
    }
}

@Composable
private fun PanelCameraTile(
    tile: HapanelsTileConfig,
    liveState: EntityState?,
    cameraActions: List<String>,
    modifier: Modifier,
    iconSize: Dp = 92.dp,
) {
    PanelTileShell(modifier = modifier, padding = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PanelIcons.Icon(tile.icon, tint = tile.accent.color(), modifier = Modifier.size(iconSize))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tile.displayLabel(),
                    color = Color.White,
                    style = R1.body.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, fontFamily = NunitoPanelFont),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                Text(
                    text = "Podgląd na żywo",
                    color = Color.White.copy(alpha = 0.72f),
                    style = R1.labelMicro.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = NunitoPanelFont),
                )
            }
            val cameraState = when {
                liveState == null -> "PODGLĄD"
                !liveState.isAvailable -> "NIEDOSTĘPNA"
                else -> liveState.rawState?.uppercase(Locale.getDefault())?.takeIf { it.isNotBlank() } ?: "PODGLĄD"
            }
            Text(
                text = cameraState,
                color = when {
                    liveState == null -> Orange
                    !liveState.isAvailable -> Red
                    else -> Green
                },
                style = R1.labelMicro.copy(fontSize = 12.sp, fontFamily = NunitoPanelFont, fontWeight = FontWeight.Bold),
                textAlign = TextAlign.End,
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.18f))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PanelIcons.Icon(HapanelsPanelIcon.CCTV, tint = Orange, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "LIVE SNAPSHOT",
                    color = Color.White.copy(alpha = 0.84f),
                    style = R1.labelMicro.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = NunitoPanelFont),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PanelTextAction(cameraActions.getOrElse(0) { "Lista kamer" }, modifier = Modifier.weight(1f))
            PanelTextAction(cameraActions.getOrElse(1) { "Pełny ekran" }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PanelSmallTile(
    tile: HapanelsTileConfig,
    liveState: EntityState?,
    modifier: Modifier,
    iconSize: Dp = 50.dp,
) {
    PanelTileShell(modifier = modifier, padding = 10.dp) {
        PanelIcons.Icon(tile.icon, tint = tile.accent.color(), modifier = Modifier.size(iconSize))
        Spacer(Modifier.height(7.dp))
        Text(
            tile.displayLabel(),
            color = Color.White,
            style = R1.body.copy(
                fontSize = 14.sp,
                fontFamily = NunitoPanelFont,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
                lineHeight = 16.sp,
            ),
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
        )
        PanelLiveStatus(tile = tile, liveState = liveState, compact = true)
    }
}

@Composable
private fun PanelActionTile(
    action: HapanelsTileConfig,
    liveState: EntityState?,
    modifier: Modifier,
    iconSize: Dp = 68.dp,
) {
    PanelTileShell(modifier = modifier, padding = 12.dp) {
        PanelIcons.Icon(action.icon, tint = action.accent.color(), modifier = Modifier.size(iconSize))
        Spacer(Modifier.height(10.dp))
        Text(
            action.displayLabel(),
            color = Color.White,
            style = R1.body.copy(
                fontSize = 15.sp,
                fontFamily = NunitoPanelFont,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
                lineHeight = 17.sp,
            ),
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
        )
        PanelLiveStatus(tile = action, liveState = liveState, compact = true)
    }
}

@Composable
private fun PanelLiveStatus(tile: HapanelsTileConfig, liveState: EntityState?, compact: Boolean) {
    val label = tile.liveLabel(liveState) ?: return
    Spacer(Modifier.height(if (compact) 4.dp else 6.dp))
    Text(
        label,
        color = liveState.statusColor(),
        style = R1.labelMicro.copy(
            fontSize = if (compact) 10.sp else 12.sp,
            fontFamily = NunitoPanelFont,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
        ),
        textAlign = TextAlign.Center,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
    )
}

@Composable
private fun PanelTextAction(label: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(TileBg)
            .r1Pressable(onClick = {})
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = Color.White,
            style = R1.body.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = NunitoPanelFont),
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

@Composable
private fun PanelTileShell(
    modifier: Modifier,
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TileBg)
            .border(1.dp, TileStroke, RoundedCornerShape(18.dp))
            .r1Pressable(onClick = {})
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

private fun HapanelsDashboardConfig.tilesBySize(size: HapanelsTileSize): List<HapanelsTileConfig> =
    tiles.filter { it.size == size }.sortedBy { it.order }

private fun HapanelsDashboardConfig.observableEntityIds(): Set<EntityId> =
    tiles.mapNotNull { it.entityId.toEntityIdOrNull() }.toSet()

private fun String?.toEntityIdOrNull(): EntityId? =
    this?.takeIf { it.isNotBlank() }?.let { runCatching { EntityId(it) }.getOrNull() }

private fun HapanelsTileConfig.liveState(liveEntities: Map<EntityId, EntityState>): EntityState? =
    entityId.toEntityIdOrNull()?.let(liveEntities::get)

private fun HapanelsTileConfig.liveLabel(liveState: EntityState?): String? {
    if (entityId.isNullOrBlank()) return null
    val entity = entityId.toEntityIdOrNull() ?: return "niewspierane"
    val state = liveState ?: return "oczekiwanie"
    if (!state.isAvailable) return "niedostępne"
    return when (entity.domain) {
        com.github.itskenny0.r1ha.core.ha.Domain.LIGHT,
        com.github.itskenny0.r1ha.core.ha.Domain.SWITCH,
        com.github.itskenny0.r1ha.core.ha.Domain.INPUT_BOOLEAN,
        com.github.itskenny0.r1ha.core.ha.Domain.AUTOMATION,
        -> if (state.isOn) "włączone" else "wyłączone"
        com.github.itskenny0.r1ha.core.ha.Domain.COVER,
        com.github.itskenny0.r1ha.core.ha.Domain.VALVE,
        -> state.percent?.let { "$it%" } ?: state.rawState.orUnknown()
        com.github.itskenny0.r1ha.core.ha.Domain.CLIMATE,
        com.github.itskenny0.r1ha.core.ha.Domain.WATER_HEATER,
        -> state.climateCurrentTemperature?.let { "${it.formatPanelNumber()}°" }
            ?: state.climateTargetTemperature?.let { "${it.formatPanelNumber()}°" }
            ?: state.climateHvacMode.orUnknown()
        com.github.itskenny0.r1ha.core.ha.Domain.MEDIA_PLAYER,
        -> state.rawState?.uppercase(Locale.getDefault())?.takeIf { it.isNotBlank() } ?: "nieznane"
        com.github.itskenny0.r1ha.core.ha.Domain.SENSOR,
        com.github.itskenny0.r1ha.core.ha.Domain.NUMBER,
        com.github.itskenny0.r1ha.core.ha.Domain.INPUT_NUMBER,
        -> listOfNotNull(state.rawState, state.unit).joinToString(" ").ifBlank { "nieznane" }
        else -> state.rawState.orUnknown()
    }
}

private fun String?.orUnknown(): String = this?.takeIf { it.isNotBlank() } ?: "nieznane"

private fun Double.formatPanelNumber(): String =
    if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.1f", this)

private fun EntityState?.statusColor(): Color = when {
    this == null -> Muted
    !isAvailable -> Red
    isOn -> Green
    else -> Color.White.copy(alpha = 0.62f)
}

private fun HapanelsTileConfig.displayLabel(): String =
    shortLabel?.takeIf { it.isNotBlank() } ?: label

private fun HapanelsPersonStatus.color(): Color = when (this) {
    HapanelsPersonStatus.HOME -> Green
    HapanelsPersonStatus.AWAY -> Red
    HapanelsPersonStatus.UNKNOWN -> Muted
}

private fun HapanelsTileAccent.color(): Color = when (this) {
    HapanelsTileAccent.ORANGE -> Orange
    HapanelsTileAccent.RED -> Red
    HapanelsTileAccent.WHITE -> Color.White.copy(alpha = 0.86f)
}

private object PanelIcons {
    @Composable
    fun Icon(icon: HapanelsPanelIcon, tint: Color, modifier: Modifier = Modifier) {
        val pathData = when (icon) {
            HapanelsPanelIcon.LIGHTBULB -> LIGHTBULB
            HapanelsPanelIcon.LIGHTBULB_OFF -> LIGHTBULB_OFF
            HapanelsPanelIcon.SHIELD_LOCK -> SHIELD_LOCK
            HapanelsPanelIcon.BLINDS -> BLINDS
            HapanelsPanelIcon.HOME_THERMOMETER -> HOME_THERMOMETER
            HapanelsPanelIcon.CCTV -> CCTV
            HapanelsPanelIcon.GATE -> GATE
            HapanelsPanelIcon.HOME_LIGHTNING -> HOME_LIGHTNING
            HapanelsPanelIcon.MOTION_SENSOR -> MOTION_SENSOR
            HapanelsPanelIcon.SPRINKLER -> SPRINKLER
            HapanelsPanelIcon.COG -> COG
        }
        val path = remember(pathData) { PathParser().parsePathString(pathData).toPath() }
        Canvas(modifier = modifier) {
            val scale = minOf(size.width, size.height) / 24f
            val dx = (size.width - 24f * scale) / 2f
            val dy = (size.height - 24f * scale) / 2f
            withTransform({
                translate(left = dx, top = dy)
                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            }) {
                drawPath(path = path, color = tint)
            }
        }
    }

    private const val LIGHTBULB = "M12,2A7,7 0,0 0,5 9C5,11.38 6.19,13.47 8,14.74V17A1,1 0,0 0,9 18H15A1,1 0,0 0,16 17V14.74C17.81,13.47 19,11.38 19,9A7,7 0,0 0,12 2M9,21A1,1 0,0 0,10 22H14A1,1 0,0 0,15 21V20H9V21Z"
    private const val LIGHTBULB_OFF = "M2.81,2.81L1.39,4.22L6.83,9.66C7.06,11.73 8.21,13.55 10,14.74V17A1,1 0,0 0,11 18H14.17L19.78,23.61L21.19,22.2L2.81,2.81M12,2A7,7 0,0 0,7.79 3.4L17.6,13.21C18.48,12.03 19,10.57 19,9A7,7 0,0 0,12 2M11,21A1,1 0,0 0,12 22H14A1,1 0,0 0,15 21V20H11V21Z"
    private const val SHIELD_LOCK = "M12,1L3,5V11C3,16.55 6.84,21.74 12,23C17.16,21.74 21,16.55 21,11V5L12,1M12,7A2,2 0,0 1,14 9V10H15V16H9V10H10V9A2,2 0,0 1,12 7M12,8.2A0.8,0.8 0,0 0,11.2 9V10H12.8V9A0.8,0.8 0,0 0,12 8.2Z"
    private const val BLINDS = "M5,4H19V6H5V4M5,8H19V10H5V8M5,12H19V14H5V12M5,16H19V18H5V16M4,20H20V22H4V20Z"
    private const val HOME_THERMOMETER = "M10,20V14H14V20H19V11H21L12,3L3,11H5V20H10M16,4A2,2 0,0 1,18 6V12.1A4,4 0,1 1,14 12.1V6A2,2 0,0 1,16 4M15.2,6V13L14.7,13.4A2,2 0,1 0,17.3 13.4L16.8,13V6H15.2Z"
    private const val CCTV = "M17,10.5V7A1,1 0,0 0,16 6H4A1,1 0,0 0,3 7V17A1,1 0,0 0,4 18H16A1,1 0,0 0,17 17V13.5L21,17V7L17,10.5Z"
    private const val GATE = "M4,20V10L12,4L20,10V20H18V11.2L12,6.7L6,11.2V20H4M8,20V13H10V20H8M11,20V13H13V20H11M14,20V13H16V20H14Z"
    private const val HOME_LIGHTNING = "M12,3L2,12H5V20H19V12H22L12,3M11,18V14H8L13,7V12H16L11,18Z"
    private const val MOTION_SENSOR = "M12,6A3,3 0,1 0,12,12A3,3 0,0 0,12,6M5,10A7,7 0,0 1,12,3V5A5,5 0,0 0,7,10H5M19,10H17A5,5 0,0 0,12,5V3A7,7 0,0 1,19,10M7,14A5,5 0,0 0,12,19V21A7,7 0,0 1,5,14H7M17,14H19A7,7 0,0 1,12,21V19A5,5 0,0 0,17,14Z"
    private const val SPRINKLER = "M7,4H17V6H7V4M11,6H13V11H18A2,2 0,0 1,20 13V15H18V13H6V15H4V13A2,2 0,0 1,6 11H11V6M7,17H9V20H7V17M11,17H13V22H11V17M15,17H17V20H15V17Z"
    private const val COG = "M12,8A4,4 0,1 0,12,16A4,4 0,0 0,12,8M21,13V11L18.9,10.65C18.75,10.05 18.5,9.5 18.18,9L19.42,7.26L17.74,5.58L16,6.82C15.5,6.5 14.95,6.25 14.35,6.1L14,4H10L9.65,6.1C9.05,6.25 8.5,6.5 8,6.82L6.26,5.58L4.58,7.26L5.82,9C5.5,9.5 5.25,10.05 5.1,10.65L3,11V13L5.1,13.35C5.25,13.95 5.5,14.5 5.82,15L4.58,16.74L6.26,18.42L8,17.18C8.5,17.5 9.05,17.75 9.65,17.9L10,20H14L14.35,17.9C14.95,17.75 15.5,17.5 16,17.18L17.74,18.42L19.42,16.74L18.18,15C18.5,14.5 18.75,13.95 18.9,13.35L21,13Z"
}
