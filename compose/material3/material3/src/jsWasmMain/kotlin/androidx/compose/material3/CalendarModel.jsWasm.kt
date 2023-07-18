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

// TODO(https://github.com/JetBrains/compose-multiplatform/issues/3359) Support CalendarModel

/**
 * Returns a [CalendarModel] to be used by the date picker.
 */
@ExperimentalMaterial3Api
internal actual fun CalendarModel(): CalendarModel =
    throw UnsupportedOperationException("DatePicker isn't supported on Web yet. Follow https://github.com/JetBrains/compose-multiplatform/issues/3359")

/**
 * Formats a UTC timestamp into a string with a given date format skeleton.
 *
 * @param utcTimeMillis a UTC timestamp to format (milliseconds from epoch)
 * @param skeleton a date format skeleton
 * @param locale the [Locale] to use when formatting the given timestamp
 */
@ExperimentalMaterial3Api
internal actual fun formatWithSkeleton(
    utcTimeMillis: Long,
    skeleton: String,
    locale: CalendarLocale
): String {
    throw UnsupportedOperationException("DatePicker isn't supported on Web yet. Follow https://github.com/JetBrains/compose-multiplatform/issues/3359")
}
