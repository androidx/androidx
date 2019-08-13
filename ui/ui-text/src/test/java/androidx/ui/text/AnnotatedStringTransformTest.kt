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
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.Locale

@RunWith(JUnit4::class)
class AnnotatedStringTransformTest {

    private val TextStyle1 = TextStyle(fontSize = 8.sp)
    private val TextStyle2 = TextStyle(fontSize = 16.sp)
    private val TextStyle3 = TextStyle(fontSize = 24.sp)

    private val ParaStyle1 = ParagraphStyle(lineHeight = 1.0f)
    private val ParaStyle2 = ParagraphStyle(lineHeight = 2.0f)

    /**
     * Helper function for creating AnnotatedString.Item with TextRange.
     */
    private fun <T> makeItem(style: T, range: TextRange) =
        AnnotatedString.Item(style, range.start, range.end)

    /**
     * Make AnnotatedString.Item with original string with using "(" and ")" characters.
     *
     * For example.
     *   val text = "aaa bbb ccc"
     *
     *   AnnotatedString.Item(STYLE, 4, 8)
     *
     * can be written as
     *
     *   val text = "aaa bbb ccc"
     *
     *   makeItem(STYLE, "aaa (bbb )ccc")
     */
    private fun <T> makeItem(style: T, rangeStr: String): AnnotatedString.Item<T> {
        val start = rangeStr.indexOf('(')
        val end = rangeStr.indexOf(')')

        if (start >= end) throw RuntimeException("Invalid range str: $rangeStr")
        return makeItem(style, TextRange(start, end - 1 /* subtract start marker */))
    }

    @Test
    fun `English uppercase`() {
        val input = AnnotatedString("aaa bbb ccc",
            listOf(
                makeItem(TextStyle1, "(aaa bbb ccc)"),
                makeItem(TextStyle2, "(aaa )bbb ccc"),
                makeItem(TextStyle3, "aaa (bbb ccc)")
            ), listOf(
                makeItem(ParaStyle1, "(aaa bbb )ccc"),
                makeItem(ParaStyle2, "aaa bbb (ccc)")
            )
        )

        val uppercase = input.toUpperCase()

        assertEquals(input.text.toUpperCase(), uppercase.text)
        assertEquals(input.textStyles, uppercase.textStyles)
        assertEquals(input.paragraphStyles, uppercase.paragraphStyles)
    }

    @Test
    fun `English lowercase`() {
        val input = AnnotatedString("aaa bbb ccc",
            listOf(
                makeItem(TextStyle1, "(aaa bbb ccc)"),
                makeItem(TextStyle2, "(aaa )bbb ccc"),
                makeItem(TextStyle3, "aaa (bbb ccc)")
            ), listOf(
                makeItem(ParaStyle1, "(aaa bbb )ccc"),
                makeItem(ParaStyle2, "aaa bbb (ccc)")
            )
        )

        val lowercase = input.toLowerCase()

        assertEquals(input.text.toLowerCase(), lowercase.text)
        assertEquals(input.textStyles, lowercase.textStyles)
        assertEquals(input.paragraphStyles, lowercase.paragraphStyles)
    }

    @Test
    fun `English capitalize`() {
        val input = AnnotatedString("aaa bbb ccc",
            listOf(
                makeItem(TextStyle1, "(aaa bbb ccc)"),
                makeItem(TextStyle2, "(aaa )bbb ccc"),
                makeItem(TextStyle3, "aaa (bbb ccc)")
            ), listOf(
                makeItem(ParaStyle1, "(aaa bbb )ccc"),
                makeItem(ParaStyle2, "aaa bbb (ccc)")
            )
        )

        val capitalized = input.capitalize()

        assertEquals(input.text.capitalize(), capitalized.text)
        assertEquals(input.textStyles, capitalized.textStyles)
        assertEquals(input.paragraphStyles, capitalized.paragraphStyles)
    }

    @Test
    fun `English decapitalize`() {
        val input = AnnotatedString("aaa bbb ccc",
            listOf(
                makeItem(TextStyle1, "(aaa bbb ccc)"),
                makeItem(TextStyle2, "(aaa )bbb ccc"),
                makeItem(TextStyle3, "aaa (bbb ccc)")
            ), listOf(
                makeItem(ParaStyle1, "(aaa bbb )ccc"),
                makeItem(ParaStyle2, "aaa bbb (ccc)")
            )
        )

        val decapitalized = input.decapitalize()

        assertEquals(input.text.decapitalize(), decapitalized.text)
        assertEquals(input.textStyles, decapitalized.textStyles)
        assertEquals(input.paragraphStyles, decapitalized.paragraphStyles)
    }

    @Test
    fun `locale dependent uppercase or lowercase (Turkish uppercase)`() {
        val input = AnnotatedString("hhh iii jjj",
            listOf(
                makeItem(TextStyle1, "(hhh iii jjj)"),
                makeItem(TextStyle2, "(hhh )iii jjj"),
                makeItem(TextStyle3, "hhh (iii jjj)")
            ), listOf(
                makeItem(ParaStyle1, "(hhh iii )jjj"),
                makeItem(ParaStyle2, "hhh iii (jjj)")
            )
        )

        val uppercase = input.toUpperCase(Locale.forLanguageTag("tr"))

        assertEquals(input.text.toUpperCase(Locale.forLanguageTag("tr")), uppercase.text)

        val upperI = "i".toUpperCase(Locale.forLanguageTag("tr"))

        assertEquals(
            listOf(
                makeItem(TextStyle1, "(HHH $upperI$upperI$upperI JJJ)"),
                makeItem(TextStyle2, "(HHH )$upperI$upperI$upperI JJJ"),
                makeItem(TextStyle3, "HHH ($upperI$upperI$upperI JJJ)")
            ),
            uppercase.textStyles)
        assertEquals(
            listOf(
                makeItem(ParaStyle1, "(HHH $upperI$upperI$upperI )JJJ"),
                makeItem(ParaStyle2, "HHH $upperI$upperI$upperI (JJJ)")
            ),
            uppercase.paragraphStyles)
    }

    @Test
    fun `not 1-by-1 mapping uppercase or lowercase (Lithuanian lowercase)`() {
        val input = AnnotatedString("HHH ÌÌÌ YYY",
            listOf(
                makeItem(TextStyle1, "(HHH ÌÌÌ YYY)"),
                makeItem(TextStyle2, "(HHH )ÌÌÌ YYY"),
                makeItem(TextStyle3, "HHH (ÌÌÌ YYY)")
            ), listOf(
                makeItem(ParaStyle1, "(HHH ÌÌÌ )YYY"),
                makeItem(ParaStyle2, "HHH ÌÌÌ (YYY)")
            )
        )

        val lowercase = input.toLowerCase(Locale.forLanguageTag("lt"))

        assertEquals(input.text.toLowerCase(Locale.forLanguageTag("lt")), lowercase.text)

        // Usually generate U+0069 U+0307 U+0300
        val lowerIDot = "Ì".toLowerCase(Locale.forLanguageTag("lt"))
        assertEquals(
            listOf(
                makeItem(TextStyle1, "(hhh $lowerIDot$lowerIDot$lowerIDot yyy)"),
                makeItem(TextStyle2, "(hhh )$lowerIDot$lowerIDot$lowerIDot yyy"),
                makeItem(TextStyle3, "hhh ($lowerIDot$lowerIDot$lowerIDot yyy)")
            ),
            lowercase.textStyles)
        assertEquals(
            listOf(
                makeItem(ParaStyle1, "(hhh $lowerIDot$lowerIDot$lowerIDot )yyy"),
                makeItem(ParaStyle2, "hhh $lowerIDot$lowerIDot$lowerIDot (yyy)")
            ),
            lowercase.paragraphStyles)
    }

    @Test
    fun `nothing happens for CJK uppercase or lowercase (Japanese uppercase)`() {
        val input = AnnotatedString("あああ いいい ううう",
            listOf(
                makeItem(TextStyle1, "(あああ いいい ううう)"),
                makeItem(TextStyle2, "(あああ )いいい ううう"),
                makeItem(TextStyle3, "あああ (いいい ううう)")
            ), listOf(
                makeItem(ParaStyle1, "(あああ いいい )ううう"),
                makeItem(ParaStyle2, "あああ いいい (ううう)")
            )
        )

        val uppercase = input.toUpperCase()

        // No upper case concept in Japanese, so should be the same
        assertEquals(input.text, uppercase.text)
        assertEquals(input.textStyles, uppercase.textStyles)
        assertEquals(input.paragraphStyles, uppercase.paragraphStyles)
    }
}