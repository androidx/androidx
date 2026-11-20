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

import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AnnotatedStringTransformTest {

    private val spanStyle1 = SpanStyle(fontSize = 8.sp)
    private val spanStyle2 = SpanStyle(fontSize = 16.sp)
    private val spanStyle3 = SpanStyle(fontSize = 24.sp)

    private val paraStyle1 = ParagraphStyle(lineHeight = 10.sp)
    private val paraStyle2 = ParagraphStyle(lineHeight = 20.sp)

    /** Helper function for creating AnnotatedString.Item with TextRange. */
    private fun <T> makeRange(style: T, range: TextRange) =
        AnnotatedString.Range(style, range.min, range.max)

    /**
     * Make AnnotatedString.Item with original string with using "(" and ")" characters.
     *
     * For example. val text = "aaa bbb ccc"
     *
     * AnnotatedString.Item(STYLE, 4, 8)
     *
     * can be written as
     *
     * val text = "aaa bbb ccc"
     *
     * makeRange(STYLE, "aaa (bbb )ccc")
     */
    private fun <T> makeRange(style: T, rangeStr: String): AnnotatedString.Range<T> {
        val start = rangeStr.indexOf('(')
        val end = rangeStr.indexOf(')')

        if (start >= end) throw RuntimeException("Invalid range str: $rangeStr")
        return makeRange(style, TextRange(start, end - 1 /* subtract start marker */))
    }

    @Test
    fun `English uppercase plaintext`() {
        val input = AnnotatedString("aaa bbb ccc")

        val uppercase = input.toUpperCase(androidx.compose.ui.text.intl.Locale(Locale.ROOT))

        assertThat(uppercase.text).isEqualTo(input.text.uppercase())
    }

    @Test
    fun `English uppercase sparse`() {
        val input = AnnotatedString("aaa bbb ccc", listOf(makeRange(spanStyle1, "aaa (bbb) ccc")))

        val uppercase = input.toUpperCase(androidx.compose.ui.text.intl.Locale(Locale.ROOT))

        assertThat(uppercase.text).isEqualTo(input.text.uppercase())
    }

    @Test
    fun `English uppercase`() {
        val input =
            AnnotatedString(
                "aaa bbb ccc",
                listOf(
                    makeRange(spanStyle1, "(aaa bbb ccc)"),
                    makeRange(spanStyle2, "(aaa )bbb ccc"),
                    makeRange(spanStyle3, "aaa (bbb ccc)"),
                ),
                listOf(
                    makeRange(paraStyle1, "(aaa bbb )ccc"),
                    makeRange(paraStyle2, "aaa bbb (ccc)"),
                ),
            )

        val uppercase = input.toUpperCase(androidx.compose.ui.text.intl.Locale(Locale.ROOT))

        assertThat(uppercase.text).isEqualTo(input.text.uppercase())
        assertThat(uppercase.spanStyles).isEqualTo(input.spanStyles)
        assertThat(uppercase.paragraphStyles).isEqualTo(input.paragraphStyles)
    }

    @Test
    fun `English lowercase`() {
        val input =
            AnnotatedString(
                "aaa bbb ccc",
                listOf(
                    makeRange(spanStyle1, "(aaa bbb ccc)"),
                    makeRange(spanStyle2, "(aaa )bbb ccc"),
                    makeRange(spanStyle3, "aaa (bbb ccc)"),
                ),
                listOf(
                    makeRange(paraStyle1, "(aaa bbb )ccc"),
                    makeRange(paraStyle2, "aaa bbb (ccc)"),
                ),
            )

        val lowercase = input.toLowerCase(androidx.compose.ui.text.intl.Locale(Locale.ROOT))

        assertThat(lowercase.text).isEqualTo(input.text.lowercase())
        assertThat(lowercase.spanStyles).isEqualTo(input.spanStyles)
        assertThat(lowercase.paragraphStyles).isEqualTo(input.paragraphStyles)
    }

    @Test
    fun `English capitalize`() {
        val input =
            AnnotatedString(
                "aaa bbb ccc",
                listOf(
                    makeRange(spanStyle1, "(aaa bbb ccc)"),
                    makeRange(spanStyle2, "(aaa )bbb ccc"),
                    makeRange(spanStyle3, "aaa (bbb ccc)"),
                ),
                listOf(
                    makeRange(paraStyle1, "(aaa bbb )ccc"),
                    makeRange(paraStyle2, "aaa bbb (ccc)"),
                ),
            )

        val capitalized = input.capitalize(androidx.compose.ui.text.intl.Locale(Locale.ROOT))

        assertThat(capitalized.text)
            .isEqualTo(
                input.text.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
            )
        assertThat(capitalized.spanStyles).isEqualTo(input.spanStyles)
        assertThat(capitalized.paragraphStyles).isEqualTo(input.paragraphStyles)
    }

    @Test
    fun `English decapitalize`() {
        val input =
            AnnotatedString(
                "aaa bbb ccc",
                listOf(
                    makeRange(spanStyle1, "(aaa bbb ccc)"),
                    makeRange(spanStyle2, "(aaa )bbb ccc"),
                    makeRange(spanStyle3, "aaa (bbb ccc)"),
                ),
                listOf(
                    makeRange(paraStyle1, "(aaa bbb )ccc"),
                    makeRange(paraStyle2, "aaa bbb (ccc)"),
                ),
            )

        val decapitalized = input.decapitalize(androidx.compose.ui.text.intl.Locale(Locale.ROOT))

        assertThat(decapitalized.text)
            .isEqualTo(input.text.replaceFirstChar { it.lowercase(Locale.getDefault()) })
        assertThat(decapitalized.spanStyles).isEqualTo(input.spanStyles)
        assertThat(decapitalized.paragraphStyles).isEqualTo(input.paragraphStyles)
    }

    @Test
    fun `locale dependent capitalize (Turkish)`() {
        val input =
            AnnotatedString(
                "iii hhh jjj",
                listOf(
                    makeRange(spanStyle1, "(iii hhh jjj)"),
                    makeRange(spanStyle2, "(iii )hhh jjj"),
                    makeRange(spanStyle3, "iii (hhh jjj)"),
                ),
                listOf(
                    makeRange(paraStyle1, "(iii hhh )jjj"),
                    makeRange(paraStyle2, "iii hhh (jjj)"),
                ),
            )
        val capitalized = input.capitalize(androidx.compose.ui.text.intl.Locale("tr"))

        assertThat(capitalized.text)
            .isEqualTo(
                input.text.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("tr"))
                    else it.toString()
                }
            )
        assertThat(capitalized.spanStyles).isEqualTo(input.spanStyles)
        assertThat(capitalized.paragraphStyles).isEqualTo(input.paragraphStyles)
    }

    @Test
    fun `locale dependent decapitalize (Turkish)`() {
        val input =
            AnnotatedString(
                "III HHH JJJ",
                listOf(
                    makeRange(spanStyle1, "(III HHH JJJ)"),
                    makeRange(spanStyle2, "(III )HHH JJJ"),
                    makeRange(spanStyle3, "III (HHH JJJ)"),
                ),
                listOf(
                    makeRange(paraStyle1, "(III HHH )JJJ"),
                    makeRange(paraStyle2, "III HHH (JJJ)"),
                ),
            )

        val decapitalized = input.decapitalize(androidx.compose.ui.text.intl.Locale("tr"))

        assertThat(decapitalized.text)
            .isEqualTo(input.text.replaceFirstChar { it.lowercase(Locale.forLanguageTag("tr")) })
        assertThat(decapitalized.spanStyles).isEqualTo(input.spanStyles)
        assertThat(decapitalized.paragraphStyles).isEqualTo(input.paragraphStyles)
    }

    @Test
    fun `locale dependent uppercase or lowercase (Turkish uppercase)`() {
        val input =
            AnnotatedString(
                "hhh iii jjj",
                listOf(
                    makeRange(spanStyle1, "(hhh iii jjj)"),
                    makeRange(spanStyle2, "(hhh )iii jjj"),
                    makeRange(spanStyle3, "hhh (iii jjj)"),
                ),
                listOf(
                    makeRange(paraStyle1, "(hhh iii )jjj"),
                    makeRange(paraStyle2, "hhh iii (jjj)"),
                ),
            )

        val uppercase = input.toUpperCase(androidx.compose.ui.text.intl.Locale("tr"))

        assertThat(uppercase.text).isEqualTo(input.text.uppercase(Locale.forLanguageTag("tr")))

        val upperI = "i".uppercase(Locale.forLanguageTag("tr"))

        assertThat(uppercase.spanStyles)
            .isEqualTo(
                listOf(
                    makeRange(spanStyle1, "(HHH $upperI$upperI$upperI JJJ)"),
                    makeRange(spanStyle2, "(HHH )$upperI$upperI$upperI JJJ"),
                    makeRange(spanStyle3, "HHH ($upperI$upperI$upperI JJJ)"),
                )
            )
        assertThat(uppercase.paragraphStyles)
            .isEqualTo(
                listOf(
                    makeRange(paraStyle1, "(HHH $upperI$upperI$upperI )JJJ"),
                    makeRange(paraStyle2, "HHH $upperI$upperI$upperI (JJJ)"),
                )
            )
    }

    @Test
    fun `not 1-by-1 mapping uppercase or lowercase (Lithuanian lowercase)`() {
        val input =
            AnnotatedString(
                "HHH ÌÌÌ YYY",
                listOf(
                    makeRange(spanStyle1, "(HHH ÌÌÌ YYY)"),
                    makeRange(spanStyle2, "(HHH )ÌÌÌ YYY"),
                    makeRange(spanStyle3, "HHH (ÌÌÌ YYY)"),
                ),
                listOf(
                    makeRange(paraStyle1, "(HHH ÌÌÌ )YYY"),
                    makeRange(paraStyle2, "HHH ÌÌÌ (YYY)"),
                ),
            )

        val lowercase = input.toLowerCase(androidx.compose.ui.text.intl.Locale("lt"))

        assertThat(lowercase.text).isEqualTo(input.text.lowercase(Locale.forLanguageTag("lt")))

        // Usually generate U+0069 U+0307 U+0300
        val lowerIDot = "Ì".lowercase(Locale.forLanguageTag("lt"))
        assertThat(lowercase.spanStyles)
            .isEqualTo(
                listOf(
                    makeRange(spanStyle1, "(hhh $lowerIDot$lowerIDot$lowerIDot yyy)"),
                    makeRange(spanStyle2, "(hhh )$lowerIDot$lowerIDot$lowerIDot yyy"),
                    makeRange(spanStyle3, "hhh ($lowerIDot$lowerIDot$lowerIDot yyy)"),
                )
            )
        assertThat(lowercase.paragraphStyles)
            .isEqualTo(
                listOf(
                    makeRange(paraStyle1, "(hhh $lowerIDot$lowerIDot$lowerIDot )yyy"),
                    makeRange(paraStyle2, "hhh $lowerIDot$lowerIDot$lowerIDot (yyy)"),
                )
            )
    }

    @Test
    fun `nothing happens for CJK uppercase or lowercase (Japanese uppercase)`() {
        val input =
            AnnotatedString(
                "あああ いいい ううう",
                listOf(
                    makeRange(spanStyle1, "(あああ いいい ううう)"),
                    makeRange(spanStyle2, "(あああ )いいい ううう"),
                    makeRange(spanStyle3, "あああ (いいい ううう)"),
                ),
                listOf(
                    makeRange(paraStyle1, "(あああ いいい )ううう"),
                    makeRange(paraStyle2, "あああ いいい (ううう)"),
                ),
            )

        val uppercase = input.toUpperCase(androidx.compose.ui.text.intl.Locale(Locale.ROOT))

        // No upper case concept in Japanese, so should be the same
        assertThat(uppercase.text).isEqualTo(input.text)
        assertThat(uppercase.spanStyles).isEqualTo(input.spanStyles)
        assertThat(uppercase.paragraphStyles).isEqualTo(input.paragraphStyles)
    }

    /**
     * Exact pre-CL 1 reference implementation of `AnnotatedString.transform` using `sortedSetOf(0,
     * text.length)` and `windowed(size = 2)` to verify 100% behavioral equivalence.
     */
    private fun referenceTransform(
        input: AnnotatedString,
        transform: (String, Int, Int) -> String,
    ): AnnotatedString {
        val transitions = sortedSetOf(0, input.text.length)
        input.annotations?.forEach { range ->
            transitions.add(range.start)
            transitions.add(range.end)
        }
        var resultStr = ""
        val offsetMap = mutableMapOf(0 to 0)
        transitions.windowed(size = 2) { (start, end) ->
            resultStr += transform(input.text, start, end)
            offsetMap.put(end, resultStr.length)
        }
        return AnnotatedString(
            text = resultStr,
            annotations =
                input.annotations?.map {
                    AnnotatedString.Range(
                        it.item,
                        offsetMap[it.start]!!,
                        offsetMap[it.end]!!,
                        it.tag,
                    )
                },
        )
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun `transform matches pre-optimization reference across empty, collapsed, overlapping, and multi-annotation inputs`() {
        val drawAnnotation = DrawAnnotation(drawBehind = {})
        val inputs =
            listOf(
                AnnotatedString(""),
                AnnotatedString(
                    text = "",
                    annotations =
                        listOf(
                            AnnotatedString.Range(spanStyle1, 0, 0),
                            AnnotatedString.Range(StringAnnotation("v"), 0, 0, "tag"),
                        ),
                ),
                AnnotatedString("single"),
                buildAnnotatedString {
                    append("abcdefghij")
                    addStyle(spanStyle1, 0, 0) // collapsed at start
                    addStyle(spanStyle2, 2, 7) // interior
                    addStyle(spanStyle3, 5, 5) // collapsed in middle
                    addStyle(spanStyle1, 5, 10) // overlapping + touching end
                    addStyle(spanStyle2, 10, 10) // collapsed at end
                    addStyle(paraStyle1, 0, 5)
                    addStyle(paraStyle2, 5, 10)
                    addStringAnnotation("tag1", "val1", 1, 9)
                    addTtsAnnotation(VerbatimTtsAnnotation("tts"), 3, 6)
                    @Suppress("DEPRECATION") addUrlAnnotation(UrlAnnotation("https://a.com"), 0, 4)
                    addLink(LinkAnnotation.Url("https://b.com"), 4, 8)
                    addDrawAnnotation(drawAnnotation, 2, 8)
                },
            )

        val transforms: List<(String, Int, Int) -> String> =
            listOf(
                // 1:1 length-preserving transform (offsetMap remains null)
                { str, s, e -> str.substring(s, e).uppercase(Locale.ROOT) },
                // Uniform 2x expansion (offsetMap allocated at first non-empty slice)
                { str, s, e -> str.substring(s, e).repeat(2) },
                // Prefix 1:1 then expansion then contraction (exercises lazy offsetMap backfill)
                { str, s, e ->
                    val sub = str.substring(s, e)
                    when {
                        e <= 3 -> sub.uppercase(Locale.ROOT)
                        s < 6 -> sub + sub + "!"
                        else -> if (sub.isNotEmpty()) sub.substring(0, 1) else ""
                    }
                },
                // Complete deletion of all slices (maps every cut to 0)
                { _, _, _ -> "" },
            )

        for (input in inputs) {
            for (fn in transforms) {
                var actualCalls = 0
                var expectedCalls = 0
                val actual = input.transform { str, s, e ->
                    actualCalls++
                    fn(str, s, e)
                }
                val expected =
                    referenceTransform(input) { str, s, e ->
                        expectedCalls++
                        fn(str, s, e)
                    }
                assertThat(actualCalls).isEqualTo(expectedCalls)
                assertThat(actual.text).isEqualTo(expected.text)
                assertThat(actual.annotations).isEqualTo(expected.annotations)
                assertThat(actual).isEqualTo(expected)
            }
        }
    }

    @Test
    fun `empty text with collapsed range never invokes transform callback and matches legacy`() {
        val emptyWithCollapsedRanges =
            AnnotatedString(
                text = "",
                annotations =
                    listOf(
                        AnnotatedString.Range(spanStyle1, 0, 0),
                        AnnotatedString.Range(StringAnnotation("value"), 0, 0, "customTag"),
                    ),
            )
        var packedCalls = 0
        var legacyCalls = 0
        val packedResult = emptyWithCollapsedRanges.transform { _, _, _ ->
            packedCalls++
            "unexpected"
        }
        val legacyResult = emptyWithCollapsedRanges.legacyTransform { _, _, _ ->
            legacyCalls++
            "unexpected"
        }
        assertThat(packedCalls).isEqualTo(0)
        assertThat(legacyCalls).isEqualTo(0)
        assertThat(packedResult).isEqualTo(legacyResult)
        assertThat(packedResult.getStringAnnotations("customTag", 0, 0))
            .containsExactly(AnnotatedString.Range("value", 0, 0, "customTag"))
    }
}
