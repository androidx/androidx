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

package androidx.compose.runtime.tooling

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ComposeToolingApi::class)
class SourceInfoParseTests {

    @Test
    fun parseOffsetOnly() {
        val s = parseSourceInformationInternal("123@32L2")
        assertEquals(1, s.locations.size)
        assertEquals(124, s.locations[0].lineNumber)
        assertEquals(32, s.locations[0].offset)
        assertEquals(2, s.locations[0].length)
    }

    @Test
    fun parseRepeatableOffset() {
        val s = parseSourceInformationInternal("*123@32L2")
        assertEquals(1, s.locations.size)
        assertEquals(124, s.locations[0].lineNumber)
        assertEquals(true, s.locations[0].isRepeatable)
    }

    @Test
    fun parseParameterInlineClass() {
        val s =
            "C(BasicText)P(8,3,7,4,5:c#ui.text.style.TextOverflow,6,1,2)318@15279L93:BasicText.kt#423gt5"
        val info = parseSourceInformationInternal(s)
        assertEquals(8, info.parameters.size)
        assertEquals(8, info.parameters[0].sortedIndex)
        assertEquals(null, info.parameters[0].inlineClass)
        assertEquals(5, info.parameters[4].sortedIndex)
        assertEquals("androidx.compose.ui.text.style.TextOverflow", info.parameters[4].inlineClass)
    }

    @Test
    fun parseParameterRun() {
        val s = "C(Test)P(3!4)"
        val info = parseSourceInformationInternal(s)
        assertEquals(listOf(3, 0, 1, 2, 4), info.parameters.map { it.sortedIndex })
    }

    @Test
    fun parseParameterOnlyRun() {
        val s = "C(Test)P(!4)"
        val info = parseSourceInformationInternal(s)
        assertEquals(listOf(0, 1, 2, 3), info.parameters.map { it.sortedIndex })
    }

    @Test
    fun parseParameterRun2() {
        val s = "C(Test)P(3,4!2,6,8!4)"
        val info = parseSourceInformationInternal(s)
        assertEquals(listOf(3, 4, 0, 1, 6, 8, 2, 5, 7, 9), info.parameters.map { it.sortedIndex })
    }

    @Test // regression test for b/428978168
    fun parseLastParameterRun() {
        val s = "C(Test)P(2!,3)"
        val info = parseSourceInformationInternal(s)
        assertEquals(listOf(2, 0, 1, 3), info.parameters.map { it.sortedIndex })
    }

    @Test
    fun parseParameterNames() {
        val s = "C(Test)N(a,b:c#ui.text.style.TextOverflow,d:f)"
        val info = parseSourceInformationInternal(s)
        assertEquals(listOf("a", "b", "d"), info.parameters.map { it.name })
        assertEquals("androidx.compose.ui.text.style.TextOverflow", info.parameters[1].inlineClass)
        assertEquals("f", info.parameters[2].inlineClass)
        assertEquals(listOf(0, 1, 2), info.parameters.map { it.sortedIndex })
    }

    @Test
    fun unexpectedSection() {
        val s = "C(Test)A(Test(me))B(<important-data>)N(a,b,c)"
        val info = parseSourceInformationInternal(s)
        assertEquals(listOf("a", "b", "c"), info.parameters.map { it.name })
    }

    @Test
    fun parseErrorReturnsNull() {
        val s = "C(Test)N(a,b,c"
        val info = parseSourceInformation(s)
        assertNull(info)
    }

    @Test
    fun emptyStringReturnsNull() {
        val info = parseSourceInformation("")
        assertNull(info)
    }

    @Test
    fun parseIncomplete() {
        var ex: Exception? = null
        try {
            parseSourceInformationInternal("123@")
        } catch (e: Exception) {
            ex = e
        }

        assertEquals("Error while parsing source information: expected int at 123@|", ex?.message)
    }
}
