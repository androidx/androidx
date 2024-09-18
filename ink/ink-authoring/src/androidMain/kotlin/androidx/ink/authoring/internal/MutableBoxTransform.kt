/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.authoring.internal

import android.graphics.Matrix
import androidx.ink.geometry.ImmutableVec
import androidx.ink.geometry.MutableBox

/** Used for temporary calculations. */
private val scratchPoints by threadLocal { FloatArray(8) }

/**
 * Apply a [Matrix] transform to this [MutableBox] and save the result in [destination] (which can
 * be this [MutableBox]). Because [MutableBox] is axis-aligned, this transform may grow the
 * transformed region if the transform involved rotation, such that the entire transformed rectangle
 * fits inside of the result.
 */
internal fun MutableBox.transform(transform: Matrix, destination: MutableBox = this) {
    // Set scratchPoints to the 4 corners of the source rect, alternating between x and y.
    // (min, min)
    scratchPoints[0] = xMin
    scratchPoints[1] = yMin
    // (min, max)
    scratchPoints[2] = xMin
    scratchPoints[3] = yMax
    // (max, min)
    scratchPoints[4] = xMax
    scratchPoints[5] = yMin
    // (max, max)
    scratchPoints[6] = xMax
    scratchPoints[7] = yMax

    // Apply the transform to scratchPoints, updating it in place.
    transform.mapPoints(scratchPoints)

    var newXMin = scratchPoints[0]
    var newYMin = scratchPoints[1]
    var newXMax = scratchPoints[0]
    var newYMax = scratchPoints[1]
    for (xIndex in 2..6 step 2) scratchPoints[xIndex].let {
        when {
            it < newXMin -> newXMin = it
            it > newXMax -> newXMax = it
        }
    }
    for (yIndex in 3..7 step 2) scratchPoints[yIndex].let {
        when {
            it < newYMin -> newYMin = it
            it > newYMax -> newYMax = it
        }
    }
    destination.populateFromTwoPoints(
        ImmutableVec(newXMin, newYMin),
        ImmutableVec(newXMax, newYMax)
    )
}
