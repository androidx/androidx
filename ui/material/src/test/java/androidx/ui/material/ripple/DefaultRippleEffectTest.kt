/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.ui.material.ripple

import androidx.ui.core.Bounds
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Size
import androidx.ui.core.dp
import androidx.ui.core.toBounds
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.sqrt

@RunWith(JUnit4::class)
class DefaultRippleEffectTest {

    @Test
    fun testSurfaceSizeWithoutBoundsCallback() {
        val size = Size(100f.dp, 50.dp)
        val coordinates = mock<LayoutCoordinates> {
            on { this.size } doReturn size
        }

        // Top-level functions are not resolved properly in IR modules
        val result = DefaultRippleEffectKt.getSurfaceSize(coordinates, null)
        assertThat(result).isEqualTo(size)
    }

    @Test
    fun testSurfaceSizeWithBoundsCallback() {
        val size = Size(10f.dp, 40.dp)
        val coordinates = mock<LayoutCoordinates>()
        val boundsCallback: (LayoutCoordinates) -> Bounds = { size.toBounds() }

        // Top-level functions are not resolved properly in IR modules
        val result = DefaultRippleEffectKt.getSurfaceSize(coordinates, boundsCallback)
        assertThat(result).isEqualTo(size)
    }

    @Test
    fun testStartRadius() {
        val size = Size(10f.dp, 30f.dp)
        val expectedRadius = 9.dp // 30% of 30

        // Top-level functions are not resolved properly in IR modules
        val result = DefaultRippleEffectKt.getRippleStartRadius(size)
        assertThat(result).isEqualTo(expectedRadius)
    }

    @Test
    fun testTargetRadius() {
        val width = 100f
        val height = 160f
        val size = Size(width.dp, height.dp)
        val expectedRadius = halfDistance(width, height) + 10 // 10 is an extra offset from spec

        // Top-level functions are not resolved properly in IR modules
        val result = DefaultRippleEffectKt.getRippleTargetRadius(size)
        assertThat(result).isEqualTo(expectedRadius.dp)
    }

    private fun halfDistance(width: Float, height: Float) =
        sqrt(width * width + height * height) / 2
}
