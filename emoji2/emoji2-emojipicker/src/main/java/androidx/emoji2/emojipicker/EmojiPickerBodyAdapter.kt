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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.annotation.IntRange
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.tracing.Trace

/** RecyclerView adapter for emoji body.  */
internal class EmojiPickerBodyAdapter(
    context: Context,
    private val emojiGridColumns: Int,
    private val emojiGridRows: Float,
    private val categoryNames: Array<String>
) : Adapter<ViewHolder>() {
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    private val context = context

    private var flattenSource: ItemViewDataFlatList

    init {
        val categorizedEmojis: MutableList<MutableList<ItemViewData>> = mutableListOf()
        for (i in categoryNames.indices) {
            categorizedEmojis.add(mutableListOf())
        }
        flattenSource = ItemViewDataFlatList(
            categorizedEmojis,
            emojiGridColumns
        )
    }

    @UiThread
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Trace.beginSection("EmojiPickerBodyAdapter.onCreateViewHolder")
        return try {
            val view: View
            if (viewType == CategorySeparatorViewData.TYPE) {
                view = layoutInflater.inflate(
                    R.layout.category_text_view,
                    parent,
                    /* attachToRoot= */ false
                )
                view.layoutParams =
                    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            } else if (viewType == EmptyCategoryViewData.TYPE) {
                view = layoutInflater.inflate(
                    R.layout.emoji_picker_empty_category_text_view,
                    parent,
                    /* attachToRoot= */ false
                )
                view.layoutParams =
                    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                view.minimumHeight = (parent.measuredHeight / emojiGridRows).toInt()
            } else if (viewType == EmojiViewData.TYPE) {
                return EmojiViewHolder(
                    parent,
                    layoutInflater,
                    getParentWidth(parent) / emojiGridColumns,
                    (parent.measuredHeight / emojiGridRows).toInt(),
                )
            } else if (viewType == DummyViewData.TYPE) {
                view = View(context)
                view.layoutParams = LayoutParams(
                    getParentWidth(parent) / emojiGridColumns,
                    (parent.measuredHeight / emojiGridRows).toInt()
                )
            } else {
                Log.e(
                    "EmojiPickerBodyAdapter",
                    "EmojiPickerBodyAdapter gets unsupported view type."
                )
                view = View(context)
                view.layoutParams =
                    LayoutParams(
                        getParentWidth(parent) / emojiGridColumns,
                        (parent.measuredHeight / emojiGridRows).toInt()
                    )
            }
            object : ViewHolder(view) {}
        } finally {
            Trace.endSection()
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val viewType = viewHolder.itemViewType
        val view = viewHolder.itemView
        if (viewType == CategorySeparatorViewData.TYPE) {
            val categoryIndex = flattenSource.getCategoryIndex(position)
            val item = flattenSource[position] as CategorySeparatorViewData
            var categoryName = item.categoryName
            if (categoryName.isEmpty()) {
                categoryName = categoryNames[categoryIndex]
            }
            // Show category label.
            val categoryLabel = view.findViewById<AppCompatTextView>(R.id.category_name)
            if (categoryName.isEmpty()) {
                categoryLabel.visibility = View.GONE
            } else {
                categoryLabel.text = categoryName
                categoryLabel.visibility = View.VISIBLE
            }
        } else if (viewType == EmptyCategoryViewData.TYPE) {
            // Show empty category description.
            val emptyCategoryView =
                view.findViewById<AppCompatTextView>(R.id.emoji_picker_empty_category_view)
            val item = flattenSource[position] as EmptyCategoryViewData
            var content = item.description
            if (content.isEmpty()) {
                val categoryIndex: Int = getCategoryIndex(position)
                content = context.getString(
                    if (categoryIndex == EmojiPickerConstants.RECENT_CATEGORY_INDEX)
                        R.string.emoji_empty_recent_category
                    else R.string.emoji_empty_non_recent_category
                )
            }
            emptyCategoryView.text = content
        } else if (viewType == EmojiViewData.TYPE) {
            val item = flattenSource[position] as EmojiViewData
            val emojiViewHolder = viewHolder as EmojiViewHolder
            emojiViewHolder.bindEmoji(
                EmojiViewItem(
                    item.primary,
                    item.secondaries.toList()
                )
            )
        }
    }

    override fun getItemCount(): Int {
        return (emojiGridColumns * emojiGridRows).toInt()
    }

    override fun getItemViewType(position: Int): Int {
        return flattenSource[position].type
    }

    @IntRange(from = 0)
    fun getCategoryIndex(@IntRange(from = 0) position: Int): Int {
        return getFlattenSource().getCategoryIndex(position)
    }

    @VisibleForTesting
    fun getFlattenSource(): ItemViewDataFlatList {
        return flattenSource
    }

    fun getParentWidth(parent: ViewGroup): Int {
        return parent.measuredWidth - parent.paddingLeft - parent.paddingRight
    }

    internal fun updateEmojis(emojis: List<List<ItemViewData>>) {
        flattenSource = ItemViewDataFlatList(
            emojis,
            emojiGridColumns
        )
        notifyDataSetChanged()
    }
}