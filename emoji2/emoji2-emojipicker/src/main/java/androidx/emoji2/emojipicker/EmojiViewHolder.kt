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
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup.LayoutParams
import android.view.accessibility.AccessibilityEvent
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/** A [ViewHolder] containing an emoji view and emoji data. */
internal class EmojiViewHolder(
    context: Context,
    width: Int,
    height: Int,
    private val stickyVariantProvider: StickyVariantProvider,
    private val onEmojiPickedListener: EmojiViewHolder.(EmojiViewItem) -> Unit,
    private val onEmojiPickedFromPopupListener: EmojiViewHolder.(String) -> Unit
) : ViewHolder(EmojiView(context)) {
    private val onEmojiLongClickListener: OnLongClickListener =
        OnLongClickListener { targetEmojiView ->
            showEmojiPopup(context, targetEmojiView)
        }

    private val emojiView: EmojiView =
        (itemView as EmojiView).apply {
            layoutParams = LayoutParams(width, height)
            isClickable = true
            setOnClickListener {
                it.sendAccessibilityEvent(AccessibilityEvent.TYPE_ANNOUNCEMENT)
                onEmojiPickedListener(emojiViewItem)
            }
        }
    private lateinit var emojiViewItem: EmojiViewItem
    private lateinit var emojiPickerPopupViewController: EmojiPickerPopupViewController

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

    private fun showEmojiPopup(context: Context, clickedEmojiView: View): Boolean {
        val emojiPickerPopupView =
            EmojiPickerPopupView(
                context,
                /* attrs= */ null,
                targetEmojiView = clickedEmojiView,
                targetEmojiItem = emojiViewItem,
                emojiViewOnClickListener = { view ->
                    val emojiPickedInPopup = (view as EmojiView).emoji.toString()
                    onEmojiPickedFromPopupListener(emojiPickedInPopup)
                    onEmojiPickedListener(makeEmojiViewItem(emojiPickedInPopup))
                    // variants[0] is always the base (i.e., primary) emoji
                    stickyVariantProvider.update(emojiViewItem.variants[0], emojiPickedInPopup)
                    emojiPickerPopupViewController.dismiss()
                    // Hover on the base emoji after popup dismissed
                    clickedEmojiView.sendAccessibilityEvent(
                        AccessibilityEvent.TYPE_VIEW_HOVER_ENTER
                    )
                }
            )
        emojiPickerPopupViewController =
            EmojiPickerPopupViewController(context, emojiPickerPopupView, clickedEmojiView)
        emojiPickerPopupViewController.show()
        return true
    }

    private fun makeEmojiViewItem(emoji: String) =
        EmojiViewItem(emoji, BundledEmojiListLoader.getEmojiVariantsLookup()[emoji] ?: listOf())
}
