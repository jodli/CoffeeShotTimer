package com.jodli.coffeeshottimer.domain.model

/**
 * Represents a specific grind adjustment recommendation based on shot performance.
 * This model encapsulates all the information needed to provide actionable grind advice
 * to users based on their taste feedback and extraction metrics.
 */
data class GrindAdjustmentRecommendation(
    val currentGrindSetting: String,
    val suggestedGrindSetting: String,
    val adjustmentDirection: AdjustmentDirection,
    val adjustmentSteps: Int,
    val explanation: String,
    val extractionTimeDeviation: Int, // seconds off from optimal (25-30s)
    val tasteIssue: TastePrimary?,
    val confidence: ConfidenceLevel
) {
    /**
     * Check if this recommendation suggests any change.
     * @return true if an adjustment is recommended, false for no-change recommendations
     */
    fun hasAdjustment(): Boolean = adjustmentDirection != AdjustmentDirection.NO_CHANGE
    
}

/**
 * Direction of grind adjustment recommendation.
 * Based on extraction theory: finer grind increases extraction, coarser grind decreases extraction.
 */
enum class AdjustmentDirection {
    /** Grind finer to increase extraction (for sour/under-extracted shots) */
    FINER,
    
    /** Grind coarser to decrease extraction (for bitter/over-extracted shots) */
    COARSER,
    
    /** No adjustment needed (optimal extraction achieved) */
    NO_CHANGE
}

/**
 * Confidence level of the recommendation based on available evidence.
 * Higher confidence recommendations should be displayed more prominently.
 */
enum class ConfidenceLevel {
    /** Strong evidence from both taste feedback and extraction time */
    HIGH,
    
    /** Evidence from either taste feedback or extraction time */
    MEDIUM,
    
    /** Limited evidence, edge case, or conflicting signals */
    LOW
}
