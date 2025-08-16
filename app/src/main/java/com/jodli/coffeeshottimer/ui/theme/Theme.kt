package com.jodli.coffeeshottimer.ui.theme

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
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
    val touchTarget: Dp = 44.dp, // Minimum touch target size for accessibility
    val cardPadding: Dp = 16.dp,
    val screenPadding: Dp = 16.dp,
    // Icon sizes
    val iconSmall: Dp = 16.dp,
    val iconMedium: Dp = 24.dp,
    val iconLarge: Dp = 32.dp,
    val iconEmptyState: Dp = 64.dp,
    // Corner radius values
    val cornerSmall: Dp = 4.dp,
    val cornerMedium: Dp = 8.dp,
    val cornerLarge: Dp = 16.dp,
    // Elevation values
    val elevationCard: Dp = 4.dp,
    val elevationDialog: Dp = 8.dp,
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
 * Remembers landscape configuration based on current device configuration.
 * Provides reactive landscape detection and layout parameters.
 */
@Composable
fun rememberLandscapeConfiguration(): LandscapeConfiguration {
    val configuration = LocalConfiguration.current
    
    return remember(configuration) {
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        
        LandscapeConfiguration(
            isLandscape = isLandscape,
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
            timerSize = if (isLandscape) 160.dp else 200.dp,
            contentSpacing = if (isLandscape) 12.dp else 16.dp
        )
    }
}

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
            availableWidth,  // 70% of available width  
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

// Light coffee theme - warm and creamy like the launcher icon
private val LightColorScheme = lightColorScheme(
    primary = WarmCaramel,
    onPrimary = Color.White,
    primaryContainer = SoftBeige,
    onPrimaryContainer = RichEspresso,
    secondary = SoftTeal,
    onSecondary = Color.White,
    secondaryContainer = LightCream,
    onSecondaryContainer = MediumCoffee,
    tertiary = TealAccent,
    onTertiary = Color.White,
    background = CreamyBeige,     // Creamy beige background like icon
    onBackground = RichEspresso,  // Dark brown text
    surface = LightCream,         // Light cream cards
    onSurface = RichEspresso,     // Dark brown text on cards
    surfaceVariant = SoftBeige,   // Soft beige variants
    onSurfaceVariant = MediumCoffee,
    outline = MediumCoffee,       // Brown outlines
    error = ErrorRed,
    onError = Color.White
)

// Dark coffee theme - rich espresso tones with warm accents
private val DarkColorScheme = darkColorScheme(
    primary = LightCaramel,       // Lighter caramel for visibility
    onPrimary = DeepBrown,
    primaryContainer = MediumCoffee,
    onPrimaryContainer = DarkCream,
    secondary = WarmTeal,         // Warm teal accent
    onSecondary = DeepBrown,
    secondaryContainer = RichEspresso,
    onSecondaryContainer = DarkCream,
    tertiary = TealAccent,
    onTertiary = DeepBrown,
    background = DeepBrown,       // Deep coffee brown background
    onBackground = DarkCream,     // Cream text on dark background
    surface = RichEspresso,       // Rich espresso cards
    onSurface = DarkCream,        // Cream text on dark cards
    surfaceVariant = MediumCoffee, // Medium coffee variants
    onSurfaceVariant = LightCaramel,
    outline = WarmAmber,          // Warm amber outlines
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun CoffeeShotTimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled to use custom launcher-inspired colors
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
        LocalIsLandscape provides isLandscape
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}