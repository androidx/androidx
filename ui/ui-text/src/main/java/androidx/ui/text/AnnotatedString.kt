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

import java.util.SortedSet

/**
 * The basic data structure of text with multiple styles.
 */
data class AnnotatedString(
    val text: String,
    val textStyles: List<Item<TextStyle>> = listOf(),
    val paragraphStyles: List<Item<ParagraphStyle>> = listOf()
) {

    init {
        var lastStyleEnd = -1
        for (paragraphStyle in paragraphStyles) {
            if (paragraphStyle.start < lastStyleEnd) {
                throw IllegalArgumentException("ParagraphStyle should not overlap")
            }
            if (paragraphStyle.end > text.length) {
                throw IllegalArgumentException("ParagraphStyle range " +
                        "[${paragraphStyle.start}, ${paragraphStyle.end}) is out of boundary")
            }
            lastStyleEnd = paragraphStyle.end
        }
    }

    /**
     * The information attached on the text such as a TextStyle.
     *
     * @param style The style being applied on this part of [AnnotatedString].
     * @param start The start of the range where [style] takes effect. It's inclusive.
     * @param end The end of the range where [style] takes effect. It's exclusive.
     */
    // TODO(haoyuchang): Check some other naming options.
    data class Item<T>(val style: T, val start: Int, val end: Int) {
        init {
            if (start > end) {
                throw IllegalArgumentException("Reversed range is not supported")
            }
        }
    }

    /**
     * Create upper case transformed [AnnotatedString]
     *
     * The uppercase sometimes maps different number of characters. This function adjusts the text
     * style and paragraph style ranges to transformed offset.
     *
     * Note, if the style's offset is middle of the uppercase mapping context, this function won't
     * transform the character, e.g. style starts from between base alphabet character and accent
     * character.
     *
     * @param localeList A locale list used for upper case mapping. Only the first locale is
     *                   effective. If empty locale list is passed, use the current locale instead.
     * @return A uppercase transformed string.
     */
    fun toUpperCase(localeList: LocaleList = LocaleList.current): AnnotatedString {
        return transform { str, start, end -> str.substring(start, end).toUpperCase(localeList) }
    }

    /**
     * Create lower case transformed [AnnotatedString]
     *
     * The lowercase sometimes maps different number of characters. This function adjusts the text
     * style and paragraph style ranges to transformed offset.
     *
     * Note, if the style's offset is middle of the lowercase mapping context, this function won't
     * transform the character, e.g. style starts from between base alphabet character and accent
     * character.
     *
     * @param localeList A locale list used for lower case mapping. Only the first locale is
     *                   effective. If empty locale list is passed, use the current locale instead.
     * @return A lowercase transformed string.
     */
    fun toLowerCase(localeList: LocaleList = LocaleList.current): AnnotatedString {
        return transform { str, start, end -> str.substring(start, end).toLowerCase(localeList) }
    }

    /**
     * Create capitalized [AnnotatedString]
     *
     * The capitalization sometimes maps different number of characters. This function adjusts the
     * text style and paragraph style ranges to transformed offset.
     *
     * Note, if the style's offset is middle of the capitalization context, this function won't
     * transform the character, e.g. style starts from between base alphabet character and accent
     * character.
     *
     * @param localeList A locale list used for capitalize mapping. Only the first locale is
     *                   effective. If empty locale list is passed, use the current locale instead.
     *                   Note that, this locale is currently ignored since underlying Kotlin method
     *                   is experimental.
     * @return A capitalized string.
     */
    fun capitalize(localeList: LocaleList = LocaleList.current): AnnotatedString {
        return transform { str, start, end ->
            if (start == 0) {
                str.substring(start, end).capitalize(localeList)
            } else {
                str.substring(start, end)
            }
        }
    }

    /**
     * Create capitalized [AnnotatedString]
     *
     * The decapitalization sometimes maps different number of characters. This function adjusts
     * the text style and paragraph style ranges to transformed offset.
     *
     * Note, if the style's offset is middle of the decapitalization context, this function won't
     * transform the character, e.g. style starts from between base alphabet character and accent
     * character.
     *
     * @param localeList A locale list used for decapitalize mapping. Only the first locale is
     *                   effective. If empty locale list is passed, use the current locale instead.
     *                   Note that, this locale is currently ignored since underlying Kotlin method
     *                   is experimental.
     * @return A decapitalized string.
     */
    fun decapitalize(localeList: LocaleList = LocaleList.current): AnnotatedString {
        return transform { str, start, end ->
            if (start == 0) {
                str.substring(start, end).decapitalize(localeList)
            } else {
                str.substring(start, end)
            }
        }
    }

    /**
     * The core function of [AnnotatedString] transformation.
     *
     * @param transform the transformation method
     * @return newly allocated transformed AnnotatedString
     */
    private fun transform(transform: (String, Int, Int) -> String): AnnotatedString {
        val transitions = sortedSetOf<Int>()
        collectItemTransitions(textStyles, transitions)
        collectItemTransitions(paragraphStyles, transitions)

        var resultStr = ""
        val offsetMap = mutableMapOf(0 to 0)
        transitions.windowed(size = 2) { (start, end) ->
            resultStr += transform(text, start, end)
            offsetMap.put(end, resultStr.length)
        }

        val newTextStyles = mutableListOf<Item<TextStyle>>()
        val newParaStyles = mutableListOf<Item<ParagraphStyle>>()

        for (textStyle in textStyles) {
            // The offset map must have mapping entry from all style start, end position.
            newTextStyles.add(
                Item(
                    textStyle.style,
                    offsetMap[textStyle.start]!!,
                    offsetMap[textStyle.end]!!
                )
            )
        }

        for (paraStyle in paragraphStyles) {
            newParaStyles.add(
                Item(
                    paraStyle.style,
                    offsetMap[paraStyle.start]!!,
                    offsetMap[paraStyle.end]!!
                )
            )
        }

        return AnnotatedString(
            text = resultStr,
            textStyles = newTextStyles,
            paragraphStyles = newParaStyles)
    }

    /**
     * Adds all [AnnotatedString.Item] transition points
     *
     * @param items The list of AnnotatedString.Item
     * @param target The output list
     */
    private fun <T> collectItemTransitions(items: List<Item<T>>, target: SortedSet<Int>) {
        items.fold(target) { acc, item ->
            acc.apply {
                add(item.start)
                add(item.end)
            }
        }
    }
}