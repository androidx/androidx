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

package androidx.ui.baseui.shape.corner

import androidx.test.filters.SmallTest
import androidx.ui.core.Density
import androidx.ui.core.PxSize
import androidx.ui.core.dp
import androidx.ui.core.px
import androidx.ui.core.withDensity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class CornerSizeTest {

    private val density = Density(2.5f)
    private val size = PxSize(150.px, 300.px)

    @Test
    fun pxCorners() = withDensity(density) {
        val corner = CornerSize(24.px)
        assertThat(corner(size)).isEqualTo(24.px)
    }

    @Test
    fun dpCorners() = withDensity(density) {
        val corner = CornerSize(5.dp)
        assertThat(corner(size)).isEqualTo(12.5.px)
    }

    @Test
    fun intPercentCorners() = withDensity(density) {
        val corner = CornerSize(15)
        assertThat(corner(size)).isEqualTo(22.5.px)
    }

    @Test
    fun floatPercentCorners() = withDensity(density) {
        val corner = CornerSize(21.6f)
        assertThat(corner(PxSize(1000.px, 120.px))).isEqualTo(25.92.px)
    }

    @Test
    fun zeroCorners() = withDensity(density) {
        val corner = ZeroCornerSize
        assertThat(corner(size)).isEqualTo(0.px)
    }
}
