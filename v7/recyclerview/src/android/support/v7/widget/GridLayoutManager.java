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
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;

/**
 * A {@link RecyclerView.LayoutManager} implementations that lays out items in a grid.
 * <p>
 * By default, each item occupies 1 span. You can change it by providing a custom
 * {@link SpanSizeLookup} instance via {@link #setSpanSizeLookup(SpanSizeLookup)}.
 */
public class GridLayoutManager extends LinearLayoutManager {

    private static final boolean DEBUG = false;
    private static final String TAG = "GridLayoutManager";
    public static final int DEFAULT_SPAN_COUNT = -1;
    int mSpanCount = DEFAULT_SPAN_COUNT;
    /**
     * The size of each span
     */
    int mSizePerSpan;
    /**
     * Temporary array to keep views in layoutChunk method
     */
    View[] mSet;

    /**
     * The measure spec for the perpendicular orientation to {@link #getOrientation()}.
     */
    static int OTHER_DIM_SPEC = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

    SpanSizeLookup mSpanSizeLookup = new DefaultSpanSizeLookup();

    // re-used variable to acquire decor insets from RecyclerView
    final Rect mDecorInsets = new Rect();

    public GridLayoutManager(Context context, int spanCount) {
        super(context);
        setSpanCount(spanCount);
    }

    public GridLayoutManager(Context context, int spanCount, int orientation,
            boolean reverseLayout) {
        super(context, orientation, reverseLayout);
        setSpanCount(spanCount);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        super.onLayoutChildren(recycler, state);
        if (DEBUG) {
            validateChildOrder();
        }
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

    /**
     * Sets the source to get the number of spans occupied by each item in the adapter.
     *
     * @param spanSizeLookup {@link SpanSizeLookup} instance to be used to query number of spans
     *                                             occupied by each item
     */
    public void setSpanSizeLookup(SpanSizeLookup spanSizeLookup) {
        mSpanSizeLookup = spanSizeLookup;
    }

    /**
     * Returns the current {@link SpanSizeLookup} used by the GridLayoutManager.
     *
     * @return The current {@link SpanSizeLookup} used by the GridLayoutManager.
     */
    public SpanSizeLookup getSpanSizeLookup() {
        return mSpanSizeLookup;
    }

    private void updateMeasurements() {
        int totalSpace;
        if (getOrientation() == VERTICAL) {
            totalSpace = getWidth() - getPaddingRight() - getPaddingLeft();
        } else {
            totalSpace = getHeight() - getPaddingBottom() - getPaddingTop();
        }
        mSizePerSpan = totalSpace / mSpanCount;
    }

    @Override
    void onAnchorReady(LinearLayoutManager.AnchorInfo anchorInfo) {
        super.onAnchorReady(anchorInfo);
        updateMeasurements();
        int span = mSpanSizeLookup.getSpanIndex(anchorInfo.mPosition, mSpanCount);
        while (span > 0 && anchorInfo.mPosition > 0) {
            anchorInfo.mPosition --;
            span -= mSpanSizeLookup.getSpanSize(anchorInfo.mPosition);
        }
        if (mSet == null || mSet.length != mSpanCount) {
            mSet = new View[mSpanCount];
        }
    }

    @Override
    void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state,
            LayoutState layoutState, LayoutChunkResult result) {
        int count = 0;
        int remainingSpan = mSpanCount;
        while (count < mSpanCount && layoutState.hasMore(state) && remainingSpan > 0) {
            int pos = layoutState.mCurrentPosition;
            final int spanSize = mSpanSizeLookup.getSpanSize(pos);
            if (spanSize > mSpanCount) {
                throw new IllegalArgumentException("Item at position " + pos + " requires " +
                        spanSize + " spans but GridLayoutManager has only " + mSpanCount
                        + " spans.");
            }
            remainingSpan -= spanSize;
            if (remainingSpan < 0) {
                break; // item did not fit into this row or column
            }
            View view = layoutState.next(recycler);
            if (view == null) {
                break;
            }
            mSet[count] = view;
            count ++;
        }

        if (count == 0) {
            result.mFinished = true;
            return;
        }

        int maxSize = 0;
        final boolean layingOutInPrimaryDirection = mShouldReverseLayout ==
                (layoutState.mLayoutDirection == LayoutState.LAYOUT_START);
        // we should assign spans before item decor offsets are calculated
        assignSpans(count, layingOutInPrimaryDirection);
        for (int i = 0; i < count; i ++) {
            View view = mSet[i];
            if (layingOutInPrimaryDirection) {
                addView(view);
            } else {
                addView(view, 0);
            }
            int spanSize = mSpanSizeLookup.getSpanSize(getPosition(view));
            final int spec = View.MeasureSpec.makeMeasureSpec(mSizePerSpan * spanSize,
                    View.MeasureSpec.EXACTLY);
            if (mOrientation == VERTICAL) {
                measureChildWithDecorationsAndMargin(view, spec, OTHER_DIM_SPEC);
            } else {
                measureChildWithDecorationsAndMargin(view, OTHER_DIM_SPEC, spec);
            }
            final int size = mOrientationHelper.getDecoratedMeasurement(view);
            if (size > maxSize) {
                maxSize = size;
            }
        }
        result.mConsumed = maxSize;

        int left = 0, right = 0, top = 0, bottom = 0;
        if (mOrientation == VERTICAL) {
            if (layoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
                bottom = layoutState.mOffset;
                top = bottom - maxSize;
            } else {
                top = layoutState.mOffset;
                bottom = top + maxSize;
            }
        } else {
            if (layoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
                right = layoutState.mOffset;
                left = right - maxSize;
            } else {
                left = layoutState.mOffset;
                right = left + maxSize;
            }
        }
        for (int i = 0; i < count; i ++) {
            View view = mSet[i];
            LayoutParams params = (LayoutParams) view.getLayoutParams();
            if (mOrientation == VERTICAL) {
                left = getPaddingLeft() + mSizePerSpan * params.mSpanIndex;
                right = left + mOrientationHelper.getDecoratedMeasurementInOther(view);
            } else {
                top = getPaddingTop() + mSizePerSpan * params.mSpanIndex;
                bottom = top + mOrientationHelper.getDecoratedMeasurementInOther(view);
            }
            // We calculate everything with View's bounding box (which includes decor and margins)
            // To calculate correct layout position, we subtract margins.
            layoutDecorated(view, left + params.leftMargin, top + params.topMargin,
                    right - params.rightMargin, bottom - params.bottomMargin);
            if (DEBUG) {
                Log.d(TAG, "laid out child at position " + getPosition(view) + ", with l:"
                        + (left + params.leftMargin) + ", t:" + (top + params.topMargin) + ", r:"
                        + (right - params.rightMargin) + ", b:" + (bottom - params.bottomMargin)
                        + ", span:" + params.mSpanIndex + ", spanSize:" + params.mSpanSize);
            }
            // Consume the available space if the view is not removed OR changed
            if (params.isItemRemoved() || params.isItemChanged()) {
                result.mIgnoreConsumed = true;
            }
            result.mFocusable |= view.isFocusable();
        }
        Arrays.fill(mSet, null);
    }

    private void measureChildWithDecorationsAndMargin(View child, int widthSpec, int heightSpec) {
        calculateItemDecorationsForChild(child, mDecorInsets);
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
        widthSpec = updateSpecWithExtra(widthSpec, lp.leftMargin + mDecorInsets.left,
                lp.rightMargin + mDecorInsets.right);
        heightSpec = updateSpecWithExtra(heightSpec, lp.topMargin + mDecorInsets.top,
                lp.bottomMargin + mDecorInsets.bottom);
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

    private void assignSpans(int count, boolean layingOutInPrimaryDirection) {
        int span, spanDiff, start, end, diff;
        // make sure we traverse from min position to max position
        if (layingOutInPrimaryDirection) {
            start = 0;
            end = count;
            diff = 1;
        } else {
            start = count - 1;
            end = -1;
            diff = -1;
        }
        if (mOrientation == VERTICAL && isLayoutRTL()) { // start from last span
            span = mSpanCount - 1;
            spanDiff = -1;
        } else {
            span = 0;
            spanDiff = 1;
        }
        for (int i = start; i != end; i += diff) {
            View view = mSet[i];
            LayoutParams params = (LayoutParams) view.getLayoutParams();
            params.mSpanSize = mSpanSizeLookup.getSpanSize(getPosition(view));
            if (spanDiff == -1 && params.mSpanSize > 1) {
                params.mSpanIndex = span - (params.mSpanSize - 1);
            } else {
                params.mSpanIndex = span;
            }
            span += spanDiff * params.mSpanSize;
        }
    }

    /**
     * Returns the number of spans laid out by this grid.
     *
     * @return The number of spans
     * @see #setSpanCount(int)
     */
    public int getSpanCount() {
        return mSpanCount;
    }

    /**
     * Sets the number of spans to be laid out.
     * <p>
     * If {@link #getOrientation()} is {@link #VERTICAL}, this is the number of columns.
     * If {@link #getOrientation()} is {@link #HORIZONTAL}, this is the number of rows.
     *
     * @param spanCount The total number of spans in the grid
     * @see #getSpanCount()
     */
    public void setSpanCount(int spanCount) {
        if (spanCount == mSpanCount) {
            return;
        }
        if (spanCount < 1) {
            throw new IllegalArgumentException("Span count should be at least 1. Provided "
                    + spanCount);
        }
        mSpanCount = spanCount;
    }

    /**
     * A helper class to provide the number of spans each item occupies.
     * <p>
     * Default implementation sets each item to occupy exactly 1 span.
     *
     * @see GridLayoutManager#setSpanSizeLookup(SpanSizeLookup)
     */
    public static abstract class SpanSizeLookup {
        /**
         * Returns the number of span occupied by the item at <code>position</code>.
         *
         * @param position The adapter position of the item
         * @return The number of spans occupied by the item at the provided position
         */
        abstract public int getSpanSize(int position);

        /**
         * Returns the final span index of the provided position.
         * <p>
         * Default implementation traverses all items before the current position to decide which
         * span offset this item should be positioned at. You can override this method if you have
         * a faster way to calculate it based on your data set.
         * <p>
         * Note that span offsets always start with 0 and is not affected by RTL.
         *
         * @param position The position of the item
         * @param spanCount The total number of spans in the grid
         * @return The final span position of the item. Should be between 0 (inclusive) and
         * <code>spanCount</code>(exclusive)
         */
        public int getSpanIndex(int position, int spanCount) {
            int positionSpanSize = getSpanSize(position);
            if (positionSpanSize == spanCount) {
                return 0; // quick return for full-span items
            }
            int span = 0;
            for (int i = 0; i < position; i++) {
                int size = getSpanSize(i);
                span += size;
                if (span == spanCount) {
                    span = 0;
                } else if (span > spanCount) {
                    // did not fit, moving to next row / column
                    span = size;
                }
            }
            if (span + positionSpanSize <= spanCount) {
                return span;
            }
            return 0;
        }
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return false;
    }

    /**
     * Default implementation for {@link SpanSizeLookup}. Each item occupies 1 span.
     */
    public static final class DefaultSpanSizeLookup extends SpanSizeLookup {
        @Override
        public int getSpanSize(int position) {
            return 1;
        }

        @Override
        public int getSpanIndex(int position, int spanCount) {
            return position % spanCount;
        }
    }

    /**
     * LayoutParams used by GridLayoutManager.
     */
    public static class LayoutParams extends RecyclerView.LayoutParams {

        /**
         * Span Id for Views that are not laid out yet.
         */
        public static final int INVALID_SPAN_ID = -1;

        private int mSpanIndex = INVALID_SPAN_ID;

        private int mSpanSize = 0;

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
         * Returns the current span index of this View. If the View is not laid out yet, the return
         * value is <code>undefined</code>.
         * <p>
         * Note that span index may change by whether the RecyclerView is RTL or not. For
         * example, if the number of spans is 3 and layout is RTL, the rightmost item will have
         * span index of 2. If the layout changes back to LTR, span index for this view will be 0.
         * If the item was occupying 2 spans, span indices would be 1 and 0 respectively.
         * <p>
         * If the View occupies multiple spans, span with the minimum index is returned.
         *
         * @return The span index of the View.
         */
        public int getSpanIndex() {
            return mSpanIndex;
        }

        /**
         * Returns the number of spans occupied by this View. If the View not laid out yet, the
         * return value is <code>undefined</code>.
         *
         * @return The number of spans occupied by this View.
         */
        public int getSpanSize() {
            return mSpanSize;
        }
    }

}
