/*
 * Copyright 2017 The Android Open Source Project
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

import android.content.Context;
import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.car.R;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Recycler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

/**
 * Custom {@link RecyclerView.LayoutManager} that behaves similar to LinearLayoutManager except that
 * it has a few tricks up its sleeve.
 *
 * <ol>
 *   <li>In a normal ListView, when views reach the top of the list, they are clipped. In
 *       PagedLayoutManager, views have the option of flying off of the top of the screen as the
 *       next row settles in to place. This functionality can be enabled or disabled with
 *       {@link #setOffsetRows(boolean)}.
 *   <li>Standard list physics is disabled. Instead, when the user scrolls, it will settle on the
 *       next page.
 *   <li>Items can scroll past the bottom edge of the screen. This helps with pagination so that the
 *       last page can be properly aligned.
 * </ol>
 *
 * This LayoutManger should be used with {@link CarRecyclerView}.
 */
public class PagedLayoutManager extends RecyclerView.LayoutManager {
    private static final String TAG = "PagedLayoutManager";

    /**
     * Any fling below the threshold will just scroll to the top fully visible row. The units is
     * whatever {@link android.widget.Scroller} would return.
     *
     * <p>A reasonable value is ~200
     *
     * <p>This can be disabled by setting the threshold to -1.
     */
    private static final int FLING_THRESHOLD_TO_PAGINATE = -1;

    /**
     * Any fling shorter than this threshold (in px) will just scroll to the top fully visible row.
     *
     * <p>A reasonable value is 15.
     *
     * <p>This can be disabled by setting the distance to -1.
     */
    private static final int DRAG_DISTANCE_TO_PAGINATE = -1;

    /**
     * If you scroll really quickly, you can hit the end of the laid out rows before Android has a
     * chance to layout more. To help counter this, we can layout a number of extra rows past
     * wherever the focus is if necessary.
     */
    private static final int NUM_EXTRA_ROWS_TO_LAYOUT_PAST_FOCUS = 2;

    /**
     * Scroll bar calculation is a bit complicated. This basically defines the granularity we want
     * our scroll bar to move. Set this to 1 means our scrollbar will have really jerky movement.
     * Setting it too big will risk an overflow (although there is no performance impact). Ideally
     * we want to set this higher than the height of our list view. We can't use our list view
     * height directly though because we might run into situations where getHeight() returns 0,
     * for example, when the view is not yet measured.
     */
    private static final int SCROLL_RANGE = 1000;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({BEFORE, AFTER})
    private @interface LayoutDirection {}

    private static final int BEFORE = 0;
    private static final int AFTER = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ROW_OFFSET_MODE_INDIVIDUAL, ROW_OFFSET_MODE_PAGE})
    public @interface RowOffsetMode {}

    public static final int ROW_OFFSET_MODE_INDIVIDUAL = 0;
    public static final int ROW_OFFSET_MODE_PAGE = 1;

    private final AccelerateInterpolator mDanglingRowInterpolator = new AccelerateInterpolator(2);
    private final Context mContext;

    /** Determines whether or not rows will be offset as they slide off screen * */
    private boolean mOffsetRows;

    /** Determines whether rows will be offset individually or a page at a time * */
    @RowOffsetMode private int mRowOffsetMode = ROW_OFFSET_MODE_PAGE;

    /**
     * The LayoutManager only gets {@link #onScrollStateChanged(int)} updates. This enables the
     * scroll state to be used anywhere.
     */
    private int mScrollState = RecyclerView.SCROLL_STATE_IDLE;

    /** Used to inspect the current scroll state to help with the various calculations. */
    private CarSmoothScroller mSmoothScroller;

    private PagedListView.OnScrollListener mOnScrollListener;

    /** The distance that the list has actually scrolled in the most recent drag gesture. */
    private int mLastDragDistance = 0;

    /** {@code True} if the current drag was limited/capped because it was at some boundary. */
    private boolean mReachedLimitOfDrag;

    /** The index of the first item on the current page. */
    private int mAnchorPageBreakPosition = 0;

    /** The index of the first item on the previous page. */
    private int mUpperPageBreakPosition = -1;

    /** The index of the first item on the next page. */
    private int mLowerPageBreakPosition = -1;

    /** Used in the bookkeeping of mario style scrolling to prevent extra calculations. */
    private int mLastChildPositionToRequestFocus = -1;

    private int mSampleViewHeight = -1;

    /** Used for onPageUp and onPageDown */
    private int mViewsPerPage = 1;

    private int mCurrentPage = 0;

    private static final int MAX_ANIMATIONS_IN_CACHE = 30;
    /**
     * Cache of TranslateAnimation per child view. These are needed since using a single animation
     * for all children doesn't apply the animation effect multiple times. Key = the view the
     * animation will transform.
     */
    private LruCache<View, TranslateAnimation> mFlyOffscreenAnimations;

    /** Set the anchor to the following position on the next layout pass. */
    private int mPendingScrollPosition = -1;

    public PagedLayoutManager(Context context) {
        mContext = context;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    /**
     * onLayoutChildren is sort of like a "reset" for the layout state. At a high level, it should:
     *
     * <ol>
     *   <li>Check the current views to get the current state of affairs
     *   <li>Detach all views from the window (a lightweight operation) so that rows not re-added
     *       will be removed after onLayoutChildren.
     *   <li>Re-add rows as necessary.
     * </ol>
     *
     * @see super#onLayoutChildren(RecyclerView.Recycler, RecyclerView.State)
     */
    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        /*
         * The anchor view is the first fully visible view on screen at the beginning of
         * onLayoutChildren (or 0 if there is none). This row will be laid out first. After that,
         * layoutNextRow will layout rows above and below it until the boundaries of what should be
         * laid out have been reached. See shouldLayoutNextRow(View, int) for more info.
         */
        int anchorPosition = 0;
        int anchorTop = -1;
        if (mPendingScrollPosition == -1) {
            View anchor = getFirstFullyVisibleChild();
            if (anchor != null) {
                anchorPosition = getPosition(anchor);
                anchorTop = getDecoratedTop(anchor);
            }
        } else {
            anchorPosition = mPendingScrollPosition;
            mPendingScrollPosition = -1;
            mAnchorPageBreakPosition = anchorPosition;
            mUpperPageBreakPosition = -1;
            mLowerPageBreakPosition = -1;
        }

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(
                    TAG,
                    String.format(
                            ":: onLayoutChildren anchorPosition:%s, anchorTop:%s,"
                                    + " mPendingScrollPosition: %s, mAnchorPageBreakPosition:%s,"
                                    + " mUpperPageBreakPosition:%s, mLowerPageBreakPosition:%s",
                            anchorPosition,
                            anchorTop,
                            mPendingScrollPosition,
                            mAnchorPageBreakPosition,
                            mUpperPageBreakPosition,
                            mLowerPageBreakPosition));
        }

        /*
         * Detach all attached view for 2 reasons:
         *
         * 1) So that views are put in the scrap heap. This enables us to call {@link
         *    RecyclerView.Recycler#getViewForPosition(int)} which will either return one of these
         *    detached views if it is in the scrap heap, one from the recycled pool (will only call
         *    onBind in the adapter), or create an entirely new row if needed (will call onCreate
         *    and onBind in the adapter).
         * 2) So that views are automatically removed if they are not manually re-added.
         */
        detachAndScrapAttachedViews(recycler);

        /*
         * Layout the views recursively.
         *
         * It's possible that this re-layout is triggered because an item gets removed. If the
         * anchor view is at the end of the list, the anchor view position will be bigger than the
         * number of available items. Correct that, and only start the layout if the anchor
         * position is valid.
         */
        anchorPosition = Math.min(anchorPosition, getItemCount() - 1);
        if (anchorPosition >= 0) {
            View anchor = layoutAnchor(recycler, anchorPosition, anchorTop);
            View adjacentRow = anchor;
            while (shouldLayoutNextRow(state, adjacentRow, BEFORE)) {
                adjacentRow = layoutNextRow(recycler, adjacentRow, BEFORE);
            }
            adjacentRow = anchor;
            while (shouldLayoutNextRow(state, adjacentRow, AFTER)) {
                adjacentRow = layoutNextRow(recycler, adjacentRow, AFTER);
            }
        }

        updatePageBreakPositions();
        offsetRows();

        if (Log.isLoggable(TAG, Log.VERBOSE) && getChildCount() > 1) {
            Log.v(TAG, "Currently showing "
                    + getChildCount()
                    + " views "
                    + getPosition(getChildAt(0))
                    + " to "
                    + getPosition(getChildAt(getChildCount() - 1))
                    + " anchor "
                    + anchorPosition);
        }
        // Should be at least 1
        mViewsPerPage =
                Math.max(getLastFullyVisibleChildIndex() + 1 - getFirstFullyVisibleChildIndex(), 1);
        mCurrentPage = getFirstFullyVisibleChildPosition() / mViewsPerPage;
        Log.v(TAG, "viewsPerPage " + mViewsPerPage);
    }

    /**
     * scrollVerticallyBy does the work of what should happen when the list scrolls in addition to
     * handling cases where the list hits the end. It should be lighter weight than
     * onLayoutChildren. It doesn't have to detach all views. It only looks at the end of the list
     * and removes views that have gone out of bounds and lays out new ones that scroll in.
     *
     * @param dy The amount that the list is supposed to scroll. > 0 means the list is scrolling
     *     down. < 0 means the list is scrolling up.
     * @param recycler The recycler that enables views to be reused or created as they scroll in.
     * @param state Various information about the current state of affairs.
     * @return The amount the list actually scrolled.
     * @see super#scrollVerticallyBy(int, RecyclerView.Recycler, RecyclerView.State)
     */
    @Override
    public int scrollVerticallyBy(
            int dy, @NonNull RecyclerView.Recycler recycler, @NonNull RecyclerView.State state) {
        // If the list is empty, we can prevent the overscroll glow from showing by just
        // telling RecycerView that we scrolled.
        if (getItemCount() == 0) {
            return dy;
        }

        // Prevent redundant computations if there is definitely nowhere to scroll to.
        if (getChildCount() <= 1 || dy == 0) {
            mReachedLimitOfDrag = true;
            return 0;
        }

        View firstChild = getChildAt(0);
        if (firstChild == null) {
            mReachedLimitOfDrag = true;
            return 0;
        }
        int firstChildPosition = getPosition(firstChild);
        RecyclerView.LayoutParams firstChildParams = getParams(firstChild);
        int firstChildTopWithMargin = getDecoratedTop(firstChild) - firstChildParams.topMargin;

        View lastFullyVisibleView = getChildAt(getLastFullyVisibleChildIndex());
        if (lastFullyVisibleView == null) {
            mReachedLimitOfDrag = true;
            return 0;
        }
        boolean isLastViewVisible = getPosition(lastFullyVisibleView) == getItemCount() - 1;

        View firstFullyVisibleChild = getFirstFullyVisibleChild();
        if (firstFullyVisibleChild == null) {
            mReachedLimitOfDrag = true;
            return 0;
        }
        int firstFullyVisiblePosition = getPosition(firstFullyVisibleChild);
        RecyclerView.LayoutParams firstFullyVisibleChildParams = getParams(firstFullyVisibleChild);
        int topRemainingSpace =
                getDecoratedTop(firstFullyVisibleChild)
                        - firstFullyVisibleChildParams.topMargin
                        - getPaddingTop();

        if (isLastViewVisible
                && firstFullyVisiblePosition == mAnchorPageBreakPosition
                && dy > topRemainingSpace
                && dy > 0) {
            // Prevent dragging down more than 1 page. As a side effect, this also prevents you
            // from dragging past the bottom because if you are on the second to last page, it
            // prevents you from dragging past the last page.
            dy = topRemainingSpace;
            mReachedLimitOfDrag = true;
        } else if (dy < 0
                && firstChildPosition == 0
                && firstChildTopWithMargin + Math.abs(dy) > getPaddingTop()) {
            // Prevent scrolling past the beginning
            dy = firstChildTopWithMargin - getPaddingTop();
            mReachedLimitOfDrag = true;
        } else {
            mReachedLimitOfDrag = false;
        }

        boolean isDragging = mScrollState == RecyclerView.SCROLL_STATE_DRAGGING;
        if (isDragging) {
            mLastDragDistance += dy;
        }
        // We offset by -dy because the views translate in the opposite direction that the
        // list scrolls (think about it.)
        offsetChildrenVertical(-dy);

        // The last item in the layout should never scroll above the viewport
        View view = getChildAt(getChildCount() - 1);
        if (view.getTop() < 0) {
            view.setTop(0);
        }

        // This is the meat of this function. We remove views on the trailing edge of the scroll
        // and add views at the leading edge as necessary.
        View adjacentRow;
        if (dy > 0) {
            recycleChildrenFromStart(recycler);
            adjacentRow = getChildAt(getChildCount() - 1);
            while (shouldLayoutNextRow(state, adjacentRow, AFTER)) {
                adjacentRow = layoutNextRow(recycler, adjacentRow, AFTER);
            }
        } else {
            recycleChildrenFromEnd(recycler);
            adjacentRow = getChildAt(0);
            while (shouldLayoutNextRow(state, adjacentRow, BEFORE)) {
                adjacentRow = layoutNextRow(recycler, adjacentRow, BEFORE);
            }
        }
        // Now that the correct views are laid out, offset rows as necessary so we can do whatever
        // fancy animation we want such as having the top view fly off the screen as the next one
        // settles in to place.
        updatePageBreakPositions();
        offsetRows();

        if (getChildCount() > 1) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(
                        TAG,
                        String.format(
                                "Currently showing  %d views (%d to %d)",
                                getChildCount(),
                                getPosition(getChildAt(0)),
                                getPosition(getChildAt(getChildCount() - 1))));
            }
        }
        updatePagedState();
        return dy;
    }

    private void updatePagedState() {
        int page = getFirstFullyVisibleChildPosition() / mViewsPerPage;
        if (mOnScrollListener != null) {
            if (page > mCurrentPage) {
                mOnScrollListener.onPageDown();
            } else if (page < mCurrentPage) {
                mOnScrollListener.onPageUp();
            }
        }
        mCurrentPage = page;
    }

    @Override
    public void scrollToPosition(int position) {
        mPendingScrollPosition = position;
        requestLayout();
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state,
            int position) {
        /*
         * startSmoothScroll will handle stopping the old one if there is one. We only keep a copy
         * of it to handle the translation of rows as they slide off the screen in
         * offsetRowsWithPageBreak().
         */
        mSmoothScroller = new CarSmoothScroller(mContext, position);
        mSmoothScroller.setTargetPosition(position);
        startSmoothScroll(mSmoothScroller);
    }

    /** Miscellaneous bookkeeping. */
    @Override
    public void onScrollStateChanged(int state) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, ":: onScrollStateChanged " + state);
        }
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            // If the focused view is off screen, give focus to one that is.
            // If the first fully visible view is first in the list, focus the first item.
            // Otherwise, focus the second so that you have the first item as scrolling context.
            View focusedChild = getFocusedChild();
            if (focusedChild != null
                    && (getDecoratedTop(focusedChild) >= getHeight() - getPaddingBottom()
                    || getDecoratedBottom(focusedChild) <= getPaddingTop())) {
                focusedChild.clearFocus();
                requestLayout();
            }

        } else if (state == RecyclerView.SCROLL_STATE_DRAGGING) {
            mLastDragDistance = 0;
        }

        if (state != RecyclerView.SCROLL_STATE_SETTLING) {
            mSmoothScroller = null;
        }

        mScrollState = state;
        updatePageBreakPositions();
    }

    @Override
    public void onItemsChanged(RecyclerView recyclerView) {
        super.onItemsChanged(recyclerView);
        // When item changed, our sample view height is no longer accurate, and need to be
        // recomputed.
        mSampleViewHeight = -1;
    }

    /**
     * Gives us the opportunity to override the order of the focused views. By default, it will just
     * go from top to bottom. However, if there is no focused views, we take over the logic and
     * start the focused views from the middle of what is visible and move from there until the
     * end of the laid out views in the specified direction.
     */
    @Override
    public boolean onAddFocusables(
            RecyclerView recyclerView, ArrayList<View> views, int direction, int focusableMode) {
        View focusedChild = getFocusedChild();
        if (focusedChild != null) {
            // If there is a view that already has focus, we can just return false and the normal
            // Android addFocusables will work fine.
            return false;
        }

        // Now we know that there isn't a focused view. We need to set up focusables such that
        // instead of just focusing the first item that has been laid out, it focuses starting
        // from a visible item.

        int firstFullyVisibleChildIndex = getFirstFullyVisibleChildIndex();
        if (firstFullyVisibleChildIndex == -1) {
            // Somehow there is a focused view but there is no fully visible view. There shouldn't
            // be a way for this to happen but we'd better stop here and return instead of
            // continuing on with -1.
            Log.w(TAG, "There is a focused child but no first fully visible child.");
            return false;
        }
        View firstFullyVisibleChild = getChildAt(firstFullyVisibleChildIndex);
        int firstFullyVisibleChildPosition = getPosition(firstFullyVisibleChild);

        int firstFocusableChildIndex = firstFullyVisibleChildIndex;
        if (firstFullyVisibleChildPosition > 0 && firstFocusableChildIndex + 1 < getItemCount()) {
            // We are somewhere in the middle of the list. Instead of starting focus on the first
            // item, start focus on the second item to give some context that we aren't at
            // the beginning.
            firstFocusableChildIndex++;
        }

        if (direction == View.FOCUS_FORWARD) {
            // Iterate from the first focusable view to the end.
            for (int i = firstFocusableChildIndex; i < getChildCount(); i++) {
                views.add(getChildAt(i));
            }
            return true;
        } else if (direction == View.FOCUS_BACKWARD) {
            // Iterate from the first focusable view to the beginning.
            for (int i = firstFocusableChildIndex; i >= 0; i--) {
                views.add(getChildAt(i));
            }
            return true;
        } else if (direction == View.FOCUS_DOWN) {
            // Framework calls onAddFocusables with FOCUS_DOWN direction when the focus is first
            // gained. Thereafter, it calls onAddFocusables with FOCUS_FORWARD or FOCUS_BACKWARD.
            // First we try to put the focus back on the last focused item, if it is visible
            int lastFocusedVisibleChildIndex = getLastFocusedChildIndexIfVisible();
            if (lastFocusedVisibleChildIndex != -1) {
                views.add(getChildAt(lastFocusedVisibleChildIndex));
                return true;
            }
        }
        return false;
    }

    @Override
    public View onFocusSearchFailed(
            View focused, int direction, RecyclerView.Recycler recycler, RecyclerView.State state) {
        // This doesn't seem to get called the way focus is handled in gearhead...
        return null;
    }

    /**
     * This is the function that decides where to scroll to when a new view is focused. You can get
     * the position of the currently focused child through the child parameter. Once you have that,
     * determine where to smooth scroll to and scroll there.
     *
     * @param parent The RecyclerView hosting this LayoutManager
     * @param state Current state of RecyclerView
     * @param child Direct child of the RecyclerView containing the newly focused view
     * @param focused The newly focused view. This may be the same view as child or it may be null
     * @return {@code true} if the default scroll behavior should be suppressed
     */
    @Override
    public boolean onRequestChildFocus(
            RecyclerView parent, RecyclerView.State state, View child, View focused) {
        if (child == null) {
            Log.w(TAG, "onRequestChildFocus with a null child!");
            return true;
        }

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, String.format(":: onRequestChildFocus child: %s, focused: %s", child,
                    focused));
        }

        return onRequestChildFocusMarioStyle(parent, child);
    }

    /**
     * Goal: the scrollbar maintains the same size throughout scrolling and that the scrollbar
     * reaches the bottom of the screen when the last item is fully visible. This is because there
     * are multiple points that could be considered the bottom since the last item can scroll past
     * the bottom edge of the screen.
     *
     * <p>To find the extent, we divide the number of items that can fit on screen by the number of
     * items in total.
     */
    @Override
    public int computeVerticalScrollExtent(RecyclerView.State state) {
        if (getChildCount() <= 1) {
            return 0;
        }

        int sampleViewHeight = getSampleViewHeight();
        int availableHeight = getAvailableHeight();
        int sampleViewsThatCanFitOnScreen = availableHeight / sampleViewHeight;

        if (state.getItemCount() <= sampleViewsThatCanFitOnScreen) {
            return SCROLL_RANGE;
        } else {
            return SCROLL_RANGE * sampleViewsThatCanFitOnScreen / state.getItemCount();
        }
    }

    /**
     * The scrolling offset is calculated by determining what position is at the top of the list.
     * However, instead of using fixed integer positions for each row, the scroll position is
     * factored in and the position is recalculated as a float that takes in to account the
     * current scroll state. This results in a smooth animation for the scrollbar when the user
     * scrolls the list.
     */
    @Override
    public int computeVerticalScrollOffset(RecyclerView.State state) {
        View firstChild = getFirstFullyVisibleChild();
        if (firstChild == null) {
            return 0;
        }

        RecyclerView.LayoutParams params = getParams(firstChild);
        int firstChildPosition = getPosition(firstChild);
        float previousChildHieght = (float) (getDecoratedMeasuredHeight(firstChild)
                + params.topMargin + params.bottomMargin);

        // Assume the previous view is the same height as the current one.
        float percentOfPreviousViewShowing = (getDecoratedTop(firstChild) - params.topMargin)
                / previousChildHieght;
        // If the previous view is actually larger than the current one then this the percent
        // can be greater than 1.
        percentOfPreviousViewShowing = Math.min(percentOfPreviousViewShowing, 1);

        float currentPosition = (float) firstChildPosition - percentOfPreviousViewShowing;

        int sampleViewHeight = getSampleViewHeight();
        int availableHeight = getAvailableHeight();
        int numberOfSampleViewsThatCanFitOnScreen = availableHeight / sampleViewHeight;
        int positionWhenLastItemIsVisible =
                state.getItemCount() - numberOfSampleViewsThatCanFitOnScreen;

        if (positionWhenLastItemIsVisible <= 0) {
            return 0;
        }

        if (currentPosition >= positionWhenLastItemIsVisible) {
            return SCROLL_RANGE;
        }

        return (int) (SCROLL_RANGE * currentPosition / positionWhenLastItemIsVisible);
    }

    /**
     * The range of the scrollbar can be understood as the granularity of how we want the scrollbar
     * to scroll.
     */
    @Override
    public int computeVerticalScrollRange(RecyclerView.State state) {
        return SCROLL_RANGE;
    }

    @Override
    public void onAttachedToWindow(RecyclerView view) {
        super.onAttachedToWindow(view);
        // The purpose of calling this is so that any animation offsets are re-applied. These are
        // cleared in View.onDetachedFromWindow().
        // This fixes b/27672379
        updatePageBreakPositions();
        offsetRows();
    }

    @Override
    public void onDetachedFromWindow(RecyclerView recyclerView, Recycler recycler) {
        super.onDetachedFromWindow(recyclerView, recycler);
    }

    /**
     * @return The first view that starts on screen. It assumes that it fully fits on the screen
     *     though. If the first fully visible child is also taller than the screen then it will
     *     still be returned. However, since the LayoutManager snaps to view starts, having a row
     *     that tall would lead to a broken experience anyways.
     */
    public int getFirstFullyVisibleChildIndex() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            RecyclerView.LayoutParams params = getParams(child);
            if (getDecoratedTop(child) - params.topMargin >= getPaddingTop()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return The position of first visible child in the list. -1 will be returned if there is no
     *     child.
     */
    public int getFirstFullyVisibleChildPosition() {
        View child = getFirstFullyVisibleChild();
        if (child == null) {
            return -1;
        }
        return getPosition(child);
    }

    /**
     * @return The position of last visible child in the list. -1 will be returned if there is no
     *     child.
     */
    public int getLastFullyVisibleChildPosition() {
        View child = getLastFullyVisibleChild();
        if (child == null) {
            return -1;
        }
        return getPosition(child);
    }

    /** @return The first View that is completely visible on-screen. */
    public View getFirstFullyVisibleChild() {
        int firstFullyVisibleChildIndex = getFirstFullyVisibleChildIndex();
        View firstChild = null;
        if (firstFullyVisibleChildIndex != -1) {
            firstChild = getChildAt(firstFullyVisibleChildIndex);
        }
        return firstChild;
    }

    /** @return The last View that is completely visible on-screen. */
    public View getLastFullyVisibleChild() {
        int lastFullyVisibleChildIndex = getLastFullyVisibleChildIndex();
        View lastChild = null;
        if (lastFullyVisibleChildIndex != -1) {
            lastChild = getChildAt(lastFullyVisibleChildIndex);
        }
        return lastChild;
    }

    /**
     * @return The last view that ends on screen. It assumes that the start is also on screen
     *     though. If the last fully visible child is also taller than the screen then it will
     *     still be returned. However, since the LayoutManager snaps to view starts, having a row
     *     that tall would lead to a broken experience anyways.
     */
    public int getLastFullyVisibleChildIndex() {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            RecyclerView.LayoutParams params = getParams(child);
            int childBottom = getDecoratedBottom(child) + params.bottomMargin;
            int listBottom = getHeight() - getPaddingBottom();
            if (childBottom <= listBottom) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the index of the child in the list that was last focused and is currently visible to
     * the user. If no child is found, returns -1.
     */
    public int getLastFocusedChildIndexIfVisible() {
        if (mLastChildPositionToRequestFocus == -1) {
            return -1;
        }
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (getPosition(child) == mLastChildPositionToRequestFocus) {
                RecyclerView.LayoutParams params = getParams(child);
                int childBottom = getDecoratedBottom(child) + params.bottomMargin;
                int listBottom = getHeight() - getPaddingBottom();
                if (childBottom <= listBottom) {
                    return i;
                }
                break;
            }
        }
        return -1;
    }

    /** @return Whether or not the first view is fully visible. */
    public boolean isAtTop() {
        // getFirstFullyVisibleChildIndex() can return -1 which indicates that there are no views
        // and also means that the list is at the top.
        return getFirstFullyVisibleChildIndex() <= 0;
    }

    /** @return Whether or not the last view is fully visible. */
    public boolean isAtBottom() {
        int lastFullyVisibleChildIndex = getLastFullyVisibleChildIndex();
        if (lastFullyVisibleChildIndex == -1) {
            return true;
        }
        View lastFullyVisibleChild = getChildAt(lastFullyVisibleChildIndex);
        return getPosition(lastFullyVisibleChild) == getItemCount() - 1;
    }

    /**
     * Sets whether or not the rows have an offset animation when it scrolls off-screen. The type
     * of offset is determined by {@link #setRowOffsetMode(int)}.
     *
     * <p>A row being offset means that when they reach the top of the screen, the row is flung off
     * respectively to the rest of the list. This creates a gap between the offset row(s) and the
     * list.
     *
     * @param offsetRows {@code true} if the rows should be offset.
     */
    public void setOffsetRows(boolean offsetRows) {
        mOffsetRows = offsetRows;
        if (offsetRows) {
            // Card animation offsets are only needed when we use the flying off the screen effect
            if (mFlyOffscreenAnimations == null) {
                mFlyOffscreenAnimations = new LruCache<>(MAX_ANIMATIONS_IN_CACHE);
            }
            offsetRows();
        } else {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                setCardFlyingEffectOffset(getChildAt(i), 0);
            }
            mFlyOffscreenAnimations = null;
        }
    }

    /**
     * Sets the manner of offsetting the rows when they are scrolled off-screen. The rows are either
     * offset individually or the entire page being scrolled off is offset.
     *
     * @param mode One of {@link #ROW_OFFSET_MODE_INDIVIDUAL} or {@link #ROW_OFFSET_MODE_PAGE}.
     */
    public void setRowOffsetMode(@RowOffsetMode int mode) {
        if (mode == mRowOffsetMode) {
            return;
        }

        mRowOffsetMode = mode;
        offsetRows();
    }

    /**
     * Sets the listener that will be notified of various scroll events in the list.
     *
     * @param listener The on-scroll listener.
     */
    public void setOnScrollListener(PagedListView.OnScrollListener listener) {
        mOnScrollListener = listener;
    }

    /**
     * Finish the pagination taking into account where the gesture started (not where we are now).
     *
     * @return Whether the list was scrolled as a result of the fling.
     */
    public boolean settleScrollForFling(RecyclerView parent, int flingVelocity) {
        if (getChildCount() == 0) {
            return false;
        }

        if (mReachedLimitOfDrag) {
            return false;
        }

        // If the fling was too slow or too short, settle on the first fully visible row instead.
        if (Math.abs(flingVelocity) <= FLING_THRESHOLD_TO_PAGINATE
                || Math.abs(mLastDragDistance) <= DRAG_DISTANCE_TO_PAGINATE) {
            int firstFullyVisibleChildIndex = getFirstFullyVisibleChildIndex();
            if (firstFullyVisibleChildIndex != -1) {
                int scrollPosition = getPosition(getChildAt(firstFullyVisibleChildIndex));
                parent.smoothScrollToPosition(scrollPosition);
                return true;
            }
            return false;
        }

        // Finish the pagination taking into account where the gesture
        // started (not where we are now).
        boolean isDownGesture = flingVelocity > 0 || (flingVelocity == 0 && mLastDragDistance >= 0);
        boolean isUpGesture = flingVelocity < 0 || (flingVelocity == 0 && mLastDragDistance < 0);
        if (isDownGesture && mLowerPageBreakPosition != -1) {
            // If the last view is fully visible then only settle on the first fully visible view
            // instead of the original page down position. However, don't page down if the last
            // item has come fully into view.
            parent.smoothScrollToPosition(mAnchorPageBreakPosition);
            if (mOnScrollListener != null) {
                mOnScrollListener.onGestureDown();
            }
            return true;
        } else if (isUpGesture && mUpperPageBreakPosition != -1) {
            parent.smoothScrollToPosition(mUpperPageBreakPosition);
            if (mOnScrollListener != null) {
                mOnScrollListener.onGestureUp();
            }
            return true;
        } else {
            Log.e(
                    TAG,
                    "Error setting scroll for fling! flingVelocity: \t"
                            + flingVelocity
                            + "\tlastDragDistance: "
                            + mLastDragDistance
                            + "\tpageUpAtStartOfDrag: "
                            + mUpperPageBreakPosition
                            + "\tpageDownAtStartOfDrag: "
                            + mLowerPageBreakPosition);
            // As a last resort, at the last smooth scroller target position if there is one.
            if (mSmoothScroller != null) {
                parent.smoothScrollToPosition(mSmoothScroller.getTargetPosition());
                return true;
            }
        }
        return false;
    }

    /** @return The position that paging up from the current position would settle at. */
    public int getPageUpPosition() {
        return mUpperPageBreakPosition;
    }

    /** @return The position that paging down from the current position would settle at. */
    public int getPageDownPosition() {
        return mLowerPageBreakPosition;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState();
        savedState.mFirstChildPosition = getFirstFullyVisibleChildPosition();
        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            scrollToPosition(((SavedState) state).mFirstChildPosition);
        }
    }

    /** The state that will be saved across configuration changes. */
    static class SavedState implements Parcelable {
        /** The position of the first visible child view in the list. */
        int mFirstChildPosition;

        SavedState() {}

        private SavedState(Parcel in) {
            mFirstChildPosition = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mFirstChildPosition);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    /**
     * Layout the anchor row. The anchor row is the first fully visible row.
     *
     * @param anchorTop The decorated top of the anchor. If it is not known or should be reset to
     *     the top, pass -1.
     */
    private View layoutAnchor(RecyclerView.Recycler recycler, int anchorPosition, int anchorTop) {
        View anchor = recycler.getViewForPosition(anchorPosition);
        RecyclerView.LayoutParams params = getParams(anchor);
        measureChildWithMargins(anchor, 0, 0);
        int left = getPaddingLeft() + params.leftMargin;
        int top = (anchorTop == -1) ? params.topMargin : anchorTop;
        int right = left + getDecoratedMeasuredWidth(anchor);
        int bottom = top + getDecoratedMeasuredHeight(anchor);
        layoutDecorated(anchor, left, top, right, bottom);
        addView(anchor);
        return anchor;
    }

    /**
     * Lays out the next row in the specified direction next to the specified adjacent row.
     *
     * @param recycler The recycler from which a new view can be created.
     * @param adjacentRow The View of the adjacent row which will be used to position the new one.
     * @param layoutDirection The side of the adjacent row that the new row will be laid out on.
     * @return The new row that was laid out.
     */
    private View layoutNextRow(RecyclerView.Recycler recycler, View adjacentRow,
            @LayoutDirection int layoutDirection) {
        int adjacentRowPosition = getPosition(adjacentRow);
        int newRowPosition = adjacentRowPosition;
        if (layoutDirection == BEFORE) {
            newRowPosition = adjacentRowPosition - 1;
        } else if (layoutDirection == AFTER) {
            newRowPosition = adjacentRowPosition + 1;
        }

        // Because we detach all rows in onLayoutChildren, this will often just return a view from
        // the scrap heap.
        View newRow = recycler.getViewForPosition(newRowPosition);

        measureChildWithMargins(newRow, 0, 0);
        RecyclerView.LayoutParams newRowParams =
                (RecyclerView.LayoutParams) newRow.getLayoutParams();
        RecyclerView.LayoutParams adjacentRowParams =
                (RecyclerView.LayoutParams) adjacentRow.getLayoutParams();
        int left = getPaddingLeft() + newRowParams.leftMargin;
        int right = left + getDecoratedMeasuredWidth(newRow);
        int top;
        int bottom;
        if (layoutDirection == BEFORE) {
            bottom = adjacentRow.getTop() - adjacentRowParams.topMargin - newRowParams.bottomMargin;
            top = bottom - getDecoratedMeasuredHeight(newRow);
        } else {
            top = getDecoratedBottom(adjacentRow) + adjacentRowParams.bottomMargin
                    + newRowParams.topMargin;
            bottom = top + getDecoratedMeasuredHeight(newRow);
        }
        layoutDecorated(newRow, left, top, right, bottom);

        if (layoutDirection == BEFORE) {
            addView(newRow, 0);
        } else {
            addView(newRow);
        }

        return newRow;
    }

    /** @return Whether another row should be laid out in the specified direction. */
    private boolean shouldLayoutNextRow(
            RecyclerView.State state, View adjacentRow, @LayoutDirection int layoutDirection) {
        int adjacentRowPosition = getPosition(adjacentRow);

        if (layoutDirection == BEFORE) {
            if (adjacentRowPosition == 0) {
                // We already laid out the first row.
                return false;
            }
        } else if (layoutDirection == AFTER) {
            if (adjacentRowPosition >= state.getItemCount() - 1) {
                // We already laid out the last row.
                return false;
            }
        }

        // If we are scrolling layout views until the target position.
        if (mSmoothScroller != null) {
            if (layoutDirection == BEFORE
                    && adjacentRowPosition >= mSmoothScroller.getTargetPosition()) {
                return true;
            } else if (layoutDirection == AFTER
                    && adjacentRowPosition <= mSmoothScroller.getTargetPosition()) {
                return true;
            }
        }

        View focusedRow = getFocusedChild();
        if (focusedRow != null) {
            int focusedRowPosition = getPosition(focusedRow);
            if (layoutDirection == BEFORE && adjacentRowPosition
                    >= focusedRowPosition - NUM_EXTRA_ROWS_TO_LAYOUT_PAST_FOCUS) {
                return true;
            } else if (layoutDirection == AFTER && adjacentRowPosition
                    <= focusedRowPosition + NUM_EXTRA_ROWS_TO_LAYOUT_PAST_FOCUS) {
                return true;
            }
        }

        RecyclerView.LayoutParams params = getParams(adjacentRow);
        int adjacentRowTop = getDecoratedTop(adjacentRow) - params.topMargin;
        int adjacentRowBottom = getDecoratedBottom(adjacentRow) - params.bottomMargin;
        if (layoutDirection == BEFORE && adjacentRowTop < getPaddingTop() - getHeight()) {
            // View is more than 1 page past the top of the screen and also past where the user has
            // scrolled to. We want to keep one page past the top to make the scroll up calculation
            // easier and scrolling smoother.
            return false;
        } else if (layoutDirection == AFTER
                && adjacentRowBottom > getHeight() - getPaddingBottom()) {
            // View is off of the bottom and also past where the user has scrolled to.
            return false;
        }

        return true;
    }

    /** Remove and recycle views that are no longer needed. */
    private void recycleChildrenFromStart(RecyclerView.Recycler recycler) {
        // Start laying out children one page before the top of the viewport.
        int childrenStart = getPaddingTop() - getHeight();

        int focusedChildPosition = Integer.MAX_VALUE;
        View focusedChild = getFocusedChild();
        if (focusedChild != null) {
            focusedChildPosition = getPosition(focusedChild);
        }

        // Count the number of views that should be removed.
        int detachedCount = 0;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            int childEnd = getDecoratedBottom(child);
            int childPosition = getPosition(child);

            if (childEnd >= childrenStart || childPosition >= focusedChildPosition - 1) {
                break;
            }

            detachedCount++;
        }

        // Remove the number of views counted above. Done by removing the first child n times.
        while (--detachedCount >= 0) {
            final View child = getChildAt(0);
            removeAndRecycleView(child, recycler);
        }
    }

    /** Remove and recycle views that are no longer needed. */
    private void recycleChildrenFromEnd(RecyclerView.Recycler recycler) {
        // Layout views until the end of the viewport.
        int childrenEnd = getHeight();

        int focusedChildPosition = Integer.MIN_VALUE + 1;
        View focusedChild = getFocusedChild();
        if (focusedChild != null) {
            focusedChildPosition = getPosition(focusedChild);
        }

        // Count the number of views that should be removed.
        int firstDetachedPos = 0;
        int detachedCount = 0;
        int childCount = getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            int childStart = getDecoratedTop(child);
            int childPosition = getPosition(child);

            if (childStart <= childrenEnd || childPosition <= focusedChildPosition - 1) {
                break;
            }

            firstDetachedPos = i;
            detachedCount++;
        }

        while (--detachedCount >= 0) {
            final View child = getChildAt(firstDetachedPos);
            removeAndRecycleView(child, recycler);
        }
    }

    /**
     * Offset rows to do fancy animations. If offset rows was not enabled with
     * {@link #setOffsetRows}, this will do nothing.
     *
     * @see #offsetRowsIndividually
     * @see #offsetRowsByPage
     * @see #setOffsetRows
     */
    public void offsetRows() {
        if (!mOffsetRows) {
            return;
        }

        if (mRowOffsetMode == ROW_OFFSET_MODE_PAGE) {
            offsetRowsByPage();
        } else if (mRowOffsetMode == ROW_OFFSET_MODE_INDIVIDUAL) {
            offsetRowsIndividually();
        }
    }

    /**
     * Offset the single row that is scrolling off the screen such that by the time the next row
     * reaches the top, it will have accelerated completely off of the screen.
     */
    private void offsetRowsIndividually() {
        if (getChildCount() == 0) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, ":: offsetRowsIndividually getChildCount=0");
            }
            return;
        }

        // Identify the dangling row. It will be the first row that is at the top of the
        // list or above.
        int danglingChildIndex = -1;
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (getDecoratedTop(child) - getParams(child).topMargin <= getPaddingTop()) {
                danglingChildIndex = i;
                break;
            }
        }

        mAnchorPageBreakPosition = danglingChildIndex;

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, ":: offsetRowsIndividually danglingChildIndex: " + danglingChildIndex);
        }

        // Calculate the total amount that the view will need to scroll in order to go completely
        // off screen.
        RecyclerView rv = (RecyclerView) getChildAt(0).getParent();
        int[] locs = new int[2];
        rv.getLocationInWindow(locs);
        int listTopInWindow = locs[1] + rv.getPaddingTop();
        int maxDanglingViewTranslation;

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            RecyclerView.LayoutParams params = getParams(child);

            maxDanglingViewTranslation = listTopInWindow;
            // If the child has a negative margin, we'll actually need to translate the view a
            // little but further to get it completely off screen.
            if (params.topMargin < 0) {
                maxDanglingViewTranslation -= params.topMargin;
            }
            if (params.bottomMargin < 0) {
                maxDanglingViewTranslation -= params.bottomMargin;
            }

            if (i < danglingChildIndex) {
                child.setAlpha(0f);
            } else if (i > danglingChildIndex) {
                child.setAlpha(1f);
                setCardFlyingEffectOffset(child, 0);
            } else {
                int totalScrollDistance =
                        getDecoratedMeasuredHeight(child) + params.topMargin + params.bottomMargin;

                int distanceLeftInScroll =
                        getDecoratedBottom(child) + params.bottomMargin - getPaddingTop();
                float percentageIntoScroll = 1 - distanceLeftInScroll / (float) totalScrollDistance;
                float interpolatedPercentage =
                        mDanglingRowInterpolator.getInterpolation(percentageIntoScroll);

                child.setAlpha(1f);
                setCardFlyingEffectOffset(child, -(maxDanglingViewTranslation
                        * interpolatedPercentage));
            }
        }
    }

    /**
     * When the list scrolls, the entire page of rows will offset in one contiguous block. This
     * significantly reduces the amount of extra motion at the top of the screen.
     */
    private void offsetRowsByPage() {
        View anchorView = findViewByPosition(mAnchorPageBreakPosition);
        if (anchorView == null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, ":: offsetRowsByPage anchorView null");
            }
            return;
        }
        int anchorViewTop = getDecoratedTop(anchorView) - getParams(anchorView).topMargin;

        View upperPageBreakView = findViewByPosition(mUpperPageBreakPosition);
        int upperViewTop =
                getDecoratedTop(upperPageBreakView) - getParams(upperPageBreakView).topMargin;

        int scrollDistance = upperViewTop - anchorViewTop;

        int distanceLeft = anchorViewTop - getPaddingTop();
        float scrollPercentage =
                (Math.abs(scrollDistance) - distanceLeft) / (float) Math.abs(scrollDistance);

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, String.format(":: offsetRowsByPage scrollDistance:%s, distanceLeft:%s, "
                            + "scrollPercentage:%s",
                    scrollDistance, distanceLeft, scrollPercentage));
        }

        // Calculate the total amount that the view will need to scroll in order to go completely
        // off screen.
        RecyclerView rv = (RecyclerView) getChildAt(0).getParent();
        int[] locs = new int[2];
        rv.getLocationInWindow(locs);
        int listTopInWindow = locs[1] + rv.getPaddingTop();

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            int position = getPosition(child);
            if (position < mUpperPageBreakPosition) {
                child.setAlpha(0f);
                setCardFlyingEffectOffset(child, -listTopInWindow);
            } else if (position < mAnchorPageBreakPosition) {
                // If the child has a negative margin, we need to offset the row by a little bit
                // extra so that it moves completely off screen.
                RecyclerView.LayoutParams params = getParams(child);
                int extraTranslation = 0;
                if (params.topMargin < 0) {
                    extraTranslation -= params.topMargin;
                }
                if (params.bottomMargin < 0) {
                    extraTranslation -= params.bottomMargin;
                }
                int translation = (int) ((listTopInWindow + extraTranslation)
                        * mDanglingRowInterpolator.getInterpolation(scrollPercentage));
                child.setAlpha(1f);
                setCardFlyingEffectOffset(child, -translation);
            } else {
                child.setAlpha(1f);
                setCardFlyingEffectOffset(child, 0);
            }
        }
    }

    /**
     * Apply an offset to this view. This offset is applied post-layout so it doesn't affect when
     * views are recycled
     *
     * @param child The view to apply this to
     * @param verticalOffset The offset for this child.
     */
    private void setCardFlyingEffectOffset(View child, float verticalOffset) {
        // Ideally instead of doing all this, we could use View.setTranslationY(). However, the
        // default RecyclerView.ItemAnimator also uses this method which causes layout issues.
        // See: http://b/25977087
        TranslateAnimation anim = mFlyOffscreenAnimations.get(child);
        if (anim == null) {
            anim = new TranslateAnimation();
            anim.setFillEnabled(true);
            anim.setFillAfter(true);
            anim.setDuration(0);
            mFlyOffscreenAnimations.put(child, anim);
        } else if (anim.verticalOffset == verticalOffset) {
            return;
        }

        anim.reset();
        anim.verticalOffset = verticalOffset;
        anim.setStartTime(Animation.START_ON_FIRST_FRAME);
        child.setAnimation(anim);
        anim.startNow();
    }

    /**
     * Update the page break positions based on the position of the views on screen. This should be
     * called whenever view move or change such as during a scroll or layout.
     */
    private void updatePageBreakPositions() {
        if (getChildCount() == 0) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, ":: updatePageBreakPosition getChildCount: 0");
            }
            return;
        }

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, String.format(":: #BEFORE updatePageBreakPositions "
                            + "mAnchorPageBreakPosition:%s, mUpperPageBreakPosition:%s, "
                            + "mLowerPageBreakPosition:%s",
                            mAnchorPageBreakPosition, mUpperPageBreakPosition,
                            mLowerPageBreakPosition));
        }

        mAnchorPageBreakPosition = getPosition(getFirstFullyVisibleChild());

        if (mAnchorPageBreakPosition == -1) {
            Log.w(TAG, "Unable to update anchor positions. There is no anchor position.");
            return;
        }

        View anchorPageBreakView = findViewByPosition(mAnchorPageBreakPosition);
        if (anchorPageBreakView == null) {
            return;
        }
        int topMargin = getParams(anchorPageBreakView).topMargin;
        int anchorTop = getDecoratedTop(anchorPageBreakView) - topMargin;
        View upperPageBreakView = findViewByPosition(mUpperPageBreakPosition);
        int upperPageBreakTop = upperPageBreakView == null
                ? Integer.MIN_VALUE
                : getDecoratedTop(upperPageBreakView) - getParams(upperPageBreakView).topMargin;

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, String.format(":: #MID updatePageBreakPositions topMargin:%s, anchorTop:%s"
                            + " mAnchorPageBreakPosition:%s, mUpperPageBreakPosition:%s,"
                            + " mLowerPageBreakPosition:%s",
                            topMargin,
                            anchorTop,
                            mAnchorPageBreakPosition,
                            mUpperPageBreakPosition,
                            mLowerPageBreakPosition));
        }

        if (anchorTop < getPaddingTop()) {
            // The anchor has moved above the viewport. We are now on the next page. Shift the page
            // break positions and calculate a new lower one.
            mUpperPageBreakPosition = mAnchorPageBreakPosition;
            mAnchorPageBreakPosition = mLowerPageBreakPosition;
            mLowerPageBreakPosition = calculateNextPageBreakPosition(mAnchorPageBreakPosition);
        } else if (mAnchorPageBreakPosition > 0 && upperPageBreakTop >= getPaddingTop()) {
            // The anchor has moved below the viewport. We are now on the previous page. Shift
            // the page break positions and calculate a new upper one.
            mLowerPageBreakPosition = mAnchorPageBreakPosition;
            mAnchorPageBreakPosition = mUpperPageBreakPosition;
            mUpperPageBreakPosition = calculatePreviousPageBreakPosition(mAnchorPageBreakPosition);
        } else {
            mUpperPageBreakPosition = calculatePreviousPageBreakPosition(mAnchorPageBreakPosition);
            mLowerPageBreakPosition = calculateNextPageBreakPosition(mAnchorPageBreakPosition);
        }

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, String.format(":: #AFTER updatePageBreakPositions"
                            + " mAnchorPageBreakPosition:%s, mUpperPageBreakPosition:%s,"
                            + " mLowerPageBreakPosition:%s",
                            mAnchorPageBreakPosition, mUpperPageBreakPosition,
                    mLowerPageBreakPosition));
        }
    }

    /**
     * @return The page break position of the page before the anchor page break position. However,
     *     if it reaches the end of the laid out children or position 0, it will just return that.
     */
    @VisibleForTesting
    int calculatePreviousPageBreakPosition(int position) {
        if (position == -1) {
            return -1;
        }
        View referenceView = findViewByPosition(position);
        int referenceViewTop = getDecoratedTop(referenceView) - getParams(referenceView).topMargin;

        int previousPagePosition = position;
        while (previousPagePosition > 0) {
            previousPagePosition--;
            View child = findViewByPosition(previousPagePosition);
            if (child == null) {
                // View has not been laid out yet.
                return previousPagePosition + 1;
            }

            int childTop = getDecoratedTop(child) - getParams(child).topMargin;
            if (childTop < referenceViewTop - getHeight()) {
                return previousPagePosition + 1;
            }
        }
        // Beginning of the list.
        return 0;
    }

    /**
     * @return The page break position of the next page after the anchor page break position.
     *     However, if it reaches the end of the laid out children or end of the list, it will just
     *     return that.
     */
    @VisibleForTesting
    int calculateNextPageBreakPosition(int position) {
        if (position == -1) {
            return -1;
        }

        View referenceView = findViewByPosition(position);
        if (referenceView == null) {
            return position;
        }
        int referenceViewTop = getDecoratedTop(referenceView) - getParams(referenceView).topMargin;

        int nextPagePosition = position;

        // Search for the first child item after the referenceView that didn't fully fit on to the
        // screen. The next page should start from the item before this child, so that users have
        // a visual anchoring point of the page change.
        while (nextPagePosition < getItemCount() - 1) {
            nextPagePosition++;
            View child = findViewByPosition(nextPagePosition);
            if (child == null) {
                // The next view has not been laid out yet.
                return nextPagePosition - 1;
            }

            int childTop = getDecoratedTop(child) - getParams(child).topMargin;
            if (childTop > referenceViewTop + getHeight()) {
                // If choosing the previous child causes the view to snap back to the referenceView
                // position, then skip that and go directly to the child. This avoids the case
                // where a tall card in the layout causes the view to constantly snap back to
                // the top when scrolled.
                return nextPagePosition - 1 == position ? nextPagePosition : nextPagePosition - 1;
            }
        }
        // End of the list.
        return nextPagePosition;
    }

    /**
     * In this style, the focus will scroll down to the middle of the screen and lock there so that
     * moving in either direction will move the entire list by 1.
     */
    private boolean onRequestChildFocusMarioStyle(RecyclerView parent, View child) {
        int focusedPosition = getPosition(child);
        if (focusedPosition == mLastChildPositionToRequestFocus) {
            return true;
        }
        mLastChildPositionToRequestFocus = focusedPosition;

        int availableHeight = getAvailableHeight();
        int focusedChildTop = getDecoratedTop(child);
        int focusedChildBottom = getDecoratedBottom(child);

        int childIndex = parent.indexOfChild(child);
        // Iterate through children starting at the focused child to find the child above it to
        // smooth scroll to such that the focused child will be as close to the middle of the screen
        // as possible.
        for (int i = childIndex; i >= 0; i--) {
            View childAtI = getChildAt(i);
            if (childAtI == null) {
                Log.e(TAG, "Child is null at index " + i);
                continue;
            }
            // We haven't found a view that is more than half of the recycler view height above it
            // but we've reached the top so we can't go any further.
            if (i == 0) {
                parent.smoothScrollToPosition(getPosition(childAtI));
                break;
            }

            // Because we want to scroll to the first view that is less than half of the screen
            // away from the focused view, we "look ahead" one view. When the look ahead view
            // is more than availableHeight / 2 away, the current child at i is the one we want to
            // scroll to. However, sometimes, that view can be null (ie, if the view is in
            // transition). In that case, just skip that view.

            View childBefore = getChildAt(i - 1);
            if (childBefore == null) {
                continue;
            }
            int distanceToChildBeforeFromTop = focusedChildTop - getDecoratedTop(childBefore);
            int distanceToChildBeforeFromBottom = focusedChildBottom - getDecoratedTop(childBefore);

            if (distanceToChildBeforeFromTop > availableHeight / 2
                    || distanceToChildBeforeFromBottom > availableHeight) {
                parent.smoothScrollToPosition(getPosition(childAtI));
                break;
            }
        }
        return true;
    }

    /**
     * We don't actually know the size of every single view, only what is currently laid out. This
     * makes it difficult to do accurate scrollbar calculations. However, lists in the car often
     * consist of views with identical heights. Because of that, we can use a single sample view to
     * do our calculations for. The main exceptions are in the first items of a list (hero card,
     * last call card, etc) so if the first view is at position 0, we pick the next one.
     *
     * @return The decorated measured height of the sample view plus its margins.
     */
    private int getSampleViewHeight() {
        if (mSampleViewHeight != -1) {
            return mSampleViewHeight;
        }
        int sampleViewIndex = getFirstFullyVisibleChildIndex();
        View sampleView = getChildAt(sampleViewIndex);
        if (getPosition(sampleView) == 0 && sampleViewIndex < getChildCount() - 1) {
            sampleView = getChildAt(++sampleViewIndex);
        }
        RecyclerView.LayoutParams params = getParams(sampleView);
        int height = getDecoratedMeasuredHeight(sampleView) + params.topMargin
                + params.bottomMargin;
        if (height == 0) {
            // This can happen if the view isn't measured yet.
            Log.w(
                    TAG,
                    "The sample view has a height of 0. Returning a dummy value for now "
                            + "that won't be cached.");
            height = mContext.getResources().getDimensionPixelSize(R.dimen.car_sample_row_height);
        } else {
            mSampleViewHeight = height;
        }
        return height;
    }

    /** @return The height of the RecyclerView excluding padding. */
    private int getAvailableHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    /**
     * @return {@link RecyclerView.LayoutParams} for the given view or null if it isn't a child of
     *     {@link RecyclerView}.
     */
    private static RecyclerView.LayoutParams getParams(View view) {
        return (RecyclerView.LayoutParams) view.getLayoutParams();
    }

    /**
     * Custom {@link LinearSmoothScroller} that has: a) Custom control over the speed of scrolls. b)
     * Scrolling snaps to start. All of our scrolling logic depends on that. c) Keeps track of some
     * state of the current scroll so that can aid in things like the scrollbar calculations.
     */
    private final class CarSmoothScroller extends LinearSmoothScroller {
        /** This value (150) was hand tuned by UX for what felt right. * */
        private static final float MILLISECONDS_PER_INCH = 150f;
        /** This value (0.45) was hand tuned by UX for what felt right. * */
        private static final float DECELERATION_TIME_DIVISOR = 0.45f;

        /** This value (1.8) was hand tuned by UX for what felt right. * */
        private final Interpolator mInterpolator = new DecelerateInterpolator(1.8f);

        private final int mTargetPosition;

        CarSmoothScroller(Context context, int targetPosition) {
            super(context);
            mTargetPosition = targetPosition;
        }

        @Override
        public PointF computeScrollVectorForPosition(int i) {
            if (getChildCount() == 0) {
                return null;
            }
            final int firstChildPos = getPosition(getChildAt(getFirstFullyVisibleChildIndex()));
            final int direction = (mTargetPosition < firstChildPos) ? -1 : 1;
            return new PointF(0, direction);
        }

        @Override
        protected int getVerticalSnapPreference() {
            // This is key for most of the scrolling logic that guarantees that scrolling
            // will settle with a view aligned to the top.
            return LinearSmoothScroller.SNAP_TO_START;
        }

        @Override
        protected void onTargetFound(View targetView, RecyclerView.State state, Action action) {
            int dy = calculateDyToMakeVisible(targetView, SNAP_TO_START);
            if (dy == 0) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Scroll distance is 0");
                }
                return;
            }

            final int time = calculateTimeForDeceleration(dy);
            if (time > 0) {
                action.update(0, -dy, time, mInterpolator);
            }
        }

        @Override
        protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
            return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
        }

        @Override
        protected int calculateTimeForDeceleration(int dx) {
            return (int) Math.ceil(calculateTimeForScrolling(dx) / DECELERATION_TIME_DIVISOR);
        }

        @Override
        public int getTargetPosition() {
            return mTargetPosition;
        }
    }

    /**
     * Animation that translates a view by the specified amount. Used for card flying off the screen
     * effect.
     */
    private static class TranslateAnimation extends Animation {
        public float verticalOffset;

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            t.getMatrix().setTranslate(0, verticalOffset);
        }
    }
}
