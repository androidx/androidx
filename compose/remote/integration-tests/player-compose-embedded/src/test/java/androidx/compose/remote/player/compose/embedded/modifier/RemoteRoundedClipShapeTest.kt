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

package androidx.compose.remote.player.compose.embedded.modifier

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemoteRoundedClipShapeTest {

    private val density = Density(1f)
    private val size = Size(100f, 200f)

    @Test
    fun resolvesToFallbackWhenNaN() {
        val topStart = mutableStateOf(Float.NaN)
        val topEnd = mutableStateOf(10f)
        val bottomEnd = mutableStateOf(10f)
        val bottomStart = mutableStateOf(10f)

        val shape = RemoteRoundedClipShape(topStart, topEnd, bottomEnd, bottomStart)
        val outline = shape.createOutline(size, LayoutDirection.Ltr, density) as Outline.Rounded

        // minDimension = 100f, fallback = minDimension / 2f = 50f
        assertEquals(50f, outline.roundRect.topLeftCornerRadius.x)
        assertEquals(10f, outline.roundRect.topRightCornerRadius.x)
    }

    @Test
    fun resolvesToZeroWhenZero() {
        val topStart = mutableStateOf(0f)
        val topEnd = mutableStateOf(10f)
        val bottomEnd = mutableStateOf(10f)
        val bottomStart = mutableStateOf(10f)

        val shape = RemoteRoundedClipShape(topStart, topEnd, bottomEnd, bottomStart)
        val outline = shape.createOutline(size, LayoutDirection.Ltr, density) as Outline.Rounded

        assertEquals(0f, outline.roundRect.topLeftCornerRadius.x)
    }

    @Test
    fun resolvesToPercentageWhenFraction() {
        val topStart = mutableStateOf(0.25f)
        val topEnd = mutableStateOf(10f)
        val bottomEnd = mutableStateOf(10f)
        val bottomStart = mutableStateOf(10f)

        val shape = RemoteRoundedClipShape(topStart, topEnd, bottomEnd, bottomStart)
        val outline = shape.createOutline(size, LayoutDirection.Ltr, density) as Outline.Rounded

        // 0.25 * minDimension (100f) = 25f
        assertEquals(25f, outline.roundRect.topLeftCornerRadius.x)
    }

    @Test
    fun resolvesToAbsoluteValueWhenGreaterThanOne() {
        val topStart = mutableStateOf(15f)
        val topEnd = mutableStateOf(10f)
        val bottomEnd = mutableStateOf(10f)
        val bottomStart = mutableStateOf(10f)

        val shape = RemoteRoundedClipShape(topStart, topEnd, bottomEnd, bottomStart)
        val outline = shape.createOutline(size, LayoutDirection.Ltr, density) as Outline.Rounded

        assertEquals(15f, outline.roundRect.topLeftCornerRadius.x)
    }

    @Test
    fun updatesReactively() {
        val topStart = mutableStateOf(0f)
        val topEnd = mutableStateOf(10f)
        val bottomEnd = mutableStateOf(10f)
        val bottomStart = mutableStateOf(10f)

        val shape = RemoteRoundedClipShape(topStart, topEnd, bottomEnd, bottomStart)

        val outline1 = shape.createOutline(size, LayoutDirection.Ltr, density) as Outline.Rounded
        assertEquals(0f, outline1.roundRect.topLeftCornerRadius.x)

        // Mutate the state value
        topStart.value = 40f

        val outline2 = shape.createOutline(size, LayoutDirection.Ltr, density) as Outline.Rounded
        assertEquals(40f, outline2.roundRect.topLeftCornerRadius.x)
    }
}
