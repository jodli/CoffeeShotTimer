package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.domain.exception.DomainException
import com.jodli.coffeeshottimer.domain.model.DomainErrorCode
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
     * @return Result containing the created bean or validation errors
     */
    suspend fun execute(
        name: String,
        roastDate: LocalDate,
        notes: String = "",
        isActive: Boolean = true
    ): Result<Bean> {
        return try {
            // Create bean instance
            val bean = Bean(
                name = name.trim(),
                roastDate = roastDate,
                notes = notes.trim(),
                isActive = isActive,
                createdAt = LocalDateTime.now()
            )

            // Validate bean through repository (includes uniqueness check)
            val validationResult = beanRepository.validateBean(bean)
            if (!validationResult.isValid) {
                return Result.failure(
                    DomainException(
                        DomainErrorCode.VALIDATION_FAILED,
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
                        ?: DomainException(DomainErrorCode.FAILED_TO_ADD_BEAN)
                )
            }
        } catch (exception: Exception) {
            Result.failure(
                DomainException(
                    DomainErrorCode.UNKNOWN_ERROR,
                    "Unexpected error adding bean",
                    exception
                )
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
                Result.failure(DomainException(DomainErrorCode.BEAN_NAME_EMPTY))
            } else {
                val existingBean = beanRepository.getBeanByName(name.trim())
                if (existingBean.isSuccess) {
                    Result.success(existingBean.getOrNull() == null)
                } else {
                    Result.failure(
                        existingBean.exceptionOrNull()
                            ?: DomainException(DomainErrorCode.FAILED_TO_CHECK_BEAN_NAME)
                    )
                }
            }
        } catch (exception: Exception) {
            Result.failure(
                DomainException(
                    DomainErrorCode.UNKNOWN_ERROR,
                    "Unexpected error checking bean name",
                    exception
                )
            )
        }
    }
}
