/*
 * Copyright 2022 The Android Open Source Project
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
import android.view.LayoutInflater
import android.view.View.OnLongClickListener
import android.view.ViewGroup.LayoutParams
import android.view.accessibility.AccessibilityEvent
import android.widget.GridLayout
import android.widget.PopupWindow
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlin.math.roundToInt

/** A [ViewHolder] containing an emoji view and emoji data.  */
internal class EmojiViewHolder(
    context: Context,
    width: Int,
    height: Int,
    private val layoutInflater: LayoutInflater,
    private val stickyVariantProvider: StickyVariantProvider,
    private val onEmojiPickedListener: EmojiViewHolder.(EmojiViewItem) -> Unit,
    private val onEmojiPickedFromPopupListener: EmojiViewHolder.(String) -> Unit
) : ViewHolder(EmojiView(context)) {
    private val onEmojiLongClickListener: OnLongClickListener = OnLongClickListener {
        showPopupWindow(context) {
            PopupViewHelper(context).fillPopupView(
                it,
                emojiView.measuredWidth,
                emojiView.measuredHeight,
                emojiViewItem.variants,
                clickListener = { view ->
                    val emojiPickedInPopup = (view as EmojiView).emoji.toString()
                    onEmojiPickedFromPopupListener(emojiPickedInPopup)
                    onEmojiPickedListener(makeEmojiViewItem(emojiPickedInPopup))
                    // variants[0] is always the base (i.e., primary) emoji
                    stickyVariantProvider.update(emojiViewItem.variants[0], emojiPickedInPopup)
                    dismiss()
                    // Hover on the base emoji after popup dismissed
                    emojiView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_HOVER_ENTER)
                }
            )
        }
        true
    }

    private val emojiView: EmojiView = (itemView as EmojiView).apply {
        layoutParams = LayoutParams(width, height)
        isClickable = true
        setOnClickListener {
            it.sendAccessibilityEvent(AccessibilityEvent.TYPE_ANNOUNCEMENT)
            onEmojiPickedListener(emojiViewItem)
        }
    }
    private lateinit var emojiViewItem: EmojiViewItem

    fun bindEmoji(
        emoji: String,
    ) {
        emojiView.emoji = emoji
        emojiViewItem = makeEmojiViewItem(emoji)

        if (emojiViewItem.variants.isNotEmpty()) {
            emojiView.setOnLongClickListener(onEmojiLongClickListener)
            emojiView.isLongClickable = true
        } else {
            emojiView.setOnLongClickListener(null)
            emojiView.isLongClickable = false
        }
    }

    private fun showPopupWindow(
        context: Context,
        init: PopupWindow.(GridLayout) -> Unit
    ) {
        val popupView = layoutInflater
            .inflate(R.layout.variant_popup, null, false)
            .findViewById<GridLayout>(R.id.variant_popup)
        PopupWindow(popupView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, false).apply {
            init(popupView)
            val location = IntArray(2)
            emojiView.getLocationInWindow(location)
            // Make the popup view center align with the target emoji view.
            val x =
                location[0] + emojiView.width / 2f - popupView.columnCount * emojiView.width / 2f
            val y =
                location[1] - popupView.rowCount * emojiView.height -
                    popupView.paddingBottom - popupView.paddingTop
            // Set background drawable so that the popup window is dismissed properly when clicking
            // outside / scrolling for API < 23.
            setBackgroundDrawable(context.getDrawable(R.drawable.popup_view_rounded_background))
            isOutsideTouchable = true
            isTouchable = true
            animationStyle = R.style.VariantPopupAnimation
            elevation =
                emojiView.context.resources
                    .getDimensionPixelSize(R.dimen.emoji_picker_popup_view_elevation)
                    .toFloat()
            showAtLocation(
                popupView,
                Gravity.NO_GRAVITY,
                x.roundToInt(),
                y
            )
        }
    }

    private fun makeEmojiViewItem(emoji: String) =
        EmojiViewItem(emoji, BundledEmojiListLoader.getEmojiVariantsLookup()[emoji] ?: listOf())
}