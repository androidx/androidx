/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.appcompat.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.R;
import androidx.appcompat.graphics.drawable.DrawableWrapper;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewPropertyAnimatorCompat;
import androidx.core.widget.ListViewAutoScrollHelper;

import java.lang.reflect.Field;

/**
 * <p>Wrapper class for a ListView. This wrapper can hijack the focus to
 * make sure the list uses the appropriate drawables and states when
 * displayed on screen within a drop down. The focus is never actually
 * passed to the drop down in this mode; the list only looks focused.</p>
 */
class DropDownListView extends ListView {
    public static final int INVALID_POSITION = -1;
    public static final int NO_POSITION = -1;

    private final Rect mSelectorRect = new Rect();
    private int mSelectionLeftPadding = 0;
    private int mSelectionTopPadding = 0;
    private int mSelectionRightPadding = 0;
    private int mSelectionBottomPadding = 0;

    private int mMotionPosition;

    private Field mIsChildViewEnabled;

    private GateKeeperDrawable mSelector;

    /*
    * WARNING: This is a workaround for a touch mode issue.
    *
    * Touch mode is propagated lazily to windows. This causes problems in
    * the following scenario:
    * - Type something in the AutoCompleteTextView and get some results
    * - Move down with the d-pad to select an item in the list
    * - Move up with the d-pad until the selection disappears
    * - Type more text in the AutoCompleteTextView *using the soft keyboard*
    *   and get new results; you are now in touch mode
    * - The selection comes back on the first item in the list, even though
    *   the list is supposed to be in touch mode
    *
    * Using the soft keyboard triggers the touch mode change but that change
    * is propagated to our window only after the first list layout, therefore
    * after the list attempts to resurrect the selection.
    *
    * The trick to work around this issue is to pretend the list is in touch
    * mode when we know that the selection should not appear, that is when
    * we know the user moved the selection away from the list.
    *
    * This boolean is set to true whenever we explicitly hide the list's
    * selection and reset to false whenever we know the user moved the
    * selection back to the list.
    *
    * When this boolean is true, isInTouchMode() returns true, otherwise it
    * returns super.isInTouchMode().
    */
    private boolean mListSelectionHidden;

    /**
     * True if this wrapper should fake focus.
     */
    private boolean mHijackFocus;

    /** Whether to force drawing of the pressed state selector. */
    private boolean mDrawsInPressedState;

    /** Current drag-to-open click animation, if any. */
    private ViewPropertyAnimatorCompat mClickAnimation;

    /** Helper for drag-to-open auto scrolling. */
    private ListViewAutoScrollHelper mScrollHelper;

    /**
     * Runnable posted when we are awaiting hover event resolution. When set,
     * drawable state changes are postponed.
     */
    private ResolveHoverRunnable mResolveHoverRunnable;

    /**
     * <p>Creates a new list view wrapper.</p>
     *
     * @param context this view's context
     */
    DropDownListView(Context context, boolean hijackFocus) {
        super(context, null, R.attr.dropDownListViewStyle);
        mHijackFocus = hijackFocus;
        setCacheColorHint(0); // Transparent, since the background drawable could be anything.

        try {
            mIsChildViewEnabled = AbsListView.class.getDeclaredField("mIsChildViewEnabled");
            mIsChildViewEnabled.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean isInTouchMode() {
        // WARNING: Please read the comment where mListSelectionHidden is declared
        return (mHijackFocus && mListSelectionHidden) || super.isInTouchMode();
    }

    /**
     * <p>Returns the focus state in the drop down.</p>
     *
     * @return true always if hijacking focus
     */
    @Override
    public boolean hasWindowFocus() {
        return mHijackFocus || super.hasWindowFocus();
    }

    /**
     * <p>Returns the focus state in the drop down.</p>
     *
     * @return true always if hijacking focus
     */
    @Override
    public boolean isFocused() {
        return mHijackFocus || super.isFocused();
    }

    /**
     * <p>Returns the focus state in the drop down.</p>
     *
     * @return true always if hijacking focus
     */
    @Override
    public boolean hasFocus() {
        return mHijackFocus || super.hasFocus();
    }

    @Override
    public void setSelector(Drawable sel) {
        mSelector = sel != null ? new GateKeeperDrawable(sel) : null;
        super.setSelector(mSelector);

        final Rect padding = new Rect();
        if (sel != null) {
            sel.getPadding(padding);
        }

        mSelectionLeftPadding = padding.left;
        mSelectionTopPadding = padding.top;
        mSelectionRightPadding = padding.right;
        mSelectionBottomPadding = padding.bottom;
    }

    @Override
    protected void drawableStateChanged() {
        //postpone drawableStateChanged until pending hover to pressed transition finishes.
        if (mResolveHoverRunnable != null) {
            return;
        }

        super.drawableStateChanged();

        setSelectorEnabled(true);
        updateSelectorStateCompat();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        final boolean drawSelectorOnTop = false;
        if (!drawSelectorOnTop) {
            drawSelectorCompat(canvas);
        }

        super.dispatchDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mMotionPosition = pointToPosition((int) ev.getX(), (int) ev.getY());
                break;
        }
        if (mResolveHoverRunnable != null) {
            // Resolved hover event as hover => touch transition.
            mResolveHoverRunnable.cancel();
        }
        return super.onTouchEvent(ev);
    }

    /**
     * Find a position that can be selected (i.e., is not a separator).
     *
     * @param position The starting position to look at.
     * @param lookDown Whether to look down for other positions.
     * @return The next selectable position starting at position and then searching either up or
     *         down. Returns {@link #INVALID_POSITION} if nothing can be found.
     */
    public int lookForSelectablePosition(int position, boolean lookDown) {
        final ListAdapter adapter = getAdapter();
        if (adapter == null || isInTouchMode()) {
            return INVALID_POSITION;
        }

        final int count = adapter.getCount();
        if (!getAdapter().areAllItemsEnabled()) {
            if (lookDown) {
                position = Math.max(0, position);
                while (position < count && !adapter.isEnabled(position)) {
                    position++;
                }
            } else {
                position = Math.min(position, count - 1);
                while (position >= 0 && !adapter.isEnabled(position)) {
                    position--;
                }
            }

            if (position < 0 || position >= count) {
                return INVALID_POSITION;
            }
            return position;
        } else {
            if (position < 0 || position >= count) {
                return INVALID_POSITION;
            }
            return position;
        }
    }

    /**
     * Measures the height of the given range of children (inclusive) and returns the height
     * with this ListView's padding and divider heights included. If maxHeight is provided, the
     * measuring will stop when the current height reaches maxHeight.
     *
     * @param widthMeasureSpec             The width measure spec to be given to a child's
     *                                     {@link View#measure(int, int)}.
     * @param startPosition                The position of the first child to be shown.
     * @param endPosition                  The (inclusive) position of the last child to be
     *                                     shown. Specify {@link #NO_POSITION} if the last child
     *                                     should be the last available child from the adapter.
     * @param maxHeight                    The maximum height that will be returned (if all the
     *                                     children don't fit in this value, this value will be
     *                                     returned).
     * @param disallowPartialChildPosition In general, whether the returned height should only
     *                                     contain entire children. This is more powerful--it is
     *                                     the first inclusive position at which partial
     *                                     children will not be allowed. Example: it looks nice
     *                                     to have at least 3 completely visible children, and
     *                                     in portrait this will most likely fit; but in
     *                                     landscape there could be times when even 2 children
     *                                     can not be completely shown, so a value of 2
     *                                     (remember, inclusive) would be good (assuming
     *                                     startPosition is 0).
     * @return The height of this ListView with the given children.
     */
    public int measureHeightOfChildrenCompat(int widthMeasureSpec, int startPosition,
            int endPosition, final int maxHeight,
            int disallowPartialChildPosition) {

        final int paddingTop = getListPaddingTop();
        final int paddingBottom = getListPaddingBottom();
        final int paddingLeft = getListPaddingLeft();
        final int paddingRight = getListPaddingRight();
        final int reportedDividerHeight = getDividerHeight();
        final Drawable divider = getDivider();

        final ListAdapter adapter = getAdapter();

        if (adapter == null) {
            return paddingTop + paddingBottom;
        }

        // Include the padding of the list
        int returnedHeight = paddingTop + paddingBottom;
        final int dividerHeight = ((reportedDividerHeight > 0) && divider != null)
                ? reportedDividerHeight : 0;

        // The previous height value that was less than maxHeight and contained
        // no partial children
        int prevHeightWithoutPartialChild = 0;

        View child = null;
        int viewType = 0;
        int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            int newType = adapter.getItemViewType(i);
            if (newType != viewType) {
                child = null;
                viewType = newType;
            }
            child = adapter.getView(i, child, this);

            // Compute child height spec
            int heightMeasureSpec;
            ViewGroup.LayoutParams childLp = child.getLayoutParams();

            if (childLp == null) {
                childLp = generateDefaultLayoutParams();
                child.setLayoutParams(childLp);
            }

            if (childLp.height > 0) {
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(childLp.height,
                        MeasureSpec.EXACTLY);
            } else {
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            }
            child.measure(widthMeasureSpec, heightMeasureSpec);

            // Since this view was measured directly against the parent measure
            // spec, we must measure it again before reuse.
            child.forceLayout();

            if (i > 0) {
                // Count the divider for all but one child
                returnedHeight += dividerHeight;
            }

            returnedHeight += child.getMeasuredHeight();

            if (returnedHeight >= maxHeight) {
                // We went over, figure out which height to return.  If returnedHeight >
                // maxHeight, then the i'th position did not fit completely.
                return (disallowPartialChildPosition >= 0) // Disallowing is enabled (> -1)
                        && (i > disallowPartialChildPosition) // We've past the min pos
                        && (prevHeightWithoutPartialChild > 0) // We have a prev height
                        && (returnedHeight != maxHeight) // i'th child did not fit completely
                        ? prevHeightWithoutPartialChild
                        : maxHeight;
            }

            if ((disallowPartialChildPosition >= 0) && (i >= disallowPartialChildPosition)) {
                prevHeightWithoutPartialChild = returnedHeight;
            }
        }

        // At this point, we went through the range of children, and they each
        // completely fit, so return the returnedHeight
        return returnedHeight;
    }

    private void setSelectorEnabled(boolean enabled) {
        if (mSelector != null) {
            mSelector.setEnabled(enabled);
        }
    }

    private static class GateKeeperDrawable extends DrawableWrapper {
        private boolean mEnabled;

        GateKeeperDrawable(Drawable drawable) {
            super(drawable);
            mEnabled = true;
        }

        void setEnabled(boolean enabled) {
            mEnabled = enabled;
        }

        @Override
        public boolean setState(int[] stateSet) {
            if (mEnabled) {
                return super.setState(stateSet);
            }
            return false;
        }

        @Override
        public void draw(Canvas canvas) {
            if (mEnabled) {
                super.draw(canvas);
            }
        }

        @Override
        public void setHotspot(float x, float y) {
            if (mEnabled) {
                super.setHotspot(x, y);
            }
        }

        @Override
        public void setHotspotBounds(int left, int top, int right, int bottom) {
            if (mEnabled) {
                super.setHotspotBounds(left, top, right, bottom);
            }
        }

        @Override
        public boolean setVisible(boolean visible, boolean restart) {
            if (mEnabled) {
                return super.setVisible(visible, restart);
            }
            return false;
        }
    }

    @Override
    public boolean onHoverEvent(@NonNull MotionEvent ev) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // For SDK_INT prior to O the code below fails to change the selection.
            // This is because prior to O mouse events used to enable touch mode, and
            //  View.setSelectionFromTop does not do the right thing in touch mode.
            return super.onHoverEvent(ev);
        }

        final int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_HOVER_EXIT && mResolveHoverRunnable == null) {
            // This may be transitioning to TOUCH_DOWN. Postpone drawable state
            // updates until either the next frame or the next touch event.
            mResolveHoverRunnable = new ResolveHoverRunnable();
            mResolveHoverRunnable.post();
        }

        // Allow the super class to handle hover state management first.
        final boolean handled = super.onHoverEvent(ev);
        if (action == MotionEvent.ACTION_HOVER_ENTER
                || action == MotionEvent.ACTION_HOVER_MOVE) {
            final int position = pointToPosition((int) ev.getX(), (int) ev.getY());

            if (position != INVALID_POSITION && position != getSelectedItemPosition()) {
                final View hoveredItem = getChildAt(position - getFirstVisiblePosition());
                if (hoveredItem.isEnabled()) {
                    // Force a focus on the hovered item so that
                    // the proper selector state gets used when we update.
                    setSelectionFromTop(position, hoveredItem.getTop() - this.getTop());
                }
                updateSelectorStateCompat();
            }
        } else {
            // Do not cancel the selected position if the selection is visible
            // by other means.
            setSelection(INVALID_POSITION);
        }

        return handled;
    }

    @Override
    protected void onDetachedFromWindow() {
        mResolveHoverRunnable = null;
        super.onDetachedFromWindow();
    }

    /**
     * Handles forwarded events.
     *
     * @param activePointerId id of the pointer that activated forwarding
     * @return whether the event was handled
     */
    public boolean onForwardedEvent(MotionEvent event, int activePointerId) {
        boolean handledEvent = true;
        boolean clearPressedItem = false;

        final int actionMasked = event.getActionMasked();
        switch (actionMasked) {
            case MotionEvent.ACTION_CANCEL:
                handledEvent = false;
                break;
            case MotionEvent.ACTION_UP:
                handledEvent = false;
                // $FALL-THROUGH$
            case MotionEvent.ACTION_MOVE:
                final int activeIndex = event.findPointerIndex(activePointerId);
                if (activeIndex < 0) {
                    handledEvent = false;
                    break;
                }

                final int x = (int) event.getX(activeIndex);
                final int y = (int) event.getY(activeIndex);
                final int position = pointToPosition(x, y);
                if (position == INVALID_POSITION) {
                    clearPressedItem = true;
                    break;
                }

                final View child = getChildAt(position - getFirstVisiblePosition());
                setPressedItem(child, position, x, y);
                handledEvent = true;

                if (actionMasked == MotionEvent.ACTION_UP) {
                    clickPressedItem(child, position);
                }
                break;
        }

        // Failure to handle the event cancels forwarding.
        if (!handledEvent || clearPressedItem) {
            clearPressedItem();
        }

        // Manage automatic scrolling.
        if (handledEvent) {
            if (mScrollHelper == null) {
                mScrollHelper = new ListViewAutoScrollHelper(this);
            }
            mScrollHelper.setEnabled(true);
            mScrollHelper.onTouch(this, event);
        } else if (mScrollHelper != null) {
            mScrollHelper.setEnabled(false);
        }

        return handledEvent;
    }

    /**
     * Starts an alpha animation on the selector. When the animation ends,
     * the list performs a click on the item.
     */
    private void clickPressedItem(final View child, final int position) {
        final long id = getItemIdAtPosition(position);
        performItemClick(child, position, id);
    }

    /**
     * Sets whether the list selection is hidden, as part of a workaround for a
     * touch mode issue (see the declaration for mListSelectionHidden).
     *
     * @param hideListSelection {@code true} to hide list selection,
     *                          {@code false} to show
     */
    void setListSelectionHidden(boolean hideListSelection) {
        mListSelectionHidden = hideListSelection;
    }

    private void updateSelectorStateCompat() {
        Drawable selector = getSelector();
        if (selector != null && touchModeDrawsInPressedStateCompat() && isPressed()) {
            selector.setState(getDrawableState());
        }
    }

    private void drawSelectorCompat(Canvas canvas) {
        if (!mSelectorRect.isEmpty()) {
            final Drawable selector = getSelector();
            if (selector != null) {
                selector.setBounds(mSelectorRect);
                selector.draw(canvas);
            }
        }
    }

    private void positionSelectorLikeTouchCompat(int position, View sel, float x, float y) {
        positionSelectorLikeFocusCompat(position, sel);

        Drawable selector = getSelector();
        if (selector != null && position != INVALID_POSITION) {
            DrawableCompat.setHotspot(selector, x, y);
        }
    }

    private void positionSelectorLikeFocusCompat(int position, View sel) {
        // If we're changing position, update the visibility since the selector
        // is technically being detached from the previous selection.
        final Drawable selector = getSelector();
        final boolean manageState = selector != null && position != INVALID_POSITION;
        if (manageState) {
            selector.setVisible(false, false);
        }

        positionSelectorCompat(position, sel);

        if (manageState) {
            final Rect bounds = mSelectorRect;
            final float x = bounds.exactCenterX();
            final float y = bounds.exactCenterY();
            selector.setVisible(getVisibility() == VISIBLE, false);
            DrawableCompat.setHotspot(selector, x, y);
        }
    }

    private void positionSelectorCompat(int position, View sel) {
        final Rect selectorRect = mSelectorRect;
        selectorRect.set(sel.getLeft(), sel.getTop(), sel.getRight(), sel.getBottom());

        // Adjust for selection padding.
        selectorRect.left -= mSelectionLeftPadding;
        selectorRect.top -= mSelectionTopPadding;
        selectorRect.right += mSelectionRightPadding;
        selectorRect.bottom += mSelectionBottomPadding;

        try {
            // AbsListView.mIsChildViewEnabled controls the selector's state so we need to
            // modify its value
            final boolean isChildViewEnabled = mIsChildViewEnabled.getBoolean(this);
            if (sel.isEnabled() != isChildViewEnabled) {
                mIsChildViewEnabled.set(this, !isChildViewEnabled);
                if (position != INVALID_POSITION) {
                    refreshDrawableState();
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void clearPressedItem() {
        mDrawsInPressedState = false;
        setPressed(false);
        // This will call through to updateSelectorState()
        drawableStateChanged();

        final View motionView = getChildAt(mMotionPosition - getFirstVisiblePosition());
        if (motionView != null) {
            motionView.setPressed(false);
        }

        if (mClickAnimation != null) {
            mClickAnimation.cancel();
            mClickAnimation = null;
        }
    }

    private void setPressedItem(View child, int position, float x, float y) {
        mDrawsInPressedState = true;

        // Ordering is essential. First, update the container's pressed state.
        if (Build.VERSION.SDK_INT >= 21) {
            drawableHotspotChanged(x, y);
        }
        if (!isPressed()) {
            setPressed(true);
        }

        // Next, run layout to stabilize child positions.
        layoutChildren();

        // Manage the pressed view based on motion position. This allows us to
        // play nicely with actual touch and scroll events.
        if (mMotionPosition != INVALID_POSITION) {
            final View motionView = getChildAt(mMotionPosition - getFirstVisiblePosition());
            if (motionView != null && motionView != child && motionView.isPressed()) {
                motionView.setPressed(false);
            }
        }
        mMotionPosition = position;

        // Offset for child coordinates.
        final float childX = x - child.getLeft();
        final float childY = y - child.getTop();
        if (Build.VERSION.SDK_INT >= 21) {
            child.drawableHotspotChanged(childX, childY);
        }
        if (!child.isPressed()) {
            child.setPressed(true);
        }

        // Ensure that keyboard focus starts from the last touched position.
        positionSelectorLikeTouchCompat(position, child, x, y);

        // This needs some explanation. We need to disable the selector for this next call
        // due to the way that ListViewCompat works. Otherwise both ListView and ListViewCompat
        // will draw the selector and bad things happen.
        setSelectorEnabled(false);

        // Refresh the drawable state to reflect the new pressed state,
        // which will also update the selector state.
        refreshDrawableState();
    }

    private boolean touchModeDrawsInPressedStateCompat() {
        return mDrawsInPressedState;
    }

    /**
     * Runnable that forces hover event resolution and updates drawable state.
     */
    private class ResolveHoverRunnable implements Runnable {
        @Override
        public void run() {
            // Resolved hover event as standard hover exit.
            mResolveHoverRunnable = null;
            drawableStateChanged();
        }

        public void cancel() {
            mResolveHoverRunnable = null;
            removeCallbacks(this);
        }

        public void post() {
            DropDownListView.this.post(this);
        }
    }
}
