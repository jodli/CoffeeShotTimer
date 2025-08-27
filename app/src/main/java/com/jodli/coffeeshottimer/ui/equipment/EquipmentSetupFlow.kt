package com.jodli.coffeeshottimer.ui.equipment

/**
 * Represents the different steps in the equipment setup flow
 */
enum class EquipmentSetupStep {
    WELCOME,
    GRINDER_SETUP,
    BASKET_SETUP,
    SUMMARY
}

/**
 * Data class to hold the state of the entire equipment setup flow
 */
data class EquipmentSetupFlowState(
    val currentStep: EquipmentSetupStep = EquipmentSetupStep.WELCOME,
    val grinderConfig: GrinderConfigState = GrinderConfigState(),
    val basketConfig: BasketConfigState = BasketConfigState(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * State for grinder configuration step
 */
data class GrinderConfigState(
    val scaleMin: String = "",
    val scaleMax: String = "",
    val minError: String? = null,
    val maxError: String? = null,
    val generalError: String? = null,
    val isValid: Boolean = false
)

/**
 * State for basket configuration step
 */
data class BasketConfigState(
    val coffeeInMin: String = "",
    val coffeeInMax: String = "",
    val coffeeOutMin: String = "",
    val coffeeOutMax: String = "",
    val coffeeInMinError: String? = null,
    val coffeeInMaxError: String? = null,
    val coffeeOutMinError: String? = null,
    val coffeeOutMaxError: String? = null,
    val generalError: String? = null,
    val isValid: Boolean = false
)

/**
 * Utility functions for flow navigation
 */
object EquipmentSetupFlowNavigator {
    
    fun canNavigateForward(state: EquipmentSetupFlowState): Boolean {
        return when (state.currentStep) {
            EquipmentSetupStep.WELCOME -> true
            EquipmentSetupStep.GRINDER_SETUP -> state.grinderConfig.isValid
            EquipmentSetupStep.BASKET_SETUP -> state.basketConfig.isValid
            EquipmentSetupStep.SUMMARY -> true
        }
    }
    
    fun canNavigateBackward(state: EquipmentSetupFlowState): Boolean {
        return state.currentStep != EquipmentSetupStep.WELCOME
    }
    
    fun getNextStep(currentStep: EquipmentSetupStep): EquipmentSetupStep? {
        return when (currentStep) {
            EquipmentSetupStep.WELCOME -> EquipmentSetupStep.GRINDER_SETUP
            EquipmentSetupStep.GRINDER_SETUP -> EquipmentSetupStep.BASKET_SETUP
            EquipmentSetupStep.BASKET_SETUP -> EquipmentSetupStep.SUMMARY
            EquipmentSetupStep.SUMMARY -> null
        }
    }
    
    fun getPreviousStep(currentStep: EquipmentSetupStep): EquipmentSetupStep? {
        return when (currentStep) {
            EquipmentSetupStep.WELCOME -> null
            EquipmentSetupStep.GRINDER_SETUP -> EquipmentSetupStep.WELCOME
            EquipmentSetupStep.BASKET_SETUP -> EquipmentSetupStep.GRINDER_SETUP
            EquipmentSetupStep.SUMMARY -> EquipmentSetupStep.BASKET_SETUP
        }
    }
    
    fun getStepNumber(step: EquipmentSetupStep): Int {
        return when (step) {
            EquipmentSetupStep.WELCOME -> 1
            EquipmentSetupStep.GRINDER_SETUP -> 2
            EquipmentSetupStep.BASKET_SETUP -> 3
            EquipmentSetupStep.SUMMARY -> 4
        }
    }
    
    fun getTotalSteps(): Int = 4
}
