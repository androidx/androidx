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
 * See the License for the specific languag`e governing permissions and
 * limitations under the License.
 */

package android.support.v7.widget;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * A {@link android.support.v7.widget.RecyclerView.LayoutManager} implementation which provides
 * similar functionality to {@link android.widget.ListView}.
 */
public class LinearLayoutManager extends RecyclerView.LayoutManager {

    private static final String TAG = "LinearLayoutManager";

    private static final boolean DEBUG = false;

    public static final int HORIZONTAL = LinearLayout.HORIZONTAL;

    public static final int VERTICAL = LinearLayout.VERTICAL;

    /**
     * Current orientation. Either {@link #HORIZONTAL} or {@link #VERTICAL}
     */
    private int mOrientation;

    /**
     * Helper class that keeps temporary rendering state.
     * It does not keep state after rendering is complete but we still keep a reference to re-use
     * the same object.
     */
    private RenderState mRenderState;

    /**
     * Many calculations are made depending on orientation. To keep it clean, this interface
     * helps {@link LinearLayoutManager} make those decisions.
     * Based on {@link #mOrientation}, an implementation is lazily created in
     * {@link #ensureRenderState} method.
     */
    private OrientationHelper mOrientationHelper;

    /**
     * To be able to detect stacking order changes, we keep the last <em>rendered</em> value.
     */
    private boolean mLastLayoutStackOrder;

    /**
     * To be able to detect layout direction changes, we keep the last <em>rendered</em> value.
     */
    private int mLastLayoutOrientation;

    /**
     * We need to track this so that we can ignore current position when it changes.
     */
    private boolean mLastLegacyStackFromEnd;


    /**
     * Defines if layout should be calculated from end to start.
     *
     * @see #mShouldReverseLayout
     */
    private boolean mReverseLayout = false;

    /**
     * This keeps the final value for how LayoutManager shouls start laying out views.
     * It is calculated by checking {@link #getReverseLayout()} and View's layout direction.
     * {@link #layoutChildren(RecyclerView.Adapter, RecyclerView.Recycler, boolean)} is run.
     */
    private boolean mShouldReverseLayout = false;

    /**
     * Works the same way as {@link android.widget.AbsListView#setStackFromBottom(boolean)} and
     * it supports both orientations.
     * see {@link android.widget.AbsListView#setStackFromBottom(boolean)}
     */
    private boolean mStackFromEnd = false;

    /**
     * Creates a vertical LinearLayoutManager
     *
     * @param context Current context, will be used to access resources.
     */
    public LinearLayoutManager(Context context) {
        this(context, VERTICAL, false);
    }

    /**
     * @param context       Current context, will be used to access resources.
     * @param orientation   Layout orientation. Should be {@link #HORIZONTAL} or {@link
     *                      #VERTICAL}.
     * @param reverseLayout When set to true, renders the layout from end to start.
     */
    public LinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        setOrientation(orientation);
        setReverseLayout(reverseLayout);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /**
     * @return true if {@link #getOrientation()} is {@link #HORIZONTAL}
     */
    @Override
    public boolean canScrollHorizontally() {
        return mOrientation == HORIZONTAL;
    }

    /**
     * @return true if {@link #getOrientation()} is {@link #VERTICAL}
     */
    @Override
    public boolean canScrollVertically() {
        return mOrientation == VERTICAL;
    }

    /**
     * Compatibility support for {@link android.widget.AbsListView#setStackFromBottom(boolean)}
     */
    public void setStackFromEnd(boolean stackFromEnd) {
        mStackFromEnd = stackFromEnd;
        requestLayout();
    }

    public boolean getStackFromEnd() {
        return mStackFromEnd;
    }

    /**
     * Returns the current orientaion of the layout.
     *
     * @return Current orientation.
     * @see #mOrientation
     * @see #setOrientation(int)
     */
    public int getOrientation() {
        return mOrientation;
    }

    /**
     * Sets the orientation of the layout. {@link android.support.v7.widget.LinearLayoutManager}
     * will do its best to keep scroll position.
     *
     * @param orientation {@link #HORIZONTAL} or {@link #VERTICAL}
     */
    public void setOrientation(int orientation) {
        if (orientation != HORIZONTAL && orientation != VERTICAL) {
            throw new IllegalArgumentException("invalid orientation.");
        }
        if (orientation == mOrientation) {
            return;
        }
        mOrientation = orientation;
        mOrientationHelper = null;
        requestLayout();
    }

    /**
     * Calculates the view layout order. (e.g. from end to start or start to end)
     * RTL layout support is applied automatically. So if layout is RTL and
     * {@link #getReverseLayout()} is {@code true}, elements will be laid out starting from left.
     */
    private void resolveShouldLayoutReverse() {
        // A == B is the same result, but we rather keep it readable
        if (mOrientation == VERTICAL || isLayoutRTL() == false) {
            mShouldReverseLayout = mReverseLayout;
        } else {
            mShouldReverseLayout = !mReverseLayout;
        }
    }

    /**
     * Returns if views are laid out from the opposite direction of the layout.
     *
     * @return If layout is reversed or not.
     * @see {@link #setReverseLayout(boolean)}
     */
    public boolean getReverseLayout() {
        return mReverseLayout;
    }

    /**
     * Used to reverse item traversal and layout order.
     * This behaves similar to the layout change for RTL views. When set to true, first item is
     * rendered at the end of the UI, second item is render before it etc.
     *
     * For horizontal layouts, it depends on the layout direction.
     * When set to true, If {@link android.support.v7.widget.RecyclerView} is LTR, than it will
     * render from RTL, if {@link android.support.v7.widget.RecyclerView}} is RTL, it will render
     * from LTR.
     *
     * If you are looking for the exact same behavior of
     * {@link android.widget.AbsListView#setStackFromBottom(boolean)}, use
     * {@link #setStackFromEnd(boolean)}
     */
    public void setReverseLayout(boolean reverseLayout) {
        if (reverseLayout == mReverseLayout) {
            return;
        }
        mReverseLayout = reverseLayout;
        requestLayout();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void layoutChildren(RecyclerView.Adapter adapter, RecyclerView.Recycler recycler,
            boolean structureChanged) {
        // update stacking direction.
        resolveShouldLayoutReverse();

        final boolean stackOrderChanged = mLastLayoutStackOrder != mShouldReverseLayout;
        final boolean orientationChanged = mLastLayoutOrientation != mOrientation;
        final boolean legacyStackFromEndChanged = mLastLegacyStackFromEnd != mStackFromEnd;
        final boolean layoutFromEnd = mStackFromEnd ^ mShouldReverseLayout;
        ensureRenderState();

        //find current scroll position
        if (legacyStackFromEndChanged == false && getChildCount() > 0) {
            View referenceChild = mStackFromEnd ? getChildAt(getChildCount() - 1)
                    : getChildAt(0);
            mRenderState.mCurrentPosition = getPosition(referenceChild);
            final boolean ignoreOffsetOfChild = orientationChanged || stackOrderChanged;
            if (ignoreOffsetOfChild) {
                // on orientation change, snap item to beginning because its current relative
                // positioning is meaningless
                if (layoutFromEnd) {
                    mRenderState.mOffset = mOrientationHelper.getEndAfterPadding();
                    mRenderState.mAvailable = mRenderState.mOffset
                            - mOrientationHelper.getStartAfterPadding();
                } else {
                    mRenderState.mOffset = mOrientationHelper.getStartAfterPadding();
                    mRenderState.mAvailable = mOrientationHelper.getEndAfterPadding()
                            - mRenderState.mOffset;
                }
            } else {
                if (layoutFromEnd) {
                    mRenderState.mOffset = mOrientationHelper.getDecoratedEnd(referenceChild);
                    mRenderState.mAvailable = mRenderState.mOffset
                            - mOrientationHelper.getStartAfterPadding();
                } else {
                    mRenderState.mOffset = mOrientationHelper.getDecoratedStart(referenceChild);
                    mRenderState.mAvailable = mOrientationHelper.getEndAfterPadding()
                            - mRenderState.mOffset;
                }
            }

        } else {
            if (layoutFromEnd) {
                mRenderState.mOffset = mOrientationHelper.getEndAfterPadding();
                mRenderState.mAvailable = mRenderState.mOffset
                        - mOrientationHelper.getStartAfterPadding();
            } else {
                mRenderState.mOffset = mOrientationHelper.getStartAfterPadding();
                mRenderState.mAvailable = mOrientationHelper.getEndAfterPadding()
                        - mRenderState.mOffset;
            }
            mRenderState.mCurrentPosition = mStackFromEnd ? adapter.getItemCount() - 1 : 0;
        }
        detachAndScrapAttachedViews(recycler);
        mRenderState.mItemDirection = mStackFromEnd ? RenderState.ITEM_DIRECTION_HEAD
                : RenderState.ITEM_DIRECTION_TAIL;
        mRenderState.mLayoutDirection = layoutFromEnd ? RenderState.LAYOUT_START
                : RenderState.LAYOUT_END;
        mRenderState.mScrollingOffset = RenderState.SCOLLING_OFFSET_NaN;

        fill(recycler, adapter, mRenderState);
        removeAndRecycleScrap(recycler);
        mLastLayoutOrientation = mOrientation;
        mLastLayoutStackOrder = mShouldReverseLayout;
        mLastLegacyStackFromEnd = mStackFromEnd;
    }

    private boolean isLayoutRTL() {
        return getLayoutDirection() == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    private void ensureRenderState() {
        if (mRenderState == null) {
            mRenderState = new RenderState();
        }
        if (mOrientationHelper == null) {
            mOrientationHelper = mOrientation == HORIZONTAL ? createHorizontalOrientationHelper()
                    : createVerticalOrientationHelper();
        }
    }

    // TODO consider moving down to recycle view
    private int getPosition(View view) {
        return ((RecyclerView.LayoutParams) view.getLayoutParams()).getViewPosition();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Adapter adapter,
            RecyclerView.Recycler recycler) {
        return scrollBy(dx, adapter, recycler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Adapter adapter,
            RecyclerView.Recycler recycler) {
        return scrollBy(dy, adapter, recycler);
    }

    private int scrollBy(int dy, RecyclerView.Adapter adapter, RecyclerView.Recycler recycler) {
        if (getChildCount() == 0) {
            return 0;
        }
        ensureRenderState();
        if (dy > 0) {
            // get the first child in the direction we are going
            View child = mShouldReverseLayout ? getChildAt(0) : getChildAt(getChildCount() - 1);
            // calculate how much we can scroll without adding new children (independent of layout)
            final int fastScrollSpace = mOrientationHelper.getDecoratedEnd(child)
                    - mOrientationHelper.getEndAfterPadding();
            // the direction in which we are traversing children
            mRenderState.mItemDirection = mShouldReverseLayout ? RenderState.ITEM_DIRECTION_HEAD
                    : RenderState.ITEM_DIRECTION_TAIL;
            // the direction we are going to draw (ignores stacking logic)
            mRenderState.mLayoutDirection = RenderState.LAYOUT_END;
            mRenderState.mCurrentPosition = getPosition(child) + mRenderState.mItemDirection;
            mRenderState.mAvailable = dy - fastScrollSpace;
            mRenderState.mOffset = mOrientationHelper.getDecoratedEnd(child);
            mRenderState.mScrollingOffset = fastScrollSpace;
            final int consumed = fill(recycler, adapter, mRenderState) + fastScrollSpace;
            final int scrolled = Math.min(dy, consumed);
            mOrientationHelper.offsetChildren(-scrolled);
            if (DEBUG) {
                Log.d(TAG, "scroll req: " + dy + " scrolled: " + scrolled);
            }
            return scrolled;
        } else {
            View child = mShouldReverseLayout ? getChildAt(getChildCount() - 1) : getChildAt(0);
            final int fastScrollSpace = -mOrientationHelper.getDecoratedStart(child)
                    + mOrientationHelper.getStartAfterPadding();
            mRenderState.mItemDirection = mShouldReverseLayout ? RenderState.ITEM_DIRECTION_TAIL
                    : RenderState.ITEM_DIRECTION_HEAD;
            mRenderState.mLayoutDirection = RenderState.LAYOUT_START;
            mRenderState.mCurrentPosition = getPosition(child) + mRenderState.mItemDirection;
            mRenderState.mAvailable = -dy - fastScrollSpace;
            mRenderState.mOffset = mOrientationHelper.getDecoratedStart(child);
            mRenderState.mScrollingOffset = fastScrollSpace;
            final int consumed = fastScrollSpace + fill(recycler, adapter, mRenderState);
            final int scrolled = Math.max(dy, -consumed);
            mOrientationHelper.offsetChildren(-scrolled);
            if (DEBUG) {
                Log.d(TAG, "scroll req: " + dy + " scrolled: " + scrolled);
            }
            return scrolled;
        }
    }

    /**
     * Recycles children between given indices.
     *
     * @param startIndex inclusive
     * @param endIndex   exclusive
     */
    private void recycleChildren(RecyclerView.Recycler recycler, int startIndex, int endIndex) {
        if (startIndex == endIndex) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "Recycling " + Math.abs(startIndex - endIndex) + " items");
        }
        if (endIndex > startIndex) {
            for (int i = endIndex - 1; i >= startIndex; i--) {
                removeAndRecycleViewAt(i, recycler);
            }
        } else {
            for (int i = startIndex; i > endIndex; i--) {
                removeAndRecycleViewAt(i, recycler);
            }
        }
    }

    /**
     * Recycles views that went out of bounds after scrolling towards the end of the layout.
     *
     * @param recycler Recycler instance of {@link android.support.v7.widget.RecyclerView}
     * @param dt       This can be used to add additional padding to the visible area. This is used
     *                 to
     *                 detect children that will go out of bounds after scrolling, without actually
     *                 moving them.
     */
    private void recycleViewsFromStart(RecyclerView.Recycler recycler, int dt) {
        if (dt < 0) {
            if (DEBUG) {
                Log.d(TAG, "Called recycle from start with a negative value. This might happen"
                        + " during layout changes but may be sign of a bug");
            }
            return;
        }
        final int limit = mOrientationHelper.getStartAfterPadding() + dt;
        final int childCount = getChildCount();
        if (mShouldReverseLayout) {
            for (int i = childCount - 1; i >= 0; i--) {
                View child = getChildAt(i);
                if (mOrientationHelper.getDecoratedEnd(child) > limit) {// stop here
                    recycleChildren(recycler, childCount - 1, i);
                    return;
                }
            }
        } else {
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (mOrientationHelper.getDecoratedEnd(child) > limit) {// stop here
                    recycleChildren(recycler, 0, i);
                    return;
                }
            }
        }
    }


    /**
     * Recycles views that went out of bounds after scrolling towards the start of the layout.
     *
     * @param recycler Recycler instance of {@link android.support.v7.widget.RecyclerView}
     * @param dt       This can be used to add additional padding to the visible area. This is used
     *                 to detect children that will go out of bounds after scrolling, without
     *                 actually moving them.
     */
    private void recycleViewsFromEnd(RecyclerView.Recycler recycler, int dt) {
        final int childCount = getChildCount();
        if (dt < 0) {
            if (DEBUG) {
                Log.d(TAG, "Called recycle from end with a negative value. This might happen"
                        + " during layout changes but may be sign of a bug");
            }
            return;
        }
        final int limit = mOrientationHelper.getEndAfterPadding() - dt;
        if (mShouldReverseLayout) {
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (mOrientationHelper.getDecoratedStart(child) < limit) {// stop here
                    recycleChildren(recycler, 0, i);
                    return;
                }
            }
        } else {
            for (int i = childCount - 1; i >= 0; i--) {
                View child = getChildAt(i);
                if (mOrientationHelper.getDecoratedStart(child) < limit) {// stop here
                    recycleChildren(recycler, childCount - 1, i);
                    return;
                }
            }
        }

    }

    /**
     * Helper method to call appropriate recycle method depending on current render layout
     * direction
     *
     * @param recycler    Current recycler that is attached to RecyclerView
     * @param renderState Current render state. Right now, this object does not change but
     *                    we may consider moving it out of this view so passing around as a
     *                    parameter for now, rather than accessing {@link #mRenderState}
     * @see #recycleViewsFromStart(android.support.v7.widget.RecyclerView.Recycler, int)
     * @see #recycleViewsFromEnd(android.support.v7.widget.RecyclerView.Recycler, int)
     * @see android.support.v7.widget.LinearLayoutManager.RenderState#mLayoutDirection
     */
    private void recycleByRenderState(RecyclerView.Recycler recycler, RenderState renderState) {
        if (renderState.mLayoutDirection == RenderState.LAYOUT_START) {
            recycleViewsFromEnd(recycler, renderState.mScrollingOffset);
        } else {
            recycleViewsFromStart(recycler, renderState.mScrollingOffset);
        }
    }

    /**
     * The magic functions :). Fills the given layout, defined by the renderState. This is fairly
     * independent from the rest of the {@link android.support.v7.widget.LinearLayoutManager}
     * and with little change, can be made publicly available as a helper class.
     *
     * @param recycler    Current recycler that is attached to RecyclerView
     * @param adapter     Current adapter that is attached to RecyclerView
     * @param renderState Configuration on how we should fill out the available space.
     * @return Number of pixels that it added. Useful for scoll functions.
     */
    private int fill(RecyclerView.Recycler recycler, RecyclerView.Adapter adapter,
            RenderState renderState) {
        // max offset we should set is mFastScroll + available
        final int start = renderState.mAvailable;
        if (renderState.mScrollingOffset != RenderState.SCOLLING_OFFSET_NaN) {
            // TODO ugly bug fix. should not happen
            if (renderState.mAvailable < 0) {
                renderState.mScrollingOffset += renderState.mAvailable;
            }
            recycleByRenderState(recycler, renderState);
        }
        while (renderState.mAvailable > 0 && renderState.hasMore(adapter)) {
            View view = renderState.next(recycler, adapter);
            if (mShouldReverseLayout) {
                if (renderState.mLayoutDirection == RenderState.LAYOUT_START) {
                    addView(view);
                } else {
                    addView(view, 0);
                }
            } else {
                if (renderState.mLayoutDirection == RenderState.LAYOUT_START) {
                    addView(view, 0);
                } else {
                    addView(view);
                }
            }
            measureChildWithMargins(view, 0, 0);
            int consumed = mOrientationHelper.getDecoratedMeasurement(view);
            int left, top, right, bottom;
            if (mOrientation == VERTICAL) {
                if (isLayoutRTL()) {
                    right = getWidth() - getPaddingRight();
                    left = right - mOrientationHelper.getDecoratedMeasurementInOther(view);
                } else {
                    left = getPaddingLeft();
                    right = left + mOrientationHelper.getDecoratedMeasurementInOther(view);
                }
                if (renderState.mLayoutDirection == RenderState.LAYOUT_START) {
                    bottom = renderState.mOffset;
                    top = renderState.mOffset - consumed;
                } else {
                    top = renderState.mOffset;
                    bottom = renderState.mOffset + consumed;
                }
            } else {
                top = getPaddingTop();
                bottom = top + mOrientationHelper.getDecoratedMeasurementInOther(view);

                if (renderState.mLayoutDirection == RenderState.LAYOUT_START) {
                    right = renderState.mOffset;
                    left = renderState.mOffset - consumed;
                } else {
                    left = renderState.mOffset;
                    right = renderState.mOffset + consumed;
                }
            }
            // We calculate everything with View's bounding box (which includes decor and margins)
            // To calculate correct layout position, we subtract margins.
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
            layoutDecorated(view, left + params.leftMargin, top + params.topMargin
                    , right - params.rightMargin, bottom - params.bottomMargin);
            renderState.mOffset += consumed * renderState.mLayoutDirection;
            renderState.mAvailable -= consumed;

            if (renderState.mScrollingOffset != RenderState.SCOLLING_OFFSET_NaN) {
                renderState.mScrollingOffset += consumed;
                if (renderState.mAvailable < 0) {
                    renderState.mScrollingOffset += renderState.mAvailable;
                }
                recycleByRenderState(recycler, renderState);
            }
        }
        if (DEBUG) {
            validateChildOrder();
        }
        return start - renderState.mAvailable;
    }


    /**
     * Used for debugging.
     * Logs the internal representation of children to default logger.
     */
    private void logChildren() {
        Log.d(TAG, "internal representation of views on the screen");
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            Log.d(TAG, "item " + getPosition(child) + ", coord:"
                    + mOrientationHelper.getDecoratedStart(child));
        }
        Log.d(TAG, "==============");
    }

    /**
     * Used for debugging.
     * Validates that child views are laid out in correct order. This is important because rest of
     * the algorithm relies on this constraint.
     *
     * In default layout, child 0 should be closest to screen position 0 and last child should be
     * closest to position WIDTH or HEIGHT.
     * In reverse layout, last child should be closes to screen position 0 and first child should
     * be closest to position WIDTH  or HEIGHT
     */
    private void validateChildOrder() {
        if (getChildCount() < 1) {
            return;
        }
        int lastPos = getPosition(getChildAt(0));
        int lastScreenLoc = mOrientationHelper.getDecoratedStart(getChildAt(0));
        if (mShouldReverseLayout) {
            for (int i = 1; i < getChildCount(); i++) {
                View child = getChildAt(i);
                int pos = getPosition(child);
                int screenLoc = mOrientationHelper.getDecoratedStart(child);
                if (pos < lastPos) {
                    logChildren();
                    throw new RuntimeException("detected invalid position. loc invalid? " +
                            (screenLoc < lastScreenLoc));
                }
                if (screenLoc > lastScreenLoc) {
                    logChildren();
                    throw new RuntimeException("detected invalid location");
                }
            }
        } else {
            for (int i = 1; i < getChildCount(); i++) {
                View child = getChildAt(i);
                int pos = getPosition(child);
                int screenLoc = mOrientationHelper.getDecoratedStart(child);
                if (pos < lastPos) {
                    logChildren();
                    throw new RuntimeException("detected invalid position. loc invalid? " +
                            (screenLoc < lastScreenLoc));
                }
                if (screenLoc < lastScreenLoc) {
                    logChildren();
                    throw new RuntimeException("detected invalid location");
                }
            }
        }
    }


    /**
     * Helper class that keeps temporary state while {LayoutManager} is filling out the empty
     * space.
     */
    private static class RenderState {

        final static String TAG = "LinearLayoutManager#RenderState";

        final static int LAYOUT_START = -1;

        final static int LAYOUT_END = 1;

        final static int ITEM_DIRECTION_HEAD = -1;

        final static int ITEM_DIRECTION_TAIL = 1;

        final static int SCOLLING_OFFSET_NaN = Integer.MIN_VALUE;

        /**
         * Pixel offset where rendering should start
         */
        int mOffset;

        /**
         * Number of pixels that we should fill, in the layout direction.
         * {@link #fill(RecyclerView.Recycler, RecyclerView.Adapter, RenderState)} tries to fill
         * at least this many pixels.
         */
        int mAvailable;

        /**
         * Current position on the adapter to get the next item.
         */
        int mCurrentPosition;

        /**
         * Defines the direction in which the data adapter is traversed.
         * Should be {@link #ITEM_DIRECTION_HEAD} or {@link #ITEM_DIRECTION_TAIL}
         */
        int mItemDirection;

        /**
         * Defines the direction in which the layout is filled.
         * Should be {@link #LAYOUT_START} or {@link #LAYOUT_END}
         */
        int mLayoutDirection;

        /**
         * Used when RenderState is constructed in a scrolling state.
         * It should be set the amount of scrolling we can make without creating a new view.
         * Settings this is required for efficient view recycling.
         */
        int mScrollingOffset;

        /**
         * @return true if there are more items in the data adapter
         */
        boolean hasMore(RecyclerView.Adapter adapter) {
            return mCurrentPosition >= 0 && mCurrentPosition < adapter.getItemCount();
        }

        /**
         * Gets the view for the next element that we should render.
         * Also updates current item index to the next item, based on {@link #mItemDirection}
         *
         * @return The next element that we should render.
         */
        View next(RecyclerView.Recycler recycler, RecyclerView.Adapter adapter) {
            final View view = recycler.getViewForPosition(adapter, mCurrentPosition);
            mCurrentPosition += mItemDirection;
            return view;
        }

        void log() {
            Log.d(TAG, "avail:" + mAvailable + ", ind:" + mCurrentPosition + ", dir:" +
                    mItemDirection + ", offset:" + mOffset + ", layoutDir:" + mLayoutDirection);
        }
    }

    private OrientationHelper createVerticalOrientationHelper() {
        return new OrientationHelper() {
            @Override
            public int getEndAfterPadding() {
                return getHeight() - getPaddingBottom();
            }

            @Override
            public void offsetChildren(int amount) {
                offsetChildrenVertical(amount);
            }

            @Override
            public int getStartAfterPadding() {
                return getPaddingTop();
            }

            @Override
            public int getDecoratedMeasurement(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                        view.getLayoutParams();
                return getDecoratedMeasuredHeight(view) + params.topMargin + params.bottomMargin;
            }

            @Override
            public int getDecoratedMeasurementInOther(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                        view.getLayoutParams();
                return getDecoratedMeasuredWidth(view) + params.leftMargin + params.rightMargin;
            }

            @Override
            public int getDecoratedEnd(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                        view.getLayoutParams();
                return getDecoratedBottom(view) + params.bottomMargin;
            }

            @Override
            public int getDecoratedStart(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                        view.getLayoutParams();
                return getDecoratedTop(view) - params.topMargin;
            }
        };
    }

    private OrientationHelper createHorizontalOrientationHelper() {
        return new OrientationHelper() {
            @Override
            public int getEndAfterPadding() {
                return getWidth() - getPaddingRight();
            }

            @Override
            public void offsetChildren(int amount) {
                offsetChildrenHorizontal(amount);
            }

            @Override
            public int getStartAfterPadding() {
                return getPaddingLeft();
            }

            @Override
            public int getDecoratedMeasurement(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                        view.getLayoutParams();
                return getDecoratedMeasuredWidth(view) + params.leftMargin + params.rightMargin;
            }

            @Override
            public int getDecoratedMeasurementInOther(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                        view.getLayoutParams();
                return getDecoratedMeasuredHeight(view) + params.topMargin + params.bottomMargin;
            }

            @Override
            public int getDecoratedEnd(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                        view.getLayoutParams();
                return getDecoratedRight(view) + params.rightMargin;
            }

            @Override
            public int getDecoratedStart(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                        view.getLayoutParams();
                return getDecoratedLeft(view) - params.leftMargin;
            }
        };
    }


    /**
     * Helper interface to offload orientation based decisions
     */
    private static interface OrientationHelper {

        /**
         * @param view The view element to check
         * @return The first pixel of the element
         * @see #getDecoratedEnd(android.view.View)
         */
        int getDecoratedStart(View view);

        /**
         * @param view The view element to check
         * @return The last pixel of the element
         * @see #getDecoratedStart(android.view.View)
         */
        int getDecoratedEnd(View view);

        /**
         * @param view The view element to check
         * @return Total space occupied by this view
         */
        int getDecoratedMeasurement(View view);

        /**
         * @param view The view element to check
         * @return Total space occupied by this view in the perpendicular orientation to current one
         */
        int getDecoratedMeasurementInOther(View view);

        /**
         * @return The very first pixel we can draw.
         */
        int getStartAfterPadding();

        /**
         * @return The last pixel we can draw
         */
        int getEndAfterPadding();

        /**
         * Offsets all children's positions by the given amount
         *
         * @param amount Value to add to each child's layout parameters
         */
        void offsetChildren(int amount);
    }
}
