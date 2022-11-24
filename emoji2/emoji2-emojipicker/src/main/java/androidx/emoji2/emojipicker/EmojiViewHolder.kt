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

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlin.math.roundToInt

/** A [ViewHolder] containing an emoji view and emoji data.  */
internal class EmojiViewHolder(
    parent: ViewGroup,
    layoutInflater: LayoutInflater,
    width: Int,
    height: Int,
    stickyVariantProvider: StickyVariantProvider,
    onEmojiPickedListener: EmojiViewHolder.(EmojiViewItem) -> Unit,
    onEmojiPickedFromPopupListener: EmojiViewHolder.(String) -> Unit
) : ViewHolder(
    layoutInflater
        .inflate(R.layout.emoji_view_holder, parent, /* attachToRoot = */false)
) {
    private val onEmojiClickListener: OnClickListener = OnClickListener { v ->
        v.findViewById<EmojiView>(R.id.emoji_view).emoji?.let {
            onEmojiPickedListener(EmojiViewItem(it.toString(), emojiViewItem.variants))
        }
    }

    private val onEmojiLongClickListener: OnLongClickListener = OnLongClickListener {
        val variants = emojiViewItem.variants
        val popupView = layoutInflater
            .inflate(R.layout.variant_popup, null, false)
            .findViewById<GridLayout>(R.id.variant_popup)
            .apply {
                // Show 6 emojis in one row at most
                this.columnCount = minOf(6, variants.size)
                this.rowCount =
                    variants.size / this.columnCount +
                        if (variants.size % this.columnCount == 0) 0 else 1
                this.orientation = GridLayout.HORIZONTAL
            }
        val popupWindow = showPopupWindow(emojiView, popupView)
        for (v in variants) {
            // Add variant emoji view to the popup view
            layoutInflater
                .inflate(R.layout.emoji_view_holder, null, false).apply {
                    this as FrameLayout
                    (getChildAt(0) as EmojiView).emoji = v
                    setOnClickListener {
                        onEmojiPickedFromPopupListener(this@EmojiViewHolder, v)
                        onEmojiClickListener.onClick(it)
                        // variants[0] is always the base (i.e., primary) emoji
                        stickyVariantProvider.update(variants[0], v)
                        popupWindow.dismiss()
                        // Hover on the base emoji after popup dismissed
                        emojiView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_HOVER_ENTER)
                    }
                }.also {
                    popupView.addView(it)
                    it.layoutParams.width = emojiView.measuredWidth
                    it.layoutParams.height = emojiView.measuredHeight
                }
        }
        popupView.post {
            // Hover on the first emoji in the popup
            (popupView.getChildAt(0) as FrameLayout).getChildAt(0)
                .sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_HOVER_ENTER)
        }
        true
    }

    private val emojiView: EmojiView
    private val indicator: ImageView
    private lateinit var emojiViewItem: EmojiViewItem

    init {
        itemView.layoutParams = LayoutParams(width, height)
        emojiView = itemView.findViewById(R.id.emoji_view)
        emojiView.isClickable = true
        emojiView.setOnClickListener(onEmojiClickListener)
        indicator = itemView.findViewById(R.id.variant_availability_indicator)
    }

    fun bindEmoji(
        emojiViewItem: EmojiViewItem,
    ) {
        emojiView.emoji = emojiViewItem.emoji
        this.emojiViewItem = emojiViewItem

        if (emojiViewItem.variants.isNotEmpty()) {
            indicator.visibility = VISIBLE
            emojiView.setOnLongClickListener(onEmojiLongClickListener)
            emojiView.isLongClickable = true
        } else {
            indicator.visibility = GONE
            emojiView.setOnLongClickListener(null)
            emojiView.isLongClickable = false
        }
    }

    private fun showPopupWindow(
        parent: EmojiView,
        popupView: GridLayout
    ): PopupWindow {
        val location = IntArray(2)
        parent.getLocationInWindow(location)
        // Make the popup view center align with the target emoji view.
        val x =
            location[0] + parent.width / 2f - popupView.columnCount * parent.width / 2f
        val y = location[1] - popupView.rowCount * parent.height
        return PopupWindow(
            popupView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, false
        ).also {
            it.isOutsideTouchable = true
            it.isTouchable = true
            it.animationStyle = R.style.VariantPopupAnimation
            it.showAtLocation(
                parent,
                Gravity.NO_GRAVITY,
                x.roundToInt(),
                y
            )
        }
    }
}