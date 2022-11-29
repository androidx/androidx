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
import android.content.res.TypedArray
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.util.Consumer
import androidx.emoji2.emojipicker.EmojiPickerConstants.DEFAULT_MAX_RECENT_ITEM_ROWS
import androidx.emoji2.emojipicker.Extensions.toEmojiViewData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The emoji picker view that provides up-to-date emojis in a vertical scrollable view with a
 * clickable horizontal header.
 */
class EmojiPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) :
    FrameLayout(context, attrs) {
    /**
     * This is the number of rows displayed in emoji picker. Some apps like Gboard have their
     * default values(e.g. 7.5f which means there are height of 7.5 rows displayed on the UI).
     * Clients could specify this value by their own. The default value will be used if
     * emojiGridRows is set to non-positive value. Float value indicates that we will display
     * partial of the last row and have content down, so the users get the idea that they can scroll
     * down for more contents.
     */
    var emojiGridRows: Float = EmojiPickerConstants.DEFAULT_BODY_ROWS
        set(value) {
            field = if (value > 0) value else EmojiPickerConstants.DEFAULT_BODY_ROWS
        }

    /**
     * This is the number of columns of emoji picker. Some apps like Gboard have their default
     * values(e.g. 9). Clients could specify this value by their own. The default value will be used
     * if emojiGridColumns is set to non-positive value.
     */
    var emojiGridColumns: Int = EmojiPickerConstants.DEFAULT_BODY_COLUMNS
        set(value) {
            field = if (value > 0) value else EmojiPickerConstants.DEFAULT_BODY_COLUMNS
        }

    private val stickyVariantProvider = StickyVariantProvider(context)
    private var recentEmojiProvider = DefaultRecentEmojiProvider(context)

    private lateinit var headerView: RecyclerView
    private lateinit var bodyView: RecyclerView
    private var onEmojiPickedListener: Consumer<EmojiViewItem>? = null

    init {
        val typedArray: TypedArray =
            context.obtainStyledAttributes(attrs, R.styleable.EmojiPickerView, 0, 0)
        emojiGridRows = typedArray.getFloat(
            R.styleable.EmojiPickerView_emojiGridRows,
            EmojiPickerConstants.DEFAULT_BODY_ROWS
        )
        emojiGridColumns = typedArray.getInt(
            R.styleable.EmojiPickerView_emojiGridColumns,
            EmojiPickerConstants.DEFAULT_BODY_COLUMNS
        )
        typedArray.recycle()

        CoroutineScope(Dispatchers.IO).launch {
            val load = launch { BundledEmojiListLoader.load(context) }
            val recent = recentEmojiProvider.getRecentItemList()
            load.join()

            withContext(Dispatchers.Main) {
                showEmojiPickerView(context, recent)
            }
        }
    }

    private fun createEmojiPickerBodyAdapter(
        recent: List<String>,
        categorizedEmojiData: List<BundledEmojiListLoader.EmojiDataCategory>,
        variantToBaseEmojiMap: Map<String, String>,
        baseToVariantsEmojiMap: Map<String, List<String>>,
        onEmojiPickedListener: Consumer<EmojiViewItem>?,
        recentEmojiProvider: RecentEmojiProvider
    ): EmojiPickerBodyAdapter {
        val recentItems = recent.map {
            EmojiViewData(
                it,
                baseToVariantsEmojiMap[variantToBaseEmojiMap[it]] ?: listOf(),
                updateToVariants = false,
            )
        }.toMutableList()
        val recentGroup = ItemGroup(
            CategoryTitle(context.getString(R.string.emoji_category_recent)),
            recentItems,
            forceContentSize = DEFAULT_MAX_RECENT_ITEM_ROWS * emojiGridColumns,
            emptyPlaceholderItem = PlaceholderText(
                context.getString(R.string.emoji_empty_recent_category)
            ),
        )
        val itemViewDataFlatList =
            ItemViewDataFlatList(listOf(recentGroup) + categorizedEmojiData.map { (name, emojis) ->
                ItemGroup(
                    CategoryTitle(name),
                    emojis.map {
                        EmojiViewData(stickyVariantProvider[it.emoji], it.variants)
                    },
                )
            })
        val adapter = EmojiPickerBodyAdapter(
            context,
            emojiGridColumns,
            emojiGridRows,
            stickyVariantProvider,
            itemViewDataFlatList,
            onEmojiPickedListener = { emojiViewItem ->
                onEmojiPickedListener?.accept(emojiViewItem)
                recentItems.indexOfFirst { it.primary == emojiViewItem.emoji }
                    .takeIf { it >= 0 }?.let { recentItems.removeAt(it) }
                recentItems.add(0, emojiViewItem.toEmojiViewData(updateToVariants = false))
                recentEmojiProvider.insert(emojiViewItem.emoji)
            }
        )
        return adapter
    }

    private fun showEmojiPickerView(context: Context, recent: List<String>) {
        // get emoji picker
        val emojiPicker = inflate(context, R.layout.emoji_picker, this)

        // set headerView
        headerView = emojiPicker.findViewById(R.id.emoji_picker_header)
        headerView.layoutManager =
            LinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL,
                /* reverseLayout = */ false
            )
        headerView.adapter = EmojiPickerHeaderAdapter(context)

        // set bodyView
        bodyView = emojiPicker.findViewById(R.id.emoji_picker_body)
        bodyView.layoutManager = GridLayoutManager(
            getContext(),
            emojiGridColumns,
            LinearLayoutManager.VERTICAL,
            /* reverseLayout = */ false
        ).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val adapter = (bodyView.adapter as? EmojiPickerBodyAdapter) ?: return 1
                    return if (adapter.flattenSource[position].occupyEntireRow)
                        emojiGridColumns
                    else 1
                }
            }
        }
        val categorizedEmojiData = BundledEmojiListLoader.getCategorizedEmojiData()
        val variantToBaseEmojiMap = BundledEmojiListLoader.getPrimaryEmojiLookup()
        val baseToVariantsEmojiMap = BundledEmojiListLoader.getEmojiVariantsLookup()
        bodyView.adapter =
            createEmojiPickerBodyAdapter(
                recent,
                categorizedEmojiData,
                variantToBaseEmojiMap,
                baseToVariantsEmojiMap,
                onEmojiPickedListener,
                recentEmojiProvider
            )
    }

    /**
     * This function is used to set the custom behavior after clicking on an emoji icon. Clients
     * could specify their own behavior inside this function.
     */
    fun setOnEmojiPickedListener(onEmojiPickedListener: Consumer<EmojiViewItem>?) {
        this.onEmojiPickedListener = onEmojiPickedListener
    }
}
