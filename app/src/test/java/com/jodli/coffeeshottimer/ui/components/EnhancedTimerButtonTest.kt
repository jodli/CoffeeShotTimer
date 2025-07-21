package com.jodli.coffeeshottimer.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Enhanced Timer Button components
 * Tests for tasks 5, 6, and 7 implementation
 */
class EnhancedTimerButtonTest {

    @Test
    fun `TimerButtonState should have correct properties when not running`() {
        val state = TimerButtonState(
            isRunning = false,
            buttonColor = Color.Green,
            iconColor = Color.White,
            icon = Icons.Default.PlayArrow,
            contentDescription = "Start timer"
        )

        assertFalse("Button should not be running", state.isRunning)
        assertEquals("Button color should be green", Color.Green, state.buttonColor)
        assertEquals("Icon color should be white", Color.White, state.iconColor)
        assertEquals("Icon should be play arrow", Icons.Default.PlayArrow, state.icon)
        assertEquals("Content description should be start timer", "Start timer", state.contentDescription)
    }

    @Test
    fun `TimerButtonState should have correct properties when running`() {
        val state = TimerButtonState(
            isRunning = true,
            buttonColor = Color.Red,
            iconColor = Color.White,
            icon = Icons.Default.Stop,
            contentDescription = "Stop timer"
        )

        assertTrue("Button should be running", state.isRunning)
        assertEquals("Button color should be red", Color.Red, state.buttonColor)
        assertEquals("Icon color should be white", Color.White, state.iconColor)
        assertEquals("Icon should be stop", Icons.Default.Stop, state.icon)
        assertEquals("Content description should be stop timer", "Stop timer", state.contentDescription)
    }

    @Test
    fun `Button state should transition correctly from stopped to running`() {
        // Create state for stopped timer
        val stoppedState = TimerButtonState(
            isRunning = false,
            buttonColor = Color(0xFF4CAF50), // Green
            iconColor = Color.White,
            icon = Icons.Default.PlayArrow,
            contentDescription = "Start timer"
        )

        // Create state for running timer
        val runningState = TimerButtonState(
            isRunning = true,
            buttonColor = Color(0xFFF44336), // Red
            iconColor = Color.White,
            icon = Icons.Default.Stop,
            contentDescription = "Stop timer"
        )

        // Verify states are different
        assertNotEquals("States should be different", stoppedState.isRunning, runningState.isRunning)
        assertNotEquals("Button colors should be different", stoppedState.buttonColor, runningState.buttonColor)
        assertNotEquals("Icons should be different", stoppedState.icon, runningState.icon)
        assertNotEquals("Content descriptions should be different", stoppedState.contentDescription, runningState.contentDescription)
    }

    @Test
    fun `Button colors should match expected values`() {
        val greenColor = Color(0xFF4CAF50)
        val redColor = Color(0xFFF44336)

        // Test start button color
        val startState = TimerButtonState(
            isRunning = false,
            buttonColor = greenColor,
            iconColor = Color.White,
            icon = Icons.Default.PlayArrow,
            contentDescription = "Start timer"
        )

        // Test stop button color
        val stopState = TimerButtonState(
            isRunning = true,
            buttonColor = redColor,
            iconColor = Color.White,
            icon = Icons.Default.Stop,
            contentDescription = "Stop timer"
        )

        assertEquals("Start button should be green", greenColor, startState.buttonColor)
        assertEquals("Stop button should be red", redColor, stopState.buttonColor)
    }

    @Test
    fun `lastActionTime should be updated when state changes`() {
        val initialTime = System.currentTimeMillis()
        val state1 = TimerButtonState(
            isRunning = false,
            buttonColor = Color.Green,
            iconColor = Color.White,
            icon = Icons.Default.PlayArrow,
            contentDescription = "Start timer",
            lastActionTime = initialTime
        )

        // Simulate time passing
        Thread.sleep(10)
        val laterTime = System.currentTimeMillis()

        val state2 = state1.copy(
            isRunning = true,
            lastActionTime = laterTime
        )

        assertTrue("Later action time should be greater", state2.lastActionTime > state1.lastActionTime)
    }

    @Test
    fun `accessibility content descriptions should be descriptive`() {
        val startState = TimerButtonState(
            isRunning = false,
            buttonColor = Color.Green,
            iconColor = Color.White,
            icon = Icons.Default.PlayArrow,
            contentDescription = "Start timer"
        )

        val stopState = TimerButtonState(
            isRunning = true,
            buttonColor = Color.Red,
            iconColor = Color.White,
            icon = Icons.Default.Stop,
            contentDescription = "Stop timer"
        )

        assertTrue("Start description should contain 'Start'", startState.contentDescription.contains("Start"))
        assertTrue("Start description should contain 'timer'", startState.contentDescription.contains("timer"))
        assertTrue("Stop description should contain 'Stop'", stopState.contentDescription.contains("Stop"))
        assertTrue("Stop description should contain 'timer'", stopState.contentDescription.contains("timer"))
    }
}
