package com.jodli.coffeeshottimer.ui.util

import android.content.Context
import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.domain.model.AdjustmentDirection
import com.jodli.coffeeshottimer.domain.model.ConfidenceLevel
import com.jodli.coffeeshottimer.domain.model.GrindAdjustmentRecommendation
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GrindAdjustmentFormatter.
 * Tests the UI formatting logic for grind adjustment recommendations.
 */
class GrindAdjustmentFormatterTest {

    private lateinit var context: Context
    private lateinit var formatter: GrindAdjustmentFormatter

    @Before
    fun setup() {
        context = mockk()
        formatter = GrindAdjustmentFormatter(context)
        
        // Mock string resources
        every { context.getString(R.string.grind_adjustment_finer_single) } returns "Grind 1 step finer"
        every { context.getString(R.string.grind_adjustment_finer_multiple, any()) } returns "Grind %d steps finer"
        every { context.getString(R.string.grind_adjustment_coarser_single) } returns "Grind 1 step coarser"
        every { context.getString(R.string.grind_adjustment_coarser_multiple, any()) } returns "Grind %d steps coarser"
        every { context.getString(R.string.grind_adjustment_no_change) } returns "No adjustment needed"
        
        every { context.getString(R.string.grind_adjustment_summary_finer_single, any()) } returns "1 step finer → %s"
        every { context.getString(R.string.grind_adjustment_summary_finer_multiple, any(), any()) } returns "%d steps finer → %s"
        every { context.getString(R.string.grind_adjustment_summary_coarser_single, any()) } returns "1 step coarser → %s"
        every { context.getString(R.string.grind_adjustment_summary_coarser_multiple, any(), any()) } returns "%d steps coarser → %s"
        every { context.getString(R.string.grind_adjustment_summary_no_change) } returns "Keep current setting"
    }

    @Test
    fun `formatAdjustmentDescription returns correct descriptions for different adjustments`() {
        val testCases = listOf(
            // direction, steps, expected
            Triple(AdjustmentDirection.FINER, 1, "Grind 1 step finer"),
            Triple(AdjustmentDirection.FINER, 2, "Grind %d steps finer"),
            Triple(AdjustmentDirection.COARSER, 1, "Grind 1 step coarser"),
            Triple(AdjustmentDirection.COARSER, 3, "Grind %d steps coarser"),
            Triple(AdjustmentDirection.NO_CHANGE, 0, "No adjustment needed")
        )

        testCases.forEach { (direction, steps, expected) ->
            val recommendation = createRecommendation(direction, steps)
            val result = formatter.formatAdjustmentDescription(recommendation)
            assertEquals("Description should match for $direction/$steps", expected, result)
        }
    }

    @Test
    fun `formatAdjustmentSummary returns correct summaries for different adjustments`() {
        val testCases = listOf(
            // direction, steps, expected
            Triple(AdjustmentDirection.FINER, 1, "1 step finer → %s"),
            Triple(AdjustmentDirection.FINER, 2, "%d steps finer → %s"),
            Triple(AdjustmentDirection.COARSER, 1, "1 step coarser → %s"),
            Triple(AdjustmentDirection.COARSER, 3, "%d steps coarser → %s"),
            Triple(AdjustmentDirection.NO_CHANGE, 0, "Keep current setting")
        )

        testCases.forEach { (direction, steps, expected) ->
            val recommendation = createRecommendation(direction, steps)
            val result = formatter.formatAdjustmentSummary(recommendation)
            assertEquals("Summary should match for $direction/$steps", expected, result)
        }
    }

    private fun createRecommendation(direction: AdjustmentDirection, steps: Int): GrindAdjustmentRecommendation {
        return GrindAdjustmentRecommendation(
            currentGrindSetting = "15.0",
            suggestedGrindSetting = "14.5",
            adjustmentDirection = direction,
            adjustmentSteps = steps,
            extractionTimeDeviation = -3,
            tasteIssue = TastePrimary.SOUR,
            confidence = ConfidenceLevel.HIGH
        )
    }
}
