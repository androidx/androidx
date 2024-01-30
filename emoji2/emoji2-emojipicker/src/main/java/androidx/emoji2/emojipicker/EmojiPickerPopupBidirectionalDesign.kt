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
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView

/**
 * Emoji picker popup view with bidirectional UI design to switch emoji to face left or right.
 */
internal class EmojiPickerPopupBidirectionalDesign(
    override val context: Context,
    override val targetEmojiView: View,
    override val variants: List<String>,
    override val popupView: LinearLayout,
    override val emojiViewOnClickListener: View.OnClickListener
) : EmojiPickerPopupDesign() {
    private var emojiFacingLeft = true

    init {
        updateTemplate()
    }
    override fun addLayoutHeader() {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        FrameLayout.inflate(context, R.layout.emoji_picker_popup_bidirectional, row)
            .findViewById<AppCompatImageView>(R.id.emoji_picker_popup_bidirectional_icon)
            .apply {
                layoutParams = LinearLayout.LayoutParams(
                    targetEmojiView.width, targetEmojiView.height)
            }
        popupView.addView(row)
        val imageView =
            row.findViewById<AppCompatImageView>(R.id.emoji_picker_popup_bidirectional_icon)
        imageView.setOnClickListener {
            emojiFacingLeft = !emojiFacingLeft
            updateTemplate()
            popupView.removeViews( /* start= */1, getActualNumberOfRows())
            addRowsToPopupView()
        }
    }

    override fun getNumberOfRows(): Int {
        // Adding one row for the bidirectional switcher.
        return variants.size / 2 / BIDIRECTIONAL_COLUMN_COUNT + 1
    }
    override fun getNumberOfColumns(): Int {
        return BIDIRECTIONAL_COLUMN_COUNT
    }

    private fun getActualNumberOfRows(): Int {
        // Removing one extra row of the bidirectional switcher.
        return getNumberOfRows() - 1
    }

    private fun updateTemplate() {
        template = if (emojiFacingLeft)
            arrayOf((variants.indices.filter { it % 12 < 6 }.map { it + 1 }).toIntArray())
        else
            arrayOf((variants.indices.filter { it % 12 >= 6 }.map { it + 1 }).toIntArray())

        val row = getActualNumberOfRows()
        val column = getNumberOfColumns()
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

    companion object {
        private const val BIDIRECTIONAL_COLUMN_COUNT = 6
    }
}
