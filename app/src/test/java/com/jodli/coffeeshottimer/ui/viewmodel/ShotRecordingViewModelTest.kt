package com.jodli.coffeeshottimer.ui.viewmodel

import android.content.Context
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.data.repository.ShotRepository
import com.jodli.coffeeshottimer.domain.usecase.RecordShotUseCase
import com.jodli.coffeeshottimer.domain.usecase.GetShotDetailsUseCase
import com.jodli.coffeeshottimer.ui.util.StringResourceProvider
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import com.jodli.coffeeshottimer.ui.validation.ValidationStringProvider
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertNotNull

/**
 * Test class for ShotRecordingViewModel to verify Hilt injection works correctly.
 */
@ExperimentalCoroutinesApi
class ShotRecordingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var recordShotUseCase: RecordShotUseCase
    private lateinit var getShotDetailsUseCase: GetShotDetailsUseCase
    private lateinit var beanRepository: BeanRepository
    private lateinit var shotRepository: ShotRepository
    private lateinit var stringResourceProvider: StringResourceProvider
    private lateinit var domainErrorTranslator: DomainErrorTranslator
    private lateinit var validationStringProvider: ValidationStringProvider
    private lateinit var context: Context
    private lateinit var viewModel: ShotRecordingViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Create mock dependencies
        recordShotUseCase = mockk<RecordShotUseCase>(relaxed = true)
        getShotDetailsUseCase = mockk<GetShotDetailsUseCase>(relaxed = true)
        beanRepository = mockk<BeanRepository>(relaxed = true)
        shotRepository = mockk<ShotRepository>(relaxed = true)
        context = mockk<Context>(relaxed = true)
        stringResourceProvider = mockk<StringResourceProvider>(relaxed = true)
        domainErrorTranslator = mockk<DomainErrorTranslator>(relaxed = true)
        validationStringProvider = mockk<ValidationStringProvider>(relaxed = true)

        // Create ViewModel with injected dependencies
        viewModel = ShotRecordingViewModel(recordShotUseCase, getShotDetailsUseCase, beanRepository, shotRepository, domainErrorTranslator, stringResourceProvider, validationStringProvider, context)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `viewModel should be created with injected dependencies`() {
        // Verify that the ViewModel was created successfully with dependencies
        assertNotNull(viewModel)
        assertNotNull(viewModel.activeBeans)
        assertNotNull(viewModel.isLoading)
        assertNotNull(viewModel.errorMessage)
        assertNotNull(viewModel.successMessage)
        assertNotNull(viewModel.isDraftSaved)
    }
    
    @Test
    fun `viewModel should have draft functionality`() {
        // Verify that draft-related state flows are available
        assertNotNull(viewModel.isDraftSaved)
        assertNotNull(viewModel.lastDraftSaveTime)
        
        // Verify that manual draft save method is available
        viewModel.saveDraftManually()
        
        // Verify that success message functionality is available
        viewModel.clearSuccessMessage()
    }
    
    @Test
    fun `viewModel should have bean-specific suggested values`() {
        // Verify that suggested values state flows are available
        assertNotNull(viewModel.suggestedGrinderSetting)
        assertNotNull(viewModel.suggestedCoffeeWeightIn)
        assertNotNull(viewModel.suggestedCoffeeWeightOut)
        
        // Verify that previous successful settings are available
        assertNotNull(viewModel.previousSuccessfulSettings)
    }
}