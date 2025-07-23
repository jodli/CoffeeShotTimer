package com.jodli.coffeeshottimer.data.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Memory optimization utility for managing app performance.
 * Provides memory cleanup, cache management, and resource optimization.
 */
@Singleton
class MemoryOptimizer @Inject constructor() {

    private val memoryCleanupJobs = ConcurrentHashMap<String, Job>()
    private val weakReferences = ConcurrentHashMap<String, WeakReference<Any>>()

    companion object {
        private const val MEMORY_CLEANUP_DELAY = 30_000L // 30 seconds
        private const val CACHE_CLEANUP_DELAY = 60_000L // 1 minute
        private const val MAX_CACHE_SIZE = 100
    }

    /**
     * Schedule memory cleanup for a specific component.
     * @param componentId Unique identifier for the component
     * @param cleanupAction Action to perform during cleanup
     */
    fun scheduleMemoryCleanup(componentId: String, cleanupAction: suspend () -> Unit) {
        // Cancel existing cleanup job if any
        memoryCleanupJobs[componentId]?.cancel()

        // Schedule new cleanup job
        val job = CoroutineScope(Dispatchers.IO).launch {
            delay(MEMORY_CLEANUP_DELAY)
            try {
                cleanupAction()
            } catch (e: Exception) {
                // Log error but don't crash
                android.util.Log.w("MemoryOptimizer", "Memory cleanup failed for $componentId", e)
            } finally {
                memoryCleanupJobs.remove(componentId)
            }
        }

        memoryCleanupJobs[componentId] = job
    }

    /**
     * Cancel scheduled memory cleanup for a component.
     * @param componentId Unique identifier for the component
     */
    fun cancelMemoryCleanup(componentId: String) {
        memoryCleanupJobs[componentId]?.cancel()
        memoryCleanupJobs.remove(componentId)
    }

    /**
     * Store a weak reference to an object for memory management.
     * @param key Unique key for the reference
     * @param obj Object to store weakly
     */
    fun storeWeakReference(key: String, obj: Any) {
        weakReferences[key] = WeakReference(obj)

        // Clean up null references periodically
        if (weakReferences.size > MAX_CACHE_SIZE) {
            cleanupWeakReferences()
        }
    }

    /**
     * Retrieve a weakly referenced object.
     * @param key Unique key for the reference
     * @return The referenced object or null if garbage collected
     */
    fun <T> getWeakReference(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return weakReferences[key]?.get() as? T
    }

    /**
     * Clean up null weak references.
     */
    private fun cleanupWeakReferences() {
        val keysToRemove = mutableListOf<String>()

        weakReferences.forEach { (key, ref) ->
            if (ref.get() == null) {
                keysToRemove.add(key)
            }
        }

        keysToRemove.forEach { key ->
            weakReferences.remove(key)
        }
    }

    /**
     * Force garbage collection (use sparingly).
     */
    fun forceGarbageCollection() {
        System.gc()
    }

    /**
     * Get current memory usage information.
     * @return Memory usage statistics
     */
    fun getMemoryUsage(): MemoryUsage {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()

        return MemoryUsage(
            totalMemory = totalMemory,
            freeMemory = freeMemory,
            usedMemory = usedMemory,
            maxMemory = maxMemory,
            usagePercentage = (usedMemory.toDouble() / maxMemory.toDouble()) * 100
        )
    }

    /**
     * Check if memory usage is high and cleanup is recommended.
     * @return True if memory usage is above 80%
     */
    fun isMemoryUsageHigh(): Boolean {
        return getMemoryUsage().usagePercentage > 80.0
    }

    /**
     * Perform comprehensive memory cleanup.
     */
    suspend fun performMemoryCleanup() {
        // Clean up weak references
        cleanupWeakReferences()

        // Cancel old cleanup jobs
        val expiredJobs = memoryCleanupJobs.values.filter { it.isCompleted || it.isCancelled }
        expiredJobs.forEach { job ->
            memoryCleanupJobs.values.removeAll { it == job }
        }

        // Suggest garbage collection if memory usage is high
        if (isMemoryUsageHigh()) {
            forceGarbageCollection()
        }
    }

    /**
     * Clean up all resources when the optimizer is no longer needed.
     */
    fun cleanup() {
        memoryCleanupJobs.values.forEach { it.cancel() }
        memoryCleanupJobs.clear()
        weakReferences.clear()
    }
}

/**
 * Data class representing memory usage statistics.
 */
data class MemoryUsage(
    val totalMemory: Long,
    val freeMemory: Long,
    val usedMemory: Long,
    val maxMemory: Long,
    val usagePercentage: Double
) {
    fun getFormattedUsage(): String {
        val usedMB = usedMemory / (1024 * 1024)
        val maxMB = maxMemory / (1024 * 1024)
        return "${usedMB}MB / ${maxMB}MB (${String.format("%.1f", usagePercentage)}%)"
    }
}