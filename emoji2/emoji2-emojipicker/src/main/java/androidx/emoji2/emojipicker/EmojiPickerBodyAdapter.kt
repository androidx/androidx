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
import androidx.annotation.UiThread
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.util.Consumer
import androidx.emoji2.emojipicker.EmojiPickerConstants.RECENT_CATEGORY_INDEX
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.tracing.Trace

/** RecyclerView adapter for emoji body.  */
internal class EmojiPickerBodyAdapter(
    private val context: Context,
    private val emojiGridColumns: Int,
    private val emojiGridRows: Float,
    private val categoryNames: Array<String>,
    private val variantToBaseEmojiMap: Map<String, String>,
    private val baseToVariantsEmojiMap: Map<String, List<String>>,
    private val stickyVariantProvider: StickyVariantProvider,
    private val onEmojiPickedListener: Consumer<EmojiViewItem>?,
    private val recentEmojiList: MutableList<String>,
    private val recentEmojiProvider: RecentEmojiProvider
) : Adapter<ViewHolder>() {
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
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
            when (viewType) {
                CategorySeparatorViewData.TYPE -> {
                    view = layoutInflater.inflate(
                        R.layout.category_text_view,
                        parent,
                        /* attachToRoot = */ false
                    )
                    view.layoutParams =
                        LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                }

                EmptyCategoryViewData.TYPE -> {
                    view = layoutInflater.inflate(
                        R.layout.emoji_picker_empty_category_text_view,
                        parent,
                        /* attachToRoot = */ false
                    )
                    view.layoutParams =
                        LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    view.minimumHeight = (parent.measuredHeight / emojiGridRows).toInt()
                }

                EmojiViewData.TYPE -> {
                    return EmojiViewHolder(
                        parent,
                        layoutInflater,
                        getParentWidth(parent) / emojiGridColumns,
                        (parent.measuredHeight / emojiGridRows).toInt(),
                        stickyVariantProvider,
                        onEmojiPickedListener = { emojiViewItem ->
                            recentEmojiProvider.insert(emojiViewItem.emoji)
                            // update the recentEmojiList in the mean time
                            recentEmojiList.remove(emojiViewItem.emoji)
                            recentEmojiList.add(0, emojiViewItem.emoji)
                            onEmojiPickedListener?.accept(emojiViewItem)
                            // update the recent category to reload
                            this@EmojiPickerBodyAdapter.updateRecent(recentEmojiList.map { emoji ->
                                EmojiViewData(
                                    RECENT_CATEGORY_INDEX,
                                    recentEmojiList.indexOf(emoji),
                                    emoji,
                                    baseToVariantsEmojiMap[variantToBaseEmojiMap[emoji]]
                                        ?.toTypedArray()
                                        ?: arrayOf()
                                )
                            })
                        },
                        onEmojiPickedFromPopupListener = { emoji ->
                            (flattenSource[bindingAdapterPosition] as EmojiViewData).primary = emoji
                            notifyItemChanged(bindingAdapterPosition)
                        }
                    )
                }

                DummyViewData.TYPE -> {
                    view = View(context)
                    view.layoutParams = LayoutParams(
                        getParentWidth(parent) / emojiGridColumns,
                        (parent.measuredHeight / emojiGridRows).toInt()
                    )
                }

                else -> {
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
            if (categoryIndex == RECENT_CATEGORY_INDEX) {
                categoryLabel.text = context.getString(R.string.emoji_category_recent)
                categoryLabel.visibility = View.VISIBLE
            } else {
                if (categoryName.isEmpty()) {
                    categoryLabel.visibility = View.GONE
                } else {
                    categoryLabel.text = categoryName
                    categoryLabel.visibility = View.VISIBLE
                }
            }
        } else if (viewType == EmptyCategoryViewData.TYPE) {
            // Show empty category description.
            val emptyCategoryView =
                view.findViewById<AppCompatTextView>(R.id.emoji_picker_empty_category_view)
            val item = flattenSource[position] as EmptyCategoryViewData
            var content = item.description
            if (content.isEmpty()) {
                val categoryIndex = flattenSource.getCategoryIndex(position)
                content = context.getString(
                    if (categoryIndex == RECENT_CATEGORY_INDEX)
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
        return flattenSource.size
    }

    override fun getItemViewType(position: Int): Int {
        return flattenSource[position].type
    }

    private fun getParentWidth(parent: ViewGroup): Int {
        return parent.measuredWidth - parent.paddingLeft - parent.paddingRight
    }

    fun updateEmojis(emojis: List<List<ItemViewData>>) {
        flattenSource = ItemViewDataFlatList(
            emojis,
            emojiGridColumns
        )
        notifyDataSetChanged()
    }

    fun updateRecent(recents: List<ItemViewData>) {
        flattenSource.updateSourcesByIndex(RECENT_CATEGORY_INDEX, recents)
        notifyItemRangeChanged(
            RECENT_CATEGORY_INDEX,
            flattenSource.getCategorySize(RECENT_CATEGORY_INDEX)
        )
    }
}