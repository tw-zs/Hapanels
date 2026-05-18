package com.github.itskenny0.r1ha.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.itskenny0.r1ha.core.theme.R1

/**
 * Stylised microphone glyph in the R1 idiom. Replaces the bare 🎤 emoji that was sitting in
 * the chrome row — emojis pick up the system colour-emoji font, which renders at a different
 * weight on every Android version and tinted with the device's own emoji palette. That gave
 * the Assist button a fuzzier outline than the hand-drawn [HamburgerGlyph] sitting two columns
 * over.
 *
 * The construction here mirrors [HamburgerGlyph]: 1.5 dp butt strokes, monochrome tint, no
 * fills. A vertical capsule (the mic head), a small U-curve underneath (the cradle), and a
 * short vertical stem with a horizontal base — minimal enough to read at 18 dp but distinct
 * enough that a glance picks it out from the hamburger to its left.
 */
@Composable
fun AssistMicGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    tint: Color = R1.Ink.copy(alpha = 0.85f),
) {
    Canvas(modifier = modifier.size(size)) {
        val sw = 1.5.dp.toPx()
        val w = this.size.width
        val h = this.size.height

        // Mic head — vertical capsule. The corner radius equals half the head's width
        // so the top and bottom render as perfect half-circles, which reads as 'mic'
        // even at 14 dp.
        val headW = w * 0.40f
        val headH = h * 0.55f
        val headLeft = (w - headW) / 2f
        val headTop = h * 0.10f
        drawRoundRect(
            color = tint,
            topLeft = Offset(headLeft, headTop),
            size = Size(headW, headH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(headW / 2f, headW / 2f),
            style = Stroke(width = sw, cap = StrokeCap.Round),
        )

        // Cradle — a shallow arc just below the head. Drawn as an open arc on a bounding
        // box that's wider than the head so the arc 'cups' the mic rather than tracing
        // around it.
        val cradleW = w * 0.66f
        val cradleH = h * 0.32f
        val cradleLeft = (w - cradleW) / 2f
        val cradleTop = headTop + headH - cradleH * 0.4f
        drawArc(
            color = tint,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cradleLeft, cradleTop),
            size = Size(cradleW, cradleH),
            style = Stroke(width = sw, cap = StrokeCap.Round),
        )

        // Stem — short vertical line from the bottom of the cradle to the base bar.
        val stemTop = cradleTop + cradleH * 0.78f
        val stemBottom = h * 0.92f
        drawLine(
            color = tint,
            start = Offset(w / 2f, stemTop),
            end = Offset(w / 2f, stemBottom),
            strokeWidth = sw,
            cap = StrokeCap.Butt,
        )

        // Base — short horizontal foot.
        val baseHalf = w * 0.16f
        drawLine(
            color = tint,
            start = Offset(w / 2f - baseHalf, stemBottom),
            end = Offset(w / 2f + baseHalf, stemBottom),
            strokeWidth = sw,
            cap = StrokeCap.Butt,
        )
    }
}
