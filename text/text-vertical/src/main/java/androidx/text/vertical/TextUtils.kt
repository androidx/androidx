/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.text.vertical

import android.graphics.Paint
import android.text.Spanned
import android.text.TextPaint
import android.util.ArrayMap
import java.text.BreakIterator
import java.text.CharacterIterator
import java.util.Locale
import kotlin.concurrent.getOrSet

/**
 * Iterates through spans of a specific type within a given range of a CharSequence.
 *
 * @param text The CharSequence
 * @param start The inclusive starting index.
 * @param end The exclusive ending index.
 * @param consumer A lambda function that will be called for each segment containing spans.
 */
internal inline fun <reified T> forEachSpan(
    text: CharSequence,
    start: Int,
    end: Int,
    crossinline consumer: (Int, Int, Array<T>) -> Unit,
) {
    if (text !is Spanned) {
        consumer(start, end, arrayOf())
        return
    }

    var spanI = start
    while (spanI < end) {
        val next = text.nextSpanTransition(spanI, end, T::class.java)
        val spans = text.getSpans(spanI, next, T::class.java)
        consumer(spanI, next, spans)
        spanI = next
    }
}

/**
 * Iterates over the code points within a specified range of a [CharSequence].
 *
 * @param text The CharSequence
 * @param start The inclusive starting index.
 * @param end The exclusive ending index.
 * @param consumer A lambda function that will be called for each code points.
 */
internal inline fun fotEachCodePoints(
    text: CharSequence,
    start: Int,
    end: Int,
    crossinline consumer: (Int, Int) -> Unit,
) {
    var i = start
    while (i < end) {
        val cp = Character.codePointAt(text, i)
        consumer(i, cp)
        i += Character.charCount(cp)
    }
}

/**
 * Extension function to iterate over paragraphs within a CharSequence.
 *
 * This function iterates through the specified range of the CharSequence, identifying paragraphs
 * based on newline characters ('\n'). For each paragraph found, it invokes the provided block with
 * the start and end indices of the paragraph.
 *
 * @param start The inclusive starting index.
 * @param end The exclusive ending index.
 * @param block A lambda function that takes two Int parameters representing the start and end
 *   indices of a paragraph.
 */
internal inline fun CharSequence.forEachParagraph(
    start: Int,
    end: Int,
    crossinline block: (Int, Int) -> Unit,
) {
    var paraStart = start
    while (paraStart < end) {
        var paraEnd = indexOf('\n', paraStart) // TODO support other paragraph separator
        if (paraEnd == -1 || paraEnd >= end) {
            paraEnd = end
        } else {
            // Include the paragraph separator in the current paragraph.
            paraEnd++
        }

        block(paraStart, paraEnd)

        paraStart = paraEnd
    }
}

/**
 * Executes a block of code with a temporary applying scaling of the text size of a given
 * [TextPaint].
 */
internal inline fun <T : Paint, R> withTempScale(
    textPaint: T,
    scale: Float,
    crossinline block: () -> R,
): R {
    val originalSize = textPaint.textSize
    textPaint.textSize *= scale
    try {
        return block()
    } finally {
        textPaint.textSize = originalSize
    }
}

/**
 * A [CharacterIterator] implementation that iterates over a substring of a [CharSequence].
 *
 * This class is used to provide a `CharacterIterator` for `BreakIterator` when working with a
 * portion of a `CharSequence`.
 *
 * @param text The source `CharSequence`.
 * @param start The starting index (inclusive) of the substring in the `text`.
 * @param end The ending index (exclusive) of the substring in the `text`.
 */
internal class CharSequenceCharacterIterator(
    private val text: CharSequence,
    private val start: Int,
    private val end: Int,
) : CharacterIterator {

    private var index: Int = start

    override fun clone(): Any {
        val it = CharSequenceCharacterIterator(text, start, end)
        it.index = index
        return it
    }

    override fun current(): Char {
        return if (index == end) CharacterIterator.DONE else text[index]
    }

    override fun first(): Char {
        index = start
        return current()
    }

    override fun getBeginIndex(): Int = start

    override fun getEndIndex(): Int = end

    override fun getIndex(): Int = index

    override fun last(): Char {
        return if (start == end) {
            index = end
            CharacterIterator.DONE
        } else {
            index = end - 1
            text[index]
        }
    }

    override fun next(): Char {
        index++
        return if (index >= end) {
            index = end
            CharacterIterator.DONE
        } else {
            text[index]
        }
    }

    override fun previous(): Char {
        return if (index <= start) {
            CharacterIterator.DONE
        } else {
            index--
            text[index]
        }
    }

    override fun setIndex(pos: Int): Char {
        return if (pos in start..end) {
            index = pos
            current()
        } else {
            throw IllegalArgumentException("index out of range")
        }
    }
}

private val sGraphemeIteratorPool = ThreadLocal<ArrayMap<Locale, BreakIterator>>()

private fun acquireGraphemeIterator(locale: Locale): BreakIterator {
    val map = sGraphemeIteratorPool.getOrSet { ArrayMap() }
    val pooled = map[locale]
    if (pooled != null) {
        map[locale] = null
        return pooled
    } else {
        return BreakIterator.getCharacterInstance(locale)
    }
}

private fun releaseGraphemeIterator(locale: Locale, iterator: BreakIterator) {
    val map = sGraphemeIteratorPool.getOrSet { ArrayMap() }
    if (map[locale] == null) {
        map[locale] = iterator
    }
}

internal inline fun CharSequence.forEachGrapheme(
    start: Int,
    end: Int,
    locale: Locale,
    crossinline block: (Int, Int) -> Unit,
) {
    val it = acquireGraphemeIterator(locale)
    it.text = CharSequenceCharacterIterator(this, start, end)

    var grStart = start
    var grNext = it.following(grStart)
    while (grNext != BreakIterator.DONE) {
        block(grStart, grNext)
        grStart = grNext
        grNext = it.next()
    }

    releaseGraphemeIterator(locale, it)
}
