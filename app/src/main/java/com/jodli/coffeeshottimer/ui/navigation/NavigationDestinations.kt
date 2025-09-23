package com.jodli.coffeeshottimer.ui.navigation

/**
 * Navigation destinations for the app
 */
sealed class NavigationDestinations(val route: String) {
    object RecordShot : NavigationDestinations("record_shot")
    object ShotHistory : NavigationDestinations("shot_history")
    object BeanManagement : NavigationDestinations("bean_management")
    object More : NavigationDestinations("more")

    // Onboarding destinations
    object OnboardingIntroduction : NavigationDestinations("onboarding_introduction")
    object OnboardingEquipmentSetup : NavigationDestinations("onboarding_equipment_setup")
    object OnboardingGuidedBeanCreation : NavigationDestinations("onboarding_guided_bean_creation")

    // Settings destinations
    object EquipmentSettings : NavigationDestinations("settings_grinder") // Renamed for clarity
    object BasketSettings : NavigationDestinations("settings_basket")
    object About : NavigationDestinations("about")

    // Modal destinations
    object ShotDetails : NavigationDestinations("shot_details/{shotId}") {
        fun createRoute(shotId: String) = "shot_details/$shotId"
    }

    object AddEditBean : NavigationDestinations("add_edit_bean?beanId={beanId}") {
        fun createRoute(beanId: String? = null) =
            if (beanId != null) "add_edit_bean?beanId=$beanId" else "add_edit_bean"
    }
}
