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
}