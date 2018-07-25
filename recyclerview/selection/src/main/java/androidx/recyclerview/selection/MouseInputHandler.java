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

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkState;
import static androidx.recyclerview.selection.Shared.DEBUG;
import static androidx.recyclerview.selection.Shared.VERBOSE;

import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A MotionInputHandler that provides the high-level glue for mouse driven selection. This
 * class works with {@link RecyclerView}, {@link GestureRouter}, and {@link GestureSelectionHelper}
 * to implement the primary policies around mouse input.
 */
final class MouseInputHandler<K> extends MotionInputHandler<K> {

    private static final String TAG = "MouseInputDelegate";

    private final ItemDetailsLookup<K> mDetailsLookup;
    private final OnContextClickListener mOnContextClickListener;
    private final OnItemActivatedListener<K> mOnItemActivatedListener;
    private final FocusDelegate<K> mFocusDelegate;

    // The event has been handled in onSingleTapUp
    private boolean mHandledTapUp;
    // true when the previous event has consumed a right click motion event
    private boolean mHandledOnDown;

    MouseInputHandler(
            @NonNull SelectionTracker<K> selectionTracker,
            @NonNull ItemKeyProvider<K> keyProvider,
            @NonNull ItemDetailsLookup<K> detailsLookup,
            @NonNull OnContextClickListener onContextClickListener,
            @NonNull OnItemActivatedListener<K> onItemActivatedListener,
            @NonNull FocusDelegate<K> focusDelegate) {

        super(selectionTracker, keyProvider, focusDelegate);

        checkArgument(detailsLookup != null);
        checkArgument(onContextClickListener != null);
        checkArgument(onItemActivatedListener != null);

        mDetailsLookup = detailsLookup;
        mOnContextClickListener = onContextClickListener;
        mOnItemActivatedListener = onItemActivatedListener;
        mFocusDelegate = focusDelegate;
    }

    @Override
    public boolean onDown(@NonNull MotionEvent e) {
        if (VERBOSE) Log.v(TAG, "Delegated onDown event.");
        if ((MotionEvents.isAltKeyPressed(e) && MotionEvents.isPrimaryMouseButtonPressed(e))
                || MotionEvents.isSecondaryMouseButtonPressed(e)) {
            mHandledOnDown = true;
            return onRightClick(e);
        }

        return false;
    }

    @Override
    public boolean onScroll(@NonNull MotionEvent e1, @NonNull MotionEvent e2,
            float distanceX, float distanceY) {
        // Don't scroll content window in response to mouse drag
        // If it's two-finger trackpad scrolling, we want to scroll
        return !MotionEvents.isTouchpadScroll(e2);
    }

    @Override
    public boolean onSingleTapUp(@NonNull MotionEvent e) {
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
            mSelectionTracker.clearSelection();
            mFocusDelegate.clearFocus();
            return false;
        }

        if (MotionEvents.isTertiaryMouseButtonPressed(e)) {
            if (DEBUG) Log.d(TAG, "Ignoring middle click");
            return false;
        }

        if (mSelectionTracker.hasSelection()) {
            onItemClick(e, mDetailsLookup.getItemDetails(e));
            mHandledTapUp = true;
            return true;
        }

        return false;
    }

    // tap on an item when there is an existing selection. We could extend
    // a selection, we could clear selection (then launch)
    private void onItemClick(@NonNull MotionEvent e, @NonNull ItemDetails<K> item) {
        checkState(mSelectionTracker.hasSelection());
        checkArgument(item != null);

        if (isRangeExtension(e)) {
            extendSelectionRange(item);
        } else {
            if (shouldClearSelection(e, item)) {
                mSelectionTracker.clearSelection();
            }
            if (mSelectionTracker.isSelected(item.getSelectionKey())) {
                if (mSelectionTracker.deselect(item.getSelectionKey())) {
                    mFocusDelegate.clearFocus();
                }
            } else {
                selectOrFocusItem(item, e);
            }
        }
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
        if (mHandledTapUp) {
            if (VERBOSE) {
                Log.v(TAG,
                        "Ignoring onSingleTapConfirmed, previously handled in onSingleTapUp.");
            }
            mHandledTapUp = false;
            return false;
        }

        if (mSelectionTracker.hasSelection()) {
            return false;  // should have been handled by onSingleTapUp.
        }

        if (!mDetailsLookup.overItem(e)) {
            if (DEBUG) Log.d(TAG, "Ignoring Confirmed Tap on non-item.");
            return false;
        }

        if (MotionEvents.isTertiaryMouseButtonPressed(e)) {
            if (DEBUG) Log.d(TAG, "Ignoring middle click");
            return false;
        }

        @Nullable ItemDetails<K> item = mDetailsLookup.getItemDetails(e);
        if (item == null || !item.hasSelectionKey()) {
            return false;
        }

        if (mFocusDelegate.hasFocusedItem() && MotionEvents.isShiftKeyPressed(e)) {
            mSelectionTracker.startRange(mFocusDelegate.getFocusedPosition());
            mSelectionTracker.extendRange(item.getPosition());
        } else {
            selectOrFocusItem(item, e);
        }
        return true;
    }

    @Override
    public boolean onDoubleTap(@NonNull MotionEvent e) {
        mHandledTapUp = false;

        if (!mDetailsLookup.overItemWithSelectionKey(e)) {
            if (DEBUG) Log.d(TAG, "Ignoring DoubleTap on non-model-backed item.");
            return false;
        }

        if (MotionEvents.isTertiaryMouseButtonPressed(e)) {
            if (DEBUG) Log.d(TAG, "Ignoring middle click");
            return false;
        }

        ItemDetails<K> item = mDetailsLookup.getItemDetails(e);
        return (item != null) && mOnItemActivatedListener.onItemActivated(item, e);
    }

    private boolean onRightClick(@NonNull MotionEvent e) {
        if (mDetailsLookup.overItemWithSelectionKey(e)) {
            @Nullable ItemDetails<K> item = mDetailsLookup.getItemDetails(e);
            if (item != null && !mSelectionTracker.isSelected(item.getSelectionKey())) {
                mSelectionTracker.clearSelection();
                selectItem(item);
            }
        }

        // We always delegate final handling of the event,
        // since the handler might want to show a context menu
        // in an empty area or some other weirdo view.
        return mOnContextClickListener.onContextClick(e);
    }

    private void selectOrFocusItem(@NonNull ItemDetails<K> item, @NonNull MotionEvent e) {
        if (item.inSelectionHotspot(e) || MotionEvents.isCtrlKeyPressed(e)) {
            selectItem(item);
        } else {
            focusItem(item);
        }
    }
}
