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
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontWeight
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

    @Test
    fun pushStyle() {
        val text = "Test"
        val style = TextStyle(color = Color.Red)
        val buildResult = AnnotatedString.Builder().apply {
            push(style)
            append(text)
            pop()
        }.build()

        assertThat(buildResult.text).isEqualTo(text)
        assertThat(buildResult.textStyles).hasSize(1)
        assertThat(buildResult.textStyles[0].style).isEqualTo(style)
        assertThat(buildResult.textStyles[0].start).isEqualTo(0)
        assertThat(buildResult.textStyles[0].end).isEqualTo(buildResult.length)
    }

    @Test
    fun pushStyle_without_pop() {
        val styles = arrayOf(
            TextStyle(color = Color.Red),
            TextStyle(fontStyle = FontStyle.Italic),
            TextStyle(fontWeight = FontWeight.Bold)
        )

        val buildResult = AnnotatedString.Builder().apply {
            styles.forEachIndexed { index, textStyle ->
                // pop is intentionally not called here
                push(textStyle)
                append("Style$index")
            }
        }.build()

        assertThat(buildResult.text).isEqualTo("Style0Style1Style2")
        assertThat(buildResult.textStyles).hasSize(3)

        styles.forEachIndexed { index, textStyle ->
            assertThat(buildResult.textStyles[index].style).isEqualTo(textStyle)
            assertThat(buildResult.textStyles[index].end).isEqualTo(buildResult.length)
        }

        assertThat(buildResult.textStyles[0].start).isEqualTo(0)
        assertThat(buildResult.textStyles[1].start).isEqualTo("Style0".length)
        assertThat(buildResult.textStyles[2].start).isEqualTo("Style0Style1".length)
    }

    @Test
    fun pushStyle_with_multiple_styles() {
        val textStyle1 = TextStyle(color = Color.Red)
        val textStyle2 = TextStyle(fontStyle = FontStyle.Italic)

        val buildResult = AnnotatedString.Builder().apply {
            push(textStyle1)
            append("Test")
            push(textStyle2)
            append(" me")
            pop()
            pop()
        }.build()

        assertThat(buildResult.text).isEqualTo("Test me")
        assertThat(buildResult.textStyles).hasSize(2)

        assertThat(buildResult.textStyles[0].style).isEqualTo(textStyle1)
        assertThat(buildResult.textStyles[0].start).isEqualTo(0)
        assertThat(buildResult.textStyles[0].end).isEqualTo(buildResult.length)

        assertThat(buildResult.textStyles[1].style).isEqualTo(textStyle2)
        assertThat(buildResult.textStyles[1].start).isEqualTo("Test".length)
        assertThat(buildResult.textStyles[1].end).isEqualTo(buildResult.length)
    }

    @Test
    fun pushStyle_with_multiple_styles_on_top_of_each_other() {
        val styles = arrayOf(
            TextStyle(color = Color.Red),
            TextStyle(fontStyle = FontStyle.Italic),
            TextStyle(fontWeight = FontWeight.Bold)
        )

        val buildResult = AnnotatedString.Builder().apply {
            styles.forEach { textStyle ->
                // pop is intentionally not called here
                push(textStyle)
            }
        }.build()

        assertThat(buildResult.text).isEmpty()
        assertThat(buildResult.textStyles).hasSize(3)
        styles.forEachIndexed { index, textStyle ->
            assertThat(buildResult.textStyles[index].style).isEqualTo(textStyle)
            assertThat(buildResult.textStyles[index].start).isEqualTo(buildResult.length)
            assertThat(buildResult.textStyles[index].end).isEqualTo(buildResult.length)
        }
    }

    @Test
    fun pushStyle_with_multiple_stacks_should_construct_styles_in_the_same_order() {
        val styles = arrayOf(
            TextStyle(color = Color.Red),
            TextStyle(fontStyle = FontStyle.Italic),
            TextStyle(fontWeight = FontWeight.Bold),
            TextStyle(letterSpacing = 1.2f)
        )

        val buildResult = AnnotatedString.Builder()
            .push(styles[0])
            .append("layer1-1")
            .push(styles[1])
            .append("layer2-1")
            .push(styles[2])
            .append("layer3-1")
            .pop()
            .push(styles[3])
            .append("layer3-2")
            .pop()
            .append("layer2-2")
            .pop()
            .append("layer1-2")
            .build()

        assertThat(buildResult.textStyles).hasSize(4)
        styles.forEachIndexed { index, textStyle ->
            assertThat(buildResult.textStyles[index].style).isEqualTo(textStyle)
        }
    }

    @Test
    fun pushStyle_with_multiple_nested_styles_should_return_styles_in_same_order() {
        val styles = arrayOf(
            TextStyle(color = Color.Red),
            TextStyle(fontStyle = FontStyle.Italic),
            TextStyle(fontWeight = FontWeight.Bold),
            TextStyle(letterSpacing = 1.2f)
        )

        val buildResult = AnnotatedString.Builder()
            .push(styles[0])
            .append("layer1-1")
            .push(styles[1])
            .append("layer2-1")
            .pop()
            .push(styles[2])
            .append("layer2-2")
            .push(styles[3])
            .append("layer3-1")
            .pop()
            .append("layer2-3")
            .pop()
            .append("layer1-2")
            .pop()
            .build()

        assertThat(buildResult.textStyles).hasSize(4)
        styles.forEachIndexed { index, textStyle ->
            assertThat(buildResult.textStyles[index].style).isEqualTo(textStyle)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun pop_when_empty_does_not_throw_exception() {
        AnnotatedString.Builder().pop()
    }

    @Test
    fun pop_in_the_middle() {
        val textStyle1 = TextStyle(color = Color.Red)
        val textStyle2 = TextStyle(fontStyle = FontStyle.Italic)

        val buildResult = AnnotatedString.Builder()
            .append("Style0")
            .push(textStyle1)
            .append("Style1")
            .pop()
            .push(textStyle2)
            .append("Style2")
            .pop()
            .append("Style3")
            .build()

        assertThat(buildResult.text).isEqualTo("Style0Style1Style2Style3")
        assertThat(buildResult.textStyles).hasSize(2)

        // the order is first applied is in the second
        assertThat(buildResult.textStyles[0].style).isEqualTo((textStyle1))
        assertThat(buildResult.textStyles[0].start).isEqualTo(("Style0".length))
        assertThat(buildResult.textStyles[0].end).isEqualTo(("Style0Style1".length))

        assertThat(buildResult.textStyles[1].style).isEqualTo((textStyle2))
        assertThat(buildResult.textStyles[1].start).isEqualTo(("Style0Style1".length))
        assertThat(buildResult.textStyles[1].end).isEqualTo(("Style0Style1Style2".length))
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