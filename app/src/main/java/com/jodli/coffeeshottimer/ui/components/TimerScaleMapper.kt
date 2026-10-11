package com.jodli.coffeeshottimer.ui.components

import kotlin.math.roundToInt

/**
 * Maps between dial angles and seconds for the shot timer dial.
 *
 * One revolution is [DIAL_SECONDS], linear, 0 s at the top, clockwise. Both timer modes and the
 * app icon (scripts/generate-icons.py, DIAL_S) use this scale, so the 25-30 s band always sits in
 * the same place. 45 s rather than 60 s gives the band 40 degrees of ring instead of 30.
 */
object TimerScaleMapper {
    const val DIAL_SECONDS = 45

    private const val MIN_TIME = ValidationUtils.MANUAL_TIMER_MIN_SECONDS

    // how close to the top a drag has to be before crossing it counts as a wrap, in seconds
    private const val WRAP_ZONE = 10

    /** Angle in degrees (0 at the top, clockwise) for a time; times past the dial sit at the end. */
    fun timeToAngle(seconds: Float): Float = seconds.coerceIn(0f, DIAL_SECONDS.toFloat()) / DIAL_SECONDS * 360f

    /** Nearest whole second for an angle, never below the manual minimum. */
    fun angleToTime(angleDegrees: Float): Int {
        val normalized = (angleDegrees % 360f + 360f) % 360f
        return (normalized / 360f * DIAL_SECONDS).roundToInt().coerceIn(MIN_TIME, DIAL_SECONDS)
    }

    /**
     * Time for a drag position. Dragging across the top would jump between the ends of the scale,
     * so the value pins to the end it came from instead.
     */
    fun dragToTime(angleDegrees: Float, previousSeconds: Int): Int {
        val next = angleToTime(angleDegrees)
        return when {
            previousSeconds >= DIAL_SECONDS - WRAP_ZONE && next <= MIN_TIME + WRAP_ZONE -> DIAL_SECONDS
            previousSeconds <= MIN_TIME + WRAP_ZONE && next >= DIAL_SECONDS - WRAP_ZONE -> MIN_TIME
            else -> next
        }
    }
}
