/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.foundation.lazy

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import kotlin.math.abs

/** Creates a [LazyColumnState] that is remembered across compositions. */
@Composable fun rememberLazyColumnState() = remember { LazyColumnState() }

/**
 * A state object that can be hoisted to control and observe scrolling.
 *
 * In most cases, this will be created via [rememberLazyColumnState].
 */
class LazyColumnState : ScrollableState {
    override val isScrollInProgress: Boolean
        get() = scrollableState.isScrollInProgress

    override fun dispatchRawDelta(delta: Float): Float = scrollableState.dispatchRawDelta(delta)

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ) = scrollableState.scroll(scrollPriority, block)

    var layoutInfo: LazyColumnLayoutInfo by mutableStateOf(LazyColumnLayoutInfoImpl(emptyList(), 0))
        private set

    private data class LazyColumnLayoutInfoImpl(
        override val visibleItems: List<LazyColumnVisibleItemInfo>,
        override val totalItemsCount: Int,
    ) : LazyColumnLayoutInfo

    internal var scrollToBeConsumed = 0f
        private set

    internal var anchorItemIndex by mutableIntStateOf(0)
        private set

    internal var anchorItemScrollOffset by mutableIntStateOf(0)
        private set

    internal var lastMeasuredAnchorItemHeight: Int = Int.MIN_VALUE
        private set

    internal var remeasurement: Remeasurement? = null
        private set

    /** The modifier which provides [remeasurement]. */
    internal val remeasurementModifier =
        object : RemeasurementModifier {
            override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
                this@LazyColumnState.remeasurement = remeasurement
            }
        }

    internal fun applyMeasureResult(measureResult: LazyColumnMeasureResult) {
        // TODO(artemiy): Don't consume all scroll.
        scrollToBeConsumed = 0f
        anchorItemIndex = measureResult.anchorItemIndex
        anchorItemScrollOffset = measureResult.anchorItemScrollOffset
        lastMeasuredAnchorItemHeight = measureResult.lastMeasuredItemHeight
        layoutInfo =
            LazyColumnLayoutInfoImpl(
                visibleItems = measureResult.visibleItems,
                totalItemsCount = measureResult.totalItemsCount
            )
    }

    private val scrollableState = ScrollableState { -onScroll(-it) }

    private fun onScroll(distance: Float): Float {
        if (distance < 0 && !canScrollForward || distance > 0 && !canScrollBackward) {
            return 0f
        }
        scrollToBeConsumed += distance
        if (abs(scrollToBeConsumed) > 0.5f) {
            remeasurement?.forceRemeasure()
        }

        // here scrollToBeConsumed is already consumed during the forceRemeasure invocation
        if (abs(scrollToBeConsumed) <= 0.5f) {
            // We consumed all of it - we'll hold onto the fractional scroll for later, so report
            // that we consumed the whole thing
            return distance
        } else {
            val scrollConsumed = distance - scrollToBeConsumed

            // We did not consume all of it - return the rest to be consumed elsewhere (e.g.,
            // nested scrolling)
            scrollToBeConsumed = 0f // We're not consuming the rest, give it back
            return scrollConsumed
        }
    }
}
