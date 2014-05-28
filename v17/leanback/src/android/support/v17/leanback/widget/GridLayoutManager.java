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

import android.animation.TimeAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.Recycler;

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
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

final class GridLayoutManager extends RecyclerView.LayoutManager {

     /*
      * LayoutParams for {@link HorizontalGridView} and {@link VerticalGridView}.
      * The class currently does three internal jobs:
      * - Saves optical bounds insets.
      * - Caches focus align view center.
      * - Manages child view layout animation.
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

        // For animations
        private TimeAnimator mAnimator;
        private long mDuration;
        private boolean mFirstAttached;
        // current virtual view position (scrollOffset + left/top) in the GridLayoutManager
        private int mViewX, mViewY;
        // animation start value of translation x and y
        private float mAnimationStartTranslationX, mAnimationStartTranslationY;

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

        void onViewAttached() {
            endAnimate();
            mFirstAttached = true;
        }

        void onViewDetached() {
            endAnimate();
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

        private TimeAnimator.TimeListener mTimeListener = new TimeAnimator.TimeListener() {
            @Override
            public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
                if (mView == null) {
                    return;
                }
                if (totalTime >= mDuration) {
                    endAnimate();
                } else {
                    float fraction = (float) (totalTime / (double)mDuration);
                    float fractionToEnd = 1 - mAnimator
                        .getInterpolator().getInterpolation(fraction);
                    mView.setTranslationX(fractionToEnd * mAnimationStartTranslationX);
                    mView.setTranslationY(fractionToEnd * mAnimationStartTranslationY);
                    invalidateItemDecoration();
                }
            }
        };

        void startAnimate(GridLayoutManager layout, View view, long startDelay) {
            if (mAnimator == null) {
                mAnimator = new TimeAnimator();
                mAnimator.setTimeListener(mTimeListener);
            }
            if (mFirstAttached) {
                // first time record the initial location and return without animation
                // TODO do we need initial animation?
                mViewX = layout.getScrollOffsetX() + getOpticalLeft(view);
                mViewY = layout.getScrollOffsetY() + getOpticalTop(view);
                mFirstAttached = false;
                return;
            }
            mView = view;
            int newViewX = layout.getScrollOffsetX() + getOpticalLeft(mView);
            int newViewY = layout.getScrollOffsetY() + getOpticalTop(mView);
            if (newViewX != mViewX || newViewY != mViewY) {
                mAnimator.cancel();
                mAnimationStartTranslationX = mView.getTranslationX();
                mAnimationStartTranslationY = mView.getTranslationY();
                mAnimationStartTranslationX += mViewX - newViewX;
                mAnimationStartTranslationY += mViewY - newViewY;
                mDuration = layout.getChildLayoutAnimationDuration();
                mAnimator.setDuration(mDuration);
                mAnimator.setInterpolator(layout.getChildLayoutAnimationInterpolator());
                mAnimator.setStartDelay(startDelay);
                mAnimator.start();
                mViewX = newViewX;
                mViewY = newViewY;
            }
        }

        void endAnimate() {
            if (mAnimator != null) {
                mAnimator.end();
            }
            if (mView != null) {
                mView.setTranslationX(0);
                mView.setTranslationY(0);
                mView = null;
            }
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

    private static final Interpolator sDefaultAnimationChildLayoutInterpolator
            = new DecelerateInterpolator();

    private static final long DEFAULT_CHILD_ANIMATION_DURATION_MS = 250;

    private String getTag() {
        return TAG + ":" + mBaseGridView.getId();
    }

    private final BaseGridView mBaseGridView;

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

    public void setGravity(int gravity) {
        mGravity = gravity;
    }

    protected boolean hasDoneFirstLayout() {
        return mGrid != null;
    }

    public void setOnChildSelectedListener(OnChildSelectedListener listener) {
        mChildSelectedListener = listener;
    }

    private int getPositionByView(View view) {
        return getPositionByIndex(mBaseGridView.indexOfChild(view));
    }

    private int getPositionByIndex(int index) {
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
            return NO_POSITION;
        }
        return position - mFirstVisiblePos;
    }

    private void dispatchChildSelected() {
        if (mChildSelectedListener == null) {
            return;
        }

        View view = getViewByPosition(mFocusPosition);

        if (mFocusPosition != NO_POSITION) {
            mChildSelectedListener.onChildSelected(mBaseGridView, view, mFocusPosition,
                    mAdapter.getItemId(mFocusPosition));
        } else {
            mChildSelectedListener.onChildSelected(mBaseGridView, null, NO_POSITION, NO_ID);
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
        View v = mRecycler.getViewForPosition(position);
        if (v != null) {
            ((LayoutParams) v.getLayoutParams()).onViewAttached();
        }
        return v;
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
        mFirstVisiblePos = mLastVisiblePos = NO_POSITION;

        initScrollController();

        return focusPosition;
    }

    @Override
    public void onMeasure(int widthSpec, int heightSpec) {
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
            setMeasuredDimension(sizePrimary, measuredSizeSecondary);
        } else {
            setMeasuredDimension(measuredSizeSecondary, sizePrimary);
        }
        if (DEBUG) {
            Log.v(getTag(), "onMeasure sizePrimary " + sizePrimary +
                    " measuredSizeSecondary " + measuredSizeSecondary +
                    " mItemLengthSecondary " + mItemLengthSecondary +
                    " mNumRows " + mNumRows);
        }
    }

    private void measureChild(View child) {
        final ViewGroup.LayoutParams lp = child.getLayoutParams();

        int widthSpec, heightSpec;
        if (mOrientation == HORIZONTAL) {
            widthSpec = ViewGroup.getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, lp.width);
            heightSpec = ViewGroup.getChildMeasureSpec(MeasureSpec.makeMeasureSpec(
                    mItemLengthSecondary, MeasureSpec.EXACTLY), 0, lp.height);
        } else {
            heightSpec = ViewGroup.getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, lp.height);
            widthSpec = ViewGroup.getChildMeasureSpec(MeasureSpec.makeMeasureSpec(
                    mItemLengthSecondary, MeasureSpec.EXACTLY), 0, lp.width);
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

            int length = mOrientation == HORIZONTAL ? v.getMeasuredWidth() : v.getMeasuredHeight();
            int start, end;
            if (append) {
                start = mRows[rowIndex].high;
                if (start != mRows[rowIndex].low) {
                    // if there are existing item in the row,  add margin between
                    start += mMarginPrimary;
                } else {
                    final int lastRow = mRows.length - 1;
                    if (lastRow != rowIndex && mRows[lastRow].high != mRows[lastRow].low) {
                        // if there are existing item in the last row, insert
                        // the new item after the last item of last row.
                        start = mRows[lastRow].high + mMarginPrimary;
                    }
                }
                end = start + length;
                mRows[rowIndex].high = end;
            } else {
                end = mRows[rowIndex].low;
                if (end != mRows[rowIndex].high) {
                    end -= mMarginPrimary;
                } else if (0 != rowIndex && mRows[0].high != mRows[0].low) {
                    // if there are existing item in the first row, insert
                    // the new item before the first item of first row.
                    end = mRows[0].low - mMarginPrimary;
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
         int measuredSecondary = mOrientation == HORIZONTAL ? v.getMeasuredHeight()
                 : v.getMeasuredWidth();
        if (measuredSecondary > mItemLengthSecondary) {
           measuredSecondary = mItemLengthSecondary;
        }
        final int verticalGravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
        final int horizontalGravity = mGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        if (mOrientation == HORIZONTAL && verticalGravity == Gravity.TOP
                || mOrientation == VERTICAL && horizontalGravity == Gravity.LEFT) {
            // do nothing
        } else if (mOrientation == HORIZONTAL && verticalGravity == Gravity.BOTTOM
                || mOrientation == VERTICAL && horizontalGravity == Gravity.RIGHT) {
            startSecondary += mItemLengthSecondary - measuredSecondary;
        } else if (mOrientation == HORIZONTAL && verticalGravity == Gravity.CENTER_VERTICAL
                || mOrientation == VERTICAL && horizontalGravity == Gravity.CENTER_HORIZONTAL) {
            startSecondary += (mItemLengthSecondary - measuredSecondary) / 2;
        }
        int left, top, right, bottom;
        if (mOrientation == HORIZONTAL) {
            left = start;
            top = startSecondary;
            right = end;
            bottom = startSecondary + measuredSecondary;
        } else {
            top = start;
            left = startSecondary;
            bottom = end;
            right = startSecondary + measuredSecondary;
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
            if (mLastVisiblePos != NO_POSITION && mLastVisiblePos < mAdapter.getItemCount() -1 &&
                    mLastVisiblePos < mGrid.getLastIndex()) {
                // append invisible view of saved location till last row
                final int index = mLastVisiblePos + 1;
                final int row = mGrid.getLocation(index).row;
                mGridProvider.createItem(index, row, true);
                if (row == mNumRows - 1) {
                    return false;
                }
            } else if ((mLastVisiblePos == NO_POSITION && mAdapter.getItemCount() > 0) ||
                    (mLastVisiblePos != NO_POSITION &&
                            mLastVisiblePos < mAdapter.getItemCount() - 1)) {
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
        View v = getViewByPosition(position);
        if (v != null) {
            if (DEBUG) {
                Log.d(getTag(), "removeAndRecycleViewAt " + position);
            }
            ((LayoutParams) v.getLayoutParams()).onViewDetached();
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

    // Fast layout when there is no structure change, adapter change, etc.
    protected void fastRelayout() {
        initScrollController();

        List<Integer>[] rows = mGrid.getItemPositionsInRows(mFirstVisiblePos, mLastVisiblePos);

        // relayout and repositioning views on each row
        for (int i = 0; i < mNumRows; i++) {
            List<Integer> row = rows[i];
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

        if (mFocusScrollStrategy == BaseGridView.FOCUS_SCROLL_ALIGNED) {
            View focusView = getViewByPosition(mFocusPosition == NO_POSITION ? 0 : mFocusPosition);
            scrollToView(focusView, false);
        }
    }

    // Lays out items based on the current scroll position
    public void onLayoutChildren(RecyclerView.Adapter adapter, RecyclerView.Recycler recycler,
            boolean structureChanged, RecyclerView.State state) {
        if (DEBUG) {
            Log.v(getTag(), "layoutChildren start numRows " + mNumRows + " mScrollOffsetSecondary "
                    + mScrollOffsetSecondary + " mScrollOffsetPrimary " + mScrollOffsetPrimary
                    + " structureChanged " + structureChanged
                    + " mForceFullLayout " + mForceFullLayout);
            Log.v(getTag(), "width " + getWidth() + " height " + getHeight());
        }

        if (mNumRows == 0) {
            // haven't done measure yet
            return;
        }
        final int itemCount = adapter.getItemCount();
        if (itemCount < 0) {
            return;
        }

        mInLayout = true;

        // Track the old focus view so we can adjust our system scroll position
        // so that any scroll animations happening now will remain valid.
        int delta = 0, deltaSecondary = 0;
        if (mFocusPosition != NO_POSITION
                && mFocusScrollStrategy == BaseGridView.FOCUS_SCROLL_ALIGNED) {
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
            boolean hadFocus = mBaseGridView.hasFocus();

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

        if (mFocusScrollStrategy == BaseGridView.FOCUS_SCROLL_ALIGNED) {
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
    public int scrollHorizontallyBy(int dx, Recycler recycler, RecyclerView.State state) {
        if (DEBUG) Log.v(TAG, "scrollHorizontallyBy " + dx);

        if (mOrientation == HORIZONTAL) {
            return scrollDirectionPrimary(dx);
        } else {
            return scrollDirectionSecondary(dx);
        }
    }

    @Override
    public int scrollVerticallyBy(int dy, Recycler recycler, RecyclerView.State state) {
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
        mBaseGridView.invalidate();
        return da;
    }

    // scroll in second direction will not add/prune views
    private int scrollDirectionSecondary(int dy) {
        offsetChildrenSecondary(-dy);
        mBaseGridView.invalidate();
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
            scrollToView(view, smooth);
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
                scrollToView(getViewByPosition(position), smooth);
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
        if (mFocusSearchDisabled) {
            return true;
        }
        if (!mInLayout) {
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
    private void scrollToView(View view, boolean smooth) {
        int newFocusPosition = getPositionByView(view);
        if (mInLayout || newFocusPosition != mFocusPosition) {
            mFocusPosition = newFocusPosition;
            dispatchChildSelected();
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
        switch (mFocusScrollStrategy) {
        case BaseGridView.FOCUS_SCROLL_ALIGNED:
        default:
            scrollToAlignedPosition(view, smooth);
            break;
        case BaseGridView.FOCUS_SCROLL_ITEM:
        case BaseGridView.FOCUS_SCROLL_PAGE:
            scrollItemOrPage(view, smooth);
            break;
        }
    }

    private void scrollItemOrPage(View view, boolean smooth) {
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
                    firstView = getViewByPosition(positions.get(0));
                    if (viewMax - getViewMin(firstView) > clientSize) {
                        if (positions.size() > 1) {
                            firstView = getViewByPosition(positions.get(1));
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
                    lastView = getViewByPosition(positions.get(positions.size() - 1));
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
        int viewCenterSecondary = mScrollOffsetSecondary +
                getViewCenterSecondary(secondaryAlignedView);
        mWindowAlignment.secondAxis().updateScrollCenter(viewCenterSecondary);
        scrollSecondary = mWindowAlignment.secondAxis().getSystemScrollPos();
        scrollSecondary -= mScrollOffsetSecondary;
        scrollGrid(scrollPrimary, scrollSecondary, smooth);
    }

    private void scrollToAlignedPosition(View view, boolean smooth) {
        int viewCenterPrimary = mScrollOffsetPrimary + getViewCenter(view);
        int viewCenterSecondary = mScrollOffsetSecondary + getViewCenterSecondary(view);
        if (DEBUG) {
            Log.v(getTag(), "scrollAligned smooth=" + smooth + " pos=" + mFocusPosition + " "
                    + viewCenterPrimary +","+viewCenterSecondary + " " + mWindowAlignment);
        }

        if (mInLayout || viewCenterPrimary != mWindowAlignment.mainAxis().getScrollCenter()
                || viewCenterSecondary != mWindowAlignment.secondAxis().getScrollCenter()) {
            mWindowAlignment.mainAxis().updateScrollCenter(viewCenterPrimary);
            mWindowAlignment.secondAxis().updateScrollCenter(viewCenterSecondary);
            int scrollPrimary = mWindowAlignment.mainAxis().getSystemScrollPos();
            int scrollSecondary = mWindowAlignment.secondAxis().getSystemScrollPos();
            if (DEBUG) {
                Log.v(getTag(), "scrollAligned " + scrollPrimary + " " + scrollSecondary
                        +" " + mWindowAlignment);
            }

            scrollPrimary -= mScrollOffsetPrimary;
            scrollSecondary -= mScrollOffsetSecondary;

            scrollGrid(scrollPrimary, scrollSecondary, smooth);
        }
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

    public void setAnimateChildLayout(boolean animateChildLayout) {
        mAnimateChildLayout = animateChildLayout;
        for (int i = 0, c = getChildCount(); i < c; i++) {
            View v = getChildAt(i);
            LayoutParams p = (LayoutParams) v.getLayoutParams();
            if (!mAnimateChildLayout) {
                p.endAnimate();
            } else {
                // record initial location values
                p.mFirstAttached = true;
                p.startAnimate(this, v, 0);
            }
        }
    }

    private void attemptAnimateLayoutChild() {
        if (!mAnimateChildLayout) {
            return;
        }
        for (int i = 0, c = getChildCount(); i < c; i++) {
            // TODO: start delay can be staggered
            View v = getChildAt(i);
            ((LayoutParams) v.getLayoutParams()).startAnimate(this, v, 0);
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
                getViewByPosition(focusedPos).addFocusables(views,  direction, focusableMode);
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
        View view = getViewByPosition(mFocusPosition);
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
        int focusIndex = getIndexByPosition(mFocusPosition);
        if (focusIndex == NO_POSITION) {
            return i;
        }
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
        mGrid = null;
        mRows = null;
        super.onAdapterChanged(oldAdapter, newAdapter);
    }
}
