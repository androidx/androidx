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
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

/**
 * Base class for vertically and horizontally scrolling lists. The items come
 * from the {@link RecyclerView.Adapter} associated with this view.
 * @hide
 */
abstract class BaseGridView extends RecyclerView {

    /**
     * Always keep focused item at a aligned position.  Developer can use
     * WINDOW_ALIGN_XXX and ITEM_ALIGN_XXX to define how focused item is aligned.
     * In this mode, the last focused position will be remembered and restored when focus
     * is back to the view.
     */
    public final static int FOCUS_SCROLL_ALIGNED = 0;

    /**
     * Scroll to make the focused item inside client area.
     */
    public final static int FOCUS_SCROLL_ITEM = 1;

    /**
     * Scroll a page of items when focusing to item outside the client area.
     * The page size matches the client area size of RecyclerView.
     */
    public final static int FOCUS_SCROLL_PAGE = 2;

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

    /**
     * Dont save states of any child views.
     */
    public static final int SAVE_NO_CHILD = 0;

    /**
     * Only save on screen child views, the states are lost when they become off screen.
     */
    public static final int SAVE_ON_SCREEN_CHILD = 1;

    /**
     * Save on screen views plus save off screen child views states up to
     * {@link #getSaveChildrenLimitNumber()}.
     */
    public static final int SAVE_LIMITED_CHILD = 2;

    /**
     * Save on screen views plus save off screen child views without any limitation.
     * This might cause out of memory, only use it when you are dealing with limited data.
     */
    public static final int SAVE_ALL_CHILD = 3;

    /**
     * Listener for intercepting touch dispatch events.
     */
    public interface OnTouchInterceptListener {
        /**
         * Returns true if the touch dispatch event should be consumed.
         */
        public boolean onInterceptTouchEvent(MotionEvent event);
    }

    /**
     * Listener for intercepting generic motion dispatch events.
     */
    public interface OnMotionInterceptListener {
        /**
         * Returns true if the touch dispatch event should be consumed.
         */
        public boolean onInterceptMotionEvent(MotionEvent event);
    }

    /**
     * Listener for intercepting key dispatch events.
     */
    public interface OnKeyInterceptListener {
        /**
         * Returns true if the key dispatch event should be consumed.
         */
        public boolean onInterceptKeyEvent(KeyEvent event);
    }

    protected final GridLayoutManager mLayoutManager;

    /**
     * Animate layout changes from a child resizing or adding/removing a child.
     */
    private boolean mAnimateChildLayout = true;

    private boolean mHasOverlappingRendering = true;

    private RecyclerView.ItemAnimator mSavedItemAnimator;

    private OnTouchInterceptListener mOnTouchInterceptListener;
    private OnMotionInterceptListener mOnMotionInterceptListener;
    private OnKeyInterceptListener mOnKeyInterceptListener;

    public BaseGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mLayoutManager = new GridLayoutManager(this);
        setLayoutManager(mLayoutManager);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setHasFixedSize(true);
        setChildrenDrawingOrderEnabled(true);
        setWillNotDraw(true);
        setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    protected void initBaseGridViewAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.lbBaseGridView);
        boolean throughFront = a.getBoolean(R.styleable.lbBaseGridView_focusOutFront, false);
        boolean throughEnd = a.getBoolean(R.styleable.lbBaseGridView_focusOutEnd, false);
        mLayoutManager.setFocusOutAllowed(throughFront, throughEnd);
        mLayoutManager.setVerticalMargin(
                a.getDimensionPixelSize(R.styleable.lbBaseGridView_verticalMargin, 0));
        mLayoutManager.setHorizontalMargin(
                a.getDimensionPixelSize(R.styleable.lbBaseGridView_horizontalMargin, 0));
        if (a.hasValue(R.styleable.lbBaseGridView_android_gravity)) {
            setGravity(a.getInt(R.styleable.lbBaseGridView_android_gravity, Gravity.NO_GRAVITY));
        }
        a.recycle();
    }

    /**
     * Set the strategy used to scroll in response to item focus changing:
     * <ul>
     * <li>{@link #FOCUS_SCROLL_ALIGNED} (default) </li>
     * <li>{@link #FOCUS_SCROLL_ITEM}</li>
     * <li>{@link #FOCUS_SCROLL_PAGE}</li>
     * </ul>
     */
    public void setFocusScrollStrategy(int scrollStrategy) {
        if (scrollStrategy != FOCUS_SCROLL_ALIGNED && scrollStrategy != FOCUS_SCROLL_ITEM
            && scrollStrategy != FOCUS_SCROLL_PAGE) {
            throw new IllegalArgumentException("Invalid scrollStrategy");
        }
        mLayoutManager.setFocusScrollStrategy(scrollStrategy);
        requestLayout();
    }

    /**
     * Returns the strategy used to scroll in response to item focus changing.
     * <ul>
     * <li>{@link #FOCUS_SCROLL_ALIGNED} (default) </li>
     * <li>{@link #FOCUS_SCROLL_ITEM}</li>
     * <li>{@link #FOCUS_SCROLL_PAGE}</li>
     * </ul>
     */
    public int getFocusScrollStrategy() {
        return mLayoutManager.getFocusScrollStrategy();
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
     * #getWindowAlignmentOffset()}.
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
     * {@link #getWindowAlignmentOffset()}.
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
     * Set to true if include padding in calculating item align offset.
     *
     * @param withPadding When it is true: we include left/top padding for positive
     *          item offset, include right/bottom padding for negative item offset.
     */
    public void setItemAlignmentOffsetWithPadding(boolean withPadding) {
        mLayoutManager.setItemAlignmentOffsetWithPadding(withPadding);
        requestLayout();
    }

    /**
     * Returns true if include padding in calculating item align offset.
     */
    public boolean isItemAlignmentOffsetWithPadding() {
        return mLayoutManager.isItemAlignmentOffsetWithPadding();
    }

    /**
     * Set offset percent for item alignment in addition to {@link
     * #getItemAlignmentOffset()}.
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
     * #getItemAlignmentOffset()}.
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
     * Set the margin in pixels between two child items horizontally.
     */
    public void setHorizontalMargin(int margin) {
        mLayoutManager.setHorizontalMargin(margin);
        requestLayout();
    }

    /**
     * Get the margin in pixels between two child items horizontally.
     */
    public int getHorizontalMargin() {
        return mLayoutManager.getHorizontalMargin();
    }

    /**
     * Register a callback to be invoked when an item in BaseGridView has
     * been selected.  Note that the listener may be invoked when there is a
     * layout pending on the view, affording the listener an opportunity to
     * adjust the upcoming layout based on the selection state.
     *
     * @param listener The listener to be invoked.
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
        if (mAnimateChildLayout != animateChildLayout) {
            mAnimateChildLayout = animateChildLayout;
            if (!mAnimateChildLayout) {
                mSavedItemAnimator = getItemAnimator();
                super.setItemAnimator(null);
            } else {
                super.setItemAnimator(mSavedItemAnimator);
            }
        }
    }

    /**
     * Return true if an animation will run when a child changes size or when
     * adding or removing a child.
     * <p><i>Unstable API, might change later.</i>
     */
    public boolean isChildLayoutAnimated() {
        return mAnimateChildLayout;
    }

    /**
     * Describes how the child views are positioned. Defaults to
     * GRAVITY_TOP|GRAVITY_LEFT.
     *
     * @param gravity See {@link android.view.Gravity}
     */
    public void setGravity(int gravity) {
        mLayoutManager.setGravity(gravity);
        requestLayout();
    }

    @Override
    public boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        return mLayoutManager.gridOnRequestFocusInDescendants(this, direction,
                previouslyFocusedRect);
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

    @Override
    public int getChildDrawingOrder(int childCount, int i) {
        return mLayoutManager.getChildDrawingOrder(this, childCount, i);
    }

    final boolean isChildrenDrawingOrderEnabledInternal() {
        return isChildrenDrawingOrderEnabled();
    }

    /**
     * Disable or enable focus search.
     */
    public final void setFocusSearchDisabled(boolean disabled) {
        mLayoutManager.setFocusSearchDisabled(disabled);
    }

    /**
     * Return true if focus search is disabled.
     */
    public final boolean isFocusSearchDisabled() {
        return mLayoutManager.isFocusSearchDisabled();
    }

    /**
     * Enable or disable layout.  All children will be removed when layout is
     * disabled.
     */
    public void setLayoutEnabled(boolean layoutEnabled) {
        mLayoutManager.setLayoutEnabled(layoutEnabled);
    }

    /**
     * Change and override children's visibility.
     */
    public void setChildrenVisibility(int visibility) {
        mLayoutManager.setChildrenVisibility(visibility);
    }

    /**
     * Enable or disable pruning child.  Disable is useful during transition.
     */
    public void setPruneChild(boolean pruneChild) {
        mLayoutManager.setPruneChild(pruneChild);
    }

    /**
     * Enable or disable scrolling.  Disable is useful during transition.
     */
    public void setScrollEnabled(boolean scrollEnabled) {
        mLayoutManager.setScrollEnabled(scrollEnabled);
    }

    /**
     * Returns true if scrolling is enabled.
     */
    public boolean isScrollEnabled() {
        return mLayoutManager.isScrollEnabled();
    }

    /**
     * Returns true if the view at the given position has a same row sibling
     * in front of it.
     *
     * @param position Position in adapter.
     */
    public boolean hasPreviousViewInSameRow(int position) {
        return mLayoutManager.hasPreviousViewInSameRow(position);
    }

    /**
     * Enable or disable the default "focus draw at last" order rule.
     */
    public void setFocusDrawingOrderEnabled(boolean enabled) {
        super.setChildrenDrawingOrderEnabled(enabled);
    }

    /**
     * Returns true if default "focus draw at last" order rule is enabled.
     */
    public boolean isFocusDrawingOrderEnabled() {
        return super.isChildrenDrawingOrderEnabled();
    }

    /**
     * Sets the touch intercept listener.
     */
    public void setOnTouchInterceptListener(OnTouchInterceptListener listener) {
        mOnTouchInterceptListener = listener;
    }

    /**
     * Sets the generic motion intercept listener.
     */
    public void setOnMotionInterceptListener(OnMotionInterceptListener listener) {
        mOnMotionInterceptListener = listener;
    }

    /**
     * Sets the key intercept listener.
     */
    public void setOnKeyInterceptListener(OnKeyInterceptListener listener) {
        mOnKeyInterceptListener = listener;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mOnKeyInterceptListener != null) {
            if (mOnKeyInterceptListener.onInterceptKeyEvent(event)) {
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mOnTouchInterceptListener != null) {
            if (mOnTouchInterceptListener.onInterceptTouchEvent(event)) {
                return true;
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchGenericFocusedEvent(MotionEvent event) {
        if (mOnMotionInterceptListener != null) {
            if (mOnMotionInterceptListener.onInterceptMotionEvent(event)) {
                return true;
            }
        }
        return super.dispatchGenericFocusedEvent(event);
    }

    /**
     * @return policy for saving children.  One of {@link #SAVE_NO_CHILD}
     * {@link #SAVE_ON_SCREEN_CHILD} {@link #SAVE_LIMITED_CHILD} {@link #SAVE_ALL_CHILD}.
     */
    public final int getSaveChildrenPolicy() {
        return mLayoutManager.mChildrenStates.getSavePolicy();
    }

    /**
     * @return The limit number when {@link #getSaveChildrenPolicy()} is
     *         {@link #SAVE_LIMITED_CHILD}
     */
    public final int getSaveChildrenLimitNumber() {
        return mLayoutManager.mChildrenStates.getLimitNumber();
    }

    /**
     * Set policy for saving children.
     * @param savePolicy One of {@link #SAVE_NO_CHILD} {@link #SAVE_ON_SCREEN_CHILD}
     * {@link #SAVE_LIMITED_CHILD} {@link #SAVE_ALL_CHILD}.
     */
    public final void setSaveChildrenPolicy(int savePolicy) {
        mLayoutManager.mChildrenStates.setSavePolicy(savePolicy);
    }

    /**
     * Set limit number when {@link #getSaveChildrenPolicy()} is {@link #SAVE_LIMITED_CHILD}.
     */
    public final void setSaveChildrenLimitNumber(int limitNumber) {
        mLayoutManager.mChildrenStates.setLimitNumber(limitNumber);
    }

    /**
     * Set the factor by which children should be laid out beyond the view bounds
     * in the direction of orientation.  1.0 disables over reach.
     *
     * @param fraction fraction of over reach
     */
    public final void setPrimaryOverReach(float fraction) {
        mLayoutManager.setPrimaryOverReach(fraction);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return mHasOverlappingRendering;
    }

    public void setHasOverlappingRendering(boolean hasOverlapping) {
        mHasOverlappingRendering = hasOverlapping;
    }
}
