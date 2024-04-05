/*
 * Copyright 2024 The Android Open Source Project
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
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

internal actual class PlatformDateFormat actual constructor(private val locale: CalendarLocale) {
    actual val firstDayOfWeek: Int
        get() = firstDayOfWeek()

    private val NUMERIC = "numeric"
    private val SHORT = "short"
    private val LONG = "long"
    private val NARROW = "narrow"

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
    ): String {

        val date = Instant
            .fromEpochMilliseconds(utcTimeMillis)
            .toLocalDateTime(TimeZone.UTC)

        val jsDate = Date(utcTimeMillis.toDouble())

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
    ): String {
        val jsDate = Date(utcTimeMillis.toDouble())

        return jsDate.toLocaleDateString(
            locales = locale.toLanguageTag(),
            options = dateLocaleOptions {
                when {
                    skeleton.contains("y", true) -> NUMERIC
                    else -> null
                }?.also { year = it }

                when {
                    skeleton.contains("MMMM", true) -> LONG
                    skeleton.contains("MMM", true) -> SHORT
                    skeleton.contains("M", true) -> NUMERIC
                    else -> null
                }?.also { month = it }

                when {
                    skeleton.contains("eeee", true) -> LONG
                    skeleton.contains("eee", true) -> SHORT
                    skeleton.contains("e", true) -> NARROW
                    else -> null
                }?.also { weekday = it }

                when {
                    skeleton.contains("d", true) -> NUMERIC
                    else -> null
                }?.also { day = it }

                when {
                    skeleton.contains("h", true) -> NUMERIC
                    else -> null
                }?.also { hour = it }

                when {
                    skeleton.contains("i", true) -> NUMERIC
                    else -> null
                }?.also { minute = it }

                when {
                    skeleton.contains("s", true) -> NUMERIC
                    else -> null
                }?.also { second = it }
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
        ).atTime(Midnight)
            .toInstant(TimeZone.UTC)
            .toCalendarDate(TimeZone.UTC)
    }

    private fun parseSegment(date: String, pattern: String, segmentPattern: String): Int? {
        val index = pattern
            .indexOf(segmentPattern, ignoreCase = true)
            .takeIf { it >= 0 } ?: return null

        return date.substring(index, index + segmentPattern.length)
            .toIntOrNull()
    }

    actual fun getDateInputFormat(): DateInputFormat {

        val date = Date(year = 2000, month = 10, day = 23)

        val shortDate = date.toLocaleDateString(locale.toLanguageTag())

        val pattern = shortDate
            .replace("2000", "yyyy")
            .replace("11", "MM") //10 -> 11 not an error. month is index
            .replace("23", "dd")

        return datePatternAsInputFormat(pattern)
    }

    actual val weekdayNames: List<Pair<String, String>> get() {
        val now = Date.now()

        val week = List(DaysInWeek) {
            Date(now + MillisecondsIn24HoursDouble * it)
        }.sortedBy { it.getDay() } // sunday to saturday

        val mondayToSunday = week.drop(1) + week.first()

        val longAndShortWeekDays = listOf(LONG, NARROW).map { format ->
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
        val locale = Locale.current.toLanguageTag()

        return try {
            // unsupported in Firefox or in older browsers
            // -1 means it's not supported
            getFirstDayOfWeek(locale).also { check(it != -1) }
        } catch (e: Exception) {
            fallbackFirstDayOfWeek()
        }
    }

    private fun fallbackFirstDayOfWeek() : Int {
        return firstDaysOfWeekByRegionCode[locale.region.uppercase()] ?: 1
    }

    actual fun is24HourFormat(): Boolean {
        val localeTag = locale.toLanguageTag()

        return try {
            // unsupported in Firefox or in older browsers
            // -1 means it's not supported
            getIs24HourFormat(localeTag)
                .also { check(it != -1) } == 1
        } catch (e: Exception) {
            fallbackIs24HourFormat(locale)
        }
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

@Suppress("UnsafeCastFromDynamic")
// handle an exception in js because it's not propagated to wasm
internal fun getFirstDayOfWeek(locale: String): Int =
    js("""{ try { 
           return new Intl.Locale(locale).weekInfo.firstDay; 
        } catch (error) { 
           return -1; 
      }}"""
    )


@Suppress("UnsafeCastFromDynamic")
// returns 1 if true, 0 if false, or -1 if failed to detect
internal fun getIs24HourFormat(localeTag: String): Int =
    js("""{
            var locale = new Intl.Locale(localeTag);
            // Check for the hourCycles property first
            if (locale.hourCycles) {
                return locale.hourCycles.includes('h23') || locale.hourCycles.includes('h24') ? 1 : 0;
            }
        
            // Fallback to hourCycle property
            if (locale.hourCycle) {
                return locale.hourCycle === 'h23' || locale.hourCycle === 'h24' ? 1 : 0;
            }
            return -1;
        }"""
    )


/**
 * This is an incomplete declaration of js Date:
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date
 * It's copy-pasted with some modifications from kotlin.js.Date, which is not available for k/wasm.
 * The modifications allow to make it reusable for both k/js and k/wasm.
 *
 * It contains only required methods.
 */
private external class Date() {
    constructor(milliseconds: Double)
    constructor(year: Int, month: Int, day: Int)
    fun getDay(): Int
    fun toLocaleDateString(locales: String, options: LocaleOptions = definedExternally): String

    companion object {
        fun now(): Double
    }

    interface LocaleOptions {
        var localeMatcher: String?
        var timeZone: String?
        var hour12: Boolean?
        var formatMatcher: String?
        var weekday: String?
        var era: String?
        var year: String?
        var month: String?
        var day: String?
        var hour: String?
        var minute: String?
        var second: String?
        var timeZoneName: String?
    }
}

private fun emptyLocaleOptions(): Date.LocaleOptions = js("new Object()")

private inline fun dateLocaleOptions(init: Date.LocaleOptions.() -> Unit): Date.LocaleOptions {
    val result = emptyLocaleOptions()
    init(result)
    return result
}