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

package androidx.navigation3.ui

import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventState
import androidx.navigationevent.NavigationEventState.InProgress

/**
 * Represents a snapshot of the visible destinations in a navigation container.
 *
 * This class provides the necessary context for building animations during navigation gestures,
 * like predictive back. It's a simple data holder that feeds into the [NavigationEventState].
 *
 * During a gesture, the [InProgress] state provides two instances of this info:
 * - [InProgress.currentInfo]: The state you're navigating *to*.
 * - [InProgress.previousInfo]: The state you're navigating *from*.
 *
 * You can use these two snapshots, along with [NavigationEventState.progress], to create a smooth
 * visual transition between destinations.
 *
 * @property visibleEntries A list of unique keys for the visible navigation destinations, typically
 *   ordered from the bottom to the top of the back stack.
 */
public class NavDisplayInfo internal constructor(public val visibleEntries: List<Any>) :
    NavigationEventInfo {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as NavDisplayInfo

        return visibleEntries == other.visibleEntries
    }

    override fun hashCode(): Int {
        return visibleEntries.hashCode()
    }

    override fun toString(): String {
        return "NavDisplayInfo(visibleEntries=$visibleEntries)"
    }
}
