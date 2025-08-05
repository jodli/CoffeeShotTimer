package com.jodli.coffeeshottimer.ui.viewmodel

import com.jodli.coffeeshottimer.domain.usecase.AddBeanUseCase
import com.jodli.coffeeshottimer.domain.usecase.UpdateBeanUseCase
import com.jodli.coffeeshottimer.ui.util.StringResourceProvider
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import com.jodli.coffeeshottimer.ui.validation.ValidationStringProvider
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import java.time.LocalDate

/**
 * Test class for AddEditBeanViewModel.
 * Tests form validation, bean creation, and editing functionality.
 */
@ExperimentalCoroutinesApi
class AddEditBeanViewModelTest {

    private val addBeanUseCase: AddBeanUseCase = mockk(relaxed = true)
    private val updateBeanUseCase: UpdateBeanUseCase = mockk(relaxed = true)
    private val stringResourceProvider: StringResourceProvider = mockk(relaxed = true)
    private val validationStringProvider: ValidationStringProvider = mockk(relaxed = true)
    private val domainErrorTranslator: DomainErrorTranslator = mockk(relaxed = true)

    private lateinit var viewModel: AddEditBeanViewModel
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup mock responses for validation string provider
        io.mockk.every { validationStringProvider.getGrinderSettingMaximumLengthError(50) } returns "Grinder setting cannot exceed 50 characters"
        
        viewModel = AddEditBeanViewModel(
            addBeanUseCase,
            updateBeanUseCase,
            stringResourceProvider,
            validationStringProvider,
            domainErrorTranslator
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be correct`() = runTest(testDispatcher) {
        val initialState = viewModel.uiState.value
        
        assertEquals("", initialState.name)
        assertEquals(LocalDate.now(), initialState.roastDate)
        assertEquals("", initialState.notes)
        assertTrue(initialState.isActive)
        assertEquals("", initialState.lastGrinderSetting)
        assertNull(initialState.nameError)
        assertNull(initialState.roastDateError)
        assertNull(initialState.notesError)
        assertNull(initialState.grinderSettingError)
        assertFalse(initialState.isLoading)
        assertFalse(initialState.isSaving)
        assertFalse(initialState.saveSuccess)
        assertNull(initialState.error)
        assertFalse(initialState.isEditMode)
    }

    @Test
    fun `updateName should update name and clear error`() = runTest(testDispatcher) {
        val testName = "Test Bean"
        
        viewModel.updateName(testName)
        
        assertEquals(testName, viewModel.uiState.value.name)
        assertNull(viewModel.uiState.value.nameError)
    }

    @Test
    fun `updateRoastDate should update date and clear error`() = runTest(testDispatcher) {
        val testDate = LocalDate.now().minusDays(5)
        
        viewModel.updateRoastDate(testDate)
        
        assertEquals(testDate, viewModel.uiState.value.roastDate)
        assertNull(viewModel.uiState.value.roastDateError)
    }

    @Test
    fun `updateNotes should update notes and clear error`() = runTest(testDispatcher) {
        val testNotes = "Test notes"
        
        viewModel.updateNotes(testNotes)
        
        assertEquals(testNotes, viewModel.uiState.value.notes)
        assertNull(viewModel.uiState.value.notesError)
    }

    @Test
    fun `updateIsActive should update active status`() = runTest(testDispatcher) {
        viewModel.updateIsActive(false)
        
        assertFalse(viewModel.uiState.value.isActive)
        
        viewModel.updateIsActive(true)
        
        assertTrue(viewModel.uiState.value.isActive)
    }

    @Test
    fun `updateLastGrinderSetting should update grinder setting`() = runTest(testDispatcher) {
        val testSetting = "15"
        
        viewModel.updateLastGrinderSetting(testSetting)
        
        assertEquals(testSetting, viewModel.uiState.value.lastGrinderSetting)
    }

    @Test
    fun `updateAndValidateGrinderSetting should validate correctly`() = runTest(testDispatcher) {
        // Test valid grinder setting
        viewModel.updateAndValidateGrinderSetting("15")
        assertEquals("15", viewModel.uiState.value.lastGrinderSetting)
        assertNull(viewModel.uiState.value.grinderSettingError)
        
        // Test too long grinder setting
        val longSetting = "a".repeat(51)
        viewModel.updateAndValidateGrinderSetting(longSetting)
        assertEquals(longSetting, viewModel.uiState.value.lastGrinderSetting)
        assertNotNull(viewModel.uiState.value.grinderSettingError)
        assertTrue(viewModel.uiState.value.grinderSettingError!!.contains("cannot exceed 50 characters"))
    }

    @Test
    fun `hasUnsavedChanges should detect changes correctly`() = runTest(testDispatcher) {
        // Initially no changes
        assertFalse(viewModel.hasUnsavedChanges())
        
        // After adding name
        viewModel.updateName("Test Bean")
        assertTrue(viewModel.hasUnsavedChanges())
        
        // After resetting
        viewModel.resetForm()
        assertFalse(viewModel.hasUnsavedChanges())
    }

    @Test
    fun `resetForm should reset to initial state`() = runTest(testDispatcher) {
        // Make some changes
        viewModel.updateName("Test Bean")
        viewModel.updateNotes("Test notes")
        viewModel.updateIsActive(false)
        
        // Reset form
        viewModel.resetForm()
        
        val state = viewModel.uiState.value
        assertEquals("", state.name)
        assertEquals("", state.notes)
        assertTrue(state.isActive)
        assertFalse(state.isEditMode)
    }

    @Test
    fun `clearError should reset error state`() = runTest(testDispatcher) {
        viewModel.clearError()
        
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `resetSaveSuccess should reset save success state`() = runTest(testDispatcher) {
        viewModel.resetSaveSuccess()
        
        assertFalse(viewModel.uiState.value.saveSuccess)
    }
}