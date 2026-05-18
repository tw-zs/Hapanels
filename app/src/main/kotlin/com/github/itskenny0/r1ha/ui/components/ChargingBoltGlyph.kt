package com.github.itskenny0.r1ha.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.itskenny0.r1ha.core.theme.R1

/**
 * Lightning-bolt glyph used as the 'charging' indicator on the DeviceScreen battery card.
 * The emoji ⚡ rendered with the system colour-emoji font and shipped its own yellow tint
 * regardless of how the surrounding text was coloured — so the charging marker stayed
 * golden-yellow on a card where everything else used R1.AccentGreen.
 *
 * Drawn as a filled path (closed Z-shape with two flat horizontals capping the angled
 * middle) so the colour follows the supplied [tint]. A thin outline gives the shape a
 * crisper edge at small sizes.
 */
@Composable
fun ChargingBoltGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    tint: Color = R1.AccentGreen,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            // Coordinates as fractions of the canvas — keeps the shape consistent at
            // any size. Top half wider on the right, bottom half wider on the left,
            // mid-line offset diagonally to give the classic lightning-Z silhouette.
            moveTo(w * 0.56f, h * 0.04f)
            lineTo(w * 0.18f, h * 0.56f)
            lineTo(w * 0.44f, h * 0.56f)
            lineTo(w * 0.36f, h * 0.96f)
            lineTo(w * 0.82f, h * 0.42f)
            lineTo(w * 0.56f, h * 0.42f)
            close()
        }
        drawPath(path = path, color = tint, style = Fill)
        // Tiny stroke outline so the path edge stays crisp when the surrounding bg
        // is close in luminance — Fill alone gets fuzzy edges at 12 dp on some
        // densities.
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Round),
        )
    }
}
