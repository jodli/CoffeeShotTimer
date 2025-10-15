package com.jodli.coffeeshottimer.ui.viewmodel

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.data.repository.ShotRepository
import com.jodli.coffeeshottimer.domain.usecase.CalculateGrindAdjustmentUseCase
import com.jodli.coffeeshottimer.domain.usecase.GetShotDetailsUseCase
import com.jodli.coffeeshottimer.domain.usecase.GetTastePreselectionUseCase
import com.jodli.coffeeshottimer.domain.usecase.ManageGrindRecommendationUseCase
import com.jodli.coffeeshottimer.domain.usecase.RecordShotUseCase
import com.jodli.coffeeshottimer.domain.usecase.RecordTasteFeedbackUseCase
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import com.jodli.coffeeshottimer.ui.util.StringResourceProvider
import com.jodli.coffeeshottimer.ui.validation.ValidationStringProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Integration tests for timer state preservation across configuration changes.
 * Tests the interaction between ShotRecordingViewModel and RecordShotUseCase
 * for maintaining timer accuracy during orientation changes.
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class TimerStatePreservationTest {

    private lateinit var recordShotUseCase: RecordShotUseCase
    private lateinit var getShotDetailsUseCase: GetShotDetailsUseCase
    private lateinit var getTastePreselectionUseCase: GetTastePreselectionUseCase
    private lateinit var recordTasteFeedbackUseCase: RecordTasteFeedbackUseCase
    private lateinit var calculateGrindAdjustmentUseCase: CalculateGrindAdjustmentUseCase
    private lateinit var manageGrindRecommendationUseCase: ManageGrindRecommendationUseCase
    private lateinit var beanRepository: BeanRepository
    private lateinit var shotRepository: ShotRepository
    private lateinit var domainErrorTranslator: DomainErrorTranslator
    private lateinit var stringResourceProvider: StringResourceProvider
    private lateinit var validationStringProvider: ValidationStringProvider
    private lateinit var grinderConfigRepository: com.jodli.coffeeshottimer.data.repository.GrinderConfigRepository
    private lateinit var basketConfigRepository: com.jodli.coffeeshottimer.data.repository.BasketConfigRepository
    private lateinit var context: Context
    private lateinit var savedStateHandle: SavedStateHandle

    private lateinit var viewModel: ShotRecordingViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        // Mock SystemClock for consistent testing
        mockkStatic(SystemClock::class)

        // Mock dependencies
        shotRepository = mockk(relaxed = true)
        recordShotUseCase = RecordShotUseCase(shotRepository)
        getShotDetailsUseCase = mockk(relaxed = true)
        getTastePreselectionUseCase = mockk(relaxed = true)
        recordTasteFeedbackUseCase = mockk(relaxed = true)
        calculateGrindAdjustmentUseCase = mockk(relaxed = true)
        manageGrindRecommendationUseCase = mockk(relaxed = true)
        beanRepository = mockk(relaxed = true)
        domainErrorTranslator = mockk(relaxed = true)
        stringResourceProvider = mockk(relaxed = true)
        validationStringProvider = mockk(relaxed = true)
        grinderConfigRepository = mockk(relaxed = true)
        basketConfigRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle()

        // Mock repository responses
        every { beanRepository.getActiveBeans() } returns kotlinx.coroutines.flow.flowOf(Result.success(emptyList()))
        io.mockk.coEvery { beanRepository.getCurrentBean() } returns Result.success(null)
        io.mockk.coEvery { grinderConfigRepository.getOrCreateDefaultConfig() } returns Result.success(
            com.jodli.coffeeshottimer.data.model.GrinderConfiguration.DEFAULT_CONFIGURATION
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun `timer state is preserved when ViewModel is recreated`() = testScope.runTest {
        // Mock SystemClock to return predictable values
        var mockTime = 1000L
        every { SystemClock.elapsedRealtime() } answers { mockTime }

        // Create initial ViewModel
        viewModel = ShotRecordingViewModel(
            recordShotUseCase = recordShotUseCase,
            getShotDetailsUseCase = getShotDetailsUseCase,
            recordTasteFeedbackUseCase = recordTasteFeedbackUseCase,
            calculateGrindAdjustmentUseCase = calculateGrindAdjustmentUseCase,
            manageGrindRecommendationUseCase = manageGrindRecommendationUseCase,
            beanRepository = beanRepository,
            shotRepository = shotRepository,
            domainErrorTranslator = domainErrorTranslator,
            stringResourceProvider = stringResourceProvider,
            validationStringProvider = validationStringProvider,
            grinderConfigRepository = grinderConfigRepository,
            basketConfigRepository = basketConfigRepository,
            context = context,
            savedStateHandle = savedStateHandle
        )

        // Start timer
        viewModel.startTimer()
        assertTrue("Timer should be running", viewModel.timerState.value.isRunning)
        assertEquals("Timer should start at mock time", mockTime, viewModel.timerState.value.startTime)

        // Advance time by 5 seconds
        mockTime += 5000L
        advanceTimeBy(5000L)

        // Verify timer is running and has elapsed time
        assertTrue("Timer should still be running", viewModel.timerState.value.isRunning)
        assertEquals("Timer should show 5 seconds elapsed", 5, viewModel.timerState.value.elapsedTimeSeconds)

        // Simulate configuration change by creating new ViewModel with same SavedStateHandle
        val newViewModel = ShotRecordingViewModel(
            recordShotUseCase = RecordShotUseCase(shotRepository), // New use case instance
            getShotDetailsUseCase = getShotDetailsUseCase,
            recordTasteFeedbackUseCase = recordTasteFeedbackUseCase,
            calculateGrindAdjustmentUseCase = calculateGrindAdjustmentUseCase,
            manageGrindRecommendationUseCase = manageGrindRecommendationUseCase,
            beanRepository = beanRepository,
            shotRepository = shotRepository,
            domainErrorTranslator = domainErrorTranslator,
            stringResourceProvider = stringResourceProvider,
            validationStringProvider = validationStringProvider,
            grinderConfigRepository = grinderConfigRepository,
            basketConfigRepository = basketConfigRepository,
            context = context,
            savedStateHandle = savedStateHandle // Same SavedStateHandle
        )

        // Verify timer state is restored
        assertTrue("Timer should be running after restoration", newViewModel.timerState.value.isRunning)
        assertEquals("Timer start time should be preserved", mockTime - 5000L, newViewModel.timerState.value.startTime)
        assertEquals("Timer elapsed time should be preserved", 5, newViewModel.timerState.value.elapsedTimeSeconds)
    }

    @Test
    fun `paused timer state is preserved across configuration changes`() = testScope.runTest {
        var mockTime = 2000L
        every { SystemClock.elapsedRealtime() } answers { mockTime }

        viewModel = ShotRecordingViewModel(
            recordShotUseCase = recordShotUseCase,
            getShotDetailsUseCase = getShotDetailsUseCase,
            recordTasteFeedbackUseCase = recordTasteFeedbackUseCase,
            calculateGrindAdjustmentUseCase = calculateGrindAdjustmentUseCase,
            manageGrindRecommendationUseCase = manageGrindRecommendationUseCase,
            beanRepository = beanRepository,
            shotRepository = shotRepository,
            domainErrorTranslator = domainErrorTranslator,
            stringResourceProvider = stringResourceProvider,
            validationStringProvider = validationStringProvider,
            grinderConfigRepository = grinderConfigRepository,
            basketConfigRepository = basketConfigRepository,
            context = context,
            savedStateHandle = savedStateHandle
        )

        // Start timer
        viewModel.startTimer()

        // Advance time by 3 seconds
        mockTime += 3000L
        advanceTimeBy(3000L)

        // Pause timer
        viewModel.pauseTimer()
        assertFalse("Timer should be paused", viewModel.timerState.value.isRunning)
        assertEquals("Timer should show 3 seconds elapsed", 3, viewModel.timerState.value.elapsedTimeSeconds)

        // Create new ViewModel (simulate configuration change)
        val newViewModel = ShotRecordingViewModel(
            recordShotUseCase = RecordShotUseCase(shotRepository),
            getShotDetailsUseCase = getShotDetailsUseCase,
            recordTasteFeedbackUseCase = recordTasteFeedbackUseCase,
            calculateGrindAdjustmentUseCase = calculateGrindAdjustmentUseCase,
            manageGrindRecommendationUseCase = manageGrindRecommendationUseCase,
            beanRepository = beanRepository,
            shotRepository = shotRepository,
            domainErrorTranslator = domainErrorTranslator,
            stringResourceProvider = stringResourceProvider,
            validationStringProvider = validationStringProvider,
            grinderConfigRepository = grinderConfigRepository,
            basketConfigRepository = basketConfigRepository,
            context = context,
            savedStateHandle = savedStateHandle
        )

        // Verify paused state is preserved
        assertFalse("Timer should remain paused after restoration", newViewModel.timerState.value.isRunning)
        assertEquals("Elapsed time should be preserved", 3, newViewModel.timerState.value.elapsedTimeSeconds)
    }

    @Test
    fun `reset timer state is preserved across configuration changes`() = testScope.runTest {
        var mockTime = 3000L
        every { SystemClock.elapsedRealtime() } answers { mockTime }

        viewModel = ShotRecordingViewModel(
            recordShotUseCase = recordShotUseCase,
            getShotDetailsUseCase = getShotDetailsUseCase,
            recordTasteFeedbackUseCase = recordTasteFeedbackUseCase,
            calculateGrindAdjustmentUseCase = calculateGrindAdjustmentUseCase,
            manageGrindRecommendationUseCase = manageGrindRecommendationUseCase,
            beanRepository = beanRepository,
            shotRepository = shotRepository,
            domainErrorTranslator = domainErrorTranslator,
            stringResourceProvider = stringResourceProvider,
            validationStringProvider = validationStringProvider,
            grinderConfigRepository = grinderConfigRepository,
            basketConfigRepository = basketConfigRepository,
            context = context,
            savedStateHandle = savedStateHandle
        )

        // Start and then reset timer
        viewModel.startTimer()
        mockTime += 2000L
        advanceTimeBy(2000L)
        viewModel.resetTimer()

        // Verify reset state
        assertFalse("Timer should be stopped after reset", viewModel.timerState.value.isRunning)
        assertEquals("Timer should show 0 seconds after reset", 0, viewModel.timerState.value.elapsedTimeSeconds)

        // Create new ViewModel (simulate configuration change)
        val newViewModel = ShotRecordingViewModel(
            recordShotUseCase = RecordShotUseCase(shotRepository),
            getShotDetailsUseCase = getShotDetailsUseCase,
            recordTasteFeedbackUseCase = recordTasteFeedbackUseCase,
            calculateGrindAdjustmentUseCase = calculateGrindAdjustmentUseCase,
            manageGrindRecommendationUseCase = manageGrindRecommendationUseCase,
            beanRepository = beanRepository,
            shotRepository = shotRepository,
            domainErrorTranslator = domainErrorTranslator,
            stringResourceProvider = stringResourceProvider,
            validationStringProvider = validationStringProvider,
            grinderConfigRepository = grinderConfigRepository,
            basketConfigRepository = basketConfigRepository,
            context = context,
            savedStateHandle = savedStateHandle
        )

        // Verify reset state is preserved
        assertFalse("Timer should remain reset after restoration", newViewModel.timerState.value.isRunning)
        assertEquals(
            "Elapsed time should remain 0 after restoration",
            0,
            newViewModel.timerState.value.elapsedTimeSeconds
        )
    }

    @Test
    fun `timer accuracy is maintained across multiple configuration changes`() = testScope.runTest {
        var mockTime = 4000L
        every { SystemClock.elapsedRealtime() } answers { mockTime }

        viewModel = ShotRecordingViewModel(
            recordShotUseCase = recordShotUseCase,
            getShotDetailsUseCase = getShotDetailsUseCase,
            recordTasteFeedbackUseCase = recordTasteFeedbackUseCase,
            calculateGrindAdjustmentUseCase = calculateGrindAdjustmentUseCase,
            manageGrindRecommendationUseCase = manageGrindRecommendationUseCase,
            beanRepository = beanRepository,
            shotRepository = shotRepository,
            domainErrorTranslator = domainErrorTranslator,
            stringResourceProvider = stringResourceProvider,
            validationStringProvider = validationStringProvider,
            grinderConfigRepository = grinderConfigRepository,
            basketConfigRepository = basketConfigRepository,
            context = context,
            savedStateHandle = savedStateHandle
        )

        // Start timer
        viewModel.startTimer()
        val originalStartTime = viewModel.timerState.value.startTime

        // First configuration change after 2 seconds
        mockTime += 2000L
        advanceTimeBy(2000L)

        var newViewModel = ShotRecordingViewModel(
            recordShotUseCase = RecordShotUseCase(shotRepository),
            getShotDetailsUseCase = getShotDetailsUseCase,
            recordTasteFeedbackUseCase = recordTasteFeedbackUseCase,
            calculateGrindAdjustmentUseCase = calculateGrindAdjustmentUseCase,
            manageGrindRecommendationUseCase = manageGrindRecommendationUseCase,
            beanRepository = beanRepository,
            shotRepository = shotRepository,
            domainErrorTranslator = domainErrorTranslator,
            stringResourceProvider = stringResourceProvider,
            validationStringProvider = validationStringProvider,
            grinderConfigRepository = grinderConfigRepository,
            basketConfigRepository = basketConfigRepository,
            context = context,
            savedStateHandle = savedStateHandle
        )

        assertTrue("Timer should be running after first restoration", newViewModel.timerState.value.isRunning)
        assertEquals("Start time should be preserved", originalStartTime, newViewModel.timerState.value.startTime)
        assertEquals("Elapsed time should be 2 seconds", 2, newViewModel.timerState.value.elapsedTimeSeconds)

        // Second configuration change after another 3 seconds
        mockTime += 3000L
        advanceTimeBy(3000L)

        newViewModel = ShotRecordingViewModel(
            recordShotUseCase = RecordShotUseCase(shotRepository),
            getShotDetailsUseCase = getShotDetailsUseCase,
            recordTasteFeedbackUseCase = recordTasteFeedbackUseCase,
            calculateGrindAdjustmentUseCase = calculateGrindAdjustmentUseCase,
            manageGrindRecommendationUseCase = manageGrindRecommendationUseCase,
            beanRepository = beanRepository,
            shotRepository = shotRepository,
            domainErrorTranslator = domainErrorTranslator,
            stringResourceProvider = stringResourceProvider,
            validationStringProvider = validationStringProvider,
            grinderConfigRepository = grinderConfigRepository,
            basketConfigRepository = basketConfigRepository,
            context = context,
            savedStateHandle = savedStateHandle
        )

        assertTrue("Timer should be running after second restoration", newViewModel.timerState.value.isRunning)
        assertEquals("Start time should still be preserved", originalStartTime, newViewModel.timerState.value.startTime)
        assertEquals("Elapsed time should be 5 seconds total", 5, newViewModel.timerState.value.elapsedTimeSeconds)
    }

    @Test
    fun `timer continues accurately after configuration change during running state`() = testScope.runTest {
        var mockTime = 5000L
        every { SystemClock.elapsedRealtime() } answers { mockTime }

        viewModel = ShotRecordingViewModel(
            recordShotUseCase = recordShotUseCase,
            getShotDetailsUseCase = getShotDetailsUseCase,
            recordTasteFeedbackUseCase = recordTasteFeedbackUseCase,
            calculateGrindAdjustmentUseCase = calculateGrindAdjustmentUseCase,
            manageGrindRecommendationUseCase = manageGrindRecommendationUseCase,
            beanRepository = beanRepository,
            shotRepository = shotRepository,
            domainErrorTranslator = domainErrorTranslator,
            stringResourceProvider = stringResourceProvider,
            validationStringProvider = validationStringProvider,
            grinderConfigRepository = grinderConfigRepository,
            basketConfigRepository = basketConfigRepository,
            context = context,
            savedStateHandle = savedStateHandle
        )

        // Start timer
        viewModel.startTimer()

        // Advance time by 4 seconds
        mockTime += 4000L
        advanceTimeBy(4000L)

        // Configuration change while timer is running
        val newViewModel = ShotRecordingViewModel(
            recordShotUseCase = RecordShotUseCase(shotRepository),
            getShotDetailsUseCase = getShotDetailsUseCase,
            recordTasteFeedbackUseCase = recordTasteFeedbackUseCase,
            calculateGrindAdjustmentUseCase = calculateGrindAdjustmentUseCase,
            manageGrindRecommendationUseCase = manageGrindRecommendationUseCase,
            beanRepository = beanRepository,
            shotRepository = shotRepository,
            domainErrorTranslator = domainErrorTranslator,
            stringResourceProvider = stringResourceProvider,
            validationStringProvider = validationStringProvider,
            grinderConfigRepository = grinderConfigRepository,
            basketConfigRepository = basketConfigRepository,
            context = context,
            savedStateHandle = savedStateHandle
        )

        // Verify timer continues running with correct elapsed time
        assertTrue("Timer should continue running after restoration", newViewModel.timerState.value.isRunning)
        assertEquals("Elapsed time should be 4 seconds", 4, newViewModel.timerState.value.elapsedTimeSeconds)

        // Advance time by another 2 seconds and verify accuracy
        mockTime += 2000L
        advanceTimeBy(2000L)

        // The timer should automatically update through the ViewModel's periodic updates
        // We can verify the elapsed time through the timerState

        assertEquals("Total elapsed time should be 6 seconds", 6, newViewModel.timerState.value.elapsedTimeSeconds)
    }
}
