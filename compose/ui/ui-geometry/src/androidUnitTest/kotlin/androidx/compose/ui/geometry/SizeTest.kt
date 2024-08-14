/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.geometry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SizeTest {

    @Test
    fun sizeTimesInt() {
        assertEquals(Size(10f, 10f), Size(2.5f, 2.5f) * 4f)
        assertEquals(Size(10f, 10f), 4f * Size(2.5f, 2.5f))
    }

    @Test
    fun sizeDivInt() {
        assertEquals(Size(10f, 10f), Size(40f, 40f) / 4f)
    }

    @Test
    fun sizeTimesFloat() {
        assertEquals(Size(10f, 10f), Size(4f, 4f) * 2.5f)
        assertEquals(Size(10f, 10f), 2.5f * Size(4f, 4f))
    }

    @Test
    fun sizeDivFloat() {
        assertEquals(Size(10f, 10f), Size(40f, 40f) / 4f)
    }

    @Test
    fun sizeTimesDouble() {
        assertEquals(Size(10f, 10f), Size(4f, 4f) * 2.5f)
        assertEquals(Size(10f, 10f), 2.5f * Size(4f, 4f))
    }

    @Test
    fun sizeDivDouble() {
        assertEquals(Size(10f, 10f), Size(40f, 40f) / 4.0f)
    }

    @Test
    fun testSizeCopy() {
        val size = Size(100f, 200f)
        assertEquals(size, size.copy())
    }

    @Test
    fun testSizeCopyOverwriteWidth() {
        val size = Size(100f, 200f)
        val copy = size.copy(width = 50f)
        assertEquals(50f, copy.width)
        assertEquals(200f, copy.height)
    }

    @Test
    fun testSizeCopyOverwriteHeight() {
        val size = Size(100f, 200f)
        val copy = size.copy(height = 300f)
        assertEquals(100f, copy.width)
        assertEquals(300f, copy.height)
    }

    @Test
    fun testSizeLerp() {
        val size1 = Size(100f, 200f)
        val size2 = Size(300f, 500f)
        assertEquals(Size(200f, 350f), lerp(size1, size2, 0.5f))
    }

    @Test
    fun testIsSpecified() {
        assertFalse(Size.Unspecified.isSpecified)
        assertTrue(Size(1f, 1f).isSpecified)
    }

    @Test
    fun testIsUnspecified() {
        assertTrue(Size.Unspecified.isUnspecified)
        assertFalse(Size(1f, 1f).isUnspecified)
    }

    @Test
    fun testTakeOrElseTrue() {
        assertTrue(Size(1f, 1f).takeOrElse { Size.Unspecified }.isSpecified)
    }

    @Test
    fun testTakeOrElseFalse() {
        assertTrue(Size.Unspecified.takeOrElse { Size(1f, 1f) }.isSpecified)
    }

    @Test
    fun testUnspecifiedSizeToString() {
        assertEquals("Size.Unspecified", Size.Unspecified.toString())
    }

    @Test
    fun testSpecifiedSizeToString() {
        assertEquals("Size(10.0, 20.0)", Size(10f, 20f).toString())
    }

    @Test
    fun testIsEmpty() {
        assertFalse(Size(10.0f, 20.0f).isEmpty())
        assertFalse(Size(10.0f, Float.POSITIVE_INFINITY).isEmpty())
        assertFalse(Size(Float.POSITIVE_INFINITY, 20.0f).isEmpty())

        assertTrue(Size(0.0f, 20.0f).isEmpty())
        assertTrue(Size(10.0f, 0.0f).isEmpty())
        assertTrue(Size(0.0f, 0.0f).isEmpty())
        assertTrue(Size(-10.0f, 20.0f).isEmpty())
        assertTrue(Size(10.0f, -20.0f).isEmpty())
        assertTrue(Size(0.0f, Float.POSITIVE_INFINITY).isEmpty())
        assertTrue(Size(Float.POSITIVE_INFINITY, 0.0f).isEmpty())
        assertTrue(Size(0.0f, Float.NEGATIVE_INFINITY).isEmpty())
        assertTrue(Size(Float.NEGATIVE_INFINITY, 0.0f).isEmpty())
        assertTrue(Size(Float.NEGATIVE_INFINITY, 20.0f).isEmpty())
        assertTrue(Size(10.0f, Float.NEGATIVE_INFINITY).isEmpty())

        assertTrue(Size.Unspecified.isEmpty())
    }
}
