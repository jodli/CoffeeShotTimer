package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.model.PaginatedResult
import com.jodli.coffeeshottimer.data.model.PaginationConfig
import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.data.repository.ShotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for retrieving shot history with filtering capabilities.
 * Provides methods to get shots with various filtering options including date range,
 * bean type, grinder settings, and brew ratio ranges.
 */
@Singleton
class GetShotHistoryUseCase @Inject constructor(
    private val shotRepository: ShotRepository
) {

    /**
     * Get all shots ordered by timestamp (newest first).
     * @return Flow of Result containing list of shots
     */
    fun getAllShots(): Flow<Result<List<Shot>>> {
        return shotRepository.getAllShots()
    }

    /**
     * Get shots with comprehensive filtering options.
     * @param filter ShotHistoryFilter containing all filter criteria
     * @return Flow of Result containing filtered shots
     */
    fun getFilteredShots(filter: ShotHistoryFilter): Flow<Result<List<Shot>>> {
        return shotRepository.getFilteredShots(
            beanId = filter.beanId,
            startDate = filter.startDate,
            endDate = filter.endDate
        ).map { result ->
            result.map { shots ->
                // Apply additional client-side filtering for complex criteria
                var filteredShots = shots

                // Filter by grinder setting if specified
                filter.grinderSetting?.let { setting ->
                    filteredShots = filteredShots.filter { shot ->
                        shot.grinderSetting.equals(setting, ignoreCase = true)
                    }
                }

                // Filter by brew ratio range if specified
                if (filter.minBrewRatio != null || filter.maxBrewRatio != null) {
                    filteredShots = filteredShots.filter { shot ->
                        val ratio = shot.brewRatio
                        val minCheck = filter.minBrewRatio?.let { ratio >= it } ?: true
                        val maxCheck = filter.maxBrewRatio?.let { ratio <= it } ?: true
                        minCheck && maxCheck
                    }
                }

                // Filter by extraction time range if specified
                if (filter.minExtractionTime != null || filter.maxExtractionTime != null) {
                    filteredShots = filteredShots.filter { shot ->
                        val time = shot.extractionTimeSeconds
                        val minCheck = filter.minExtractionTime?.let { time >= it } ?: true
                        val maxCheck = filter.maxExtractionTime?.let { time <= it } ?: true
                        minCheck && maxCheck
                    }
                }

                // Filter by optimal extraction time if specified
                if (filter.onlyOptimalExtractionTime == true) {
                    filteredShots = filteredShots.filter { it.isOptimalExtractionTime() }
                }

                // Filter by typical brew ratio if specified
                if (filter.onlyTypicalBrewRatio == true) {
                    filteredShots = filteredShots.filter { it.isTypicalBrewRatio() }
                }

                // Apply limit if specified
                filter.limit?.let { limit ->
                    filteredShots = filteredShots.take(limit)
                }

                filteredShots
            }
        }
    }

    // PERFORMANCE OPTIMIZATION METHODS

    /**
     * Get shots with pagination support for large datasets.
     * @param paginationConfig Configuration for pagination
     * @return Result containing paginated shots
     */
    suspend fun getShotsPaginated(paginationConfig: PaginationConfig): Result<PaginatedResult<Shot>> {
        return shotRepository.getShotsPaginated(paginationConfig)
    }

    /**
     * Get filtered shots with pagination support for large datasets.
     * @param beanId Optional bean ID filter
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @param paginationConfig Configuration for pagination
     * @return Result containing paginated filtered shots
     */
    suspend fun getFilteredShotsPaginated(
        beanId: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        paginationConfig: PaginationConfig
    ): Result<PaginatedResult<Shot>> {
        return shotRepository.getFilteredShotsPaginated(
            beanId,
            startDate,
            endDate,
            paginationConfig
        )
    }
}

/**
 * Data class representing shot history filtering criteria.
 */
data class ShotHistoryFilter(
    val beanId: String? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val grinderSetting: String? = null,
    val minBrewRatio: Double? = null,
    val maxBrewRatio: Double? = null,
    val minExtractionTime: Int? = null,
    val maxExtractionTime: Int? = null,
    val onlyOptimalExtractionTime: Boolean? = null,
    val onlyTypicalBrewRatio: Boolean? = null,
    val limit: Int? = null
) {
    /**
     * Check if any filters are applied.
     * @return true if at least one filter is set
     */
    fun hasFilters(): Boolean {
        return beanId != null ||
            startDate != null ||
            endDate != null ||
            grinderSetting != null ||
            minBrewRatio != null ||
            maxBrewRatio != null ||
            minExtractionTime != null ||
            maxExtractionTime != null ||
            onlyOptimalExtractionTime == true ||
            onlyTypicalBrewRatio == true
    }
}
