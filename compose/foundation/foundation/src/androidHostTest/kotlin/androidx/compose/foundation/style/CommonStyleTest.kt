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

@file:OptIn(ExperimentalFoundationStyleApi::class)

package androidx.compose.foundation.style

import junit.framework.TestCase.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class CommonStyleTest {
    @Test fun testOneStyle() = styleTest("A") { CommonStyle { add("A") } }

    @Test
    fun testTwoStyles() =
        styleTest("A", "B") {
            val styleA = CommonStyle { add("A") }
            val styleB = CommonStyle { add("B") }
            CommonStyle(styleA, styleB)
        }

    @Test
    fun testThreeStyles() =
        styleTest("A", "B", "C") {
            val styleA = CommonStyle { add("A") }
            val styleB = CommonStyle { add("B") }
            val styleC = CommonStyle { add("C") }
            CommonStyle(styleA, styleB, styleC)
        }

    @Test
    fun testFourStyles() =
        styleTest("A", "B", "C", "D") {
            val styleA = CommonStyle { add("A") }
            val styleB = CommonStyle { add("B") }
            val styleC = CommonStyle { add("C") }
            val styleD = CommonStyle { add("D") }
            CommonStyle(styleA, styleB, styleC, styleD)
        }

    @Test fun testEmpty() = styleTest { CommonStyle(CommonStyle, CommonStyle) }

    @Test fun testEmpty_First() = styleTest("A") { CommonStyle({ add("A") }, CommonStyle) }

    @Test fun testEmpty_Second() = styleTest("B") { CommonStyle(CommonStyle, { add("B") }) }
}

fun styleTest(vararg expected: String, block: MutableList<String>.() -> CommonStyle) {
    val result = mutableListOf<String>()
    val style = result.block()
    invoke(style)
    assertEquals(expected.toList(), result)
    assertCombinedStylesCount(style, expected.size)
}

internal fun invoke(style: CommonStyle) {
    scope { apply(style) }
}

internal fun assertCombinedStylesCount(style: CommonStyle, count: Int) {
    when (count) {
        0 -> assertEquals(CommonStyle, style)
        1 -> assertFalse(style is CombinedCommonStyle)
        2 -> {
            val dualStyle = style as? DualCommonStyle
            assertNotNull(dualStyle)
            assertTrue(dualStyle.style1 != CommonStyle && dualStyle.style2 != CommonStyle)
        }
        else -> {
            val combinedStyle = style as? CombinedCommonStyle
            assertNotNull(combinedStyle)
            assertEquals(count, combinedStyle.styles.size)
        }
    }
}
