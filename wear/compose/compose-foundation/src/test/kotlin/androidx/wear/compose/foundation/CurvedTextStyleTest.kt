/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.foundation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CurvedTextStyleTest {
    @Test
    fun `constructor with default values`() {
        val style = CurvedTextStyle()

        assertEquals(style.color, Color.Unspecified)
        assert(style.fontSize.isUnspecified)
        assertEquals(style.background, Color.Unspecified)
    }

    @Test
    fun `constructor with customized color`() {
        val color = Color.Red

        val style = CurvedTextStyle(color = color)

        assertEquals(style.color, color)
    }

    @Test
    fun `constructor with customized fontSize`() {
        val fontSize = 18.sp

        val style = CurvedTextStyle(fontSize = fontSize)

        assertEquals(style.fontSize, fontSize)
    }

    @Test
    fun `constructor with customized background`() {
        val color = Color.Red

        val style = CurvedTextStyle(background = color)

        assertEquals(style.background, color)
    }

    @Test
    fun `merge with empty other should return this`() {
        val style = CurvedTextStyle()

        val newStyle = style.merge()

        assertEquals(newStyle, style)
    }

    @Test
    fun `merge with other's color is unspecified should use this' color`() {
        val style = CurvedTextStyle(color = Color.Red)

        val newStyle = style.merge(CurvedTextStyle(color = Color.Unspecified))

        assertEquals(newStyle.color, style.color)
    }

    @Test
    fun `merge with other's color is set should use other's color`() {
        val style = CurvedTextStyle(color = Color.Red)
        val otherStyle = CurvedTextStyle(color = Color.Green)

        val newStyle = style.merge(otherStyle)

        assertEquals(newStyle.color, otherStyle.color)
    }

    @Test
    fun `merge with other's fontSize is unspecified should use this' fontSize`() {
        val style = CurvedTextStyle(fontSize = 3.5.sp)

        val newStyle = style.merge(CurvedTextStyle(fontSize = TextUnit.Unspecified))

        assertEquals(newStyle.fontSize, style.fontSize)
    }

    @Test
    fun `merge with other's fontSize is set should use other's fontSize`() {
        val style = CurvedTextStyle(fontSize = 3.5.sp)
        val otherStyle = CurvedTextStyle(fontSize = 8.7.sp)

        val newStyle = style.merge(otherStyle)

        assertEquals(newStyle.fontSize, otherStyle.fontSize)
    }

    @Test
    fun `merge with other's background is unspecified should use this' background`() {
        val style = CurvedTextStyle(background = Color.Red)

        val newStyle = style.merge(CurvedTextStyle(background = Color.Unspecified))

        assertEquals(newStyle.background, style.background)
    }

    @Test
    fun `merge with other's background is set should use other's background`() {
        val style = CurvedTextStyle(background = Color.Red)
        val otherStyle = CurvedTextStyle(background = Color.Green)

        val newStyle = style.merge(otherStyle)

        assertEquals(newStyle.background, otherStyle.background)
    }
}