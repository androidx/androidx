// ktlint-disable filename

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

import java.text.NumberFormat

/* Copy of androidx.compose.material.ActualJvm, mirrored from Foundation. This is used for the
   M2/M3-internal copy of MutatorMutex.
 */
internal actual typealias InternalAtomicReference<V> =
    java.util.concurrent.atomic.AtomicReference<V>

/**
 * Represents a Locale for the calendar. This locale will be used when formatting dates, determining
 * the input format, and more.
 */
actual typealias CalendarLocale = java.util.Locale

/**
 * Returns the default [CalendarLocale].
 */
internal actual fun defaultLocale(): CalendarLocale = java.util.Locale.getDefault()

/**
 * Returns a string representation of an integer for the current Locale.
 */
internal actual fun Int.toLocalString(
    minDigits: Int,
    maxDigits: Int,
    isGroupingUsed: Boolean
): String {
    val formatter = NumberFormat.getIntegerInstance()
    formatter.isGroupingUsed = isGroupingUsed
    formatter.minimumIntegerDigits = minDigits
    formatter.maximumIntegerDigits = maxDigits
    return formatter.format(this)
}
