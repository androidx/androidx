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

package androidx.xr.compose.subspace.draw

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpatialSmoothFeatheringSizeTest {
    private val pixelHeight = 200f
    private val pixelWidth = 400f
    private val density = Density(density = 2.0f)

    @Test
    fun percent() {
        val size = spatialSmoothFeatheringSize(25, 25)

        assertThat(size.toHeightPercent(pixelHeight, density)).isEqualTo(0.25f)
        assertThat(size.toWidthPercent(pixelWidth, density)).isEqualTo(0.25f)
    }

    @Test
    fun dp() {
        val size = spatialSmoothFeatheringSize(25.dp, 25.dp)

        assertThat(size.toHeightPercent(pixelHeight, density)).isEqualTo(0.5f)
        assertThat(size.toWidthPercent(pixelWidth, density)).isEqualTo(0.25f)
    }

    @Test
    fun dp_coerced() {
        val size = spatialSmoothFeatheringSize(1000.dp, 1000.dp)

        assertThat(size.toHeightPercent(pixelHeight, density)).isEqualTo(0.5f)
        assertThat(size.toWidthPercent(pixelWidth, density)).isEqualTo(0.5f)
    }

    @Test
    fun pixel() {
        val size = spatialSmoothFeatheringSize(25f, 25f)

        assertThat(size.toHeightPercent(pixelHeight, density)).isEqualTo(0.25f)
        assertThat(size.toWidthPercent(pixelWidth, density)).isEqualTo(0.125f)
    }

    @Test
    fun pixel_coerced() {
        val size = spatialSmoothFeatheringSize(1000f, 1000f)

        assertThat(size.toHeightPercent(pixelHeight, density)).isEqualTo(0.5f)
        assertThat(size.toWidthPercent(pixelWidth, density)).isEqualTo(0.5f)
    }

    @Test
    fun zeroOrNegativeCanvasPixels_returnsZero() {
        val dpZero = spatialSmoothFeatheringSize(0.dp, 0.dp)
        val dpNonZero = spatialSmoothFeatheringSize(25.dp, 25.dp)
        val pixelZero = spatialSmoothFeatheringSize(0f, 0f)
        val pixelNonZero = spatialSmoothFeatheringSize(25f, 25f)

        for (size in listOf(dpZero, dpNonZero, pixelZero, pixelNonZero)) {
            assertThat(size.toWidthPercent(0f, density)).isEqualTo(0f)
            assertThat(size.toHeightPercent(0f, density)).isEqualTo(0f)
            assertThat(size.toWidthPercent(-10f, density)).isEqualTo(0f)
            assertThat(size.toHeightPercent(-10f, density)).isEqualTo(0f)
        }
    }

    @Test
    fun equalsAndHashCode() {
        val percent1 = spatialSmoothFeatheringEffect(25, 30)
        val percent2 = spatialSmoothFeatheringEffect(25, 30)
        val percentDifferent = spatialSmoothFeatheringEffect(25, 35)
        assertThat(percent1).isEqualTo(percent2)
        assertThat(percent1.hashCode()).isEqualTo(percent2.hashCode())
        assertThat(percent1).isNotEqualTo(percentDifferent)

        val dp1 = spatialSmoothFeatheringEffect(25.dp, 30.dp)
        val dp2 = spatialSmoothFeatheringEffect(25.dp, 30.dp)
        val dpDifferent = spatialSmoothFeatheringEffect(25.dp, 35.dp)
        assertThat(dp1).isEqualTo(dp2)
        assertThat(dp1.hashCode()).isEqualTo(dp2.hashCode())
        assertThat(dp1).isNotEqualTo(dpDifferent)

        val pixel1 = spatialSmoothFeatheringEffect(25f, 30f)
        val pixel2 = spatialSmoothFeatheringEffect(25f, 30f)
        val pixelDifferent = spatialSmoothFeatheringEffect(25f, 35f)
        assertThat(pixel1).isEqualTo(pixel2)
        assertThat(pixel1.hashCode()).isEqualTo(pixel2.hashCode())
        assertThat(pixel1).isNotEqualTo(pixelDifferent)

        assertThat(percent1).isNotEqualTo(dp1)
        assertThat(dp1).isNotEqualTo(pixel1)
    }
}
