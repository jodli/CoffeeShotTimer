package com.jodli.coffeeshottimer.data.repository

import com.jodli.coffeeshottimer.data.dao.BeanDao
import com.jodli.coffeeshottimer.data.dao.ShotDao
import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import com.jodli.coffeeshottimer.domain.model.TasteSecondary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ShotRepository taste feedback methods.
 * Tests taste-related functionality including recording, querying, and statistics.
 */
class ShotRepositoryTasteTest {

    private lateinit var shotDao: ShotDao
    private lateinit var beanDao: BeanDao
    private lateinit var repository: ShotRepository

    @Before
    fun setup() {
        shotDao = mockk()
        beanDao = mockk()
        repository = ShotRepository(shotDao, beanDao)
    }

    @Test
    fun `updateTasteFeedback with valid data calls DAO and succeeds`() = runTest {
        // Given
        val shotId = "test-shot-id"
        val tastePrimary = TastePrimary.PERFECT
        val tasteSecondary = TasteSecondary.STRONG
        val mockShot = Shot(
            id = shotId,
            beanId = "test-bean",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = 28,
            grinderSetting = "15"
        )

        coEvery { shotDao.getShotById(shotId) } returns mockShot
        coEvery { shotDao.updateTasteFeedback(shotId, tastePrimary, tasteSecondary) } returns Unit

        // When
        val result = repository.updateTasteFeedback(shotId, tastePrimary, tasteSecondary)

        // Then
        assertTrue(result.isSuccess)
        coVerify { shotDao.getShotById(shotId) }
        coVerify { shotDao.updateTasteFeedback(shotId, tastePrimary, tasteSecondary) }
    }

    @Test
    fun `updateTasteFeedback with primary only calls DAO correctly`() = runTest {
        // Given
        val shotId = "test-shot-id"
        val tastePrimary = TastePrimary.SOUR
        val mockShot = Shot(
            id = shotId,
            beanId = "test-bean",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = 22,
            grinderSetting = "15"
        )

        coEvery { shotDao.getShotById(shotId) } returns mockShot
        coEvery { shotDao.updateTasteFeedback(shotId, tastePrimary, null) } returns Unit

        // When
        val result = repository.updateTasteFeedback(shotId, tastePrimary, null)

        // Then
        assertTrue(result.isSuccess)
        coVerify { shotDao.updateTasteFeedback(shotId, tastePrimary, null) }
    }

    @Test
    fun `updateTasteFeedback with blank shotId returns validation error`() = runTest {
        // Given
        val shotId = ""
        val tastePrimary = TastePrimary.PERFECT

        // When
        val result = repository.updateTasteFeedback(shotId, tastePrimary)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as? RepositoryException.ValidationError
        assertNotNull(exception)
        assertEquals("Shot ID cannot be empty", exception!!.message)
    }

    @Test
    fun `updateTasteFeedback with non-existent shot returns not found error`() = runTest {
        // Given
        val shotId = "non-existent-shot"
        val tastePrimary = TastePrimary.BITTER

        coEvery { shotDao.getShotById(shotId) } returns null

        // When
        val result = repository.updateTasteFeedback(shotId, tastePrimary)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as? RepositoryException.NotFoundError
        assertNotNull(exception)
        assertEquals("Shot not found", exception!!.message)
    }

    @Test
    fun `updateTasteFeedback with database error returns database error`() = runTest {
        // Given
        val shotId = "test-shot-id"
        val tastePrimary = TastePrimary.PERFECT
        val databaseException = RuntimeException("Database connection lost")

        coEvery { shotDao.getShotById(shotId) } throws databaseException

        // When
        val result = repository.updateTasteFeedback(shotId, tastePrimary)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as? RepositoryException.DatabaseError
        assertNotNull(exception)
        assertEquals("Failed to update taste feedback", exception!!.message)
        assertEquals(databaseException, exception.cause)
    }

    @Test
    fun `updateTasteFeedback with all taste combinations works`() = runTest {
        // Test all combinations of primary and secondary tastes
        val testCases = listOf(
            Pair(TastePrimary.SOUR, null),
            Pair(TastePrimary.SOUR, TasteSecondary.WEAK),
            Pair(TastePrimary.SOUR, TasteSecondary.STRONG),
            Pair(TastePrimary.PERFECT, null),
            Pair(TastePrimary.PERFECT, TasteSecondary.WEAK),
            Pair(TastePrimary.PERFECT, TasteSecondary.STRONG),
            Pair(TastePrimary.BITTER, null),
            Pair(TastePrimary.BITTER, TasteSecondary.WEAK),
            Pair(TastePrimary.BITTER, TasteSecondary.STRONG)
        )

        testCases.forEachIndexed { index, (primary, secondary) ->
            // Given
            val shotId = "shot-$index"
            val mockShot = Shot(
                id = shotId,
                beanId = "test-bean",
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                extractionTimeSeconds = 28,
                grinderSetting = "15"
            )

            coEvery { shotDao.getShotById(shotId) } returns mockShot
            coEvery { shotDao.updateTasteFeedback(shotId, primary, secondary) } returns Unit

            // When
            val result = repository.updateTasteFeedback(shotId, primary, secondary)

            // Then
            assertTrue("Combination $primary + $secondary should succeed", result.isSuccess)
            coVerify { shotDao.updateTasteFeedback(shotId, primary, secondary) }
        }
    }

    @Test
    fun `clear taste feedback works correctly`() = runTest {
        // Given
        val shotId = "test-shot-id"
        val mockShot = Shot(
            id = shotId,
            beanId = "test-bean",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = 28,
            grinderSetting = "15",
            tastePrimary = TastePrimary.PERFECT,
            tasteSecondary = TasteSecondary.STRONG
        )

        coEvery { shotDao.getShotById(shotId) } returns mockShot
        coEvery { shotDao.updateTasteFeedback(shotId, null, null) } returns Unit

        // When
        val result = repository.updateTasteFeedback(shotId, null, null)

        // Then
        assertTrue(result.isSuccess)
        coVerify { shotDao.updateTasteFeedback(shotId, null, null) }
    }
}
