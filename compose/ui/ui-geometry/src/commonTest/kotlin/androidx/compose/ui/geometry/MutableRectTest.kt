/*
 * Copyright 2018 The Android Open Source Project
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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MutableRectTest {

    companion object {
        private const val DELTA = 0.01f
    }

    @Test
    fun testAccessors() {
        val r = MutableRect(1f, 3f, 5f, 9f)
        assertEquals(1f, r.left, 0f)
        assertEquals(3f, r.top, 0f)
        assertEquals(5f, r.right, 0f)
        assertEquals(9f, r.bottom, 0f)
        assertEquals(4f, r.width, 0f)
        assertEquals(6f, r.height, 0f)
        assertEquals(Size(4f, 6f), r.size)
    }

    @Test
    fun testRectCreatedByWidthAndHeight() {
        val r = MutableRect(Offset(1.0f, 3.0f), Size(5.0f, 7.0f))
        assertEquals(1.0f, r.left, DELTA)
        assertEquals(3.0f, r.top, DELTA)
        assertEquals(6.0f, r.right, DELTA)
        assertEquals(10.0f, r.bottom, DELTA)
    }

    @Test
    fun testRectWidth() {
        assertEquals(210f, MutableRect(70f, 10f, 280f, 300f).width)
    }

    @Test
    fun testRectHeight() {
        assertEquals(290f, MutableRect(70f, 10f, 280f, 300f).height)
    }

    @Test
    fun testRectSize() {
        assertEquals(Size(210f, 290f), MutableRect(70f, 10f, 280f, 300f).size)
    }

    @Test
    fun testRectInfinite() {
        assertTrue(MutableRect(Float.POSITIVE_INFINITY, 10f, 200f, 500f).isInfinite)
        assertTrue(MutableRect(10f, Float.POSITIVE_INFINITY, 200f, 500f).isInfinite)
        assertTrue(MutableRect(10f, 200f, Float.POSITIVE_INFINITY, 500f).isInfinite)
        assertTrue(MutableRect(10f, 200f, 500f, Float.POSITIVE_INFINITY).isInfinite)

        assertFalse(MutableRect(0f, 1f, 2f, 3f).isInfinite)
    }

    @Test
    fun testRectFinite() {
        assertTrue(MutableRect(0f, 1f, 2f, 3f).isFinite)
        assertFalse(MutableRect(0f, 1f, 2f, Float.POSITIVE_INFINITY).isFinite)
    }

    @Test
    fun testEmpty() {
        val r = MutableRect(1f, 3f, 5f, 9f)
        assertFalse(r.isEmpty)
        r.left = 5f
        assertTrue(r.isEmpty)
        r.left = 1f
        r.bottom = 3f
        assertTrue(r.isEmpty)
    }

    @Test
    fun testRectTranslateOffset() {
        val shifted = MutableRect(0f, 5f, 10f, 15f)
        shifted.translate(Offset(10f, 15f))
        assertEquals(MutableRect(10f, 20f, 20f, 30f).toRect(), shifted.toRect())
    }

    @Test
    fun testRectTranslate() {
        val translated = MutableRect(0f, 5f, 10f, 15f)
        translated.translate(10f, 15f)
        assertEquals(MutableRect(10f, 20f, 20f, 30f).toRect(), translated.toRect())
    }

    @Test
    fun testRectInflate() {
        val inflated = MutableRect(5f, 10f, 10f, 20f)
        inflated.inflate(5f)
        assertEquals(MutableRect(0f, 5f, 15f, 25f).toRect(), inflated.toRect())
    }

    @Test
    fun testRectDeflate() {
        val deflated = MutableRect(0f, 5f, 15f, 25f)
        deflated.deflate(5f)
        assertEquals(MutableRect(5f, 10f, 10f, 20f).toRect(), deflated.toRect())
    }

    @Test
    fun testRectIntersect() {
        val intersected = MutableRect(0f, 0f, 20f, 20f)
        intersected.intersect(10f, 10f, 30f, 30f)
        assertEquals(MutableRect(10f, 10f, 20f, 20f).toRect(), intersected.toRect())
    }

    @Test
    fun testRectOverlap() {
        val rect1 = MutableRect(0f, 5f, 10f, 15f)
        val rect2 = MutableRect(5f, 10f, 15f, 20f)
        assertTrue(rect1.overlaps(rect2))
        assertTrue(rect2.overlaps(rect1))
    }

    @Test
    fun testRectDoesNotOverlap() {
        val rect1 = MutableRect(0f, 5f, 10f, 15f)
        val rect2 = MutableRect(10f, 5f, 20f, 15f)
        assertFalse(rect1.overlaps(rect2))
        assertFalse(rect2.overlaps(rect1))
    }

    @Test
    fun testRectMinDimension() {
        val rect = MutableRect(0f, 5f, 100f, 25f)
        assertEquals(20f, rect.minDimension)
    }

    @Test
    fun testRectMaxDimension() {
        val rect = MutableRect(0f, 5f, 100f, 25f)
        assertEquals(100f, rect.maxDimension)
    }

    @Test
    fun testRectTopLeft() {
        val rect = MutableRect(27f, 38f, 100f, 200f)
        assertEquals(Offset(27f, 38f), rect.topLeft)
    }

    @Test
    fun testRectTopCenter() {
        val rect = MutableRect(100f, 15f, 200f, 300f)
        assertEquals(Offset(150f, 15f), rect.topCenter)
    }

    @Test
    fun testRectTopRight() {
        val rect = MutableRect(100f, 15f, 200f, 300f)
        assertEquals(Offset(200f, 15f), rect.topRight)
    }

    @Test
    fun testRectCenterLeft() {
        val rect = MutableRect(100f, 10f, 200f, 300f)
        assertEquals(Offset(100f, 155f), rect.centerLeft)
    }

    @Test
    fun testRectCenter() {
        val rect = MutableRect(100f, 10f, 200f, 300f)
        assertEquals(Offset(150f, 155f), rect.center)
    }

    @Test
    fun testRectCenterRight() {
        val rect = MutableRect(100f, 10f, 200f, 300f)
        assertEquals(Offset(200f, 155f), rect.centerRight)
    }

    @Test
    fun restRectBottomLeft() {
        val rect = MutableRect(100f, 10f, 200f, 300f)
        assertEquals(Offset(100f, 300f), rect.bottomLeft)
    }

    @Test
    fun testRectBottomCenter() {
        val rect = MutableRect(100f, 10f, 200f, 300f)
        assertEquals(Offset(150f, 300f), rect.bottomCenter)
    }

    @Test
    fun testRectBottomRight() {
        val rect = MutableRect(100f, 10f, 200f, 300f)
        assertEquals(Offset(200f, 300f), rect.bottomRight)
    }

    @Test
    fun testContains() {
        val r = MutableRect(1f, 3f, 5f, 9f)
        assertTrue(Offset(1f, 3f) in r)
        assertTrue(Offset(3f, 3f) in r)
        assertFalse(Offset(5f, 3f) in r)
        assertTrue(Offset(1f, 6f) in r)
        assertTrue(Offset(3f, 6f) in r)
        assertFalse(Offset(5f, 6f) in r)
        assertFalse(Offset(1f, 9f) in r)
        assertFalse(Offset(3f, 9f) in r)
        assertFalse(Offset(5f, 9f) in r)
        assertFalse(Offset(0f, 0f) in r)
        assertFalse(Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY) in r)
        assertFalse(Offset(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY) in r)
    }

    @Test
    fun testIntersect() {
        val r = MutableRect(0f, 0f, 100f, 100f)
        r.intersect(50f, 50f, 200f, 200f)
        assertEquals(50f, r.left, 0f)
        assertEquals(50f, r.top, 0f)
        assertEquals(100f, r.right, 0f)
        assertEquals(100f, r.bottom, 0f)

        val r2 = MutableRect(50f, 50f, 200f, 200f)
        r2.intersect(0f, 0f, 100f, 100f)
        assertEquals(50f, r2.left, 0f)
        assertEquals(50f, r2.top, 0f)
        assertEquals(100f, r2.right, 0f)
        assertEquals(100f, r2.bottom, 0f)
    }

    @Test
    fun testSet() {
        val r = MutableRect(0f, 0f, 100f, 100f)
        r.set(10f, 3f, 20f, 6f)
        assertEquals(10f, r.left, 0f)
        assertEquals(3f, r.top, 0f)
        assertEquals(20f, r.right, 0f)
        assertEquals(6f, r.bottom, 0f)
    }
}
