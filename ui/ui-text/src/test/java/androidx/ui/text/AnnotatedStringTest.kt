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

import androidx.ui.text.style.TextAlign
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AnnotatedStringTest {
    @Test
    fun `test normalizedParagraphStyles`() {
        val text = "Hello World"
        val paragraphStyle = ParagraphStyle(textAlign = TextAlign.Center)
        val paragraphStyles = listOf(AnnotatedString.Item(paragraphStyle, 0, 5))
        val annotatedString = AnnotatedString(text = text, paragraphStyles = paragraphStyles)
        val defaultParagraphStyle = ParagraphStyle(lineHeight = 2.0f)

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
    fun `test normalizedParagraphStyles only string`() {
        val text = "Hello World"
        val annotatedString = AnnotatedString(text = text)
        val defaultParagraphStyle = ParagraphStyle(lineHeight = 2.0f)

        val paragraphs = annotatedString.normalizedParagraphStyles(defaultParagraphStyle)

        assertThat(paragraphs.size).isEqualTo(1)

        assertThat(paragraphs[0].style).isEqualTo(defaultParagraphStyle)
        assertThat(paragraphs[0].start).isEqualTo(0)
        assertThat(paragraphs[0].end).isEqualTo(text.length)
    }

    @Test
    fun `test normalizedParagraphStyles empty string`() {
        val text = ""
        val annotatedString = AnnotatedString(text = text)
        val defaultParagraphStyle = ParagraphStyle(lineHeight = 2.0f)

        val paragraphs = annotatedString.normalizedParagraphStyles(defaultParagraphStyle)

        assertThat(paragraphs.size).isEqualTo(1)

        assertThat(paragraphs[0].style).isEqualTo(defaultParagraphStyle)
        assertThat(paragraphs[0].start).isEqualTo(0)
        assertThat(paragraphs[0].end).isEqualTo(text.length)
    }

    @Test
    fun `test normalizedParagraphStyles with newLine`() {
        val text = "Hello\nWorld"
        val annotatedString = AnnotatedString(text = text)
        val defaultParagraphStyle = ParagraphStyle(lineHeight = 2.0f)

        val paragraphs = annotatedString.normalizedParagraphStyles(defaultParagraphStyle)

        assertThat(paragraphs.size).isEqualTo(1)

        assertThat(paragraphs[0].style).isEqualTo(defaultParagraphStyle)
        assertThat(paragraphs[0].start).isEqualTo(0)
        assertThat(paragraphs[0].end).isEqualTo(text.length)
    }

    @Test
    fun `test normalizedParagraphStyles with only lineFeed`() {
        val text = "\n"
        val annotatedString = AnnotatedString(text = text)
        val defaultParagraphStyle = ParagraphStyle(lineHeight = 2.0f)

        val paragraphs = annotatedString.normalizedParagraphStyles(defaultParagraphStyle)

        assertThat(paragraphs.size).isEqualTo(1)

        assertThat(paragraphs[0].style).isEqualTo(defaultParagraphStyle)
        assertThat(paragraphs[0].start).isEqualTo(0)
        assertThat(paragraphs[0].end).isEqualTo(1)
    }
}