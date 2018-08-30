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

package androidx.ui.vectormath64

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MatrixTest {

    @Test
    fun `Matrix3 identity`() {
        assertEquals(
                Matrix3(
                        Vector3(1.0, 0.0, 0.0),
                        Vector3(0.0, 1.0, 0.0),
                        Vector3(0.0, 0.0, 1.0)
                ),
                Matrix3.identity()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Matrix3 of fails if less than 9 arguments`() {
        Matrix3.of(*8.doubleArray())
    }

    @Test
    fun `Matrix3 of`() {
        assertEquals(MAT_3, Matrix3.of(*9.doubleArray()))
    }

    @Test
    fun `Matrix4 identity`() {
        assertEquals(
                Matrix4(
                        Vector4(1.0, 0.0, 0.0, 0.0),
                        Vector4(0.0, 1.0, 0.0, 0.0),
                        Vector4(0.0, 0.0, 1.0, 0.0),
                        Vector4(0.0, 0.0, 0.0, 1.0)
                ),
                Matrix4.identity()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Matrix4 of fails if less than 16 arguments`() {
        Matrix4.of(*15.doubleArray())
    }

    @Test
    fun `Matrix4 of`() {
        assertEquals(MAT_4, Matrix4.of(*16.doubleArray()))
    }

    @Test
    fun `transpose Matrix3`() {
        assertEquals(
                Matrix3(
                        Vector3(1.0, 2.0, 3.0),
                        Vector3(4.0, 5.0, 6.0),
                        Vector3(7.0, 8.0, 9.0)
                ),
                transpose(MAT_3)
        )
    }

    @Test
    fun `transpose Matrix3 of identity is identity`() {
        assertEquals(transpose(Matrix3.identity()), Matrix3.identity())
    }

    @Test
    fun `inverse Matrix3`() {
        assertEquals(
                Matrix3(
                        Vector3(0.0, 1.0, 0.0),
                        Vector3(-2.0, 1.0, 1.0),
                        Vector3(2.0, -2.0, 0.0)
                ),
                inverse(Matrix3(
                        Vector3(1.0, 0.0, 0.5),
                        Vector3(1.0, 0.0, 0.0),
                        Vector3(1.0, 1.0, 1.0)
                ))
        )
    }

    @Test
    fun `inverse Matrix3 of identity is identity`() {
        assertEquals(Matrix3.identity(), inverse(Matrix3.identity()))
    }

    @Test
    fun `scale Vector3`() {
        assertEquals(Matrix4.identity(), scale(Vector3(1.0, 1.0, 1.0)))
    }

    @Test
    fun `scale Matrix4`() {
        assertEquals(
                Matrix4(
                        Vector4(2.0, 0.0, 0.0, 0.0),
                        Vector4(0.0, 4.0, 0.0, 0.0),
                        Vector4(0.0, 0.0, 6.0, 0.0),
                        Vector4(0.0, 0.0, 0.0, 1.0)
                ),
                scale(Matrix4(
                        Vector4(2.0, 0.0, 0.0, 0.0),
                        Vector4(4.0, 0.0, 0.0, 0.0),
                        Vector4(6.0, 0.0, 0.0, 0.0),
                        Vector4(0.0, 0.0, 0.0, 0.0)
                ))
        )
    }

    @Test
    fun `translation Vector3`() {
        assertEquals(
                Matrix4(
                        Vector4(1.0, 0.0, 0.0, 0.0),
                        Vector4(0.0, 1.0, 0.0, 0.0),
                        Vector4(0.0, 0.0, 1.0, 0.0),
                        Vector4(1.0, 2.0, 3.0, 1.0)
                ),
                translation(Vector3(1.0, 2.0, 3.0))
        )
    }

    @Test
    fun `translation Matrix4`() {
        assertEquals(
                Matrix4(
                        Vector4(1.0, 0.0, 0.0, 0.0),
                        Vector4(0.0, 1.0, 0.0, 0.0),
                        Vector4(0.0, 0.0, 1.0, 0.0),
                        Vector4(4.0, 8.0, 12.0, 1.0)
                ),
                translation(MAT_4)
        )
    }

    @Test
    fun `inverse Matrix4 of identity is identity`() {
        assertEquals(Matrix4.identity(), inverse(Matrix4.identity()))
    }

    @Test
    fun `inverse Matrix4`() {
        assertEquals(
                Matrix4(
                        Vector4(1.0, 0.0, 0.0, 0.0),
                        Vector4(-1.0, 1.0, 0.0, 0.0),
                        Vector4(4.0, -4.0, 1.0, -2.0),
                        Vector4(-2.0, 2.0, 0.0, 1.0)

                ),
                inverse(
                        Matrix4(
                                Vector4(1.0, 0.0, 0.0, 0.0),
                                Vector4(1.0, 1.0, 0.0, 0.0),
                                Vector4(0.0, 0.0, 1.0, 2.0),
                                Vector4(0.0, -2.0, 0.0, 1.0)
                        ))
        )
    }

    @Test
    fun `inverse non-invertible Matrix4`() {
        assertEquals(
                Matrix4(
                        Vector4(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                                Double.NaN, Double.NaN),
                        Vector4(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                                Double.NaN, Double.NaN),
                        Vector4(Double.NaN, Double.NaN, Double.NaN, Double.NaN),
                        Vector4(Double.NaN, Double.NaN, Double.NaN, Double.NaN)

                ),
                inverse(
                        Matrix4(
                                Vector4(1.0, 1.0, 0.0, 0.0),
                                Vector4(1.0, 1.0, 0.0, 0.0),
                                Vector4(0.0, 0.0, 1.0, 2.0),
                                Vector4(0.0, 0.0, 0.0, 1.0)
                        ))
        )
    }

    @Test
    fun `rotation Vector3`() {
        assertArrayEquals(
                Matrix4(
                        Vector4(0.998, 0.0523, -0.0348, 0.0),
                        Vector4(-0.0517, 0.9985, 0.0174, 0.0),
                        Vector4(0.0357, -0.0156, 0.9992, 0.0),
                        Vector4(0.0, 0.0, 0.0, 1.0)
                ).toDoubleArray(),
                rotation(Vector3(1.0, 2.0, 3.0)).toDoubleArray()
        )
    }

    @Test
    fun `rotation Matrix4`() {
        assertArrayEquals(
                Matrix4(
                        Vector4(0.0966, 0.4833, 0.87, 0.0),
                        Vector4(0.169, 0.507, 0.8451, 0.0),
                        Vector4(0.2242, 0.5232, 0.8221, 0.0),
                        Vector4(0.0, 0.0, 0.0, 1.0)
                ).toDoubleArray(),
                rotation(MAT_4).toDoubleArray()
        )
    }

    @Test
    fun `rotation axis angle`() {
        assertArrayEquals(
                Matrix4(
                        Vector4(0.9999, 5.0, 1.0, 0.0),
                        Vector4(-1.0, 4.0, 7.0, 0.0),
                        Vector4(4.0, 5.0, 9.0, 0.0),
                        Vector4(0.0, 0.0, 0.0, 1.0)
                ).toDoubleArray(),
                rotation(Vector3(1.0, 2.0, 3.0), 90.0).toDoubleArray()
        )
    }

    @Test
    fun normal() {
        assertArrayEquals(
                Matrix4(
                        Vector4(0.0093, 0.0357, 0.0502, 13.0),
                        Vector4(0.0186, 0.0428, 0.0558, 14.0),
                        Vector4(0.0280, 0.05, 0.0614, 15.0),
                        Vector4(0.0373, 0.0571, 0.0670, 16.0)
                ).toDoubleArray(),
                normal(MAT_4).toDoubleArray()
        )
    }

    @Test
    fun lookAt() {
        assertArrayEquals(
                Matrix4(
                        Vector4(0.53606, -0.7862, 0.30734, 0.0),
                        Vector4(0.28377, 0.51073, 0.81155, 0.0),
                        Vector4(0.79504, 0.34783, -0.4969, 0.0),
                        Vector4(1.0, 2.0, 3.0, 1.0)
                ).toDoubleArray(),
                lookAt(
                        eye = Vector3(1.0, 2.0, 3.0),
                        target = Vector3(9.0, 5.5, -2.0),
                        up = Vector3(3.0, 4.0, 5.0)
                ).toDoubleArray()
        )
    }

    @Test
    fun lookTowards() {
        assertArrayEquals(
                Matrix4(
                        Vector4(-0.6549, -0.3475, 0.67100, 0.0),
                        Vector4(0.10792, 0.83584, 0.53825, 0.0),
                        Vector4(0.74791, -0.4249, 0.50994, 0.0),
                        Vector4(1.0, 2.0, 3.0, 1.0)
                ).toDoubleArray(),
                lookTowards(
                        eye = Vector3(1.0, 2.0, 3.0),
                        forward = Vector3(4.4, -2.5, 3.0),
                        up = Vector3(3.0, 4.0, 5.0)
                ).toDoubleArray()
        )
    }

    @Test
    fun perspective() {
        assertArrayEquals(
                Matrix4(
                        Vector4(57.2943, 0.0, 0.0, 0.0),
                        Vector4(0.0, 114.5886, 0.0, 0.0),
                        Vector4(0.0, 0.0, -7.0, 1.0),
                        Vector4(0.0, 0.0, 24.0, 0.0)
                ).toDoubleArray(),
                perspective(
                        fov = 1.0,
                        ratio = 2.0,
                        far = 3.0,
                        near = 4.0
                ).toDoubleArray()
        )
    }

    @Test
    fun ortho() {
        assertArrayEquals(
                Matrix4(
                        Vector4(2.0, 0.0, 0.0, 0.0),
                        Vector4(0.0, 2.0, 0.0, 0.0),
                        Vector4(0.0, 0.0, -2.0, 0.0),
                        Vector4(-3.0, -7.0, -11.0, 1.0)
                ).toDoubleArray(),
                ortho(
                        l = 1.0,
                        r = 2.0,
                        b = 3.0,
                        t = 4.0,
                        n = 5.0,
                        f = 6.0
                ).toDoubleArray()
        )
    }

    companion object {
        private val MAT_3 = Matrix3(
                Vector3(1.0, 4.0, 7.0),
                Vector3(2.0, 5.0, 8.0),
                Vector3(3.0, 6.0, 9.0)
        )
        private val MAT_4 = Matrix4(
                Vector4(1.0, 5.0, 9.0, 13.0),
                Vector4(2.0, 6.0, 10.0, 14.0),
                Vector4(3.0, 7.0, 11.0, 15.0),
                Vector4(4.0, 8.0, 12.0, 16.0)
        )

        private fun assertArrayEquals(
            expected: DoubleArray,
            actual: DoubleArray,
            delta: Double = 0.0001
        ) = Assert.assertArrayEquals(expected, actual, delta)

        /**
         * @return a VectorArray containing n floats 1f,2f,...,n (float) where n
         * is the @receiver integer.
         */
        private fun Int.doubleArray() = DoubleArray(this) { (it + 1).toDouble() }
    }
}