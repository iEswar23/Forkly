package io.github.ieswar23.forkly.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ieswar23.forkly.domain.model.Restaurant
import io.github.ieswar23.forkly.util.formatDistance

@Composable
fun FavoriteButton(isFavorite: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, onDark: Boolean = true) {
    val scale by animateFloatAsState(
        targetValue = if (isFavorite) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        label = "heartScale",
    )
    val tint by animateColorAsState(
        if (isFavorite) MaterialTheme.colorScheme.primary else if (onDark) Color.White else MaterialTheme.colorScheme.onSurface,
        label = "heartTint",
    )
    IconButton(
        onClick = onToggle,
        modifier = modifier
            .size(40.dp)
            .background(if (onDark) Color.Black.copy(alpha = 0.25f) else Color.Transparent, CircleShape),
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from favourites" else "Add to favourites",
            tint = tint,
            modifier = Modifier.scale(scale),
        )
    }
}

/** Full-width card used in the "All restaurants" feed, search results and favourites. */
@Composable
fun RestaurantCard(
    restaurant: Restaurant,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(164.dp)) {
            EmojiTile(
                emoji = restaurant.emoji,
                gradientStart = restaurant.gradientStart,
                gradientEnd = restaurant.gradientEnd,
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                emojiSize = 72.sp,
                modifier = Modifier.fillMaxWidth().height(164.dp),
            )
            FavoriteButton(
                isFavorite = restaurant.isFavorite,
                onToggle = onToggleFavorite,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            )
            if (restaurant.hasOffer) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = restaurant.offerText.orEmpty().uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                }
            }
            Text(
                text = "${restaurant.deliveryTimeMins} MINS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    restaurant.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                RatingPill(restaurant.rating)
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    restaurant.cuisineLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "₹${restaurant.costForTwo} for two",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.Schedule, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${restaurant.deliveryTimeMins}–${restaurant.deliveryTimeMins + 5} mins • ${formatDistance(restaurant.distanceKm)} • ${restaurant.area}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (restaurant.isPureVeg) PureVegBadge()
            }
        }
    }
}

/** Compact card used in the horizontal "Top rated near you" rail. */
@Composable
fun RestaurantMiniCard(restaurant: Restaurant, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.width(156.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box {
            EmojiTile(
                emoji = restaurant.emoji,
                gradientStart = restaurant.gradientStart,
                gradientEnd = restaurant.gradientEnd,
                shape = RoundedCornerShape(16.dp),
                emojiSize = 48.sp,
                modifier = Modifier.fillMaxWidth().height(112.dp),
            )
            if (restaurant.hasOffer) {
                Row(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.LocalOffer, null, tint = Color.White, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        restaurant.offerText.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Column(Modifier.padding(10.dp)) {
            Text(restaurant.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RatingPill(restaurant.rating)
                Spacer(Modifier.width(6.dp))
                Text(
                    "${restaurant.deliveryTimeMins} mins",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                restaurant.cuisineLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun RestaurantCardPlaceholder(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        ShimmerBlock(Modifier.fillMaxWidth().height(164.dp), RoundedCornerShape(22.dp))
        Spacer(Modifier.height(12.dp))
        ShimmerBlock(Modifier.fillMaxWidth(0.6f).height(18.dp), RoundedCornerShape(6.dp))
        Spacer(Modifier.height(8.dp))
        ShimmerBlock(Modifier.fillMaxWidth(0.85f).height(12.dp), RoundedCornerShape(6.dp))
    }
}
