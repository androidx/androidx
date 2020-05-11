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

import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.ipx
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AlignmentTest {
    private val space = IntPxSize(100.ipx, 100.ipx)
    private val space1D = 100.ipx

    @Test
    fun testAlign_topStart() {
        assertEquals(
            IntPxPosition(0.ipx, 0.ipx),
            Alignment.TopStart.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntPxPosition(100.ipx, 0.ipx),
            Alignment.TopStart.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_topCenter() {
        assertEquals(
            IntPxPosition(50.ipx, 0.ipx),
            Alignment.TopCenter.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntPxPosition(50.ipx, 0.ipx),
            Alignment.TopCenter.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_topEnd() {
        assertEquals(
            IntPxPosition(100.ipx, 0.ipx),
            Alignment.TopEnd.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntPxPosition(0.ipx, 0.ipx),
            Alignment.TopEnd.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_centerStart() {
        assertEquals(
            IntPxPosition(0.ipx, 50.ipx),
            Alignment.CenterStart.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntPxPosition(100.ipx, 50.ipx),
            Alignment.CenterStart.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_center() {
        assertEquals(
            IntPxPosition(50.ipx, 50.ipx),
            Alignment.Center.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntPxPosition(50.ipx, 50.ipx),
            Alignment.Center.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_centerEnd() {
        assertEquals(
            IntPxPosition(100.ipx, 50.ipx),
            Alignment.CenterEnd.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntPxPosition(0.ipx, 50.ipx),
            Alignment.CenterEnd.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_bottomStart() {
        assertEquals(
            IntPxPosition(0.ipx, 100.ipx),
            Alignment.BottomStart.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntPxPosition(100.ipx, 100.ipx),
            Alignment.BottomStart.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_bottomCenter() {
        assertEquals(
            IntPxPosition(50.ipx, 100.ipx),
            Alignment.BottomCenter.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntPxPosition(50.ipx, 100.ipx),
            Alignment.BottomCenter.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_bottomEnd() {
        assertEquals(
            IntPxPosition(100.ipx, 100.ipx),
            Alignment.BottomEnd.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntPxPosition(0.ipx, 100.ipx),
            Alignment.BottomEnd.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_top() {
        assertEquals(
            0.ipx,
            Alignment.Top.align(space1D)
        )
    }

    @Test
    fun testAlign_centerVertically() {
        assertEquals(
            50.ipx,
            Alignment.CenterVertically.align(space1D)
        )
    }

    @Test
    fun testAlign_bottom() {
        assertEquals(
            100.ipx,
            Alignment.Bottom.align(space1D)
        )
    }

    @Test
    fun testAlign_start() {
        assertEquals(
                0.ipx,
        Alignment.Start.align(space1D)
        )
        assertEquals(
            100.ipx,
            Alignment.Start.align(space1D, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_centerHorizontally() {
        assertEquals(
            50.ipx,
            Alignment.CenterHorizontally.align(space1D)
        )
        assertEquals(
            50.ipx,
            Alignment.CenterHorizontally.align(space1D, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_end() {
        assertEquals(
            100.ipx,
            Alignment.End.align(space1D)
        )
        assertEquals(
            0.ipx,
            Alignment.End.align(space1D, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAbsoluteAlign_left() {
        assertEquals(
            0.ipx,
            AbsoluteAlignment.Left.align(space1D)
        )
        assertEquals(
            0.ipx,
            AbsoluteAlignment.Left.align(space1D, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAbsoluteAlign_right() {
        assertEquals(
            100.ipx,
            AbsoluteAlignment.Right.align(space1D)
        )
        assertEquals(
            100.ipx,
            AbsoluteAlignment.Right.align(space1D, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAbsoluteAlign_topLeft() {
        assertEquals(
            IntPxPosition(0.ipx, 0.ipx),
            AbsoluteAlignment.TopLeft.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntPxPosition(0.ipx, 0.ipx),
            AbsoluteAlignment.TopLeft.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAbsoluteAlign_topRight() {
        assertEquals(
            IntPxPosition(100.ipx, 0.ipx),
            AbsoluteAlignment.TopRight.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntPxPosition(100.ipx, 0.ipx),
            AbsoluteAlignment.TopRight.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAbsoluteAlign_centerLeft() {
        assertEquals(
            IntPxPosition(0.ipx, 50.ipx),
            AbsoluteAlignment.CenterLeft.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntPxPosition(0.ipx, 50.ipx),
            AbsoluteAlignment.CenterLeft.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAbsoluteAlign_centerRight() {
        assertEquals(
            IntPxPosition(100.ipx, 50.ipx),
            AbsoluteAlignment.CenterRight.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntPxPosition(100.ipx, 50.ipx),
            AbsoluteAlignment.CenterRight.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAbsoluteAlign_bottomLeft() {
        assertEquals(
            IntPxPosition(0.ipx, 100.ipx),
            AbsoluteAlignment.BottomLeft.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntPxPosition(0.ipx, 100.ipx),
            AbsoluteAlignment.BottomLeft.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAbsoluteAlign_bottomRight() {
        assertEquals(
            IntPxPosition(100.ipx, 100.ipx),
            AbsoluteAlignment.BottomRight.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntPxPosition(100.ipx, 100.ipx),
            AbsoluteAlignment.BottomRight.align(space, LayoutDirection.Rtl)
        )
    }
}