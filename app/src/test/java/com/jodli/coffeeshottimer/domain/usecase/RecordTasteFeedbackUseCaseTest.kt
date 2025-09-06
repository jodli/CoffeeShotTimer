package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.repository.RepositoryException
import com.jodli.coffeeshottimer.data.repository.ShotRepository
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import com.jodli.coffeeshottimer.domain.model.TasteSecondary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for RecordTasteFeedbackUseCase.
 * Tests the taste feedback recording functionality and error handling.
 */
class RecordTasteFeedbackUseCaseTest {

    private lateinit var shotRepository: ShotRepository
    private lateinit var useCase: RecordTasteFeedbackUseCase

    @Before
    fun setup() {
        shotRepository = mockk()
        useCase = RecordTasteFeedbackUseCase(shotRepository)
    }

    @Test
    fun `invoke with valid data calls repository updateTasteFeedback`() = runTest {
        // Given
        val shotId = "test-shot-id"
        val tastePrimary = TastePrimary.PERFECT
        val tasteSecondary = TasteSecondary.STRONG
        coEvery { shotRepository.updateTasteFeedback(shotId, tastePrimary, tasteSecondary) } returns Result.success(Unit)

        // When
        val result = useCase(shotId, tastePrimary, tasteSecondary)

        // Then
        assertTrue(result.isSuccess)
        coVerify { shotRepository.updateTasteFeedback(shotId, tastePrimary, tasteSecondary) }
    }

    @Test
    fun `invoke with primary taste only calls repository without secondary`() = runTest {
        // Given
        val shotId = "test-shot-id"
        val tastePrimary = TastePrimary.SOUR
        coEvery { shotRepository.updateTasteFeedback(shotId, tastePrimary, null) } returns Result.success(Unit)

        // When
        val result = useCase(shotId, tastePrimary)

        // Then
        assertTrue(result.isSuccess)
        coVerify { shotRepository.updateTasteFeedback(shotId, tastePrimary, null) }
    }

    @Test
    fun `invoke returns repository success result`() = runTest {
        // Given
        val shotId = "test-shot-id"
        val tastePrimary = TastePrimary.BITTER
        coEvery { shotRepository.updateTasteFeedback(shotId, tastePrimary, null) } returns Result.success(Unit)

        // When
        val result = useCase(shotId, tastePrimary)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
    }

    @Test
    fun `invoke returns repository failure result`() = runTest {
        // Given
        val shotId = "test-shot-id"
        val tastePrimary = TastePrimary.PERFECT
        val error = RepositoryException.NotFoundError("Shot not found")
        coEvery { shotRepository.updateTasteFeedback(shotId, tastePrimary, null) } returns Result.failure(error)

        // When
        val result = useCase(shotId, tastePrimary)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun `invoke with all taste combinations works correctly`() = runTest {
        // Test all combinations of primary and secondary tastes
        val testCases = listOf(
            Triple(TastePrimary.SOUR, null, "Sour only"),
            Triple(TastePrimary.SOUR, TasteSecondary.WEAK, "Sour + Weak"),
            Triple(TastePrimary.SOUR, TasteSecondary.STRONG, "Sour + Strong"),
            Triple(TastePrimary.PERFECT, null, "Perfect only"),
            Triple(TastePrimary.PERFECT, TasteSecondary.WEAK, "Perfect + Weak"),
            Triple(TastePrimary.PERFECT, TasteSecondary.STRONG, "Perfect + Strong"),
            Triple(TastePrimary.BITTER, null, "Bitter only"),
            Triple(TastePrimary.BITTER, TasteSecondary.WEAK, "Bitter + Weak"),
            Triple(TastePrimary.BITTER, TasteSecondary.STRONG, "Bitter + Strong")
        )

        testCases.forEach { (primary, secondary, description) ->
            // Given
            val shotId = "test-shot-${testCases.indexOf(Triple(primary, secondary, description))}"
            coEvery { shotRepository.updateTasteFeedback(shotId, primary, secondary) } returns Result.success(Unit)

            // When
            val result = if (secondary != null) {
                useCase(shotId, primary, secondary)
            } else {
                useCase(shotId, primary)
            }

            // Then
            assertTrue("$description should succeed", result.isSuccess)
            coVerify { shotRepository.updateTasteFeedback(shotId, primary, secondary) }
        }
    }

    @Test
    fun `clearTasteFeedback calls repository with null values`() = runTest {
        // Given
        val shotId = "test-shot-id"
        coEvery { shotRepository.updateTasteFeedback(shotId, null, null) } returns Result.success(Unit)

        // When
        val result = useCase.clearTasteFeedback(shotId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { shotRepository.updateTasteFeedback(shotId, null, null) }
    }

    @Test
    fun `clearTasteFeedback returns repository success result`() = runTest {
        // Given
        val shotId = "test-shot-id"
        coEvery { shotRepository.updateTasteFeedback(shotId, null, null) } returns Result.success(Unit)

        // When
        val result = useCase.clearTasteFeedback(shotId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
    }

    @Test
    fun `clearTasteFeedback returns repository failure result`() = runTest {
        // Given
        val shotId = "test-shot-id"
        val error = RepositoryException.DatabaseError("Database error", RuntimeException())
        coEvery { shotRepository.updateTasteFeedback(shotId, null, null) } returns Result.failure(error)

        // When
        val result = useCase.clearTasteFeedback(shotId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun `invoke with various repository errors propagates correctly`() = runTest {
        val shotId = "test-shot-id"
        val tastePrimary = TastePrimary.PERFECT

        val errorCases = listOf(
            RepositoryException.ValidationError("Invalid shot ID"),
            RepositoryException.NotFoundError("Shot not found"),
            RepositoryException.DatabaseError("Database connection failed", RuntimeException()),
            RuntimeException("Unexpected error")
        )

        errorCases.forEach { error ->
            // Given
            coEvery { shotRepository.updateTasteFeedback(shotId, tastePrimary, null) } returns Result.failure(error)

            // When
            val result = useCase(shotId, tastePrimary)

            // Then
            assertTrue("Error should be propagated: ${error.javaClass.simpleName}", result.isFailure)
            assertEquals("Error should match", error, result.exceptionOrNull())
        }
    }

    @Test
    fun `use case is thread safe with concurrent calls`() = runTest {
        // Given
        val shotIds = (1..10).map { "shot-$it" }
        coEvery { shotRepository.updateTasteFeedback(any(), any(), any()) } returns Result.success(Unit)

        // When - make concurrent calls
        val results = shotIds.map { shotId ->
            useCase(shotId, TastePrimary.PERFECT, TasteSecondary.STRONG)
        }

        // Then
        results.forEach { result ->
            assertTrue("All concurrent calls should succeed", result.isSuccess)
        }
        
        // Verify all repository calls were made
        shotIds.forEach { shotId ->
            coVerify { shotRepository.updateTasteFeedback(shotId, TastePrimary.PERFECT, TasteSecondary.STRONG) }
        }
    }
}
