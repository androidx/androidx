/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Recycler;
import android.support.v7.widget.RecyclerView.State;

import static android.support.v7.widget.RecyclerView.NO_ID;
import static android.support.v7.widget.RecyclerView.NO_POSITION;
import static android.support.v7.widget.RecyclerView.HORIZONTAL;
import static android.support.v7.widget.RecyclerView.VERTICAL;

import android.util.AttributeSet;
import android.util.Log;
import android.view.FocusFinder;
import android.view.Gravity;
import android.view.View;
import android.view.ViewParent;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewGroup;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

final class GridLayoutManager extends RecyclerView.LayoutManager {

     /*
      * LayoutParams for {@link HorizontalGridView} and {@link VerticalGridView}.
      * The class currently does two internal jobs:
      * - Saves optical bounds insets.
      * - Caches focus align view center.
      */
    static class LayoutParams extends RecyclerView.LayoutParams {

        // The view is saved only during animation.
        private View mView;

        // For placement
        private int mLeftInset;
        private int mTopInset;
        private int mRighInset;
        private int mBottomInset;

        // For alignment
        private int mAlignX;
        private int mAlignY;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(RecyclerView.LayoutParams source) {
            super(source);
        }

        public LayoutParams(LayoutParams source) {
            super(source);
        }

        int getAlignX() {
            return mAlignX;
        }

        int getAlignY() {
            return mAlignY;
        }

        int getOpticalLeft(View view) {
            return view.getLeft() + mLeftInset;
        }

        int getOpticalTop(View view) {
            return view.getTop() + mTopInset;
        }

        int getOpticalRight(View view) {
            return view.getRight() - mRighInset;
        }

        int getOpticalBottom(View view) {
            return view.getBottom() - mBottomInset;
        }

        int getOpticalWidth(View view) {
            return view.getWidth() - mLeftInset - mRighInset;
        }

        int getOpticalHeight(View view) {
            return view.getHeight() - mTopInset - mBottomInset;
        }

        int getOpticalLeftInset() {
            return mLeftInset;
        }

        int getOpticalRightInset() {
            return mRighInset;
        }

        int getOpticalTopInset() {
            return mTopInset;
        }

        int getOpticalBottomInset() {
            return mBottomInset;
        }

        void setAlignX(int alignX) {
            mAlignX = alignX;
        }

        void setAlignY(int alignY) {
            mAlignY = alignY;
        }

        void setOpticalInsets(int leftInset, int topInset, int rightInset, int bottomInset) {
            mLeftInset = leftInset;
            mTopInset = topInset;
            mRighInset = rightInset;
            mBottomInset = bottomInset;
        }

        private void invalidateItemDecoration() {
            ViewParent parent = mView.getParent();
            if (parent instanceof RecyclerView) {
                // TODO: we only need invalidate parent if it has ItemDecoration
                ((RecyclerView) parent).invalidate();
            }
        }
    }

    private static final String TAG = "GridLayoutManager";
    private static final boolean DEBUG = false;

    private String getTag() {
        return TAG + ":" + mBaseGridView.getId();
    }

    private final BaseGridView mBaseGridView;

    /**
     * The orientation of a "row".
     */
    private int mOrientation = HORIZONTAL;

    private RecyclerView.State mState;
    private RecyclerView.Recycler mRecycler;

    private boolean mInLayout = false;
    private boolean mInSelection = false;

    private OnChildSelectedListener mChildSelectedListener = null;

    /**
     * The focused position, it's not the currently visually aligned position
     * but it is the final position that we intend to focus on. If there are
     * multiple setSelection() called, mFocusPosition saves last value.
     */
    private int mFocusPosition = NO_POSITION;

    /**
     * The offset to be applied to mFocusPosition, due to adapter change, on the next
     * layout.  Set to Integer.MIN_VALUE means item was removed.
     * TODO:  This is somewhat duplication of RecyclerView getOldPosition() which is
     * unfortunately cleared after prelayout.
     */
    private int mFocusPositionOffset = 0;

    /**
     * Force a full layout under certain situations.
     */
    private boolean mForceFullLayout;

    /**
     * True if layout is enabled.
     */
    private boolean mLayoutEnabled = true;

    /**
     * override child visibility
     */
    private int mChildVisibility = -1;

    /**
     * The scroll offsets of the viewport relative to the entire view.
     */
    private int mScrollOffsetPrimary;
    private int mScrollOffsetSecondary;

    /**
     * User-specified row height/column width.  Can be WRAP_CONTENT.
     */
    private int mRowSizeSecondaryRequested;

    /**
     * The fixed size of each grid item in the secondary direction. This corresponds to
     * the row height, equal for all rows. Grid items may have variable length
     * in the primary direction.
     */
    private int mFixedRowSizeSecondary;

    /**
     * Tracks the secondary size of each row.
     */
    private int[] mRowSizeSecondary;

    /**
     * Flag controlling whether the current/next layout should
     * be updating the secondary size of rows.
     */
    private boolean mRowSecondarySizeRefresh;

    /**
     * The maximum measured size of the view.
     */
    private int mMaxSizeSecondary;

    /**
     * Margin between items.
     */
    private int mHorizontalMargin;
    /**
     * Margin between items vertically.
     */
    private int mVerticalMargin;
    /**
     * Margin in main direction.
     */
    private int mMarginPrimary;
    /**
     * Margin in second direction.
     */
    private int mMarginSecondary;
    /**
     * How to position child in secondary direction.
     */
    private int mGravity = Gravity.LEFT | Gravity.TOP;
    /**
     * The number of rows in the grid.
     */
    private int mNumRows;
    /**
     * Number of rows requested, can be 0 to be determined by parent size and
     * rowHeight.
     */
    private int mNumRowsRequested = 1;

    /**
     * Tracking start/end position of each row for visible items.
     */
    private StaggeredGrid.Row[] mRows;

    /**
     * Saves grid information of each view.
     */
    private StaggeredGrid mGrid;
    /**
     * Position of first item (included) that has attached views.
     */
    private int mFirstVisiblePos;
    /**
     * Position of last item (included) that has attached views.
     */
    private int mLastVisiblePos;

    /**
     * Focus Scroll strategy.
     */
    private int mFocusScrollStrategy = BaseGridView.FOCUS_SCROLL_ALIGNED;
    /**
     * Defines how item view is aligned in the window.
     */
    private final WindowAlignment mWindowAlignment = new WindowAlignment();

    /**
     * Defines how item view is aligned.
     */
    private final ItemAlignment mItemAlignment = new ItemAlignment();

    /**
     * Dimensions of the view, width or height depending on orientation.
     */
    private int mSizePrimary;

    /**
     *  Allow DPAD key to navigate out at the front of the View (where position = 0),
     *  default is false.
     */
    private boolean mFocusOutFront;

    /**
     * Allow DPAD key to navigate out at the end of the view, default is false.
     */
    private boolean mFocusOutEnd;

    /**
     * True if focus search is disabled.
     */
    private boolean mFocusSearchDisabled;

    /**
     * True if prune child,  might be disabled during transition.
     */
    private boolean mPruneChild = true;

    /**
     * True if scroll content,  might be disabled during transition.
     */
    private boolean mScrollEnabled = true;

    /**
     * Percent of overreach.
     */
    private float mPrimaryOverReach = 1f;

    private int[] mTempDeltas = new int[2];

    /**
     * Temporaries used for measuring.
     */
    private int[] mMeasuredDimension = new int[2];

    final ViewsStateBundle mChildrenStates = new ViewsStateBundle();

    public GridLayoutManager(BaseGridView baseGridView) {
        mBaseGridView = baseGridView;
    }

    public void setOrientation(int orientation) {
        if (orientation != HORIZONTAL && orientation != VERTICAL) {
            if (DEBUG) Log.v(getTag(), "invalid orientation: " + orientation);
            return;
        }

        mOrientation = orientation;
        mWindowAlignment.setOrientation(orientation);
        mItemAlignment.setOrientation(orientation);
        mForceFullLayout = true;
    }

    public int getFocusScrollStrategy() {
        return mFocusScrollStrategy;
    }

    public void setFocusScrollStrategy(int focusScrollStrategy) {
        mFocusScrollStrategy = focusScrollStrategy;
    }

    public void setWindowAlignment(int windowAlignment) {
        mWindowAlignment.mainAxis().setWindowAlignment(windowAlignment);
    }

    public int getWindowAlignment() {
        return mWindowAlignment.mainAxis().getWindowAlignment();
    }

    public void setWindowAlignmentOffset(int alignmentOffset) {
        mWindowAlignment.mainAxis().setWindowAlignmentOffset(alignmentOffset);
    }

    public int getWindowAlignmentOffset() {
        return mWindowAlignment.mainAxis().getWindowAlignmentOffset();
    }

    public void setWindowAlignmentOffsetPercent(float offsetPercent) {
        mWindowAlignment.mainAxis().setWindowAlignmentOffsetPercent(offsetPercent);
    }

    public float getWindowAlignmentOffsetPercent() {
        return mWindowAlignment.mainAxis().getWindowAlignmentOffsetPercent();
    }

    public void setItemAlignmentOffset(int alignmentOffset) {
        mItemAlignment.mainAxis().setItemAlignmentOffset(alignmentOffset);
        updateChildAlignments();
    }

    public int getItemAlignmentOffset() {
        return mItemAlignment.mainAxis().getItemAlignmentOffset();
    }

    public void setItemAlignmentOffsetWithPadding(boolean withPadding) {
        mItemAlignment.mainAxis().setItemAlignmentOffsetWithPadding(withPadding);
        updateChildAlignments();
    }

    public boolean isItemAlignmentOffsetWithPadding() {
        return mItemAlignment.mainAxis().isItemAlignmentOffsetWithPadding();
    }

    public void setItemAlignmentOffsetPercent(float offsetPercent) {
        mItemAlignment.mainAxis().setItemAlignmentOffsetPercent(offsetPercent);
        updateChildAlignments();
    }

    public float getItemAlignmentOffsetPercent() {
        return mItemAlignment.mainAxis().getItemAlignmentOffsetPercent();
    }

    public void setItemAlignmentViewId(int viewId) {
        mItemAlignment.mainAxis().setItemAlignmentViewId(viewId);
        updateChildAlignments();
    }

    public int getItemAlignmentViewId() {
        return mItemAlignment.mainAxis().getItemAlignmentViewId();
    }

    public void setFocusOutAllowed(boolean throughFront, boolean throughEnd) {
        mFocusOutFront = throughFront;
        mFocusOutEnd = throughEnd;
    }

    public void setNumRows(int numRows) {
        if (numRows < 0) throw new IllegalArgumentException();
        mNumRowsRequested = numRows;
        mForceFullLayout = true;
    }

    /**
     * Set the row height. May be WRAP_CONTENT, or a size in pixels.
     */
    public void setRowHeight(int height) {
        if (height >= 0 || height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            mRowSizeSecondaryRequested = height;
        } else {
            throw new IllegalArgumentException("Invalid row height: " + height);
        }
    }

    public void setItemMargin(int margin) {
        mVerticalMargin = mHorizontalMargin = margin;
        mMarginPrimary = mMarginSecondary = margin;
    }

    public void setVerticalMargin(int margin) {
        if (mOrientation == HORIZONTAL) {
            mMarginSecondary = mVerticalMargin = margin;
        } else {
            mMarginPrimary = mVerticalMargin = margin;
        }
    }

    public void setHorizontalMargin(int margin) {
        if (mOrientation == HORIZONTAL) {
            mMarginPrimary = mHorizontalMargin = margin;
        } else {
            mMarginSecondary = mHorizontalMargin = margin;
        }
    }

    public int getVerticalMargin() {
        return mVerticalMargin;
    }

    public int getHorizontalMargin() {
        return mHorizontalMargin;
    }

    public void setGravity(int gravity) {
        mGravity = gravity;
    }

    protected boolean hasDoneFirstLayout() {
        return mGrid != null;
    }

    public void setOnChildSelectedListener(OnChildSelectedListener listener) {
        mChildSelectedListener = listener;
    }

    public void setPrimaryOverReach(float fraction) {
        if (fraction != mPrimaryOverReach) {
            if (DEBUG) Log.v(getTag(), "setPrimaryOverReach " + fraction);
            mPrimaryOverReach = fraction;
            requestLayout();
        }
    }

    private int getPositionByView(View view) {
        if (view == null) {
            return NO_POSITION;
        }
        LayoutParams params = (LayoutParams) view.getLayoutParams();
        if (params == null || params.isItemRemoved()) {
            // when item is removed, the position value can be any value.
            return NO_POSITION;
        }
        return params.getViewPosition();
    }

    private int getPositionByIndex(int index) {
        return getPositionByView(getChildAt(index));
    }

    private void dispatchChildSelected() {
        if (mChildSelectedListener == null) {
            return;
        }
        if (mFocusPosition != NO_POSITION) {
            View view = findViewByPosition(mFocusPosition);
            if (view != null) {
                RecyclerView.ViewHolder vh = mBaseGridView.getChildViewHolder(view);
                mChildSelectedListener.onChildSelected(mBaseGridView, view, mFocusPosition,
                        vh == null? NO_ID: vh.getItemId());
                return;
            }
        }
        mChildSelectedListener.onChildSelected(mBaseGridView, null, NO_POSITION, NO_ID);
    }

    @Override
    public boolean canScrollHorizontally() {
        // We can scroll horizontally if we have horizontal orientation, or if
        // we are vertical and have more than one column.
        return mOrientation == HORIZONTAL || mNumRows > 1;
    }

    @Override
    public boolean canScrollVertically() {
        // We can scroll vertically if we have vertical orientation, or if we
        // are horizontal and have more than one row.
        return mOrientation == VERTICAL || mNumRows > 1;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context context, AttributeSet attrs) {
        return new LayoutParams(context, attrs);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) lp);
        } else if (lp instanceof RecyclerView.LayoutParams) {
            return new LayoutParams((RecyclerView.LayoutParams) lp);
        } else if (lp instanceof MarginLayoutParams) {
            return new LayoutParams((MarginLayoutParams) lp);
        } else {
            return new LayoutParams(lp);
        }
    }

    protected View getViewForPosition(int position) {
        return mRecycler.getViewForPosition(position);
    }

    final int getOpticalLeft(View v) {
        return ((LayoutParams) v.getLayoutParams()).getOpticalLeft(v);
    }

    final int getOpticalRight(View v) {
        return ((LayoutParams) v.getLayoutParams()).getOpticalRight(v);
    }

    final int getOpticalTop(View v) {
        return ((LayoutParams) v.getLayoutParams()).getOpticalTop(v);
    }

    final int getOpticalBottom(View v) {
        return ((LayoutParams) v.getLayoutParams()).getOpticalBottom(v);
    }

    private int getViewMin(View v) {
        return (mOrientation == HORIZONTAL) ? getOpticalLeft(v) : getOpticalTop(v);
    }

    private int getViewMax(View v) {
        return (mOrientation == HORIZONTAL) ? getOpticalRight(v) : getOpticalBottom(v);
    }

    private int getViewCenter(View view) {
        return (mOrientation == HORIZONTAL) ? getViewCenterX(view) : getViewCenterY(view);
    }

    private int getViewCenterSecondary(View view) {
        return (mOrientation == HORIZONTAL) ? getViewCenterY(view) : getViewCenterX(view);
    }

    private int getViewCenterX(View v) {
        LayoutParams p = (LayoutParams) v.getLayoutParams();
        return p.getOpticalLeft(v) + p.getAlignX();
    }

    private int getViewCenterY(View v) {
        LayoutParams p = (LayoutParams) v.getLayoutParams();
        return p.getOpticalTop(v) + p.getAlignY();
    }

    /**
     * Save Recycler and State for convenience.  Must be paired with leaveContext().
     */
    private void saveContext(Recycler recycler, State state) {
        if (mRecycler != null || mState != null) {
            Log.e(TAG, "Recycler information was not released, bug!");
        }
        mRecycler = recycler;
        mState = state;
    }

    /**
     * Discard saved Recycler and State.
     */
    private void leaveContext() {
        mRecycler = null;
        mState = null;
    }

    /**
     * Re-initialize data structures for a data change or handling invisible
     * selection. The method tries its best to preserve position information so
     * that staggered grid looks same before and after re-initialize.
     * @param focusPosition The initial focusPosition that we would like to
     *        focus on.
     * @return Actual position that can be focused on.
     */
    private int init(int focusPosition) {

        final int newItemCount = mState.getItemCount();

        if (focusPosition == NO_POSITION && newItemCount > 0) {
            // if focus position is never set before,  initialize it to 0
            focusPosition = 0;
        }
        // If adapter has changed then caches are invalid; otherwise,
        // we try to maintain each row's position if number of rows keeps the same
        // and existing mGrid contains the focusPosition.
        if (mRows != null && mNumRows == mRows.length &&
                mGrid != null && mGrid.getSize() > 0 && focusPosition >= 0 &&
                focusPosition >= mGrid.getFirstIndex() &&
                focusPosition <= mGrid.getLastIndex()) {
            // strip mGrid to a subset (like a column) that contains focusPosition
            mGrid.stripDownTo(focusPosition);
            // make sure that remaining items do not exceed new adapter size
            int firstIndex = mGrid.getFirstIndex();
            int lastIndex = mGrid.getLastIndex();
            if (DEBUG) {
                Log .v(getTag(), "mGrid firstIndex " + firstIndex + " lastIndex " + lastIndex);
            }
            for (int i = lastIndex; i >=firstIndex; i--) {
                if (i >= newItemCount) {
                    mGrid.removeLast();
                }
            }
            if (mGrid.getSize() == 0) {
                focusPosition = newItemCount - 1;
                // initialize row start locations
                for (int i = 0; i < mNumRows; i++) {
                    mRows[i].low = 0;
                    mRows[i].high = 0;
                }
                if (DEBUG) Log.v(getTag(), "mGrid zero size");
            } else {
                // initialize row start locations
                for (int i = 0; i < mNumRows; i++) {
                    mRows[i].low = Integer.MAX_VALUE;
                    mRows[i].high = Integer.MIN_VALUE;
                }
                firstIndex = mGrid.getFirstIndex();
                lastIndex = mGrid.getLastIndex();
                if (focusPosition > lastIndex) {
                    focusPosition = mGrid.getLastIndex();
                }
                if (DEBUG) {
                    Log.v(getTag(), "mGrid firstIndex " + firstIndex + " lastIndex "
                        + lastIndex + " focusPosition " + focusPosition);
                }
                // fill rows with minimal view positions of the subset
                for (int i = firstIndex; i <= lastIndex; i++) {
                    View v = findViewByPosition(i);
                    if (v == null) {
                        continue;
                    }
                    int row = mGrid.getLocation(i).row;
                    int low = getViewMin(v) + mScrollOffsetPrimary;
                    if (low < mRows[row].low) {
                        mRows[row].low = mRows[row].high = low;
                    }
                }
                int firstItemRowPosition = mRows[mGrid.getLocation(firstIndex).row].low;
                if (firstItemRowPosition == Integer.MAX_VALUE) {
                    firstItemRowPosition = 0;
                }
                if (mState.didStructureChange()) {
                    // if there is structure change, the removed item might be in the
                    // subset,  so it is meaningless to maintain the low locations.
                    for (int i = 0; i < mNumRows; i++) {
                        mRows[i].low = firstItemRowPosition;
                        mRows[i].high = firstItemRowPosition;
                    }
                } else {
                    // fill other rows that does not include the subset using first item
                    for (int i = 0; i < mNumRows; i++) {
                        if (mRows[i].low == Integer.MAX_VALUE) {
                            mRows[i].low = mRows[i].high = firstItemRowPosition;
                        }
                    }
                }
            }

            // Same adapter, we can reuse any attached views
            detachAndScrapAttachedViews(mRecycler);

        } else {
            // otherwise recreate data structure
            mRows = new StaggeredGrid.Row[mNumRows];

            for (int i = 0; i < mNumRows; i++) {
                mRows[i] = new StaggeredGrid.Row();
            }
            mGrid = new StaggeredGridDefault();
            if (newItemCount == 0) {
                focusPosition = NO_POSITION;
            } else if (focusPosition >= newItemCount) {
                focusPosition = newItemCount - 1;
            }

            // Adapter may have changed so remove all attached views permanently
            removeAndRecycleAllViews(mRecycler);

            mScrollOffsetPrimary = 0;
            mScrollOffsetSecondary = 0;
            mWindowAlignment.reset();
        }

        mGrid.setProvider(mGridProvider);
        // mGrid share the same Row array information
        mGrid.setRows(mRows);
        mFirstVisiblePos = mLastVisiblePos = NO_POSITION;

        initScrollController();
        updateScrollSecondAxis();

        return focusPosition;
    }

    private int getRowSizeSecondary(int rowIndex) {
        if (mFixedRowSizeSecondary != 0) {
            return mFixedRowSizeSecondary;
        }
        if (mRowSizeSecondary == null) {
            return 0;
        }
        return mRowSizeSecondary[rowIndex];
    }

    private int getRowStartSecondary(int rowIndex) {
        int start = 0;
        for (int i = 0; i < rowIndex; i++) {
            start += getRowSizeSecondary(i) + mMarginSecondary;
        }
        return start;
    }

    private int getSizeSecondary() {
        return getRowStartSecondary(mNumRows - 1) + getRowSizeSecondary(mNumRows - 1);
    }

    private void measureScrapChild(int position, int widthSpec, int heightSpec,
            int[] measuredDimension) {
        View view = mRecycler.getViewForPosition(position);
        if (view != null) {
            LayoutParams p = (LayoutParams) view.getLayoutParams();
            int childWidthSpec = ViewGroup.getChildMeasureSpec(widthSpec,
                    getPaddingLeft() + getPaddingRight(), p.width);
            int childHeightSpec = ViewGroup.getChildMeasureSpec(heightSpec,
                    getPaddingTop() + getPaddingBottom(), p.height);
            view.measure(childWidthSpec, childHeightSpec);
            measuredDimension[0] = view.getMeasuredWidth();
            measuredDimension[1] = view.getMeasuredHeight();
            mRecycler.recycleView(view);
        }
    }

    private boolean processRowSizeSecondary(boolean measure) {
        if (mFixedRowSizeSecondary != 0) {
            return false;
        }

        List<Integer>[] rows = mGrid == null ? null :
            mGrid.getItemPositionsInRows(mFirstVisiblePos, mLastVisiblePos);
        boolean changed = false;
        int scrapChildWidth = -1;
        int scrapChildHeight = -1;

        for (int rowIndex = 0; rowIndex < mNumRows; rowIndex++) {
            final int rowItemCount = rows == null ? 0 : rows[rowIndex].size();
            if (DEBUG) Log.v(getTag(), "processRowSizeSecondary row " + rowIndex +
                    " rowItemCount " + rowItemCount);

            int rowSize = -1;
            for (int i = 0; i < rowItemCount; i++) {
                final View view = findViewByPosition(rows[rowIndex].get(i));
                if (view == null) {
                    continue;
                }
                if (measure && view.isLayoutRequested()) {
                    measureChild(view);
                }
                final int secondarySize = mOrientation == HORIZONTAL ?
                        view.getMeasuredHeight() : view.getMeasuredWidth();
                if (secondarySize > rowSize) {
                    rowSize = secondarySize;
                }
            }

            if (measure && rowSize < 0 && mState.getItemCount() > 0) {
                if (scrapChildWidth < 0 && scrapChildHeight < 0) {
                    measureScrapChild(mFocusPosition == NO_POSITION ? 0 : mFocusPosition,
                            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                            mMeasuredDimension);
                    scrapChildWidth = mMeasuredDimension[0];
                    scrapChildHeight = mMeasuredDimension[1];
                    if (DEBUG) Log.v(TAG, "measured scrap child: " + scrapChildWidth +
                            " " + scrapChildHeight);
                }
                rowSize = mOrientation == HORIZONTAL ? scrapChildHeight : scrapChildWidth;
            }

            if (rowSize < 0) {
                rowSize = 0;
            }

            if (DEBUG) Log.v(getTag(), "row " + rowIndex + " rowItemCount " + rowItemCount +
                    " rowSize " + rowSize);

            if (mRowSizeSecondary[rowIndex] != rowSize) {
                if (DEBUG) Log.v(getTag(), "row size secondary changed: " + mRowSizeSecondary[rowIndex] +
                        ", " + rowSize);

                mRowSizeSecondary[rowIndex] = rowSize;
                changed = true;
            }
        }

        return changed;
    }

    /**
     * Checks if we need to update row secondary sizes.
     */
    private void updateRowSecondarySizeRefresh() {
        mRowSecondarySizeRefresh = processRowSizeSecondary(false);
        if (mRowSecondarySizeRefresh) {
            if (DEBUG) Log.v(getTag(), "mRowSecondarySizeRefresh now set");
            forceRequestLayout();
        }
    }

    private void forceRequestLayout() {
        if (DEBUG) Log.v(getTag(), "forceRequestLayout");
        // RecyclerView prevents us from requesting layout in many cases
        // (during layout, during scroll, etc.)
        // For secondary row size wrap_content support we currently need a
        // second layout pass to update the measured size after having measured
        // and added child views in layoutChildren.
        // Force the second layout by posting a delayed runnable.
        // TODO: investigate allowing a second layout pass,
        // or move child add/measure logic to the measure phase.
        ViewCompat.postOnAnimation(mBaseGridView, mRequestLayoutRunnable);
    }

    private final Runnable mRequestLayoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (DEBUG) Log.v(getTag(), "request Layout from runnable");
            requestLayout();
        }
     };

    @Override
    public void onMeasure(Recycler recycler, State state, int widthSpec, int heightSpec) {
        saveContext(recycler, state);

        int sizePrimary, sizeSecondary, modeSecondary, paddingSecondary;
        int measuredSizeSecondary;
        if (mOrientation == HORIZONTAL) {
            sizePrimary = MeasureSpec.getSize(widthSpec);
            sizeSecondary = MeasureSpec.getSize(heightSpec);
            modeSecondary = MeasureSpec.getMode(heightSpec);
            paddingSecondary = getPaddingTop() + getPaddingBottom();
        } else {
            sizeSecondary = MeasureSpec.getSize(widthSpec);
            sizePrimary = MeasureSpec.getSize(heightSpec);
            modeSecondary = MeasureSpec.getMode(widthSpec);
            paddingSecondary = getPaddingLeft() + getPaddingRight();
        }
        if (DEBUG) Log.v(getTag(), "onMeasure widthSpec " + Integer.toHexString(widthSpec) +
                " heightSpec " + Integer.toHexString(heightSpec) +
                " modeSecondary " + Integer.toHexString(modeSecondary) +
                " sizeSecondary " + sizeSecondary + " " + this);

        mMaxSizeSecondary = sizeSecondary;

        if (mRowSizeSecondaryRequested == ViewGroup.LayoutParams.WRAP_CONTENT) {
            mNumRows = mNumRowsRequested == 0 ? 1 : mNumRowsRequested;
            mFixedRowSizeSecondary = 0;

            if (mRowSizeSecondary == null || mRowSizeSecondary.length != mNumRows) {
                mRowSizeSecondary = new int[mNumRows];
            }

            // Measure all current children and update cached row heights
            processRowSizeSecondary(true);

            switch (modeSecondary) {
            case MeasureSpec.UNSPECIFIED:
                measuredSizeSecondary = getSizeSecondary() + paddingSecondary;
                break;
            case MeasureSpec.AT_MOST:
                measuredSizeSecondary = Math.min(getSizeSecondary() + paddingSecondary,
                        mMaxSizeSecondary);
                break;
            case MeasureSpec.EXACTLY:
                measuredSizeSecondary = mMaxSizeSecondary;
                break;
            default:
                throw new IllegalStateException("wrong spec");
            }

        } else {
            switch (modeSecondary) {
            case MeasureSpec.UNSPECIFIED:
                if (mRowSizeSecondaryRequested == 0) {
                    if (mOrientation == HORIZONTAL) {
                        throw new IllegalStateException("Must specify rowHeight or view height");
                    } else {
                        throw new IllegalStateException("Must specify columnWidth or view width");
                    }
                }
                mFixedRowSizeSecondary = mRowSizeSecondaryRequested;
                mNumRows = mNumRowsRequested == 0 ? 1 : mNumRowsRequested;
                measuredSizeSecondary = mFixedRowSizeSecondary * mNumRows + mMarginSecondary
                    * (mNumRows - 1) + paddingSecondary;
                break;
            case MeasureSpec.AT_MOST:
            case MeasureSpec.EXACTLY:
                if (mNumRowsRequested == 0 && mRowSizeSecondaryRequested == 0) {
                    mNumRows = 1;
                    mFixedRowSizeSecondary = sizeSecondary - paddingSecondary;
                } else if (mNumRowsRequested == 0) {
                    mFixedRowSizeSecondary = mRowSizeSecondaryRequested;
                    mNumRows = (sizeSecondary + mMarginSecondary)
                        / (mRowSizeSecondaryRequested + mMarginSecondary);
                } else if (mRowSizeSecondaryRequested == 0) {
                    mNumRows = mNumRowsRequested;
                    mFixedRowSizeSecondary = (sizeSecondary - paddingSecondary - mMarginSecondary
                            * (mNumRows - 1)) / mNumRows;
                } else {
                    mNumRows = mNumRowsRequested;
                    mFixedRowSizeSecondary = mRowSizeSecondaryRequested;
                }
                measuredSizeSecondary = sizeSecondary;
                if (modeSecondary == MeasureSpec.AT_MOST) {
                    int childrenSize = mFixedRowSizeSecondary * mNumRows + mMarginSecondary
                        * (mNumRows - 1) + paddingSecondary;
                    if (childrenSize < measuredSizeSecondary) {
                        measuredSizeSecondary = childrenSize;
                    }
                }
                break;
            default:
                throw new IllegalStateException("wrong spec");
            }
        }
        if (mOrientation == HORIZONTAL) {
            setMeasuredDimension(sizePrimary, measuredSizeSecondary);
        } else {
            setMeasuredDimension(measuredSizeSecondary, sizePrimary);
        }
        if (DEBUG) {
            Log.v(getTag(), "onMeasure sizePrimary " + sizePrimary +
                    " measuredSizeSecondary " + measuredSizeSecondary +
                    " mFixedRowSizeSecondary " + mFixedRowSizeSecondary +
                    " mNumRows " + mNumRows);
        }

        leaveContext();
    }

    private void measureChild(View child) {
        final ViewGroup.LayoutParams lp = child.getLayoutParams();
        final int secondarySpec = (mRowSizeSecondaryRequested == ViewGroup.LayoutParams.WRAP_CONTENT) ?
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED) :
                MeasureSpec.makeMeasureSpec(mFixedRowSizeSecondary, MeasureSpec.EXACTLY);
        int widthSpec, heightSpec;

        if (mOrientation == HORIZONTAL) {
            widthSpec = ViewGroup.getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    0, lp.width);
            heightSpec = ViewGroup.getChildMeasureSpec(secondarySpec, 0, lp.height);
        } else {
            heightSpec = ViewGroup.getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    0, lp.height);
            widthSpec = ViewGroup.getChildMeasureSpec(secondarySpec, 0, lp.width);
        }

        child.measure(widthSpec, heightSpec);

        if (DEBUG) Log.v(getTag(), "measureChild secondarySpec " + Integer.toHexString(secondarySpec) +
                " widthSpec " + Integer.toHexString(widthSpec) +
                " heightSpec " + Integer.toHexString(heightSpec) +
                " measuredWidth " + child.getMeasuredWidth() +
                " measuredHeight " + child.getMeasuredHeight());
        if (DEBUG) Log.v(getTag(), "child lp width " + lp.width + " height " + lp.height);
    }

    private StaggeredGrid.Provider mGridProvider = new StaggeredGrid.Provider() {

        @Override
        public int getCount() {
            return mState.getItemCount();
        }

        @Override
        public void createItem(int index, int rowIndex, boolean append) {
            View v = getViewForPosition(index);
            if (mFirstVisiblePos >= 0) {
                // when StaggeredGrid append or prepend item, we must guarantee
                // that sibling item has created views already.
                if (append && index != mLastVisiblePos + 1) {
                    throw new RuntimeException();
                } else if (!append && index != mFirstVisiblePos - 1) {
                    throw new RuntimeException();
                }
            }

            // See recyclerView docs:  we don't need re-add scraped view if it was removed.
            if (!((RecyclerView.LayoutParams) v.getLayoutParams()).isItemRemoved()) {
                if (append) {
                    addView(v);
                } else {
                    addView(v, 0);
                }
                if (mChildVisibility != -1) {
                    v.setVisibility(mChildVisibility);
                }

                // View is added first or it won't be found by dispatchChildSelected.
                if (mInLayout && index == mFocusPosition) {
                    dispatchChildSelected();
                }

                measureChild(v);
            }

            int length = mOrientation == HORIZONTAL ? v.getMeasuredWidth() : v.getMeasuredHeight();
            int start, end;
            final boolean rowIsEmpty = mRows[rowIndex].high == mRows[rowIndex].low;
            if (append) {
                if (!rowIsEmpty) {
                    // if there are existing item in the row,  add margin between
                    start = mRows[rowIndex].high + mMarginPrimary;
                } else {
                    if (mLastVisiblePos >= 0) {
                        int lastRow = mGrid.getLocation(mLastVisiblePos).row;
                        // if the last visible item is not last row,  align to beginning,
                        // otherwise start a new column after.
                        if (lastRow < mNumRows - 1) {
                            start = mRows[lastRow].low;
                        } else {
                            start = mRows[lastRow].high + mMarginPrimary;
                        }
                    } else {
                        start = 0;
                    }
                    mRows[rowIndex].low = start;
                }
                end = start + length;
                mRows[rowIndex].high = end;
            } else {
                if (!rowIsEmpty) {
                    // if there are existing item in the row,  add margin between
                    end = mRows[rowIndex].low - mMarginPrimary;
                    start = end - length;
                } else {
                    if (mFirstVisiblePos >= 0) {
                        int firstRow = mGrid.getLocation(mFirstVisiblePos).row;
                        // if the first visible item is not first row,  align to beginning,
                        // otherwise start a new column before.
                        if (firstRow > 0) {
                            start = mRows[firstRow].low;
                            end = start + length;
                        } else {
                            end = mRows[firstRow].low - mMarginPrimary;
                            start = end - length;
                        }
                    } else {
                        start = 0;
                        end = length;
                    }
                    mRows[rowIndex].high = end;
                }
                mRows[rowIndex].low = start;
            }
            if (mFirstVisiblePos < 0) {
                mFirstVisiblePos = mLastVisiblePos = index;
            } else {
                if (append) {
                    mLastVisiblePos++;
                } else {
                    mFirstVisiblePos--;
                }
            }
            if (DEBUG) Log.v(getTag(), "start " + start + " end " + end);
            int startSecondary = getRowStartSecondary(rowIndex) - mScrollOffsetSecondary;
            mChildrenStates.loadView(v, index);
            layoutChild(rowIndex, v, start - mScrollOffsetPrimary, end - mScrollOffsetPrimary,
                    startSecondary);
            if (DEBUG) {
                Log.d(getTag(), "addView " + index + " " + v);
            }
            if (index == mFirstVisiblePos) {
                updateScrollMin();
            }
            if (index == mLastVisiblePos) {
                updateScrollMax();
            }
        }
    };

    private void layoutChild(int rowIndex, View v, int start, int end, int startSecondary) {
        int sizeSecondary = mOrientation == HORIZONTAL ? v.getMeasuredHeight()
                : v.getMeasuredWidth();
        if (mFixedRowSizeSecondary > 0) {
            sizeSecondary = Math.min(sizeSecondary, mFixedRowSizeSecondary);
        }
        final int verticalGravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
        final int horizontalGravity = mGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        if (mOrientation == HORIZONTAL && verticalGravity == Gravity.TOP
                || mOrientation == VERTICAL && horizontalGravity == Gravity.LEFT) {
            // do nothing
        } else if (mOrientation == HORIZONTAL && verticalGravity == Gravity.BOTTOM
                || mOrientation == VERTICAL && horizontalGravity == Gravity.RIGHT) {
            startSecondary += getRowSizeSecondary(rowIndex) - sizeSecondary;
        } else if (mOrientation == HORIZONTAL && verticalGravity == Gravity.CENTER_VERTICAL
                || mOrientation == VERTICAL && horizontalGravity == Gravity.CENTER_HORIZONTAL) {
            startSecondary += (getRowSizeSecondary(rowIndex) - sizeSecondary) / 2;
        }
        int left, top, right, bottom;
        if (mOrientation == HORIZONTAL) {
            left = start;
            top = startSecondary;
            right = end;
            bottom = startSecondary + sizeSecondary;
        } else {
            top = start;
            left = startSecondary;
            bottom = end;
            right = startSecondary + sizeSecondary;
        }
        v.layout(left, top, right, bottom);
        updateChildOpticalInsets(v, left, top, right, bottom);
        updateChildAlignments(v);
    }

    private void updateChildOpticalInsets(View v, int left, int top, int right, int bottom) {
        LayoutParams p = (LayoutParams) v.getLayoutParams();
        p.setOpticalInsets(left - v.getLeft(), top - v.getTop(),
                v.getRight() - right, v.getBottom() - bottom);
    }

    private void updateChildAlignments(View v) {
        LayoutParams p = (LayoutParams) v.getLayoutParams();
        p.setAlignX(mItemAlignment.horizontal.getAlignmentPosition(v));
        p.setAlignY(mItemAlignment.vertical.getAlignmentPosition(v));
    }

    private void updateChildAlignments() {
        for (int i = 0, c = getChildCount(); i < c; i++) {
            updateChildAlignments(getChildAt(i));
        }
    }

    private boolean needsAppendVisibleItem() {
        if (mLastVisiblePos < mFocusPosition) {
            return true;
        }
        int right = mScrollOffsetPrimary + mSizePrimary;
        for (int i = 0; i < mNumRows; i++) {
            if (mRows[i].low == mRows[i].high) {
                if (mRows[i].high < right) {
                    return true;
                }
            } else if (mRows[i].high < right - mMarginPrimary) {
                return true;
            }
        }
        return false;
    }

    private boolean needsPrependVisibleItem() {
        if (mFirstVisiblePos > mFocusPosition) {
            return true;
        }
        for (int i = 0; i < mNumRows; i++) {
            if (mRows[i].low == mRows[i].high) {
                if (mRows[i].low > mScrollOffsetPrimary) {
                    return true;
                }
            } else if (mRows[i].low - mMarginPrimary > mScrollOffsetPrimary) {
                return true;
            }
        }
        return false;
    }

    // Append one column if possible and return true if reach end.
    private boolean appendOneVisibleItem() {
        while (true) {
            if (mLastVisiblePos != NO_POSITION && mLastVisiblePos < mState.getItemCount() -1 &&
                    mLastVisiblePos < mGrid.getLastIndex()) {
                // append invisible view of saved location till last row
                final int index = mLastVisiblePos + 1;
                final int row = mGrid.getLocation(index).row;
                mGridProvider.createItem(index, row, true);
                if (row == mNumRows - 1) {
                    return false;
                }
            } else if ((mLastVisiblePos == NO_POSITION && mState.getItemCount() > 0) ||
                    (mLastVisiblePos != NO_POSITION &&
                            mLastVisiblePos < mState.getItemCount() - 1)) {
                mGrid.appendItems(mScrollOffsetPrimary + mSizePrimary);
                return false;
            } else {
                return true;
            }
        }
    }

    private void appendVisibleItems() {
        while (needsAppendVisibleItem()) {
            if (appendOneVisibleItem()) {
                break;
            }
        }
    }

    // Prepend one column if possible and return true if reach end.
    private boolean prependOneVisibleItem() {
        while (true) {
            if (mFirstVisiblePos > 0) {
                if (mFirstVisiblePos > mGrid.getFirstIndex()) {
                    // prepend invisible view of saved location till first row
                    final int index = mFirstVisiblePos - 1;
                    final int row = mGrid.getLocation(index).row;
                    mGridProvider.createItem(index, row, false);
                    if (row == 0) {
                        return false;
                    }
                } else {
                    mGrid.prependItems(mScrollOffsetPrimary);
                    return false;
                }
            } else {
                return true;
            }
        }
    }

    private void prependVisibleItems() {
        while (needsPrependVisibleItem()) {
            if (prependOneVisibleItem()) {
                break;
            }
        }
    }

    private void removeChildAt(int position) {
        View v = findViewByPosition(position);
        if (v != null) {
            if (DEBUG) {
                Log.d(getTag(), "removeAndRecycleViewAt " + position + " " + v);
            }
            mChildrenStates.saveOffscreenView(v, position);
            removeAndRecycleView(v, mRecycler);
        }
    }

    private void removeInvisibleViewsAtEnd() {
        if (!mPruneChild) {
            return;
        }
        boolean update = false;
        while(mLastVisiblePos > mFirstVisiblePos && mLastVisiblePos > mFocusPosition) {
            View view = findViewByPosition(mLastVisiblePos);
            if (getViewMin(view) > mSizePrimary) {
                removeChildAt(mLastVisiblePos);
                mLastVisiblePos--;
                update = true;
            } else {
                break;
            }
        }
        if (update) {
            updateRowsMinMax();
        }
    }

    private void removeInvisibleViewsAtFront() {
        if (!mPruneChild) {
            return;
        }
        boolean update = false;
        while(mLastVisiblePos > mFirstVisiblePos && mFirstVisiblePos < mFocusPosition) {
            View view = findViewByPosition(mFirstVisiblePos);
            if (getViewMax(view) < 0) {
                removeChildAt(mFirstVisiblePos);
                mFirstVisiblePos++;
                update = true;
            } else {
                break;
            }
        }
        if (update) {
            updateRowsMinMax();
        }
    }

    private void updateRowsMinMax() {
        if (mFirstVisiblePos < 0) {
            return;
        }
        for (int i = 0; i < mNumRows; i++) {
            mRows[i].low = Integer.MAX_VALUE;
            mRows[i].high = Integer.MIN_VALUE;
        }
        for (int i = mFirstVisiblePos; i <= mLastVisiblePos; i++) {
            View view = findViewByPosition(i);
            int row = mGrid.getLocation(i).row;
            int low = getViewMin(view) + mScrollOffsetPrimary;
            if (low < mRows[row].low) {
                mRows[row].low = low;
            }
            int high = getViewMax(view) + mScrollOffsetPrimary;
            if (high > mRows[row].high) {
                mRows[row].high = high;
            }
        }
    }

    // Fast layout when there is no structure change, adapter change, etc.
    protected void fastRelayout(boolean scrollToFocus) {
        initScrollController();

        List<Integer>[] rows = mGrid.getItemPositionsInRows(mFirstVisiblePos, mLastVisiblePos);

        // relayout and repositioning views on each row
        for (int i = 0; i < mNumRows; i++) {
            List<Integer> row = rows[i];
            final int startSecondary = getRowStartSecondary(i) - mScrollOffsetSecondary;
            for (int j = 0, size = row.size(); j < size; j++) {
                final int position = row.get(j);
                View view = findViewByPosition(position);
                int primaryDelta, end;

                int start = getViewMin(view);
                int oldPrimarySize = (mOrientation == HORIZONTAL) ?
                        view.getMeasuredWidth() :
                        view.getMeasuredHeight();

                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                if (lp.viewNeedsUpdate()) {
                    int index = mBaseGridView.indexOfChild(view);
                    detachAndScrapView(view, mRecycler);
                    view = getViewForPosition(position);
                    addView(view, index);
                }

                if (view.isLayoutRequested()) {
                    measureChild(view);
                }

                if (mOrientation == HORIZONTAL) {
                    end = start + view.getMeasuredWidth();
                    primaryDelta = view.getMeasuredWidth() - oldPrimarySize;
                    if (primaryDelta != 0) {
                        for (int k = j + 1; k < size; k++) {
                            findViewByPosition(row.get(k)).offsetLeftAndRight(primaryDelta);
                        }
                    }
                } else {
                    end = start + view.getMeasuredHeight();
                    primaryDelta = view.getMeasuredHeight() - oldPrimarySize;
                    if (primaryDelta != 0) {
                        for (int k = j + 1; k < size; k++) {
                            findViewByPosition(row.get(k)).offsetTopAndBottom(primaryDelta);
                        }
                    }
                }
                layoutChild(i, view, start, end, startSecondary);
            }
        }

        updateRowsMinMax();
        appendVisibleItems();
        prependVisibleItems();

        updateRowsMinMax();
        updateScrollMin();
        updateScrollMax();
        updateScrollSecondAxis();

        if (scrollToFocus) {
            View focusView = findViewByPosition(mFocusPosition == NO_POSITION ? 0 : mFocusPosition);
            scrollToView(focusView, false);
        }
    }

    public void removeAndRecycleAllViews(RecyclerView.Recycler recycler) {
        if (DEBUG) Log.v(TAG, "removeAndRecycleAllViews " + getChildCount());
        for (int i = getChildCount() - 1; i >= 0; i--) {
            removeAndRecycleViewAt(i, recycler);
        }
    }

    // Lays out items based on the current scroll position
    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (DEBUG) {
            Log.v(getTag(), "layoutChildren start numRows " + mNumRows + " mScrollOffsetSecondary "
                    + mScrollOffsetSecondary + " mScrollOffsetPrimary " + mScrollOffsetPrimary
                    + " inPreLayout " + state.isPreLayout()
                    + " didStructureChange " + state.didStructureChange()
                    + " mForceFullLayout " + mForceFullLayout);
            Log.v(getTag(), "width " + getWidth() + " height " + getHeight());
        }

        if (mNumRows == 0) {
            // haven't done measure yet
            return;
        }
        final int itemCount = state.getItemCount();
        if (itemCount < 0) {
            return;
        }

        if (!mLayoutEnabled) {
            discardLayoutInfo();
            removeAndRecycleAllViews(recycler);
            return;
        }
        mInLayout = true;

        final boolean scrollToFocus = !isSmoothScrolling()
                && mFocusScrollStrategy == BaseGridView.FOCUS_SCROLL_ALIGNED;
        if (mFocusPosition != NO_POSITION && mFocusPositionOffset != Integer.MIN_VALUE) {
            mFocusPosition = mFocusPosition + mFocusPositionOffset;
            mFocusPositionOffset = 0;
        }
        saveContext(recycler, state);
        // Track the old focus view so we can adjust our system scroll position
        // so that any scroll animations happening now will remain valid.
        // We must use same delta in Pre Layout (if prelayout exists) and second layout.
        // So we cache the deltas in PreLayout and use it in second layout.
        int delta = 0, deltaSecondary = 0;
        if (mFocusPosition != NO_POSITION && scrollToFocus) {
            // FIXME: we should get the remaining scroll animation offset from RecyclerView
            View focusView = findViewByPosition(mFocusPosition);
            if (focusView != null) {
                delta = mWindowAlignment.mainAxis().getSystemScrollPos(mScrollOffsetPrimary
                        + getViewCenter(focusView), false, false) - mScrollOffsetPrimary;
                deltaSecondary = mWindowAlignment.secondAxis().getSystemScrollPos(
                        mScrollOffsetSecondary + getViewCenterSecondary(focusView),
                        false, false) - mScrollOffsetSecondary;
            }
        }

        final boolean hasDoneFirstLayout = hasDoneFirstLayout();
        int savedFocusPos = mFocusPosition;
        boolean fastRelayout = false;
        if (!mState.didStructureChange() && !mForceFullLayout && hasDoneFirstLayout) {
            fastRelayout = true;
            fastRelayout(scrollToFocus);
        } else {
            boolean hadFocus = mBaseGridView.hasFocus();

            mFocusPosition = init(mFocusPosition);
            if (mFocusPosition != savedFocusPos) {
                if (DEBUG) Log.v(getTag(), "savedFocusPos " + savedFocusPos +
                        " mFocusPosition " + mFocusPosition);
            }

            mWindowAlignment.mainAxis().invalidateScrollMin();
            mWindowAlignment.mainAxis().invalidateScrollMax();
            // depending on result of init(), either recreating everything
            // or try to reuse the row start positions near mFocusPosition
            if (mGrid.getSize() == 0) {
                // this is a fresh creating all items, starting from
                // mFocusPosition with a estimated row index.
                mGrid.setStart(mFocusPosition, StaggeredGrid.START_DEFAULT);

                // Can't track the old focus view
                delta = deltaSecondary = 0;

            } else {
                // mGrid remembers Locations for the column that
                // contains mFocusePosition and also mRows remembers start
                // positions of each row.
                // Manually re-create child views for that column
                int firstIndex = mGrid.getFirstIndex();
                int lastIndex = mGrid.getLastIndex();
                for (int i = firstIndex; i <= lastIndex; i++) {
                    mGridProvider.createItem(i, mGrid.getLocation(i).row, true);
                }
            }

            // add visible views at end until reach the end of window
            appendVisibleItems();
            // add visible views at front until reach the start of window
            prependVisibleItems();
            // multiple rounds: scrollToView of first round may drag first/last child into
            // "visible window" and we update scrollMin/scrollMax then run second scrollToView
            int oldFirstVisible;
            int oldLastVisible;
            do {
                updateScrollMin();
                updateScrollMax();
                oldFirstVisible = mFirstVisiblePos;
                oldLastVisible = mLastVisiblePos;
                View focusView = findViewByPosition(mFocusPosition);
                // we need force to initialize the child view's position
                scrollToView(focusView, false);
                if (focusView != null && hadFocus) {
                    focusView.requestFocus();
                }
                appendVisibleItems();
                prependVisibleItems();
                removeInvisibleViewsAtFront();
                removeInvisibleViewsAtEnd();
            } while (mFirstVisiblePos != oldFirstVisible || mLastVisiblePos != oldLastVisible);
        }
        mForceFullLayout = false;

        if (scrollToFocus) {
            scrollDirectionPrimary(-delta);
            scrollDirectionSecondary(-deltaSecondary);
        }
        appendVisibleItems();
        prependVisibleItems();
        removeInvisibleViewsAtFront();
        removeInvisibleViewsAtEnd();

        if (DEBUG) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            mGrid.debugPrint(pw);
            Log.d(getTag(), sw.toString());
        }

        if (mRowSecondarySizeRefresh) {
            mRowSecondarySizeRefresh = false;
        } else {
            updateRowSecondarySizeRefresh();
        }

        if (fastRelayout && mFocusPosition != savedFocusPos) {
            dispatchChildSelected();
        }
        mInLayout = false;
        leaveContext();
        if (DEBUG) Log.v(getTag(), "layoutChildren end");
    }

    private void offsetChildrenSecondary(int increment) {
        final int childCount = getChildCount();
        if (mOrientation == HORIZONTAL) {
            for (int i = 0; i < childCount; i++) {
                getChildAt(i).offsetTopAndBottom(increment);
            }
        } else {
            for (int i = 0; i < childCount; i++) {
                getChildAt(i).offsetLeftAndRight(increment);
            }
        }
    }

    private void offsetChildrenPrimary(int increment) {
        final int childCount = getChildCount();
        if (mOrientation == VERTICAL) {
            for (int i = 0; i < childCount; i++) {
                getChildAt(i).offsetTopAndBottom(increment);
            }
        } else {
            for (int i = 0; i < childCount; i++) {
                getChildAt(i).offsetLeftAndRight(increment);
            }
        }
    }

    @Override
    public int scrollHorizontallyBy(int dx, Recycler recycler, RecyclerView.State state) {
        if (DEBUG) Log.v(getTag(), "scrollHorizontallyBy " + dx);
        if (!mLayoutEnabled || !hasDoneFirstLayout()) {
            return 0;
        }
        saveContext(recycler, state);
        int result;
        if (mOrientation == HORIZONTAL) {
            result = scrollDirectionPrimary(dx);
        } else {
            result = scrollDirectionSecondary(dx);
        }
        leaveContext();
        return result;
    }

    @Override
    public int scrollVerticallyBy(int dy, Recycler recycler, RecyclerView.State state) {
        if (DEBUG) Log.v(getTag(), "scrollVerticallyBy " + dy);
        if (!mLayoutEnabled || !hasDoneFirstLayout()) {
            return 0;
        }
        saveContext(recycler, state);
        int result;
        if (mOrientation == VERTICAL) {
            result = scrollDirectionPrimary(dy);
        } else {
            result = scrollDirectionSecondary(dy);
        }
        leaveContext();
        return result;
    }

    // scroll in main direction may add/prune views
    private int scrollDirectionPrimary(int da) {
        if (da > 0) {
            if (!mWindowAlignment.mainAxis().isMaxUnknown()) {
                int maxScroll = mWindowAlignment.mainAxis().getMaxScroll();
                if (mScrollOffsetPrimary + da > maxScroll) {
                    da = maxScroll - mScrollOffsetPrimary;
                }
            }
        } else if (da < 0) {
            if (!mWindowAlignment.mainAxis().isMinUnknown()) {
                int minScroll = mWindowAlignment.mainAxis().getMinScroll();
                if (mScrollOffsetPrimary + da < minScroll) {
                    da = minScroll - mScrollOffsetPrimary;
                }
            }
        }
        if (da == 0) {
            return 0;
        }
        offsetChildrenPrimary(-da);
        mScrollOffsetPrimary += da;
        if (mInLayout) {
            return da;
        }

        int childCount = getChildCount();
        boolean updated;

        if (da > 0) {
            appendVisibleItems();
        } else if (da < 0) {
            prependVisibleItems();
        }
        updated = getChildCount() > childCount;
        childCount = getChildCount();

        if (da > 0) {
            removeInvisibleViewsAtFront();
        } else if (da < 0) {
            removeInvisibleViewsAtEnd();
        }
        updated |= getChildCount() < childCount;

        if (updated) {
            updateRowSecondarySizeRefresh();
        }

        mBaseGridView.invalidate();
        return da;
    }

    // scroll in second direction will not add/prune views
    private int scrollDirectionSecondary(int dy) {
        if (dy == 0) {
            return 0;
        }
        offsetChildrenSecondary(-dy);
        mScrollOffsetSecondary += dy;
        mBaseGridView.invalidate();
        return dy;
    }

    private void updateScrollMax() {
        if (mLastVisiblePos < 0) {
            return;
        }
        final boolean lastAvailable = mLastVisiblePos == mState.getItemCount() - 1;
        final boolean maxUnknown = mWindowAlignment.mainAxis().isMaxUnknown();
        if (!lastAvailable && maxUnknown) {
            return;
        }
        int maxEdge = Integer.MIN_VALUE;
        int rowIndex = -1;
        for (int i = 0; i < mRows.length; i++) {
            if (mRows[i].high > maxEdge) {
                maxEdge = mRows[i].high;
                rowIndex = i;
            }
        }
        int maxScroll = Integer.MAX_VALUE;
        for (int i = mLastVisiblePos; i >= mFirstVisiblePos; i--) {
            StaggeredGrid.Location location = mGrid.getLocation(i);
            if (location != null && location.row == rowIndex) {
                int savedMaxEdge = mWindowAlignment.mainAxis().getMaxEdge();
                mWindowAlignment.mainAxis().setMaxEdge(maxEdge);
                maxScroll = getPrimarySystemScrollPosition(findViewByPosition(i));
                mWindowAlignment.mainAxis().setMaxEdge(savedMaxEdge);
                break;
            }
        }
        if (lastAvailable) {
            mWindowAlignment.mainAxis().setMaxEdge(maxEdge);
            mWindowAlignment.mainAxis().setMaxScroll(maxScroll);
            if (DEBUG) Log.v(getTag(), "updating scroll maxEdge to " + maxEdge +
                    " scrollMax to " + maxScroll);
        } else {
            // the maxScroll for currently last visible item is larger,
            // so we must invalidate the max scroll value.
            if (maxScroll > mWindowAlignment.mainAxis().getMaxScroll()) {
                mWindowAlignment.mainAxis().invalidateScrollMax();
                if (DEBUG) Log.v(getTag(), "Invalidate scrollMax since it should be "
                        + "greater than " + maxScroll);
            }
        }
    }

    private void updateScrollMin() {
        if (mFirstVisiblePos < 0) {
            return;
        }
        final boolean firstAvailable = mFirstVisiblePos == 0;
        final boolean minUnknown = mWindowAlignment.mainAxis().isMinUnknown();
        if (!firstAvailable && minUnknown) {
            return;
        }
        int minEdge = Integer.MAX_VALUE;
        int rowIndex = -1;
        for (int i = 0; i < mRows.length; i++) {
            if (mRows[i].low < minEdge) {
                minEdge = mRows[i].low;
                rowIndex = i;
            }
        }
        int minScroll = Integer.MIN_VALUE;
        for (int i = mFirstVisiblePos; i <= mLastVisiblePos; i++) {
            StaggeredGrid.Location location = mGrid.getLocation(i);
            if (location != null && location.row == rowIndex) {
                int savedMinEdge = mWindowAlignment.mainAxis().getMinEdge();
                mWindowAlignment.mainAxis().setMinEdge(minEdge);
                minScroll = getPrimarySystemScrollPosition(findViewByPosition(i));
                mWindowAlignment.mainAxis().setMinEdge(savedMinEdge);
                break;
            }
        }
        if (firstAvailable) {
            mWindowAlignment.mainAxis().setMinEdge(minEdge);
            mWindowAlignment.mainAxis().setMinScroll(minScroll);
            if (DEBUG) Log.v(getTag(), "updating scroll minEdge to " + minEdge +
                    " scrollMin to " + minScroll);
        } else {
            // the minScroll for currently first visible item is smaller,
            // so we must invalidate the min scroll value.
            if (minScroll < mWindowAlignment.mainAxis().getMinScroll()) {
                mWindowAlignment.mainAxis().invalidateScrollMin();
                if (DEBUG) Log.v(getTag(), "Invalidate scrollMin, since it should be "
                        + "less than " + minScroll);
            }
        }
    }

    private void updateScrollSecondAxis() {
        mWindowAlignment.secondAxis().setMinEdge(0);
        mWindowAlignment.secondAxis().setMaxEdge(getSizeSecondary());
    }

    private void initScrollController() {
        // mScrollOffsetPrimary and mScrollOffsetSecondary includes the padding.
        // e.g. when topPadding is 16 for horizontal grid view,  the initial
        // mScrollOffsetSecondary is -16.  fastLayout() put views based on offsets(not padding),
        // when padding changes to 20,  we also need update mScrollOffsetSecondary to -20 before
        // fastLayout() is performed
        int paddingPrimaryDiff, paddingSecondaryDiff;
        if (mOrientation == HORIZONTAL) {
            paddingPrimaryDiff = getPaddingLeft() - mWindowAlignment.horizontal.getPaddingLow();
            paddingSecondaryDiff = getPaddingTop() - mWindowAlignment.vertical.getPaddingLow();
        } else {
            paddingPrimaryDiff = getPaddingTop() - mWindowAlignment.vertical.getPaddingLow();
            paddingSecondaryDiff = getPaddingLeft() - mWindowAlignment.horizontal.getPaddingLow();
        }
        mScrollOffsetPrimary -= paddingPrimaryDiff;
        mScrollOffsetSecondary -= paddingSecondaryDiff;

        if (mOrientation == HORIZONTAL) {
            mWindowAlignment.horizontal.setSize((int)(getWidth() * mPrimaryOverReach + 0.5f));
            mWindowAlignment.vertical.setSize(getHeight());
        } else {
            mWindowAlignment.horizontal.setSize(getWidth());
            mWindowAlignment.vertical.setSize((int) (getHeight() * mPrimaryOverReach + 0.5f));
        }
        mWindowAlignment.horizontal.setPadding(getPaddingLeft(), getPaddingRight());
        mWindowAlignment.vertical.setPadding(getPaddingTop(), getPaddingBottom());
        mSizePrimary = mWindowAlignment.mainAxis().getSize();

        if (DEBUG) {
            Log.v(getTag(), "initScrollController mSizePrimary " + mSizePrimary
                    + " mWindowAlignment " + mWindowAlignment);
        }
    }

    public void setSelection(RecyclerView parent, int position) {
        setSelection(parent, position, false);
    }

    public void setSelectionSmooth(RecyclerView parent, int position) {
        setSelection(parent, position, true);
    }

    public int getSelection() {
        return mFocusPosition;
    }

    public void setSelection(RecyclerView parent, int position, boolean smooth) {
        if (mFocusPosition == position) {
            return;
        }
        View view = findViewByPosition(position);
        if (view != null) {
            mInSelection = true;
            scrollToView(view, smooth);
            mInSelection = false;
        } else {
            mFocusPosition = position;
            mFocusPositionOffset = 0;
            if (!mLayoutEnabled) {
                return;
            }
            if (smooth) {
                if (!hasDoneFirstLayout()) {
                    Log.w(getTag(), "setSelectionSmooth should " +
                            "not be called before first layout pass");
                    return;
                }
                LinearSmoothScroller linearSmoothScroller =
                        new LinearSmoothScroller(parent.getContext()) {
                    @Override
                    public PointF computeScrollVectorForPosition(int targetPosition) {
                        if (getChildCount() == 0) {
                            return null;
                        }
                        final int firstChildPos = getPosition(getChildAt(0));
                        final int direction = targetPosition < firstChildPos ? -1 : 1;
                        if (mOrientation == HORIZONTAL) {
                            return new PointF(direction, 0);
                        } else {
                            return new PointF(0, direction);
                        }
                    }
                    @Override
                    protected void onTargetFound(View targetView,
                            RecyclerView.State state, Action action) {
                        if (hasFocus()) {
                            targetView.requestFocus();
                        }
                        dispatchChildSelected();
                        if (getScrollPosition(targetView, mTempDeltas)) {
                            int dx, dy;
                            if (mOrientation == HORIZONTAL) {
                                dx = mTempDeltas[0];
                                dy = mTempDeltas[1];
                            } else {
                                dx = mTempDeltas[1];
                                dy = mTempDeltas[0];
                            }
                            final int distance = (int) Math.sqrt(dx * dx + dy * dy);
                            final int time = calculateTimeForDeceleration(distance);
                            action.update(dx, dy, time, mDecelerateInterpolator);
                        }
                    }
                };
                linearSmoothScroller.setTargetPosition(position);
                startSmoothScroll(linearSmoothScroller);
            } else {
                mForceFullLayout = true;
                parent.requestLayout();
            }
        }
    }

    @Override
    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        if (DEBUG) Log.v(getTag(), "onItemsAdded positionStart "
                + positionStart + " itemCount " + itemCount);
        if (mFocusPosition != NO_POSITION && mFocusPositionOffset != Integer.MIN_VALUE
                && getChildAt(mFocusPosition) != null) {
            int pos = mFocusPosition + mFocusPositionOffset;
            if (positionStart <= pos) {
                mFocusPositionOffset += itemCount;
            }
        }
        mChildrenStates.clear();
    }

    @Override
    public void onItemsChanged(RecyclerView recyclerView) {
        if (DEBUG) Log.v(getTag(), "onItemsChanged");
        mFocusPositionOffset = 0;
        mChildrenStates.clear();
    }

    @Override
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        if (DEBUG) Log.v(getTag(), "onItemsAdded positionStart "
                + positionStart + " itemCount " + itemCount);
        if (mFocusPosition != NO_POSITION && mFocusPositionOffset != Integer.MIN_VALUE
                && getChildAt(mFocusPosition) != null) {
            int pos = mFocusPosition + mFocusPositionOffset;
            if (positionStart <= pos) {
                if (positionStart + itemCount > pos) {
                    // stop updating offset after the focus item was removed
                    mFocusPositionOffset = Integer.MIN_VALUE;
                } else {
                    mFocusPositionOffset -= itemCount;
                }
            }
        }
        mChildrenStates.clear();
    }

    @Override
    public void onItemsMoved(RecyclerView recyclerView, int fromPosition, int toPosition,
            int itemCount) {
        if (DEBUG) Log.v(getTag(), "onItemsAdded fromPosition "
                + fromPosition + " toPosition " + toPosition);
        if (mFocusPosition != NO_POSITION && mFocusPositionOffset != Integer.MIN_VALUE
                && getChildAt(mFocusPosition) != null) {
            int pos = mFocusPosition + mFocusPositionOffset;
            if (fromPosition <= pos && pos < fromPosition + itemCount) {
                // moved items include focused position
                mFocusPositionOffset += toPosition - fromPosition;
            } else if (fromPosition < pos && toPosition > pos - itemCount) {
                // move items before focus position to after focused position
                mFocusPositionOffset -= itemCount;
            } else if (fromPosition > pos && toPosition < pos) {
                // move items after focus position to before focused position
                mFocusPositionOffset += itemCount;
            }
        }
        mChildrenStates.clear();
    }

    @Override
    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount) {
        for (int i = positionStart, end = positionStart + itemCount; i < end; i++) {
            mChildrenStates.remove(i);
        }
    }

    @Override
    public boolean onRequestChildFocus(RecyclerView parent, View child, View focused) {
        if (mFocusSearchDisabled) {
            return true;
        }
        if (!mInLayout && !mInSelection) {
            scrollToView(child, true);
        }
        return true;
    }

    @Override
    public boolean requestChildRectangleOnScreen(RecyclerView parent, View view, Rect rect,
            boolean immediate) {
        if (DEBUG) Log.v(getTag(), "requestChildRectangleOnScreen " + view + " " + rect);
        return false;
    }

    int getScrollOffsetX() {
        return mOrientation == HORIZONTAL ? mScrollOffsetPrimary : mScrollOffsetSecondary;
    }

    int getScrollOffsetY() {
        return mOrientation == HORIZONTAL ? mScrollOffsetSecondary : mScrollOffsetPrimary;
    }

    public void getViewSelectedOffsets(View view, int[] offsets) {
        if (mOrientation == HORIZONTAL) {
            offsets[0] = getPrimarySystemScrollPosition(view) - mScrollOffsetPrimary;
            offsets[1] = getSecondarySystemScrollPosition(view) - mScrollOffsetSecondary;
        } else {
            offsets[1] = getPrimarySystemScrollPosition(view) - mScrollOffsetPrimary;
            offsets[0] = getSecondarySystemScrollPosition(view) - mScrollOffsetSecondary;
        }
    }

    private int getPrimarySystemScrollPosition(View view) {
        int viewCenterPrimary = mScrollOffsetPrimary + getViewCenter(view);
        int pos = getPositionByView(view);
        StaggeredGrid.Location location = mGrid.getLocation(pos);
        final int row = location.row;
        boolean isFirst = mFirstVisiblePos == 0;
        // TODO: change to use State object in onRequestChildFocus()
        boolean isLast = mLastVisiblePos == (mState == null ?
                getItemCount() : mState.getItemCount()) - 1;
        if (isFirst || isLast) {
            for (int i = getChildCount() - 1; i >= 0; i--) {
                int position = getPositionByIndex(i);
                StaggeredGrid.Location loc = mGrid.getLocation(position);
                if (loc != null && loc.row == row) {
                    if (position < pos) {
                        isFirst = false;
                    } else if (position > pos) {
                        isLast = false;
                    }
                }
            }
        }
        return mWindowAlignment.mainAxis().getSystemScrollPos(viewCenterPrimary, isFirst, isLast);
    }

    private int getSecondarySystemScrollPosition(View view) {
        int viewCenterSecondary = mScrollOffsetSecondary + getViewCenterSecondary(view);
        int pos = getPositionByView(view);
        StaggeredGrid.Location location = mGrid.getLocation(pos);
        final int row = location.row;
        boolean isFirst = row == 0;
        boolean isLast = row == mGrid.getNumRows() - 1;
        return mWindowAlignment.secondAxis().getSystemScrollPos(viewCenterSecondary,
                isFirst, isLast);
    }

    /**
     * Scroll to a given child view and change mFocusPosition.
     */
    private void scrollToView(View view, boolean smooth) {
        int newFocusPosition = getPositionByView(view);
        if (newFocusPosition != mFocusPosition) {
            mFocusPosition = newFocusPosition;
            mFocusPositionOffset = 0;
            if (!mInLayout) {
                dispatchChildSelected();
            }
        }
        if (mBaseGridView.isChildrenDrawingOrderEnabledInternal()) {
            mBaseGridView.invalidate();
        }
        if (view == null) {
            return;
        }
        if (!view.hasFocus() && mBaseGridView.hasFocus()) {
            // transfer focus to the child if it does not have focus yet (e.g. triggered
            // by setSelection())
            view.requestFocus();
        }
        if (!mScrollEnabled) {
            return;
        }
        if (getScrollPosition(view, mTempDeltas)) {
            scrollGrid(mTempDeltas[0], mTempDeltas[1], smooth);
        }
    }

    private boolean getScrollPosition(View view, int[] deltas) {
        switch (mFocusScrollStrategy) {
        case BaseGridView.FOCUS_SCROLL_ALIGNED:
        default:
            return getAlignedPosition(view, deltas);
        case BaseGridView.FOCUS_SCROLL_ITEM:
        case BaseGridView.FOCUS_SCROLL_PAGE:
            return getNoneAlignedPosition(view, deltas);
        }
    }

    private boolean getNoneAlignedPosition(View view, int[] deltas) {
        int pos = getPositionByView(view);
        int viewMin = getViewMin(view);
        int viewMax = getViewMax(view);
        // we either align "firstView" to left/top padding edge
        // or align "lastView" to right/bottom padding edge
        View firstView = null;
        View lastView = null;
        int paddingLow = mWindowAlignment.mainAxis().getPaddingLow();
        int clientSize = mWindowAlignment.mainAxis().getClientSize();
        final int row = mGrid.getLocation(pos).row;
        if (viewMin < paddingLow) {
            // view enters low padding area:
            firstView = view;
            if (mFocusScrollStrategy == BaseGridView.FOCUS_SCROLL_PAGE) {
                // scroll one "page" left/top,
                // align first visible item of the "page" at the low padding edge.
                while (!prependOneVisibleItem()) {
                    List<Integer> positions =
                            mGrid.getItemPositionsInRows(mFirstVisiblePos, pos)[row];
                    firstView = findViewByPosition(positions.get(0));
                    if (viewMax - getViewMin(firstView) > clientSize) {
                        if (positions.size() > 1) {
                            firstView = findViewByPosition(positions.get(1));
                        }
                        break;
                    }
                }
            }
        } else if (viewMax > clientSize + paddingLow) {
            // view enters high padding area:
            if (mFocusScrollStrategy == BaseGridView.FOCUS_SCROLL_PAGE) {
                // scroll whole one page right/bottom, align view at the low padding edge.
                firstView = view;
                do {
                    List<Integer> positions =
                            mGrid.getItemPositionsInRows(pos, mLastVisiblePos)[row];
                    lastView = findViewByPosition(positions.get(positions.size() - 1));
                    if (getViewMax(lastView) - viewMin > clientSize) {
                        lastView = null;
                        break;
                    }
                } while (!appendOneVisibleItem());
                if (lastView != null) {
                    // however if we reached end,  we should align last view.
                    firstView = null;
                }
            } else {
                lastView = view;
            }
        }
        int scrollPrimary = 0;
        int scrollSecondary = 0;
        if (firstView != null) {
            scrollPrimary = getViewMin(firstView) - paddingLow;
        } else if (lastView != null) {
            scrollPrimary = getViewMax(lastView) - (paddingLow + clientSize);
        }
        View secondaryAlignedView;
        if (firstView != null) {
            secondaryAlignedView = firstView;
        } else if (lastView != null) {
            secondaryAlignedView = lastView;
        } else {
            secondaryAlignedView = view;
        }
        scrollSecondary = getSecondarySystemScrollPosition(secondaryAlignedView);
        scrollSecondary -= mScrollOffsetSecondary;
        if (scrollPrimary != 0 || scrollSecondary != 0) {
            deltas[0] = scrollPrimary;
            deltas[1] = scrollSecondary;
            return true;
        }
        return false;
    }

    private boolean getAlignedPosition(View view, int[] deltas) {
        int scrollPrimary = getPrimarySystemScrollPosition(view);
        int scrollSecondary = getSecondarySystemScrollPosition(view);
        if (DEBUG) {
            Log.v(getTag(), "getAlignedPosition " + scrollPrimary + " " + scrollSecondary
                    +" " + mWindowAlignment);
        }
        scrollPrimary -= mScrollOffsetPrimary;
        scrollSecondary -= mScrollOffsetSecondary;
        if (scrollPrimary != 0 || scrollSecondary != 0) {
            deltas[0] = scrollPrimary;
            deltas[1] = scrollSecondary;
            return true;
        }
        return false;
    }

    private void scrollGrid(int scrollPrimary, int scrollSecondary, boolean smooth) {
        if (mInLayout) {
            scrollDirectionPrimary(scrollPrimary);
            scrollDirectionSecondary(scrollSecondary);
        } else {
            int scrollX;
            int scrollY;
            if (mOrientation == HORIZONTAL) {
                scrollX = scrollPrimary;
                scrollY = scrollSecondary;
            } else {
                scrollX = scrollSecondary;
                scrollY = scrollPrimary;
            }
            if (smooth) {
                mBaseGridView.smoothScrollBy(scrollX, scrollY);
            } else {
                mBaseGridView.scrollBy(scrollX, scrollY);
            }
        }
    }

    public void setPruneChild(boolean pruneChild) {
        if (mPruneChild != pruneChild) {
            mPruneChild = pruneChild;
            if (mPruneChild) {
                requestLayout();
            }
        }
    }

    public boolean getPruneChild() {
        return mPruneChild;
    }

    public void setScrollEnabled(boolean scrollEnabled) {
        if (mScrollEnabled != scrollEnabled) {
            mScrollEnabled = scrollEnabled;
            if (mScrollEnabled && mFocusScrollStrategy == BaseGridView.FOCUS_SCROLL_ALIGNED) {
                View focusView = findViewByPosition(mFocusPosition == NO_POSITION ? 0 :
                    mFocusPosition);
                if (focusView != null) {
                    scrollToView(focusView, true);
                }
            }
        }
    }

    public boolean isScrollEnabled() {
        return mScrollEnabled;
    }

    private int findImmediateChildIndex(View view) {
        while (view != null && view != mBaseGridView) {
            int index = mBaseGridView.indexOfChild(view);
            if (index >= 0) {
                return index;
            }
            view = (View) view.getParent();
        }
        return NO_POSITION;
    }

    void setFocusSearchDisabled(boolean disabled) {
        mFocusSearchDisabled = disabled;
    }

    boolean isFocusSearchDisabled() {
        return mFocusSearchDisabled;
    }

    @Override
    public View onInterceptFocusSearch(View focused, int direction) {
        if (mFocusSearchDisabled) {
            return focused;
        }
        return null;
    }

    boolean hasPreviousViewInSameRow(int pos) {
        if (mGrid == null || pos == NO_POSITION) {
            return false;
        }
        if (mFirstVisiblePos > 0) {
            return true;
        }
        final int focusedRow = mGrid.getLocation(pos).row;
        for (int i = getChildCount() - 1; i >= 0; i--) {
            int position = getPositionByIndex(i);
            StaggeredGrid.Location loc = mGrid.getLocation(position);
            if (loc != null && loc.row == focusedRow) {
                if (position < pos) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onAddFocusables(RecyclerView recyclerView,
            ArrayList<View> views, int direction, int focusableMode) {
        if (mFocusSearchDisabled) {
            return true;
        }
        // If this viewgroup or one of its children currently has focus then we
        // consider our children for focus searching in main direction on the same row.
        // If this viewgroup has no focus and using focus align, we want the system
        // to ignore our children and pass focus to the viewgroup, which will pass
        // focus on to its children appropriately.
        // If this viewgroup has no focus and not using focus align, we want to
        // consider the child that does not overlap with padding area.
        if (recyclerView.hasFocus()) {
            final int movement = getMovement(direction);
            if (movement != PREV_ITEM && movement != NEXT_ITEM) {
                // Move on secondary direction uses default addFocusables().
                return false;
            }
            final View focused = recyclerView.findFocus();
            final int focusedPos = getPositionByIndex(findImmediateChildIndex(focused));
            // Add focusables of focused item.
            if (focusedPos != NO_POSITION) {
                findViewByPosition(focusedPos).addFocusables(views,  direction, focusableMode);
            }
            final int focusedRow = mGrid != null && focusedPos != NO_POSITION ?
                    mGrid.getLocation(focusedPos).row : NO_POSITION;
            // Add focusables of next neighbor of same row on the focus search direction.
            if (mGrid != null) {
                final int focusableCount = views.size();
                for (int i = 0, count = getChildCount(); i < count; i++) {
                    int index = movement == NEXT_ITEM ? i : count - 1 - i;
                    final View child = getChildAt(index);
                    if (child.getVisibility() != View.VISIBLE) {
                        continue;
                    }
                    int position = getPositionByIndex(index);
                    StaggeredGrid.Location loc = mGrid.getLocation(position);
                    if (focusedRow == NO_POSITION || (loc != null && loc.row == focusedRow)) {
                        if (focusedPos == NO_POSITION ||
                                (movement == NEXT_ITEM && position > focusedPos)
                                || (movement == PREV_ITEM && position < focusedPos)) {
                            child.addFocusables(views,  direction, focusableMode);
                            if (views.size() > focusableCount) {
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            if (mFocusScrollStrategy != BaseGridView.FOCUS_SCROLL_ALIGNED) {
                // adding views not overlapping padding area to avoid scrolling in gaining focus
                int left = mWindowAlignment.mainAxis().getPaddingLow();
                int right = mWindowAlignment.mainAxis().getClientSize() + left;
                int focusableCount = views.size();
                for (int i = 0, count = getChildCount(); i < count; i++) {
                    View child = getChildAt(i);
                    if (child.getVisibility() == View.VISIBLE) {
                        if (getViewMin(child) >= left && getViewMax(child) <= right) {
                            child.addFocusables(views, direction, focusableMode);
                        }
                    }
                }
                // if we cannot find any, then just add all children.
                if (views.size() == focusableCount) {
                    for (int i = 0, count = getChildCount(); i < count; i++) {
                        View child = getChildAt(i);
                        if (child.getVisibility() == View.VISIBLE) {
                            child.addFocusables(views, direction, focusableMode);
                        }
                    }
                    if (views.size() != focusableCount) {
                        return true;
                    }
                } else {
                    return true;
                }
                // if still cannot find any, fall through and add itself
            }
            if (recyclerView.isFocusable()) {
                views.add(recyclerView);
            }
        }
        return true;
    }

    @Override
    public View onFocusSearchFailed(View focused, int direction, Recycler recycler,
            RecyclerView.State state) {
        if (DEBUG) Log.v(getTag(), "onFocusSearchFailed direction " + direction);

        View view = null;
        int movement = getMovement(direction);
        if (mNumRows == 1) {
            // for simple row, use LinearSmoothScroller to smooth animation.
            // It will stay at a fixed cap speed in continuous scroll.
            if (movement == NEXT_ITEM) {
                int newPos = mFocusPosition + mNumRows;
                if (newPos < getItemCount()) {
                    setSelectionSmooth(mBaseGridView, newPos);
                    view = focused;
                } else {
                    if (!mFocusOutEnd) {
                        view = focused;
                    }
                }
            } else if (movement == PREV_ITEM){
                int newPos = mFocusPosition - mNumRows;
                if (newPos >= 0) {
                    setSelectionSmooth(mBaseGridView, newPos);
                    view = focused;
                } else {
                    if (!mFocusOutFront) {
                        view = focused;
                    }
                }
            }
        } else if (mNumRows > 1) {
            // for possible staggered grid,  we need guarantee focus to same row/column.
            // TODO: we may also use LinearSmoothScroller.
            saveContext(recycler, state);
            final FocusFinder ff = FocusFinder.getInstance();
            if (movement == NEXT_ITEM) {
                while (view == null && !appendOneVisibleItem()) {
                    view = ff.findNextFocus(mBaseGridView, focused, direction);
                }
            } else if (movement == PREV_ITEM){
                while (view == null && !prependOneVisibleItem()) {
                    view = ff.findNextFocus(mBaseGridView, focused, direction);
                }
            }
            if (view == null) {
                // returning the same view to prevent focus lost when scrolling past the end of the list
                if (movement == PREV_ITEM) {
                    view = mFocusOutFront ? null : focused;
                } else if (movement == NEXT_ITEM){
                    view = mFocusOutEnd ? null : focused;
                }
            }
            leaveContext();
        }
        if (DEBUG) Log.v(getTag(), "returning view " + view);
        return view;
    }

    boolean gridOnRequestFocusInDescendants(RecyclerView recyclerView, int direction,
            Rect previouslyFocusedRect) {
        switch (mFocusScrollStrategy) {
        case BaseGridView.FOCUS_SCROLL_ALIGNED:
        default:
            return gridOnRequestFocusInDescendantsAligned(recyclerView,
                    direction, previouslyFocusedRect);
        case BaseGridView.FOCUS_SCROLL_PAGE:
        case BaseGridView.FOCUS_SCROLL_ITEM:
            return gridOnRequestFocusInDescendantsUnaligned(recyclerView,
                    direction, previouslyFocusedRect);
        }
    }

    private boolean gridOnRequestFocusInDescendantsAligned(RecyclerView recyclerView,
            int direction, Rect previouslyFocusedRect) {
        View view = findViewByPosition(mFocusPosition);
        if (view != null) {
            boolean result = view.requestFocus(direction, previouslyFocusedRect);
            if (!result && DEBUG) {
                Log.w(getTag(), "failed to request focus on " + view);
            }
            return result;
        }
        return false;
    }

    private boolean gridOnRequestFocusInDescendantsUnaligned(RecyclerView recyclerView,
            int direction, Rect previouslyFocusedRect) {
        // focus to view not overlapping padding area to avoid scrolling in gaining focus
        int index;
        int increment;
        int end;
        int count = getChildCount();
        if ((direction & View.FOCUS_FORWARD) != 0) {
            index = 0;
            increment = 1;
            end = count;
        } else {
            index = count - 1;
            increment = -1;
            end = -1;
        }
        int left = mWindowAlignment.mainAxis().getPaddingLow();
        int right = mWindowAlignment.mainAxis().getClientSize() + left;
        for (int i = index; i != end; i += increment) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                if (getViewMin(child) >= left && getViewMax(child) <= right) {
                    if (child.requestFocus(direction, previouslyFocusedRect)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private final static int PREV_ITEM = 0;
    private final static int NEXT_ITEM = 1;
    private final static int PREV_ROW = 2;
    private final static int NEXT_ROW = 3;

    private int getMovement(int direction) {
        int movement = View.FOCUS_LEFT;

        if (mOrientation == HORIZONTAL) {
            switch(direction) {
                case View.FOCUS_LEFT:
                    movement = PREV_ITEM;
                    break;
                case View.FOCUS_RIGHT:
                    movement = NEXT_ITEM;
                    break;
                case View.FOCUS_UP:
                    movement = PREV_ROW;
                    break;
                case View.FOCUS_DOWN:
                    movement = NEXT_ROW;
                    break;
            }
         } else if (mOrientation == VERTICAL) {
             switch(direction) {
                 case View.FOCUS_LEFT:
                     movement = PREV_ROW;
                     break;
                 case View.FOCUS_RIGHT:
                     movement = NEXT_ROW;
                     break;
                 case View.FOCUS_UP:
                     movement = PREV_ITEM;
                     break;
                 case View.FOCUS_DOWN:
                     movement = NEXT_ITEM;
                     break;
             }
         }

        return movement;
    }

    int getChildDrawingOrder(RecyclerView recyclerView, int childCount, int i) {
        View view = findViewByPosition(mFocusPosition);
        if (view == null) {
            return i;
        }
        int focusIndex = recyclerView.indexOfChild(view);
        // supposely 0 1 2 3 4 5 6 7 8 9, 4 is the center item
        // drawing order is 0 1 2 3 9 8 7 6 5 4
        if (i < focusIndex) {
            return i;
        } else if (i < childCount - 1) {
            return focusIndex + childCount - 1 - i;
        } else {
            return focusIndex;
        }
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter,
            RecyclerView.Adapter newAdapter) {
        if (DEBUG) Log.v(getTag(), "onAdapterChanged to " + newAdapter);
        if (oldAdapter != null) {
            discardLayoutInfo();
            mFocusPosition = NO_POSITION;
            mFocusPositionOffset = 0;
            mChildrenStates.clear();
        }
        super.onAdapterChanged(oldAdapter, newAdapter);
    }

    private void discardLayoutInfo() {
        mGrid = null;
        mRows = null;
        mRowSizeSecondary = null;
        mFirstVisiblePos = -1;
        mLastVisiblePos = -1;
        mRowSecondarySizeRefresh = false;
    }

    public void setLayoutEnabled(boolean layoutEnabled) {
        if (mLayoutEnabled != layoutEnabled) {
            mLayoutEnabled = layoutEnabled;
            requestLayout();
        }
    }

    void setChildrenVisibility(int visiblity) {
        mChildVisibility = visiblity;
        if (mChildVisibility != -1) {
            int count = getChildCount();
            for (int i= 0; i < count; i++) {
                getChildAt(i).setVisibility(mChildVisibility);
            }
        }
    }

    final static class SavedState implements Parcelable {

        int index; // index inside adapter of the current view
        Bundle childStates = Bundle.EMPTY;

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(index);
            out.writeBundle(childStates);
        }

        @SuppressWarnings("hiding")
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

        @Override
        public int describeContents() {
            return 0;
        }

        SavedState(Parcel in) {
            index = in.readInt();
            childStates = in.readBundle(GridLayoutManager.class.getClassLoader());
        }

        SavedState() {
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        if (DEBUG) Log.v(getTag(), "onSaveInstanceState getSelection() " + getSelection());
        SavedState ss = new SavedState();
        for (int i = 0, count = getChildCount(); i < count; i++) {
            View view = getChildAt(i);
            int position = getPositionByView(view);
            if (position != NO_POSITION) {
                mChildrenStates.saveOnScreenView(view, position);
            }
        }
        ss.index = getSelection();
        ss.childStates = mChildrenStates.saveAsBundle();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            return;
        }
        SavedState loadingState = (SavedState)state;
        mFocusPosition = loadingState.index;
        mFocusPositionOffset = 0;
        mChildrenStates.loadFromBundle(loadingState.childStates);
        mForceFullLayout = true;
        requestLayout();
        if (DEBUG) Log.v(getTag(), "onRestoreInstanceState mFocusPosition " + mFocusPosition);
    }
}
