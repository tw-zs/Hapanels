package com.github.itskenny0.r1ha.ui.i18n

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun formatPanelDate(date: LocalDate, locale: Locale = Locale.getDefault()): String {
    val monthDayPattern = when {
        locale.language == "de" -> "d. MMMM"
        locale.language == "en" && locale.country == "US" -> "MMMM d"
        else -> "d MMMM"
    }
    return "${date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)}, ${date.format(DateTimeFormatter.ofPattern(monthDayPattern, locale))}"
}
