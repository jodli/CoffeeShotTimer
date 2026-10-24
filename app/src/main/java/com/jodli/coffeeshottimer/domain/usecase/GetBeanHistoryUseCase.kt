package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.domain.exception.DomainException
import com.jodli.coffeeshottimer.domain.model.DomainErrorCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
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
                emit(
                    Result.failure(
                        DomainException(
                            DomainErrorCode.FAILED_TO_GET_ACTIVE_BEANS,
                            "Failed to get bean history",
                            exception
                        )
                    )
                )
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
                emit(
                    Result.failure(
                        DomainException(
                            DomainErrorCode.FAILED_TO_GET_ACTIVE_BEANS,
                            "Failed to get filtered bean history",
                            exception
                        )
                    )
                )
            }
    }
}
