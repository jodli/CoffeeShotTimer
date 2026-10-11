package com.jodli.coffeeshottimer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jodli.coffeeshottimer.R

// Archivo variable font, same face as the website. The width axis gives three
// voices from one file: wide for display, normal for reading, narrow for data labels.
// API 24/25 can't apply variation settings and fall back to the default instance.

private const val WIDTH_WIDE = 118f
private const val WIDTH_NORMAL = 100f
private const val WIDTH_NARROW = 80f

private val WEIGHTS = listOf(400, 500, 600, 700, 800)

@OptIn(ExperimentalTextApi::class)
private fun archivo(width: Float) = FontFamily(
    WEIGHTS.map { w ->
        Font(
            resId = R.font.archivo,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(
                FontVariation.weight(w),
                FontVariation.width(width)
            )
        )
    }
)

val ArchivoWide = archivo(WIDTH_WIDE)
val ArchivoNormal = archivo(WIDTH_NORMAL)
val ArchivoNarrow = archivo(WIDTH_NARROW)

// tabular figures everywhere: grams, seconds and ratios should line up
private const val TNUM = "tnum"

// trackingEm is relative to font size, like CSS em
private fun style(family: FontFamily, weight: FontWeight, size: Int, line: Int, trackingEm: Float = 0f) =
    TextStyle(
        fontFamily = family,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = (size * trackingEm).sp,
        fontFeatureSettings = TNUM
    )

private fun wide(size: Int, line: Int, trackingEm: Float = -0.01f) =
    style(ArchivoWide, FontWeight.ExtraBold, size, line, trackingEm)

private fun normal(size: Int, line: Int, weight: FontWeight, trackingEm: Float = 0f) =
    style(ArchivoNormal, weight, size, line, trackingEm)

private fun narrow(size: Int, line: Int, trackingEm: Float) =
    style(ArchivoNarrow, FontWeight.SemiBold, size, line, trackingEm)

// headlines keep >= 1.2x line height so German umlauts on capitals don't clip
val Typography = Typography(
    displayLarge = wide(57, 60, -0.025f),
    displayMedium = wide(45, 48, -0.02f),
    displaySmall = wide(36, 40, -0.02f),

    headlineLarge = wide(32, 40),
    headlineMedium = wide(28, 36),
    headlineSmall = wide(24, 30),

    titleLarge = wide(22, 28),
    titleMedium = normal(16, 24, FontWeight.Bold, 0.01f),
    titleSmall = normal(14, 20, FontWeight.Bold),

    bodyLarge = normal(16, 24, FontWeight.Normal, 0.01f),
    bodyMedium = normal(14, 20, FontWeight.Normal, 0.01f),
    bodySmall = normal(12, 16, FontWeight.Normal, 0.015f),

    // labelLarge is the button voice; the smaller labels are data labels
    labelLarge = normal(14, 20, FontWeight.Bold, 0.01f),
    labelMedium = narrow(12, 16, 0.04f),
    labelSmall = narrow(11, 16, 0.04f)
)
