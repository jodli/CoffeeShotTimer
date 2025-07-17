package com.example.coffeeshottimer.ui.theme

import android.app.Activity
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

private val DarkColorScheme = darkColorScheme(
    primary = LightCoffee,
    onPrimary = DarkEspresso,
    primaryContainer = RichBrown,
    onPrimaryContainer = WarmCream,
    secondary = WarmCream,
    onSecondary = DeepBrown,
    secondaryContainer = DeepBrown,
    onSecondaryContainer = WarmCream,
    tertiary = GoldenAccent,
    onTertiary = DarkEspresso,
    background = DarkEspresso,
    onBackground = WarmCream,
    surface = DeepBrown,
    onSurface = WarmCream,
    surfaceVariant = RichBrown,
    onSurfaceVariant = LightCream,
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = CoffeeBrown,
    onPrimary = CreamWhite,
    primaryContainer = LightCream,
    onPrimaryContainer = EspressoBlack,
    secondary = WarmBrown,
    onSecondary = CreamWhite,
    secondaryContainer = LightCream,
    onSecondaryContainer = MediumBrown,
    tertiary = GoldenAccent,
    onTertiary = EspressoBlack,
    background = CreamWhite,
    onBackground = EspressoBlack,
    surface = Color.White,
    onSurface = EspressoBlack,
    surfaceVariant = LightCream,
    onSurfaceVariant = MediumBrown,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun CoffeeShotTimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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