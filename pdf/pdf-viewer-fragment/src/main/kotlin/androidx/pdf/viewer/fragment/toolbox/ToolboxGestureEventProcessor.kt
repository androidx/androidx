/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.viewer.fragment.toolbox

import androidx.annotation.RestrictTo
import androidx.pdf.viewer.fragment.toolbox.ToolboxGestureEventProcessor.MotionEventType.ScrollTo
import androidx.pdf.viewer.fragment.toolbox.ToolboxGestureEventProcessor.MotionEventType.SingleTap

/**
 * Processes toolbox gesture events and delegates them to the appropriate handler.
 *
 * This class acts as a mediator between incoming motion events and the [ToolboxGestureDelegate],
 * allowing for centralized handling of gestures.
 *
 * @param toolboxGestureDelegate The handler responsible for responding to toolbox gestures.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ToolboxGestureEventProcessor(
    public val toolboxGestureDelegate: ToolboxGestureDelegate
) {

    /**
     * Processes a given motion event.
     *
     * This function determines the type of motion event and calls the corresponding method on the
     * [toolboxGestureDelegate].
     *
     * @param event The motion event to process.
     */
    public fun processEvent(event: MotionEventType) {
        when (event) {
            is SingleTap -> toolboxGestureDelegate.onSingleTap()
            is ScrollTo -> toolboxGestureDelegate.onScroll(event.position)
        }
    }

    /** Interface for handling toolbox gesture events. */
    public interface ToolboxGestureDelegate {
        /** Called when a single tap gesture is detected. */
        public fun onSingleTap()

        /**
         * Called when a scroll gesture is detected.
         *
         * @param position The current position of the scroll.
         */
        public fun onScroll(position: Int)
    }

    /** Sealed interface representing different types of motion events. */
    public sealed interface MotionEventType {
        /** Represents a single tap event. */
        public object SingleTap : MotionEventType

        /**
         * Represents a scroll event.
         *
         * @param position The current position of the scroll.
         */
        public class ScrollTo(public val position: Int) : MotionEventType
    }
}
