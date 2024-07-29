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

package androidx.ink.geometry

import android.graphics.Matrix
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidGraphicsConversionExtensionsTest {
    @Test
    fun populateMatrix_resultingMatrixIsAffine() {
        val affineTransform = ImmutableAffineTransform(A, B, C, D, E, F)
        val matrix = Matrix()
        affineTransform.populateMatrix(matrix)
        assertThat(matrix.isAffine()).isTrue()
    }

    @Test
    fun populateMatrix_resultsInEquivalentVecTransformations() {
        val affineTransform = ImmutableAffineTransform(A, B, C, D, E, F)

        // First, apply the affineTransform to an ink Vec for later comparison.
        val inputVec = ImmutableVec(1f, 2f)
        val outputVec = MutableVec()
        affineTransform.applyTransform(inputVec, outputVec)

        // Then, populate an android.graphics.Matrix from the affineTransform and perform the
        // equivalent
        // operation.
        val matrix = Matrix()
        affineTransform.populateMatrix(matrix)
        val vecFloatArray = floatArrayOf(inputVec.x, inputVec.y)
        matrix.mapPoints(vecFloatArray)

        assertThat(outputVec).isEqualTo(ImmutableVec(vecFloatArray[0], vecFloatArray[1]))
    }

    companion object {
        private const val A = 1f
        private const val B = 2f
        private const val C = -3f
        private const val D = -4f
        private const val E = 5f
        private const val F = 6f
    }
}
