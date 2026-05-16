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

package androidx.xr.scenecore

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config as RoboConfig

@RunWith(RobolectricTestRunner::class)
@RoboConfig(sdk = [RoboConfig.TARGET_SDK])
class PixelDensityTest {

    @Test
    fun constructor_setsPixelsPerMeter() {
        val density = PixelDensity(1234f)

        assertThat(density.pixelsPerMeter).isEqualTo(1234f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun constructor_withZero_throwsException() {
        PixelDensity(0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun constructor_withNegative_throwsException() {
        PixelDensity(-100f)
    }

    @Test
    fun convertMetersToPixels_returnsCorrectValue() {
        val density = PixelDensity(2000f)

        assertThat(density.convertMetersToPixels(0.5f)).isEqualTo(1000f)
        assertThat(density.convertMetersToPixels(2f)).isEqualTo(4000f)
    }

    @Test
    fun convertPixelsToMeters_returnsCorrectValue() {
        val density = PixelDensity(2000f)

        assertThat(density.convertPixelsToMeters(1000f)).isEqualTo(0.5f)
        assertThat(density.convertPixelsToMeters(4000f)).isEqualTo(2f)
    }
}
