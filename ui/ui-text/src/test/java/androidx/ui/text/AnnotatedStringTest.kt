/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.text

import androidx.ui.core.sp
import androidx.ui.graphics.Color
import androidx.ui.text.style.TextAlign
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AnnotatedStringTest {
    @Test
    fun normalizedParagraphStyles() {
        val text = "Hello World"
        val paragraphStyle = ParagraphStyle(textAlign = TextAlign.Center)
        val paragraphStyles = listOf(AnnotatedString.Item(paragraphStyle, 0, 5))
        val annotatedString = AnnotatedString(text = text, paragraphStyles = paragraphStyles)
        val defaultParagraphStyle = ParagraphStyle(lineHeight = 20.sp)

        val paragraphs = annotatedString.normalizedParagraphStyles(defaultParagraphStyle)

        assertThat(paragraphs.size).isEqualTo(2)

        assertThat(paragraphs[0].style).isEqualTo(defaultParagraphStyle.merge(paragraphStyle))
        assertThat(paragraphs[0].start).isEqualTo(0)
        assertThat(paragraphs[0].end).isEqualTo(5)

        assertThat(paragraphs[1].style).isEqualTo(defaultParagraphStyle)
        assertThat(paragraphs[1].start).isEqualTo(5)
        assertThat(paragraphs[1].end).isEqualTo(text.length)
    }

    @Test
    fun normalizedParagraphStyles_only_string() {
        val text = "Hello World"
        val annotatedString = AnnotatedString(text = text)
        val defaultParagraphStyle = ParagraphStyle(lineHeight = 20.sp)

        val paragraphs = annotatedString.normalizedParagraphStyles(defaultParagraphStyle)

        assertThat(paragraphs.size).isEqualTo(1)

        assertThat(paragraphs[0].style).isEqualTo(defaultParagraphStyle)
        assertThat(paragraphs[0].start).isEqualTo(0)
        assertThat(paragraphs[0].end).isEqualTo(text.length)
    }

    @Test
    fun normalizedParagraphStyles_empty_string() {
        val text = ""
        val annotatedString = AnnotatedString(text = text)
        val defaultParagraphStyle = ParagraphStyle(lineHeight = 20.sp)

        val paragraphs = annotatedString.normalizedParagraphStyles(defaultParagraphStyle)

        assertThat(paragraphs.size).isEqualTo(1)

        assertThat(paragraphs[0].style).isEqualTo(defaultParagraphStyle)
        assertThat(paragraphs[0].start).isEqualTo(0)
        assertThat(paragraphs[0].end).isEqualTo(text.length)
    }

    @Test
    fun normalizedParagraphStyles_with_newLine() {
        val text = "Hello\nWorld"
        val annotatedString = AnnotatedString(text = text)
        val defaultParagraphStyle = ParagraphStyle(lineHeight = 20.sp)

        val paragraphs = annotatedString.normalizedParagraphStyles(defaultParagraphStyle)

        assertThat(paragraphs.size).isEqualTo(1)

        assertThat(paragraphs[0].style).isEqualTo(defaultParagraphStyle)
        assertThat(paragraphs[0].start).isEqualTo(0)
        assertThat(paragraphs[0].end).isEqualTo(text.length)
    }

    @Test
    fun normalizedParagraphStyles_with_only_lineFeed() {
        val text = "\n"
        val annotatedString = AnnotatedString(text = text)
        val defaultParagraphStyle = ParagraphStyle(lineHeight = 20.sp)

        val paragraphs = annotatedString.normalizedParagraphStyles(defaultParagraphStyle)

        assertThat(paragraphs.size).isEqualTo(1)

        assertThat(paragraphs[0].style).isEqualTo(defaultParagraphStyle)
        assertThat(paragraphs[0].start).isEqualTo(0)
        assertThat(paragraphs[0].end).isEqualTo(1)
    }

    @Test
    fun length_returns_text_length() {
        val text = "abc"
        val annotatedString = AnnotatedString(text)
        assertThat(annotatedString.length).isEqualTo(text.length)
    }

    @Test
    fun toString_returns_text() {
        val text = "abc"
        val annotatedString = AnnotatedString(text)
        assertThat(annotatedString.toString()).isEqualTo(text)
    }

    @Test
    fun plus_operator_creates_a_new_annotated_string() {
        val text1 = "Hello"
        val textStyles1 = listOf(
            AnnotatedString.Item(TextStyle(color = Color.Red), 0, 3),
            AnnotatedString.Item(TextStyle(color = Color.Blue), 2, 4)
        )
        val paragraphStyles1 = listOf(
            AnnotatedString.Item(ParagraphStyle(lineHeight = 20.sp), 0, 1),
            AnnotatedString.Item(ParagraphStyle(lineHeight = 30.sp), 1, 5)
        )
        val annotatedString1 = AnnotatedString(
            text = text1,
            textStyles = textStyles1,
            paragraphStyles = paragraphStyles1
        )

        val text2 = "World"
        val textStyle = TextStyle(color = Color.Cyan)
        val paragraphStyle = ParagraphStyle(lineHeight = 10.sp)
        val annotatedString2 = AnnotatedString(
            text = text2,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text2.length)),
            paragraphStyles = listOf(AnnotatedString.Item(paragraphStyle, 0, text2.length))
        )

        val plusResult = annotatedString1 + annotatedString2

        val expectedTextStyles = textStyles1 + listOf(
            AnnotatedString.Item(textStyle, text1.length, text1.length + text2.length)
        )
        val expectedParagraphStyles = paragraphStyles1 + listOf(
            AnnotatedString.Item(paragraphStyle, text1.length, text1.length + text2.length)
        )
        assertThat(plusResult.text).isEqualTo("$text1$text2")
        assertThat(plusResult.textStyles).isEqualTo(expectedTextStyles)
        assertThat(plusResult.paragraphStyles).isEqualTo(expectedParagraphStyles)
    }

    @Test
    fun string_plus_annotated_string_returns_string() {
        val string = "Hello"
        val text = "World"
        assertThat(
            string + AnnotatedString(
                text = text,
                textStyles = listOf(
                    AnnotatedString.Item(TextStyle(color = Color.Red), 0, 1)
                ),
                paragraphStyles = listOf(
                    AnnotatedString.Item(ParagraphStyle(lineHeight = 20.sp), 0, 2)
                )
            )
        ).isEqualTo("$string$text")
    }
}