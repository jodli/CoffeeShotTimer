package com.jodli.coffeeshottimer.data.model

/**
 * Preset basket configurations for common basket sizes
 */
enum class BasketPreset(
    val displayName: String,
    val coffeeInMin: Float,
    val coffeeInMax: Float,
    val coffeeOutMin: Float,
    val coffeeOutMax: Float
) {
    SINGLE(
        displayName = "Single",
        coffeeInMin = 7f,
        coffeeInMax = 12f,
        coffeeOutMin = 14f,
        coffeeOutMax = 30f
    ),
    DOUBLE(
        displayName = "Double",
        coffeeInMin = 14f,
        coffeeInMax = 22f,
        coffeeOutMin = 28f,
        coffeeOutMax = 55f
    );

    fun toBasketConfiguration(): BasketConfiguration {
        return BasketConfiguration(
            coffeeInMin = coffeeInMin,
            coffeeInMax = coffeeInMax,
            coffeeOutMin = coffeeOutMin,
            coffeeOutMax = coffeeOutMax,
            isActive = true
        )
    }
}
