
/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.ui.painting.matrixutils

import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.vectormath64.Matrix4
import androidx.ui.vectormath64.Vector3

// / Applies the given matrix as a perspective transform to the given point.
// /
// / this function assumes the given point has a z-coordinate of 0.0. the
// / z-coordinate of the result is ignored.
fun Matrix4.transformPoint(point: Offset): Offset {
    val position3 = Vector3(point.dx.toFloat(), point.dy.toFloat(), 0f)
    val transformed3 = perspectiveTransform(position3)
    return Offset(transformed3.x.toDouble(), transformed3.y.toDouble())
}

// / Returns a rect that bounds the result of applying the given matrix as a
// / perspective transform to the given rect.
// /
// / This function assumes the given rect is in the plane with z equals 0.0.
// / The transformed rect is then projected back into the plane with z equals
// / 0.0 before computing its bounding rect.
fun Matrix4.transformRect(rect: Rect): Rect {
    val point1 = transformPoint(rect.getTopLeft())
    val point2 = transformPoint(rect.getTopRight())
    val point3 = transformPoint(rect.getBottomLeft())
    val point4 = transformPoint(rect.getBottomRight())
    return Rect.fromLTRB(
            minOf(point1.dx, minOf(point2.dx, minOf(point3.dx, point4.dx))),
            minOf(point1.dy, minOf(point2.dy, minOf(point3.dy, point4.dy))),
            maxOf(point1.dx, maxOf(point2.dx, maxOf(point3.dx, point4.dx))),
            maxOf(point1.dy, maxOf(point2.dy, maxOf(point3.dy, point4.dy)))
    )
}