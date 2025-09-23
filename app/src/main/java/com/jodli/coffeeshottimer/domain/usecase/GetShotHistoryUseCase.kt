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
     * Get shots for a specific bean ordered by timestamp.
     * @param beanId The ID of the bean to filter by
     * @return Flow of Result containing filtered shots
     */
    fun getShotsByBean(beanId: String): Flow<Result<List<Shot>>> {
        return shotRepository.getShotsByBean(beanId)
    }

    /**
     * Get recent shots with a specified limit.
     * @param limit Maximum number of shots to return
     * @return Flow of Result containing recent shots
     */
    fun getRecentShots(limit: Int = 10): Flow<Result<List<Shot>>> {
        return shotRepository.getRecentShots(limit)
    }

    /**
     * Get shots within a specific date range.
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @return Flow of Result containing shots in the date range
     */
    fun getShotsByDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<Result<List<Shot>>> {
        return shotRepository.getShotsByDateRange(startDate, endDate)
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

    /**
     * Get shots by grinder setting.
     * @param grinderSetting The grinder setting to filter by
     * @return Flow of Result containing shots with the specified grinder setting
     */
    fun getShotsByGrinderSetting(grinderSetting: String): Flow<Result<List<Shot>>> {
        return shotRepository.getShotsByGrinderSetting(grinderSetting)
    }

    /**
     * Get shots within a brew ratio range.
     * @param minRatio Minimum brew ratio (inclusive)
     * @param maxRatio Maximum brew ratio (inclusive)
     * @return Flow of Result containing shots within the brew ratio range
     */
    fun getShotsByBrewRatioRange(
        minRatio: Double,
        maxRatio: Double
    ): Flow<Result<List<Shot>>> {
        return shotRepository.getShotsByBrewRatioRange(minRatio, maxRatio)
    }

    /**
     * Get shots within an extraction time range.
     * @param minSeconds Minimum extraction time in seconds (inclusive)
     * @param maxSeconds Maximum extraction time in seconds (inclusive)
     * @return Flow of Result containing shots within the extraction time range
     */
    fun getShotsByExtractionTimeRange(
        minSeconds: Int,
        maxSeconds: Int
    ): Flow<Result<List<Shot>>> {
        return shotRepository.getShotsByExtractionTimeRange(minSeconds, maxSeconds)
    }

    /**
     * Get shots with optimal extraction time (25-30 seconds).
     * @return Flow of Result containing shots with optimal extraction time
     */
    fun getOptimalExtractionTimeShots(): Flow<Result<List<Shot>>> {
        return getShotsByExtractionTimeRange(25, 30)
    }

    /**
     * Get shots with typical brew ratio (1.5-3.0).
     * @return Flow of Result containing shots with typical brew ratio
     */
    fun getTypicalBrewRatioShots(): Flow<Result<List<Shot>>> {
        return getShotsByBrewRatioRange(1.5, 3.0)
    }

    /**
     * Search shots by notes content.
     * @param searchTerm The term to search for in shot notes
     * @return Flow of Result containing shots with matching notes
     */
    fun searchShotsByNotes(searchTerm: String): Flow<Result<List<Shot>>> {
        return getAllShots().map { result ->
            result.map { shots ->
                shots.filter { shot ->
                    shot.notes.contains(searchTerm, ignoreCase = true)
                }
            }
        }
    }

    /**
     * Get total shot count.
     * @return Result containing the total number of shots
     */
    suspend fun getTotalShotCount(): Result<Int> {
        return shotRepository.getTotalShotCount()
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

    /**
     * Create a copy with only date range filters.
     * @return ShotHistoryFilter with only date filters
     */
    fun dateRangeOnly(): ShotHistoryFilter {
        return copy(
            beanId = null,
            grinderSetting = null,
            minBrewRatio = null,
            maxBrewRatio = null,
            minExtractionTime = null,
            maxExtractionTime = null,
            onlyOptimalExtractionTime = null,
            onlyTypicalBrewRatio = null,
            limit = null
        )
    }

    /**
     * Create a copy with only bean filter.
     * @return ShotHistoryFilter with only bean filter
     */
    fun beanOnly(): ShotHistoryFilter {
        return copy(
            startDate = null,
            endDate = null,
            grinderSetting = null,
            minBrewRatio = null,
            maxBrewRatio = null,
            minExtractionTime = null,
            maxExtractionTime = null,
            onlyOptimalExtractionTime = null,
            onlyTypicalBrewRatio = null,
            limit = null
        )
    }
}
