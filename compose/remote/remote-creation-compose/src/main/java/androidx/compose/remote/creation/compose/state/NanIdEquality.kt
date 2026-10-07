/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.state

import androidx.compose.remote.core.operations.Utils

// RemoteCompose encodes variable ids and RPN operators as NaN payloads. FloatArray.contentEquals
// and FloatArray.contentHashCode canonicalize every NaN, so two expressions that only differ in a
// referenced id or operator (e.g. [a, b, ADD] and [a, b, MUL]) would be considered equal. These
// helpers compare NaNs by the id returned from Utils.idFromNan, and all other values by their raw
// bits so that 0f and -0f stay distinct.

private fun nanIdEquals(a: Float, b: Float): Boolean {
    val aIsNan = a.isNaN()
    if (aIsNan != b.isNaN()) return false
    return if (aIsNan) {
        Utils.idFromNan(a) == Utils.idFromNan(b)
    } else {
        a.toRawBits() == b.toRawBits()
    }
}

private fun nanIdHashCode(value: Float): Int =
    if (value.isNaN()) Utils.idFromNan(value) else value.toRawBits()

/**
 * Returns true if this array and [other] have the same size and every element encodes the same
 * RemoteCompose value. NaN-encoded ids are compared by [Utils.idFromNan]. Two null arrays are
 * equal.
 */
internal fun FloatArray?.nanIdContentEquals(other: FloatArray?): Boolean {
    if (this === other) return true
    if (this == null || other == null) return false
    if (size != other.size) return false
    for (i in indices) {
        if (!nanIdEquals(this[i], other[i])) return false
    }
    return true
}

/** Returns a hash code for this array that is consistent with [nanIdContentEquals]. */
internal fun FloatArray?.nanIdContentHashCode(): Int {
    if (this == null) return 0
    var result = 1
    for (value in this) {
        result = 31 * result + nanIdHashCode(value)
    }
    return result
}
