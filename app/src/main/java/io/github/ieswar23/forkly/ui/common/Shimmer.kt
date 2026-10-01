package io.github.ieswar23.forkly.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import io.github.ieswar23.forkly.ui.theme.ForklyTheme

fun Modifier.shimmer(shape: Shape): Modifier = composed {
    val colors = ForklyTheme.extraColors
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX",
    )
    background(
        brush = Brush.linearGradient(
            colors = listOf(colors.shimmerBase, colors.shimmerHighlight, colors.shimmerBase),
            start = Offset(x, 0f),
            end = Offset(x + 500f, 250f),
        ),
        shape = shape,
    )
}

@Composable
fun ShimmerBlock(modifier: Modifier, shape: Shape) {
    Box(modifier.shimmer(shape))
}
