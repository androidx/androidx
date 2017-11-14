/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.recyclerview.selection;

import static android.support.v4.util.Preconditions.checkArgument;
import static android.support.v4.util.Preconditions.checkState;

import static androidx.recyclerview.selection.Shared.DEBUG;
import static androidx.recyclerview.selection.Shared.VERBOSE;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;

import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails;

/**
 * A MotionInputHandler that provides the high-level glue for mouse driven selection. This
 * class works with {@link RecyclerView}, {@link GestureRouter}, and {@link GestureSelectionHelper}
 * to provide robust user driven selection support.
 */
final class MouseInputHandler<K> extends MotionInputHandler<K> {

    private static final String TAG = "MouseInputDelegate";

    private final ItemDetailsLookup<K> mDetailsLookup;
    private final MouseCallbacks mMouseCallbacks;
    private final ActivationCallbacks<K> mActivationCallbacks;
    private final FocusCallbacks<K> mFocusCallbacks;

    // The event has been handled in onSingleTapUp
    private boolean mHandledTapUp;
    // true when the previous event has consumed a right click motion event
    private boolean mHandledOnDown;

    MouseInputHandler(
            SelectionHelper<K> selectionHelper,
            ItemKeyProvider<K> keyProvider,
            ItemDetailsLookup<K> detailsLookup,
            MouseCallbacks mouseCallbacks,
            ActivationCallbacks<K> activationCallbacks,
            FocusCallbacks<K> focusCallbacks) {

        super(selectionHelper, keyProvider, focusCallbacks);

        checkArgument(detailsLookup != null);
        checkArgument(mouseCallbacks != null);
        checkArgument(activationCallbacks != null);

        mDetailsLookup = detailsLookup;
        mMouseCallbacks = mouseCallbacks;
        mActivationCallbacks = activationCallbacks;
        mFocusCallbacks = focusCallbacks;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        if (VERBOSE) Log.v(TAG, "Delegated onDown event.");
        if ((MotionEvents.isAltKeyPressed(e) && MotionEvents.isPrimaryButtonPressed(e))
                || MotionEvents.isSecondaryButtonPressed(e)) {
            mHandledOnDown = true;
            return onRightClick(e);
        }

        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        // Don't scroll content window in response to mouse drag
        // If it's two-finger trackpad scrolling, we want to scroll
        return !MotionEvents.isTouchpadScroll(e2);
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        // See b/27377794. Since we don't get a button state back from UP events, we have to
        // explicitly save this state to know whether something was previously handled by
        // DOWN events or not.
        if (mHandledOnDown) {
            if (VERBOSE) Log.v(TAG, "Ignoring onSingleTapUp, previously handled in onDown.");
            mHandledOnDown = false;
            return false;
        }

        if (!mDetailsLookup.overItemWithSelectionKey(e)) {
            if (DEBUG) Log.d(TAG, "Tap not associated w/ model item. Clearing selection.");
            mSelectionHelper.clearSelection();
            mFocusCallbacks.clearFocus();
            return false;
        }

        if (MotionEvents.isTertiaryButtonPressed(e)) {
            if (DEBUG) Log.d(TAG, "Ignoring middle click");
            return false;
        }

        if (mSelectionHelper.hasSelection()) {
            onItemClick(e, mDetailsLookup.getItemDetails(e));
            mHandledTapUp = true;
            return true;
        }

        return false;
    }

    // tap on an item when there is an existing selection. We could extend
    // a selection, we could clear selection (then launch)
    private void onItemClick(MotionEvent e, ItemDetails<K> item) {
        checkState(mSelectionHelper.hasSelection());
        checkArgument(item != null);

        if (isRangeExtension(e)) {
            extendSelectionRange(item);
        } else {
            if (shouldClearSelection(e, item)) {
                mSelectionHelper.clearSelection();
            }
            if (mSelectionHelper.isSelected(item.getSelectionKey())) {
                if (mSelectionHelper.deselect(item.getSelectionKey())) {
                    mFocusCallbacks.clearFocus();
                }
            } else {
                selectOrFocusItem(item, e);
            }
        }
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if (mHandledTapUp) {
            if (VERBOSE) {
                Log.v(TAG,
                        "Ignoring onSingleTapConfirmed, previously handled in onSingleTapUp.");
            }
            mHandledTapUp = false;
            return false;
        }

        if (mSelectionHelper.hasSelection()) {
            return false;  // should have been handled by onSingleTapUp.
        }

        if (!mDetailsLookup.overItem(e)) {
            if (DEBUG) Log.d(TAG, "Ignoring Confirmed Tap on non-item.");
            return false;
        }

        if (MotionEvents.isTertiaryButtonPressed(e)) {
            if (DEBUG) Log.d(TAG, "Ignoring middle click");
            return false;
        }

        @Nullable ItemDetails<K> item = mDetailsLookup.getItemDetails(e);
        if (item == null || !item.hasSelectionKey()) {
            return false;
        }

        if (mFocusCallbacks.hasFocusedItem() && MotionEvents.isShiftKeyPressed(e)) {
            mSelectionHelper.startRange(mFocusCallbacks.getFocusedPosition());
            mSelectionHelper.extendRange(item.getPosition());
        } else {
            selectOrFocusItem(item, e);
        }
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        mHandledTapUp = false;

        if (!mDetailsLookup.overItemWithSelectionKey(e)) {
            if (DEBUG) Log.d(TAG, "Ignoring DoubleTap on non-model-backed item.");
            return false;
        }

        if (MotionEvents.isTertiaryButtonPressed(e)) {
            if (DEBUG) Log.d(TAG, "Ignoring middle click");
            return false;
        }

        ItemDetails<K> item = mDetailsLookup.getItemDetails(e);
        return (item != null) && mActivationCallbacks.onItemActivated(item, e);
    }

    private boolean onRightClick(MotionEvent e) {
        if (mDetailsLookup.overItemWithSelectionKey(e)) {
            @Nullable ItemDetails<K> item = mDetailsLookup.getItemDetails(e);
            if (item != null && !mSelectionHelper.isSelected(item.getSelectionKey())) {
                mSelectionHelper.clearSelection();
                selectItem(item);
            }
        }

        // We always delegate final handling of the event,
        // since the handler might want to show a context menu
        // in an empty area or some other weirdo view.
        return mMouseCallbacks.onContextClick(e);
    }

    private void selectOrFocusItem(ItemDetails<K> item, MotionEvent e) {
        if (item.inSelectionHotspot(e) || MotionEvents.isCtrlKeyPressed(e)) {
            selectItem(item);
        } else {
            focusItem(item);
        }
    }
}
