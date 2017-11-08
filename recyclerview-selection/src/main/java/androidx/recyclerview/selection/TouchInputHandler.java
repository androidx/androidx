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

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;

import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails;
import androidx.recyclerview.selection.SelectionHelper.SelectionPredicate;

/**
 * A MotionInputHandler that provides the high-level glue for touch driven selection. This class
 * works with {@link RecyclerView}, {@link GestureRouter}, and {@link GestureSelectionHelper} to
 * provide robust user drive selection support.
 */
final class TouchInputHandler<K> extends MotionInputHandler<K> {

    private static final String TAG = "TouchInputDelegate";
    private static final boolean DEBUG = false;

    private final ItemDetailsLookup<K> mDetailsLookup;
    private final SelectionPredicate<K> mSelectionPredicate;
    private final ActivationCallbacks<K> mActivationCallbacks;
    private final TouchCallbacks mTouchCallbacks;
    private final Runnable mGestureStarter;
    private final Runnable mHapticPerformer;

    TouchInputHandler(
            SelectionHelper<K> selectionHelper,
            ItemKeyProvider<K> keyProvider,
            ItemDetailsLookup<K> detailsLookup,
            SelectionPredicate<K> selectionPredicate,
            Runnable gestureStarter,
            TouchCallbacks touchCallbacks,
            ActivationCallbacks<K> activationCallbacks,
            FocusCallbacks<K> focusCallbacks,
            Runnable hapticPerformer) {

        super(selectionHelper, keyProvider, focusCallbacks);

        checkArgument(detailsLookup != null);
        checkArgument(selectionPredicate != null);
        checkArgument(gestureStarter != null);
        checkArgument(activationCallbacks != null);
        checkArgument(touchCallbacks != null);
        checkArgument(hapticPerformer != null);

        mDetailsLookup = detailsLookup;
        mSelectionPredicate = selectionPredicate;
        mGestureStarter = gestureStarter;
        mActivationCallbacks = activationCallbacks;
        mTouchCallbacks = touchCallbacks;
        mHapticPerformer = hapticPerformer;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if (!mDetailsLookup.overItemWithSelectionKey(e)) {
            if (DEBUG) Log.d(TAG, "Tap not associated w/ model item. Clearing selection.");
            mSelectionHelper.clearSelection();
            return false;
        }

        ItemDetails<K> item = mDetailsLookup.getItemDetails(e);
        // Should really not be null at this point, but...
        if (item == null) {
            return false;
        }

        if (mSelectionHelper.hasSelection()) {
            if (isRangeExtension(e)) {
                extendSelectionRange(item);
            } else if (mSelectionHelper.isSelected(item.getSelectionKey())) {
                mSelectionHelper.deselect(item.getSelectionKey());
            } else {
                selectItem(item);
            }

            return true;
        }

        // Touch events select if they occur in the selection hotspot,
        // otherwise they activate.
        return item.inSelectionHotspot(e)
                ? selectItem(item)
                : mActivationCallbacks.onItemActivated(item, e);
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (!mDetailsLookup.overItemWithSelectionKey(e)) {
            if (DEBUG) Log.d(TAG, "Ignoring LongPress on non-model-backed item.");
            return;
        }

        ItemDetails<K> item = mDetailsLookup.getItemDetails(e);
        // Should really not be null at this point, but...
        if (item == null) {
            return;
        }

        boolean handled = false;

        if (isRangeExtension(e)) {
            extendSelectionRange(item);
            handled = true;
        } else {
            if (!mSelectionHelper.isSelected(item.getSelectionKey())
                    && mSelectionPredicate.canSetStateForKey(item.getSelectionKey(), true)) {
                // If we cannot select it, we didn't apply anchoring - therefore should not
                // start gesture selection
                if (selectItem(item)) {
                    // And finally if the item was selected && we can select multiple
                    // we kick off gesture selection.
                    if (mSelectionPredicate.canSelectMultiple()) {
                        mGestureStarter.run();
                    }
                    handled = true;
                }
            } else {
                // We only initiate drag and drop on long press for touch to allow regular
                // touch-based scrolling
                mTouchCallbacks.onDragInitiated(e);
                handled = true;
            }
        }

        if (handled) {
            mHapticPerformer.run();
        }
    }
}
