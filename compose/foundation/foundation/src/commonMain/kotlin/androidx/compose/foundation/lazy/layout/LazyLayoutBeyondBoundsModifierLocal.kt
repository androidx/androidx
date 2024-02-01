/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.lazy.layout

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.layout.LazyLayoutBeyondBoundsInfo.Interval
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.BeyondBoundsLayout
import androidx.compose.ui.layout.BeyondBoundsLayout.BeyondBoundsScope
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Above
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.After
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Before
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Below
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Left
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Right
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.ProvidableModifierLocal
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.LayoutDirection.Rtl

/**
 * This modifier is used to measure and place additional items when the lazy layout receives a
 * request to layout items beyond the visible bounds.
 */
@Suppress("ComposableModifierFactory")
@Composable
internal fun Modifier.lazyLayoutBeyondBoundsModifier(
    state: LazyLayoutBeyondBoundsState,
    beyondBoundsInfo: LazyLayoutBeyondBoundsInfo,
    reverseLayout: Boolean,
    layoutDirection: LayoutDirection,
    orientation: Orientation,
    enabled: Boolean
): Modifier = if (!enabled) {
    this
} else {
    this then remember(state, beyondBoundsInfo, reverseLayout, layoutDirection, orientation) {
        LazyLayoutBeyondBoundsModifierLocal(
            state,
            beyondBoundsInfo,
            reverseLayout,
            layoutDirection,
            orientation
        )
    }
}

internal class LazyLayoutBeyondBoundsModifierLocal(
    private val state: LazyLayoutBeyondBoundsState,
    private val beyondBoundsInfo: LazyLayoutBeyondBoundsInfo,
    private val reverseLayout: Boolean,
    private val layoutDirection: LayoutDirection,
    private val orientation: Orientation
) : ModifierLocalProvider<BeyondBoundsLayout?>, BeyondBoundsLayout {
    override val key: ProvidableModifierLocal<BeyondBoundsLayout?>
        get() = ModifierLocalBeyondBoundsLayout
    override val value: BeyondBoundsLayout
        get() = this

    companion object {
        private val emptyBeyondBoundsScope = object : BeyondBoundsScope {
            override val hasMoreContent = false
        }
    }

    override fun <T> layout(
        direction: BeyondBoundsLayout.LayoutDirection,
        block: BeyondBoundsScope.() -> T?
    ): T? {
        // If the lazy list is empty, or if it does not have any visible items (Which implies
        // that there isn't space to add a single item), we don't attempt to layout any more items.
        if (state.itemCount <= 0 || !state.hasVisibleItems) {
            return block.invoke(emptyBeyondBoundsScope)
        }

        // We use a new interval each time because this function is re-entrant.
        val startIndex = if (direction.isForward()) {
            state.lastPlacedIndex
        } else {
            state.firstPlacedIndex
        }
        var interval = beyondBoundsInfo.addInterval(startIndex, startIndex)
        var found: T? = null
        while (found == null && interval.hasMoreContent(direction)) {

            // Add one extra beyond bounds item.
            interval = addNextInterval(interval, direction).also {
                beyondBoundsInfo.removeInterval(interval)
            }
            state.remeasure()

            // When we invoke this block, the beyond bounds items are present.
            found = block.invoke(
                object : BeyondBoundsScope {
                    override val hasMoreContent: Boolean
                        get() = interval.hasMoreContent(direction)
                }
            )
        }

        // Dispose the items that are beyond the visible bounds.
        beyondBoundsInfo.removeInterval(interval)
        state.remeasure()
        return found
    }

    private fun BeyondBoundsLayout.LayoutDirection.isForward(): Boolean =
        when (this) {
            Before -> false
            After -> true
            Above -> reverseLayout
            Below -> !reverseLayout
            Left -> when (layoutDirection) {
                Ltr -> reverseLayout
                Rtl -> !reverseLayout
            }

            Right -> when (layoutDirection) {
                Ltr -> !reverseLayout
                Rtl -> reverseLayout
            }

            else -> unsupportedDirection()
        }

    private fun addNextInterval(
        currentInterval: Interval,
        direction: BeyondBoundsLayout.LayoutDirection
    ): Interval {
        var start = currentInterval.start
        var end = currentInterval.end
        if (direction.isForward()) {
            end++
        } else {
            start--
        }
        return beyondBoundsInfo.addInterval(start, end)
    }

    private fun Interval.hasMoreContent(direction: BeyondBoundsLayout.LayoutDirection): Boolean {
        if (direction.isOppositeToOrientation()) return false
        return if (direction.isForward()) end < state.itemCount - 1 else start > 0
    }

    private fun BeyondBoundsLayout.LayoutDirection.isOppositeToOrientation(): Boolean {
        return when (this) {
            Above, Below -> orientation == Orientation.Horizontal
            Left, Right -> orientation == Orientation.Vertical
            Before, After -> false
            else -> unsupportedDirection()
        }
    }
}

private fun unsupportedDirection(): Nothing = error(
    "Lazy list does not support beyond bounds layout for the specified direction"
)
