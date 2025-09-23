package com.jodli.coffeeshottimer.ui.util

import android.content.Context
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.domain.usecase.RecommendationType
import com.jodli.coffeeshottimer.domain.usecase.ShotRecommendation

/**
 * Utility class for formatting shot recommendations into user-friendly strings.
 * Handles internationalization and contextual formatting.
 */
class RecommendationFormatter(private val context: Context) {

    /**
     * Format a single recommendation into a user-friendly string.
     */
    fun formatRecommendation(recommendation: ShotRecommendation): String {
        return when (recommendation.type) {
            RecommendationType.GRIND_FINER -> {
                val currentTime = recommendation.context["currentTime"] ?: "unknown"
                context.getString(R.string.recommendation_grind_finer, currentTime)
            }

            RecommendationType.GRIND_COARSER -> {
                val currentTime = recommendation.context["currentTime"] ?: "unknown"
                context.getString(R.string.recommendation_grind_coarser, currentTime)
            }

            RecommendationType.INCREASE_YIELD -> {
                val currentRatio = recommendation.context["currentRatio"] ?: "unknown"
                context.getString(R.string.recommendation_increase_yield, currentRatio)
            }

            RecommendationType.DECREASE_YIELD -> {
                val currentRatio = recommendation.context["currentRatio"] ?: "unknown"
                context.getString(R.string.recommendation_decrease_yield, currentRatio)
            }

            RecommendationType.RATIO_INCONSISTENCY -> {
                val deviation = recommendation.context["deviation"] ?: "unknown"
                val avgRatio = recommendation.context["avgRatio"] ?: "unknown"
                context.getString(R.string.recommendation_ratio_inconsistency, deviation, avgRatio)
            }

            RecommendationType.DOSE_ADJUSTMENT -> {
                context.getString(R.string.recommendation_dose_adjustment)
            }

            RecommendationType.TIMING_CONSISTENCY -> {
                context.getString(R.string.recommendation_timing_consistency)
            }
        }
    }

    /**
     * Format multiple recommendations with priority sorting (HIGH → MEDIUM → LOW).
     */
    fun formatRecommendations(recommendations: List<ShotRecommendation>): List<FormattedRecommendation> {
        return recommendations.map { recommendation ->
            FormattedRecommendation(
                text = formatRecommendation(recommendation),
                priority = recommendation.priority,
                type = recommendation.type
            )
        }.sortedWith(compareBy { getPriorityOrder(it.priority) })
    }

    /**
     * Get priority order for sorting (lower number = higher priority).
     */
    private fun getPriorityOrder(priority: com.jodli.coffeeshottimer.domain.usecase.RecommendationPriority): Int {
        return when (priority) {
            com.jodli.coffeeshottimer.domain.usecase.RecommendationPriority.HIGH -> 0
            com.jodli.coffeeshottimer.domain.usecase.RecommendationPriority.MEDIUM -> 1
            com.jodli.coffeeshottimer.domain.usecase.RecommendationPriority.LOW -> 2
        }
    }
}

/**
 * Formatted recommendation for UI display.
 */
data class FormattedRecommendation(
    val text: String,
    val priority: com.jodli.coffeeshottimer.domain.usecase.RecommendationPriority,
    val type: RecommendationType
)
