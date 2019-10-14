/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.layout.test

import androidx.ui.core.Dp
import androidx.ui.core.dp
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.enforce
import androidx.ui.layout.hasBoundedHeight
import androidx.ui.layout.hasBoundedWidth
import androidx.ui.layout.hasTightHeight
import androidx.ui.layout.hasTightWidth
import androidx.ui.layout.isTight
import androidx.ui.layout.isZero
import androidx.ui.layout.looseMax
import androidx.ui.layout.looseMin
import androidx.ui.layout.offset
import androidx.ui.layout.tightMax
import androidx.ui.layout.tightMin
import androidx.ui.layout.withTight
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DpConstraintsTest {

    @Test
    fun constructor() {
        val defaultDpConstraints = DpConstraints()
        defaultDpConstraints.assertEquals(0.dp, Dp.Infinity, 0.dp, Dp.Infinity)

        val constraints = DpConstraints(0.dp, 1.dp, 2.dp, 3.dp)
        constraints.assertEquals(0.dp, 1.dp, 2.dp, 3.dp)

        val tightDpConstraintsForWidth = DpConstraints.tightConstraintsForWidth(5.dp)
        tightDpConstraintsForWidth.assertEquals(5.dp, 5.dp, 0.dp, Dp.Infinity)

        val tightDpConstraintsForHeight = DpConstraints.tightConstraintsForHeight(5.dp)
        tightDpConstraintsForHeight.assertEquals(0.dp, Dp.Infinity, 5.dp, 5.dp)

        val tightDpConstraints = DpConstraints.tightConstraints(5.dp, 7.dp)
        tightDpConstraints.assertEquals(5.dp, 5.dp, 7.dp, 7.dp)
    }

    @Test
    fun hasBoundedDimensions() {
        val unbounded = DpConstraints(3.dp, Dp.Infinity, 3.dp, Dp.Infinity)
        assertFalse(unbounded.hasBoundedWidth)
        assertFalse(unbounded.hasBoundedHeight)

        val bounded = DpConstraints(3.dp, 5.dp, 3.dp, 5.dp)
        assertTrue(bounded.hasBoundedWidth)
        assertTrue(bounded.hasBoundedHeight)
    }

    @Test
    fun hasTightDimensions() {
        val untight = DpConstraints(3.dp, 4.dp, 8.dp, 9.dp)
        assertFalse(untight.hasTightWidth)
        assertFalse(untight.hasTightHeight)
        assertFalse(untight.isTight)

        val tight = DpConstraints(3.dp, 3.dp, 5.dp, 5.dp)
        assertTrue(tight.hasTightWidth)
        assertTrue(tight.hasTightHeight)
        assertTrue(tight.isTight)
    }

    @Test
    fun isZero() {
        val nonZero = DpConstraints(1.dp, 2.dp, 1.dp, 2.dp)
        assertFalse(nonZero.isZero)

        val zero = DpConstraints(0.dp, 0.dp, 0.dp, 0.dp)
        assertTrue(zero.isZero)
    }

    @Test
    fun enforce() {
        val constraints = DpConstraints(5.dp, 10.dp, 5.dp, 10.dp)
        constraints.enforce(DpConstraints(4.dp, 11.dp, 4.dp, 11.dp)).assertEquals(
            5.dp, 10.dp, 5.dp, 10.dp
        )
        constraints.enforce(DpConstraints(7.dp, 9.dp, 7.dp, 9.dp)).assertEquals(
            7.dp, 9.dp, 7.dp, 9.dp
        )
        constraints.enforce(DpConstraints(2.dp, 3.dp, 2.dp, 3.dp)).assertEquals(
            3.dp, 3.dp, 3.dp, 3.dp
        )
        constraints.enforce(DpConstraints(10.dp, 11.dp, 10.dp, 11.dp)).assertEquals(
            10.dp, 10.dp, 10.dp, 10.dp
        )
    }

    @Test
    fun withTight() {
        val constraints = DpConstraints(2.dp, 3.dp, 2.dp, 3.dp)
        constraints.withTight().assertEquals(2.dp, 3.dp, 2.dp, 3.dp)
        constraints.withTight(7.dp, 8.dp).assertEquals(7.dp, 7.dp, 8.dp, 8.dp)
    }

    @Test
    fun loose() {
        val bounded = DpConstraints(2.dp, 5.dp, 2.dp, 5.dp)
        bounded.looseMin().assertEquals(0.dp, 5.dp, 0.dp, 5.dp)
        bounded.looseMax().assertEquals(2.dp, Dp.Infinity, 2.dp, Dp.Infinity)

        val unbounded = DpConstraints(2.dp, Dp.Infinity, 2.dp, Dp.Infinity)
        unbounded.looseMin().assertEquals(0.dp, Dp.Infinity, 0.dp, Dp.Infinity)
        unbounded.looseMax().assertEquals(2.dp, Dp.Infinity, 2.dp, Dp.Infinity)
    }

    @Test
    fun tight() {
        val bounded = DpConstraints(2.dp, 5.dp, 2.dp, 5.dp)
        bounded.tightMin().assertEquals(2.dp, 2.dp, 2.dp, 2.dp)
        bounded.tightMax().assertEquals(5.dp, 5.dp, 5.dp, 5.dp)

        val unbounded = DpConstraints(2.dp, Dp.Infinity, 2.dp, Dp.Infinity)
        unbounded.tightMin().assertEquals(2.dp, 2.dp, 2.dp, 2.dp)
        unbounded.tightMax().assertEquals(2.dp, Dp.Infinity, 2.dp, Dp.Infinity)
    }

    @Test
    fun offset() {
        val constraints = DpConstraints(2.dp, 2.dp, 5.dp, 5.dp)
        constraints.offset(horizontal = 2.dp, vertical = 3.dp).assertEquals(
            4.dp, 4.dp, 8.dp, 8.dp
        )
        constraints.offset(horizontal = -7.dp, vertical = -7.dp).assertEquals(
            0.dp, 0.dp, 0.dp, 0.dp
        )
    }

    @Test
    fun validity() {
        assertInvalid(minWidth = Dp.Infinity)
        assertInvalid(minHeight = Dp.Infinity)
        assertInvalid(minWidth = Float.NaN.dp)
        assertInvalid(maxWidth = Float.NaN.dp)
        assertInvalid(minHeight = Float.NaN.dp)
        assertInvalid(maxHeight = Float.NaN.dp)
        assertInvalid(minWidth = 3.dp, maxWidth = 2.dp)
        assertInvalid(minHeight = 3.dp, maxHeight = 2.dp)
        assertInvalid(minWidth = -1.dp)
        assertInvalid(maxWidth = -1.dp)
        assertInvalid(minHeight = -1.dp)
        assertInvalid(maxHeight = -1.dp)
    }

    private fun DpConstraints.assertEquals(
        minWidth: Dp,
        maxWidth: Dp,
        minHeight: Dp,
        maxHeight: Dp
    ) {
        assertTrue(this.minWidth == minWidth && this.maxWidth == maxWidth &&
                this.minHeight == minHeight && this.maxHeight == maxHeight)
    }

    private fun assertInvalid(
        minWidth: Dp = 0.dp,
        maxWidth: Dp = 0.dp,
        minHeight: Dp = 0.dp,
        maxHeight: Dp = 0.dp
    ) {
        val constraints: DpConstraints
        try {
            constraints = DpConstraints(minWidth, maxWidth, minHeight, maxHeight)
        } catch (_: IllegalArgumentException) {
            return
        }
        fail("Invalid constraints $constraints are considered valid")
    }
}
