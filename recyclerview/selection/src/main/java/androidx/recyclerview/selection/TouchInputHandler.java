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

import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails;
import androidx.recyclerview.selection.SelectionTracker.SelectionPredicate;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A MotionInputHandler that provides the high-level glue for touch driven selection. This class
 * works with {@link RecyclerView}, {@link GestureRouter}, and {@link GestureSelectionHelper} to
 * to implement the primary policies around touch input.
 *
 * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
 */
final class TouchInputHandler<K> extends MotionInputHandler<K> {

    private static final String TAG = "TouchInputDelegate";
    private static final boolean DEBUG = false;

    private final ItemDetailsLookup<K> mDetailsLookup;
    private final SelectionPredicate<K> mSelectionPredicate;
    private final OnItemActivatedListener<K> mOnItemActivatedListener;
    private final OnDragInitiatedListener mOnDragInitiatedListener;
    private final Runnable mGestureStarter;
    private final Runnable mHapticPerformer;

    TouchInputHandler(
            @NonNull SelectionTracker<K> selectionTracker,
            @NonNull ItemKeyProvider<K> keyProvider,
            @NonNull ItemDetailsLookup<K> detailsLookup,
            @NonNull SelectionPredicate<K> selectionPredicate,
            @NonNull Runnable gestureStarter,
            @NonNull OnDragInitiatedListener onDragInitiatedListener,
            @NonNull OnItemActivatedListener<K> onItemActivatedListener,
            @NonNull FocusDelegate<K> focusDelegate,
            @NonNull Runnable hapticPerformer) {

        super(selectionTracker, keyProvider, focusDelegate);

        checkArgument(detailsLookup != null);
        checkArgument(selectionPredicate != null);
        checkArgument(gestureStarter != null);
        checkArgument(onItemActivatedListener != null);
        checkArgument(onDragInitiatedListener != null);
        checkArgument(hapticPerformer != null);

        mDetailsLookup = detailsLookup;
        mSelectionPredicate = selectionPredicate;
        mGestureStarter = gestureStarter;
        mOnItemActivatedListener = onItemActivatedListener;
        mOnDragInitiatedListener = onDragInitiatedListener;
        mHapticPerformer = hapticPerformer;
    }

    @Override
    public boolean onSingleTapUp(@NonNull MotionEvent e) {
        if (!mDetailsLookup.overItemWithSelectionKey(e)) {
            if (DEBUG) Log.d(TAG, "Tap not associated w/ model item. Clearing selection.");
            mSelectionTracker.clearSelection();
            return false;
        }

        ItemDetails<K> item = mDetailsLookup.getItemDetails(e);
        // Should really not be null at this point, but...
        if (item == null) {
            return false;
        }

        if (mSelectionTracker.hasSelection()) {
            if (isRangeExtension(e)) {
                extendSelectionRange(item);
            } else if (mSelectionTracker.isSelected(item.getSelectionKey())) {
                mSelectionTracker.deselect(item.getSelectionKey());
            } else {
                selectItem(item);
            }

            return true;
        }

        // Touch events select if they occur in the selection hotspot,
        // otherwise they activate.
        return item.inSelectionHotspot(e)
                ? selectItem(item)
                : mOnItemActivatedListener.onItemActivated(item, e);
    }

    @Override
    public void onLongPress(@NonNull MotionEvent e) {
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
            if (!mSelectionTracker.isSelected(item.getSelectionKey())
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
                mOnDragInitiatedListener.onDragInitiated(e);
                handled = true;
            }
        }

        if (handled) {
            mHapticPerformer.run();
        }
    }
}
