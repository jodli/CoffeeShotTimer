package com.jodli.coffeeshottimer.ui.viewmodel

import android.net.Uri
import com.jodli.coffeeshottimer.domain.usecase.AddBeanUseCase
import com.jodli.coffeeshottimer.domain.usecase.UpdateBeanUseCase
import com.jodli.coffeeshottimer.domain.usecase.AddPhotoToBeanUseCase
import com.jodli.coffeeshottimer.domain.usecase.RemovePhotoFromBeanUseCase
import com.jodli.coffeeshottimer.domain.usecase.GetBeanPhotoUseCase
import com.jodli.coffeeshottimer.domain.usecase.CheckPhotoCapabilityUseCase
import com.jodli.coffeeshottimer.data.storage.PhotoCaptureManager
import com.jodli.coffeeshottimer.ui.util.StringResourceProvider
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import com.jodli.coffeeshottimer.ui.validation.ValidationStringProvider
import com.jodli.coffeeshottimer.domain.exception.DomainException
import com.jodli.coffeeshottimer.domain.model.DomainErrorCode
import com.jodli.coffeeshottimer.data.model.Bean
import io.mockk.mockk
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
    private val addPhotoToBeanUseCase: AddPhotoToBeanUseCase = mockk(relaxed = true)
    private val removePhotoFromBeanUseCase: RemovePhotoFromBeanUseCase = mockk(relaxed = true)
    private val getBeanPhotoUseCase: GetBeanPhotoUseCase = mockk(relaxed = true)
    private val checkPhotoCapabilityUseCase: CheckPhotoCapabilityUseCase = mockk(relaxed = true)
    private val photoCaptureManager: PhotoCaptureManager = mockk(relaxed = true)
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
            addPhotoToBeanUseCase,
            removePhotoFromBeanUseCase,
            getBeanPhotoUseCase,
            checkPhotoCapabilityUseCase,
            photoCaptureManager,
            stringResourceProvider,
            validationStringProvider,
            domainErrorTranslator
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setupEditMode(beanId: String, existingPhotoPath: String? = null): Bean {
        val mockBean = mockk<Bean> {
            every { id } returns beanId
            every { name } returns "Test Bean"
            every { roastDate } returns LocalDate.now()
            every { notes } returns ""
            every { isActive } returns true
            every { lastGrinderSetting } returns null
            every { photoPath } returns existingPhotoPath
        }

        coEvery { updateBeanUseCase.getBeanForEditing(beanId) } returns Result.success(mockBean)
        return mockBean
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
        assertNull(initialState.photoPath)
        assertNull(initialState.pendingPhotoUri)
        assertFalse(initialState.isPhotoLoading)
        assertNull(initialState.photoError)
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

    // Photo functionality tests

    @Test
    fun `addPhoto in create mode should store pending photo URI`() = runTest(testDispatcher) {
        val mockUri: Uri = mockk()

        viewModel.addPhoto(mockUri)

        val state = viewModel.uiState.value
        assertEquals(mockUri, state.pendingPhotoUri)
        assertNull(state.photoPath)
        assertFalse(state.isPhotoLoading)
        assertNull(state.photoError)
    }

    @Test
    fun `addPhoto in edit mode should call use case and update photo path`() = runTest(testDispatcher) {
        val mockUri: Uri = mockk()
        val beanId = "test-bean-id"
        val photoPath = "/path/to/photo.jpg"
        // Setup edit mode
        setupEditMode(beanId)
        viewModel.initializeForEdit(beanId)

        // Mock successful photo addition
        coEvery { addPhotoToBeanUseCase.execute(beanId, mockUri) } returns Result.success(photoPath)

        viewModel.addPhoto(mockUri)

        val state = viewModel.uiState.value
        assertEquals(photoPath, state.photoPath)
        assertNull(state.pendingPhotoUri)
        assertFalse(state.isPhotoLoading)
        assertNull(state.photoError)

        coVerify { addPhotoToBeanUseCase.execute(beanId, mockUri) }
    }

    @Test
    fun `addPhoto in edit mode should handle failure`() = runTest(testDispatcher) {
        val mockUri: Uri = mockk()
        val beanId = "test-bean-id"
        val errorMessage = "Failed to save photo"
        val exception = DomainException(DomainErrorCode.PHOTO_SAVE_FAILED, errorMessage)

        // Setup edit mode
        setupEditMode(beanId)
        viewModel.initializeForEdit(beanId)

        // Mock failed photo addition
        coEvery { addPhotoToBeanUseCase.execute(beanId, mockUri) } returns Result.failure(exception)
        every { domainErrorTranslator.translateResultError(any<Result<String>>()) } returns errorMessage

        viewModel.addPhoto(mockUri)

        val state = viewModel.uiState.value
        assertNull(state.photoPath)
        assertFalse(state.isPhotoLoading)
        assertEquals(errorMessage, state.photoError)

        coVerify { addPhotoToBeanUseCase.execute(beanId, mockUri) }
    }

    @Test
    fun `replacePhoto should call addPhoto`() = runTest(testDispatcher) {
        val mockUri: Uri = mockk()

        viewModel.replacePhoto(mockUri)

        // In create mode, should store as pending
        assertEquals(mockUri, viewModel.uiState.value.pendingPhotoUri)
    }

    @Test
    fun `removePhoto in create mode should clear pending photo URI`() = runTest(testDispatcher) {
        val mockUri: Uri = mockk()

        // First add a photo
        viewModel.addPhoto(mockUri)
        assertEquals(mockUri, viewModel.uiState.value.pendingPhotoUri)

        // Then remove it
        viewModel.removePhoto()

        val state = viewModel.uiState.value
        assertNull(state.pendingPhotoUri)
        assertNull(state.photoPath)
        assertFalse(state.isPhotoLoading)
        assertNull(state.photoError)
    }

    @Test
    fun `removePhoto in edit mode should call use case and clear photo path`() = runTest(testDispatcher) {
        val beanId = "test-bean-id"

        // Setup edit mode
        setupEditMode(beanId)
        viewModel.initializeForEdit(beanId)

        // Mock successful photo removal
        coEvery { removePhotoFromBeanUseCase.execute(beanId) } returns Result.success(Unit)

        viewModel.removePhoto()

        val state = viewModel.uiState.value
        assertNull(state.photoPath)
        assertNull(state.pendingPhotoUri)
        assertFalse(state.isPhotoLoading)
        assertNull(state.photoError)

        coVerify { removePhotoFromBeanUseCase.execute(beanId) }
    }

    @Test
    fun `removePhoto in edit mode should handle failure`() = runTest(testDispatcher) {
        val beanId = "test-bean-id"
        val errorMessage = "Failed to remove photo"
        val exception = DomainException(DomainErrorCode.PHOTO_DELETE_FAILED, errorMessage)

        // Setup edit mode
        setupEditMode(beanId)
        viewModel.initializeForEdit(beanId)

        // Mock failed photo removal
        coEvery { removePhotoFromBeanUseCase.execute(beanId) } returns Result.failure(exception)
        every { domainErrorTranslator.translateResultError(any<Result<Unit>>()) } returns errorMessage

        viewModel.removePhoto()

        val state = viewModel.uiState.value
        assertFalse(state.isPhotoLoading)
        assertEquals(errorMessage, state.photoError)

        coVerify { removePhotoFromBeanUseCase.execute(beanId) }
    }

    @Test
    fun `clearPhotoError should clear photo error`() = runTest(testDispatcher) {
        // Simulate an error state
        val mockUri: Uri = mockk()
        val beanId = "test-bean-id"
        val exception = DomainException(DomainErrorCode.PHOTO_SAVE_FAILED, "Error")

        setupEditMode(beanId)
        viewModel.initializeForEdit(beanId)
        coEvery { addPhotoToBeanUseCase.execute(beanId, mockUri) } returns Result.failure(exception)
        every { domainErrorTranslator.translateResultError(any<Result<String>>()) } returns "Error"

        viewModel.addPhoto(mockUri)
        assertNotNull(viewModel.uiState.value.photoError)

        // Clear the error
        viewModel.clearPhotoError()

        assertNull(viewModel.uiState.value.photoError)
    }

    @Test
    fun `hasPhoto should return true when photo path exists`() = runTest(testDispatcher) {
        // Simulate having a photo path
        val beanId = "test-bean-id"
        setupEditMode(beanId)
        viewModel.initializeForEdit(beanId)

        // Manually set photo path for testing
        val mockUri: Uri = mockk()
        coEvery { addPhotoToBeanUseCase.execute(beanId, mockUri) } returns Result.success("/path/to/photo.jpg")

        viewModel.addPhoto(mockUri)

        assertTrue(viewModel.hasPhoto())
    }

    @Test
    fun `hasPhoto should return true when pending photo URI exists`() = runTest(testDispatcher) {
        val mockUri: Uri = mockk()

        viewModel.addPhoto(mockUri)

        assertTrue(viewModel.hasPhoto())
    }

    @Test
    fun `hasPhoto should return false when no photo exists`() = runTest(testDispatcher) {
        assertFalse(viewModel.hasPhoto())
    }

    @Test
    fun `getCurrentPhotoUri should return pending photo URI in create mode`() = runTest(testDispatcher) {
        val mockUri: Uri = mockk()

        viewModel.addPhoto(mockUri)

        assertEquals(mockUri, viewModel.getCurrentPhotoUri())
    }

    @Test
    fun `getCurrentPhotoUri should return null when no pending photo`() = runTest(testDispatcher) {
        assertNull(viewModel.getCurrentPhotoUri())
    }

    @Test
    fun `hasUnsavedChanges should detect pending photo changes`() = runTest(testDispatcher) {
        assertFalse(viewModel.hasUnsavedChanges())

        val mockUri: Uri = mockk()
        viewModel.addPhoto(mockUri)

        assertTrue(viewModel.hasUnsavedChanges())
    }

    @Test
    fun `saveBean in create mode should handle pending photo`() = runTest(testDispatcher) {
        val mockUri: Uri = mockk()
        val beanId = "new-bean-id"
        val photoPath = "/path/to/photo.jpg"
        val mockBean = mockk<Bean> {
            every { id } returns beanId
        }

        // Setup form with valid data and pending photo
        viewModel.updateName("Test Bean")
        viewModel.addPhoto(mockUri)

        // Mock successful bean creation and photo addition
        coEvery { addBeanUseCase.execute(any(), any(), any(), any(), any()) } returns Result.success(mockBean)
        coEvery { addPhotoToBeanUseCase.execute(beanId, mockUri) } returns Result.success(photoPath)
        coEvery { addBeanUseCase.isBeanNameAvailable(any()) } returns Result.success(true)

        viewModel.saveBean()

        val state = viewModel.uiState.value
        assertTrue(state.saveSuccess)
        assertFalse(state.isSaving)
        assertNull(state.error)

        coVerify { addBeanUseCase.execute(any(), any(), any(), any(), any()) }
        coVerify { addPhotoToBeanUseCase.execute(beanId, mockUri) }
    }

    @Test
    fun `saveBean should handle photo save failure gracefully`() = runTest(testDispatcher) {
        val mockUri: Uri = mockk()
        val beanId = "new-bean-id"
        val photoError = "Photo save failed"
        val exception = DomainException(DomainErrorCode.PHOTO_SAVE_FAILED, photoError)
        val mockBean = mockk<Bean> {
            every { id } returns beanId
        }

        // Setup form with valid data and pending photo
        viewModel.updateName("Test Bean")
        viewModel.addPhoto(mockUri)

        // Mock successful bean creation but failed photo addition
        coEvery { addBeanUseCase.execute(any(), any(), any(), any(), any()) } returns Result.success(mockBean)
        coEvery { addPhotoToBeanUseCase.execute(beanId, mockUri) } returns Result.failure(exception)
        coEvery { addBeanUseCase.isBeanNameAvailable(any()) } returns Result.success(true)
        every { domainErrorTranslator.translateResultError(any<Result<String>>()) } returns photoError

        viewModel.saveBean()

        val state = viewModel.uiState.value
        assertTrue(state.saveSuccess) // Bean was saved successfully
        assertFalse(state.isSaving)
        assertEquals(photoError, state.photoError) // But photo error is shown
        assertNull(state.error) // Main error is null

        coVerify { addBeanUseCase.execute(any(), any(), any(), any(), any()) }
        coVerify { addPhotoToBeanUseCase.execute(beanId, mockUri) }
    }

    @Test
    fun `saveBean in onboarding mode should store savedBean in state`() = runTest(testDispatcher) {
        val beanId = "new-bean-id"
        val mockBean = mockk<Bean>(relaxed = true) {
            every { id } returns beanId
            every { name } returns "Test Bean"
            every { roastDate } returns java.time.LocalDate.now()
            every { notes } returns ""
            every { isActive } returns true
        }

        // Setup form with valid data
        viewModel.updateName("Test Bean")

        // Mock successful bean creation
        coEvery { addBeanUseCase.execute(any(), any(), any(), any(), any()) } returns Result.success(mockBean)
        coEvery { addBeanUseCase.isBeanNameAvailable(any()) } returns Result.success(true)

        viewModel.saveBean()

        val state = viewModel.uiState.value
        assertTrue(state.saveSuccess)
        assertFalse(state.isSaving)
        assertNull(state.error)
        assertEquals(mockBean, state.savedBean) // Should store the created bean

        coVerify { addBeanUseCase.execute(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `saveBean with pending photo in onboarding mode should handle photo success`() = runTest(testDispatcher) {
        val mockUri: Uri = mockk()
        val beanId = "new-bean-id"
        val photoPath = "/path/to/photo.jpg"
        val mockBean = mockk<Bean>(relaxed = true) {
            every { id } returns beanId
            every { name } returns "Test Bean"
            every { roastDate } returns java.time.LocalDate.now()
            every { notes } returns ""
            every { isActive } returns true
        }

        // Setup form with valid data and pending photo
        viewModel.updateName("Test Bean")
        viewModel.addPhoto(mockUri)

        // Mock successful bean creation and photo addition
        coEvery { addBeanUseCase.execute(any(), any(), any(), any(), any()) } returns Result.success(mockBean)
        coEvery { addPhotoToBeanUseCase.execute(beanId, mockUri) } returns Result.success(photoPath)
        coEvery { addBeanUseCase.isBeanNameAvailable(any()) } returns Result.success(true)

        viewModel.saveBean()

        val state = viewModel.uiState.value
        assertTrue(state.saveSuccess)
        assertFalse(state.isSaving)
        assertNull(state.error)
        assertEquals(mockBean, state.savedBean) // Should store the created bean

        coVerify { addBeanUseCase.execute(any(), any(), any(), any(), any()) }
        coVerify { addPhotoToBeanUseCase.execute(beanId, mockUri) }
    }

    @Test
    fun `resetSaveSuccess should clear savedBean`() = runTest(testDispatcher) {
        val mockBean = mockk<Bean>()

        // First set saveSuccess and savedBean by triggering a successful save
        viewModel.updateName("Test Bean")
        coEvery { addBeanUseCase.execute(any(), any(), any(), any(), any()) } returns Result.success(mockBean)
        coEvery { addBeanUseCase.isBeanNameAvailable(any()) } returns Result.success(true)

        viewModel.saveBean()

        var state = viewModel.uiState.value
        assertTrue(state.saveSuccess)
        assertEquals(mockBean, state.savedBean)

        // Reset save success
        viewModel.resetSaveSuccess()

        state = viewModel.uiState.value
        assertFalse(state.saveSuccess)
        assertNull(state.savedBean)
    }
}