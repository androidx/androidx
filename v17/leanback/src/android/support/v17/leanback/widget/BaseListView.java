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
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.v17.leanback.R;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;

import java.util.ArrayList;

/**
 * Base class for vertically and horizontally scrolling lists. The items come
 * from the {@link RecyclerView.Adapter} associated with this view.
 * @hide
 */
abstract class BaseListView extends RecyclerView {

    /**
     * The first item is aligned with the low edge of the viewport. When
     * navigating away from the first item, the focus maintains a middle
     * location.
     * <p>
     * The middle location is calculated by "windowAlignOffset" and
     * "windowAlignOffsetPercent"; if neither of these two is defined, the
     * default value is 1/2 of the size.
     */
    public final static int WINDOW_ALIGN_LOW_EDGE = 1;

    /**
     * The last item is aligned with the high edge of the viewport when
     * navigating to the end of list. When navigating away from the end, the
     * focus maintains a middle location.
     * <p>
     * The middle location is calculated by "windowAlignOffset" and
     * "windowAlignOffsetPercent"; if neither of these two is defined, the
     * default value is 1/2 of the size.
     */
    public final static int WINDOW_ALIGN_HIGH_EDGE = 1 << 1;

    /**
     * The first item and last item are aligned with the two edges of the
     * viewport. When navigating in the middle of list, the focus maintains a
     * middle location.
     * <p>
     * The middle location is calculated by "windowAlignOffset" and
     * "windowAlignOffsetPercent"; if neither of these two is defined, the
     * default value is 1/2 of the size.
     */
    public final static int WINDOW_ALIGN_BOTH_EDGE =
            WINDOW_ALIGN_LOW_EDGE | WINDOW_ALIGN_HIGH_EDGE;

    /**
     * The focused item always stays in a middle location.
     * <p>
     * The middle location is calculated by "windowAlignOffset" and
     * "windowAlignOffsetPercent"; if neither of these two is defined, the
     * default value is 1/2 of the size.
     */
    public final static int WINDOW_ALIGN_NO_EDGE = 0;

    /**
     * Value indicates that percent is not used.
     */
    public final static float WINDOW_ALIGN_OFFSET_PERCENT_DISABLED = -1;

    /**
     * Value indicates that percent is not used.
     */
    public final static float ITEM_ALIGN_OFFSET_PERCENT_DISABLED = -1;

    protected final GridLayoutManager mLayoutManager;

    private int[] mMeasuredSize = new int[2];

    public BaseListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mLayoutManager = new GridLayoutManager(this);
        setLayoutManager(mLayoutManager);
        setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        setHasFixedSize(true);
    }

    protected void initBaseListViewAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.lbBaseListView);
        boolean throughFront = a.getBoolean(R.styleable.lbBaseListView_focusOutFront, false);
        boolean throughEnd = a.getBoolean(R.styleable.lbBaseListView_focusOutEnd, false);
        mLayoutManager.setFocusOutAllowed(throughFront, throughEnd);
        mLayoutManager.setVerticalMargin(
                a.getDimensionPixelSize(R.styleable.lbBaseListView_verticalMargin, 0));
        mLayoutManager.setHorizontalMargin(
                a.getDimensionPixelSize(R.styleable.lbBaseListView_horizontalMargin, 0));
        a.recycle();
    }

    /**
     * Set how the focused item gets aligned in the view.
     *
     * @param windowAlignment {@link #WINDOW_ALIGN_BOTH_EDGE},
     *        {@link #WINDOW_ALIGN_LOW_EDGE}, {@link #WINDOW_ALIGN_HIGH_EDGE} or
     *        {@link #WINDOW_ALIGN_NO_EDGE}.
     */
    public void setWindowAlignment(int windowAlignment) {
        mLayoutManager.setWindowAlignment(windowAlignment);
        requestLayout();
    }

    /**
     * Get how the focused item gets aligned in the view.
     *
     * @return {@link #WINDOW_ALIGN_BOTH_EDGE}, {@link #WINDOW_ALIGN_LOW_EDGE},
     *         {@link #WINDOW_ALIGN_HIGH_EDGE} or {@link #WINDOW_ALIGN_NO_EDGE}.
     */
    public int getWindowAlignment() {
        return mLayoutManager.getWindowAlignment();
    }

    /**
     * Set the absolute offset in pixels for window alignment.
     *
     * @param offset The number of pixels to offset. Can be negative for
     *        alignment from the high edge, or positive for alignment from the
     *        low edge.
     */
    public void setWindowAlignmentOffset(int offset) {
        mLayoutManager.setWindowAlignmentOffset(offset);
        requestLayout();
    }

    /**
     * Get the absolute offset in pixels for window alignment.
     *
     * @return The number of pixels to offset. Will be negative for alignment
     *         from the high edge, or positive for alignment from the low edge.
     *         Default value is 0.
     */
    public int getWindowAlignmentOffset() {
        return mLayoutManager.getWindowAlignmentOffset();
    }

    /**
     * Set offset percent for window alignment in addition to {@link
     * #getWindowAlignOffset()}.
     *
     * @param offsetPercent Percentage to offset. E.g., 40 means 40% of the
     *        width from low edge. Use
     *        {@link #WINDOW_ALIGN_OFFSET_PERCENT_DISABLED} to disable.
     */
    public void setWindowAlignmentOffsetPercent(float offsetPercent) {
        mLayoutManager.setWindowAlignmentOffsetPercent(offsetPercent);
        requestLayout();
    }

    /**
     * Get offset percent for window alignment in addition to
     * {@link #getWindowAlignOffset()}.
     *
     * @return Percentage to offset. E.g., 40 means 40% of the width from the 
     *         low edge, or {@link #WINDOW_ALIGN_OFFSET_PERCENT_DISABLED} if
     *         disabled. Default value is 50.
     */
    public float getWindowAlignmentOffsetPercent() {
        return mLayoutManager.getWindowAlignmentOffsetPercent();
    }

    /**
     * Set the absolute offset in pixels for item alignment.
     *
     * @param offset The number of pixels to offset. Can be negative for
     *        alignment from the high edge, or positive for alignment from the
     *        low edge.
     */
    public void setItemAlignmentOffset(int offset) {
        mLayoutManager.setItemAlignmentOffset(offset);
        requestLayout();
    }

    /**
     * Get the absolute offset in pixels for item alignment.
     *
     * @return The number of pixels to offset. Will be negative for alignment
     *         from the high edge, or positive for alignment from the low edge.
     *         Default value is 0.
     */
    public int getItemAlignmentOffset() {
        return mLayoutManager.getItemAlignmentOffset();
    }

    /**
     * Set offset percent for item alignment in addition to {@link
     * #getItemAlignOffset()}.
     *
     * @param offsetPercent Percentage to offset. E.g., 40 means 40% of the
     *        width from the low edge. Use
     *        {@link #ITEM_ALIGN_OFFSET_PERCENT_DISABLED} to disable.
     */
    public void setItemAlignmentOffsetPercent(float offsetPercent) {
        mLayoutManager.setItemAlignmentOffsetPercent(offsetPercent);
        requestLayout();
    }

    /**
     * Get offset percent for item alignment in addition to {@link
     * #getItemAlignOffset()}.
     *
     * @return Percentage to offset. E.g., 40 means 40% of the width from the
     *         low edge, or {@link #ITEM_ALIGN_OFFSET_PERCENT_DISABLED} if
     *         disabled. Default value is 50.
     */
    public float getItemAlignmentOffsetPercent() {
        return mLayoutManager.getItemAlignmentOffsetPercent();
    }

    /**
     * Set the id of the view to align with. Use zero (default) for the item
     * view itself.
     */
    public void setItemAlignmentViewId(int viewId) {
        mLayoutManager.setItemAlignmentViewId(viewId);
    }

    /**
     * Get the id of the view to align with, or zero for the item view itself.
     */
    public int getItemAlignmentViewId() {
        return mLayoutManager.getItemAlignmentViewId();
    }

    /**
     * Set the margin in pixels between two child items.
     */
    public void setItemMargin(int margin) {
        mLayoutManager.setItemMargin(margin);
        requestLayout();
    }

    @Deprecated
    public void setMargin(int margin) {
        setItemMargin(margin);
    }

    /**
     * Set the margin in pixels between two child items vertically.
     */
    public void setVerticalMargin(int margin) {
        mLayoutManager.setVerticalMargin(margin);
        requestLayout();
    }

    /**
     * Get the margin in pixels between two child items vertically.
     */
    public int getVerticalMargin() {
        return mLayoutManager.getVerticalMargin();
    }

    /**
     * Register a callback to be invoked when an item in BaseListView has
     * been selected.
     */
    public void setOnChildSelectedListener(OnChildSelectedListener listener) {
        mLayoutManager.setOnChildSelectedListener(listener);
    }

    /**
     * Change the selected item immediately without animation.
     */
    public void setSelectedPosition(int position) {
        mLayoutManager.setSelection(this, position);
    }

    /**
     * Change the selected item and run an animation to scroll to the target
     * position.
     */
    public void setSelectedPositionSmooth(int position) {
        mLayoutManager.setSelectionSmooth(this, position);
    }

    /**
     * Get the selected item position.
     */
    public int getSelectedPosition() {
        return mLayoutManager.getSelection();
    }

    /**
     * Set if an animation should run when a child changes size or when adding
     * or removing a child.
     * <p><i>Unstable API, might change later.</i>
     */
    public void setAnimateChildLayout(boolean animateChildLayout) {
        mLayoutManager.setAnimateChildLayout(animateChildLayout);
    }

    /**
     * Return true if an animation will run when a child changes size or when
     * adding or removing a child.
     * <p><i>Unstable API, might change later.</i>
     */
    public boolean isChildLayoutAnimated() {
        return mLayoutManager.isChildLayoutAnimated();
    }

    /**
     * Set an interpolator for the animation when a child changes size or when 
     * adding or removing a child.
     * <p><i>Unstable API, might change later.</i>
     */
    public void setChildLayoutAnimationInterpolator(Interpolator interpolator) {
        mLayoutManager.setChildLayoutAnimationInterpolator(interpolator);
    }

    /**
     * Get the interpolator for the animation when a child changes size or when
     * adding or removing a child.
     * <p><i>Unstable API, might change later.</i>
     */
    public Interpolator getChildLayoutAnimationInterpolator() {
        return mLayoutManager.getChildLayoutAnimationInterpolator();
    }

    /**
     * Set the duration of the animation when a child changes size or when 
     * adding or removing a child.
     * <p><i>Unstable API, might change later.</i>
     */
    public void setChildLayoutAnimationDuration(long duration) {
        mLayoutManager.setChildLayoutAnimationDuration(duration);
    }

    /**
     * Get the duration of the animation when a child changes size or when 
     * adding or removing a child.
     * <p><i>Unstable API, might change later.</i>
     */
    public long getChildLayoutAnimationDuration() {
        return mLayoutManager.getChildLayoutAnimationDuration();
    }

    @Override
    protected final void onMeasure(int widthSpec, int heightSpec) {
        mLayoutManager.onMeasure(widthSpec, heightSpec, mMeasuredSize);
        setMeasuredDimension(mMeasuredSize[0], mMeasuredSize[1]);
    }

    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        if (mLayoutManager.focusSelectedChild(direction, previouslyFocusedRect)) {
            return true;
        }
        return super.requestFocus(direction, previouslyFocusedRect);
    }

    /**
     * Get the x/y offsets to final position from current position if the view
     * is selected.
     *
     * @param view The view to get offsets.
     * @param offsets offsets[0] holds offset of X, offsets[1] holds offset of
     *        Y.
     */
    public void getViewSelectedOffsets(View view, int[] offsets) {
        mLayoutManager.getViewSelectedOffsets(view, offsets);
    }
}
