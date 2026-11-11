/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.viewfinder.impl

import android.graphics.PointF
import androidx.camera.viewfinder.core.impl.ContentRotationGeometry
import androidx.camera.viewfinder.core.impl.ContentRotationGeometry.Companion.BUFFER_TRANSFORM_IDENTITY
import androidx.camera.viewfinder.core.impl.ContentRotationGeometry.Companion.BUFFER_TRANSFORM_ROTATE_180
import androidx.camera.viewfinder.core.impl.ContentRotationGeometry.Companion.BUFFER_TRANSFORM_ROTATE_270
import androidx.camera.viewfinder.core.impl.ContentRotationGeometry.Companion.BUFFER_TRANSFORM_ROTATE_90
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private const val WIDTH = 1080
private const val HEIGHT = 1920

/** Unit tests for [ContentRotationGeometry]. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@DoNotInstrument
class ContentRotationGeometryTest {

    @Test
    fun rotations_rotateClockwiseAndFitOriginalBounds() {
        for ((rotationDegrees, transform) in
            listOf(
                0 to BUFFER_TRANSFORM_IDENTITY,
                90 to BUFFER_TRANSFORM_ROTATE_270,
                180 to BUFFER_TRANSFORM_ROTATE_180,
                270 to BUFFER_TRANSFORM_ROTATE_90,
            )) {
            val geometry = ContentRotationGeometry.create(rotationDegrees, WIDTH, HEIGHT)

            assertThat(geometry.transform).isEqualTo(transform)
            assertContentRotated(geometry, rotationDegrees, WIDTH, HEIGHT)
        }
    }

    @Test
    fun negativeRotation_isNormalized() {
        val geometry = ContentRotationGeometry.create(-90, WIDTH, HEIGHT)

        assertThat(geometry.transform).isEqualTo(BUFFER_TRANSFORM_ROTATE_90)
    }

    @Test
    fun invalidArguments_throws() {
        // An invalid rotation, an invalid size, and sizes too large to rotate without overflow.
        for ((rotationDegrees, width, height) in
            listOf(
                Triple(45, WIDTH, HEIGHT),
                Triple(90, 0, HEIGHT),
                Triple(90, 50_000, 50_001),
                Triple(90, Int.MAX_VALUE, Int.MAX_VALUE),
            )) {
            assertThrows(IllegalArgumentException::class.java) {
                ContentRotationGeometry.create(rotationDegrees, width, height)
            }
        }
    }

    @Test
    fun squareContent_doesNotCropAndMapsCornersAccurately() {
        for (rotationDegrees in listOf(90, 270)) {
            val geometry = ContentRotationGeometry.create(rotationDegrees, 1080, 1080)

            assertContentRotated(geometry, rotationDegrees, 1080, 1080)
        }
    }

    @Test
    fun resolution8k_preservesExactFloat32Precision() {
        val width8k = 7680
        val height8k = 4320
        val maxExactFloat32Int = 1 shl 24 // 16,777,216
        for (rotationDegrees in listOf(90, 270)) {
            val geometry = ContentRotationGeometry.create(rotationDegrees, width8k, height8k)

            assertThat(geometry.sourceCrop.right).isAtMost(maxExactFloat32Int)
            assertThat(abs(geometry.destinationFrame.left)).isAtMost(maxExactFloat32Int)
            assertThat(abs(geometry.destinationFrame.top)).isAtMost(maxExactFloat32Int)
            assertThat(abs(geometry.destinationFrame.right)).isAtMost(maxExactFloat32Int)
            assertThat(abs(geometry.destinationFrame.bottom)).isAtMost(maxExactFloat32Int)

            assertContentRotated(geometry, rotationDegrees, width8k, height8k)
        }
    }

    @Test
    fun coprimeDimensions_mapsCornersAccurately() {
        val coprimeWidth = 1921
        val coprimeHeight = 1081
        val geometry = ContentRotationGeometry.create(90, coprimeWidth, coprimeHeight)

        assertContentRotated(geometry, 90, coprimeWidth, coprimeHeight)
    }

    /**
     * Asserts that [geometry] crops the whole [width]x[height] content and maps its corners to the
     * corners of the same bounds rotated clockwise by [rotationDegrees].
     */
    private fun assertContentRotated(
        geometry: ContentRotationGeometry,
        rotationDegrees: Int,
        width: Int,
        height: Int,
    ) {
        assertThat(geometry.sourceCrop.left).isEqualTo(0)
        assertThat(geometry.sourceCrop.top).isEqualTo(0)
        assertThat(geometry.sourceCrop.right).isAtLeast(width)
        assertThat(geometry.sourceCrop.bottom).isAtLeast(height)

        val w = width.toFloat()
        val h = height.toFloat()
        val (topLeft, topRight, bottomLeft) =
            when (rotationDegrees) {
                0 -> listOf(PointF(0f, 0f), PointF(w, 0f), PointF(0f, h))
                90 -> listOf(PointF(w, 0f), PointF(w, h), PointF(0f, 0f))
                180 -> listOf(PointF(w, h), PointF(0f, h), PointF(w, 0f))
                270 -> listOf(PointF(0f, h), PointF(0f, 0f), PointF(w, h))
                else -> throw IllegalArgumentException("Unexpected rotation: $rotationDegrees")
            }
        assertPointEquals(geometry.map(0f, 0f), topLeft)
        assertPointEquals(geometry.map(w, 0f), topRight)
        assertPointEquals(geometry.map(0f, h), bottomLeft)
    }

    private fun assertPointEquals(actual: PointF, expected: PointF) {
        assertThat(actual.x).isWithin(1e-3f).of(expected.x)
        assertThat(actual.y).isWithin(1e-3f).of(expected.y)
    }

    /**
     * Maps a point of the layer to its parent in the same way as the native implementation of
     * `SurfaceControl.Transaction.setGeometry()`.
     */
    private fun ContentRotationGeometry.map(x: Float, y: Float): PointF {
        val xs = destinationFrame.width().toFloat() / sourceCrop.width()
        val ys = destinationFrame.height().toFloat() / sourceCrop.height()
        val l = destinationFrame.left - xs * sourceCrop.left
        val t = destinationFrame.top - ys * sourceCrop.top
        return when (transform) {
            BUFFER_TRANSFORM_ROTATE_90 -> PointF(l + sourceCrop.height() + xs * y, t - ys * x)
            BUFFER_TRANSFORM_ROTATE_180 ->
                PointF(l + sourceCrop.width() - xs * x, t + sourceCrop.height() - ys * y)
            BUFFER_TRANSFORM_ROTATE_270 -> PointF(l - xs * y, t + sourceCrop.width() + ys * x)
            else -> PointF(l + xs * x, t + ys * y)
        }
    }
}
