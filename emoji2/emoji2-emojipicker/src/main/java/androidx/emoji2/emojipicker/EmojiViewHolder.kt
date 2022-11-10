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

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/** A [ViewHolder] containing an emoji view and emoji data.  */
internal class EmojiViewHolder(
    parent: ViewGroup,
    layoutInflater: LayoutInflater,
    width: Int,
    height: Int
) : ViewHolder(
    layoutInflater
        .inflate(R.layout.emoji_view_holder, parent, /* attachToRoot= */false)
) {
    private val emojiView: EmojiView

    init {
        itemView.layoutParams = LayoutParams(width, height)
        emojiView = itemView.findViewById(R.id.emoji_view)
        emojiView.isClickable = true
    }

    fun bindEmoji(
        emojiViewItem: EmojiViewItem
    ) {
        emojiView.emoji = emojiViewItem.primary
    }
}