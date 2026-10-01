package io.github.ieswar23.forkly.ui.tracking

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ieswar23.forkly.domain.model.OrderStatus
import kotlin.math.roundToInt

/**
 * A stylised, offline "map": a street grid with the route from the restaurant to the customer,
 * and the rider moving along it while the order is out for delivery.
 */
@Composable
fun DeliveryMap(
    status: OrderStatus,
    stageProgress: Float,
    restaurantEmoji: String,
    modifier: Modifier = Modifier,
) {
    val riderFraction = when (status) {
        OrderStatus.PLACED, OrderStatus.PREPARING -> 0f
        OrderStatus.OUT_FOR_DELIVERY -> stageProgress
        OrderStatus.DELIVERED -> 1f
    }
    val animatedFraction by animateFloatAsState(riderFraction, tween(900), label = "riderFraction")
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val routeBase = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val routeDone = MaterialTheme.colorScheme.primary
    val parkColor = androidx.compose.ui.graphics.Color(0xFF8BC34A).copy(alpha = 0.18f)
    val waterColor = androidx.compose.ui.graphics.Color(0xFF4FC3F7).copy(alpha = 0.18f)
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val route = remember(widthPx, heightPx) { buildRoute(widthPx, heightPx) }
        val measure = remember(route) { PathMeasure().apply { setPath(route, false) } }
        val length = measure.length
        val riderPos = measure.getPosition(length * animatedFraction)
        val start = measure.getPosition(0f)
        val end = measure.getPosition(length)

        Canvas(Modifier.fillMaxSize()) {
            // Water body & park for a bit of texture.
            drawCircle(waterColor, radius = size.minDimension * 0.28f, center = Offset(size.width * 0.82f, size.height * 0.1f))
            drawRoundRect(
                parkColor,
                topLeft = Offset(size.width * 0.08f, size.height * 0.62f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.22f, size.height * 0.28f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
            )
            val step = 36.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
                y += step
            }
            drawPath(
                route,
                routeBase,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f))),
            )
            val done = Path()
            measure.getSegment(0f, length * animatedFraction, done, true)
            drawPath(done, routeDone, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
        }
        MapPin(restaurantEmoji, start)
        MapPin("🏠", end)
        if (status == OrderStatus.OUT_FOR_DELIVERY || status == OrderStatus.DELIVERED) {
            MapPin("🛵", riderPos, elevated = true)
        }
    }
}

@Composable
private fun MapPin(emoji: String, center: Offset, elevated: Boolean = false) {
    val sizeDp = if (elevated) 40.dp else 36.dp
    val half = with(LocalDensity.current) { (sizeDp / 2).toPx() }
    Box(
        Modifier
            .offset { IntOffset((center.x - half).roundToInt(), (center.y - half).roundToInt()) }
            .size(sizeDp)
            .shadow(if (elevated) 6.dp else 3.dp, CircleShape)
            .background(MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, style = TextStyle(fontSize = if (elevated) 22.sp else 18.sp))
    }
}

private fun buildRoute(w: Float, h: Float): Path = Path().apply {
    moveTo(w * 0.12f, h * 0.28f)
    lineTo(w * 0.36f, h * 0.28f)
    cubicTo(w * 0.46f, h * 0.28f, w * 0.46f, h * 0.55f, w * 0.56f, h * 0.55f)
    lineTo(w * 0.72f, h * 0.55f)
    cubicTo(w * 0.82f, h * 0.55f, w * 0.82f, h * 0.76f, w * 0.88f, h * 0.76f)
}
