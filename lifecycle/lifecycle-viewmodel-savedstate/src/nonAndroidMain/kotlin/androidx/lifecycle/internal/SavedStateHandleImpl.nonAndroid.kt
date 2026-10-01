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

package androidx.lifecycle.internal

import androidx.savedstate.SavedState
import androidx.savedstate.read

internal actual fun <T> unwrapSavedStateValue(value: T): T {
    if (value is SavedState && value.read { contains(SAVED_STATE_VALUE_KEY) }) {
        @Suppress("UNCHECKED_CAST")
        return value.read { toMap()[SAVED_STATE_VALUE_KEY] } as T
    }
    return value
}

internal actual fun isAcceptableType(value: Any?): Boolean =
    when (value) {
        null -> true

        // Scalars
        is Boolean,
        is Byte,
        is Char,
        is Double,
        is Float,
        is Int,
        is Long,
        is Short -> true

        // References
        is SavedState,
        is String,
        is CharSequence -> true

        // Scalar arrays
        is BooleanArray,
        is ByteArray,
        is CharArray,
        is DoubleArray,
        is FloatArray,
        is IntArray,
        is LongArray,
        is ShortArray -> true

        // Reference arrays
        // [bundleOf] might support [List] instead of [ArrayList] in some cases.
        is List<*> -> {
            // Unlike JVM, there is no reflection available to check component type
            when (value.firstOrNull()) {
                is Int,
                is String -> true
                else -> value.isEmpty()
            }
        }
        else -> false
    }
