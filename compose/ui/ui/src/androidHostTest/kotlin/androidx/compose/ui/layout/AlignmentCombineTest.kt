/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAbsoluteAlignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AlignmentCombineTest {
    private val space = IntSize(200, 200)

    /** A custom, non-[BiasAlignment] horizontal alignment implementation. */
    private fun inverseHorizontal(bias: Float) =
        Alignment.Horizontal { size, space, ld ->
            BiasAlignment.Horizontal(-bias).align(size, space, ld)
        }

    /** A custom, non-[BiasAlignment] vertical alignment implementation. */
    private fun inverseVertical(bias: Float) =
        Alignment.Vertical { size, space -> BiasAlignment.Vertical(-bias).align(size, space) }

    @Test
    fun testCombineHorizontalAndVertical() {
        val h = inverseHorizontal(.58f)
        val v = inverseVertical(.76f)
        val a = h + v
        assertEquals(IntOffset(42, 24), a.align(IntSize.Zero, space, LayoutDirection.Ltr))
        assertEquals(IntOffset(158, 24), a.align(IntSize.Zero, space, LayoutDirection.Rtl))
    }

    @Test
    fun testCombineHorizontalAndBiasVertical() {
        val h = inverseHorizontal(.58f)
        val v = BiasAlignment.Vertical(-.76f)
        val a = h + v
        assertEquals(IntOffset(42, 24), a.align(IntSize.Zero, space, LayoutDirection.Ltr))
        assertEquals(IntOffset(158, 24), a.align(IntSize.Zero, space, LayoutDirection.Rtl))
    }

    @Test
    fun testCombineBiasHorizontalAndVertical() {
        val h = BiasAlignment.Horizontal(-.58f)
        val v = inverseVertical(.76f)
        val a = h + v
        assertEquals(IntOffset(42, 24), a.align(IntSize.Zero, space, LayoutDirection.Ltr))
        assertEquals(IntOffset(158, 24), a.align(IntSize.Zero, space, LayoutDirection.Rtl))
    }

    @Test
    fun testCombineBiasHorizontalAndBiasVertical() {
        val h = BiasAlignment.Horizontal(-.58f)
        val v = BiasAlignment.Vertical(-.76f)
        val a = h + v
        assertEquals(IntOffset(42, 24), a.align(IntSize.Zero, space, LayoutDirection.Ltr))
        assertEquals(IntOffset(158, 24), a.align(IntSize.Zero, space, LayoutDirection.Rtl))
        // Ensure optimization applied.
        assertTrue(a is BiasAlignment)
    }

    @Test
    fun testCombineBiasAbsoluteHorizontalAndVertical() {
        val h = BiasAbsoluteAlignment.Horizontal(-.58f)
        val v = inverseVertical(.76f)
        val a = h + v
        assertEquals(IntOffset(42, 24), a.align(IntSize.Zero, space, LayoutDirection.Ltr))
        assertEquals(IntOffset(42, 24), a.align(IntSize.Zero, space, LayoutDirection.Rtl))
    }

    @Test
    fun testCombineBiasAbsoluteHorizontalAndBiasVertical() {
        val h = BiasAbsoluteAlignment.Horizontal(-.58f)
        val v = BiasAlignment.Vertical(-.76f)
        val a = h + v
        assertEquals(IntOffset(42, 24), a.align(IntSize.Zero, space, LayoutDirection.Ltr))
        assertEquals(IntOffset(42, 24), a.align(IntSize.Zero, space, LayoutDirection.Rtl))
        // Ensure optimization applied.
        assertTrue(a is BiasAbsoluteAlignment)
    }

    @Test
    fun testCombineVerticalAndHorizontal() {
        val v = inverseVertical(.76f)
        val h = inverseHorizontal(.58f)
        val a = v + h
        assertEquals(IntOffset(42, 24), a.align(IntSize.Zero, space, LayoutDirection.Ltr))
        assertEquals(IntOffset(158, 24), a.align(IntSize.Zero, space, LayoutDirection.Rtl))
    }

    @Test
    fun testCombineVerticalAndBiasHorizontal() {
        val v = inverseVertical(.76f)
        val h = BiasAlignment.Horizontal(-.58f)
        val a = v + h
        assertEquals(IntOffset(42, 24), a.align(IntSize.Zero, space, LayoutDirection.Ltr))
        assertEquals(IntOffset(158, 24), a.align(IntSize.Zero, space, LayoutDirection.Rtl))
    }

    @Test
    fun testCombineVerticalAndBiasAbsoluteHorizontal() {
        val v = inverseVertical(.76f)
        val h = BiasAbsoluteAlignment.Horizontal(-.58f)
        val a = v + h
        assertEquals(IntOffset(42, 24), a.align(IntSize.Zero, space, LayoutDirection.Ltr))
        assertEquals(IntOffset(42, 24), a.align(IntSize.Zero, space, LayoutDirection.Rtl))
    }

    @Test
    fun testCombineBiasVerticalAndHorizontal() {
        val v = BiasAlignment.Vertical(-.76f)
        val h = inverseHorizontal(.58f)
        val a = v + h
        assertEquals(IntOffset(42, 24), a.align(IntSize.Zero, space, LayoutDirection.Ltr))
        assertEquals(IntOffset(158, 24), a.align(IntSize.Zero, space, LayoutDirection.Rtl))
    }

    @Test
    fun testCombineBiasVerticalAndBiasHorizontal() {
        val v = BiasAlignment.Vertical(-.76f)
        val h = BiasAlignment.Horizontal(-.58f)
        val a = v + h
        assertEquals(IntOffset(42, 24), a.align(IntSize.Zero, space, LayoutDirection.Ltr))
        assertEquals(IntOffset(158, 24), a.align(IntSize.Zero, space, LayoutDirection.Rtl))
        // Ensure optimization applied.
        assertTrue(a is BiasAlignment)
    }

    @Test
    fun testCombineBiasVerticalAndBiasAbsoluteHorizontal() {
        val v = BiasAlignment.Vertical(-.76f)
        val h = BiasAbsoluteAlignment.Horizontal(-.58f)
        val a = v + h
        assertEquals(IntOffset(42, 24), a.align(IntSize.Zero, space, LayoutDirection.Ltr))
        assertEquals(IntOffset(42, 24), a.align(IntSize.Zero, space, LayoutDirection.Rtl))
        // Ensure optimization applied.
        assertTrue(a is BiasAbsoluteAlignment)
    }
}
