/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v7.widget;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;


import static android.support.v7.widget.LayoutState.LAYOUT_START;
import static android.support.v7.widget.LayoutState.LAYOUT_END;
import static android.support.v7.widget.LayoutState.ITEM_DIRECTION_HEAD;
import static android.support.v7.widget.LayoutState.ITEM_DIRECTION_TAIL;
/**
 * A LayoutManager that lays out children in a staggered grid formation.
 * It supports horizontal & vertical layout as well as an ability to layout children in reverse.
 * <p>
 * Staggered grids are likely to have gaps at the edges of the layout. To avoid these gaps,
 * StaggeredGridLayoutManager can offset spans independently or move items between spans. You can
 * control this behavior via {@link #setGapStrategy(int)}.
 */
public class StaggeredGridLayoutManager extends RecyclerView.LayoutManager {

    public static final String TAG = "StaggeredGridLayoutManager";

    private static final boolean DEBUG = false;

    public static final int HORIZONTAL = OrientationHelper.HORIZONTAL;

    public static final int VERTICAL = OrientationHelper.VERTICAL;

    /**
     * Does not do anything to hide gaps
     */
    public static final int GAP_HANDLING_NONE = 0;

    /**
     * Scroll the shorter span slower to avoid gaps in the UI.
     * <p>
     * For example, if LayoutManager ends up with the following layout:
     * <code>
     * BXC
     * DEF
     * </code>
     * Where B has two spans height, if user scrolls down it will keep the positions of 2nd and 3rd
     * columns,
     * which will result in:
     * <code>
     * BXC
     * BEF
     * </code>
     * instead of
     * <code>
     * B
     * BEF
     * </code>
     */
    public static final int GAP_HANDLING_LAZY = 1;

    /**
     * On scroll, LayoutManager checks for a view that is assigned to wrong span.
     * When such a situation is detected, LayoutManager will wait until scroll is complete and then
     * move children to their correct spans.
     * <p>
     * For example, if LayoutManager ends up with the following layout due to adapter changes:
     * <code>
     * AAA
     * _BC
     * DDD
     * </code>
     * It will animate to the following state:
     * <code>
     * AAA
     * BC_
     * DDD
     * </code>
     */
    public static final int GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS = 2;

    private static final int INVALID_OFFSET = Integer.MIN_VALUE;

    /**
     * Number of spans
     */
    private int mSpanCount = -1;

    private Span[] mSpans;

    /**
     * Primary orientation is the layout's orientation, secondary orientation is the orientation
     * for spans. Having both makes code much cleaner for calculations.
     */
    OrientationHelper mPrimaryOrientation;
    OrientationHelper mSecondaryOrientation;

    private int mOrientation;

    /**
     * The width or height per span, depending on the orientation.
     */
    private int mSizePerSpan;

    private LayoutState mLayoutState;

    private boolean mReverseLayout = false;

    /**
     * Aggregated reverse layout value that takes RTL into account.
     */
    private boolean mShouldReverseLayout = false;

    /**
     * Temporary variable used during fill method to check which spans needs to be filled.
     */
    private BitSet mRemainingSpans;

    /**
     * When LayoutManager needs to scroll to a position, it sets this variable and requests a
     * layout which will check this variable and re-layout accordingly.
     */
    int mPendingScrollPosition = RecyclerView.NO_POSITION;

    /**
     * Used to keep the offset value when {@link #scrollToPositionWithOffset(int, int)} is
     * called.
     */
    int mPendingScrollPositionOffset = INVALID_OFFSET;

    /**
     * Keeps the mapping between the adapter positions and spans. This is necessary to provide
     * a consistent experience when user scrolls the list.
     */
    LazySpanLookup mLazySpanLookup = new LazySpanLookup();

    /**
     * how we handle gaps in UI.
     */
    private int mGapStrategy = GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS;

    /**
     * Saved state needs this information to properly layout on restore.
     */
    private boolean mLastLayoutFromEnd;

    /**
     * SavedState is not handled until a layout happens. This is where we keep it until next
     * layout.
     */
    private SavedState mPendingSavedState;

    /**
     * If LayoutManager detects an unwanted gap in the layout, it sets this flag which will trigger
     * a runnable after scrolling ends and will re-check. If invalid view state is still present,
     * it will request a layout to fix it.
     */
    private boolean mHasGaps;

    /**
     * Creates a StaggeredGridLayoutManager with given parameters.
     *
     * @param spanCount   If orientation is vertical, spanCount is number of columns. If
     *                    orientation is horizontal, spanCount is number of rows.
     * @param orientation {@link #VERTICAL} or {@link #HORIZONTAL}
     */
    public StaggeredGridLayoutManager(int spanCount, int orientation) {
        mOrientation = orientation;
        setSpanCount(spanCount);
    }

    @Override
    public void onScrollStateChanged(int state) {
        if (state == RecyclerView.SCROLL_STATE_IDLE && mHasGaps) {
            // re-check for gaps
            View gapView = hasGapsToFix(0, getChildCount());
            if (gapView == null) {
                mHasGaps = false; // yay, gap disappeared :)
                // We should invalidate positions after the last visible child. No reason to
                // re-layout.
                final int lastVisiblePosition = mShouldReverseLayout ? getFirstChildPosition()
                        : getLastChildPosition();
                mLazySpanLookup.invalidateAfter(lastVisiblePosition + 1);
            } else {
                mLazySpanLookup.invalidateAfter(getPosition(gapView));
                requestSimpleAnimationsInNextLayout();
                requestLayout(); // Trigger a re-layout which will fix the layout assignments.
            }
        }
    }

    /**
     * Sets the number of spans for the layout. This will invalidate all of the span assignments
     * for Views.
     * <p>
     * Calling this method will automatically result in a new layout request unless the spanCount
     * parameter is equal to current span count.
     *
     * @param spanCount Number of spans to layout
     */
    public void setSpanCount(int spanCount) {
        assertNotInLayoutOrScroll(null);
        if (mPendingSavedState != null && mPendingSavedState.mSpanCount != spanCount) {
            // invalidate span info in saved state
            mPendingSavedState.invalidateSpanInfo();
            mPendingSavedState.mSpanCount = spanCount;
            mPendingSavedState.mAnchorPosition = mPendingSavedState.mVisibleAnchorPosition;
        }
        if (spanCount != mSpanCount) {
            invalidateSpanAssignments();
            mSpanCount = spanCount;
            mRemainingSpans = new BitSet(mSpanCount);
            mSpans = new Span[mSpanCount];
            for (int i = 0; i < mSpanCount; i++) {
                mSpans[i] = new Span(i);
            }
            requestLayout();
        }
    }

    /**
     * Sets the orientation of the layout. StaggeredGridLayoutManager will do its best to keep
     * scroll position.
     *
     * @param orientation {@link OrientationHelper#HORIZONTAL} or {@link OrientationHelper#VERTICAL}
     */
    public void setOrientation(int orientation) {
        if (orientation != HORIZONTAL && orientation != VERTICAL) {
            throw new IllegalArgumentException("invalid orientation.");
        }
        assertNotInLayoutOrScroll(null);
        if (mPendingSavedState != null && mPendingSavedState.mOrientation != orientation) {
            // override pending state
            mPendingSavedState.mOrientation = orientation;
        }
        if (orientation == mOrientation) {
            return;
        }
        mOrientation = orientation;
        if (mPrimaryOrientation != null && mSecondaryOrientation != null) {
            // swap
            OrientationHelper tmp = mPrimaryOrientation;
            mPrimaryOrientation = mSecondaryOrientation;
            mSecondaryOrientation = tmp;
        }
        requestLayout();
    }

    /**
     * Sets whether LayoutManager should start laying out items from the end of the UI. The order
     * items are traversed is not affected by this call.
     * <p>
     * This behaves similar to the layout change for RTL views. When set to true, first item is
     * laid out at the end of the ViewGroup, second item is laid out before it etc.
     * <p>
     * For horizontal layouts, it depends on the layout direction.
     * When set to true, If {@link RecyclerView} is LTR, than it will layout from RTL, if
     * {@link RecyclerView}} is RTL, it will layout from LTR.
     *
     * @param reverseLayout Whether layout should be in reverse or not
     */
    public void setReverseLayout(boolean reverseLayout) {
        assertNotInLayoutOrScroll(null);
        if (mPendingSavedState != null && mPendingSavedState.mReverseLayout != reverseLayout) {
            mPendingSavedState.mReverseLayout = reverseLayout;
        }
        mReverseLayout = reverseLayout;
        requestLayout();
    }

    /**
     * Returns the current gap handling strategy for StaggeredGridLayoutManager.
     * <p>
     * Staggered grid may have gaps in the layout as items may have different sizes. To avoid gaps,
     * StaggeredGridLayoutManager provides 3 options. Check {@link #GAP_HANDLING_NONE},
     * {@link #GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS}, {@link #GAP_HANDLING_LAZY} for details.
     * <p>
     * By default, StaggeredGridLayoutManager uses {@link #GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS}.
     *
     * @return Current gap handling strategy.
     * @see #setGapStrategy(int)
     * @see #GAP_HANDLING_NONE
     * @see #GAP_HANDLING_LAZY
     * @see #GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
     */
    public int getGapStrategy() {
        return mGapStrategy;
    }

    /**
     * Sets the gap handling strategy for StaggeredGridLayoutManager. If the gapStrategy parameter
     * is different than the current strategy, calling this method will trigger a layout request.
     *
     * @param gapStrategy The new gap handling strategy. Should be {@link #GAP_HANDLING_LAZY}
     *                    , {@link #GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS} or
     *                    {@link #GAP_HANDLING_NONE}
     * @see #getGapStrategy()
     */
    public void setGapStrategy(int gapStrategy) {
        assertNotInLayoutOrScroll(null);
        if (mPendingSavedState != null && mPendingSavedState.mGapStrategy != gapStrategy) {
            mPendingSavedState.mGapStrategy = gapStrategy;
        }
        if (gapStrategy == mGapStrategy) {
            return;
        }
        if (gapStrategy != GAP_HANDLING_LAZY && gapStrategy != GAP_HANDLING_NONE &&
                gapStrategy != GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS) {
            throw new IllegalArgumentException("invalid gap strategy. Must be GAP_HANDLING_NONE "
                    + ", GAP_HANDLING_LAZY or GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS");
        }
        mGapStrategy = gapStrategy;
        requestLayout();
    }

    @Override
    public void assertNotInLayoutOrScroll(String message) {
        if (mPendingSavedState == null) {
            super.assertNotInLayoutOrScroll(message);
        }
    }

    /**
     * Returns the number of spans laid out by StaggeredGridLayoutManager.
     *
     * @return Number of spans in the layout
     */
    public int getSpanCount() {
        return mSpanCount;
    }

    /**
     * For consistency, StaggeredGridLayoutManager keeps a mapping between spans and items.
     * <p>
     * If you need to cancel current assignments, you can call this method which will clear all
     * assignments and request a new layout.
     */
    public void invalidateSpanAssignments() {
        mLazySpanLookup.clear();
        requestLayout();
    }

    private void ensureOrientationHelper() {
        if (mPrimaryOrientation == null) {
            mPrimaryOrientation = OrientationHelper.createOrientationHelper(this, mOrientation);
            mSecondaryOrientation = OrientationHelper
                    .createOrientationHelper(this, 1 - mOrientation);
            mLayoutState = new LayoutState();
        }
    }

    /**
     * Calculates the views' layout order. (e.g. from end to start or start to end)
     * RTL layout support is applied automatically. So if layout is RTL and
     * {@link #getReverseLayout()} is {@code true}, elements will be laid out starting from left.
     */
    private void resolveShouldLayoutReverse() {
        // A == B is the same result, but we rather keep it readable
        if (mOrientation == VERTICAL || !isLayoutRTL()) {
            mShouldReverseLayout = mReverseLayout;
        } else {
            mShouldReverseLayout = !mReverseLayout;
        }
    }

    private boolean isLayoutRTL() {
        return getLayoutDirection() == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    /**
     * Returns whether views are laid out in reverse order or not.
     * <p>
     * Not that this value is not affected by RecyclerView's layout direction.
     *
     * @return True if layout is reversed, false otherwise
     * @see #setReverseLayout(boolean)
     */
    public boolean getReverseLayout() {
        return mReverseLayout;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        ensureOrientationHelper();
        // Update adapter size.
        mLazySpanLookup.mAdapterSize = state.getItemCount();
        int anchorItemPosition;
        int anchorOffset;
        // This value may change if we are jumping to a position.
        boolean layoutFromEnd;

        // If set to true, spans will clear their offsets and they'll be laid out from start
        // depending on the layout direction. Invalidating span offsets is necessary to be able
        // to jump to a position.
        boolean invalidateSpanOffsets = false;

        if (mPendingSavedState != null) {
            if (DEBUG) {
                Log.d(TAG, "found saved state: " + mPendingSavedState);
            }
            setOrientation(mPendingSavedState.mOrientation);
            setSpanCount(mPendingSavedState.mSpanCount);
            setGapStrategy(mPendingSavedState.mGapStrategy);
            setReverseLayout(mPendingSavedState.mReverseLayout);
            resolveShouldLayoutReverse();

            if (mPendingSavedState.mAnchorPosition != RecyclerView.NO_POSITION) {
                mPendingScrollPosition = mPendingSavedState.mAnchorPosition;
                layoutFromEnd = mPendingSavedState.mAnchorLayoutFromEnd;
            } else {
                layoutFromEnd = mShouldReverseLayout;
            }
            if (mPendingSavedState.mHasSpanOffsets) {
                for (int i = 0; i < mSpanCount; i++) {
                    mSpans[i].clear();
                    mSpans[i].setLine(mPendingSavedState.mSpanOffsets[i]);
                }
            }
            if (mPendingSavedState.mSpanLookupSize > 1) {
                mLazySpanLookup.mData = mPendingSavedState.mSpanLookup;
            }

        } else {
            resolveShouldLayoutReverse();
            layoutFromEnd = mShouldReverseLayout; // get updated value.
        }

        // Validate scroll position if exists.
        if (!state.isPreLayout() && mPendingScrollPosition != RecyclerView.NO_POSITION) {
            // Validate it.
            if (mPendingScrollPosition < 0 || mPendingScrollPosition >= state.getItemCount()) {
                mPendingScrollPosition = RecyclerView.NO_POSITION;
                mPendingScrollPositionOffset = INVALID_OFFSET;
            }
        }

        if (!state.isPreLayout() && mPendingScrollPosition != RecyclerView.NO_POSITION) {
            if (mPendingSavedState == null
                    || mPendingSavedState.mAnchorPosition == RecyclerView.NO_POSITION
                    || !mPendingSavedState.mHasSpanOffsets) {
                // If item is visible, make it fully visible.
                final View child = findViewByPosition(mPendingScrollPosition);
                if (child != null) {
                    if (mPendingScrollPositionOffset != INVALID_OFFSET) {
                        // Use regular anchor position.
                        anchorItemPosition = mShouldReverseLayout ? getLastChildPosition()
                                : getFirstChildPosition();
                        if (layoutFromEnd) {
                            final int target = mPrimaryOrientation.getEndAfterPadding() -
                                    mPendingScrollPositionOffset;
                            anchorOffset = target - mPrimaryOrientation.getDecoratedEnd(child);
                        } else {
                            final int target = mPrimaryOrientation.getStartAfterPadding() +
                                    mPendingScrollPositionOffset;
                            anchorOffset = target - mPrimaryOrientation.getDecoratedStart(child);
                        }
                    } else {
                        final int startGap = mPrimaryOrientation.getDecoratedStart(child)
                                - mPrimaryOrientation.getStartAfterPadding();
                        final int endGap = mPrimaryOrientation.getEndAfterPadding() -
                                mPrimaryOrientation.getDecoratedEnd(child);
                        final int childSize = mPrimaryOrientation.getDecoratedMeasurement(child);
                        // Use regular anchor item, just offset the layout.
                        anchorItemPosition = mShouldReverseLayout ? getLastChildPosition()
                                : getFirstChildPosition();
                        if (childSize > mPrimaryOrientation.getTotalSpace()) {
                            // Item does not fit. Fix depending on layout direction.
                            anchorOffset = layoutFromEnd ? mPrimaryOrientation.getEndAfterPadding()
                                    : mPrimaryOrientation.getStartAfterPadding();
                        } else if (startGap < 0) {
                            anchorOffset = -startGap;
                        } else if (endGap < 0) {
                            anchorOffset = endGap;
                        } else {
                            // Nothing to do, just layout normal.
                            anchorItemPosition = mShouldReverseLayout ? getLastChildPosition()
                                    : getFirstChildPosition();
                            anchorOffset = INVALID_OFFSET;
                        }
                    }
                } else {
                    // Child is not visible. Set anchor coordinate depending on in which direction
                    // child will be visible.
                    anchorItemPosition = mPendingScrollPosition;
                    if (mPendingScrollPositionOffset == INVALID_OFFSET) {
                        final int position = calculateScrollDirectionForPosition(
                                anchorItemPosition);
                        if (position == LAYOUT_START) {
                            anchorOffset = mPrimaryOrientation.getStartAfterPadding();
                            layoutFromEnd = false;
                        } else {
                            anchorOffset = mPrimaryOrientation.getEndAfterPadding();
                            layoutFromEnd = true;
                        }
                    } else {
                        if (layoutFromEnd) {
                            anchorOffset = mPrimaryOrientation.getEndAfterPadding()
                                    - mPendingScrollPositionOffset;
                        } else {
                            anchorOffset = mPrimaryOrientation.getStartAfterPadding()
                                    + mPendingScrollPositionOffset;
                        }
                    }
                    invalidateSpanOffsets = true;
                }
            } else {
                anchorOffset = INVALID_OFFSET;
                anchorItemPosition = mPendingScrollPosition;
            }

        } else {
            // We don't recycle views out of adapter order. This way, we can rely on the first or
            // last child as the anchor position.
            anchorItemPosition = mShouldReverseLayout
                    ? findLastReferenceChildPosition(state.getItemCount())
                    : findFirstReferenceChildPosition(state.getItemCount());
            anchorOffset = INVALID_OFFSET;
        }
        if (getChildCount() > 0 && (mPendingSavedState == null ||
                !mPendingSavedState.mHasSpanOffsets)) {
            if (invalidateSpanOffsets || mHasGaps) {
                for (int i = 0; i < mSpanCount; i++) {
                    // Scroll to position is set, clear.
                    mSpans[i].clear();
                    if (anchorOffset != INVALID_OFFSET) {
                        mSpans[i].setLine(anchorOffset);
                    }
                }
            } else {
                for (int i = 0; i < mSpanCount; i++) {
                    mSpans[i].cacheReferenceLineAndClear(mShouldReverseLayout, anchorOffset);
                }
                if (DEBUG) {
                    for (int i = 0; i < mSpanCount; i++) {
                        Log.d(TAG, "cached start-end lines for " + i + ":" +
                                mSpans[i].mCachedStart + ":" + mSpans[i].mCachedEnd);
                    }
                }
            }
        }
        mSizePerSpan = mSecondaryOrientation.getTotalSpace() / mSpanCount;
        detachAndScrapAttachedViews(recycler);
        // Layout start.
        updateLayoutStateToFillStart(anchorItemPosition, state);
        if (!layoutFromEnd) {
            mLayoutState.mCurrentPosition += mLayoutState.mItemDirection;
        }
        fill(recycler, mLayoutState, state);

        // Layout end.
        updateLayoutStateToFillEnd(anchorItemPosition, state);
        if (layoutFromEnd) {
            mLayoutState.mCurrentPosition += mLayoutState.mItemDirection;
        }
        fill(recycler, mLayoutState, state);

        if (getChildCount() > 0) {
            if (mShouldReverseLayout) {
                fixEndGap(recycler, state, true);
                fixStartGap(recycler, state, false);
            } else {
                fixStartGap(recycler, state, true);
                fixEndGap(recycler, state, false);
            }
        }

        if (!state.isPreLayout()) {
            mPendingScrollPosition = RecyclerView.NO_POSITION;
            mPendingScrollPositionOffset = INVALID_OFFSET;
        }
        mLastLayoutFromEnd = layoutFromEnd;
        mPendingSavedState = null; // we don't need this anymore
    }

    /**
     * Checks if a child is assigned to the non-optimal span.
     *
     * @param startChildIndex Starts checking after this child, inclusive
     * @param endChildIndex   Starts checking until this child, exclusive
     * @return The first View that is assigned to the wrong span.
     */
    View hasGapsToFix(int startChildIndex, int endChildIndex) {
        // quick reject
        if (startChildIndex >= endChildIndex) {
            return null;
        }
        final int firstChildIndex, childLimit;
        final int nextSpanDiff = mOrientation == VERTICAL && isLayoutRTL() ? 1 : -1;

        if (mShouldReverseLayout) {
            firstChildIndex = endChildIndex - 1;
            childLimit = startChildIndex - 1;
        } else {
            firstChildIndex = startChildIndex;
            childLimit = endChildIndex;
        }
        final int nextChildDiff = firstChildIndex < childLimit ? 1 : -1;
        for (int i = firstChildIndex; i != childLimit; i += nextChildDiff) {
            View child = getChildAt(i);
            final int start = mPrimaryOrientation.getDecoratedStart(child);
            final int end = mPrimaryOrientation.getDecoratedEnd(child);
            LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
            if (layoutParams.mFullSpan) {
                continue; // quick reject
            }
            int nextSpanIndex = layoutParams.getSpanIndex() + nextSpanDiff;
            while (nextSpanIndex >= 0 && nextSpanIndex < mSpanCount) {
                Span nextSpan = mSpans[nextSpanIndex];
                if (nextSpan.isEmpty(start, end)) {
                    return child;
                }
                nextSpanIndex += nextSpanDiff;
            }
        }
        // everything looks good
        return null;
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return mPendingSavedState == null;
    }

    /**
     * Returns the adapter position of the first visible view for each span.
     * <p>
     * Note that, this value is not affected by layout orientation or item order traversal.
     * ({@link #setReverseLayout(boolean)}). Views are sorted by their positions in the adapter,
     * not in the layout.
     * <p>
     * If RecyclerView has item decorators, they will be considered in calculations as well.
     * <p>
     * StaggeredGridLayoutManager may pre-cache some views that are not necessarily visible. Those
     * views are ignored in this method.
     *
     * @return The adapter position of the first visible item in each span. If a span does not have
     * any items, {@link RecyclerView#NO_POSITION} is returned for that span.
     *
     * @param into An array to put the results into. If you don't provide any, LayoutManager will
     *             create a new one.
     * @see #findFirstCompletelyVisibleItemPositions(int[])
     * @see #findLastVisibleItemPositions(int[])
     */
    public int[] findFirstVisibleItemPositions(int[] into) {
        if (into == null) {
            into = new int[mSpanCount];
        } else if (into.length < mSpanCount) {
            throw new IllegalArgumentException("Provided int[]'s size must be more than or equal"
                    + " to span count. Expected:" + mSpanCount + ", array size:" + into.length);
        }
        for (int i = 0; i < mSpanCount; i ++) {
            into[i] = mSpans[i].findFirstVisibleItemPosition();
        }
        return into;
    }

    /**
     * Returns the adapter position of the first completely visible view for each span.
     * <p>
     * Note that, this value is not affected by layout orientation or item order traversal.
     * ({@link #setReverseLayout(boolean)}). Views are sorted by their positions in the adapter,
     * not in the layout.
     * <p>
     * If RecyclerView has item decorators, they will be considered in calculations as well.
     * <p>
     * StaggeredGridLayoutManager may pre-cache some views that are not necessarily visible. Those
     * views are ignored in this method.
     *
     * @return The adapter position of the first fully visible item in each span. If a span does
     * not have any items, {@link RecyclerView#NO_POSITION} is returned for that span.
     * @param into An array to put the results into. If you don't provide any, LayoutManager will
     *             create a new one.
     * @see #findFirstVisibleItemPositions(int[])
     * @see #findLastCompletelyVisibleItemPositions(int[])
     */
    public int[] findFirstCompletelyVisibleItemPositions(int[] into) {
        if (into == null) {
            into = new int[mSpanCount];
        } else if (into.length < mSpanCount) {
            throw new IllegalArgumentException("Provided int[]'s size must be more than or equal"
                    + " to span count. Expected:" + mSpanCount + ", array size:" + into.length);
        }
        for (int i = 0; i < mSpanCount; i ++) {
            into[i] = mSpans[i].findFirstCompletelyVisibleItemPosition();
        }
        return into;
    }

    /**
     * Returns the adapter position of the last visible view for each span.
     * <p>
     * Note that, this value is not affected by layout orientation or item order traversal.
     * ({@link #setReverseLayout(boolean)}). Views are sorted by their positions in the adapter,
     * not in the layout.
     * <p>
     * If RecyclerView has item decorators, they will be considered in calculations as well.
     * <p>
     * StaggeredGridLayoutManager may pre-cache some views that are not necessarily visible. Those
     * views are ignored in this method.
     *
     * @return The adapter position of the last visible item in each span. If a span does not have
     * any items, {@link RecyclerView#NO_POSITION} is returned for that span.
     *
     * @param into An array to put the results into. If you don't provide any, LayoutManager will
     *             create a new one.
     * @see #findLastCompletelyVisibleItemPositions(int[])
     * @see #findFirstVisibleItemPositions(int[])
     */
    public int[] findLastVisibleItemPositions(int[] into) {
        if (into == null) {
            into = new int[mSpanCount];
        } else if (into.length < mSpanCount) {
            throw new IllegalArgumentException("Provided int[]'s size must be more than or equal"
                    + " to span count. Expected:" + mSpanCount + ", array size:" + into.length);
        }
        for (int i = 0; i < mSpanCount; i ++) {
            into[i] = mSpans[i].findLastVisibleItemPosition();
        }
        return into;
    }

    /**
     * Returns the adapter position of the last completely visible view for each span.
     * <p>
     * Note that, this value is not affected by layout orientation or item order traversal.
     * ({@link #setReverseLayout(boolean)}). Views are sorted by their positions in the adapter,
     * not in the layout.
     * <p>
     * If RecyclerView has item decorators, they will be considered in calculations as well.
     * <p>
     * StaggeredGridLayoutManager may pre-cache some views that are not necessarily visible. Those
     * views are ignored in this method.
     *
     * @return The adapter position of the last fully visible item in each span. If a span does not
     * have any items, {@link RecyclerView#NO_POSITION} is returned for that span.
     *
     * @param into An array to put the results into. If you don't provide any, LayoutManager will
     *             create a new one.
     * @see #findFirstCompletelyVisibleItemPositions(int[])
     * @see #findLastVisibleItemPositions(int[])
     */
    public int[] findLastCompletelyVisibleItemPositions(int[] into) {
        if (into == null) {
            into = new int[mSpanCount];
        } else if (into.length < mSpanCount) {
            throw new IllegalArgumentException("Provided int[]'s size must be more than or equal"
                    + " to span count. Expected:" + mSpanCount + ", array size:" + into.length);
        }
        for (int i = 0; i < mSpanCount; i ++) {
            into[i] = mSpans[i].findLastCompletelyVisibleItemPosition();
        }
        return into;
    }

    private void measureChildWithDecorationsAndMargin(View child, int widthSpec,
            int heightSpec) {
        final Rect insets = mRecyclerView.getItemDecorInsetsForChild(child);
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        widthSpec = updateSpecWithExtra(widthSpec, lp.leftMargin + insets.left,
                lp.rightMargin + insets.right);
        heightSpec = updateSpecWithExtra(heightSpec, lp.topMargin + insets.top,
                lp.bottomMargin + insets.bottom);
        child.measure(widthSpec, heightSpec);
    }

    private int updateSpecWithExtra(int spec, int startInset, int endInset) {
        if (startInset == 0 && endInset == 0) {
            return spec;
        }
        final int mode = View.MeasureSpec.getMode(spec);
        if (mode == View.MeasureSpec.AT_MOST || mode == View.MeasureSpec.EXACTLY) {
            return View.MeasureSpec.makeMeasureSpec(
                    View.MeasureSpec.getSize(spec) - startInset - endInset, mode);
        }
        return spec;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            mPendingSavedState = (SavedState) state;
            requestLayout();
        } else if (DEBUG) {
            Log.d(TAG, "invalid saved state class");
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        if (mPendingSavedState != null) {
            return new SavedState(mPendingSavedState);
        }
        SavedState state = new SavedState();
        state.mOrientation = mOrientation;
        state.mReverseLayout = mReverseLayout;
        state.mSpanCount = mSpanCount;
        state.mAnchorLayoutFromEnd = mLastLayoutFromEnd;
        state.mGapStrategy = mGapStrategy;

        if (mLazySpanLookup != null && mLazySpanLookup.mData != null) {
            state.mSpanLookup = mLazySpanLookup.mData;
            state.mSpanLookupSize = state.mSpanLookup.length;
        } else {
            state.mSpanLookupSize = 0;
        }

        if (getChildCount() > 0) {
            state.mAnchorPosition = mLastLayoutFromEnd ? getLastChildPosition()
                    : getFirstChildPosition();
            state.mVisibleAnchorPosition = findFirstVisibleItemPositionInt();
            state.mHasSpanOffsets = true;
            state.mSpanOffsets = new int[mSpanCount];
            for (int i = 0; i < mSpanCount; i++) {
                state.mSpanOffsets[i] = mLastLayoutFromEnd ? mSpans[i].getEndLine(Span.INVALID_LINE)
                        : mSpans[i].getStartLine(Span.INVALID_LINE);
            }
        } else {
            state.mAnchorPosition = RecyclerView.NO_POSITION;
            state.mVisibleAnchorPosition = RecyclerView.NO_POSITION;
            state.mHasSpanOffsets = false;
        }
        if (DEBUG) {
            Log.d(TAG, "saved state:\n" + state);
        }
        return state;
    }

    /**
     * Finds the first fully visible child to be used as an anchor child if span count changes when
     * state is restored.
     */
    int findFirstVisibleItemPositionInt() {
        final int start, end, diff;
        if (mLastLayoutFromEnd) {
            start = getChildCount() - 1;
            end = -1;
            diff = -1;
        } else {
            start = 0;
            end = getChildCount();
            diff = 1;
        }
        final int boundsStart = mPrimaryOrientation.getStartAfterPadding();
        final int boundsEnd = mPrimaryOrientation.getEndAfterPadding();
        for (int i = start; i != end; i += diff) {
            final View child = getChildAt(i);
            if (mPrimaryOrientation.getDecoratedStart(child) >= boundsStart
                    && mPrimaryOrientation.getDecoratedEnd(child) <= boundsEnd) {
                return getPosition(child);
            }
        }
        return RecyclerView.NO_POSITION;
    }

    private void fixEndGap(RecyclerView.Recycler recycler, RecyclerView.State state,
            boolean canOffsetChildren) {
        final int maxEndLine = getMaxEnd(mPrimaryOrientation.getEndAfterPadding());
        int gap = mPrimaryOrientation.getEndAfterPadding() - maxEndLine;
        int fixOffset;
        if (gap > 0) {
            fixOffset = -scrollBy(-gap, recycler, state);
        } else {
            return; // nothing to fix
        }
        gap -= fixOffset;
        if (canOffsetChildren && gap > 0) {
            mPrimaryOrientation.offsetChildren(gap);
        }
    }

    private void fixStartGap(RecyclerView.Recycler recycler, RecyclerView.State state,
            boolean canOffsetChildren) {
        final int minStartLine = getMinStart(mPrimaryOrientation.getStartAfterPadding());
        int gap = minStartLine - mPrimaryOrientation.getStartAfterPadding();
        int fixOffset;
        if (gap > 0) {
            fixOffset = scrollBy(gap, recycler, state);
        } else {
            return; // nothing to fix
        }
        gap -= fixOffset;
        if (canOffsetChildren && gap > 0) {
            mPrimaryOrientation.offsetChildren(-gap);
        }
    }

    private void updateLayoutStateToFillStart(int anchorPosition, RecyclerView.State state) {
        mLayoutState.mAvailable = 0;
        mLayoutState.mCurrentPosition = anchorPosition;
        if (isSmoothScrolling()) {
            final int targetPos = state.getTargetScrollPosition();
            if (mShouldReverseLayout == targetPos < anchorPosition) {
                mLayoutState.mExtra = 0;
            } else {
                mLayoutState.mExtra = mPrimaryOrientation.getTotalSpace();
            }
        } else {
            mLayoutState.mExtra = 0;
        }
        mLayoutState.mLayoutDirection = LAYOUT_START;
        mLayoutState.mItemDirection = mShouldReverseLayout ? ITEM_DIRECTION_TAIL
                : ITEM_DIRECTION_HEAD;
    }

    private void updateLayoutStateToFillEnd(int anchorPosition, RecyclerView.State state) {
        mLayoutState.mAvailable = 0;
        mLayoutState.mCurrentPosition = anchorPosition;
        if (isSmoothScrolling()) {
            final int targetPos = state.getTargetScrollPosition();
            if (mShouldReverseLayout == targetPos > anchorPosition) {
                mLayoutState.mExtra = 0;
            } else {
                mLayoutState.mExtra = mPrimaryOrientation.getTotalSpace();
            }
        } else {
            mLayoutState.mExtra = 0;
        }
        mLayoutState.mLayoutDirection = LAYOUT_END;
        mLayoutState.mItemDirection = mShouldReverseLayout ? ITEM_DIRECTION_HEAD
                : ITEM_DIRECTION_TAIL;
    }

    @Override
    public void offsetChildrenHorizontal(int dx) {
        super.offsetChildrenHorizontal(dx);
        for (int i = 0; i < mSpanCount; i++) {
            mSpans[i].onOffset(dx);
        }
    }

    @Override
    public void offsetChildrenVertical(int dy) {
        super.offsetChildrenVertical(dy);
        for (int i = 0; i < mSpanCount; i++) {
            mSpans[i].onOffset(dy);
        }
    }

    @Override
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        if (!considerSpanInvalidate(positionStart, itemCount)) {
            // If positions are not invalidated, move span offsets.
            mLazySpanLookup.offsetForRemoval(positionStart, itemCount);
        }
    }

    @Override
    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        if (!considerSpanInvalidate(positionStart, itemCount)) {
            // If positions are not invalidated, move span offsets.
            mLazySpanLookup.offsetForAddition(positionStart, itemCount);
        }
    }

    /**
     * Checks whether it should invalidate span assignments in response to an adapter change.
     */
    private boolean considerSpanInvalidate(int positionStart, int itemCount) {
        int minPosition = mShouldReverseLayout ? getLastChildPosition() : getFirstChildPosition();
        if (positionStart + itemCount <= minPosition) {
            return false;// nothing to update.
        }
        int maxPosition = mShouldReverseLayout ? getFirstChildPosition() : getLastChildPosition();
        mLazySpanLookup.invalidateAfter(positionStart);
        if (positionStart <= maxPosition) {
            requestLayout();
        }
        return true;
    }

    private int fill(RecyclerView.Recycler recycler, LayoutState layoutState,
            RecyclerView.State state) {
        mRemainingSpans.set(0, mSpanCount, true);
        // The target position we are trying to reach.
        final int targetLine;

        /*
        * The line until which we can recycle, as long as we add views.
        * Keep in mind, it is still the line in layout direction which means; to calculate the
        * actual recycle line, we should subtract/add the size in orientation.
        */
        final int recycleLine;
        // Line of the furthest row.
        if (layoutState.mLayoutDirection == LAYOUT_END) {
            // ignore padding for recycler
            recycleLine = mPrimaryOrientation.getEndAfterPadding() + mLayoutState.mAvailable;
            targetLine = recycleLine + mLayoutState.mExtra + mPrimaryOrientation.getEndPadding();
            final int defaultLine = mPrimaryOrientation.getStartAfterPadding();
            for (int i = 0; i < mSpanCount; i++) {
                final Span span = mSpans[i];
                final int line = span.getEndLine(defaultLine);
                if (line > targetLine) {
                    mRemainingSpans.set(i, false);
                }
            }
        } else { // LAYOUT_START
            // ignore padding for recycler
            recycleLine = mPrimaryOrientation.getStartAfterPadding() - mLayoutState.mAvailable;
            targetLine = recycleLine - mLayoutState.mExtra -
                    mPrimaryOrientation.getStartAfterPadding();
            for (int i = 0; i < mSpanCount; i++) {
                final Span span = mSpans[i];
                final int defaultLine = mPrimaryOrientation.getEndAfterPadding();
                final int line = span.getStartLine(defaultLine);
                if (line < targetLine) {
                    mRemainingSpans.set(i, false);
                }
            }
        }

        final int widthSpec, heightSpec;
        if (mOrientation == VERTICAL) {
            widthSpec = View.MeasureSpec.makeMeasureSpec(mSizePerSpan, View.MeasureSpec.EXACTLY);
            heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        } else {
            heightSpec = View.MeasureSpec.makeMeasureSpec(mSizePerSpan, View.MeasureSpec.EXACTLY);
            widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        }

        while (layoutState.hasMore(state) && !mRemainingSpans.isEmpty()) {
            View view = layoutState.next(recycler);
            LayoutParams lp = ((LayoutParams) view.getLayoutParams());
            if (layoutState.mLayoutDirection == LAYOUT_END) {
                addView(view);
            } else {
                addView(view, 0);
            }
            if (lp.mFullSpan) {
                final int fullSizeSpec = View.MeasureSpec.makeMeasureSpec(
                        mSecondaryOrientation.getTotalSpace(), View.MeasureSpec.EXACTLY);
                if (mOrientation == VERTICAL) {
                    measureChildWithDecorationsAndMargin(view, fullSizeSpec, heightSpec);
                } else {
                    measureChildWithDecorationsAndMargin(view, widthSpec, fullSizeSpec);
                }
            } else {
                measureChildWithDecorationsAndMargin(view, widthSpec, heightSpec);
            }

            final int position = getPosition(view);
            final int spanIndex = mLazySpanLookup.getSpan(position);
            Span currentSpan;
            if (spanIndex == LayoutParams.INVALID_SPAN_ID) {
                if (lp.mFullSpan) {
                    // assign full span items to first span
                    currentSpan = mSpans[0];
                } else {
                    currentSpan = getNextSpan(layoutState);
                }
                mLazySpanLookup.setSpan(position, currentSpan);
            } else {
                currentSpan = mSpans[spanIndex];
            }
            final int start;
            final int end;
            if (layoutState.mLayoutDirection == LAYOUT_END) {
                final int def = mShouldReverseLayout ? mPrimaryOrientation.getEndAfterPadding()
                        : mPrimaryOrientation.getStartAfterPadding();
                start = lp.mFullSpan ? getMaxEnd(def) : currentSpan.getEndLine(def);
                end = start + mPrimaryOrientation.getDecoratedMeasurement(view);
                if (lp.mFullSpan) {
                    for (int i = 0; i < mSpanCount; i++) {
                        mSpans[i].appendToSpan(view);
                    }
                } else {
                    currentSpan.appendToSpan(view);
                }
            } else {
                final int def = mShouldReverseLayout ? mPrimaryOrientation.getEndAfterPadding()
                        : mPrimaryOrientation.getStartAfterPadding();
                end = lp.mFullSpan ? getMinStart(def) : currentSpan.getStartLine(def);
                start = end - mPrimaryOrientation.getDecoratedMeasurement(view);
                if (lp.mFullSpan) {
                    for (int i = 0; i < mSpanCount; i++) {
                        mSpans[i].prependToSpan(view);
                    }
                } else {
                    currentSpan.prependToSpan(view);
                }

            }
            lp.mSpan = currentSpan;

            if (DEBUG) {
                Log.d(TAG, "adding view item " + lp.getViewPosition() + " between " + start + ","
                        + end);
            }

            final int otherStart = lp.mFullSpan ? mSecondaryOrientation.getStartAfterPadding()
                    : currentSpan.mIndex * mSizePerSpan + mSecondaryOrientation
                            .getStartAfterPadding();
            final int otherEnd = otherStart + mSecondaryOrientation.getDecoratedMeasurement(view);
            if (mOrientation == VERTICAL) {
                layoutDecoratedWithMargins(view, otherStart, start, otherEnd, end);
            } else {
                layoutDecoratedWithMargins(view, start, otherStart, end, otherEnd);
            }
            if (lp.mFullSpan) {
                for (int i = 0; i < mSpanCount; i++) {
                    updateRemainingSpans(mSpans[i], mLayoutState.mLayoutDirection, targetLine);
                }
            } else {
                updateRemainingSpans(currentSpan, mLayoutState.mLayoutDirection, targetLine);
            }
            if (mLayoutState.mLayoutDirection == LAYOUT_START) {
                // calculate recycle line
                int maxStart = getMaxStart(currentSpan.getStartLine());
                recycleFromEnd(recycler, Math.max(recycleLine, maxStart) +
                        (mPrimaryOrientation.getEnd() - mPrimaryOrientation.getStartAfterPadding()));
            } else {
                // calculate recycle line
                int minEnd = getMinEnd(currentSpan.getEndLine());
                recycleFromStart(recycler, Math.min(recycleLine, minEnd) -
                        (mPrimaryOrientation.getEnd() - mPrimaryOrientation.getStartAfterPadding()));
            }
        }
        if (DEBUG) {
            Log.d(TAG, "fill, " + getChildCount());
        }
        if (mLayoutState.mLayoutDirection == LAYOUT_START) {
            final int minStart = getMinStart(mPrimaryOrientation.getStartAfterPadding());
            return Math.max(0, mLayoutState.mAvailable + (recycleLine - minStart));
        } else {
            final int max = getMaxEnd(mPrimaryOrientation.getEndAfterPadding());
            return Math.max(0, mLayoutState.mAvailable + (max - recycleLine));
        }
    }

    private void layoutDecoratedWithMargins(View child, int left, int top, int right,
            int bottom) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        layoutDecorated(child, left + lp.leftMargin, top + lp.topMargin, right - lp.rightMargin
                , bottom - lp.bottomMargin);
    }

    private void updateRemainingSpans(Span span, int layoutDir, int targetLine) {
        final int deletedSize = span.getDeletedSize();
        if (layoutDir == LAYOUT_START) {
            final int line = span.getStartLine();
            if (line + deletedSize < targetLine) {
                mRemainingSpans.set(span.mIndex, false);
            }
        } else {
            final int line = span.getEndLine();
            if (line - deletedSize > targetLine) {
                mRemainingSpans.set(span.mIndex, false);
            }
        }
    }

    private int getMaxStart(int def) {
        int maxStart = mSpans[0].getStartLine(def);
        for (int i = 1; i < mSpanCount; i++) {
            final int spanStart = mSpans[i].getStartLine(def);
            if (spanStart > maxStart) {
                maxStart = spanStart;
            }
        }
        return maxStart;
    }

    private int getMinStart(int def) {
        int minStart = mSpans[0].getStartLine(def);
        for (int i = 1; i < mSpanCount; i++) {
            final int spanStart = mSpans[i].getStartLine(def);
            if (spanStart < minStart) {
                minStart = spanStart;
            }
        }
        return minStart;
    }

    private int getMaxEnd(int def) {
        int maxEnd = mSpans[0].getEndLine(def);
        for (int i = 1; i < mSpanCount; i++) {
            final int spanEnd = mSpans[i].getEndLine(def);
            if (spanEnd > maxEnd) {
                maxEnd = spanEnd;
            }
        }
        return maxEnd;
    }

    private int getMinEnd(int def) {
        int minEnd = mSpans[0].getEndLine(def);
        for (int i = 1; i < mSpanCount; i++) {
            final int spanEnd = mSpans[i].getEndLine(def);
            if (spanEnd < minEnd) {
                minEnd = spanEnd;
            }
        }
        return minEnd;
    }

    private void recycleFromStart(RecyclerView.Recycler recycler, int line) {
        if (DEBUG) {
            Log.d(TAG, "recycling from start for line " + line);
        }
        while (getChildCount() > 0) {
            View child = getChildAt(0);
            if (mPrimaryOrientation.getDecoratedEnd(child) < line) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.mFullSpan) {
                    for (int j = 0; j < mSpanCount; j++) {
                        mSpans[j].popStart();
                    }
                } else {
                    lp.mSpan.popStart();
                }
                removeAndRecycleView(child, recycler);
            } else {
                return;// done
            }
        }
    }

    private void recycleFromEnd(RecyclerView.Recycler recycler, int line) {
        final int childCount = getChildCount();
        int i;
        for (i = childCount - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (mPrimaryOrientation.getDecoratedStart(child) > line) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.mFullSpan) {
                    for (int j = 0; j < mSpanCount; j++) {
                        mSpans[j].popEnd();
                    }
                } else {
                    lp.mSpan.popEnd();
                }
                removeAndRecycleView(child, recycler);
            } else {
                return;// done
            }
        }
    }

    /**
     * Finds the span for the next view.
     */
    private Span getNextSpan(LayoutState layoutState) {
        final boolean preferLastSpan = mOrientation == VERTICAL && isLayoutRTL();
        if (layoutState.mLayoutDirection == LAYOUT_END) {
            Span min = mSpans[0];
            int minLine = min.getEndLine(mPrimaryOrientation.getStartAfterPadding());
            final int defaultLine = mPrimaryOrientation.getStartAfterPadding();
            for (int i = 1; i < mSpanCount; i++) {
                final Span other = mSpans[i];
                final int otherLine = other.getEndLine(defaultLine);
                if (otherLine < minLine || (otherLine == minLine && preferLastSpan)) {
                    min = other;
                    minLine = otherLine;
                }
            }
            return min;
        } else {
            Span max = mSpans[0];
            int maxLine = max.getStartLine(mPrimaryOrientation.getEndAfterPadding());
            final int defaultLine = mPrimaryOrientation.getEndAfterPadding();
            for (int i = 1; i < mSpanCount; i++) {
                final Span other = mSpans[i];
                final int otherLine = other.getStartLine(defaultLine);
                if (otherLine > maxLine || (otherLine == maxLine && !preferLastSpan)) {
                    max = other;
                    maxLine = otherLine;
                }
            }
            return max;
        }
    }

    @Override
    public boolean canScrollVertically() {
        return mOrientation == VERTICAL;
    }

    @Override
    public boolean canScrollHorizontally() {
        return mOrientation == HORIZONTAL;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
            RecyclerView.State state) {
        return scrollBy(dx, recycler, state);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
            RecyclerView.State state) {
        return scrollBy(dy, recycler, state);
    }

    private int calculateScrollDirectionForPosition(int position) {
        if (getChildCount() == 0) {
            return mShouldReverseLayout ? LAYOUT_END : LAYOUT_START;
        }
        final int firstChildPos = getFirstChildPosition();
        return position < firstChildPos != mShouldReverseLayout ? LAYOUT_START : LAYOUT_END;
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state,
            int position) {
        LinearSmoothScroller scroller = new LinearSmoothScroller(recyclerView.getContext()) {
            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                final int direction = calculateScrollDirectionForPosition(targetPosition);
                if (direction == 0) {
                    return null;
                }
                if (mOrientation == HORIZONTAL) {
                    return new PointF(direction, 0);
                } else {
                    return new PointF(0, direction);
                }
            }
        };
        scroller.setTargetPosition(position);
        startSmoothScroll(scroller);
    }

    @Override
    public void scrollToPosition(int position) {
        if (mPendingSavedState != null && mPendingSavedState.mAnchorPosition != position) {
            mPendingSavedState.invalidateAnchorPositionInfo();
        }
        mPendingScrollPosition = position;
        mPendingScrollPositionOffset = INVALID_OFFSET;
        requestLayout();
    }

    /**
     * Scroll to the specified adapter position with the given offset from layout start.
     * <p>
     * Note that scroll position change will not be reflected until the next layout call.
     * <p>
     * If you are just trying to make a position visible, use {@link #scrollToPosition(int)}.
     *
     * @param position Index (starting at 0) of the reference item.
     * @param offset   The distance (in pixels) between the start edge of the item view and
     *                 start edge of the RecyclerView.
     * @see #setReverseLayout(boolean)
     * @see #scrollToPosition(int)
     */
    public void scrollToPositionWithOffset(int position, int offset) {
        if (mPendingSavedState != null) {
            mPendingSavedState.invalidateAnchorPositionInfo();
        }
        mPendingScrollPosition = position;
        mPendingScrollPositionOffset = offset;
        requestLayout();
    }

    private int scrollBy(int dt, RecyclerView.Recycler recycler, RecyclerView.State state) {
        ensureOrientationHelper();
        final int referenceChildPosition;
        if (dt > 0) { // layout towards end
            mLayoutState.mLayoutDirection = LAYOUT_END;
            mLayoutState.mItemDirection = mShouldReverseLayout ? ITEM_DIRECTION_HEAD
                    : ITEM_DIRECTION_TAIL;
            referenceChildPosition = getLastChildPosition();
        } else {
            mLayoutState.mLayoutDirection = LAYOUT_START;
            mLayoutState.mItemDirection = mShouldReverseLayout ? ITEM_DIRECTION_TAIL
                    : ITEM_DIRECTION_HEAD;
            referenceChildPosition = getFirstChildPosition();
        }
        mLayoutState.mCurrentPosition = referenceChildPosition + mLayoutState.mItemDirection;
        final int absDt = Math.abs(dt);
        mLayoutState.mAvailable = absDt;
        mLayoutState.mExtra = isSmoothScrolling() ? mPrimaryOrientation.getTotalSpace() : 0;
        int consumed = fill(recycler, mLayoutState, state);
        final int totalScroll;
        if (absDt < consumed) {
            totalScroll = dt;
        } else if (dt < 0) {
            totalScroll = -consumed;
        } else { // dt > 0
            totalScroll = consumed;
        }
        if (DEBUG) {
            Log.d(TAG, "asked " + dt + " scrolled" + totalScroll);
        }

        if (mGapStrategy == GAP_HANDLING_LAZY
                && mLayoutState.mItemDirection == ITEM_DIRECTION_HEAD) {
            final int targetStart = mPrimaryOrientation.getStartAfterPadding();
            final int targetEnd = mPrimaryOrientation.getEndAfterPadding();
            lazyOffsetSpans(-totalScroll, targetStart, targetEnd);
        } else {
            mPrimaryOrientation.offsetChildren(-totalScroll);
        }
        // always reset this if we scroll for a proper save instance state
        mLastLayoutFromEnd = mShouldReverseLayout;

        if (totalScroll != 0 && mGapStrategy != GAP_HANDLING_NONE
                && mLayoutState.mItemDirection == ITEM_DIRECTION_HEAD && !mHasGaps) {
            final int addedChildCount = Math.abs(mLayoutState.mCurrentPosition
                    - (referenceChildPosition + mLayoutState.mItemDirection));
            if (addedChildCount > 0) {
                // check if any child has been attached to wrong span. If so, trigger a re-layout
                // after scroll
                final View viewInWrongSpan;
                final View referenceView = findViewByPosition(referenceChildPosition);
                if (referenceView == null) {
                    viewInWrongSpan = hasGapsToFix(0, getChildCount());
                } else {
                    if (mLayoutState.mLayoutDirection == LAYOUT_START) {
                        viewInWrongSpan = hasGapsToFix(0, addedChildCount);
                    } else {
                        viewInWrongSpan = hasGapsToFix(getChildCount() - addedChildCount,
                                getChildCount());
                    }
                }
                mHasGaps = viewInWrongSpan != null;
            }
        }
        return totalScroll;
    }

    /**
     * The actual method that implements {@link #GAP_HANDLING_LAZY}
     */
    private void lazyOffsetSpans(int offset, int targetStart, int targetEnd) {
        // For each span offset children one by one.
        // When a fullSpan item is reached, stop and wait for other spans to reach to that span.
        // When all reach, offset fullSpan to max of others and continue.
        int childrenToOffset = getChildCount();
        int[] indexPerSpan = new int[mSpanCount];
        int[] offsetPerSpan = new int[mSpanCount];

        final int childOrder = offset > 0 ? ITEM_DIRECTION_TAIL : ITEM_DIRECTION_HEAD;
        if (offset > 0) {
            Arrays.fill(indexPerSpan, 0);
        } else {
            for (int i = 0; i < mSpanCount; i++) {
                indexPerSpan[i] = mSpans[i].mViews.size() - 1;
            }
        }

        for (int i = 0; i < mSpanCount; i++) {
            offsetPerSpan[i] = mSpans[i].getNormalizedOffset(offset, targetStart, targetEnd);
        }
        if (DEBUG) {
            Log.d(TAG, "lazy offset start. normalized: " + Arrays.toString(offsetPerSpan));
        }

        while (childrenToOffset > 0) {
            View fullSpanView = null;
            for (int spanIndex = 0; spanIndex < mSpanCount; spanIndex++) {
                Span span = mSpans[spanIndex];
                int viewIndex;
                for (viewIndex = indexPerSpan[spanIndex];
                        viewIndex < span.mViews.size() && viewIndex >= 0; viewIndex += childOrder) {
                    View view = span.mViews.get(viewIndex);
                    if (DEBUG) {
                        Log.d(TAG, "span " + spanIndex + ", view:" + viewIndex + ", pos:"
                                + getPosition(view));
                    }
                    LayoutParams lp = (LayoutParams) view.getLayoutParams();
                    if (lp.mFullSpan) {
                        if (DEBUG) {
                            Log.d(TAG, "stopping on full span view on index " + viewIndex
                                    + " in span " + spanIndex);
                        }
                        fullSpanView = view;
                        viewIndex += childOrder;// move to next view
                        break;
                    }
                    // offset this child normally
                    mPrimaryOrientation.offsetChild(view, offsetPerSpan[spanIndex]);
                    final int nextChildIndex = viewIndex + childOrder;
                    if (nextChildIndex < span.mViews.size() && nextChildIndex >= 0) {
                        View nextView = span.mViews.get(nextChildIndex);
                        // find gap between, before offset
                        if (childOrder == ITEM_DIRECTION_HEAD) {// negative
                            offsetPerSpan[spanIndex] = Math
                                    .min(0, mPrimaryOrientation.getDecoratedStart(view)
                                            - mPrimaryOrientation.getDecoratedEnd(nextView));
                        } else {
                            offsetPerSpan[spanIndex] = Math
                                    .max(0, mPrimaryOrientation.getDecoratedEnd(view) -
                                            mPrimaryOrientation.getDecoratedStart(nextView));
                        }
                        if (DEBUG) {
                            Log.d(TAG, "offset diff:" + offsetPerSpan[spanIndex] + " between "
                                    + getPosition(nextView) + " and " + getPosition(view));
                        }
                    }
                    childrenToOffset--;
                }
                indexPerSpan[spanIndex] = viewIndex;
            }
            if (fullSpanView != null) {
                // we have to offset this view. We'll offset it as the biggest amount necessary
                int winnerSpan = 0;
                int winnerSpanOffset = Math.abs(offsetPerSpan[winnerSpan]);
                for (int i = 1; i < mSpanCount; i++) {
                    final int spanOffset = Math.abs(offsetPerSpan[i]);
                    if (spanOffset > winnerSpanOffset) {
                        winnerSpan = i;
                        winnerSpanOffset = spanOffset;
                    }
                }
                if (DEBUG) {
                    Log.d(TAG, "winner offset:" + offsetPerSpan[winnerSpan] + " of " + winnerSpan);
                }
                mPrimaryOrientation.offsetChild(fullSpanView, offsetPerSpan[winnerSpan]);
                childrenToOffset--;

                for (int spanIndex = 0; spanIndex < mSpanCount; spanIndex++) {
                    final int nextViewIndex = indexPerSpan[spanIndex];
                    final Span span = mSpans[spanIndex];
                    if (nextViewIndex < span.mViews.size() && nextViewIndex > 0) {
                        View nextView = span.mViews.get(nextViewIndex);
                        // find gap between, before offset
                        if (childOrder == ITEM_DIRECTION_HEAD) {// negative
                            offsetPerSpan[spanIndex] = Math
                                    .min(0, mPrimaryOrientation.getDecoratedStart(fullSpanView)
                                            - mPrimaryOrientation.getDecoratedEnd(nextView));
                        } else {
                            offsetPerSpan[spanIndex] = Math
                                    .max(0, mPrimaryOrientation.getDecoratedEnd(fullSpanView) -
                                            mPrimaryOrientation.getDecoratedStart(nextView));
                        }
                    }
                }
            }
        }
        for (int spanIndex = 0; spanIndex < mSpanCount; spanIndex++) {
            mSpans[spanIndex].invalidateCache();
        }
    }

    private int getLastChildPosition() {
        final int childCount = getChildCount();
        return childCount == 0 ? 0 : getPosition(getChildAt(childCount - 1));
    }

    private int getFirstChildPosition() {
        final int childCount = getChildCount();
        return childCount == 0 ? 0 : getPosition(getChildAt(0));
    }

    /**
     * Finds the first View that can be used as an anchor View.
     *
     * @return Position of the View or 0 if it cannot find any such View.
     */
    private int findFirstReferenceChildPosition(int itemCount) {
        final int limit = getChildCount();
        for (int i = 0; i < limit; i++) {
            final View view = getChildAt(i);
            final int position = getPosition(view);
            if (position >= 0 && position < itemCount) {
                return position;
            }
        }
        return 0;
    }

    /**
     * Finds the last View that can be used as an anchor View.
     *
     * @return Position of the View or 0 if it cannot find any such View.
     */
    private int findLastReferenceChildPosition(int itemCount) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            final View view = getChildAt(i);
            final int position = getPosition(view);
            if (position >= 0 && position < itemCount) {
                return position;
            }
        }
        return 0;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            return new LayoutParams((ViewGroup.MarginLayoutParams) lp);
        } else {
            return new LayoutParams(lp);
        }
    }

    @Override
    public boolean checkLayoutParams(RecyclerView.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }

    public int getOrientation() {
        return mOrientation;
    }


    /**
     * LayoutParams used by StaggeredGridLayoutManager.
     */
    public static class LayoutParams extends RecyclerView.LayoutParams {

        /**
         * Span Id for Views that are not laid out yet.
         */
        public static final int INVALID_SPAN_ID = -1;

        // Package scope to be able to access from tests.
        Span mSpan;

        boolean mFullSpan;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(RecyclerView.LayoutParams source) {
            super(source);
        }

        /**
         * When set to true, the item will layout using all span area. That means, if orientation
         * is vertical, the view will have full width; if orientation is horizontal, the view will
         * have full height.
         *
         * @param fullSpan True if this item should traverse all spans.
         */
        public void setFullSpan(boolean fullSpan) {
            mFullSpan = fullSpan;
        }

        /**
         * Returns the Span index to which this View is assigned.
         *
         * @return The Span index of the View. If View is not yet assigned to any span, returns
         * {@link #INVALID_SPAN_ID}.
         */
        public final int getSpanIndex() {
            if (mSpan == null) {
                return INVALID_SPAN_ID;
            }
            return mSpan.mIndex;
        }
    }

    // Package scoped to access from tests.
    class Span {

        static final int INVALID_LINE = Integer.MIN_VALUE;

        private ArrayList<View> mViews = new ArrayList<View>();

        int mCachedStart = INVALID_LINE;

        int mCachedEnd = INVALID_LINE;

        int mDeletedSize = 0;

        final int mIndex;

        private Span(int index) {
            mIndex = index;
        }

        int getStartLine(int def) {
            if (mCachedStart != INVALID_LINE) {
                return mCachedStart;
            }
            if (mViews.size() == 0) {
                return def;
            }
            mCachedStart = mPrimaryOrientation.getDecoratedStart(mViews.get(0));
            return mCachedStart;
        }

        // Use this one when default value does not make sense and not having a value means a bug.
        int getStartLine() {
            if (mCachedStart != INVALID_LINE) {
                return mCachedStart;
            }
            mCachedStart = mPrimaryOrientation.getDecoratedStart(mViews.get(0));
            return mCachedStart;
        }

        int getEndLine(int def) {
            if (mCachedEnd != INVALID_LINE) {
                return mCachedEnd;
            }
            final int size = mViews.size();
            if (size == 0) {
                return def;
            }
            mCachedEnd = mPrimaryOrientation.getDecoratedEnd(mViews.get(size - 1));
            return mCachedEnd;
        }

        // Use this one when default value does not make sense and not having a value means a bug.
        int getEndLine() {
            if (mCachedEnd != INVALID_LINE) {
                return mCachedEnd;
            }
            mCachedEnd = mPrimaryOrientation.getDecoratedEnd(mViews.get(mViews.size() - 1));
            return mCachedEnd;
        }

        void prependToSpan(View view) {
            LayoutParams lp = getLayoutParams(view);
            lp.mSpan = this;
            mViews.add(0, view);
            mCachedStart = INVALID_LINE;
            if (mViews.size() == 1) {
                mCachedEnd = INVALID_LINE;
            }
            if (lp.isItemRemoved() || lp.isItemChanged()) {
                mDeletedSize += mPrimaryOrientation.getDecoratedMeasurement(view);
            }
        }

        void appendToSpan(View view) {
            LayoutParams lp = getLayoutParams(view);
            lp.mSpan = this;
            mViews.add(view);
            mCachedEnd = INVALID_LINE;
            if (mViews.size() == 1) {
                mCachedStart = INVALID_LINE;
            }
            if (lp.isItemRemoved() || lp.isItemChanged()) {
                mDeletedSize += mPrimaryOrientation.getDecoratedMeasurement(view);
            }
        }

        // Useful method to preserve positions on a re-layout.
        void cacheReferenceLineAndClear(boolean reverseLayout, int offset) {
            int reference;
            if (reverseLayout) {
                reference = getEndLine(INVALID_LINE);
            } else {
                reference = getStartLine(INVALID_LINE);
            }
            clear();
            if (reference == INVALID_LINE) {
                return;
            }
            if (offset != INVALID_OFFSET) {
                reference += offset;
            }
            mCachedStart = mCachedEnd = reference;
        }

        void clear() {
            mViews.clear();
            invalidateCache();
            mDeletedSize = 0;
        }

        void invalidateCache() {
            mCachedStart = INVALID_LINE;
            mCachedEnd = INVALID_LINE;
        }

        void setLine(int line) {
            mCachedEnd = mCachedStart = line;
        }

        void popEnd() {
            final int size = mViews.size();
            View end = mViews.remove(size - 1);
            final LayoutParams lp = getLayoutParams(end);
            lp.mSpan = null;
            if (lp.isItemRemoved() || lp.isItemChanged()) {
                mDeletedSize -= mPrimaryOrientation.getDecoratedMeasurement(end);
            }
            if (size == 1) {
                mCachedStart = INVALID_LINE;
            }
            mCachedEnd = INVALID_LINE;
        }

        void popStart() {
            View start = mViews.remove(0);
            final LayoutParams lp = getLayoutParams(start);
            lp.mSpan = null;
            if (mViews.size() == 0) {
                mCachedEnd = INVALID_LINE;
            }
            if (lp.isItemRemoved() || lp.isItemChanged()) {
                mDeletedSize -= mPrimaryOrientation.getDecoratedMeasurement(start);
            }
            mCachedStart = INVALID_LINE;
        }

        // TODO cache this.
        public int getDeletedSize() {
            return mDeletedSize;
        }

        LayoutParams getLayoutParams(View view) {
            return (LayoutParams) view.getLayoutParams();
        }

        void onOffset(int dt) {
            if (mCachedStart != INVALID_LINE) {
                mCachedStart += dt;
            }
            if (mCachedEnd != INVALID_LINE) {
                mCachedEnd += dt;
            }
        }

        // normalized offset is how much this span can scroll
        int getNormalizedOffset(int dt, int targetStart, int targetEnd) {
            if (mViews.size() == 0) {
                return 0;
            }
            if (dt < 0) {
                final int endSpace = getEndLine() - targetEnd;
                if (endSpace <= 0) {
                    return 0;
                }
                return -dt > endSpace ? -endSpace : dt;
            } else {
                final int startSpace = targetStart - getStartLine();
                if (startSpace <= 0) {
                    return 0;
                }
                return startSpace < dt ? startSpace : dt;
            }
        }

        /**
         * Returns if there is no child between start-end lines
         *
         * @param start The start line
         * @param end   The end line
         * @return true if a new child can be added between start and end
         */
        boolean isEmpty(int start, int end) {
            final int count = mViews.size();
            for (int i = 0; i < count; i++) {
                final View view = mViews.get(i);
                if (mPrimaryOrientation.getDecoratedStart(view) < end &&
                        mPrimaryOrientation.getDecoratedEnd(view) > start) {
                    return false;
                }
            }
            return true;
        }

        public int findFirstVisibleItemPosition() {
            return mReverseLayout
                    ? findOneVisibleChild(mViews.size() - 1, -1, false)
                    : findOneVisibleChild(0, mViews.size(), false);
        }

        public int findFirstCompletelyVisibleItemPosition() {
            return mReverseLayout
                    ? findOneVisibleChild(mViews.size() -1, -1, true)
                    : findOneVisibleChild(0, mViews.size(), true);
        }

        public int findLastVisibleItemPosition() {
            return mReverseLayout
                    ? findOneVisibleChild(0, mViews.size(), false)
                    : findOneVisibleChild(mViews.size() - 1, -1, false);
        }

        public int findLastCompletelyVisibleItemPosition() {
            return mReverseLayout
                    ? findOneVisibleChild(0, mViews.size(), true)
                    : findOneVisibleChild(mViews.size() - 1, -1, true);
        }

        int findOneVisibleChild(int fromIndex, int toIndex, boolean completelyVisible) {
            final int start = mPrimaryOrientation.getStartAfterPadding();
            final int end = mPrimaryOrientation.getEndAfterPadding();
            final int next = toIndex > fromIndex ? 1 : -1;
            for (int i = fromIndex; i != toIndex; i+=next) {
                final View child = mViews.get(i);
                final int childStart = mPrimaryOrientation.getDecoratedStart(child);
                final int childEnd = mPrimaryOrientation.getDecoratedEnd(child);
                if (childStart < end && childEnd > start) {
                    if (completelyVisible) {
                        if (childStart >= start && childEnd <= end) {
                            return getPosition(child);
                        }
                    } else {
                        return getPosition(child);
                    }
                }
            }
            return RecyclerView.NO_POSITION;
        }
    }

    /**
     * An array of mappings from adapter position to span.
     * This only grows when a write happens and it grows up to the size of the adapter.
     */
    static class LazySpanLookup {

        private static final int MIN_SIZE = 10;

        int[] mData;

        int mAdapterSize; // we don't want to grow beyond that, unless it grows

        void invalidateAfter(int position) {
            if (mData == null) {
                return;
            }
            if (position >= mData.length) {
                return;
            }
            Arrays.fill(mData, position, mData.length, LayoutParams.INVALID_SPAN_ID);
        }

        int getSpan(int position) {
            if (mData == null || position >= mData.length) {
                return LayoutParams.INVALID_SPAN_ID;
            } else {
                return mData[position];
            }
        }

        void setSpan(int position, Span span) {
            ensureSize(position);
            mData[position] = span.mIndex;
        }

        int sizeForPosition(int position) {
            int len = mData.length;
            while (len <= position) {
                len *= 2;
            }
            if (len > mAdapterSize) {
                len = mAdapterSize;
            }
            return len;
        }

        void ensureSize(int position) {
            if (mData == null) {
                mData = new int[Math.max(position, MIN_SIZE) + 1];
                Arrays.fill(mData, LayoutParams.INVALID_SPAN_ID);
            } else if (position >= mData.length) {
                int[] old = mData;
                mData = new int[sizeForPosition(position)];
                System.arraycopy(old, 0, mData, 0, old.length);
                Arrays.fill(mData, old.length, mData.length, LayoutParams.INVALID_SPAN_ID);
            }
        }

        void clear() {
            if (mData != null) {
                Arrays.fill(mData, LayoutParams.INVALID_SPAN_ID);
            }
        }

        void offsetForRemoval(int positionStart, int itemCount) {
            ensureSize(positionStart + itemCount);
            System.arraycopy(mData, positionStart + itemCount, mData, positionStart,
                    mData.length - positionStart - itemCount);
            Arrays.fill(mData, mData.length - itemCount, mData.length,
                    LayoutParams.INVALID_SPAN_ID);
        }

        void offsetForAddition(int positionStart, int itemCount) {
            ensureSize(positionStart + itemCount);
            System.arraycopy(mData, positionStart, mData, positionStart + itemCount,
                    mData.length - positionStart - itemCount);
            Arrays.fill(mData, positionStart, positionStart + itemCount,
                    LayoutParams.INVALID_SPAN_ID);
        }
    }

    static class SavedState implements Parcelable {

        int mOrientation;

        int mSpanCount;

        int mGapStrategy;

        int mAnchorPosition;

        int mVisibleAnchorPosition; // if span count changes (span offsets are invalidated),
        // we use this one instead

        int[] mSpanOffsets;

        int mSpanLookupSize;

        int[] mSpanLookup;

        boolean mReverseLayout;

        boolean mAnchorLayoutFromEnd;

        boolean mHasSpanOffsets;

        public SavedState() {
        }

        SavedState(Parcel in) {
            mOrientation = in.readInt();
            mSpanCount = in.readInt();
            mGapStrategy = in.readInt();
            mAnchorPosition = in.readInt();
            mVisibleAnchorPosition = in.readInt();
            mHasSpanOffsets = in.readInt() == 1;
            if (mHasSpanOffsets) {
                mSpanOffsets = new int[mSpanCount];
                in.readIntArray(mSpanOffsets);
            }

            mSpanLookupSize = in.readInt();
            if (mSpanLookupSize > 0) {
                mSpanLookup = new int[mSpanLookupSize];
                in.readIntArray(mSpanLookup);
            }
            mReverseLayout = in.readInt() == 1;
            mAnchorLayoutFromEnd = in.readInt() == 1;
        }

        public SavedState(SavedState other) {
            mOrientation = other.mOrientation;
            mSpanCount = other.mSpanCount;
            mGapStrategy = other.mGapStrategy;
            mAnchorPosition = other.mAnchorPosition;
            mVisibleAnchorPosition = other.mVisibleAnchorPosition;
            mHasSpanOffsets = other.mHasSpanOffsets;
            mSpanOffsets = other.mSpanOffsets;
            mSpanLookupSize = other.mSpanLookupSize;
            mSpanLookup = other.mSpanLookup;
            mReverseLayout = other.mReverseLayout;
            mAnchorLayoutFromEnd = other.mAnchorLayoutFromEnd;
        }

        void invalidateSpanInfo() {
            mSpanOffsets = null;
            mHasSpanOffsets = false;
            mSpanCount = -1;
            mSpanLookupSize = 0;
            mSpanLookup = null;
        }

        void invalidateAnchorPositionInfo() {
            mSpanOffsets = null;
            mHasSpanOffsets = false;
            mAnchorPosition = RecyclerView.NO_POSITION;
            mVisibleAnchorPosition = RecyclerView.NO_POSITION;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mOrientation);
            dest.writeInt(mSpanCount);
            dest.writeInt(mGapStrategy);
            dest.writeInt(mAnchorPosition);
            dest.writeInt(mVisibleAnchorPosition);
            dest.writeInt(mHasSpanOffsets ? 1 : 0);
            if (mHasSpanOffsets) {
                dest.writeIntArray(mSpanOffsets);
            }
            dest.writeInt(mSpanLookupSize);
            if (mSpanLookupSize > 0) {
                dest.writeIntArray(mSpanLookup);
            }
            dest.writeInt(mReverseLayout ? 1 : 0);
            dest.writeInt(mAnchorLayoutFromEnd ? 1 : 0);
        }

        @Override
        public String toString() {
            return "SavedState{" +
                    "mOrientation=" + mOrientation +
                    ", mSpanCount=" + mSpanCount +
                    ", mGapStrategy=" + mGapStrategy +
                    ", mAnchorPosition=" + mAnchorPosition +
                    ", mVisibleAnchorPosition=" + mVisibleAnchorPosition +
                    ", mSpanOffsets=" + Arrays.toString(mSpanOffsets) +
                    ", mSpanLookupSize=" + mSpanLookupSize +
                    ", mSpanLookup=" + Arrays.toString(mSpanLookup) +
                    ", mReverseLayout=" + mReverseLayout +
                    ", mAnchorLayoutFromEnd=" + mAnchorLayoutFromEnd +
                    ", mHasSpanOffsets=" + mHasSpanOffsets +
                    '}';
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
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
}
