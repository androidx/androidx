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

package androidx.graphics.shapes.test

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.scaleMatrix
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.RoundedPolygon

fun RoundedPolygon.transformed(matrix: Matrix, tmp: FloatArray = FloatArray(2)):
    RoundedPolygon = transformed {
        // TODO: Should we have a fast path for when the MutablePoint is array-backed?
        tmp[0] = x
        tmp[1] = y
        matrix.mapPoints(tmp)
        x = tmp[0]
        x = tmp[1]
    }

/**
 * Function used to create a Path from this CubicShape.
 * This usually should only be called once and cached, since CubicShape is immutable.
 */
fun Iterator<Cubic>.toPath(path: Path = Path()): Path {
    path.rewind()
    var first = true
    for (bezier in this) {
        if (first) {
            path.moveTo(bezier.anchor0X, bezier.anchor0Y)
            first = false
        }
        path.cubicTo(
            bezier.control0X, bezier.control0Y,
            bezier.control1X, bezier.control1Y,
            bezier.anchor1X, bezier.anchor1Y
        )
    }
    path.close()
    return path
}

fun Canvas.drawPolygon(shape: RoundedPolygon, scale: Int, paint: Paint) =
    drawPath(shape.cubics.iterator().toPath().apply {
        transform(scaleMatrix(scale.toFloat(), scale.toFloat()))
}, paint)
