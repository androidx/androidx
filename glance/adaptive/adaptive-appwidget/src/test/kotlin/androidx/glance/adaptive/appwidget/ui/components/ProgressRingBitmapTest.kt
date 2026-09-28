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

package androidx.glance.adaptive.appwidget.ui.components

import android.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the size clamping in [createProgressRingBitmap], which has to survive degenerate sizes.
 */
@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class ProgressRingBitmapTest {

    @Test
    fun createProgressRingBitmap_atOnePixel_clampsUpToTwoPixels() {
        // A 1 px edge would make the stroke's upper bound (edge / 2 = 0.5) fall below its 1f
        // floor, and coerceIn throws on an inverted range, so it must not throw.
        val bitmap = ring(sizePx = 1, strokeWidthPx = 4f)

        assertThat(bitmap.width).isEqualTo(2)
        assertThat(bitmap.height).isEqualTo(2)
    }

    @Test
    fun createProgressRingBitmap_atZeroOrNegative_clampsUpToTwoPixels() {
        // Bitmap.createBitmap rejects a non-positive edge, so the floor has to hold before it.
        assertThat(ring(sizePx = 0, strokeWidthPx = 4f).width).isEqualTo(2)
        assertThat(ring(sizePx = -10, strokeWidthPx = 4f).width).isEqualTo(2)
    }

    @Test
    fun createProgressRingBitmap_beyondTheCap_clampsToMaxRingSize() {
        val bitmap = ring(sizePx = MAX_RING_SIZE_PX + 100, strokeWidthPx = 8f)

        assertThat(bitmap.width).isEqualTo(MAX_RING_SIZE_PX)
        assertThat(bitmap.height).isEqualTo(MAX_RING_SIZE_PX)
    }

    @Test
    fun createProgressRingBitmap_withoutProgress_stillRasterizesTheTrack() {
        assertThat(ring(sizePx = 56, strokeWidthPx = 4f, progress = null).width).isEqualTo(56)
    }

    private fun ring(sizePx: Int, strokeWidthPx: Float, progress: Float? = 0.5f) =
        createProgressRingBitmap(
            sizePx = sizePx,
            strokeWidthPx = strokeWidthPx,
            progress = progress,
            trackColor = Color.LTGRAY,
            progressColor = Color.BLUE,
            containerColor = Color.WHITE,
        )
}
