/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class TextLimitsTest {

    @Test
    fun surrogateSafeEndMovesACutInsideASurrogatePairBack() {
        val text = "ab\uD83D\uDE00c"

        assertEquals(2, surrogateSafeEnd(text, 3))
        assertEquals(2, surrogateSafeEnd(text, 2))
        assertEquals(4, surrogateSafeEnd(text, 4))
        assertEquals(0, surrogateSafeEnd(text, 0))
        assertEquals(text.length, surrogateSafeEnd(text, text.length))
    }

    @Test
    fun truncatedKeepsAShortText() {
        val text = "a".repeat(10)

        assertSame(text, truncated(text, 10))
    }

    @Test
    fun truncatedCutsALongTextAndMarksTheCut() {
        assertEquals("aaa$TRUNCATION_MARKER", truncated("aaaa", 3))
    }

    @Test
    fun truncatedNeverSplitsASurrogatePair() {
        assertEquals("aa$TRUNCATION_MARKER", truncated("aa\uD83D\uDE00b", 3))
    }
}
