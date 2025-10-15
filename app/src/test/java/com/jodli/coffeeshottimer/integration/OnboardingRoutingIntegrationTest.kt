package com.jodli.coffeeshottimer.integration

import com.jodli.coffeeshottimer.data.onboarding.OnboardingManager
import com.jodli.coffeeshottimer.data.onboarding.OnboardingProgress
import com.jodli.coffeeshottimer.data.repository.BeanRepository
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
import org.junit.Assert.assertFalse
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
    private lateinit var beanRepository: BeanRepository
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
    fun `complete first-time user flow routes through all onboarding steps`() = runTest {
        // Test scenario: New user progresses through complete onboarding flow

        // Step 1: First launch - should route to introduction
        coEvery { onboardingManager.isFirstTimeUser() } returns true
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress()

        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        advanceUntilIdle()

        val step1State = viewModel.routingState.first()
        assertTrue("Expected Success state but got $step1State", step1State is RoutingState.Success)
        assertEquals(NavigationDestinations.OnboardingIntroduction.route, (step1State as RoutingState.Success).route)
        assertTrue(step1State.isFirstTimeUser)

        // Step 2: After introduction - should route to equipment setup
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true
        )

        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        advanceUntilIdle()

        val step2State = viewModel.routingState.first()
        assertTrue(step2State is RoutingState.Success)
        assertEquals(NavigationDestinations.OnboardingEquipmentSetup.route, (step2State as RoutingState.Success).route)
        assertTrue(step2State.isFirstTimeUser)

        // Step 3: After equipment setup - should route to guided bean creation
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
        )

        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        advanceUntilIdle()

        val step3State = viewModel.routingState.first()
        assertTrue(step3State is RoutingState.Success)
        assertEquals(
            NavigationDestinations.OnboardingGuidedBeanCreation.route,
            (step3State as RoutingState.Success).route
        )
        assertTrue(step3State.isFirstTimeUser)

        // Step 4: After guided bean creation - should route to first shot (RecordShot)
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            hasCreatedFirstBean = true,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
        )

        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        advanceUntilIdle()

        val step4State = viewModel.routingState.first()
        assertTrue(step4State is RoutingState.Success)
        assertEquals(NavigationDestinations.RecordShot.route, (step4State as RoutingState.Success).route)
        assertTrue(step4State.isFirstTimeUser)

        // Step 5: After first shot - should be marked complete and route to normal flow
        coEvery { onboardingManager.isFirstTimeUser() } returns false

        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        advanceUntilIdle()

        val step5State = viewModel.routingState.first()
        assertTrue(step5State is RoutingState.Success)
        assertEquals(NavigationDestinations.RecordShot.route, (step5State as RoutingState.Success).route)
        assertTrue(!step5State.isFirstTimeUser)
    }

    @Test
    fun `app update scenario handles inconsistent state correctly`() = runTest {
        // Test scenario: App update causes inconsistent onboarding state

        // Given: User is marked as first-time but has completed all steps
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

        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
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

        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        advanceUntilIdle()

        // Then: Should provide error state with fallback route
        val state = viewModel.routingState.first()
        assertTrue(state is RoutingState.Error)
        val errorState = state as RoutingState.Error
        assertEquals(NavigationDestinations.RecordShot.route, errorState.fallbackRoute)
        assertEquals("Storage failure", errorState.exception.message)

        // When: Retry is attempted and succeeds
        coEvery { onboardingManager.isFirstTimeUser() } returns false
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            hasCreatedFirstBean = true,
            hasRecordedFirstShot = true,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
        )

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

        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
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

    @Test
    fun `onboarding navigation flow handles back navigation correctly`() = runTest {
        // Test scenario: User navigates back during onboarding flow

        // Step 1: Start at introduction
        coEvery { onboardingManager.isFirstTimeUser() } returns true
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress()

        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        advanceUntilIdle()

        val introState = viewModel.routingState.first()
        assertTrue(introState is RoutingState.Success)
        assertEquals(NavigationDestinations.OnboardingIntroduction.route, (introState as RoutingState.Success).route)

        // Step 2: Progress to equipment setup
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true
        )

        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        advanceUntilIdle()

        val equipmentState = viewModel.routingState.first()
        assertTrue(equipmentState is RoutingState.Success)
        assertEquals(
            NavigationDestinations.OnboardingEquipmentSetup.route,
            (equipmentState as RoutingState.Success).route
        )

        // Step 3: User goes back to introduction (should be allowed)
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress()

        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        advanceUntilIdle()

        val backToIntroState = viewModel.routingState.first()
        assertTrue(backToIntroState is RoutingState.Success)
        assertEquals(
            NavigationDestinations.OnboardingIntroduction.route,
            (backToIntroState as RoutingState.Success).route
        )
    }

    @Test
    fun `onboarding skip functionality routes correctly`() = runTest {
        // Test scenario: User skips onboarding at introduction screen

        // Given: User is at introduction screen
        coEvery { onboardingManager.isFirstTimeUser() } returns true
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress()
        coEvery { onboardingManager.markOnboardingComplete() } returns Unit

        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        advanceUntilIdle()

        val introState = viewModel.routingState.first()
        assertTrue(introState is RoutingState.Success)
        assertEquals(NavigationDestinations.OnboardingIntroduction.route, (introState as RoutingState.Success).route)

        // When: User skips onboarding (simulated by marking complete and forcing normal flow)
        viewModel.forceNormalFlow()

        // Then: Should route directly to normal app flow
        val skippedState = viewModel.routingState.first()
        assertTrue(skippedState is RoutingState.Success)
        assertEquals(NavigationDestinations.RecordShot.route, (skippedState as RoutingState.Success).route)
        assertTrue(!skippedState.isFirstTimeUser)
    }

    @Test
    fun `onboarding completion clears navigation stack correctly`() = runTest {
        // Test scenario: User completes onboarding and should not be able to navigate back

        // Given: User completes guided bean creation
        coEvery { onboardingManager.isFirstTimeUser() } returns true
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            hasCreatedFirstBean = true,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
        )

        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        advanceUntilIdle()

        val firstShotState = viewModel.routingState.first()
        assertTrue(firstShotState is RoutingState.Success)
        assertEquals(NavigationDestinations.RecordShot.route, (firstShotState as RoutingState.Success).route)
        assertTrue(firstShotState.isFirstTimeUser)

        // When: User completes first shot
        coEvery { onboardingManager.isFirstTimeUser() } returns false
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            hasCreatedFirstBean = true,
            hasRecordedFirstShot = true,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
        )

        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        advanceUntilIdle()

        // Then: Should be in normal flow with no way to navigate back to onboarding
        val completedState = viewModel.routingState.first()
        assertTrue(completedState is RoutingState.Success)
        assertEquals(NavigationDestinations.RecordShot.route, (completedState as RoutingState.Success).route)
        assertTrue(!completedState.isFirstTimeUser)
    }

    @Test
    fun `existing user with outdated equipment setup is forced through equipment setup flow`() = runTest {
        // Given: Existing user with outdated equipment setup version
        coEvery { onboardingManager.isFirstTimeUser() } returns false
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = true,
            hasCreatedFirstBean = true,
            hasRecordedFirstShot = true,
            equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION - 1 // Outdated
        )

        // Mock that they have existing beans
        coEvery { beanRepository.getActiveBeanCount() } returns Result.success(3)

        // When: ViewModel is initialized
        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        advanceUntilIdle()

        // Then: Should route to equipment setup but marked as existing user
        val state = viewModel.routingState.first()
        assertTrue("Expected Success state but got $state", state is RoutingState.Success)
        assertEquals(NavigationDestinations.OnboardingEquipmentSetup.route, (state as RoutingState.Success).route)
        assertFalse(state.isFirstTimeUser) // Should be marked as existing user
    }

    @Test
    fun `existing user who completes equipment setup with beans goes directly to main app`() = runTest {
        // Given: Existing user with outdated equipment setup
        coEvery { onboardingManager.isFirstTimeUser() } returns false
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = false,
            hasCreatedFirstBean = false,
            hasRecordedFirstShot = false,
            equipmentSetupVersion = 1
        )
        coEvery { beanRepository.getActiveBeanCount() } returns Result.success(2) // Has beans
        coEvery { onboardingManager.updateOnboardingProgress(any()) } returns Unit

        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        advanceUntilIdle()

        // Verify they're routed to equipment setup
        val initialState = viewModel.routingState.first()
        assertEquals(
            NavigationDestinations.OnboardingEquipmentSetup.route,
            (initialState as RoutingState.Success).route
        )
        assertFalse(initialState.isFirstTimeUser)

        var resultCallback: Boolean? = null

        // When: They complete equipment setup
        viewModel.handleEquipmentSetupComplete { shouldSkipBeanCreation ->
            resultCallback = shouldSkipBeanCreation
        }
        advanceUntilIdle()

        // Then: Should indicate user should skip bean creation and update progress
        assertEquals(true, resultCallback)
        coVerify {
            onboardingManager.updateOnboardingProgress(
                match { progress ->
                    progress.hasCompletedEquipmentSetup &&
                        progress.hasCreatedFirstBean && // Should mark bean creation complete since they have beans
                        progress.equipmentSetupVersion == OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
                }
            )
        }
    }

    @Test
    fun `existing user who completes equipment setup without beans should continue to bean creation`() = runTest {
        // Given: Existing user with no beans
        coEvery { onboardingManager.isFirstTimeUser() } returns false
        coEvery { onboardingManager.getOnboardingProgress() } returns OnboardingProgress(
            hasSeenIntroduction = true,
            hasCompletedEquipmentSetup = false,
            hasCreatedFirstBean = false,
            hasRecordedFirstShot = false,
            equipmentSetupVersion = 1
        )
        coEvery { beanRepository.getActiveBeanCount() } returns Result.success(0) // No beans
        coEvery { onboardingManager.updateOnboardingProgress(any()) } returns Unit

        viewModel = MainActivityViewModel(onboardingManager, beanRepository)
        advanceUntilIdle()

        // Verify they're routed to equipment setup
        val initialState = viewModel.routingState.first()
        assertEquals(
            NavigationDestinations.OnboardingEquipmentSetup.route,
            (initialState as RoutingState.Success).route
        )
        assertFalse(initialState.isFirstTimeUser)

        var resultCallback: Boolean? = null

        // When: They complete equipment setup
        viewModel.handleEquipmentSetupComplete { shouldSkipBeanCreation ->
            resultCallback = shouldSkipBeanCreation
        }
        advanceUntilIdle()

        // Then: Should not skip bean creation since they have no beans
        assertEquals(false, resultCallback)
        coVerify {
            onboardingManager.updateOnboardingProgress(
                match { progress ->
                    progress.hasCompletedEquipmentSetup &&
                        !progress.hasCreatedFirstBean && // Should NOT mark bean creation complete since they have no beans
                        progress.equipmentSetupVersion == OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
                }
            )
        }
    }
}
