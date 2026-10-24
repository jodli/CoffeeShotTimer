package com.jodli.coffeeshottimer.data.repository

import android.util.Log
import com.jodli.coffeeshottimer.data.dao.GrinderConfigDao
import com.jodli.coffeeshottimer.data.model.GrinderConfiguration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for GrinderConfiguration entity operations.
 * Provides business logic layer between UI and data access layer.
 * Handles offline-first data access and error management with retry mechanisms.
 */
@Singleton
class GrinderConfigRepository @Inject constructor(
    private val grinderConfigDao: GrinderConfigDao
) {

    /**
     * Get the current grinder configuration.
     * @return Result containing the current configuration or null if none exists
     */
    suspend fun getCurrentConfig(): Result<GrinderConfiguration?> {
        return executeWithRetry(
            operation = { grinderConfigDao.getCurrentConfig() },
            errorMessage = "Failed to get current grinder configuration"
        )
    }

    /**
     * Get the current grinder configuration as a Flow for reactive updates.
     * @return Flow of current configuration with error handling
     */
    fun getCurrentConfigFlow(): Flow<Result<GrinderConfiguration?>> = flow {
        grinderConfigDao.getCurrentConfigFlow().collect { config ->
            emit(Result.success(config))
        }
    }.catch { exception ->
        emit(
            Result.failure(
                RepositoryException.DatabaseError(
                    "Failed to get current grinder configuration flow",
                    exception
                )
            )
        )
    }

    /**
     * Get a specific grinder configuration by ID.
     * @param configId The ID of the configuration to retrieve
     * @return Result containing the configuration or error
     */
    suspend fun getConfigById(configId: String): Result<GrinderConfiguration?> {
        return try {
            if (configId.isBlank()) {
                Result.failure(RepositoryException.ValidationError("Configuration ID cannot be empty"))
            } else {
                executeWithRetry(
                    operation = { grinderConfigDao.getConfigById(configId) },
                    errorMessage = "Failed to get grinder configuration by ID"
                )
            }
        } catch (exception: Exception) {
            Result.failure(RepositoryException.DatabaseError("Failed to get grinder configuration by ID", exception))
        }
    }

    /**
     * Save a new grinder configuration with validation.
     * @param config The configuration to save
     * @return Result indicating success or failure with validation errors
     */
    suspend fun saveConfig(config: GrinderConfiguration): Result<Unit> {
        Log.d(TAG, "saveConfig: Starting save process for config: $config")
        return try {
            // Validate configuration data
            Log.d(TAG, "saveConfig: Validating configuration")
            val validationResult = config.validate()
            Log.d(
                TAG,
                "saveConfig: Validation result - isValid=${validationResult.isValid}, errors=${validationResult.errors}"
            )

            if (!validationResult.isValid) {
                Log.e(TAG, "saveConfig: Validation failed - ${validationResult.errors}")
                return Result.failure(
                    RepositoryException.ValidationError(
                        "Grinder configuration validation failed: ${validationResult.errors.joinToString(", ")}"
                    )
                )
            }

            val result = executeWithRetry(
                operation = {
                    Log.d(TAG, "saveConfig: Inserting new config")
                    grinderConfigDao.insertConfig(config)
                    Log.d(TAG, "saveConfig: Config inserted successfully")
                    Unit
                },
                errorMessage = "Failed to save grinder configuration"
            )
            Log.d(TAG, "saveConfig: executeWithRetry result - isSuccess=${result.isSuccess}")
            result
        } catch (exception: Exception) {
            Log.e(TAG, "saveConfig: Exception caught", exception)
            Result.failure(RepositoryException.DatabaseError("Failed to save grinder configuration", exception))
        }
    }

    /**
     * Get or create a default grinder configuration.
     * If no configuration exists, creates and saves the default one.
     * @return Result containing the current or newly created default configuration
     */
    suspend fun getOrCreateDefaultConfig(): Result<GrinderConfiguration> {
        return try {
            // Try to get current configuration
            val currentConfigResult = getCurrentConfig()
            if (currentConfigResult.isSuccess) {
                val currentConfig = currentConfigResult.getOrNull()
                if (currentConfig != null) {
                    return Result.success(currentConfig)
                }
            }

            // No configuration exists, create default
            val defaultConfig = GrinderConfiguration.DEFAULT_CONFIGURATION
            val saveResult = saveConfig(defaultConfig)

            if (saveResult.isSuccess) {
                Result.success(defaultConfig)
            } else {
                saveResult.fold(
                    onSuccess = { Result.success(defaultConfig) },
                    onFailure = { Result.failure(it) }
                )
            }
        } catch (exception: Exception) {
            Result.failure(
                RepositoryException.DatabaseError("Failed to get or create default configuration", exception)
            )
        }
    }

    /**
     * Execute a database operation with retry mechanism for handling transient failures.
     * @param operation The database operation to execute
     * @param errorMessage The error message to use if all retries fail
     * @param maxRetries Maximum number of retry attempts (default: 3)
     * @param delayMs Delay between retries in milliseconds (default: 100)
     * @return Result of the operation
     */
    private suspend fun <T> executeWithRetry(
        operation: suspend () -> T,
        errorMessage: String,
        maxRetries: Int = 3,
        delayMs: Long = 100
    ): Result<T> {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                val result = operation()
                return Result.success(result)
            } catch (exception: Exception) {
                lastException = exception
                if (attempt < maxRetries - 1) {
                    delay(delayMs * (attempt + 1)) // Exponential backoff
                }
            }
        }

        return Result.failure(
            RepositoryException.DatabaseError(
                "$errorMessage (after $maxRetries attempts)",
                lastException
            )
        )
    }

    companion object {
        private const val TAG = "GrinderConfigRepository"
    }
}
