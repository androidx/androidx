/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.compose.ui.util

import kotlin.math.roundToLong

/**
 * Linearly interpolate between [start] and [stop] with [fraction] fraction between them.
 */
fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * stop
}

/**
 * Linearly interpolate between [start] and [stop] with [fraction] fraction between them.
 */
fun lerp(start: Int, stop: Int, fraction: Float): Int {
    return start + ((stop - start) * fraction.toDouble()).fastRoundToInt()
}

/**
 * Linearly interpolate between [start] and [stop] with [fraction] fraction between them.
 */
fun lerp(start: Long, stop: Long, fraction: Float): Long {
    return start + ((stop - start) * fraction.toDouble()).roundToLong()
}

/**
 * Returns this float value clamped in the inclusive range defined by
 * [minimumValue] and [maximumValue]. Unlike [Float.coerceIn], the range
 * is not validated: the caller must ensure that [minimumValue] is less than
 * [maximumValue].
 */
@Suppress("NOTHING_TO_INLINE")
inline fun Float.fastCoerceIn(minimumValue: Float, maximumValue: Float) =
    this.fastCoerceAtLeast(minimumValue).fastCoerceAtMost(maximumValue)

/**
 * Ensures that this value is not less than the specified [minimumValue].
 */
@Suppress("NOTHING_TO_INLINE")
inline fun Float.fastCoerceAtLeast(minimumValue: Float): Float {
    return if (this < minimumValue) minimumValue else this
}

/**
 * Ensures that this value is not greater than the specified [maximumValue].
 */
@Suppress("NOTHING_TO_INLINE")
inline fun Float.fastCoerceAtMost(maximumValue: Float): Float {
    return if (this > maximumValue) maximumValue else this
}

/**
 * Returns this double value clamped in the inclusive range defined by
 * [minimumValue] and [maximumValue]. Unlike [Float.coerceIn], the range
 * is not validated: the caller must ensure that [minimumValue] is less than
 * [maximumValue].
 */
@Suppress("NOTHING_TO_INLINE")
inline fun Double.fastCoerceIn(minimumValue: Double, maximumValue: Double) =
    this.fastCoerceAtLeast(minimumValue).fastCoerceAtMost(maximumValue)

/**
 * Ensures that this value is not less than the specified [minimumValue].
 */
@Suppress("NOTHING_TO_INLINE")
inline fun Double.fastCoerceAtLeast(minimumValue: Double): Double {
    return if (this < minimumValue) minimumValue else this
}

/**
 * Ensures that this value is not greater than the specified [maximumValue].
 */
@Suppress("NOTHING_TO_INLINE")
inline fun Double.fastCoerceAtMost(maximumValue: Double): Double {
    return if (this > maximumValue) maximumValue else this
}
