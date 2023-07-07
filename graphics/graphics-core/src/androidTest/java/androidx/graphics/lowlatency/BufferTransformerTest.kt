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

package androidx.graphics.lowlatency

import android.opengl.Matrix
import android.os.Build
import androidx.graphics.surface.SurfaceControlCompat.Companion.BUFFER_TRANSFORM_IDENTITY
import androidx.graphics.surface.SurfaceControlCompat.Companion.BUFFER_TRANSFORM_ROTATE_180
import androidx.graphics.surface.SurfaceControlCompat.Companion.BUFFER_TRANSFORM_ROTATE_270
import androidx.graphics.surface.SurfaceControlCompat.Companion.BUFFER_TRANSFORM_ROTATE_90
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
@RunWith(AndroidJUnit4::class)
@SmallTest
internal class BufferTransformerTest {

    companion object {
        const val THRESHOLD = .0001f
        const val WIDTH = 2560
        const val HEIGHT = 1200
        const val SIZE = 16
    }

    @Test
    fun testIdentity() {
        val transform = BufferTransformer().apply {
            computeTransform(WIDTH, HEIGHT, BUFFER_TRANSFORM_IDENTITY)
        }
        assertEquals(transform.glWidth, WIDTH)
        assertEquals(transform.glHeight, HEIGHT)
        val expected = createMatrix()
        assertEquals(transform.transform.size, SIZE)
        assertIsEqual(transform.transform, expected)
        assertEquals(BUFFER_TRANSFORM_IDENTITY, transform.computedTransform)
    }

    @Test
    fun test90rotation() {
        val transform = BufferTransformer().apply {
            computeTransform(WIDTH, HEIGHT, BUFFER_TRANSFORM_ROTATE_90)
        }
        assertEquals(transform.glWidth, HEIGHT)
        assertEquals(transform.glHeight, WIDTH)
        val expected = computeResult(
            createMatrix(),
            createMatrix {
                Matrix.setRotateM(this, 0, -90f, 0f, 0f, 1f)
                Matrix.translateM(this, 0, -WIDTH.toFloat(), 0f, 0f)
            }
        )
        assertIsEqual(transform.transform, expected)
        assertEquals(BUFFER_TRANSFORM_ROTATE_90, transform.computedTransform)
    }

    @Test
    fun test180rotation() {
        val transform = BufferTransformer().apply {
            computeTransform(WIDTH, HEIGHT, BUFFER_TRANSFORM_ROTATE_180)
        }
        assertEquals(transform.glWidth, WIDTH)
        assertEquals(transform.glHeight, HEIGHT)
        val expected = computeResult(
            createMatrix(),
            createMatrix {
                Matrix.setRotateM(this, 0, 180f, 0f, 0f, 1f)
                Matrix.translateM(this, 0, -WIDTH.toFloat(), -HEIGHT.toFloat(), 0f)
            }
        )
        assertIsEqual(transform.transform, expected)
        assertEquals(BUFFER_TRANSFORM_ROTATE_180, transform.computedTransform)
    }

    @Test
    fun test270rotation() {
        val transform = BufferTransformer().apply {
            computeTransform(WIDTH, HEIGHT, BUFFER_TRANSFORM_ROTATE_270)
        }
        assertEquals(transform.glWidth, HEIGHT)
        assertEquals(transform.glHeight, WIDTH)
        val expected = computeResult(
            createMatrix(),
            createMatrix {
                Matrix.setRotateM(this, 0, 90f, 0f, 0f, 1f)
                Matrix.translateM(this, 0, 0f, -HEIGHT.toFloat(), 0f)
            }
        )
        assertIsEqual(transform.transform, expected)
        assertEquals(BUFFER_TRANSFORM_ROTATE_270, transform.computedTransform)
    }

    @Test
    fun testUnknown() {
        val transform = BufferTransformer().apply {
            computeTransform(WIDTH, HEIGHT, 42)
        }
        assertEquals(transform.glWidth, WIDTH)
        assertEquals(transform.glHeight, HEIGHT)
        val expected = createMatrix()
        assertEquals(transform.transform.size, SIZE)
        assertIsEqual(transform.transform, expected)
        assertEquals(BufferTransformHintResolver.UNKNOWN_TRANSFORM, transform.computedTransform)
    }

    private inline fun createMatrix(block: FloatArray.() -> Unit = {}): FloatArray =
        FloatArray(SIZE).apply {
            Matrix.setIdentityM(this, 0)
            block(this)
        }

    private fun computeResult(ortho: FloatArray, transform: FloatArray): FloatArray =
        createMatrix {
            Matrix.multiplyMM(this, 0, ortho, 0, transform, 0)
        }

    private fun assertIsEqual(actual: FloatArray, expected: FloatArray) {
        assertEquals(actual.size, SIZE)
        for (i in 0 until SIZE) {
            val result = Math.abs(actual[i] - expected[i]) < THRESHOLD
            assertTrue("Index: $i, actual: ${actual[i]} expected: ${expected[i]}", result)
        }
    }
}
