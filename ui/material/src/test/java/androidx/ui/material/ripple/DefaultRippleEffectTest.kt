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

import androidx.ui.core.Density
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.PxBounds
import androidx.ui.core.PxSize
import androidx.ui.core.dp
import androidx.ui.core.px
import androidx.ui.core.toBounds
import androidx.ui.core.withDensity
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
        val size = PxSize(100.px, 50.px)
        val coordinates = mock<LayoutCoordinates> {
            on { this.size } doReturn size
        }

        // Top-level functions are not resolved properly in IR modules
        val result = getSurfaceSize(coordinates, null)
        assertThat(result).isEqualTo(size)
    }

    @Test
    fun testSurfaceSizeWithBoundsCallback() {
        val size = PxSize(10.px, 40.px)
        val coordinates = mock<LayoutCoordinates>()
        val boundsCallback: (LayoutCoordinates) -> PxBounds = { size.toBounds() }

        // Top-level functions are not resolved properly in IR modules
        val result = getSurfaceSize(coordinates, boundsCallback)
        assertThat(result).isEqualTo(size)
    }

    @Test
    fun testStartRadius() {
        val size = PxSize(10.px, 30.px)
        val expectedRadius = 9.px // 30% of 30

        // Top-level functions are not resolved properly in IR modules
        val result = getRippleStartRadius(size)
        assertThat(result).isEqualTo(expectedRadius)
    }

    @Test
    fun testTargetRadius() {
        val width = 100f
        val height = 160f
        val size = PxSize(width.px, height.px)
        val density = Density(2f)
        val expectedRadius = withDensity(density) {
            // 10 is an extra offset from spec
            halfDistance(width, height) + 10.dp.toPx().value
        }
        val result = withDensity(density) { getRippleTargetRadius(size) }
        assertThat(result).isEqualTo(expectedRadius.px)
    }

    private fun halfDistance(width: Float, height: Float) =
        sqrt(width * width + height * height) / 2
}
