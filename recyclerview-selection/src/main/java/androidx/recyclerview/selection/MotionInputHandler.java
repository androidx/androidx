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

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails;

/**
 * Base class for handlers that can be registered w/ {@link GestureRouter}.
 */
abstract class MotionInputHandler<K> extends SimpleOnGestureListener {

    protected final SelectionHelper<K> mSelectionHelper;

    private final ItemKeyProvider<K> mKeyProvider;
    private final FocusCallbacks<K> mFocusCallbacks;

    MotionInputHandler(
            SelectionHelper<K> selectionHelper,
            ItemKeyProvider<K> keyProvider,
            FocusCallbacks<K> focusCallbacks) {

        checkArgument(selectionHelper != null);
        checkArgument(keyProvider != null);
        checkArgument(focusCallbacks != null);

        mSelectionHelper = selectionHelper;
        mKeyProvider = keyProvider;
        mFocusCallbacks = focusCallbacks;
    }

    final boolean selectItem(ItemDetails<K> details) {
        checkArgument(details != null);
        checkArgument(hasPosition(details));
        checkArgument(hasSelectionKey(details));

        if (mSelectionHelper.select(details.getSelectionKey())) {
            mSelectionHelper.anchorRange(details.getPosition());
        }

        // we set the focus on this doc so it will be the origin for keyboard events or shift+clicks
        // if there is only a single item selected, otherwise clear focus
        if (mSelectionHelper.getSelection().size() == 1) {
            mFocusCallbacks.focusItem(details);
        } else {
            mFocusCallbacks.clearFocus();
        }
        return true;
    }

    protected final boolean focusItem(ItemDetails<K> details) {
        checkArgument(details != null);
        checkArgument(hasSelectionKey(details));

        mSelectionHelper.clearSelection();
        mFocusCallbacks.focusItem(details);
        return true;
    }

    protected final void extendSelectionRange(ItemDetails<K> details) {
        checkState(mKeyProvider.hasAccess(ItemKeyProvider.SCOPE_MAPPED));
        checkArgument(hasPosition(details));
        checkArgument(hasSelectionKey(details));

        mSelectionHelper.extendRange(details.getPosition());
        mFocusCallbacks.focusItem(details);
    }

    final boolean isRangeExtension(MotionEvent e) {
        return MotionEvents.isShiftKeyPressed(e)
                && mSelectionHelper.isRangeActive()
                // Without full corpus access we can't reliably implement range
                // as a user can scroll *anywhere* then SHIFT+click.
                && mKeyProvider.hasAccess(ItemKeyProvider.SCOPE_MAPPED);
    }

    boolean shouldClearSelection(MotionEvent e, ItemDetails<K> item) {
        return !MotionEvents.isCtrlKeyPressed(e)
                && !item.inSelectionHotspot(e)
                && !mSelectionHelper.isSelected(item.getSelectionKey());
    }

    static boolean hasSelectionKey(@Nullable ItemDetails<?> item) {
        return item != null && item.getSelectionKey() != null;
    }

    static boolean hasPosition(@Nullable ItemDetails<?> item) {
        return item != null && item.getPosition() != RecyclerView.NO_POSITION;
    }
}
