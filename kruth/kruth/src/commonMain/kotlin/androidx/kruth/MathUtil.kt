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

package androidx.kruth

import kotlin.math.abs

/**
* Useful math functions
*/

/**
 * Returns true iff [left] and [right] are finite values within [tolerance] of
 * each other. Note that both this method and [notEqualWithinTolerance] returns false if
 * either [left] or [right] is infinite or NaN.
 */
internal fun equalWithinTolerance(left: Double, right: Double, tolerance: Double): Boolean {
    return abs(left - right) <= abs(tolerance)
}

/**
 * Returns true iff [left] and [right] are finite values within [tolerance] of
 * each other. Note that both this method and [notEqualWithinTolerance] returns false if
 * either [left] or [right] is infinite or NaN.
 */
internal fun equalWithinTolerance(left: Float, right: Float, tolerance: Float): Boolean {
    return equalWithinTolerance(left.toDouble(), right.toDouble(), tolerance.toDouble())
}

/**
 * Returns true iff [left] and [right] are values within [tolerance] of each other.
 */
internal fun equalWithinTolerance(left: Long, right: Long, tolerance: Long): Boolean =
    try {
        val absDiff = abs(left subtractExact right)
        0 <= absDiff && absDiff <= abs(tolerance)
    } catch (e: ArithmeticException) {
        // The numbers are so far apart their difference isn't even a long.
        false
    }

private infix fun Long.subtractExact(other: Long): Long {
    val result = this - other
    if ((this xor other) and (this xor result) < 0) {
        throw ArithmeticException("Overflow when subtracting $other flow $this")
    }

    return result
}

/**
 * Returns true iff [left] and [right] are finite values not within [tolerance]
 * of each other. Note that both this method and [equalWithinTolerance] returns false if
 * either [left] or [right] is infinite or NaN.
 */
internal fun notEqualWithinTolerance(left: Double, right: Double, tolerance: Double): Boolean {
    if (!left.isFinite()) return false
    if (!right.isFinite()) return false
    return abs(left - right) > abs(tolerance)
}

/**
 * Returns true iff [left] and [right] are finite values not within [tolerance]
 * of each other. Note that both this method and [equalWithinTolerance] returns false if
 * either [left] or [right] is infinite or NaN.
 */
internal fun notEqualWithinTolerance(left: Float, right: Float, tolerance: Float): Boolean {
    return notEqualWithinTolerance(left.toDouble(), right.toDouble(), tolerance.toDouble())
}
