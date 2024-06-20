/*
 * Copyright 2024 The Android Open Source Project
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

import android.graphics.Paint
import android.os.Build
import android.text.TextPaint
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.android.selection.SegmentFinder.Companion.DONE
import java.text.BreakIterator

/**
 * Compose version of [android.text.SegmentFinder].
 *
 * Finds text segment boundaries within text. Subclasses can implement different types of text
 * segments. Grapheme clusters and words are examples of possible text segments. These are
 * implemented by [GraphemeClusterSegmentFinder] and [WordSegmentFinder].
 *
 * Text segments may not overlap, so every character belongs to at most one text segment. A
 * character may belong to no text segments.
 *
 * For example, WordSegmentFinder subdivides the text "Hello, World!" into four text segments:
 * "Hello", ",", "World", "!". The space character does not belong to any text segments.
 */
internal interface SegmentFinder {
    /**
     * Returns the character offset of the previous text segment start boundary before the specified
     * character offset, or DONE if there are none.
     */
    fun previousStartBoundary(offset: Int): Int

    /**
     * Returns the character offset of the previous text segment end boundary before the specified
     * character offset, or DONE if there are none.
     */
    fun previousEndBoundary(offset: Int): Int

    /**
     * Returns the character offset of the next text segment start boundary after the specified
     * character offset, or DONE if there are none.
     */
    fun nextStartBoundary(offset: Int): Int

    /**
     * Returns the character offset of the next text segment end boundary after the specified
     * character offset, or DONE if there are none.
     */
    fun nextEndBoundary(offset: Int): Int

    companion object {
        /**
         * Return value of previousStartBoundary(int), previousEndBoundary(int),
         * nextStartBoundary(int), and nextEndBoundary(int) when there are no boundaries of the
         * specified type in the specified direction.
         */
        const val DONE = -1
    }
}

/**
 * Implementation of [SegmentFinder] using words as the text segment. Word boundaries are found
 * using `WordIterator`. Whitespace characters are excluded, so they are not included in any text
 * segments.
 *
 * For example, the text "Hello, World!" would be subdivided into four text segments: "Hello", ",",
 * "World", "!". The space character does not belong to any text segments.
 *
 * @see
 *   [Unicode Text Segmentation - Word Boundaries](https://unicode.org/reports/tr29/.Word_Boundaries)
 */
internal class WordSegmentFinder(
    private val text: CharSequence,
    private val wordIterator: WordIterator
) : SegmentFinder {
    override fun previousStartBoundary(offset: Int): Int {
        var boundary = offset
        do {
            boundary = wordIterator.prevBoundary(boundary)
            if (boundary == BreakIterator.DONE) {
                return DONE
            }
        } while (Character.isWhitespace(text[boundary]))
        return boundary
    }

    override fun previousEndBoundary(offset: Int): Int {
        var boundary = offset
        do {
            boundary = wordIterator.prevBoundary(boundary)
            if (boundary == BreakIterator.DONE || boundary == 0) {
                return DONE
            }
        } while (Character.isWhitespace(text[boundary - 1]))
        return boundary
    }

    override fun nextStartBoundary(offset: Int): Int {
        var boundary = offset
        do {
            boundary = wordIterator.nextBoundary(boundary)
            if (boundary == BreakIterator.DONE || boundary == text.length) {
                return DONE
            }
        } while (Character.isWhitespace(text[boundary]))
        return boundary
    }

    override fun nextEndBoundary(offset: Int): Int {
        var boundary = offset
        do {
            boundary = wordIterator.nextBoundary(boundary)
            if (boundary == BreakIterator.DONE) {
                return DONE
            }
        } while (Character.isWhitespace(text[boundary - 1]))
        return boundary
    }
}

internal abstract class GraphemeClusterSegmentFinder : SegmentFinder {
    /** Return the offset of the previous grapheme bounds before the given [offset]. */
    abstract fun previous(offset: Int): Int

    /** Return the offset of the next grapheme bounds after the given [offset]. */
    abstract fun next(offset: Int): Int

    override fun previousStartBoundary(offset: Int): Int {
        return previous(offset)
    }

    override fun previousEndBoundary(offset: Int): Int {
        val previousBoundary = previous(offset)
        if (previousBoundary == DONE) {
            return DONE
        }

        // Check that there is another cursor position before, otherwise this is not a valid
        // end boundary.
        return if (previous(previousBoundary) == DONE) {
            DONE
        } else {
            previousBoundary
        }
    }

    override fun nextStartBoundary(offset: Int): Int {
        val nextBoundary = next(offset)
        if (nextBoundary == DONE) {
            return DONE
        }

        // Check that there is another cursor position after, otherwise this is not a valid
        // end boundary.
        return if (next(nextBoundary) == DONE) {
            DONE
        } else {
            nextBoundary
        }
    }

    override fun nextEndBoundary(offset: Int): Int {
        return next(offset)
    }
}

internal class GraphemeClusterSegmentFinderUnderApi29(private val text: CharSequence) :
    GraphemeClusterSegmentFinder() {

    private val breakIterator =
        BreakIterator.getCharacterInstance().also { it.setText(text.toString()) }

    override fun previous(offset: Int): Int {
        return breakIterator.preceding(offset)
    }

    override fun next(offset: Int): Int {
        return breakIterator.following(offset)
    }
}

@RequiresApi(29)
internal class GraphemeClusterSegmentFinderApi29(
    private val text: CharSequence,
    private val textPaint: TextPaint
) : GraphemeClusterSegmentFinder() {
    override fun previous(offset: Int): Int {
        // getTextRunCursor will return -1 or DONE when it can't find the previous cursor position.
        return textPaint.getTextRunCursor(text, 0, text.length, false, offset, Paint.CURSOR_BEFORE)
    }

    override fun next(offset: Int): Int {
        // getTextRunCursor will return -1 or DONE when it can't find the next cursor position.
        return textPaint.getTextRunCursor(text, 0, text.length, false, offset, Paint.CURSOR_AFTER)
    }
}

internal fun createGraphemeClusterSegmentFinder(
    text: CharSequence,
    textPaint: TextPaint
): SegmentFinder {
    return if (Build.VERSION.SDK_INT >= 29) {
        GraphemeClusterSegmentFinderApi29(text, textPaint)
    } else {
        GraphemeClusterSegmentFinderUnderApi29(text)
    }
}

@RequiresApi(34)
internal object Api34SegmentFinder {
    internal fun SegmentFinder.toAndroidSegmentFinder(): android.text.SegmentFinder {
        return object : android.text.SegmentFinder() {
            override fun previousStartBoundary(offset: Int): Int =
                this@toAndroidSegmentFinder.previousStartBoundary(offset)

            override fun previousEndBoundary(offset: Int): Int =
                this@toAndroidSegmentFinder.previousEndBoundary(offset)

            override fun nextStartBoundary(offset: Int): Int =
                this@toAndroidSegmentFinder.nextStartBoundary(offset)

            override fun nextEndBoundary(offset: Int): Int =
                this@toAndroidSegmentFinder.nextEndBoundary(offset)
        }
    }
}
