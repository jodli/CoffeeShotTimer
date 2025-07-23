package com.jodli.coffeeshottimer.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jodli.coffeeshottimer.data.model.Bean
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Bean entity operations.
 * Provides methods for CRUD operations and queries related to coffee beans.
 */
@Dao
interface BeanDao {

    /**
     * Get all beans ordered by creation date (newest first).
     */
    @Query("SELECT * FROM beans ORDER BY createdAt DESC")
    fun getAllBeans(): Flow<List<Bean>>

    /**
     * Get all active beans ordered by creation date (newest first).
     */
    @Query("SELECT * FROM beans WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveBeans(): Flow<List<Bean>>

    /**
     * Get a specific bean by ID.
     */
    @Query("SELECT * FROM beans WHERE id = :beanId")
    suspend fun getBeanById(beanId: String): Bean?

    /**
     * Get a bean by name (for uniqueness validation).
     */
    @Query("SELECT * FROM beans WHERE name = :name LIMIT 1")
    suspend fun getBeanByName(name: String): Bean?

    /**
     * Insert a new bean.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBean(bean: Bean)

    /**
     * Update an existing bean.
     */
    @Update
    suspend fun updateBean(bean: Bean)

    /**
     * Delete a bean (this will cascade delete all associated shots).
     */
    @Delete
    suspend fun deleteBean(bean: Bean)

    /**
     * Update the last grinder setting for a specific bean.
     */
    @Query("UPDATE beans SET lastGrinderSetting = :grinderSetting WHERE id = :beanId")
    suspend fun updateLastGrinderSetting(beanId: String, grinderSetting: String)

    /**
     * Set a bean as active/inactive.
     */
    @Query("UPDATE beans SET isActive = :isActive WHERE id = :beanId")
    suspend fun updateBeanActiveStatus(beanId: String, isActive: Boolean)

    /**
     * Get beans filtered by active status and name search.
     */
    @Query(
        """
        SELECT * FROM beans 
        WHERE (:activeOnly = 0 OR isActive = 1) 
        AND (:searchQuery = '' OR name LIKE '%' || :searchQuery || '%')
        ORDER BY createdAt DESC
    """
    )
    fun getFilteredBeans(activeOnly: Boolean, searchQuery: String): Flow<List<Bean>>

    /**
     * Get count of active beans.
     */
    @Query("SELECT COUNT(*) FROM beans WHERE isActive = 1")
    suspend fun getActiveBeanCount(): Int
}