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

/** Emoji picker popup view with flat design to list emojis. */
internal class EmojiPickerPopupFlatDesign(
    override val context: Context,
    override val targetEmojiView: View,
    override val variants: List<String>,
    override val popupView: LinearLayout,
    override val emojiViewOnClickListener: View.OnClickListener
) : EmojiPickerPopupDesign() {
    init {
        template = arrayOf(variants.indices.map { it + 1 }.toIntArray())
        var row = getNumberOfRows()
        var column = getNumberOfColumns()
        val overrideTemplate = Array(row) { IntArray(column) }
        var index = 0
        for (i in 0 until row) {
            for (j in 0 until column) {
                if (index < template[0].size) {
                    overrideTemplate[i][j] = template[0][index]
                    index++
                }
            }
        }
        template = overrideTemplate
    }

    override fun getNumberOfRows(): Int {
        val column = getNumberOfColumns()
        return variants.size / column + if (variants.size % column == 0) 0 else 1
    }

    override fun getNumberOfColumns(): Int {
        return minOf(FLAT_COLUMN_MAX_COUNT, template[0].size)
    }

    companion object {
        private const val FLAT_COLUMN_MAX_COUNT = 6
    }
}
