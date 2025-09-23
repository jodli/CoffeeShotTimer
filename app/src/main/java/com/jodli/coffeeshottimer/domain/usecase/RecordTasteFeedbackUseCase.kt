package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.repository.ShotRepository
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import com.jodli.coffeeshottimer.domain.model.TasteSecondary
import javax.inject.Inject

/**
 * Use case for recording taste feedback for an espresso shot.
 * Handles the business logic of saving taste data to the repository.
 */
class RecordTasteFeedbackUseCase @Inject constructor(
    private val shotRepository: ShotRepository
) {

    /**
     * Record taste feedback for a specific shot.
     *
     * @param shotId The ID of the shot to update
     * @param tastePrimary The primary taste feedback (Sour/Perfect/Bitter)
     * @param tasteSecondary Optional secondary taste qualifier (Weak/Strong)
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(
        shotId: String,
        tastePrimary: TastePrimary,
        tasteSecondary: TasteSecondary? = null
    ): Result<Unit> {
        return shotRepository.updateTasteFeedback(
            shotId = shotId,
            tastePrimary = tastePrimary,
            tasteSecondary = tasteSecondary
        )
    }

    /**
     * Clear taste feedback for a specific shot.
     *
     * @param shotId The ID of the shot to update
     * @return Result indicating success or failure
     */
    suspend fun clearTasteFeedback(shotId: String): Result<Unit> {
        return shotRepository.updateTasteFeedback(
            shotId = shotId,
            tastePrimary = null,
            tasteSecondary = null
        )
    }
}
