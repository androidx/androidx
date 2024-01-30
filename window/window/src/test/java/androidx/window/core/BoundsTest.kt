/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.window.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [Bounds] to check the public API and that some methods
 * match [android.graphics.Rect].
 */
class BoundsTest {

    @Test
    fun sameBounds_equals() {
        val first = Bounds(0, 0, dimension, doubleDimension)
        val second = Bounds(0, 0, dimension, doubleDimension)
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun differentBounds_equals() {
        val base = Bounds(0, 0, dimension, doubleDimension)
        val diffLeft = Bounds(dimension, 0, dimension, doubleDimension)
        val diffTop = Bounds(0, dimension, dimension, doubleDimension)
        val diffRight = Bounds(0, 0, doubleDimension, doubleDimension)
        val diffBottom = Bounds(0, 0, dimension, dimension)

        assertNotEquals(base, diffLeft)
        assertNotEquals(base, diffTop)
        assertNotEquals(base, diffRight)
        assertNotEquals(base, diffBottom)

        assertNotEquals(base.hashCode(), diffLeft.hashCode())
        assertNotEquals(base.hashCode(), diffTop.hashCode())
        assertNotEquals(base.hashCode(), diffRight.hashCode())
        assertNotEquals(base.hashCode(), diffBottom.hashCode())
    }

    @Test
    fun testWidthCalculation() {
        val bounds = Bounds(0, 0, dimension, doubleDimension)

        assertEquals(dimension, bounds.width)
    }

    @Test
    fun testHeightCalculation() {
        val bounds = Bounds(0, 0, doubleDimension, dimension)

        assertEquals(dimension, bounds.height)
    }

    @Test
    fun zeroBounds_isZero() {
        val zero = Bounds(dimension, dimension, dimension, dimension)
        assertTrue(zero.isZero)
    }

    @Test
    fun zeroWidth_isEmpty() {
        val zeroWidth = Bounds(dimension, dimension, dimension, doubleDimension)
        assertTrue(zeroWidth.isEmpty)
    }

    @Test
    fun zeroHeight_isEmpty() {
        val zeroHeight = Bounds(dimension, dimension, doubleDimension, dimension)
        assertTrue(zeroHeight.isEmpty)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeWidth_throwsException() {
        Bounds(0, 0, -dimension, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeHeight_throwsException() {
        Bounds(0, 0, 0, -dimension)
    }

    companion object {
        const val dimension = 100
        const val doubleDimension = 200
    }
}
