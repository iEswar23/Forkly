package io.github.ieswar23.forkly.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import io.github.ieswar23.forkly.domain.model.Banner
import io.github.ieswar23.forkly.ui.common.rememberGradient
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

private const val AUTO_SCROLL_MS = 3_500L

/** Auto-scrolling promo banners. Pauses while the user is dragging. */
@Composable
fun PromoCarousel(
    banners: List<Banner>,
    onBannerClick: (Banner) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (banners.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { banners.size })
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(pagerState, isDragged, banners.size) {
        if (isDragged) return@LaunchedEffect
        while (true) {
            delay(AUTO_SCROLL_MS)
            val next = (pagerState.currentPage + 1) % banners.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column(modifier) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
        ) { page ->
            val banner = banners[page]
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            BannerCard(
                banner = banner,
                onClick = { onBannerClick(banner) },
                modifier = Modifier.graphicsLayer {
                    val scale = lerp(0.94f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                    scaleY = scale
                    alpha = lerp(0.6f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                },
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(banners.size) { index ->
                val selected = pagerState.currentPage == index
                val width by animateDpAsState(if (selected) 18.dp else 6.dp, label = "dotWidth")
                val color by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    label = "dotColor",
                )
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }
    }
}

@Composable
private fun BannerCard(banner: Banner, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(rememberGradient(banner.gradientStart, banner.gradientEnd))
            .clickable(onClick = onClick),
    ) {
        // Decorative circles for depth
        Box(
            Modifier
                .size(170.dp)
                .align(Alignment.CenterEnd)
                .graphicsLayer { translationX = 60f }
                .background(Color.White.copy(alpha = 0.12f), CircleShape),
        )
        Box(
            Modifier
                .size(90.dp)
                .align(Alignment.TopStart)
                .graphicsLayer { translationX = -30f; translationY = -40f }
                .background(Color.White.copy(alpha = 0.08f), CircleShape),
        )
        Row(
            Modifier.fillMaxSize().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    banner.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    banner.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f),
                )
                if (banner.couponCode != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        banner.couponCode,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.22f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            Text(banner.emoji, style = TextStyle(fontSize = 64.sp))
        }
    }
}
