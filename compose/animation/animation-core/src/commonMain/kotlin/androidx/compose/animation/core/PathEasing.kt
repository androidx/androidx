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

package androidx.compose.animation.core

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import kotlin.math.absoluteValue

/**
 * An easing function for an arbitrary [Path].
 *
 * The [Path] must begin at `(0, 0)` and end at `(1, 1)`. The x-coordinate along the
 * [Path] is the input value and the output is the y coordinate of the line at that
 * point. This means that the Path must conform to a function `y = f(x)`.
 *
 * The [Path] must not have gaps in the x direction and must not
 * loop back on itself such that there can be two points sharing the same x coordinate.
 *
 * This is equivalent to the Android `PathInterpolator`.
 *
 * [CubicBezierEasing] should be used if a bezier curve is required as it performs less allocations.
 * [PathEasing] should be used when creating an arbitrary path.
 *
 * @sample androidx.compose.animation.core.samples.PathEasingSample
 *
 * @param path The path to use to make the line representing the Easing Curve.
 *
 */
@Immutable
class PathEasing(path: Path) : Easing {

    private val offsetX: FloatArray
    private val offsetY: FloatArray

    init {
        val pathMeasure = PathMeasure()
        pathMeasure.setPath(path, false)

        val pathLength: Float = pathMeasure.length
        require(pathLength > 0) {
            "Path cannot be zero in length. " +
                "Ensure that supplied Path starts at [0,0] and ends at [1,1]"
        }
        val numPoints: Int =
            (pathLength / Precision).toInt() + 1

        offsetX = FloatArray(numPoints) { 0f }
        offsetY = FloatArray(numPoints) { 0f }

        for (i in 0 until numPoints) {
            val distance = i * pathLength / (numPoints - 1)
            val offset = pathMeasure.getPosition(distance)
            offsetX[i] = offset.x
            offsetY[i] = offset.y
            if (i > 0 && offsetX[i] < offsetX[i - 1]) {
                throw IllegalArgumentException("Path needs to be continuously increasing")
            }
        }
    }

    override fun transform(fraction: Float): Float {
        if (fraction <= 0.0f) {
            return 0.0f
        } else if (fraction >= 1.0f) {
            return 1.0f
        }

        // Do a binary search for the correct x to interpolate between.
        val startIndex = offsetX.binarySearch(fraction)
        // the index will be negative if an exact match is not found,
        // so return the exact item if the index is positive.
        if (startIndex > 0) {
            return offsetY[startIndex]
        }
        val insertionStartIndex = startIndex.absoluteValue
        if (insertionStartIndex >= offsetX.size - 1) {
            return offsetY.last()
        }
        val endIndex: Int = insertionStartIndex + 1

        val xRange: Float = offsetX[endIndex] - offsetX[insertionStartIndex]

        val tInRange: Float = fraction - offsetX[insertionStartIndex]
        val newFraction = tInRange / xRange

        val startY: Float = offsetY[insertionStartIndex]
        val endY: Float = offsetY[endIndex]

        return startY + newFraction * (endY - startY)
    }
}

/**
 * Governs the accuracy of the approximation of [PathEasing].
 */
private const val Precision = 0.002f
