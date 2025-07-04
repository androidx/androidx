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

package androidx.compose.ui.graphics.shadow

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ShadowParamsTest {

    @Test
    fun testShadowParamsWithColor() {
        val shadow = Shadow(1.dp, Color.Cyan, 2.dp, DpOffset(3.dp, 4.dp), 0.5f, BlendMode.Dst)
        assertEquals(shadow.radius, 1.dp)
        assertEquals(shadow.spread, 2.dp)
        assertEquals(shadow.color, Color.Cyan)
        assertEquals(shadow.brush, null)
        assertEquals(shadow.offset, DpOffset(3.dp, 4.dp))
        assertEquals(shadow.alpha, 0.5f)
        assertEquals(shadow.blendMode, BlendMode.Dst)
    }

    @Test
    fun testShadowParamsWithGradient() {
        val gradient = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val shadow = Shadow(1.dp, gradient, 2.dp, DpOffset(3.dp, 4.dp), 0.5f, BlendMode.Dst)
        assertEquals(shadow.radius, 1.dp)
        assertEquals(shadow.spread, 2.dp)
        assertEquals(shadow.color, Color.Black)
        assertEquals(shadow.brush, gradient)
        assertEquals(shadow.offset, DpOffset(3.dp, 4.dp))
        assertEquals(shadow.alpha, 0.5f)
        assertEquals(shadow.blendMode, BlendMode.Dst)
    }

    @Test
    fun testShadowParamsWithSolidColorBrush() {
        val solidColor = SolidColor(Color.Magenta)
        val shadow = Shadow(1.dp, solidColor, 2.dp, DpOffset(3.dp, 4.dp), 0.5f, BlendMode.Dst)
        assertEquals(shadow.radius, 1.dp)
        assertEquals(shadow.spread, 2.dp)
        assertEquals(shadow.color, Color.Magenta)
        assertEquals(shadow.brush, null)
        assertEquals(shadow.offset, DpOffset(3.dp, 4.dp))
        assertEquals(shadow.alpha, 0.5f)
        assertEquals(shadow.blendMode, BlendMode.Dst)
    }

    @Test
    fun testShadowParamsEquals() {
        val shadow1 = Shadow(1.dp, Color.Cyan, 2.dp, DpOffset(5.dp, 6.dp), 0.5f, BlendMode.Dst)
        val shadow2 = Shadow(1.dp, Color.Cyan, 2.dp, DpOffset(5.dp, 6.dp), 0.5f, BlendMode.Dst)
        val shadow3 =
            Shadow(1.dp, SolidColor(Color.Cyan), 2.dp, DpOffset(5.dp, 6.dp), 0.5f, BlendMode.Dst)
        assertEquals(shadow1, shadow2)
        assertEquals(shadow2, shadow3)

        val shadow4 = Shadow(2.dp, Color.Cyan, 2.dp, DpOffset(5.dp, 6.dp), 0.5f, BlendMode.Dst)
        assertNotEquals(shadow1, shadow4)

        val shadow5 = Shadow(1.dp, Color.Cyan, 3.dp, DpOffset(5.dp, 6.dp), 0.5f, BlendMode.Dst)
        assertNotEquals(shadow2, shadow5)

        val shadow6 = Shadow(1.dp, Color.Yellow, 2.dp, DpOffset(5.dp, 6.dp), 0.5f, BlendMode.Dst)
        assertNotEquals(shadow3, shadow6)

        val shadow7 = Shadow(1.dp, Color.Cyan, 2.dp, DpOffset(5.dp, 6.dp), 0.6f, BlendMode.Dst)
        assertNotEquals(shadow1, shadow7)

        val shadow8 = Shadow(1.dp, Color.Cyan, 2.dp, DpOffset(5.dp, 6.dp), 0.5f, BlendMode.Src)
        assertNotEquals(shadow1, shadow8)

        val shadow9 = Shadow(1.dp, Color.Cyan, 2.dp, DpOffset(6.dp, 7.dp), 0.5f, BlendMode.Dst)
        assertNotEquals(shadow2, shadow9)
    }

    @Test
    fun testShadowParamsHashCode() {
        val shadow1 = Shadow(1.dp, Color.Cyan, 2.dp, DpOffset(3.dp, 4.dp), 0.5f, BlendMode.Dst)
        val shadow2 = Shadow(1.dp, Color.Cyan, 2.dp, DpOffset(3.dp, 4.dp), 0.5f, BlendMode.Dst)
        assertEquals(shadow1.hashCode(), shadow2.hashCode())
    }

    @Test
    fun testShadowParamsToString() {
        val shadow1 = Shadow(1.dp, Color.Cyan, 2.dp, DpOffset(3.dp, 4.dp), 0.5f, BlendMode.Dst)
        val shadow2 = Shadow(1.dp, Color.Cyan, 2.dp, DpOffset(3.dp, 4.dp), 0.5f, BlendMode.Dst)
        assertEquals(shadow1.toString(), shadow2.toString())
    }
}
