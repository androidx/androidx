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

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.support.annotation.RestrictTo;
import android.view.MotionEvent;

/**
 * Override methods in this class to connect specialized behaviors of the selection
 * code to the application environment.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public abstract class TouchCallbacks {

    static final TouchCallbacks DUMMY = new TouchCallbacks() {
        @Override
        public boolean onDragInitiated(MotionEvent e) {
            return false;
        }
    };

    /**
     * Called when a drag is initiated. Touch input handler only considers
     * a drag to be initiated on long press on an existing selection,
     * as normal touch and drag events are strongly associated with scrolling of the view.
     *
     * <p>Drag will only be initiated when the item under the event is already selected.
     *
     * <p>The RecyclerView item at the coordinates of the MotionEvent is not supplied as a parameter
     * to this method as there may be multiple items selected. Clients can obtain the current
     * list of selected items from {@link SelectionHelper#copySelection(Selection)}.
     *
     * @param e the event associated with the drag.
     * @return true if the event was handled.
     */
    public abstract boolean onDragInitiated(MotionEvent e);
}
