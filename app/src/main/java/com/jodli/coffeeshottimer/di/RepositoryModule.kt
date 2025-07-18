package com.jodli.coffeeshottimer.di

import com.jodli.coffeeshottimer.data.dao.BeanDao
import com.jodli.coffeeshottimer.data.dao.ShotDao
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.data.repository.ShotRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing repository dependencies.
 * This module provides repository instances with their required dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    /**
     * Provides the BeanRepository instance.
     * Uses singleton scope to ensure consistent data access.
     * 
     * @param beanDao The BeanDao dependency
     * @return BeanRepository instance
     */
    @Provides
    @Singleton
    fun provideBeanRepository(beanDao: BeanDao): BeanRepository {
        return BeanRepository(beanDao)
    }
    
    /**
     * Provides the ShotRepository instance.
     * Uses singleton scope to ensure consistent data access.
     * 
     * @param shotDao The ShotDao dependency
     * @param beanDao The BeanDao dependency (needed for bean validation)
     * @return ShotRepository instance
     */
    @Provides
    @Singleton
    fun provideShotRepository(
        shotDao: ShotDao,
        beanDao: BeanDao
    ): ShotRepository {
        return ShotRepository(shotDao, beanDao)
    }
}