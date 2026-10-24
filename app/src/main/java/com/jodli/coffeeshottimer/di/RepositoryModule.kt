package com.jodli.coffeeshottimer.di

import com.jodli.coffeeshottimer.data.storage.PhotoCaptureManager
import com.jodli.coffeeshottimer.data.storage.PhotoCaptureManagerImpl
import com.jodli.coffeeshottimer.data.storage.PhotoStorageManager
import com.jodli.coffeeshottimer.data.storage.PhotoStorageManagerImpl
import dagger.Binds
import dagger.Module
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
