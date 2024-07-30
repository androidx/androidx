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

package androidx.compose.foundation.gestures.snapping

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.internal.checkPrecondition
import androidx.compose.foundation.pager.PagerDebugConfig
import androidx.compose.foundation.pager.PagerLayoutInfo
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.mainAxisViewportSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sign

internal fun SnapLayoutInfoProvider(
    pagerState: PagerState,
    pagerSnapDistance: PagerSnapDistance,
    calculateFinalSnappingBound: (Float, Float, Float) -> Float
): SnapLayoutInfoProvider {
    return object : SnapLayoutInfoProvider {
        val layoutInfo: PagerLayoutInfo
            get() = pagerState.layoutInfo

        fun Float.isValidDistance(): Boolean {
            return this != Float.POSITIVE_INFINITY && this != Float.NEGATIVE_INFINITY
        }

        override fun calculateSnapOffset(velocity: Float): Float {
            val snapPosition = pagerState.layoutInfo.snapPosition
            val (lowerBoundOffset, upperBoundOffset) = searchForSnappingBounds(snapPosition)

            val finalDistance =
                calculateFinalSnappingBound(velocity, lowerBoundOffset, upperBoundOffset)

            checkPrecondition(
                finalDistance == lowerBoundOffset ||
                    finalDistance == upperBoundOffset ||
                    finalDistance == 0.0f
            ) {
                "Final Snapping Offset Should Be one of $lowerBoundOffset, $upperBoundOffset or 0.0"
            }

            debugLog { "Snapping to=$finalDistance" }

            return if (finalDistance.isValidDistance()) {
                finalDistance
            } else {
                0f
            }
        }

        override fun calculateApproachOffset(velocity: Float, decayOffset: Float): Float {
            debugLog { "Approach Velocity=$velocity" }
            val effectivePageSizePx = pagerState.pageSize + pagerState.pageSpacing

            // Page Size is Zero, do not proceed.
            if (effectivePageSizePx == 0) return 0f

            // given this velocity, where can I go with a decay animation.
            val animationOffsetPx = decayOffset

            val startPage =
                if (velocity < 0) {
                    pagerState.firstVisiblePage + 1
                } else {
                    pagerState.firstVisiblePage
                }

            debugLog {
                "\nAnimation Offset=$animationOffsetPx " +
                    "\nFling Start Page=$startPage " +
                    "\nEffective Page Size=$effectivePageSizePx"
            }

            // How many pages fit in the animation offset.
            val pagesInAnimationOffset = animationOffsetPx / effectivePageSizePx

            debugLog { "Pages in Animation Offset=$pagesInAnimationOffset" }

            // Decide where this will get us in terms of target page.
            val targetPage =
                (startPage + pagesInAnimationOffset.toInt()).coerceIn(0, pagerState.pageCount)

            debugLog { "Fling Target Page=$targetPage" }

            // Apply the snap distance suggestion.
            val correctedTargetPage =
                pagerSnapDistance
                    .calculateTargetPage(
                        startPage,
                        targetPage,
                        velocity,
                        pagerState.pageSize,
                        pagerState.pageSpacing
                    )
                    .coerceIn(0, pagerState.pageCount)

            debugLog { "Fling Corrected Target Page=$correctedTargetPage" }

            // Calculate the offset with the new target page. The offset is the amount of
            // pixels between the start page and the new target page.
            val proposedFlingOffset = (correctedTargetPage - startPage) * effectivePageSizePx

            debugLog { "Proposed Fling Approach Offset=$proposedFlingOffset" }

            // We'd like the approach animation to finish right before the last page so we can
            // use a snapping animation for the rest.
            val flingApproachOffsetPx =
                (abs(proposedFlingOffset) - effectivePageSizePx).coerceAtLeast(0)

            // Apply the correct sign.
            return if (flingApproachOffsetPx == 0) {
                    flingApproachOffsetPx.toFloat()
                } else {
                    flingApproachOffsetPx * velocity.sign
                }
                .also { debugLog { "Fling Approach Offset=$it" } }
        }

        private fun searchForSnappingBounds(snapPosition: SnapPosition): Pair<Float, Float> {
            debugLog { "Calculating Snapping Bounds" }
            var lowerBoundOffset = Float.NEGATIVE_INFINITY
            var upperBoundOffset = Float.POSITIVE_INFINITY

            layoutInfo.visiblePagesInfo.fastForEach { page ->
                val offset =
                    calculateDistanceToDesiredSnapPosition(
                        mainAxisViewPortSize = layoutInfo.mainAxisViewportSize,
                        beforeContentPadding = layoutInfo.beforeContentPadding,
                        afterContentPadding = layoutInfo.afterContentPadding,
                        itemSize = layoutInfo.pageSize,
                        itemOffset = page.offset,
                        itemIndex = page.index,
                        snapPosition = snapPosition,
                        itemCount = pagerState.pageCount
                    )

                // Find page that is closest to the snap position, but before it
                if (offset <= 0 && offset > lowerBoundOffset) {
                    lowerBoundOffset = offset
                }

                // Find page that is closest to the snap position, but after it
                if (offset >= 0 && offset < upperBoundOffset) {
                    upperBoundOffset = offset
                }
            }

            // If any of the bounds is unavailable, use the other.
            if (lowerBoundOffset == Float.NEGATIVE_INFINITY) {
                lowerBoundOffset = upperBoundOffset
            }

            if (upperBoundOffset == Float.POSITIVE_INFINITY) {
                upperBoundOffset = lowerBoundOffset
            }

            // Don't move if we are at the bounds

            val isDragging = pagerState.dragGestureDelta() != 0f

            if (!pagerState.canScrollForward) {
                upperBoundOffset = 0.0f
                // If we can not scroll forward but are trying to move towards the bound, set both
                // bounds to 0 as we don't want to move
                if (isDragging && pagerState.isScrollingForward()) {
                    lowerBoundOffset = 0.0f
                }
            }

            if (!pagerState.canScrollBackward) {
                lowerBoundOffset = 0.0f
                // If we can not scroll backward but are trying to move towards the bound, set both
                // bounds to 0 as we don't want to move
                if (isDragging && !pagerState.isScrollingForward()) {
                    upperBoundOffset = 0.0f
                }
            }
            return lowerBoundOffset to upperBoundOffset
        }
    }
}

private fun PagerState.isLtrDragging() = dragGestureDelta() > 0

private fun PagerState.isScrollingForward(): Boolean {
    val reverseScrollDirection = layoutInfo.reverseLayout
    return (isLtrDragging() && reverseScrollDirection ||
        !isLtrDragging() && !reverseScrollDirection)
}

private fun PagerState.dragGestureDelta() =
    if (layoutInfo.orientation == Orientation.Horizontal) {
        upDownDifference.x
    } else {
        upDownDifference.y
    }

private inline fun debugLog(generateMsg: () -> String) {
    if (PagerDebugConfig.PagerSnapLayoutInfoProvider) {
        println("PagerSnapLayoutInfoProvider: ${generateMsg()}")
    }
}

/**
 * Given two possible bounds that this Pager can settle in represented by [lowerBoundOffset] and
 * [upperBoundOffset], this function will decide which one of them it will settle to.
 */
internal fun calculateFinalSnappingBound(
    pagerState: PagerState,
    layoutDirection: LayoutDirection,
    snapPositionalThreshold: Float,
    flingVelocity: Float,
    lowerBoundOffset: Float,
    upperBoundOffset: Float
): Float {

    val isForward =
        if (pagerState.layoutInfo.orientation == Orientation.Vertical) {
            pagerState.isScrollingForward()
        } else {
            if (layoutDirection == LayoutDirection.Ltr) {
                pagerState.isScrollingForward()
            } else {
                !pagerState.isScrollingForward()
            }
        }
    debugLog {
        "isLtrDragging=${pagerState.isLtrDragging()} " +
            "isForward=$isForward " +
            "layoutDirection=$layoutDirection"
    }
    // how many pages have I scrolled using a drag gesture.
    val pageSize = pagerState.layoutInfo.pageSize
    val offsetFromSnappedPosition =
        if (pageSize == 0) {
            0f
        } else {
            pagerState.dragGestureDelta() / pageSize.toFloat()
        }

    // we're only interested in the decimal part of the offset.
    val offsetFromSnappedPositionOverflow =
        offsetFromSnappedPosition - offsetFromSnappedPosition.toInt().toFloat()

    // If the velocity is not high, use the positional threshold to decide where to go.
    // This is applicable mainly when the user scrolls and lets go without flinging.
    val finalSnappingItem = with(pagerState.density) { calculateFinalSnappingItem(flingVelocity) }

    debugLog {
        "\nfinalSnappingItem=$finalSnappingItem" +
            "\nlower=$lowerBoundOffset" +
            "\nupper=$upperBoundOffset"
    }

    return when (finalSnappingItem) {
        FinalSnappingItem.ClosestItem -> {
            if (offsetFromSnappedPositionOverflow.absoluteValue > snapPositionalThreshold) {
                // If we crossed the threshold, go to the next bound
                debugLog { "Crossed Snap Positional Threshold" }
                if (isForward) upperBoundOffset else lowerBoundOffset
            } else {
                // if we haven't crossed the threshold. but scrolled minimally, we should
                // bound to the previous bound
                if (abs(offsetFromSnappedPosition) >= abs(pagerState.positionThresholdFraction)) {
                    debugLog { "Crossed Positional Threshold Fraction" }
                    if (isForward) lowerBoundOffset else upperBoundOffset
                } else {
                    // if we haven't scrolled minimally, settle for the closest bound
                    debugLog { "Snap To Closest" }
                    if (lowerBoundOffset.absoluteValue < upperBoundOffset.absoluteValue) {
                        lowerBoundOffset
                    } else {
                        upperBoundOffset
                    }
                }
            }
        }
        FinalSnappingItem.NextItem -> upperBoundOffset
        FinalSnappingItem.PreviousItem -> lowerBoundOffset
        else -> 0f
    }
}
