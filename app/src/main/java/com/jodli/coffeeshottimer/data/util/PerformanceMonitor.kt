package com.jodli.coffeeshottimer.data.util

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance monitoring utility for tracking app performance metrics.
 * Helps identify performance bottlenecks and optimize critical operations.
 */
@Singleton
class PerformanceMonitor @Inject constructor() {
    
    private val operationTimes = ConcurrentHashMap<String, MutableList<Long>>()
    private val activeOperations = ConcurrentHashMap<String, Long>()
    
    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val MAX_SAMPLES = 100
    }
    
    /**
     * Start timing an operation.
     * @param operationName Unique name for the operation
     * @return Operation ID for stopping the timer
     */
    fun startOperation(operationName: String): String {
        val operationId = "${operationName}_${System.currentTimeMillis()}"
        val startTime = SystemClock.elapsedRealtime()
        activeOperations[operationId] = startTime
        return operationId
    }
    
    /**
     * Stop timing an operation and record the duration.
     * @param operationId The operation ID returned from startOperation
     * @param operationName The name of the operation (for grouping)
     */
    fun stopOperation(operationId: String, operationName: String) {
        val startTime = activeOperations.remove(operationId)
        if (startTime != null) {
            val duration = SystemClock.elapsedRealtime() - startTime
            recordOperationTime(operationName, duration)
        }
    }
    
    /**
     * Record a single operation time directly.
     * @param operationName Name of the operation
     * @param durationMs Duration in milliseconds
     */
    fun recordOperationTime(operationName: String, durationMs: Long) {
        val times = operationTimes.getOrPut(operationName) { mutableListOf() }
        
        synchronized(times) {
            times.add(durationMs)
            
            // Keep only the most recent samples
            if (times.size > MAX_SAMPLES) {
                times.removeAt(0)
            }
        }
        
        // Log slow operations
        if (durationMs > 1000) { // More than 1 second
            Log.w(TAG, "Slow operation detected: $operationName took ${durationMs}ms")
        }
    }
    
    /**
     * Get performance statistics for an operation.
     * @param operationName Name of the operation
     * @return Performance statistics or null if no data
     */
    fun getOperationStats(operationName: String): OperationStats? {
        val times = operationTimes[operationName] ?: return null
        
        synchronized(times) {
            if (times.isEmpty()) return null
            
            val sortedTimes = times.sorted()
            val count = times.size
            val sum = times.sum()
            val average = sum.toDouble() / count
            val median = if (count % 2 == 0) {
                (sortedTimes[count / 2 - 1] + sortedTimes[count / 2]).toDouble() / 2
            } else {
                sortedTimes[count / 2].toDouble()
            }
            val p95Index = ((count - 1) * 0.95).toInt()
            val p95 = sortedTimes[p95Index]
            
            return OperationStats(
                operationName = operationName,
                count = count,
                averageMs = average,
                medianMs = median,
                minMs = sortedTimes.first(),
                maxMs = sortedTimes.last(),
                p95Ms = p95,
                totalMs = sum
            )
        }
    }
    
    /**
     * Get all operation statistics.
     * @return Map of operation names to their statistics
     */
    fun getAllOperationStats(): Map<String, OperationStats> {
        return operationTimes.keys.mapNotNull { operationName ->
            getOperationStats(operationName)?.let { stats ->
                operationName to stats
            }
        }.toMap()
    }
    
    /**
     * Clear all performance data.
     */
    fun clearStats() {
        operationTimes.clear()
        activeOperations.clear()
    }
    
    /**
     * Log performance summary for debugging.
     */
    fun logPerformanceSummary() {
        CoroutineScope(Dispatchers.IO).launch {
            val allStats = getAllOperationStats()
            
            if (allStats.isEmpty()) {
                Log.i(TAG, "No performance data available")
                return@launch
            }
            
            Log.i(TAG, "=== Performance Summary ===")
            allStats.values.sortedByDescending { it.averageMs }.forEach { stats ->
                Log.i(TAG, "${stats.operationName}: avg=${String.format("%.1f", stats.averageMs)}ms, " +
                        "median=${String.format("%.1f", stats.medianMs)}ms, " +
                        "p95=${stats.p95Ms}ms, count=${stats.count}")
            }
            Log.i(TAG, "=========================")
        }
    }
    
    /**
     * Get operations that are performing poorly.
     * @param thresholdMs Threshold in milliseconds for considering an operation slow
     * @return List of slow operations
     */
    fun getSlowOperations(thresholdMs: Long = 500): List<OperationStats> {
        return getAllOperationStats().values.filter { stats ->
            stats.averageMs > thresholdMs || stats.p95Ms > thresholdMs * 2
        }.sortedByDescending { it.averageMs }
    }
    
    /**
     * Check if any operations are currently running longer than expected.
     * @param thresholdMs Threshold for considering an operation stuck
     * @return List of potentially stuck operations
     */
    fun getStuckOperations(thresholdMs: Long = 5000): List<String> {
        val currentTime = SystemClock.elapsedRealtime()
        return activeOperations.entries.mapNotNull { (operationId, startTime) ->
            if (currentTime - startTime > thresholdMs) {
                operationId
            } else {
                null
            }
        }
    }
}

/**
 * Data class representing performance statistics for an operation.
 */
data class OperationStats(
    val operationName: String,
    val count: Int,
    val averageMs: Double,
    val medianMs: Double,
    val minMs: Long,
    val maxMs: Long,
    val p95Ms: Long,
    val totalMs: Long
) {
    fun getFormattedSummary(): String {
        return "$operationName: avg=${String.format("%.1f", averageMs)}ms, " +
                "median=${String.format("%.1f", medianMs)}ms, " +
                "p95=${p95Ms}ms, range=${minMs}-${maxMs}ms, count=$count"
    }
}

/**
 * Inline function to measure the execution time of a block of code.
 * @param operationName Name of the operation for tracking
 * @param monitor Performance monitor instance
 * @param block Code block to measure
 * @return Result of the block execution
 */
inline fun <T> measurePerformance(
    operationName: String,
    monitor: PerformanceMonitor,
    block: () -> T
): T {
    val operationId = monitor.startOperation(operationName)
    try {
        return block()
    } finally {
        monitor.stopOperation(operationId, operationName)
    }
}