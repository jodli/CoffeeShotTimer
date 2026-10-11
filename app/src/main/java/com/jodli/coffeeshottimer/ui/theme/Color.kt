package com.jodli.coffeeshottimer.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.jodli.coffeeshottimer.ui.components.ValidationUtils

// ============================================================================
// Control chart palette, shared with the website (see DESIGN.md)
// One ink on graph paper. Yellow, green and rust only ever mean
// sour/fast, ideal and bitter/slow.
// ============================================================================

// Light: ink on graph paper
val Paper = Color(0xFFF6F6F1)
val PaperLow = Color(0xFFF1F1EB)
val Paper2 = Color(0xFFECECE4)
val PaperHigh = Color(0xFFE6E6DD)
val PaperHighest = Color(0xFFE0E0D6)
val Ink = Color(0xFF17202B)
val Ink2 = Color(0xFF4A5361)
val InkTint = Color(0xFFDDE0E4)
val RuleLight = Color(0xFF8A9099)
val PlotGridLight = Color(0xFFD5D6D0)

// Dark: the same sheet at night
val NightPaper = Color(0xFF10161D)
val NightPaperLowest = Color(0xFF0B1015)
val NightPaperLow = Color(0xFF141B23)
val NightPaper2 = Color(0xFF18202A)
val NightPaperHigh = Color(0xFF1E2732)
val NightPaperHighest = Color(0xFF253040)
val NightInk = Color(0xFFE8E8E1)
val NightInk2 = Color(0xFFA7B0BC)
val NightInkTint = Color(0xFF2B3542)
val RuleDark = Color(0xFF6B7480)
val PlotGridDark = Color(0xFF2C3542)

// Zones
val Sour = Color(0xFFE9C23B)
val SourContainerLight = Color(0xFFF7E9B5)
val SourContainerDark = Color(0xFF4A3D0E)
val Ideal = Color(0xFF3E8E5A)
val IdealInk = Color(0xFF2A6B41)
val IdealNight = Color(0xFF5DB37B)
val IdealInkNight = Color(0xFF7FCB98)
val IdealContainerLight = Color(0xFFD6E8DC)
val IdealContainerDark = Color(0xFF1F4A2E)
val Bitter = Color(0xFF8E3B2A)
val BitterNight = Color(0xFFC2614B)
val BitterContainerLight = Color(0xFFF0D9D3)
val BitterContainerDark = Color(0xFF4A2219)

val ErrorLight = Color(0xFFBA1A1A)
val ErrorDark = Color(0xFFFFB4AB)

/** Where an extraction time falls relative to the 25-30 s window the recommendations use. */
enum class ExtractionZone { FAST, IDEAL, SLOW }

fun extractionZone(seconds: Int): ExtractionZone = when {
    seconds < ValidationUtils.OPTIMAL_EXTRACTION_TIME_MIN -> ExtractionZone.FAST
    seconds <= ValidationUtils.OPTIMAL_EXTRACTION_TIME_MAX -> ExtractionZone.IDEAL
    else -> ExtractionZone.SLOW
}

// quality score tiers, same cut-offs as the score labels
private const val SCORE_GOOD = 85
private const val SCORE_FAIR = 60

/**
 * Meaningful colours outside Material's roles: the three extraction zones,
 * and a single-hue scale for quality scores so a "fair" score never borrows
 * the sour or bitter colour.
 */
@Immutable
data class ZoneColors(
    val fast: Color,
    val fastInk: Color,
    val fastContainer: Color,
    val ideal: Color,
    val idealInk: Color,
    val idealContainer: Color,
    val slow: Color,
    val slowInk: Color,
    val slowContainer: Color,
    val idle: Color,
    val qualityFair: Color
) {
    val qualityGood: Color get() = ideal
    val qualityPoor: Color get() = idle

    /** Fill colour, for rings, dots and bars. */
    fun fill(zone: ExtractionZone): Color = when (zone) {
        ExtractionZone.FAST -> fast
        ExtractionZone.IDEAL -> ideal
        ExtractionZone.SLOW -> slow
    }

    /** Text colour; the fills are too light to read as text on paper. */
    fun ink(zone: ExtractionZone): Color = when (zone) {
        ExtractionZone.FAST -> fastInk
        ExtractionZone.IDEAL -> idealInk
        ExtractionZone.SLOW -> slowInk
    }

    fun container(zone: ExtractionZone): Color = when (zone) {
        ExtractionZone.FAST -> fastContainer
        ExtractionZone.IDEAL -> idealContainer
        ExtractionZone.SLOW -> slowContainer
    }

    fun forScore(score: Int): Color = when {
        score >= SCORE_GOOD -> qualityGood
        score >= SCORE_FAIR -> qualityFair
        else -> qualityPoor
    }
}

val LightZoneColors = ZoneColors(
    fast = Sour,
    fastInk = Color(0xFF7A5E00),
    fastContainer = SourContainerLight,
    ideal = Ideal,
    idealInk = IdealInk,
    idealContainer = IdealContainerLight,
    slow = Bitter,
    slowInk = Bitter,
    slowContainer = BitterContainerLight,
    idle = RuleLight,
    qualityFair = Color(0xFF8DB89B)
)

val DarkZoneColors = ZoneColors(
    fast = Sour,
    fastInk = Sour,
    fastContainer = SourContainerDark,
    ideal = IdealNight,
    idealInk = IdealInkNight,
    idealContainer = IdealContainerDark,
    slow = BitterNight,
    slowInk = Color(0xFFE08A74),
    slowContainer = BitterContainerDark,
    idle = RuleDark,
    qualityFair = Color(0xFF3F7552)
)

val LocalZoneColors = staticCompositionLocalOf { LightZoneColors }
