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
package androidx.compose.ui.text.android.selection

import androidx.compose.ui.text.android.CharSequenceCharacterIterator
import androidx.compose.ui.text.internal.requirePrecondition
import androidx.emoji2.text.EmojiCompat
import java.text.BreakIterator
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Walks through cursor positions at word boundaries.
 *
 * Also provides methods to determine word boundaries.
 *
 * Note: This file is copied from
 * [WordIterator.java](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/text/method/WordIterator.java)
 *
 * @param locale The locale to be used for analyzing the text. Caches [CharSequence] for performance
 *   reasons.
 * @constructor Constructs a new WordIterator for the specified locale.
 */
internal class WordIterator(val charSequence: CharSequence, start: Int, end: Int, locale: Locale?) {
    private val start: Int
    private val end: Int
    private val iterator: BreakIterator

    init {
        requirePrecondition(start in 0..charSequence.length) {
            "input start index is outside the CharSequence"
        }
        requirePrecondition(end in 0..charSequence.length) {
            "input end index is outside the CharSequence"
        }
        iterator = BreakIterator.getWordInstance(locale)
        this.start = max(0, start - WINDOW_WIDTH)
        this.end = min(charSequence.length, end + WINDOW_WIDTH)
        iterator.text = CharSequenceCharacterIterator(charSequence, start, end)
    }

    /**
     * Returns the position of next boundary after the given offset. Returns `BreakIterator.DONE` if
     * there is no boundary after the given offset.
     *
     * @param offset the given start position to search from.
     * @return the position of the last boundary preceding the given offset.
     */
    fun nextBoundary(offset: Int): Int {
        checkOffsetIsValid(offset)
        val following = iterator.following(offset)
        // We should iterate through if the boundary is between a letter/digit and emoji
        // These should not be considered boundaries
        if (
            isOnLetterOrDigitOrEmoji(following - 1) &&
                isOnLetterOrDigitOrEmoji(following) &&
                // Extra logic for Japanese to detect boundary between Hiragana and Katakana
                // characters
                !isHiraganaKatakanaBoundary(following)
        ) {
            return nextBoundary(following)
        }
        return following
    }

    /**
     * Returns the position of boundary preceding the given offset or `BreakIterator.DONE` if the
     * given offset specifies the starting position.
     *
     * @param offset the given start position to search from.
     * @return the position of the last boundary preceding the given offset.
     */
    fun prevBoundary(offset: Int): Int {
        checkOffsetIsValid(offset)
        val preceding = iterator.preceding(offset)
        // We should iterate through if the boundary is between a letter/digit and emoji
        // These should not be considered boundaries
        return if (
            isOnLetterOrDigitOrEmoji(preceding) &&
                isAfterLetterOrDigitOrEmoji(preceding) &&
                // Extra logic for Japanese to detect boundary between Hiragana and Katakana
                // characters
                !isHiraganaKatakanaBoundary(preceding)
        ) {
            prevBoundary(preceding)
        } else preceding
    }

    /**
     * If the `offset` is within a word or on a word boundary that can only be considered the start
     * of a word (e.g. _word where "_" is any character that would not be considered part of the
     * word) then this returns the index of the first character of that word.
     *
     * If the offset is on a word boundary that can be considered the start and end of a word, e.g.
     * AABB (where AA and BB are both words) and the offset is the boundary between AA and BB, this
     * would return the start of the previous word, AA.
     *
     * Returns BreakIterator.DONE if there is no previous boundary.
     *
     * @throws IllegalArgumentException is offset is not valid.
     */
    fun getPrevWordBeginningOnTwoWordsBoundary(offset: Int): Int {
        return getBeginning(offset, true)
    }

    /**
     * If the `offset` is within a word or on a word boundary that can only be considered the end of
     * a word (e.g. word_ where "_" is any character that would not be considered part of the word)
     * then this returns the index of the last character plus one of that word.
     *
     * If the offset is on a word boundary that can be considered the start and end of a word, e.g.
     * AABB (where AA and BB are both words) and the offset is the boundary between AA and BB, this
     * would return the end of the next word, BB.
     *
     * Returns BreakIterator.DONE if there is no next boundary.
     *
     * @throws IllegalArgumentException is offset is not valid.
     */
    fun getNextWordEndOnTwoWordBoundary(offset: Int): Int {
        return getEnd(offset, true)
    }

    /**
     * If `offset` is within a group of punctuation as defined by [isPunctuation], returns the index
     * of the first character of that group, otherwise returns BreakIterator.DONE.
     *
     * @param offset the offset to search from.
     */
    fun getPunctuationBeginning(offset: Int): Int {
        checkOffsetIsValid(offset)
        var result = offset
        while (result != BreakIterator.DONE && !isPunctuationStartBoundary(result)) {
            result = prevBoundary(result)
        }
        // No need to shift offset, prevBoundary handles that.
        return result
    }

    /**
     * If `offset` is within a group of punctuation as defined by [isPunctuation], returns the index
     * of the last character of that group plus one, otherwise returns BreakIterator.DONE.
     *
     * @param offset the offset to search from.
     */
    fun getPunctuationEnd(offset: Int): Int {
        checkOffsetIsValid(offset)
        var result = offset
        while (result != BreakIterator.DONE && !isPunctuationEndBoundary(result)) {
            result = nextBoundary(result)
        }
        // No need to shift offset, nextBoundary handles that.
        return result
    }

    /**
     * Indicates if the provided offset is after a punctuation character as defined by
     * [isPunctuation].
     *
     * @param offset the offset to check from.
     * @return Whether the offset is after a punctuation character.
     */
    fun isAfterPunctuation(offset: Int): Boolean {
        if (offset in (start + 1)..end) {
            val codePoint = Character.codePointBefore(charSequence, offset)
            return isPunctuation(codePoint)
        }
        return false
    }

    /**
     * Indicates if the provided offset is at a punctuation character as defined by [isPunctuation].
     *
     * @param offset the offset to check from.
     * @return Whether the offset is at a punctuation character.
     */
    fun isOnPunctuation(offset: Int): Boolean {
        if (offset in start until end) {
            val codePoint = Character.codePointAt(charSequence, offset)
            return isPunctuation(codePoint)
        }
        return false
    }

    /**
     * If the `offset` is within a word or on a word boundary that can only be considered the start
     * of a word (e.g. _word where "_" is any character that would not be considered part of the
     * word) then this returns the index of the first character of that word.
     *
     * If the offset is on a word boundary that can be considered the start and end of a word, e.g.
     * AABB (where AA and BB are both words) and the offset is the boundary between AA and BB, and
     * getPrevWordBeginningOnTwoWordsBoundary is true then this would return the start of the
     * previous word, AA. Otherwise it would return the current offset, the start of BB.
     *
     * Returns BreakIterator.DONE if there is no previous boundary.
     *
     * @throws IllegalArgumentException is offset is not valid.
     */
    private fun getBeginning(offset: Int, getPrevWordBeginningOnTwoWordsBoundary: Boolean): Int {
        checkOffsetIsValid(offset)
        if (isOnLetterOrDigitOrEmoji(offset)) {
            return if (
                isBoundary(offset) &&
                    (!isAfterLetterOrDigitOrEmoji(offset) ||
                        !getPrevWordBeginningOnTwoWordsBoundary)
            ) {
                offset
            } else {
                prevBoundary(offset)
            }
        } else {
            if (isAfterLetterOrDigitOrEmoji(offset)) {
                return prevBoundary(offset)
            }
        }
        return BreakIterator.DONE
    }

    /**
     * If the `offset` is within a word or on a word boundary that can only be considered the end of
     * a word (e.g. word_ where "_" is any character that would not be considered part of the word)
     * then this returns the index of the last character plus one of that word.
     *
     * If the offset is on a word boundary that can be considered the start and end of a word, e.g.
     * AABB (where AA and BB are both words) and the offset is the boundary between AA and BB, and
     * getNextWordEndOnTwoWordBoundary is true then this would return the end of the next word, BB.
     * Otherwise it would return the current offset, the end of AA.
     *
     * Returns BreakIterator.DONE if there is no next boundary.
     *
     * @throws IllegalArgumentException is offset is not valid.
     */
    private fun getEnd(offset: Int, getNextWordEndOnTwoWordBoundary: Boolean): Int {
        checkOffsetIsValid(offset)
        if (isAfterLetterOrDigitOrEmoji(offset)) {
            return if (
                isBoundary(offset) &&
                    (!isOnLetterOrDigitOrEmoji(offset) || !getNextWordEndOnTwoWordBoundary)
            ) {
                offset
            } else {
                nextBoundary(offset)
            }
        } else {
            if (isOnLetterOrDigitOrEmoji(offset)) {
                return nextBoundary(offset)
            }
        }
        return BreakIterator.DONE
    }

    private fun isPunctuationStartBoundary(offset: Int): Boolean {
        return isOnPunctuation(offset) && !isAfterPunctuation(offset)
    }

    private fun isPunctuationEndBoundary(offset: Int): Boolean {
        return !isOnPunctuation(offset) && isAfterPunctuation(offset)
    }

    /**
     * Check if offset before current offset points to letter or digit. Emoji and surrogate
     * codepoints are treated as letters or digits.
     *
     * Note: if EmojiCompat is not initialized, emojis are not treated like letters/digits
     */
    private fun isAfterLetterOrDigitOrEmoji(offset: Int): Boolean {
        if (offset in (start + 1)..end) {
            val codePoint = Character.codePointBefore(charSequence, offset)
            if (Character.isLetterOrDigit(codePoint)) return true
            if (Character.isSurrogate(charSequence[offset - 1])) return true

            if (EmojiCompat.isConfigured()) {
                val emojiCompat = EmojiCompat.get()
                if (emojiCompat.loadState == EmojiCompat.LOAD_STATE_SUCCEEDED) {
                    val emojiStart = emojiCompat.getEmojiStart(charSequence, offset - 1)
                    // If given offset points to emoji return true
                    if (emojiStart != -1) return true
                }
            }
        }
        return false
    }

    /**
     * Check if current offset points to letter or digit. Emoji and surrogate codepoints are treated
     * as letters or digits.
     *
     * Note: if EmojiCompat is not initialized, emojis are not treated like letters/digits
     */
    private fun isOnLetterOrDigitOrEmoji(offset: Int): Boolean {
        if (offset in start until end) {
            val codePoint = Character.codePointAt(charSequence, offset)
            if (Character.isLetterOrDigit(codePoint)) return true
            if (Character.isSurrogate(charSequence[offset])) return true

            if (EmojiCompat.isConfigured()) {
                val emojiCompat = EmojiCompat.get()
                if (emojiCompat.loadState == EmojiCompat.LOAD_STATE_SUCCEEDED) {
                    val emojiStart = emojiCompat.getEmojiStart(charSequence, offset)
                    // If given offset points to emoji return true
                    if (emojiStart != -1) return true
                }
            }
        }
        return false
    }

    /** Check if the given offset is in the given range. */
    private fun checkOffsetIsValid(offset: Int) {
        requirePrecondition(offset in start..end) {
            "Invalid offset: $offset. Valid range is [$start , $end]"
        }
    }

    /**
     * Modified implementation of `iterator.isBoundary` that additionally checks boundary between
     * letters/digits and emojis as these should not be treated as boundaries.
     */
    private fun isBoundary(offset: Int): Boolean {
        checkOffsetIsValid(offset)
        return iterator.isBoundary(offset) &&
            // check offset and two characters before and after to see if all three characters
            // are letters/digits/emojis
            !(isOnLetterOrDigitOrEmoji(offset) &&
                isOnLetterOrDigitOrEmoji(offset - 1) &&
                isOnLetterOrDigitOrEmoji(offset + 1)) &&
            // check if there is boundary between hiragana and katakana characters
            // indexes 0 and charSequence.length - 1 should always be considered boundaries
            !(offset > 0 &&
                offset < charSequence.length - 1 &&
                (isHiraganaKatakanaBoundary(offset) || isHiraganaKatakanaBoundary(offset + 1)))
    }

    /** Checks if characters before and at `offset` are either hiragana or katakana */
    private fun isHiraganaKatakanaBoundary(offset: Int): Boolean {
        return ((Character.UnicodeBlock.of(charSequence[offset - 1]) ==
            Character.UnicodeBlock.HIRAGANA &&
            Character.UnicodeBlock.of(charSequence[offset]) == Character.UnicodeBlock.KATAKANA) ||
            (Character.UnicodeBlock.of(charSequence[offset]) == Character.UnicodeBlock.HIRAGANA &&
                Character.UnicodeBlock.of(charSequence[offset - 1]) ==
                    Character.UnicodeBlock.KATAKANA))
    }

    companion object {
        // The size of the WINDOW_WIDTH is currently 50, as in Android.
        // According to Wikipedia https://en.wikipedia.org/wiki/Longest_word_in_English , the
        // longest English word in English contains 45 letters. Then 50 is a good number for
        // WINDOW_WIDTH. Size of the window for the word iterator, should be greater than the
        // longest word's length.
        private const val WINDOW_WIDTH = 50

        internal fun isPunctuation(cp: Int): Boolean {
            val type = Character.getType(cp)
            return type == Character.CONNECTOR_PUNCTUATION.toInt() ||
                type == Character.DASH_PUNCTUATION.toInt() ||
                type == Character.END_PUNCTUATION.toInt() ||
                type == Character.FINAL_QUOTE_PUNCTUATION.toInt() ||
                type == Character.INITIAL_QUOTE_PUNCTUATION.toInt() ||
                type == Character.OTHER_PUNCTUATION.toInt() ||
                type == Character.START_PUNCTUATION.toInt()
        }
    }
}
