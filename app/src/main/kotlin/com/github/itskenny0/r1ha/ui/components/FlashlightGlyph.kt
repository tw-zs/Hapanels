package com.github.itskenny0.r1ha.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.itskenny0.r1ha.core.theme.R1

/**
 * Stylised flashlight glyph in the R1 idiom. Replaces the 🔦 emoji that DeviceScreen was
 * rendering at numeralXl — the colour-emoji font blew it up into a chunky orange pictograph
 * with its own drop-shadow that visibly clashed with the hairline-stroke chrome elsewhere
 * on the screen.
 *
 * Construction: a body trapezoid (head wider than handle), a short tab on top to suggest
 * the on/off switch, and (when [emitting] is true) three short light-rays fanning out from
 * the head. The rays are skipped when off so the off-state reads cleanly as 'inert'.
 */
@Composable
fun FlashlightGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = R1.Ink.copy(alpha = 0.85f),
    emitting: Boolean = false,
) {
    Canvas(modifier = modifier.size(size)) {
        val sw = 1.5.dp.toPx()
        val w = this.size.width
        val h = this.size.height

        // Body — a trapezoid wider at the top (the bezel / lens face) and narrower at
        // the bottom (the user's grip). Drawn as a closed path so the corners join
        // cleanly.
        val bodyTop = h * 0.30f
        val bodyBottom = h * 0.92f
        val bodyHeadHalf = w * 0.22f
        val bodyTailHalf = w * 0.14f
        val cx = w / 2f
        val body = Path().apply {
            moveTo(cx - bodyHeadHalf, bodyTop)
            lineTo(cx + bodyHeadHalf, bodyTop)
            lineTo(cx + bodyTailHalf, bodyBottom)
            lineTo(cx - bodyTailHalf, bodyBottom)
            close()
        }
        drawPath(path = body, color = tint, style = Stroke(width = sw, cap = StrokeCap.Round))

        // Switch tab on top of the body — a short rectangle straddling the centreline.
        val tabTop = h * 0.18f
        val tabBottom = bodyTop
        val tabHalf = w * 0.10f
        drawLine(
            color = tint,
            start = Offset(cx - tabHalf, tabTop),
            end = Offset(cx + tabHalf, tabTop),
            strokeWidth = sw,
        )
        drawLine(color = tint, start = Offset(cx - tabHalf, tabTop), end = Offset(cx - tabHalf, tabBottom), strokeWidth = sw)
        drawLine(color = tint, start = Offset(cx + tabHalf, tabTop), end = Offset(cx + tabHalf, tabBottom), strokeWidth = sw)

        // Light rays — three short strokes fanning out from the top corner of the bezel.
        // Only drawn when emitting so the off-state stays simple.
        if (emitting) {
            val rayLen = w * 0.18f
            val rayBaseY = bodyTop - sw
            // Left ray — angled up-left from the left-top corner.
            drawLine(
                color = tint,
                start = Offset(cx - bodyHeadHalf, rayBaseY),
                end = Offset(cx - bodyHeadHalf - rayLen * 0.7f, rayBaseY - rayLen * 0.7f),
                strokeWidth = sw,
                cap = StrokeCap.Round,
            )
            // Centre ray — straight up.
            drawLine(
                color = tint,
                start = Offset(cx, rayBaseY),
                end = Offset(cx, rayBaseY - rayLen),
                strokeWidth = sw,
                cap = StrokeCap.Round,
            )
            // Right ray — angled up-right from the right-top corner.
            drawLine(
                color = tint,
                start = Offset(cx + bodyHeadHalf, rayBaseY),
                end = Offset(cx + bodyHeadHalf + rayLen * 0.7f, rayBaseY - rayLen * 0.7f),
                strokeWidth = sw,
                cap = StrokeCap.Round,
            )
        }
    }
}
