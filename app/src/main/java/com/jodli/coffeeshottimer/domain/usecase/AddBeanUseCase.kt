package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.model.ValidationResult
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for adding new coffee bean profiles with validation and business rules.
 * Handles bean creation, validation, and uniqueness checks.
 */
@Singleton
class AddBeanUseCase @Inject constructor(
    private val beanRepository: BeanRepository
) {

    /**
     * Add a new bean with comprehensive validation.
     * @param name Bean name (required, unique, max 100 characters)
     * @param roastDate Date the bean was roasted (cannot be future, max 365 days ago)
     * @param notes Optional notes about the bean (max 500 characters)
     * @param isActive Whether the bean should be active (default: true)
     * @param lastGrinderSetting Optional initial grinder setting
     * @return Result containing the created bean or validation errors
     */
    suspend fun execute(
        name: String,
        roastDate: LocalDate,
        notes: String = "",
        isActive: Boolean = true,
        lastGrinderSetting: String? = null
    ): Result<Bean> {
        return try {
            // Create bean instance
            val bean = Bean(
                name = name.trim(),
                roastDate = roastDate,
                notes = notes.trim(),
                isActive = isActive,
                lastGrinderSetting = lastGrinderSetting?.trim()?.takeIf { it.isNotEmpty() },
                createdAt = LocalDateTime.now()
            )

            // Validate bean through repository (includes uniqueness check)
            val validationResult = beanRepository.validateBean(bean)
            if (!validationResult.isValid) {
                return Result.failure(
                    BeanUseCaseException.ValidationError(
                        "Bean validation failed: ${validationResult.errors.joinToString(", ")}"
                    )
                )
            }

            // Add bean to repository
            val addResult = beanRepository.addBean(bean)
            if (addResult.isSuccess) {
                Result.success(bean)
            } else {
                Result.failure(
                    addResult.exceptionOrNull()
                        ?: BeanUseCaseException.UnknownError("Failed to add bean")
                )
            }
        } catch (exception: Exception) {
            Result.failure(
                BeanUseCaseException.UnknownError(
                    "Unexpected error adding bean",
                    exception
                )
            )
        }
    }

    /**
     * Validate bean parameters without saving.
     * @param name Bean name to validate
     * @param roastDate Roast date to validate
     * @param notes Notes to validate
     * @return ValidationResult with validation status and errors
     */
    suspend fun validateBeanParameters(
        name: String,
        roastDate: LocalDate,
        notes: String = ""
    ): ValidationResult {
        return try {
            val bean = Bean(
                name = name.trim(),
                roastDate = roastDate,
                notes = notes.trim()
            )

            beanRepository.validateBean(bean)
        } catch (exception: Exception) {
            ValidationResult(
                isValid = false,
                errors = listOf("Failed to validate bean parameters: ${exception.message}")
            )
        }
    }

    /**
     * Check if a bean name is available (not already used).
     * @param name Bean name to check
     * @return Result indicating if name is available
     */
    suspend fun isBeanNameAvailable(name: String): Result<Boolean> {
        return try {
            if (name.trim().isEmpty()) {
                Result.failure(BeanUseCaseException.ValidationError("Bean name cannot be empty"))
            } else {
                val existingBean = beanRepository.getBeanByName(name.trim())
                if (existingBean.isSuccess) {
                    Result.success(existingBean.getOrNull() == null)
                } else {
                    Result.failure(
                        existingBean.exceptionOrNull()
                            ?: BeanUseCaseException.UnknownError("Failed to check bean name")
                    )
                }
            }
        } catch (exception: Exception) {
            Result.failure(
                BeanUseCaseException.UnknownError(
                    "Unexpected error checking bean name",
                    exception
                )
            )
        }
    }

    /**
     * Create a bean with default values for quick setup.
     * @param name Bean name (required)
     * @param roastDate Roast date (defaults to today if not provided)
     * @return Result containing the created bean
     */
    suspend fun createQuickBean(
        name: String,
        roastDate: LocalDate = LocalDate.now()
    ): Result<Bean> {
        return execute(
            name = name,
            roastDate = roastDate,
            notes = "",
            isActive = true,
            lastGrinderSetting = null
        )
    }

    /**
     * Get validation rules for bean creation (for UI display).
     * @return Map of field names to validation rules
     */
    fun getValidationRules(): Map<String, String> {
        return mapOf(
            "name" to "Required, unique, max 100 characters",
            "roastDate" to "Cannot be future date, max 365 days ago",
            "notes" to "Optional, max 500 characters",
            "grinderSetting" to "Optional, max 50 characters"
        )
    }
}

/**
 * Sealed class representing different types of bean use case exceptions.
 */
sealed class BeanUseCaseException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {
    class ValidationError(message: String) : BeanUseCaseException(message)
    class UnknownError(message: String, cause: Throwable? = null) :
        BeanUseCaseException(message, cause)
}