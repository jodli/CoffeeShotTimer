package com.jodli.coffeeshottimer.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.jodli.coffeeshottimer.domain.usecase.ShotRecommendation

/**
 * Extension functions for working with recommendations in Compose UI.
 */

/**
 * Format a list of recommendations for display in Compose UI.
 */
@Composable
fun List<ShotRecommendation>.formatForDisplay(): List<FormattedRecommendation> {
    val context = LocalContext.current
    val formatter = RecommendationFormatter(context)
    return formatter.formatRecommendations(this)
}

/**
 * Format a single recommendation for display in Compose UI.
 */
@Composable
fun ShotRecommendation.formatForDisplay(): String {
    val context = LocalContext.current
    val formatter = RecommendationFormatter(context)
    return formatter.formatRecommendation(this)
}
