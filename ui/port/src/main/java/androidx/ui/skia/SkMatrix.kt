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

package androidx.ui.skia

import android.graphics.Matrix
import android.graphics.RectF
import androidx.ui.engine.geometry.Rect
import androidx.ui.vectormath64.Matrix4

class SkMatrix(val frameworkMatrix: Matrix) {

    companion object {

        fun concat(a: SkMatrix, b: SkMatrix): SkMatrix {
            val result = Matrix()
            result.setConcat(a.frameworkMatrix, b.frameworkMatrix)
            return SkMatrix(result)
        }

        fun I(): SkMatrix {
            return SkMatrix(Matrix4.identity())
        }
    }

    constructor(matrix4: Matrix4) : this(
            Matrix().apply {
                with(matrix4) {
                    // (Migration/Andrey): Conversion from Matrix4 to SkMatrix(3x3)
                    // https://github.com/flutter/engine/blob/master/lib/ui/painting/matrix.cc
                    // Mappings from SkMatrix-index to input-index.
                    // 0, 4, 12,
                    // 1, 5, 13,
                    // 3, 7, 15,
                    val array = matrix4.toDoubleArray()
                    val map = { i: Int -> array[i].toFloat() }
                    setValues(floatArrayOf(
                            map(0), map(4), map(12),
                            map(1), map(5), map(13),
                            map(3), map(7), map(15)
                    ))
                }
            }
    )

    /**
     * Returns true if the matrix contains perspective elements. SkMatrix form is:
     * |       --            --              --          |
     * |       --            --              --          |
     * | perspective-x  perspective-y  perspective-scale |
     * where perspective-x or perspective-y is non-zero, or perspective-scale is
     * not one. All other elements may have any value.
     * @return true if SkMatrix is in most general form
     */
    fun hasPerspective(): Boolean {
        val values = FloatArray(9)
        frameworkMatrix.getValues(values)
        return values[6] != 0.0f || values[7] != 0.0f || values[8] != 1.0f
    }

    fun invert(): SkMatrix? {
        val result = Matrix()
        return if (frameworkMatrix.invert(result)) {
            SkMatrix(result)
        } else {
            null
        }
    }

    fun mapRect(src: Rect): Rect {
        val srcF = src.toFrameworkRectF()
        val dstF = RectF()
        frameworkMatrix.mapRect(dstF, srcF)
        return Rect(
                dstF.left.toDouble(),
                dstF.top.toDouble(),
                dstF.right.toDouble(),
                dstF.bottom.toDouble()
        )
    }
}