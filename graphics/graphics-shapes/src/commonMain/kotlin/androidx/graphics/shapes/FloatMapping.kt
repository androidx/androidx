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

package androidx.graphics.shapes

/**
 * Checks if the given progress is in the given progress range, since progress is in the [0..1)
 * interval, and wraps, there is a special case when progressTo < progressFrom.
 * For example, if the progress range is 0.7 to 0.2, both 0.8 and 0.1 are inside and 0.5 is outside.
 */
internal fun progressInRange(progress: Float, progressFrom: Float, progressTo: Float) =
    if (progressTo >= progressFrom) {
        progress in progressFrom..progressTo
    } else {
        progress >= progressFrom || progress <= progressTo
    }

/**
 * Maps from one set of progress values to another. This is used by DoubleMapper to retrieve the
 * value on one shape that maps to the appropriate value on the other.
 */
internal fun linearMap(xValues: List<Float>, yValues: List<Float>, x: Float): Float {
    require(x in 0f..1f) { "Invalid progress: $x" }
    val segmentStartIndex = xValues.indices.first {
        progressInRange(x, xValues[it], xValues[(it + 1) % xValues.size])
    }
    val segmentEndIndex = (segmentStartIndex + 1) % xValues.size
    val segmentSizeX = positiveModulo(
        xValues[segmentEndIndex] - xValues[segmentStartIndex],
        1f
    )
    val segmentSizeY = positiveModulo(
        yValues[segmentEndIndex] - yValues[segmentStartIndex],
        1f
    )
    val positionInSegment = segmentSizeX.let {
        if (it < 0.001f) 0.5f else positiveModulo(x - xValues[segmentStartIndex], 1f) / it
    }
    return positiveModulo(
        yValues[segmentStartIndex] + segmentSizeY * positionInSegment,
        1f
    )
}

/**
 * DoubleMapper creates mappings from values in the [0..1) source space to values in the [0..1)
 * target space, and back.
 * This mapping is created given a finite list of representative mappings, and this is extended to
 * the whole interval by linear interpolation, and wrapping around.
 * For example, if we have mappings 0.2 to 0.5 and 0.4 to 0.6, then 0.3 (which is in the middle of
 * the source interval) will be mapped to 0.55 (the middle of the targets for the interval), 0.21
 * will map to 0.505, and so on.
 * As a more complete example, if we use x to represent a value in the source space and y for the
 * target space, and given as input the mappings 0 to 0, 0.5 to 0.25, this will create a mapping
 * that:
 * { if x in [0 .. 0.5] } y = x / 2
 * { if x in [0.5 .. 1] } y = 0.25 + (x - 0.5) * 1.5 = x * 1.5 - 0.5
 * The mapping can also be used the other way around (using the mapBack function), resulting in:
 * { if y in [0 .. 0.25] } x = y * 2
 * { if y in [0.25 .. 1] } x = (y + 0.5) / 1.5
 * This is used to create mappings of progress values between the start and end shape, which is then
 * used to insert new curves and match curves overall.
 */
internal class DoubleMapper(vararg mappings: Pair<Float, Float>) {
    private val sourceValues = mappings.map { it.first }
    private val targetValues = mappings.map { it.second }

    init {
        validateProgress(sourceValues)
        validateProgress(targetValues)
    }

    fun map(x: Float) = linearMap(sourceValues, targetValues, x)

    fun mapBack(x: Float) = linearMap(targetValues, sourceValues, x)

    companion object {
        @JvmField
        val Identity = DoubleMapper(
            // We need any 2 points in the (x, x) diagonal, with x in the [0, 1) range,
            // We spread them as much as possible to minimize float errors.
            0f to 0f,
            0.5f to 0.5f
        )
    }
}

internal fun validateProgress(p: List<Float>) {
    require(p.all { it in 0f..1f }) {
        "FloatMapping - Progress outside of range: " + p.joinToString()
    }
    val wraps = (1 until p.size).count { p[it] < p[it - 1] }
    require(wraps <= 1) {
        "FloatMapping - Progress wraps more than once: " + p.joinToString()
    }
}
