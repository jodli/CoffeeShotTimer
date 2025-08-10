package com.jodli.coffeeshottimer.di

import com.jodli.coffeeshottimer.data.dao.BeanDao
import com.jodli.coffeeshottimer.data.dao.GrinderConfigDao
import com.jodli.coffeeshottimer.data.dao.ShotDao
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.data.repository.GrinderConfigRepository
import com.jodli.coffeeshottimer.data.repository.ShotRepository
import com.jodli.coffeeshottimer.data.storage.PhotoCaptureManager
import com.jodli.coffeeshottimer.data.storage.PhotoCaptureManagerImpl
import com.jodli.coffeeshottimer.data.storage.PhotoStorageManager
import com.jodli.coffeeshottimer.data.storage.PhotoStorageManagerImpl
import com.jodli.coffeeshottimer.data.util.MemoryOptimizer
import com.jodli.coffeeshottimer.data.util.PerformanceMonitor
import dagger.Binds
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
abstract class RepositoryModule {

    companion object {
        /**
         * Provides the BeanRepository instance.
         * Uses singleton scope to ensure consistent data access.
         *
         * @param beanDao The BeanDao dependency
         * @param photoStorageManager The PhotoStorageManager dependency
         * @return BeanRepository instance
         */
        @Provides
        @Singleton
        fun provideBeanRepository(
            beanDao: BeanDao,
            photoStorageManager: PhotoStorageManager
        ): BeanRepository {
            return BeanRepository(beanDao, photoStorageManager)
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

        /**
         * Provides the GrinderConfigRepository instance.
         * Uses singleton scope to ensure consistent data access.
         *
         * @param grinderConfigDao The GrinderConfigDao dependency
         * @return GrinderConfigRepository instance
         */
        @Provides
        @Singleton
        fun provideGrinderConfigRepository(
            grinderConfigDao: GrinderConfigDao
        ): GrinderConfigRepository {
            return GrinderConfigRepository(grinderConfigDao)
        }

        /**
         * Provides the MemoryOptimizer instance.
         * Uses singleton scope to ensure consistent memory management across the app.
         *
         * @return MemoryOptimizer instance
         */
        @Provides
        @Singleton
        fun provideMemoryOptimizer(): MemoryOptimizer {
            return MemoryOptimizer()
        }

        /**
         * Provides the PerformanceMonitor instance.
         * Uses singleton scope to ensure consistent performance monitoring across the app.
         *
         * @return PerformanceMonitor instance
         */
        @Provides
        @Singleton
        fun providePerformanceMonitor(): PerformanceMonitor {
            return PerformanceMonitor()
        }
    }

    /**
     * Binds the PhotoStorageManagerImpl to PhotoStorageManager interface.
     * Uses singleton scope to ensure consistent photo storage management.
     *
     * @param photoStorageManagerImpl The implementation to bind
     * @return PhotoStorageManager interface
     */
    @Binds
    @Singleton
    abstract fun bindPhotoStorageManager(
        photoStorageManagerImpl: PhotoStorageManagerImpl
    ): PhotoStorageManager

    /**
     * Binds the PhotoCaptureManagerImpl to PhotoCaptureManager interface.
     * Uses singleton scope to ensure consistent photo capture management.
     *
     * @param photoCaptureManagerImpl The implementation to bind
     * @return PhotoCaptureManager interface
     */
    @Binds
    @Singleton
    abstract fun bindPhotoCaptureManager(
        photoCaptureManagerImpl: PhotoCaptureManagerImpl
    ): PhotoCaptureManager
}