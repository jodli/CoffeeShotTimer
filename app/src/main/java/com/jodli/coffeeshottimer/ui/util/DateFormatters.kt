package com.jodli.coffeeshottimer.ui.util

import android.content.Context
import com.jodli.coffeeshottimer.R
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private const val MAX_DAYS_TO_SHOW = 30

/**
 * Format the last-used date of a bean for display.
 * Returns localized strings like "Used today", "Used yesterday", "Used 3 days ago", etc.
 *
 * @param lastUsedDate The timestamp when the bean was last used, or null if never used
 * @param context Android context for string resources
 * @return Formatted string for display
 */
fun formatLastUsed(
    lastUsedDate: LocalDateTime?,
    context: Context
): String {
    if (lastUsedDate == null) {
        return context.getString(R.string.bean_last_used_never)
    }

    val now = LocalDateTime.now()
    val daysDiff = ChronoUnit.DAYS.between(
        lastUsedDate.toLocalDate(),
        now.toLocalDate()
    ).toInt()

    return when (daysDiff) {
        0 -> context.getString(R.string.bean_last_used_today)
        1 -> context.getString(R.string.bean_last_used_yesterday)
        in 2..MAX_DAYS_TO_SHOW -> context.getString(R.string.bean_last_used_days, daysDiff)
        else -> context.getString(R.string.bean_last_used_never)
    }
}
