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

import androidx.ui.unit.Density
import androidx.ui.unit.PxSize
import androidx.ui.unit.dp
import androidx.ui.unit.px
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.sqrt

@RunWith(JUnit4::class)
class DefaultRippleEffectTest {

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
        val expectedRadius = with(density) {
            // 10 is an extra offset from spec
            halfDistance(width, height) + 10.dp.toPx().value
        }
        val result = with(density) { getRippleTargetRadius(size) }
        assertThat(result).isEqualTo(expectedRadius.px)
    }

    private fun halfDistance(width: Float, height: Float) =
        sqrt(width * width + height * height) / 2
}
