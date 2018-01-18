/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.car.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.ColorRes;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.car.R;

/**
 * View that wraps a {@link android.support.v7.widget.RecyclerView} and a scroll bar that has
 * page up and down arrows. Interaction with this view is similar to a {@code RecyclerView} as it
 * takes the same adapter.
 *
 * <p>By default, this PagedListView utilizes a vertical {@link LinearLayoutManager} to display
 * its items.
 */
public class PagedListView extends FrameLayout {
    /**
     * The key used to save the state of this PagedListView's super class in
     * {@link #onSaveInstanceState()}.
     */
    private static final String SAVED_SUPER_STATE_KEY = "PagedListViewSuperState";

    /**
     * The key used to save the state of {@link #mRecyclerView} so that it can be restored
     * on configuration change. The actual saving of state will be controlled by the LayoutManager
     * of the RecyclerView; this value simply ensures the state is passed on to the LayoutManager.
     */
    private static final String SAVED_RECYCLER_VIEW_STATE_KEY = "RecyclerViewState";

    /** Default maximum number of clicks allowed on a list */
    public static final int DEFAULT_MAX_CLICKS = 6;

    /**
     * Value to pass to {@link #setMaxPages(int)} to indicate there is no restriction on the
     * maximum number of pages to show.
     */
    public static final int UNLIMITED_PAGES = -1;

    /**
     * The amount of time after settling to wait before autoscrolling to the next page when the user
     * holds down a pagination button.
     */
    private static final int PAGINATION_HOLD_DELAY_MS = 400;

    /**
     * A fling distance to use when the up button is pressed. This value is arbitrary and just needs
     * to be large enough so that the maximum amount of fling is applied. The
     * {@link PagedSnapHelper} will handle capping this value so that the RecyclerView is scrolled
     * one page upwards.
     */
    private static final int FLING_UP_DISTANCE = -10000;

    /**
     * A fling distance to use when the down button is pressed. This value is arbitrary and just
     * needs to be large enough so that the maximum amount of fling is applied. The
     * {@link PagedSnapHelper} will handle capping this value so that the RecyclerView is scrolled
     * one page downwards.
     */
    private static final int FLING_DOWN_DISTANCE = 10000;

    private static final String TAG = "PagedListView";
    private static final int INVALID_RESOURCE_ID = -1;

    private final RecyclerView mRecyclerView;
    private final PagedSnapHelper mSnapHelper;
    private final Handler mHandler = new Handler();
    private final boolean mScrollBarEnabled;
    @VisibleForTesting
    final PagedScrollBarView mScrollBarView;

    private int mRowsPerPage = -1;
    private RecyclerView.Adapter<? extends RecyclerView.ViewHolder> mAdapter;

    /** Maximum number of pages to show. */
    private int mMaxPages;

    private OnScrollListener mOnScrollListener;

    /** Number of visible rows per page */
    private int mDefaultMaxPages = DEFAULT_MAX_CLICKS;

    /** Used to check if there are more items added to the list. */
    private int mLastItemCount;

    private boolean mNeedsFocus;

    @Gutter
    private int mGutter;
    private int mGutterSize;

    /**
     * Interface for a {@link android.support.v7.widget.RecyclerView.Adapter} to cap the number of
     * items.
     *
     * <p>NOTE: it is still up to the adapter to use maxItems in {@link
     * android.support.v7.widget.RecyclerView.Adapter#getItemCount()}.
     *
     * <p>the recommended way would be with:
     *
     * <pre>{@code
     * {@literal@}Override
     * public int getItemCount() {
     *   return Math.min(super.getItemCount(), mMaxItems);
     * }
     * }</pre>
     */
    public interface ItemCap {
        /**
         * A value to pass to {@link #setMaxItems(int)} that indicates there should be no limit.
         */
        int UNLIMITED = -1;

        /**
         * Sets the maximum number of items available in the adapter. A value less than '0' means
         * the list should not be capped.
         */
        void setMaxItems(int maxItems);
    }

    /**
     * Interface for controlling visibility of item dividers for individual items based on the
     * item's position.
     *
     * <p> NOTE: interface takes effect only when dividers are enabled.
     */
    public interface DividerVisibilityManager {
        /**
         * Given an item position, returns whether the divider coming after that item should be
         * hidden.
         *
         * @param position item position inside the adapter.
         * @return true if divider is to be hidden, false if divider should be shown.
         */
        boolean shouldHideDivider(int position);
    }

    /**
     * The possible values for @{link #setGutter}. The default value is actually
     * {@link Gutter#BOTH}.
     */
    @IntDef({
            Gutter.NONE,
            Gutter.START,
            Gutter.END,
            Gutter.BOTH,
    })
    public @interface Gutter {
        /**
         * No gutter on either side of the list items. The items will span the full width of the
         * {@link PagedListView}.
         */
        int NONE = 0;

        /**
         * Include a gutter only on the start side (that is, the same side as the scroll bar).
         */
        int START = 1;

        /**
         * Include a gutter only on the end side (that is, the opposite side of the scroll bar).
         */
        int END = 2;

        /**
         * Include a gutter on both sides of the list items. This is the default behaviour.
         */
        int BOTH = 3;
    }

    /**
     * Interface for a {@link android.support.v7.widget.RecyclerView.Adapter} to set the position
     * offset for the adapter to load the data.
     *
     * <p>For example in the adapter, if the positionOffset is 20, then for position 0 it will show
     * the item in position 20 instead, for position 1 it will show the item in position 21 instead
     * and so on.
     */
    public interface ItemPositionOffset {
        /** Sets the position offset for the adapter. */
        void setPositionOffset(int positionOffset);
    }

    public PagedListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0 /*defStyleAttrs*/, 0 /*defStyleRes*/);
    }

    public PagedListView(Context context, AttributeSet attrs, int defStyleAttrs) {
        this(context, attrs, defStyleAttrs, 0 /*defStyleRes*/);
    }

    public PagedListView(Context context, AttributeSet attrs, int defStyleAttrs, int defStyleRes) {
        this(context, attrs, defStyleAttrs, defStyleRes, 0);
    }

    public PagedListView(
            Context context, AttributeSet attrs, int defStyleAttrs, int defStyleRes, int layoutId) {
        super(context, attrs, defStyleAttrs, defStyleRes);
        if (layoutId == 0) {
            layoutId = R.layout.car_paged_recycler_view;
        }
        LayoutInflater.from(context).inflate(layoutId, this /*root*/, true /*attachToRoot*/);

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.PagedListView, defStyleAttrs, defStyleRes);
        mRecyclerView = findViewById(R.id.recycler_view);

        mMaxPages = getDefaultMaxPages();

        RecyclerView.LayoutManager layoutManager =
                new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);

        mSnapHelper = new PagedSnapHelper(context);
        mSnapHelper.attachToRecyclerView(mRecyclerView);

        mRecyclerView.setOnScrollListener(mRecyclerViewOnScrollListener);
        mRecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 12);

        int defaultGutterSize = getResources().getDimensionPixelSize(R.dimen.car_margin);
        mGutterSize = a.getDimensionPixelSize(R.styleable.PagedListView_gutterSize,
                defaultGutterSize);

        if (a.hasValue(R.styleable.PagedListView_gutter)) {
            int gutter = a.getInt(R.styleable.PagedListView_gutter, Gutter.BOTH);
            setGutter(gutter);
        } else if (a.hasValue(R.styleable.PagedListView_offsetScrollBar)) {
            boolean offsetScrollBar =
                    a.getBoolean(R.styleable.PagedListView_offsetScrollBar, false);
            if (offsetScrollBar) {
                setGutter(Gutter.START);
            }
        } else {
            setGutter(Gutter.BOTH);
        }

        if (a.getBoolean(R.styleable.PagedListView_showPagedListViewDivider, true)) {
            int dividerStartMargin = a.getDimensionPixelSize(
                    R.styleable.PagedListView_dividerStartMargin, 0);
            int dividerStartId = a.getResourceId(
                    R.styleable.PagedListView_alignDividerStartTo, INVALID_RESOURCE_ID);
            int dividerEndId = a.getResourceId(
                    R.styleable.PagedListView_alignDividerEndTo, INVALID_RESOURCE_ID);

            mRecyclerView.addItemDecoration(new DividerDecoration(context, dividerStartMargin,
                    dividerStartId, dividerEndId));
        }

        int itemSpacing = a.getDimensionPixelSize(R.styleable.PagedListView_itemSpacing, 0);
        if (itemSpacing > 0) {
            mRecyclerView.addItemDecoration(new ItemSpacingDecoration(itemSpacing));
        }

        // Set this to true so that this view consumes clicks events and views underneath
        // don't receive this click event. Without this it's possible to click places in the
        // view that don't capture the event, and as a result, elements visually hidden consume
        // the event.
        setClickable(true);

        // Set focusable false explicitly to handle the behavior change in Android O where
        // clickable view becomes focusable by default.
        setFocusable(false);

        mScrollBarEnabled = a.getBoolean(R.styleable.PagedListView_scrollBarEnabled, true);
        mScrollBarView = (PagedScrollBarView) findViewById(R.id.paged_scroll_view);
        mScrollBarView.setPaginationListener(direction -> {
            switch (direction) {
                case PagedScrollBarView.PaginationListener.PAGE_UP:
                    pageUp();
                    if (mOnScrollListener != null) {
                        mOnScrollListener.onScrollUpButtonClicked();
                    }
                    break;
                case PagedScrollBarView.PaginationListener.PAGE_DOWN:
                    pageDown();
                    if (mOnScrollListener != null) {
                        mOnScrollListener.onScrollDownButtonClicked();
                    }
                    break;
                default:
                    Log.e(TAG, "Unknown pagination direction (" + direction + ")");
            }
        });

        Drawable upButtonIcon = a.getDrawable(R.styleable.PagedListView_upButtonIcon);
        if (upButtonIcon != null) {
            setUpButtonIcon(upButtonIcon);
        }

        Drawable downButtonIcon = a.getDrawable(R.styleable.PagedListView_downButtonIcon);
        if (downButtonIcon != null) {
            setDownButtonIcon(downButtonIcon);
        }

        mScrollBarView.setVisibility(mScrollBarEnabled ? VISIBLE : GONE);

        if (mScrollBarEnabled) {
            int topMargin =
                    a.getDimensionPixelSize(R.styleable.PagedListView_scrollBarTopMargin, 0);
            setScrollBarTopMargin(topMargin);
        } else {
            MarginLayoutParams params = (MarginLayoutParams) mRecyclerView.getLayoutParams();
            params.setMarginStart(0);
        }

        if (a.hasValue(R.styleable.PagedListView_scrollBarContainerWidth)) {
            int carMargin = getResources().getDimensionPixelSize(R.dimen.car_margin);
            int scrollBarContainerWidth = a.getDimensionPixelSize(
                    R.styleable.PagedListView_scrollBarContainerWidth, carMargin);
            setScrollBarContainerWidth(scrollBarContainerWidth);
        }

        setDayNightStyle(DayNightStyle.AUTO);
        a.recycle();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeCallbacks(mUpdatePaginationRunnable);
    }

    /**
     * Returns the position of the given View in the list.
     *
     * @param v The View to check for.
     * @return The position or -1 if the given View is {@code null} or not in the list.
     */
    public int positionOf(@Nullable View v) {
        if (v == null || v.getParent() != mRecyclerView
                || mRecyclerView.getLayoutManager() == null) {
            return -1;
        }
        return mRecyclerView.getLayoutManager().getPosition(v);
    }

    /**
     * Set the gutter to the specified value.
     *
     * <p>The gutter is the space to the start/end of the list view items and will be equal in size
     * to the scroll bars. By default, there is a gutter to both the left and right of the list
     * view items, to account for the scroll bar.
     *
     * @param gutter A {@link Gutter} value that identifies which sides to apply the gutter to.
     */
    public void setGutter(@Gutter int gutter) {
        mGutter = gutter;

        int startPadding = 0;
        int endPadding = 0;
        if ((mGutter & Gutter.START) != 0) {
            startPadding = mGutterSize;
        }
        if ((mGutter & Gutter.END) != 0) {
            endPadding = mGutterSize;
        }
        mRecyclerView.setPaddingRelative(startPadding, 0, endPadding, 0);

        // If there's a gutter, set ClipToPadding to false so that CardView's shadow will still
        // appear outside of the padding.
        mRecyclerView.setClipToPadding(startPadding == 0 && endPadding == 0);
    }

    /**
     * Sets the size of the gutter that appears at the start, end or both sizes of the items in
     * the {@code PagedListView}.
     *
     * @param gutterSize The size of the gutter in pixels.
     * @see #setGutter(int)
     */
    public void setGutterSize(int gutterSize) {
        mGutterSize = gutterSize;

        // Call setGutter to reset the gutter.
        setGutter(mGutter);
    }

    /**
     * Sets the width of the container that holds the scrollbar. The scrollbar will be centered
     * within this width.
     *
     * @param width The width of the scrollbar container.
     */
    public void setScrollBarContainerWidth(int width) {
        ViewGroup.LayoutParams layoutParams = mScrollBarView.getLayoutParams();
        layoutParams.width = width;
        mScrollBarView.requestLayout();
    }

    /**
     * Sets the top margin above the scroll bar. By default, this margin is 0.
     *
     * @param topMargin The top margin.
     */
    public void setScrollBarTopMargin(int topMargin) {
        MarginLayoutParams params = (MarginLayoutParams) mScrollBarView.getLayoutParams();
        params.topMargin = topMargin;
        mScrollBarView.requestLayout();
    }

    @NonNull
    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    /**
     * Scrolls to the given position in the PagedListView.
     *
     * @param position The position in the list to scroll to.
     */
    public void scrollToPosition(int position) {
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager == null) {
            return;
        }

        RecyclerView.SmoothScroller smoothScroller = mSnapHelper.createScroller(layoutManager);
        smoothScroller.setTargetPosition(position);

        layoutManager.startSmoothScroll(smoothScroller);

        // Sometimes #scrollToPosition doesn't change the scroll state so we need to make sure
        // the pagination arrows actually get updated. See b/15801119
        mHandler.post(mUpdatePaginationRunnable);
    }

    /** Sets the icon to be used for the up button. */
    public void setUpButtonIcon(Drawable icon) {
        mScrollBarView.setUpButtonIcon(icon);
    }

    /** Sets the icon to be used for the down button. */
    public void setDownButtonIcon(Drawable icon) {
        mScrollBarView.setDownButtonIcon(icon);
    }

    /**
     * Sets the adapter for the list.
     *
     * <p>The given Adapter can implement {@link ItemCap} if it wishes to control the behavior of
     * a max number of items. Otherwise, methods in the PagedListView to limit the content, such as
     * {@link #setMaxPages(int)}, will do nothing.
     */
    public void setAdapter(
            @NonNull RecyclerView.Adapter<? extends RecyclerView.ViewHolder> adapter) {
        mAdapter = adapter;
        mRecyclerView.setAdapter(adapter);

        updateMaxItems();
    }

    /**
     * Sets {@link DividerVisibilityManager} on all {@code DividerDecoration} item decorations.
     *
     * @param dvm {@code DividerVisibilityManager} to be set.
     */
    public void setDividerVisibilityManager(DividerVisibilityManager dvm) {
        int decorCount = mRecyclerView.getItemDecorationCount();
        for (int i = 0; i < decorCount; i++) {
            RecyclerView.ItemDecoration decor = mRecyclerView.getItemDecorationAt(i);
            if (decor instanceof DividerDecoration) {
                ((DividerDecoration) decor).setVisibilityManager(dvm);
            }
        }
        mRecyclerView.invalidateItemDecorations();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public RecyclerView.Adapter<? extends RecyclerView.ViewHolder> getAdapter() {
        return mRecyclerView.getAdapter();
    }

    /**
     * Sets the maximum number of the pages that can be shown in the PagedListView. The size of a
     * page is defined as the number of items that fit completely on the screen at once.
     *
     * <p>Passing {@link #UNLIMITED_PAGES} will remove any restrictions on a maximum number
     * of pages.
     *
     * <p>Note that for any restriction on maximum pages to work, the adapter passed to this
     * PagedListView needs to implement {@link ItemCap}.
     *
     * @param maxPages The maximum number of pages that fit on the screen. Should be positive or
     * {@link #UNLIMITED_PAGES}.
     */
    public void setMaxPages(int maxPages) {
        mMaxPages = Math.max(UNLIMITED_PAGES, maxPages);
        updateMaxItems();
    }

    /**
     * Returns the maximum number of pages allowed in the PagedListView. This number is set by
     * {@link #setMaxPages(int)}. If that method has not been called, then this value should match
     * the default value.
     *
     * @return The maximum number of pages to be shown or {@link #UNLIMITED_PAGES} if there is
     * no limit.
     */
    public int getMaxPages() {
        return mMaxPages;
    }

    /**
     * Gets the number of rows per page. Default value of mRowsPerPage is -1. If the first child of
     * PagedLayoutManager is null or the height of the first child is 0, it will return 1.
     */
    public int getRowsPerPage() {
        return mRowsPerPage;
    }

    /** Resets the maximum number of pages to be shown to be the default. */
    public void resetMaxPages() {
        mMaxPages = getDefaultMaxPages();
        updateMaxItems();
    }

    /**
     * Adds an {@link android.support.v7.widget.RecyclerView.ItemDecoration} to this PagedListView.
     *
     * @param decor The decoration to add.
     * @see RecyclerView#addItemDecoration(RecyclerView.ItemDecoration)
     */
    public void addItemDecoration(@NonNull RecyclerView.ItemDecoration decor) {
        mRecyclerView.addItemDecoration(decor);
    }

    /**
     * Removes the given {@link android.support.v7.widget.RecyclerView.ItemDecoration} from this
     * PagedListView.
     *
     * <p>The decoration will function the same as the item decoration for a {@link RecyclerView}.
     *
     * @param decor The decoration to remove.
     * @see RecyclerView#removeItemDecoration(RecyclerView.ItemDecoration)
     */
    public void removeItemDecoration(@NonNull RecyclerView.ItemDecoration decor) {
        mRecyclerView.removeItemDecoration(decor);
    }

    /**
     * Sets spacing between each item in the list. The spacing will not be added before the first
     * item and after the last.
     *
     * @param itemSpacing the spacing between each item.
     */
    public void setItemSpacing(int itemSpacing) {
        ItemSpacingDecoration existing = null;
        for (int i = 0, count = mRecyclerView.getItemDecorationCount(); i < count; i++) {
            RecyclerView.ItemDecoration itemDecoration = mRecyclerView.getItemDecorationAt(i);
            if (itemDecoration instanceof ItemSpacingDecoration) {
                existing = (ItemSpacingDecoration) itemDecoration;
                break;
            }
        }

        if (itemSpacing == 0 && existing != null) {
            mRecyclerView.removeItemDecoration(existing);
        } else if (existing == null) {
            mRecyclerView.addItemDecoration(new ItemSpacingDecoration(itemSpacing));
        } else {
            existing.setItemSpacing(itemSpacing);
        }
        mRecyclerView.invalidateItemDecorations();
    }

    /**
     * Sets the color of scrollbar.
     *
     * <p>Custom color ignores {@link DayNightStyle}. Calling {@link #resetScrollbarColor} resets to
     * default color.
     *
     * @param color Resource identifier of the color.
     */
    public void setScrollbarColor(@ColorRes int color) {
        mScrollBarView.setThumbColor(color);
    }

    /**
     * Resets the color of scrollbar to default.
     */
    public void resetScrollbarColor() {
        mScrollBarView.resetThumbColor();
    }

    /**
     * Adds an {@link android.support.v7.widget.RecyclerView.OnItemTouchListener} to this
     * PagedListView.
     *
     * <p>The listener will function the same as the listener for a regular {@link RecyclerView}.
     *
     * @param touchListener The touch listener to add.
     * @see RecyclerView#addOnItemTouchListener(RecyclerView.OnItemTouchListener)
     */
    public void addOnItemTouchListener(@NonNull RecyclerView.OnItemTouchListener touchListener) {
        mRecyclerView.addOnItemTouchListener(touchListener);
    }

    /**
     * Removes the given {@link android.support.v7.widget.RecyclerView.OnItemTouchListener} from
     * the PagedListView.
     *
     * @param touchListener The touch listener to remove.
     * @see RecyclerView#removeOnItemTouchListener(RecyclerView.OnItemTouchListener)
     */
    public void removeOnItemTouchListener(@NonNull RecyclerView.OnItemTouchListener touchListener) {
        mRecyclerView.removeOnItemTouchListener(touchListener);
    }

    /**
     * Sets how this {@link PagedListView} responds to day/night configuration changes. By
     * default, the PagedListView is darker in the day and lighter at night.
     *
     * @param dayNightStyle A value from {@link DayNightStyle}.
     * @see DayNightStyle
     */
    public void setDayNightStyle(@DayNightStyle int dayNightStyle) {
        // Update the scrollbar
        mScrollBarView.setDayNightStyle(dayNightStyle);

        int decorCount = mRecyclerView.getItemDecorationCount();
        for (int i = 0; i < decorCount; i++) {
            RecyclerView.ItemDecoration decor = mRecyclerView.getItemDecorationAt(i);
            if (decor instanceof DividerDecoration) {
                ((DividerDecoration) decor).updateDividerColor();
            }
        }
    }

    /**
     * Sets the {@link OnScrollListener} that will be notified of scroll events within the
     * PagedListView.
     *
     * @param listener The scroll listener to set.
     */
    public void setOnScrollListener(OnScrollListener listener) {
        mOnScrollListener = listener;
    }

    /** Returns the page the given position is on, starting with page 0. */
    public int getPage(int position) {
        if (mRowsPerPage == -1) {
            return -1;
        }
        if (mRowsPerPage == 0) {
            return 0;
        }
        return position / mRowsPerPage;
    }

    /** Scrolls the contents of the RecyclerView up a page. */
    private void pageUp() {
        mRecyclerView.fling(0, FLING_UP_DISTANCE);
    }

    /** Scrolls the contents of the RecyclerView down a page. */
    private void pageDown() {
        mRecyclerView.fling(0, FLING_DOWN_DISTANCE);
    }

    /**
     * Sets the default number of pages that this PagedListView is limited to.
     *
     * @param newDefault The default number of pages. Should be positive.
     */
    public void setDefaultMaxPages(int newDefault) {
        if (newDefault < 0) {
            return;
        }
        mDefaultMaxPages = newDefault;
        resetMaxPages();
    }

    /** Returns the default number of pages the list should have */
    private int getDefaultMaxPages() {
        // assume list shown in response to a click, so, reduce number of clicks by one
        return mDefaultMaxPages - 1;
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // if a late item is added to the top of the layout after the layout is stabilized, causing
        // the former top item to be pushed to the 2nd page, the focus will still be on the former
        // top item. Since our car layout manager tries to scroll the viewport so that the focused
        // item is visible, the view port will be on the 2nd page. That means the newly added item
        // will not be visible, on the first page.

        // what we want to do is: if the formerly focused item is the first one in the list, any
        // item added above it will make the focus to move to the new first item.
        // if the focus is not on the formerly first item, then we don't need to do anything. Let
        // the layout manager do the job and scroll the viewport so the currently focused item
        // is visible.
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();

        if (layoutManager == null) {
            return;
        }

        // we need to calculate whether we want to request focus here, before the super call,
        // because after the super call, the first born might be changed.
        View focusedChild = layoutManager.getFocusedChild();
        View firstBorn = layoutManager.getChildAt(0);

        super.onLayout(changed, left, top, right, bottom);

        if (mAdapter != null) {
            int itemCount = mAdapter.getItemCount();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, String.format(
                        "onLayout hasFocus: %s, mLastItemCount: %s, itemCount: %s, "
                                + "focusedChild: %s, firstBorn: %s, isInTouchMode: %s, "
                                + "mNeedsFocus: %s",
                        hasFocus(),
                        mLastItemCount,
                        itemCount,
                        focusedChild,
                        firstBorn,
                        isInTouchMode(),
                        mNeedsFocus));
            }
            updateMaxItems();
            // This is a workaround for missing focus because isInTouchMode() is not always
            // returning the right value.
            // This is okay for the Engine release since focus is always showing.
            // However, in Tala and Fender, we want to show focus only when the user uses
            // hardware controllers, so we need to revisit this logic. b/22990605.
            if (mNeedsFocus && itemCount > 0) {
                if (focusedChild == null) {
                    requestFocus();
                }
                mNeedsFocus = false;
            }
            if (itemCount > mLastItemCount && focusedChild == firstBorn) {
                requestFocus();
            }
            mLastItemCount = itemCount;
        }

        // We need to update the scroll buttons after layout has happened.
        // Determining if a scrollbar is necessary requires looking at the layout of the child
        // views. Therefore, this determination can only be done after layout has happened.
        // Note: don't animate here to prevent b/26849677
        updatePaginationButtons(false /*animate*/);
    }

    /**
     * Determines if scrollbar should be visible or not and shows/hides it accordingly. If this is
     * being called as a result of adapter changes, it should be called after the new layout has
     * been calculated because the method of determining scrollbar visibility uses the current
     * layout. If this is called after an adapter change but before the new layout, the visibility
     * determination may not be correct.
     *
     * @param animate {@code true} if the scrollbar should animate to its new position.
     *                {@code false} if no animation is used
     */
    private void updatePaginationButtons(boolean animate) {
        if (!mScrollBarEnabled) {
            // Don't change the visibility of the ScrollBar unless it's enabled.
            return;
        }

        boolean isAtStart = isAtStart();
        boolean isAtEnd = isAtEnd();
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();

        if ((isAtStart && isAtEnd) || layoutManager == null || layoutManager.getItemCount() == 0) {
            mScrollBarView.setVisibility(View.INVISIBLE);
        } else {
            mScrollBarView.setVisibility(View.VISIBLE);
        }
        mScrollBarView.setUpEnabled(!isAtStart);
        mScrollBarView.setDownEnabled(!isAtEnd);

        if (layoutManager == null) {
            return;
        }

        if (mRecyclerView.getLayoutManager().canScrollVertically()) {
            mScrollBarView.setParameters(
                    mRecyclerView.computeVerticalScrollRange(),
                    mRecyclerView.computeVerticalScrollOffset(),
                    mRecyclerView.computeVerticalScrollExtent(), animate);
        } else {
            mScrollBarView.setParameters(
                    mRecyclerView.computeHorizontalScrollRange(),
                    mRecyclerView.computeHorizontalScrollOffset(),
                    mRecyclerView.computeHorizontalScrollExtent(), animate);
        }

        invalidate();
    }

    /** Returns {@code true} if the RecyclerView is completely displaying the first item. */
    public boolean isAtStart() {
        return mSnapHelper.isAtStart(mRecyclerView.getLayoutManager());
    }

    /** Returns {@code true} if the RecyclerView is completely displaying the last item. */
    public boolean isAtEnd() {
        return mSnapHelper.isAtEnd(mRecyclerView.getLayoutManager());
    }

    @UiThread
    private void updateMaxItems() {
        if (mAdapter == null) {
            return;
        }

        // Ensure mRowsPerPage regardless of if the adapter implements ItemCap.
        updateRowsPerPage();

        // If the adapter does not implement ItemCap, then the max items on it cannot be updated.
        if (!(mAdapter instanceof ItemCap)) {
            return;
        }

        final int originalCount = mAdapter.getItemCount();
        ((ItemCap) mAdapter).setMaxItems(calculateMaxItemCount());
        final int newCount = mAdapter.getItemCount();
        if (newCount == originalCount) {
            return;
        }

        if (newCount < originalCount) {
            mAdapter.notifyItemRangeRemoved(newCount, originalCount - newCount);
        } else {
            mAdapter.notifyItemRangeInserted(originalCount, newCount - originalCount);
        }
    }

    private int calculateMaxItemCount() {
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager == null) {
            return -1;
        }

        View firstChild = layoutManager.getChildAt(0);
        if (firstChild == null || firstChild.getHeight() == 0) {
            return -1;
        } else {
            return (mMaxPages < 0) ? -1 : mRowsPerPage * mMaxPages;
        }
    }

    /**
     * Updates the rows number per current page, which is used for calculating how many items we
     * want to show.
     */
    private void updateRowsPerPage() {
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager == null) {
            mRowsPerPage = 1;
            return;
        }

        View firstChild = layoutManager.getChildAt(0);
        if (firstChild == null || firstChild.getHeight() == 0) {
            mRowsPerPage = 1;
        } else {
            mRowsPerPage = Math.max(1, (getHeight() - getPaddingTop()) / firstChild.getHeight());
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(SAVED_SUPER_STATE_KEY, super.onSaveInstanceState());

        SparseArray<Parcelable> recyclerViewState = new SparseArray<>();
        mRecyclerView.saveHierarchyState(recyclerViewState);
        bundle.putSparseParcelableArray(SAVED_RECYCLER_VIEW_STATE_KEY, recyclerViewState);

        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof Bundle)) {
            super.onRestoreInstanceState(state);
            return;
        }

        Bundle bundle = (Bundle) state;
        mRecyclerView.restoreHierarchyState(
                bundle.getSparseParcelableArray(SAVED_RECYCLER_VIEW_STATE_KEY));

        super.onRestoreInstanceState(bundle.getParcelable(SAVED_SUPER_STATE_KEY));
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        // There is the possibility of multiple PagedListViews on a page. This means that the ids
        // of the child Views of PagedListView are no longer unique, and onSaveInstanceState()
        // cannot be used as is. As a result, PagedListViews needs to manually dispatch the instance
        // states. Call dispatchFreezeSelfOnly() so that no child views have onSaveInstanceState()
        // called by the system.
        dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        // Prevent onRestoreInstanceState() from being called on child Views. Instead, PagedListView
        // will manually handle passing the state. See the comment in dispatchSaveInstanceState()
        // for more information.
        dispatchThawSelfOnly(container);
    }

    private final RecyclerView.OnScrollListener mRecyclerViewOnScrollListener =
            new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (mOnScrollListener != null) {
                        mOnScrollListener.onScrolled(recyclerView, dx, dy);

                        if (!isAtStart() && isAtEnd()) {
                            mOnScrollListener.onReachBottom();
                        }
                    }
                    updatePaginationButtons(false);
                }

                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    if (mOnScrollListener != null) {
                        mOnScrollListener.onScrollStateChanged(recyclerView, newState);
                    }
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        mHandler.postDelayed(mPaginationRunnable, PAGINATION_HOLD_DELAY_MS);
                    }
                }
            };

    private final Runnable mPaginationRunnable =
            new Runnable() {
                @Override
                public void run() {
                    boolean upPressed = mScrollBarView.isUpPressed();
                    boolean downPressed = mScrollBarView.isDownPressed();
                    if (upPressed && downPressed) {
                        return;
                    }
                    if (upPressed) {
                        pageUp();
                    } else if (downPressed) {
                        pageDown();
                    }
                }
            };

    private final Runnable mUpdatePaginationRunnable =
            () -> updatePaginationButtons(true /*animate*/);

    /** Used to listen for {@code PagedListView} scroll events. */
    public abstract static class OnScrollListener {
        /** Called when menu reaches the bottom */
        public void onReachBottom() {}
        /** Called when scroll up button is clicked */
        public void onScrollUpButtonClicked() {}
        /** Called when scroll down button is clicked */
        public void onScrollDownButtonClicked() {}

        /**
         * Called when RecyclerView.OnScrollListener#onScrolled is called. See
         * RecyclerView.OnScrollListener
         */
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {}

        /** See RecyclerView.OnScrollListener */
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {}
    }

    /**
     * A {@link android.support.v7.widget.RecyclerView.ItemDecoration} that will add spacing
     * between each item in the RecyclerView that it is added to.
     */
    private static class ItemSpacingDecoration extends RecyclerView.ItemDecoration {
        private int mItemSpacing;

        private ItemSpacingDecoration(int itemSpacing) {
            mItemSpacing = itemSpacing;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            int position = parent.getChildAdapterPosition(view);

            // Skip offset for last item.
            if (position == state.getItemCount() - 1) {
                return;
            }

            outRect.bottom = mItemSpacing;
        }

        /**
         * @param itemSpacing sets spacing between each item.
         */
        public void setItemSpacing(int itemSpacing) {
            mItemSpacing = itemSpacing;
        }
    }

    /**
     * A {@link android.support.v7.widget.RecyclerView.ItemDecoration} that will draw a dividing
     * line between each item in the RecyclerView that it is added to.
     */
    private static class DividerDecoration extends RecyclerView.ItemDecoration {
        private final Context mContext;
        private final Paint mPaint;
        private final int mDividerHeight;
        private final int mDividerStartMargin;
        @IdRes private final int mDividerStartId;
        @IdRes private final int mDividerEndId;
        private DividerVisibilityManager mVisibilityManager;

        /**
         * @param dividerStartMargin The start offset of the dividing line. This offset will be
         *     relative to {@code dividerStartId} if that value is given.
         * @param dividerStartId A child view id whose starting edge will be used as the starting
         *     edge of the dividing line. If this value is {@link #INVALID_RESOURCE_ID}, the top
         *     container of each child view will be used.
         * @param dividerEndId A child view id whose ending edge will be used as the starting edge
         *     of the dividing lin.e If this value is {@link #INVALID_RESOURCE_ID}, then the top
         *     container view of each child will be used.
         */
        private DividerDecoration(Context context, int dividerStartMargin,
                @IdRes int dividerStartId, @IdRes int dividerEndId) {
            mContext = context;
            mDividerStartMargin = dividerStartMargin;
            mDividerStartId = dividerStartId;
            mDividerEndId = dividerEndId;

            Resources res = context.getResources();
            mPaint = new Paint();
            mPaint.setColor(res.getColor(R.color.car_list_divider));
            mDividerHeight = res.getDimensionPixelSize(R.dimen.car_list_divider_height);
        }

        /** Updates the list divider color which may have changed due to a day night transition. */
        public void updateDividerColor() {
            mPaint.setColor(mContext.getResources().getColor(R.color.car_list_divider));
        }

        /** Sets {@link DividerVisibilityManager} on the DividerDecoration.*/
        public void setVisibilityManager(DividerVisibilityManager dvm) {
            mVisibilityManager = dvm;
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            // Draw a divider line between each item. No need to draw the line for the last item.
            for (int i = 0, childCount = parent.getChildCount(); i < childCount - 1; i++) {
                View container = parent.getChildAt(i);

                // if divider should be hidden for this item, proceeds without drawing it
                int itemPosition = parent.getChildAdapterPosition(container);
                if (hideDividerForAdapterPosition(itemPosition)) {
                    continue;
                }

                View nextContainer = parent.getChildAt(i + 1);
                int spacing = nextContainer.getTop() - container.getBottom();

                View startChild =
                        mDividerStartId != INVALID_RESOURCE_ID
                                ? container.findViewById(mDividerStartId)
                                : container;

                View endChild =
                        mDividerEndId != INVALID_RESOURCE_ID
                                ? container.findViewById(mDividerEndId)
                                : container;

                if (startChild == null || endChild == null) {
                    continue;
                }

                Rect containerRect = new Rect();
                container.getGlobalVisibleRect(containerRect);

                Rect startRect = new Rect();
                startChild.getGlobalVisibleRect(startRect);

                Rect endRect = new Rect();
                endChild.getGlobalVisibleRect(endRect);

                int left = container.getLeft() + mDividerStartMargin
                        + (startRect.left - containerRect.left);
                int right = container.getRight() - (endRect.right - containerRect.right);
                int bottom = container.getBottom() + spacing / 2 + mDividerHeight / 2;
                int top = bottom - mDividerHeight;

                c.drawRect(left, top, right, bottom, mPaint);
            }
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            int pos = parent.getChildAdapterPosition(view);

            // Skip top offset when there is no divider above.
            if (pos > 0 && !hideDividerForAdapterPosition(pos - 1)) {
                outRect.top = mDividerHeight / 2;
            }

            // Skip bottom offset when there is no divider below.
            if (pos < state.getItemCount() - 1 && !hideDividerForAdapterPosition(pos)) {
                outRect.bottom = mDividerHeight / 2;
            }
        }

        private boolean hideDividerForAdapterPosition(int position) {
            return mVisibilityManager != null && mVisibilityManager.shouldHideDivider(position);
        }
    }
}
