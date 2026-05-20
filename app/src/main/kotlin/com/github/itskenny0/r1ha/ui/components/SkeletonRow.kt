package com.github.itskenny0.r1ha.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.github.itskenny0.r1ha.core.theme.R1

/**
 * Animated grey block used as a placeholder while data loads. Pulses between
 * SurfaceMuted and a slightly lighter shade on a 1.2-second cycle so the
 * affordance reads as "loading" rather than "broken empty row". Used in
 * first-load states for screens where a centred spinner felt too abstract
 * (notifications, cameras LIST) — laying out skeleton rows that match the
 * eventual content shape teaches the eye what to expect.
 */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "skeleton-pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton-alpha",
    )
    Box(
        modifier = modifier
            .clip(R1.ShapeS)
            .background(R1.SurfaceMuted.copy(alpha = alpha)),
    )
}

/**
 * Two-line skeleton sized to roughly match a notification card. Repeating
 * this 3-4× in a screen's loading state covers the typical empty viewport.
 */
@Composable
fun SkeletonRow(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(R1.ShapeS)
            .background(R1.SurfaceMuted.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SkeletonBlock(
            modifier = Modifier
                .width(140.dp)
                .height(12.dp),
        )
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
        )
        Spacer(Modifier.size(2.dp))
        SkeletonBlock(
            modifier = Modifier
                .width(80.dp)
                .height(8.dp),
        )
    }
}
