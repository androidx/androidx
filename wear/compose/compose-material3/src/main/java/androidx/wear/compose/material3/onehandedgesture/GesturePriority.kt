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

package androidx.wear.compose.material3.onehandedgesture

import androidx.compose.runtime.Immutable

/**
 * Defines the fixed precedence levels for one-handed gesture interception.
 *
 * This priority system is used by the [GestureManager] to resolve conflicts when multiple
 * components in a UI hierarchy are eligible to handle a gesture. Because the system cannot
 * automatically determine intent in overlapping areas, developers should assign one of these
 * predefined priority levels to their components to establish a functional gesture hierarchy.
 *
 * Higher priority values take precedence over lower ones.
 *
 * This class wraps an internal integer value used to resolve gesture conflicts.
 */
@Immutable
@JvmInline
public value class GesturePriority private constructor(internal val value: Int) {
    public companion object {
        /**
         * Default priority level.
         *
         * Used when no specific priority is defined. This level typically yields to any other
         * specified gesture priority.
         */
        public val Unspecified: GesturePriority = GesturePriority(0)

        /**
         * Priority for general scrollable areas, such as lists, pagers or containers.
         *
         * This level but yields to [Clickable] elements to ensure interactive controls remain
         * accessible within a scrollable parent.
         */
        public val Scrollable: GesturePriority = GesturePriority(100)

        /**
         * Priority for interactive components like buttons, switches, or chips.
         *
         * This is the highest priority tier. It must be used on individual controls to ensure they
         * capture gestures before their containing scrollable or pageable parents.
         */
        public val Clickable: GesturePriority = GesturePriority(200)
    }
}
