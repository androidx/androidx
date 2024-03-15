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

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle


@OptIn(ExperimentalMaterial3Api::class)
internal actual class PlatformDateFormat actual constructor(private val locale: CalendarLocale) {
    private val delegate = LegacyCalendarModelImpl(locale)

    actual val firstDayOfWeek: Int
        get() = delegate.firstDayOfWeek

    actual fun formatWithPattern(
        utcTimeMillis: Long,
        pattern: String,
    ): String {
        return delegate.formatWithPattern(utcTimeMillis, pattern, locale)
    }

    actual fun formatWithSkeleton(
        utcTimeMillis: Long,
        skeleton: String
    ): String {
        // Note: there is no equivalent in Java for Android's DateFormat.getBestDateTimePattern.
        // The JDK SimpleDateFormat expects a pattern, so the results for unrecognized skeletons will be "2023Jan7",
        // "2023January", etc. in case a skeleton holds an actual ICU skeleton and not a pattern.

        // TODO: support ICU skeleton on JVM
        // Maybe it will be supported in kotlinx.datetime in the future.

        val pattern = when(skeleton){
            DatePickerDefaults.YearAbbrMonthDaySkeleton -> {
                return DateTimeFormatter
                    .ofLocalizedDate(FormatStyle.MEDIUM)
                    .localizedBy(locale)
                    .format(Instant.ofEpochMilli(utcTimeMillis).atOffset(ZoneOffset.UTC))
            }
            DatePickerDefaults.YearMonthWeekdayDaySkeleton -> {
                return DateTimeFormatter
                    .ofLocalizedDate(FormatStyle.FULL)
                    .localizedBy(locale)
                    .format(Instant.ofEpochMilli(utcTimeMillis).atOffset(ZoneOffset.UTC))
            }
            DatePickerDefaults.YearMonthSkeleton -> "LLLL yyyy" // L is a pattern for standalone month (without day)
            else -> skeleton
        }
        return formatWithPattern(utcTimeMillis, pattern)
    }

    actual fun parse(
        date: String,
        pattern: String
    ): CalendarDate? {
        return delegate.parse(date, pattern)
    }

    actual fun getDateInputFormat(): DateInputFormat {
        return delegate.getDateInputFormat(locale)
    }

    // From CalendarModelImpl.android.kt weekdayNames.
    //
    // Legacy model returns short ('Mon') format while newer version returns narrow ('M') format
    actual val weekdayNames: List<Pair<String, String>> get() {
        return DayOfWeek.values().map {
            it.getDisplayName(
                TextStyle.FULL,
                locale
            ) to it.getDisplayName(
                TextStyle.NARROW,
                locale
            )
        }
    }

    // https://android.googlesource.com/platform/frameworks/base/+/jb-release/core/java/android/text/format/DateFormat.java
    //
    // public static boolean is24HourFormat(Context context) -- used by Android date format
    actual fun is24HourFormat() : Boolean {
        val dateFormat = DateFormat.getTimeInstance(DateFormat.LONG, locale)

        if (dateFormat !is SimpleDateFormat)
            return false

        return 'H' in dateFormat.toPattern()
    }
}
