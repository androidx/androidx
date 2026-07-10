/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.watchface.complications.data.formatting

import android.content.Context
import android.icu.util.TimeZone
import androidx.test.core.app.ApplicationProvider
import androidx.wear.watchface.complications.data.SharedRobolectricTestRunner
import androidx.wear.watchface.complications.data.TimeFormatComplicationText
import com.google.common.truth.Truth
import java.time.Instant
import java.util.Locale
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(SharedRobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class ComplicationTextFormattingTest {

    @Test
    fun time24HourUsesCustomFormatIfNotTooLong() {
        // In ShadowDateFormat, best time format for Norwegian is set to return with a dot instead
        // of a colon (which matches the actual return value for that language).
        Truth.assertThat(
                ComplicationTextFormatting(Locale("no"), FakeDateFormat()).shortTextTimeFormat24Hour
            )
            .isEqualTo("HH.mm")
    }

    @Test
    fun time24HourUsesStandardExpectedFormatIfNotTooLong() {
        Truth.assertThat(
                ComplicationTextFormatting(Locale("en", "GB"), FakeDateFormat())
                    .shortTextTimeFormat24Hour
            )
            .isEqualTo("HH:mm")
    }

    @Test
    fun time12HourRemovesSpaceBeforeAmPmIfNecessary() {
        // Default is "h:mm a" but that can be 8 characters
        Truth.assertThat(
                ComplicationTextFormatting(Locale("en", "US"), FakeDateFormat())
                    .shortTextTimeFormat12Hour
            )
            .isEqualTo("h:mma")
    }

    @Test
    fun time12HourStripsAmPmIfTooLong() {
        // Czech am/pm is "odp./dop." which are too long to fit after the time.
        Truth.assertThat(
                ComplicationTextFormatting(Locale("cs"), FakeDateFormat()).shortTextTimeFormat12Hour
            )
            .isEqualTo("h:mm")
    }

    @Test
    fun time12HourDoesNotStripAmPmIfTooLongWhenShorteningAmPmAlowed() {
        // Spanish am/pm is "a. m." or "p. m." which is too long to fit after the time-part
        // unless they are allowed to be stripped of spaces and periods.
        Truth.assertThat(
                ComplicationTextFormatting(Locale("es"), FakeDateFormat()).shortTextTimeFormat12Hour
            )
            .isEqualTo("h:mma")
    }

    @Test
    fun time12HourStripsAmPmIfTooLongWhenShorteningAmPmNotAllowed() {
        // Spanish am/pm is "a. m." or "p. m." which is too long to fit after the time-part
        // if stripping the am/pm value of spaces and periods is not allowed.
        Truth.assertThat(
                ComplicationTextFormatting(Locale("es"), FakeDateFormat())
                    .shortTextTimeFormat12HourWithoutAmPmShortening
            )
            .isEqualTo("h:mm")
    }

    @Test
    fun standardDayMonthFormat() {
        Truth.assertThat(
                ComplicationTextFormatting(Locale("en", "GB"), FakeDateFormat())
                    .shortTextDayMonthFormat
            )
            .isEqualTo("d MMM")
    }

    @Test
    fun usDayMonthFormat() {
        Truth.assertThat(
                ComplicationTextFormatting(Locale("en", "US"), FakeDateFormat())
                    .shortTextDayMonthFormat
            )
            .isEqualTo("MMM d")
    }

    @Test
    fun tooLongTextDayMonthFormat() {
        // In ShadowDateFormat, best date format for Brazilian Portuguese is set to return with
        // extra characters that mean it cannot fit within the short text limit. This matches the
        // actual return value for that language.
        Truth.assertThat(
                ComplicationTextFormatting(Locale("pt", "BR"), FakeDateFormat())
                    .shortTextDayMonthFormat
            )
            .isEqualTo("d/MM")
    }

    @Test
    fun vietnameseTextDayMonthFormat() {
        // In Vietnamese, an MMM month can be e.g. "thg 10", which means "d MMM" is too long even
        // though the same format pattern is acceptable in other languages.
        Truth.assertThat(
                ComplicationTextFormatting(Locale("vi"), FakeDateFormat()).shortTextDayMonthFormat
            )
            .isEqualTo("d/MM")
    }

    @Test
    fun germanTextDayMonthFormat() {
        Truth.assertThat(
                ComplicationTextFormatting(Locale("de", "DE"), FakeDateFormat())
                    .shortTextDayMonthFormat
            )
            .isEqualTo("dd.MM.")
    }

    @Test
    fun monthFallback() {
        // In ShadowDateFormat, best date format for this language returns a too-long pattern for
        // any input. Result should be the specified fallback.
        Truth.assertThat(
                ComplicationTextFormatting(Locale("mfe"), FakeDateFormat()).shortTextMonthFormat
            )
            .isEqualTo("MM")
    }

    @Test
    fun standardShortMonthFormat() {
        Truth.assertThat(
                ComplicationTextFormatting(Locale("en", "GB"), FakeDateFormat())
                    .shortTextMonthFormat
            )
            .isEqualTo("LLL")
    }

    @Test
    fun germanShortMonthFormat() {
        // In German, the desired output is e.g. "Jan." instead of "Jan". To get this result, the
        // pattern "MMM" should be used instead of "LLL".
        Truth.assertThat(
                ComplicationTextFormatting(Locale("de"), FakeDateFormat()).shortTextMonthFormat
            )
            .isEqualTo("MMM")
    }

    @Test
    fun standardDayOfMonthFormat() {
        Truth.assertThat(
                ComplicationTextFormatting(Locale("en", "GB"), FakeDateFormat())
                    .shortTextDayOfMonthFormat
            )
            .isEqualTo("dd")
    }

    @Test
    fun finnishDayOfMonthFormat() {
        Truth.assertThat(
                ComplicationTextFormatting(Locale("fi"), FakeDateFormat()).shortTextDayOfMonthFormat
            )
            .isEqualTo("dd.")
    }

    @Test
    fun germanDayOfMonthFormat() {
        Truth.assertThat(
                ComplicationTextFormatting(Locale("de"), FakeDateFormat()).shortTextDayOfMonthFormat
            )
            .isEqualTo("dd.")
    }

    @Test
    fun standardDayOfWeekFormat() {
        Truth.assertThat(
                ComplicationTextFormatting(Locale("en", "GB"), FakeDateFormat())
                    .shortTextDayOfWeekFormat
            )
            .isEqualTo("EEE")
    }

    @Test
    fun fullDayOfWeekFormat() {
        Truth.assertThat(
                ComplicationTextFormatting(Locale("en", "GB"), FakeDateFormat())
                    .fullTextDayOfWeekFormat
            )
            .isEqualTo(FULL_DAY_OF_WEEK_TEXT_SKELETON)
    }

    @Test
    fun fallbackDayOfWeekFormat() {
        Truth.assertThat(
                ComplicationTextFormatting(Locale("mfe"), FakeDateFormat()).shortTextDayOfWeekFormat
            )
            .isEqualTo("EEEEE")
    }

    @Test
    fun japaneseDayOfWeekFormat() {
        Truth.assertThat(
                ComplicationTextFormatting(Locale("ja", "JP"), FakeDateFormat())
                    .shortTextDayOfWeekFormat
            )
            .isEqualTo("EEEE")
    }

    @Test
    fun japaneseDayOfWeekString() {
        val testTime = Instant.ofEpochMilli(1610093242000000L) // Fri, 08 Jan 2021 08:07:22 +0000
        val locale = Locale("ja", "JP")

        val defaultLocale = Locale.getDefault()
        try {
            Locale.setDefault(locale)
            val format =
                ComplicationTextFormatting(locale, FakeDateFormat()).shortTextDayOfWeekFormat
            Truth.assertThat(
                    TimeFormatComplicationText.Builder(format!!)
                        .build()
                        .getTextAt(
                            ApplicationProvider.getApplicationContext<Context>().resources,
                            testTime,
                        )
                        .toString()
                )
                .isEqualTo("金曜日")
        } finally {
            Locale.setDefault(defaultLocale)
        }
    }

    @Test
    fun spanishShortTime12HoursTimeAmPmDoesNotContainSpacesAndPeriods() {
        // With space and periods removed, the am/pm indicator is not too long and will be shown.
        val time = 1648867260000L // 02:41 a. m. GMT
        val gmt = TimeZone.getTimeZone("GMT")
        val complicationTextFormatting =
            ComplicationTextFormatting(Locale("es", "ES"), FakeDateFormat())

        val formattedTime =
            complicationTextFormatting.getFormattedTimeForShortText(time, gmt, use24Hour = false)

        Truth.assertThat(formattedTime).isEqualTo("2:41am")
    }

    @Test
    fun spanishShortTime24HoursTime() {
        val time = 1648867260000L // 02:41 a. m. GMT
        val gmt = TimeZone.getTimeZone("GMT")
        val complicationTextFormatting =
            ComplicationTextFormatting(Locale("es", "ES"), FakeDateFormat())

        val formattedTime =
            complicationTextFormatting.getFormattedTimeForShortText(time, gmt, use24Hour = true)

        Truth.assertThat(formattedTime).isEqualTo("02:41")
    }

    @Test
    fun bulgarianShortTime12HoursTimeDoesNotShowAmPmIndicator() {
        // Even with spaces and periods removed, the formatted time is too long in Bulgarian and the
        // am/pm indicator will not be shown.
        val time = 1648867260000L // 02:41 пр.об. GMT
        val gmt = TimeZone.getTimeZone("GMT")
        val complicationTextFormatting =
            ComplicationTextFormatting(Locale("bg", "BG"), FakeDateFormat())

        val formattedTime =
            complicationTextFormatting.getFormattedTimeForShortText(time, gmt, use24Hour = false)

        Truth.assertThat(formattedTime).isEqualTo("2:41")
    }

    @Test
    fun norwegianShortTime12HoursTimePeriodsAreNotStrippedFromTimePart() {
        val time = 1648867260000L // 2.41 a.m. GMT
        val gmt = TimeZone.getTimeZone("GMT")
        val complicationTextFormatting =
            ComplicationTextFormatting(Locale.forLanguageTag("no"), FakeDateFormat())

        val formattedTime =
            complicationTextFormatting.getFormattedTimeForShortText(time, gmt, use24Hour = false)

        Truth.assertThat(formattedTime).isEqualTo("2.41am")
    }

    @Test
    fun getFormattedDayOfWeekForShortText() {
        val time = 1648867260000L // Saturday GMT
        val gmt = TimeZone.getTimeZone("GMT")
        val complicationTextFormatting =
            ComplicationTextFormatting(Locale("en", "US"), FakeDateFormat())

        val formattedTime = complicationTextFormatting.getFormattedDayOfWeekForShortText(time, gmt)

        Truth.assertThat(formattedTime).isEqualTo("Sat")
    }

    private class FakeDateFormat : WrappedDateFormat() {
        override fun getBestDateTimePattern(locale: Locale, skeleton: String): String {
            // Special case returning too long pattern for any skeleton, to test fallback.
            if (locale.language == "mfe") {
                return "'too long'MMMd"
            }

            return when (skeleton) {
                "HHmm" ->
                    when (locale.language) {
                        "no" -> "HH.mm"
                        else -> "HH:mm"
                    }

                "hhmm" ->
                    when (locale.language) {
                        "no" -> "h.mm a"
                        else -> "hh:mm a"
                    }

                "hmm" ->
                    when (locale.language) {
                        "no" -> "h.mm a"
                        else -> "h:mm a"
                    }

                "MMMd" ->
                    when (locale.country) {
                        "BR" -> "d 'de' MMM"
                        "US" -> "MMM d"
                        "DE" -> "d. MMM"
                        else -> "d MMM"
                    }

                "MMd" ->
                    return when (locale.country) {
                        "US" -> "MM/d"
                        else -> "d/MM"
                    }

                "MMM" -> return "LLL"
                else -> return skeleton
            }
        }
    }

    private companion object {
        private const val FULL_DAY_OF_WEEK_TEXT_SKELETON: String = "EEEE"
    }
}
