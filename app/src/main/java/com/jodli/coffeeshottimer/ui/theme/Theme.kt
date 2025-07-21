package com.jodli.coffeeshottimer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
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
    val screenPadding: Dp = 16.dp
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

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

    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}