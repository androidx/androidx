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

import android.graphics.Matrix
import android.graphics.Path

/**
 * Transforms a [RoundedPolygon] by the given matrix.
 *
 * @param matrix The matrix by which the polygon is to be transformed
 */
fun RoundedPolygon.transformed(matrix: Matrix):
    RoundedPolygon = transformed {
        val tempArray = FloatArray(2)
        // TODO: Should we have a fast path for when the MutablePoint is array-backed?
        tempArray[0] = x
        tempArray[1] = y
        matrix.mapPoints(tempArray)
        x = tempArray[0]
        y = tempArray[1]
    }

/**
 * Gets a [Path] representation for a [RoundedPolygon] shape.
 *
 * @param path an optional [Path] object which, if supplied, will avoid the function having
 * to create a new [Path] object
 */
@JvmOverloads
fun RoundedPolygon.toPath(path: Path = Path()): Path {
    pathFromCubics(path, cubics)
    return path
}

fun Morph.toPath(progress: Float, path: Path = Path()): Path {
    pathFromCubics(path, asCubics(progress))
    return path
}

private fun pathFromCubics(
    path: Path,
    cubics: List<Cubic>
) {
    var first = true
    path.rewind()
    for (i in 0 until cubics.size) {
        val cubic = cubics[i]
        if (first) {
            path.moveTo(cubic.anchor0X, cubic.anchor0Y)
            first = false
        }
        path.cubicTo(
            cubic.control0X, cubic.control0Y,
            cubic.control1X, cubic.control1Y,
            cubic.anchor1X, cubic.anchor1Y
        )
    }
    path.close()
}
