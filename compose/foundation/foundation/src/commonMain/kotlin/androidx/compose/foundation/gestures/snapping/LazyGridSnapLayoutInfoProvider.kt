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

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastSumBy
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.sign

/**
 * A [SnapLayoutInfoProvider] for LazyGrids.
 *
 * @param lazyGridState The [LazyGridState] with information about the current state of the grid
 * @param positionInLayout The desired positioning of the snapped item within the main layout.
 * This position should be considered with regards to the start edge of the item and the placement
 * within the viewport.
 *
 * @return A [SnapLayoutInfoProvider] that can be used with [SnapFlingBehavior]
 */
@ExperimentalFoundationApi
fun SnapLayoutInfoProvider(
    lazyGridState: LazyGridState,
    positionInLayout: SnapPositionInLayout = SnapPositionInLayout.CenterToCenter
) = object : SnapLayoutInfoProvider {
    private val layoutInfo: LazyGridLayoutInfo
        get() = lazyGridState.layoutInfo

    override fun calculateApproachOffset(initialVelocity: Float): Float {
        val decayAnimationSpec: DecayAnimationSpec<Float> = splineBasedDecay(lazyGridState.density)
        val offset =
            decayAnimationSpec.calculateTargetValue(NoDistance, initialVelocity).absoluteValue

        val estimatedNumberOfItemsInDecay = floor(offset.absoluteValue / averageItemSize())

        // Decay to exactly half an item before the item where this decay would let us finish.
        // The rest of the animation will be a snapping animation.
        val approachOffset = estimatedNumberOfItemsInDecay * averageItemSize() - averageItemSize()
        val finalDecayOffset = approachOffset.coerceAtLeast(0f)

        return if (finalDecayOffset == 0f) {
            finalDecayOffset
        } else {
            finalDecayOffset * initialVelocity.sign
        }
    }

    private fun singleAxisItems(): List<LazyGridItemInfo> {
        return lazyGridState.layoutInfo.visibleItemsInfo.fastFilter {
            if (lazyGridState.layoutInfo.orientation == Orientation.Horizontal) {
                it.row == 0
            } else {
                it.column == 0
            }
        }
    }

    override fun calculateSnappingOffset(
        currentVelocity: Float
    ): Float {
        var distanceFromItemBeforeTarget = Float.NEGATIVE_INFINITY
        var distanceFromItemAfterTarget = Float.POSITIVE_INFINITY

        layoutInfo.visibleItemsInfo.fastForEach { item ->
            val distance =
                calculateDistanceToDesiredSnapPosition(
                    mainAxisViewPortSize = layoutInfo.singleAxisViewportSize,
                    beforeContentPadding = layoutInfo.beforeContentPadding,
                    afterContentPadding = layoutInfo.afterContentPadding,
                    itemSize = item.sizeOnMainAxis(orientation = layoutInfo.orientation),
                    itemOffset = item.offsetOnMainAxis(orientation = layoutInfo.orientation),
                    itemIndex = item.index,
                    snapPositionInLayout = positionInLayout
                )

            // Find item that is closest to the center
            if (distance <= 0 && distance > distanceFromItemBeforeTarget) {
                distanceFromItemBeforeTarget = distance
            }

            // Find item that is closest to center, but after it
            if (distance >= 0 && distance < distanceFromItemAfterTarget) {
                distanceFromItemAfterTarget = distance
            }
        }

        return calculateFinalOffset(
            currentVelocity,
            distanceFromItemBeforeTarget,
            distanceFromItemAfterTarget
        )
    }

    fun averageItemSize(): Float {
        val items = singleAxisItems()
        return if (items.isNotEmpty()) {
            val size = if (layoutInfo.orientation == Orientation.Vertical) {
                items.fastSumBy { it.size.height }
            } else {
                items.fastSumBy { it.size.width }
            }
            size / items.size.toFloat()
        } else {
            0f
        }
    }
}

internal val LazyGridLayoutInfo.singleAxisViewportSize: Int
    get() = if (orientation == Orientation.Vertical) {
        viewportSize.height
    } else {
        viewportSize.width
    }

internal fun LazyGridItemInfo.sizeOnMainAxis(orientation: Orientation): Int {
    return if (orientation == Orientation.Vertical) {
        size.height
    } else {
        size.width
    }
}

internal fun LazyGridItemInfo.offsetOnMainAxis(orientation: Orientation): Int {
    return if (orientation == Orientation.Vertical) {
        offset.y
    } else {
        offset.x
    }
}
