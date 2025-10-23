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

import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import kotlin.math.sqrt
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val delta = 0.01f

@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidMatrixTest {

    @Test
    fun rotate90() {
        val point = FloatArray(2)
        val m = Matrix()
        m.rotateZ(90f)
        val p = android.graphics.Matrix().apply { setFrom(m) }
        p.mapPoints(point, floatArrayOf(0f, 0f))
        assertThat(point[0]).isWithin(delta).of(0f)
        assertThat(point[1]).isWithin(delta).of(0f)
        p.mapPoints(point, floatArrayOf(100f, 100f))
        assertThat(point[0]).isWithin(delta).of(-100f)
        assertThat(point[1]).isWithin(delta).of(100f)

        val composeMatrix = Matrix().apply { setFrom(p) }
        assertTrue(composeMatrix.values.contentAlmostEquals(m.values))
    }

    @Test
    fun rotate30() {
        val point = FloatArray(2)
        val m = Matrix()
        m.rotateZ(30f)
        val p = android.graphics.Matrix().apply { setFrom(m) }
        p.mapPoints(point, floatArrayOf(0f, 0f))
        assertThat(point[0]).isWithin(delta).of(0f)
        assertThat(point[1]).isWithin(delta).of(0f)
        p.mapPoints(point, floatArrayOf(100f, 0f))
        assertThat(point[0]).isWithin(delta).of(86.60254f)
        assertThat(point[1]).isWithin(delta).of(50f)

        val composeMatrix = Matrix().apply { setFrom(p) }
        assertTrue(composeMatrix.values.contentAlmostEquals(m.values))
    }

    @Test
    fun translateX() {
        val point = FloatArray(2)
        val m = Matrix()
        m.translate(10f, 0f)
        val p = android.graphics.Matrix().apply { setFrom(m) }
        p.mapPoints(point, floatArrayOf(0f, 0f))
        assertThat(point[0]).isWithin(delta).of(10f)
        assertThat(point[1]).isWithin(delta).of(0f)
        p.mapPoints(point, floatArrayOf(100f, 100f))
        assertThat(point[0]).isWithin(delta).of(110f)
        assertThat(point[1]).isWithin(delta).of(100f)

        val composeMatrix = Matrix().apply { setFrom(p) }
        assertTrue(composeMatrix.values.contentAlmostEquals(m.values))
    }

    @Test
    fun translateY() {
        val point = FloatArray(2)
        val m = Matrix()
        m.translate(0f, 10f)
        val p = android.graphics.Matrix().apply { setFrom(m) }
        p.mapPoints(point, floatArrayOf(0f, 0f))
        assertThat(point[0]).isWithin(delta).of(0f)
        assertThat(point[1]).isWithin(delta).of(10f)
        p.mapPoints(point, floatArrayOf(100f, 100f))
        val message = "Matrix:\n$m\nPlatform:\n$p"
        assertWithMessage(message).that(point[0]).isWithin(delta).of(100f)
        assertWithMessage(message).that(point[1]).isWithin(delta).of(110f)
        m.translate(0f, 10f)
        val q = android.graphics.Matrix().apply { setFrom(m) }
        q.mapPoints(point, floatArrayOf(0f, 0f))
        assertThat(point[0]).isWithin(delta).of(0f)
        assertThat(point[1]).isWithin(delta).of(20f)

        val composeMatrix = Matrix().apply { setFrom(q) }
        assertTrue(composeMatrix.values.contentAlmostEquals(m.values))
    }

    @Test
    fun scale() {
        val point = FloatArray(2)
        val m = Matrix()
        m.scale(2f, 3f)
        val p = android.graphics.Matrix().apply { setFrom(m) }
        p.mapPoints(point, floatArrayOf(0f, 0f))
        assertThat(point[0]).isWithin(delta).of(0f)
        assertThat(point[1]).isWithin(delta).of(0f)
        p.mapPoints(point, floatArrayOf(100f, 100f))
        assertThat(point[0]).isWithin(delta).of(200f)
        assertThat(point[1]).isWithin(delta).of(300f)

        val composeMatrix = Matrix().apply { setFrom(p) }
        assertTrue(composeMatrix.values.contentAlmostEquals(m.values))
    }

    @Test
    fun rotate90Scale() {
        val point = FloatArray(2)
        val m = Matrix()
        m.rotateZ(90f)
        m.scale(2f, 3f)
        val p = android.graphics.Matrix().apply { setFrom(m) }
        p.mapPoints(point, floatArrayOf(0f, 0f))
        assertThat(point[0]).isWithin(delta).of(0f)
        assertThat(point[1]).isWithin(delta).of(0f)
        p.mapPoints(point, floatArrayOf(100f, 100f))
        assertThat(point[0]).isWithin(delta).of(-300f)
        assertThat(point[1]).isWithin(delta).of(200f)

        val composeMatrix = Matrix().apply { setFrom(p) }
        assertTrue(composeMatrix.values.contentAlmostEquals(m.values))
    }

    @Test
    fun rotateX45() {
        val m = Matrix().apply { rotateX(45f) }
        val mapped00 = m.map(Offset(0f, 0f))
        assertThat(mapped00.x).isWithin(delta).of(0f)
        assertThat(mapped00.y).isWithin(delta).of(0f)
        val mapped11 = m.map(Offset(1f, 1f))
        assertThat(mapped11.x).isWithin(delta).of(1f)
        assertThat(mapped11.y).isWithin(delta).of(sqrt(2f) / 2f)

        val androidMatrix = android.graphics.Matrix().apply { setFrom(m) }
        val points = floatArrayOf(0f, 0f, 1f, 1f)
        androidMatrix.mapPoints(points)
        assertThat(points[0]).isWithin(delta).of(0f)
        assertThat(points[1]).isWithin(delta).of(0f)
        assertThat(points[2]).isWithin(delta).of(1f)
        assertThat(points[3]).isWithin(delta).of(sqrt(2f) / 2f)
    }

    @Test
    fun rotateY45() {
        val m = Matrix().apply { rotateY(45f) }
        val mapped00 = m.map(Offset(0f, 0f))
        assertThat(mapped00.x).isWithin(delta).of(0f)
        assertThat(mapped00.y).isWithin(delta).of(0f)
        val mapped11 = m.map(Offset(1f, 1f))
        assertThat(mapped11.x).isWithin(delta).of(sqrt(2f) / 2f)
        assertThat(mapped11.y).isWithin(delta).of(1f)

        val androidMatrix = android.graphics.Matrix().apply { setFrom(m) }
        val points = floatArrayOf(0f, 0f, 1f, 1f)
        androidMatrix.mapPoints(points)
        assertThat(points[0]).isWithin(delta).of(0f)
        assertThat(points[1]).isWithin(delta).of(0f)
        assertThat(points[2]).isWithin(delta).of(sqrt(2f) / 2f)
        assertThat(points[3]).isWithin(delta).of(1f)
    }
}

private fun FloatArray.contentAlmostEquals(values: FloatArray, tolerance: Float = 1e-4f): Boolean {
    if (size != values.size) return false
    for (i in indices) {
        if (abs(this[i] - values[i]) > tolerance) return false
    }
    return true
}
