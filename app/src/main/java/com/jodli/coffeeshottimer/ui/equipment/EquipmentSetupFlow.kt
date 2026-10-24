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
