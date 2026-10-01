package io.github.ieswar23.forkly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Tomato,
    onPrimary = Color.White,
    primaryContainer = TomatoContainer,
    onPrimaryContainer = Color(0xFF410200),
    secondary = Saffron,
    onSecondary = Color.White,
    secondaryContainer = SaffronContainer,
    onSecondaryContainer = Color(0xFF3A1A00),
    tertiary = VegGreen,
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = CreamVariant,
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFBF8),
    surfaceContainer = Color(0xFFFDF3EC),
    surfaceContainerHigh = CreamVariant,
    surfaceContainerHighest = Color(0xFFF5E5D9),
    outline = Color(0xFFCBB8AB),
    outlineVariant = Outline,
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = TomatoLight,
    onPrimary = Color(0xFF3B0900),
    primaryContainer = Color(0xFF7A1D0C),
    onPrimaryContainer = Color(0xFFFFDAD3),
    secondary = Color(0xFFFFB27A),
    onSecondary = Color(0xFF4A2300),
    secondaryContainer = Color(0xFF6A3400),
    onSecondaryContainer = SaffronContainer,
    tertiary = Color(0xFF4ADE80),
    background = Night,
    onBackground = Paper,
    surface = NightSurface,
    onSurface = Paper,
    surfaceVariant = NightVariant,
    onSurfaceVariant = PaperMuted,
    surfaceContainerLowest = Color(0xFF110C0A),
    surfaceContainerLow = Color(0xFF1B1512),
    surfaceContainer = Color(0xFF221B17),
    surfaceContainerHigh = NightVariant,
    surfaceContainerHighest = Color(0xFF362D27),
    outline = Color(0xFF8C7C72),
    outlineVariant = NightOutline,
    error = Color(0xFFFFB4AB),
)

/** Food-app specific colors that don't map onto Material roles. */
@Immutable
data class ForklyExtraColors(
    val veg: Color,
    val nonVeg: Color,
    val rating: Color,
    val ratingLow: Color,
    val offer: Color,
    val success: Color,
    val shimmerBase: Color,
    val shimmerHighlight: Color,
)

private val LightExtras = ForklyExtraColors(
    veg = VegGreen, nonVeg = NonVegRed, rating = RatingGreen, ratingLow = RatingAmber, offer = OfferBlue,
    success = SuccessGreen, shimmerBase = Color(0xFFF1E4DA), shimmerHighlight = Color(0xFFFFF8F2),
)

private val DarkExtras = ForklyExtraColors(
    veg = Color(0xFF34C072), nonVeg = Color(0xFFFF6B5E), rating = RatingGreen, ratingLow = RatingAmber,
    offer = Color(0xFF7AA7FF), success = Color(0xFF4ADE80), shimmerBase = Color(0xFF2B231E),
    shimmerHighlight = Color(0xFF3A302A),
)

val LocalForklyColors = staticCompositionLocalOf { LightExtras }

val ForklyShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ForklyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalForklyColors provides if (darkTheme) DarkExtras else LightExtras) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = ForklyTypography,
            shapes = ForklyShapes,
            content = content,
        )
    }
}

object ForklyTheme {
    val extraColors: ForklyExtraColors
        @Composable get() = LocalForklyColors.current
}

/** 4-pt spacing scale used across screens. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}
