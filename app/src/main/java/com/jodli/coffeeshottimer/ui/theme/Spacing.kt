package com.jodli.coffeeshottimer.ui.theme

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
    // Landscape-specific values
    val landscapeContentSpacing: Dp = 12.dp
)
