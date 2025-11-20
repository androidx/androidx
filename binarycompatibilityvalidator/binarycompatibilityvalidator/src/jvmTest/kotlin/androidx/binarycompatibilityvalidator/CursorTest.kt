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

package androidx.binarycompatibilityvalidator

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CursorTest {

    @Test
    fun cursorShowsCurrentLine() {
        val input = "one\ntwo\nthree"
        val cursor = Cursor(input)
        assertThat(cursor.currentLine).isEqualTo("one")
        cursor.nextLine()
        assertThat(cursor.currentLine).isEqualTo("two")
        cursor.nextLine()
        assertThat(cursor.currentLine).isEqualTo("three")
        cursor.nextLine()
        assertThat(cursor.isFinished()).isTrue()
    }

    @Test
    fun cursorGetsNextWord() {
        val input = "one two three"
        val cursor = Cursor(input)
        val word = cursor.parseWord()
        assertThat(word).isEqualTo("one")
        assertThat("two three").isEqualTo(cursor.currentLine)
    }

    @Test
    fun parseIdentifiersThatArePossibleByEscaping() {
        val validEscapedIdentifiers = listOf("123", "🙂", "identifiers can include spaces")
        for (id in validEscapedIdentifiers) {
            assertThat(Cursor(id).parseValidIdentifier()).isEqualTo(id)
        }
    }

    @Test
    fun parseValidIdentifierDoesNotIncludeIllegalCharacters() {
        val identifiersIncludingIllegalChar =
            listOf(
                "identifiers can't include :",
                "identifiers can't include \\",
                "identifiers can't include /",
                "identifiers can't include ;",
                "identifiers can't include (",
                "identifiers can't include )",
                "identifiers can't include <",
                "identifiers can't include >",
                "identifiers can't include [",
                "identifiers can't include ]",
                "identifiers can't include {",
                "identifiers can't include }",
                "identifiers can't include ?",
                "identifiers can't include ,",
            )
        for (id in identifiersIncludingIllegalChar) {
            val cursor = Cursor(id)
            assertThat(cursor.parseValidIdentifier()).isEqualTo("identifiers can't include ")
            assertThat(cursor.currentLine).isEqualTo(id.last().toString())
        }
    }

    @Test
    fun parseIdentifierThatEndsWithASpaceAtEndOfLine() {
        val cursor = Cursor("identifiers can include spaces at the end  //")
        val id = cursor.parseValidIdentifier()
        assertThat(id).isEqualTo("identifiers can include spaces at the end  ")
        assertThat(cursor.currentLine).isEqualTo("//")
    }

    @Test
    fun parseIdentifierThatEndsWithASpaceAtEndOfFunctionName() {
        val cursor = Cursor("identifiers can include spaces at the end ()")
        val id = cursor.parseValidIdentifier()
        assertThat(id).isEqualTo("identifiers can include spaces at the end ")
        assertThat(cursor.currentLine).isEqualTo("()")
    }

    @Test
    fun skipWhitespace() {
        val input = "    test"
        val cursor = Cursor(input)
        cursor.skipInlineWhitespace()
        assertThat(cursor.currentLine).isEqualTo("test")
    }

    @Test
    fun skipWhitespaceOnBlankLine() {
        val input = ""
        val cursor = Cursor(input)
        cursor.skipInlineWhitespace()
        assertThat(cursor.currentLine).isEqualTo("")
    }

    @Test
    fun skipWhitespaceSkipsEntireLine() {
        val input = "    "
        val cursor = Cursor(input)
        cursor.skipInlineWhitespace()
        assertThat(cursor.currentLine).isEqualTo("")
    }
}
