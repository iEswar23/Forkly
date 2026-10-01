package io.github.ieswar23.forkly.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun String.toColor(fallback: Color = Color(0xFFE8452C)): Color =
    runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrDefault(fallback)

@Composable
fun rememberGradient(start: String, end: String): Brush = remember(start, end) {
    Brush.linearGradient(listOf(start.toColor(), end.toColor()))
}

/** A gradient tile with a big emoji — our offline stand-in for food photography. */
@Composable
fun EmojiTile(
    emoji: String,
    gradientStart: String,
    gradientEnd: String,
    modifier: Modifier = Modifier,
    shape: Shape,
    emojiSize: TextUnit = 40.sp,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(rememberGradient(gradientStart, gradientEnd)),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, style = TextStyle(fontSize = emojiSize))
    }
}

/** Soft circular emoji badge used for dishes and list rows. */
@Composable
fun EmojiCircle(emoji: String, background: Color, size: Dp = 48.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, style = TextStyle(fontSize = (size.value * 0.48f).sp))
    }
}

/** Initials avatar, e.g. "AR" for Aarav Reddy. */
@Composable
fun InitialsAvatar(name: String, background: Color, contentColor: Color, size: Dp, modifier: Modifier = Modifier) {
    val initials = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(
        modifier = modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials.ifEmpty { "?" },
            style = TextStyle(fontSize = (size.value * 0.38f).sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = contentColor,
        )
    }
}
