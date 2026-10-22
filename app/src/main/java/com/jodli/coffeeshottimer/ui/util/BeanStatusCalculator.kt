package com.jodli.coffeeshottimer.ui.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.domain.usecase.GetShotQualityAnalysisUseCase
import com.jodli.coffeeshottimer.domain.usecase.QualityTier
import com.jodli.coffeeshottimer.ui.theme.zoneColors

/**
 * Represents the status of a coffee bean based on shot quality analysis.
 */
enum class BeanStatus {
    DIALED_IN, // Consistent good results - avg quality >= 70, all shots >= 60
    EXPERIMENTING, // Early stage or inconsistent - mixed results or not enough data
    NEEDS_WORK, // Poor recent results - avg quality < 40
    FRESH_START // No shots recorded yet
}

fun BeanStatus.labelRes(): Int = when (this) {
    BeanStatus.DIALED_IN -> R.string.bean_status_dialed_in_short
    BeanStatus.EXPERIMENTING -> R.string.bean_status_experimenting_short
    BeanStatus.NEEDS_WORK -> R.string.bean_status_needs_work_short
    BeanStatus.FRESH_START -> R.string.bean_status_fresh_start_short
}

/**
 * Constants for bean status calculation.
 * These are shared with ShotHistoryViewModel coaching insights for consistency.
 */
object BeanStatusConstants {
    const val DIAL_IN_CONSISTENCY_THRESHOLD = 70
    const val DIAL_IN_MIN_SCORE = 60
    const val NEEDS_WORK_THRESHOLD = 40
    const val MIN_SHOTS_FOR_DIAL_IN = 3
    const val SCORE_PERFECT_THRESHOLD = 80
    const val SCORE_GOOD_THRESHOLD = QualityTier.GOOD_MIN_SCORE
}

/**
 * Calculate the status of a bean based on its shot history.
 *
 * @param shots List of shots for this bean (can be unsorted)
 * @param qualityAnalysisUseCase Use case for calculating shot quality scores
 * @return BeanStatus representing the current state of the bean
 */
fun calculateBeanStatus(
    shots: List<Shot>,
    qualityAnalysisUseCase: GetShotQualityAnalysisUseCase
): BeanStatus {
    // Handle early return cases
    if (shots.isEmpty() || shots.size < BeanStatusConstants.MIN_SHOTS_FOR_DIAL_IN) {
        return if (shots.isEmpty()) BeanStatus.FRESH_START else BeanStatus.EXPERIMENTING
    }

    // Analyze the last 3 shots
    val recentShots = shots.sortedBy { it.timestamp }.takeLast(BeanStatusConstants.MIN_SHOTS_FOR_DIAL_IN)
    val qualityScores = recentShots.map { shot ->
        qualityAnalysisUseCase.calculateShotQualityScore(shot, shots)
    }
    val avgQuality = qualityScores.average().toInt()
    val isDialedIn = avgQuality >= BeanStatusConstants.DIAL_IN_CONSISTENCY_THRESHOLD &&
        qualityScores.all { it >= BeanStatusConstants.DIAL_IN_MIN_SCORE }
    val needsWork = avgQuality < BeanStatusConstants.NEEDS_WORK_THRESHOLD

    return when {
        isDialedIn -> BeanStatus.DIALED_IN
        needsWork -> BeanStatus.NEEDS_WORK
        else -> BeanStatus.EXPERIMENTING
    }
}

/**
 * Get the theme color for a bean status indicator.
 *
 * @param status The bean status to get a color for
 * @return Color from theme constants
 */
@Composable
fun getStatusColor(status: BeanStatus): Color {
    return when (status) {
        // quality scale, not the zone colours: "needs work" isn't the same thing as "bitter"
        BeanStatus.DIALED_IN -> MaterialTheme.zoneColors.qualityGood
        BeanStatus.EXPERIMENTING -> MaterialTheme.zoneColors.qualityFair
        BeanStatus.NEEDS_WORK -> MaterialTheme.colorScheme.onSurfaceVariant
        BeanStatus.FRESH_START -> MaterialTheme.zoneColors.idle
    }
}
