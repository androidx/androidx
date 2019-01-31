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
    fun testRippleTargetRadiusWithoutBoundsCallback() {
        val width = 100f
        val height = 50f
        val coordinates = mock<LayoutCoordinates> {
            on { this.size } doReturn Size(width.dp, height.dp)
        }

        // Top-level functions are not resolved properly in IR modules
        val result = DefaultRippleEffectKt.getRippleTargetRadius(coordinates, null)
        assertThat(result.dp).isEqualTo(halfDistance(width, height))
    }

    @Test
    fun testRippleTargetRadiusWithBoundsCallback() {
        val width = 10f
        val height = 40f
        val coordinates = mock<LayoutCoordinates>()
        val boundsCallback: (LayoutCoordinates) -> Bounds =
            { Bounds(0.dp, 0.dp, width.dp, height.dp) }

        // Top-level functions are not resolved properly in IR modules
        val result = DefaultRippleEffectKt.getRippleTargetRadius(coordinates, boundsCallback)
        assertThat(result.dp).isEqualTo(halfDistance(width, height))
    }

    private fun halfDistance(width: Float, height: Float) =
        sqrt(width * width + height * height) / 2
}
