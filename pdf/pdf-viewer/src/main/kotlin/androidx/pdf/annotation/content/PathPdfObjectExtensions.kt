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

package androidx.pdf.annotation.content

import android.graphics.Path
import androidx.annotation.RestrictTo

/** Tolerance level for path approximation. */
private const val ACCEPTABLE_TOLERANCE_IN_PATH = 0.5f

/**
 * Creates a [Path] object from a list of [PathPdfObject.PathInput] points.
 *
 * @return A [Path] object constructed from the input points. Returns an empty Path if the input
 *   list is empty.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun List<PathPdfObject.PathInput>.getPathFromPathInputs(): Path {
    val path = Path()
    forEach { pathInput ->
        if (pathInput.command == PathPdfObject.PathInput.MOVE_TO) {
            path.moveTo(pathInput.x, pathInput.y)
        } else if (pathInput.command == PathPdfObject.PathInput.LINE_TO) {
            path.lineTo(pathInput.x, pathInput.y)
        }
    }
    return path
}

/**
 * Creates a list of [PathPdfObject.PathInput] points from [Path] object.
 *
 * @return A list of [PathPdfObject.PathInput] constructed from the path object. Returns an empty
 *   list if the path is empty.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Path.getPathInputsFromPath(): List<PathPdfObject.PathInput> {

    val pathInputs = mutableListOf<PathPdfObject.PathInput>()

    // Approx array for a path is in the format [fraction, x0, y0, fraction, x1, y1...]
    val approx: FloatArray = this.approximate(ACCEPTABLE_TOLERANCE_IN_PATH)

    for (i in 0 until approx.size step 3) {
        // Determine the operation command based on the approximation array.
        // approx[i] is the fractional distance along the path.
        // If it equals the previous point's fraction, it indicates a new contour (MOVE).
        val command =
            if (i == 0 || approx[i] == approx[i - 3]) {
                PathPdfObject.PathInput.MOVE_TO
            } else {
                PathPdfObject.PathInput.LINE_TO
            }

        pathInputs.add(PathPdfObject.PathInput(approx[i + 1], approx[i + 2], command))
    }
    return pathInputs
}
