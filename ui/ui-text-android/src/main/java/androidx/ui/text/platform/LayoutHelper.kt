/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.text.platform

import android.text.Layout
import androidx.annotation.IntRange

private const val LINE_FEED = '\n'

/**
 * Provide utilities for Layout class
 *
 * @suppress
 */
@InternalPlatformTextApi
class LayoutHelper(val layout: Layout) {

    private val paragraphEnds: List<Int>

    init {
        var paragraphEnd = 0
        val lineFeeds = mutableListOf<Int>()
        do {
            paragraphEnd = layout.text.indexOf(char = LINE_FEED, startIndex = paragraphEnd)
            if (paragraphEnd < 0) {
                // No more LINE_FEED char found. Use the end of the text as the paragraph end.
                paragraphEnd = layout.text.length
            } else {
                // increment since end offset is exclusive.
                paragraphEnd++
            }
            lineFeeds.add(paragraphEnd)
        } while (paragraphEnd < layout.text.length)
        paragraphEnds = lineFeeds
    }

    /**
     * Retrieve the number of the paragraph in this layout.
     */
    val paragraphCount = paragraphEnds.size

    /**
     * Returns the zero based paragraph number at the offset.
     *
     * The paragraphs are divided by line feed character (U+000A) and line feed character is
     * included in the preceding paragraph, i.e. if the offset points the line feed character,
     * this function returns preceding paragraph index.
     *
     * @param offset a character offset in the text
     * @return the paragraph number
     */
    fun getParagraphForOffset(@IntRange(from = 0) offset: Int): Int =
        paragraphEnds.binarySearch(offset).let { if (it < 0) - (it + 1) else it + 1 }

    /**
     * Returns the inclusive paragraph starting offset of the given paragraph index.
     *
     * @param paragraphIndex a paragraph index.
     * @return an inclusive start character offset of the given paragraph.
     */
    fun getParagraphStart(@IntRange(from = 0) paragraphIndex: Int) =
        if (paragraphIndex == 0) 0 else paragraphEnds[paragraphIndex - 1]

    /**
     * Returns the exclusive paragraph end offset of the given paragraph index.
     *
     * @param paragraphIndex a paragraph index.
     * @return an exclusive end character offset of the given paragraph.
     */
    fun getParagraphEnd(@IntRange(from = 0) paragraphIndex: Int) = paragraphEnds[paragraphIndex]

    /**
     * Returns true if the resolved paragraph direction is RTL, otherwise return false.
     *
     * @param paragraphIndex a paragraph index
     * @return true if the paragraph is RTL, otherwise false
     */
    fun isRTLParagraph(@IntRange(from = 0) paragraphIndex: Int): Boolean {
        val lineNumber = layout.getLineForOffset(getParagraphStart(paragraphIndex))
        return layout.getParagraphDirection(lineNumber) == Layout.DIR_RIGHT_TO_LEFT
    }
}