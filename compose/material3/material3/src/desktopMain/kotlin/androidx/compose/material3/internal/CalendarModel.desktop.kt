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

/**
 * Returns a [CalendarModel] to be used by the date picker.
 */
internal actual fun createCalendarModel(locale: CalendarLocale): CalendarModel =
    LegacyCalendarModelImpl(locale)

/**
 * Formats a UTC timestamp into a string with a given date format skeleton.
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
    // Note: there is no equivalent in Java for Android's DateFormat.getBestDateTimePattern.
    // The JDK SimpleDateFormat expects a pattern, so the results will be "2023Jan7",
    // "2023January", etc. in case a skeleton holds an actual ICU skeleton and not a pattern.
    return LegacyCalendarModelImpl.formatWithPattern(
        utcTimeMillis = utcTimeMillis,
        pattern = skeleton,
        locale = locale,
        cache = cache
    )
}
