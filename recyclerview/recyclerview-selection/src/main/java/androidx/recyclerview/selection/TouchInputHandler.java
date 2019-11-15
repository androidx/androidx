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
import static androidx.recyclerview.selection.Shared.DEBUG;

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
        if (DEBUG) {
            checkArgument(MotionEvents.isFingerEvent(e));
            checkArgument(MotionEvents.isActionUp(e));
        }

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
            if (shouldExtendRange(e)) {
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
        if (DEBUG) {
            checkArgument(MotionEvents.isFingerEvent(e));
            checkArgument(MotionEvents.isActionDown(e));
        }

        if (!mDetailsLookup.overItemWithSelectionKey(e)) {
            if (DEBUG) Log.d(TAG, "Ignoring LongPress on non-model-backed item.");
            return;
        }

        ItemDetails<K> item = mDetailsLookup.getItemDetails(e);
        // Should really not be null at this point, but...
        if (item == null) {
            return;
        }

        if (shouldExtendRange(e)) {
            extendSelectionRange(item);
            mHapticPerformer.run();
        } else {
            if (mSelectionTracker.isSelected(item.getSelectionKey())) {
                // Long press on existing selected item initiates drag/drop.
                mOnDragInitiatedListener.onDragInitiated(e);
                mHapticPerformer.run();
            } else if (mSelectionPredicate.canSetStateForKey(item.getSelectionKey(), true)
                    && selectItem(item)) {
                // And finally if the item was selected && we can select multiple
                // we kick off gesture selection.
                // NOTE: isRangeActive should ALWAYS be true at this point, but there have
                // been reports indicating that assumption isn't correct. So we explicitly
                // check isRangeActive.
                if (mSelectionPredicate.canSelectMultiple() && mSelectionTracker.isRangeActive()) {
                    mGestureStarter.run();
                }
                mHapticPerformer.run();
            }
        }
    }
}
