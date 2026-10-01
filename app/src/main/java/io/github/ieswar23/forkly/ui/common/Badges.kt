package io.github.ieswar23.forkly.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.ieswar23.forkly.ui.theme.ForklyTheme
import io.github.ieswar23.forkly.util.formatRating

/** The FSSAI-style veg (green dot) / non-veg (red triangle) marker. */
@Composable
fun VegIndicator(isVeg: Boolean, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    val color = if (isVeg) ForklyTheme.extraColors.veg else ForklyTheme.extraColors.nonVeg
    Box(
        modifier = modifier
            .size(size)
            .border(1.5.dp, color, RoundedCornerShape(3.dp))
            .semantics { contentDescription = if (isVeg) "Vegetarian" else "Non-vegetarian" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size * 0.5f)) {
            if (isVeg) {
                drawCircle(color)
            } else {
                val path = Path().apply {
                    moveTo(this@Canvas.size.width / 2f, 0f)
                    lineTo(this@Canvas.size.width, this@Canvas.size.height)
                    lineTo(0f, this@Canvas.size.height)
                    close()
                }
                drawPath(path, color)
            }
        }
    }
}

@Composable
fun RatingPill(rating: Double, modifier: Modifier = Modifier) {
    val color = if (rating >= 4.0) ForklyTheme.extraColors.rating else ForklyTheme.extraColors.ratingLow
    Row(
        modifier = modifier
            .background(color, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(formatRating(rating), style = MaterialTheme.typography.labelMedium, color = Color.White)
        Icon(Icons.Rounded.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
    }
}

@Composable
fun PureVegBadge(modifier: Modifier = Modifier) {
    val green = ForklyTheme.extraColors.veg
    Row(
        modifier = modifier
            .background(green.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        VegIndicator(isVeg = true, size = 10.dp)
        Text("PURE VEG", style = MaterialTheme.typography.labelSmall, color = green)
    }
}

@Composable
fun BestsellerTag(modifier: Modifier = Modifier) {
    Text(
        text = "★ Bestseller",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
