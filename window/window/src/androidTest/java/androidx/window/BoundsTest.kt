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

package androidx.window

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

public class BoundsTest {

    @Test
    public fun testConstructor_matchesRect() {
        val rect = Rect(1, 2, 3, 4)
        val actual = Bounds(rect)

        assertEquals(rect.left, actual.left)
        assertEquals(rect.top, actual.top)
        assertEquals(rect.right, actual.right)
        assertEquals(rect.bottom, actual.bottom)
    }

    // equals and hashCode do not seem to be implemented on API 15 so need to check properties
    @Test
    public fun testToRect_matchesBounds() {
        val rect = Rect(1, 2, 3, 4)
        val bounds = Bounds(rect)
        val actual = bounds.toRect()

        assertEquals(rect.left, actual.left)
        assertEquals(rect.top, actual.top)
        assertEquals(rect.right, actual.right)
        assertEquals(rect.bottom, actual.bottom)
    }

    @Test
    public fun testWidth_matchesRect() {
        val rect = Rect(1, 2, 3, 4)
        val actual = Bounds(rect).width

        assertEquals(rect.width(), actual)
    }

    @Test
    public fun testHeight_matchesRect() {
        val rect = Rect(1, 2, 3, 4)
        val actual = Bounds(rect).height

        assertEquals(rect.height(), actual)
    }

    @Test
    public fun testIsEmpty_matchesRect() {
        val emptyRect = Rect(0, 0, 111, 0)
        val isRectEmpty = Bounds(emptyRect).isEmpty
        val nonEmptyRect = Rect(0, 0, 100, 100)
        val isRectNotEmpty = Bounds(nonEmptyRect).isEmpty

        assertTrue(isRectEmpty)
        assertFalse(isRectNotEmpty)
    }
}