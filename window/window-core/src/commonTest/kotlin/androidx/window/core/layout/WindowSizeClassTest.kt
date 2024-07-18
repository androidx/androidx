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

package androidx.window.core.layout

import kotlin.test.Test
import kotlin.test.assertEquals

public class WindowSizeClassTest {

    @Test
    public fun testWidthSizeClass_construction() {
        val expected = listOf(
            WindowWidthSizeClass.COMPACT,
            WindowWidthSizeClass.MEDIUM,
            WindowWidthSizeClass.EXPANDED
        )

        val actual = listOf(100f, 700f, 900f).map { width ->
            WindowSizeClass.compute(dpWidth = width, dpHeight = 100f)
        }.map { sizeClass ->
            sizeClass.windowWidthSizeClass
        }

        assertEquals(expected, actual)
    }

    @Test
    public fun testHeightSizeClass_construction() {
        val expected = listOf(
            WindowHeightSizeClass.COMPACT,
            WindowHeightSizeClass.MEDIUM,
            WindowHeightSizeClass.EXPANDED
        )

        val actual = listOf(100f, 500f, 900f).map { height ->
            WindowSizeClass.compute(dpHeight = height, dpWidth = 100f)
        }.map { sizeClass ->
            sizeClass.windowHeightSizeClass
        }

        assertEquals(expected, actual)
    }

    @Test
    public fun testEqualsImpliesHashCode() {
        val first = WindowSizeClass.compute(100f, 500f)
        val second = WindowSizeClass.compute(100f, 500f)

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }
}
