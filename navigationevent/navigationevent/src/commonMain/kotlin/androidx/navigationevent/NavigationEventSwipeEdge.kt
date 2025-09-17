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

package androidx.navigationevent

import kotlin.jvm.JvmInline
import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic

/** Defines the possible screen edges from which a swipe gesture can originate. */
@Suppress("ValueClassDefinition")
@JvmInline
public value class NavigationEventSwipeEdge private constructor(internal val value: Int) {

    public companion object {

        /** Indicates the navigation gesture originates from the left edge of the screen. */
        @get:JvmName("getLeft") // Disable name mangling for Java
        @JvmStatic
        public val Left: NavigationEventSwipeEdge = NavigationEventSwipeEdge(0)

        /** Indicates the navigation gesture originates from the right edge of the screen. */
        @get:JvmName("getRight") // Disable name mangling for Java
        @JvmStatic
        public val Right: NavigationEventSwipeEdge = NavigationEventSwipeEdge(1)

        /**
         * Indicates the navigation event was not caused by an edge swipe. This applies to actions
         * like a 3-button navigation press or a hardware back button event.
         */
        @get:JvmName("getNone") // Disable name mangling for Java
        @JvmStatic
        public val None: NavigationEventSwipeEdge = NavigationEventSwipeEdge(2)
    }
}
