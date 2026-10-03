package io.github.ieswar23.forkly.ui.checkout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ieswar23.forkly.ui.theme.ForklyTheme
import io.github.ieswar23.forkly.ui.tracking.TrackingViewModel
import io.github.ieswar23.forkly.util.formatRupees
import io.github.ieswar23.forkly.util.formatScheduledFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private val confetti = listOf("🎉", "🍕", "✨", "🍛", "🎊", "🍔", "🥳", "🍩")

/** Celebratory confirmation: circle pops in, the tick draws itself, food confetti bursts out. */
@Composable
fun OrderSuccessRoute(
    onTrackOrder: (String) -> Unit,
    onDone: () -> Unit,
    viewModel: TrackingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val circleScale = remember { Animatable(0f) }
    val tickProgress = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val successColor = ForklyTheme.extraColors.success

    BackHandler(onBack = onDone)

    LaunchedEffect(Unit) {
        circleScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        delay(250)
        launch { burst.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
        tickProgress.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
        textAlpha.animateTo(1f, tween(400))
        delay(2_600)
        onTrackOrder(viewModel.orderId)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
                confetti.forEachIndexed { index, emoji ->
                    val angle = Math.toRadians((index * 360.0 / confetti.size) - 90)
                    val distance = 115f * burst.value
                    Text(
                        emoji,
                        style = TextStyle(fontSize = 26.sp),
                        modifier = Modifier.graphicsLayer {
                            translationX = (cos(angle) * distance * density).toFloat()
                            translationY = (sin(angle) * distance * density).toFloat()
                            alpha = if (burst.value < 0.85f) burst.value * 1.2f else (1f - burst.value) * 6f
                            rotationZ = 180f * burst.value
                        },
                    )
                }
                Box(
                    Modifier
                        .size(132.dp)
                        .graphicsLayer {
                            scaleX = circleScale.value
                            scaleY = circleScale.value
                        }
                        .background(successColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(64.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.14f, size.height * 0.52f)
                            lineTo(size.width * 0.40f, size.height * 0.76f)
                            lineTo(size.width * 0.88f, size.height * 0.26f)
                        }
                        val measure = PathMeasure().apply { setPath(path, false) }
                        val partial = Path()
                        measure.getSegment(0f, measure.length * tickProgress.value, partial, true)
                        drawPath(
                            partial,
                            Color.White,
                            style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.graphicsLayer {
                    alpha = textAlpha.value
                    translationY = (1f - textAlpha.value) * 40f
                },
            ) {
                val order = state.order
                Text(if (order?.isScheduled == true) "Order scheduled!" else "Order placed!", style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (order != null) {
                        val items = "${order.itemCount} item${if (order.itemCount == 1) "" else "s"} (${formatRupees(order.totalPaise)})"
                        if (order.isScheduled) {
                            "${order.restaurantName} will prepare your order of $items in time for your slot."
                        } else {
                            "${order.restaurantName} is confirming your order of $items."
                        }
                    } else {
                        "Your order is confirmed."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                order?.scheduledFor?.let { slot ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Rounded.Schedule, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Scheduled for ${formatScheduledFor(slot)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                order?.let {
                    Text(
                        "Order #${it.id}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(24.dp)
                .graphicsLayer { alpha = textAlpha.value },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = { onTrackOrder(viewModel.orderId) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Track your order") }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onDone) { Text("Back to home") }
        }
    }
}
