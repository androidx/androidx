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
package androidx.ui.material

import androidx.ui.core.Bounds
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Position
import androidx.ui.core.Size
import androidx.ui.core.dp
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.ceil
import kotlin.math.sqrt

@RunWith(JUnit4::class)
class InkSplashTest {

    @Test
    fun testSplashTargetRadiusIsDefaultOneWhenContainedInkWellFalse() {
        // Top-level functions are not resolved properly in IR modules
        val result = InkSplashKt.getSplashTargetRadius(mock<LayoutCoordinates>(), false, null,
            Position(0.dp, 0.dp))

        // Top-level properties are not resolved properly in IR modules
        assertThat(result).isEqualTo(MaterialKt.DefaultSplashRadius)
    }

    @Test
    fun testSplashTargetRadiusWithoutBoundsCallback() {
        val width = 100f
        val height = 50f
        val offset = 10f
        val coordinates = mock<LayoutCoordinates> {
            on { this.size } doReturn Size(width.dp, height.dp)
        }

        val x = offset - width
        val y = offset - height
        val expected = ceil(sqrt(x * x + y * y))

        // Top-level functions are not resolved properly in IR modules
        val result = InkSplashKt.getSplashTargetRadius(coordinates, true, null,
            Position(offset.dp, offset.dp))

        assertThat(result.dp).isEqualTo(expected)
    }

    @Test
    fun testSplashTargetRadiusWithBoundsCallback() {
        val width = 10f
        val height = 200f
        val boundsCallback: (LayoutCoordinates) -> Bounds =
            { Bounds(0.dp, 0.dp, width.dp, height.dp) }

        val expected = ceil(sqrt(width * width + height * height))

        // Top-level functions are not resolved properly in IR modules
        val result = InkSplashKt.getSplashTargetRadius(mock<LayoutCoordinates>(), true,
            boundsCallback, Position(0.dp, 0.dp))

        assertThat(result.dp).isEqualTo(expected)
    }
}
