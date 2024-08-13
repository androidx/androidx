/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.gestures.snapping

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasuredItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastForEach
import kotlin.math.absoluteValue
import kotlin.math.sign

/**
 * A [SnapLayoutInfoProvider] for LazyLists.
 *
 * @param lazyListState The [LazyListState] with information about the current state of the list
 * @param snapPosition The desired positioning of the snapped item within the main layout. This
 *   position should be considered with regard to the start edge of the item and the placement
 *   within the viewport.
 * @return A [SnapLayoutInfoProvider] that can be used with [snapFlingBehavior]
 */
fun SnapLayoutInfoProvider(
    lazyListState: LazyListState,
    snapPosition: SnapPosition = SnapPosition.Center
): SnapLayoutInfoProvider =
    object : SnapLayoutInfoProvider {

        private val layoutInfo: LazyListLayoutInfo
            get() = lazyListState.layoutInfo

        private val averageItemSize: Int
            get() {
                val layoutInfo = layoutInfo
                return if (layoutInfo.visibleItemsInfo.isEmpty()) {
                    0
                } else {
                    val numberOfItems = layoutInfo.visibleItemsInfo.size
                    layoutInfo.visibleItemsInfo.sumOf { it.size } / numberOfItems
                }
            }

        override fun calculateApproachOffset(velocity: Float, decayOffset: Float): Float {
            return (decayOffset.absoluteValue - averageItemSize).coerceAtLeast(0.0f) *
                decayOffset.sign
        }

        override fun calculateSnapOffset(velocity: Float): Float {
            var lowerBoundOffset = Float.NEGATIVE_INFINITY
            var upperBoundOffset = Float.POSITIVE_INFINITY

            layoutInfo.visibleItemsInfo.fastForEach { item ->
                if ((item as? LazyLayoutMeasuredItem)?.nonScrollableItem == true) return@fastForEach
                val offset =
                    calculateDistanceToDesiredSnapPosition(
                        mainAxisViewPortSize = layoutInfo.singleAxisViewportSize,
                        beforeContentPadding = layoutInfo.beforeContentPadding,
                        afterContentPadding = layoutInfo.afterContentPadding,
                        itemSize = item.size,
                        itemOffset = item.offset,
                        itemIndex = item.index,
                        snapPosition = snapPosition,
                        itemCount = layoutInfo.totalItemsCount
                    )

                // Find item that is closest to the center
                if (offset <= 0 && offset > lowerBoundOffset) {
                    lowerBoundOffset = offset
                }

                // Find item that is closest to center, but after it
                if (offset >= 0 && offset < upperBoundOffset) {
                    upperBoundOffset = offset
                }
            }

            return calculateFinalOffset(
                with(lazyListState.density) { calculateFinalSnappingItem(velocity) },
                lowerBoundOffset,
                upperBoundOffset
            )
        }
    }

/**
 * Create and remember a FlingBehavior for decayed snapping in Lazy Lists. This will snap the item
 * according to [snapPosition].
 *
 * @param lazyListState The [LazyListState] from the LazyList where this [FlingBehavior] will be
 *   used.
 * @param snapPosition The desired positioning of the snapped item within the main layout. This
 *   position should be considered with regards to the start edge of the item and the placement
 *   within the viewport.
 */
@Composable
fun rememberSnapFlingBehavior(
    lazyListState: LazyListState,
    snapPosition: SnapPosition = SnapPosition.Center
): FlingBehavior {
    val snappingLayout =
        remember(lazyListState) { SnapLayoutInfoProvider(lazyListState, snapPosition) }
    return rememberSnapFlingBehavior(snappingLayout)
}

internal val LazyListLayoutInfo.singleAxisViewportSize: Int
    get() = if (orientation == Orientation.Vertical) viewportSize.height else viewportSize.width

@kotlin.jvm.JvmInline
internal value class FinalSnappingItem
internal constructor(@Suppress("unused") private val value: Int) {
    companion object {

        val ClosestItem: FinalSnappingItem = FinalSnappingItem(0)

        val NextItem: FinalSnappingItem = FinalSnappingItem(1)

        val PreviousItem: FinalSnappingItem = FinalSnappingItem(2)
    }
}

internal fun Density.calculateFinalSnappingItem(velocity: Float): FinalSnappingItem {
    return if (velocity.absoluteValue < MinFlingVelocityDp.toPx()) {
        FinalSnappingItem.ClosestItem
    } else {
        if (velocity > 0) FinalSnappingItem.NextItem else FinalSnappingItem.PreviousItem
    }
}
