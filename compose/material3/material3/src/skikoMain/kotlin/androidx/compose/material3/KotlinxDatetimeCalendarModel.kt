/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

internal class KotlinxDatetimeCalendarModel : CalendarModel {

    override val today: CalendarDate
        get() = Clock.System.now().toCalendarDate(systemTZ)

    override val firstDayOfWeek: Int
        get() = PlatformDateFormat.firstDayOfWeek

    override val weekdayNames: List<Pair<String, String>>
        get() = weekdayNames(defaultLocale())

    private val systemTZ
        get() = TimeZone.currentSystemDefault()

    fun weekdayNames(locale: CalendarLocale): List<Pair<String, String>> {
        return PlatformDateFormat.weekdayNames(locale) ?: EnglishWeekdaysNames
    }

    override fun getDateInputFormat(locale: CalendarLocale): DateInputFormat {
        return PlatformDateFormat
            .getDateInputFormat(locale)
            .applyCalendarModelStyle()
    }

    override fun getCanonicalDate(timeInMillis: Long): CalendarDate {
        return Instant
            .fromEpochMilliseconds(timeInMillis)
            .toLocalDateTime(TimeZone.UTC)
            .date
            .atStartOfDayIn(TimeZone.UTC)
            .toCalendarDate(TimeZone.UTC)
    }

    override fun getMonth(timeInMillis: Long): CalendarMonth {
        return Instant
            .fromEpochMilliseconds(timeInMillis)
            .toCalendarMonth(TimeZone.UTC)
    }

    override fun getMonth(date: CalendarDate): CalendarMonth {
        return getMonth(date.utcTimeMillis)
    }

    override fun getMonth(year: Int, month: Int): CalendarMonth {
        val instant = LocalDate(
            year = year,
            monthNumber = month,
            dayOfMonth = 1,
        ).atStartOfDayIn(TimeZone.UTC)

        return getMonth(instant.toEpochMilliseconds())
    }

    override fun getDayOfWeek(date: CalendarDate): Int {
        return Instant
            .fromEpochMilliseconds(date.utcTimeMillis)
            .toLocalDateTime(systemTZ)
            .dayOfWeek.isoDayNumber
    }

    override fun plusMonths(from: CalendarMonth, addedMonthsCount: Int): CalendarMonth {
        return Instant
            .fromEpochMilliseconds(from.startUtcTimeMillis)
            .toLocalDateTime(TimeZone.UTC)
            .date
            .plus(DatePeriod(months = addedMonthsCount))
            .atStartOfDayIn(TimeZone.UTC)
            .toCalendarMonth(TimeZone.UTC)
    }

    override fun minusMonths(from: CalendarMonth, subtractedMonthsCount: Int): CalendarMonth {
        return plusMonths(from, -subtractedMonthsCount)
    }

    override fun formatWithPattern(
        utcTimeMillis: Long,
        pattern: String,
        locale: CalendarLocale
    ): String {
        return PlatformDateFormat.formatWithPattern(utcTimeMillis, pattern, locale)
    }

    override fun parse(date: String, pattern: String): CalendarDate? {
        return PlatformDateFormat.parse(date, pattern)
    }

    private fun Instant.toCalendarMonth(
        timeZone : TimeZone
    ) : CalendarMonth {

        val dateTime = toLocalDateTime(timeZone)

        val monthStart = LocalDate(
            year = dateTime.year,
            month = dateTime.month,
            dayOfMonth = 1,
        )

        return CalendarMonth(
            year = dateTime.year,
            month = dateTime.monthNumber,
            numberOfDays = dateTime.month
                .numberOfDays(dateTime.year.isLeapYear()),
            daysFromStartOfWeekToFirstOfMonth = monthStart
                .daysFromStartOfWeekToFirstOfMonth(),
            startUtcTimeMillis = monthStart
                .atStartOfDayIn(TimeZone.UTC)
                .toEpochMilliseconds()
        )
    }

    /**
     * Applies some specific rules to fit the Android one
     * */
    private fun DateInputFormat.applyCalendarModelStyle() : DateInputFormat {

        var pattern = patternWithDelimiters
        // the following checks are the result of testing

        // most of time dateFormat returns dd.MM.y -> we need dd.MM.yyyy
        if (!pattern.contains("yyyy", true)) {

            // it can also return dd.MM.yy because such formats exist so check for it
            while (pattern.contains("yy", true)) {
                pattern = pattern.replace("yy", "y",true)
            }

            pattern = pattern.replace("y", "yyyy",true)
        }

        // it can return M.d -> we need MM.dd
        if ("MM" !in pattern){
            pattern = pattern.replace("M","MM")
        }
        if ("dd" !in pattern){
            pattern = pattern.replace("d", "dd")
        }

        // it can return "yyyy. MM. dd."
        pattern = pattern
            .dropWhile { !it.isLetter() } // remove prefix non-letters
            .dropLastWhile { !it.isLetter() } // remove suffix non-letters
            .filter { it != ' ' } // remove whitespaces

        val delimiter = pattern.first { !it.isLetter() }

        return DateInputFormat(pattern, delimiter)
    }


    private fun LocalDate.daysFromStartOfWeekToFirstOfMonth() =
        (dayOfWeek.isoDayNumber - firstDayOfWeek).let { if (it > 0) it else 7 + it }
}

private val EnglishWeekdaysNames = listOf(
    "Monday" to "Mon",
    "Tuesday" to "Tue",
    "Wednesday" to "Wed",
    "Thursday" to "Thu",
    "Friday" to "Fri",
    "Saturday" to "Sat",
    "Sunday" to "Sun",
)

internal fun Instant.toCalendarDate(
    timeZone : TimeZone
) : CalendarDate {

    val dateTime = toLocalDateTime(timeZone)

    return CalendarDate(
        year = dateTime.year,
        month = dateTime.monthNumber,
        dayOfMonth = dateTime.dayOfMonth,
        utcTimeMillis = toEpochMilliseconds()
    )
}

private fun Int.isLeapYear() = this % 4 == 0 && (this % 100 != 0 || this % 400 == 0)

private fun Month.numberOfDays(isLeap : Boolean) : Int {
    return when(this){
        Month.FEBRUARY -> if (isLeap) 29 else 28
        Month.JANUARY,
        Month.MARCH,
        Month.MAY,
        Month.JULY,
        Month.AUGUST,
        Month.OCTOBER,
        Month.DECEMBER -> 31

        else -> 30
    }
}
