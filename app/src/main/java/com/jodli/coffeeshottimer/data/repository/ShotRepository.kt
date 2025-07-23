package com.jodli.coffeeshottimer.data.repository

import com.jodli.coffeeshottimer.data.dao.BeanDao
import com.jodli.coffeeshottimer.data.dao.ShotDao
import com.jodli.coffeeshottimer.data.dao.ShotStatistics
import com.jodli.coffeeshottimer.data.model.PaginatedResult
import com.jodli.coffeeshottimer.data.model.PaginationConfig
import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.data.model.ValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Shot entity operations.
 * Provides business logic layer between UI and data access layer.
 * Handles offline-first data access and error management.
 */
@Singleton
class ShotRepository @Inject constructor(
    private val shotDao: ShotDao,
    private val beanDao: BeanDao
) {

    /**
     * Get all shots ordered by timestamp.
     * @return Flow of list of shots with error handling
     */
    fun getAllShots(): Flow<Result<List<Shot>>> = flow {
        shotDao.getAllShots().collect { shots ->
            emit(Result.success(shots))
        }
    }.catch { exception ->
        emit(
            Result.failure(
                RepositoryException.DatabaseError(
                    "Failed to get all shots",
                    exception
                )
            )
        )
    }

    /**
     * Get shots for a specific bean ordered by timestamp.
     * @param beanId The ID of the bean
     * @return Flow of list of shots for the bean with error handling
     */
    fun getShotsByBean(beanId: String): Flow<Result<List<Shot>>> = flow {
        if (beanId.isBlank()) {
            emit(Result.failure(RepositoryException.ValidationError("Bean ID cannot be empty")))
            return@flow
        }

        shotDao.getShotsByBean(beanId).collect { shots ->
            emit(Result.success(shots))
        }
    }.catch { exception ->
        emit(
            Result.failure(
                RepositoryException.DatabaseError(
                    "Failed to get shots by bean",
                    exception
                )
            )
        )
    }

    /**
     * Get a specific shot by ID.
     * @param shotId The ID of the shot to retrieve
     * @return Result containing the shot or error
     */
    suspend fun getShotById(shotId: String): Result<Shot?> {
        return try {
            if (shotId.isBlank()) {
                Result.failure(RepositoryException.ValidationError("Shot ID cannot be empty"))
            } else {
                val shot = shotDao.getShotById(shotId)
                Result.success(shot)
            }
        } catch (exception: Exception) {
            Result.failure(RepositoryException.DatabaseError("Failed to get shot by ID", exception))
        }
    }

    /**
     * Get recent shots (last N shots).
     * @param limit Maximum number of shots to return
     * @return Flow of recent shots with error handling
     */
    fun getRecentShots(limit: Int): Flow<Result<List<Shot>>> = flow {
        if (limit <= 0) {
            emit(Result.failure(RepositoryException.ValidationError("Limit must be greater than 0")))
            return@flow
        }

        shotDao.getRecentShots(limit).collect { shots ->
            emit(Result.success(shots))
        }
    }.catch { exception ->
        emit(
            Result.failure(
                RepositoryException.DatabaseError(
                    "Failed to get recent shots",
                    exception
                )
            )
        )
    }

    /**
     * Get shots within a date range.
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @return Flow of shots in date range with error handling
     */
    fun getShotsByDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<Result<List<Shot>>> = flow {
        if (startDate.isAfter(endDate)) {
            emit(Result.failure(RepositoryException.ValidationError("Start date cannot be after end date")))
            return@flow
        }

        shotDao.getShotsByDateRange(startDate, endDate).collect { shots ->
            emit(Result.success(shots))
        }
    }.catch { exception ->
        emit(
            Result.failure(
                RepositoryException.DatabaseError(
                    "Failed to get shots by date range",
                    exception
                )
            )
        )
    }

    /**
     * Get shots filtered by bean and date range.
     * @param beanId Optional bean ID filter
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @return Flow of filtered shots with error handling
     */
    fun getFilteredShots(
        beanId: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?
    ): Flow<Result<List<Shot>>> = flow {
        // Validate date range if both dates are provided
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            emit(Result.failure(RepositoryException.ValidationError("Start date cannot be after end date")))
            return@flow
        }

        shotDao.getFilteredShots(beanId, startDate, endDate).collect { shots ->
            emit(Result.success(shots))
        }
    }.catch { exception ->
        emit(
            Result.failure(
                RepositoryException.DatabaseError(
                    "Failed to get filtered shots",
                    exception
                )
            )
        )
    }

    /**
     * Get shots by grinder setting.
     * @param grinderSetting The grinder setting to filter by
     * @return Flow of shots with the specified grinder setting
     */
    fun getShotsByGrinderSetting(grinderSetting: String): Flow<Result<List<Shot>>> = flow {
        if (grinderSetting.isBlank()) {
            emit(Result.failure(RepositoryException.ValidationError("Grinder setting cannot be empty")))
            return@flow
        }

        shotDao.getShotsByGrinderSetting(grinderSetting).collect { shots ->
            emit(Result.success(shots))
        }
    }.catch { exception ->
        emit(
            Result.failure(
                RepositoryException.DatabaseError(
                    "Failed to get shots by grinder setting",
                    exception
                )
            )
        )
    }

    /**
     * Record a new shot with validation and business logic.
     * @param shot The shot to record
     * @return Result indicating success or failure with validation errors
     */
    suspend fun recordShot(shot: Shot): Result<Unit> {
        return try {
            // Validate shot data
            val validationResult = shot.validate()
            if (!validationResult.isValid) {
                return Result.failure(
                    RepositoryException.ValidationError(
                        "Shot validation failed: ${validationResult.errors.joinToString(", ")}"
                    )
                )
            }

            // Verify that the bean exists
            val bean = beanDao.getBeanById(shot.beanId)
                ?: return Result.failure(RepositoryException.ValidationError("Selected bean does not exist"))

            // Record the shot
            shotDao.insertShot(shot)

            // Update the bean's last grinder setting
            beanDao.updateLastGrinderSetting(shot.beanId, shot.grinderSetting)

            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(RepositoryException.DatabaseError("Failed to record shot", exception))
        }
    }

    /**
     * Update an existing shot with validation.
     * @param shot The shot to update
     * @return Result indicating success or failure with validation errors
     */
    suspend fun updateShot(shot: Shot): Result<Unit> {
        return try {
            // Validate shot data
            val validationResult = shot.validate()
            if (!validationResult.isValid) {
                return Result.failure(
                    RepositoryException.ValidationError(
                        "Shot validation failed: ${validationResult.errors.joinToString(", ")}"
                    )
                )
            }

            // Check if shot exists
            val existingShot = shotDao.getShotById(shot.id)
                ?: return Result.failure(RepositoryException.NotFoundError("Shot not found"))

            // Verify that the bean exists
            val bean = beanDao.getBeanById(shot.beanId)
                ?: return Result.failure(RepositoryException.ValidationError("Selected bean does not exist"))

            shotDao.updateShot(shot)
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(RepositoryException.DatabaseError("Failed to update shot", exception))
        }
    }

    /**
     * Delete a shot.
     * @param shot The shot to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteShot(shot: Shot): Result<Unit> {
        return try {
            // Check if shot exists
            val existingShot = shotDao.getShotById(shot.id)
                ?: return Result.failure(RepositoryException.NotFoundError("Shot not found"))

            shotDao.deleteShot(shot)
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(RepositoryException.DatabaseError("Failed to delete shot", exception))
        }
    }

    /**
     * Delete all shots for a specific bean.
     * @param beanId The ID of the bean
     * @return Result indicating success or failure
     */
    suspend fun deleteShotsByBean(beanId: String): Result<Unit> {
        return try {
            if (beanId.isBlank()) {
                return Result.failure(RepositoryException.ValidationError("Bean ID cannot be empty"))
            }

            shotDao.deleteShotsByBean(beanId)
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(
                RepositoryException.DatabaseError(
                    "Failed to delete shots by bean",
                    exception
                )
            )
        }
    }

    /**
     * Get shot statistics for a specific bean.
     * @param beanId The ID of the bean
     * @return Result containing shot statistics or error
     */
    suspend fun getShotStatistics(beanId: String): Result<ShotStatistics?> {
        return try {
            if (beanId.isBlank()) {
                Result.failure(RepositoryException.ValidationError("Bean ID cannot be empty"))
            } else {
                val statistics = shotDao.getShotStatistics(beanId)
                Result.success(statistics)
            }
        } catch (exception: Exception) {
            Result.failure(
                RepositoryException.DatabaseError(
                    "Failed to get shot statistics",
                    exception
                )
            )
        }
    }

    /**
     * Get the last shot for a specific bean (for grinder setting memory).
     * @param beanId The ID of the bean
     * @return Result containing the last shot or error
     */
    suspend fun getLastShotForBean(beanId: String): Result<Shot?> {
        return try {
            if (beanId.isBlank()) {
                Result.failure(RepositoryException.ValidationError("Bean ID cannot be empty"))
            } else {
                val lastShot = shotDao.getLastShotForBean(beanId)
                Result.success(lastShot)
            }
        } catch (exception: Exception) {
            Result.failure(
                RepositoryException.DatabaseError(
                    "Failed to get last shot for bean",
                    exception
                )
            )
        }
    }

    /**
     * Get total shot count.
     * @return Result containing the total number of shots
     */
    suspend fun getTotalShotCount(): Result<Int> {
        return try {
            val count = shotDao.getTotalShotCount()
            Result.success(count)
        } catch (exception: Exception) {
            Result.failure(
                RepositoryException.DatabaseError(
                    "Failed to get total shot count",
                    exception
                )
            )
        }
    }

    /**
     * Get shots with brew ratio in a specific range.
     * @param minRatio Minimum brew ratio
     * @param maxRatio Maximum brew ratio
     * @return Flow of shots within the brew ratio range
     */
    fun getShotsByBrewRatioRange(minRatio: Double, maxRatio: Double): Flow<Result<List<Shot>>> =
        flow {
            if (minRatio < 0 || maxRatio < 0) {
                emit(Result.failure(RepositoryException.ValidationError("Brew ratios cannot be negative")))
                return@flow
            }
            if (minRatio > maxRatio) {
                emit(Result.failure(RepositoryException.ValidationError("Minimum ratio cannot be greater than maximum ratio")))
                return@flow
            }

            shotDao.getShotsByBrewRatioRange(minRatio, maxRatio).collect { shots ->
                emit(Result.success(shots))
            }
        }.catch { exception ->
            emit(
                Result.failure(
                    RepositoryException.DatabaseError(
                        "Failed to get shots by brew ratio range",
                        exception
                    )
                )
            )
        }

    /**
     * Get shots with extraction time in a specific range.
     * @param minSeconds Minimum extraction time in seconds
     * @param maxSeconds Maximum extraction time in seconds
     * @return Flow of shots within the extraction time range
     */
    fun getShotsByExtractionTimeRange(minSeconds: Int, maxSeconds: Int): Flow<Result<List<Shot>>> =
        flow {
            if (minSeconds < 0 || maxSeconds < 0) {
                emit(Result.failure(RepositoryException.ValidationError("Extraction times cannot be negative")))
                return@flow
            }
            if (minSeconds > maxSeconds) {
                emit(Result.failure(RepositoryException.ValidationError("Minimum time cannot be greater than maximum time")))
                return@flow
            }

            shotDao.getShotsByExtractionTimeRange(minSeconds, maxSeconds).collect { shots ->
                emit(Result.success(shots))
            }
        }.catch { exception ->
            emit(
                Result.failure(
                    RepositoryException.DatabaseError(
                        "Failed to get shots by extraction time range",
                        exception
                    )
                )
            )
        }

    /**
     * Validate shot data without saving.
     * @param shot The shot to validate
     * @return ValidationResult with validation status and errors
     */
    suspend fun validateShot(shot: Shot): ValidationResult {
        val validationResult = shot.validate()

        // Additional repository-level validation
        if (validationResult.isValid) {
            try {
                // Verify that the bean exists
                val bean = beanDao.getBeanById(shot.beanId)
                    ?: return ValidationResult(
                        isValid = false,
                        errors = listOf("Selected bean does not exist")
                    )
            } catch (exception: Exception) {
                return ValidationResult(
                    isValid = false,
                    errors = listOf("Failed to validate bean existence")
                )
            }
        }

        return validationResult
    }

    /**
     * Get suggested grinder setting for a bean based on last successful shot.
     * @param beanId The ID of the bean
     * @return Result containing suggested grinder setting or null if no history
     */
    suspend fun getSuggestedGrinderSetting(beanId: String): Result<String?> {
        return try {
            if (beanId.isBlank()) {
                Result.failure(RepositoryException.ValidationError("Bean ID cannot be empty"))
            } else {
                // First check the bean's last grinder setting
                val bean = beanDao.getBeanById(beanId)
                if (bean?.lastGrinderSetting != null) {
                    return Result.success(bean.lastGrinderSetting)
                }

                // If no saved setting, get from last shot
                val lastShot = shotDao.getLastShotForBean(beanId)
                Result.success(lastShot?.grinderSetting)
            }
        } catch (exception: Exception) {
            Result.failure(
                RepositoryException.DatabaseError(
                    "Failed to get suggested grinder setting",
                    exception
                )
            )
        }
    }

    // PERFORMANCE OPTIMIZATION METHODS

    /**
     * Get shots with pagination support for large datasets.
     * @param paginationConfig Configuration for pagination
     * @return Result containing paginated shots
     */
    suspend fun getShotsPaginated(paginationConfig: PaginationConfig): Result<PaginatedResult<Shot>> {
        return try {
            val shots =
                shotDao.getShotsPaginated(paginationConfig.pageSize, paginationConfig.offset)
            val totalCount = shotDao.getTotalShotCount()

            val paginatedResult = PaginatedResult(
                items = shots,
                totalCount = totalCount,
                currentPage = paginationConfig.page,
                pageSize = paginationConfig.pageSize,
                hasNextPage = (paginationConfig.offset + paginationConfig.pageSize) < totalCount,
                hasPreviousPage = paginationConfig.page > 0
            )

            Result.success(paginatedResult)
        } catch (exception: Exception) {
            Result.failure(
                RepositoryException.DatabaseError(
                    "Failed to get paginated shots",
                    exception
                )
            )
        }
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
        return try {
            // Validate date range if both dates are provided
            if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
                return Result.failure(RepositoryException.ValidationError("Start date cannot be after end date"))
            }

            val shots = shotDao.getFilteredShotsPaginated(
                beanId, startDate, endDate,
                paginationConfig.pageSize, paginationConfig.offset
            )
            val totalCount = shotDao.getFilteredShotsCount(beanId, startDate, endDate)

            val paginatedResult = PaginatedResult(
                items = shots,
                totalCount = totalCount,
                currentPage = paginationConfig.page,
                pageSize = paginationConfig.pageSize,
                hasNextPage = (paginationConfig.offset + paginationConfig.pageSize) < totalCount,
                hasPreviousPage = paginationConfig.page > 0
            )

            Result.success(paginatedResult)
        } catch (exception: Exception) {
            Result.failure(
                RepositoryException.DatabaseError(
                    "Failed to get paginated filtered shots",
                    exception
                )
            )
        }
    }
}