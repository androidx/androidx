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

@file:Suppress("FacadeClassJvmName") // Cannot be updated, the Kt name has been released

package androidx.graphics.shapes

import android.graphics.Matrix
import android.graphics.Path

/**
 * Transforms a [RoundedPolygon] by the given matrix.
 *
 * @param matrix The matrix by which the polygon is to be transformed
 */
fun RoundedPolygon.transformed(matrix: Matrix): RoundedPolygon {
    val tempArray = FloatArray(2)
    return transformed { x, y ->
        tempArray[0] = x
        tempArray[1] = y
        matrix.mapPoints(tempArray)
        TransformResult(tempArray[0], tempArray[1])
    }
}

/**
 * Gets a [Path] representation for a [RoundedPolygon] shape. Note that there is some rounding
 * happening (to the nearest thousandth), to work around rendering artifacts introduced by some
 * points being just slightly off from each other (far less than a pixel). This also allows for a
 * more optimal path, as redundant curves (usually a single point) can be detected and not added to
 * the resulting path.
 *
 * @param path an optional [Path] object which, if supplied, will avoid the function having to
 *   create a new [Path] object
 */
@JvmOverloads
fun RoundedPolygon.toPath(path: Path = Path()): Path {
    pathFromCubics(path, cubics)
    return path
}

fun Morph.toPath(progress: Float, path: Path = Path()): Path {
    // The first/last mechanism here ensures that the final anchor point in the shape
    // exactly matches the first anchor point. There can be rendering artifacts introduced
    // by those points being slightly off, even by much less than a pixel
    path.rewind()

    var firstX = 0f
    var firstY = 0f
    var prevControl0X = 0f
    var prevControl0Y = 0f
    var prevControl1X = 0f
    var prevControl1Y = 0f
    var prevAnchor1X = 0f
    var prevAnchor1Y = 0f
    var first = true
    forEachCubic(progress) {
        if (first) {
            path.moveTo(it.anchor0X, it.anchor0Y)
            firstX = it.anchor0X
            firstY = it.anchor0Y
            first = false
        } else {
            // We delay using the current cubic, because we need to do something special for the
            // last one and we can't detect it is the last one until the loop ends.
            path.cubicTo(
                prevControl0X,
                prevControl0Y,
                prevControl1X,
                prevControl1Y,
                prevAnchor1X,
                prevAnchor1Y,
            )
        }
        prevControl0X = it.control0X
        prevControl0Y = it.control0Y
        prevControl1X = it.control1X
        prevControl1Y = it.control1Y
        prevAnchor1X = it.anchor1X
        prevAnchor1Y = it.anchor1Y
    }
    if (!first) {
        path.cubicTo(prevControl0X, prevControl0Y, prevControl1X, prevControl1Y, firstX, firstY)
    }
    path.close()
    return path
}

private fun pathFromCubics(path: Path, cubics: List<Cubic>) {
    var first = true
    path.rewind()
    for (i in 0 until cubics.size) {
        val cubic = cubics[i]
        if (first) {
            path.moveTo(cubic.anchor0X, cubic.anchor0Y)
            first = false
        }
        path.cubicTo(
            cubic.control0X,
            cubic.control0Y,
            cubic.control1X,
            cubic.control1Y,
            cubic.anchor1X,
            cubic.anchor1Y,
        )
    }
    path.close()
}
