/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.window.core.layout

import kotlin.test.Test
import kotlin.test.assertEquals

class WindowSizeClassGridCreatorTest {

    val widthDpBreakpoints1 = setOf(10)
    val heightDpBreakpoints1 = setOf(20)
    val widthDpBreakpoints2 = setOf(10, 100)
    val heightDpBreakpoints2 = setOf(20, 200)
    val expectedGrid =
        setOf(
            WindowSizeClass(10, 20),
            WindowSizeClass(100, 20),
            WindowSizeClass(10, 200),
            WindowSizeClass(100, 200),
        )

    @Test
    fun creating_a_singleton_set() {
        val actual = createGridWindowSizeClassSet(widthDpBreakpoints1, heightDpBreakpoints1)
        val expected =
            setOf(
                WindowSizeClass(
                    minWidthDp = widthDpBreakpoints1.first(),
                    minHeightDp = heightDpBreakpoints1.first(),
                )
            )
        assertEquals(expected, actual)
    }

    @Test
    fun creating_a_grid() {
        val actual = createGridWindowSizeClassSet(widthDpBreakpoints2, heightDpBreakpoints2)

        assertEquals(expectedGrid, actual)
    }

    @Test
    fun adding_a_height_breakpoint() {
        val actual =
            setOf(WindowSizeClass(10, 20), WindowSizeClass(100, 20))
                .addHeightDpBreakpoints(setOf(200))

        assertEquals(expectedGrid, actual)
    }
}
