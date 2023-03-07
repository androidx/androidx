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
import androidx.compose.foundation.fastFilter
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastSumBy
import kotlin.math.absoluteValue
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

    override fun Density.calculateApproachOffset(initialVelocity: Float): Float {
        val decayAnimationSpec: DecayAnimationSpec<Float> = splineBasedDecay(this)
        val offset =
            decayAnimationSpec.calculateTargetValue(NoDistance, initialVelocity).absoluteValue
        val finalDecayOffset = (offset - calculateSnapStepSize()).coerceAtLeast(0f)
        return if (finalDecayOffset == 0f) {
            finalDecayOffset
        } else {
            finalDecayOffset * initialVelocity.sign
        }
    }

    private val singleAxisItems: List<LazyGridItemInfo>
        get() = lazyGridState.layoutInfo.visibleItemsInfo.fastFilter {
            if (lazyGridState.layoutInfo.orientation == Orientation.Horizontal) {
                it.row == 0
            } else {
                it.column == 0
            }
        }

    override fun Density.calculateSnappingOffset(
        currentVelocity: Float
    ): Float {
        var distanceFromItemBeforeTarget = Float.NEGATIVE_INFINITY
        var distanceFromItemAfterTarget = Float.POSITIVE_INFINITY

        layoutInfo.visibleItemsInfo.fastForEach { item ->
            val distance =
                calculateDistanceToDesiredSnapPosition(layoutInfo, item, positionInLayout)

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

    override fun Density.calculateSnapStepSize(): Float {
        return if (singleAxisItems.isNotEmpty()) {
            val size = if (layoutInfo.orientation == Orientation.Vertical) {
                singleAxisItems.fastSumBy { it.size.height }
            } else {
                singleAxisItems.fastSumBy { it.size.width }
            }
            size / singleAxisItems.size.toFloat()
        } else {
            0f
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun Density.calculateDistanceToDesiredSnapPosition(
    layoutInfo: LazyGridLayoutInfo,
    item: LazyGridItemInfo,
    positionInLayout: SnapPositionInLayout = SnapPositionInLayout.CenterToCenter
): Float {

    val containerSize =
        with(layoutInfo) { singleAxisViewportSize - beforeContentPadding - afterContentPadding }

    val desiredDistance = with(positionInLayout) {
        position(containerSize, item.sizeOnMainAxis(layoutInfo.orientation), item.index)
    }

    val itemCurrentPosition = item.offsetOnMainAxis(layoutInfo.orientation)
    return itemCurrentPosition - desiredDistance.toFloat()
}

private val LazyGridLayoutInfo.singleAxisViewportSize: Int
    get() = if (orientation == Orientation.Vertical) {
        viewportSize.height
    } else {
        viewportSize.width
    }

private fun LazyGridItemInfo.sizeOnMainAxis(orientation: Orientation): Int {
    return if (orientation == Orientation.Vertical) {
        size.height
    } else {
        size.width
    }
}

private fun LazyGridItemInfo.offsetOnMainAxis(orientation: Orientation): Int {
    return if (orientation == Orientation.Vertical) {
        offset.y
    } else {
        offset.x
    }
}