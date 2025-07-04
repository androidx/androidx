/*
 * Copyright 2025 The Android Open Source Project
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

import android.util.Size
import android.util.SizeF
import android.view.Surface
import androidx.camera.viewfinder.core.impl.RotationValue
import androidx.camera.viewfinder.core.impl.Transformations
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.internal.DoNotInstrument

// Size of the PreviewView. Aspect ratio 2:1.
private val VIEWFINDER_SIZE = Size(400, 200)

// Size of the Surface. Aspect ratio 3:2.
private val SURFACE_SIZE = Size(60, 40)

/** Unit tests for [androidx.camera.viewfinder.core.impl.Transformations]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
class TransformationsTest {

    @Test
    fun cropRectWidthOffByOnePixel_match() {
        assertThat(
                Transformations.isViewportAspectRatioMatchViewfinder(
                    SizeF(
                        (VIEWFINDER_SIZE.width + 1).toFloat(),
                        (VIEWFINDER_SIZE.height - 1).toFloat(),
                    ),
                    VIEWFINDER_SIZE,
                )
            )
            .isTrue()
    }

    @Test
    fun cropRectWidthOffByTwoPixels_mismatch() {
        assertThat(
                Transformations.isViewportAspectRatioMatchViewfinder(
                    SizeF(
                        (VIEWFINDER_SIZE.width + 2).toFloat(),
                        (VIEWFINDER_SIZE.height - 2).toFloat(),
                    ),
                    VIEWFINDER_SIZE,
                )
            )
            .isFalse()
    }

    @Test
    fun correctTextureViewWith0Rotation() {
        assertThat(getTextureViewCorrection(Surface.ROTATION_0))
            .isEqualTo(
                intArrayOf(
                    0,
                    0,
                    SURFACE_SIZE.width,
                    0,
                    SURFACE_SIZE.width,
                    SURFACE_SIZE.height,
                    0,
                    SURFACE_SIZE.height,
                )
            )
    }

    @Test
    fun correctTextureViewWith90Rotation() {
        assertThat(getTextureViewCorrection(Surface.ROTATION_90))
            .isEqualTo(
                intArrayOf(
                    0,
                    SURFACE_SIZE.height,
                    0,
                    0,
                    SURFACE_SIZE.width,
                    0,
                    SURFACE_SIZE.width,
                    SURFACE_SIZE.height,
                )
            )
    }

    @Test
    fun correctTextureViewWith180Rotation() {
        assertThat(getTextureViewCorrection(Surface.ROTATION_180))
            .isEqualTo(
                intArrayOf(
                    SURFACE_SIZE.width,
                    SURFACE_SIZE.height,
                    0,
                    SURFACE_SIZE.height,
                    0,
                    0,
                    SURFACE_SIZE.width,
                    0,
                )
            )
    }

    @Test
    fun correctTextureViewWith270Rotation() {
        assertThat(getTextureViewCorrection(Surface.ROTATION_270))
            .isEqualTo(
                intArrayOf(
                    SURFACE_SIZE.width,
                    0,
                    SURFACE_SIZE.width,
                    SURFACE_SIZE.height,
                    0,
                    SURFACE_SIZE.height,
                    0,
                    0,
                )
            )
    }

    /** Corrects TextureView based on target rotation and return the corrected vertices. */
    private fun getTextureViewCorrection(@RotationValue rotation: Int): IntArray =
        SURFACE_SIZE.toVertices()
            .apply {
                Transformations.getTextureViewCorrectionMatrix(
                        displayRotationDegrees =
                            Transformations.surfaceRotationToRotationDegrees(rotation),
                        width = SURFACE_SIZE.width,
                        height = SURFACE_SIZE.height,
                    )
                    .mapPoints(this)
            }
            .convertToIntArray()

    private fun FloatArray.convertToIntArray(): IntArray {
        var result = IntArray(size)

        for ((index, element) in withIndex()) {
            result[index] = element.roundToInt()
        }

        return result
    }

    /** Converts a {@link Size} to a float array of vertices. */
    private fun Size.toVertices(): FloatArray =
        floatArrayOf(
            0f,
            0f,
            width.toFloat(),
            0f,
            width.toFloat(),
            height.toFloat(),
            0f,
            height.toFloat(),
        )
}
