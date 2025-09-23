package com.jodli.coffeeshottimer.data.util

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

    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val MAX_SAMPLES = 100
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
)
