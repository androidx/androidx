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

package androidx.ui.text.platform.animation

import androidx.ui.text.platform.CharSequenceCharacterIterator
import androidx.ui.text.platform.InternalPlatformTextApi
import androidx.ui.text.platform.LayoutHelper
import java.text.BreakIterator
import java.util.Locale

/**
 * Porvide a segmentation breaker for the text animation.
 * @suppress
 */
@InternalPlatformTextApi
object SegmentBreaker {
    private fun breakInWords(layoutHelper: LayoutHelper): List<Int> {
        val text = layoutHelper.layout.text
        val words = breakWithBreakIterator(text, BreakIterator.getLineInstance(Locale.getDefault()))

        val set = words.toSortedSet()
        for (paraIndex in 0 until layoutHelper.paragraphCount) {
            val bidi = layoutHelper.analyzeBidi(paraIndex) ?: continue
            val paragraphStart = layoutHelper.getParagraphStart(paraIndex)
            for (i in 0 until bidi.runCount) {
                set.add(bidi.getRunStart(i) + paragraphStart)
            }
        }
        return set.toList()
    }

    private fun breakWithBreakIterator(text: CharSequence, breaker: BreakIterator): List<Int> {
        val iter = CharSequenceCharacterIterator(text, 0, text.length)

        val res = mutableListOf(0)
        breaker.setText(iter)
        while (breaker.next() != BreakIterator.DONE) {
            res.add(breaker.current())
        }
        return res
    }

    /**
     * Gets all offsets of the given segment type for animation.
     *
     * @param layoutHelper a layout helper
     * @param segmentType a segmentation type
     * @return all break offsets of the given segmentation type including 0 and text length.
     */
    fun breakOffsets(layoutHelper: LayoutHelper, segmentType: SegmentType): List<Int> {
        val layout = layoutHelper.layout
        val text = layout.text

        return when (segmentType) {
            SegmentType.Document -> listOf(0, text.length)
            SegmentType.Paragraph -> {
                mutableListOf(0).also {
                    for (i in 0 until layoutHelper.paragraphCount) {
                        it.add(layoutHelper.getParagraphEnd(i))
                    }
                }
            }
            SegmentType.Line -> {
                mutableListOf(0).also {
                    for (i in 0 until layout.lineCount) {
                        it.add(layout.getLineEnd(i))
                    }
                }
            }
            SegmentType.Word -> breakInWords(layoutHelper)
            SegmentType.Character -> breakWithBreakIterator(
                text,
                BreakIterator.getCharacterInstance(Locale.getDefault()))
        }
    }
}