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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.annotation.UiThread
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.tracing.Trace

/** RecyclerView adapter for emoji body.  */
internal class EmojiPickerBodyAdapter(
    context: Context,
    private val emojiGridColumns: Int,
    private val emojiGridRows: Float
) : RecyclerView.Adapter<ViewHolder>() {
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    private val context = context

    @UiThread
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Trace.beginSection("EmojiPickerBodyAdapter.onCreateViewHolder")
        try {
            // TODO: Load real emoji data in the next change
            val view: View = layoutInflater.inflate(
                R.layout.emoji_picker_empty_category_text_view, parent,
                /*attachToRoot= */ false
            )
            view.layoutParams = LayoutParams(
                parent.width / emojiGridColumns, (parent.measuredHeight / emojiGridRows).toInt()
            )
            view.minimumHeight = (parent.measuredHeight / emojiGridRows).toInt()
            return object : ViewHolder(view) {}
        } finally {
            Trace.endSection()
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val emptyCategoryView: AppCompatTextView =
            viewHolder.itemView.findViewById(R.id.emoji_picker_empty_category_view)
        emptyCategoryView.setText(R.string.emoji_empty_non_recent_category)
    }

    override fun getItemCount(): Int {
        return (emojiGridColumns * emojiGridRows).toInt()
    }
}