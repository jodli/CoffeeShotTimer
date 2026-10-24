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

val LocalSpacing = staticCompositionLocalOf { Spacing() }

// Landscape configuration system
val LocalIsLandscape = staticCompositionLocalOf { false }

/**
 * Landscape-appropriate content spacing
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
