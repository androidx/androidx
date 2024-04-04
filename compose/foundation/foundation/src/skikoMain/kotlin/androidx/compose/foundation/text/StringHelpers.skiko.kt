/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text

import org.jetbrains.skia.BreakIterator

internal actual fun String.findPrecedingBreak(index: Int): Int {
    val it = BreakIterator.makeCharacterInstance()
    it.setText(this)
    return it.preceding(index)
}

internal actual fun String.findFollowingBreak(index: Int): Int {
    val it = BreakIterator.makeCharacterInstance()
    it.setText(this)
    return it.following(index)
}

// Copied from CharHelpers.skiko.kt
// TODO Remove once it's available in common stdlib https://youtrack.jetbrains.com/issue/KT-23251
internal typealias CodePoint = Int


// Copied from CharHelpers.skiko.kt
/**
 * Converts a surrogate pair to a unicode code point.
 */
private fun Char.Companion.toCodePoint(high: Char, low: Char): CodePoint =
    (((high - MIN_HIGH_SURROGATE) shl 10) or (low - MIN_LOW_SURROGATE)) + 0x10000

// Copied from CharHelpers.skiko.kt
/**
 * The minimum value of a supplementary code point, `\u0x10000`.
 */
private const val MIN_SUPPLEMENTARY_CODE_POINT: Int = 0x10000

// Copied from CharHelpers.skiko.kt
internal fun CodePoint.charCount(): Int = if (this >= MIN_SUPPLEMENTARY_CODE_POINT) 2 else 1

// Copied from CharHelpers.skiko.kt
internal val String.codePoints
    get() = sequence {
        var index = 0
        while (index < length) {
            val codePoint = codePointAt(index)
            yield(codePoint)
            index += codePoint.charCount()
        }
    }

// Copied from CharHelpers.skiko.kt
/**
 * Returns the character (Unicode code point) at the specified index.
 */
internal fun CharSequence.codePointAt(index: Int): CodePoint {
    val high = this[index]
    if (high.isHighSurrogate() && index + 1 < this.length) {
        val low = this[index + 1]
        if (low.isLowSurrogate()) {
            return Char.toCodePoint(high, low)
        }
    }
    return high.code
}

/**
 * Returns the count of Unicode code points.
 */
internal fun CharSequence.codePointCount(): Int {
    var count = length
    var i = 0
    while (i < length - 1) {
        if (this[i].isHighSurrogate() && this[i + 1].isLowSurrogate()) {
            count--
            i += 2
        } else {
            i++
        }
    }
    return count
}

/**
 * Finds the offset of the next non-whitespace symbols subsequence (word) in the given text
 * starting from the specified caret offset.
 *
 * @param offset The offset where to start looking for the next word.
 * @param currentText The current text in which to search for the next word.
 * @return The offset of the next non-whitespace symbols subsequence (word), or the end of the string
 *         if no such word is found.
 */
internal fun findNextNonWhitespaceSymbolsSubsequenceStartOffset(
    offset: Int,
    currentText: String
): Int {
    /* Assume that next non whitespaces symbols subsequence (word) is when current char is whitespace and next character is not.
     * Emoji (compound incl.) should be treated as a new word.
     */
    val charIterator =
        BreakIterator.makeCharacterInstance() // wordInstance doesn't consider symbols sequence as word
    charIterator.setText(currentText)

    var currentOffset: Int
    var nextOffset = charIterator.next()
    while (nextOffset < offset) {
        nextOffset = charIterator.next()
    }
    currentOffset = nextOffset

    nextOffset = charIterator.next()

    while (nextOffset < currentText.length) { // charIterator.next() works one more time than needed, better use this
        if (currentText.codePointAt(currentOffset).isWhitespace() && !currentText.codePointAt(
                nextOffset
            ).isWhitespace()
        ) {
            return nextOffset
        } else {
            currentOffset = nextOffset
        }

        nextOffset = charIterator.next()
    }
    return currentOffset
}

/**
 * Checks if the character at the specified offset in the given string is either a whitespace or punctuation character.
 *
 * @param offset The offset of the character to check.
 * @return `true` if the character is a whitespace or punctuation character, `false` otherwise.
 */
internal fun String.isWhitespaceOrPunctuation(offset: Int): Boolean {
    val codePoint = this.codePointAt(offset)
    return codePoint.isPunctuation() || codePoint.isWhitespace()
}

/**
 * Returns the midpoint position in the string considering Unicode symbols.
 *
 * This function calculates the midpoint position in the given string, taking into account Unicode symbols.
 * It counts the number of symbols in the string to determine the midpoint position.
 *
 * @return The midpoint position in the string.
 */
internal fun String.midpointPositionWithUnicodeSymbols(): Int {
    val symbolsCount = this.codePoints.count()
    val charIterator = BreakIterator.makeCharacterInstance()
    charIterator.setText(this)
    var currentOffset = 0
    for (i in 0..symbolsCount / 2) {
        currentOffset = charIterator.next()
    }
    return currentOffset
}

/**
 * Checks if the given Unicode code point is a whitespace character.
 *
 * @return `true` if the code point is a whitespace character, `false` otherwise.
 */
private fun CodePoint.isWhitespace(): Boolean {
    // TODO: Extend this behavior when (if) Unicode will have compound whitespace characters.
    if (this.charCount() != 1) {
        return false
    }
    return this.toChar().isWhitespace()
}

/**
 * Checks if the given Unicode code point is a punctuation character.
 *
 * @return 'true' if the CodePoint is a punctuation character, 'false' otherwise.
 */
private fun CodePoint.isPunctuation(): Boolean {
    // TODO: Extend this behavior when (if) Unicode will have compound punctuation characters.
    if (this.charCount() != 1) {
        return false
    }
    val punctuationSet = setOf(
        CharCategory.DASH_PUNCTUATION,
        CharCategory.START_PUNCTUATION,
        CharCategory.END_PUNCTUATION,
        CharCategory.CONNECTOR_PUNCTUATION,
        CharCategory.OTHER_PUNCTUATION,
        CharCategory.INITIAL_QUOTE_PUNCTUATION,
        CharCategory.FINAL_QUOTE_PUNCTUATION
    )
    return punctuationSet.any { it.contains(this.toChar()) }
}