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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.coroutines.EmptyCoroutineContext
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
    private val scope = CoroutineScope(EmptyCoroutineContext)

    private var recentEmojiProvider: RecentEmojiProvider = DefaultRecentEmojiProvider(context)
    private val recentItems: MutableList<EmojiViewData> = mutableListOf()
    private lateinit var recentItemGroup: ItemGroup

    private lateinit var emojiPickerItems: EmojiPickerItems
    private lateinit var bodyAdapter: EmojiPickerBodyAdapter

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

        scope.launch(Dispatchers.IO) {
            val load = launch { BundledEmojiListLoader.load(context) }
            refreshRecentItems()
            load.join()

            withContext(Dispatchers.Main) {
                showEmojiPickerView()
            }
        }
    }

    private fun createEmojiPickerBodyAdapter(
        emojiPickerItems: EmojiPickerItems,
    ): EmojiPickerBodyAdapter {
        return EmojiPickerBodyAdapter(
            context,
            emojiGridColumns,
            emojiGridRows,
            stickyVariantProvider,
            emojiPickerItems,
            onEmojiPickedListener = { emojiViewItem ->
                onEmojiPickedListener?.accept(emojiViewItem)

                scope.launch {
                    recentEmojiProvider.insert(emojiViewItem.emoji)
                    refreshRecentItems()
                }
            }
        )
    }

    private fun showEmojiPickerView() {
        emojiPickerItems = EmojiPickerItems(buildList {
            add(ItemGroup(
                R.drawable.quantum_gm_ic_access_time_filled_vd_theme_24,
                CategoryTitle(context.getString(R.string.emoji_category_recent)),
                recentItems,
                forceContentSize = DEFAULT_MAX_RECENT_ITEM_ROWS * emojiGridColumns,
                emptyPlaceholderItem = PlaceholderText(
                    context.getString(R.string.emoji_empty_recent_category)
                )
            ).also { recentItemGroup = it })

            for ((headerIconId, name, emojis) in BundledEmojiListLoader.getCategorizedEmojiData()) {
                add(
                    ItemGroup(
                        headerIconId,
                        CategoryTitle(name),
                        emojis.map {
                            EmojiViewData(stickyVariantProvider[it.emoji])
                        },
                    )
                )
            }
        })

        val bodyLayoutManager = GridLayoutManager(
            context,
            emojiGridColumns,
            LinearLayoutManager.VERTICAL,
            /* reverseLayout = */ false
        ).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (emojiPickerItems.getBodyItem(position).occupyEntireRow)
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
                adapter = createEmojiPickerBodyAdapter(emojiPickerItems).also { bodyAdapter = it }
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

    private suspend fun refreshRecentItems() {
        val recent = recentEmojiProvider.getRecentItemList()
        recentItems.clear()
        recentItems.addAll(recent.map {
            EmojiViewData(
                it,
                updateToSticky = false,
            )
        })
    }

    /**
     * This function is used to set the custom behavior after clicking on an emoji icon. Clients
     * could specify their own behavior inside this function.
     */
    fun setOnEmojiPickedListener(onEmojiPickedListener: Consumer<EmojiViewItem>?) {
        this.onEmojiPickedListener = onEmojiPickedListener
    }

    internal fun setRecentEmojiProvider(recentEmojiProvider: RecentEmojiProvider) {
        this.recentEmojiProvider = recentEmojiProvider

        if (::emojiPickerItems.isInitialized) {
            scope.launch {
                refreshRecentItems()
                val range = emojiPickerItems.groupRange(recentItemGroup)

                withContext(Dispatchers.Main) {
                    bodyAdapter.notifyItemRangeChanged(range.first, range.last + 1)
                }
            }
        }
    }
}
