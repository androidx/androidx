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

class RectTest {

    companion object {
        private const val DELTA = 0.01f
    }

    @Test
    fun testRectAccessors() {
        val r = Rect(1.0f, 3.0f, 5.0f, 7.0f)
        assertEquals(1.0f, r.left, DELTA)
        assertEquals(3.0f, r.top, DELTA)
        assertEquals(5.0f, r.right, DELTA)
        assertEquals(7.0f, r.bottom, DELTA)
    }

    @Test
    fun testRectCreatedByWidthAndHeight() {
        val r = Rect(Offset(1.0f, 3.0f), Size(5.0f, 7.0f))
        assertEquals(1.0f, r.left, DELTA)
        assertEquals(3.0f, r.top, DELTA)
        assertEquals(6.0f, r.right, DELTA)
        assertEquals(10.0f, r.bottom, DELTA)
    }

    @Test
    fun testRectIntersection() {
        val r1 = Rect(0.0f, 0.0f, 100.0f, 100.0f)
        val r2 = Rect(50.0f, 50.0f, 200.0f, 200.0f)
        val r3 = r1.intersect(r2)
        assertEquals(50.0f, r3.left, DELTA)
        assertEquals(50.0f, r3.top, DELTA)
        assertEquals(100.0f, r3.right, DELTA)
        assertEquals(100.0f, r3.bottom, DELTA)
        val r4 = r2.intersect(r1)
        assertEquals(r3, r4)
    }

    @Test
    fun testRectWidth() {
        assertEquals(210f, Rect(70f, 10f, 280f, 300f).width)
    }

    @Test
    fun testRectHeight() {
        assertEquals(290f, Rect(70f, 10f, 280f, 300f).height)
    }

    @Test
    fun testRectSize() {
        assertEquals(Size(210f, 290f), Rect(70f, 10f, 280f, 300f).size)
    }

    @Test
    fun testRectInfinite() {
        assertTrue(Rect(Float.POSITIVE_INFINITY, 10f, 200f, 500f).isInfinite)
        assertTrue(Rect(10f, Float.POSITIVE_INFINITY, 200f, 500f).isInfinite)
        assertTrue(Rect(10f, 200f, Float.POSITIVE_INFINITY, 500f).isInfinite)
        assertTrue(Rect(10f, 200f, 500f, Float.POSITIVE_INFINITY).isInfinite)

        assertFalse(Rect(0f, 1f, 2f, 3f).isInfinite)
    }

    @Test
    fun testRectFinite() {
        assertTrue(Rect(0f, 1f, 2f, 3f).isFinite)
        assertFalse(Rect(0f, 1f, 2f, Float.POSITIVE_INFINITY).isFinite)
    }

    @Test
    fun testRectIsEmpty() {
        assertTrue(Rect(0f, 0f, 0f, 10f).isEmpty)
        assertTrue(Rect(1f, 0f, 0f, 10f).isEmpty)
        assertTrue(Rect(0f, 1f, 10f, 0f).isEmpty)
        assertTrue(Rect(0f, 1f, 10f, 1f).isEmpty)

        assertFalse(Rect(0f, 1f, 2f, 3f).isEmpty)
    }

    @Test
    fun testRectTranslateOffset() {
        val shifted = Rect(0f, 5f, 10f, 15f).translate(Offset(10f, 15f))
        assertEquals(Rect(10f, 20f, 20f, 30f), shifted)
    }

    @Test
    fun testRectTranslate() {
        val translated = Rect(0f, 5f, 10f, 15f).translate(10f, 15f)
        assertEquals(Rect(10f, 20f, 20f, 30f), translated)
    }

    @Test
    fun testRectInflate() {
        val inflated = Rect(5f, 10f, 10f, 20f).inflate(5f)
        assertEquals(Rect(0f, 5f, 15f, 25f), inflated)
    }

    @Test
    fun testRectDeflate() {
        val deflated = Rect(0f, 5f, 15f, 25f).deflate(5f)
        assertEquals(Rect(5f, 10f, 10f, 20f), deflated)
    }

    @Test
    fun testRectIntersect() {
        val intersected = Rect(0f, 0f, 20f, 20f).intersect(Rect(10f, 10f, 30f, 30f))
        assertEquals(Rect(10f, 10f, 20f, 20f), intersected)
    }

    @Test
    fun testRectOverlap() {
        val rect1 = Rect(0f, 5f, 10f, 15f)
        val rect2 = Rect(5f, 10f, 15f, 20f)
        assertTrue(rect1.overlaps(rect2))
        assertTrue(rect2.overlaps(rect1))
    }

    @Test
    fun testRectDoesNotOverlap() {
        val rect1 = Rect(0f, 5f, 10f, 15f)
        val rect2 = Rect(10f, 5f, 20f, 15f)
        assertFalse(rect1.overlaps(rect2))
        assertFalse(rect2.overlaps(rect1))
    }

    @Test
    fun testRectMinDimension() {
        val rect = Rect(0f, 5f, 100f, 25f)
        assertEquals(20f, rect.minDimension)
    }

    @Test
    fun testRectMaxDimension() {
        val rect = Rect(0f, 5f, 100f, 25f)
        assertEquals(100f, rect.maxDimension)
    }

    @Test
    fun testRectTopLeft() {
        val rect = Rect(27f, 38f, 100f, 200f)
        assertEquals(Offset(27f, 38f), rect.topLeft)
    }

    @Test
    fun testRectTopCenter() {
        val rect = Rect(100f, 15f, 200f, 300f)
        assertEquals(Offset(150f, 15f), rect.topCenter)
    }

    @Test
    fun testRectTopRight() {
        val rect = Rect(100f, 15f, 200f, 300f)
        assertEquals(Offset(200f, 15f), rect.topRight)
    }

    @Test
    fun testRectCenterLeft() {
        val rect = Rect(100f, 10f, 200f, 300f)
        assertEquals(Offset(100f, 155f), rect.centerLeft)
    }

    @Test
    fun testRectCenter() {
        val rect = Rect(100f, 10f, 200f, 300f)
        assertEquals(Offset(150f, 155f), rect.center)
    }

    @Test
    fun testRectCenterRight() {
        val rect = Rect(100f, 10f, 200f, 300f)
        assertEquals(Offset(200f, 155f), rect.centerRight)
    }

    @Test
    fun testRectBottomLeft() {
        val rect = Rect(100f, 10f, 200f, 300f)
        assertEquals(Offset(100f, 300f), rect.bottomLeft)
    }

    @Test
    fun testRectBottomCenter() {
        val rect = Rect(100f, 10f, 200f, 300f)
        assertEquals(Offset(150f, 300f), rect.bottomCenter)
    }

    @Test
    fun testRectBottomRight() {
        val rect = Rect(100f, 10f, 200f, 300f)
        assertEquals(Offset(200f, 300f), rect.bottomRight)
    }

    @Test
    fun testRectContains() {
        val rect = Rect(100f, 10f, 200f, 300f)
        val offset = Offset(177f, 288f)
        assertTrue(offset in rect)
    }

    @Test
    fun testRectDoesNotContain() {
        val rect = Rect(100f, 10f, 200f, 300f)
        val offset1 = Offset(201f, 150f)
        assertFalse(offset1 in rect)

        val offset2 = Offset(200f, 301f)
        assertFalse(offset2 in rect)
    }

    @Test
    fun testRectFromOffsetAndSize() {
        val offset = Offset(220f, 300f)
        val size = Size(80f, 200f)
        assertEquals(Rect(220f, 300f, 300f, 500f), Rect(offset, size))
    }

    @Test
    fun testRectFromTopleftAndBottomRight() {
        val offset1 = Offset(27f, 38f)
        val offset2 = Offset(130f, 280f)
        assertEquals(Rect(27f, 38f, 130f, 280f), Rect(offset1, offset2))
    }

    @Test
    fun testRectFromCenterAndRadius() {
        val offset = Offset(100f, 50f)
        val radius = 25f
        assertEquals(Rect(75f, 25f, 125f, 75f), Rect(offset, radius))
    }

    @Test
    fun testRectLerp() {
        val rect1 = Rect(0f, 0f, 100f, 100f)
        val rect2 = Rect(50f, 50f, 200f, 200f)

        assertEquals(Rect(25f, 25f, 150f, 150f), lerp(rect1, rect2, 0.5f))
    }
}
