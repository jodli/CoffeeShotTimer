package com.jodli.coffeeshottimer.data.repository

import com.jodli.coffeeshottimer.data.dao.GrinderConfigDao
import com.jodli.coffeeshottimer.data.model.GrinderConfiguration
import com.jodli.coffeeshottimer.data.model.ValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
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
     * Get all grinder configurations ordered by creation date.
     * @return Flow of list of configurations with error handling
     */
    fun getAllConfigs(): Flow<Result<List<GrinderConfiguration>>> = flow {
        grinderConfigDao.getAllConfigs().collect { configs ->
            emit(Result.success(configs))
        }
    }.catch { exception ->
        emit(
            Result.failure(
                RepositoryException.DatabaseError(
                    "Failed to get all grinder configurations",
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
        return try {
            // Validate configuration data
            val validationResult = config.validate()
            if (!validationResult.isValid) {
                return Result.failure(
                    RepositoryException.ValidationError(
                        "Grinder configuration validation failed: ${validationResult.errors.joinToString(", ")}"
                    )
                )
            }

            // Treat duplicate range configuration as idempotent success (no-op)
            val existingConfig = grinderConfigDao.getConfigByRange(config.scaleMin, config.scaleMax)
            if (existingConfig != null) {
                return Result.success(Unit)
            }

            executeWithRetry(
                operation = { 
                    grinderConfigDao.insertConfig(config)
                    Unit
                },
                errorMessage = "Failed to save grinder configuration"
            )
        } catch (exception: Exception) {
            Result.failure(RepositoryException.DatabaseError("Failed to save grinder configuration", exception))
        }
    }

    /**
     * Update an existing grinder configuration with validation.
     * @param config The configuration to update
     * @return Result indicating success or failure with validation errors
     */
    suspend fun updateConfig(config: GrinderConfiguration): Result<Unit> {
        return try {
            // Validate configuration data
            val validationResult = config.validate()
            if (!validationResult.isValid) {
                return Result.failure(
                    RepositoryException.ValidationError(
                        "Grinder configuration validation failed: ${validationResult.errors.joinToString(", ")}"
                    )
                )
            }

            // Check if configuration exists
            val existingConfig = grinderConfigDao.getConfigById(config.id)
                ?: return Result.failure(RepositoryException.NotFoundError("Grinder configuration not found"))

            // Check for duplicate range configuration (excluding current config)
            val configWithSameRange = grinderConfigDao.getConfigByRange(config.scaleMin, config.scaleMax)
            if (configWithSameRange != null && configWithSameRange.id != config.id) {
                return Result.failure(
                    RepositoryException.ValidationError(
                        "A configuration with range ${config.scaleMin}-${config.scaleMax} already exists"
                    )
                )
            }

            executeWithRetry(
                operation = { 
                    grinderConfigDao.updateConfig(config)
                    Unit
                },
                errorMessage = "Failed to update grinder configuration"
            )
        } catch (exception: Exception) {
            Result.failure(RepositoryException.DatabaseError("Failed to update grinder configuration", exception))
        }
    }

    /**
     * Delete a grinder configuration.
     * @param config The configuration to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteConfig(config: GrinderConfiguration): Result<Unit> {
        return try {
            // Check if configuration exists
            val existingConfig = grinderConfigDao.getConfigById(config.id)
                ?: return Result.failure(RepositoryException.NotFoundError("Grinder configuration not found"))

            executeWithRetry(
                operation = { 
                    grinderConfigDao.deleteConfig(config)
                    Unit
                },
                errorMessage = "Failed to delete grinder configuration"
            )
        } catch (exception: Exception) {
            Result.failure(RepositoryException.DatabaseError("Failed to delete grinder configuration", exception))
        }
    }

    /**
     * Delete all grinder configurations.
     * @return Result indicating success or failure
     */
    suspend fun deleteAllConfigs(): Result<Unit> {
        return executeWithRetry(
            operation = { 
                grinderConfigDao.deleteAllConfigs()
                Unit
            },
            errorMessage = "Failed to delete all grinder configurations"
        )
    }

    /**
     * Get count of grinder configurations.
     * @return Result containing the count of configurations
     */
    suspend fun getConfigCount(): Result<Int> {
        return executeWithRetry(
            operation = { grinderConfigDao.getConfigCount() },
            errorMessage = "Failed to get grinder configuration count"
        )
    }

    /**
     * Check if a configuration with the same range already exists.
     * @param scaleMin The minimum scale value
     * @param scaleMax The maximum scale value
     * @return Result containing the existing configuration or null
     */
    suspend fun getConfigByRange(scaleMin: Int, scaleMax: Int): Result<GrinderConfiguration?> {
        return try {
            if (scaleMin >= scaleMax) {
                return Result.failure(RepositoryException.ValidationError("Minimum scale must be less than maximum scale"))
            }

            executeWithRetry(
                operation = { grinderConfigDao.getConfigByRange(scaleMin, scaleMax) },
                errorMessage = "Failed to get grinder configuration by range"
            )
        } catch (exception: Exception) {
            Result.failure(RepositoryException.DatabaseError("Failed to get grinder configuration by range", exception))
        }
    }

    /**
     * Validate grinder configuration data without saving.
     * @param config The configuration to validate
     * @return ValidationResult with validation status and errors
     */
    suspend fun validateConfig(config: GrinderConfiguration): ValidationResult {
        val validationResult = config.validate()

        // Additional repository-level validation
        if (validationResult.isValid) {
            try {
                // Check for duplicate range configuration
                val existingConfig = grinderConfigDao.getConfigByRange(config.scaleMin, config.scaleMax)
                if (existingConfig != null && existingConfig.id != config.id) {
                    return ValidationResult(
                        isValid = false,
                        errors = listOf("A configuration with range ${config.scaleMin}-${config.scaleMax} already exists")
                    )
                }
            } catch (exception: Exception) {
                return ValidationResult(
                    isValid = false,
                    errors = listOf("Failed to validate configuration uniqueness")
                )
            }
        }

        return validationResult
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
            Result.failure(RepositoryException.DatabaseError("Failed to get or create default configuration", exception))
        }
    }

    /**
     * Save a preset configuration from the common presets.
     * @param presetIndex The index of the preset in COMMON_PRESETS
     * @return Result indicating success or failure
     */
    suspend fun savePresetConfig(presetIndex: Int): Result<Unit> {
        return try {
            if (presetIndex < 0 || presetIndex >= GrinderConfiguration.COMMON_PRESETS.size) {
                return Result.failure(RepositoryException.ValidationError("Invalid preset index"))
            }

            val presetConfig = GrinderConfiguration.COMMON_PRESETS[presetIndex]
            saveConfig(presetConfig)
        } catch (exception: Exception) {
            Result.failure(RepositoryException.DatabaseError("Failed to save preset configuration", exception))
        }
    }

    /**
     * Clean up old configurations, keeping only the most recent ones.
     * @param keepCount Number of configurations to keep (default: 5)
     * @return Result indicating success or failure
     */
    suspend fun cleanupOldConfigs(keepCount: Int = 5): Result<Unit> {
        return try {
            if (keepCount < 1) {
                return Result.failure(RepositoryException.ValidationError("Keep count must be at least 1"))
            }

            executeWithRetry(
                operation = { 
                    grinderConfigDao.deleteOldConfigs(keepCount)
                    Unit
                },
                errorMessage = "Failed to cleanup old grinder configurations"
            )
        } catch (exception: Exception) {
            Result.failure(RepositoryException.DatabaseError("Failed to cleanup old grinder configurations", exception))
        }
    }

    /**
     * Check if the user has any grinder configuration set up.
     * @return Result containing true if any configuration exists
     */
    suspend fun hasAnyConfig(): Result<Boolean> {
        return try {
            val countResult = getConfigCount()
            countResult.fold(
                onSuccess = { count -> Result.success(count > 0) },
                onFailure = { Result.failure(it) }
            )
        } catch (exception: Exception) {
            Result.failure(RepositoryException.DatabaseError("Failed to check if any configuration exists", exception))
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
}