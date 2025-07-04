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

import static androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails;

import android.content.ClipData;
import android.view.MotionEvent;
import android.view.View;

import org.jspecify.annotations.NonNull;

/**
 * Register an OnDragInitiatedListener to be notified when user intent to perform drag and drop
 * operations on an item or items has been detected. Handle these events using {@link View}
 * support for Drag and drop.
 *
 * <p>
 * See {@link View#startDragAndDrop(ClipData, View.DragShadowBuilder, Object, int)}
 * for details.
 */
public interface OnDragInitiatedListener {

    /**
     * Called when user intent to perform a drag and drop operation has been detected.
     *
     * <p>
     * The following circumstances are considered to be expressing drag and drop intent:
     *
     * <ol>
     *     <li>Long press on selected item.</li>
     *     <li>Click and drag in the {@link ItemDetails#inDragRegion(MotionEvent) drag region}
     *     of selected item with a pointer device.</li>
     *     <li>Click and drag in drag region of un-selected item with a pointer device.</li>
     * </ol>
     *
     * <p>
     * The RecyclerView item at the coordinates of the MotionEvent is not supplied as a parameter
     * to this method as there may be multiple items selected or no items selected (as may be
     * the case in pointer drive drag and drop.)
     *
     * <p>
     * Obtain the current list of selected items from
     * {@link SelectionTracker#copySelection(MutableSelection)}. If there is no selection
     * get the item under the event using {@link ItemDetailsLookup#getItemDetails(MotionEvent)}.
     *
     * <p>
     * Drag region used with pointer devices is specified by
     * {@link ItemDetails#inDragRegion(MotionEvent)}
     *
     * <p>
     * See {@link android.view.View#startDragAndDrop(ClipData, View.DragShadowBuilder, Object, int)}
     * for details on drag and drop implementation.
     *
     * @param e the event associated with the drag.
     * @return true if drag and drop was initiated.
     */
    boolean onDragInitiated(@NonNull MotionEvent e);
}
