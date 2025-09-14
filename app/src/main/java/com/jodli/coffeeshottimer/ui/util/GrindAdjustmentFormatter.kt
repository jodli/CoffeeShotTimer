package com.jodli.coffeeshottimer.ui.util

import android.content.Context
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.domain.model.AdjustmentDirection
import com.jodli.coffeeshottimer.domain.model.GrindAdjustmentRecommendation
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import kotlin.math.abs

/**
 * UI formatter for GrindAdjustmentRecommendation.
 * Handles localized formatting of grind adjustment data for display in UI components.
 */
class GrindAdjustmentFormatter(private val context: Context) {

    /**
     * Format the adjustment description for display.
     */
    fun formatAdjustmentDescription(recommendation: GrindAdjustmentRecommendation): String {
        return when (recommendation.adjustmentDirection) {
            AdjustmentDirection.FINER -> {
                if (recommendation.adjustmentSteps == 1) {
                    context.getString(R.string.grind_adjustment_finer_single)
                } else {
                    context.getString(R.string.grind_adjustment_finer_multiple, recommendation.adjustmentSteps)
                }
            }
            AdjustmentDirection.COARSER -> {
                if (recommendation.adjustmentSteps == 1) {
                    context.getString(R.string.grind_adjustment_coarser_single)
                } else {
                    context.getString(R.string.grind_adjustment_coarser_multiple, recommendation.adjustmentSteps)
                }
            }
            AdjustmentDirection.NO_CHANGE -> {
                context.getString(R.string.grind_adjustment_no_change)
            }
        }
    }

    /**
     * Format the adjustment summary for display.
     */
    fun formatAdjustmentSummary(recommendation: GrindAdjustmentRecommendation): String {
        return when (recommendation.adjustmentDirection) {
            AdjustmentDirection.FINER -> {
                if (recommendation.adjustmentSteps == 1) {
                    context.getString(R.string.grind_adjustment_summary_finer_single, recommendation.suggestedGrindSetting)
                } else {
                    context.getString(R.string.grind_adjustment_summary_finer_multiple, recommendation.adjustmentSteps, recommendation.suggestedGrindSetting)
                }
            }
            AdjustmentDirection.COARSER -> {
                if (recommendation.adjustmentSteps == 1) {
                    context.getString(R.string.grind_adjustment_summary_coarser_single, recommendation.suggestedGrindSetting)
                } else {
                    context.getString(R.string.grind_adjustment_summary_coarser_multiple, recommendation.adjustmentSteps, recommendation.suggestedGrindSetting)
                }
            }
            AdjustmentDirection.NO_CHANGE -> {
                context.getString(R.string.grind_adjustment_summary_no_change)
            }
        }
    }

    /**
     * Generate a complete explanation for the recommendation.
     * This replaces the hardcoded strings that were previously in the domain layer.
     */
    fun formatExplanation(recommendation: GrindAdjustmentRecommendation): String {
        val tasteDescription = when (recommendation.tasteIssue) {
            TastePrimary.SOUR -> context.getString(R.string.recommendation_taste_sour)
            TastePrimary.BITTER -> context.getString(R.string.recommendation_taste_bitter)
            TastePrimary.PERFECT -> context.getString(R.string.recommendation_taste_perfect)
            null -> context.getString(R.string.recommendation_timing_issue)
        }
        
        val timeDescription = when {
            recommendation.extractionTimeDeviation < 0 -> {
                context.getString(
                    R.string.recommendation_time_too_fast,
                    abs(recommendation.extractionTimeDeviation)
                )
            }
            recommendation.extractionTimeDeviation > 0 -> {
                context.getString(
                    R.string.recommendation_time_too_slow,
                    recommendation.extractionTimeDeviation
                )
            }
            else -> {
                // Calculate actual extraction time from deviation (deviation = actual - optimal_mid)
                // Since deviation is 0, we're in optimal range, use a reasonable default
                context.getString(R.string.recommendation_time_good, "27")
            }
        }
        
        val actionDescription = when (recommendation.adjustmentDirection) {
            AdjustmentDirection.FINER -> context.getString(R.string.recommendation_action_finer)
            AdjustmentDirection.COARSER -> context.getString(R.string.recommendation_action_coarser)
            AdjustmentDirection.NO_CHANGE -> context.getString(R.string.recommendation_action_no_change)
        }
        
        return context.getString(R.string.recommendation_explanation_format, tasteDescription, timeDescription, actionDescription)
    }
}
