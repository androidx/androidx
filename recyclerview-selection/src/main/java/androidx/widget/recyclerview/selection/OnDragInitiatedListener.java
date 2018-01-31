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

package androidx.widget.recyclerview.selection;

import android.support.annotation.NonNull;
import android.view.MotionEvent;

/**
 * Register an OnDragInitiatedListener to be notified of potential drag operations,
 * and to handle them.
 */
public interface OnDragInitiatedListener {

    /**
     * Called when a drag is initiated. Touch input handler only considers
     * a drag to be initiated on long press on an existing selection,
     * as normal touch and drag events are strongly associated with scrolling of the view.
     *
     * <p>
     * Drag will only be initiated when the item under the event is already selected.
     *
     * <p>
     * The RecyclerView item at the coordinates of the MotionEvent is not supplied as a parameter
     * to this method as there may be multiple items selected. Clients can obtain the current
     * list of selected items from {@link SelectionTracker#copySelection(Selection)}.
     *
     * @param e the event associated with the drag.
     * @return true if the event was handled.
     */
    boolean onDragInitiated(@NonNull MotionEvent e);
}
