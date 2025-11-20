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

package androidx.compose.ui.unit

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntRectTest {
    @Test
    fun testRectCreatedByWidthAndHeight() {
        val r = IntRect(IntOffset(1, 3), IntSize(5, 7))
        assertEquals(1, r.left)
        assertEquals(3, r.top)
        assertEquals(6, r.right)
        assertEquals(10, r.bottom)
    }

    @Test
    fun testRectIntersection() {
        val r1 = IntRect(0, 0, 100, 100)
        val r2 = IntRect(50, 50, 200, 200)
        val r3 = r1.intersect(r2)
        assertEquals(50, r3.left)
        assertEquals(50, r3.top)
        assertEquals(100, r3.right)
        assertEquals(100, r3.bottom)
        val r4 = r2.intersect(r1)
        assertEquals(r3, r4)
    }

    @Test
    fun testRectWidth() {
        assertEquals(210, IntRect(70, 10, 280, 300).width)
    }

    @Test
    fun testRectHeight() {
        assertEquals(290, IntRect(70, 10, 280, 300).height)
    }

    @Test
    fun testRectSize() {
        assertEquals(IntSize(210, 290), IntRect(70, 10, 280, 300).size)
    }

    @Test
    fun testRectIsEmpty() {
        assertTrue(IntRect(0, 0, 0, 10).isEmpty)
        assertTrue(IntRect(1, 0, 0, 10).isEmpty)
        assertTrue(IntRect(0, 1, 10, 0).isEmpty)
        assertTrue(IntRect(0, 1, 10, 1).isEmpty)

        assertFalse(IntRect(0, 1, 2, 3).isEmpty)
    }

    @Test
    fun testRectTranslateIntOffset() {
        val shifted = IntRect(0, 5, 10, 15).translate(IntOffset(10, 15))
        assertEquals(IntRect(10, 20, 20, 30), shifted)
    }

    @Test
    fun testRectTranslate() {
        val translated = IntRect(0, 5, 10, 15).translate(10, 15)
        assertEquals(IntRect(10, 20, 20, 30), translated)
    }

    @Test
    fun testRectInflate() {
        val inflated = IntRect(5, 10, 10, 20).inflate(5)
        assertEquals(IntRect(0, 5, 15, 25), inflated)
    }

    @Test
    fun testRectDeflate() {
        val deflated = IntRect(0, 5, 15, 25).deflate(5)
        assertEquals(IntRect(5, 10, 10, 20), deflated)
    }

    @Test
    fun testRectIntersect() {
        val intersected = IntRect(0, 0, 20, 20).intersect(IntRect(10, 10, 30, 30))
        assertEquals(IntRect(10, 10, 20, 20), intersected)
    }

    @Test
    fun testRectOverlap() {
        val rect1 = IntRect(0, 5, 10, 15)
        val rect2 = IntRect(5, 10, 15, 20)
        assertTrue(rect1.overlaps(rect2))
        assertTrue(rect2.overlaps(rect1))
    }

    @Test
    fun testRectDoesNotOverlap() {
        val rect1 = IntRect(0, 5, 10, 15)
        val rect2 = IntRect(10, 5, 20, 15)
        assertFalse(rect1.overlaps(rect2))
        assertFalse(rect2.overlaps(rect1))
    }

    @Test
    fun testRectMinDimension() {
        val rect = IntRect(0, 5, 100, 25)
        assertEquals(20, rect.minDimension)
    }

    @Test
    fun testRectMaxDimension() {
        val rect = IntRect(0, 5, 100, 25)
        assertEquals(100, rect.maxDimension)
    }

    @Test
    fun testRectTopLeft() {
        val rect = IntRect(27, 38, 100, 200)
        assertEquals(IntOffset(27, 38), rect.topLeft)
    }

    @Test
    fun testRectTopCenter() {
        val rect = IntRect(100, 15, 200, 300)
        assertEquals(IntOffset(150, 15), rect.topCenter)
    }

    @Test
    fun testRectTopRight() {
        val rect = IntRect(100, 15, 200, 300)
        assertEquals(IntOffset(200, 15), rect.topRight)
    }

    @Test
    fun testRectCenterLeft() {
        val rect = IntRect(100, 10, 200, 300)
        assertEquals(IntOffset(100, 155), rect.centerLeft)
    }

    @Test
    fun testRectCenter() {
        val rect = IntRect(100, 10, 200, 300)
        assertEquals(IntOffset(150, 155), rect.center)
    }

    @Test
    fun testRectCenterRight() {
        val rect = IntRect(100, 10, 200, 300)
        assertEquals(IntOffset(200, 155), rect.centerRight)
    }

    @Test
    fun testRectBottomLeft() {
        val rect = IntRect(100, 10, 200, 300)
        assertEquals(IntOffset(100, 300), rect.bottomLeft)
    }

    @Test
    fun testRectBottomCenter() {
        val rect = IntRect(100, 10, 200, 300)
        assertEquals(IntOffset(150, 300), rect.bottomCenter)
    }

    @Test
    fun testRectBottomRight() {
        val rect = IntRect(100, 10, 200, 300)
        assertEquals(IntOffset(200, 300), rect.bottomRight)
    }

    @Test
    fun testRectContains() {
        val rect = IntRect(100, 10, 200, 300)
        val IntOffset = IntOffset(177, 288)
        assertTrue(rect.contains(IntOffset))
    }

    @Test
    fun testRectDoesNotContain() {
        val rect = IntRect(100, 10, 200, 300)
        val IntOffset1 = IntOffset(201, 150)
        assertFalse(rect.contains(IntOffset1))

        val IntOffset2 = IntOffset(200, 301)
        assertFalse(rect.contains(IntOffset2))
    }

    @Test
    fun testRectFromIntOffsetAndSize() {
        val offset = IntOffset(220, 300)
        val size = IntSize(80, 200)
        assertEquals(IntRect(220, 300, 300, 500), IntRect(offset, size))
    }

    @Test
    fun testRectFromTopleftAndBottomRight() {
        val offset1 = IntOffset(27, 38)
        val offset2 = IntOffset(130, 280)
        assertEquals(IntRect(27, 38, 130, 280), IntRect(offset1, offset2))
    }

    @Test
    fun testRectFromCenterAndRadius() {
        val offset = IntOffset(100, 50)
        val radius = 25
        assertEquals(IntRect(75, 25, 125, 75), IntRect(offset, radius))
    }

    @Test
    fun testRectLerp() {
        val rect1 = IntRect(0, 0, 100, 100)
        val rect2 = IntRect(50, 50, 200, 200)

        assertEquals(IntRect(25, 25, 150, 150), lerp(rect1, rect2, 0.5f))
    }

    @Test
    fun testToRect() {
        assertEquals(Rect(25.0f, 25.0f, 150.0f, 150.0f), IntRect(25, 25, 150, 150).toRect())
    }

    @Test
    fun testRoundRectToIntRect() {
        assertEquals(IntRect(2, 3, 4, 5), Rect(2.4f, 2.5f, 3.9f, 5.3f).roundToIntRect())
    }
}
