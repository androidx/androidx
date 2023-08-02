/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.core.impl.utils

import android.opengl.Matrix
import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private const val FLOAT_TOLERANCE = 1E-4

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class MatrixExtTest {

    @Test
    fun setRotate() {
        val transform = createIdentityMatrix().also {
            // 0.5x scaling on the Y axis
            Matrix.scaleM(it, 0, 1f, 0.5f, 1f)
        }

        // Act.
        // 90° clockwise rotation around (0.5, 0.5).
        MatrixExt.setRotate(transform, -90f, 0.5f, 0.5f)

        // Assert.
        // Scaling should be gone.
        //      90° rotation
        // (0,1) -------------> (1,1)
        val data = floatArrayOf(0f, 1f, 0f, 1f)
        val result = FloatArray(4)
        Matrix.multiplyMV(result, 0, transform, 0, data, 0)
        assertThat(result).usingTolerance(FLOAT_TOLERANCE)
            .containsExactly(floatArrayOf(1f, 1f, 0f, 1f))
    }

    @Test
    fun preRotate() {
        val transform = createIdentityMatrix().also {
            // 0.5x scaling on the Y axis
            Matrix.scaleM(it, 0, 1f, 0.5f, 1f)
        }

        // Act.
        // 90° clockwise rotation around (0.5, 0.5).
        MatrixExt.preRotate(transform, -90f, 0.5f, 0.5f)

        // Assert.
        //      90° rotation         0.5x scaling on the Y axis
        // (0,1) -------------> (1,1) -------------------------> (1,0.5)
        val data = floatArrayOf(0f, 1f, 0f, 1f)
        val result = FloatArray(4)
        Matrix.multiplyMV(result, 0, transform, 0, data, 0)
        assertThat(result).usingTolerance(FLOAT_TOLERANCE)
            .containsExactly(floatArrayOf(1f, 0.5f, 0f, 1f))
    }

    @Test
    fun postRotate() {
        val transform = createIdentityMatrix().also {
            // 0.5x scaling on the Y axis
            Matrix.scaleM(it, 0, 1f, 0.5f, 1f)
        }

        // Act.
        // 90° clockwise rotation around (0.5, 0.5).
        MatrixExt.postRotate(transform, -90f, 0.5f, 0.5f)

        // Assert.
        //      0.5x scaling on the Y axis         90° rotation
        // (0,1) ------------------------> (0,0.5) ------------> (0.5,1)
        val data = floatArrayOf(0f, 1f, 0f, 1f)
        val result = FloatArray(4)
        Matrix.multiplyMV(result, 0, transform, 0, data, 0)
        assertThat(result).usingTolerance(FLOAT_TOLERANCE)
            .containsExactly(floatArrayOf(0.5f, 1f, 0f, 1f))
    }

    private fun createIdentityMatrix() = FloatArray(16).apply { setIdentity() }

    private fun FloatArray.setIdentity() {
        Matrix.setIdentityM(this, 0)
    }
}
