/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.graphics

import androidx.compose.ui.InternalComposeUiApi
import kotlin.math.abs
import org.jetbrains.skia.Matrix33

internal fun identityMatrix33() = Matrix33(
    1f, 0f, 0f,
    0f, 1f, 0f,
    0f, 0f, 1f
)

internal fun Matrix33.setFrom(matrix: Matrix) {
    require(
        matrix[0, 2] == 0f &&
            matrix[1, 2] == 0f &&
            matrix[2, 2] == 1f &&
            matrix[3, 2] == 0f &&
            matrix[2, 0] == 0f &&
            matrix[2, 1] == 0f &&
            matrix[2, 3] == 0f
    ) {
        "Matrix33 does not support arbitrary transforms"
    }

    // We'll reuse the array used in Matrix to avoid allocation by temporarily
    // setting it to the 3x3 matrix used by android.graphics.Matrix
    // Store the values of the 4 x 4 matrix into temporary variables
    // to be reset after the 3 x 3 matrix is configured
    val scaleX = matrix.values[Matrix.ScaleX] // 0
    val skewY = matrix.values[Matrix.SkewY] // 1
    val v2 = matrix.values[2] // 2
    val persp0 = matrix.values[Matrix.Perspective0] // 3
    val skewX = matrix.values[Matrix.SkewX] // 4
    val scaleY = matrix.values[Matrix.ScaleY] // 5
    val v6 = matrix.values[6] // 6
    val persp1 = matrix.values[Matrix.Perspective1] // 7
    val v8 = matrix.values[8] // 8

    val translateX = matrix.values[Matrix.TranslateX]
    val translateY = matrix.values[Matrix.TranslateY]
    val persp2 = matrix.values[Matrix.Perspective2]

    val v = matrix.values

    v[0] = scaleX // MSCALE_X = 0
    v[1] = skewX // MSKEW_X = 1
    v[2] = translateX // MTRANS_X = 2
    v[3] = skewY // MSKEW_Y = 3
    v[4] = scaleY // MSCALE_Y = 4
    v[5] = translateY // MTRANS_Y
    v[6] = persp0 // MPERSP_0 = 6
    v[7] = persp1 // MPERSP_1 = 7
    v[8] = persp2 // MPERSP_2 = 8

    for (i in 0..8) {
        mat[i] = v[i]
    }

    // Reset the values back after the android.graphics.Matrix is configured
    v[Matrix.ScaleX] = scaleX // 0
    v[Matrix.SkewY] = skewY // 1
    v[2] = v2 // 2
    v[Matrix.Perspective0] = persp0 // 3
    v[Matrix.SkewX] = skewX // 4
    v[Matrix.ScaleY] = scaleY // 5
    v[6] = v6 // 6
    v[Matrix.Perspective1] = persp1 // 7
    v[8] = v8 // 8
}

@InternalComposeUiApi
fun prepareTransformationMatrix(
    matrix: Matrix,
    pivotX: Float,
    pivotY: Float,
    translationX: Float,
    translationY: Float,
    rotationX: Float,
    rotationY: Float,
    rotationZ: Float,
    scaleX: Float,
    scaleY: Float,
    cameraDistance: Float,
) {
    matrix.reset()
    matrix.translate(x = -pivotX, y = -pivotY)
    matrix *= Matrix().apply {
        rotateZ(rotationZ)
        rotateY(rotationY)
        rotateX(rotationX)
        scale(scaleX, scaleY)
    }
    // Perspective transform should be applied only in case of rotations to avoid
    // multiply application in hierarchies.
    // See Android's frameworks/base/libs/hwui/RenderProperties.cpp for reference
    if (!rotationX.isZero() || !rotationY.isZero()) {
        matrix *= Matrix().apply {
            // The camera location is passed in inches, set in pt
            val depth = cameraDistance * 72f
            this[2, 3] = -1f / depth
        }
    }
    matrix *= Matrix().apply {
        translate(x = pivotX + translationX, y = pivotY + translationY)
    }

    // Third column and row are irrelevant for 2D space.
    // Zeroing required to get correct inverse transformation matrix.
    matrix[2, 0] = 0f
    matrix[2, 1] = 0f
    matrix[2, 3] = 0f
    matrix[0, 2] = 0f
    matrix[1, 2] = 0f
    matrix[3, 2] = 0f
}

// Copy from Android's frameworks/base/libs/hwui/utils/MathUtils.h
private const val NON_ZERO_EPSILON = 0.001f

@Suppress("NOTHING_TO_INLINE")
private inline fun Float.isZero(): Boolean = abs(this) <= NON_ZERO_EPSILON
