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

package androidx.ui.core

import androidx.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.IllegalArgumentException

@RunWith(JUnit4::class)
class Constraints2Test {

    @Test
    fun constructor() {
        val defaultConstraints2 = Constraints2()
        defaultConstraints2.assertEquals(0, Constraints2.Infinity, 0, Constraints2.Infinity)

        val constraints = Constraints2(0, 1, 2, 3)
        constraints.assertEquals(0, 1, 2, 3)

        val fixedWidth = Constraints2.fixedWidth(5)
        fixedWidth.assertEquals(5, 5, 0, Constraints2.Infinity)

        val fixedHeight = Constraints2.fixedHeight(5)
        fixedHeight.assertEquals(0, Constraints2.Infinity, 5, 5)

        val fixed = Constraints2.fixed(5, 7)
        fixed.assertEquals(5, 5, 7, 7)
    }

    @Test
    fun retrieveSimpleValues() {
        testConstraints() // Infinity max
        testConstraints(0, 0, 0, 0)
    }

    @Test
    fun retrieveValueMinFocusWidth() {
        testConstraints(minWidth = 1, maxWidth = 64000, minHeight = 2, maxHeight = 32000)
        testConstraints(minWidth = 64000, minHeight = 32000)
        testConstraints(minWidth = 0xFFFE, minHeight = 0x7FFE)
        testConstraints(maxWidth = 0xFFFE, maxHeight = 0x7FFE)
    }

    @Test
    fun retrieveValueMinFocusHeight() {
        testConstraints(minWidth = 1, maxWidth = 32000, minHeight = 2, maxHeight = 64000)
        testConstraints(minWidth = 32000, maxWidth = 32001, minHeight = 64000, maxHeight = 64001)
        testConstraints(minWidth = 32000, minHeight = 64000)
        testConstraints(minWidth = 0x7FFE, minHeight = 0xFFFE)
        testConstraints(maxWidth = 0x7FFE, maxHeight = 0xFFFE)
    }

    @Test
    fun retrieveValueMaxFocusWidth() {
        testConstraints(minWidth = 1, maxWidth = 250000, minHeight = 2, maxHeight = 8000)
        testConstraints(minWidth = 250000, maxWidth = 250001, minHeight = 8000, maxHeight = 8001)
        testConstraints(minWidth = 250000, minHeight = 8000)
        testConstraints(minWidth = 0x3FFFE, minHeight = 0x1FFE)
        testConstraints(maxWidth = 0x3FFFE, maxHeight = 0x1FFE)
    }

    @Test
    fun retrieveValueMaxFocusHeight() {
        testConstraints(minWidth = 1, maxWidth = 8000, minHeight = 2, maxHeight = 250000)
        testConstraints(minWidth = 8000, maxWidth = 8001, minHeight = 250000, maxHeight = 250001)
        testConstraints(minWidth = 8000, minHeight = 250000)
        testConstraints(minWidth = 0x1FFE, minHeight = 0x3FFFE)
        testConstraints(maxWidth = 0x1FFE, maxHeight = 0x3FFFE)
    }

    @Test
    fun hasBoundedDimensions() {
        val unbounded = Constraints2(3, Constraints2.Infinity, 3, Constraints2.Infinity)
        assertFalse(unbounded.hasBoundedWidth)
        assertFalse(unbounded.hasBoundedHeight)

        val bounded = Constraints2(3, 5, 3, 5)
        assertTrue(bounded.hasBoundedWidth)
        assertTrue(bounded.hasBoundedHeight)
    }

    @Test
    fun hasFixedDimensions() {
        val untight = Constraints2(3, 4, 8, 9)
        assertFalse(untight.hasFixedWidth)
        assertFalse(untight.hasFixedHeight)

        val tight = Constraints2(3, 3, 5, 5)
        assertTrue(tight.hasFixedWidth)
        assertTrue(tight.hasFixedHeight)
    }

    @Test
    fun isZero() {
        val nonZero = Constraints2(1, 2, 1, 2)
        assertFalse(nonZero.isZero)

        val zero = Constraints2(0, 0, 0, 0)
        assertTrue(zero.isZero)

        val zero12 = Constraints2(0, 0, 1, 2)
        assertTrue(zero12.isZero)
    }

    @Test
    fun enforce() {
        val constraints = Constraints2(5, 10, 5, 10)
        constraints.enforce(Constraints2(4, 11, 4, 11)).assertEquals(
            5, 10, 5, 10
        )
        constraints.enforce(Constraints2(7, 9, 7, 9)).assertEquals(
            7, 9, 7, 9
        )
        constraints.enforce(Constraints2(2, 3, 2, 3)).assertEquals(
            3, 3, 3, 3
        )
        constraints.enforce(Constraints2(10, 11, 10, 11)).assertEquals(
            10, 10, 10, 10
        )
    }

    @Test
    fun constrain() {
        val constraints = Constraints2(2, 5, 2, 5)
        assertEquals(IntSize(2, 2), constraints.constrain(IntSize(1, 1)))
        assertEquals(IntSize(3, 3), constraints.constrain(IntSize(3, 3)))
        assertEquals(IntSize(5, 5), constraints.constrain(IntSize(7, 7)))
    }

    @Test
    fun satisfiedBy() {
        val constraints = Constraints2(2, 5, 7, 9)
        assertTrue(constraints.satisfiedBy(IntSize(4, 8)))
        assertTrue(constraints.satisfiedBy(IntSize(2, 7)))
        assertTrue(constraints.satisfiedBy(IntSize(5, 9)))
        assertFalse(constraints.satisfiedBy(IntSize(1, 8)))
        assertFalse(constraints.satisfiedBy(IntSize(7, 8)))
        assertFalse(constraints.satisfiedBy(IntSize(4, 5)))
        assertFalse(constraints.satisfiedBy(IntSize(4, 11)))
    }

    @Test
    fun offset() {
        val constraints = Constraints2(2, 2, 5, 5)
        constraints.offset(horizontal = 2, vertical = 3).assertEquals(
            4, 4, 8, 8
        )
        constraints.offset(horizontal = -7, vertical = -7).assertEquals(
            0, 0, 0, 0
        )
    }

    @Test
    fun validity() {
        assertInvalid(minWidth = Constraints2.Infinity)
        assertInvalid(minHeight = Constraints2.Infinity)
        assertInvalid(minWidth = 3, maxWidth = 2)
        assertInvalid(minHeight = 3, maxHeight = 2)
        assertInvalid(minWidth = -1)
        assertInvalid(maxWidth = -1)
        assertInvalid(minHeight = -1)
        assertInvalid(maxHeight = -1)
        assertInvalid(minWidth = 1000000)
        assertInvalid(minHeight = 1000000)
        assertInvalid(minWidth = 0x3FFFF)
        assertInvalid(maxWidth = 0x3FFFF)
        assertInvalid(minHeight = 0x3FFFF)
        assertInvalid(maxHeight = 0x3FFFF)
        assertInvalid(maxWidth = 0x1FFF, maxHeight = 0x3FFFE)
        assertInvalid(maxWidth = 0x3FFFF, maxHeight = 0x1FFF)
        assertInvalid(minWidth = 0x7FFE, minHeight = 0xFFFF)
        assertInvalid(minWidth = 0x7FFF, minHeight = 0xFFFE)
        assertInvalid(minWidth = 0xFFFE, minHeight = 0x7FFF)
        assertInvalid(minWidth = 0xFFFF, minHeight = 0x7FFE)
    }

    private fun testConstraints(
        minWidth: Int = 0,
        maxWidth: Int = Constraints2.Infinity,
        minHeight: Int = 0,
        maxHeight: Int = Constraints2.Infinity
    ) {
        val constraints = Constraints2(
            minWidth = minWidth,
            minHeight = minHeight,
            maxWidth = maxWidth,
            maxHeight = maxHeight
        )
        assertEquals(minWidth, constraints.minWidth)
        assertEquals(minHeight, constraints.minHeight)
        assertEquals(maxWidth, constraints.maxWidth)
        assertEquals(maxHeight, constraints.maxHeight)
    }

    private fun Constraints2.assertEquals(
        minWidth: Int,
        maxWidth: Int,
        minHeight: Int,
        maxHeight: Int
    ) {
        assertTrue(
            this.minWidth == minWidth && this.maxWidth == maxWidth &&
                    this.minHeight == minHeight && this.maxHeight == maxHeight
        )
    }

    private fun assertInvalid(
        minWidth: Int = 0,
        maxWidth: Int = Constraints2.Infinity,
        minHeight: Int = 0,
        maxHeight: Int = Constraints2.Infinity
    ) {
        val constraints: Constraints2
        try {
            constraints = Constraints2(minWidth, maxWidth, minHeight, maxHeight)
        } catch (_: IllegalArgumentException) {
            return
        }
        fail("Invalid constraints $constraints are considered valid")
    }
}
