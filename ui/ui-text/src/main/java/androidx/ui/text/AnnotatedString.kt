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

import androidx.ui.text.AnnotatedString.Builder
import androidx.ui.text.AnnotatedString.Item
import java.util.SortedSet

/**
 * The class changes the character level style of the specified range.
 * @see AnnotatedString.Builder
 */
typealias SpanStyleItem = Item<SpanStyle>

/**
 * The class changes the paragraph level style of the specified range.
 * @see AnnotatedString.Builder
 */
typealias ParagraphStyleItem = Item<ParagraphStyle>

/**
 * The basic data structure of text with multiple styles. To construct an [AnnotatedString] you
 * can use [Builder].
 */
data class AnnotatedString(
    val text: String,
    val spanStyles: List<SpanStyleItem> = listOf(),
    val paragraphStyles: List<ParagraphStyleItem> = listOf()
) {

    init {
        var lastStyleEnd = -1
        for (paragraphStyle in paragraphStyles) {
            require(paragraphStyle.start >= lastStyleEnd) { "ParagraphStyle should not overlap" }
            require(paragraphStyle.end <= text.length) {
                "ParagraphStyle range [${paragraphStyle.start}, ${paragraphStyle.end})" +
                        " is out of boundary"
            }
            lastStyleEnd = paragraphStyle.end
        }
    }

    operator fun plus(other: AnnotatedString): AnnotatedString {
        return with(Builder(this)) {
            append(other)
            toAnnotatedString()
        }
    }

    /**
     * The information attached on the text such as a [SpanStyle].
     *
     * @param item The object attached to [AnnotatedString]s.
     * @param start The start of the range where [item] takes effect. It's inclusive
     * @param end The end of the range where [item] takes effect. It's exclusive
     */
    data class Item<T>(val item: T, val start: Int, val end: Int) {
        init {
            require(start <= end) { "Reversed range is not supported" }
        }
    }

    /**
     * Builder class for AnnotatedString. Enables construction of an [AnnotatedString] using
     * methods such as [append] and [addStyle].
     *
     * @sample androidx.ui.text.samples.AnnotatedStringBuilderSample
     *
     * @param capacity initial capacity for the internal char buffer
     */
    class Builder(capacity: Int = 16) {

        private data class MutableItem<T>(
            val item: T,
            val start: Int,
            var end: Int = Int.MIN_VALUE
        ) {
            /**
             * Create an immutable [Item] object.
             *
             * @param defaultEnd if the end is not set yet, it will be set to this value.
             */
            fun toItem(defaultEnd: Int = Int.MIN_VALUE): Item<T> {
                val end = if (end == Int.MIN_VALUE) defaultEnd else end
                check(end != Int.MIN_VALUE) { "Item.end should be set first" }
                return Item(item = item, start = start, end = end)
            }
        }

        private val text: StringBuilder = StringBuilder(capacity)
        private val spanStyles: MutableList<MutableItem<SpanStyle>> = mutableListOf()
        private val paragraphStyles: MutableList<MutableItem<ParagraphStyle>> = mutableListOf()
        private val styleStack: MutableList<MutableItem<out Any>> = mutableListOf()

        /**
         * Create an [Builder] instance using the given [String].
         */
        constructor(text: String) : this() {
            append(text)
        }

        /**
         * Create an [Builder] instance using the given [AnnotatedString].
         */
        constructor(text: AnnotatedString) : this() {
            append(text)
        }

        /**
         * Returns the length of the [String].
         */
        val length: Int get() = text.length

        override fun toString(): String = text.toString()

        /**
         * Appends the given [String] to this [Builder].
         *
         * @param text the text to append
         */
        fun append(text: String) {
            this.text.append(text)
        }

        /**
         * Appends the given [Char] to this [Builder].
         *
         * @param char the Char to append
         */
        fun append(char: Char) {
            this.text.append(char)
        }

        /**
         * Appends the given [AnnotatedString] to this [Builder].
         *
         * @param text the text to append
         */
        fun append(text: AnnotatedString) {
            val start = this.text.length
            this.text.append(text.text)
            // offset every style with start and add to the builder
            text.spanStyles.forEach {
                addStyle(it.item, start + it.start, start + it.end)
            }
            text.paragraphStyles.forEach {
                addStyle(it.item, start + it.start, start + it.end)
            }
        }

        /**
         * Set a [SpanStyle] for the given [range].
         *
         * @param style [SpanStyle] to be applied
         * @param start the inclusive starting offset of the range
         * @param end the exclusive end offset of the range
         */
        fun addStyle(style: SpanStyle, start: Int, end: Int) {
            spanStyles.add(MutableItem(item = style, start = start, end = end))
        }

        /**
         * Set a [ParagraphStyle] for the given [range]. When a [ParagraphStyle] is applied to the
         * [AnnotatedString], it will be rendered as a separate paragraph.
         *
         * @param style [ParagraphStyle] to be applied
         * @param start the inclusive starting offset of the range
         * @param end the exclusive end offset of the range
         */
        fun addStyle(style: ParagraphStyle, start: Int, end: Int) {
            paragraphStyles.add(MutableItem(item = style, start = start, end = end))
        }

        /**
         * Applies the given [SpanStyle] to any appended text until a corresponding [popStyle] is
         * called.
         *
         * @sample androidx.ui.text.samples.AnnotatedStringBuilderPushSample
         *
         * @param style SpanStyle to be applied
         */
        fun pushStyle(style: SpanStyle): Int {
            MutableItem(item = style, start = text.length).also {
                styleStack.add(it)
                spanStyles.add(it)
            }
            return styleStack.size - 1
        }

        /**
         * Applies the given [ParagraphStyle] to any appended text until a corresponding [popStyle]
         * is called.
         *
         * @sample androidx.ui.text.samples.AnnotatedStringBuilderPushParagraphStyleSample
         *
         * @param style ParagraphStyle to be applied
         */
        fun pushStyle(style: ParagraphStyle): Int {
            MutableItem(item = style, start = text.length).also {
                styleStack.add(it)
                paragraphStyles.add(it)
            }
            return styleStack.size - 1
        }

        /**
         * Ends the style that was added via a push operation before.
         *
         * @see pushStyle
         */
        fun popStyle() {
            check(styleStack.isNotEmpty()) { "Nothing to pop." }
            // pop the last element
            val item = styleStack.removeAt(styleStack.size - 1)
            item.end = text.length
        }

        /**
         * Ends the styles up to and `including` the [pushStyle] that returned the given index.
         *
         * @param index the result of the a previous [pushStyle] in order to pop to
         *
         * @see popStyle
         * @see pushStyle
         */
        fun popStyle(index: Int) {
            check(index < styleStack.size) { "$index should be less than ${styleStack.size}" }
            while ((styleStack.size - 1) >= index) {
                popStyle()
            }
        }

        /**
         * Constructs an [AnnotatedString] based on the configurations applied to the [Builder].
         */
        fun toAnnotatedString(): AnnotatedString {
            return AnnotatedString(
                text = text.toString(),
                spanStyles = spanStyles.map { it.toItem(text.length) }.toList(),
                paragraphStyles = paragraphStyles.map { it.toItem(text.length) }.toList()
            )
        }
    }
}

/**
 * A helper function used to determine the paragraph boundaries in [MultiParagraph].
 *
 * It reads paragraph information from [AnnotatedString.paragraphStyles] where only some parts of
 * text has [ParagraphStyle] specified, and unspecified parts(gaps between specified paragraphs)
 * are considered as default paragraph with default [ParagraphStyle].
 * For example, the following string with a specified paragraph denoted by "[]"
 *      "Hello WorldHi!"
 *      [          ]
 * The result paragraphs are "Hello World" and "Hi!".
 *
 * @param defaultParagraphStyle The default [ParagraphStyle]. It's used for both unspecified
 *  default paragraphs and specified paragraph. When a specified paragraph's [ParagraphStyle] has
 *  a null attribute, the default one will be used instead.
 */
internal fun AnnotatedString.normalizedParagraphStyles(
    defaultParagraphStyle: ParagraphStyle
): List<ParagraphStyleItem> {
    val length = text.length
    val paragraphStyles = paragraphStyles

    var lastOffset = 0
    val result = mutableListOf<ParagraphStyleItem>()
    for ((style, start, end) in paragraphStyles) {
        if (start != lastOffset) {
            result.add(Item(defaultParagraphStyle, lastOffset, start))
        }
        result.add(Item(defaultParagraphStyle.merge(style), start, end))
        lastOffset = end
    }
    if (lastOffset != length) {
        result.add(Item(defaultParagraphStyle, lastOffset, length))
    }
    // This is a corner case where annotatedString is an empty string without any ParagraphStyle.
    // In this case, a dummy ParagraphStyle is created.
    if (result.isEmpty()) {
        result.add(Item(defaultParagraphStyle, 0, 0))
    }
    return result
}

/**
 * Helper function used to find the [SpanStyle]s in the given paragraph range and also convert the
 * range of those [SpanStyle]s to paragraph local range.
 *
 * @param start The start index of the paragraph range, inclusive
 * @param end The end index of the paragraph range, exclusive
 * @return The list of converted [SpanStyle]s in the given paragraph range
 */
private fun AnnotatedString.getLocalStyles(
    start: Int,
    end: Int
): List<SpanStyleItem> {
    if (start == end) {
        return listOf()
    }
    // If the given range covers the whole AnnotatedString, return SpanStyles without conversion.
    if (start == 0 && end >= this.text.length) {
        return spanStyles
    }
    return spanStyles.filter { it.start < end && it.end > start }
        .map {
            Item(
                it.item,
                it.start.coerceIn(start, end) - start,
                it.end.coerceIn(start, end) - start
            )
        }
}

/**
 * Helper function used to return another AnnotatedString that is a substring from [start] to
 * [end]. This will ignore the [ParagraphStyle]s and the resulting [AnnotatedString] will have no
 * [ParagraphStyle]s.
 *
 * @param start The start index of the paragraph range, inclusive
 * @param end The end index of the paragraph range, exclusive
 * @return The list of converted [SpanStyle]s in the given paragraph range
 */
private fun AnnotatedString.substringWithoutParagraphStyles(
    start: Int,
    end: Int
): AnnotatedString {
    return AnnotatedString(
        text = if (start != end) text.substring(start, end) else "",
        spanStyles = getLocalStyles(start, end)
    )
}

internal inline fun <T> AnnotatedString.mapEachParagraphStyle(
    defaultParagraphStyle: ParagraphStyle,
    crossinline block: (
        annotatedString: AnnotatedString,
        paragraphStyle: ParagraphStyleItem
    ) -> T
): List<T> {
    return normalizedParagraphStyles(defaultParagraphStyle).map { paragraphStyleItem ->
        val annotatedString = substringWithoutParagraphStyles(
            paragraphStyleItem.start,
            paragraphStyleItem.end
        )
        block(annotatedString, paragraphStyleItem)
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
fun AnnotatedString.toUpperCase(localeList: LocaleList = LocaleList.current): AnnotatedString {
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
fun AnnotatedString.toLowerCase(localeList: LocaleList = LocaleList.current): AnnotatedString {
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
fun AnnotatedString.capitalize(localeList: LocaleList = LocaleList.current): AnnotatedString {
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
fun AnnotatedString.decapitalize(localeList: LocaleList = LocaleList.current): AnnotatedString {
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
private fun AnnotatedString.transform(transform: (String, Int, Int) -> String): AnnotatedString {
    val transitions = sortedSetOf<Int>()
    collectItemTransitions(spanStyles, transitions)
    collectItemTransitions(paragraphStyles, transitions)

    var resultStr = ""
    val offsetMap = mutableMapOf(0 to 0)
    transitions.windowed(size = 2) { (start, end) ->
        resultStr += transform(text, start, end)
        offsetMap.put(end, resultStr.length)
    }

    val newSpanStyles = mutableListOf<SpanStyleItem>()
    val newParaStyles = mutableListOf<ParagraphStyleItem>()

    for (spanStyle in spanStyles) {
        // The offset map must have mapping entry from all style start, end position.
        newSpanStyles.add(
            Item(
                spanStyle.item,
                offsetMap[spanStyle.start]!!,
                offsetMap[spanStyle.end]!!
            )
        )
    }

    for (paraStyle in paragraphStyles) {
        newParaStyles.add(
            Item(
                paraStyle.item,
                offsetMap[paraStyle.start]!!,
                offsetMap[paraStyle.end]!!
            )
        )
    }

    return AnnotatedString(
        text = resultStr,
        spanStyles = newSpanStyles,
        paragraphStyles = newParaStyles)
}

/**
 * Adds all [AnnotatedString.Item] transition points
 *
 * @param items The list of AnnotatedString.Item
 * @param target The output list
 */
private fun <T> collectItemTransitions(
    items: List<Item<T>>,
    target: SortedSet<Int>
) {
    items.fold(target) { acc, item ->
        acc.apply {
            add(item.start)
            add(item.end)
        }
    }
}

/**
 * Returns the length of the [AnnotatedString].
 */
val AnnotatedString.length: Int get() = text.length

/**
 * Pushes [style] to the [AnnotatedString.Builder], executes [block] and then pops the [style].
 *
 * @sample androidx.ui.text.samples.AnnotatedStringBuilderWithStyleSample
 *
 * @param style [SpanStyle] to be applied
 * @param block function to be executed
 *
 * @return result of the [block]
 *
 * @see AnnotatedString.Builder.pushStyle
 * @see AnnotatedString.Builder.popStyle
 */
inline fun <R : Any> Builder.withStyle(
    style: SpanStyle,
    crossinline block: Builder.() -> R
): R {
    val index = pushStyle(style)
    return try {
        block(this)
    } finally {
        popStyle(index)
    }
}

/**
 * Pushes [style] to the [AnnotatedString.Builder], executes [block] and then pops the [style].
 *
 * @sample androidx.ui.text.samples.AnnotatedStringBuilderWithStyleSample
 *
 * @param style [SpanStyle] to be applied
 * @param block function to be executed
 *
 * @return result of the [block]
 *
 * @see AnnotatedString.Builder.pushStyle
 * @see AnnotatedString.Builder.popStyle
 */
inline fun <R : Any> Builder.withStyle(
    style: ParagraphStyle,
    crossinline block: Builder.() -> R
): R {
    val index = pushStyle(style)
    return try {
        block(this)
    } finally {
        popStyle(index)
    }
}

/**
 * Filter the Item list based on [Item.start] and [Item.end] to include items only in the range
 * of [start] (inclusive) and [end] (exclusive).
 *
 * @param start the inclusive start offset of the text range
 * @param end the exclusive end offset of the text range
 */
private fun <T> filterItemsByRange(items: List<Item<T>>, start: Int, end: Int): List<Item<T>> {
    require(start <= end) { "start ($start) should be less than or equal to end ($end)" }
    return items.filter {
        // for collapsed items/ranges, if starts are same accept. Otherwise:
        // item.end is exclusive, therefore if equals to start which is inclusive should be
        // filtered
        // likewise end is exclusive, if equals to item.start which is inclusive should be
        // filtered
        (it.start == start) || !(it.end <= start || it.start >= end)
    }.map {
        Item(
            item = it.item,
            start = (if (it.start < start) start else it.start) - start,
            end = (if (end < it.end) end else it.end) - start
        )
    }
}

/**
 * Return a substring for the AnnotatedString and include the styles in the range of [start]
 * (inclusive) and [end] (exclusive).
 *
 * @param start the inclusive start offset of the range
 * @param end the exclusive end offset of the range
 */
fun AnnotatedString.subSequence(start: Int, end: Int): AnnotatedString {
    require(start <= end) { "start ($start) should be less or equal to end ($end)" }

    val text = text.substring(start, end)
    return AnnotatedString(
        text = text,
        spanStyles = filterItemsByRange(spanStyles, start, end),
        paragraphStyles = filterItemsByRange(paragraphStyles, start, end)
    )
}

/**
 * Create an AnnotatedString with a [spanStyle] that will apply to the whole text.
 *
 * @param spanStyle [SpanStyle] to be applied to whole text
 * @param paragraphStyle [ParagraphStyle] to be applied to whole text
 */
fun AnnotatedString(
    text: String,
    spanStyle: SpanStyle,
    paragraphStyle: ParagraphStyle? = null
): AnnotatedString = AnnotatedString(
    text,
    listOf(Item(spanStyle, 0, text.length)),
    if (paragraphStyle == null) listOf() else listOf(Item(paragraphStyle, 0, text.length))
)

/**
 * Create an AnnotatedString with a [paragraphStyle] that will apply to the whole text.
 *
 * @param paragraphStyle [ParagraphStyle] to be applied to whole text
 */
fun AnnotatedString(
    text: String,
    paragraphStyle: ParagraphStyle
): AnnotatedString = AnnotatedString(
    text,
    listOf(),
    listOf(Item(paragraphStyle, 0, text.length))
)

/**
 * Build a new AnnotatedString by populating newly created [AnnotatedString.Builder] provided
 * by [builder].
 *
 * @sample androidx.ui.text.samples.AnnotatedStringBuilderLambdaSample
 *
 * @param builder lambda to modify [AnnotatedString.Builder]
 */
inline fun AnnotatedString(builder: (Builder).() -> Unit): AnnotatedString =
    Builder().apply(builder).toAnnotatedString()