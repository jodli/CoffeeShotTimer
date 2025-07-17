package com.example.coffeeshottimer.domain.usecase

import com.example.coffeeshottimer.data.model.Bean
import com.example.coffeeshottimer.data.repository.BeanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for retrieving coffee bean history with filtering and sorting capabilities.
 * Provides business logic for displaying all beans (active and inactive) with historical context.
 */
@Singleton
class GetBeanHistoryUseCase @Inject constructor(
    private val beanRepository: BeanRepository
) {
    
    /**
     * Get all beans (active and inactive) ordered by creation date (newest first).
     * @return Flow of Result containing list of all beans
     */
    fun execute(): Flow<Result<List<Bean>>> {
        return beanRepository.getAllBeans()
            .map { result ->
                if (result.isSuccess) {
                    val beans = result.getOrNull() ?: emptyList()
                    Result.success(beans.sortedByDescending { it.createdAt })
                } else {
                    result
                }
            }
            .catch { exception ->
                emit(Result.failure(BeanUseCaseException.UnknownError("Failed to get bean history", exception)))
            }
    }
    
    /**
     * Get bean history with search filtering.
     * @param searchQuery Search query to filter bean names (case-insensitive)
     * @param activeOnly Whether to show only active beans
     * @return Flow of Result containing filtered list of beans
     */
    fun getBeanHistoryWithSearch(
        searchQuery: String,
        activeOnly: Boolean = false
    ): Flow<Result<List<Bean>>> {
        return beanRepository.getFilteredBeans(activeOnly = activeOnly, searchQuery = searchQuery)
            .map { result ->
                if (result.isSuccess) {
                    val beans = result.getOrNull() ?: emptyList()
                    Result.success(beans.sortedByDescending { it.createdAt })
                } else {
                    result
                }
            }
            .catch { exception ->
                emit(Result.failure(BeanUseCaseException.UnknownError("Failed to get filtered bean history", exception)))
            }
    }
    
    /**
     * Get beans grouped by active status.
     * @return Flow of Result containing map of active status to beans
     */
    fun getBeansGroupedByStatus(): Flow<Result<Map<Boolean, List<Bean>>>> {
        return beanRepository.getAllBeans()
            .map { result ->
                if (result.isSuccess) {
                    val beans = result.getOrNull() ?: emptyList()
                    val groupedBeans = beans
                        .sortedByDescending { it.createdAt }
                        .groupBy { it.isActive }
                    Result.success(groupedBeans)
                } else {
                    Result.failure(result.exceptionOrNull() ?: BeanUseCaseException.UnknownError("Failed to get beans"))
                }
            }
            .catch { exception ->
                emit(Result.failure(BeanUseCaseException.UnknownError("Failed to group beans by status", exception)))
            }
    }
    
    /**
     * Get beans filtered by roast date range.
     * @param startDate Start date for filtering (inclusive)
     * @param endDate End date for filtering (inclusive)
     * @return Flow of Result containing filtered list of beans
     */
    fun getBeansByRoastDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<List<Bean>>> {
        return beanRepository.getAllBeans()
            .map { result ->
                if (result.isSuccess) {
                    val beans = result.getOrNull() ?: emptyList()
                    val filteredBeans = beans.filter { bean ->
                        !bean.roastDate.isBefore(startDate) && !bean.roastDate.isAfter(endDate)
                    }
                    Result.success(filteredBeans.sortedByDescending { it.roastDate })
                } else {
                    result
                }
            }
            .catch { exception ->
                emit(Result.failure(BeanUseCaseException.UnknownError("Failed to get beans by date range", exception)))
            }
    }
    
    /**
     * Get beans grouped by roast month for historical analysis.
     * @return Flow of Result containing map of year-month to beans
     */
    fun getBeansGroupedByRoastMonth(): Flow<Result<Map<String, List<Bean>>>> {
        return beanRepository.getAllBeans()
            .map { result ->
                if (result.isSuccess) {
                    val beans = result.getOrNull() ?: emptyList()
                    val groupedBeans = beans.groupBy { bean ->
                        "${bean.roastDate.year}-${bean.roastDate.monthValue.toString().padStart(2, '0')}"
                    }
                    Result.success(groupedBeans)
                } else {
                    Result.failure(result.exceptionOrNull() ?: BeanUseCaseException.UnknownError("Failed to get beans"))
                }
            }
            .catch { exception ->
                emit(Result.failure(BeanUseCaseException.UnknownError("Failed to group beans by roast month", exception)))
            }
    }
    
    /**
     * Get recently added beans (within the last 30 days).
     * @return Flow of Result containing list of recently added beans
     */
    fun getRecentlyAddedBeans(): Flow<Result<List<Bean>>> {
        val thirtyDaysAgo = LocalDate.now().minusDays(30)
        
        return beanRepository.getAllBeans()
            .map { result ->
                if (result.isSuccess) {
                    val beans = result.getOrNull() ?: emptyList()
                    val recentBeans = beans.filter { bean ->
                        bean.createdAt.toLocalDate().isAfter(thirtyDaysAgo) || 
                        bean.createdAt.toLocalDate().isEqual(thirtyDaysAgo)
                    }
                    Result.success(recentBeans.sortedByDescending { it.createdAt })
                } else {
                    result
                }
            }
            .catch { exception ->
                emit(Result.failure(BeanUseCaseException.UnknownError("Failed to get recently added beans", exception)))
            }
    }
    
    /**
     * Get beans with grinder settings history.
     * Useful for analyzing grinder setting patterns across different beans.
     * @return Flow of Result containing list of beans that have grinder settings
     */
    fun getBeansWithGrinderSettings(): Flow<Result<List<Bean>>> {
        return beanRepository.getAllBeans()
            .map { result ->
                if (result.isSuccess) {
                    val beans = result.getOrNull() ?: emptyList()
                    val beansWithSettings = beans.filter { !it.lastGrinderSetting.isNullOrBlank() }
                    Result.success(beansWithSettings.sortedByDescending { it.createdAt })
                } else {
                    result
                }
            }
            .catch { exception ->
                emit(Result.failure(BeanUseCaseException.UnknownError("Failed to get beans with grinder settings", exception)))
            }
    }
    
    /**
     * Get bean statistics for historical analysis.
     * @return Result containing BeanHistoryStats
     */
    suspend fun getBeanHistoryStats(): Result<BeanHistoryStats> {
        return try {
            val beansFlow = beanRepository.getAllBeans()
            var stats: BeanHistoryStats? = null
            
            beansFlow.collect { result ->
                if (result.isSuccess) {
                    val beans = result.getOrNull() ?: emptyList()
                    
                    val totalBeans = beans.size
                    val activeBeans = beans.count { it.isActive }
                    val inactiveBeans = totalBeans - activeBeans
                    val beansWithGrinderSettings = beans.count { !it.lastGrinderSetting.isNullOrBlank() }
                    
                    val freshBeans = beans.count { it.isFresh() }
                    val averageDaysSinceRoast = if (beans.isNotEmpty()) {
                        beans.map { it.daysSinceRoast() }.average()
                    } else {
                        0.0
                    }
                    
                    val oldestBean = beans.minByOrNull { it.roastDate }
                    val newestBean = beans.maxByOrNull { it.roastDate }
                    
                    stats = BeanHistoryStats(
                        totalBeans = totalBeans,
                        activeBeans = activeBeans,
                        inactiveBeans = inactiveBeans,
                        beansWithGrinderSettings = beansWithGrinderSettings,
                        freshBeans = freshBeans,
                        averageDaysSinceRoast = averageDaysSinceRoast,
                        oldestRoastDate = oldestBean?.roastDate,
                        newestRoastDate = newestBean?.roastDate
                    )
                }
            }
            
            Result.success(stats ?: BeanHistoryStats())
        } catch (exception: Exception) {
            Result.failure(BeanUseCaseException.UnknownError("Unexpected error getting bean history stats", exception))
        }
    }
    
    /**
     * Get inactive beans (for potential reactivation).
     * @return Flow of Result containing list of inactive beans
     */
    fun getInactiveBeans(): Flow<Result<List<Bean>>> {
        return beanRepository.getAllBeans()
            .map { result ->
                if (result.isSuccess) {
                    val beans = result.getOrNull() ?: emptyList()
                    val inactiveBeans = beans.filter { !it.isActive }
                    Result.success(inactiveBeans.sortedByDescending { it.createdAt })
                } else {
                    result
                }
            }
            .catch { exception ->
                emit(Result.failure(BeanUseCaseException.UnknownError("Failed to get inactive beans", exception)))
            }
    }
    
    /**
     * Search beans by partial name match (case-insensitive).
     * @param query Search query
     * @return Flow of Result containing matching beans
     */
    fun searchBeansByName(query: String): Flow<Result<List<Bean>>> {
        return if (query.trim().isEmpty()) {
            execute() // Return all beans if query is empty
        } else {
            getBeanHistoryWithSearch(query.trim(), activeOnly = false)
        }
    }
}

/**
 * Data class containing statistical information about bean history.
 */
data class BeanHistoryStats(
    val totalBeans: Int = 0,
    val activeBeans: Int = 0,
    val inactiveBeans: Int = 0,
    val beansWithGrinderSettings: Int = 0,
    val freshBeans: Int = 0,
    val averageDaysSinceRoast: Double = 0.0,
    val oldestRoastDate: LocalDate? = null,
    val newestRoastDate: LocalDate? = null
)