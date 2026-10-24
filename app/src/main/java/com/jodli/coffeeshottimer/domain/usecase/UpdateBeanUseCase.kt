package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.domain.exception.DomainException
import com.jodli.coffeeshottimer.domain.model.DomainErrorCode
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
     * @return Result containing the updated bean or validation errors
     */
    suspend fun execute(
        beanId: String,
        name: String,
        roastDate: LocalDate,
        notes: String = "",
        isActive: Boolean = true
    ): Result<Bean> {
        val trimmedBeanId = beanId.trim()
        if (trimmedBeanId.isEmpty()) {
            return Result.failure(DomainException(DomainErrorCode.BEAN_ID_EMPTY))
        }

        return try {
            val existingBean = getExistingBeanOrThrow(trimmedBeanId)

            val updatedBean = existingBean.copy(
                name = name.trim(),
                roastDate = roastDate,
                notes = notes.trim(),
                isActive = isActive,
                lastGrinderSetting = existingBean.lastGrinderSetting
            )

            validateBeanOrThrow(updatedBean)
            updateBeanOrThrow(updatedBean)

            Result.success(updatedBean)
        } catch (exception: DomainException) {
            Result.failure(exception)
        } catch (exception: Exception) {
            Result.failure(
                DomainException(
                    DomainErrorCode.UNKNOWN_ERROR,
                    "Unexpected error updating bean",
                    exception
                )
            )
        }
    }

    private suspend fun getExistingBeanOrThrow(beanId: String): Bean {
        val result = beanRepository.getBeanById(beanId)
        if (result.isFailure) {
            throw result.exceptionOrNull() as? DomainException
                ?: DomainException(DomainErrorCode.UNKNOWN_ERROR, "Failed to get existing bean")
        }

        return result.getOrNull()
            ?: throw DomainException(DomainErrorCode.BEAN_NOT_FOUND)
    }

    private suspend fun validateBeanOrThrow(bean: Bean) {
        val validationResult = beanRepository.validateBean(bean)
        if (!validationResult.isValid) {
            throw DomainException(
                DomainErrorCode.VALIDATION_FAILED,
                "Bean validation failed: ${validationResult.errors.joinToString(", ")}"
            )
        }
    }

    private suspend fun updateBeanOrThrow(bean: Bean) {
        val updateResult = beanRepository.updateBean(bean)
        if (updateResult.isFailure) {
            throw updateResult.exceptionOrNull() as? DomainException
                ?: DomainException(DomainErrorCode.UNKNOWN_ERROR, "Failed to update bean")
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
                return Result.failure(DomainException(DomainErrorCode.BEAN_ID_EMPTY))
            }

            val result = beanRepository.updateBeanActiveStatus(beanId.trim(), isActive)
            if (result.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(
                    result.exceptionOrNull()
                        ?: DomainException(DomainErrorCode.UNKNOWN_ERROR, "Failed to update active status")
                )
            }
        } catch (exception: Exception) {
            Result.failure(
                DomainException(
                    DomainErrorCode.UNKNOWN_ERROR,
                    "Unexpected error updating active status",
                    exception
                )
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
                Result.failure(DomainException(DomainErrorCode.BEAN_ID_EMPTY))
            } else if (name.trim().isEmpty()) {
                Result.failure(DomainException(DomainErrorCode.BEAN_NAME_EMPTY))
            } else {
                val existingBean = beanRepository.getBeanByName(name.trim())
                if (existingBean.isSuccess) {
                    val bean = existingBean.getOrNull()
                    // Name is available if no bean exists with that name, or if the existing bean is the one being updated
                    Result.success(bean == null || bean.id == beanId.trim())
                } else {
                    Result.failure(
                        existingBean.exceptionOrNull()
                            ?: DomainException(DomainErrorCode.UNKNOWN_ERROR, "Failed to check bean name")
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

    /**
     * Get the current bean data for editing.
     * @param beanId ID of the bean to get
     * @return Result containing the bean data
     */
    suspend fun getBeanForEditing(beanId: String): Result<Bean> {
        return try {
            if (beanId.trim().isEmpty()) {
                Result.failure(DomainException(DomainErrorCode.BEAN_ID_EMPTY))
            } else {
                val result = beanRepository.getBeanById(beanId.trim())
                if (result.isSuccess) {
                    val bean = result.getOrNull()
                    if (bean != null) {
                        Result.success(bean)
                    } else {
                        Result.failure(DomainException(DomainErrorCode.BEAN_NOT_FOUND))
                    }
                } else {
                    Result.failure(
                        result.exceptionOrNull()
                            ?: DomainException(DomainErrorCode.UNKNOWN_ERROR, "Failed to get bean")
                    )
                }
            }
        } catch (exception: Exception) {
            Result.failure(
                DomainException(
                    DomainErrorCode.UNKNOWN_ERROR,
                    "Unexpected error getting bean",
                    exception
                )
            )
        }
    }
}
