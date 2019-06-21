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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.car.R;
import androidx.car.widget.itemdecorators.BottomOffsetDecoration;
import androidx.car.widget.itemdecorators.DividerDecoration;
import androidx.car.widget.itemdecorators.ItemSpacingDecoration;
import androidx.car.widget.itemdecorators.TopOffsetDecoration;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;

/**
 * View that wraps a {@link RecyclerView} and a scroll bar that has
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
     * When doing a snap, offset the snap by this number of position and then do a smooth scroll to
     * the final position.
     */
    private static final int SNAP_SCROLL_OFFSET_POSITION = 2;

    private static final String TAG = "PagedListView";

    private final RecyclerView mRecyclerView;
    private final PagedSnapHelper mSnapHelper;
    final Handler mHandler = new Handler();
    private boolean mScrollBarEnabled;
    @VisibleForTesting
    PagedScrollBarView mScrollBarView;

    /**
     * AlphaJumpOverlayView that will be null until the first time you tap the alpha jump button, at
     * which point we'll construct it and add it to the view hierarchy as a child of this frame
     * layout.
     */
    @Nullable
    private AlphaJumpOverlayView mAlphaJumpView;

    private int mRowsPerPage = -1;
    private RecyclerView.Adapter<? extends RecyclerView.ViewHolder> mAdapter;

    /** Maximum number of pages to show. */
    private int mMaxPages = UNLIMITED_PAGES;

    /** Package private to allow access to nested classes. */
    final List<Callback> mCallbacks = new ArrayList<>();
    OnScrollListener mOnScrollListener;

    /** Used to check if there are more items added to the list. */
    private int mLastItemCount;

    private boolean mNeedsFocus;

    @Nullable
    private OrientationHelper mOrientationHelper;

    @Gutter
    private int mGutter;
    private int mGutterSize;

    /**
     * Interface for a {@link RecyclerView.Adapter} to cap the number of
     * items.
     *
     * <p>NOTE: it is still up to the adapter to use maxItems in {@link
     * RecyclerView.Adapter#getItemCount()}.
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
         * Given an item position, returns whether the divider below that item should be shown.
         *
         * @param position item position inside the adapter.
         * @return {@code true} if divider is to be shown; {@code false} if hidden.
         */
        boolean getShowDivider(int position);
    }

    /**
     * The possible values for @{link #setGutter}. The default value is actually
     * {@link Gutter#BOTH}.
     */
    @IntDef({Gutter.NONE, Gutter.START, Gutter.END, Gutter.BOTH})
    @Retention(SOURCE)
    public @interface Gutter {
        /**
         * No gutter on either side of the list items. The items will span the full width of the
         * {@link PagedListView}, but will not overlap the scroll bar.
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
         * Include a gutter on both sides of the list items. This is the default behavior.
         */
        int BOTH = 3;
    }

    /**
     * Interface for a {@link RecyclerView.Adapter} to set the position
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

    public PagedListView(Context context) {
        this(context, /* attrs= */ null, R.attr.pagedListViewStyle, R.style.Widget_Car_List_Light);
    }

    public PagedListView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.pagedListViewStyle, R.style.Widget_Car_List_Light);
    }

    public PagedListView(Context context, AttributeSet attrs, int defStyleAttrs) {
        this(context, attrs, defStyleAttrs, R.style.Widget_Car_List_Light);
    }

    public PagedListView(Context context, AttributeSet attrs, int defStyleAttrs, int defStyleRes) {
        super(context, attrs, defStyleAttrs, defStyleRes);

        LayoutInflater.from(context).inflate(R.layout.car_paged_recycler_view,
                /* root= */ this, /* attachToRoot= */ true);

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.PagedListView, defStyleAttrs, defStyleRes);
        mRecyclerView = findViewById(R.id.recycler_view);

        RecyclerView.LayoutManager layoutManager =
                new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);

        mSnapHelper = new PagedSnapHelper(context);
        mSnapHelper.attachToRecyclerView(mRecyclerView);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (mOnScrollListener != null) {
                    mOnScrollListener.onScrollStateChanged(recyclerView, newState);
                }
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mHandler.postDelayed(mPaginationRunnable, PAGINATION_HOLD_DELAY_MS);
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (mOnScrollListener != null) {
                    mOnScrollListener.onScrolled(recyclerView, dx, dy);

                    if (!isAtStart() && isAtEnd()) {
                        mOnScrollListener.onReachBottom();
                    }
                }
                if (!isAtStart() && isAtEnd()) {
                    for (Callback callback : mCallbacks) {
                        callback.onReachBottom();
                    }
                }
                updatePaginationButtons(false);
            }
        });
        mRecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 12);

        if (a.getBoolean(R.styleable.PagedListView_verticallyCenterListContent, false)) {
            // Setting the height of wrap_content allows the RecyclerView to center itself.
            mRecyclerView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }

        if (a.getBoolean(R.styleable.PagedListView_showPagedListViewDivider, true)) {
            int dividerStartMargin = a.getDimensionPixelSize(
                    R.styleable.PagedListView_dividerStartMargin, 0);
            int dividerEndMargin = a.getDimensionPixelSize(
                    R.styleable.PagedListView_dividerEndMargin, 0);
            int dividerStartId = a.getResourceId(R.styleable.PagedListView_alignDividerStartTo,
                    DividerDecoration.INVALID_RESOURCE_ID);
            int dividerEndId = a.getResourceId(R.styleable.PagedListView_alignDividerEndTo,
                    DividerDecoration.INVALID_RESOURCE_ID);

            int listDividerColorRes = a.getResourceId(R.styleable.PagedListView_listDividerColor,
                    R.color.car_list_divider);
            int listDividerColor = ContextCompat.getColor(context, listDividerColorRes);

            mRecyclerView.addItemDecoration(new DividerDecoration(context, dividerStartMargin,
                    dividerEndMargin, dividerStartId, dividerEndId, listDividerColor));
        }

        int itemSpacing = a.getDimensionPixelSize(R.styleable.PagedListView_itemSpacing, 0);
        if (itemSpacing > 0) {
            mRecyclerView.addItemDecoration(new ItemSpacingDecoration(itemSpacing));
        }

        int listContentTopOffset =
                a.getDimensionPixelSize(R.styleable.PagedListView_listContentTopOffset, 0);
        if (listContentTopOffset > 0) {
            mRecyclerView.addItemDecoration(new TopOffsetDecoration(listContentTopOffset));
        }

        int listContentBottomOffset =
                a.getDimensionPixelSize(R.styleable.PagedListView_listContentTopOffset, 0);
        if (listContentBottomOffset > 0) {
            mRecyclerView.addItemDecoration(new BottomOffsetDecoration(listContentBottomOffset));
        }

        // Set focusable false explicitly to handle the behavior change in Android O where
        // clickable view becomes focusable by default.
        setFocusable(false);

        mScrollBarEnabled = a.getBoolean(R.styleable.PagedListView_scrollBarEnabled, true);
        mScrollBarView = findViewById(R.id.paged_scroll_view);
        mScrollBarView.setPaginationListener(new PagedScrollBarView.PaginationListener() {
            @Override
            public void onPaginate(int direction) {
                switch (direction) {
                    case PagedScrollBarView.PaginationListener.PAGE_UP:
                        pageUp();
                        for (Callback callback : mCallbacks) {
                            callback.onScrollUpButtonClicked();
                        }
                        if (mOnScrollListener != null) {
                            mOnScrollListener.onScrollUpButtonClicked();
                        }
                        break;
                    case PagedScrollBarView.PaginationListener.PAGE_DOWN:
                        pageDown();
                        for (Callback callback : mCallbacks) {
                            callback.onScrollDownButtonClicked();
                        }
                        if (mOnScrollListener != null) {
                            mOnScrollListener.onScrollDownButtonClicked();
                        }
                        break;
                    default:
                        Log.e(TAG, "Unknown pagination direction (" + direction + ")");
                }
            }

            @Override
            public void onAlphaJump() {
                setAlphaJumpVisible(true);
            }
        });

        if (a.hasValue(R.styleable.PagedListView_scrollBarGravity)) {
            FrameLayout.LayoutParams layoutParams =
                    (FrameLayout.LayoutParams) mScrollBarView.getLayoutParams();
            layoutParams.gravity =
                    a.getInt(R.styleable.PagedListView_scrollBarGravity, Gravity.LEFT);
        }

        mScrollBarView.setVisibility(mScrollBarEnabled ? VISIBLE : GONE);

        if (mScrollBarEnabled) {
            // Use the top margin that is defined in the layout as the default value.
            int topMargin = a.getDimensionPixelSize(
                    R.styleable.PagedListView_scrollBarTopMargin,
                    ((MarginLayoutParams) mScrollBarView.getLayoutParams()).topMargin);
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

        int defaultGutterSize = getResources().getDimensionPixelSize(R.dimen.car_margin);
        mGutterSize = a.getDimensionPixelSize(R.styleable.PagedListView_gutterSize,
                defaultGutterSize);

        // Initialization of the gutter has to come after the scroll bar container width has been
        // set to prevent the internal RecyclerView from overlapping the scroll bar.
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

        // Default starting margin is either the width of the scroll bar if it's enabled or just
        // flush to the edge.
        int startMargin = mScrollBarEnabled ? mScrollBarView.getLayoutParams().width : 0;
        if ((mGutter & Gutter.START) != 0) {
            // Ensure that the gutter value is large enough so that the RecyclerView does not
            // overlap the scroll bar, if it's enabled.
            startMargin = Math.max(mGutterSize, startMargin);
        }

        int endMargin = 0;
        if ((mGutter & Gutter.END) != 0) {
            endMargin = mGutterSize;
        }

        MarginLayoutParams layoutParams = (MarginLayoutParams) mRecyclerView.getLayoutParams();
        layoutParams.setMarginStart(startMargin);
        layoutParams.setMarginEnd(endMargin);

        // requestLayout() isn't sufficient because we also need to resolveLayoutParams().
        mRecyclerView.setLayoutParams(layoutParams);

        // If there's a gutter, set ClipToPadding to false so that CardView's shadow will still
        // appear outside of the padding.
        mRecyclerView.setClipToPadding(startMargin == 0 && endMargin == 0);
    }

    /**
     * Sets the size of the gutter that appears at the start, end or both sizes of the items in
     * the {@code PagedListView}.
     *
     * <p>Note, that if the gutter size given is smaller than the width of the scroll bar container
     * set via {@link #setScrollBarContainerWidth(int)}, then the container width will be used as
     * the gutter at the start. This ensures the scroll bar is never overlapped.
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

        // Ensure that the gutter is updated so that the RecyclerView does not overlap the scroll
        // bar.
        setGutter(mGutter);
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

    /**
     * Set the visibility of scroll bar thumb in the scroll bar, the default visibility is true.
     *
     * @param show Whether to show the scrollbar thumb or not.
     */
    public void setScrollbarThumbEnabled(boolean show) {
        mScrollBarView.setScrollbarThumbEnabled(show);
    }

    /**
     * Returns {@code true} if the scroll bar thumb is visible
     */
    public boolean isScrollbarThumbEnabled() {
        return mScrollBarView.isScrollbarThumbEnabled();
    }

    /**
     * Sets whether the scroll bar is enabled.
     *
     * If enabled, a scroll bar will appear when the number of items causes the PagedListView to
     * be scrollable. Otherwise, the scroll bar is hidden regardless of item count.
     *
     * @param enabled {@code true} to enable the scroll bar.
     */
    public final void setScrollBarEnabled(boolean enabled) {
        mScrollBarEnabled = enabled;
        mScrollBarView.setVisibility(mScrollBarEnabled ? VISIBLE : GONE);
    }

    /**
     * Returns {@code true} if the scroll bar is enabled.
     */
    public final boolean isScrollBarEnabled() {
        return mScrollBarEnabled;
    }

    /**
     * Sets an offset above the first item in the {@code PagedListView}. This offset is scrollable
     * with the contents of the list.
     *
     * @param offset The top offset to add in pixels.
     *
     *               {@link R.attr#listContentTopOffset}
     */
    public void setListContentTopOffset(@Px int offset) {
        TopOffsetDecoration existing = null;
        for (int i = 0, count = mRecyclerView.getItemDecorationCount(); i < count; i++) {
            RecyclerView.ItemDecoration itemDecoration = mRecyclerView.getItemDecorationAt(i);
            if (itemDecoration instanceof TopOffsetDecoration) {
                existing = (TopOffsetDecoration) itemDecoration;
                break;
            }
        }

        if (offset == 0) {
            if (existing != null) {
                mRecyclerView.removeItemDecoration(existing);
            }
        } else if (existing == null) {
            mRecyclerView.addItemDecoration(new TopOffsetDecoration(offset));
        } else {
            existing.setTopOffset(offset);
        }
        mRecyclerView.invalidateItemDecorations();
    }

    /**
     * Returns the top offset of the list that was set by {@link #setListContentTopOffset(int)}. If
     * no top offset was set, 0 is returned.
     *
     * @return The top offset that was set or 0 if none was set.
     */
    public int getListContentTopOffset() {
        for (int i = 0, count = mRecyclerView.getItemDecorationCount(); i < count; i++) {
            RecyclerView.ItemDecoration itemDecoration = mRecyclerView.getItemDecorationAt(i);
            if (itemDecoration instanceof TopOffsetDecoration) {
                return ((TopOffsetDecoration) itemDecoration).getTopOffset();
            }
        }

        return 0;
    }

    /**
     * Sets an offset after the last item in the {@code PagedListView}. This offset is scrollable
     * with the contents of the list.
     *
     * @param offset The bottom offset to add in pixels
     *
     *               {@link R.attr#listContentBottomOffset}
     */
    public void setListContentBottomOffset(@Px int offset) {
        BottomOffsetDecoration existing = null;
        for (int i = 0, count = mRecyclerView.getItemDecorationCount(); i < count; i++) {
            RecyclerView.ItemDecoration itemDecoration = mRecyclerView.getItemDecorationAt(i);
            if (itemDecoration instanceof BottomOffsetDecoration) {
                existing = (BottomOffsetDecoration) itemDecoration;
                break;
            }
        }

        if (offset == 0) {
            if (existing != null) {
                mRecyclerView.removeItemDecoration(existing);
            }
        } else if (existing == null) {
            mRecyclerView.addItemDecoration(new BottomOffsetDecoration(offset));
        } else {
            existing.setBottomOffset(offset);
        }
        mRecyclerView.invalidateItemDecorations();
    }

    /**
     * Returns the bottom offset of the list that was set by
     * {@link #setListContentBottomOffset(int)}. If no top offset was set, 0 is returned.
     *
     * @return The bottom offset that was set or 0 if none was set.
     */
    public int getListContentBottomOffset() {
        for (int i = 0, count = mRecyclerView.getItemDecorationCount(); i < count; i++) {
            RecyclerView.ItemDecoration itemDecoration = mRecyclerView.getItemDecorationAt(i);
            if (itemDecoration instanceof BottomOffsetDecoration) {
                return ((BottomOffsetDecoration) itemDecoration).getBottomOffset();
            }
        }

        return 0;
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

    /**
     * Snap to the given position. This method will snap instantly to a position that's "close" to
     * the given position and then animate a short decelerate to indicate the direction that the
     * snap happened.
     *
     * @param position The position in the list to scroll to.
     */
    public void snapToPosition(int position) {
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();

        if (layoutManager == null) {
            return;
        }

        int startPosition = position;
        if ((layoutManager instanceof RecyclerView.SmoothScroller.ScrollVectorProvider)) {
            PointF vector = ((RecyclerView.SmoothScroller.ScrollVectorProvider) layoutManager)
                    .computeScrollVectorForPosition(position);
            // A positive value in the vector means scrolling down, so should offset by scrolling to
            // an item previous in the list.
            int offsetDirection = (vector == null || vector.y > 0) ? -1 : 1;
            startPosition += offsetDirection * SNAP_SCROLL_OFFSET_POSITION;

            // Clamp the start position.
            startPosition = Math.max(0, Math.min(startPosition, layoutManager.getItemCount() - 1));
        } else {
            // If the LayoutManager doesn't implement ScrollVectorProvider (the default for
            // PagedListView, LinearLayoutManager does, but if the user has overridden it) then we
            // cannot compute the direction we need to scroll. So just snap instantly instead.
            Log.w(TAG, "LayoutManager is not a ScrollVectorProvider, can't do snap animation.");
        }

        if (layoutManager instanceof LinearLayoutManager) {
            ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(startPosition, 0);
        } else {
            layoutManager.scrollToPosition(startPosition);
        }

        if (startPosition != position) {
            // The actual scroll above happens on the next update, so we wait for that to finish
            // before doing the smooth scroll.
            post(() -> scrollToPosition(position));
        }
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
        updateAlphaJump();
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
     * of pages. By default, there is no restriction on the number of pages.
     *
     * <p>Note that for any restriction on maximum pages to work, the adapter passed to this
     * PagedListView needs to implement {@link ItemCap}.
     *
     * @param maxPages The maximum number of pages that fit on the screen. Should be positive or
     *                 {@link #UNLIMITED_PAGES}.
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

    /**
     * Adds an {@link RecyclerView.ItemDecoration} to this PagedListView.
     *
     * @param decor The decoration to add.
     * @see RecyclerView#addItemDecoration(RecyclerView.ItemDecoration)
     */
    public void addItemDecoration(@NonNull RecyclerView.ItemDecoration decor) {
        mRecyclerView.addItemDecoration(decor);
    }

    /**
     * Removes the given {@link RecyclerView.ItemDecoration} from this
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
     * Adds an {@link RecyclerView.OnItemTouchListener} to this
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
     * Removes the given {@link RecyclerView.OnItemTouchListener} from
     * the PagedListView.
     *
     * @param touchListener The touch listener to remove.
     * @see RecyclerView#removeOnItemTouchListener(RecyclerView.OnItemTouchListener)
     */
    public void removeOnItemTouchListener(@NonNull RecyclerView.OnItemTouchListener touchListener) {
        mRecyclerView.removeOnItemTouchListener(touchListener);
    }

    /**
     * Sets the color that should be used for the dividers in the PagedListView.
     *
     * @param dividerColor The packed color int for the divider color.
     */
    public void setDividerColor(@ColorInt int dividerColor) {
        int decorCount = mRecyclerView.getItemDecorationCount();
        for (int i = 0; i < decorCount; i++) {
            RecyclerView.ItemDecoration decor = mRecyclerView.getItemDecorationAt(i);
            if (decor instanceof DividerDecoration) {
                ((DividerDecoration) decor).setDividerColor(dividerColor);
            }
        }
    }

    /**
     * Sets the color of the scrollbar thumb.
     *
     * @param color Resource identifier of the color.
     */
    public void setScrollbarThumbColor(@ColorRes int color) {
        mScrollBarView.setScrollbarThumbColor(color);
    }

    /**
     * Sets the tint color for the up and down buttons of the scrollbar.
     *
     * @param tintResId Resource identifier of the tint color.
     */
    public void setScrollBarButtonTintColor(@ColorRes int tintResId) {
        mScrollBarView.setButtonTintColor(tintResId);
    }

    /**
     * Sets the drawable that will function as the background for the buttons of the scrollbar. This
     * background should provide the ripple.
     *
     * @param backgroundResId The drawable resource identifier for the ripple background.
     */
    public void setScrollBarButtonRippleBackground(@DrawableRes int backgroundResId) {
        mScrollBarView.setButtonRippleBackground(backgroundResId);
    }

    /**
     * Sets the {@link OnScrollListener} that will be notified of scroll events within the
     * PagedListView.
     *
     * @param listener The scroll listener to set.
     * @deprecated Use {@link #addOnScrollListener(RecyclerView.OnScrollListener)} to be notified
     * of scroll events within the PagedListView. To be notified of other PagedListView events, use
     * {@link #registerCallback(Callback)}.
     */
    @Deprecated
    public void setOnScrollListener(OnScrollListener listener) {
        mOnScrollListener = listener;
    }

    /**
     * Adds a {@link RecyclerView.OnScrollListener} that will be notified of scroll events
     * within the PagedListView.
     *
     * @param listener The scroll listener to add.
     */
    public void addOnScrollListener(@NonNull RecyclerView.OnScrollListener listener) {
        mRecyclerView.addOnScrollListener(listener);
    }

    /**
     * Remove a {@link RecyclerView.OnScrollListener} that was notified of scroll events
     * within the PagedListView.
     *
     * @param listener The scroll listener to remove.
     */
    public void removeOnScrollListener(@NonNull RecyclerView.OnScrollListener listener) {
        mRecyclerView.removeOnScrollListener(listener);
    }

    /**
     * Add a {@link Callback} that will be notified of PagedListView events.
     *
     * @param callback The callback to add.
     */
    public void registerCallback(@NonNull Callback callback) {
        mCallbacks.add(callback);
    }

    /**
     * Remove a {@link Callback} that was notified of PagedListView events.
     *
     * @param callback The callback to remove.
     */
    public void unregisterCallback(@NonNull Callback callback) {
        mCallbacks.remove(callback);
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

    private OrientationHelper getOrientationHelper(RecyclerView.LayoutManager layoutManager) {
        if (mOrientationHelper == null || mOrientationHelper.getLayoutManager() != layoutManager) {
            // PagedListView is assumed to be a list that always vertically scrolls.
            mOrientationHelper = OrientationHelper.createVerticalHelper(layoutManager);
        }
        return mOrientationHelper;
    }

    /**
     * Scrolls the contents of the RecyclerView up a page. A page is defined as the height of the
     * {@code PagedListView}.
     *
     * <p>The resulting first item in the list will be snapped to so that it is completely visible.
     * If this is not possible due to the first item being taller than the containing
     * {@code PagedListView}, then the snapping will not occur.
     */
    public void pageUp() {
        if (mRecyclerView.getLayoutManager() == null || mRecyclerView.getChildCount() == 0) {
            return;
        }

        // Use OrientationHelper to calculate scroll distance in order to match snapping behavior.
        OrientationHelper orientationHelper =
                getOrientationHelper(mRecyclerView.getLayoutManager());

        int screenSize = mRecyclerView.getHeight();
        int scrollDistance = screenSize;
        // The iteration order matters. In case where there are 2 items longer than screen size, we
        // want to focus on upcoming view.
        for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
            /*
             * We treat child View longer than screen size differently:
             * 1) When it enters screen, next pageUp will align its bottom with parent bottom;
             * 2) When it leaves screen, next pageUp will align its top with parent top.
             */
            View child = mRecyclerView.getChildAt(i);
            if (child.getHeight() > screenSize) {
                if (orientationHelper.getDecoratedEnd(child) < screenSize) {
                    // Child view bottom is entering screen. Align its bottom with parent bottom.
                    scrollDistance = screenSize - orientationHelper.getDecoratedEnd(child);
                } else if (-screenSize < orientationHelper.getDecoratedStart(child)
                        && orientationHelper.getDecoratedStart(child) < 0) {
                    // Child view top is about to enter screen - its distance to parent top
                    // is less than a full scroll. Align child top with parent top.
                    scrollDistance = Math.abs(orientationHelper.getDecoratedStart(child));
                }
                // There can be two items that are longer than the screen. We stop at the first one.
                // This is affected by the iteration order.
                break;
            }
        }
        // Distance should always be positive. Negate its value to scroll up.
        mRecyclerView.smoothScrollBy(0, -scrollDistance);
    }

    /**
     * Scrolls the contents of the RecyclerView down a page. A page is defined as the height of the
     * {@code PagedListView}.
     *
     * <p>This method will attempt to bring the last item in the list as the first item. If the
     * current first item in the list is taller than the {@code PagedListView}, then it will be
     * scrolled the length of a page, but not snapped to.
     */
    public void pageDown() {
        if (mRecyclerView.getLayoutManager() == null || mRecyclerView.getChildCount() == 0) {
            return;
        }

        OrientationHelper orientationHelper =
                getOrientationHelper(mRecyclerView.getLayoutManager());
        int screenSize = mRecyclerView.getHeight();
        int scrollDistance = screenSize;

        // If the last item is partially visible, page down should bring it to the top.
        View lastChild = mRecyclerView.getChildAt(mRecyclerView.getChildCount() - 1);
        if (mRecyclerView.getLayoutManager().isViewPartiallyVisible(lastChild,
                /* completelyVisible= */ false, /* acceptEndPointInclusion= */ false)) {
            scrollDistance = orientationHelper.getDecoratedStart(lastChild);
            if (scrollDistance <= 0) {
                // - Scroll value is zero if the top of last item is aligned with top of the screen;
                // - Scroll value can be negative if the child is longer than the screen size and
                //   the visible area of the screen does not show the start of the child.
                // Scroll to the next screen in both cases.
                scrollDistance = screenSize;
            }
        }

        // The iteration order matters. In case where there are 2 items longer than screen size, we
        // want to focus on upcoming view (the one at the bottom of screen).
        for (int i = mRecyclerView.getChildCount() - 1; i >= 0; i--) {
            /* We treat child View longer than screen size differently:
             * 1) When it enters screen, next pageDown will align its top with parent top;
             * 2) When it leaves screen, next pageDown will align its bottom with parent bottom.
             */
            View child = mRecyclerView.getChildAt(i);
            if (child.getHeight() > screenSize) {
                if (orientationHelper.getDecoratedStart(child) > 0) {
                    // Child view top is entering screen. Align its top with parent top.
                    scrollDistance = orientationHelper.getDecoratedStart(child);
                } else if (screenSize < orientationHelper.getDecoratedEnd(child)
                        && orientationHelper.getDecoratedEnd(child) < 2 * screenSize) {
                    // Child view bottom is about to enter screen - its distance to parent bottom
                    // is less than a full scroll. Align child bottom with parent bottom.
                    scrollDistance = orientationHelper.getDecoratedEnd(child) - screenSize;
                }
                // There can be two items that are longer than the screen. We stop at the first one.
                // This is affected by the iteration order.
                break;
            }
        }

        mRecyclerView.smoothScrollBy(0, scrollDistance);
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

        if (!mScrollBarEnabled) {
            // Don't change the visibility of the ScrollBar unless it's enabled.
            return;
        }

        boolean isAtStart = isAtStart();
        boolean isAtEnd = isAtEnd();

        if ((isAtStart && isAtEnd) || layoutManager.getItemCount() == 0) {
            mScrollBarView.setVisibility(View.INVISIBLE);
            return;
        }

        mScrollBarView.setVisibility(VISIBLE);
        mScrollBarView.setUpEnabled(!isAtStart);
        mScrollBarView.setDownEnabled(!isAtEnd);

        if (mRecyclerView.getLayoutManager().canScrollVertically()) {
            mScrollBarView.setParametersInLayout(
                    mRecyclerView.computeVerticalScrollRange(),
                    mRecyclerView.computeVerticalScrollOffset(),
                    mRecyclerView.computeVerticalScrollExtent());
        } else {
            mScrollBarView.setParametersInLayout(
                    mRecyclerView.computeHorizontalScrollRange(),
                    mRecyclerView.computeHorizontalScrollOffset(),
                    mRecyclerView.computeHorizontalScrollExtent());
        }
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
    void updatePaginationButtons(boolean animate) {
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
            mScrollBarView.setVisibility(VISIBLE);
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

    private void updateAlphaJump() {
        boolean supportsAlphaJump = (mAdapter instanceof AlphaJumpAdapter);
        mScrollBarView.setShowAlphaJump(supportsAlphaJump);
    }

    /**
     * Returns {@code true} if the Alpha Jump Overlay is shown.
     */
    public boolean isAlphaJumpShown() {
        return mAlphaJumpView != null && mAlphaJumpView.getVisibility() == VISIBLE;
    }

    private void ensureAlphaJumpViewIsChildView() {
        if (mAlphaJumpView == null && mAdapter instanceof AlphaJumpAdapter) {
            mAlphaJumpView = new AlphaJumpOverlayView(getContext());
            mAlphaJumpView.init(this, (AlphaJumpAdapter) mAdapter);
            addView(mAlphaJumpView);
        }
    }

    /**
     * Sets whether the Alpha Jump Overlay is visible.
     *
     * @param visible {@code true} to show the Alpha Jump Overlay or {@code false} to hide it.
     */
    public void setAlphaJumpVisible(boolean visible) {
        if (visible) {
            ensureAlphaJumpViewIsChildView();
            mAlphaJumpView.show();
        } else if (mAlphaJumpView != null) {
            mAlphaJumpView.hide();
        }
    }

    final Runnable mPaginationRunnable =
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

    /** Used to listen for {@code PagedListView} events. */
    public interface Callback {
        /**
         * Called when the {@code PagedListView} has been scrolled so that the last item is
         * completely visible.
         */
        default void onReachBottom() {
        }

        /** Called when scroll up button is clicked */
        default void onScrollUpButtonClicked() {
        }

        /** Called when scroll down button is clicked */
        default void onScrollDownButtonClicked() {
        }
    }

    /**
     * Used to listen for {@code PagedListView} scroll events.
     *
     * @deprecated Use {@link RecyclerView.OnScrollListener} to be notified of scroll events within
     * the PagedListView. To be notified of other PagedListView events, use {@link Callback}.
     */
    @Deprecated
    public abstract static class OnScrollListener {
        /**
         * Called when the {@code PagedListView} has been scrolled so that the last item is
         * completely visible.
         */
        public void onReachBottom() {
        }

        /** Called when scroll up button is clicked */
        public void onScrollUpButtonClicked() {
        }

        /** Called when scroll down button is clicked */
        public void onScrollDownButtonClicked() {
        }

        /**
         * Called when RecyclerView.OnScrollListener#onScrolled is called. See
         * RecyclerView.OnScrollListener
         */
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        }

        /** See RecyclerView.OnScrollListener */
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        }
    }
}
