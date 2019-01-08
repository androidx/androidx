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
                        Vector3(1.0f, 0.0f, 0.0f),
                        Vector3(0.0f, 1.0f, 0.0f),
                        Vector3(0.0f, 0.0f, 1.0f)
                ),
                Matrix3.identity()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Matrix3 of fails if less than 9 arguments`() {
        Matrix3.of(*8.floatArray())
    }

    @Test
    fun `Matrix3 of`() {
        assertEquals(MAT_3, Matrix3.of(*9.floatArray()))
    }

    @Test
    fun `Matrix4 identity`() {
        assertEquals(
                Matrix4(
                        Vector4(1.0f, 0.0f, 0.0f, 0.0f),
                        Vector4(0.0f, 1.0f, 0.0f, 0.0f),
                        Vector4(0.0f, 0.0f, 1.0f, 0.0f),
                        Vector4(0.0f, 0.0f, 0.0f, 1.0f)
                ),
                Matrix4.identity()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Matrix4 of fails if less than 16 arguments`() {
        Matrix4.of(*15.floatArray())
    }

    @Test
    fun `Matrix4 of`() {
        assertEquals(MAT_4, Matrix4.of(*16.floatArray()))
    }

    @Test
    fun `transpose Matrix3`() {
        assertEquals(
                Matrix3(
                        Vector3(1.0f, 2.0f, 3.0f),
                        Vector3(4.0f, 5.0f, 6.0f),
                        Vector3(7.0f, 8.0f, 9.0f)
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
                        Vector3(0.0f, 1.0f, 0.0f),
                        Vector3(-2.0f, 1.0f, 1.0f),
                        Vector3(2.0f, -2.0f, 0.0f)
                ),
                inverse(Matrix3(
                        Vector3(1.0f, 0.0f, 0.5f),
                        Vector3(1.0f, 0.0f, 0.0f),
                        Vector3(1.0f, 1.0f, 1.0f)
                ))
        )
    }

    @Test
    fun `inverse Matrix3 of identity is identity`() {
        assertEquals(Matrix3.identity(), inverse(Matrix3.identity()))
    }

    @Test
    fun `scale Vector3`() {
        assertEquals(Matrix4.identity(), scale(Vector3(1.0f, 1.0f, 1.0f)))
    }

    @Test
    fun `scale Matrix4`() {
        assertEquals(
                Matrix4(
                        Vector4(2.0f, 0.0f, 0.0f, 0.0f),
                        Vector4(0.0f, 4.0f, 0.0f, 0.0f),
                        Vector4(0.0f, 0.0f, 6.0f, 0.0f),
                        Vector4(0.0f, 0.0f, 0.0f, 1.0f)
                ),
                scale(Matrix4(
                        Vector4(2.0f, 0.0f, 0.0f, 0.0f),
                        Vector4(4.0f, 0.0f, 0.0f, 0.0f),
                        Vector4(6.0f, 0.0f, 0.0f, 0.0f),
                        Vector4(0.0f, 0.0f, 0.0f, 0.0f)
                ))
        )
    }

    @Test
    fun `translation Vector3`() {
        assertEquals(
                Matrix4(
                        Vector4(1.0f, 0.0f, 0.0f, 0.0f),
                        Vector4(0.0f, 1.0f, 0.0f, 0.0f),
                        Vector4(0.0f, 0.0f, 1.0f, 0.0f),
                        Vector4(1.0f, 2.0f, 3.0f, 1.0f)
                ),
                translation(Vector3(1.0f, 2.0f, 3.0f))
        )
    }

    @Test
    fun `translation Matrix4`() {
        assertEquals(
                Matrix4(
                        Vector4(1.0f, 0.0f, 0.0f, 0.0f),
                        Vector4(0.0f, 1.0f, 0.0f, 0.0f),
                        Vector4(0.0f, 0.0f, 1.0f, 0.0f),
                        Vector4(4.0f, 8.0f, 12.0f, 1.0f)
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
                        Vector4(1.0f, 0.0f, 0.0f, 0.0f),
                        Vector4(-1.0f, 1.0f, 0.0f, 0.0f),
                        Vector4(4.0f, -4.0f, 1.0f, -2.0f),
                        Vector4(-2.0f, 2.0f, 0.0f, 1.0f)

                ),
                inverse(
                        Matrix4(
                                Vector4(1.0f, 0.0f, 0.0f, 0.0f),
                                Vector4(1.0f, 1.0f, 0.0f, 0.0f),
                                Vector4(0.0f, 0.0f, 1.0f, 2.0f),
                                Vector4(0.0f, -2.0f, 0.0f, 1.0f)
                        ))
        )
    }

    @Test
    fun `inverse non-invertible Matrix4`() {
        assertEquals(
                Matrix4(
                        Vector4(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
                                Float.NaN, Float.NaN),
                        Vector4(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY,
                                Float.NaN, Float.NaN),
                        Vector4(Float.NaN, Float.NaN, Float.NaN, Float.NaN),
                        Vector4(Float.NaN, Float.NaN, Float.NaN, Float.NaN)

                ),
                inverse(
                        Matrix4(
                                Vector4(1.0f, 1.0f, 0.0f, 0.0f),
                                Vector4(1.0f, 1.0f, 0.0f, 0.0f),
                                Vector4(0.0f, 0.0f, 1.0f, 2.0f),
                                Vector4(0.0f, 0.0f, 0.0f, 1.0f)
                        ))
        )
    }

    @Test
    fun `rotation Vector3`() {
        assertArrayEquals(
                Matrix4(
                        Vector4(0.998f, 0.0523f, -0.0348f, 0.0f),
                        Vector4(-0.0517f, 0.9985f, 0.0174f, 0.0f),
                        Vector4(0.0357f, -0.0156f, 0.9992f, 0.0f),
                        Vector4(0.0f, 0.0f, 0.0f, 1.0f)
                ).toFloatArray(),
                rotation(Vector3(1.0f, 2.0f, 3.0f)).toFloatArray()
        )
    }

    @Test
    fun `rotation Matrix4`() {
        assertArrayEquals(
                Matrix4(
                        Vector4(0.0966f, 0.4833f, 0.87f, 0.0f),
                        Vector4(0.169f, 0.507f, 0.8451f, 0.0f),
                        Vector4(0.2242f, 0.5232f, 0.8221f, 0.0f),
                        Vector4(0.0f, 0.0f, 0.0f, 1.0f)
                ).toFloatArray(),
                rotation(MAT_4).toFloatArray()
        )
    }

    @Test
    fun `rotation axis angle`() {
        assertArrayEquals(
                Matrix4(
                        Vector4(0.9999f, 5.0f, 1.0f, 0.0f),
                        Vector4(-1.0f, 4.0f, 7.0f, 0.0f),
                        Vector4(4.0f, 5.0f, 9.0f, 0.0f),
                        Vector4(0.0f, 0.0f, 0.0f, 1.0f)
                ).toFloatArray(),
                rotation(Vector3(1.0f, 2.0f, 3.0f), 90.0f).toFloatArray()
        )
    }

    @Test
    fun normal() {
        assertArrayEquals(
                Matrix4(
                        Vector4(0.0093f, 0.0357f, 0.0502f, 13.0f),
                        Vector4(0.0186f, 0.0428f, 0.0558f, 14.0f),
                        Vector4(0.0280f, 0.05f, 0.0614f, 15.0f),
                        Vector4(0.0373f, 0.0571f, 0.0670f, 16.0f)
                ).toFloatArray(),
                normal(MAT_4).toFloatArray()
        )
    }

    @Test
    fun lookAt() {
        assertArrayEquals(
                Matrix4(
                        Vector4(0.53606f, -0.7862f, 0.30734f, 0.0f),
                        Vector4(0.28377f, 0.51073f, 0.81155f, 0.0f),
                        Vector4(0.79504f, 0.34783f, -0.4969f, 0.0f),
                        Vector4(1.0f, 2.0f, 3.0f, 1.0f)
                ).toFloatArray(),
                lookAt(
                        eye = Vector3(1.0f, 2.0f, 3.0f),
                        target = Vector3(9.0f, 5.5f, -2.0f),
                        up = Vector3(3.0f, 4.0f, 5.0f)
                ).toFloatArray()
        )
    }

    @Test
    fun lookTowards() {
        assertArrayEquals(
                Matrix4(
                        Vector4(-0.6549f, -0.3475f, 0.67100f, 0.0f),
                        Vector4(0.10792f, 0.83584f, 0.53825f, 0.0f),
                        Vector4(0.74791f, -0.4249f, 0.50994f, 0.0f),
                        Vector4(1.0f, 2.0f, 3.0f, 1.0f)
                ).toFloatArray(),
                lookTowards(
                        eye = Vector3(1.0f, 2.0f, 3.0f),
                        forward = Vector3(4.4f, -2.5f, 3.0f),
                        up = Vector3(3.0f, 4.0f, 5.0f)
                ).toFloatArray()
        )
    }

    @Test
    fun perspective() {
        assertArrayEquals(
                Matrix4(
                        Vector4(57.2943f, 0.0f, 0.0f, 0.0f),
                        Vector4(0.0f, 114.5886f, 0.0f, 0.0f),
                        Vector4(0.0f, 0.0f, -7.0f, 1.0f),
                        Vector4(0.0f, 0.0f, 24.0f, 0.0f)
                ).toFloatArray(),
                perspective(
                        fov = 1.0f,
                        ratio = 2.0f,
                        far = 3.0f,
                        near = 4.0f
                ).toFloatArray()
        )
    }

    @Test
    fun ortho() {
        assertArrayEquals(
                Matrix4(
                        Vector4(2.0f, 0.0f, 0.0f, 0.0f),
                        Vector4(0.0f, 2.0f, 0.0f, 0.0f),
                        Vector4(0.0f, 0.0f, -2.0f, 0.0f),
                        Vector4(-3.0f, -7.0f, -11.0f, 1.0f)
                ).toFloatArray(),
                ortho(
                        l = 1.0f,
                        r = 2.0f,
                        b = 3.0f,
                        t = 4.0f,
                        n = 5.0f,
                        f = 6.0f
                ).toFloatArray()
        )
    }

    companion object {
        private val MAT_3 = Matrix3(
                Vector3(1.0f, 4.0f, 7.0f),
                Vector3(2.0f, 5.0f, 8.0f),
                Vector3(3.0f, 6.0f, 9.0f)
        )
        private val MAT_4 = Matrix4(
                Vector4(1.0f, 5.0f, 9.0f, 13.0f),
                Vector4(2.0f, 6.0f, 10.0f, 14.0f),
                Vector4(3.0f, 7.0f, 11.0f, 15.0f),
                Vector4(4.0f, 8.0f, 12.0f, 16.0f)
        )

        private fun assertArrayEquals(
            expected: FloatArray,
            actual: FloatArray,
            delta: Float = 0.0001f
        ) = Assert.assertArrayEquals(expected, actual, delta)

        /**
         * @return a VectorArray containing n floats 1f,2f,...,n (float) where n
         * is the @receiver integer.
         */
        private fun Int.floatArray() = FloatArray(this) { (it + 1).toFloat() }
    }
}