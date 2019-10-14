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

package androidx.ui.foundation.shape.corner

import androidx.test.filters.SmallTest
import androidx.ui.core.Density
import androidx.ui.core.PxSize
import androidx.ui.core.dp
import androidx.ui.core.px
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
    fun pxCorners() {
        val corner = CornerSize(24.px)
        assertThat(corner.toPx(size, density)).isEqualTo(24.px)
    }

    @Test
    fun dpCorners() {
        val corner = CornerSize(5.dp)
        assertThat(corner.toPx(size, density)).isEqualTo(12.5.px)
    }

    @Test
    fun intPercentCorners() {
        val corner = CornerSize(15)
        assertThat(corner.toPx(size, density)).isEqualTo(22.5.px)
    }

    @Test
    fun floatPercentCorners() {
        val corner = CornerSize(21.6f)
        assertThat(corner.toPx(PxSize(1000.px, 120.px), density)).isEqualTo(25.92.px)
    }

    @Test
    fun zeroCorners() {
        val corner = ZeroCornerSize
        assertThat(corner.toPx(size, density)).isEqualTo(0.px)
    }

    @Test
    fun pxCornersAreEquals() {
        assertThat(CornerSize(24.px)).isEqualTo(CornerSize(24.px))
    }

    @Test
    fun dpCornersAreEquals() {
        assertThat(CornerSize(8.dp)).isEqualTo(CornerSize(8.dp))
    }

    @Test
    fun percentCornersAreEquals() {
        assertThat(CornerSize(20f)).isEqualTo(CornerSize(20))
    }
}
