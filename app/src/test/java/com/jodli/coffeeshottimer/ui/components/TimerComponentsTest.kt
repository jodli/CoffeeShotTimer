package com.jodli.coffeeshottimer.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class TimerComponentsTest {

    @Test
    fun `formatExtractionTime formats seconds correctly for under 60 seconds`() {
        // Test various second values under 60
        assertEquals("0s", formatExtractionTime(0L, "s"))
        assertEquals("1s", formatExtractionTime(1000L, "s"))
        assertEquals("25s", formatExtractionTime(25000L, "s"))
        assertEquals("30s", formatExtractionTime(30000L, "s"))
        assertEquals("59s", formatExtractionTime(59000L, "s"))
    }

    @Test
    fun `formatExtractionTime formats MM SS correctly for 60 seconds and over`() {
        // Test minute formatting for longer times
        assertEquals("01:00", formatExtractionTime(60000L, "s"))
        assertEquals("01:30", formatExtractionTime(90000L, "s"))
        assertEquals("02:00", formatExtractionTime(120000L, "s"))
        assertEquals("02:30", formatExtractionTime(150000L, "s"))
    }

    @Test
    fun `formatExtractionTime handles edge cases correctly`() {
        // Test boundary conditions
        assertEquals("59s", formatExtractionTime(59999L, "s")) // Just under 60 seconds
        assertEquals("01:00", formatExtractionTime(60000L, "s")) // Exactly 60 seconds
        assertEquals("01:00", formatExtractionTime(60001L, "s")) // Just over 60 seconds
    }

    @Test
    fun `getExtractionQuality returns correct quality for different time ranges`() {
        // Test under-extraction range (< 20s)
        assertEquals(ExtractionQuality.UNDER_EXTRACTED, getExtractionQuality(0, true))
        assertEquals(ExtractionQuality.UNDER_EXTRACTED, getExtractionQuality(10, true))
        assertEquals(ExtractionQuality.UNDER_EXTRACTED, getExtractionQuality(19, true))

        // Test optimal range (20-35s)
        assertEquals(ExtractionQuality.OPTIMAL, getExtractionQuality(20, true))
        assertEquals(ExtractionQuality.OPTIMAL, getExtractionQuality(25, true))
        assertEquals(ExtractionQuality.OPTIMAL, getExtractionQuality(30, true))
        assertEquals(ExtractionQuality.OPTIMAL, getExtractionQuality(35, true))

        // Test over-extraction range (> 35s)
        assertEquals(ExtractionQuality.OVER_EXTRACTED, getExtractionQuality(36, true))
        assertEquals(ExtractionQuality.OVER_EXTRACTED, getExtractionQuality(45, true))
        assertEquals(ExtractionQuality.OVER_EXTRACTED, getExtractionQuality(60, true))

        // Test neutral when not running
        assertEquals(ExtractionQuality.NEUTRAL, getExtractionQuality(25, false))
        assertEquals(ExtractionQuality.NEUTRAL, getExtractionQuality(0, false))
    }

    @Test
    fun `getExtractionTimeColor returns correct colors for different time ranges`() {
        // Test under-extraction color (yellow)
        val yellowColor = Color(0xFFFFC107)
        assertEquals(yellowColor, getExtractionTimeColor(10, true))
        assertEquals(yellowColor, getExtractionTimeColor(19, true))

        // Test optimal color (green)
        val greenColor = Color(0xFF4CAF50)
        assertEquals(greenColor, getExtractionTimeColor(20, true))
        assertEquals(greenColor, getExtractionTimeColor(25, true))
        assertEquals(greenColor, getExtractionTimeColor(35, true))

        // Test over-extraction color (red)
        val redColor = Color(0xFFF44336)
        assertEquals(redColor, getExtractionTimeColor(36, true))
        assertEquals(redColor, getExtractionTimeColor(45, true))

        // Test neutral color (gray)
        val grayColor = Color.Gray.copy(alpha = 0.3f)
        assertEquals(grayColor, getExtractionTimeColor(25, false))
    }

    @Test
    fun `getTimerColor converts milliseconds to seconds correctly`() {
        // Test that milliseconds are converted to seconds properly
        val greenColor = Color(0xFF4CAF50)
        assertEquals(greenColor, getTimerColor(25000L, true)) // 25 seconds
        assertEquals(greenColor, getTimerColor(30000L, true)) // 30 seconds

        val yellowColor = Color(0xFFFFC107)
        assertEquals(yellowColor, getTimerColor(15000L, true)) // 15 seconds

        val redColor = Color(0xFFF44336)
        assertEquals(redColor, getTimerColor(40000L, true)) // 40 seconds
    }

    @Test
    fun `extraction quality boundary conditions work correctly`() {
        // Test exact boundary values
        assertEquals(ExtractionQuality.UNDER_EXTRACTED, getExtractionQuality(19, true))
        assertEquals(ExtractionQuality.OPTIMAL, getExtractionQuality(20, true))
        assertEquals(ExtractionQuality.OPTIMAL, getExtractionQuality(35, true))
        assertEquals(ExtractionQuality.OVER_EXTRACTED, getExtractionQuality(36, true))
    }

    @Test
    fun `color consistency between different functions`() {
        // Ensure getTimerColor and getExtractionTimeColor return same results
        val timeMs = 25000L // 25 seconds
        val elapsedSeconds = 25
        val isRunning = true

        assertEquals(
            getTimerColor(timeMs, isRunning),
            getExtractionTimeColor(elapsedSeconds, isRunning)
        )
    }

    @Test
    fun `formatExtractionTime handles negative and zero values`() {
        // Test edge cases with negative or zero values
        assertEquals("0s", formatExtractionTime(0L, "s"))
        assertEquals("0s", formatExtractionTime(-1000L, "s")) // Should handle negative gracefully
    }
}
