package com.jodli.coffeeshottimer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jodli.coffeeshottimer.data.onboarding.OnboardingManager
import com.jodli.coffeeshottimer.data.onboarding.OnboardingProgress
import com.jodli.coffeeshottimer.data.onboarding.OnboardingStep
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.ui.navigation.NavigationDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for MainActivity that handles initial routing logic based on onboarding state.
 * Determines whether users should be directed to onboarding flow or normal app usage.
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val onboardingManager: OnboardingManager,
    private val beanRepository: BeanRepository
) : ViewModel() {

    private val _routingState = MutableStateFlow<RoutingState>(RoutingState.Loading)
    val routingState: StateFlow<RoutingState> = _routingState.asStateFlow()

    init {
        determineInitialRoute()
    }

    /**
     * Determines the initial route based on onboarding state.
     * Handles various edge cases and provides fallback to normal flow.
     */
    private fun determineInitialRoute() {
        viewModelScope.launch {
            try {
                val isFirstTimeUser = onboardingManager.isFirstTimeUser()
                val progress = onboardingManager.getOnboardingProgress()
                
                if (isFirstTimeUser) {
                    // New user - follow normal onboarding flow
                    val nextStep = progress.getNextStep()
                    
                    val destination = when (nextStep) {
                        OnboardingStep.INTRODUCTION -> NavigationDestinations.OnboardingIntroduction.route
                        OnboardingStep.EQUIPMENT_SETUP -> NavigationDestinations.OnboardingEquipmentSetup.route
                        OnboardingStep.GUIDED_BEAN_CREATION -> NavigationDestinations.OnboardingGuidedBeanCreation.route
                        OnboardingStep.FIRST_SHOT -> NavigationDestinations.RecordShot.route
                        OnboardingStep.COMPLETED, null -> {
                            // Edge case: marked as first-time but progress shows complete
                            // Mark as complete and route to normal flow
                            onboardingManager.markOnboardingComplete()
                            NavigationDestinations.RecordShot.route
                        }
                    }
                    
                    _routingState.value = RoutingState.Success(destination, isFirstTimeUser = true)
                } else {
                    // Existing user - check if they need to complete new equipment setup
                    if (progress.needsEquipmentSetup()) {
                        // Force existing users to go through equipment setup for new features
                        _routingState.value = RoutingState.Success(
                            NavigationDestinations.OnboardingEquipmentSetup.route,
                            isFirstTimeUser = false // Mark as existing user to skip intro/bean creation
                        )
                    } else {
                        // Existing user with up-to-date setup - route to normal app flow
                        _routingState.value = RoutingState.Success(
                            NavigationDestinations.RecordShot.route, 
                            isFirstTimeUser = false
                        )
                    }
                }
            } catch (exception: Exception) {
                // Error handling: graceful fallback to normal flow
                _routingState.value = RoutingState.Error(
                    exception = exception,
                    fallbackRoute = NavigationDestinations.RecordShot.route
                )
            }
        }
    }

    /**
     * Retries the routing determination in case of initial failure.
     */
    fun retryRouting() {
        _routingState.value = RoutingState.Loading
        determineInitialRoute()
    }

    /**
     * Forces routing to normal app flow, bypassing onboarding.
     * Used as a fallback when onboarding encounters issues.
     */
    fun forceNormalFlow() {
        _routingState.value = RoutingState.Success(
            NavigationDestinations.RecordShot.route,
            isFirstTimeUser = false
        )
    }

    /**
     * Marks onboarding as complete explicitly (used when user skips onboarding).
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            try {
                onboardingManager.markOnboardingComplete()
            } catch (_: Exception) {
                // Ignore and let routing logic handle on next launch
            }
        }
    }

    /**
     * Handles equipment setup completion and determines next step based on whether user needs bean creation.
     * Returns true if the user should skip bean creation (they already have beans).
     */
    fun handleEquipmentSetupComplete(onResult: (shouldSkipBeanCreation: Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val progress = onboardingManager.getOnboardingProgress()
                
                // Update progress to mark equipment setup as complete with current version
                val updatedProgress = progress.copy(
                    hasCompletedEquipmentSetup = true,
                    equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
                )
                onboardingManager.updateOnboardingProgress(updatedProgress)
                
                // Check if user has existing beans - if so, they can skip bean creation
                val beanCountResult = beanRepository.getActiveBeanCount()
                val hasExistingBeans = beanCountResult.getOrNull() ?: 0 > 0
                
                if (hasExistingBeans) {
                    // User with beans - mark bean creation as complete and skip to main app
                    val finalProgress = updatedProgress.copy(hasCreatedFirstBean = true)
                    onboardingManager.updateOnboardingProgress(finalProgress)
                    onResult(true) // Skip bean creation
                } else {
                    // User without beans - needs to go through bean creation
                    onResult(false) // Don't skip bean creation
                }
            } catch (exception: Exception) {
                // On error, assume they need bean creation to be safe
                onResult(false)
            }
        }
    }
    
    /**
     * Handles equipment setup skip and determines next step based on whether user needs bean creation.
     * Returns true if the user should skip bean creation (they already have beans).
     */
    fun handleEquipmentSetupSkip(onResult: (shouldSkipBeanCreation: Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val progress = onboardingManager.getOnboardingProgress()
                
                // Even when skipping, mark equipment setup as complete with current version
                // so they don't get forced through it again
                val updatedProgress = progress.copy(
                    hasCompletedEquipmentSetup = true,
                    equipmentSetupVersion = OnboardingProgress.CURRENT_EQUIPMENT_SETUP_VERSION
                )
                onboardingManager.updateOnboardingProgress(updatedProgress)
                
                // Check if user has existing beans - if so, they can skip bean creation
                val beanCountResult = beanRepository.getActiveBeanCount()
                val hasExistingBeans = beanCountResult.getOrNull() ?: 0 > 0
                
                if (hasExistingBeans) {
                    // User with beans - mark bean creation as complete and skip to main app
                    val finalProgress = updatedProgress.copy(hasCreatedFirstBean = true)
                    onboardingManager.updateOnboardingProgress(finalProgress)
                    onResult(true) // Skip bean creation
                } else {
                    // User without beans - needs to go through bean creation
                    onResult(false) // Don't skip bean creation
                }
            } catch (exception: Exception) {
                // On error, assume they need bean creation to be safe
                onResult(false)
            }
        }
    }

    /**
     * Handles edge cases like app updates and data clearing.
     * This method can be called when the app detects inconsistent state.
     */
    fun handleAppUpdate() {
        viewModelScope.launch {
            try {
                // Check if onboarding state is corrupted or inconsistent
                val progress = onboardingManager.getOnboardingProgress()
                val isFirstTimeUser = onboardingManager.isFirstTimeUser()
                
                // If user is marked as first-time but has completed all steps,
                // mark onboarding as complete
                if (isFirstTimeUser && progress.isComplete()) {
                    onboardingManager.markOnboardingComplete()
                    _routingState.value = RoutingState.Success(
                        NavigationDestinations.RecordShot.route,
                        isFirstTimeUser = false
                    )
                } else {
                    // Re-determine routing based on current state
                    determineInitialRoute()
                }
            } catch (exception: Exception) {
                // If we can't determine state, default to normal flow
                _routingState.value = RoutingState.Error(
                    exception = exception,
                    fallbackRoute = NavigationDestinations.RecordShot.route
                )
            }
        }
    }
}

/**
 * Represents the different states of the routing determination process.
 */
sealed class RoutingState {
    /**
     * Initial state while determining the appropriate route.
     */
    object Loading : RoutingState()

    /**
     * Successfully determined the route.
     * 
     * @param route The navigation route to use
     * @param isFirstTimeUser Whether this is a first-time user
     */
    data class Success(
        val route: String,
        val isFirstTimeUser: Boolean
    ) : RoutingState()

    /**
     * Error occurred during routing determination.
     * 
     * @param exception The exception that occurred
     * @param fallbackRoute The route to use as fallback
     */
    data class Error(
        val exception: Exception,
        val fallbackRoute: String
    ) : RoutingState()
}