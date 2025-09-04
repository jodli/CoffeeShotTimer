package com.jodli.coffeeshottimer.ui.viewmodel

import com.jodli.coffeeshottimer.data.onboarding.OnboardingManager
import com.jodli.coffeeshottimer.data.onboarding.OnboardingProgress
import com.jodli.coffeeshottimer.data.onboarding.OnboardingStep
import com.jodli.coffeeshottimer.data.repository.BeanRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainActivityViewModelTest {

    private lateinit var onboardingManager: OnboardingManager
    private lateinit var beanRepository: BeanRepository
    private lateinit var viewModel: MainActivityViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        onboardingManager = mockk()
        beanRepository = mockk()
        
        // Default mock behavior for BeanRepository
        coEvery { beanRepository.getActiveBeanCount() } returns Result.success(0)
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
        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
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
        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        advanceUntilIdle()

        // Then
        val state = viewModel.routingState.first()
        assertTrue(state is RoutingState.Success)
        val successState = state as RoutingState.Success
        assertEquals(NavigationDestinations.OnboardingEquipmentSetup.route, successState.route)
        assertTrue(successState.isFirstTimeUser)
    }

    @Test
    fun `when user is first time and has completed equipment setup, routes to guided bean creation`() = runTest {
        // Given
        coEvery { onboardingManager.isFirstTimeUser() } returns true
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
        )

        // When
        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        advanceUntilIdle()

        // Then
        val state = viewModel.routingState.first()
        assertTrue(state is RoutingState.Success)
        val successState = state as RoutingState.Success
        assertEquals(NavigationDestinations.OnboardingGuidedBeanCreation.route, successState.route)
        assertTrue(successState.isFirstTimeUser)
    }

    @Test
    fun `when user is first time and has created bean, routes to record shot`() = runTest {
        // Given
        coEvery { onboardingManager.isFirstTimeUser() } returns true
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            hasCreatedFirstBean = true,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
        )

        // When
        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        advanceUntilIdle()

        // Then
        val state = viewModel.routingState.first()
        assertTrue(state is RoutingState.Success)
        val successState = state as RoutingState.Success
        assertEquals(NavigationDestinations.RecordShot.route, successState.route)
        assertTrue(successState.isFirstTimeUser)
    }

    @Test
    fun `when user is not first time and up-to-date, routes to record shot`() = runTest {
        // Given
        coEvery { onboardingManager.isFirstTimeUser() } returns false
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            hasCreatedFirstBean = true,
            hasRecordedFirstShot = true,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
        )

        // When
        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        advanceUntilIdle()

        // Then
        val state = viewModel.routingState.first()
        assertTrue(state is RoutingState.Success)
        assertEquals(NavigationDestinations.RecordShot.route, (state as RoutingState.Success).route)
        assertFalse(state.isFirstTimeUser)
    }
    
    @Test
    fun `when existing user has outdated equipment setup, routes to equipment setup`() = runTest {
        // Given
        coEvery { onboardingManager.isFirstTimeUser() } returns false
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            hasCreatedFirstBean = true,
            hasRecordedFirstShot = true,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION - 1 // Outdated
        )

        // When
        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        advanceUntilIdle()

        // Then
        val state = viewModel.routingState.first()
        assertTrue(state is RoutingState.Success)
        assertEquals(NavigationDestinations.OnboardingEquipmentSetup.route, (state as RoutingState.Success).route)
        assertFalse(state.isFirstTimeUser) // Should be marked as existing user
    }
    
    @Test
    fun `when existing user never completed equipment setup, routes to equipment setup`() = runTest {
        // Given
        coEvery { onboardingManager.isFirstTimeUser() } returns false
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = false, // Never completed
            hasCreatedFirstBean = true,
            hasRecordedFirstShot = true,
            equipmentSetupVersion = 1
        )

        // When
        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        advanceUntilIdle()

        // Then
        val state = viewModel.routingState.first()
        assertTrue(state is RoutingState.Success)
        assertEquals(NavigationDestinations.OnboardingEquipmentSetup.route, (state as RoutingState.Success).route)
        assertFalse(state.isFirstTimeUser) // Should be marked as existing user
    }

    @Test
    fun `when first time user has complete progress, marks onboarding complete and routes to record shot`() = runTest {
        // Given
        coEvery { onboardingManager.isFirstTimeUser() } returns true
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            hasCreatedFirstBean = true,
            hasRecordedFirstShot = true,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
        )
        coEvery { onboardingManager.markOnboardingComplete() } returns Unit

        // When
        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
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
        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        advanceUntilIdle()

        // Then
        val state = viewModel.routingState.first()
        assertTrue(state is RoutingState.Error)
        val errorState = state as RoutingState.Error
        assertEquals(NavigationDestinations.RecordShot.route, errorState.fallbackRoute)
        assertEquals(exception.message, errorState.exception.message)
    }

    @Test
    fun `retryRouting resets state and re-determines route`() = runTest {
        // Given
        val exception = RuntimeException("Test exception")
        coEvery { onboardingManager.isFirstTimeUser() } throws exception andThen false
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            hasCreatedFirstBean = true,
            hasRecordedFirstShot = true,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
        )

        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
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

        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
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
            hasCreatedFirstBean = true,
            hasRecordedFirstShot = true,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
        )
        coEvery { onboardingManager.markOnboardingComplete() } returns Unit

        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
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

        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
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
    
    @Test
    fun `handleEquipmentSetupComplete for user without beans returns false`() = runTest {
        // Given
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = false,
            equipmentSetupVersion = 1
        )
        coEvery { onboardingManager.updateOnboardingProgress(any()) } returns Unit
        coEvery { beanRepository.getActiveBeanCount() } returns Result.success(0) // No beans
        
        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        var resultShouldSkipBeanCreation: Boolean? = null
        
        // When
        viewModel.handleEquipmentSetupComplete { shouldSkip ->
            resultShouldSkipBeanCreation = shouldSkip
        }
        advanceUntilIdle()
        
        // Then
        assertEquals(false, resultShouldSkipBeanCreation) // Should not skip bean creation
        coVerify { 
            onboardingManager.updateOnboardingProgress(match { progress ->
                progress.hasCompletedEquipmentSetup && 
                !progress.hasCreatedFirstBean && // Should NOT mark bean creation complete
                progress.equipmentSetupVersion == OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
            })
        }
    }
    
    @Test
    fun `handleEquipmentSetupComplete for user with beans returns true and skips bean creation`() = runTest {
        // Given
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = false,
            hasCreatedFirstBean = false,
            equipmentSetupVersion = 1
        )
        coEvery { onboardingManager.updateOnboardingProgress(any()) } returns Unit
        coEvery { beanRepository.getActiveBeanCount() } returns Result.success(3) // Has beans
        
        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        var resultShouldSkipBeanCreation: Boolean? = null
        
        // When
        viewModel.handleEquipmentSetupComplete { shouldSkip ->
            resultShouldSkipBeanCreation = shouldSkip
        }
        advanceUntilIdle()
        
        // Then
        assertEquals(true, resultShouldSkipBeanCreation) // Should skip bean creation
        coVerify { 
            onboardingManager.updateOnboardingProgress(match { progress ->
                progress.hasCompletedEquipmentSetup && 
                progress.hasCreatedFirstBean && // Should also mark bean creation complete
                progress.equipmentSetupVersion == OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
            })
        }
    }
    
    @Test
    fun `handleEquipmentSetupSkip for user with beans returns true and skips bean creation`() = runTest {
        // Given
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = false,
            hasCreatedFirstBean = false,
            equipmentSetupVersion = 1
        )
        coEvery { onboardingManager.updateOnboardingProgress(any()) } returns Unit
        coEvery { beanRepository.getActiveBeanCount() } returns Result.success(5) // Has beans
        
        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        var resultShouldSkipBeanCreation: Boolean? = null
        
        // When
        viewModel.handleEquipmentSetupSkip { shouldSkip ->
            resultShouldSkipBeanCreation = shouldSkip
        }
        advanceUntilIdle()
        
        // Then
        assertEquals(true, resultShouldSkipBeanCreation) // Should skip bean creation
        coVerify { 
            onboardingManager.updateOnboardingProgress(match { progress ->
                progress.hasCompletedEquipmentSetup && 
                progress.hasCreatedFirstBean && // Should mark bean creation complete since they have beans
                progress.equipmentSetupVersion == OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
            })
        }
    }
    
}
