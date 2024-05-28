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
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout

/** Popup view for emoji picker to show emoji variants. */
internal class EmojiPickerPopupView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int = 0,
    private val targetEmojiView: View,
    private val targetEmojiItem: EmojiViewItem,
    private val emojiViewOnClickListener: OnClickListener
) : FrameLayout(context, attrs, defStyleAttr) {

    private val variants = targetEmojiItem.variants
    private val targetEmoji = targetEmojiItem.emoji
    private val popupView: LinearLayout
    private val popupDesign: EmojiPickerPopupDesign

    init {
        popupView =
            inflate(context, R.layout.variant_popup, /* root= */ null)
                .findViewById<LinearLayout>(R.id.variant_popup)
        val layout = getLayout()
        popupDesign =
            when (layout) {
                Layout.FLAT ->
                    EmojiPickerPopupFlatDesign(
                        context,
                        targetEmojiView,
                        variants,
                        popupView,
                        emojiViewOnClickListener
                    )
                Layout.SQUARE ->
                    EmojiPickerPopupSquareDesign(
                        context,
                        targetEmojiView,
                        variants,
                        popupView,
                        emojiViewOnClickListener
                    )
                Layout.SQUARE_WITH_SKIN_TONE_CIRCLE ->
                    EmojiPickerPopupMultiSkintoneDesign(
                        context,
                        targetEmojiView,
                        variants,
                        popupView,
                        emojiViewOnClickListener,
                        targetEmoji
                    )
                Layout.BIDIRECTIONAL ->
                    EmojiPickerPopupBidirectionalDesign(
                        context,
                        targetEmojiView,
                        variants,
                        popupView,
                        emojiViewOnClickListener
                    )
            }
        popupDesign.addLayoutHeader()
        popupDesign.addRowsToPopupView()
        popupDesign.addLayoutFooter()
        addView(popupView)
    }

    fun getPopupViewWidth(): Int {
        return popupDesign.getNumberOfColumns() * targetEmojiView.width +
            popupView.paddingStart +
            popupView.paddingEnd
    }

    fun getPopupViewHeight(): Int {
        return popupDesign.getNumberOfRows() * targetEmojiView.height +
            popupView.paddingTop +
            popupView.paddingBottom
    }

    private fun getLayout(): Layout {
        if (variants.size == SQUARE_LAYOUT_VARIANT_COUNT)
            if (SQUARE_LAYOUT_EMOJI_NO_SKIN_TONE.contains(variants[0])) return Layout.SQUARE
            else return Layout.SQUARE_WITH_SKIN_TONE_CIRCLE
        else if (variants.size == BIDIRECTIONAL_VARIANTS_COUNT) return Layout.BIDIRECTIONAL
        else return Layout.FLAT
    }

    companion object {
        private enum class Layout {
            FLAT,
            SQUARE,
            SQUARE_WITH_SKIN_TONE_CIRCLE,
            BIDIRECTIONAL
        }

        /**
         * The number of variants expected when using a square layout strategy. Square layouts are
         * comprised of a 5x5 grid + the base variant.
         */
        private const val SQUARE_LAYOUT_VARIANT_COUNT = 26

        /**
         * The number of variants expected when using a bidirectional layout strategy. Bidirectional
         * layouts are comprised of bidirectional icon and a 3x6 grid with left direction emojis as
         * default. After clicking the bidirectional icon, it switches to a bidirectional icon and a
         * 3x6 grid with right direction emojis.
         */
        private const val BIDIRECTIONAL_VARIANTS_COUNT = 36

        // Set of emojis that use the square layout without skin tone swatches.
        private val SQUARE_LAYOUT_EMOJI_NO_SKIN_TONE = setOf("👪")
    }
}
