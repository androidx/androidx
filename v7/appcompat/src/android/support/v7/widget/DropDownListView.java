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

package android.support.v7.widget;

import android.content.Context;
import android.os.Build;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.widget.ListViewAutoScrollHelper;
import android.support.v7.appcompat.R;
import android.view.MotionEvent;
import android.view.View;

/**
 * <p>Wrapper class for a ListView. This wrapper can hijack the focus to
 * make sure the list uses the appropriate drawables and states when
 * displayed on screen within a drop down. The focus is never actually
 * passed to the drop down in this mode; the list only looks focused.</p>
 */
class DropDownListView extends ListViewCompat {

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
     * <p>Creates a new list view wrapper.</p>
     *
     * @param context this view's context
     */
    public DropDownListView(Context context, boolean hijackFocus) {
        super(context, null, R.attr.dropDownListViewStyle);
        mHijackFocus = hijackFocus;
        setCacheColorHint(0); // Transparent, since the background drawable could be anything.
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

    @Override
    protected boolean touchModeDrawsInPressedStateCompat() {
        return mDrawsInPressedState || super.touchModeDrawsInPressedStateCompat();
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
}
