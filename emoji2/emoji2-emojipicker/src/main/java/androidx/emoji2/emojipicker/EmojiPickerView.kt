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
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.util.Consumer
import androidx.core.view.ViewCompat
import androidx.emoji2.emojipicker.EmojiPickerConstants.DEFAULT_MAX_RECENT_ITEM_ROWS
import androidx.emoji2.text.EmojiCompat
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
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    FrameLayout(context, attrs, defStyleAttr) {

    internal companion object {
        internal var emojiCompatLoaded: Boolean = false
    }

    private var _emojiGridRows: Float? = null
    /**
     * The number of rows of the emoji picker.
     *
     * Optional field. If not set, the value will be calculated based on parent view height and
     * [emojiGridColumns].
     * Float value indicates that the picker could display the last row partially, so the users get
     * the idea that they can scroll down for more contents.
     * @attr ref androidx.emoji2.emojipicker.R.styleable.EmojiPickerView_emojiGridRows
     */
    var emojiGridRows: Float
        get() = _emojiGridRows ?: -1F
        set(value) {
            _emojiGridRows = value.takeIf { it > 0 }
            // Refresh when emojiGridRows is reset
            if (isLaidOut) {
                showEmojiPickerView()
            }
        }

    /**
     * The number of columns of the emoji picker.
     *
     * Default value([EmojiPickerConstants.DEFAULT_BODY_COLUMNS]: 9) will be used if
     * emojiGridColumns is set to non-positive value.
     * @attr ref androidx.emoji2.emojipicker.R.styleable.EmojiPickerView_emojiGridColumns
     */
    var emojiGridColumns: Int = EmojiPickerConstants.DEFAULT_BODY_COLUMNS
        set(value) {
            field = value.takeIf { it > 0 } ?: EmojiPickerConstants.DEFAULT_BODY_COLUMNS
            // Refresh when emojiGridColumns is reset
            if (isLaidOut) {
                showEmojiPickerView()
            }
        }

    private val stickyVariantProvider = StickyVariantProvider(context)
    private val scope = CoroutineScope(EmptyCoroutineContext)

    private var recentEmojiProvider: RecentEmojiProvider = DefaultRecentEmojiProvider(context)
    private var recentNeedsRefreshing: Boolean = true
    private val recentItems: MutableList<EmojiViewData> = mutableListOf()
    private lateinit var recentItemGroup: ItemGroup

    private lateinit var emojiPickerItems: EmojiPickerItems
    private lateinit var bodyAdapter: EmojiPickerBodyAdapter

    private var onEmojiPickedListener: Consumer<EmojiViewItem>? = null

    init {
        val typedArray: TypedArray =
            context.obtainStyledAttributes(attrs, R.styleable.EmojiPickerView, 0, 0)
        _emojiGridRows = with(R.styleable.EmojiPickerView_emojiGridRows) {
            if (typedArray.hasValue(this)) {
                typedArray.getFloat(this, 0F)
            } else null
        }
        emojiGridColumns = typedArray.getInt(
            R.styleable.EmojiPickerView_emojiGridColumns,
            EmojiPickerConstants.DEFAULT_BODY_COLUMNS
        )
        typedArray.recycle()

        if (EmojiCompat.isConfigured()) {
            when (EmojiCompat.get().loadState) {
                EmojiCompat.LOAD_STATE_SUCCEEDED -> emojiCompatLoaded = true

                EmojiCompat.LOAD_STATE_LOADING, EmojiCompat.LOAD_STATE_DEFAULT ->
                    EmojiCompat.get().registerInitCallback(object : EmojiCompat.InitCallback() {
                        override fun onInitialized() {
                            emojiCompatLoaded = true
                            scope.launch(Dispatchers.IO) {
                                BundledEmojiListLoader.load(context)
                                withContext(Dispatchers.Main) {
                                    emojiPickerItems = buildEmojiPickerItems()
                                    bodyAdapter.notifyDataSetChanged()
                                }
                            }
                        }

                        override fun onFailed(throwable: Throwable?) {}
                    })
            }
        }
        scope.launch(Dispatchers.IO) {
            val load = launch { BundledEmojiListLoader.load(context) }
            refreshRecent()
            load.join()

            withContext(Dispatchers.Main) {
                showEmojiPickerView()
            }
        }
    }

    private fun createEmojiPickerBodyAdapter(): EmojiPickerBodyAdapter {
        return EmojiPickerBodyAdapter(
            context,
            emojiGridColumns,
            _emojiGridRows,
            stickyVariantProvider,
            emojiPickerItemsProvider = { emojiPickerItems },
            onEmojiPickedListener = { emojiViewItem ->
                onEmojiPickedListener?.accept(emojiViewItem)
                recentEmojiProvider.recordSelection(emojiViewItem.emoji)
                recentNeedsRefreshing = true
            }
        )
    }

    internal fun buildEmojiPickerItems() = EmojiPickerItems(buildList {
        add(ItemGroup(
            R.drawable.quantum_gm_ic_access_time_filled_vd_theme_24,
            CategoryTitle(context.getString(R.string.emoji_category_recent)),
            recentItems,
            maxContentItemCount = DEFAULT_MAX_RECENT_ITEM_ROWS * emojiGridColumns,
            emptyPlaceholderItem = PlaceholderText(
                context.getString(R.string.emoji_empty_recent_category)
            )
        ).also { recentItemGroup = it })

        for ((i, category) in BundledEmojiListLoader.getCategorizedEmojiData().withIndex()) {
            add(
                ItemGroup(
                    category.headerIconId,
                    CategoryTitle(category.categoryName),
                    category.emojiDataList.mapIndexed { j, emojiData ->
                        EmojiViewData(stickyVariantProvider[emojiData.emoji], dataIndex = i + j)
                    },
                )
            )
        }
    })

    private fun showEmojiPickerView() {
        emojiPickerItems = buildEmojiPickerItems()

        val bodyLayoutManager = GridLayoutManager(
            context,
            emojiGridColumns,
            LinearLayoutManager.VERTICAL,
            /* reverseLayout = */ false
        ).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (emojiPickerItems.getBodyItem(position).itemType) {
                        ItemType.CATEGORY_TITLE, ItemType.PLACEHOLDER_TEXT -> emojiGridColumns
                        else -> 1
                    }
                }
            }
        }

        val headerAdapter =
            EmojiPickerHeaderAdapter(context, emojiPickerItems, onHeaderIconClicked = {
                with(emojiPickerItems.firstItemPositionByGroupIndex(it)) {
                    if (this == emojiPickerItems.groupRange(recentItemGroup).first) {
                        scope.launch {
                            refreshRecent()
                        }
                    }
                    bodyLayoutManager.scrollToPositionWithOffset(this, 0)
                    // The scroll position change will not be reflected until the next layout call,
                    // so force a new layout call here.
                    invalidate()
                }
            })

        // clear view's children in case of resetting layout
        super.removeAllViews()
        with(inflate(context, R.layout.emoji_picker, this)) {
            // set headerView
            ViewCompat.requireViewById<RecyclerView>(this, R.id.emoji_picker_header).apply {
                layoutManager =
                    object : LinearLayoutManager(
                        context,
                        HORIZONTAL,
                        /* reverseLayout = */ false
                    ) {
                        override fun checkLayoutParams(lp: RecyclerView.LayoutParams): Boolean {
                            lp.width =
                                (width - paddingStart - paddingEnd) / emojiPickerItems.numGroups
                            return true
                        }
                    }
                adapter = headerAdapter
            }

            // set bodyView
            ViewCompat.requireViewById<RecyclerView>(this, R.id.emoji_picker_body).apply {
                layoutManager = bodyLayoutManager
                adapter = createEmojiPickerBodyAdapter().apply {
                    setHasStableIds(true)
                }.also { bodyAdapter = it }
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        headerAdapter.selectedGroupIndex =
                            emojiPickerItems.groupIndexByItemPosition(
                                bodyLayoutManager.findFirstCompletelyVisibleItemPosition()
                            )
                        if (recentNeedsRefreshing &&
                            bodyLayoutManager.findFirstVisibleItemPosition() !in
                            emojiPickerItems.groupRange(recentItemGroup)
                        ) {
                            scope.launch {
                                refreshRecent()
                            }
                        }
                    }
                })
                // Disable item insertion/deletion animation. This keeps view holder unchanged when
                // item updates.
                itemAnimator = null
                setRecycledViewPool(RecyclerView.RecycledViewPool().apply {
                    setMaxRecycledViews(
                        ItemType.EMOJI.ordinal,
                        EmojiPickerConstants.EMOJI_VIEW_POOL_SIZE
                    )
                })
            }
        }
    }

    internal suspend fun refreshRecent() {
        if (!recentNeedsRefreshing) {
            return
        }
        val oldGroupSize = if (::recentItemGroup.isInitialized) recentItemGroup.size else 0
        val recent = recentEmojiProvider.getRecentEmojiList()
        withContext(Dispatchers.Main) {
            recentItems.clear()
            recentItems.addAll(recent.map {
                EmojiViewData(
                    it,
                    updateToSticky = false,
                )
            })
            if (::emojiPickerItems.isInitialized) {
                val range = emojiPickerItems.groupRange(recentItemGroup)
                if (recentItemGroup.size > oldGroupSize) {
                    bodyAdapter.notifyItemRangeInserted(
                        range.first + oldGroupSize,
                        recentItemGroup.size - oldGroupSize
                    )
                } else if (recentItemGroup.size < oldGroupSize) {
                    bodyAdapter.notifyItemRangeRemoved(
                        range.first + recentItemGroup.size,
                        oldGroupSize - recentItemGroup.size
                    )
                }
                bodyAdapter.notifyItemRangeChanged(
                    range.first,
                    minOf(oldGroupSize, recentItemGroup.size)
                )
                recentNeedsRefreshing = false
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

    fun setRecentEmojiProvider(recentEmojiProvider: RecentEmojiProvider) {
        this.recentEmojiProvider = recentEmojiProvider
        scope.launch {
            recentNeedsRefreshing = true
            refreshRecent()
        }
    }

    /**
     * The following functions disallow clients to add view to the EmojiPickerView
     *
     * @param child the child view to be added
     * @throws UnsupportedOperationException
     */
    override fun addView(child: View?) {
        if (childCount > 0)
            throw UnsupportedOperationException(EmojiPickerConstants.ADD_VIEW_EXCEPTION_MESSAGE)
        else super.addView(child)
    }

    /**
     * @param child
     * @param params
     * @throws UnsupportedOperationException
     */
    override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
        if (childCount > 0)
            throw UnsupportedOperationException(EmojiPickerConstants.ADD_VIEW_EXCEPTION_MESSAGE)
        else super.addView(child, params)
    }

    /**
     * @param child
     * @param index
     * @throws UnsupportedOperationException
     */
    override fun addView(child: View?, index: Int) {
        if (childCount > 0)
            throw UnsupportedOperationException(EmojiPickerConstants.ADD_VIEW_EXCEPTION_MESSAGE)
        else super.addView(child, index)
    }

    /**
     * @param child
     * @param index
     * @param params
     * @throws UnsupportedOperationException
     */
    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (childCount > 0)
            throw UnsupportedOperationException(EmojiPickerConstants.ADD_VIEW_EXCEPTION_MESSAGE)
        else super.addView(child, index, params)
    }

    /**
     * @param child
     * @param width
     * @param height
     * @throws UnsupportedOperationException
     */
    override fun addView(child: View?, width: Int, height: Int) {
        if (childCount > 0)
            throw UnsupportedOperationException(EmojiPickerConstants.ADD_VIEW_EXCEPTION_MESSAGE)
        else super.addView(child, width, height)
    }

    /**
     * The following functions disallow clients to remove view from the EmojiPickerView
     * @throws UnsupportedOperationException
     */
    override fun removeAllViews() {
        throw UnsupportedOperationException(EmojiPickerConstants.REMOVE_VIEW_EXCEPTION_MESSAGE)
    }

    /**
     * @param child
     * @throws UnsupportedOperationException
     */
    override fun removeView(child: View?) {
        throw UnsupportedOperationException(EmojiPickerConstants.REMOVE_VIEW_EXCEPTION_MESSAGE)
    }

    /**
     * @param index
     * @throws UnsupportedOperationException
     */
    override fun removeViewAt(index: Int) {
        throw UnsupportedOperationException(EmojiPickerConstants.REMOVE_VIEW_EXCEPTION_MESSAGE)
    }

    /**
     * @param child
     * @throws UnsupportedOperationException
     */
    override fun removeViewInLayout(child: View?) {
        throw UnsupportedOperationException(EmojiPickerConstants.REMOVE_VIEW_EXCEPTION_MESSAGE)
    }

    /**
     * @param start
     * @param count
     * @throws UnsupportedOperationException
     */
    override fun removeViews(start: Int, count: Int) {
        throw UnsupportedOperationException(EmojiPickerConstants.REMOVE_VIEW_EXCEPTION_MESSAGE)
    }

    /**
     * @param start
     * @param count
     * @throws UnsupportedOperationException
     */
    override fun removeViewsInLayout(start: Int, count: Int) {
        throw UnsupportedOperationException(EmojiPickerConstants.REMOVE_VIEW_EXCEPTION_MESSAGE)
    }
}
