package com.jodli.coffeeshottimer.data.repository

import com.jodli.coffeeshottimer.data.dao.BeanDao
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.model.ValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Bean entity operations.
 * Provides business logic layer between UI and data access layer.
 * Handles offline-first data access and error management.
 */
@Singleton
class BeanRepository @Inject constructor(
    private val beanDao: BeanDao
) {
    
    // In-memory storage for current bean selection (could be replaced with SharedPreferences for persistence)
    private var _currentBeanId: String? = null
    
    /**
     * Get all beans ordered by creation date.
     * @return Flow of list of beans with error handling
     */
    fun getAllBeans(): Flow<Result<List<Bean>>> = flow {
        beanDao.getAllBeans().collect { beans ->
            emit(Result.success(beans))
        }
    }.catch { exception ->
        emit(Result.failure(RepositoryException.DatabaseError("Failed to get all beans", exception)))
    }
    
    /**
     * Get all active beans ordered by creation date.
     * @return Flow of list of active beans with error handling
     */
    fun getActiveBeans(): Flow<Result<List<Bean>>> = flow {
        beanDao.getActiveBeans().collect { beans ->
            emit(Result.success(beans))
        }
    }.catch { exception ->
        emit(Result.failure(RepositoryException.DatabaseError("Failed to get active beans", exception)))
    }
    
    /**
     * Get a specific bean by ID.
     * @param beanId The ID of the bean to retrieve
     * @return Result containing the bean or error
     */
    suspend fun getBeanById(beanId: String): Result<Bean?> {
        return try {
            if (beanId.isBlank()) {
                Result.failure(RepositoryException.ValidationError("Bean ID cannot be empty"))
            } else {
                val bean = beanDao.getBeanById(beanId)
                Result.success(bean)
            }
        } catch (exception: Exception) {
            Result.failure(RepositoryException.DatabaseError("Failed to get bean by ID", exception))
        }
    }
    
    /**
     * Get a bean by name (for uniqueness validation).
     * @param name The name of the bean to search for
     * @return Result containing the bean or error
     */
    suspend fun getBeanByName(name: String): Result<Bean?> {
        return try {
            if (name.isBlank()) {
                Result.failure(RepositoryException.ValidationError("Bean name cannot be empty"))
            } else {
                val bean = beanDao.getBeanByName(name)
                Result.success(bean)
            }
        } catch (exception: Exception) {
            Result.failure(RepositoryException.DatabaseError("Failed to get bean by name", exception))
        }
    }
    
    /**
     * Add a new bean with validation.
     * @param bean The bean to add
     * @return Result indicating success or failure with validation errors
     */
    suspend fun addBean(bean: Bean): Result<Unit> {
        return try {
            // Validate bean data
            val validationResult = bean.validate()
            if (!validationResult.isValid) {
                return Result.failure(RepositoryException.ValidationError(
                    "Bean validation failed: ${validationResult.errors.joinToString(", ")}"
                ))
            }
            
            // Check for name uniqueness
            val existingBean = beanDao.getBeanByName(bean.name)
            if (existingBean != null && existingBean.id != bean.id) {
                return Result.failure(RepositoryException.ValidationError(
                    "A bean with the name '${bean.name}' already exists"
                ))
            }
            
            beanDao.insertBean(bean)
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(RepositoryException.DatabaseError("Failed to add bean", exception))
        }
    }
    
    /**
     * Update an existing bean with validation.
     * @param bean The bean to update
     * @return Result indicating success or failure with validation errors
     */
    suspend fun updateBean(bean: Bean): Result<Unit> {
        return try {
            // Validate bean data
            val validationResult = bean.validate()
            if (!validationResult.isValid) {
                return Result.failure(RepositoryException.ValidationError(
                    "Bean validation failed: ${validationResult.errors.joinToString(", ")}"
                ))
            }
            
            // Check if bean exists
            val existingBean = beanDao.getBeanById(bean.id)
            if (existingBean == null) {
                return Result.failure(RepositoryException.NotFoundError("Bean not found"))
            }
            
            // Check for name uniqueness (excluding current bean)
            val beanWithSameName = beanDao.getBeanByName(bean.name)
            if (beanWithSameName != null && beanWithSameName.id != bean.id) {
                return Result.failure(RepositoryException.ValidationError(
                    "A bean with the name '${bean.name}' already exists"
                ))
            }
            
            beanDao.updateBean(bean)
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(RepositoryException.DatabaseError("Failed to update bean", exception))
        }
    }
    
    /**
     * Delete a bean.
     * @param bean The bean to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteBean(bean: Bean): Result<Unit> {
        return try {
            // Check if bean exists
            val existingBean = beanDao.getBeanById(bean.id)
            if (existingBean == null) {
                return Result.failure(RepositoryException.NotFoundError("Bean not found"))
            }
            
            beanDao.deleteBean(bean)
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(RepositoryException.DatabaseError("Failed to delete bean", exception))
        }
    }
    
    /**
     * Update the last grinder setting for a specific bean.
     * @param beanId The ID of the bean
     * @param grinderSetting The grinder setting to save
     * @return Result indicating success or failure
     */
    suspend fun updateLastGrinderSetting(beanId: String, grinderSetting: String): Result<Unit> {
        return try {
            if (beanId.isBlank()) {
                return Result.failure(RepositoryException.ValidationError("Bean ID cannot be empty"))
            }
            if (grinderSetting.isBlank()) {
                return Result.failure(RepositoryException.ValidationError("Grinder setting cannot be empty"))
            }
            if (grinderSetting.length > 50) {
                return Result.failure(RepositoryException.ValidationError("Grinder setting cannot exceed 50 characters"))
            }
            
            // Check if bean exists
            val existingBean = beanDao.getBeanById(beanId)
            if (existingBean == null) {
                return Result.failure(RepositoryException.NotFoundError("Bean not found"))
            }
            
            beanDao.updateLastGrinderSetting(beanId, grinderSetting)
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(RepositoryException.DatabaseError("Failed to update grinder setting", exception))
        }
    }
    
    /**
     * Set a bean as active or inactive.
     * @param beanId The ID of the bean
     * @param isActive Whether the bean should be active
     * @return Result indicating success or failure
     */
    suspend fun updateBeanActiveStatus(beanId: String, isActive: Boolean): Result<Unit> {
        return try {
            if (beanId.isBlank()) {
                return Result.failure(RepositoryException.ValidationError("Bean ID cannot be empty"))
            }
            
            // Check if bean exists
            val existingBean = beanDao.getBeanById(beanId)
            if (existingBean == null) {
                return Result.failure(RepositoryException.NotFoundError("Bean not found"))
            }
            
            beanDao.updateBeanActiveStatus(beanId, isActive)
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(RepositoryException.DatabaseError("Failed to update bean active status", exception))
        }
    }
    
    /**
     * Get beans filtered by active status and name search.
     * @param activeOnly Whether to show only active beans
     * @param searchQuery Search query for bean names
     * @return Flow of filtered beans with error handling
     */
    fun getFilteredBeans(activeOnly: Boolean, searchQuery: String): Flow<Result<List<Bean>>> = flow {
        beanDao.getFilteredBeans(activeOnly, searchQuery.trim()).collect { beans ->
            emit(Result.success(beans))
        }
    }.catch { exception ->
        emit(Result.failure(RepositoryException.DatabaseError("Failed to get filtered beans", exception)))
    }
    
    /**
     * Get count of active beans.
     * @return Result containing the count of active beans
     */
    suspend fun getActiveBeanCount(): Result<Int> {
        return try {
            val count = beanDao.getActiveBeanCount()
            Result.success(count)
        } catch (exception: Exception) {
            Result.failure(RepositoryException.DatabaseError("Failed to get active bean count", exception))
        }
    }
    
    /**
     * Validate bean data without saving.
     * @param bean The bean to validate
     * @return ValidationResult with validation status and errors
     */
    suspend fun validateBean(bean: Bean): ValidationResult {
        val validationResult = bean.validate()
        
        // Additional repository-level validation
        if (validationResult.isValid) {
            try {
                // Check for name uniqueness
                val existingBean = beanDao.getBeanByName(bean.name)
                if (existingBean != null && existingBean.id != bean.id) {
                    return ValidationResult(
                        isValid = false,
                        errors = listOf("A bean with the name '${bean.name}' already exists")
                    )
                }
            } catch (exception: Exception) {
                return ValidationResult(
                    isValid = false,
                    errors = listOf("Failed to validate bean uniqueness")
                )
            }
        }
        
        return validationResult
    }
    
    /**
     * Set the current bean for shot recording.
     * Implements requirement 3.3 for remembering bean selection.
     * @param beanId The ID of the bean to set as current
     * @return Result indicating success or failure
     */
    suspend fun setCurrentBean(beanId: String): Result<Unit> {
        return try {
            if (beanId.isBlank()) {
                return Result.failure(RepositoryException.ValidationError("Bean ID cannot be empty"))
            }
            
            // Check if bean exists and is active
            val bean = beanDao.getBeanById(beanId)
            if (bean == null) {
                return Result.failure(RepositoryException.NotFoundError("Bean not found"))
            }
            if (!bean.isActive) {
                return Result.failure(RepositoryException.ValidationError("Cannot select inactive bean"))
            }
            
            _currentBeanId = beanId
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(RepositoryException.DatabaseError("Failed to set current bean", exception))
        }
    }
    
    /**
     * Get the current bean for shot recording.
     * @return Result containing the current bean or null if none selected
     */
    suspend fun getCurrentBean(): Result<Bean?> {
        return try {
            val currentBeanId = _currentBeanId
            if (currentBeanId != null) {
                val bean = beanDao.getBeanById(currentBeanId)
                // If bean is no longer active or doesn't exist, clear current selection
                if (bean == null || !bean.isActive) {
                    _currentBeanId = null
                    Result.success(null)
                } else {
                    Result.success(bean)
                }
            } else {
                Result.success(null)
            }
        } catch (exception: Exception) {
            Result.failure(RepositoryException.DatabaseError("Failed to get current bean", exception))
        }
    }
    
    /**
     * Clear the current bean selection.
     */
    fun clearCurrentBean() {
        _currentBeanId = null
    }
    
    /**
     * Get the current bean ID.
     * @return Current bean ID or null if none selected
     */
    fun getCurrentBeanId(): String? {
        return _currentBeanId
    }
}

/**
 * Sealed class representing different types of repository exceptions.
 */
sealed class RepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class DatabaseError(message: String, cause: Throwable? = null) : RepositoryException(message, cause)
    class ValidationError(message: String) : RepositoryException(message)
    class NotFoundError(message: String) : RepositoryException(message)
    class NetworkError(message: String, cause: Throwable? = null) : RepositoryException(message, cause)
    class PermissionError(message: String) : RepositoryException(message)
}