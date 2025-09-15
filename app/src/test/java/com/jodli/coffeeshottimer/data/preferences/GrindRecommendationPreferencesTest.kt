package com.jodli.coffeeshottimer.data.preferences

import android.content.SharedPreferences
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class GrindRecommendationPreferencesTest {

    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var grindRecommendationPreferences: GrindRecommendationPreferences

    @Before
    fun setUp() {
        mockSharedPreferences = mockk()
        mockEditor = mockk()
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.remove(any()) } returns mockEditor
        every { mockEditor.apply() } just Runs

        grindRecommendationPreferences = GrindRecommendationPreferences(mockSharedPreferences)
    }

    @Test
    fun `saveRecommendation stores recommendation as JSON with correct key`() = runTest {
        // Given
        val beanId = "bean123"
        val recommendation = SerializableGrindRecommendation.create(
            beanId = beanId,
            suggestedGrindSetting = "5.5",
            adjustmentDirection = "FINER",
            reason = "Last shot was sour (24s)",
            recommendedDose = 18.5,
            basedOnTaste = true,
            confidence = "HIGH"
        )

        val keySlot = slot<String>()
        val jsonSlot = slot<String>()

        // When
        grindRecommendationPreferences.saveRecommendation(beanId, recommendation)

        // Then
        verify { mockEditor.putString(capture(keySlot), capture(jsonSlot)) }
        verify { mockEditor.apply() }

        assertEquals("grind_recommendation_bean123", keySlot.captured)
        assertTrue("JSON should contain bean ID", jsonSlot.captured.contains("\"beanId\":\"bean123\""))
        assertTrue("JSON should contain grind setting", jsonSlot.captured.contains("\"suggestedGrindSetting\":\"5.5\""))
        assertTrue("JSON should contain adjustment direction", jsonSlot.captured.contains("\"adjustmentDirection\":\"FINER\""))
        assertTrue("JSON should contain based on taste", jsonSlot.captured.contains("\"basedOnTaste\":true"))
    }

    @Test
    fun `getRecommendation returns null when no recommendation exists`() = runTest {
        // Given
        val beanId = "nonexistent"
        every { mockSharedPreferences.getString("grind_recommendation_nonexistent", null) } returns null

        // When
        val result = grindRecommendationPreferences.getRecommendation(beanId)

        // Then
        assertNull(result)
    }

    @Test
    fun `getRecommendation returns recommendation when valid JSON exists`() = runTest {
        // Given
        val beanId = "bean123"
        val expectedRecommendation = SerializableGrindRecommendation.create(
            beanId = beanId,
            suggestedGrindSetting = "5.5",
            adjustmentDirection = "FINER",
            reason = "Last shot was sour (24s)",
            recommendedDose = 18.5,
            basedOnTaste = true,
            confidence = "HIGH"
        )
        val validJson = """
            {
                "beanId": "bean123",
                "suggestedGrindSetting": "5.5",
                "adjustmentDirection": "FINER",
                "reason": "Last shot was sour (24s)",
                "recommendedDose": 18.5,
                "targetExtractionTimeMin": 25,
                "targetExtractionTimeMax": 30,
                "timestamp": "${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}",
                "wasFollowed": false,
                "basedOnTaste": true,
                "confidence": "HIGH"
            }
        """.trimIndent()

        every { mockSharedPreferences.getString("grind_recommendation_bean123", null) } returns validJson

        // When
        val result = grindRecommendationPreferences.getRecommendation(beanId)

        // Then
        assertNotNull(result)
        assertEquals(beanId, result!!.beanId)
        assertEquals("5.5", result.suggestedGrindSetting)
        assertEquals("FINER", result.adjustmentDirection)
        assertEquals("Last shot was sour (24s)", result.reason)
        assertEquals(18.5, result.recommendedDose, 0.01)
        assertEquals(true, result.basedOnTaste)
        assertEquals("HIGH", result.confidence)
    }

    @Test
    fun `getRecommendation returns null and clears data when JSON is invalid`() = runTest {
        // Given
        val beanId = "bean123"
        val invalidJson = "{invalid json structure"
        every { mockSharedPreferences.getString("grind_recommendation_bean123", null) } returns invalidJson

        // When
        val result = grindRecommendationPreferences.getRecommendation(beanId)

        // Then
        assertNull(result)
        verify { mockEditor.remove("grind_recommendation_bean123") }
        verify { mockEditor.apply() }
    }

    @Test
    fun `clearRecommendation removes the correct key`() = runTest {
        // Given
        val beanId = "bean123"

        // When
        grindRecommendationPreferences.clearRecommendation(beanId)

        // Then
        verify { mockEditor.remove("grind_recommendation_bean123") }
        verify { mockEditor.apply() }
    }

    @Test
    fun `markRecommendationFollowed updates wasFollowed flag when recommendation exists`() = runTest {
        // Given
        val beanId = "bean123"
        val existingRecommendation = SerializableGrindRecommendation.create(
            beanId = beanId,
            suggestedGrindSetting = "5.5",
            adjustmentDirection = "FINER",
            reason = "Last shot was sour (24s)",
            recommendedDose = 18.5,
            basedOnTaste = true,
            confidence = "HIGH"
        )
        val existingJson = """
            {
                "beanId": "bean123",
                "suggestedGrindSetting": "5.5",
                "adjustmentDirection": "FINER",
                "reason": "Last shot was sour (24s)",
                "recommendedDose": 18.5,
                "targetExtractionTimeMin": 25,
                "targetExtractionTimeMax": 30,
                "timestamp": "${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}",
                "wasFollowed": false,
                "basedOnTaste": true,
                "confidence": "HIGH"
            }
        """.trimIndent()

        every { mockSharedPreferences.getString("grind_recommendation_bean123", null) } returns existingJson

        val jsonSlot = slot<String>()

        // When
        grindRecommendationPreferences.markRecommendationFollowed(beanId)

        // Then
        verify { mockEditor.putString(eq("grind_recommendation_bean123"), capture(jsonSlot)) }
        assertTrue("Updated JSON should have wasFollowed=true", jsonSlot.captured.contains("\"wasFollowed\":true"))
    }

    @Test
    fun `markRecommendationFollowed does nothing when no recommendation exists`() = runTest {
        // Given
        val beanId = "nonexistent"
        every { mockSharedPreferences.getString("grind_recommendation_nonexistent", null) } returns null

        // When
        grindRecommendationPreferences.markRecommendationFollowed(beanId)

        // Then
        verify(exactly = 0) { mockEditor.putString(any(), any()) }
    }

    @Test
    fun `getAllRecommendationBeanIds returns correct bean IDs`() = runTest {
        // Given
        val allKeys = mapOf(
            "grind_recommendation_bean1" to "json1",
            "grind_recommendation_bean2" to "json2",
            "other_preference" to "value",
            "grind_recommendation_bean3" to "json3"
        )
        every { mockSharedPreferences.all } returns allKeys

        // When
        val result = grindRecommendationPreferences.getAllRecommendationBeanIds()

        // Then
        assertEquals(listOf("bean1", "bean2", "bean3"), result.sorted())
    }

    @Test
    fun `getAllRecommendationBeanIds returns empty list when no recommendations exist`() = runTest {
        // Given
        val allKeys = mapOf(
            "other_preference1" to "value1",
            "other_preference2" to "value2"
        )
        every { mockSharedPreferences.all } returns allKeys

        // When
        val result = grindRecommendationPreferences.getAllRecommendationBeanIds()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `clearAllRecommendations removes only recommendation keys`() = runTest {
        // Given
        val allKeys = mapOf(
            "grind_recommendation_bean1" to "json1",
            "grind_recommendation_bean2" to "json2",
            "other_preference" to "value",
            "grind_recommendation_bean3" to "json3"
        )
        every { mockSharedPreferences.all } returns allKeys

        // When
        grindRecommendationPreferences.clearAllRecommendations()

        // Then
        verify { mockEditor.remove("grind_recommendation_bean1") }
        verify { mockEditor.remove("grind_recommendation_bean2") }
        verify { mockEditor.remove("grind_recommendation_bean3") }
        verify(exactly = 0) { mockEditor.remove("other_preference") }
        verify { mockEditor.apply() }
    }

    @Test
    fun `SerializableGrindRecommendation create method sets correct defaults`() {
        // Given
        val now = LocalDateTime.now()

        // When
        val result = SerializableGrindRecommendation.create(
            beanId = "bean123",
            suggestedGrindSetting = "5.5",
            adjustmentDirection = "FINER",
            reason = "Test reason",
            recommendedDose = 18.5,
            basedOnTaste = true,
            confidence = "HIGH"
        )

        // Then
        assertEquals("bean123", result.beanId)
        assertEquals("5.5", result.suggestedGrindSetting)
        assertEquals("FINER", result.adjustmentDirection)
        assertEquals("Test reason", result.reason)
        assertEquals(18.5, result.recommendedDose, 0.01)
        assertEquals(25, result.targetExtractionTimeMin)
        assertEquals(30, result.targetExtractionTimeMax)
        assertEquals(false, result.wasFollowed)
        assertEquals(true, result.basedOnTaste)
        assertEquals("HIGH", result.confidence)
        
        // Timestamp should be recent (within 1 minute)
        val timestamp = result.getTimestamp()
        assertTrue("Timestamp should be recent", timestamp.isAfter(now.minusMinutes(1)))
        assertTrue("Timestamp should not be in future", timestamp.isBefore(now.plusMinutes(1)))
    }

    @Test
    fun `SerializableGrindRecommendation getTargetExtractionTimeRange returns correct range`() {
        // Given
        val recommendation = SerializableGrindRecommendation.create(
            beanId = "bean123",
            suggestedGrindSetting = "5.5",
            adjustmentDirection = "FINER",
            reason = "Test reason",
            recommendedDose = 18.5,
            basedOnTaste = true,
            confidence = "HIGH"
        )

        // When
        val range = recommendation.getTargetExtractionTimeRange()

        // Then
        assertEquals(25..30, range)
        assertTrue("25 should be in range", 25 in range)
        assertTrue("30 should be in range", 30 in range)
        assertFalse("24 should not be in range", 24 in range)
        assertFalse("31 should not be in range", 31 in range)
    }

    @Test
    fun `SerializableGrindRecommendation timestamp parsing works correctly`() {
        // Given
        val originalDateTime = LocalDateTime.of(2024, 1, 15, 14, 30, 45)
        val timestampString = originalDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        // When
        val parsedDateTime = SerializableGrindRecommendation.parseTimestamp(timestampString)

        // Then
        assertEquals(originalDateTime, parsedDateTime)
    }
}
