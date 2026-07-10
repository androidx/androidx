/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.rendering.android.canvas.internal

import android.graphics.Matrix
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class CanvasStrokeUnifiedRendererRobolectricTest {

    private val renderer = CanvasStrokeUnifiedRenderer(forcePathRendering = false)

    @Test
    fun assertAffineTransform_whenNonAffine_shouldThrow() {
        assertFailsWith<IllegalArgumentException> {
            renderer.assertIsAffine(
                Matrix().apply {
                    setValues(
                        floatArrayOf(
                            1F,
                            0F,
                            0F, // first row looks affine
                            0F,
                            1F,
                            0F, // second row looks affine
                            4F,
                            0F,
                            1F, // third row should be [0, 0, 1] to be affine
                        )
                    )
                }
            )
        }

        assertFailsWith<IllegalArgumentException> {
            renderer.assertIsAffine(
                Matrix().apply {
                    setValues(
                        floatArrayOf(
                            1F,
                            0F,
                            0F, // first row looks affine
                            0F,
                            1F,
                            0F, // second row looks affine
                            0F,
                            3F,
                            1F, // third row should be [0, 0, 1] to be affine
                        )
                    )
                }
            )
        }

        assertFailsWith<IllegalArgumentException> {
            renderer.assertIsAffine(
                Matrix().apply {
                    setValues(
                        floatArrayOf(
                            1F,
                            0F,
                            0F, // first row looks affine
                            0F,
                            1F,
                            0F, // second row looks affine
                            0F,
                            0F,
                            2F, // third row should be [0, 0, 1] to be affine
                        )
                    )
                }
            )
        }
    }

    @Test
    fun drawStroke_withAffineTransform_shouldNotThrow() {
        // The simplest affine transform - the identity matrix.
        renderer.assertIsAffine(Matrix())

        // Test for an edge case where the input Matrix is actually affine if inspected directly,
        // but
        // where Android Matrix.isAffine returns false. See b/418261442 for more details.
        val falseNegativeAffineMatrix =
            Matrix().apply {
                setValues(
                    floatArrayOf(
                        1.2887144F,
                        0.33863622F,
                        -776.0461F, // first row looks affine
                        -0.33863622F,
                        1.2887144F,
                        -297.80093F, // second row looks affine
                        0F,
                        0F,
                        0.99999994F, // third row is nearly affine, except for floating point
                        // precision
                    )
                )
                // Inverting this matrix yields a transform that is actually affine, but
                // Matrix.isAffine
                // incorrectly does not consider it to be.
                invert(this)
                check(!isAffine) {
                    "Trying to test the case where Matrix.isAffine is false but the bottom row is " +
                        "[0, 0, 1], but Matrix.isAffine is actually true."
                }
                val values = FloatArray(9).also { getValues(it) }
                check(
                    values[Matrix.MPERSP_0] == 0F &&
                        values[Matrix.MPERSP_1] == 0F &&
                        values[Matrix.MPERSP_2] == 1F
                ) {
                    "Trying to test the case where Matrix.isAffine is false but the bottom row is " +
                        "[0, 0, 1], but the Matrix is actually $this."
                }
            }
        renderer.assertIsAffine(falseNegativeAffineMatrix)
    }
}
