/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.pdf.ink.view.draganddrop

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/**
 * Interface defining a contract for UI components that can be docked to specific edges of a
 * container, such as the start, end, or bottom.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface ToolbarDockState {

    /**
     * An annotation that defines the set of integer constants representing the valid docking states
     * for a component.
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(DOCK_STATE_START, DOCK_STATE_BOTTOM, DOCK_STATE_END)
    public annotation class DockState

    /**
     * The current docking state of the component.
     *
     * Implementations should update their layout and orientation according to the value of this
     * property.
     */
    @DockState public var dockState: Int

    public companion object {
        /**
         * Represents a state where the component is docked to the start (left, in LTR mode) edge.
         */
        public const val DOCK_STATE_START: Int = 0

        /** Represents a state where the component is docked to the bottom edge. */
        public const val DOCK_STATE_BOTTOM: Int = 1

        /**
         * Represents a state where the component is docked to the end (right, in LTR mode) edge.
         */
        public const val DOCK_STATE_END: Int = 2
    }
}
