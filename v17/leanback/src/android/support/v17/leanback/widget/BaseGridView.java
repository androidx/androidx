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
import android.support.v7.widget.SimpleItemAnimator;

/**
 * An abstract base class for vertically and horizontally scrolling lists. The items come
 * from the {@link RecyclerView.Adapter} associated with this view.
 * Do not directly use this class, use {@link VerticalGridView} and {@link HorizontalGridView}.
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
     * For HorizontalGridView, low edge refers to left edge when RTL is false or
     * right edge when RTL is true.
     * For VerticalGridView, low edge refers to top edge.
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
     * For HorizontalGridView, high edge refers to right edge when RTL is false or
     * left edge when RTL is true.
     * For VerticalGridView, high edge refers to bottom edge.
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
    public final static float ITEM_ALIGN_OFFSET_PERCENT_DISABLED =
            ItemAlignmentFacet.ITEM_ALIGN_OFFSET_PERCENT_DISABLED;

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

    public interface OnUnhandledKeyListener {
        /**
         * Returns true if the key event should be consumed.
         */
        public boolean onUnhandledKey(KeyEvent event);
    }

    final GridLayoutManager mLayoutManager;

    /**
     * Animate layout changes from a child resizing or adding/removing a child.
     */
    private boolean mAnimateChildLayout = true;

    private boolean mHasOverlappingRendering = true;

    private RecyclerView.ItemAnimator mSavedItemAnimator;

    private OnTouchInterceptListener mOnTouchInterceptListener;
    private OnMotionInterceptListener mOnMotionInterceptListener;
    private OnKeyInterceptListener mOnKeyInterceptListener;
    private RecyclerView.RecyclerListener mChainedRecyclerListener;
    private OnUnhandledKeyListener mOnUnhandledKeyListener;

    public BaseGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mLayoutManager = new GridLayoutManager(this);
        setLayoutManager(mLayoutManager);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setHasFixedSize(true);
        setChildrenDrawingOrderEnabled(true);
        setWillNotDraw(true);
        setOverScrollMode(View.OVER_SCROLL_NEVER);
        // Disable change animation by default on leanback.
        // Change animation will create a new view and cause undesired
        // focus animation between the old view and new view.
        ((SimpleItemAnimator)getItemAnimator()).setSupportsChangeAnimations(false);
        super.setRecyclerListener(new RecyclerView.RecyclerListener() {
            @Override
            public void onViewRecycled(RecyclerView.ViewHolder holder) {
                mLayoutManager.onChildRecycled(holder);
                if (mChainedRecyclerListener != null) {
                    mChainedRecyclerListener.onViewRecycled(holder);
                }
            }
        });
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
     * Sets the strategy used to scroll in response to item focus changing:
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
     * Sets the method for focused item alignment in the view.
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
     * Returns the method for focused item alignment in the view.
     *
     * @return {@link #WINDOW_ALIGN_BOTH_EDGE}, {@link #WINDOW_ALIGN_LOW_EDGE},
     *         {@link #WINDOW_ALIGN_HIGH_EDGE} or {@link #WINDOW_ALIGN_NO_EDGE}.
     */
    public int getWindowAlignment() {
        return mLayoutManager.getWindowAlignment();
    }

    /**
     * Sets the offset in pixels for window alignment.
     *
     * @param offset The number of pixels to offset.  If the offset is positive,
     *        it is distance from low edge (see {@link #WINDOW_ALIGN_LOW_EDGE});
     *        if the offset is negative, the absolute value is distance from high
     *        edge (see {@link #WINDOW_ALIGN_HIGH_EDGE}).
     *        Default value is 0.
     */
    public void setWindowAlignmentOffset(int offset) {
        mLayoutManager.setWindowAlignmentOffset(offset);
        requestLayout();
    }

    /**
     * Returns the offset in pixels for window alignment.
     *
     * @return The number of pixels to offset.  If the offset is positive,
     *        it is distance from low edge (see {@link #WINDOW_ALIGN_LOW_EDGE});
     *        if the offset is negative, the absolute value is distance from high
     *        edge (see {@link #WINDOW_ALIGN_HIGH_EDGE}).
     *        Default value is 0.
     */
    public int getWindowAlignmentOffset() {
        return mLayoutManager.getWindowAlignmentOffset();
    }

    /**
     * Sets the offset percent for window alignment in addition to {@link
     * #getWindowAlignmentOffset()}.
     *
     * @param offsetPercent Percentage to offset. E.g., 40 means 40% of the
     *        width from low edge. Use
     *        {@link #WINDOW_ALIGN_OFFSET_PERCENT_DISABLED} to disable.
     *         Default value is 50.
     */
    public void setWindowAlignmentOffsetPercent(float offsetPercent) {
        mLayoutManager.setWindowAlignmentOffsetPercent(offsetPercent);
        requestLayout();
    }

    /**
     * Returns the offset percent for window alignment in addition to
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
     * Sets the absolute offset in pixels for item alignment.
     * Item alignment settings are ignored for the child if {@link ItemAlignmentFacet}
     * is provided by {@link RecyclerView.ViewHolder} or {@link FacetProviderAdapter}.
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
     * Returns the absolute offset in pixels for item alignment.
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
     * Item alignment settings are ignored for the child if {@link ItemAlignmentFacet}
     * is provided by {@link RecyclerView.ViewHolder} or {@link FacetProviderAdapter}.
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
     * Sets the offset percent for item alignment in addition to {@link
     * #getItemAlignmentOffset()}.
     * Item alignment settings are ignored for the child if {@link ItemAlignmentFacet}
     * is provided by {@link RecyclerView.ViewHolder} or {@link FacetProviderAdapter}.
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
     * Returns the offset percent for item alignment in addition to {@link
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
     * Sets the id of the view to align with. Use {@link android.view.View#NO_ID} (default)
     * for the item view itself.
     * Item alignment settings are ignored for the child if {@link ItemAlignmentFacet}
     * is provided by {@link RecyclerView.ViewHolder} or {@link FacetProviderAdapter}.
     */
    public void setItemAlignmentViewId(int viewId) {
        mLayoutManager.setItemAlignmentViewId(viewId);
    }

    /**
     * Returns the id of the view to align with, or zero for the item view itself.
     */
    public int getItemAlignmentViewId() {
        return mLayoutManager.getItemAlignmentViewId();
    }

    /**
     * Sets the margin in pixels between two child items.
     */
    public void setItemMargin(int margin) {
        mLayoutManager.setItemMargin(margin);
        requestLayout();
    }

    /**
     * Sets the margin in pixels between two child items vertically.
     */
    public void setVerticalMargin(int margin) {
        mLayoutManager.setVerticalMargin(margin);
        requestLayout();
    }

    /**
     * Returns the margin in pixels between two child items vertically.
     */
    public int getVerticalMargin() {
        return mLayoutManager.getVerticalMargin();
    }

    /**
     * Sets the margin in pixels between two child items horizontally.
     */
    public void setHorizontalMargin(int margin) {
        mLayoutManager.setHorizontalMargin(margin);
        requestLayout();
    }

    /**
     * Returns the margin in pixels between two child items horizontally.
     */
    public int getHorizontalMargin() {
        return mLayoutManager.getHorizontalMargin();
    }

    /**
     * Registers a callback to be invoked when an item in BaseGridView has
     * been laid out.
     *
     * @param listener The listener to be invoked.
     */
    public void setOnChildLaidOutListener(OnChildLaidOutListener listener) {
        mLayoutManager.setOnChildLaidOutListener(listener);
    }

    /**
     * Registers a callback to be invoked when an item in BaseGridView has
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
     * Registers a callback to be invoked when an item in BaseGridView has
     * been selected.  Note that the listener may be invoked when there is a
     * layout pending on the view, affording the listener an opportunity to
     * adjust the upcoming layout based on the selection state.
     *
     * @param listener The listener to be invoked.
     */
    public void setOnChildViewHolderSelectedListener(OnChildViewHolderSelectedListener listener) {
        mLayoutManager.setOnChildViewHolderSelectedListener(listener);
    }

    /**
     * Changes the selected item immediately without animation.
     */
    public void setSelectedPosition(int position) {
        mLayoutManager.setSelection(this, position, 0);
    }

    /**
     * Changes the selected item and/or subposition immediately without animation.
     */
    public void setSelectedPositionWithSub(int position, int subposition) {
        mLayoutManager.setSelectionWithSub(this, position, subposition, 0);
    }

    /**
     * Changes the selected item immediately without animation, scrollExtra is
     * applied in primary scroll direction.  The scrollExtra will be kept until
     * another {@link #setSelectedPosition} or {@link #setSelectedPositionSmooth} call.
     */
    public void setSelectedPosition(int position, int scrollExtra) {
        mLayoutManager.setSelection(this, position, scrollExtra);
    }

    /**
     * Changes the selected item and/or subposition immediately without animation, scrollExtra is
     * applied in primary scroll direction.  The scrollExtra will be kept until
     * another {@link #setSelectedPosition} or {@link #setSelectedPositionSmooth} call.
     */
    public void setSelectedPositionWithSub(int position, int subposition, int scrollExtra) {
        mLayoutManager.setSelectionWithSub(this, position, subposition, scrollExtra);
    }

    /**
     * Changes the selected item and run an animation to scroll to the target
     * position.
     */
    public void setSelectedPositionSmooth(int position) {
        mLayoutManager.setSelectionSmooth(this, position);
    }

    /**
     * Changes the selected item and/or subposition, runs an animation to scroll to the target
     * position.
     */
    public void setSelectedPositionSmoothWithSub(int position, int subposition) {
        mLayoutManager.setSelectionSmoothWithSub(this, position, subposition);
    }

    /**
     * Returns the selected item position.
     */
    public int getSelectedPosition() {
        return mLayoutManager.getSelection();
    }

    /**
     * Returns the sub selected item position started from zero.  An item can have
     * multiple {@link ItemAlignmentFacet}s provided by {@link RecyclerView.ViewHolder}
     * or {@link FacetProviderAdapter}.  Zero is returned when no {@link ItemAlignmentFacet}
     * is defined.
     */
    public int getSelectedSubPosition() {
        return mLayoutManager.getSubSelection();
    }

    /**
     * Sets whether an animation should run when a child changes size or when adding
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
     * Returns true if an animation will run when a child changes size or when
     * adding or removing a child.
     * <p><i>Unstable API, might change later.</i>
     */
    public boolean isChildLayoutAnimated() {
        return mAnimateChildLayout;
    }

    /**
     * Sets the gravity used for child view positioning. Defaults to
     * GRAVITY_TOP|GRAVITY_START.
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
     * Returns the x/y offsets to final position from current position if the view
     * is selected.
     *
     * @param view The view to get offsets.
     * @param offsets offsets[0] holds offset of X, offsets[1] holds offset of Y.
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

    @Override
    public View focusSearch(int direction) {
        if (isFocused()) {
            // focusSearch(int) is called when GridView itself is focused.
            // Calling focusSearch(view, int) to get next sibling of current selected child.
            View view = mLayoutManager.findViewByPosition(mLayoutManager.getSelection());
            if (view != null) {
                return focusSearch(view, direction);
            }
        }
        // otherwise, go to mParent to perform focusSearch
        return super.focusSearch(direction);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        mLayoutManager.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    /**
     * Disables or enables focus search.
     */
    public final void setFocusSearchDisabled(boolean disabled) {
        // LayoutManager may detachView and attachView in fastRelayout, it causes RowsFragment
        // re-gain focus after a BACK key pressed, so block children focus during transition.
        setDescendantFocusability(disabled ? FOCUS_BLOCK_DESCENDANTS: FOCUS_AFTER_DESCENDANTS);
        mLayoutManager.setFocusSearchDisabled(disabled);
    }

    /**
     * Returns true if focus search is disabled.
     */
    public final boolean isFocusSearchDisabled() {
        return mLayoutManager.isFocusSearchDisabled();
    }

    /**
     * Enables or disables layout.  All children will be removed when layout is
     * disabled.
     */
    public void setLayoutEnabled(boolean layoutEnabled) {
        mLayoutManager.setLayoutEnabled(layoutEnabled);
    }

    /**
     * Changes and overrides children's visibility.
     */
    public void setChildrenVisibility(int visibility) {
        mLayoutManager.setChildrenVisibility(visibility);
    }

    /**
     * Enables or disables pruning of children.  Disable is useful during transition.
     */
    public void setPruneChild(boolean pruneChild) {
        mLayoutManager.setPruneChild(pruneChild);
    }

    /**
     * Enables or disables scrolling.  Disable is useful during transition.
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
     * in front of it.  This will return true if first item view is not created.
     * So application should check in both {@link OnChildSelectedListener} and {@link
     * OnChildLaidOutListener}.
     *
     * @param position Position in adapter.
     */
    public boolean hasPreviousViewInSameRow(int position) {
        return mLayoutManager.hasPreviousViewInSameRow(position);
    }

    /**
     * Enables or disables the default "focus draw at last" order rule.
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

    /**
     * Sets the unhandled key listener.
     */
    public void setOnUnhandledKeyListener(OnUnhandledKeyListener listener) {
        mOnUnhandledKeyListener = listener;
    }

    /**
     * Returns the unhandled key listener.
     */
    public OnUnhandledKeyListener getOnUnhandledKeyListener() {
        return mOnUnhandledKeyListener;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mOnKeyInterceptListener != null && mOnKeyInterceptListener.onInterceptKeyEvent(event)) {
            return true;
        }
        if (super.dispatchKeyEvent(event)) {
            return true;
        }
        if (mOnUnhandledKeyListener != null && mOnUnhandledKeyListener.onUnhandledKey(event)) {
            return true;
        }
        return false;
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
     * Returns the policy for saving children.
     *
     * @return policy, one of {@link #SAVE_NO_CHILD}
     * {@link #SAVE_ON_SCREEN_CHILD} {@link #SAVE_LIMITED_CHILD} {@link #SAVE_ALL_CHILD}.
     */
    public final int getSaveChildrenPolicy() {
        return mLayoutManager.mChildrenStates.getSavePolicy();
    }

    /**
     * Returns the limit used when when {@link #getSaveChildrenPolicy()} is
     *         {@link #SAVE_LIMITED_CHILD}
     */
    public final int getSaveChildrenLimitNumber() {
        return mLayoutManager.mChildrenStates.getLimitNumber();
    }

    /**
     * Sets the policy for saving children.
     * @param savePolicy One of {@link #SAVE_NO_CHILD} {@link #SAVE_ON_SCREEN_CHILD}
     * {@link #SAVE_LIMITED_CHILD} {@link #SAVE_ALL_CHILD}.
     */
    public final void setSaveChildrenPolicy(int savePolicy) {
        mLayoutManager.mChildrenStates.setSavePolicy(savePolicy);
    }

    /**
     * Sets the limit number when {@link #getSaveChildrenPolicy()} is {@link #SAVE_LIMITED_CHILD}.
     */
    public final void setSaveChildrenLimitNumber(int limitNumber) {
        mLayoutManager.mChildrenStates.setLimitNumber(limitNumber);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return mHasOverlappingRendering;
    }

    public void setHasOverlappingRendering(boolean hasOverlapping) {
        mHasOverlappingRendering = hasOverlapping;
    }

    /**
     * Notify layout manager that layout directionality has been updated
     */
    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        mLayoutManager.onRtlPropertiesChanged(layoutDirection);
    }

    @Override
    public void setRecyclerListener(RecyclerView.RecyclerListener listener) {
        mChainedRecyclerListener = listener;
    }

    /**
     * Sets pixels of extra space for layout child in invisible area.
     *
     * @param extraLayoutSpace  Pixels of extra space for layout invisible child.
     *                          Must be bigger or equals to 0.
     * @hide
     */
    public void setExtraLayoutSpace(int extraLayoutSpace) {
        mLayoutManager.setExtraLayoutSpace(extraLayoutSpace);
    }

    /**
     * Returns pixels of extra space for layout child in invisible area.
     *
     * @hide
     */
    public int getExtraLayoutSpace() {
        return mLayoutManager.getExtraLayoutSpace();
    }
}
