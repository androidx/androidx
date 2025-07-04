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

package androidx.xr.compose.subspace.layout

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpatialSmoothFeatheringSizeTest {
    private val pixelHeight = 100f
    private val pixelWidth = 200f
    private val density = Density(density = 2.0f)

    @Test
    fun percent() {
        val size = spatialSmoothFeatheringSize(25, 25)

        assertThat(size.toHeightPercent(pixelHeight, density)).isEqualTo(0.25f)
        assertThat(size.toWidthPercent(pixelWidth, density)).isEqualTo(0.25f)
    }

    fun dp() {
        val size = spatialSmoothFeatheringSize(25.dp, 25.dp)

        assertThat(size.toHeightPercent(pixelHeight, density)).isEqualTo(0.5f)
        assertThat(size.toWidthPercent(pixelWidth, density)).isEqualTo(0.25f)
    }

    fun dp_coerced() {
        val size = spatialSmoothFeatheringSize(1000.dp, 1000.dp)

        assertThat(size.toHeightPercent(pixelHeight, density)).isEqualTo(0.5f)
        assertThat(size.toWidthPercent(pixelWidth, density)).isEqualTo(0.5f)
    }

    fun pixel() {
        val size = spatialSmoothFeatheringSize(25f, 25f)

        assertThat(size.toHeightPercent(pixelHeight, density)).isEqualTo(0.25f)
        assertThat(size.toWidthPercent(pixelWidth, density)).isEqualTo(0.125f)
    }

    fun pixel_coerced() {
        val size = spatialSmoothFeatheringSize(1000f, 1000f)

        assertThat(size.toHeightPercent(pixelHeight, density)).isEqualTo(0.5f)
        assertThat(size.toWidthPercent(pixelWidth, density)).isEqualTo(0.5f)
    }
}
