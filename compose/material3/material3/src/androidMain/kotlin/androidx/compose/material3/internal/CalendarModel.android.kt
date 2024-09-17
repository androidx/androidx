/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3.internal

import android.icu.text.DateFormat
import android.icu.text.DisplayContext
import android.icu.util.TimeZone
import android.os.Build
import androidx.compose.material3.CalendarLocale
import java.util.Date

/** Returns a [CalendarModel] to be used by the date picker. */
internal actual fun createCalendarModel(locale: CalendarLocale): CalendarModel {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        CalendarModelImpl(locale)
    } else {
        LegacyCalendarModelImpl(locale)
    }
}

/**
 * Formats a UTC timestamp into a string with a given date format skeleton.
 *
 * A skeleton is similar to, and uses the same format characters as described in
 * [Unicode Technical Standard #35](https://unicode.org/reports/tr35/tr35-dates.html#Date_Field_Symbol_Table)
 *
 * One difference is that order is irrelevant. For example, "MMMMd" will return "MMMM d" in the
 * en_US locale, but "d. MMMM" in the de_CH locale.
 *
 * @param utcTimeMillis a UTC timestamp to format (milliseconds from epoch)
 * @param skeleton a date format skeleton
 * @param locale the [CalendarLocale] to use when formatting the given timestamp
 * @param cache a [MutableMap] for caching formatter related results for better performance
 */
internal actual fun formatWithSkeleton(
    utcTimeMillis: Long,
    skeleton: String,
    locale: CalendarLocale,
    cache: MutableMap<String, Any>
): String {
    // Use ICU to format the time on API 24+.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val instanceForSkeleton =
            cache.getOrPut(key = "S:$skeleton${locale.toLanguageTag()}") {
                val instanceForSkeleton = DateFormat.getInstanceForSkeleton(skeleton, locale)
                instanceForSkeleton.setContext(DisplayContext.CAPITALIZATION_FOR_STANDALONE)
                // Note: ICU uses the terms GMT and UTC interchangeably, as it does not handle leap
                // seconds or historical behavior.
                instanceForSkeleton.timeZone = TimeZone.GMT_ZONE
                instanceForSkeleton
            } as DateFormat
        return instanceForSkeleton.format(Date(utcTimeMillis))
    }
    // On older APIs, resolve the best pattern from the skeleton and format the time using the
    // pattern and the supported CalendarModel.
    // For the caching, prepend the skeleton and language tag with a "S" to avoid cache collisions
    // by marking this as a cached result for a skeleton.
    val pattern =
        cache
            .getOrPut(key = "S:$skeleton${locale.toLanguageTag()}") {
                android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
            }
            .toString()

    return LegacyCalendarModelImpl.formatWithPattern(utcTimeMillis, pattern, locale, cache)
}

/**
 * Receives a given local date format string and returns a string that can be displayed to the user
 * and parsed by the date parser.
 *
 * This function:
 * - Removes all characters that don't match `d`, `M` and `y`, or any of the date format delimiters
 *   `.`, `/` and `-`.
 * - Ensures that the format is for two digits day and month, and four digits year.
 *
 * The output of this cleanup is always a 10 characters string in one of the following variations:
 * - yyyy/MM/dd
 * - yyyy-MM-dd
 * - yyyy.MM.dd
 * - dd/MM/yyyy
 * - dd-MM-yyyy
 * - dd.MM.yyyy
 * - MM/dd/yyyy
 */
internal actual fun datePatternAsInputFormat(localeFormat: String): DateInputFormat {
    val patternWithDelimiters =
        localeFormat
            .replace(Regex("[^dMy/\\-.]"), "")
            .replace(Regex("d{1,2}"), "dd")
            .replace(Regex("M{1,2}"), "MM")
            .replace(Regex("y{1,4}"), "yyyy")
            .replace("My", "M/y") // Edge case for the Kako locale
            .removeSuffix(".") // Removes a dot suffix that appears in some formats

    val delimiterRegex = Regex("[/\\-.]")
    val delimiterMatchResult = delimiterRegex.find(patternWithDelimiters)
    val delimiterIndex = delimiterMatchResult!!.groups[0]!!.range.first
    val delimiter = patternWithDelimiters.substring(delimiterIndex, delimiterIndex + 1)
    return DateInputFormat(patternWithDelimiters = patternWithDelimiters, delimiter = delimiter[0])
}
