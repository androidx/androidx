/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore.impl

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Matrix4ExtTest {
    @Test
    fun unscaled_returnsUnscaledMatrix() {
        // Column major, right handed 4x4 Transformation Matrix with
        // translation of (4, 8, 12) and rotation 90 (@) around Z axis, and scale of 3.3.
        // --     cos(@),   sin(@), 0,  0
        // --    -sin(@),  cos(@), 0,  0
        // --     0,        0,      1,  0
        // --      tx,       ty,     tz, 1
        val underTest =
            Matrix4(
                floatArrayOf(0f, 3.3f, 0f, 0f, -3.3f, 0f, 0f, 0f, 0f, 0f, 3.3f, 0f, 4f, 8f, 12f, 1f)
            )
        val underTestUnscaled = underTest.getUnscaled()
        val expected =
            Matrix4(floatArrayOf(0f, 1f, 0f, 0f, -1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 4f, 8f, 12f, 1f))
        assertMatrix(underTestUnscaled, expected)
    }

    @Test
    fun unscaled_withNonUniformScale_returnsUnscaledMatrix() {
        val underTest =
            Matrix4.fromTrs(Vector3(1f, 2f, 3f), Quaternion(1f, 2f, 3f, 4f), Vector3(2f, 3f, 4f))
        val underTestUnscaled = underTest.getUnscaled()
        assertMatrix(
            underTestUnscaled,
            Matrix4.fromTrs(Vector3(1f, 2f, 3f), Quaternion(1f, 2f, 3f, 4f), Vector3(1f, 1f, 1f)),
        )
    }

    @Test
    fun unscaled_withNegativeScale_returnsUnscaledMatrix() {
        val underTest =
            Matrix4.fromTrs(Vector3(1f, 2f, 3f), Quaternion(1f, 2f, 3f, 4f), Vector3(-2f, -3f, -4f))
        val underTestUnscaled = underTest.getUnscaled()
        // TODO: b/367780918 - Update when negative scales are correctly handled.
        assertMatrix(
            underTestUnscaled,
            Matrix4(
                floatArrayOf(
                    -0.13333327f,
                    -0.9333334f,
                    0.3333334f,
                    0f,
                    0.6666667f,
                    -0.33333328f,
                    -0.6666667f,
                    0f,
                    -0.73333347f,
                    -0.13333333f,
                    -0.6666666f,
                    0f,
                    1f,
                    2f,
                    3f,
                    1f,
                )
            ),
        )
    }

    @Test
    fun unscaled_withIdentityTranslationAndRotation_returnsUnscaledMatrix() {
        val underTest = Matrix4.fromTrs(Vector3.Zero, Quaternion.Identity, Vector3(2f, 3f, 4f))
        val underTestUnscaled = underTest.getUnscaled()
        assertMatrix(
            underTestUnscaled,
            Matrix4.fromTrs(Vector3.Zero, Quaternion.Identity, Vector3(1f, 1f, 1f)),
        )
    }

    private fun assertMatrix(matrix: Matrix4, expected: Matrix4) {
        assertThat(matrix.data.size).isEqualTo(expected.data.size)
        for (i in matrix.data.indices) {
            assertThat(matrix.data[i]).isWithin(1e-5f).of(expected.data[i])
        }
    }
}
