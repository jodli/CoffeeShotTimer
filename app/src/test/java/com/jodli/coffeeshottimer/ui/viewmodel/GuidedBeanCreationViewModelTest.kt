package com.jodli.coffeeshottimer.ui.viewmodel

import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.onboarding.BeanCreationPhase
import com.jodli.coffeeshottimer.data.onboarding.BeanFormField
import com.jodli.coffeeshottimer.data.onboarding.OnboardingManager
import com.jodli.coffeeshottimer.data.onboarding.OnboardingProgress
import com.jodli.coffeeshottimer.data.repository.BeanRepository
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
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class GuidedBeanCreationViewModelTest {

    private lateinit var viewModel: GuidedBeanCreationViewModel
    private lateinit var mockBeanRepository: BeanRepository
    private lateinit var mockOnboardingManager: OnboardingManager
    private lateinit var mockDomainErrorTranslator: DomainErrorTranslator

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockBeanRepository = mockk()
        mockOnboardingManager = mockk()
        mockDomainErrorTranslator = mockk()

        // Setup default mock responses
        every { mockDomainErrorTranslator.translateError(any()) } returns "Test error"
        every { mockDomainErrorTranslator.getString(any()) } returns "Test message"

        viewModel = GuidedBeanCreationViewModel(
            beanRepository = mockBeanRepository,
            onboardingManager = mockOnboardingManager,
            domainErrorTranslator = mockDomainErrorTranslator
        )
    }

    @Test
    fun `initial state should be education phase`() = runTest {
        val uiState = viewModel.uiState.first()

        assertEquals(BeanCreationPhase.EDUCATION, uiState.currentPhase)
        assertFalse(uiState.isLoading)
        assertNull(uiState.error)
        assertNull(uiState.createdBean)
        assertEquals("", uiState.formState.name)
    }

    @Test
    fun `proceedToForm should change phase to FORM`() = runTest {
        viewModel.proceedToForm()
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals(BeanCreationPhase.FORM, uiState.currentPhase)
        assertNull(uiState.error)
    }

    @Test
    fun `returnToEducation should change phase back to EDUCATION`() = runTest {
        // First go to form
        viewModel.proceedToForm()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then return to education
        viewModel.returnToEducation()
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals(BeanCreationPhase.EDUCATION, uiState.currentPhase)
        assertNull(uiState.error)
    }

    @Test
    fun `updateField with NAME should update form state and validate`() = runTest {
        val testName = "Test Bean"

        viewModel.updateField(BeanFormField.NAME, testName)
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals(testName, uiState.formState.name)
        assertNull(uiState.formState.nameError) // Valid name should have no error
        assertNull(uiState.error)
    }

    @Test
    fun `updateField with invalid NAME should set validation error`() = runTest {
        val invalidName = "A" // Too short

        viewModel.updateField(BeanFormField.NAME, invalidName)
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals(invalidName, uiState.formState.name)
        assertEquals("Bean name must be at least 2 characters", uiState.formState.nameError)
    }

    @Test
    fun `updateField with NOTES should update notes without validation`() = runTest {
        val testNotes = "Test notes about the bean"

        viewModel.updateField(BeanFormField.NOTES, testNotes)
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals(testNotes, uiState.formState.notes)
        assertNull(uiState.error)
    }

    @Test
    fun `updateRoastDate should update form state and validate`() = runTest {
        val testDate = LocalDate.now().minusDays(7) // Valid roast date

        viewModel.updateRoastDate(testDate)
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals(testDate, uiState.formState.roastDate)
        assertNull(uiState.formState.roastDateError)
        assertNull(uiState.error)
    }

    @Test
    fun `updateRoastDate with future date should set validation error`() = runTest {
        val futureDate = LocalDate.now().plusDays(1)

        viewModel.updateRoastDate(futureDate)
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals(futureDate, uiState.formState.roastDate)
        assertEquals("Roast date cannot be in the future", uiState.formState.roastDateError)
    }

    @Test
    fun `createBean with valid form should create bean and move to success`() = runTest {
        val testBean = Bean(
            id = "test-id",
            name = "Test Bean",
            roastDate = LocalDate.now().minusDays(7),
            notes = "Test notes",
            isActive = true
        )

        // Setup mocks
        coEvery { mockBeanRepository.addBean(any()) } returns Result.success(Unit)
        coEvery { mockOnboardingManager.getOnboardingProgress() } returns OnboardingProgress()
        coEvery { mockOnboardingManager.updateOnboardingProgress(any()) } returns Unit

        // Set valid form data
        viewModel.updateField(BeanFormField.NAME, "Test Bean")
        testDispatcher.scheduler.advanceUntilIdle()

        // Create bean
        viewModel.createBean()
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals(BeanCreationPhase.SUCCESS, uiState.currentPhase)
        assertNotNull(uiState.createdBean)
        assertEquals("Test Bean", uiState.createdBean?.name)
        assertFalse(uiState.isLoading)
        assertNull(uiState.error)

        // Verify onboarding progress was updated
        coVerify {
            mockOnboardingManager.updateOnboardingProgress(
                match { it.hasCreatedFirstBean }
            )
        }
    }

    @Test
    fun `createBean with invalid form should show error`() = runTest {
        // Don't set any form data (invalid state)

        viewModel.createBean()
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals(BeanCreationPhase.EDUCATION, uiState.currentPhase) // Should stay in current phase
        assertFalse(uiState.isLoading)
        assertEquals("Please fill in all required fields correctly", uiState.error)
    }

    @Test
    fun `createBean with repository error should handle error`() = runTest {
        val exception = Exception("Database error")

        // Setup mocks
        coEvery { mockBeanRepository.addBean(any()) } returns Result.failure(exception)
        every { mockDomainErrorTranslator.translateError(exception) } returns "Database error occurred"

        // Set valid form data
        viewModel.updateField(BeanFormField.NAME, "Test Bean")
        testDispatcher.scheduler.advanceUntilIdle()

        // Create bean
        viewModel.createBean()
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals(BeanCreationPhase.EDUCATION, uiState.currentPhase) // Should stay in current phase
        assertFalse(uiState.isLoading)
        assertEquals("Database error occurred", uiState.error)
        assertFalse(uiState.formState.isSubmitting)
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
        assertFalse(uiState.isLoading)

        // Verify onboarding progress was updated
        coVerify {
            mockOnboardingManager.updateOnboardingProgress(
                match { it.hasCreatedFirstBean }
            )
        }
    }

    @Test
    fun `clearError should clear error state`() = runTest {
        // First set an error
        viewModel.createBean() // This will create an error due to invalid form
        testDispatcher.scheduler.advanceUntilIdle()

        var uiState = viewModel.uiState.first()
        assertNotNull(uiState.error)

        // Clear the error
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        uiState = viewModel.uiState.first()
        assertNull(uiState.error)
    }

    @Test
    fun `getFreshnessMessage should return appropriate message for fresh beans`() = runTest {
        val freshBean = Bean(
            id = "test-id",
            name = "Fresh Bean",
            roastDate = LocalDate.now().minusDays(2), // Very fresh
            isActive = true
        )

        every { mockDomainErrorTranslator.getString(any()) } returns "Very fresh beans!"

        val message = viewModel.getFreshnessMessage(freshBean)

        assertEquals("Very fresh beans!", message)
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

        assertEquals("Your bean is ready for brewing!", message)
    }

    @Test
    fun `form validation should handle empty name`() = runTest {
        viewModel.updateField(BeanFormField.NAME, "")
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals("Bean name is required", uiState.formState.nameError)
        assertFalse(uiState.formState.isValid)
    }

    @Test
    fun `form validation should handle name with invalid characters`() = runTest {
        viewModel.updateField(BeanFormField.NAME, "Test@Bean#")
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals("Bean name contains invalid characters", uiState.formState.nameError)
        assertFalse(uiState.formState.isValid)
    }

    @Test
    fun `form validation should handle too long name`() = runTest {
        val longName = "A".repeat(101) // Too long
        viewModel.updateField(BeanFormField.NAME, longName)
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals("Bean name cannot exceed 100 characters", uiState.formState.nameError)
        assertFalse(uiState.formState.isValid)
    }

    @Test
    fun `form validation should handle very old roast date`() = runTest {
        val oldDate = LocalDate.now().minusDays(400) // Too old

        viewModel.updateRoastDate(oldDate)
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals("Roast date cannot be more than 365 days ago", uiState.formState.roastDateError)
        assertFalse(uiState.formState.isValid)
    }
}
