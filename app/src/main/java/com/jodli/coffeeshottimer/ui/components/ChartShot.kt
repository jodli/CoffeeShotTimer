package com.jodli.coffeeshottimer.ui.components

/** One plotted shot: extraction time in seconds and brew ratio (out/in). */
data class ChartShot(
    val seconds: Int,
    val ratio: Double,
    val number: Int? = null
)
