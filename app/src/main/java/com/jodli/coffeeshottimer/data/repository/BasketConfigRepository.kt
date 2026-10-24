package com.jodli.coffeeshottimer.data.repository

import android.util.Log
import com.jodli.coffeeshottimer.data.dao.BasketConfigDao
import com.jodli.coffeeshottimer.data.model.BasketConfiguration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for BasketConfiguration entity operations.
 * Provides business logic layer between UI and data access layer.
 * Handles offline-first data access and error management with retry mechanisms.
 */
@Singleton
class BasketConfigRepository @Inject constructor(
    private val basketConfigDao: BasketConfigDao
) {

    /**
     * Get the current active basket configuration.
     * @return Result containing the current active configuration or null if none exists
     */
    suspend fun getActiveConfig(): Result<BasketConfiguration?> {
        return executeWithRetry(
            operation = { basketConfigDao.getActiveConfig() },
            errorMessage = "Failed to get active basket configuration"
        )
    }

    /**
     * Get the current active basket configuration as a Flow for reactive updates.
     * @return Flow of current active configuration with error handling
     */
    fun getActiveConfigFlow(): Flow<Result<BasketConfiguration?>> = flow {
        basketConfigDao.getActiveConfigFlow().collect { config ->
            emit(Result.success(config))
        }
    }.catch { exception ->
        emit(
            Result.failure(
                RepositoryException.DatabaseError(
                    "Failed to get active basket configuration flow",
                    exception
                )
            )
        )
    }

    /**
     * Save a new basket configuration with validation.
     * Deactivates all existing configurations and sets the new one as active.
     * @param config The configuration to save
     * @return Result indicating success or failure with validation errors
     */
    suspend fun saveConfig(config: BasketConfiguration): Result<Unit> {
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
                        "Basket configuration validation failed: ${validationResult.errors.joinToString(", ")}"
                    )
                )
            }

            // Treat duplicate range configuration as idempotent success (no-op)
            Log.d(TAG, "saveConfig: Checking for existing config with same ranges")
            val existingConfig = try {
                basketConfigDao.getConfigByRanges(
                    config.coffeeInMin,
                    config.coffeeInMax,
                    config.coffeeOutMin,
                    config.coffeeOutMax
                )
            } catch (e: Exception) {
                Log.e(TAG, "saveConfig: Error checking for existing config", e)
                throw e
            }
            Log.d(TAG, "saveConfig: Existing config found: $existingConfig")

            if (existingConfig != null) {
                Log.d(TAG, "saveConfig: Duplicate config found, checking if it's active: ${existingConfig.isActive}")
                // If duplicate exists but it's not active, activate it
                if (!existingConfig.isActive) {
                    Log.d(TAG, "saveConfig: Activating existing duplicate config")
                    basketConfigDao.deactivateAllConfigs()
                    basketConfigDao.activateConfig(existingConfig.id)
                }
                Log.d(TAG, "saveConfig: Returning success for duplicate config")
                return Result.success(Unit)
            }

            Log.d(TAG, "saveConfig: No duplicate found, proceeding with save")
            val result = executeWithRetry(
                operation = {
                    Log.d(TAG, "saveConfig: Deactivating all existing configs")
                    // Deactivate all existing configurations
                    basketConfigDao.deactivateAllConfigs()
                    Log.d(TAG, "saveConfig: Inserting new config")
                    // Save the new configuration as active
                    basketConfigDao.insertConfig(config.copy(isActive = true))
                    Log.d(TAG, "saveConfig: Config inserted successfully")
                    Unit
                },
                errorMessage = "Failed to save basket configuration"
            )
            Log.d(TAG, "saveConfig: executeWithRetry result - isSuccess=${result.isSuccess}")
            result
        } catch (exception: Exception) {
            Log.e(TAG, "saveConfig: Exception caught", exception)
            Result.failure(RepositoryException.DatabaseError("Failed to save basket configuration", exception))
        }
    }

    /**
     * Get or create a default basket configuration.
     * If no configuration exists, creates and saves the default one.
     * @return Result containing the current active or newly created default configuration
     */
    suspend fun getOrCreateDefaultConfig(): Result<BasketConfiguration> {
        return try {
            // Try to get current active configuration
            val currentConfigResult = getActiveConfig()
            if (currentConfigResult.isSuccess) {
                val currentConfig = currentConfigResult.getOrNull()
                if (currentConfig != null) {
                    return Result.success(currentConfig)
                }
            }

            // No configuration exists, create default
            val defaultConfig = BasketConfiguration.DEFAULT
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
        Log.d(TAG, "executeWithRetry: Starting operation with maxRetries=$maxRetries")
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                Log.d(TAG, "executeWithRetry: Attempt ${attempt + 1} of $maxRetries")
                val result = operation()
                Log.d(TAG, "executeWithRetry: Operation successful on attempt ${attempt + 1}")
                return Result.success(result)
            } catch (exception: Exception) {
                Log.e(TAG, "executeWithRetry: Attempt ${attempt + 1} failed", exception)
                lastException = exception
                if (attempt < maxRetries - 1) {
                    val delayTime = delayMs * (attempt + 1)
                    Log.d(TAG, "executeWithRetry: Retrying after ${delayTime}ms delay")
                    delay(delayTime) // Exponential backoff
                }
            }
        }

        Log.e(TAG, "executeWithRetry: All attempts failed", lastException)
        return Result.failure(
            RepositoryException.DatabaseError(
                "$errorMessage (after $maxRetries attempts)",
                lastException
            )
        )
    }

    companion object {
        private const val TAG = "BasketConfigRepository"
    }
}
