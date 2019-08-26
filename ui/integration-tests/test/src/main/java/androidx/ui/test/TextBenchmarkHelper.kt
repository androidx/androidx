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

package androidx.ui.test

import androidx.ui.graphics.Color
import androidx.ui.text.AnnotatedString
import androidx.ui.text.TextStyle
import androidx.ui.text.font.FontWeight
import androidx.ui.text.style.BaselineShift
import androidx.ui.text.style.TextDecoration
import androidx.ui.text.style.TextGeometricTransform
import kotlin.random.Random

class RandomTextGenerator(
    private val alphabet: Alphabet = Alphabet.Latin,
    private val random: Random = Random(0)
) {
    // a set of predefined TextStyle's to add to styled text
    val textStyleList = arrayOf(
        TextStyle(color = Color.Blue),
        TextStyle(fontSizeScale = 2f),
        TextStyle(fontWeight = FontWeight.bold),
        TextStyle(letterSpacing = 0.2f),
        TextStyle(baselineShift = BaselineShift.Subscript),
        TextStyle(textGeometricTransform = TextGeometricTransform(0.5f, 0.5f)),
        TextStyle(background = Color.Cyan),
        TextStyle(decoration = TextDecoration.Underline)
    )

    /**
     * Creates a sequence of characters group of length [length].
     */
    private fun nextWord(length: Int): String = List(length) {
        alphabet.charRanges.random(random).random(random).toChar()
    }.joinToString(separator = "")

    /**
     * Create a sequence of character groups separated by the [Alphabet.space]. Each character group consists of
     * [wordLength] characters. The total length of the returned string is [length].
     */
    fun nextParagraph(
        length: Int,
        wordLength: Int = 9
    ): String {
        return if (length == 0) {
            ""
        } else {
            StringBuilder().apply {
                while (this.length < length) {
                    append(nextWord(wordLength))
                    append(alphabet.space)
                }
            }.substring(0, length)
        }
    }

    /**
     * Given a [text] mark each character group with a predefined TextStyle. The order of TextStyles is predefined,
     * and not randomized on purpose in order to get a consistent result in our benchmarks.
     */
    fun createStyles(
        text: String
    ): List<AnnotatedString.Item<TextStyle>> {
        var index = 0
        var styleCount = 0
        return text.split(alphabet.space).map {
            val start = index
            val end = start + it.length
            index += it.length + 1
            AnnotatedString.Item(
                start = start,
                end = end,
                style = textStyleList[styleCount++ % textStyleList.size]
            )
        }
    }
}

/**
 * Defines the character ranges to be picked randomly for a script.
 */
class Alphabet(
    val charRanges: List<IntRange>,
    val space: Char,
    val name: String
) {

    override fun toString(): String {
        return name
    }

    companion object {
        val Latin = Alphabet(
            charRanges = listOf(
                IntRange('a'.toInt(), 'z'.toInt()),
                IntRange('A'.toInt(), 'Z'.toInt())
            ),
            space = ' ',
            name = "Latin"
        )

        val Cjk = Alphabet(
            charRanges = listOf(
                IntRange(0x4E00, 0x62FF),
                IntRange(0x6300, 0x77FF),
                IntRange(0x7800, 0x8CFF)
            ),
            space = 0x3000.toChar(),
            name = "CJK"
        )
    }
}

/**
 * Used by [RandomTextGenerator] in order to create plain text or multi-styled text.
 */
enum class TextType {
    PlainText,
    StyledText
}

/**
 * Creates a cartesian product of the given arrays.
 */
fun cartesian(vararg lists: Array<Any>): List<Array<Any>> {
    return lists.fold(listOf(arrayOf())) { acc, list ->
        // add items from the current list
        // to each list that was accumulated
        acc.flatMap { accListItem -> list.map { accListItem + it } }
    }
}