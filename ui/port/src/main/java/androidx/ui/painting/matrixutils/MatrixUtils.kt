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
import androidx.ui.vectormath64.Vector3
import androidx.ui.painting.basictypes.Axis
import androidx.ui.vectormath64.Matrix4

// / Returns the given [transform] matrix as an [Offset], if the matrix is
// / nothing but a 2D translation.
// /
// / Otherwise, returns null.
fun Matrix4.getAsTranslation(): Offset? {
    val values = m4storage
    // Values are stored in column-major order.
    return if (values[0] == 1.0 && // col 1
            values[1] == 0.0 &&
            values[2] == 0.0 &&
            values[3] == 0.0 &&
            values[4] == 0.0 && // col 2
            values[5] == 1.0 &&
            values[6] == 0.0 &&
            values[7] == 0.0 &&
            values[8] == 0.0 && // col 3
            values[9] == 0.0 &&
            values[10] == 1.0 &&
            values[11] == 0.0 &&
            values[14] == 0.0 && // bottom of col 4 (values 12 and 13 are the x and y offsets)

            values[15] == 1.0) {
        Offset(values[12], values[13])
    } else null
}

// / Returns the given [transform] matrix as a [double] describing a uniform
// / scale, if the matrix is nothing but a symmetric 2D scale transform.
// /
// / Otherwise, returns null.
fun Matrix4.getAsScale(): Double? {
    val values = m4storage
    // Values are stored in column-major order.
    return if (values[1] == 0.0 && // col 1 (value 0 is the scale)
            values[2] == 0.0 &&
            values[3] == 0.0 &&
            values[4] == 0.0 && // col 2 (value 5 is the scale)
            values[6] == 0.0 &&
            values[7] == 0.0 &&
            values[8] == 0.0 && // col 3
            values[9] == 0.0 &&
            values[10] == 1.0 &&
            values[11] == 0.0 &&
            values[12] == 0.0 && // col 4
            values[13] == 0.0 &&
            values[14] == 0.0 &&
            values[15] == 1.0 &&
            values[0] == values[5]) { // uniform scale
        values[0].toDouble()
    } else null
}

// / Returns true if the given matrices are exactly equal, and false
// / otherwise. Null values are assumed to be the identity matrix.
fun matrixEquals(a: Matrix4?, b: Matrix4?): Boolean {
    if (a === b)
        return true
    assert(a != null || b != null)
    if (a == null)
        return b!!.isIdentity()
    if (b == null) {
        return a.isIdentity()
    }
    assert(a != null && b != null)
    val astorage = a.m4storage
    val bstorage = b.m4storage
    return astorage.subList(0, 16) == bstorage.subList(0, 16)
}

// / Whether the given matrix is the identity matrix.
fun Matrix4.isIdentity(): Boolean {
    val storage = m4storage
    return (storage[0] == 1.0 && // col 1
            storage[1] == 0.0 &&
            storage[2] == 0.0 &&
            storage[3] == 0.0 &&
            storage[4] == 0.0 && // col 2
            storage[5] == 1.0 &&
            storage[6] == 0.0 &&
            storage[7] == 0.0 &&
            storage[8] == 0.0 && // col 3
            storage[9] == 0.0 &&
            storage[10] == 1.0 &&
            storage[11] == 0.0 &&
            storage[12] == 0.0 && // col 4
            storage[13] == 0.0 &&
            storage[14] == 0.0 &&
            storage[15] == 1.0)
}

// / Applies the given matrix as a perspective transform to the given point.
// /
// / this function assumes the given point has a z-coordinate of 0.0. the
// / z-coordinate of the result is ignored.
fun Matrix4.transformPoint(point: Offset): Offset {
    val position3 = Vector3(point.dx, point.dy, 0.0)
    val transformed3 = perspectiveTransform(position3)
    return Offset(transformed3.x, transformed3.y)
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
            _min4(point1.dx, point2.dx, point3.dx, point4.dx),
            _min4(point1.dy, point2.dy, point3.dy, point4.dy),
            _max4(point1.dx, point2.dx, point3.dx, point4.dx),
            _max4(point1.dy, point2.dy, point3.dy, point4.dy)
    )
}

fun _min4(a: Double, b: Double, c: Double, d: Double): Double {
    return minOf(a, minOf(b, minOf(c, d)))
}

fun _max4(a: Double, b: Double, c: Double, d: Double): Double {
    return maxOf(a, maxOf(b, maxOf(c, d)))
}

// / Returns a rect that bounds the result of applying the inverse of the given
// / matrix as a perspective transform to the given rect.
// /
// / This function assumes the given rect is in the plane with z equals 0.0.
// / The transformed rect is then projected back into the plane with z equals
// / 0.0 before computing its bounding rect.
fun inverseTransformRect(transform: Matrix4, rect: Rect): Rect {
    assert(rect != null)
    assert(transform.determinant != 0.0)
    if (transform.isIdentity())
        return rect
    val inverted = Matrix4(transform).apply { invert() }
    return inverted.transformRect(rect)
}

// / Create a transformation matrix which mimics the effects of tangentially
// / wrapping the plane on which this transform is applied around a cylinder
// / and then looking at the cylinder from a point outside the cylinder.
// /
// / The `radius` simulates the radius of the cylinder the plane is being
// / wrapped onto. If the transformation is applied to a 0-dimensional dot
// / instead of a plane, the dot would simply translate by +/- `radius` pixels
// / along the `orientation` [Axis] when rotating from 0 to +/- 90 degrees.
// /
// / A positive radius means the object is closest at 0 `angle` and a negative
// / radius means the object is closest at π `angle` or 180 degrees.
// /
// / The `angle` argument is the difference in angle in radians between the
// / object and the viewing point. A positive `angle` on a positive `radius`
// / moves the object up when `orientation` is vertical and right when
// / horizontal.
// /
// / The transformation is always done such that a 0 `angle` keeps the
// / transformed object at exactly the same size as before regardless of
// / `radius` and `perspective` when `radius` is positive.
// /
// / The `perspective` argument is a number between 0 and 1 where 0 means
// / looking at the object from infinitely far with an infinitely narrow field
// / of view and 1 means looking at the object from infinitely close with an
// / infinitely wide field of view. Defaults to a sane but arbitrary 0.001.
// /
// / The `orientation` is the direction of the rotation axis.
// /
// / Because the viewing position is a point, it's never possible to see the
// / outer side of the cylinder at or past +/- π / 2 or 90 degrees and it's
// / almost always possible to end up seeing the inner side of the cylinder
// / or the back side of the transformed plane before π / 2 when perspective > 0.
fun createCylindricalProjectionTransform(
    radius: Double,
    angle: Double,
    perspective: Double = 0.001,
    orientation: Axis = Axis.vertical
): Matrix4 {
    assert(radius != null)
    assert(angle != null)
    assert(perspective >= 0 && perspective <= 1.0)
    assert(orientation != null)

    // Pre-multiplied matrix of a projection matrix and a view matrix.
    //
    // Projection matrix is a simplified perspective matrix
    // http://web.iitd.ac.in/~hegde/cad/lecture/L9_persproj.pdf
    // in the form of
    // [[1.0, 0.0, 0.0, 0.0],
    //  [0.0, 1.0, 0.0, 0.0],
    //  [0.0, 0.0, 1.0, 0.0],
    //  [0.0, 0.0, -perspective, 1.0]]
    //
    // View matrix is a simplified camera view matrix.
    // Basically re-scales to keep object at original size at angle = 0 at
    // any radius in the form of
    // [[1.0, 0.0, 0.0, 0.0],
    //  [0.0, 1.0, 0.0, 0.0],
    //  [0.0, 0.0, 1.0, -radius],
    //  [0.0, 0.0, 0.0, 1.0]]
    val result = Matrix4.identity().apply {
        set(2, 3, -perspective)
        set(3, 2, -radius)
        set(3, 3, perspective * radius + 1.0)
    }

    // Model matrix by first translating the object from the origin of the world
    // by radius in the z axis and then rotating against the world.
    result *=
            (if (orientation == Axis.horizontal)
                Matrix4.rotationY(angle) else Matrix4.rotationX(angle)) *
            Matrix4.translationValues(0.0, 0.0, radius)

    // Essentially perspective * view * model.
    return result
}

// / Returns a list of strings representing the given transform in a format
// / useful for [TransformProperty].
// /
// / If the argument is null, returns a list with the single string "null".
fun debugDescribeTransform(transform: Matrix4): List<String> {
    if (transform == null)
        return listOf("null")
    val matrix = transform.toString().split('\n').toMutableList()
    matrix.removeAt(matrix.size - 1)
    return matrix
}
