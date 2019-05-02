/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.ui.graphics
import androidx.test.filters.SmallTest
import androidx.ui.lerp
import androidx.ui.toHexString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class ColorTest {
    private val adobeColorSpace = ColorSpace.Named.AdobeRgb.colorSpace
    private val srgbColor = Color(0xFFFF8000.toInt())
    private val adobeColor = Color(red = 0.8916f, green = 0.4980f, blue = 0.1168f,
            colorSpace = ColorSpace.Named.AdobeRgb.colorSpace)
    private val epsilon = 0.0001f // Float16 squished into ColorLong isn't very accurate.

    @Test
    fun colorSpace() {
        assertEquals(ColorSpace.Named.Srgb.colorSpace, srgbColor.colorSpace)
        assertEquals(ColorSpace.Named.AdobeRgb.colorSpace, adobeColor.colorSpace)
    }

    @Test
    fun componentCount() {
        assertEquals(4, srgbColor.componentCount)
        assertEquals(4, adobeColor.componentCount)
    }

    @Test
    fun model() {
        assertEquals(ColorSpace.Named.Srgb.colorSpace.model, srgbColor.model)
        assertEquals(ColorSpace.Named.AdobeRgb.colorSpace.model, adobeColor.model)
    }

    @Test
    fun isWideGamut() {
        assertFalse(srgbColor.isWideGamut)
        assertTrue(adobeColor.isWideGamut)
    }

    @Test
    fun isSrgb() {
        assertTrue(srgbColor.isSrgb)
        assertFalse(adobeColor.isSrgb)
    }

    @Test
    fun getComponents() {
        val components = srgbColor.getComponents()
        assertEquals(4, components.size)
        assertEquals(1f, components[0], 0f)
        assertEquals(0.5019608f, components[1], epsilon)
        assertEquals(0f, components[2], 0f)
        assertEquals(1f, components[3], 0f)
    }

    @Test
    fun convert() {
        val targetColor = srgbColor.convert(adobeColorSpace)

        assertEquals(adobeColor.colorSpace, targetColor.colorSpace)
        assertEquals(adobeColor.red, targetColor.red, epsilon)
        assertEquals(adobeColor.green, targetColor.green, epsilon)
        assertEquals(adobeColor.blue, targetColor.blue, epsilon)
        assertEquals(adobeColor.alpha, targetColor.alpha, epsilon)
    }

    @Test
    fun toArgb_fromSrgb() {
        assertEquals(0xFFFF8000.toInt(), srgbColor.toArgb())
    }

    @Test
    fun toArgb_fromAdobeRgb() {
        assertEquals(0xFFFF8000.toInt(), adobeColor.toArgb())
    }

    @Test
    fun red() {
        assertEquals(1f, srgbColor.red, 0f)
        assertEquals(0.8916f, adobeColor.red, epsilon)
    }

    @Test
    fun green() {
        assertEquals(0.5019608f, srgbColor.green, epsilon)
        assertEquals(0.4980f, adobeColor.green, epsilon)
    }

    @Test
    fun blue() {
        assertEquals(0f, srgbColor.blue, 0f)
        assertEquals(0.1168f, adobeColor.blue, epsilon)
    }

    @Test
    fun alpha() {
        assertEquals(1f, srgbColor.alpha, 0f)
        assertEquals(1f, adobeColor.alpha, 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getComponents_fail() {
        srgbColor.getComponents(floatArrayOf(0f))
    }

    @Test
    fun getComponents_success() {
        val inputComponents = floatArrayOf(0f, 0f, 0f, 0f)
        val components = srgbColor.getComponents(inputComponents)
        assertSame(inputComponents, components)
        assertEquals(1f, components[0], 0f)
        assertEquals(0.5019608f, components[1], epsilon)
        assertEquals(0f, components[2], 0f)
        assertEquals(1f, components[3], 0f)
    }

    @Test
    fun getComponent_inRange() {
        assertEquals(1f, srgbColor.getComponent(0), epsilon)
        assertEquals(0.5019608f, srgbColor.getComponent(1), epsilon)
        assertEquals(0f, srgbColor.getComponent(2), epsilon)
        assertEquals(1f, srgbColor.getComponent(3), epsilon)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun getComponent_outOfRange() {
        srgbColor.getComponent(4)
    }

    @Test
    fun luminance() {
        assertEquals(0f, Color.Black.luminance(), 0f)
        assertEquals(0.0722f, Color.Blue.luminance(), epsilon)
        assertEquals(0.2126f, Color.Red.luminance(), epsilon)
        assertEquals(0.7152f, Color.Green.luminance(), epsilon)
        assertEquals(1f, Color.White.luminance(), 0f)
    }

    @Test
    fun testToString() {
        assertEquals("Color(1.0, 0.5019608, 0.0, 1.0, sRGB IEC61966-2.1)", srgbColor.toString())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testParseStringOfInvalidLength() {
        // abnormal case: colorString starts with '#' but length is neither 7 nor 9
        Color.parse("#ff00ff0")
    }

    @Test
    fun testParse() {
        assertEquals(Color.Red, Color.parse("#ff0000"))
        assertEquals(Color.Red, Color.parse("#ffff0000"))

        assertEquals(Color.Black, Color.parse("black"))
        assertEquals(Color.DarkGray, Color.parse("darkgray"))
        assertEquals(Color.Gray, Color.parse("gray"))
        assertEquals(Color.LightGray, Color.parse("lightgray"))
        assertEquals(Color.White, Color.parse("white"))
        assertEquals(Color.Red, Color.parse("red"))
        assertEquals(Color.Green, Color.parse("green"))
        assertEquals(Color.Blue, Color.parse("blue"))
        assertEquals(Color.Yellow, Color.parse("yellow"))
        assertEquals(Color.Cyan, Color.parse("cyan"))
        assertEquals(Color.Magenta, Color.parse("magenta"))
        assertEquals(Color.Transparent, Color.parse("Transparent"))
        assertEquals(Color.Aqua, Color.parse("Aqua"))
        assertEquals(Color.Fuchsia, Color.parse("Fuchsia"))
        assertEquals(Color.Lime, Color.parse("Lime"))
        assertEquals(Color.Maroon, Color.parse("Maroon"))
        assertEquals(Color.Navy, Color.parse("Navy"))
        assertEquals(Color.Olive, Color.parse("Olive"))
        assertEquals(Color.Purple, Color.parse("Purple"))
        assertEquals(Color.Silver, Color.parse("Silver"))
        assertEquals(Color.Teal, Color.parse("Teal"))
    }

    @Test
    fun lerp() {
        val red = Color.Red
        val green = Color.Green

        val redLinear = red.convert(ColorSpace.Named.LinearExtendedSrgb.colorSpace)
        val greenLinear = green.convert(ColorSpace.Named.LinearExtendedSrgb.colorSpace)

        for (i in 0..255) {
            val t = i / 255f
            val color = lerp(red, green, t)
            val expectedLinear = Color(
                red = lerp(redLinear.red, greenLinear.red, t),
                green = lerp(redLinear.green, greenLinear.green, t),
                blue = lerp(redLinear.blue, greenLinear.blue, t),
                colorSpace = ColorSpace.Named.LinearExtendedSrgb.colorSpace
            )
            val expected = expectedLinear.convert(ColorSpace.Named.Srgb.colorSpace)
            val colorARGB = Color(color.toArgb())
            val expectedARGB = Color(expected.toArgb())
            assertEquals("at t = $t[$i] was ${colorARGB.toArgb().toHexString()}, " +
                    "expecting ${expectedARGB.toArgb().toHexString()}", expectedARGB, colorARGB)
        }

        val transparentRed = Color.Red.copy(alpha = 0f)
        for (i in 0..255) {
            val t = i / 255f
            val color = lerp(red, transparentRed, t)
            val expected = Color.Red.copy(alpha = lerp(1f, 0f, t))
            val colorARGB = Color(color.toArgb())
            val expectedARGB = Color(expected.toArgb())
            assertEquals("at t = $t[$i] was ${colorARGB.toArgb().toHexString()}, " +
                    "expecting ${expectedARGB.toArgb().toHexString()}", expectedARGB, colorARGB)
        }
    }
}
