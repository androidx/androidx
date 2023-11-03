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
import java.util.Locale
import java.util.WeakHashMap

/* Copy of androidx.compose.material.ActualJvm, mirrored from Foundation. This is used for the
   M2/M3-internal copy of MutatorMutex.
 */
internal actual typealias InternalAtomicReference<V> =
    java.util.concurrent.atomic.AtomicReference<V>

/**
 * Represents a Locale for the calendar. This locale will be used when formatting dates, determining
 * the input format, and more.
 */
actual typealias CalendarLocale = Locale

/**
 * Returns a string representation of an integer for the current Locale.
 */
internal actual fun Int.toLocalString(
    minDigits: Int,
    maxDigits: Int,
    isGroupingUsed: Boolean
): String {
    return getCachedDateTimeFormatter(
        minDigits = minDigits,
        maxDigits = maxDigits,
        isGroupingUsed = isGroupingUsed
    ).format(this)
}

private val cachedFormatters = WeakHashMap<String, NumberFormat>()
private fun getCachedDateTimeFormatter(
    minDigits: Int,
    maxDigits: Int,
    isGroupingUsed: Boolean
): NumberFormat {
    // Note: Using Locale.getDefault() as a best effort to obtain a unique key and keeping this
    // function non-composable.
    val key = "$minDigits.$maxDigits.$isGroupingUsed.${Locale.getDefault().toLanguageTag()}"
    return cachedFormatters.getOrPut(key) {
        NumberFormat.getIntegerInstance().apply {
            this.isGroupingUsed = isGroupingUsed
            this.minimumIntegerDigits = minDigits
            this.maximumIntegerDigits = maxDigits
        }
    }
}
