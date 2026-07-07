package com.github.itskenny0.r1ha.feature.panelgrid

import android.content.Context
import android.content.res.Configuration
import android.os.FileObserver
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
import com.github.itskenny0.r1ha.core.ha.ServiceCall
import com.github.itskenny0.r1ha.core.theme.R1
import com.github.itskenny0.r1ha.ui.components.ChevronBack
import com.github.itskenny0.r1ha.ui.components.r1Pressable
import com.github.itskenny0.r1ha.ui.i18n.Text
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.math.abs
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import org.json.JSONObject

private val LocalHapanelsTheme = staticCompositionLocalOf { defaultHapanelsThemeConfig.resolveHapanelsThemeColors() }
private val NunitoPanelFont = FontFamily(
    Font(R.font.nunito_regular, weight = FontWeight.Normal),
    Font(R.font.nunito_bold, weight = FontWeight.Bold),
)

private data class HapanelsPanelRenderConfig(
    val dashboard: HapanelsDashboardConfig,
    val theme: HapanelsThemeColors,
)

@Composable
fun PanelGridMockupScreen(
    haRepository: HaRepository,
    dashboardConfigSource: HapanelsDashboardConfigSource,
    onBack: () -> Unit,
) {
    val cfg = LocalConfiguration.current
    val compact = cfg.screenWidthDp < 820
    val systemDark = (cfg.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
    val today = LocalDate.now()
    val dateText = buildString {
        append(today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("pl", "PL")))
        append(", ")
        append(today.format(DateTimeFormatter.ofPattern("dd MMMM", Locale("pl", "PL"))))
    }
    val context = LocalContext.current.applicationContext
    val renderConfig by produceState<HapanelsPanelRenderConfig?>(initialValue = null, key1 = context, key2 = systemDark) {
        val changes = Channel<Unit>(Channel.CONFLATED)
        val observer = object : FileObserver(context.filesDir.absolutePath, CLOSE_WRITE or CREATE or MOVED_TO) {
            override fun onEvent(event: Int, path: String?) {
                if (path == "hapanels_dashboard_config.json") changes.trySend(Unit)
            }
        }
        observer.startWatching()
        val sourceJob = launch { dashboardConfigSource.changes.collect { changes.trySend(Unit) } }
        suspend fun loadRenderConfig(): HapanelsPanelRenderConfig {
            val dashboard = dashboardConfigSource.loadOrSeed()
            val raw = runCatching { context.filesDir.resolve("hapanels_dashboard_config.json").readText() }.getOrDefault("")
            return HapanelsPanelRenderConfig(dashboard, raw.toHapanelsPanelTheme(systemDark))
        }
        try {
            value = loadRenderConfig()
            for (ignored in changes) value = loadRenderConfig()
        } finally {
            sourceJob.cancel()
            observer.stopWatching()
            changes.close()
        }
    }
    val config = renderConfig?.dashboard
    val observedEntityIds = remember(config) { config?.observableEntityIds() ?: emptySet() }
    var currentPanelId by remember(config) { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var currentPanelTitle by remember(config) { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var popupTile by remember(config) { androidx.compose.runtime.mutableStateOf<HapanelsTileConfig?>(null) }
    var popupOpen by remember(config) { androidx.compose.runtime.mutableStateOf(false) }
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
    val scope = rememberCoroutineScope()
    val onSetCoverPercent = remember(haRepository) {
        { tile: HapanelsTileConfig, percent: Int ->
            tile.entityId.toEntityIdOrNull()?.let { entityId ->
                scope.launch { haRepository.call(ServiceCall.setPercent(entityId, percent)) }
            }
            Unit
        }
    }
    val onTileClick = remember(haRepository, liveEntities) {
        { tile: HapanelsTileConfig ->
            when (tile.kind) {
                HapanelsTileKind.FOLDER -> {
                    currentPanelId = tile.panelId?.takeIf { it.isNotBlank() }
                    currentPanelTitle = tile.displayLabel()
                }
                HapanelsTileKind.POPUP -> { popupTile = tile; popupOpen = true }
                else -> tile.tapAction(liveEntities)?.let { call -> scope.launch { haRepository.call(call) } }
            }
            Unit
        }
    }

    CompositionLocalProvider(LocalHapanelsTheme provides (renderConfig?.theme ?: defaultHapanelsThemeConfig.resolveHapanelsThemeColors())) {
        val theme = LocalHapanelsTheme.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.background)
                .systemBarsPadding(),
        ) {
            val loadedConfig = config
            val panelConfig = loadedConfig?.forPanel(currentPanelId)
            Box(modifier = Modifier.fillMaxSize()) {
                if (loadedConfig == null) {
                    LoadingPanelConfig()
                } else if (compact) {
                    CompactPanel(config = panelConfig!!, liveEntities = liveEntities, now = now, dateText = dateText, isSubPanel = currentPanelId != null, onTileClick = onTileClick, onSetCoverPercent = onSetCoverPercent)
                } else {
                    WidePanel(config = panelConfig!!, liveEntities = liveEntities, now = now, dateText = dateText, isSubPanel = currentPanelId != null, onTileClick = onTileClick, onSetCoverPercent = onSetCoverPercent)
                }
                if (currentPanelTitle != null) {
                    Text(
                        text = currentPanelTitle!!,
                        color = theme.textPrimary,
                        style = R1.body.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = NunitoPanelFont),
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                ) {
                    ChevronBack(onClick = { if (currentPanelId != null) { currentPanelId = null; currentPanelTitle = null } else onBack() })
                }
            }
            if (loadedConfig != null && popupTile != null) {
                val tile = popupTile!!
                val tiles = loadedConfig.popupTiles(tile)
                AnimatedVisibility(
                    visible = popupOpen,
                    enter = slideInVertically(animationSpec = tween(180), initialOffsetY = { it / 18 }) + fadeIn(tween(120)) + scaleIn(initialScale = 0.965f, animationSpec = tween(180)),
                    exit = slideOutVertically(animationSpec = tween(140), targetOffsetY = { it / 24 }) + fadeOut(tween(90)) + scaleOut(targetScale = 0.985f, animationSpec = tween(140)),
                ) {
                    PanelPopup(
                        tile = tile,
                        tiles = tiles,
                        liveEntities = liveEntities,
                        now = now,
                        dateText = dateText,
                        onTileClick = onTileClick,
                        onSetCoverPercent = onSetCoverPercent,
                        onClose = { popupOpen = false },
                    )
                }
                LaunchedEffect(popupOpen) {
                    if (!popupOpen) {
                        kotlinx.coroutines.delay(180)
                        if (!popupOpen) popupTile = null
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingPanelConfig() {
    val theme = LocalHapanelsTheme.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "Ładowanie panelu...",
            color = theme.textPrimary.copy(alpha = 0.78f),
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
    isSubPanel: Boolean,
    onTileClick: (HapanelsTileConfig) -> Unit,
    onSetCoverPercent: (HapanelsTileConfig, Int) -> Unit,
) {
    if (isSubPanel && config.tiles.isEmpty()) {
        EmptyPanelMessage()
        return
    }
    if (isSubPanel && config.tiles.none { it.hasGridCell() }) {
        PanelSubTiles(config.tiles, liveEntities, onTileClick, onSetCoverPercent)
        return
    }
    if (config.tiles.any { it.hasGridCell() }) {
        WideGridPanel(config = config, liveEntities = liveEntities, now = now, dateText = dateText, onTileClick = onTileClick, onSetCoverPercent = onSetCoverPercent)
        return
    }
    val actionTiles = remember(config) { config.tilesBySize(HapanelsTileSize.ACTION) }
    val largeTiles = remember(config) { config.tilesBySize(HapanelsTileSize.LARGE).padPanelTiles(HapanelsTileSize.LARGE, 5) }
    val smallTiles = remember(config) { config.tilesBySize(HapanelsTileSize.SMALL).padPanelTiles(HapanelsTileSize.SMALL, 4) }
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
                    PanelActionTile(action, liveState = action.liveState(liveEntities), modifier = Modifier.weight(1f).fillMaxHeight(), onClick = { onTileClick(action) }, onSetPercent = { onSetCoverPercent(action, it) })
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
                    onClick = { onTileClick(largeTiles[0]) },
                    onSetPercent = { onSetCoverPercent(largeTiles[0], it) },
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PanelLargeTileOrCamera(
                        tile = largeTiles[3],
                        liveState = largeTiles[3].liveState(liveEntities),
                        cameraActions = config.cameraActions,
                        modifier = Modifier.weight(1f),
                        onClick = { onTileClick(largeTiles[3]) },
                        onSetPercent = { onSetCoverPercent(largeTiles[3], it) },
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
                    onClick = { onTileClick(largeTiles[1]) },
                    onSetPercent = { onSetCoverPercent(largeTiles[1], it) },
                )
                PanelLargeTileOrCamera(
                    tile = largeTiles[4],
                    liveState = largeTiles[4].liveState(liveEntities),
                    cameraActions = config.cameraActions,
                    modifier = Modifier.weight(1f),
                    onClick = { onTileClick(largeTiles[4]) },
                    onSetPercent = { onSetCoverPercent(largeTiles[4], it) },
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
                    onClick = { onTileClick(largeTiles[2]) },
                    onSetPercent = { onSetCoverPercent(largeTiles[2], it) },
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PanelSmallTile(smallTiles[0], liveState = smallTiles[0].liveState(liveEntities), modifier = Modifier.weight(1f), onClick = { onTileClick(smallTiles[0]) }, onSetPercent = { onSetCoverPercent(smallTiles[0], it) })
                        PanelSmallTile(smallTiles[2], liveState = smallTiles[2].liveState(liveEntities), modifier = Modifier.weight(1f), onClick = { onTileClick(smallTiles[2]) }, onSetPercent = { onSetCoverPercent(smallTiles[2], it) })
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PanelSmallTile(smallTiles[1], liveState = smallTiles[1].liveState(liveEntities), modifier = Modifier.weight(1f), onClick = { onTileClick(smallTiles[1]) }, onSetPercent = { onSetCoverPercent(smallTiles[1], it) })
                        PanelSmallTile(smallTiles[3], liveState = smallTiles[3].liveState(liveEntities), modifier = Modifier.weight(1f), onClick = { onTileClick(smallTiles[3]) }, onSetPercent = { onSetCoverPercent(smallTiles[3], it) })
                    }
                }
            }
        }
    }
}

@Composable
private fun WideGridPanel(
    config: HapanelsDashboardConfig,
    liveEntities: Map<EntityId, EntityState>,
    now: String,
    dateText: String,
    onTileClick: (HapanelsTileConfig) -> Unit,
    onSetCoverPercent: (HapanelsTileConfig, Int) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 14.dp, top = 18.dp, end = 14.dp, bottom = 18.dp),
    ) {
        val columns = 12
        val rows = 9
        val gap = 8.dp
        val cellWidth = (maxWidth - gap * (columns - 1)) / columns
        val cellHeight = (maxHeight - gap * (rows - 1)) / rows
        val customGrid = config.tiles.any { it.col != null && it.row != null }

        if (!customGrid) {
            PanelClockBlock(now = now, dateText = dateText, modifier = Modifier.gridCell(1, 1, 4, 2, cellWidth, cellHeight, gap))
            PeopleGrid(config.people, modifier = Modifier.gridCell(5, 1, 4, 2, cellWidth, cellHeight, gap))
        }
        config.tiles.sortedBy { it.order }.forEach { tile ->
            val modifier = Modifier.gridCell(
                col = tile.col ?: return@forEach,
                row = tile.row ?: return@forEach,
                colSpan = tile.colSpan ?: 1,
                rowSpan = tile.rowSpan ?: 1,
                cellWidth = cellWidth,
                cellHeight = cellHeight,
                gap = gap,
            )
            when (tile.size) {
                HapanelsTileSize.ACTION -> PanelActionTile(tile, liveState = tile.liveState(liveEntities), modifier = modifier, onClick = { onTileClick(tile) }, onSetPercent = { onSetCoverPercent(tile, it) })
                HapanelsTileSize.SMALL -> PanelSmallTile(tile, liveState = tile.liveState(liveEntities), modifier = modifier, onClick = { onTileClick(tile) }, onSetPercent = { onSetCoverPercent(tile, it) })
                HapanelsTileSize.LARGE -> PanelLargeTileOrCamera(
                    tile = tile,
                    liveState = tile.liveState(liveEntities),
                    cameraActions = config.cameraActions,
                    modifier = modifier,
                    now = now,
                    dateText = dateText,
                    onClick = { onTileClick(tile) },
                    onSetPercent = { onSetCoverPercent(tile, it) },
                )
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
    isSubPanel: Boolean,
    onTileClick: (HapanelsTileConfig) -> Unit,
    onSetCoverPercent: (HapanelsTileConfig, Int) -> Unit,
) {
    if (isSubPanel && config.tiles.isEmpty()) {
        EmptyPanelMessage()
        return
    }
    if (isSubPanel && config.tiles.none { it.hasGridCell() }) {
        PanelSubTiles(config.tiles, liveEntities, onTileClick, onSetCoverPercent)
        return
    }
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
                PanelActionTile(action, liveState = action.liveState(liveEntities), modifier = Modifier.weight(1f).height(112.dp), iconSize = 54.dp, onClick = { onTileClick(action) }, onSetPercent = { onSetCoverPercent(action, it) })
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
                        now = now,
                        dateText = dateText,
                        onClick = { onTileClick(tile) },
                        onSetPercent = { onSetCoverPercent(tile, it) },
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
                        now = now,
                        dateText = dateText,
                        onClick = { onTileClick(tile) },
                        onSetPercent = { onSetCoverPercent(tile, it) },
                    )
                }
                PanelSmallTile(smallTiles[3], liveState = smallTiles[3].liveState(liveEntities), modifier = Modifier.weight(1f), iconSize = 48.dp, onClick = { onTileClick(smallTiles[3]) }, onSetPercent = { onSetCoverPercent(smallTiles[3], it) })
            }
        }
    }
}

@Composable
private fun EmptyPanelMessage() {
    val theme = LocalHapanelsTheme.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "Brak elementów. Dodaj je w Hapanels Studio.",
            color = theme.textPrimary.copy(alpha = 0.78f),
            style = R1.body.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = NunitoPanelFont),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PanelSubTiles(
    tiles: List<HapanelsTileConfig>,
    liveEntities: Map<EntityId, EntityState>,
    onTileClick: (HapanelsTileConfig) -> Unit,
    onSetCoverPercent: (HapanelsTileConfig, Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 72.dp, vertical = 72.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        tiles.sortedBy { it.order }.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { tile ->
                    PanelActionTile(tile, liveState = tile.liveState(liveEntities), modifier = Modifier.weight(1f).height(126.dp), iconSize = 56.dp, onClick = { onTileClick(tile) }, onSetPercent = { onSetCoverPercent(tile, it) })
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PanelClockBlock(now: String, dateText: String, modifier: Modifier, style: String? = null) {
    val theme = LocalHapanelsTheme.current
    val dateFirst = style == "date_top"
    val compact = style == "compact"
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (dateFirst) ClockDateText(dateText)
        Text(
            now,
            color = theme.textPrimary,
            fontSize = if (compact) 58.sp else 80.sp,
            fontWeight = FontWeight.Black,
            fontFamily = NunitoPanelFont,
            lineHeight = if (compact) 58.sp else 72.sp,
        )
        if (!dateFirst) ClockDateText(dateText)
    }
}

@Composable
private fun ClockDateText(dateText: String) {
    val theme = LocalHapanelsTheme.current
    Text(
        dateText,
        color = theme.textPrimary,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = NunitoPanelFont,
        lineHeight = 22.sp,
    )
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
    val theme = LocalHapanelsTheme.current
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(theme.tileBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(theme.textPrimary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(person.status.color(theme)),
            )
        }
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                person.name,
                color = theme.textPrimary,
                style = R1.body.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = NunitoPanelFont),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            Text(
                person.state,
                color = theme.textPrimary.copy(alpha = 0.84f),
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
    onClick: () -> Unit,
    onSetPercent: (Int) -> Unit = {},
) {
    val theme = LocalHapanelsTheme.current
    if (tile.kind == HapanelsTileKind.COVER) {
        PanelCoverTile(tile = tile, liveState = liveState, modifier = modifier, onClick = onClick, onSetPercent = onSetPercent)
        return
    }
    PanelTileShell(modifier = modifier, onClick = onClick) {
        PanelIcons.Icon(tile.icon, tint = tile.accent.color(theme), modifier = Modifier.size(iconSize))
        Spacer(Modifier.height(14.dp))
        Text(
            tile.displayLabel(),
            color = theme.textPrimary,
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
    now: String = "",
    dateText: String = "",
    onClick: () -> Unit,
    onSetPercent: (Int) -> Unit = {},
) {
    when (tile.kind) {
        HapanelsTileKind.CLOCK -> PanelClockBlock(now = now.ifBlank { "--:--" }, dateText = dateText, modifier = modifier, style = tile.clockStyle)
        HapanelsTileKind.CAMERA -> PanelCameraTile(tile = tile, liveState = liveState, cameraActions = cameraActions, modifier = modifier, iconSize = iconSize, onClick = onClick)
        HapanelsTileKind.COVER -> PanelCoverTile(tile = tile, liveState = liveState, modifier = modifier, onClick = onClick, onSetPercent = onSetPercent)
        else -> PanelLargeTile(tile = tile, liveState = liveState, modifier = modifier, iconSize = iconSize, onClick = onClick)
    }
}

@Composable
private fun PanelPopup(
    tile: HapanelsTileConfig,
    tiles: List<HapanelsTileConfig>,
    liveEntities: Map<EntityId, EntityState>,
    now: String,
    dateText: String,
    onTileClick: (HapanelsTileConfig) -> Unit,
    onSetCoverPercent: (HapanelsTileConfig, Int) -> Unit,
    onClose: () -> Unit,
) {
    val theme = LocalHapanelsTheme.current
    Box(
        modifier = Modifier.fillMaxSize().background(theme.popupOverlay).r1Pressable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.70f)
                .fillMaxHeight(0.70f)
                .clip(RoundedCornerShape(28.dp))
                .background(theme.popupBackground)
                .border(1.dp, theme.tileStroke.copy(alpha = (theme.tileStroke.alpha * 2.85f).coerceAtMost(1f)), RoundedCornerShape(28.dp))
                .r1Pressable(onClick = { }),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(tile.displayLabel(), color = theme.textPrimary, style = R1.body.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = NunitoPanelFont))
                if (tiles.isEmpty()) {
                    Text("Brak kafli w popupie. W Studio ustaw Panel na: ${tile.panelId.orEmpty()}", color = theme.textMuted, style = R1.body)
                } else if (tiles.any { it.hasGridCell() }) {
                    PopupGrid(tiles, liveEntities, now, dateText, Modifier.weight(1f).fillMaxWidth(), onTileClick, onSetCoverPercent)
                } else {
                    tiles.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { item ->
                                PanelActionTile(item, liveState = item.liveState(liveEntities), modifier = Modifier.weight(1f).height(118.dp), iconSize = 52.dp, onClick = { onTileClick(item) }, onSetPercent = { onSetCoverPercent(item, it) })
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PopupGrid(
    tiles: List<HapanelsTileConfig>,
    liveEntities: Map<EntityId, EntityState>,
    now: String,
    dateText: String,
    modifier: Modifier,
    onTileClick: (HapanelsTileConfig) -> Unit,
    onSetCoverPercent: (HapanelsTileConfig, Int) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val columns = 12
        val rows = 9
        val gap = 8.dp
        val cellWidth = (maxWidth - gap * (columns - 1)) / columns
        val cellHeight = (maxHeight - gap * (rows - 1)) / rows
        tiles.sortedBy { it.order }.forEach { item ->
            val itemModifier = Modifier.gridCell(item.col ?: return@forEach, item.row ?: return@forEach, item.colSpan ?: 1, item.rowSpan ?: 1, cellWidth, cellHeight, gap)
            when (item.size) {
                HapanelsTileSize.ACTION -> PanelActionTile(item, liveState = item.liveState(liveEntities), modifier = itemModifier, onClick = { onTileClick(item) }, onSetPercent = { onSetCoverPercent(item, it) })
                HapanelsTileSize.SMALL -> PanelSmallTile(item, liveState = item.liveState(liveEntities), modifier = itemModifier, onClick = { onTileClick(item) }, onSetPercent = { onSetCoverPercent(item, it) })
                HapanelsTileSize.LARGE -> PanelLargeTileOrCamera(item, liveState = item.liveState(liveEntities), cameraActions = emptyList(), modifier = itemModifier, now = now, dateText = dateText, onClick = { onTileClick(item) }, onSetPercent = { onSetCoverPercent(item, it) })
            }
        }
    }
}

@Composable
private fun PanelCameraTile(
    tile: HapanelsTileConfig,
    liveState: EntityState?,
    cameraActions: List<String>,
    modifier: Modifier,
    iconSize: Dp = 92.dp,
    onClick: () -> Unit,
) {
    val theme = LocalHapanelsTheme.current
    PanelTileShell(modifier = modifier, padding = 12.dp, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PanelIcons.Icon(tile.icon, tint = tile.accent.color(theme), modifier = Modifier.size(iconSize))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tile.displayLabel(),
                    color = theme.textPrimary,
                    style = R1.body.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, fontFamily = NunitoPanelFont),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                Text(
                    text = "Podgląd na żywo",
                    color = theme.textPrimary.copy(alpha = 0.72f),
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
                    liveState == null -> theme.accentOrange
                    !liveState.isAvailable -> theme.accentRed
                    else -> theme.accentGreen
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
                .border(1.dp, theme.tileStroke, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PanelIcons.Icon("mdi:cctv", tint = theme.accentOrange, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "LIVE SNAPSHOT",
                    color = theme.textPrimary.copy(alpha = 0.84f),
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
private fun PanelCoverTile(
    tile: HapanelsTileConfig,
    liveState: EntityState?,
    modifier: Modifier,
    compact: Boolean = false,
    onClick: () -> Unit,
    onSetPercent: (Int) -> Unit,
) {
    val theme = LocalHapanelsTheme.current
    val target = ((liveState?.percent ?: if (liveState?.isOn == true) 100 else 0).coerceIn(0, 100)) / 100f
    val openFraction by animateFloatAsState(targetValue = target, animationSpec = tween(520), label = "coverPosition")
    PanelTileShell(modifier = modifier, padding = if (compact) 10.dp else 12.dp, onClick = onClick, pressedScale = 1f, pressedAlpha = 1f) {
        CoverAnimation(
            visual = tile.coverVisual ?: "blind",
            direction = tile.coverDirection ?: "top",
            openFraction = openFraction,
            accent = tile.accent.color(theme),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            onSetPercent = onSetPercent,
        )
        Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
        Text(
            tile.displayLabel(),
            color = theme.textPrimary,
            style = R1.body.copy(fontSize = if (compact) 14.sp else 17.sp, fontWeight = FontWeight.Bold, fontFamily = NunitoPanelFont),
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        PanelLiveStatus(tile = tile, liveState = liveState, compact = compact)
    }
}

@Composable
private fun CoverAnimation(
    visual: String,
    direction: String,
    openFraction: Float,
    accent: Color,
    modifier: Modifier,
    onSetPercent: (Int) -> Unit,
) {
    val theme = LocalHapanelsTheme.current
    BoxWithConstraints(modifier = modifier.clip(RoundedCornerShape(16.dp))) {
        var frameHeightPx by remember { mutableFloatStateOf(0f) }
        var dragFraction by remember { mutableFloatStateOf(-1f) }
        LaunchedEffect(openFraction) {
            if (dragFraction >= 0f && abs(dragFraction - openFraction) < 0.02f) dragFraction = -1f
        }
        fun setFromY(y: Float): Int? {
            val height = frameHeightPx.takeIf { it > 0f } ?: return null
            val ratio = (y / height).coerceIn(0f, 1f)
            val percent = if (direction.startsWith("bottom")) ratio * 100f else (1f - ratio) * 100f
            val value = percent.toInt().coerceIn(0, 100)
            dragFraction = value / 100f
            return value
        }
        val fraction = (if (dragFraction >= 0f) dragFraction else openFraction).coerceIn(0f, 1f)
        val blindHeight = (1f - fraction).coerceIn(0.06f, 1f)
        val scaleWidth = maxWidth * 0.16f
        val gap = maxWidth * 0.035f
        val frameWidth = (maxWidth - scaleWidth - gap).coerceAtMost(maxHeight * 0.62f).coerceAtLeast(maxWidth * 0.52f)
        val frameHeight = maxHeight * 0.74f
        val railWidth = frameWidth * 1.10f
        val railHeight = (frameHeight * 0.075f).coerceIn(6.dp, 24.dp)
        val bottomRailHeight = (frameHeight * 0.035f).coerceIn(4.dp, 10.dp)
        val frameShape = RoundedCornerShape(2.dp)
        val railShape = RoundedCornerShape(2.dp)
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.width(railWidth).fillMaxHeight()) {
                Box(
                    modifier = Modifier
                        .width(railWidth)
                        .height(railHeight)
                        .align(Alignment.TopCenter)
                        .clip(railShape)
                        .background(theme.coverRail.copy(alpha = 0.96f))
                        .border(1.dp, theme.textPrimary.copy(alpha = 0.65f), railShape),
                )
                BoxWithConstraints(
                    modifier = Modifier
                        .width(frameWidth)
                        .height(frameHeight)
                        .align(Alignment.Center)
                        .clip(frameShape)
                        .onSizeChanged { frameHeightPx = it.height.toFloat() }
                        .pointerInput(direction) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                var pendingPercent = setFromY(down.position.y)
                                down.consume()
                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (change.pressed) {
                                        pendingPercent = setFromY(change.position.y)
                                        change.consume()
                                    }
                                } while (change.pressed)
                                pendingPercent?.let(onSetPercent)
                            }
                        }
                        .background(Brush.verticalGradient(listOf(theme.textPrimary.copy(alpha = 0.18f), Color.Black.copy(alpha = 0.08f))))
                        .border(2.dp, theme.textPrimary.copy(alpha = 0.72f), frameShape),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(if (visual == "curtain") 1f else blindHeight)
                            .fillMaxWidth()
                            .align(if (direction.startsWith("bottom")) Alignment.BottomCenter else Alignment.TopCenter)
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            .background(Brush.verticalGradient(listOf(theme.coverRail.copy(alpha = 0.96f), accent.copy(alpha = 0.34f), accent.copy(alpha = 0.55f))))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val pitch = 8.dp.toPx()
                            val highlight = theme.textPrimary.copy(alpha = 0.46f)
                            val shadow = accent.copy(alpha = 0.24f)
                            var y = 6.dp.toPx()
                            while (y < size.height) {
                                drawLine(highlight, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                                drawLine(shadow, Offset(0f, y + 3.dp.toPx()), Offset(size.width, y + 3.dp.toPx()), strokeWidth = 1.dp.toPx())
                                y += pitch
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width((frameWidth * 0.18f).coerceIn(18.dp, 34.dp))
                            .height((frameHeight * 0.025f).coerceIn(5.dp, 8.dp))
                            .align(Alignment.TopCenter)
                            .offset(y = (maxHeight * blindHeight - 4.dp).coerceAtLeast(10.dp))
                            .clip(R1.ShapeRound)
                            .background(theme.textPrimary.copy(alpha = 0.78f)),
                    )
                }
                Box(
                    modifier = Modifier
                        .width(railWidth)
                        .height(bottomRailHeight)
                        .align(Alignment.BottomCenter)
                        .clip(frameShape)
                        .background(theme.coverRail.copy(alpha = 0.92f))
                        .border(1.dp, theme.textPrimary.copy(alpha = 0.48f), railShape),
                )
            }
            Spacer(Modifier.width(gap))
            CoverMiniScale(accent = theme.textPrimary.copy(alpha = 0.72f), modifier = Modifier.width(scaleWidth).fillMaxHeight(0.76f))
        }
    }
}

@Composable
private fun CoverMiniScale(accent: Color, modifier: Modifier) {
    val labels = listOf("100", "75", "50", "25", "0")
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.fillMaxHeight().width(12.dp)) {
            Box(modifier = Modifier.fillMaxHeight().width(1.dp).align(Alignment.CenterStart).background(accent.copy(alpha = 0.5f)))
            Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                repeat(labels.size) { Box(modifier = Modifier.width(12.dp).height(1.dp).background(accent.copy(alpha = 0.5f))) }
            }
        }
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { label ->
                Text(label, color = accent, style = R1.labelMicro.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = NunitoPanelFont), maxLines = 1)
            }
        }
    }
}

@Composable
private fun PanelSmallTile(
    tile: HapanelsTileConfig,
    liveState: EntityState?,
    modifier: Modifier,
    iconSize: Dp = 50.dp,
    onClick: () -> Unit,
    onSetPercent: (Int) -> Unit = {},
) {
    val theme = LocalHapanelsTheme.current
    if (tile.kind == HapanelsTileKind.COVER) {
        PanelCoverTile(tile = tile, liveState = liveState, modifier = modifier, compact = true, onClick = onClick, onSetPercent = onSetPercent)
        return
    }
    PanelTileShell(modifier = modifier, padding = 10.dp, onClick = onClick) {
        PanelIcons.Icon(tile.icon, tint = tile.accent.color(theme), modifier = Modifier.size(iconSize))
        Spacer(Modifier.height(7.dp))
        Text(
            tile.displayLabel(),
            color = theme.textPrimary,
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
    onClick: () -> Unit,
    onSetPercent: (Int) -> Unit = {},
) {
    val theme = LocalHapanelsTheme.current
    if (action.kind == HapanelsTileKind.COVER) {
        PanelCoverTile(tile = action, liveState = liveState, modifier = modifier, compact = true, onClick = onClick, onSetPercent = onSetPercent)
        return
    }
    PanelTileShell(modifier = modifier, padding = 12.dp, onClick = onClick) {
        PanelIcons.Icon(action.icon, tint = action.accent.color(theme), modifier = Modifier.size(iconSize))
        Spacer(Modifier.height(10.dp))
        Text(
            action.displayLabel(),
            color = theme.textPrimary,
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
    val theme = LocalHapanelsTheme.current
    val label = tile.liveLabel(liveState) ?: return
    Spacer(Modifier.height(if (compact) 4.dp else 6.dp))
    Text(
        label,
        color = liveState.statusColor(theme),
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
    val theme = LocalHapanelsTheme.current
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(theme.tileBackground)
            .r1Pressable(onClick = {})
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = theme.textPrimary,
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
    onClick: () -> Unit,
    pressedScale: Float = 0.97f,
    pressedAlpha: Float = 0.78f,
    content: @Composable ColumnScope.() -> Unit,
) {
    val theme = LocalHapanelsTheme.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(theme.tileBackground)
            .border(1.dp, theme.tileStroke, RoundedCornerShape(18.dp))
            .r1Pressable(onClick = onClick, pressedScale = pressedScale, pressedAlpha = pressedAlpha)
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

private fun HapanelsDashboardConfig.tilesBySize(size: HapanelsTileSize): List<HapanelsTileConfig> =
    tiles.filter { it.size == size }.sortedBy { it.order }

private fun HapanelsDashboardConfig.forPanel(panelId: String?): HapanelsDashboardConfig = copy(
    tiles = tiles.filter { tile ->
        if (panelId == null) tile.panelId.isNullOrBlank() || tile.kind == HapanelsTileKind.FOLDER || tile.kind == HapanelsTileKind.POPUP
        else tile.panelId == panelId && tile.kind != HapanelsTileKind.FOLDER && tile.kind != HapanelsTileKind.POPUP
    },
)

private fun HapanelsDashboardConfig.popupTiles(popup: HapanelsTileConfig): List<HapanelsTileConfig> =
    popup.panelId?.takeIf { it.isNotBlank() }
        ?.let { target -> tiles.filter { it.id != popup.id && it.panelId == target }.sortedBy { it.order } }
        ?: emptyList()

private fun HapanelsDashboardConfig.observableEntityIds(): Set<EntityId> =
    tiles.mapNotNull { it.entityId.toEntityIdOrNull() }.toSet()

private fun String?.toEntityIdOrNull(): EntityId? =
    this?.takeIf { it.isNotBlank() }?.let { runCatching { EntityId(it) }.getOrNull() }

private fun HapanelsTileConfig.liveState(liveEntities: Map<EntityId, EntityState>): EntityState? =
    entityId.toEntityIdOrNull()?.let(liveEntities::get)

private fun HapanelsTileConfig.tapAction(liveEntities: Map<EntityId, EntityState>): ServiceCall? {
    val target = entityId.toEntityIdOrNull() ?: return null
    return ServiceCall.tapAction(target, liveEntities[target]?.isOn == true)
}

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
        -> listOfNotNull(
            state.climateHvacMode?.uppercase(Locale.getDefault()),
            state.climateCurrentTemperature?.let { "${it.formatPanelNumber()}°" }
                ?: state.climateTargetTemperature?.let { "${it.formatPanelNumber()}°" },
        ).joinToString(" ").ifBlank { "nieznane" }
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

private fun EntityState?.statusColor(theme: HapanelsThemeColors): Color = when {
    this == null -> theme.textMuted
    !isAvailable -> theme.accentRed
    isOn -> theme.accentGreen
    else -> theme.textPrimary.copy(alpha = 0.62f)
}

private fun HapanelsTileConfig.displayLabel(): String =
    shortLabel?.takeIf { it.isNotBlank() } ?: label

private fun HapanelsTileConfig.hasGridCell(): Boolean =
    col != null && row != null

private fun Modifier.gridCell(
    col: Int,
    row: Int,
    colSpan: Int,
    rowSpan: Int,
    cellWidth: Dp,
    cellHeight: Dp,
    gap: Dp,
): Modifier = this
    .offset(
        x = (cellWidth + gap) * (col - 1),
        y = (cellHeight + gap) * (row - 1),
    )
    .width(cellWidth * colSpan + gap * (colSpan - 1))
    .height(cellHeight * rowSpan + gap * (rowSpan - 1))

private fun List<HapanelsTileConfig>.padPanelTiles(tileSize: HapanelsTileSize, count: Int): List<HapanelsTileConfig> =
    this + List((count - size).coerceAtLeast(0)) { index ->
        HapanelsTileConfig(
            id = "placeholder_${tileSize.name.lowercase()}_$index",
            kind = HapanelsTileKind.ENTITY,
            size = tileSize,
            label = "Brak kafla",
            icon = "mdi:cog",
            accent = HapanelsTileAccent.WHITE,
            order = Int.MAX_VALUE,
        )
    }

private fun HapanelsPersonStatus.color(theme: HapanelsThemeColors): Color = when (this) {
    HapanelsPersonStatus.HOME -> theme.accentGreen
    HapanelsPersonStatus.AWAY -> theme.accentRed
    HapanelsPersonStatus.UNKNOWN -> theme.textMuted
}

private fun HapanelsTileAccent.color(theme: HapanelsThemeColors): Color = when (this) {
    HapanelsTileAccent.ORANGE -> theme.accentOrange
    HapanelsTileAccent.RED -> theme.accentRed
    HapanelsTileAccent.WHITE -> theme.textPrimary.copy(alpha = 0.86f)
}

private val HapanelsThemeColors.background: Color get() = panelBg
private val HapanelsThemeColors.tileBackground: Color get() = surface
private val HapanelsThemeColors.tileStroke: Color get() = border
private val HapanelsThemeColors.accentOrange: Color get() = accent
private val HapanelsThemeColors.accentRed: Color get() = danger
private val HapanelsThemeColors.accentGreen: Color get() = success
private val HapanelsThemeColors.popupBackground: Color get() = surfaceVariant
private val HapanelsThemeColors.popupOverlay: Color get() = panelBg.copy(alpha = 0.96f)
private val HapanelsThemeColors.coverRail: Color get() = primary

private fun String.toHapanelsPanelTheme(systemDark: Boolean): HapanelsThemeColors = runCatching {
    val root = JSONObject(this)
    val theme = root.optJSONObject("theme") ?: return@runCatching defaultHapanelsThemeConfig.resolveHapanelsThemeColors()
    HapanelsThemeConfig(
        preset = theme.optString("preset").toHapanelsThemePreset(),
        mode = theme.optString("mode").toHapanelsThemeMode(),
    ).resolveHapanelsThemeColors(isSystemInDarkTheme = systemDark)
}.getOrDefault(defaultHapanelsThemeConfig.resolveHapanelsThemeColors())

private fun String.toHapanelsThemePreset(): HapanelsThemePreset = when (this) {
    "light_breeze" -> HapanelsThemePreset.LIGHT_BREEZE
    "desert_sun" -> HapanelsThemePreset.DESERT_SUN
    "forest_leaves" -> HapanelsThemePreset.FOREST_LEAVES
    "baltic_dawn" -> HapanelsThemePreset.BALTIC_DAWN
    "bieszczady_sunset" -> HapanelsThemePreset.BIESZCZADY_SUNSET
    "masurian_nights" -> HapanelsThemePreset.MASURIAN_NIGHTS
    "aurora_glass" -> HapanelsThemePreset.AURORA_GLASS
    "neon_noir" -> HapanelsThemePreset.NEON_NOIR
    "velvet_spectrum" -> HapanelsThemePreset.VELVET_SPECTRUM
    else -> HapanelsThemePreset.DEFAULT
}

private fun String.toHapanelsThemeMode(): HapanelsThemeMode = when (this) {
    "light" -> HapanelsThemeMode.LIGHT
    "system" -> HapanelsThemeMode.SYSTEM
    else -> HapanelsThemeMode.DARK
}

internal object PanelIcons {
    @Composable
    fun Icon(icon: String, tint: Color, modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val codepoints = remember(context) { loadMdiCodepoints(context) }
        val iconName = icon.toMdiName()
        val codepoint = codepoints[iconName] ?: codepoints["mdi:cog"]
        if (codepoint != null) {
            BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
                val fontSize = with(LocalDensity.current) { (minOf(maxWidth, maxHeight) * 0.92f).toSp() }
                Text(
                    text = String(Character.toChars(codepoint)),
                    style = R1.body.copy(fontFamily = MdiFontFamily, fontSize = fontSize),
                    color = tint,
                    textAlign = TextAlign.Center,
                )
            }
            return
        }
        val pathData = COG
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

    private val MdiFontFamily = FontFamily(Font(R.font.material_design_icons))

    private fun String.toMdiName(): String =
        if (startsWith("mdi:")) lowercase() else "mdi:${lowercase().replace('_', '-')}"

    private fun loadMdiCodepoints(context: Context): Map<String, Int> = runCatching {
        val json = context.assets.open("mdi-codepoints.json").bufferedReader().use { it.readText() }
        val obj = JSONObject(json)
        buildMap {
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                put(key, obj.getInt(key))
            }
        }
    }.getOrDefault(emptyMap())

    private const val CLOCK = "M12,20A8,8 0,1 0,12,4A8,8 0,0 0,12,20M12,2A10,10 0,1 1,12,22A10,10 0,0 1,12,2M12.5,7V12.25L17,14.92L16.25,16.15L11,13V7H12.5Z"
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
