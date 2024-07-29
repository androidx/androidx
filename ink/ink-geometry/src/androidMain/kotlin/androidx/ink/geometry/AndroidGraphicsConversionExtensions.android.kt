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

@file:JvmName("AndroidGraphicsConversionHelpers")

package androidx.ink.geometry

import android.graphics.Matrix
import androidx.annotation.RestrictTo
import androidx.ink.geometry.internal.getValue
import androidx.ink.geometry.internal.threadLocal

/** Scratch space to be used as the argument to [Matrix.getValues] and [Matrix.setValues]. */
private val matrixValuesScratchArray by threadLocal { FloatArray(9) }

/** Writes the values from this [AffineTransform] to [matrixOut]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
public fun AffineTransform.populateMatrix(matrixOut: Matrix) {
    matrixValuesScratchArray[Matrix.MSCALE_X] = a
    matrixValuesScratchArray[Matrix.MSKEW_X] = b
    matrixValuesScratchArray[Matrix.MTRANS_X] = c
    matrixValuesScratchArray[Matrix.MSKEW_Y] = d
    matrixValuesScratchArray[Matrix.MSCALE_Y] = e
    matrixValuesScratchArray[Matrix.MTRANS_Y] = f
    matrixValuesScratchArray[Matrix.MPERSP_0] = 0f
    matrixValuesScratchArray[Matrix.MPERSP_1] = 0f
    matrixValuesScratchArray[Matrix.MPERSP_2] = 1f
    matrixOut.setValues(matrixValuesScratchArray)
}
