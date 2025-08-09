package com.jodli.coffeeshottimer.ui.viewmodel

import com.jodli.coffeeshottimer.data.onboarding.OnboardingManager
import com.jodli.coffeeshottimer.data.onboarding.OnboardingProgress
import com.jodli.coffeeshottimer.data.onboarding.OnboardingStep
import com.jodli.coffeeshottimer.ui.navigation.NavigationDestinations
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

@OptIn(ExperimentalCoroutinesApi::class)
class MainActivityViewModelTest {

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
    fun `when user is first time and has no progress, routes to introduction`() = runTest {
        // Given
        coEvery { onboardingManager.isFirstTimeUser() } returns true
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress()

        // When
        viewModel = MainActivityViewModel(onboardingManager)
        advanceUntilIdle()

        // Then
        val state = viewModel.routingState.first()
        assertTrue(state is RoutingState.Success)
        val successState = state as RoutingState.Success
        assertEquals(NavigationDestinations.OnboardingIntroduction.route, successState.route)
        assertTrue(successState.isFirstTimeUser)
    }

    @Test
    fun `when user is first time and has seen introduction, routes to equipment setup`() = runTest {
        // Given
        coEvery { onboardingManager.isFirstTimeUser() } returns true
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true
        )

        // When
        viewModel = MainActivityViewModel(onboardingManager)
        advanceUntilIdle()

        // Then
        val state = viewModel.routingState.first()
        assertTrue(state is RoutingState.Success)
        val successState = state as RoutingState.Success
        assertEquals(NavigationDestinations.OnboardingEquipmentSetup.route, successState.route)
        assertTrue(successState.isFirstTimeUser)
    }

    @Test
    fun `when user is first time and has completed equipment setup, routes to record shot`() = runTest {
        // Given
        coEvery { onboardingManager.isFirstTimeUser() } returns true
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true
        )

        // When
        viewModel = MainActivityViewModel(onboardingManager)
        advanceUntilIdle()

        // Then
        val state = viewModel.routingState.first()
        assertTrue(state is RoutingState.Success)
        val successState = state as RoutingState.Success
        assertEquals(NavigationDestinations.RecordShot.route, successState.route)
        assertTrue(successState.isFirstTimeUser)
    }

    @Test
    fun `when user is not first time, routes to record shot`() = runTest {
        // Given
        coEvery { onboardingManager.isFirstTimeUser() } returns false

        // When
        viewModel = MainActivityViewModel(onboardingManager)
        advanceUntilIdle()

        // Then
        val state = viewModel.routingState.first()
        assertTrue(state is RoutingState.Success)
        val successState = state as RoutingState.Success
        assertEquals(NavigationDestinations.RecordShot.route, successState.route)
        assertTrue(!successState.isFirstTimeUser)
    }

    @Test
    fun `when first time user has complete progress, marks onboarding complete and routes to record shot`() = runTest {
        // Given
        coEvery { onboardingManager.isFirstTimeUser() } returns true
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            hasRecordedFirstShot = true
        )
        coEvery { onboardingManager.markOnboardingComplete() } returns Unit

        // When
        viewModel = MainActivityViewModel(onboardingManager)
        advanceUntilIdle()

        // Then
        val state = viewModel.routingState.first()
        assertTrue(state is RoutingState.Success)
        val successState = state as RoutingState.Success
        assertEquals(NavigationDestinations.RecordShot.route, successState.route)
        assertTrue(successState.isFirstTimeUser)
        
        // Verify onboarding was marked complete
        coVerify { onboardingManager.markOnboardingComplete() }
    }

    @Test
    fun `when onboarding manager throws exception, returns error state with fallback route`() = runTest {
        // Given
        val exception = RuntimeException("Test exception")
        coEvery { onboardingManager.isFirstTimeUser() } throws exception

        // When
        viewModel = MainActivityViewModel(onboardingManager)
        advanceUntilIdle()

        // Then
        val state = viewModel.routingState.first()
        assertTrue(state is RoutingState.Error)
        val errorState = state as RoutingState.Error
        assertEquals(NavigationDestinations.RecordShot.route, errorState.fallbackRoute)
        assertEquals(exception, errorState.exception)
    }

    @Test
    fun `retryRouting resets state and re-determines route`() = runTest {
        // Given
        val exception = RuntimeException("Test exception")
        coEvery { onboardingManager.isFirstTimeUser() } throws exception andThen false

        viewModel = MainActivityViewModel(onboardingManager)
        advanceUntilIdle()

        // Verify initial error state
        val initialState = viewModel.routingState.first()
        assertTrue(initialState is RoutingState.Error)

        // When
        viewModel.retryRouting()
        advanceUntilIdle()

        // Then
        val retryState = viewModel.routingState.first()
        assertTrue(retryState is RoutingState.Success)
        val successState = retryState as RoutingState.Success
        assertEquals(NavigationDestinations.RecordShot.route, successState.route)
        assertTrue(!successState.isFirstTimeUser)
    }

    @Test
    fun `forceNormalFlow routes to record shot regardless of onboarding state`() = runTest {
        // Given
        coEvery { onboardingManager.isFirstTimeUser() } returns true
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress()

        viewModel = MainActivityViewModel(onboardingManager)
        advanceUntilIdle()

        // Verify initial onboarding state
        val initialState = viewModel.routingState.first()
        assertTrue(initialState is RoutingState.Success)
        assertEquals(NavigationDestinations.OnboardingIntroduction.route, (initialState as RoutingState.Success).route)

        // When
        viewModel.forceNormalFlow()

        // Then
        val forcedState = viewModel.routingState.first()
        assertTrue(forcedState is RoutingState.Success)
        val successState = forcedState as RoutingState.Success
        assertEquals(NavigationDestinations.RecordShot.route, successState.route)
        assertTrue(!successState.isFirstTimeUser)
    }

    @Test
    fun `handleAppUpdate marks complete when first-time user has complete progress`() = runTest {
        // Given
        coEvery { onboardingManager.isFirstTimeUser() } returns true
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            hasRecordedFirstShot = true
        )
        coEvery { onboardingManager.markOnboardingComplete() } returns Unit

        viewModel = MainActivityViewModel(onboardingManager)
        advanceUntilIdle()

        // When
        viewModel.handleAppUpdate()
        advanceUntilIdle()

        // Then
        val state = viewModel.routingState.first()
        assertTrue(state is RoutingState.Success)
        val successState = state as RoutingState.Success
        assertEquals(NavigationDestinations.RecordShot.route, successState.route)
        assertTrue(!successState.isFirstTimeUser)
        
        // Verify onboarding was marked complete
        coVerify { onboardingManager.markOnboardingComplete() }
    }

    @Test
    fun `handleAppUpdate handles exceptions gracefully`() = runTest {
        // Given
        val exception = RuntimeException("App update exception")
        coEvery { onboardingManager.isFirstTimeUser() } returns true
        coEvery { onboardingManager.getOnboardingProgress() } throws exception

        viewModel = MainActivityViewModel(onboardingManager)
        advanceUntilIdle()

        // When
        viewModel.handleAppUpdate()
        advanceUntilIdle()

        // Then
        val state = viewModel.routingState.first()
        assertTrue(state is RoutingState.Error)
        val errorState = state as RoutingState.Error
        assertEquals(NavigationDestinations.RecordShot.route, errorState.fallbackRoute)
        assertEquals(exception, errorState.exception)
    }
}