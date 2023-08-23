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

import androidx.compose.ui.text.intl.Locale
import kotlin.js.Date
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

internal actual object PlatformDateFormat {

    actual val firstDayOfWeek: Int
        get() = firstDayOfWeek()

    private const val NUMERIC = "numeric"
    private const val SHORT = "short"
    private const val LONG = "long"
    private const val NARROW = "narrow"

    private val firstDaysOfWeekByRegionCode: Map<String, Int> by lazy {
        listOf(
            7 to listOf("TH", "ET", "SG", "JM", "BT", "IN", "US", "MO", "KE", "DO", "AU", "IL", "AS", "TW", "MZ", "MM", "CN", "PR", "PK", "BD", "NP", "HN", "BR", "HK", "TT", "ZA", "VE", "MT", "PH", "PE", "ID", "DM", "WS", "ZW", "UM", "LA", "BZ", "JP", "SV", "SA", "CO", "GT", "BW", "KR", "PA", "YE", "BS", "MX", "MH", "GU", "PY", "AG", "CA", "KH", "PT", "VI", "NI"),
            6 to listOf("EG", "AF", "SY", "IR", "OM", "IQ", "DZ", "DJ", "AE", "SD", "KW", "JO", "BH", "QA", "LY")
        ).map { (day, tags) -> tags.map { it to day } }.flatten().toMap()
    }

    private val regionsWith12HourFormat by lazy {
        listOf("AE", "AG", "AL", "AS", "AU", "BB", "BD", "BH", "BM", "BN", "BS", "BT", "CA", "CN", "CO", "CY", "DJ", "DM", "DO", "DZ", "EG", "EH", "ER", "ET", "FJ", "FM", "GD", "GH", "GM", "GR", "GU", "GY", "HK", "IN", "IQ", "JM", "JO", "KH", "KI", "KN", "KP", "KR", "KW", "KY", "LB", "LC", "LR", "LS", "LY", "MH", "MO", "MP", "MR", "MW", "MY", "NA", "NZ", "OM", "PA", "PG", "PH", "PK", "PR", "PS", "PW", "QA", "SA", "SB", "SD", "SG", "SL", "SO", "SS", "SY", "SZ", "TC", "TD", "TN", "TO", "TT", "TW", "UM", "US", "VC", "VE", "VG", "VI", "VU", "WS", "YE", "ZM")
    }

    //TODO: replace formatting with kotlinx datetime when supported (see https://github.com/Kotlin/kotlinx-datetime/pull/251)
    actual fun formatWithPattern(
        utcTimeMillis: Long,
        pattern: String,
        locale: CalendarLocale
    ): String {

        val date = Instant
            .fromEpochMilliseconds(utcTimeMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        val jsDate = Date(utcTimeMillis)

        val monthShort = jsDate.toLocaleDateString(
            locales = locale.toLanguageTag(),
            options = dateLocaleOptions {
                month = SHORT
            })

        val monthLong = jsDate.toLocaleDateString(
            locales = locale.toLanguageTag(),
            options = dateLocaleOptions {
                month = LONG
            })

        return pattern
            .replace("yyyy", date.year.toString(), ignoreCase = true)
            .replace("yy", date.year.toString().takeLast(2), ignoreCase = true)
            .replace("MMMM", monthLong)
            .replace("MMM", monthShort)
            .replace("MM", date.monthNumber.toStringWithLeadingZero())
            .replace("M", date.monthNumber.toString())
            .replace("dd", date.dayOfMonth.toStringWithLeadingZero(), ignoreCase = true)
            .replace("d", date.dayOfMonth.toString(), ignoreCase = true)
            .replace("hh", date.hour.toStringWithLeadingZero(), ignoreCase = true)
            .replace("h", date.hour.toString(), ignoreCase = true)
            .replace("ii", date.minute.toStringWithLeadingZero(), ignoreCase = true)
            .replace("i", date.minute.toString(), ignoreCase = true)
            .replace("ss", date.second.toStringWithLeadingZero(), ignoreCase = true)
            .replace("s", date.second.toString(), ignoreCase = true)
    }


    actual fun formatWithSkeleton(
        utcTimeMillis: Long,
        skeleton: String,
        locale: CalendarLocale
    ): String {
        val date = Date(utcTimeMillis)

        return date.toLocaleDateString(
            locales = locale.toLanguageTag(),
            options = dateLocaleOptions {
                year = when {
                    skeleton.contains("y", true) -> NUMERIC
                    else -> undefined
                }
                month = when {
                    skeleton.contains("MMMM", true) -> LONG
                    skeleton.contains("MMM", true) -> SHORT
                    skeleton.contains("M", true) -> NUMERIC
                    else -> undefined
                }
                weekday = when {
                    skeleton.contains("eeee", true) -> LONG
                    skeleton.contains("eee", true) -> SHORT
                    skeleton.contains("e", true) -> NARROW
                    else -> undefined
                }
                day = when {
                    skeleton.contains("d", true) -> NUMERIC
                    else -> undefined
                }
                hour = when {
                    skeleton.contains("h", true) -> NUMERIC
                    else -> undefined
                }
                minute = when {
                    skeleton.contains("i", true) -> NUMERIC
                    else -> undefined
                }
                second = when {
                    skeleton.contains("s", true) -> NUMERIC
                    else -> undefined
                }
            }
        )
    }

    actual fun parse(
        date: String,
        pattern: String
    ): CalendarDate? {
        val year = parseSegment(date, pattern, "yyyy")
            ?: return null

        val month = parseSegment(date, pattern, "mm")
            ?: parseSegment(date, pattern, "m")
            ?: return null

        val day = parseSegment(date, pattern, "dd")
            ?: parseSegment(date, pattern, "d")
            ?: 1

        return LocalDate(
            year, month, day
        ).atStartOfDayIn(TimeZone.UTC)
            .toCalendarDate(TimeZone.UTC)
    }

    private fun parseSegment(date: String, pattern: String, segmentPattern: String): Int? {
        val index = pattern
            .indexOf(segmentPattern, ignoreCase = true)
            .takeIf { it >= 0 } ?: return null

        return date.substring(index, index + segmentPattern.length)
            .toIntOrNull()
    }

    actual fun getDateInputFormat(locale: CalendarLocale): DateInputFormat {

        val date = Date(year = 2000, month = 10, day = 23)

        val shortDate = date.toLocaleDateString(locale.toLanguageTag())

        val delimiter = shortDate.first { !it.isDigit() }

        val pattern = shortDate
            .replace("2000", "yyyy")
            .replace("11", "MM") //10 -> 11 not an error. month is index
            .replace("23", "dd")

        return DateInputFormat(pattern, delimiter)
    }

    actual fun weekdayNames(locale: CalendarLocale): List<Pair<String, String>>? {
        val now = Date.now()

        val week = List(DaysInWeek) {
            Date(now + MillisecondsIn24Hours * it)
        }.sortedBy { it.getDay() } // sunday to saturday

        val mondayToSunday = week.drop(1) + week.first()

        val longAndShortWeekDays = listOf(LONG, SHORT).map { format ->
            mondayToSunday.map {
                it.toLocaleDateString(
                    locales = locale.toLanguageTag(),
                    options = dateLocaleOptions {
                        weekday = format
                    })
            }
        }
        return longAndShortWeekDays[0].zip(longAndShortWeekDays[1])
    }

    private fun firstDayOfWeek(): Int {

        @Suppress("UNUSED_VARIABLE")
        val locale = Locale.current.toLanguageTag()

        return runCatching {
            // unsupported in Firefox
            js("new Intl.Locale(locale).weekInfo.firstDay").unsafeCast<Int>()
        }.getOrElse {
            fallbackFirstDayOfWeek()
        }
    }

    private fun fallbackFirstDayOfWeek() : Int {
        return firstDaysOfWeekByRegionCode[Locale.current.region.uppercase()] ?: 1
    }

    actual fun is24HourFormat(locale: CalendarLocale): Boolean {

        @Suppress("UNUSED_VARIABLE")
        val localeTag = locale.toLanguageTag()

        runCatching {
            // unsupported in Firefox and old browsers
            return js("new Intl.Locale(localeTag).hourCycles.join().indexOf('h2') >= 0")
                .unsafeCast<Boolean>()
        }

        runCatching {
            // unsupported in old browsers
            return js("new Intl.Locale(localeTag).hourCycle.indexOf('h2') >= 0")
                .unsafeCast<Boolean>()
        }

        return fallbackIs24HourFormat(locale)
    }

    private fun fallbackIs24HourFormat(locale: CalendarLocale) : Boolean {
        val region = locale.region.uppercase()
        return regionsWith12HourFormat.binarySearch {
            it.compareTo(region)
        } < 0
    }
}

private fun Int.toStringWithLeadingZero(): String{
    return if (this >= 10) toString() else "0$this"
}
