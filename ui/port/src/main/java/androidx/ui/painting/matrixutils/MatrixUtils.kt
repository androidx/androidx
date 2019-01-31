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

import androidx.ui.painting.basictypes.Axis
import androidx.ui.vectormath64.Matrix4

/**
 * Create a transformation matrix which mimics the effects of tangentially
 * wrapping the plane on which this transform is applied around a cylinder
 * and then looking at the cylinder from a point outside the cylinder.
 *
 * The `radius` simulates the radius of the cylinder the plane is being
 * wrapped onto. If the transformation is applied to a 0-dimensional dot
 * instead of a plane, the dot would simply translate by +/- `radius` pixels
 * along the `orientation` [Axis] when rotating from 0 to +/- 90 degrees.
 *
 * A positive radius means the object is closest at 0 `angle` and a negative
 * radius means the object is closest at π `angle` or 180 degrees.
 *
 * The `angle` argument is the difference in angle in radians between the
 * object and the viewing point. A positive `angle` on a positive `radius`
 * moves the object up when `orientation` is vertical and right when
 * horizontal.
 *
 * The transformation is always done such that a 0 `angle` keeps the
 * transformed object at exactly the same size as before regardless of
 * `radius` and `perspective` when `radius` is positive.
 *
 * The `perspective` argument is a number between 0 and 1 where 0 means
 * looking at the object from infinitely far with an infinitely narrow field
 * of view and 1 means looking at the object from infinitely close with an
 * infinitely wide field of view. Defaults to a sane but arbitrary 0.001.
 *
 * The `orientation` is the direction of the rotation axis.
 *
 * Because the viewing position is a point, it's never possible to see the
 * outer side of the cylinder at or past +/- π / 2 or 90 degrees and it's
 * almost always possible to end up seeing the inner side of the cylinder
 * or the back side of the transformed plane before π / 2 when perspective > 0.
 */
fun createCylindricalProjectionTransform(
    radius: Float,
    angle: Float,
    perspective: Float = 0.001f,
    orientation: Axis = Axis.VERTICAL
): Matrix4 {
    assert(perspective >= 0 && perspective <= 1.0)

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
        set(3, 3, perspective * radius + 1.0f)
    }

    // Model matrix by first translating the object from the origin of the world
    // by radius in the z axis and then rotating against the world.
    result *=
            (if (orientation == Axis.HORIZONTAL)
                Matrix4.rotationY(angle) else Matrix4.rotationX(angle)) *
            Matrix4.translationValues(0.0f, 0.0f, radius)

    // Essentially perspective * view * model.
    return result
}

/**
 * Returns a list of strings representing the given transform in a format
 * useful for [TransformProperty].
 *
 * If the argument is null, returns a list with the single string "null".
 */
fun debugDescribeTransform(transform: Matrix4): List<String> {
//    if (transform == null)
//        return listOf("null")
    val matrix = transform.toString().split('\n').toMutableList()
    matrix.removeAt(matrix.size - 1)
    return matrix
}
