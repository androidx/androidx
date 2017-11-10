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

package android.support.car.widget;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.UiThread;
import android.support.car.R;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Custom {@link android.support.v7.widget.RecyclerView} that displays a list of items that
 * resembles a {@link android.widget.ListView} but also has page up and page down arrows on the
 * left side.
 */
public class PagedListView extends FrameLayout {
    /** Default maximum number of clicks allowed on a list */
    public static final int DEFAULT_MAX_CLICKS = 6;

    /**
     * The amount of time after settling to wait before autoscrolling to the next page when the user
     * holds down a pagination button.
     */
    protected static final int PAGINATION_HOLD_DELAY_MS = 400;

    private static final String TAG = "PagedListView";
    private static final int INVALID_RESOURCE_ID = -1;

    protected final CarRecyclerView mRecyclerView;
    protected final PagedLayoutManager mLayoutManager;
    protected final Handler mHandler = new Handler();
    private final boolean mScrollBarEnabled;
    private final PagedScrollBarView mScrollBarView;

    private int mRowsPerPage = -1;
    protected RecyclerView.Adapter<? extends RecyclerView.ViewHolder> mAdapter;

    /** Maximum number of pages to show. Values < 0 show all pages. */
    private int mMaxPages = -1;

    protected OnScrollListener mOnScrollListener;

    /** Number of visible rows per page */
    private int mDefaultMaxPages = DEFAULT_MAX_CLICKS;

    /** Used to check if there are more items added to the list. */
    private int mLastItemCount = 0;

    private boolean mNeedsFocus;

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
     * Interface for a {@link android.support.v7.widget.RecyclerView.Adapter} to set the position
     * offset for the adapter to load the data.
     *
     * <p>For example in the adapter, if the positionOffset is 20, then for position 0 it will show
     * the item in position 20 instead, for position 1 it will show the item in position 21 instead
     * and so on.
     */
    // TODO(b/28003781): ItemPositionOffset and ItemCap interfaces should be merged once
    // we enable AlphaJump outside drawer.
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
        mRecyclerView = (CarRecyclerView) findViewById(R.id.recycler_view);
        boolean fadeLastItem = a.getBoolean(R.styleable.PagedListView_fadeLastItem, false);
        mRecyclerView.setFadeLastItem(fadeLastItem);
        boolean offsetRows = a.getBoolean(R.styleable.PagedListView_offsetRows, false);

        mMaxPages = getDefaultMaxPages();

        mLayoutManager = new PagedLayoutManager(context);
        mLayoutManager.setOffsetRows(offsetRows);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setOnScrollListener(mRecyclerViewOnScrollListener);
        mRecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 12);
        mRecyclerView.setItemAnimator(new CarItemAnimator(mLayoutManager));

        boolean offsetScrollBar = a.getBoolean(R.styleable.PagedListView_offsetScrollBar, false);
        if (offsetScrollBar) {
            MarginLayoutParams params = (MarginLayoutParams) mRecyclerView.getLayoutParams();
            params.setMarginStart(getResources().getDimensionPixelSize(
                    R.dimen.car_margin));
            params.setMarginEnd(
                    a.getDimensionPixelSize(R.styleable.PagedListView_listEndMargin, 0));
            mRecyclerView.setLayoutParams(params);
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
        mScrollBarView.setPaginationListener(
                new PagedScrollBarView.PaginationListener() {
                    @Override
                    public void onPaginate(int direction) {
                        if (direction == PagedScrollBarView.PaginationListener.PAGE_UP) {
                            mRecyclerView.pageUp();
                            if (mOnScrollListener != null) {
                                mOnScrollListener.onScrollUpButtonClicked();
                            }
                        } else if (direction == PagedScrollBarView.PaginationListener.PAGE_DOWN) {
                            mRecyclerView.pageDown();
                            if (mOnScrollListener != null) {
                                mOnScrollListener.onScrollDownButtonClicked();
                            }
                        } else {
                            Log.e(TAG, "Unknown pagination direction (" + direction + ")");
                        }
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

        // Modify the layout the Scroll Bar is not visible.
        if (!mScrollBarEnabled) {
            MarginLayoutParams params = (MarginLayoutParams) mRecyclerView.getLayoutParams();
            params.setMarginStart(0);
            mRecyclerView.setLayoutParams(params);
        }

        setDayNightStyle(DayNightStyle.AUTO);
        a.recycle();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeCallbacks(mUpdatePaginationRunnable);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            // The user has interacted with the list using touch. All movements will now paginate
            // the list.
            mLayoutManager.setRowOffsetMode(PagedLayoutManager.ROW_OFFSET_MODE_PAGE);
        }
        return super.onInterceptTouchEvent(e);
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        // The user has interacted with the list using the controller. Movements through the list
        // will now be one row at a time.
        mLayoutManager.setRowOffsetMode(PagedLayoutManager.ROW_OFFSET_MODE_INDIVIDUAL);
    }

    /**
     * Returns the position of the given View in the list.
     *
     * @param v The View to check for.
     * @return The position or -1 if the given View is {@code null} or not in the list.
     */
    public int positionOf(@Nullable View v) {
        if (v == null || v.getParent() != mRecyclerView) {
            return -1;
        }
        return mLayoutManager.getPosition(v);
    }

    private void scroll(int direction) {
        View focusedView = mRecyclerView.getFocusedChild();
        if (focusedView != null) {
            int position = mLayoutManager.getPosition(focusedView);
            int newPosition =
                    Math.max(Math.min(position + direction, mLayoutManager.getItemCount() - 1), 0);
            if (newPosition != position) {
                // newPosition/position are adapter positions.
                // Convert to layout position by subtracting adapter position of view at layout
                // position 0.
                View childAt = mRecyclerView.getChildAt(
                        newPosition - mLayoutManager.getPosition(mLayoutManager.getChildAt(0)));
                if (childAt != null) {
                    childAt.requestFocus();
                }
            }
        }
    }

    private boolean canScroll(int direction) {
        View focusedView = mRecyclerView.getFocusedChild();
        if (focusedView != null) {
            int position = mLayoutManager.getPosition(focusedView);
            int newPosition =
                    Math.max(Math.min(position + direction, mLayoutManager.getItemCount() - 1), 0);
            if (newPosition != position) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    public CarRecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    /**
     * Scrolls to the given position in the PagedListView.
     *
     * @param position The position in the list to scroll to.
     */
    public void scrollToPosition(int position) {
        mLayoutManager.scrollToPosition(position);

        // Sometimes #scrollToPosition doesn't change the scroll state so we need to make sure
        // the pagination arrows actually get updated. See b/http://b/15801119
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
     * <p>It <em>must</em> implement {@link ItemCap}, otherwise, will throw an {@link
     * IllegalArgumentException}.
     */
    public void setAdapter(
            @NonNull RecyclerView.Adapter<? extends RecyclerView.ViewHolder> adapter) {
        if (!(adapter instanceof ItemCap)) {
            throw new IllegalArgumentException("ERROR: adapter ["
                    + adapter.getClass().getCanonicalName() + "] MUST implement ItemCap");
        }

        mAdapter = adapter;
        mRecyclerView.setAdapter(adapter);
        updateMaxItems();
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @NonNull
    public PagedLayoutManager getLayoutManager() {
        return mLayoutManager;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public RecyclerView.Adapter<? extends RecyclerView.ViewHolder> getAdapter() {
        return mRecyclerView.getAdapter();
    }

    /**
     * Sets the maximum number of the pages that can be shown in the PagedListView. The size of a
     * page is  defined as the number of items that fit completely on the screen at once.
     *
     * @param maxPages The maximum number of pages that fit on the screen. Should be positive.
     */
    public void setMaxPages(int maxPages) {
        if (maxPages < 0) {
            return;
        }
        mMaxPages = maxPages;
        updateMaxItems();
    }

    /**
     * Returns the maximum number of pages allowed in the PagedListView. This number is set by
     * {@link #setMaxPages(int)}. If that method has not been called, then this value should match
     * the default value.
     *
     * @return The maximum number of pages to be shown.
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
     * @return The position of first visible child in the list. -1 will be returned if there is no
     *     child.
     */
    public int getFirstFullyVisibleChildPosition() {
        return mLayoutManager.getFirstFullyVisibleChildPosition();
    }

    /**
     * @return The position of last visible child in the list. -1 will be returned if there is no
     *     child.
     */
    public int getLastFullyVisibleChildPosition() {
        return mLayoutManager.getLastFullyVisibleChildPosition();
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
     * Returns the {@link android.support.v7.widget.RecyclerView.ViewHolder} that corresponds to the
     * last child in the PagedListView that is fully visible.
     *
     * @return The corresponding ViewHolder or {@code null} if none exists.
     */
    @Nullable
    public RecyclerView.ViewHolder getLastViewHolder() {
        View lastFullyVisible = mLayoutManager.getLastFullyVisibleChild();
        if (lastFullyVisible == null) {
            return null;
        }
        int lastFullyVisibleAdapterPosition = mLayoutManager.getPosition(lastFullyVisible);
        RecyclerView.ViewHolder lastViewHolder = getRecyclerView()
                .findViewHolderForAdapterPosition(lastFullyVisibleAdapterPosition + 1);
        // We want to get the very last ViewHolder in the list, even if it's only fully visible
        // If it doesn't exist, return the last fully visible ViewHolder.
        if (lastViewHolder == null) {
            lastViewHolder = getRecyclerView()
                    .findViewHolderForAdapterPosition(lastFullyVisibleAdapterPosition);
        }
        return lastViewHolder;
    }

    /**
     * Sets the {@link OnScrollListener} that will be notified of scroll events within the
     * PagedListView.
     *
     * @param listener The scroll listener to set.
     */
    public void setOnScrollListener(OnScrollListener listener) {
        mOnScrollListener = listener;
        mLayoutManager.setOnScrollListener(mOnScrollListener);
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
    }

    /** Returns the default number of pages the list should have */
    protected int getDefaultMaxPages() {
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

        // we need to calculate whether we want to request focus here, before the super call,
        // because after the super call, the first born might be changed.
        View focusedChild = mLayoutManager.getFocusedChild();
        View firstBorn = mLayoutManager.getChildAt(0);

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
     * Returns the View at the given position within the list.
     *
     * @param position A position within the list.
     * @return The View or {@code null} if no View exists at the given position.
     */
    @Nullable
    public View findViewByPosition(int position) {
        return mLayoutManager.findViewByPosition(position);
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
    protected void updatePaginationButtons(boolean animate) {
        if (!mScrollBarEnabled) {
            // Don't change the visibility of the ScrollBar unless it's enabled.
            return;
        }

        if ((mLayoutManager.isAtTop() && mLayoutManager.isAtBottom())
                || mLayoutManager.getItemCount() == 0) {
            mScrollBarView.setVisibility(View.INVISIBLE);
        } else {
            mScrollBarView.setVisibility(View.VISIBLE);
        }
        mScrollBarView.setUpEnabled(shouldEnablePageUpButton());
        mScrollBarView.setDownEnabled(shouldEnablePageDownButton());

        mScrollBarView.setParameters(
                mRecyclerView.computeVerticalScrollRange(),
                mRecyclerView.computeVerticalScrollOffset(),
                mRecyclerView.computeVerticalScrollExtent(),
                animate);
        invalidate();
    }

    protected boolean shouldEnablePageUpButton() {
        return !mLayoutManager.isAtTop();
    }

    protected boolean shouldEnablePageDownButton() {
        return !mLayoutManager.isAtBottom();
    }

    @UiThread
    protected void updateMaxItems() {
        if (mAdapter == null) {
            return;
        }

        final int originalCount = mAdapter.getItemCount();
        updateRowsPerPage();
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

    protected int calculateMaxItemCount() {
        final View firstChild = mLayoutManager.getChildAt(0);
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
    protected void updateRowsPerPage() {
        final View firstChild = mLayoutManager.getChildAt(0);
        if (firstChild == null || firstChild.getHeight() == 0) {
            mRowsPerPage = 1;
        } else {
            mRowsPerPage = Math.max(1, (getHeight() - getPaddingTop()) / firstChild.getHeight());
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.mLayoutManagerState = mLayoutManager.onSaveInstanceState();
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        mLayoutManager.onRestoreInstanceState(savedState.mLayoutManagerState);
        super.onRestoreInstanceState(savedState.getSuperState());
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        // There is the possibility of multiple PagedListViews on a page. This means that the ids
        // of the child Views of PagedListView are no longer unique, and onSaveInstanceState()
        // cannot be used. As a result, PagedListViews needs to manually dispatch the instance
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

    /** The state that will be saved across configuration changes. */
    private static class SavedState extends BaseSavedState {
        /** The state of the {@link #mLayoutManager} of this PagedListView. */
        Parcelable mLayoutManagerState;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            mLayoutManagerState =
                    in.readParcelable(PagedLayoutManager.SavedState.class.getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeParcelable(mLayoutManagerState, flags);
        }

        public static final ClassLoaderCreator<SavedState> CREATOR =
                new ClassLoaderCreator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel source, ClassLoader loader) {
                        return new SavedState(source);
                    }

                    @Override
                    public SavedState createFromParcel(Parcel source) {
                        return createFromParcel(source, null /* loader */);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    private final RecyclerView.OnScrollListener mRecyclerViewOnScrollListener =
            new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (mOnScrollListener != null) {
                        mOnScrollListener.onScrolled(recyclerView, dx, dy);
                        if (!mLayoutManager.isAtTop() && mLayoutManager.isAtBottom()) {
                            mOnScrollListener.onReachBottom();
                        } else if (mLayoutManager.isAtTop() || !mLayoutManager.isAtBottom()) {
                            mOnScrollListener.onLeaveBottom();
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

    protected final Runnable mPaginationRunnable =
            new Runnable() {
                @Override
                public void run() {
                    boolean upPressed = mScrollBarView.isUpPressed();
                    boolean downPressed = mScrollBarView.isDownPressed();
                    if (upPressed && downPressed) {
                        return;
                    }
                    if (upPressed) {
                        mRecyclerView.pageUp();
                    } else if (downPressed) {
                        mRecyclerView.pageDown();
                    }
                }
            };

    private final Runnable mUpdatePaginationRunnable =
            new Runnable() {
                @Override
                public void run() {
                    updatePaginationButtons(true /*animate*/);
                }
            };

    /** Used to listen for {@code PagedListView} scroll events. */
    public abstract static class OnScrollListener {
        /** Called when menu reaches the bottom */
        public void onReachBottom() {}
        /** Called when menu leaves the bottom */
        public void onLeaveBottom() {}
        /** Called when scroll up button is clicked */
        public void onScrollUpButtonClicked() {}
        /** Called when scroll down button is clicked */
        public void onScrollDownButtonClicked() {}
        /** Called when scrolling to the previous page via up gesture */
        public void onGestureUp() {}
        /** Called when scrolling to the next page via down gesture */
        public void onGestureDown() {}

        /**
         * Called when RecyclerView.OnScrollListener#onScrolled is called. See
         * RecyclerView.OnScrollListener
         */
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {}

        /** See RecyclerView.OnScrollListener */
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {}

        /** Called when the view scrolls up a page */
        public void onPageUp() {}

        /** Called when the view scrolls down a page */
        public void onPageDown() {}
    }

    /**
     * A {@link android.support.v7.widget.RecyclerView.ItemDecoration} that will add spacing
     * between each item in the RecyclerView that it is added to.
     */
    private static class ItemSpacingDecoration extends RecyclerView.ItemDecoration {

        private int mHalfItemSpacing;

        private ItemSpacingDecoration(int itemSpacing) {
            mHalfItemSpacing = itemSpacing / 2;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            // Skip top offset for first item and bottom offset for last.
            int position = parent.getChildAdapterPosition(view);
            if (position > 0) {
                outRect.top = mHalfItemSpacing;
            }
            if (position < state.getItemCount() - 1) {
                outRect.bottom = mHalfItemSpacing;
            }
        }

        /**
         * @param itemSpacing sets spacing between each item.
         */
        public void setItemSpacing(int itemSpacing) {
            mHalfItemSpacing = itemSpacing / 2;
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
            mDividerHeight = res.getDimensionPixelSize(R.dimen.car_divider_height);
        }

        /** Updates the list divider color which may have changed due to a day night transition. */
        public void updateDividerColor() {
            mPaint.setColor(mContext.getResources().getColor(R.color.car_list_divider));
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            // Draw a divider line between each item. No need to draw the line for the last item.
            for (int i = 0, childCount = parent.getChildCount(); i < childCount - 1; i++) {
                View container = parent.getChildAt(i);
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

                int left = mDividerStartMargin + startChild.getLeft();
                int right = endChild.getRight();
                int bottom = container.getBottom() + spacing / 2 + mDividerHeight / 2;
                int top = bottom - mDividerHeight;

                c.drawRect(left, top, right, bottom, mPaint);
            }
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            // Skip top offset for first item and bottom offset for last.
            int position = parent.getChildAdapterPosition(view);
            if (position > 0) {
                outRect.top = mDividerHeight / 2;
            }
            if (position < state.getItemCount() - 1) {
                outRect.bottom = mDividerHeight / 2;
            }
        }
    }
}
