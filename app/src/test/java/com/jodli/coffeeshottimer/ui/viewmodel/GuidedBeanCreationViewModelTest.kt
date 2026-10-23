package com.jodli.coffeeshottimer.ui.viewmodel

import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.onboarding.BeanCreationPhase
import com.jodli.coffeeshottimer.data.onboarding.OnboardingManager
import com.jodli.coffeeshottimer.data.onboarding.OnboardingProgress
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class GuidedBeanCreationViewModelTest {

    private lateinit var viewModel: GuidedBeanCreationViewModel
    private lateinit var mockOnboardingManager: OnboardingManager
    private lateinit var mockDomainErrorTranslator: DomainErrorTranslator

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockOnboardingManager = mockk()
        mockDomainErrorTranslator = mockk()

        // Setup default mock responses
        every { mockDomainErrorTranslator.getString(any()) } returns "Test message"

        viewModel = GuidedBeanCreationViewModel(
            onboardingManager = mockOnboardingManager,
            domainErrorTranslator = mockDomainErrorTranslator
        )
    }

    @Test
    fun `initial state should be education phase`() = runTest {
        val uiState = viewModel.uiState.first()

        assertEquals(BeanCreationPhase.EDUCATION, uiState.currentPhase)
        assertNull(uiState.createdBean)
    }

    @Test
    fun `proceedToForm should change phase to FORM`() = runTest {
        viewModel.proceedToForm()

        val uiState = viewModel.uiState.first()
        assertEquals(BeanCreationPhase.FORM, uiState.currentPhase)
    }

    @Test
    fun `returnToEducation should change phase back to EDUCATION`() = runTest {
        // First go to form
        viewModel.proceedToForm()

        // Then return to education
        viewModel.returnToEducation()

        val uiState = viewModel.uiState.first()
        assertEquals(BeanCreationPhase.EDUCATION, uiState.currentPhase)
    }

    @Test
    fun `onBeanCreated should update progress and move to success`() = runTest {
        val testBean = Bean(
            id = "test-id",
            name = "Test Bean",
            roastDate = LocalDate.now().minusDays(7),
            notes = "Test notes",
            isActive = true
        )

        // Setup mocks
        coEvery { mockOnboardingManager.getOnboardingProgress() } returns OnboardingProgress()
        coEvery { mockOnboardingManager.updateOnboardingProgress(any()) } returns Unit

        viewModel.onBeanCreated(testBean)
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals(BeanCreationPhase.SUCCESS, uiState.currentPhase)
        assertEquals(testBean, uiState.createdBean)

        // Verify onboarding progress was updated
        coVerify {
            mockOnboardingManager.updateOnboardingProgress(
                match { it.hasCreatedFirstBean }
            )
        }
    }

    @Test
    fun `getFreshnessMessage should return appropriate message for fresh beans`() = runTest {
        val freshBean = Bean(
            id = "test-id",
            name = "Fresh Bean",
            roastDate = LocalDate.now().minusDays(2),
            isActive = true
        )

        val message = viewModel.getFreshnessMessage(freshBean)

        assertEquals("Test message", message)
    }

    @Test
    fun `getFreshnessMessage should return neutral message for today roast`() = runTest {
        val todayBean = Bean(
            id = "test-id",
            name = "Today Bean",
            roastDate = LocalDate.now(), // Today
            isActive = true
        )

        val message = viewModel.getFreshnessMessage(todayBean)

        assertEquals("Test message", message)
    }
}
