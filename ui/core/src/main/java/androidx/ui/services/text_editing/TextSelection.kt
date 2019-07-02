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

package androidx.ui.services.text_editing

import androidx.ui.core.TextRange

/** A range of text that represents a selection. */
data class TextSelection(
    /**
     * The offset at which the selection originates.
     *
     * Might be larger than, smaller than, or equal to extent.
     */
    val baseOffset: Int,

    /**
     * The offset at which the selection terminates.
     *
     * When the user uses the arrow keys to adjust the selection, this is the
     * value that changes. Similarly, if the current theme paints a caret on one
     * side of the selection, this is the location at which to paint the caret.
     *
     * Might be larger than, smaller than, or equal to base.
     */
    val extentOffset: Int,

    /**
     * Whether this selection has disambiguated its base and extent.
     *
     * On some platforms, the base and extent are not disambiguated until the
     * first time the user adjusts the selection. At that point, either the start
     * or the end of the selection becomes the base and the other one becomes the
     * extent and is adjusted.
     */
    val isDirectional: Boolean = false
) {
    private val range: TextRange = TextRange(
        start = if (baseOffset < extentOffset) baseOffset else extentOffset,
        end = if (baseOffset < extentOffset) extentOffset else baseOffset
    )

    val start: Int
        get() = range.start

    val end: Int
        get() = range.end

    companion object {
        /**
         * Creates a collapsed selection at the given offset.
         *
         * A collapsed selection starts and ends at the same offset, which means it
         * contains zero characters but instead serves as an insertion point in the
         * text.
         */
        fun collapsed(
            offset: Int
        ): TextSelection {
            return TextSelection(
                baseOffset = offset,
                extentOffset = offset,
                isDirectional = false
            )
        }

        /**
         * Creates a collapsed selection at the given text position.
         *
         * A collapsed selection starts and ends at the same offset, which means it
         * contains zero characters but instead serves as an insertion point in the
         * text.
         */
        fun fromPosition(position: Int): TextSelection {
            return collapsed(offset = position)
        }
    }
}
