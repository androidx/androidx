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

package androidx.camera.view

import android.graphics.PointF
import android.graphics.Rect
import androidx.camera.view.ContentRotationLayer.Geometry
import androidx.camera.view.ContentRotationLayer.Geometry.Companion.BUFFER_TRANSFORM_IDENTITY
import androidx.camera.view.ContentRotationLayer.Geometry.Companion.BUFFER_TRANSFORM_ROTATE_180
import androidx.camera.view.ContentRotationLayer.Geometry.Companion.BUFFER_TRANSFORM_ROTATE_270
import androidx.camera.view.ContentRotationLayer.Geometry.Companion.BUFFER_TRANSFORM_ROTATE_90
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Unit tests for [ContentRotationLayer.Geometry]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@SdkSuppress(minSdkVersion = 29)
@Config(sdk = [Config.TARGET_SDK])
class ContentRotationLayerTest {

    @Test
    fun noRotation_keepsBounds() {
        val geometry = Geometry.create(0, 640, 480)

        assertThat(geometry.sourceCrop).isEqualTo(Rect(0, 0, 640, 480))
        assertThat(geometry.destinationFrame).isEqualTo(Rect(0, 0, 640, 480))
        assertThat(geometry.transform).isEqualTo(BUFFER_TRANSFORM_IDENTITY)
    }

    @Test
    fun halfTurn_rotatesWithinBounds() {
        val geometry = Geometry.create(180, 640, 480)

        assertThat(geometry.sourceCrop).isEqualTo(Rect(0, 0, 640, 480))
        assertThat(geometry.destinationFrame).isEqualTo(Rect(0, 0, 640, 480))
        assertThat(geometry.transform).isEqualTo(BUFFER_TRANSFORM_ROTATE_180)
    }

    @Test
    fun clockwiseQuarterTurn_rotatesAndFitsBounds() =
        assertQuarterTurnFitsBounds(rotationDegrees = 90, width = 640, height = 480)

    @Test
    fun counterClockwiseQuarterTurn_rotatesAndFitsBounds() =
        assertQuarterTurnFitsBounds(rotationDegrees = 270, width = 640, height = 480)

    @Test
    fun quarterTurnWithPortraitContent_rotatesAndFitsBounds() =
        assertQuarterTurnFitsBounds(rotationDegrees = 90, width = 1080, height = 1920)

    @Test
    fun quarterTurnWithUnreducedAspectRatio_rotatesAndFitsBounds() =
        assertQuarterTurnFitsBounds(rotationDegrees = 270, width = 1920, height = 1088)

    @Test
    fun negativeRotation_isNormalized() {
        val geometry = Geometry.create(-90, 640, 480)

        assertThat(geometry.transform).isEqualTo(BUFFER_TRANSFORM_ROTATE_90)
    }

    @Test
    fun invalidRotation_throwsException() {
        assertThrows(IllegalArgumentException::class.java) { Geometry.create(45, 640, 480) }
    }

    @Test
    fun invalidSize_throwsException() {
        assertThrows(IllegalArgumentException::class.java) { Geometry.create(90, 0, 480) }
    }

    /**
     * Asserts that the corners of the content are rotated clockwise by [rotationDegrees] and scaled
     * to fit the original `width x height` bounds.
     */
    private fun assertQuarterTurnFitsBounds(rotationDegrees: Int, width: Int, height: Int) {
        val geometry = Geometry.create(rotationDegrees, width, height)
        val w = width.toFloat()
        val h = height.toFloat()
        val clockwise = rotationDegrees == 90
        for (corner in listOf(PointF(0f, 0f), PointF(w, 0f), PointF(w, h), PointF(0f, h))) {
            val expected =
                if (clockwise) {
                    PointF(w - corner.y * w / h, corner.x * h / w)
                } else {
                    PointF(corner.y * w / h, h - corner.x * h / w)
                }
            val actual = mapToParent(geometry, corner)
            assertThat(actual.x).isWithin(1e-3f).of(expected.x)
            assertThat(actual.y).isWithin(1e-3f).of(expected.y)
        }
        // The source crop never crops the content.
        assertThat(geometry.sourceCrop.contains(Rect(0, 0, width, height))).isTrue()
    }

    /** Maps a point of the layer to its parent the same way as `setGeometry()`. */
    private fun mapToParent(geometry: Geometry, point: PointF): PointF {
        val crop = geometry.sourceCrop
        val frame = geometry.destinationFrame
        val xs = frame.width().toFloat() / crop.width()
        val ys = frame.height().toFloat() / crop.height()
        val l = frame.left.toFloat()
        val t = frame.top.toFloat()
        return when (geometry.transform) {
            BUFFER_TRANSFORM_ROTATE_90 -> PointF(l + crop.height() + xs * point.y, t - ys * point.x)
            BUFFER_TRANSFORM_ROTATE_270 -> PointF(l - xs * point.y, t + crop.width() + ys * point.x)
            else -> throw AssertionError("Unexpected transform: ${geometry.transform}")
        }
    }
}
