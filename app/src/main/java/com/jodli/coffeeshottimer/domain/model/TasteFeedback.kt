package com.jodli.coffeeshottimer.domain.model

/**
 * Primary taste classification for espresso shots
 */
enum class TastePrimary {
    SOUR,    // Under-extracted, acidic
    PERFECT, // Balanced extraction
    BITTER   // Over-extracted, harsh
}

/**
 * Optional secondary taste qualifiers
 */
enum class TasteSecondary {
    WEAK,    // Low intensity, watery
    STRONG   // High intensity, concentrated
}
