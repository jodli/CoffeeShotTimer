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
                emit(
                    Result.failure(
                        DomainException(
                            DomainErrorCode.FAILED_TO_GET_ACTIVE_BEANS,
                            "Failed to get active beans",
                            exception
                        )
                    )
                )
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
                emit(
                    Result.failure(
                        DomainException(
                            DomainErrorCode.FAILED_TO_GET_ACTIVE_BEANS,
                            "Failed to get filtered active beans",
                            exception
                        )
                    )
                )
            }
    }
}
