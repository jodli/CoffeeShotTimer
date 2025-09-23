package com.jodli.coffeeshottimer

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for the Espresso Shot Tracker app.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 * Configures Coil image loading with optimized memory and disk caching.
 */
@HiltAndroidApp
class CoffeeShotTimerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Configure Coil for optimized image loading and caching
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    // Use 25% of available memory for image cache
                    .maxSizePercent(0.25)
                    // Weak references allow for faster garbage collection
                    .weakReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    // Set disk cache to 50MB
                    .maxSizeBytes(50 * 1024 * 1024)
                    .build()
            }
            // Optimize for photo loading use case
            .respectCacheHeaders(false)
            // Enable crossfade by default for smooth transitions
            .crossfade(true)
            // Cache policy for better performance with local files
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            // Enable debug logging in debug builds
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()

        // Set the configured ImageLoader as the default
        Coil.setImageLoader(imageLoader)
    }
}
