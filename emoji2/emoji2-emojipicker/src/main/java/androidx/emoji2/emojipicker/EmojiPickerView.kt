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
import androidx.core.view.ViewCompat
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
        onEmojiPickedListener: Consumer<EmojiViewItem>?,
        recentEmojiProvider: RecentEmojiProvider,
        recentItems: MutableList<EmojiViewData>,
        emojiPickerItems: EmojiPickerItems,
    ): EmojiPickerBodyAdapter {
        val adapter = EmojiPickerBodyAdapter(
            context,
            emojiGridColumns,
            emojiGridRows,
            stickyVariantProvider,
            emojiPickerItems,
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
        val categorizedEmojiData = BundledEmojiListLoader.getCategorizedEmojiData()
        val variantToBaseEmojiMap = BundledEmojiListLoader.getPrimaryEmojiLookup()
        val baseToVariantsEmojiMap = BundledEmojiListLoader.getEmojiVariantsLookup()
        val recentItems = recent.map {
            EmojiViewData(
                it,
                baseToVariantsEmojiMap[variantToBaseEmojiMap[it]] ?: listOf(),
                updateToSticky = false,
            )
        }.toMutableList()
        val emojiPickerItems = EmojiPickerItems(buildList {
            add(
                ItemGroup(
                    R.drawable.quantum_gm_ic_access_time_filled_vd_theme_24,
                    CategoryTitle(context.getString(R.string.emoji_category_recent)),
                    recentItems,
                    forceContentSize = DEFAULT_MAX_RECENT_ITEM_ROWS * emojiGridColumns,
                    emptyPlaceholderItem = PlaceholderText(
                        context.getString(R.string.emoji_empty_recent_category)
                    ),
                )
            )

            for ((headerIconId, name, emojis) in categorizedEmojiData) {
                add(
                    ItemGroup(
                        headerIconId,
                        CategoryTitle(name),
                        emojis.map {
                            EmojiViewData(stickyVariantProvider[it.emoji], it.variants)
                        },
                    )
                )
            }
        })

        val bodyAdapter = createEmojiPickerBodyAdapter(
            onEmojiPickedListener,
            recentEmojiProvider,
            recentItems,
            emojiPickerItems
        )

        val bodyLayoutManager = GridLayoutManager(
            getContext(),
            emojiGridColumns,
            LinearLayoutManager.VERTICAL,
            /* reverseLayout = */ false
        ).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (bodyAdapter.emojiPickerItems.getBodyItem(position).occupyEntireRow)
                        emojiGridColumns
                    else 1
                }
            }
        }

        val headerAdapter =
            EmojiPickerHeaderAdapter(context, emojiPickerItems, onHeaderIconClicked = {
                bodyLayoutManager.scrollToPositionWithOffset(
                    emojiPickerItems.firstItemPositionByGroupIndex(it),
                    0
                )
            })

        with(inflate(context, R.layout.emoji_picker, this)) {
            // set headerView
            ViewCompat.requireViewById<RecyclerView>(this, R.id.emoji_picker_header).apply {
                layoutManager =
                    LinearLayoutManager(
                        context,
                        LinearLayoutManager.HORIZONTAL,
                        /* reverseLayout = */ false
                    )
                adapter = headerAdapter
            }

            // set bodyView
            ViewCompat.requireViewById<RecyclerView>(this, R.id.emoji_picker_body).apply {
                layoutManager = bodyLayoutManager
                adapter = createEmojiPickerBodyAdapter(
                    onEmojiPickedListener,
                    recentEmojiProvider,
                    recentItems,
                    emojiPickerItems
                )
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        val position =
                            bodyLayoutManager.findFirstCompletelyVisibleItemPosition()
                        headerAdapter.selectedGroupIndex =
                            emojiPickerItems.groupIndexByItemPosition(position)
                    }
                })
            }
        }
    }

    /**
     * This function is used to set the custom behavior after clicking on an emoji icon. Clients
     * could specify their own behavior inside this function.
     */
    fun setOnEmojiPickedListener(onEmojiPickedListener: Consumer<EmojiViewItem>?) {
        this.onEmojiPickedListener = onEmojiPickedListener
    }
}
