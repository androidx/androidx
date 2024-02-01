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

package androidx.compose.ui.layout

import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.isSatisfiedBy
import androidx.compose.ui.unit.offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ConstraintsTest {

    @Test
    fun constructor() {
        val defaultConstraints2 = Constraints()
        defaultConstraints2.assertEquals(0, Constraints.Infinity, 0, Constraints.Infinity)

        val constraints = Constraints(0, 1, 2, 3)
        constraints.assertEquals(0, 1, 2, 3)

        val fixedWidth = Constraints.fixedWidth(5)
        fixedWidth.assertEquals(5, 5, 0, Constraints.Infinity)

        val fixedHeight = Constraints.fixedHeight(5)
        fixedHeight.assertEquals(0, Constraints.Infinity, 5, 5)

        val fixed = Constraints.fixed(5, 7)
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
        val unbounded = Constraints(3, Constraints.Infinity, 3, Constraints.Infinity)
        assertFalse(unbounded.hasBoundedWidth)
        assertFalse(unbounded.hasBoundedHeight)

        val bounded = Constraints(3, 5, 3, 5)
        assertTrue(bounded.hasBoundedWidth)
        assertTrue(bounded.hasBoundedHeight)
    }

    @Test
    fun hasFixedDimensions() {
        val untight = Constraints(3, 4, 8, 9)
        assertFalse(untight.hasFixedWidth)
        assertFalse(untight.hasFixedHeight)

        val tight = Constraints(3, 3, 5, 5)
        assertTrue(tight.hasFixedWidth)
        assertTrue(tight.hasFixedHeight)
    }

    @Test
    fun isZero() {
        val nonZero = Constraints(1, 2, 1, 2)
        assertFalse(nonZero.isZero)

        val zero = Constraints(0, 0, 0, 0)
        assertTrue(zero.isZero)

        val zero12 = Constraints(0, 0, 1, 2)
        assertTrue(zero12.isZero)
    }

    @Test
    fun constrain_constraints() {
        val constraints = Constraints(5, 10, 5, 10)
        Constraints(4, 11, 4, 11).constrain(constraints).assertEquals(
            5, 10, 5, 10
        )
        Constraints(7, 9, 7, 9).constrain(constraints).assertEquals(
            7, 9, 7, 9
        )
        Constraints(2, 3, 2, 3).constrain(constraints).assertEquals(
            3, 3, 3, 3
        )
        Constraints(10, 11, 10, 11).constrain(constraints).assertEquals(
            10, 10, 10, 10
        )
    }

    @Test
    fun constrain_size() {
        val constraints = Constraints(2, 5, 2, 5)
        assertEquals(IntSize(2, 2), constraints.constrain(IntSize(1, 1)))
        assertEquals(IntSize(3, 3), constraints.constrain(IntSize(3, 3)))
        assertEquals(IntSize(5, 5), constraints.constrain(IntSize(7, 7)))
    }

    @Test
    fun satisfiedBy() {
        val constraints = Constraints(2, 5, 7, 9)
        assertTrue(constraints.isSatisfiedBy(IntSize(4, 8)))
        assertTrue(constraints.isSatisfiedBy(IntSize(2, 7)))
        assertTrue(constraints.isSatisfiedBy(IntSize(5, 9)))
        assertFalse(constraints.isSatisfiedBy(IntSize(1, 8)))
        assertFalse(constraints.isSatisfiedBy(IntSize(7, 8)))
        assertFalse(constraints.isSatisfiedBy(IntSize(4, 5)))
        assertFalse(constraints.isSatisfiedBy(IntSize(4, 11)))
    }

    @Test
    fun offset() {
        val constraints = Constraints(2, 2, 5, 5)
        constraints.offset(horizontal = 2, vertical = 3).assertEquals(
            4, 4, 8, 8
        )
        constraints.offset(horizontal = -7, vertical = -7).assertEquals(
            0, 0, 0, 0
        )
    }

    @Test
    fun copy() {
        val constraints = Constraints()
        val sameCopy = constraints.copy()
        assertEquals(constraints, sameCopy)
        assertEquals(Constraints(0, 0, 0, 0), constraints.copy(maxWidth = 0, maxHeight = 0))
        assertEquals(Constraints(1, 2, 3, 4), constraints.copy(1, 2, 3, 4))
    }

    @Test
    fun validity() {
        assertInvalid(minWidth = Constraints.Infinity)
        assertInvalid(minHeight = Constraints.Infinity)
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

    @Test
    fun restrictedConstraints_noRestriction() {
        val constraints1 =
            Constraints.restrictedConstraints(0, Constraints.Infinity, 0, Constraints.Infinity)
        assertEquals(0, constraints1.minWidth)
        assertEquals(0, constraints1.minHeight)
        assertEquals(Constraints.Infinity, constraints1.maxWidth)
        assertEquals(Constraints.Infinity, constraints1.maxHeight)

        val constraints2 =
            Constraints.restrictedConstraints(0, Constraints.Infinity, 250_000, 250_001)
        assertEquals(0, constraints2.minWidth)
        assertEquals(250_000, constraints2.minHeight)
        assertEquals(Constraints.Infinity, constraints2.maxWidth)
        assertEquals(250_001, constraints2.maxHeight)

        val constraints3 =
            Constraints.restrictedConstraints(250_000, 250_001, 0, Constraints.Infinity)
        assertEquals(0, constraints3.minHeight)
        assertEquals(250_000, constraints3.minWidth)
        assertEquals(Constraints.Infinity, constraints3.maxHeight)
        assertEquals(250_001, constraints3.maxWidth)

        val constraints4 =
            Constraints.restrictedConstraints(30_000, 30_001, 60_000, 60_001)
        assertEquals(30_000, constraints4.minWidth)
        assertEquals(30_001, constraints4.maxWidth)
        assertEquals(60_000, constraints4.minHeight)
        assertEquals(60_001, constraints4.maxHeight)

        val constraints5 =
            Constraints.restrictedConstraints(60_000, 60_001, 30_000, 30_001)
        assertEquals(60_000, constraints5.minWidth)
        assertEquals(60_001, constraints5.maxWidth)
        assertEquals(30_000, constraints5.minHeight)
        assertEquals(30_001, constraints5.maxHeight)

        val constraints6 = Constraints.restrictedConstraints(
            30_000,
            Constraints.Infinity,
            60_000,
            Constraints.Infinity
        )
        assertEquals(30_000, constraints6.minWidth)
        assertEquals(Constraints.Infinity, constraints6.maxWidth)
        assertEquals(60_000, constraints6.minHeight)
        assertEquals(Constraints.Infinity, constraints6.maxHeight)

        val constraints7 = Constraints.restrictedConstraints(
            60_000,
            Constraints.Infinity,
            30_000,
            Constraints.Infinity
        )
        assertEquals(60_000, constraints7.minWidth)
        assertEquals(Constraints.Infinity, constraints7.maxWidth)
        assertEquals(30_000, constraints7.minHeight)
        assertEquals(Constraints.Infinity, constraints7.maxHeight)
    }

    @Test
    fun restrictedConstraints_bounds() {
        val maxFocus = (1 shl 18) - 2
        val maxNonFocus = (1 shl 13) - 2
        val minFocus = (1 shl 16) - 2
        val minNonFocus = (1 shl 15) - 2
        val constraints1 =
            Constraints.restrictedConstraints(maxFocus, maxFocus, maxNonFocus, maxNonFocus)
        assertEquals(maxFocus, constraints1.minWidth)
        assertEquals(maxNonFocus, constraints1.minHeight)
        assertEquals(maxFocus, constraints1.maxWidth)
        assertEquals(maxNonFocus, constraints1.maxHeight)

        val constraints2 = Constraints.restrictedConstraints(
            maxFocus,
            Constraints.Infinity,
            maxNonFocus,
            Constraints.Infinity
        )
        assertEquals(maxFocus, constraints2.minWidth)
        assertEquals(Constraints.Infinity, constraints2.maxWidth)
        assertEquals(maxNonFocus, constraints2.minHeight)
        assertEquals(Constraints.Infinity, constraints2.maxHeight)

        val constraints3 =
            Constraints.restrictedConstraints(maxNonFocus, maxNonFocus, maxFocus, maxFocus)
        assertEquals(maxNonFocus, constraints3.minWidth)
        assertEquals(maxNonFocus, constraints3.maxWidth)
        assertEquals(maxFocus, constraints3.minHeight)
        assertEquals(maxFocus, constraints3.maxHeight)

        val constraints4 = Constraints.restrictedConstraints(
            maxNonFocus,
            Constraints.Infinity,
            maxFocus,
            Constraints.Infinity
        )
        assertEquals(maxNonFocus, constraints4.minWidth)
        assertEquals(Constraints.Infinity, constraints4.maxWidth)
        assertEquals(maxFocus, constraints4.minHeight)
        assertEquals(Constraints.Infinity, constraints4.maxHeight)

        val constraints5 =
            Constraints.restrictedConstraints(minFocus, minFocus, minNonFocus, minNonFocus)
        assertEquals(minFocus, constraints5.minWidth)
        assertEquals(minNonFocus, constraints5.minHeight)
        assertEquals(minFocus, constraints5.maxWidth)
        assertEquals(minNonFocus, constraints5.maxHeight)

        val constraints6 = Constraints.restrictedConstraints(
            minFocus,
            Constraints.Infinity,
            minNonFocus,
            Constraints.Infinity
        )
        assertEquals(minFocus, constraints6.minWidth)
        assertEquals(Constraints.Infinity, constraints6.maxWidth)
        assertEquals(minNonFocus, constraints6.minHeight)
        assertEquals(Constraints.Infinity, constraints6.maxHeight)

        val constraints7 =
            Constraints.restrictedConstraints(minNonFocus, minNonFocus, minFocus, minFocus)
        assertEquals(minNonFocus, constraints7.minWidth)
        assertEquals(minNonFocus, constraints7.maxWidth)
        assertEquals(minFocus, constraints7.minHeight)
        assertEquals(minFocus, constraints7.maxHeight)

        val constraints8 = Constraints.restrictedConstraints(
            minNonFocus,
            Constraints.Infinity,
            minFocus,
            Constraints.Infinity
        )
        assertEquals(minNonFocus, constraints8.minWidth)
        assertEquals(Constraints.Infinity, constraints8.maxWidth)
        assertEquals(minFocus, constraints8.minHeight)
        assertEquals(Constraints.Infinity, constraints8.maxHeight)
    }

    @Test
    fun restrictedConstraints_widthPriority() {
        val maxFocus = (1 shl 18) - 2
        val maxNonFocus = (1 shl 13) - 2
        val minFocus = (1 shl 16) - 2
        val minNonFocus = (1 shl 15) - 2
        val constraints1 =
            Constraints.restrictedConstraints(1_000_000, 1_000_000, 1_000_000, 1_000_000)
        assertEquals(maxFocus, constraints1.minWidth)
        assertEquals(maxNonFocus, constraints1.minHeight)
        assertEquals(maxFocus, constraints1.maxWidth)
        assertEquals(maxNonFocus, constraints1.maxHeight)

        val constraints2 = Constraints.restrictedConstraints(
            1_000_000,
            Constraints.Infinity,
            1_000_000,
            Constraints.Infinity
        )
        assertEquals(maxFocus, constraints2.minWidth)
        assertEquals(Constraints.Infinity, constraints2.maxWidth)
        assertEquals(maxNonFocus, constraints2.minHeight)
        assertEquals(Constraints.Infinity, constraints2.maxHeight)

        val constraints3 =
            Constraints.restrictedConstraints(0, Constraints.Infinity, 1_000_000, 1_000_000)
        assertEquals(0, constraints3.minWidth)
        assertEquals(Constraints.Infinity, constraints3.maxWidth)
        assertEquals(maxFocus, constraints3.minHeight)
        assertEquals(maxFocus, constraints3.maxHeight)

        val constraints4 = Constraints.restrictedConstraints(
            minNonFocus,
            Constraints.Infinity,
            1_000_000,
            Constraints.Infinity
        )
        assertEquals(minNonFocus, constraints4.minWidth)
        assertEquals(Constraints.Infinity, constraints4.maxWidth)
        assertEquals(minFocus, constraints4.minHeight)
        assertEquals(Constraints.Infinity, constraints4.maxHeight)
    }

    @Test
    fun restrictedConstraints_heightPriority() {
        val maxFocus = (1 shl 18) - 2
        val maxNonFocus = (1 shl 13) - 2
        val minFocus = (1 shl 16) - 2
        val minNonFocus = (1 shl 15) - 2
        val constraints1 = Constraints.restrictedConstraints(
            1_000_000,
            1_000_000,
            1_000_000,
            1_000_000,
            prioritizeWidth = false
        )
        assertEquals(maxNonFocus, constraints1.minWidth)
        assertEquals(maxNonFocus, constraints1.maxWidth)
        assertEquals(maxFocus, constraints1.minHeight)
        assertEquals(maxFocus, constraints1.maxHeight)

        val constraints2 = Constraints.restrictedConstraints(
            1_000_000,
            Constraints.Infinity,
            1_000_000,
            Constraints.Infinity,
            prioritizeWidth = false
        )
        assertEquals(maxNonFocus, constraints2.minWidth)
        assertEquals(Constraints.Infinity, constraints2.maxWidth)
        assertEquals(maxFocus, constraints2.minHeight)
        assertEquals(Constraints.Infinity, constraints2.maxHeight)

        val constraints3 = Constraints.restrictedConstraints(
            1_000_000,
            1_000_000,
            0,
            Constraints.Infinity,
            prioritizeWidth = false
        )
        assertEquals(maxFocus, constraints3.minWidth)
        assertEquals(maxFocus, constraints3.maxWidth)
        assertEquals(0, constraints3.minHeight)
        assertEquals(Constraints.Infinity, constraints3.maxHeight)

        val constraints4 = Constraints.restrictedConstraints(
            1_000_000,
            Constraints.Infinity,
            minNonFocus,
            Constraints.Infinity,
            prioritizeWidth = false
        )
        assertEquals(minNonFocus, constraints4.minHeight)
        assertEquals(Constraints.Infinity, constraints4.maxHeight)
        assertEquals(minFocus, constraints4.minWidth)
        assertEquals(Constraints.Infinity, constraints4.maxWidth)
    }

    private fun testConstraints(
        minWidth: Int = 0,
        maxWidth: Int = Constraints.Infinity,
        minHeight: Int = 0,
        maxHeight: Int = Constraints.Infinity
    ) {
        val constraints = Constraints(
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

    private fun Constraints.assertEquals(
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
        maxWidth: Int = Constraints.Infinity,
        minHeight: Int = 0,
        maxHeight: Int = Constraints.Infinity
    ) {
        val constraints: Constraints
        try {
            constraints = Constraints(minWidth, maxWidth, minHeight, maxHeight)
        } catch (_: IllegalArgumentException) {
            return
        }
        fail("Invalid constraints $constraints are considered valid")
    }
}
