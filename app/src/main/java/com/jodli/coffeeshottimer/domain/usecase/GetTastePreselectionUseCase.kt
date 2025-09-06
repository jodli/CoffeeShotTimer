package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.domain.model.TastePrimary
import javax.inject.Inject

/**
 * Use case for determining the recommended taste feedback based on extraction time.
 * Maps extraction time ranges to likely taste outcomes to guide user feedback.
 */
class GetTastePreselectionUseCase @Inject constructor() {

    /**
     * Get the recommended taste preselection based on extraction time.
     * 
     * @param extractionTimeSeconds The extraction time in seconds
     * @return The recommended TastePrimary, or null if extraction time is invalid
     */
    operator fun invoke(extractionTimeSeconds: Double?): TastePrimary? {
        if (extractionTimeSeconds == null || extractionTimeSeconds <= 0.0) {
            return null
        }

        return when {
            extractionTimeSeconds < 25.0 -> TastePrimary.SOUR      // Under-extracted
            extractionTimeSeconds <= 30.0 -> TastePrimary.PERFECT  // Optimal range
            else -> TastePrimary.BITTER                            // Over-extracted
        }
    }

    /**
     * Get the recommended taste preselection based on extraction time in seconds (Int).
     * Convenience overload for Int values.
     */
    operator fun invoke(extractionTimeSeconds: Int?): TastePrimary? {
        return invoke(extractionTimeSeconds?.toDouble())
    }
}
