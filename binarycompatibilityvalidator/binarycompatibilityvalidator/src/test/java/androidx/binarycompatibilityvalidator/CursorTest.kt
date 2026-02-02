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
        assertThat(cursor.hasNextRow()).isFalse()
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
    fun parseValidIdentifierValid() {
        val input = "oneTwo3 four"
        val cursor = Cursor(input)
        val symbol = cursor.parseValidIdentifier()
        assertThat(symbol).isEqualTo("oneTwo3")
        assertThat("four").isEqualTo(cursor.currentLine)
    }

    @Test
    fun parseValidIdentifierValidStartsWithUnderscore() {
        val input = "_one_Two3 four"
        val cursor = Cursor(input)
        val symbol = cursor.parseValidIdentifier()
        assertThat(symbol).isEqualTo("_one_Two3")
        assertThat("four").isEqualTo(cursor.currentLine)
    }

    @Test
    fun parseValidIdentifierInvalid() {
        val input = "1twothree"
        val cursor = Cursor(input)
        val symbol = cursor.parseValidIdentifier()
        assertThat(symbol).isNull()
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
