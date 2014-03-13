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

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.Recycler;
import static android.support.v7.widget.RecyclerView.NO_ID;
import static android.support.v7.widget.RecyclerView.NO_POSITION;
import static android.support.v7.widget.RecyclerView.HORIZONTAL;
import static android.support.v7.widget.RecyclerView.VERTICAL;
import android.support.v17.leanback.R;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

final class GridLayoutManager extends RecyclerView.LayoutManager {

    private static final String TAG = "GridLayoutManager";
    private static final boolean DEBUG = false;

    private static final Interpolator sDefaultAnimationChildLayoutInterpolator
            = new DecelerateInterpolator();

    private static final long DEFAULT_CHILD_ANIMATION_DURATION_MS = 250;

    private String getTag() {
        return TAG + ":" + mBaseListView.getId();
    }

    private final BaseListView mBaseListView;

    /**
     * The orientation of a "row".
     */
    private int mOrientation = HORIZONTAL;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.Recycler mRecycler;

    private boolean mInLayout = false;

    private OnChildSelectedListener mChildSelectedListener = null;

    /**
     * The focused position, it's not the currently visually aligned position
     * but it is the final position that we intend to focus on. If there are
     * multiple setSelection() called, mFocusPosition saves last value.
     */
    private int mFocusPosition = NO_POSITION;

    /**
     * Force a full layout under certain situations.
     */
    private boolean mForceFullLayout;

    /**
     * The scroll offsets of the viewport relative to the entire view.
     */
    private int mScrollOffsetPrimary;
    private int mScrollOffsetSecondary;

    /**
     * User-specified fixed size of each grid item in the secondary direction, can be
     * 0 to be determined by parent size and number of rows.
     */
    private int mItemLengthSecondaryRequested;
    /**
     * The fixed size of each grid item in the secondary direction. This corresponds to
     * the row height, equal for all rows. Grid items may have variable length
     * in the primary direction.
     *
     */
    private int mItemLengthSecondary;

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
     * Animate layout changes from a child resizing or adding/removing a child.
     */
    private boolean mAnimateChildLayout = true;

    /**
     * Interpolator used to animate layout of children.
     */
    private Interpolator mAnimateLayoutChildInterpolator = sDefaultAnimationChildLayoutInterpolator;

    /**
     * Duration used to animate layout of children.
     */
    private long mAnimateLayoutChildDuration = DEFAULT_CHILD_ANIMATION_DURATION_MS;

    public GridLayoutManager(BaseListView baseListView) {
        mBaseListView = baseListView;
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

    public void setRowHeight(int height) {
        if (height < 0) throw new IllegalArgumentException();
        mItemLengthSecondaryRequested = height;
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

    protected int getMeasuredLengthPrimary(View v) {
        if (mOrientation == HORIZONTAL) {
            float aspectRatio = (float) v.getMeasuredWidth() / (float) v.getMeasuredHeight();
            return (int) (aspectRatio * mItemLengthSecondary);
        } else {
            float aspectRatio = (float) v.getMeasuredHeight() / (float) v.getMeasuredWidth();
            return (int) (aspectRatio * mItemLengthSecondary);
        }
    }

    protected boolean hasDoneFirstLayout() {
        return mGrid != null;
    }

    public void setOnChildSelectedListener(OnChildSelectedListener listener) {
        mChildSelectedListener = listener;
    }

    private int getPositionByView(View view) {
        int index = mBaseListView.indexOfChild(view);
        if (index < 0) {
            return NO_POSITION;
        }
        return mFirstVisiblePos + index;
    }

    private View getViewByPosition(int position) {
        int index = getIndexByPosition(position);
        if (index < 0) {
            return null;
        }
        return getChildAt(index);
    }

    private int getIndexByPosition(int position) {
        if (mFirstVisiblePos < 0 ||
                position < mFirstVisiblePos || position > mLastVisiblePos) {
            return -1;
        }
        return position - mFirstVisiblePos;
    }

    private void dispatchChildSelected() {
        if (mChildSelectedListener == null) {
            return;
        }

        View view = getViewByPosition(mFocusPosition);

        if (mFocusPosition != NO_POSITION) {
            mChildSelectedListener.onChildSelected(mBaseListView, view, mFocusPosition,
                    mAdapter.getItemId(mFocusPosition));
        } else {
            mChildSelectedListener.onChildSelected(mBaseListView, null, NO_POSITION, NO_ID);
        }
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
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static GridLayoutManagerChildTag getViewTag(View v) {
        return (GridLayoutManagerChildTag) v.getTag(R.id.lb_gridlayoutmanager_tag);
    }

    protected View getViewForPosition(int position) {
        View v = mRecycler.getViewForPosition(mAdapter, position);
        if (v != null) {
            GridLayoutManagerChildTag tag = getViewTag(v);
            if (tag == null) {
                tag = new GridLayoutManagerChildTag();
                v.setTag(R.id.lb_gridlayoutmanager_tag, tag);
            }
            tag.attach(this, v);
        }
        return v;
    }

    private int getViewMin(View view) {
        GridLayoutManagerChildTag tag = getViewTag(view);
        return (mOrientation == HORIZONTAL) ? tag.getOpticalLeft() : tag.getOpticalTop();
    }

    private int getViewMax(View view) {
        GridLayoutManagerChildTag tag = getViewTag(view);
        return (mOrientation == HORIZONTAL) ? tag.getOpticalRight() : tag.getOpticalBottom();
    }

    private int getViewCenter(View view) {
        return (mOrientation == HORIZONTAL) ? getViewCenterX(view) : getViewCenterY(view);
    }

    private int getViewCenterSecondary(View view) {
        return (mOrientation == HORIZONTAL) ? getViewCenterY(view) : getViewCenterX(view);
    }

    private int getViewCenterX(View view) {
        GridLayoutManagerChildTag tag = getViewTag(view);
        return tag.getOpticalLeft() + tag.getAlignX();
    }

    private int getViewCenterY(View view) {
        GridLayoutManagerChildTag tag = getViewTag(view);
        return tag.getOpticalTop() + tag.getAlignY();
    }

    /**
     * Re-initialize data structures for a data change or handling invisible
     * selection. The method tries its best to preserve position information so
     * that staggered grid looks same before and after re-initialize.
     * @param focusPosition The initial focusPosition that we would like to
     *        focus on.
     * @return Actual position that can be focused on.
     */
    private int init(RecyclerView.Adapter adapter, RecyclerView.Recycler recycler,
            int focusPosition) {

        final int newItemCount = adapter.getItemCount();

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
                    View v = getViewByPosition(i);
                    if (v == null) {
                        continue;
                    }
                    int row = mGrid.getLocation(i).row;
                    int low = getViewMin(v) + mScrollOffsetPrimary;
                    if (low < mRows[row].low) {
                        mRows[row].low = mRows[row].high = low;
                    }
                }
                // fill other rows that does not include the subset using first item
                int firstItemRowPosition = mRows[mGrid.getLocation(firstIndex).row].low;
                if (firstItemRowPosition == Integer.MAX_VALUE) {
                    firstItemRowPosition = 0;
                }
                for (int i = 0; i < mNumRows; i++) {
                    if (mRows[i].low == Integer.MAX_VALUE) {
                        mRows[i].low = mRows[i].high = firstItemRowPosition;
                    }
                }
            }

            // Same adapter, we can reuse any attached views
            detachAndScrapAttachedViews(recycler);

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
            removeAllViews();

            mScrollOffsetPrimary = 0;
            mScrollOffsetSecondary = 0;
            mWindowAlignment.reset();
        }

        mAdapter = adapter;
        mRecycler = recycler;
        mGrid.setProvider(mGridProvider);
        // mGrid share the same Row array information
        mGrid.setRows(mRows);
        mFirstVisiblePos = mLastVisiblePos = -1;

        initScrollController();

        return focusPosition;
    }

    // TODO: use recyclerview support for measuring the whole container, once
    // it's available.
    void onMeasure(int widthSpec, int heightSpec, int[] result) {
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
        switch (modeSecondary) {
        case MeasureSpec.UNSPECIFIED:
            if (mItemLengthSecondaryRequested == 0) {
                if (mOrientation == HORIZONTAL) {
                    throw new IllegalStateException("Must specify rowHeight or view height");
                } else {
                    throw new IllegalStateException("Must specify columnWidth or view width");
                }
            }
            mItemLengthSecondary = mItemLengthSecondaryRequested;
            if (mNumRowsRequested == 0) {
                mNumRows = 1;
            } else {
                mNumRows = mNumRowsRequested;
            }
            measuredSizeSecondary = mItemLengthSecondary * mNumRows + mMarginSecondary
                * (mNumRows - 1) + paddingSecondary;
            break;
        case MeasureSpec.AT_MOST:
        case MeasureSpec.EXACTLY:
            if (mNumRowsRequested == 0 && mItemLengthSecondaryRequested == 0) {
                mNumRows = 1;
                mItemLengthSecondary = sizeSecondary - paddingSecondary;
            } else if (mNumRowsRequested == 0) {
                mItemLengthSecondary = mItemLengthSecondaryRequested;
                mNumRows = (sizeSecondary + mMarginSecondary)
                    / (mItemLengthSecondaryRequested + mMarginSecondary);
            } else if (mItemLengthSecondaryRequested == 0) {
                mNumRows = mNumRowsRequested;
                mItemLengthSecondary = (sizeSecondary - paddingSecondary - mMarginSecondary
                        * (mNumRows - 1)) / mNumRows;
            } else {
                mNumRows = mNumRowsRequested;
                mItemLengthSecondary = mItemLengthSecondaryRequested;
            }
            measuredSizeSecondary = sizeSecondary;
            if (modeSecondary == MeasureSpec.AT_MOST) {
                int childrenSize = mItemLengthSecondary * mNumRows + mMarginSecondary 
                    * (mNumRows - 1) + paddingSecondary;
                if (childrenSize < measuredSizeSecondary) {
                    measuredSizeSecondary = childrenSize;
                }
            }
            break;
        default:
            throw new IllegalStateException("wrong spec");
        }
        if (mOrientation == HORIZONTAL) {
            result[0] = sizePrimary;
            result[1] = measuredSizeSecondary;
        } else {
            result[0] = measuredSizeSecondary;
            result[1] = sizePrimary;
        }
        if (DEBUG) {
            Log.v(getTag(), "onMeasure result " + result[0] + ", " + result[1] 
                    + " mItemLengthSecondary " + mItemLengthSecondary + " mNumRows " + mNumRows);
        }
    }

    private void measureChild(View child) {
        final ViewGroup.LayoutParams lp = child.getLayoutParams();

        int widthSpec, heightSpec;
        if (mOrientation == HORIZONTAL) {
            if (lp.width >= 0) {
                widthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
            } else {
                widthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            }
            heightSpec = MeasureSpec.makeMeasureSpec(mItemLengthSecondary, MeasureSpec.EXACTLY);
        } else {
            if (lp.height >= 0) {
                heightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
            } else {
                heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            }
            widthSpec = MeasureSpec.makeMeasureSpec(mItemLengthSecondary, MeasureSpec.EXACTLY);
        }

        child.measure(widthSpec, heightSpec);
    }

    private StaggeredGrid.Provider mGridProvider = new StaggeredGrid.Provider() {

        @Override
        public int getCount() {
            return mAdapter.getItemCount();
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

            if (append) {
                addView(v);
            } else {
                addView(v, 0);
            }

            measureChild(v);

            int length = getMeasuredLengthPrimary(v);
            int start, end;
            if (append) {
                start = mRows[rowIndex].high;
                if (start != mRows[rowIndex].low) {
                    // if there are existing item in the row,  add margin between
                    start += mMarginPrimary;
                }
                end = start + length;
                mRows[rowIndex].high = end;
            } else {
                end = mRows[rowIndex].low;
                if (end != mRows[rowIndex].high) {
                    end -= mMarginPrimary;
                }
                start = end - length;
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
            int startSecondary = rowIndex * (mItemLengthSecondary + mMarginSecondary);
            layoutChild(v, start - mScrollOffsetPrimary, end - mScrollOffsetPrimary,
                    startSecondary - mScrollOffsetSecondary);
            if (DEBUG) {
                Log.d(getTag(), "addView " + index + " " + v);
            }
            updateScrollMin();
            updateScrollMax();
        }
    };

    private void layoutChild(View v, int start, int end, int startSecondary) {
        if (mOrientation == HORIZONTAL) {
            v.layout(start, startSecondary, end, startSecondary + mItemLengthSecondary);
        } else {
            v.layout(startSecondary, start, startSecondary + mItemLengthSecondary, end);
        }
        updateChildAlignments(v);
    }

    private void updateChildAlignments(View v) {
        GridLayoutManagerChildTag tag = getViewTag(v);
        tag.setAlignX(mItemAlignment.horizontal.getAlignmentPosition(v, tag));
        tag.setAlignY(mItemAlignment.vertical.getAlignmentPosition(v, tag));
    }

    private void updateChildAlignments() {
        for (int i = 0, c = getChildCount(); i < c; i++) {
            updateChildAlignments(getChildAt(i));
        }
    }

    /**
     * append invisible view of saved location
     */
    private void appendViewWithSavedLocation() {
        int index = mLastVisiblePos + 1;
        mGridProvider.createItem(index, mGrid.getLocation(index).row, true);
    }

    /**
     * prepend invisible view of saved location
     */
    private void prependViewWithSavedLocation() {
        int index = mFirstVisiblePos - 1;
        mGridProvider.createItem(index, mGrid.getLocation(index).row, false);
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

    private void appendVisibleItems() {
        while (needsAppendVisibleItem()) {
            if (mLastVisiblePos >= 0 && mLastVisiblePos < mGrid.getLastIndex()) {
                appendViewWithSavedLocation();
            } else if (mLastVisiblePos < mAdapter.getItemCount() - 1) {
                mGrid.appendItems(mScrollOffsetPrimary + mSizePrimary);
            } else {
                break;
            }
        }
    }

    private void prependVisibleItems() {
        while (needsPrependVisibleItem()) {
            if (mFirstVisiblePos > 0) {
                if (mFirstVisiblePos > mGrid.getFirstIndex()) {
                    prependViewWithSavedLocation();
                } else {
                    mGrid.prependItems(mScrollOffsetPrimary);
                }
            } else {
                break;
            }
        }
    }

    // TODO: use removeAndRecycleViewAt() once we stop using tags.
    private void removeChildAt(int position) {
        View v = getViewByPosition(position);
        if (v != null) {
            if (DEBUG) {
                Log.d(getTag(), "detachAndScrape " + position);
            }
            getViewTag(v).detach();
            removeAndRecycleViewAt(getIndexByPosition(position), mRecycler);
        }
    }

    private void removeInvisibleViewsAtEnd() {
        boolean update = false;
        while(mLastVisiblePos > mFirstVisiblePos && mLastVisiblePos > mFocusPosition) {
            View view = getViewByPosition(mLastVisiblePos);
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
        boolean update = false;
        while(mLastVisiblePos > mFirstVisiblePos && mFirstVisiblePos < mFocusPosition) {
            View view = getViewByPosition(mFirstVisiblePos);
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
            View view = getViewByPosition(i);
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

    /**
     * Relayout and re-positioning child for a possible new size and/or a new
     * start.
     *
     * @param view View to measure and layout.
     * @param start New start of the view or Integer.MIN_VALUE for not change.
     * @return New start of next view.
     */
    private int updateChildView(View view, int start, int startSecondary) {
        if (start == Integer.MIN_VALUE) {
            start = getViewMin(view);
        }
        int end;
        if (mOrientation == HORIZONTAL) {
            if (view.isLayoutRequested() || view.getMeasuredHeight() != mItemLengthSecondary) {
                measureChild(view);
            }
            end = start + view.getMeasuredWidth();
        } else {
            if (view.isLayoutRequested() || view.getMeasuredWidth() != mItemLengthSecondary) {
                measureChild(view);
            }
            end = start + view.getMeasuredHeight();
        }

        layoutChild(view, start, end, startSecondary);
        return end + mMarginPrimary;
    }

    // create a temporary structure that remembers visible items from left to
    // right on each row
    private ArrayList<Integer>[] buildRows() {
        ArrayList<Integer>[] rows = new ArrayList[mNumRows];
        for (int i = 0; i < mNumRows; i++) {
            rows[i] = new ArrayList();
        }
        if (mFirstVisiblePos >= 0) {
            for (int i = mFirstVisiblePos; i <= mLastVisiblePos; i++) {
                rows[mGrid.getLocation(i).row].add(i);
            }
        }
        return rows;
    }

    // Fast layout when there is no structure change, adapter change, etc.
    protected void fastRelayout() {
        initScrollController();

        ArrayList<Integer>[] rows = buildRows();

        // relayout and repositioning views on each row
        for (int i = 0; i < mNumRows; i++) {
            ArrayList<Integer> row = rows[i];
            int start = Integer.MIN_VALUE;
            int startSecondary =
                i * (mItemLengthSecondary + mMarginSecondary) - mScrollOffsetSecondary;
            for (int j = 0, size = row.size(); j < size; j++) {
                int position = row.get(j);
                start = updateChildView(getViewByPosition(position), start, startSecondary);
            }
        }

        appendVisibleItems();
        prependVisibleItems();

        updateRowsMinMax();
        updateScrollMin();
        updateScrollMax();

        View focusView = getViewByPosition(mFocusPosition == NO_POSITION ? 0 : mFocusPosition);
        if (focusView != null) {
            scrollToView(focusView, false, true);
        }
    }

    // Lays out items based on the current scroll position
    @Override
    public void layoutChildren(RecyclerView.Adapter adapter, RecyclerView.Recycler recycler,
            boolean structureChanged) {
        if (DEBUG) {
            Log.v(getTag(), "layoutChildren start numRows " + mNumRows + " mScrollOffsetSecondary "
                    + mScrollOffsetSecondary + " mScrollOffsetPrimary " + mScrollOffsetPrimary
                    + " structureChanged " + structureChanged
                    + " mForceFullLayout " + mForceFullLayout);
            Log.v(getTag(), "width " + getWidth() + " height " + getHeight());
        }

        final int itemCount = adapter.getItemCount();
        if (itemCount < 0) {
            return;
        }

        mInLayout = true;

        // Track the old focus view so we can adjust our system scroll position
        // so that any scroll animations happening now will remain valid.
        int delta = 0, deltaSecondary = 0;
        if (mFocusPosition != NO_POSITION) {
            View focusView = getViewByPosition(mFocusPosition);
            if (focusView != null) {
                delta = mWindowAlignment.mainAxis().getSystemScrollPos(
                        getViewCenter(focusView) + mScrollOffsetPrimary) - mScrollOffsetPrimary;
                deltaSecondary =
                    mWindowAlignment.secondAxis().getSystemScrollPos(
                            getViewCenterSecondary(focusView) + mScrollOffsetSecondary)
                    - mScrollOffsetSecondary;
            }
        }

        boolean hasDoneFirstLayout = hasDoneFirstLayout();
        if (!structureChanged && !mForceFullLayout && hasDoneFirstLayout) {
            fastRelayout();
        } else {
            boolean hadFocus = mBaseListView.hasFocus();

            int newFocusPosition = init(adapter, recycler, mFocusPosition);
            if (DEBUG) {
                Log.v(getTag(), "mFocusPosition " + mFocusPosition + " newFocusPosition "
                    + newFocusPosition);
            }

            // depending on result of init(), either recreating everything
            // or try to reuse the row start positions near mFocusPosition
            if (mGrid.getSize() == 0) {
                // this is a fresh creating all items, starting from
                // mFocusPosition with a estimated row index.
                mGrid.setStart(newFocusPosition, StaggeredGrid.START_DEFAULT);

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
                oldFirstVisible = mFirstVisiblePos;
                oldLastVisible = mLastVisiblePos;
                View focusView = getViewByPosition(newFocusPosition);
                // we need force to initialize the child view's position
                scrollToView(focusView, false, true);
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

        scrollDirectionPrimary(-delta);
        scrollDirectionSecondary(-deltaSecondary);
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

        removeAndRecycleScrap(recycler);
        attemptAnimateLayoutChild();

        if (!hasDoneFirstLayout) {
            dispatchChildSelected();
        }
        mInLayout = false;
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
        mScrollOffsetSecondary -= increment;
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
        mScrollOffsetPrimary -= increment;
    }

    @Override
    public int scrollHorizontallyBy(int dx, Adapter adapter, Recycler recycler) {
        if (DEBUG) Log.v(TAG, "scrollHorizontallyBy " + dx);

        if (mOrientation == HORIZONTAL) {
            return scrollDirectionPrimary(dx);
        } else {
            return scrollDirectionSecondary(dx);
        }
    }

    @Override
    public int scrollVerticallyBy(int dy, Adapter adapter, Recycler recycler) {
        if (DEBUG) Log.v(TAG, "scrollVerticallyBy " + dy);
        if (mOrientation == VERTICAL) {
            return scrollDirectionPrimary(dy);
        } else {
            return scrollDirectionSecondary(dy);
        }
    }

    // scroll in main direction may add/prune views
    private int scrollDirectionPrimary(int da) {
        offsetChildrenPrimary(-da);
        if (mInLayout) {
            return da;
        }
        if (da > 0) {
            appendVisibleItems();
            removeInvisibleViewsAtFront();
        } else if (da < 0) {
            prependVisibleItems();
            removeInvisibleViewsAtEnd();
        }
        attemptAnimateLayoutChild();
        mBaseListView.invalidate();
        return da;
    }

    // scroll in second direction will not add/prune views
    private int scrollDirectionSecondary(int dy) {
        offsetChildrenSecondary(-dy);
        mBaseListView.invalidate();
        return dy;
    }

    private void updateScrollMax() {
        if (mLastVisiblePos >= 0 && mLastVisiblePos == mAdapter.getItemCount() - 1) {
            int maxEdge = Integer.MIN_VALUE;
            for (int i = 0; i < mRows.length; i++) {
                if (mRows[i].high > maxEdge) {
                    maxEdge = mRows[i].high;
                }
            }
            mWindowAlignment.mainAxis().setMaxEdge(maxEdge);
            if (DEBUG) Log.v(getTag(), "updating scroll maxEdge to " + maxEdge);
        }
    }

    private void updateScrollMin() {
        if (mFirstVisiblePos == 0) {
            int minEdge = Integer.MAX_VALUE;
            for (int i = 0; i < mRows.length; i++) {
                if (mRows[i].low < minEdge) {
                    minEdge = mRows[i].low;
                }
            }
            mWindowAlignment.mainAxis().setMinEdge(minEdge);
            if (DEBUG) Log.v(getTag(), "updating scroll minEdge to " + minEdge);
        }
    }

    private void initScrollController() {
        mWindowAlignment.horizontal.setSize(getWidth());
        mWindowAlignment.horizontal.setPadding(getPaddingLeft(), getPaddingRight());
        mWindowAlignment.vertical.setSize(getHeight());
        mWindowAlignment.vertical.setPadding(getPaddingTop(), getPaddingBottom());
        mSizePrimary = mWindowAlignment.mainAxis().getSize();
        mWindowAlignment.mainAxis().invalidateScrollMin();
        mWindowAlignment.mainAxis().invalidateScrollMax();

        // second axis min/max is determined at initialization, the mainAxis
        // min/max is determined later when we scroll to first or last item
        mWindowAlignment.secondAxis().setMinEdge(0);
        mWindowAlignment.secondAxis().setMaxEdge(mItemLengthSecondary * mNumRows + mMarginSecondary
                * (mNumRows - 1));

        if (DEBUG) {
            Log.v(getTag(), "initScrollController mSizePrimary " + mSizePrimary + " " 
                + " mItemLengthSecondary " + mItemLengthSecondary + " " + mWindowAlignment);
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
        View view = getViewByPosition(position);
        if (view != null) {
            scrollToView(view, smooth, false);
        } else {
            boolean right = position > mFocusPosition;
            mFocusPosition = position;
            if (smooth) {
                if (!hasDoneFirstLayout()) {
                    Log.w(getTag(), "setSelectionSmooth should " +
                            "not be called before first layout pass");
                    return;
                }
                if (right) {
                    appendVisibleItems();
                } else {
                    prependVisibleItems();
                }
                view = getViewByPosition(position);
                if (view != null) {
                    scrollToView(view, smooth, false);
                }
            } else {
                mForceFullLayout = true;
                parent.requestLayout();
            }
        }
    }

    @Override
    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        boolean needsLayout = false;
        if (itemCount != 0) {
            if (mFirstVisiblePos < 0) {
                needsLayout = true;
            } else if (!(positionStart > mLastVisiblePos + 1 ||
                    positionStart + itemCount < mFirstVisiblePos - 1)) {
                needsLayout = true;
            }
        }
        if (needsLayout) {
            recyclerView.requestLayout();
        }
    }

    @Override
    public boolean onRequestChildFocus(RecyclerView parent, View child, View focused) {
        if (!mInLayout) {
            scrollToView(child, true, false);
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
        int scrollOffsetX = getScrollOffsetX();
        int scrollOffsetY = getScrollOffsetY();
        int viewCenterX = scrollOffsetX + getViewCenterX(view);
        int viewCenterY = scrollOffsetY + getViewCenterY(view);
        offsets[0] = mWindowAlignment.horizontal.getSystemScrollPos(viewCenterX) - scrollOffsetX;
        offsets[1] = mWindowAlignment.vertical.getSystemScrollPos(viewCenterY) - scrollOffsetY;
    }

    /**
     * Scroll to a given child view and change mFocusPosition.
     */
    private void scrollToView(View view, boolean smooth, boolean force) {
        int newFocusPosition = getPositionByView(view);
        if (force || newFocusPosition != mFocusPosition) {
            mFocusPosition = newFocusPosition;
            dispatchChildSelected();
        }
        if (view == null) {
            return;
        }
        if (!view.hasFocus() && mBaseListView.hasFocus()) {
            // transfer focus to the child if it does not have focus yet (e.g. triggered
            // by setSelection())
            view.requestFocus();
        }
        int viewCenterY = getScrollOffsetY() + getViewCenterY(view);
        int viewCenterX = getScrollOffsetX() + getViewCenterX(view);
        if (DEBUG) {
            Log.v(TAG, "scrollToView pos=" + mFocusPosition + " "
                    + viewCenterX+","+viewCenterY + " " + mWindowAlignment);
        }

        if (force || viewCenterX != mWindowAlignment.horizontal.getScrollCenter()
                || viewCenterY != mWindowAlignment.vertical.getScrollCenter()) {
            mWindowAlignment.horizontal.updateScrollCenter(viewCenterX);
            mWindowAlignment.vertical.updateScrollCenter(viewCenterY);
            int scrollX = mWindowAlignment.horizontal.getSystemScrollPos();
            int scrollY = mWindowAlignment.vertical.getSystemScrollPos();
            if (DEBUG) {
                Log.v(getTag(), "adjustSystemScrollPos " + scrollX + " " + scrollY + " "
                    + mWindowAlignment);
            }

            scrollX -= getScrollOffsetX();
            scrollY -= getScrollOffsetY();

            if (DEBUG) Log.v(getTag(), "calling scroller with " + scrollX + " " + scrollY);

            if (smooth) {
                mBaseListView.smoothScrollBy(scrollX, scrollY);
            } else {
                mBaseListView.scrollBy(scrollX, scrollY);
            }
        }
    }

    public void setAnimateChildLayout(boolean animateChildLayout) {
        mAnimateChildLayout = animateChildLayout;
        if (!mAnimateChildLayout) {
            for (int i = 0, c = getChildCount(); i < c; i++) {
                getViewTag(getChildAt(i)).endAnimate();
            }
        }
    }

    private void attemptAnimateLayoutChild() {
        for (int i = 0, c = getChildCount(); i < c; i++) {
            // TODO: start delay can be staggered
            getViewTag(getChildAt(i)).startAnimate(this, 0);
        }
    }

    public boolean isChildLayoutAnimated() {
        return mAnimateChildLayout;
    }

    public void setChildLayoutAnimationInterpolator(Interpolator interpolator) {
        mAnimateLayoutChildInterpolator = interpolator;
    }

    public Interpolator getChildLayoutAnimationInterpolator() {
        return mAnimateLayoutChildInterpolator;
    }

    public void setChildLayoutAnimationDuration(long duration) {
        mAnimateLayoutChildDuration = duration;
    }

    public long getChildLayoutAnimationDuration() {
        return mAnimateLayoutChildDuration;
    }

    @Override
    public View onFocusSearchFailed(View focused, int direction, Adapter adapter,
            Recycler recycler) {
        if (DEBUG) Log.v(getTag(), "onFocusSearchFailed direction " + direction);

        // XXX Move this into a focusSearch method when RecyclerView.focusSearch changes to give
        // LayoutManager first dibs on focus handling.  We may still want to leave a
        // onFocusSearchFailed method for lazy loading of content?

        View view = null;
        int movement = getMovement(direction);
        // returning the same view to prevent focus lost when scrolling past the end of the list
        if (movement == PREV_ITEM) {
            view = mFocusOutFront ? null : focused;
        } else if (movement == NEXT_ITEM){
            view = mFocusOutEnd ? null : focused;
        }

        // Add the necessary view
        // findNextFocusPosition(recycler, direction);
        // FocusFinder ff = FocusFinder.getInstance();
        // view = ff.findNextFocus(getRecyclerView(), focused, direction);
        if (DEBUG) Log.v(getTag(), "returning view " + view);
        return view;
    }

    private final static int PREV_ITEM = 0;
    private final static int NEXT_ITEM = 1;
    private final static int PREV_ROW = 2;
    private final static int NEXT_ROW = 3;

    // TODO: Remove this is support for restoring focus to previous child is
    // added to RecyclerView.
    public boolean focusSelectedChild() {
        View view = getViewByPosition(mFocusPosition);
        if (view != null) {
            if (!view.requestFocus()) {
                if (DEBUG) {
                    Log.v(getTag(), "failed to request focus on " + view);
                }
            } else {
                return true;
            }
        }
        return false;
    }

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

    @Override
    public void onAdapterChanged() {
        mGrid = null;
        mRows = null;
        super.onAdapterChanged();
    }
}
