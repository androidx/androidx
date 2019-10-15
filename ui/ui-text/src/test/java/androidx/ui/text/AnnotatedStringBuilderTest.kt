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

import androidx.ui.core.Sp
import androidx.ui.core.sp
import androidx.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AnnotatedStringBuilderTest {

    @Test
    fun testDefaultConstructor() {
        val annotatedString = AnnotatedString.Builder().build()

        assertThat(annotatedString.text).isEmpty()
        assertThat(annotatedString.textStyles).isEmpty()
        assertThat(annotatedString.paragraphStyles).isEmpty()
    }

    @Test
    fun testConstructorWithString() {
        val text = "a"
        val annotatedString = AnnotatedString.Builder(text).build()

        assertThat(annotatedString.text).isEqualTo(text)
        assertThat(annotatedString.textStyles).isEmpty()
        assertThat(annotatedString.paragraphStyles).isEmpty()
    }

    @Test
    fun testConstructorWithAnnotatedString_hasSameAnnotatedStringAttributes() {
        val text = createAnnotatedString(text = "a")
        val annotatedString = AnnotatedString.Builder(text).build()

        assertThat(annotatedString.text).isEqualTo(text.text)
        assertThat(annotatedString.textStyles).isEqualTo(text.textStyles)
        assertThat(annotatedString.paragraphStyles).isEqualTo(text.paragraphStyles)
    }

    @Test
    fun textAddStyle_withTextStyle_addsStyle() {
        val style = TextStyle(color = Color.Red)
        val range = TextRange(0, 1)
        val annotatedString = AnnotatedString.Builder("ab")
            .addStyle(style, range.start, range.end)
            .build()

        val expectedTextStyles = listOf(
            AnnotatedString.Item(style, range.start, range.end)
        )

        assertThat(annotatedString.paragraphStyles).isEmpty()
        assertThat(annotatedString.textStyles).isEqualTo(expectedTextStyles)
    }

    @Test
    fun textAddStyle_withParagraphStyle_addsStyle() {
        val style = ParagraphStyle(lineHeight = 30.sp)
        val range = TextRange(0, 1)
        val annotatedString = AnnotatedString.Builder("ab")
            .addStyle(style, range.start, range.end)
            .build()

        val expectedParagraphStyles = listOf(
            AnnotatedString.Item(style, range.start, range.end)
        )

        assertThat(annotatedString.textStyles).isEmpty()
        assertThat(annotatedString.paragraphStyles).isEqualTo(expectedParagraphStyles)
    }

    @Test
    fun testAppend_withString_appendsTheText() {
        val text = "a"
        val appendedText = "b"
        val annotatedString = AnnotatedString.Builder(text).append(appendedText).build()

        val expectedString = "$text$appendedText"

        assertThat(annotatedString.text).isEqualTo(expectedString)
        assertThat(annotatedString.textStyles).isEmpty()
        assertThat(annotatedString.paragraphStyles).isEmpty()
    }

    @Test
    fun testAppend_withString_andMultipleCalls_appendsAllOfTheText() {
        val annotatedString = AnnotatedString.Builder("a").append("b").append("c").build()

        assertThat(annotatedString.text).isEqualTo("abc")
    }

    @Test
    fun testAppend_withAnnotatedString_appendsTheText() {
        val color = Color.Red
        val text = "a"
        val lineHeight = 20.sp
        val annotatedString = createAnnotatedString(
            text = text,
            color = color,
            lineHeight = lineHeight
        )

        val appendedColor = Color.Blue
        val appendedText = "b"
        val appendedLineHeight = 30.sp
        val appendedAnnotatedString = createAnnotatedString(
            text = appendedText,
            color = appendedColor,
            lineHeight = appendedLineHeight
        )

        val buildResult = AnnotatedString.Builder(annotatedString)
            .append(appendedAnnotatedString)
            .build()

        val expectedString = "$text$appendedText"
        val expectedTextStyles = listOf(
            AnnotatedString.Item(
                style = TextStyle(color),
                start = 0,
                end = text.length
            ),
            AnnotatedString.Item(
                style = TextStyle(appendedColor),
                start = text.length,
                end = expectedString.length
            )
        )

        val expectedParagraphStyles = listOf(
            AnnotatedString.Item(
                style = ParagraphStyle(lineHeight = lineHeight),
                start = 0,
                end = text.length
            ),
            AnnotatedString.Item(
                style = ParagraphStyle(lineHeight = appendedLineHeight),
                start = text.length,
                end = expectedString.length
            )
        )

        assertThat(buildResult.text).isEqualTo(expectedString)
        assertThat(buildResult.textStyles).isEqualTo(expectedTextStyles)
        assertThat(buildResult.paragraphStyles).isEqualTo(expectedParagraphStyles)
    }

    private fun createAnnotatedString(
        text: String,
        color: Color = Color.Red,
        lineHeight: Sp = 20.sp
    ): AnnotatedString {
        return AnnotatedString(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(
                    style = TextStyle(color),
                    start = 0,
                    end = text.length
                )
            ),
            paragraphStyles = listOf(
                AnnotatedString.Item(
                    style = ParagraphStyle(lineHeight = lineHeight),
                    start = 0,
                    end = text.length
                )
            )
        )
    }
}