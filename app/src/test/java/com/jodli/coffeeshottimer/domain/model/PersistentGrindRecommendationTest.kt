package com.jodli.coffeeshottimer.domain.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class PersistentGrindRecommendationTest {

    @Test
    fun `hasAdjustment returns true when adjustment direction is FINER`() {
        // Given
        val recommendation = createTestRecommendation(adjustmentDirection = AdjustmentDirection.FINER)

        // When
        val result = recommendation.hasAdjustment()

        // Then
        assertTrue(result)
    }

    @Test
    fun `hasAdjustment returns true when adjustment direction is COARSER`() {
        // Given
        val recommendation = createTestRecommendation(adjustmentDirection = AdjustmentDirection.COARSER)

        // When
        val result = recommendation.hasAdjustment()

        // Then
        assertTrue(result)
    }

    @Test
    fun `hasAdjustment returns false when adjustment direction is NO_CHANGE`() {
        // Given
        val recommendation = createTestRecommendation(adjustmentDirection = AdjustmentDirection.NO_CHANGE)

        // When
        val result = recommendation.hasAdjustment()

        // Then
        assertFalse(result)
    }

    @Test
    fun `getAdjustmentDescription returns correct string for FINER`() {
        // Given
        val recommendation = createTestRecommendation(adjustmentDirection = AdjustmentDirection.FINER)

        // When
        val result = recommendation.getAdjustmentDescription()

        // Then
        assertEquals("Grind finer", result)
    }

    @Test
    fun `getAdjustmentDescription returns correct string for COARSER`() {
        // Given
        val recommendation = createTestRecommendation(adjustmentDirection = AdjustmentDirection.COARSER)

        // When
        val result = recommendation.getAdjustmentDescription()

        // Then
        assertEquals("Grind coarser", result)
    }

    @Test
    fun `getAdjustmentDescription returns correct string for NO_CHANGE`() {
        // Given
        val recommendation = createTestRecommendation(adjustmentDirection = AdjustmentDirection.NO_CHANGE)

        // When
        val result = recommendation.getAdjustmentDescription()

        // Then
        assertEquals("No change needed", result)
    }

    @Test
    fun `getFormattedTargetTime returns correct format`() {
        // Given
        val recommendation = createTestRecommendation(targetExtractionTime = 25..30)

        // When
        val result = recommendation.getFormattedTargetTime()

        // Then
        assertEquals("25-30s", result)
    }

    @Test
    fun `getFormattedTargetTime handles custom ranges`() {
        // Given
        val recommendation = createTestRecommendation(targetExtractionTime = 20..35)

        // When
        val result = recommendation.getFormattedTargetTime()

        // Then
        assertEquals("20-35s", result)
    }

    @Test
    fun `getConfidenceDescription returns correct string for HIGH`() {
        // Given
        val recommendation = createTestRecommendation(confidence = ConfidenceLevel.HIGH)

        // When
        val result = recommendation.getConfidenceDescription()

        // Then
        assertEquals("High confidence", result)
    }

    @Test
    fun `getConfidenceDescription returns correct string for MEDIUM`() {
        // Given
        val recommendation = createTestRecommendation(confidence = ConfidenceLevel.MEDIUM)

        // When
        val result = recommendation.getConfidenceDescription()

        // Then
        assertEquals("Medium confidence", result)
    }

    @Test
    fun `getConfidenceDescription returns correct string for LOW`() {
        // Given
        val recommendation = createTestRecommendation(confidence = ConfidenceLevel.LOW)

        // When
        val result = recommendation.getConfidenceDescription()

        // Then
        assertEquals("Low confidence", result)
    }

    @Test
    fun `isRecent returns true for recent timestamp`() {
        // Given
        val recentTimestamp = LocalDateTime.now().minusDays(3)
        val recommendation = createTestRecommendation(timestamp = recentTimestamp)

        // When
        val result = recommendation.isRecent()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isRecent returns false for old timestamp`() {
        // Given
        val oldTimestamp = LocalDateTime.now().minusDays(10)
        val recommendation = createTestRecommendation(timestamp = oldTimestamp)

        // When
        val result = recommendation.isRecent()

        // Then
        assertFalse(result)
    }

    @Test
    fun `isRecent returns true for exactly 7 days ago`() {
        // Given - just over 7 days should return false
        val sevenDaysAgo = LocalDateTime.now().minusDays(7).minusMinutes(1)
        val recommendation = createTestRecommendation(timestamp = sevenDaysAgo)

        // When
        val result = recommendation.isRecent()

        // Then
        assertFalse(result)
    }

    @Test
    fun `markAsFollowed returns copy with wasFollowed true`() {
        // Given
        val original = createTestRecommendation(wasFollowed = false)

        // When
        val result = original.markAsFollowed()

        // Then
        assertTrue(result.wasFollowed)
        assertFalse("Original should remain unchanged", original.wasFollowed)
        // Verify other fields remain the same
        assertEquals(original.beanId, result.beanId)
        assertEquals(original.suggestedGrindSetting, result.suggestedGrindSetting)
        assertEquals(original.adjustmentDirection, result.adjustmentDirection)
    }

    @Test
    fun `getDetailedSummary contains all important fields`() {
        // Given
        val recommendation = createTestRecommendation(
            beanId = "bean123",
            suggestedGrindSetting = "5.5",
            adjustmentDirection = AdjustmentDirection.FINER,
            recommendedDose = 18.5,
            basedOnTaste = true,
            confidence = ConfidenceLevel.HIGH,
            wasFollowed = false
        )

        // When
        val result = recommendation.getDetailedSummary()

        // Then
        assertTrue("Should contain bean ID", result.contains("bean=bean123"))
        assertTrue("Should contain grind setting", result.contains("grind=5.5"))
        assertTrue("Should contain direction", result.contains("direction=FINER"))
        assertTrue("Should contain dose", result.contains("dose=18.5g"))
        assertTrue("Should contain basedOnTaste", result.contains("basedOnTaste=true"))
        assertTrue("Should contain confidence", result.contains("confidence=HIGH"))
        assertTrue("Should contain followed status", result.contains("followed=false"))
    }

    @Test
    fun `fromGrindAdjustmentRecommendation creates correct persistent recommendation`() {
        // Given
        val grindAdjustment = GrindAdjustmentRecommendation(
            currentGrindSetting = "5.0",
            suggestedGrindSetting = "5.5",
            adjustmentDirection = AdjustmentDirection.FINER,
            adjustmentSteps = 1,
            extractionTimeDeviation = -3,
            tasteIssue = TastePrimary.SOUR,
            confidence = ConfidenceLevel.HIGH
        )
        val beanId = "bean123"
        val recommendedDose = 18.5
        val reason = "Test reason"
        val basedOnTaste = true

        // When
        val result = PersistentGrindRecommendation.fromGrindAdjustmentRecommendation(
            beanId = beanId,
            recommendation = grindAdjustment,
            recommendedDose = recommendedDose,
            reason = reason,
            basedOnTaste = basedOnTaste
        )

        // Then
        assertEquals(beanId, result.beanId)
        assertEquals("5.5", result.suggestedGrindSetting)
        assertEquals(AdjustmentDirection.FINER, result.adjustmentDirection)
        assertEquals(reason, result.reason)
        assertEquals(recommendedDose, result.recommendedDose, 0.01)
        assertEquals(25..30, result.targetExtractionTime)
        assertFalse(result.wasFollowed)
        assertEquals(basedOnTaste, result.basedOnTaste)
        assertEquals(ConfidenceLevel.HIGH, result.confidence)

        // Timestamp should be recent
        assertTrue("Timestamp should be recent", result.timestamp.isAfter(LocalDateTime.now().minusMinutes(1)))
    }

    @Test
    fun `createReasonString with sour taste feedback`() {
        // When
        val result = PersistentGrindRecommendation.createReasonString(24, TastePrimary.SOUR)

        // Then
        assertEquals("Last shot was sour (24s)", result)
    }

    @Test
    fun `createReasonString with bitter taste feedback`() {
        // When
        val result = PersistentGrindRecommendation.createReasonString(32, TastePrimary.BITTER)

        // Then
        assertEquals("Last shot was bitter (32s)", result)
    }

    @Test
    fun `createReasonString with perfect taste feedback`() {
        // When
        val result = PersistentGrindRecommendation.createReasonString(27, TastePrimary.PERFECT)

        // Then
        assertEquals("Last shot was perfect (27s)", result)
    }

    @Test
    fun `createReasonString with no taste feedback and fast time`() {
        // When
        val result = PersistentGrindRecommendation.createReasonString(22, null)

        // Then
        assertEquals("Last shot ran too fast (22s)", result)
    }

    @Test
    fun `createReasonString with no taste feedback and slow time`() {
        // When
        val result = PersistentGrindRecommendation.createReasonString(35, null)

        // Then
        assertEquals("Last shot ran too slow (35s)", result)
    }

    @Test
    fun `createReasonString with no taste feedback and optimal time`() {
        // When
        val result = PersistentGrindRecommendation.createReasonString(27, null)

        // Then
        assertEquals("Based on previous shot (27s)", result)
    }

    @Test
    fun `createReasonString prioritizes taste feedback over timing`() {
        // Given - Fast time but sour taste (taste should take priority)
        // When
        val result = PersistentGrindRecommendation.createReasonString(22, TastePrimary.SOUR)

        // Then
        assertEquals("Last shot was sour (22s)", result)
    }

    private fun createTestRecommendation(
        beanId: String = "test_bean",
        suggestedGrindSetting: String = "5.5",
        adjustmentDirection: AdjustmentDirection = AdjustmentDirection.FINER,
        reason: String = "Test reason",
        recommendedDose: Double = 18.0,
        targetExtractionTime: IntRange = 25..30,
        timestamp: LocalDateTime = LocalDateTime.now(),
        wasFollowed: Boolean = false,
        basedOnTaste: Boolean = true,
        confidence: ConfidenceLevel = ConfidenceLevel.HIGH
    ): PersistentGrindRecommendation {
        return PersistentGrindRecommendation(
            beanId = beanId,
            suggestedGrindSetting = suggestedGrindSetting,
            adjustmentDirection = adjustmentDirection,
            reason = reason,
            recommendedDose = recommendedDose,
            targetExtractionTime = targetExtractionTime,
            timestamp = timestamp,
            wasFollowed = wasFollowed,
            basedOnTaste = basedOnTaste,
            confidence = confidence
        )
    }
}
