/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.gestures.drag

import androidx.ui.gestures.drag_details.DragEndDetails
import androidx.ui.gestures.drag_details.DragUpdateDetails

/**
 * Interface for objects that receive updates about drags.
 *
 * This interface is used in various ways. For example,
 * [MultiDragGestureRecognizer] uses it to update its clients when it
 * recognizes a gesture. Similarly, the scrolling infrastructure in the widgets
 * library uses it to notify the [DragScrollActivity] when the user drags the
 * scrollable.
 */
abstract class Drag {
    /** The pointer has moved. */
    fun update(details: DragUpdateDetails) {}

    /**
     * The pointer is no longer in contact with the screen.
     *
     * The velocity at which the pointer was moving when it stopped contacting
     * the screen is available in the `details`.
     */
    fun end(details: DragEndDetails) {}

    /**
     * The input from the pointer is no longer directed towards this receiver.
     *
     * For example, the user might have been interrupted by a system-modal dialog
     * in the middle of the drag.
     */
    fun cancel() {}
}