package com.jodli.coffeeshottimer.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

/**
 * Entity representing a grind recommendation associated with a shot.
 * Tracks whether the user followed the app's recommendation and the outcome.
 *
 * This separate entity allows for:
 * - Clean separation from core Shot data
 * - Easy extensibility for ML/analytics features
 * - Optional relationship (not all shots have recommendations)
 */
@Entity(
    tableName = "shot_recommendations",
    foreignKeys = [
        ForeignKey(
            entity = Shot::class,
            parentColumns = ["id"],
            childColumns = ["shotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["shotId"], unique = true), // One-to-one relationship
        Index(value = ["timestamp"]),
        Index(value = ["wasFollowed"])
    ]
)
data class ShotRecommendation(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    /** The shot this recommendation is associated with */
    val shotId: String,

    /** The grind setting that was recommended before this shot */
    val recommendedGrindSetting: String,

    /** Direction of adjustment (FINER, COARSER, NO_CHANGE) */
    val adjustmentDirection: String,

    /** Whether the user followed the recommendation (within ±0.1 tolerance) */
    val wasFollowed: Boolean,

    /** Confidence level of the recommendation (HIGH, MEDIUM, LOW) */
    val confidenceLevel: String,

    /** Code indicating why this recommendation was made (e.g., TIME_TOO_FAST, TASTE_SOUR) */
    val reasonCode: String,

    /** When the recommendation was created/stored */
    val timestamp: LocalDateTime = LocalDateTime.now(),

    /** Optional JSON metadata for future extensibility (ML features, A/B test data, etc.) */
    val metadata: String? = null
) {
    /**
     * Check if this was a high-confidence recommendation.
     */
    fun isHighConfidence(): Boolean = confidenceLevel == "HIGH"

    /**
     * Get a human-readable summary of this recommendation.
     */
    fun getSummary(): String {
        val followedText = if (wasFollowed) "Followed" else "Ignored"
        return "$followedText recommendation: $adjustmentDirection to $recommendedGrindSetting"
    }
}
