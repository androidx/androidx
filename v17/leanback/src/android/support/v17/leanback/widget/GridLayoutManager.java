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
import android.support.v4.util.CircularIntArray;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Recycler;
import android.support.v7.widget.RecyclerView.State;
import android.support.v17.leanback.os.TraceHelper;

import static android.support.v7.widget.RecyclerView.NO_ID;
import static android.support.v7.widget.RecyclerView.NO_POSITION;
import static android.support.v7.widget.RecyclerView.HORIZONTAL;
import static android.support.v7.widget.RecyclerView.VERTICAL;

import android.util.AttributeSet;
import android.util.Log;
import android.view.FocusFinder;
import android.view.Gravity;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewGroup;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

final class GridLayoutManager extends RecyclerView.LayoutManager {

     /*
      * LayoutParams for {@link HorizontalGridView} and {@link VerticalGridView}.
      * The class currently does two internal jobs:
      * - Saves optical bounds insets.
      * - Caches focus align view center.
      */
    final static class LayoutParams extends RecyclerView.LayoutParams {

        // For placement
        private int mLeftInset;
        private int mTopInset;
        private int mRightInset;
        private int mBottomInset;

        // For alignment
        private int mAlignX;
        private int mAlignY;
        private int[] mAlignMultiple;
        private ItemAlignmentFacet mAlignmentFacet;

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
            return view.getRight() - mRightInset;
        }

        int getOpticalBottom(View view) {
            return view.getBottom() - mBottomInset;
        }

        int getOpticalWidth(View view) {
            return view.getWidth() - mLeftInset - mRightInset;
        }

        int getOpticalHeight(View view) {
            return view.getHeight() - mTopInset - mBottomInset;
        }

        int getOpticalLeftInset() {
            return mLeftInset;
        }

        int getOpticalRightInset() {
            return mRightInset;
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

        void setItemAlignmentFacet(ItemAlignmentFacet facet) {
            mAlignmentFacet = facet;
        }

        ItemAlignmentFacet getItemAlignmentFacet() {
            return mAlignmentFacet;
        }

        void calculateItemAlignments(int orientation, View view) {
            ItemAlignmentFacet.ItemAlignmentDef[] defs = mAlignmentFacet.getAlignmentDefs();
            if (mAlignMultiple == null || mAlignMultiple.length != defs.length) {
                mAlignMultiple = new int[defs.length];
            }
            for (int i = 0; i < defs.length; i++) {
                mAlignMultiple[i] = ItemAlignmentFacetHelper
                        .getAlignmentPosition(view, defs[i], orientation);
            }
            if (orientation == HORIZONTAL) {
                mAlignX = mAlignMultiple[0];
            } else {
                mAlignY = mAlignMultiple[0];
            }
        }

        int[] getAlignMultiple() {
            return mAlignMultiple;
        }

        void setOpticalInsets(int leftInset, int topInset, int rightInset, int bottomInset) {
            mLeftInset = leftInset;
            mTopInset = topInset;
            mRightInset = rightInset;
            mBottomInset = bottomInset;
        }

    }

    /**
     * Base class which scrolls to selected view in onStop().
     */
    abstract class GridLinearSmoothScroller extends LinearSmoothScroller {
        GridLinearSmoothScroller() {
            super(mBaseGridView.getContext());
        }

        @Override
        protected void onStop() {
            // onTargetFound() may not be called if we hit the "wall" first or get cancelled.
            View targetView = findViewByPosition(getTargetPosition());
            if (targetView == null) {
                if (getTargetPosition() >= 0) {
                    // if smooth scroller is stopped without target, immediately jumps
                    // to the target position.
                    scrollToSelection(getTargetPosition(), 0, false, 0);
                }
                super.onStop();
                return;
            }
            if (hasFocus()) {
                mInSelection = true;
                targetView.requestFocus();
                mInSelection = false;
            }
            dispatchChildSelected();
            super.onStop();
        }

        @Override
        protected int calculateTimeForScrolling(int dx) {
            int ms = super.calculateTimeForScrolling(dx);
            if (mWindowAlignment.mainAxis().getSize() > 0) {
                float minMs = (float) MIN_MS_SMOOTH_SCROLL_MAIN_SCREEN /
                        mWindowAlignment.mainAxis().getSize() * dx;
                if (ms < minMs) {
                    ms = (int) minMs;
                }
            }
            return ms;
        }

        @Override
        protected void onTargetFound(View targetView,
                RecyclerView.State state, Action action) {
            if (getScrollPosition(targetView, null, sTwoInts)) {
                int dx, dy;
                if (mOrientation == HORIZONTAL) {
                    dx = sTwoInts[0];
                    dy = sTwoInts[1];
                } else {
                    dx = sTwoInts[1];
                    dy = sTwoInts[0];
                }
                final int distance = (int) Math.sqrt(dx * dx + dy * dy);
                final int time = calculateTimeForDeceleration(distance);
                action.update(dx, dy, time, mDecelerateInterpolator);
            }
        }
    }

    /**
     * The SmoothScroller that remembers pending DPAD keys and consume pending keys
     * during scroll.
     */
    final class PendingMoveSmoothScroller extends GridLinearSmoothScroller {
        // -2 is a target position that LinearSmoothScroller can never find until
        // consumePendingMovesXXX() sets real targetPosition.
        final static int TARGET_UNDEFINED = -2;
        // whether the grid is staggered.
        private final boolean mStaggeredGrid;
        // Number of pending movements on primary direction, negative if PREV_ITEM.
        private int mPendingMoves;

        PendingMoveSmoothScroller(int initialPendingMoves, boolean staggeredGrid) {
            mPendingMoves = initialPendingMoves;
            mStaggeredGrid = staggeredGrid;
            setTargetPosition(TARGET_UNDEFINED);
        }

        void increasePendingMoves() {
            if (mPendingMoves < MAX_PENDING_MOVES) {
                mPendingMoves++;
            }
        }

        void decreasePendingMoves() {
            if (mPendingMoves > -MAX_PENDING_MOVES) {
                mPendingMoves--;
            }
        }

        /**
         * Called before laid out an item when non-staggered grid can handle pending movements
         * by skipping "mNumRows" per movement;  staggered grid will have to wait the item
         * has been laid out in consumePendingMovesAfterLayout().
         */
        void consumePendingMovesBeforeLayout() {
            if (mStaggeredGrid || mPendingMoves == 0) {
                return;
            }
            View newSelected = null;
            int startPos = mPendingMoves > 0 ? mFocusPosition + mNumRows :
                    mFocusPosition - mNumRows;
            for (int pos = startPos; mPendingMoves != 0;
                    pos = mPendingMoves > 0 ? pos + mNumRows: pos - mNumRows) {
                View v = findViewByPosition(pos);
                if (v == null) {
                    break;
                }
                if (!canScrollTo(v)) {
                    continue;
                }
                newSelected = v;
                mFocusPosition = pos;
                mSubFocusPosition = 0;
                if (mPendingMoves > 0) {
                    mPendingMoves--;
                } else {
                    mPendingMoves++;
                }
            }
            if (newSelected != null && hasFocus()) {
                mInSelection = true;
                newSelected.requestFocus();
                mInSelection = false;
            }
        }

        /**
         * Called after laid out an item.  Staggered grid should find view on same
         * Row and consume pending movements.
         */
        void consumePendingMovesAfterLayout() {
            if (mStaggeredGrid && mPendingMoves != 0) {
                // consume pending moves, focus to item on the same row.
                mPendingMoves = processSelectionMoves(true, mPendingMoves);
            }
            if (mPendingMoves == 0 || (mPendingMoves > 0 && hasCreatedLastItem())
                    || (mPendingMoves < 0 && hasCreatedFirstItem())) {
                setTargetPosition(mFocusPosition);
                stop();
            }
        }

        @Override
        protected void updateActionForInterimTarget(Action action) {
            if (mPendingMoves == 0) {
                return;
            }
            super.updateActionForInterimTarget(action);
        }

        @Override
        public PointF computeScrollVectorForPosition(int targetPosition) {
            if (mPendingMoves == 0) {
                return null;
            }
            int direction = (mReverseFlowPrimary ? mPendingMoves > 0 : mPendingMoves < 0) ?
                    -1 : 1;
            if (mOrientation == HORIZONTAL) {
                return new PointF(direction, 0);
            } else {
                return new PointF(0, direction);
            }
        }

        @Override
        protected void onStop() {
            super.onStop();
            // if we hit wall,  need clear the remaining pending moves.
            mPendingMoves = 0;
            mPendingMoveSmoothScroller = null;
            View v = findViewByPosition(getTargetPosition());
            if (v != null) scrollToView(v, true);
        }
    };

    private static final String TAG = "GridLayoutManager";
    private static final boolean DEBUG = false;
    private static final boolean TRACE = false;

    // maximum pending movement in one direction.
    private final static int MAX_PENDING_MOVES = 10;
    // minimal milliseconds to scroll window size in major direction,  we put a cap to prevent the
    // effect smooth scrolling too over to bind an item view then drag the item view back.
    private final static int MIN_MS_SMOOTH_SCROLL_MAIN_SCREEN = 30;

    private String getTag() {
        return TAG + ":" + mBaseGridView.getId();
    }

    private final BaseGridView mBaseGridView;

    /**
     * Note on conventions in the presence of RTL layout directions:
     * Many properties and method names reference entities related to the
     * beginnings and ends of things.  In the presence of RTL flows,
     * it may not be clear whether this is intended to reference a
     * quantity that changes direction in RTL cases, or a quantity that
     * does not.  Here are the conventions in use:
     *
     * start/end: coordinate quantities - do reverse
     * (optical) left/right: coordinate quantities - do not reverse
     * low/high: coordinate quantities - do not reverse
     * min/max: coordinate quantities - do not reverse
     * scroll offset - coordinate quantities - do not reverse
     * first/last: positional indices - do not reverse
     * front/end: positional indices - do not reverse
     * prepend/append: related to positional indices - do not reverse
     *
     * Note that although quantities do not reverse in RTL flows, their
     * relationship does.  In LTR flows, the first positional index is
     * leftmost; in RTL flows, it is rightmost.  Thus, anywhere that
     * positional quantities are mapped onto coordinate quantities,
     * the flow must be checked and the logic reversed.
     */

    /**
     * The orientation of a "row".
     */
    private int mOrientation = HORIZONTAL;
    private OrientationHelper mOrientationHelper = OrientationHelper.createHorizontalHelper(this);

    private RecyclerView.State mState;
    private RecyclerView.Recycler mRecycler;

    private static final Rect sTempRect = new Rect();

    private boolean mInLayout;
    private boolean mInScroll;
    private boolean mInFastRelayout;
    /**
     * During full layout pass, when GridView had focus: onLayoutChildren will
     * skip non-focusable child and adjust mFocusPosition.
     */
    private boolean mInLayoutSearchFocus;
    private boolean mInSelection = false;

    private OnChildSelectedListener mChildSelectedListener = null;

    private ArrayList<OnChildViewHolderSelectedListener> mChildViewHolderSelectedListeners = null;

    private OnChildLaidOutListener mChildLaidOutListener = null;

    /**
     * The focused position, it's not the currently visually aligned position
     * but it is the final position that we intend to focus on. If there are
     * multiple setSelection() called, mFocusPosition saves last value.
     */
    private int mFocusPosition = NO_POSITION;

    /**
     * A view can have mutliple alignment position,  this is the index of which
     * alignment is used,  by default is 0.
     */
    private int mSubFocusPosition = 0;

    /**
     * LinearSmoothScroller that consume pending DPAD movements.
     */
    private PendingMoveSmoothScroller mPendingMoveSmoothScroller;

    /**
     * The offset to be applied to mFocusPosition, due to adapter change, on the next
     * layout.  Set to Integer.MIN_VALUE means we should stop adding delta to mFocusPosition
     * until next layout cycler.
     * TODO:  This is somewhat duplication of RecyclerView getOldPosition() which is
     * unfortunately cleared after prelayout.
     */
    private int mFocusPositionOffset = 0;

    /**
     * Extra pixels applied on primary direction.
     */
    private int mPrimaryScrollExtra;

    /**
     * Force a full layout under certain situations.  E.g. Rows change, jump to invisible child.
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
    private int mGravity = Gravity.START | Gravity.TOP;
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
     * Saves grid information of each view.
     */
    Grid mGrid;

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
     * Pixels of extra space for layout item (outside the widget)
     */
    private int mExtraLayoutSpace;

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
     *  Allow DPAD key to navigate out of second axis.
     *  default is true.
     */
    private boolean mFocusOutSideStart = true;

    /**
     * Allow DPAD key to navigate out of second axis.
     */
    private boolean mFocusOutSideEnd = true;

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
     * Temporary variable: an int array of length=2.
     */
    private static int[] sTwoInts = new int[2];

    /**
     * Set to true for RTL layout in horizontal orientation
     */
    private boolean mReverseFlowPrimary = false;

    /**
     * Set to true for RTL layout in vertical orientation
     */
    private boolean mReverseFlowSecondary = false;

    /**
     * Temporaries used for measuring.
     */
    private int[] mMeasuredDimension = new int[2];

    final ViewsStateBundle mChildrenStates = new ViewsStateBundle();

    /**
     * Optional interface implemented by Adapter.
     */
    private FacetProviderAdapter mFacetProviderAdapter;

    public GridLayoutManager(BaseGridView baseGridView) {
        mBaseGridView = baseGridView;
    }

    public void setOrientation(int orientation) {
        if (orientation != HORIZONTAL && orientation != VERTICAL) {
            if (DEBUG) Log.v(getTag(), "invalid orientation: " + orientation);
            return;
        }

        mOrientation = orientation;
        mOrientationHelper = OrientationHelper.createOrientationHelper(this, mOrientation);
        mWindowAlignment.setOrientation(orientation);
        mItemAlignment.setOrientation(orientation);
        mForceFullLayout = true;
    }

    public void onRtlPropertiesChanged(int layoutDirection) {
        if (mOrientation == HORIZONTAL) {
            mReverseFlowPrimary = layoutDirection == View.LAYOUT_DIRECTION_RTL;
            mReverseFlowSecondary = false;
        } else {
            mReverseFlowSecondary = layoutDirection == View.LAYOUT_DIRECTION_RTL;
            mReverseFlowPrimary = false;
        }
        mWindowAlignment.horizontal.setReversedFlow(layoutDirection == View.LAYOUT_DIRECTION_RTL);
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

    public void setFocusOutSideAllowed(boolean throughStart, boolean throughEnd) {
        mFocusOutSideStart = throughStart;
        mFocusOutSideEnd = throughEnd;
    }

    public void setNumRows(int numRows) {
        if (numRows < 0) throw new IllegalArgumentException();
        mNumRowsRequested = numRows;
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

    public void setOnChildViewHolderSelectedListener(OnChildViewHolderSelectedListener listener) {
        if (listener == null) {
            mChildViewHolderSelectedListeners = null;
            return;
        }
        if (mChildViewHolderSelectedListeners == null) {
            mChildViewHolderSelectedListeners = new ArrayList<OnChildViewHolderSelectedListener>();
        } else {
            mChildViewHolderSelectedListeners.clear();
        }
        mChildViewHolderSelectedListeners.add(listener);
    }

    public void addOnChildViewHolderSelectedListener(OnChildViewHolderSelectedListener listener) {
        if (mChildViewHolderSelectedListeners == null) {
            mChildViewHolderSelectedListeners = new ArrayList<OnChildViewHolderSelectedListener>();
        }
        mChildViewHolderSelectedListeners.add(listener);
    }

    public void removeOnChildViewHolderSelectedListener(OnChildViewHolderSelectedListener
            listener) {
        if (mChildViewHolderSelectedListeners != null) {
            mChildViewHolderSelectedListeners.remove(listener);
        }
    }

    boolean hasOnChildViewHolderSelectedListener() {
        return mChildViewHolderSelectedListeners != null &&
                mChildViewHolderSelectedListeners.size() > 0;
    }

    void fireOnChildViewHolderSelected(RecyclerView parent, RecyclerView.ViewHolder child,
            int position, int subposition) {
        if (mChildViewHolderSelectedListeners == null) {
            return;
        }
        for (int i = mChildViewHolderSelectedListeners.size() - 1; i >= 0 ; i--) {
            mChildViewHolderSelectedListeners.get(i).onChildViewHolderSelected(parent, child,
                    position, subposition);
        }
    }

    void setOnChildLaidOutListener(OnChildLaidOutListener listener) {
        mChildLaidOutListener = listener;
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

    private int getSubPositionByView(View view, View childView) {
        if (view == null || childView == null) {
            return 0;
        }
        final LayoutParams lp = (LayoutParams) view.getLayoutParams();
        final ItemAlignmentFacet facet = lp.getItemAlignmentFacet();
        if (facet != null) {
            final ItemAlignmentFacet.ItemAlignmentDef[] defs = facet.getAlignmentDefs();
            if (defs.length > 1) {
                while (childView != view) {
                    int id = childView.getId();
                    if (id != View.NO_ID) {
                        for (int i = 1; i < defs.length; i++) {
                            if (defs[i].getItemAlignmentFocusViewId() == id) {
                                return i;
                            }
                        }
                    }
                    childView = (View) childView.getParent();
                }
            }
        }
        return 0;
    }

    private int getPositionByIndex(int index) {
        return getPositionByView(getChildAt(index));
    }

    private void dispatchChildSelected() {
        if (mChildSelectedListener == null && !hasOnChildViewHolderSelectedListener()) {
            return;
        }

        if (TRACE) TraceHelper.beginSection("onChildSelected");
        View view = mFocusPosition == NO_POSITION ? null : findViewByPosition(mFocusPosition);
        if (view != null) {
            RecyclerView.ViewHolder vh = mBaseGridView.getChildViewHolder(view);
            if (mChildSelectedListener != null) {
                mChildSelectedListener.onChildSelected(mBaseGridView, view, mFocusPosition,
                        vh == null? NO_ID: vh.getItemId());
            }
            fireOnChildViewHolderSelected(mBaseGridView, vh, mFocusPosition, mSubFocusPosition);
        } else {
            if (mChildSelectedListener != null) {
                mChildSelectedListener.onChildSelected(mBaseGridView, null, NO_POSITION, NO_ID);
            }
            fireOnChildViewHolderSelected(mBaseGridView, null, NO_POSITION, 0);
        }
        if (TRACE) TraceHelper.endSection();

        // Children may request layout when a child selection event occurs (such as a change of
        // padding on the current and previously selected rows).
        // If in layout, a child requesting layout may have been laid out before the selection
        // callback.
        // If it was not, the child will be laid out after the selection callback.
        // If so, the layout request will be honoured though the view system will emit a double-
        // layout warning.
        // If not in layout, we may be scrolling in which case the child layout request will be
        // eaten by recyclerview.  Post a requestLayout.
        if (!mInLayout && !mBaseGridView.isLayoutRequested()) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (getChildAt(i).isLayoutRequested()) {
                    forceRequestLayout();
                    break;
                }
            }
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

    @Override
    public int getDecoratedLeft(View child) {
        return super.getDecoratedLeft(child) + ((LayoutParams) child.getLayoutParams()).mLeftInset;
    }

    @Override
    public int getDecoratedTop(View child) {
        return super.getDecoratedTop(child) + ((LayoutParams) child.getLayoutParams()).mTopInset;
    }

    @Override
    public int getDecoratedRight(View child) {
        return super.getDecoratedRight(child) -
                ((LayoutParams) child.getLayoutParams()).mRightInset;
    }

    @Override
    public int getDecoratedBottom(View child) {
        return super.getDecoratedBottom(child) -
                ((LayoutParams) child.getLayoutParams()).mBottomInset;
    }

    @Override
    public void getDecoratedBoundsWithMargins(View view, Rect outBounds) {
        super.getDecoratedBoundsWithMargins(view, outBounds);
        LayoutParams params = ((LayoutParams) view.getLayoutParams());
        outBounds.left += params.mLeftInset;
        outBounds.top += params.mTopInset;
        outBounds.right -= params.mRightInset;
        outBounds.bottom -= params.mBottomInset;
    }

    private int getViewMin(View v) {
        return mOrientationHelper.getDecoratedStart(v);
    }

    private int getViewMax(View v) {
        return mOrientationHelper.getDecoratedEnd(v);
    }

    private int getViewPrimarySize(View view) {
        getDecoratedBoundsWithMargins(view, sTempRect);
        return mOrientation == HORIZONTAL ? sTempRect.width() : sTempRect.height();
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
     * @return true if can fastRelayout()
     */
    private boolean layoutInit() {
        boolean focusViewWasInTree = mGrid != null && mFocusPosition >= 0
                && mFocusPosition >= mGrid.getFirstVisibleIndex()
                && mFocusPosition <= mGrid.getLastVisibleIndex();
        final int newItemCount = mState.getItemCount();
        if (newItemCount == 0) {
            mFocusPosition = NO_POSITION;
            mSubFocusPosition = 0;
        } else if (mFocusPosition >= newItemCount) {
            mFocusPosition = newItemCount - 1;
            mSubFocusPosition = 0;
        } else if (mFocusPosition == NO_POSITION && newItemCount > 0) {
            // if focus position is never set before,  initialize it to 0
            mFocusPosition = 0;
            mSubFocusPosition = 0;
        }
        if (!mState.didStructureChange() && mGrid.getFirstVisibleIndex() >= 0 &&
                !mForceFullLayout && mGrid != null && mGrid.getNumRows() == mNumRows) {
            updateScrollController();
            updateScrollSecondAxis();
            mGrid.setMargin(mMarginPrimary);
            if (!focusViewWasInTree && mFocusPosition != NO_POSITION) {
                mGrid.setStart(mFocusPosition);
            }
            return true;
        } else {
            mForceFullLayout = false;
            int firstVisibleIndex = focusViewWasInTree ? mGrid.getFirstVisibleIndex() : 0;

            if (mGrid == null || mNumRows != mGrid.getNumRows() ||
                    mReverseFlowPrimary != mGrid.isReversedFlow()) {
                mGrid = Grid.createGrid(mNumRows);
                mGrid.setProvider(mGridProvider);
                mGrid.setReversedFlow(mReverseFlowPrimary);
            }
            initScrollController();
            updateScrollSecondAxis();
            mGrid.setMargin(mMarginPrimary);
            detachAndScrapAttachedViews(mRecycler);
            mGrid.resetVisibleIndex();
            if (mFocusPosition == NO_POSITION) {
                mBaseGridView.clearFocus();
            }
            mWindowAlignment.mainAxis().invalidateScrollMin();
            mWindowAlignment.mainAxis().invalidateScrollMax();
            if (focusViewWasInTree && firstVisibleIndex <= mFocusPosition) {
                // if focusView was in tree, we will add item from first visible item
                mGrid.setStart(firstVisibleIndex);
            } else {
                // if focusView was not in tree, it's probably because focus position jumped
                // far away from visible range,  so use mFocusPosition as start
                mGrid.setStart(mFocusPosition);
            }
            return false;
        }
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
        // Iterate from left to right, which is a different index traversal
        // in RTL flow
        if (mReverseFlowSecondary) {
            for (int i = mNumRows-1; i > rowIndex; i--) {
                start += getRowSizeSecondary(i) + mMarginSecondary;
            }
        } else {
            for (int i = 0; i < rowIndex; i++) {
                start += getRowSizeSecondary(i) + mMarginSecondary;
            }
        }
        return start;
    }

    private int getSizeSecondary() {
        int rightmostIndex = mReverseFlowSecondary ? 0 : mNumRows - 1;
        return getRowStartSecondary(rightmostIndex) + getRowSizeSecondary(rightmostIndex);
    }

    int getDecoratedMeasuredWidthWithMargin(View v) {
        final LayoutParams lp = (LayoutParams) v.getLayoutParams();
        return getDecoratedMeasuredWidth(v) + lp.leftMargin + lp.rightMargin;
    }

    int getDecoratedMeasuredHeightWithMargin(View v) {
        final LayoutParams lp = (LayoutParams) v.getLayoutParams();
        return getDecoratedMeasuredHeight(v) + lp.topMargin + lp.bottomMargin;
    }

    private void measureScrapChild(int position, int widthSpec, int heightSpec,
            int[] measuredDimension) {
        View view = mRecycler.getViewForPosition(position);
        if (view != null) {
            final LayoutParams p = (LayoutParams) view.getLayoutParams();
            calculateItemDecorationsForChild(view, sTempRect);
            int widthUsed = p.leftMargin + p.rightMargin + sTempRect.left + sTempRect.right;
            int heightUsed = p.topMargin + p.bottomMargin + sTempRect.top + sTempRect.bottom;

            int childWidthSpec = ViewGroup.getChildMeasureSpec(widthSpec,
                    getPaddingLeft() + getPaddingRight() + widthUsed, p.width);
            int childHeightSpec = ViewGroup.getChildMeasureSpec(heightSpec,
                    getPaddingTop() + getPaddingBottom() + heightUsed, p.height);
            view.measure(childWidthSpec, childHeightSpec);

            measuredDimension[0] = getDecoratedMeasuredWidthWithMargin(view);
            measuredDimension[1] = getDecoratedMeasuredHeightWithMargin(view);
            mRecycler.recycleView(view);
        }
    }

    private boolean processRowSizeSecondary(boolean measure) {
        if (mFixedRowSizeSecondary != 0 || mRowSizeSecondary == null) {
            return false;
        }

        if (TRACE) TraceHelper.beginSection("processRowSizeSecondary");
        CircularIntArray[] rows = mGrid == null ? null : mGrid.getItemPositionsInRows();
        boolean changed = false;
        int scrapChildWidth = -1;
        int scrapChildHeight = -1;

        for (int rowIndex = 0; rowIndex < mNumRows; rowIndex++) {
            CircularIntArray row = rows == null ? null : rows[rowIndex];
            final int rowItemsPairCount = row == null ? 0 : row.size();
            int rowSize = -1;
            for (int rowItemPairIndex = 0; rowItemPairIndex < rowItemsPairCount;
                    rowItemPairIndex += 2) {
                final int rowIndexStart = row.get(rowItemPairIndex);
                final int rowIndexEnd = row.get(rowItemPairIndex + 1);
                for (int i = rowIndexStart; i <= rowIndexEnd; i++) {
                    final View view = findViewByPosition(i);
                    if (view == null) {
                        continue;
                    }
                    if (measure) {
                        measureChild(view);
                    }
                    final int secondarySize = mOrientation == HORIZONTAL ?
                            getDecoratedMeasuredHeightWithMargin(view)
                            : getDecoratedMeasuredWidthWithMargin(view);
                    if (secondarySize > rowSize) {
                        rowSize = secondarySize;
                    }
                }
            }

            final int itemCount = mState.getItemCount();
            if (!mBaseGridView.hasFixedSize() && measure && rowSize < 0 && itemCount > 0) {
                if (scrapChildWidth < 0 && scrapChildHeight < 0) {
                    int position;
                    if (mFocusPosition == NO_POSITION) {
                        position = 0;
                    } else if (mFocusPosition >= itemCount) {
                        position = itemCount - 1;
                    } else {
                        position = mFocusPosition;
                    }
                    measureScrapChild(position,
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
            if (mRowSizeSecondary[rowIndex] != rowSize) {
                if (DEBUG) Log.v(getTag(), "row size secondary changed: " + mRowSizeSecondary[rowIndex] +
                        ", " + rowSize);
                mRowSizeSecondary[rowIndex] = rowSize;
                changed = true;
            }
        }

        if (TRACE) TraceHelper.endSection();
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

    private final Runnable mAskFocusRunnable = new Runnable() {
        @Override
        public void run() {
            if (hasFocus()) {
                return;
            }
            View view = findViewByPosition(mFocusPosition);
            if (view != null && view.hasFocusable()) {
                mBaseGridView.focusableViewAvailable(view);
                return;
            }
            for (int i = 0, count = getChildCount(); i < count; i++) {
                view = getChildAt(i);
                if (view != null && view.hasFocusable()) {
                    mBaseGridView.focusableViewAvailable(view);
                    break;
                }
            }
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
                mFixedRowSizeSecondary = mRowSizeSecondaryRequested == 0 ?
                        sizeSecondary - paddingSecondary: mRowSizeSecondaryRequested;
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
        if (TRACE) TraceHelper.beginSection("measureChild");
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        calculateItemDecorationsForChild(child, sTempRect);
        int widthUsed = lp.leftMargin + lp.rightMargin + sTempRect.left + sTempRect.right;
        int heightUsed = lp.topMargin + lp.bottomMargin + sTempRect.top + sTempRect.bottom;

        final int secondarySpec = (mRowSizeSecondaryRequested == ViewGroup.LayoutParams.WRAP_CONTENT) ?
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED) :
                MeasureSpec.makeMeasureSpec(mFixedRowSizeSecondary, MeasureSpec.EXACTLY);
        int widthSpec, heightSpec;

        if (mOrientation == HORIZONTAL) {
            widthSpec = ViewGroup.getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), widthUsed, lp.width);
            heightSpec = ViewGroup.getChildMeasureSpec(secondarySpec, heightUsed, lp.height);
        } else {
            heightSpec = ViewGroup.getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), heightUsed, lp.height);
            widthSpec = ViewGroup.getChildMeasureSpec(secondarySpec, widthUsed, lp.width);
        }
        child.measure(widthSpec, heightSpec);
        if (DEBUG) Log.v(getTag(), "measureChild secondarySpec " + Integer.toHexString(secondarySpec) +
                " widthSpec " + Integer.toHexString(widthSpec) +
                " heightSpec " + Integer.toHexString(heightSpec) +
                " measuredWidth " + child.getMeasuredWidth() +
                " measuredHeight " + child.getMeasuredHeight());
        if (DEBUG) Log.v(getTag(), "child lp width " + lp.width + " height " + lp.height);
        if (TRACE) TraceHelper.endSection();
    }

    /**
     * Get facet from the ViewHolder or the viewType. 
     */
    private <E> E getFacet(RecyclerView.ViewHolder vh, Class<? extends E> facetClass) {
        E facet = null;
        if (vh instanceof FacetProvider) {
            facet = (E) ((FacetProvider) vh).getFacet(facetClass);
        }
        if (facet == null && mFacetProviderAdapter != null) {
            FacetProvider p = mFacetProviderAdapter.getFacetProvider(vh.getItemViewType());
            if (p != null) {
                facet = (E) p.getFacet(facetClass);
            }
        }
        return facet;
    }

    private Grid.Provider mGridProvider = new Grid.Provider() {

        @Override
        public int getCount() {
            return mState.getItemCount();
        }

        @Override
        public int createItem(int index, boolean append, Object[] item) {
            if (TRACE) TraceHelper.beginSection("createItem");
            if (TRACE) TraceHelper.beginSection("getview");
            View v = getViewForPosition(index);
            if (TRACE) TraceHelper.endSection();
            LayoutParams lp = (LayoutParams) v.getLayoutParams();
            RecyclerView.ViewHolder vh = mBaseGridView.getChildViewHolder(v);
            lp.setItemAlignmentFacet((ItemAlignmentFacet)getFacet(vh, ItemAlignmentFacet.class));
            // See recyclerView docs:  we don't need re-add scraped view if it was removed.
            if (!lp.isItemRemoved()) {
                if (TRACE) TraceHelper.beginSection("addView");
                if (append) {
                    addView(v);
                } else {
                    addView(v, 0);
                }
                if (TRACE) TraceHelper.endSection();
                if (mChildVisibility != -1) {
                    v.setVisibility(mChildVisibility);
                }

                if (mPendingMoveSmoothScroller != null) {
                    mPendingMoveSmoothScroller.consumePendingMovesBeforeLayout();
                }
                int subindex = getSubPositionByView(v, v.findFocus());
                if (!mInLayout) {
                    // when we are appending item during scroll pass and the item's position
                    // matches the mFocusPosition,  we should signal a childSelected event.
                    // However if we are still running PendingMoveSmoothScroller,  we defer and
                    // signal the event in PendingMoveSmoothScroller.onStop().  This can
                    // avoid lots of childSelected events during a long smooth scrolling and
                    // increase performance.
                    if (index == mFocusPosition && subindex == mSubFocusPosition
                            && mPendingMoveSmoothScroller == null) {
                        dispatchChildSelected();
                    }
                } else if (!mInFastRelayout) {
                    // fastRelayout will dispatch event at end of onLayoutChildren().
                    // For full layout, two situations here:
                    // 1. mInLayoutSearchFocus is false, dispatchChildSelected() at mFocusPosition.
                    // 2. mInLayoutSearchFocus is true:  dispatchChildSelected() on first child
                    //    equal to or after mFocusPosition that can take focus.
                    if (!mInLayoutSearchFocus && index == mFocusPosition
                            && subindex == mSubFocusPosition) {
                        dispatchChildSelected();
                    } else if (mInLayoutSearchFocus && index >= mFocusPosition
                            && v.hasFocusable()) {
                        mFocusPosition = index;
                        mSubFocusPosition = subindex;
                        mInLayoutSearchFocus = false;
                        dispatchChildSelected();
                    }
                }
                measureChild(v);
            }
            item[0] = v;
            return mOrientation == HORIZONTAL ? getDecoratedMeasuredWidthWithMargin(v)
                    : getDecoratedMeasuredHeightWithMargin(v);
        }

        @Override
        public void addItem(Object item, int index, int length, int rowIndex, int edge) {
            View v = (View) item;
            int start, end;
            if (edge == Integer.MIN_VALUE || edge == Integer.MAX_VALUE) {
                edge = !mGrid.isReversedFlow() ? mWindowAlignment.mainAxis().getPaddingLow()
                        : mWindowAlignment.mainAxis().getSize()
                                - mWindowAlignment.mainAxis().getPaddingHigh();
            }
            boolean edgeIsMin = !mGrid.isReversedFlow();
            if (edgeIsMin) {
                start = edge;
                end = edge + length;
            } else {
                start = edge - length;
                end = edge;
            }
            int startSecondary = getRowStartSecondary(rowIndex) - mScrollOffsetSecondary;
            mChildrenStates.loadView(v, index);
            layoutChild(rowIndex, v, start, end, startSecondary);
            if (DEBUG) {
                Log.d(getTag(), "addView " + index + " " + v);
            }
            if (TRACE) TraceHelper.endSection();

            if (index == mGrid.getFirstVisibleIndex()) {
                if (!mGrid.isReversedFlow()) {
                    updateScrollMin();
                } else {
                    updateScrollMax();
                }
            }
            if (index == mGrid.getLastVisibleIndex()) {
                if (!mGrid.isReversedFlow()) {
                    updateScrollMax();
                } else {
                    updateScrollMin();
                }
            }
            if (!mInLayout && mPendingMoveSmoothScroller != null) {
                mPendingMoveSmoothScroller.consumePendingMovesAfterLayout();
            }
            if (mChildLaidOutListener != null) {
                RecyclerView.ViewHolder vh = mBaseGridView.getChildViewHolder(v);
                mChildLaidOutListener.onChildLaidOut(mBaseGridView, v, index,
                        vh == null ? NO_ID : vh.getItemId());
            }
        }

        @Override
        public void removeItem(int index) {
            if (TRACE) TraceHelper.beginSection("removeItem");
            View v = findViewByPosition(index);
            if (mInLayout) {
                detachAndScrapView(v, mRecycler);
            } else {
                removeAndRecycleView(v, mRecycler);
            }
            if (TRACE) TraceHelper.endSection();
        }

        @Override
        public int getEdge(int index) {
            if (mReverseFlowPrimary) {
                return getViewMax(findViewByPosition(index));
            } else {
                return getViewMin(findViewByPosition(index));
            }
        }

        @Override
        public int getSize(int index) {
            return getViewPrimarySize(findViewByPosition(index));
        }
    };

    private void layoutChild(int rowIndex, View v, int start, int end, int startSecondary) {
        if (TRACE) TraceHelper.beginSection("layoutChild");
        int sizeSecondary = mOrientation == HORIZONTAL ? getDecoratedMeasuredHeightWithMargin(v)
                : getDecoratedMeasuredWidthWithMargin(v);
        if (mFixedRowSizeSecondary > 0) {
            sizeSecondary = Math.min(sizeSecondary, mFixedRowSizeSecondary);
        }
        final int verticalGravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
        final int horizontalGravity = (mReverseFlowPrimary || mReverseFlowSecondary) ?
                Gravity.getAbsoluteGravity(mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK, View.LAYOUT_DIRECTION_RTL) :
                mGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
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
        LayoutParams params = (LayoutParams) v.getLayoutParams();
        layoutDecoratedWithMargins(v, left, top, right, bottom);
        // Now super.getDecoratedBoundsWithMargins() includes the extra space for optical bounds,
        // subtracting it from value passed in layoutDecoratedWithMargins(), we can get the optical
        // bounds insets.
        super.getDecoratedBoundsWithMargins(v, sTempRect);
        params.setOpticalInsets(left - sTempRect.left, top - sTempRect.top,
                sTempRect.right - right, sTempRect.bottom - bottom);
        updateChildAlignments(v);
        if (TRACE) TraceHelper.endSection();
    }

    private void updateChildAlignments(View v) {
        final LayoutParams p = (LayoutParams) v.getLayoutParams();
        if (p.getItemAlignmentFacet() == null) {
            // Fallback to global settings on grid view
            p.setAlignX(mItemAlignment.horizontal.getAlignmentPosition(v));
            p.setAlignY(mItemAlignment.vertical.getAlignmentPosition(v));
        } else {
            // Use ItemAlignmentFacet defined on specific ViewHolder
            p.calculateItemAlignments(mOrientation, v);
            if (mOrientation == HORIZONTAL) {
                p.setAlignY(mItemAlignment.vertical.getAlignmentPosition(v));
            } else {
                p.setAlignX(mItemAlignment.horizontal.getAlignmentPosition(v));
            }
        }
    }

    private void updateChildAlignments() {
        for (int i = 0, c = getChildCount(); i < c; i++) {
            updateChildAlignments(getChildAt(i));
        }
    }

    void setExtraLayoutSpace(int extraLayoutSpace) {
        if (mExtraLayoutSpace == extraLayoutSpace) {
            return;
        } else if (mExtraLayoutSpace < 0) {
            throw new IllegalArgumentException("ExtraLayoutSpace must >= 0");
        }
        mExtraLayoutSpace = extraLayoutSpace;
        requestLayout();
    }

    int getExtraLayoutSpace() {
        return mExtraLayoutSpace;
    }

    private void removeInvisibleViewsAtEnd() {
        if (mPruneChild) {
            mGrid.removeInvisibleItemsAtEnd(mFocusPosition,
                    mReverseFlowPrimary ? -mExtraLayoutSpace : mSizePrimary + mExtraLayoutSpace);
        }
    }

    private void removeInvisibleViewsAtFront() {
        if (mPruneChild) {
            mGrid.removeInvisibleItemsAtFront(mFocusPosition,
                    mReverseFlowPrimary ? mSizePrimary + mExtraLayoutSpace: -mExtraLayoutSpace);
        }
    }

    private boolean appendOneColumnVisibleItems() {
        return mGrid.appendOneColumnVisibleItems();
    }

    private boolean prependOneColumnVisibleItems() {
        return mGrid.prependOneColumnVisibleItems();
    }

    private void appendVisibleItems() {
        mGrid.appendVisibleItems(mReverseFlowPrimary ? -mExtraLayoutSpace
                : mSizePrimary + mExtraLayoutSpace);
    }

    private void prependVisibleItems() {
        mGrid.prependVisibleItems(mReverseFlowPrimary ? mSizePrimary + mExtraLayoutSpace
                : -mExtraLayoutSpace);
    }

    /**
     * Fast layout when there is no structure change, adapter change, etc.
     * It will layout all views was layout requested or updated, until hit a view
     * with different size,  then it break and detachAndScrap all views after that. 
     */
    private void fastRelayout() {
        boolean invalidateAfter = false;
        final int childCount = getChildCount();
        int position = -1;
        for (int index = 0; index < childCount; index++) {
            View view = getChildAt(index);
            position = getPositionByIndex(index);
            Grid.Location location = mGrid.getLocation(position);
            if (location == null) {
                if (DEBUG) Log.w(getTag(), "fastRelayout(): no Location at " + position);
                invalidateAfter = true;
                break;
            }

            int startSecondary = getRowStartSecondary(location.row) - mScrollOffsetSecondary;
            int primarySize, end;
            int start = getViewMin(view);
            int oldPrimarySize = getViewPrimarySize(view);

            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (lp.viewNeedsUpdate()) {
                int viewIndex = mBaseGridView.indexOfChild(view);
                detachAndScrapView(view, mRecycler);
                view = getViewForPosition(position);
                addView(view, viewIndex);
            }

            measureChild(view);
            if (mOrientation == HORIZONTAL) {
                primarySize = getDecoratedMeasuredWidthWithMargin(view);
                end = start + primarySize;
            } else {
                primarySize = getDecoratedMeasuredHeightWithMargin(view);
                end = start + primarySize;
            }
            layoutChild(location.row, view, start, end, startSecondary);
            if (oldPrimarySize != primarySize) {
                // size changed invalidate remaining Locations
                if (DEBUG) Log.d(getTag(), "fastRelayout: view size changed at " + position);
                invalidateAfter = true;
                break;
            }
        }
        if (invalidateAfter) {
            final int savedLastPos = mGrid.getLastVisibleIndex();
            mGrid.invalidateItemsAfter(position);
            if (mPruneChild) {
                // in regular prune child mode, we just append items up to edge limit
                appendVisibleItems();
                if (mFocusPosition >= 0 && mFocusPosition <= savedLastPos) {
                    // make sure add focus view back:  the view might be outside edge limit
                    // when there is delta in onLayoutChildren().
                    while (mGrid.getLastVisibleIndex() < mFocusPosition) {
                        mGrid.appendOneColumnVisibleItems();
                    }
                }
            } else {
                // prune disabled(e.g. in RowsFragment transition): append all removed items
                while (mGrid.appendOneColumnVisibleItems()
                        && mGrid.getLastVisibleIndex() < savedLastPos);
            }
        }
        updateScrollMin();
        updateScrollMax();
        updateScrollSecondAxis();
    }

    public void removeAndRecycleAllViews(RecyclerView.Recycler recycler) {
        if (TRACE) TraceHelper.beginSection("removeAndRecycleAllViews");
        if (DEBUG) Log.v(TAG, "removeAndRecycleAllViews " + getChildCount());
        for (int i = getChildCount() - 1; i >= 0; i--) {
            removeAndRecycleViewAt(i, recycler);
        }
        if (TRACE) TraceHelper.endSection();
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

        if (state.didStructureChange()) {
            // didStructureChange() == true means attached item has been removed/added.
            // scroll animation: we are unable to continue a scroll animation,
            //    kill the scroll animation,  and let ItemAnimation move the item to new position.
            // position smooth scroller: kill the animation and stop at final position.
            // pending smooth scroller: stop and scroll to current focus position.
            mBaseGridView.stopScroll();
        }
        final boolean scrollToFocus = !isSmoothScrolling()
                && mFocusScrollStrategy == BaseGridView.FOCUS_SCROLL_ALIGNED;
        if (mFocusPosition != NO_POSITION && mFocusPositionOffset != Integer.MIN_VALUE) {
            mFocusPosition = mFocusPosition + mFocusPositionOffset;
            mSubFocusPosition = 0;
        }
        mFocusPositionOffset = 0;
        saveContext(recycler, state);

        View savedFocusView = findViewByPosition(mFocusPosition);
        int savedFocusPos = mFocusPosition;
        int savedSubFocusPos = mSubFocusPosition;
        boolean hadFocus = mBaseGridView.hasFocus();

        // Track the old focus view so we can adjust our system scroll position
        // so that any scroll animations happening now will remain valid.
        // We must use same delta in Pre Layout (if prelayout exists) and second layout.
        // So we cache the deltas in PreLayout and use it in second layout.
        int delta = 0, deltaSecondary = 0;
        if (mFocusPosition != NO_POSITION && scrollToFocus
                && mBaseGridView.getScrollState() != RecyclerView.SCROLL_STATE_IDLE) {
            // FIXME: we should get the remaining scroll animation offset from RecyclerView
            if (savedFocusView != null) {
                if (getScrollPosition(savedFocusView, savedFocusView.findFocus(), sTwoInts)) {
                    delta = sTwoInts[0];
                    deltaSecondary = sTwoInts[1];
                }
            }
        }

        if (mInFastRelayout = layoutInit()) {
            fastRelayout();
            // appends items till focus position.
            if (mFocusPosition != NO_POSITION) {
                View focusView = findViewByPosition(mFocusPosition);
                if (focusView != null) {
                    if (scrollToFocus) {
                        scrollToView(focusView, false);
                    }
                    if (hadFocus && !focusView.hasFocus()) {
                        focusView.requestFocus();
                    }
                }
            }
        } else {
            mInLayoutSearchFocus = hadFocus;
            if (mFocusPosition != NO_POSITION) {
                // appends items till focus position.
                while (appendOneColumnVisibleItems()
                        && findViewByPosition(mFocusPosition) == null) ;
            }
            // multiple rounds: scrollToView of first round may drag first/last child into
            // "visible window" and we update scrollMin/scrollMax then run second scrollToView
            int oldFirstVisible;
            int oldLastVisible;
            do {
                updateScrollMin();
                updateScrollMax();
                oldFirstVisible = mGrid.getFirstVisibleIndex();
                oldLastVisible = mGrid.getLastVisibleIndex();
                View focusView = findViewByPosition(mFocusPosition);
                // we need force to initialize the child view's position
                scrollToView(focusView, false);
                if (focusView != null && hadFocus && !focusView.hasFocus()) {
                    focusView.requestFocus();
                }
                appendVisibleItems();
                prependVisibleItems();
                removeInvisibleViewsAtFront();
                removeInvisibleViewsAtEnd();
            } while (mGrid.getFirstVisibleIndex() != oldFirstVisible ||
                    mGrid.getLastVisibleIndex() != oldLastVisible);
        }

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

        // For fastRelayout, only dispatch event when focus position changes.
        if (mInFastRelayout && (mFocusPosition != savedFocusPos || mSubFocusPosition !=
                savedSubFocusPos || findViewByPosition(mFocusPosition) != savedFocusView)) {
            dispatchChildSelected();
        } else if (!mInFastRelayout && mInLayoutSearchFocus) {
            // For full layout we dispatchChildSelected() in createItem() unless searched all
            // children and found none is focusable then dispatchChildSelected() here.
            dispatchChildSelected();
        }

        mInLayout = false;
        leaveContext();
        if (!hadFocus && !mInFastRelayout && mBaseGridView.hasFocusable()) {
            ViewCompat.postOnAnimation(mBaseGridView, mAskFocusRunnable);
        }
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
        mInScroll = true;
        int result;
        if (mOrientation == HORIZONTAL) {
            result = scrollDirectionPrimary(dx);
        } else {
            result = scrollDirectionSecondary(dx);
        }
        leaveContext();
        mInScroll = false;
        return result;
    }

    @Override
    public int scrollVerticallyBy(int dy, Recycler recycler, RecyclerView.State state) {
        if (DEBUG) Log.v(getTag(), "scrollVerticallyBy " + dy);
        if (!mLayoutEnabled || !hasDoneFirstLayout()) {
            return 0;
        }
        mInScroll = true;
        saveContext(recycler, state);
        int result;
        if (mOrientation == VERTICAL) {
            result = scrollDirectionPrimary(dy);
        } else {
            result = scrollDirectionSecondary(dy);
        }
        leaveContext();
        mInScroll = false;
        return result;
    }

    // scroll in main direction may add/prune views
    private int scrollDirectionPrimary(int da) {
        if (TRACE) TraceHelper.beginSection("scrollPrimary");
        boolean isMaxUnknown = false, isMinUnknown = false;
        int minScroll = 0, maxScroll = 0;
        if (da > 0) {
            isMaxUnknown = mWindowAlignment.mainAxis().isMaxUnknown();
            if (!isMaxUnknown) {
                maxScroll = mWindowAlignment.mainAxis().getMaxScroll();
                if (mScrollOffsetPrimary + da > maxScroll) {
                    da = maxScroll - mScrollOffsetPrimary;
                }
            }
        } else if (da < 0) {
            isMinUnknown = mWindowAlignment.mainAxis().isMinUnknown();
            if (!isMinUnknown) {
                minScroll = mWindowAlignment.mainAxis().getMinScroll();
                if (mScrollOffsetPrimary + da < minScroll) {
                    da = minScroll - mScrollOffsetPrimary;
                }
            }
        }
        if (da == 0) {
            if (TRACE) TraceHelper.endSection();
            return 0;
        }
        offsetChildrenPrimary(-da);
        mScrollOffsetPrimary += da;
        if (mInLayout) {
            if (TRACE) TraceHelper.endSection();
            return da;
        }

        int childCount = getChildCount();
        boolean updated;

        if (mReverseFlowPrimary ? da > 0 : da < 0) {
            prependVisibleItems();
        } else {
            appendVisibleItems();
        }
        updated = getChildCount() > childCount;
        childCount = getChildCount();

        if (TRACE) TraceHelper.beginSection("remove");
        if (mReverseFlowPrimary ? da > 0 : da < 0) {
            removeInvisibleViewsAtEnd();
        } else {
            removeInvisibleViewsAtFront();
        }
        if (TRACE) TraceHelper.endSection();
        updated |= getChildCount() < childCount;
        if (updated) {
            updateRowSecondarySizeRefresh();
        }

        mBaseGridView.invalidate();
        if (TRACE) TraceHelper.endSection();
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
        int highVisiblePos = (!mReverseFlowPrimary) ? mGrid.getLastVisibleIndex()
                : mGrid.getFirstVisibleIndex();
        int highMaxPos = (!mReverseFlowPrimary) ? mState.getItemCount() - 1 : 0;
        if (highVisiblePos < 0) {
            return;
        }
        final boolean highAvailable = highVisiblePos == highMaxPos;
        final boolean maxUnknown = mWindowAlignment.mainAxis().isMaxUnknown();
        if (!highAvailable && maxUnknown) {
            return;
        }
        int maxEdge = mGrid.findRowMax(true, sTwoInts) + mScrollOffsetPrimary;
        int rowIndex = sTwoInts[0];
        int pos = sTwoInts[1];
        int savedMaxEdge = mWindowAlignment.mainAxis().getMaxEdge();
        mWindowAlignment.mainAxis().setMaxEdge(maxEdge);
        int maxScroll = getPrimarySystemScrollPositionOfChildMax(findViewByPosition(pos));
        mWindowAlignment.mainAxis().setMaxEdge(savedMaxEdge);

        if (highAvailable) {
            mWindowAlignment.mainAxis().setMaxEdge(maxEdge);
            mWindowAlignment.mainAxis().setMaxScroll(maxScroll);
            if (DEBUG) Log.v(getTag(), "updating scroll maxEdge to " + maxEdge +
                    " scrollMax to " + maxScroll);
        } else {
            mWindowAlignment.mainAxis().invalidateScrollMax();
            if (DEBUG) Log.v(getTag(), "Invalidate scrollMax since it should be "
                    + "greater than " + maxScroll);
        }
    }

    private void updateScrollMin() {
        int lowVisiblePos = (!mReverseFlowPrimary) ? mGrid.getFirstVisibleIndex()
                : mGrid.getLastVisibleIndex();
        int lowMinPos = (!mReverseFlowPrimary) ? 0 : mState.getItemCount() - 1;
        if (lowVisiblePos < 0) {
            return;
        }
        final boolean lowAvailable = lowVisiblePos == lowMinPos;
        final boolean minUnknown = mWindowAlignment.mainAxis().isMinUnknown();
        if (!lowAvailable && minUnknown) {
            return;
        }
        int minEdge = mGrid.findRowMin(false, sTwoInts) + mScrollOffsetPrimary;
        int rowIndex = sTwoInts[0];
        int pos = sTwoInts[1];
        int savedMinEdge = mWindowAlignment.mainAxis().getMinEdge();
        mWindowAlignment.mainAxis().setMinEdge(minEdge);
        int minScroll = getPrimarySystemScrollPosition(findViewByPosition(pos));
        mWindowAlignment.mainAxis().setMinEdge(savedMinEdge);

        if (lowAvailable) {
            mWindowAlignment.mainAxis().setMinEdge(minEdge);
            mWindowAlignment.mainAxis().setMinScroll(minScroll);
            if (DEBUG) Log.v(getTag(), "updating scroll minEdge to " + minEdge +
                    " scrollMin to " + minScroll);
        } else {
            mWindowAlignment.mainAxis().invalidateScrollMin();
            if (DEBUG) Log.v(getTag(), "Invalidate scrollMin, since it should be "
                    + "less than " + minScroll);
        }
    }

    private void updateScrollSecondAxis() {
        mWindowAlignment.secondAxis().setMinEdge(0);
        mWindowAlignment.secondAxis().setMaxEdge(getSizeSecondary());
    }

    private void initScrollController() {
        mWindowAlignment.reset();
        mWindowAlignment.horizontal.setSize(getWidth());
        mWindowAlignment.vertical.setSize(getHeight());
        mWindowAlignment.horizontal.setPadding(getPaddingLeft(), getPaddingRight());
        mWindowAlignment.vertical.setPadding(getPaddingTop(), getPaddingBottom());
        mSizePrimary = mWindowAlignment.mainAxis().getSize();
        mScrollOffsetPrimary = -mWindowAlignment.mainAxis().getPaddingLow();
        mScrollOffsetSecondary = -mWindowAlignment.secondAxis().getPaddingLow();

        if (DEBUG) {
            Log.v(getTag(), "initScrollController mSizePrimary " + mSizePrimary
                    + " mWindowAlignment " + mWindowAlignment
                    + " mScrollOffsetPrimary " + mScrollOffsetPrimary);
        }
    }

    private void updateScrollController() {
        // mScrollOffsetPrimary and mScrollOffsetSecondary includes the padding.
        // e.g. when topPadding is 16 for horizontal grid view,  the initial
        // mScrollOffsetSecondary is -16.  fastRelayout() put views based on offsets(not padding),
        // when padding changes to 20,  we also need update mScrollOffsetSecondary to -20 before
        // fastRelayout() is performed
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

        mWindowAlignment.horizontal.setSize(getWidth());
        mWindowAlignment.vertical.setSize(getHeight());
        mWindowAlignment.horizontal.setPadding(getPaddingLeft(), getPaddingRight());
        mWindowAlignment.vertical.setPadding(getPaddingTop(), getPaddingBottom());
        mSizePrimary = mWindowAlignment.mainAxis().getSize();

        if (DEBUG) {
            Log.v(getTag(), "updateScrollController mSizePrimary " + mSizePrimary
                    + " mWindowAlignment " + mWindowAlignment
                    + " mScrollOffsetPrimary " + mScrollOffsetPrimary);
        }
    }

    @Override
    public void scrollToPosition(int position) {
        setSelection(position, 0, false, 0);
    }

    public void setSelection(int position,
            int primaryScrollExtra) {
        setSelection(position, 0, false, primaryScrollExtra);
    }

    public void setSelectionSmooth(int position) {
        setSelection(position, 0, true, 0);
    }

    public void setSelectionWithSub(int position, int subposition,
            int primaryScrollExtra) {
        setSelection(position, subposition, false, primaryScrollExtra);
    }

    public void setSelectionSmoothWithSub(int position, int subposition) {
        setSelection(position, subposition, true, 0);
    }

    public int getSelection() {
        return mFocusPosition;
    }

    public int getSubSelection() {
        return mSubFocusPosition;
    }

    public void setSelection(int position, int subposition, boolean smooth,
            int primaryScrollExtra) {
        if (mFocusPosition != position && position != NO_POSITION
                || subposition != mSubFocusPosition || primaryScrollExtra != mPrimaryScrollExtra) {
            scrollToSelection(position, subposition, smooth, primaryScrollExtra);
        }
    }

    private void scrollToSelection(int position, int subposition,
            boolean smooth, int primaryScrollExtra) {
        if (TRACE) TraceHelper.beginSection("scrollToSelection");
        mPrimaryScrollExtra = primaryScrollExtra;
        View view = findViewByPosition(position);
        if (view != null) {
            mInSelection = true;
            scrollToView(view, smooth);
            mInSelection = false;
        } else {
            mFocusPosition = position;
            mSubFocusPosition = subposition;
            mFocusPositionOffset = Integer.MIN_VALUE;
            if (!mLayoutEnabled) {
                return;
            }
            if (smooth) {
                if (!hasDoneFirstLayout()) {
                    Log.w(getTag(), "setSelectionSmooth should " +
                            "not be called before first layout pass");
                    return;
                }
                startPositionSmoothScroller(position);
            } else {
                mForceFullLayout = true;
                requestLayout();
            }
        }
        if (TRACE) TraceHelper.endSection();
    }

    void startPositionSmoothScroller(int position) {
        LinearSmoothScroller linearSmoothScroller = new GridLinearSmoothScroller() {
            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                if (getChildCount() == 0) {
                    return null;
                }
                final int firstChildPos = getPosition(getChildAt(0));
                // TODO We should be able to deduce direction from bounds of current and target
                // focus, rather than making assumptions about positions and directionality
                final boolean isStart = mReverseFlowPrimary ? targetPosition > firstChildPos
                        : targetPosition < firstChildPos;
                final int direction = isStart ? -1 : 1;
                if (mOrientation == HORIZONTAL) {
                    return new PointF(direction, 0);
                } else {
                    return new PointF(0, direction);
                }
            }

        };
        linearSmoothScroller.setTargetPosition(position);
        startSmoothScroll(linearSmoothScroller);
    }

    private void processPendingMovement(boolean forward) {
        if (forward ? hasCreatedLastItem() : hasCreatedFirstItem()) {
            return;
        }
        if (mPendingMoveSmoothScroller == null) {
            // Stop existing scroller and create a new PendingMoveSmoothScroller.
            mBaseGridView.stopScroll();
            PendingMoveSmoothScroller linearSmoothScroller = new PendingMoveSmoothScroller(
                    forward ? 1 : -1, mNumRows > 1);
            mFocusPositionOffset = 0;
            startSmoothScroll(linearSmoothScroller);
            if (linearSmoothScroller.isRunning()) {
                mPendingMoveSmoothScroller = linearSmoothScroller;
            }
        } else {
            if (forward) {
                mPendingMoveSmoothScroller.increasePendingMoves();
            } else {
                mPendingMoveSmoothScroller.decreasePendingMoves();
            }
        }
    }

    @Override
    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        if (DEBUG) Log.v(getTag(), "onItemsAdded positionStart "
                + positionStart + " itemCount " + itemCount);
        if (mFocusPosition != NO_POSITION && mGrid != null && mGrid.getFirstVisibleIndex() >= 0
                && mFocusPositionOffset != Integer.MIN_VALUE) {
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
        if (DEBUG) Log.v(getTag(), "onItemsRemoved positionStart "
                + positionStart + " itemCount " + itemCount);
        if (mFocusPosition != NO_POSITION  && mGrid != null && mGrid.getFirstVisibleIndex() >= 0
            && mFocusPositionOffset != Integer.MIN_VALUE) {
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
        if (DEBUG) Log.v(getTag(), "onItemsMoved fromPosition "
                + fromPosition + " toPosition " + toPosition);
        if (mFocusPosition != NO_POSITION && mFocusPositionOffset != Integer.MIN_VALUE) {
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
        if (DEBUG) Log.v(getTag(), "onItemsUpdated positionStart "
                + positionStart + " itemCount " + itemCount);
        for (int i = positionStart, end = positionStart + itemCount; i < end; i++) {
            mChildrenStates.remove(i);
        }
    }

    @Override
    public boolean onRequestChildFocus(RecyclerView parent, View child, View focused) {
        if (mFocusSearchDisabled) {
            return true;
        }
        if (getPositionByView(child) == NO_POSITION) {
            // This shouldn't happen, but in case it does be sure not to attempt a
            // scroll to a view whose item has been removed.
            return true;
        }
        if (!mInLayout && !mInSelection && !mInScroll) {
            scrollToView(child, focused, true);
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
        final int viewCenterPrimary = mScrollOffsetPrimary + getViewCenter(view);
        final int viewMin = getViewMin(view);
        final int viewMax = getViewMax(view);
        // TODO: change to use State object in onRequestChildFocus()
        boolean isMin, isMax;
        if (!mReverseFlowPrimary) {
            isMin = mGrid.getFirstVisibleIndex() == 0;
            isMax = mGrid.getLastVisibleIndex() == (mState == null ?
                    getItemCount() : mState.getItemCount()) - 1;
        } else {
            isMax = mGrid.getFirstVisibleIndex() == 0;
            isMin = mGrid.getLastVisibleIndex() == (mState == null ?
                    getItemCount() : mState.getItemCount()) - 1;
        }
        for (int i = getChildCount() - 1; (isMin || isMax) && i >= 0; i--) {
            View v = getChildAt(i);
            if (v == view || v == null) {
                continue;
            }
            if (isMin && getViewMin(v) < viewMin) {
                isMin = false;
            }
            if (isMax && getViewMax(v) > viewMax) {
                isMax = false;
            }
        }
        return mWindowAlignment.mainAxis().getSystemScrollPos(viewCenterPrimary, isMin, isMax);
    }

    private int getPrimarySystemScrollPositionOfChildMax(View view) {
        int scrollPosition = getPrimarySystemScrollPosition(view);
        final LayoutParams lp = (LayoutParams) view.getLayoutParams();
        int[] multipleAligns = lp.getAlignMultiple();
        if (multipleAligns != null && multipleAligns.length > 0) {
            scrollPosition += multipleAligns[multipleAligns.length - 1] - multipleAligns[0];
        }
        return scrollPosition;
    }

    /**
     * Get adjusted primary position for a given childView (if there is multiple ItemAlignment defined
     * on the view).
     */
    private int getAdjustedPrimaryScrollPosition(int scrollPrimary, View view, View childView) {
        int subindex = getSubPositionByView(view, childView);
        if (subindex != 0) {
            final LayoutParams lp = (LayoutParams) view.getLayoutParams();
            scrollPrimary += lp.getAlignMultiple()[subindex] - lp.getAlignMultiple()[0];
        }
        return scrollPrimary;
    }

    private int getSecondarySystemScrollPosition(View view) {
        int viewCenterSecondary = mScrollOffsetSecondary + getViewCenterSecondary(view);
        int pos = getPositionByView(view);
        Grid.Location location = mGrid.getLocation(pos);
        final int row = location.row;
        final boolean isMin, isMax;
        if (!mReverseFlowSecondary) {
            isMin = row == 0;
            isMax = row == mGrid.getNumRows() - 1;
        } else {
            isMax = row == 0;
            isMin = row == mGrid.getNumRows() - 1;
        }
        return mWindowAlignment.secondAxis().getSystemScrollPos(viewCenterSecondary, isMin, isMax);
    }

    /**
     * Scroll to a given child view and change mFocusPosition.
     */
    private void scrollToView(View view, boolean smooth) {
        scrollToView(view, view == null ? null : view.findFocus(), smooth);
    }

    /**
     * Scroll to a given child view and change mFocusPosition.
     */
    private void scrollToView(View view, View childView, boolean smooth) {
        int newFocusPosition = getPositionByView(view);
        int newSubFocusPosition = getSubPositionByView(view, childView);
        if (newFocusPosition != mFocusPosition || newSubFocusPosition != mSubFocusPosition) {
            mFocusPosition = newFocusPosition;
            mSubFocusPosition = newSubFocusPosition;
            mFocusPositionOffset = 0;
            if (!mInLayout) {
                dispatchChildSelected();
            }
            if (mBaseGridView.isChildrenDrawingOrderEnabledInternal()) {
                mBaseGridView.invalidate();
            }
        }
        if (view == null) {
            return;
        }
        if (!view.hasFocus() && mBaseGridView.hasFocus()) {
            // transfer focus to the child if it does not have focus yet (e.g. triggered
            // by setSelection())
            view.requestFocus();
        }
        if (!mScrollEnabled && smooth) {
            return;
        }
        if (getScrollPosition(view, childView, sTwoInts)) {
            scrollGrid(sTwoInts[0], sTwoInts[1], smooth);
        }
    }

    private boolean getScrollPosition(View view, View childView, int[] deltas) {
        switch (mFocusScrollStrategy) {
        case BaseGridView.FOCUS_SCROLL_ALIGNED:
        default:
            return getAlignedPosition(view, childView, deltas);
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
        final int row = mGrid.getRowIndex(pos);
        if (viewMin < paddingLow) {
            // view enters low padding area:
            firstView = view;
            if (mFocusScrollStrategy == BaseGridView.FOCUS_SCROLL_PAGE) {
                // scroll one "page" left/top,
                // align first visible item of the "page" at the low padding edge.
                while (prependOneColumnVisibleItems()) {
                    CircularIntArray positions =
                            mGrid.getItemPositionsInRows(mGrid.getFirstVisibleIndex(), pos)[row];
                    firstView = findViewByPosition(positions.get(0));
                    if (viewMax - getViewMin(firstView) > clientSize) {
                        if (positions.size() > 2) {
                            firstView = findViewByPosition(positions.get(2));
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
                    CircularIntArray positions =
                            mGrid.getItemPositionsInRows(pos, mGrid.getLastVisibleIndex())[row];
                    lastView = findViewByPosition(positions.get(positions.size() - 1));
                    if (getViewMax(lastView) - viewMin > clientSize) {
                        lastView = null;
                        break;
                    }
                } while (appendOneColumnVisibleItems());
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

    private boolean getAlignedPosition(View view, View childView, int[] deltas) {
        int scrollPrimary = getPrimarySystemScrollPosition(view);
        if (childView != null) {
            scrollPrimary = getAdjustedPrimaryScrollPosition(scrollPrimary, view, childView);
        }
        int scrollSecondary = getSecondarySystemScrollPosition(view);
        if (DEBUG) {
            Log.v(getTag(), "getAlignedPosition " + scrollPrimary + " " + scrollSecondary
                    + " " + mPrimaryScrollExtra + " " + mWindowAlignment);
            Log.v(getTag(), "getAlignedPosition " + mScrollOffsetPrimary + " " + mScrollOffsetSecondary);
        }
        scrollPrimary -= mScrollOffsetPrimary;
        scrollSecondary -= mScrollOffsetSecondary;
        scrollPrimary += mPrimaryScrollExtra;
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
            if (mScrollEnabled && mFocusScrollStrategy == BaseGridView.FOCUS_SCROLL_ALIGNED
                    && mFocusPosition != NO_POSITION) {
                scrollToSelection(mFocusPosition, mSubFocusPosition,
                        true, mPrimaryScrollExtra);
            }
        }
    }

    public boolean isScrollEnabled() {
        return mScrollEnabled;
    }

    private int findImmediateChildIndex(View view) {
        if (mBaseGridView != null && view != mBaseGridView) {
            view = findContainingItemView(view);
            if (view != null) {
                for (int i = 0, count = getChildCount(); i < count; i++) {
                    if (getChildAt(i) == view) {
                        return i;
                    }
                }
            }
        }
        return NO_POSITION;
    }

    void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        if (gainFocus) {
            // if gridview.requestFocus() is called, select first focusable child.
            for (int i = mFocusPosition; ;i++) {
                View view = findViewByPosition(i);
                if (view == null) {
                    break;
                }
                if (view.getVisibility() == View.VISIBLE && view.hasFocusable()) {
                    view.requestFocus();
                    break;
                }
            }
        }
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

        final FocusFinder ff = FocusFinder.getInstance();
        View result = null;
        if (direction == View.FOCUS_FORWARD || direction == View.FOCUS_BACKWARD) {
            // convert direction to absolute direction and see if we have a view there and if not
            // tell LayoutManager to add if it can.
            if (canScrollVertically()) {
                final int absDir =
                        direction == View.FOCUS_FORWARD ? View.FOCUS_DOWN : View.FOCUS_UP;
                result = ff.findNextFocus(mBaseGridView, focused, absDir);
            }
            if (canScrollHorizontally()) {
                boolean rtl = getLayoutDirection() == ViewCompat.LAYOUT_DIRECTION_RTL;
                final int absDir = (direction == View.FOCUS_FORWARD) ^ rtl
                        ? View.FOCUS_RIGHT : View.FOCUS_LEFT;
                result = ff.findNextFocus(mBaseGridView, focused, absDir);
            }
        } else {
            result = ff.findNextFocus(mBaseGridView, focused, direction);
        }
        if (result != null) {
            return result;
        }

        if (DEBUG) Log.v(getTag(), "regular focusSearch failed direction " + direction);
        int movement = getMovement(direction);
        final boolean isScroll = mBaseGridView.getScrollState() != RecyclerView.SCROLL_STATE_IDLE;
        if (movement == NEXT_ITEM) {
            if (isScroll || !mFocusOutEnd) {
                result = focused;
            }
            if (mScrollEnabled && !hasCreatedLastItem()) {
                processPendingMovement(true);
                result = focused;
            }
        } else if (movement == PREV_ITEM) {
            if (isScroll || !mFocusOutFront) {
                result = focused;
            }
            if (mScrollEnabled && !hasCreatedFirstItem()) {
                processPendingMovement(false);
                result = focused;
            }
        } else if (movement == NEXT_ROW) {
            if (isScroll || !mFocusOutSideEnd) {
                result = focused;
            }
        } else if (movement == PREV_ROW) {
            if (isScroll || !mFocusOutSideStart) {
                result = focused;
            }
        }
        if (result != null) {
            return result;
        }

        if (DEBUG) Log.v(getTag(), "now focusSearch in parent");
        result = mBaseGridView.getParent().focusSearch(focused, direction);
        if (result != null) {
            return result;
        }
        return focused != null ? focused : mBaseGridView;
    }

    boolean hasPreviousViewInSameRow(int pos) {
        if (mGrid == null || pos == NO_POSITION || mGrid.getFirstVisibleIndex() < 0) {
            return false;
        }
        if (mGrid.getFirstVisibleIndex() > 0) {
            return true;
        }
        final int focusedRow = mGrid.getLocation(pos).row;
        for (int i = getChildCount() - 1; i >= 0; i--) {
            int position = getPositionByIndex(i);
            Grid.Location loc = mGrid.getLocation(position);
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
            if (mPendingMoveSmoothScroller != null) {
                // don't find next focusable if has pending movement.
                return true;
            }
            final int movement = getMovement(direction);
            final View focused = recyclerView.findFocus();
            final int focusedIndex = findImmediateChildIndex(focused);
            final int focusedPos = getPositionByIndex(focusedIndex);
            // Add focusables of focused item.
            if (focusedPos != NO_POSITION) {
                findViewByPosition(focusedPos).addFocusables(views,  direction, focusableMode);
            }
            if (mGrid == null || getChildCount() == 0) {
                // no grid information, or no child, bail out.
                return true;
            }
            if ((movement == NEXT_ROW || movement == PREV_ROW) && mGrid.getNumRows() <= 1) {
                // For single row, cannot navigate to previous/next row.
                return true;
            }
            // Add focusables of neighbor depending on the focus search direction.
            final int focusedRow = mGrid != null && focusedPos != NO_POSITION ?
                    mGrid.getLocation(focusedPos).row : NO_POSITION;
            final int focusableCount = views.size();
            int inc = movement == NEXT_ITEM || movement == NEXT_ROW ? 1 : -1;
            int loop_end = inc > 0 ? getChildCount() - 1 : 0;
            int loop_start;
            if (focusedIndex == NO_POSITION) {
                loop_start = inc > 0 ? 0 : getChildCount() - 1;
            } else {
                loop_start = focusedIndex + inc;
            }
            for (int i = loop_start; inc > 0 ? i <= loop_end : i >= loop_end; i += inc) {
                final View child = getChildAt(i);
                if (child.getVisibility() != View.VISIBLE || !child.hasFocusable()) {
                    continue;
                }
                // if there wasn't any focusing item,  add the very first focusable
                // items and stop.
                if (focusedPos == NO_POSITION) {
                    child.addFocusables(views,  direction, focusableMode);
                    if (views.size() > focusableCount) {
                        break;
                    }
                    continue;
                }
                int position = getPositionByIndex(i);
                Grid.Location loc = mGrid.getLocation(position);
                if (loc == null) {
                    continue;
                }
                if (movement == NEXT_ITEM) {
                    // Add first focusable item on the same row
                    if (loc.row == focusedRow && position > focusedPos) {
                        child.addFocusables(views,  direction, focusableMode);
                        if (views.size() > focusableCount) {
                            break;
                        }
                    }
                } else if (movement == PREV_ITEM) {
                    // Add first focusable item on the same row
                    if (loc.row == focusedRow && position < focusedPos) {
                        child.addFocusables(views,  direction, focusableMode);
                        if (views.size() > focusableCount) {
                            break;
                        }
                    }
                } else if (movement == NEXT_ROW) {
                    // Add all focusable items after this item whose row index is bigger
                    if (loc.row == focusedRow) {
                        continue;
                    } else if (loc.row < focusedRow) {
                        break;
                    }
                    child.addFocusables(views,  direction, focusableMode);
                } else if (movement == PREV_ROW) {
                    // Add all focusable items before this item whose row index is smaller
                    if (loc.row == focusedRow) {
                        continue;
                    } else if (loc.row > focusedRow) {
                        break;
                    }
                    child.addFocusables(views,  direction, focusableMode);
                }
            }
        } else {
            int focusableCount = views.size();
            if (mFocusScrollStrategy != BaseGridView.FOCUS_SCROLL_ALIGNED) {
                // adding views not overlapping padding area to avoid scrolling in gaining focus
                int left = mWindowAlignment.mainAxis().getPaddingLow();
                int right = mWindowAlignment.mainAxis().getClientSize() + left;
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
                }
            } else {
                View view = findViewByPosition(mFocusPosition);
                if (view != null) {
                    view.addFocusables(views, direction, focusableMode);
                }
            }
            // if still cannot find any, fall through and add itself
            if (views.size() != focusableCount) {
                return true;
            }
            if (recyclerView.isFocusable()) {
                views.add(recyclerView);
            }
        }
        return true;
    }

    private boolean hasCreatedLastItem() {
        int count = getItemCount();
        return count == 0 || mBaseGridView.findViewHolderForAdapterPosition(count - 1) != null;
    }

    private boolean hasCreatedFirstItem() {
        int count = getItemCount();
        return count == 0 || mBaseGridView.findViewHolderForAdapterPosition(0) != null;
    }

    boolean canScrollTo(View view) {
        return view.getVisibility() == View.VISIBLE && (!hasFocus() || view.hasFocusable());
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
                    movement = (!mReverseFlowPrimary) ? PREV_ITEM : NEXT_ITEM;
                    break;
                case View.FOCUS_RIGHT:
                    movement = (!mReverseFlowPrimary) ? NEXT_ITEM : PREV_ITEM;
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
                     movement = (!mReverseFlowSecondary) ? PREV_ROW : NEXT_ROW;
                     break;
                 case View.FOCUS_RIGHT:
                     movement = (!mReverseFlowSecondary) ? NEXT_ROW : PREV_ROW;
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
        if (newAdapter instanceof FacetProviderAdapter) {
            mFacetProviderAdapter = (FacetProviderAdapter) newAdapter;
        } else {
            mFacetProviderAdapter = null;
        }
        super.onAdapterChanged(oldAdapter, newAdapter);
    }

    private void discardLayoutInfo() {
        mGrid = null;
        mRowSizeSecondary = null;
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
        // save selected index
        ss.index = getSelection();
        // save offscreen child (state when they are recycled)
        Bundle bundle = mChildrenStates.saveAsBundle();
        // save views currently is on screen (TODO save cached views)
        for (int i = 0, count = getChildCount(); i < count; i++) {
            View view = getChildAt(i);
            int position = getPositionByView(view);
            if (position != NO_POSITION) {
                bundle = mChildrenStates.saveOnScreenView(bundle, view, position);
            }
        }
        ss.childStates = bundle;
        return ss;
    }

    void onChildRecycled(RecyclerView.ViewHolder holder) {
        final int position = holder.getAdapterPosition();
        if (position != NO_POSITION) {
            mChildrenStates.saveOffscreenView(holder.itemView, position);
        }
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

    @Override
    public int getRowCountForAccessibility(RecyclerView.Recycler recycler,
            RecyclerView.State state) {
        if (mOrientation == HORIZONTAL && mGrid != null) {
            return mGrid.getNumRows();
        }
        return super.getRowCountForAccessibility(recycler, state);
    }

    @Override
    public int getColumnCountForAccessibility(RecyclerView.Recycler recycler,
            RecyclerView.State state) {
        if (mOrientation == VERTICAL && mGrid != null) {
            return mGrid.getNumRows();
        }
        return super.getColumnCountForAccessibility(recycler, state);
    }

    @Override
    public void onInitializeAccessibilityNodeInfoForItem(RecyclerView.Recycler recycler,
            RecyclerView.State state, View host, AccessibilityNodeInfoCompat info) {
        ViewGroup.LayoutParams lp = host.getLayoutParams();
        if (mGrid == null || !(lp instanceof LayoutParams)) {
            super.onInitializeAccessibilityNodeInfoForItem(recycler, state, host, info);
            return;
        }
        LayoutParams glp = (LayoutParams) lp;
        int position = glp.getViewLayoutPosition();
        int rowIndex = mGrid.getRowIndex(position);
        int guessSpanIndex = position / mGrid.getNumRows();
        if (mOrientation == HORIZONTAL) {
            info.setCollectionItemInfo(AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(
                    rowIndex, 1, guessSpanIndex, 1, false, false));
        } else {
            info.setCollectionItemInfo(AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(
                    guessSpanIndex, 1, rowIndex, 1, false, false));
        }
    }

    /*
     * Leanback widget is different than the default implementation because the "scroll" is driven
     * by selection change.
     */
    @Override
    public boolean performAccessibilityAction(Recycler recycler, State state, int action,
            Bundle args) {
        saveContext(recycler, state);
        switch (action) {
            case AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD:
                // try to focus all the way to the last visible item on the same row.
                processSelectionMoves(false, -mState.getItemCount());
                break;
            case AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD:
                processSelectionMoves(false, mState.getItemCount());
                break;
        }
        leaveContext();
        return true;
    }

    /*
     * Move mFocusPosition multiple steps on the same row in main direction.
     * Stops when moves are all consumed or reach first/last visible item.
     * Returning remaining moves.
     */
    private int processSelectionMoves(boolean preventScroll, int moves) {
        if (mGrid == null) {
            return moves;
        }
        int focusPosition = mFocusPosition;
        int focusedRow = focusPosition != NO_POSITION ?
                mGrid.getRowIndex(focusPosition) : NO_POSITION;
        View newSelected = null;
        for (int i = 0, count = getChildCount(); i < count && moves != 0; i++) {
            int index = moves > 0 ? i : count - 1 - i;
            final View child = getChildAt(index);
            if (!canScrollTo(child)) {
                continue;
            }
            int position = getPositionByIndex(index);
            int rowIndex = mGrid.getRowIndex(position);
            if (focusedRow == NO_POSITION) {
                focusPosition = position;
                newSelected = child;
                focusedRow = rowIndex;
            } else if (rowIndex == focusedRow) {
                if ((moves > 0 && position > focusPosition)
                        || (moves < 0 && position < focusPosition)) {
                    focusPosition = position;
                    newSelected = child;
                    if (moves > 0) {
                        moves--;
                    } else {
                        moves++;
                    }
                }
            }
        }
        if (newSelected != null) {
            if (preventScroll) {
                if (hasFocus()) {
                    mInSelection = true;
                    newSelected.requestFocus();
                    mInSelection = false;
                }
                mFocusPosition = focusPosition;
                mSubFocusPosition = 0;
            } else {
                scrollToView(newSelected, true);
            }
        }
        return moves;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(Recycler recycler, State state,
            AccessibilityNodeInfoCompat info) {
        saveContext(recycler, state);
        if (mScrollEnabled && !hasCreatedFirstItem()) {
            info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
            info.setScrollable(true);
        }
        if (mScrollEnabled && !hasCreatedLastItem()) {
            info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
            info.setScrollable(true);
        }
        final AccessibilityNodeInfoCompat.CollectionInfoCompat collectionInfo
                = AccessibilityNodeInfoCompat.CollectionInfoCompat
                .obtain(getRowCountForAccessibility(recycler, state),
                        getColumnCountForAccessibility(recycler, state),
                        isLayoutHierarchical(recycler, state),
                        getSelectionModeForAccessibility(recycler, state));
        info.setCollectionInfo(collectionInfo);
        leaveContext();
    }
}
