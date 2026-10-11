package com.jodli.coffeeshottimer.ui.theme

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Custom spacing system
data class Spacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val touchTarget: Dp = 48.dp, // Material minimum touch target
    val cardPadding: Dp = 16.dp,
    val screenPadding: Dp = 16.dp,
    // Icon sizes
    val iconSmall: Dp = 16.dp,
    val iconMedium: Dp = 24.dp,
    val iconLarge: Dp = 32.dp,
    val iconEmptyState: Dp = 64.dp,
    // Corner radius values
    // near-square corners: the app is a printed chart, not a stack of pills
    val cornerSmall: Dp = 2.dp,
    val cornerMedium: Dp = 2.dp,
    val cornerLarge: Dp = 4.dp,
    // Elevation values
    // flat sheet: cards are separated by rules, not shadows
    val elevationCard: Dp = 0.dp,
    val elevationDialog: Dp = 2.dp,
    // Component-specific sizes
    val timerSize: Dp = 200.dp,
    val buttonMaxWidth: Dp = 200.dp,
    val sliderHeight: Dp = 32.dp,
    val qualityIndicator: Dp = 8.dp,
    val priorityIndicator: Dp = 6.dp,
    val fabSize: Dp = 56.dp,
    val fabSizeSmall: Dp = 40.dp,
    val timerButtonSize: Dp = 80.dp,
    val iconButtonSize: Dp = 32.dp,
    val sliderHeightSmall: Dp = 24.dp,
    val thumbnailSize: Dp = 48.dp,
    val photoSizeLarge: Dp = 64.dp, // S1 card hero photo (Phase 3 target)
    // Landscape-specific values
    val landscapeTimerSize: Dp = 220.dp, // Used as fallback when BoxWithConstraints isn't available
    val landscapeContentSpacing: Dp = 12.dp
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

// Landscape configuration system
val LocalIsLandscape = staticCompositionLocalOf { false }

/**
 * Configuration data class for landscape-specific layout information
 */
data class LandscapeConfiguration(
    val isLandscape: Boolean,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val timerSize: Dp,
    val contentSpacing: Dp
)

/**
 * Returns adaptive timer size for landscape mode based on available space.
 * Uses BoxWithConstraints to calculate optimal size as a percentage of available space.
 * Falls back to landscapeTimerSize if constraints aren't available.
 */
@Composable
fun Spacing.adaptiveTimerSize(
    availableHeight: Dp,
    availableWidth: Dp,
    isLandscape: Boolean = false
): Dp {
    return if (isLandscape) {
        // Enhanced adaptive sizing: maximize screen height usage
        // Account for card header (~32dp) + padding (~32dp total) + margins (~24dp)
        val cardOverhead = 64.dp
        val usableHeight = (availableHeight - cardOverhead).coerceAtLeast(160.dp)

        // Use 85% of usable height or 70% of available width, whichever is smaller
        minOf(
            usableHeight, // 85% of usable height for maximum timer size
            availableWidth, // 70% of available width
            350.dp // Increased max size to allow larger timers
        ).coerceAtLeast(160.dp) // Never go below 160dp
    } else {
        timerSize // Use standard size for portrait
    }
}

/**
 * Extension function to get landscape-aware timer size.
 * In landscape mode, returns adaptive size based on available space.
 * In portrait mode, returns standard timer size.
 */
@Composable
fun Spacing.landscapeTimerSize(): Dp {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    return if (isLandscape) landscapeTimerSize else timerSize
}

/**
 * Extension function to get landscape-appropriate content spacing
 */
@Composable
fun Spacing.landscapeSpacing(): Dp {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    return if (isLandscape) landscapeContentSpacing else medium
}

// Light: ink on graph paper. Primary is the ink itself, so filled buttons read as printed.
private val LightColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = InkTint,
    onPrimaryContainer = Ink,
    inversePrimary = NightInk,
    secondary = IdealInk,
    onSecondary = Color.White,
    // containers stay neutral: green and yellow are reserved for the zones
    secondaryContainer = InkTint,
    onSecondaryContainer = Ink,
    tertiary = Ink2,
    onTertiary = Paper,
    tertiaryContainer = PaperHigh,
    onTertiaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Paper2,
    onSurfaceVariant = Ink2,
    surfaceTint = Color.Transparent,
    surfaceBright = Paper,
    surfaceDim = PaperHighest,
    surfaceContainerLowest = Color(0xFFFBFBF8),
    surfaceContainerLow = PaperLow,
    surfaceContainer = Paper2,
    surfaceContainerHigh = PaperHigh,
    surfaceContainerHighest = PaperHighest,
    inverseSurface = Ink,
    inverseOnSurface = Paper,
    outline = RuleLight,
    outlineVariant = PlotGridLight,
    error = ErrorLight,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    scrim = Color.Black
)

// Dark: the same sheet at night, ink and paper swapped
private val DarkColorScheme = darkColorScheme(
    primary = NightInk,
    onPrimary = NightPaper,
    primaryContainer = NightInkTint,
    onPrimaryContainer = NightInk,
    inversePrimary = Ink,
    secondary = IdealInkNight,
    onSecondary = Color(0xFF0B2A16),
    secondaryContainer = NightInkTint,
    onSecondaryContainer = NightInk,
    tertiary = NightInk2,
    onTertiary = NightPaper,
    tertiaryContainer = NightPaperHigh,
    onTertiaryContainer = NightInk,
    background = NightPaper,
    onBackground = NightInk,
    surface = NightPaper,
    onSurface = NightInk,
    surfaceVariant = NightPaper2,
    onSurfaceVariant = NightInk2,
    surfaceTint = Color.Transparent,
    surfaceBright = NightPaperHighest,
    surfaceDim = NightPaper,
    surfaceContainerLowest = NightPaperLowest,
    surfaceContainerLow = NightPaperLow,
    surfaceContainer = NightPaper2,
    surfaceContainerHigh = NightPaperHigh,
    surfaceContainerHighest = NightPaperHighest,
    inverseSurface = NightInk,
    inverseOnSurface = NightPaper,
    outline = RuleDark,
    outlineVariant = PlotGridDark,
    error = ErrorDark,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color.Black
)

// built from the Spacing corner tokens so there is one source for radii
private val AppShapes = Spacing().let { s ->
    Shapes(
        extraSmall = RoundedCornerShape(s.cornerSmall),
        small = RoundedCornerShape(s.cornerMedium),
        medium = RoundedCornerShape(s.cornerLarge),
        large = RoundedCornerShape(s.cornerLarge),
        extraLarge = RoundedCornerShape(s.cornerLarge * 2)
    )
}

/** Zone and quality colours for the current scheme. */
val MaterialTheme.zoneColors: ZoneColors
    @Composable
    @ReadOnlyComposable
    get() = LocalZoneColors.current

@Composable
fun CoffeeShotTimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color stays off: wallpaper colours would erase the zone meanings
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Detect landscape orientation
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalIsLandscape provides isLandscape,
        LocalZoneColors provides if (darkTheme) DarkZoneColors else LightZoneColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
