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

package androidx.emoji2.emojipicker

import android.content.Context
import android.view.View
import android.widget.LinearLayout

/** Emoji picker popup view with square design. */
internal class EmojiPickerPopupSquareDesign(
    override val context: Context,
    override val targetEmojiView: View,
    override val variants: List<String>,
    override val popupView: LinearLayout,
    override val emojiViewOnClickListener: View.OnClickListener
) : EmojiPickerPopupDesign() {
    init {
        template = SQUARE_LAYOUT_TEMPLATE
    }

    override fun getNumberOfRows(): Int {
        return SQUARE_LAYOUT_TEMPLATE.size
    }

    override fun getNumberOfColumns(): Int {
        return SQUARE_LAYOUT_TEMPLATE[0].size
    }

    companion object {
        /**
         * Square variant layout template without skin tone. 0 : a place holder Positive number is
         * the index + 1 in the variant array
         */
        private val SQUARE_LAYOUT_TEMPLATE =
            arrayOf(
                intArrayOf(0, 2, 3, 4, 5, 6),
                intArrayOf(0, 7, 8, 9, 10, 11),
                intArrayOf(0, 12, 13, 14, 15, 16),
                intArrayOf(0, 17, 18, 19, 20, 21),
                intArrayOf(1, 22, 23, 24, 25, 26)
            )
    }
}
