package com.jodli.coffeeshottimer.integration

import com.jodli.coffeeshottimer.data.onboarding.OnboardingManager
import com.jodli.coffeeshottimer.data.onboarding.OnboardingProgress
import com.jodli.coffeeshottimer.ui.navigation.NavigationDestinations
import com.jodli.coffeeshottimer.ui.viewmodel.MainActivityViewModel
import com.jodli.coffeeshottimer.ui.viewmodel.RoutingState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration test for the complete onboarding routing flow.
 * Tests the interaction between MainActivity and OnboardingManager.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingRoutingIntegrationTest {

    private lateinit var onboardingManager: OnboardingManager
    private lateinit var viewModel: MainActivityViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        onboardingManager = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `complete first-time user flow routes through all onboarding steps`() = runTest {
        // Test scenario: New user progresses through complete onboarding flow
        
        // Step 1: First launch - should route to introduction
        coEvery { onboardingManager.isFirstTimeUser() } returns true
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress()

        viewModel = MainActivityViewModel(onboardingManager)
        advanceUntilIdle()

        val step1State = viewModel.routingState.first()
        assertTrue(step1State is RoutingState.Success)
        assertEquals(NavigationDestinations.OnboardingIntroduction.route, (step1State as RoutingState.Success).route)
        assertTrue(step1State.isFirstTimeUser)

        // Step 2: After introduction - should route to equipment setup
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true
        )

        viewModel = MainActivityViewModel(onboardingManager)
        advanceUntilIdle()

        val step2State = viewModel.routingState.first()
        assertTrue(step2State is RoutingState.Success)
        assertEquals(NavigationDestinations.OnboardingEquipmentSetup.route, (step2State as RoutingState.Success).route)
        assertTrue(step2State.isFirstTimeUser)

        // Step 3: After equipment setup - should route to first shot (RecordShot)
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true
        )

        viewModel = MainActivityViewModel(onboardingManager)
        advanceUntilIdle()

        val step3State = viewModel.routingState.first()
        assertTrue(step3State is RoutingState.Success)
        assertEquals(NavigationDestinations.RecordShot.route, (step3State as RoutingState.Success).route)
        assertTrue(step3State.isFirstTimeUser)

        // Step 4: After first shot - should be marked complete and route to normal flow
        coEvery { onboardingManager.isFirstTimeUser() } returns false

        viewModel = MainActivityViewModel(onboardingManager)
        advanceUntilIdle()

        val step4State = viewModel.routingState.first()
        assertTrue(step4State is RoutingState.Success)
        assertEquals(NavigationDestinations.RecordShot.route, (step4State as RoutingState.Success).route)
        assertTrue(!step4State.isFirstTimeUser)
    }

    @Test
    fun `app update scenario handles inconsistent state correctly`() = runTest {
        // Test scenario: App update causes inconsistent onboarding state
        
        // Given: User is marked as first-time but has completed all steps
        coEvery { onboardingManager.isFirstTimeUser() } returns true
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            hasRecordedFirstShot = true
        )
        coEvery { onboardingManager.markOnboardingComplete() } returns Unit

        viewModel = MainActivityViewModel(onboardingManager)
        advanceUntilIdle()

        // When: handleAppUpdate is called
        viewModel.handleAppUpdate()
        advanceUntilIdle()

        // Then: Should mark onboarding complete and route to normal flow
        val state = viewModel.routingState.first()
        assertTrue(state is RoutingState.Success)
        assertEquals(NavigationDestinations.RecordShot.route, (state as RoutingState.Success).route)
        
        coVerify { onboardingManager.markOnboardingComplete() }
    }

    @Test
    fun `data clearing scenario resets to introduction`() = runTest {
        // Test scenario: User clears app data, should restart onboarding
        
        // Given: Fresh state after data clearing
        coEvery { onboardingManager.isFirstTimeUser() } returns true
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress()

        viewModel = MainActivityViewModel(onboardingManager)
        advanceUntilIdle()

        // Then: Should route to introduction
        val state = viewModel.routingState.first()
        assertTrue(state is RoutingState.Success)
        assertEquals(NavigationDestinations.OnboardingIntroduction.route, (state as RoutingState.Success).route)
        assertTrue(state.isFirstTimeUser)
    }

    @Test
    fun `error recovery provides fallback to normal flow`() = runTest {
        // Test scenario: OnboardingManager fails, should fallback gracefully
        
        // Given: OnboardingManager throws exception
        val exception = RuntimeException("Storage failure")
        coEvery { onboardingManager.isFirstTimeUser() } throws exception

        viewModel = MainActivityViewModel(onboardingManager)
        advanceUntilIdle()

        // Then: Should provide error state with fallback route
        val state = viewModel.routingState.first()
        assertTrue(state is RoutingState.Error)
        val errorState = state as RoutingState.Error
        assertEquals(NavigationDestinations.RecordShot.route, errorState.fallbackRoute)
        assertEquals(exception, errorState.exception)

        // When: Retry is attempted and succeeds
        coEvery { onboardingManager.isFirstTimeUser() } returns false

        viewModel.retryRouting()
        advanceUntilIdle()

        // Then: Should recover and route normally
        val recoveredState = viewModel.routingState.first()
        assertTrue(recoveredState is RoutingState.Success)
        assertEquals(NavigationDestinations.RecordShot.route, (recoveredState as RoutingState.Success).route)
        assertTrue(!recoveredState.isFirstTimeUser)
    }

    @Test
    fun `force normal flow bypasses onboarding completely`() = runTest {
        // Test scenario: Emergency bypass of onboarding system
        
        // Given: User should normally go through onboarding
        coEvery { onboardingManager.isFirstTimeUser() } returns true
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress()

        viewModel = MainActivityViewModel(onboardingManager)
        advanceUntilIdle()

        // Verify initial onboarding routing
        val initialState = viewModel.routingState.first()
        assertTrue(initialState is RoutingState.Success)
        assertEquals(NavigationDestinations.OnboardingIntroduction.route, (initialState as RoutingState.Success).route)

        // When: Force normal flow is called
        viewModel.forceNormalFlow()

        // Then: Should bypass onboarding and route to normal flow
        val forcedState = viewModel.routingState.first()
        assertTrue(forcedState is RoutingState.Success)
        assertEquals(NavigationDestinations.RecordShot.route, (forcedState as RoutingState.Success).route)
        assertTrue(!forcedState.isFirstTimeUser)
    }
}