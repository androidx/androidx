/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.text.findParagraphEnd
import androidx.compose.foundation.text.findParagraphStart
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StringHelpersTest {
    private val string = "ab\n\ncd"
    private val endOfFirstLinePos = string.indexOf("\n")
    private val emptyLinePos = endOfFirstLinePos + 1

    @Test
    fun findParagraphStart() {
        assertThat(string.findParagraphStart(string.indexOf("a"))).isEqualTo(string.indexOf("a"))
        assertThat(string.findParagraphStart(string.indexOf("b"))).isEqualTo(string.indexOf("a"))
        assertThat(string.findParagraphStart(endOfFirstLinePos)).isEqualTo(string.indexOf("a"))
        assertThat(string.findParagraphStart(emptyLinePos)).isEqualTo(emptyLinePos)
        assertThat(string.findParagraphStart(string.indexOf("c"))).isEqualTo(string.indexOf("c"))
        assertThat(string.findParagraphStart(string.indexOf("d"))).isEqualTo(string.indexOf("c"))
    }

    @Test
    fun findParagraphEnd() {
        assertThat(string.findParagraphEnd(string.indexOf("a"))).isEqualTo(endOfFirstLinePos)
        assertThat(string.findParagraphEnd(string.indexOf("b"))).isEqualTo(endOfFirstLinePos)
        assertThat(string.findParagraphEnd(endOfFirstLinePos)).isEqualTo(endOfFirstLinePos)
        assertThat(string.findParagraphEnd(emptyLinePos)).isEqualTo(emptyLinePos)
        assertThat(string.findParagraphEnd(string.indexOf("c"))).isEqualTo(string.length)
        assertThat(string.findParagraphEnd(string.indexOf("d"))).isEqualTo(string.length)
    }
}
