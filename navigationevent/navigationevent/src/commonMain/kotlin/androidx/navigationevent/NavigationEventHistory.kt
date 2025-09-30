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

import androidx.compose.runtime.Immutable
import kotlin.jvm.JvmOverloads

/**
 * Represents an immutable snapshot of the navigation history stack.
 *
 * This object tracks the combined navigation stack ([mergedHistory]) from the root to the currently
 * active destination, indicated by [currentIndex].
 *
 * This state is explicitly separate from transition state (which tracks gesture progress).
 * [NavigationEventHistory] is stable during gestures and only updates when the active handler
 * changes or when the active handler's info (its local stack) is updated.
 *
 * @param mergedHistory The combined navigation stack from the root (index 0) to the leaf (last
 *   index).
 * @param currentIndex The index within [mergedHistory] that represents the currently active
 *   destination.
 */
@Immutable
public class NavigationEventHistory
private constructor(
    /** Combined stack from root to leaf. */
    public val mergedHistory: List<NavigationEventInfo>,
    /** Index of current in mergedHistory. */
    public val currentIndex: Int,
) {

    init {
        require(
            mergedHistory.isEmpty() && currentIndex == -1 ||
                mergedHistory.isNotEmpty() && currentIndex in mergedHistory.indices
        ) {
            "Invalid 'NavigationEventHistory' state: " +
                " 'currentIndex' must be within the bounds of 'mergedHistory' (or -1 if empty)." +
                " Received: currentIndex = '$currentIndex', bounds = '${mergedHistory.indices}'."
        }
    }

    /**
     * A convenience constructor that creates an empty [NavigationEventHistory] instance,
     * representing a state with no navigation history.
     */
    internal constructor() : this(mergedHistory = emptyList(), currentIndex = -1)

    /**
     * A convenience constructor that creates a [NavigationEventHistory] instance from the history
     * partitions ([currentInfo], [backInfo], and [forwardInfo]).
     *
     * This constructor automatically calculates the canonical [mergedHistory] by concatenating
     * [backInfo] + [currentInfo] + [forwardInfo], and sets the [currentIndex] to point to
     * [currentInfo] (which is the index equal to `backInfo.size`).
     */
    @JvmOverloads
    internal constructor(
        currentInfo: NavigationEventInfo,
        backInfo: List<NavigationEventInfo> = emptyList(),
        forwardInfo: List<NavigationEventInfo> = emptyList(),
    ) : this(
        mergedHistory =
            buildList {
                this += backInfo
                this += currentInfo
                this += forwardInfo
            },
        currentIndex = backInfo.size,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as NavigationEventHistory

        if (currentIndex != other.currentIndex) return false
        if (mergedHistory != other.mergedHistory) return false

        return true
    }

    override fun hashCode(): Int {
        var result = currentIndex
        result = 31 * result + mergedHistory.hashCode()
        return result
    }

    override fun toString(): String {
        return "NavigationEventHistory(currentIndex=$currentIndex, mergedHistory=$mergedHistory)"
    }
}
