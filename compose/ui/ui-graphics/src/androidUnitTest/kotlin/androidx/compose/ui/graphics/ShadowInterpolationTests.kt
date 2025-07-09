/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.graphics

import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.shadow.lerp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShadowInterpolationTests {
    @Test
    fun testShadowToShadow() {
        val a =
            Shadow(
                radius = 1.dp,
                color = Color.Red,
                offset = DpOffset.Zero,
                spread = 1.dp,
                alpha = 0.5f,
                blendMode = BlendMode.SrcOver,
            )
        val b =
            Shadow(
                radius = 2.dp,
                color = Color.Blue,
                offset = DpOffset(1.dp, 1.dp),
                spread = 3.dp,
                alpha = 1.0f,
                blendMode = BlendMode.DstOver,
            )
        val result = lerp(a, b, 0.5f)!!
        val expected =
            Shadow(
                radius = lerp(a.radius, b.radius, 0.5f),
                color = lerp(a.color, b.color, 0.5f),
                offset = lerp(a.offset, b.offset, 0.5f),
                spread = lerp(a.spread, b.spread, 0.5f),
                alpha = lerp(a.alpha, b.alpha, 0.5f),
                blendMode = b.blendMode, // t >= 0.5f
            )
        assertEquals(expected, result)
    }

    @Test
    fun testShadowToNull() {
        val a =
            Shadow(
                radius = 1.dp,
                color = Color.Red,
                offset = DpOffset.Zero,
                spread = 1.dp,
                alpha = 0.5f,
                blendMode = BlendMode.SrcOver,
            )
        val result = lerp(a, null, 0.5f)!!
        val b = a.transparentCopy()
        val expected =
            Shadow(
                radius = lerp(a.radius, b.radius, 0.5f),
                color = lerp(a.color, b.color, 0.5f),
                offset = lerp(a.offset, b.offset, 0.5f),
                spread = lerp(a.spread, b.spread, 0.5f),
                alpha = lerp(a.alpha, b.alpha, 0.5f),
                blendMode = b.blendMode,
            )
        assertEquals(expected, result)
    }

    @Test
    fun testNullToShadow() {
        val b =
            Shadow(
                radius = 1.dp,
                color = Color.Red,
                offset = DpOffset.Zero,
                spread = 1.dp,
                alpha = 0.5f,
                blendMode = BlendMode.DstOver,
            )
        val result = lerp(null, b, 0.5f)!!
        val a = b.transparentCopy()
        val expected =
            Shadow(
                radius = lerp(a.radius, b.radius, 0.5f),
                color = lerp(a.color, b.color, 0.5f),
                offset = lerp(a.offset, b.offset, 0.5f),
                spread = lerp(a.spread, b.spread, 0.5f),
                alpha = lerp(a.alpha, b.alpha, 0.5f),
                blendMode = b.blendMode,
            )
        assertEquals(expected, result)
    }

    @Test
    fun testShadowToShadow_fraction() {
        val a =
            Shadow(
                radius = 1.dp,
                color = Color.Red,
                offset = DpOffset.Zero,
                spread = 1.dp,
                alpha = 0.0f,
                blendMode = BlendMode.SrcOver,
            )
        val b =
            Shadow(
                radius = 3.dp,
                color = Color.Blue,
                offset = DpOffset(2.dp, 2.dp),
                spread = 5.dp,
                alpha = 1.0f,
                blendMode = BlendMode.DstOver,
            )
        val result = lerp(a, b, 0.25f)!!
        val expected =
            Shadow(
                radius = lerp(a.radius, b.radius, 0.25f),
                color = lerp(a.color, b.color, 0.25f),
                offset = lerp(a.offset, b.offset, 0.25f),
                spread = lerp(a.spread, b.spread, 0.25f),
                alpha = lerp(a.alpha, b.alpha, 0.25f),
                blendMode = a.blendMode, // t < 0.5f
            )
        assertEquals(expected, result)
    }

    @Test
    fun testShadowToNull_fraction() {
        val a =
            Shadow(
                radius = 4.dp,
                color = Color.Red,
                offset = DpOffset(2.dp, 2.dp),
                spread = 2.dp,
                alpha = 1.0f,
                blendMode = BlendMode.SrcOver,
            )
        val result = lerp(a, null, 0.75f)!!
        val b = a.transparentCopy()
        val expected =
            Shadow(
                radius = lerp(a.radius, b.radius, 0.75f),
                color = lerp(a.color, b.color, 0.75f),
                offset = lerp(a.offset, b.offset, 0.75f),
                spread = lerp(a.spread, b.spread, 0.75f),
                alpha = lerp(a.alpha, b.alpha, 0.75f),
                blendMode = b.blendMode,
            )
        assertEquals(expected, result)
    }

    @Test
    fun testNullToShadow_fraction() {
        val b =
            Shadow(
                radius = 8.dp,
                color = Color.Red,
                offset = DpOffset(4.dp, 4.dp),
                spread = 4.dp,
                alpha = 1.0f,
                blendMode = BlendMode.DstOver,
            )
        val result = lerp(null, b, 0.25f)!!
        val a = b.transparentCopy()
        val expected =
            Shadow(
                radius = lerp(a.radius, b.radius, 0.25f),
                color = lerp(a.color, b.color, 0.25f),
                offset = lerp(a.offset, b.offset, 0.25f),
                spread = lerp(a.spread, b.spread, 0.25f),
                alpha = lerp(a.alpha, b.alpha, 0.25f),
                blendMode = a.blendMode,
            )
        assertEquals(expected, result)
    }
}
