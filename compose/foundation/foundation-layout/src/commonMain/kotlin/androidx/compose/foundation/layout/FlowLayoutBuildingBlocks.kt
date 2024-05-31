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
package androidx.compose.foundation.layout

import androidx.collection.IntIntPair
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class)
internal class FlowLayoutBuildingBlocks(
    private val maxItemsInMainAxis: Int,
    private val overflow: FlowLayoutOverflowState,
    private val constraints: OrientationIndependentConstraints,
    private val maxLines: Int,
    private val mainAxisSpacing: Int,
    private val crossAxisSpacing: Int,
) {
    class WrapInfo(
        val isLastItemInLine: Boolean = false,
        val isLastItemInContainer: Boolean = false
    )

    class WrapEllipsisInfo(
        val ellipsis: Measurable,
        val placeable: Placeable?,
        val ellipsisSize: IntIntPair,
        var placeEllipsisOnLastContentLine: Boolean = true,
    )

    fun getWrapEllipsisInfo(
        wrapInfo: WrapInfo,
        hasNext: Boolean,
        lastContentLineIndex: Int,
        totalCrossAxisSize: Int,
        leftOverMainAxis: Int,
        nextIndexInLine: Int
    ): WrapEllipsisInfo? {
        if (!wrapInfo.isLastItemInContainer) return null

        val ellipsisInfo =
            overflow.ellipsisInfo(hasNext, lastContentLineIndex, totalCrossAxisSize) ?: return null

        val canFitLine =
            lastContentLineIndex >= 0 &&
                (nextIndexInLine == 0 ||
                    !(leftOverMainAxis - ellipsisInfo.ellipsisSize.first < 0 ||
                        nextIndexInLine >= maxItemsInMainAxis))

        ellipsisInfo.placeEllipsisOnLastContentLine = canFitLine
        return ellipsisInfo
    }

    fun getWrapInfo(
        nextItemHasNext: Boolean,
        nextIndexInLine: Int,
        leftOver: IntIntPair,
        nextSize: IntIntPair?,
        lineIndex: Int,
        totalCrossAxisSize: Int,
        currentLineCrossAxisSize: Int,
        isWrappingRound: Boolean,
        isEllipsisWrap: Boolean,
    ): WrapInfo {
        var totalContainerCrossAxisSize = totalCrossAxisSize + currentLineCrossAxisSize
        if (nextSize == null) {
            return WrapInfo(isLastItemInLine = true, isLastItemInContainer = true)
        }

        val willOverflowCrossAxis =
            when {
                overflow.type == FlowLayoutOverflow.OverflowType.Visible -> false
                lineIndex >= maxLines -> true
                leftOver.second - nextSize.second < 0 -> true
                else -> false
            }

        if (willOverflowCrossAxis) {
            return WrapInfo(isLastItemInLine = true, isLastItemInContainer = true)
        }

        val shouldWrapItem =
            when {
                nextIndexInLine == 0 -> false
                nextIndexInLine >= maxItemsInMainAxis -> true
                leftOver.first - nextSize.first < 0 -> true
                else -> false
            }

        if (shouldWrapItem) {
            if (isWrappingRound) {
                return WrapInfo(isLastItemInLine = true, isLastItemInContainer = true)
            }
            val wrapInfo =
                getWrapInfo(
                    nextItemHasNext,
                    nextIndexInLine = 0,
                    leftOver =
                        IntIntPair(
                            constraints.mainAxisMax,
                            leftOver.second - crossAxisSpacing - currentLineCrossAxisSize
                        ),
                    // remove the mainAxisSpacing added to 2nd position or more indexed items.
                    IntIntPair(
                        first = nextSize.first.minus(mainAxisSpacing),
                        second = nextSize.second
                    ),
                    lineIndex = lineIndex + 1,
                    totalCrossAxisSize = totalContainerCrossAxisSize,
                    currentLineCrossAxisSize = 0,
                    isWrappingRound = true,
                    isEllipsisWrap = false
                )
            return WrapInfo(
                isLastItemInLine = true,
                isLastItemInContainer = wrapInfo.isLastItemInContainer
            )
        }

        totalContainerCrossAxisSize =
            totalCrossAxisSize + max(currentLineCrossAxisSize, nextSize.second)

        val ellipsis =
            if (isEllipsisWrap) {
                null
            } else {
                overflow.ellipsisSize(nextItemHasNext, lineIndex, totalContainerCrossAxisSize)
            }
        val shouldWrapEllipsis =
            ellipsis?.run {
                when {
                    nextIndexInLine + 1 >= maxItemsInMainAxis -> true
                    leftOver.first - nextSize.first - mainAxisSpacing - ellipsis.first < 0 -> true
                    else -> false
                }
            } ?: false

        if (shouldWrapEllipsis) {
            if (isEllipsisWrap) {
                return WrapInfo(isLastItemInLine = true, isLastItemInContainer = true)
            }
            val wrapInfo =
                getWrapInfo(
                    nextItemHasNext = false,
                    nextIndexInLine = 0,
                    IntIntPair(
                        constraints.mainAxisMax,
                        leftOver.second -
                            crossAxisSpacing -
                            max(currentLineCrossAxisSize, nextSize.second)
                    ),
                    ellipsis,
                    lineIndex = lineIndex + 1,
                    totalCrossAxisSize = totalContainerCrossAxisSize,
                    currentLineCrossAxisSize = 0,
                    isWrappingRound = true,
                    isEllipsisWrap = true
                )

            return WrapInfo(
                isLastItemInLine = wrapInfo.isLastItemInContainer,
                isLastItemInContainer = wrapInfo.isLastItemInContainer
            )
        }

        return WrapInfo(isLastItemInLine = false, isLastItemInContainer = false)
    }
}
