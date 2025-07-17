package com.example.coffeeshottimer.domain.usecase

import com.example.coffeeshottimer.data.model.Bean
import com.example.coffeeshottimer.data.model.ValidationResult
import com.example.coffeeshottimer.data.repository.BeanRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for updating existing coffee bean profiles with validation and business rules.
 * Handles bean updates, validation, and grinder setting memory functionality.
 */
@Singleton
class UpdateBeanUseCase @Inject constructor(
    private val beanRepository: BeanRepository
) {
    
    /**
     * Update an existing bean with comprehensive validation.
     * @param beanId ID of the bean to update
     * @param name Updated bean name (required, unique, max 100 characters)
     * @param roastDate Updated roast date (cannot be future, max 365 days ago)
     * @param notes Updated notes about the bean (max 500 characters)
     * @param isActive Whether the bean should be active
     * @param lastGrinderSetting Updated grinder setting
     * @return Result containing the updated bean or validation errors
     */
    suspend fun execute(
        beanId: String,
        name: String,
        roastDate: LocalDate,
        notes: String = "",
        isActive: Boolean = true,
        lastGrinderSetting: String? = null
    ): Result<Bean> {
        return try {
            // Validate bean ID
            if (beanId.trim().isEmpty()) {
                return Result.failure(BeanUseCaseException.ValidationError("Bean ID cannot be empty"))
            }
            
            // Get existing bean to preserve creation timestamp
            val existingBeanResult = beanRepository.getBeanById(beanId.trim())
            if (existingBeanResult.isFailure) {
                return Result.failure(
                    existingBeanResult.exceptionOrNull() ?: BeanUseCaseException.UnknownError("Failed to get existing bean")
                )
            }
            
            val existingBean = existingBeanResult.getOrNull()
            if (existingBean == null) {
                return Result.failure(BeanUseCaseException.ValidationError("Bean not found"))
            }
            
            // Create updated bean instance
            val updatedBean = existingBean.copy(
                name = name.trim(),
                roastDate = roastDate,
                notes = notes.trim(),
                isActive = isActive,
                lastGrinderSetting = lastGrinderSetting?.trim()?.takeIf { it.isNotEmpty() }
            )
            
            // Validate updated bean through repository (includes uniqueness check)
            val validationResult = beanRepository.validateBean(updatedBean)
            if (!validationResult.isValid) {
                return Result.failure(
                    BeanUseCaseException.ValidationError(
                        "Bean validation failed: ${validationResult.errors.joinToString(", ")}"
                    )
                )
            }
            
            // Update bean in repository
            val updateResult = beanRepository.updateBean(updatedBean)
            if (updateResult.isSuccess) {
                Result.success(updatedBean)
            } else {
                Result.failure(
                    updateResult.exceptionOrNull() ?: BeanUseCaseException.UnknownError("Failed to update bean")
                )
            }
        } catch (exception: Exception) {
            Result.failure(BeanUseCaseException.UnknownError("Unexpected error updating bean", exception))
        }
    }
    
    /**
     * Update only the grinder setting for a bean (grinder setting memory functionality).
     * @param beanId ID of the bean to update
     * @param grinderSetting New grinder setting to remember
     * @return Result indicating success or failure
     */
    suspend fun updateGrinderSetting(
        beanId: String,
        grinderSetting: String
    ): Result<Unit> {
        return try {
            if (beanId.trim().isEmpty()) {
                return Result.failure(BeanUseCaseException.ValidationError("Bean ID cannot be empty"))
            }
            
            if (grinderSetting.trim().isEmpty()) {
                return Result.failure(BeanUseCaseException.ValidationError("Grinder setting cannot be empty"))
            }
            
            val result = beanRepository.updateLastGrinderSetting(beanId.trim(), grinderSetting.trim())
            if (result.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(
                    result.exceptionOrNull() ?: BeanUseCaseException.UnknownError("Failed to update grinder setting")
                )
            }
        } catch (exception: Exception) {
            Result.failure(BeanUseCaseException.UnknownError("Unexpected error updating grinder setting", exception))
        }
    }
    
    /**
     * Update only the active status of a bean.
     * @param beanId ID of the bean to update
     * @param isActive Whether the bean should be active
     * @return Result indicating success or failure
     */
    suspend fun updateActiveStatus(
        beanId: String,
        isActive: Boolean
    ): Result<Unit> {
        return try {
            if (beanId.trim().isEmpty()) {
                return Result.failure(BeanUseCaseException.ValidationError("Bean ID cannot be empty"))
            }
            
            val result = beanRepository.updateBeanActiveStatus(beanId.trim(), isActive)
            if (result.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(
                    result.exceptionOrNull() ?: BeanUseCaseException.UnknownError("Failed to update active status")
                )
            }
        } catch (exception: Exception) {
            Result.failure(BeanUseCaseException.UnknownError("Unexpected error updating active status", exception))
        }
    }
    
    /**
     * Validate bean update parameters without saving.
     * @param beanId ID of the bean being updated
     * @param name Bean name to validate
     * @param roastDate Roast date to validate
     * @param notes Notes to validate
     * @return ValidationResult with validation status and errors
     */
    suspend fun validateUpdateParameters(
        beanId: String,
        name: String,
        roastDate: LocalDate,
        notes: String = ""
    ): ValidationResult {
        return try {
            if (beanId.trim().isEmpty()) {
                return ValidationResult(
                    isValid = false,
                    errors = listOf("Bean ID cannot be empty")
                )
            }
            
            // Get existing bean to preserve creation timestamp
            val existingBeanResult = beanRepository.getBeanById(beanId.trim())
            if (existingBeanResult.isFailure) {
                return ValidationResult(
                    isValid = false,
                    errors = listOf("Bean not found")
                )
            }
            
            val existingBean = existingBeanResult.getOrNull()
            if (existingBean == null) {
                return ValidationResult(
                    isValid = false,
                    errors = listOf("Bean not found")
                )
            }
            
            val updatedBean = existingBean.copy(
                name = name.trim(),
                roastDate = roastDate,
                notes = notes.trim()
            )
            
            beanRepository.validateBean(updatedBean)
        } catch (exception: Exception) {
            ValidationResult(
                isValid = false,
                errors = listOf("Failed to validate bean parameters: ${exception.message}")
            )
        }
    }
    
    /**
     * Check if a bean name is available for update (not used by other beans).
     * @param beanId ID of the bean being updated (excluded from check)
     * @param name Bean name to check
     * @return Result indicating if name is available
     */
    suspend fun isBeanNameAvailableForUpdate(beanId: String, name: String): Result<Boolean> {
        return try {
            if (beanId.trim().isEmpty()) {
                Result.failure(BeanUseCaseException.ValidationError("Bean ID cannot be empty"))
            } else if (name.trim().isEmpty()) {
                Result.failure(BeanUseCaseException.ValidationError("Bean name cannot be empty"))
            } else {
                val existingBean = beanRepository.getBeanByName(name.trim())
                if (existingBean.isSuccess) {
                    val bean = existingBean.getOrNull()
                    // Name is available if no bean exists with that name, or if the existing bean is the one being updated
                    Result.success(bean == null || bean.id == beanId.trim())
                } else {
                    Result.failure(
                        existingBean.exceptionOrNull() ?: BeanUseCaseException.UnknownError("Failed to check bean name")
                    )
                }
            }
        } catch (exception: Exception) {
            Result.failure(BeanUseCaseException.UnknownError("Unexpected error checking bean name", exception))
        }
    }
    
    /**
     * Get the current bean data for editing.
     * @param beanId ID of the bean to get
     * @return Result containing the bean data
     */
    suspend fun getBeanForEditing(beanId: String): Result<Bean> {
        return try {
            if (beanId.trim().isEmpty()) {
                Result.failure(BeanUseCaseException.ValidationError("Bean ID cannot be empty"))
            } else {
                val result = beanRepository.getBeanById(beanId.trim())
                if (result.isSuccess) {
                    val bean = result.getOrNull()
                    if (bean != null) {
                        Result.success(bean)
                    } else {
                        Result.failure(BeanUseCaseException.ValidationError("Bean not found"))
                    }
                } else {
                    Result.failure(
                        result.exceptionOrNull() ?: BeanUseCaseException.UnknownError("Failed to get bean")
                    )
                }
            }
        } catch (exception: Exception) {
            Result.failure(BeanUseCaseException.UnknownError("Unexpected error getting bean", exception))
        }
    }
    
    /**
     * Bulk update active status for multiple beans.
     * @param beanIds List of bean IDs to update
     * @param isActive Whether the beans should be active
     * @return Result with count of successfully updated beans
     */
    suspend fun bulkUpdateActiveStatus(
        beanIds: List<String>,
        isActive: Boolean
    ): Result<Int> {
        return try {
            var successCount = 0
            val errors = mutableListOf<String>()
            
            for (beanId in beanIds) {
                val result = updateActiveStatus(beanId, isActive)
                if (result.isSuccess) {
                    successCount++
                } else {
                    errors.add("Failed to update bean $beanId: ${result.exceptionOrNull()?.message}")
                }
            }
            
            if (errors.isEmpty()) {
                Result.success(successCount)
            } else {
                Result.failure(
                    BeanUseCaseException.ValidationError(
                        "Some beans failed to update: ${errors.joinToString(", ")}"
                    )
                )
            }
        } catch (exception: Exception) {
            Result.failure(BeanUseCaseException.UnknownError("Unexpected error in bulk update", exception))
        }
    }
}