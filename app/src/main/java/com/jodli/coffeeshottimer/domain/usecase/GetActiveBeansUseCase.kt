package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for retrieving active coffee beans with filtering and sorting capabilities.
 * Provides business logic for displaying active beans in the UI.
 */
@Singleton
class GetActiveBeansUseCase @Inject constructor(
    private val beanRepository: BeanRepository
) {
    
    /**
     * Get all active beans ordered by creation date (newest first).
     * @return Flow of Result containing list of active beans
     */
    fun execute(): Flow<Result<List<Bean>>> {
        return beanRepository.getActiveBeans()
            .map { result ->
                if (result.isSuccess) {
                    val beans = result.getOrNull() ?: emptyList()
                    Result.success(beans.sortedByDescending { it.createdAt })
                } else {
                    result
                }
            }
            .catch { exception ->
                emit(Result.failure(BeanUseCaseException.UnknownError("Failed to get active beans", exception)))
            }
    }
    
    /**
     * Get active beans with search filtering.
     * @param searchQuery Search query to filter bean names (case-insensitive)
     * @return Flow of Result containing filtered list of active beans
     */
    fun getActiveBeansWithSearch(searchQuery: String): Flow<Result<List<Bean>>> {
        return beanRepository.getFilteredBeans(activeOnly = true, searchQuery = searchQuery)
            .map { result ->
                if (result.isSuccess) {
                    val beans = result.getOrNull() ?: emptyList()
                    Result.success(beans.sortedByDescending { it.createdAt })
                } else {
                    result
                }
            }
            .catch { exception ->
                emit(Result.failure(BeanUseCaseException.UnknownError("Failed to get filtered active beans", exception)))
            }
    }
    
    /**
     * Get active beans sorted by freshness (optimal brewing window first).
     * Beans are considered fresh 4-14 days after roasting.
     * @return Flow of Result containing list of active beans sorted by freshness
     */
    fun getActiveBeansByFreshness(): Flow<Result<List<BeanWithFreshness>>> {
        return beanRepository.getActiveBeans()
            .map { result ->
                if (result.isSuccess) {
                    val beans = result.getOrNull() ?: emptyList()
                    val beansWithFreshness = beans.map { bean ->
                        BeanWithFreshness(
                            bean = bean,
                            daysSinceRoast = bean.daysSinceRoast(),
                            isFresh = bean.isFresh(),
                            freshnessCategory = getFreshnessCategory(bean.daysSinceRoast())
                        )
                    }
                    
                    // Sort by freshness: fresh beans first, then by days since roast (ascending)
                    val sortedBeans = beansWithFreshness.sortedWith(
                        compareByDescending<BeanWithFreshness> { it.isFresh }
                            .thenBy { it.daysSinceRoast }
                    )
                    
                    Result.success(sortedBeans)
                } else {
                    Result.failure(result.exceptionOrNull() ?: BeanUseCaseException.UnknownError("Failed to get active beans"))
                }
            }
            .catch { exception ->
                emit(Result.failure(BeanUseCaseException.UnknownError("Failed to get active beans by freshness", exception)))
            }
    }
    
    /**
     * Get active beans that have grinder settings saved.
     * Useful for quick shot recording with remembered settings.
     * @return Flow of Result containing list of active beans with grinder settings
     */
    fun getActiveBeansWithGrinderSettings(): Flow<Result<List<Bean>>> {
        return beanRepository.getActiveBeans()
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
                emit(Result.failure(BeanUseCaseException.UnknownError("Failed to get active beans with grinder settings", exception)))
            }
    }
    
    /**
     * Get count of active beans.
     * @return Result containing the count of active beans
     */
    suspend fun getActiveBeanCount(): Result<Int> {
        return try {
            val result = beanRepository.getActiveBeanCount()
            if (result.isSuccess) {
                Result.success(result.getOrNull() ?: 0)
            } else {
                Result.failure(result.exceptionOrNull() ?: BeanUseCaseException.UnknownError("Failed to get active bean count"))
            }
        } catch (exception: Exception) {
            Result.failure(BeanUseCaseException.UnknownError("Unexpected error getting active bean count", exception))
        }
    }
    
    /**
     * Get the most recently added active bean.
     * Useful for auto-selecting the current bean in shot recording.
     * @return Result containing the most recent active bean or null if none exist
     */
    suspend fun getMostRecentActiveBean(): Result<Bean?> {
        return try {
            val beansFlow = beanRepository.getActiveBeans()
            var mostRecentBean: Bean? = null
            
            beansFlow.collect { result ->
                if (result.isSuccess) {
                    val beans = result.getOrNull() ?: emptyList()
                    mostRecentBean = beans.maxByOrNull { it.createdAt }
                }
            }
            
            Result.success(mostRecentBean)
        } catch (exception: Exception) {
            Result.failure(BeanUseCaseException.UnknownError("Unexpected error getting most recent active bean", exception))
        }
    }
    
    /**
     * Check if there are any active beans available.
     * @return Result indicating if active beans exist
     */
    suspend fun hasActiveBeans(): Result<Boolean> {
        return try {
            val countResult = getActiveBeanCount()
            if (countResult.isSuccess) {
                Result.success((countResult.getOrNull() ?: 0) > 0)
            } else {
                Result.failure(countResult.exceptionOrNull() ?: BeanUseCaseException.UnknownError("Failed to check for active beans"))
            }
        } catch (exception: Exception) {
            Result.failure(BeanUseCaseException.UnknownError("Unexpected error checking for active beans", exception))
        }
    }
    
    /**
     * Get active beans grouped by freshness category.
     * @return Flow of Result containing map of freshness categories to beans
     */
    fun getActiveBeansGroupedByFreshness(): Flow<Result<Map<FreshnessCategory, List<Bean>>>> {
        return beanRepository.getActiveBeans()
            .map { result ->
                if (result.isSuccess) {
                    val beans = result.getOrNull() ?: emptyList()
                    val groupedBeans = beans.groupBy { bean ->
                        getFreshnessCategory(bean.daysSinceRoast())
                    }
                    Result.success(groupedBeans)
                } else {
                    Result.failure(result.exceptionOrNull() ?: BeanUseCaseException.UnknownError("Failed to get active beans"))
                }
            }
            .catch { exception ->
                emit(Result.failure(BeanUseCaseException.UnknownError("Failed to group active beans by freshness", exception)))
            }
    }
    
    /**
     * Determine the freshness category based on days since roast.
     * @param daysSinceRoast Number of days since the bean was roasted
     * @return FreshnessCategory enum value
     */
    private fun getFreshnessCategory(daysSinceRoast: Long): FreshnessCategory {
        return when {
            daysSinceRoast < 4 -> FreshnessCategory.TOO_FRESH
            daysSinceRoast in 4..14 -> FreshnessCategory.OPTIMAL
            daysSinceRoast in 15..30 -> FreshnessCategory.GOOD
            daysSinceRoast in 31..60 -> FreshnessCategory.ACCEPTABLE
            else -> FreshnessCategory.STALE
        }
    }
}

/**
 * Data class representing a bean with freshness information.
 */
data class BeanWithFreshness(
    val bean: Bean,
    val daysSinceRoast: Long,
    val isFresh: Boolean,
    val freshnessCategory: FreshnessCategory
)

/**
 * Enum representing different freshness categories for coffee beans.
 */
enum class FreshnessCategory(val displayName: String, val description: String) {
    TOO_FRESH("Too Fresh", "Needs more time to degas (< 4 days)"),
    OPTIMAL("Optimal", "Perfect for espresso (4-14 days)"),
    GOOD("Good", "Still great for brewing (15-30 days)"),
    ACCEPTABLE("Acceptable", "Usable but past peak (31-60 days)"),
    STALE("Stale", "Consider replacing (> 60 days)")
}