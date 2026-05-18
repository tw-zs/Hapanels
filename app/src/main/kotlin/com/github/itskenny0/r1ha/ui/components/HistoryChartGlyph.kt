package com.github.itskenny0.r1ha.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.itskenny0.r1ha.core.theme.R1

/**
 * Mini line-chart glyph used as the per-row 'history drill-in' affordance on the
 * search and helpers surfaces. Replaces the 📈 emoji that was rendering with the
 * system colour-emoji font — green bars + a red arrow on most Android versions,
 * which visibly clashed with the monochrome hairline-stroke chrome around it.
 *
 * Construction: a faint horizontal baseline + a four-point ascending polyline that
 * reads as 'value over time' at a glance. Stroke width matches the other glyphs
 * (1.5 dp butt cap) so the visual weight harmonises with HamburgerGlyph,
 * AssistMicGlyph, etc.
 */
@Composable
fun HistoryChartGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    tint: Color = R1.Ink.copy(alpha = 0.85f),
) {
    Canvas(modifier = modifier.size(size)) {
        val sw = 1.5.dp.toPx()
        val w = this.size.width
        val h = this.size.height
        // Faint horizontal baseline (dashed) for visual grounding — same
        // language the SensorHistoryChart uses elsewhere in the app.
        drawLine(
            color = tint.copy(alpha = 0.35f),
            start = Offset(w * 0.08f, h * 0.85f),
            end = Offset(w * 0.92f, h * 0.85f),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Butt,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx())),
        )
        // Four-point ascending polyline. Y-positions chosen to give the line a
        // gentle up-and-to-the-right slope with one small dip mid-curve so it
        // doesn't read as a flat 45° vector — visually distinguishes the glyph
        // from an arrow.
        val points = listOf(
            Offset(w * 0.12f, h * 0.72f),
            Offset(w * 0.38f, h * 0.48f),
            Offset(w * 0.62f, h * 0.58f),
            Offset(w * 0.90f, h * 0.20f),
        )
        for (i in 0 until points.size - 1) {
            drawLine(
                color = tint,
                start = points[i],
                end = points[i + 1],
                strokeWidth = sw,
                cap = StrokeCap.Round,
            )
        }
    }
}
