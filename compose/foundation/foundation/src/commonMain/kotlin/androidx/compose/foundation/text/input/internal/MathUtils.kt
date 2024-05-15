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

package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.text.selection.containsInclusive
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Adds [this] and [right], and if an overflow occurs returns result of [defaultValue].
 */
internal inline fun Int.addExactOrElse(right: Int, defaultValue: () -> Int): Int {
    val result = this + right
    // HD 2-12 Overflow iff both arguments have the opposite sign of the result
    return if (this xor result and (right xor result) < 0) defaultValue() else result
}

/**
 * Subtracts [right] from [this], and if an overflow occurs returns result of [defaultValue].
 */
internal inline fun Int.subtractExactOrElse(right: Int, defaultValue: () -> Int): Int {
    val result = this - right
    // HD 2-12 Overflow iff the arguments have different signs and
    // the sign of the result is different from the sign of x
    return if (this xor right and (this xor result) < 0) defaultValue() else result
}

/**
 * Returns -1 if this [Offset] is closer to [rect1], 1 if it's closer to [rect2], or 0 if it's
 * equidistant to both. If the point is inside either rectangle, the distance is calculated as zero.
 */
internal fun Offset.findClosestRect(rect1: Rect, rect2: Rect): Int {
    val comparativeDistTo1 = distanceSquaredToClosestCornerFromOutside(rect1)
    val comparativeDistTo2 = distanceSquaredToClosestCornerFromOutside(rect2)
    if (comparativeDistTo1 == comparativeDistTo2) return 0
    return if (comparativeDistTo1 < comparativeDistTo2) -1 else 1
}

/**
 * Calculates the distance from this [Offset] to the nearest point on [rect].
 * Returns 0 if the offset is within [rect].
 */
private fun Offset.distanceSquaredToClosestCornerFromOutside(rect: Rect): Float {
    if (rect.containsInclusive(this)) return 0f
    var distance = Float.MAX_VALUE
    (rect.topLeft - this).getDistanceSquared().let { if (it < distance) distance = it }
    (rect.topRight - this).getDistanceSquared().let { if (it < distance) distance = it }
    (rect.bottomLeft - this).getDistanceSquared().let { if (it < distance) distance = it }
    (rect.bottomRight - this).getDistanceSquared().let { if (it < distance) distance = it }
    return distance
}
