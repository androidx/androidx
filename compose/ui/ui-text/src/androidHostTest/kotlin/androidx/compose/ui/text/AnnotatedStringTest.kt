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

package androidx.compose.ui.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AnnotatedStringTest {

    val par1 = ParagraphStyle(textIndent = TextIndent(10.sp))
    val par2 = ParagraphStyle(hyphens = Hyphens.Auto)
    val par3 = ParagraphStyle(textDirection = TextDirection.Rtl)
    val par4 = ParagraphStyle(lineBreak = LineBreak.Simple)

    @Test
    fun normalizedParagraphStyles_nested() {
        val testString = buildAnnotatedString {
            withStyle(par1) {
                append("a")
                withStyle(par2) { append("b") }
                append("c")
            }
        }

        val paragraphs = testString.normalizedParagraphStyles(ParagraphStyle())
        assertThat(paragraphs)
            .isEqualTo(listOf(Range(par1, 0, 1), Range(par1.merge(par2), 1, 2), Range(par1, 2, 3)))
    }

    @Test
    fun normalizedParagraphStyles_overlapped() {
        val testString = buildAnnotatedString {
            append("1234")
            addStyle(par1, 0, 4)
            addStyle(par2, 0, 2)
        }
        val paragraphs = testString.normalizedParagraphStyles(ParagraphStyle())

        assertThat(paragraphs).isEqualTo(listOf(Range(par1.merge(par2), 0, 2), Range(par1, 2, 4)))
    }

    @Test
    fun normalizedParagraphStyles_stackCorrectlyCleared() {
        val testString = buildAnnotatedString {
            append("0")
            withStyle(par1) {
                append("a")
                withStyle(par2) { append("b") }
                append("c")
            }
            withStyle(par3) { append("f") }
        }

        val paragraphs = testString.normalizedParagraphStyles(ParagraphStyle())
        assertThat(paragraphs)
            .isEqualTo(
                listOf(
                    Range(ParagraphStyle(), 0, 1),
                    Range(par1, 1, 2),
                    Range(par1.merge(par2), 2, 3),
                    Range(par1, 3, 4),
                    Range(par3, 4, 5),
                )
            )
    }

    @Test
    fun normalizedParagraphStyles_stackCorrectlyCleared_whenEndsOverlap() {
        val testString = buildAnnotatedString {
            withStyle(par1) {
                append("a")
                withStyle(par2) { append("b") }
            } // check this is cleared correctly
            withStyle(par3) { append("c") }
        }

        val paragraphs = testString.normalizedParagraphStyles(ParagraphStyle())
        assertThat(paragraphs)
            .isEqualTo(listOf(Range(par1, 0, 1), Range(par1.merge(par2), 1, 2), Range(par3, 2, 3)))
    }

    @Test
    fun normalizedParagraphStyles_fullyOverlapped() {
        val testString = buildAnnotatedString {
            withStyle(par1) { withStyle(par2) { append("a") } }
        }

        val paragraphs = testString.normalizedParagraphStyles(ParagraphStyle())
        assertThat(paragraphs).isEqualTo(listOf(Range(par1.merge(par2), 0, 1)))
    }

    @Test
    fun normalizedParagraphStyles_fullyOverlapped_stackCorrectlyCleared() {
        val testString = buildAnnotatedString {
            withStyle(par1) { withStyle(par2) { append("a") } }
            withStyle(par3) { append("b") }
        }

        val paragraphs = testString.normalizedParagraphStyles(ParagraphStyle())
        assertThat(paragraphs).isEqualTo(listOf(Range(par1.merge(par2), 0, 1), Range(par3, 1, 2)))
    }

    @Test
    fun normalizedParagraphStyles_complex_withNoParagraphsInBetween() {
        val testString = buildAnnotatedString {
            append("text1") // 0-5
            withStyle(par1) {
                append("text2") // 5-10
                withStyle(par2) {
                    append("text3") // 10-15
                }
            }
            append("text4") // 15-20
            withStyle(par3) {
                append("text5") // 20-25
            }
            append("text6") // 25-30
        }

        val default = ParagraphStyle()
        val paragraphs = testString.normalizedParagraphStyles(default)

        assertThat(paragraphs)
            .isEqualTo(
                listOf(
                    Range(default, 0, 5),
                    Range(par1, 5, 10),
                    Range(par1.merge(par2), 10, 15),
                    Range(default, 15, 20),
                    Range(par3, 20, 25),
                    Range(default, 25, 30),
                )
            )
    }

    @Test
    fun normalizedParagraphStyles_complex_nestedSiblingParagraphs() {
        val testString = buildAnnotatedString {
            append("text1") // 0-5
            withStyle(par1) {
                append("text2") // 5-10
                withStyle(par2) {
                    append("text3") // 10-15
                }
                withStyle(par3) {
                    append("text4") // 15-20
                }
                append("text5") // 20-25
            }
        }

        val default = ParagraphStyle()
        val paragraphs = testString.normalizedParagraphStyles(default)

        assertThat(paragraphs)
            .isEqualTo(
                listOf(
                    Range(default, 0, 5),
                    Range(par1, 5, 10),
                    Range(par1.merge(par2), 10, 15),
                    Range(par1.merge(par3), 15, 20),
                    Range(par1, 20, 25),
                )
            )
    }

    @Test
    fun normalizedParagraphStyle_withBlankLinesAround() {
        val testString = buildAnnotatedString {
            pushStyle(par1)
            append("")
            pop()
            pushStyle(par2)
            append("a")
            pop()
            pushStyle(par3)
            append("")
            pop()
        }

        val paragraphs = testString.normalizedParagraphStyles(ParagraphStyle())

        assertThat(paragraphs)
            .isEqualTo(listOf(Range(par1, 0, 0), Range(par2, 0, 1), Range(par3, 1, 1)))
    }

    @Test
    fun normalizedParagraphStyle_withBlankLinesAtEnd() {
        val testString = buildAnnotatedString {
            pushStyle(par1)
            append("a")
            pop()
            pushStyle(par2)
            append("")
            pop()
            pushStyle(par3)
            append("")
            pop()
        }

        val paragraphs = testString.normalizedParagraphStyles(ParagraphStyle())

        assertThat(paragraphs).isEqualTo(listOf(Range(par1, 0, 1), Range(par2.merge(par3), 1, 1)))
    }

    @Test
    fun normalizedParagraphStyle_withBlankLines_correctlyClearsStack() {
        val testString = buildAnnotatedString {
            withStyle(par1) {
                append("")
                withStyle(par2) { append("") }
                append("")
            }
            append("a")
        }

        val paragraphs = testString.normalizedParagraphStyles(ParagraphStyle())

        assertThat(paragraphs)
            .isEqualTo(listOf(Range(par1.merge(par2), 0, 0), Range(ParagraphStyle(), 0, 1)))
    }

    @Test
    fun normalizedParagraphStyles_multiLevelNested() {
        val testString = buildAnnotatedString {
            append("text1") // 0-5
            withStyle(par1) {
                append("text2") // 5-10
                withStyle(par2) {
                    withStyle(par3) {
                        append("text3") // 10-15
                        withStyle(par4) {
                            append("text4") // 15-20
                        }
                    }
                }
                append("text5") // 20-25
            }
        }

        val default = ParagraphStyle()
        val paragraphs = testString.normalizedParagraphStyles(default)

        assertThat(paragraphs)
            .isEqualTo(
                listOf(
                    Range(default, 0, 5),
                    Range(par1, 5, 10),
                    Range(par1.merge(par2).merge(par3), 10, 15),
                    Range(par1.merge(par2).merge(par3).merge(par4), 15, 20),
                    Range(par1, 20, 25),
                )
            )
    }

    @Test
    fun normalizedParagraphStyles_zeroLengthParagraph_atStart() {
        val testString = buildAnnotatedString {
            withStyle(par1) {}
            append("0")
            withStyle(par2) { append("a") }
        }

        val paragraphs = testString.normalizedParagraphStyles(ParagraphStyle())
        assertThat(paragraphs)
            .isEqualTo(listOf(Range(par1, 0, 0), Range(ParagraphStyle(), 0, 1), Range(par2, 1, 2)))
    }

    @Test
    fun normalizedParagraphStyles_zeroLengthParagraph() {
        val testString = buildAnnotatedString {
            append("0")
            withStyle(par1) {}
            withStyle(par2) { append("a") }
        }

        val paragraphs = testString.normalizedParagraphStyles(ParagraphStyle())
        assertThat(paragraphs)
            .isEqualTo(listOf(Range(ParagraphStyle(), 0, 1), Range(par1, 1, 1), Range(par2, 1, 2)))
    }

    @Test
    fun normalizedParagraphStyles() {
        val text = "Hello World"
        val paragraphStyle = ParagraphStyle(textAlign = TextAlign.Center)
        val paragraphStyles = listOf(Range(paragraphStyle, 0, 5))
        val annotatedString = AnnotatedString(text = text, paragraphStyles = paragraphStyles)
        val defaultParagraphStyle = ParagraphStyle(lineHeight = 20.sp)

        val paragraphs = annotatedString.normalizedParagraphStyles(defaultParagraphStyle)

        assertThat(paragraphs)
            .isEqualTo(
                listOf(
                    Range(defaultParagraphStyle.merge(paragraphStyle), 0, 5),
                    Range(defaultParagraphStyle, 5, text.length),
                )
            )
    }

    @Test
    fun normalizedParagraphStyles_only_string() {
        val text = "Hello World"
        val annotatedString = AnnotatedString(text = text)
        val defaultParagraphStyle = ParagraphStyle(lineHeight = 20.sp)

        val paragraphs = annotatedString.normalizedParagraphStyles(defaultParagraphStyle)

        assertThat(paragraphs).isEqualTo(listOf(Range(defaultParagraphStyle, 0, text.length)))
    }

    @Test
    fun normalizedParagraphStyles_empty_string() {
        val text = ""
        val annotatedString = AnnotatedString(text = text)
        val defaultParagraphStyle = ParagraphStyle(lineHeight = 20.sp)

        val paragraphs = annotatedString.normalizedParagraphStyles(defaultParagraphStyle)

        assertThat(paragraphs).isEqualTo(listOf(Range(defaultParagraphStyle, 0, text.length)))
    }

    @Test
    fun normalizedParagraphStyles_zeroLength_paragraphStyle() {
        val text = "a"
        val par = ParagraphStyle(textDirection = TextDirection.Rtl)
        val annotatedString =
            AnnotatedString(text = text, paragraphStyles = listOf(Range(par, 0, 0)))
        val defaultParagraphStyle = ParagraphStyle(lineHeight = 20.sp)

        val paragraphs = annotatedString.normalizedParagraphStyles(defaultParagraphStyle)

        assertThat(paragraphs)
            .isEqualTo(
                listOf(
                    Range(defaultParagraphStyle.merge(par), 0, 0),
                    Range(defaultParagraphStyle, 0, 1),
                )
            )
    }

    @Test
    fun normalizedParagraphStyles_with_newLine() {
        val text = "Hello\nWorld"
        val annotatedString = AnnotatedString(text = text)
        val defaultParagraphStyle = ParagraphStyle(lineHeight = 20.sp)

        val paragraphs = annotatedString.normalizedParagraphStyles(defaultParagraphStyle)

        assertThat(paragraphs).isEqualTo(listOf(Range(defaultParagraphStyle, 0, text.length)))
    }

    @Test
    fun normalizedParagraphStyles_with_only_lineFeed() {
        val text = "\n"
        val annotatedString = AnnotatedString(text = text)
        val defaultParagraphStyle = ParagraphStyle(lineHeight = 20.sp)

        val paragraphs = annotatedString.normalizedParagraphStyles(defaultParagraphStyle)

        assertThat(paragraphs).isEqualTo(listOf(Range(defaultParagraphStyle, 0, 1)))
    }

    @Test
    fun length_returns_text_length() {
        val text = "abc"
        val annotatedString = AnnotatedString(text)
        assertThat(annotatedString.length).isEqualTo(text.length)
    }

    @Test
    fun plus_operator_creates_a_new_annotated_string() {
        val text1 = "Hello"
        val annotations1 =
            listOf(
                Range(SpanStyle(color = Color.Red), 0, 3),
                Range(SpanStyle(color = Color.Blue), 2, 4),
                Range(ParagraphStyle(lineHeight = 20.sp), 0, 1),
                Range(ParagraphStyle(lineHeight = 30.sp), 1, 5),
                Range(StringAnnotation("annotation1"), 0, 2, "scope1"),
                Range(StringAnnotation("annotation1"), 3, 5, "scope1"),
            )
        val annotatedString1 = AnnotatedString(text = text1, annotations = annotations1)

        val text2 = "World"
        val spanStyle = SpanStyle(color = Color.Cyan)
        val paragraphStyle = ParagraphStyle(lineHeight = 10.sp)
        val annotatedString2 =
            AnnotatedString(
                text = text2,
                annotations =
                    listOf(
                        Range(spanStyle, 0, text2.length),
                        Range(paragraphStyle, 0, text2.length),
                        Range(StringAnnotation("annotation2"), 0, text2.length, "scope2"),
                    ),
            )

        assertThat(annotatedString1 + annotatedString2)
            .isEqualTo(
                AnnotatedString(
                    "$text1$text2",
                    annotations1 +
                        listOf(
                            Range(spanStyle, text1.length, text1.length + text2.length),
                            Range(paragraphStyle, text1.length, text1.length + text2.length),
                            Range(
                                StringAnnotation("annotation2"),
                                text1.length,
                                text1.length + text2.length,
                                "scope2",
                            ),
                        ),
                )
            )

        assertThat((annotatedString1 + annotatedString2).spanStyles)
            .isEqualTo(
                listOf(
                    Range(SpanStyle(color = Color.Red), 0, 3),
                    Range(SpanStyle(color = Color.Blue), 2, 4),
                    Range(spanStyle, text1.length, text1.length + text2.length),
                )
            )
        assertThat((annotatedString1 + annotatedString2).paragraphStyles)
            .isEqualTo(
                listOf(
                    Range(ParagraphStyle(lineHeight = 20.sp), 0, 1),
                    Range(ParagraphStyle(lineHeight = 30.sp), 1, 5),
                    Range(paragraphStyle, text1.length, text1.length + text2.length),
                )
            )
    }

    @Test
    fun subSequence_returns_the_correct_string() {
        val annotatedString = AnnotatedString.Builder("abcd").toAnnotatedString()

        assertThat(annotatedString.subSequence(1, 3).text).isEqualTo("bc")
    }

    @Test
    fun subSequence_returns_empty_text_for_start_equals_end() {
        val annotatedString =
            with(AnnotatedString.Builder()) {
                    withStyle(SpanStyle(fontSize = 12.sp)) { append("a") }
                    withStyle(SpanStyle(fontSize = 12.sp)) { append("b") }
                    withStyle(ParagraphStyle(lineHeight = 14.sp)) { append("c") }
                    toAnnotatedString()
                }
                .subSequence(1, 1)

        assertThat(annotatedString)
            .isEqualTo(AnnotatedString("", listOf(Range(SpanStyle(fontSize = 12.sp), 0, 0))))
    }

    @Test
    fun subSequence_returns_original_text_for_text_range_is_full_range() {
        val annotatedString =
            with(AnnotatedString.Builder()) {
                withStyle(SpanStyle(fontSize = 12.sp)) { append("a") }
                withStyle(SpanStyle(fontSize = 12.sp)) { append("b") }
                withStyle(ParagraphStyle(lineHeight = 14.sp)) { append("c") }
                toAnnotatedString()
            }

        assertThat(annotatedString.subSequence(0, 3)).isSameInstanceAs(annotatedString)
    }

    @Test
    fun subSequence_doesNot_include_styles_before_the_start() {
        val annotatedString =
            with(AnnotatedString.Builder()) {
                withStyle(SpanStyle(fontSize = 12.sp)) { append("a") }
                withStyle(ParagraphStyle(lineHeight = 14.sp)) { append("b") }
                append("c")
                toAnnotatedString()
            }

        assertThat(annotatedString.subSequence("ab".length, annotatedString.length))
            .isEqualTo(AnnotatedString("c"))
    }

    @Test
    fun subSequence_doesNot_include_styles_after_the_end() {
        val annotatedString =
            with(AnnotatedString.Builder()) {
                append("a")
                withStyle(SpanStyle(fontSize = 12.sp)) { append("b") }
                withStyle(ParagraphStyle(lineHeight = 14.sp)) { append("c") }
                toAnnotatedString()
            }

        assertThat(annotatedString.subSequence(0, "a".length)).isEqualTo(AnnotatedString("a"))
    }

    @Test
    fun subSequence_collapsed_item_with_itemStart_equalTo_rangeStart() {
        val style = SpanStyle(fontSize = 12.sp)
        val annotatedString =
            with(AnnotatedString.Builder()) {
                append("abc")
                // add collapsed item at the beginning of b
                addStyle(style, 1, 1)
                toAnnotatedString()
            }

        assertThat(annotatedString.subSequence(1, 2))
            .isEqualTo(AnnotatedString("b", listOf(Range(style, 0, 0))))
    }

    @Test
    fun subSequence_collapses_included_item() {
        val style = SpanStyle(fontSize = 12.sp)
        val annotatedString =
            with(AnnotatedString.Builder()) {
                append("a")
                // will collapse this style in subsequence
                withStyle(style) { append("b") }
                append("c")
                toAnnotatedString()
            }

        // subsequence with 1,1 will remove text, but include the style
        assertThat(annotatedString.subSequence(1, 1))
            .isEqualTo(AnnotatedString("", listOf(Range(style, 0, 0))))
    }

    @Test
    fun subSequence_collapses_covering_item() {
        val style = SpanStyle(fontSize = 12.sp)
        val annotatedString =
            with(AnnotatedString.Builder()) {
                withStyle(style) { append("abc") }
                toAnnotatedString()
            }

        assertThat(annotatedString.subSequence(1, 1))
            .isEqualTo(AnnotatedString("", listOf(Range(style, 0, 0))))
    }

    @Test
    fun subSequence_with_collapsed_range_with_collapsed_item() {
        val style = SpanStyle(fontSize = 12.sp)
        val annotatedString =
            with(AnnotatedString.Builder()) {
                append("abc")
                // add collapsed item at the beginning of b
                addStyle(style, 1, 1)
                toAnnotatedString()
            }

        assertThat(annotatedString.subSequence(1, 1))
            .isEqualTo(AnnotatedString("", listOf(Range(style, 0, 0))))
    }

    @Test
    fun subSequence_includes_partial_matches() {
        val annotatedString =
            with(AnnotatedString.Builder()) {
                withStyle(SpanStyle(fontSize = 12.sp)) { append("ab") }
                withStyle(SpanStyle(fontSize = 12.sp)) { append("c") }
                withStyle(ParagraphStyle(lineHeight = 14.sp)) { append("de") }
                toAnnotatedString()
            }

        val expectedString =
            with(AnnotatedString.Builder()) {
                withStyle(SpanStyle(fontSize = 12.sp)) { append("b") }
                withStyle(SpanStyle(fontSize = 12.sp)) { append("c") }
                withStyle(ParagraphStyle(lineHeight = 14.sp)) { append("d") }
                toAnnotatedString()
            }

        val subSequence = annotatedString.subSequence("a".length, "abcd".length)

        assertThat(subSequence).isEqualTo(expectedString)
    }

    @Test
    fun subSequence_withAnnotations_noIntersection() {
        val annotatedString = buildAnnotatedString {
            append("ab")
            pushStringAnnotation("scope1", "annotation1")
            append("cd")
            pop()
            append("ef")
        }

        assertThat(annotatedString.subSequence(0, 2)).isEqualTo(AnnotatedString("ab"))
        assertThat(annotatedString.subSequence(4, 6)).isEqualTo(AnnotatedString("ef"))
    }

    @Test
    fun subSequence_withAnnotations_collapsedRange() {
        val annotatedString = buildAnnotatedString {
            append("ab")
            pushStringAnnotation("scope1", "annotation1")
            append("cd")
            pop()
            append("ef")
        }
        // Collapsed range equals to end, no annotation
        assertThat(annotatedString.subSequence(4, 4)).isEqualTo(AnnotatedString(""))

        // Collapsed range equals to start, has annotation
        assertThat(annotatedString.subSequence(2, 2))
            .isEqualTo(
                AnnotatedString(
                    "",
                    annotations = listOf(Range(StringAnnotation("annotation1"), 0, 0, "scope1")),
                )
            )

        // Collapsed range covered by annotation, has annotation
        assertThat(annotatedString.subSequence(3, 3))
            .isEqualTo(
                AnnotatedString(
                    "",
                    annotations = listOf(Range(StringAnnotation("annotation1"), 0, 0, "scope1")),
                )
            )
    }

    @Test
    fun subSequence_withAnnotations_hasIntersection() {
        val annotatedString = buildAnnotatedString {
            append("ab")
            pushStringAnnotation("scope1", "annotation1")
            append("cd")
            pop()
            append("ef")
        }
        // Overlapping range, has annotation
        assertThat(annotatedString.subSequence(0, 3))
            .isEqualTo(
                AnnotatedString(
                    "abc",
                    annotations = listOf(Range(StringAnnotation("annotation1"), 2, 3, "scope1")),
                )
            )

        // Overlapping, has annotation
        assertThat(annotatedString.subSequence(3, 5))
            .isEqualTo(
                AnnotatedString(
                    "de",
                    annotations = listOf(Range(StringAnnotation("annotation1"), 0, 1, "scope1")),
                )
            )
    }

    @Test
    fun subSequence_withAnnotations_containsRange() {
        val annotatedString = buildAnnotatedString {
            append("ab")
            pushStringAnnotation("scope1", "annotation1")
            append("cd")
            pop()
            append("ef")
        }

        // Contains range, has annotation
        assertThat(annotatedString.subSequence(0, 5))
            .isEqualTo(
                AnnotatedString(
                    "abcde",
                    annotations = listOf(Range(StringAnnotation("annotation1"), 2, 4, "scope1")),
                )
            )

        // Full range, has annotation
        assertThat(annotatedString.subSequence(2, 4))
            .isEqualTo(
                AnnotatedString(
                    "cd",
                    annotations = listOf(Range(StringAnnotation("annotation1"), 0, 2, "scope1")),
                )
            )
    }

    @Test(expected = IllegalArgumentException::class)
    fun subSequence_throws_exception_for_start_greater_than_end() {
        AnnotatedString("ab").subSequence(1, 0)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun subSequence_throws_exception_for_negative_values() {
        AnnotatedString("abc").subSequence(-1, 2)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun subSequence_throws_exception_when_start_is_out_of_bounds_values() {
        val text = "abc"
        AnnotatedString(text).subSequence(text.length, text.length + 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun creating_item_with_start_greater_than_end_throws_exception() {
        Range(SpanStyle(color = Color.Red), 1, 0)
    }

    @Test
    fun creating_item_with_start_equal_to_end_does_not_throw_exception() {
        Range(SpanStyle(color = Color.Red), 1, 1)
    }

    @Test
    fun constructor_function_with_single_spanStyle() {
        val text = "a"
        val spanStyle = SpanStyle(color = Color.Red)
        assertThat(AnnotatedString(text, spanStyle))
            .isEqualTo(AnnotatedString(text, listOf(Range(spanStyle, 0, text.length))))
    }

    @Test
    fun constructor_function_with_single_paragraphStyle() {
        val text = "a"
        val paragraphStyle = ParagraphStyle(lineHeight = 12.sp)
        assertThat(AnnotatedString(text, paragraphStyle))
            .isEqualTo(
                AnnotatedString(text, listOf(), listOf(Range(paragraphStyle, 0, text.length)))
            )
    }

    @Test
    fun constructor_function_with_single_spanStyle_and_paragraphStyle() {
        val text = "a"
        val spanStyle = SpanStyle(color = Color.Red)
        val paragraphStyle = ParagraphStyle(lineHeight = 12.sp)
        assertThat(AnnotatedString(text, spanStyle, paragraphStyle))
            .isEqualTo(
                AnnotatedString(
                    text,
                    listOf(Range(spanStyle, 0, text.length)),
                    listOf(Range(paragraphStyle, 0, text.length)),
                )
            )
    }

    @Test
    fun toString_returns_the_plain_string() {
        val text = "abc"
        assertThat(AnnotatedString(text).toString()).isEqualTo(text)
    }

    @Test
    fun toUpperCase_andAnnotatedString_dontCrash() {
        val annotatedString = buildAnnotatedString {
            append("non-empty something")
            pushStringAnnotation("tag", "annotation")
            append("non-empty anything")
            pop()
        }
        annotatedString.toUpperCase(Locale("en"))
    }

    @Test
    fun toUpperCase_andAnnotatedString_annotationAtStart_dontCrash() {
        val annotatedString = buildAnnotatedString {
            pushStringAnnotation("tag", "annotation")
            append("non-empty anything")
            pop()
            append("non-empty something")
        }
        annotatedString.toUpperCase(Locale("en"))
    }

    @Test
    fun toUpperCase_andAnnotatedString_annotationInMiddle_dontCrash() {
        val annotatedString = buildAnnotatedString {
            append("non-empty before")
            pushStringAnnotation("tag", "annotation")
            append("non-empty anything")
            pop()
            append("non-empty after")
        }
        annotatedString.toUpperCase(Locale("en"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun subSequence_throws_exception_for_overlapping_paragraphStyles() {
        buildAnnotatedString {
            append("1234")
            addStyle(ParagraphStyle(), 0, 2)
            addStyle(ParagraphStyle(), 1, 3)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun throws_exception_overlapping_exceedsLastMax() {
        buildAnnotatedString {
            append("12345")
            addStyle(ParagraphStyle(), 1, 4)
            addStyle(ParagraphStyle(), 2, 3)
            addStyle(ParagraphStyle(), 3, 5)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun throws_exception_overlapping_addsCurrentToStack_whenEmptyStack() {
        buildAnnotatedString {
            append("12345")
            addStyle(ParagraphStyle(), 1, 2)
            addStyle(ParagraphStyle(), 2, 4)
            addStyle(ParagraphStyle(), 3, 5)
        }
    }

    @Test
    fun doesNot_throws_exceedsMax() {
        buildAnnotatedString {
            append("12345")
            addStyle(ParagraphStyle(), 0, 3)
            addStyle(ParagraphStyle(), 1, 2)
            addStyle(ParagraphStyle(), 4, 5)
        }
    }

    @Test
    fun doesNot_throws_stackCleared_insideMaxRange() {
        buildAnnotatedString {
            append("12345")
            addStyle(ParagraphStyle(), 0, 3)
            addStyle(ParagraphStyle(), 1, 2)
            addStyle(ParagraphStyle(), 4, 5)
            addStyle(ParagraphStyle(), 4, 4)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun throws_exception_overlapsDisallowedRange() {
        buildAnnotatedString {
            append("1234567890")
            addStyle(ParagraphStyle(), 1, 10)
            addStyle(ParagraphStyle(), 1, 8)
            addStyle(ParagraphStyle(), 1, 6)
            addStyle(ParagraphStyle(), 1, 4)
            addStyle(ParagraphStyle(), 7, 9)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun throws_exception_overlapsDisallowedRange_first() {
        buildAnnotatedString {
            append("1234567890")
            addStyle(ParagraphStyle(), 1, 10)
            addStyle(ParagraphStyle(), 1, 8)
            addStyle(ParagraphStyle(), 1, 6)
            addStyle(ParagraphStyle(), 1, 4)
            addStyle(ParagraphStyle(), 3, 5)
        }
    }

    @Test
    fun doesNot_throw_fullyOverlapsDisallowedRange() {
        buildAnnotatedString {
            append("1234567890")
            addStyle(ParagraphStyle(), 1, 10)
            addStyle(ParagraphStyle(), 1, 8)
            addStyle(ParagraphStyle(), 1, 6)
            addStyle(ParagraphStyle(), 1, 4)
            addStyle(ParagraphStyle(), 7, 8)
        }
    }

    @Test
    fun doesNot_throw_insideAllowedRange() {
        buildAnnotatedString {
            append("1234567890")
            addStyle(ParagraphStyle(), 1, 9)
            addStyle(ParagraphStyle(), 2, 8)
            addStyle(ParagraphStyle(), 3, 6)
            addStyle(ParagraphStyle(), 4, 6)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun throws_exception_orderMatters_overlapping() {
        buildAnnotatedString {
            append("1234")
            addStyle(par1, 0, 2)
            addStyle(par2, 0, 4)
        }
    }

    @Test
    fun doesNot_throw_exception_orderMatters_nested() {
        buildAnnotatedString {
            append("1234")
            addStyle(par1, 0, 4)
            addStyle(par2, 0, 2)
        }
    }

    @Test
    fun doesNot_throw_exception_if_paragraphStyles_are_nested() {
        buildAnnotatedString {
            append("1234")
            addStyle(ParagraphStyle(), 3, 4)
            addStyle(ParagraphStyle(), 0, 4)
        }
    }

    @Test
    fun doesNot_throw_exception_if_paragraphStyles_are_fully_overlapped() {
        buildAnnotatedString {
            append("1234")
            addStyle(ParagraphStyle(), 3, 4)
            addStyle(ParagraphStyle(), 3, 4)
        }
    }

    @Test
    fun doesNot_throw_exception_if_paragraphStyles_are_not_sorted() {
        buildAnnotatedString {
            append("1234")
            addStyle(ParagraphStyle(), 3, 4)
            addStyle(ParagraphStyle(), 0, 2)
        }
    }

    @Test
    fun AnnotatedStringSaver_isAnnotatedStringSaver() {
        // already covered by existing tests if this is true
        assertThat(AnnotatedString.Saver).isSameInstanceAs(AnnotatedStringSaver)
    }

    @Test
    fun builder_packedRangeBounds_pushPopAndAddStyle() {
        val text = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Red)) {
                append("Hello ")
                withStyle(SpanStyle(fontSize = 18.sp)) {
                    append("World")
                }
            }
        }
        assertThat(text.text).isEqualTo("Hello World")
        assertThat(text.spanStyles).hasSize(2)
        assertThat(text.spanStyles[0].start).isEqualTo(0)
        assertThat(text.spanStyles[0].end).isEqualTo(11)
        assertThat(text.spanStyles[1].start).isEqualTo(6)
        assertThat(text.spanStyles[1].end).isEqualTo(11)
    }

    @Test
    fun builderAppend_andSubstringWithoutParagraphStyles_preserveZeroLengthEndRange() {
        val endSpan = AnnotatedString.Range(SpanStyle(color = Color.Red), 3, 3)
        val endTag = AnnotatedString.Range("meta", 3, 3, "tag")
        val source = buildAnnotatedString {
            append("abc")
            addStyle(ParagraphStyle(textAlign = TextAlign.Center), 0, 3)
            addStyle(endSpan.item, endSpan.start, endSpan.end)
            addStringAnnotation(endTag.tag, endTag.item, endTag.start, endTag.end)
        }

        val appended = buildAnnotatedString {
            append("12")
            append(source, 0, source.length)
        }
        assertThat(appended.spanStyles).containsExactly(AnnotatedString.Range(endSpan.item, 5, 5))
        assertThat(appended.getStringAnnotations(5, 5))
            .containsExactly(AnnotatedString.Range("meta", 5, 5, "tag"))

        val mapped =
            source.mapEachParagraphStyle(ParagraphStyle()) { paragraphText, _ -> paragraphText }
        assertThat(mapped).hasSize(1)
        assertThat(mapped[0].spanStyles).containsExactly(endSpan)
        assertThat(mapped[0].getStringAnnotations(3, 3)).containsExactly(endTag)
    }

    @Test
    fun constructor_partitioning_reusesListWhenHomogeneous_andSplitsWhenMixed() {
        assumeTrue(PackedTextStylesLatch.isEnabled)
        val spanRange = Range(SpanStyle(color = Color.Red), 0, 2)
        val parRange = Range(par1, 0, 2)
        val stringRange = Range(StringAnnotation("meta"), 0, 2, "tag")

        for (empty in
            listOf(
                AnnotatedString("ab"),
                AnnotatedString("ab", annotations = emptyList()),
                AnnotatedString("ab", annotations = ArrayList()),
                AnnotatedString("ab", spanStyles = emptyList(), paragraphStyles = emptyList()),
                AnnotatedString("ab", spanStyles = ArrayList(), paragraphStyles = ArrayList()),
                emptyAnnotatedString(),
            )) {
            assertThat(empty.annotations).isNull()
            assertThat(empty.spanStylesOrNull).isNull()
            assertThat(empty.paragraphStylesOrNull).isNull()
            assertThat(empty.spanStyles).isEmpty()
            assertThat(empty.paragraphStyles).isEmpty()
            assertThat(empty.hasOtherAnnotations).isFalse()
        }

        // Public constructors defensively copy external lists.
        val onlySpansList = listOf(spanRange)
        for (onlySpans in
            listOf(
                AnnotatedString("ab", annotations = onlySpansList),
                AnnotatedString("ab", spanStyles = onlySpansList, paragraphStyles = ArrayList()),
            )) {
            assertThat(onlySpans.annotations).isEqualTo(onlySpansList)
            assertThat(onlySpans.annotations).isNotSameInstanceAs(onlySpansList)
            assertThat(onlySpans.spanStylesOrNull).isEqualTo(onlySpansList)
            assertThat(onlySpans.paragraphStylesOrNull).isNull()
            assertThat(onlySpans.hasOtherAnnotations).isFalse()
        }
        // Trusted internal constructors reuse the list reference directly.
        val trustedSpans = AnnotatedString("ab", onlySpansList, trusted = true)
        assertThat(trustedSpans.annotations).isSameInstanceAs(onlySpansList)
        assertThat(trustedSpans.spanStylesOrNull).isSameInstanceAs(onlySpansList)

        val onlyParsList = listOf(parRange)
        for (onlyPars in
            listOf(
                AnnotatedString("ab", annotations = onlyParsList),
                AnnotatedString("ab", spanStyles = ArrayList(), paragraphStyles = onlyParsList),
            )) {
            assertThat(onlyPars.annotations).isEqualTo(onlyParsList)
            assertThat(onlyPars.annotations).isNotSameInstanceAs(onlyParsList)
            assertThat(onlyPars.paragraphStylesOrNull).isEqualTo(onlyParsList)
            assertThat(onlyPars.spanStylesOrNull).isNull()
            assertThat(onlyPars.hasOtherAnnotations).isFalse()
        }
        val trustedPars = AnnotatedString("ab", onlyParsList, trusted = true)
        assertThat(trustedPars.annotations).isSameInstanceAs(onlyParsList)
        assertThat(trustedPars.paragraphStylesOrNull).isSameInstanceAs(onlyParsList)

        val onlyOther = AnnotatedString("ab", annotations = listOf(stringRange))
        assertThat(onlyOther.spanStylesOrNull).isNull()
        assertThat(onlyOther.paragraphStylesOrNull).isNull()
        assertThat(onlyOther.hasOtherAnnotations).isTrue()

        val spanAndPar =
            AnnotatedString("ab", spanStyles = onlySpansList, paragraphStyles = onlyParsList)
        assertThat(spanAndPar.spanStyles).containsExactly(spanRange)
        assertThat(spanAndPar.paragraphStyles).containsExactly(parRange)
        assertThat(spanAndPar.hasOtherAnnotations).isFalse()

        val spanAndOther = AnnotatedString("ab", annotations = listOf(spanRange, stringRange))
        assertThat(spanAndOther.spanStyles).containsExactly(spanRange)
        assertThat(spanAndOther.paragraphStylesOrNull).isNull()
        assertThat(spanAndOther.hasOtherAnnotations).isTrue()

        val parAndOther = AnnotatedString("ab", annotations = listOf(parRange, stringRange))
        assertThat(parAndOther.spanStylesOrNull).isNull()
        assertThat(parAndOther.paragraphStyles).containsExactly(parRange)
        assertThat(parAndOther.hasOtherAnnotations).isTrue()
    }

    @Test
    fun sortParagraphStyles_preservesTieOrderOnStart_andEqualsHashCodeAndCharGet() {
        val validUnsortedWithTie =
            AnnotatedString(
                text = "0123456789",
                paragraphStyles =
                    listOf(
                        Range(par3, 6, 8),
                        Range(par1, 0, 4),
                        Range(par2, 0, 2),
                        Range(par1, 8, 10),
                    ),
            )
        assertThat(validUnsortedWithTie.normalizedParagraphStyles(ParagraphStyle()))
            .isEqualTo(
                listOf(
                    Range(par1.merge(par2), 0, 2),
                    Range(par1, 2, 4),
                    Range(ParagraphStyle(), 4, 6),
                    Range(par3, 6, 8),
                    Range(par1, 8, 10),
                )
            )

        assertFailsWith<IllegalArgumentException> {
            AnnotatedString(
                text = "0123456789",
                paragraphStyles = listOf(Range(par3, 6, 8), Range(par2, 0, 2), Range(par1, 0, 4)),
            )
        }

        val span = Range(SpanStyle(color = Color.Red), 0, 2)
        val a1 = AnnotatedString("hello", listOf(span))
        val a2 = AnnotatedString("hello", listOf(span))
        val diffTextSameAnn = AnnotatedString("world", listOf(span))
        val sameTextDiffAnn = AnnotatedString("hello")

        assertThat(a1[0]).isEqualTo('h')
        assertThat(a1[4]).isEqualTo('o')
        assertThat(a1).isEqualTo(a2)
        assertThat(a1.hashCode()).isEqualTo(a2.hashCode())
        assertThat(a1).isNotEqualTo("hello")
        assertThat(a1).isNotEqualTo(diffTextSameAnn)
        assertThat(a1).isNotEqualTo(sameTextDiffAnn)
        assertThat(sameTextDiffAnn.hashCode()).isEqualTo(31 * "hello".hashCode())
        assertThat(a1.hasEqualAnnotations(diffTextSameAnn)).isTrue()
        assertThat(a1.hasEqualAnnotations(sameTextDiffAnn)).isFalse()
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun subSequence_andSubstringWithoutParagraphStyles_andRangeIntervals() {
        assumeTrue(PackedTextStylesLatch.isEnabled)
        val span = Range(SpanStyle(color = Color.Red), 1, 4)
        val par = Range(par1, 1, 4)
        val tts = Range(VerbatimTtsAnnotation("tts"), 1, 4)
        @Suppress("DEPRECATION") val url = Range(UrlAnnotation("https://a.com"), 1, 4)
        val linkUrl = Range(LinkAnnotation.Url("https://b.com"), 1, 4)
        val linkClick = Range(LinkAnnotation.Clickable("click") {}, 1, 4)
        val bullet = Range(Bullet.Default, 1, 4)
        val tagStr = Range(StringAnnotation("val"), 1, 4, "myTag")

        val pointAt0 = Range(SpanStyle(color = Color.Blue), 0, 0)
        val pointAt1 = Range(SpanStyle(color = Color.Green), 1, 1)
        val pointAt3 = Range(SpanStyle(color = Color.Yellow), 3, 3)
        val pointAt5 = Range(SpanStyle(color = Color.Cyan), 5, 5)

        val full =
            AnnotatedString(
                text = "01234",
                annotations =
                    listOf(
                        span,
                        par,
                        tts,
                        url,
                        linkUrl,
                        linkClick,
                        bullet,
                        tagStr,
                        pointAt0,
                        pointAt1,
                        pointAt3,
                        pointAt5,
                    ),
            )

        assertThat(full.subSequence(TextRange(0, 5))).isSameInstanceAs(full)
        val clipped = full.subSequence(TextRange(2, 3))
        assertThat(clipped.text).isEqualTo("2")
        assertThat(clipped.annotations)
            .containsExactly(
                Range(span.item, 0, 1),
                Range(par.item, 0, 1),
                Range(tts.item, 0, 1),
                Range(url.item, 0, 1),
                Range(linkUrl.item, 0, 1),
                Range(linkClick.item, 0, 1),
                Range(bullet.item, 0, 1),
                Range(tagStr.item, 0, 1, "myTag"),
            )

        // Slice [1, 3) includes pointAt1 ([1, 1) -> [0, 0)) and excludes pointAt0 & pointAt3.
        assertThat(full.subSequence(1, 3).spanStyles)
            .containsExactly(Range(span.item, 0, 2), Range(pointAt1.item, 0, 0))
        assertThat(full.subSequence(0, 0).spanStyles).containsExactly(pointAt0)
        assertThat(full.subSequence(3, 3).spanStyles)
            .containsExactly(Range(span.item, 0, 0), Range(pointAt3.item, 0, 0))
        assertThat(full.subSequence(5, 5).spanStyles).containsExactly(Range(pointAt5.item, 0, 0))

        // Prefix slice [0, 4) reuses existing Range instances when start == 0 and end >= range.end.
        val prefix = full.subSequence(0, 4)
        assertThat(prefix.spanStyles[0]).isSameInstanceAs(span)
        assertThat(prefix.spanStyles[1]).isSameInstanceAs(pointAt0)
        assertThat(prefix.spanStyles[2]).isSameInstanceAs(pointAt1)
        assertThat(prefix.spanStyles[3]).isSameInstanceAs(pointAt3)

        // `substringWithoutParagraphStyles` via `mapEachParagraphStyle` (all 4 branches).
        val span1 = Range(SpanStyle(color = Color.Red), 0, 2)
        val span2 = Range(SpanStyle(color = Color.Blue), 2, 4)
        val parA = Range(par1, 0, 2)
        val parB = Range(par2, 2, 4)
        val tagA = Range(StringAnnotation("v"), 0, 3, "t")

        val fullNoOther =
            AnnotatedString(
                "abcd",
                spanStyles = listOf(span1),
                paragraphStyles = listOf(Range(par1, 0, 4)),
            )
        val mappedFullNoOther =
            fullNoOther.mapEachParagraphStyle(ParagraphStyle()) { sub, _ -> sub }
        assertThat(mappedFullNoOther.single().annotations)
            .isSameInstanceAs(fullNoOther.spanStylesOrNull)

        val splitNoOther =
            AnnotatedString(
                "abcd",
                spanStyles = listOf(span1, span2),
                paragraphStyles = listOf(parA, parB),
            )
        assertThat(splitNoOther.mapEachParagraphStyle(ParagraphStyle()) { sub, _ -> sub })
            .containsExactly(
                AnnotatedString("ab", listOf(Range(span1.item, 0, 2))),
                AnnotatedString("cd", listOf(Range(span2.item, 0, 2))),
            )
            .inOrder()

        val splitWithOther =
            AnnotatedString("abcd", annotations = listOf(span1, span2, parA, parB, tagA))
        assertThat(splitWithOther.mapEachParagraphStyle(ParagraphStyle()) { sub, _ -> sub })
            .containsExactly(
                AnnotatedString(
                    "ab",
                    annotations = listOf(Range(span1.item, 0, 2), Range(tagA.item, 0, 2, "t")),
                ),
                AnnotatedString(
                    "cd",
                    annotations = listOf(Range(span2.item, 0, 2), Range(tagA.item, 0, 1, "t")),
                ),
            )
            .inOrder()

        val zeroPar = AnnotatedString("ab", annotations = listOf(span1, Range(par1, 0, 0), tagA))
        val mappedZeroPar = zeroPar.mapEachParagraphStyle(ParagraphStyle()) { sub, _ -> sub }
        assertThat(mappedZeroPar[0].text).isEmpty()
        assertThat(mappedZeroPar[0].annotations).isNull()

        // Half-open interval helpers `contains` and `intersect`.
        for ((bS, bE, tS, tE, expected) in
            listOf(
                listOf(1, 5, 1, 5, 1),
                listOf(1, 5, 2, 4, 1),
                listOf(1, 5, 1, 1, 1),
                listOf(1, 5, 3, 3, 1),
                listOf(1, 5, 5, 5, 0),
                listOf(1, 5, 0, 3, 0),
                listOf(1, 5, 3, 6, 0),
                listOf(3, 3, 3, 3, 1),
                listOf(3, 3, 3, 4, 0),
                listOf(3, 3, 2, 2, 0),
            )) {
            assertThat(contains(bS, bE, tS, tE)).isEqualTo(expected == 1)
        }
        for ((lS, lE, rS, rE, expected) in
            listOf(
                listOf(1, 4, 2, 5, 1),
                listOf(1, 4, 4, 6, 0),
                listOf(1, 4, 0, 1, 0),
                listOf(1, 4, 1, 1, 1),
                listOf(1, 1, 1, 4, 1),
                listOf(1, 4, 2, 2, 1),
                listOf(1, 4, 4, 4, 0),
                listOf(4, 4, 1, 4, 0),
                listOf(2, 2, 2, 2, 1),
                listOf(2, 2, 3, 3, 0),
            )) {
            assertThat(intersect(lS, lE, rS, rE)).isEqualTo(expected == 1)
        }
    }

    @Test
    fun plus_fastPaths_andOffsetShifting() {
        assumeTrue(PackedTextStylesLatch.isEnabled)
        val empty = AnnotatedString("")
        val plainLhs = AnnotatedString("Hello")
        val plainRhs = AnnotatedString("World")
        val styledLhs =
            AnnotatedString(
                "Hello",
                annotations = listOf(Range(SpanStyle(color = Color.Red), 1, 3)),
            )
        val styledRhs =
            AnnotatedString(
                "World",
                annotations = listOf(Range(StringAnnotation("v"), 0, 2, "tag")),
            )

        assertThat(styledLhs + empty).isSameInstanceAs(styledLhs)
        assertThat(plainLhs + empty).isSameInstanceAs(plainLhs)
        assertThat(empty + styledRhs).isSameInstanceAs(styledRhs)
        assertThat(empty + plainRhs).isSameInstanceAs(plainRhs)

        val plainCombined = plainLhs + plainRhs
        assertThat(plainCombined.text).isEqualTo("HelloWorld")
        assertThat(plainCombined.annotations).isNull()

        val lhsOnlyCombined = styledLhs + plainRhs
        assertThat(lhsOnlyCombined.annotations).isSameInstanceAs(styledLhs.annotations)

        val rhsOnlyCombined = plainLhs + styledRhs
        assertThat(rhsOnlyCombined.annotations)
            .containsExactly(Range(StringAnnotation("v"), 5, 7, "tag"))

        val emptyWithPoint =
            AnnotatedString("", annotations = listOf(Range(SpanStyle(color = Color.Blue), 0, 0)))
        assertThat((plainLhs + emptyWithPoint).spanStyles)
            .containsExactly(Range(SpanStyle(color = Color.Blue), 5, 5))
        assertThat((emptyWithPoint + plainRhs).spanStyles)
            .containsExactly(Range(SpanStyle(color = Color.Blue), 0, 0))
    }

    @OptIn(ExperimentalTextApi::class)
    @Suppress("DEPRECATION")
    @Test
    fun queryMethods_fastReturnWhenNoOtherAnnotations_andEmptyTagIsolation() {
        val plain = AnnotatedString("hello")
        val stylesOnly =
            AnnotatedString(
                text = "hello",
                spanStyles = listOf(Range(SpanStyle(color = Color.Red), 0, 5)),
                paragraphStyles = listOf(Range(par1, 0, 5)),
            )
        val bulletOnly = AnnotatedString("hello", annotations = listOf(Range(Bullet.Default, 0, 5)))

        for (str in listOf(plain, stylesOnly, bulletOnly)) {
            assertThat(str.getStringAnnotations(0, 5)).isEmpty()
            assertThat(str.getStringAnnotations("", 0, 5)).isEmpty()
            assertThat(str.getStringAnnotations("tag", 0, 5)).isEmpty()
            assertThat(str.hasStringAnnotations("", 0, 5)).isFalse()
            assertThat(str.hasStringAnnotations("tag", 0, 5)).isFalse()
            assertThat(str.getTtsAnnotations(0, 5)).isEmpty()
            assertThat(str.getUrlAnnotations(0, 5)).isEmpty()
            assertThat(str.getLinkAnnotations(0, 5)).isEmpty()
            assertThat(str.hasLinkAnnotations(0, 5)).isFalse()
        }

        val linkUrl = LinkAnnotation.Url("https://compose.dev")
        val linkClick = LinkAnnotation.Clickable("btn") {}
        val tts = VerbatimTtsAnnotation("tts")
        val url = UrlAnnotation("https://old.dev")
        val mixed = buildAnnotatedString {
            append("0123456789")
            addStyle(SpanStyle(color = Color.Red), 0, 10)
            addStyle(par1, 0, 10)
            addBullet(Bullet.Default, 0, 10)
            addTtsAnnotation(tts, 1, 3)
            addUrlAnnotation(url, 2, 4)
            addLink(linkUrl, 3, 5)
            addLink(linkClick, 6, 8)
            addStringAnnotation(tag = "", annotation = "emptyTagValue", start = 4, end = 7)
            addStringAnnotation(tag = "custom", annotation = "customValue", start = 4, end = 7)
        }

        assertThat(mixed.hasStringAnnotations("", 0, 3)).isFalse()
        assertThat(mixed.hasStringAnnotations("", 4, 5)).isTrue()
        assertThat(mixed.getStringAnnotations("", 0, 10))
            .containsExactly(Range("emptyTagValue", 4, 7, ""))
        assertThat(mixed.getStringAnnotations(0, 10))
            .containsExactly(Range("emptyTagValue", 4, 7, ""), Range("customValue", 4, 7, "custom"))

        assertThat(mixed.hasLinkAnnotations(0, 3)).isFalse()
        assertThat(mixed.hasLinkAnnotations(3, 4)).isTrue()
        assertThat(mixed.hasLinkAnnotations(5, 6)).isFalse()
        assertThat(mixed.hasLinkAnnotations(7, 8)).isTrue()
        assertThat(mixed.hasLinkAnnotations(8, 10)).isFalse()
        assertThat(mixed.getLinkAnnotations(0, 10))
            .containsExactly(Range(linkUrl, 3, 5, ""), Range(linkClick, 6, 8, ""))

        // Reversed range [start > end] returns empty / false across all query methods.
        assertThat(mixed.getStringAnnotations(8, 2)).isEmpty()
        assertThat(mixed.getStringAnnotations("", 8, 2)).isEmpty()
        assertThat(mixed.hasStringAnnotations("", 8, 2)).isFalse()
        assertThat(mixed.getTtsAnnotations(8, 2)).isEmpty()
        assertThat(mixed.getUrlAnnotations(8, 2)).isEmpty()
        assertThat(mixed.getLinkAnnotations(8, 2)).isEmpty()
        assertThat(mixed.hasLinkAnnotations(8, 2)).isFalse()
    }

    @Test
    fun mapAnnotations_andFlatMapAnnotations_copyOnWriteAndExpansion() {
        assumeTrue(PackedTextStylesLatch.isEnabled)
        val plain = AnnotatedString("hello")
        var invoked = false
        assertThat(
                plain.mapAnnotations {
                    invoked = true
                    it
                }
            )
            .isSameInstanceAs(plain)
        assertThat(
                plain.flatMapAnnotations {
                    invoked = true
                    listOf(it)
                }
            )
            .isSameInstanceAs(plain)
        assertThat(invoked).isFalse()

        val r0 = Range(SpanStyle(color = Color.Red), 0, 2)
        val r1 = Range(SpanStyle(color = Color.Green), 2, 4)
        val r2 = Range(StringAnnotation("v"), 1, 5, "tag")
        val annotated = AnnotatedString("hello", annotations = listOf(r0, r1, r2))

        assertThat(annotated.mapAnnotations { it }).isSameInstanceAs(annotated)

        val replacedR1 = Range(SpanStyle(color = Color.Blue), 2, 5)
        val mappedMiddle = annotated.mapAnnotations { if (it === r1) replacedR1 else it }
        assertThat(mappedMiddle.annotations).containsExactly(r0, replacedR1, r2).inOrder()
        assertThat(mappedMiddle.annotations!![0]).isSameInstanceAs(r0)
        assertThat(mappedMiddle.annotations!![2]).isSameInstanceAs(r2)

        val replacedR0 = Range(SpanStyle(color = Color.Yellow), 0, 1)
        assertThat(annotated.mapAnnotations { if (it === r0) replacedR0 else it }.annotations)
            .containsExactly(replacedR0, r1, r2)
            .inOrder()

        assertThat(annotated.flatMapAnnotations { emptyList() }.annotations).isNull()
        val expanded = annotated.flatMapAnnotations { range ->
            if (range === r0) listOf(Range(r0.item, 0, 1), Range(r0.item, 1, 2)) else listOf(range)
        }
        assertThat(expanded.annotations)
            .containsExactly(Range(r0.item, 0, 1), Range(r0.item, 1, 2), r1, r2)
            .inOrder()
    }

    @Test
    fun normalizedParagraphStyles_singleParagraphFastPaths_andZeroLengthFallthroughs() {
        val defaultStyle = ParagraphStyle(lineHeight = 20.sp)
        val customStyle = ParagraphStyle(textAlign = TextAlign.Center)
        val merged = defaultStyle.merge(customStyle)

        assertThat(
                AnnotatedString("Hello World", paragraphStyles = listOf(Range(customStyle, 0, 11)))
                    .normalizedParagraphStyles(defaultStyle)
            )
            .containsExactly(Range(merged, 0, 11))

        assertThat(
                AnnotatedString("Hello World", paragraphStyles = listOf(Range(customStyle, 2, 8)))
                    .normalizedParagraphStyles(defaultStyle)
            )
            .containsExactly(
                Range(defaultStyle, 0, 2),
                Range(merged, 2, 8),
                Range(defaultStyle, 8, 11),
            )
            .inOrder()

        assertThat(
                AnnotatedString("Hello World", paragraphStyles = listOf(Range(customStyle, 6, 11)))
                    .normalizedParagraphStyles(defaultStyle)
            )
            .containsExactly(Range(defaultStyle, 0, 6), Range(merged, 6, 11))
            .inOrder()

        assertThat(
                AnnotatedString("abcd", paragraphStyles = listOf(Range(customStyle, 2, 2)))
                    .normalizedParagraphStyles(defaultStyle)
            )
            .containsExactly(
                Range(defaultStyle, 0, 2),
                Range(merged, 2, 2),
                Range(defaultStyle, 2, 4),
            )
            .inOrder()

        assertThat(
                AnnotatedString("abcd", paragraphStyles = listOf(Range(customStyle, 4, 4)))
                    .normalizedParagraphStyles(defaultStyle)
            )
            .containsExactly(Range(defaultStyle, 0, 4), Range(merged, 4, 4))
            .inOrder()

        assertThat(
                AnnotatedString("", paragraphStyles = listOf(Range(customStyle, 0, 0)))
                    .normalizedParagraphStyles(defaultStyle)
            )
            .containsExactly(Range(merged, 0, 0))

        assertThat(
                AnnotatedString(
                        text = "0123456789",
                        paragraphStyles = listOf(Range(par2, 6, 8), Range(par1, 1, 3)),
                    )
                    .normalizedParagraphStyles(defaultStyle)
            )
            .containsExactly(
                Range(defaultStyle, 0, 1),
                Range(defaultStyle.merge(par1), 1, 3),
                Range(defaultStyle, 3, 6),
                Range(defaultStyle.merge(par2), 6, 8),
                Range(defaultStyle, 8, 10),
            )
            .inOrder()
    }

    @Test
    fun zeroLengthPointRangeAtStringBoundary_isPreserved() {
        val text = "abc"
        val spanStyle = SpanStyle(color = Color.Red)
        val annotatedString =
            AnnotatedString(
                text = text,
                spanStyles = listOf(Range(spanStyle, 3, 3)),
            )
        assertThat(annotatedString.spanStyles).containsExactly(Range(spanStyle, 3, 3))

        val withoutParagraphs = annotatedString.substringWithoutParagraphStyles(0, 3)
        assertThat(withoutParagraphs.spanStyles).containsExactly(Range(spanStyle, 3, 3))
    }

    @Test
    fun publicConstructor_defensivelyCopiesAnnotations() {
        val mutableAnnotations = mutableListOf(Range(SpanStyle(color = Color.Red), 0, 2))
        val annotatedString = AnnotatedString("hello", mutableAnnotations)
        mutableAnnotations.add(Range(SpanStyle(color = Color.Blue), 2, 4))

        assertThat(annotatedString.spanStyles).hasSize(1)
        assertThat(annotatedString.spanStyles.first().item.color).isEqualTo(Color.Red)
    }

    @Test
    fun internalConstructor_andSubstringWithoutParagraphStyles_normalizeEmptyAnnotations() {
        val expected = AnnotatedString("hello")
        val candidates =
            listOf(
                AnnotatedString.Builder("hello").toAnnotatedString(),
                buildAnnotatedString { append("hello") },
                AnnotatedString("hello", annotations = null, trusted = true),
                AnnotatedString("hello", annotations = emptyList(), trusted = true),
                AnnotatedString("hello", annotations = ArrayList(), trusted = true),
                AnnotatedString("hello", annotations = null, trusted = false),
                AnnotatedString("hello", annotations = emptyList(), trusted = false),
                AnnotatedString("hello", annotations = ArrayList(), trusted = false),
                AnnotatedString(
                        "hello",
                        annotations = listOf(Range(SpanStyle(color = Color.Red), 0, 5)),
                    )
                    .flatMapAnnotations { emptyList() },
                AnnotatedString("hello").substringWithoutParagraphStyles(0, 5),
                AnnotatedString("hello world").substringWithoutParagraphStyles(0, 5),
                AnnotatedString("hello", paragraphStyles = listOf(Range(par1, 0, 5)))
                    .substringWithoutParagraphStyles(0, 5),
                AnnotatedString(
                        "hello world",
                        spanStyles = listOf(Range(SpanStyle(color = Color.Red), 6, 11)),
                    )
                    .substringWithoutParagraphStyles(0, 5),
            )

        for (candidate in candidates) {
            assertThat(candidate.annotations).isNull()
            assertThat(candidate.spanStylesOrNull).isNull()
            assertThat(candidate.paragraphStylesOrNull).isNull()
            assertThat(candidate).isEqualTo(expected)
            assertThat(expected).isEqualTo(candidate)
            assertThat(candidate.hashCode()).isEqualTo(expected.hashCode())
            assertThat(candidate.hasEqualAnnotations(expected)).isTrue()
        }

        val emptySlice = AnnotatedString("hello").substringWithoutParagraphStyles(2, 2)
        val expectedEmpty = AnnotatedString("")
        assertThat(emptySlice.annotations).isNull()
        assertThat(emptySlice).isEqualTo(expectedEmpty)
        assertThat(emptySlice.hashCode()).isEqualTo(expectedEmpty.hashCode())
    }

    @Test
    fun internalConstructor_untrusted_defensivelyCopiesAndValidatesUnsortedParagraphs() {
        val emptyMutable = mutableListOf<Range<out AnnotatedString.Annotation>>()
        val fromEmptyUntrusted =
            AnnotatedString("hello", annotations = emptyMutable, trusted = false)
        emptyMutable.add(Range(SpanStyle(color = Color.Red), 0, 5))
        assertThat(fromEmptyUntrusted.annotations).isNull()
        assertThat(fromEmptyUntrusted).isEqualTo(AnnotatedString("hello"))
        assertThat(fromEmptyUntrusted.hashCode()).isEqualTo(AnnotatedString("hello").hashCode())

        val spanFirst = Range(SpanStyle(color = Color.Red), 4, 8)
        val spanSecond = Range(SpanStyle(color = Color.Blue), 0, 6)
        val innerPar = Range(par2, 1, 3)
        val outerPar = Range(par1, 0, 4)
        val trailingPar = Range(par3, 6, 8)
        val mutableInput =
            mutableListOf<Range<out AnnotatedString.Annotation>>(
                trailingPar,
                spanFirst,
                innerPar,
                outerPar,
                spanSecond,
            )
        val untrusted = AnnotatedString("0123456789", annotations = mutableInput, trusted = false)
        mutableInput.clear()

        assertThat(untrusted.spanStyles).containsExactly(spanFirst, spanSecond).inOrder()
        assertThat(untrusted.paragraphStyles)
            .containsExactly(trailingPar, innerPar, outerPar)
            .inOrder()
        val defaultPar = ParagraphStyle()
        assertThat(untrusted.normalizedParagraphStyles(defaultPar))
            .containsExactly(
                Range(outerPar.item, 0, 1),
                Range(outerPar.item.merge(innerPar.item), 1, 3),
                Range(outerPar.item, 3, 4),
                Range(defaultPar, 4, 6),
                Range(trailingPar.item, 6, 8),
                Range(defaultPar, 8, 10),
            )
            .inOrder()

        assertFailsWith<IllegalArgumentException> {
            AnnotatedString(
                text = "0123456789",
                annotations = listOf(Range(par2, 2, 6), Range(par1, 0, 4)),
                trusted = false,
            )
        }
    }
}
