package com.jodli.coffeeshottimer.ui.util

import android.content.Context
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.domain.model.AdjustmentDirection
import com.jodli.coffeeshottimer.domain.model.GrindAdjustmentRecommendation

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
}
