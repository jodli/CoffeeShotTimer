package com.jodli.coffeeshottimer.ui.components

/**
 * Utility object for mapping between angles and time values with non-linear scaling.
 *
 * The circular slider uses a non-linear scale where the precision range (20-40 seconds)
 * gets more arc length for better precision, as this is where most espresso shots occur.
 *
 * Scale allocation:
 * - 0-90° → 5-20 seconds (15 seconds in 90°)
 * - 90-270° → 20-40 seconds (20 seconds in 180°) - PRECISION RANGE
 * - 270-360° → 40-60 seconds (20 seconds in 90°)
 */
object TimerScaleMapper {
    private const val MIN_TIME = ValidationUtils.MANUAL_TIMER_MIN_SECONDS
    private const val MAX_TIME = ValidationUtils.MANUAL_TIMER_MAX_SECONDS
    private const val PRECISION_START = ValidationUtils.MANUAL_TIMER_PRECISION_RANGE_START
    private const val PRECISION_END = ValidationUtils.MANUAL_TIMER_PRECISION_RANGE_END

    /**
     * Maps angle (0-360°) to time (5-60 seconds) with non-linear scaling.
     * 20-40 second range gets more arc length (180°) for better precision.
     *
     * @param angleDegrees Angle in degrees (0° at top, clockwise)
     * @return Time in seconds (5-60)
     */
    fun angleToTime(angleDegrees: Float): Int {
        val normalizedAngle = (angleDegrees + 360f) % 360f

        return when {
            // Range 1: 0-90° maps to 5-20 seconds (15 seconds in 90°)
            normalizedAngle < 90f -> {
                val progress = normalizedAngle / 90f
                (MIN_TIME + (PRECISION_START - MIN_TIME) * progress).toInt()
            }
            // Range 2: 90-270° maps to 20-40 seconds (20 seconds in 180°) - PRECISION RANGE
            normalizedAngle < 270f -> {
                val progress = (normalizedAngle - 90f) / 180f
                (PRECISION_START + (PRECISION_END - PRECISION_START) * progress).toInt()
            }
            // Range 3: 270-360° maps to 40-60 seconds (20 seconds in 90°)
            else -> {
                val progress = (normalizedAngle - 270f) / 90f
                (PRECISION_END + (MAX_TIME - PRECISION_END) * progress).toInt()
            }
        }
    }

    /**
     * Maps time (5-60 seconds) to angle (0-360°) for initial display.
     * Inverse of angleToTime().
     *
     * @param timeSeconds Time in seconds (5-60)
     * @return Angle in degrees (0-360°)
     */
    fun timeToAngle(timeSeconds: Int): Float {
        val clampedTime = timeSeconds.coerceIn(MIN_TIME, MAX_TIME)

        return when {
            // Range 1: 5-20 seconds maps to 0-90°
            clampedTime < PRECISION_START -> {
                val progress = (clampedTime - MIN_TIME).toFloat() / (PRECISION_START - MIN_TIME)
                progress * 90f
            }
            // Range 2: 20-40 seconds maps to 90-270° - PRECISION RANGE
            clampedTime <= PRECISION_END -> {
                val progress = (clampedTime - PRECISION_START).toFloat() / (PRECISION_END - PRECISION_START)
                90f + (progress * 180f)
            }
            // Range 3: 40-60 seconds maps to 270-360°
            else -> {
                val progress = (clampedTime - PRECISION_END).toFloat() / (MAX_TIME - PRECISION_END)
                270f + (progress * 90f)
            }
        }
    }
}
