package com.github.itskenny0.r1ha.ui.i18n

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.util.Locale
import org.junit.jupiter.api.Test

class DateTimeFormattingTest {
    @Test
    fun `formats panel date using selected locale`() {
        val date = LocalDate.of(2026, 7, 29)

        assertThat(formatPanelDate(date, Locale.forLanguageTag("pl-PL"))).isEqualTo("środa, 29 lipca")
        assertThat(formatPanelDate(date, Locale.UK)).isEqualTo("Wednesday, 29 July")
        assertThat(formatPanelDate(date, Locale.GERMANY)).isEqualTo("Mittwoch, 29. Juli")
    }
}
