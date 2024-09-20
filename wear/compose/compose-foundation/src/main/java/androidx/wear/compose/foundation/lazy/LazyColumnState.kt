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
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.MeasureResult
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

    private val layoutInfoState = mutableStateOf(EmptyLazyColumnMeasureResult, neverEqualPolicy())

    /**
     * The object of LazyColumnLayoutInfo calculated during the last layout pass. For example, you
     * can use it to calculate what items are currently visible. Note that this property is
     * observable and is updated after every scroll or remeasure. If you use it in the composable
     * function it will be recomposed on every change causing potential performance issues including
     * infinity recomposition loop. Therefore, avoid using it in the composition. If you want to run
     * some side effects like sending an analytics event or updating a state based on this value
     * consider using "snapshotFlow":
     */
    val layoutInfo: LazyColumnLayoutInfo
        get() = layoutInfoState.value

    internal var scrollToBeConsumed = 0f
        private set

    override var canScrollForward: Boolean by mutableStateOf(false)
        private set

    override var canScrollBackward: Boolean by mutableStateOf(false)
        private set

    internal var anchorItemIndex by mutableIntStateOf(0)
        private set

    internal var nearestRange: IntRange by
        mutableStateOf(IntRange.EMPTY, structuralEqualityPolicy())
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
        layoutInfoState.value = measureResult
        canScrollBackward = measureResult.canScrollBackward
        canScrollForward = measureResult.canScrollForward
        nearestRange = calculateNearestItemsRange(anchorItemIndex)
    }

    private companion object {
        /**
         * We use the idea of sliding window as an optimization, so user can scroll up to this
         * number of items until we have to regenerate the key to index map.
         */
        private const val NearestItemsSlidingWindowSize = 20

        /**
         * The minimum amount of items near the current first visible item we want to have mapping
         * for.
         */
        private const val NearestItemsExtraItemCount = 30

        private fun calculateNearestItemsRange(anchorItemIndex: Int): IntRange {
            val slidingWindowStart =
                NearestItemsSlidingWindowSize * (anchorItemIndex / NearestItemsSlidingWindowSize)

            val start = maxOf(slidingWindowStart - NearestItemsExtraItemCount, 0)
            val end =
                slidingWindowStart + NearestItemsSlidingWindowSize + NearestItemsExtraItemCount
            return start until end
        }
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

private val EmptyLazyColumnMeasureResult =
    LazyColumnMeasureResult(
        anchorItemIndex = 0,
        anchorItemScrollOffset = 0,
        visibleItems = emptyList(),
        totalItemsCount = 0,
        lastMeasuredItemHeight = Int.MIN_VALUE,
        canScrollForward = false,
        canScrollBackward = false,
        measureResult =
            object : MeasureResult {
                override val width: Int = 0
                override val height: Int = 0

                @Suppress("PrimitiveInCollection")
                override val alignmentLines: Map<AlignmentLine, Int> = emptyMap()

                override fun placeChildren() {}
            }
    )
