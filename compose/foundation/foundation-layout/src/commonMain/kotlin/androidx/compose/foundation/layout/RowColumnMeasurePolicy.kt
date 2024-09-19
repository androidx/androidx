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

package androidx.compose.foundation.layout

import androidx.compose.foundation.layout.internal.checkPrecondition
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

internal interface RowColumnMeasurePolicy {
    fun Placeable.mainAxisSize(): Int

    fun Placeable.crossAxisSize(): Int

    fun populateMainAxisPositions(
        mainAxisLayoutSize: Int,
        childrenMainAxisSize: IntArray,
        mainAxisPositions: IntArray,
        measureScope: MeasureScope
    )

    fun placeHelper(
        placeables: Array<Placeable?>,
        measureScope: MeasureScope,
        beforeCrossAxisAlignmentLine: Int,
        mainAxisPositions: IntArray,
        mainAxisLayoutSize: Int,
        crossAxisLayoutSize: Int,
        crossAxisOffset: IntArray?,
        currentLineIndex: Int,
        startIndex: Int,
        endIndex: Int
    ): MeasureResult

    fun createConstraints(
        mainAxisMin: Int,
        crossAxisMin: Int,
        mainAxisMax: Int,
        crossAxisMax: Int,
        isPrioritizing: Boolean = false
    ): Constraints
}

/**
 * Measures the row and column
 *
 * @param measureScope The measure scope to retrieve density
 * @param startIndex The startIndex (inclusive) when examining measurables, placeable and parentData
 * @param endIndex The ending index (exclusive) when examining measurable, placeable and parentData
 * @param crossAxisOffset The offset to apply to the cross axis when placing
 * @param currentLineIndex The index of the current line if in a multi-row/column setting like
 *   [FlowRow]
 */
internal fun RowColumnMeasurePolicy.measure(
    mainAxisMin: Int,
    crossAxisMin: Int,
    mainAxisMax: Int,
    crossAxisMax: Int,
    arrangementSpacingInt: Int,
    measureScope: MeasureScope,
    measurables: List<Measurable>,
    placeables: Array<Placeable?>,
    startIndex: Int,
    endIndex: Int,
    crossAxisOffset: IntArray? = null,
    currentLineIndex: Int = 0,
): MeasureResult {
    val arrangementSpacingPx = arrangementSpacingInt.toLong()

    var totalWeight = 0f
    var fixedSpace = 0
    var crossAxisSpace = 0
    var weightChildrenCount = 0

    var anyAlignBy = false
    val subSize = endIndex - startIndex
    val childrenMainAxisSize = IntArray(subSize)

    var beforeCrossAxisAlignmentLine = 0
    var afterCrossAxisAlignmentLine = 0
    // First measure children with zero weight.
    var spaceAfterLastNoWeight = 0

    for (i in startIndex until endIndex) {
        val child = measurables[i]
        val parentData = child.rowColumnParentData
        val weight = parentData.weight
        anyAlignBy = anyAlignBy || parentData.isRelative

        if (weight > 0f) {
            totalWeight += weight
            ++weightChildrenCount
        } else {
            val crossAxisDesiredSize =
                if (crossAxisMax == Constraints.Infinity) null
                else
                    parentData?.flowLayoutData?.let {
                        (it.fillCrossAxisFraction * crossAxisMax).fastRoundToInt()
                    }
            val remaining = mainAxisMax - fixedSpace
            val placeable =
                placeables[i]
                    ?: child.measure(
                        // Ask for preferred main axis size.
                        createConstraints(
                            mainAxisMin = 0,
                            crossAxisMin = crossAxisDesiredSize ?: 0,
                            mainAxisMax =
                                if (mainAxisMax == Constraints.Infinity) {
                                    Constraints.Infinity
                                } else {
                                    remaining.fastCoerceAtLeast(0)
                                },
                            crossAxisMax = crossAxisDesiredSize ?: crossAxisMax
                        )
                    )
            val placeableMainAxisSize = placeable.mainAxisSize()
            val placeableCrossAxisSize = placeable.crossAxisSize()
            childrenMainAxisSize[i - startIndex] = placeableMainAxisSize
            spaceAfterLastNoWeight =
                min(arrangementSpacingInt, (remaining - placeableMainAxisSize).fastCoerceAtLeast(0))
            fixedSpace += placeableMainAxisSize + spaceAfterLastNoWeight
            crossAxisSpace = max(crossAxisSpace, placeableCrossAxisSize)
            placeables[i] = placeable
        }
    }

    var weightedSpace = 0
    if (weightChildrenCount == 0) {
        // fixedSpace contains an extra spacing after the last non-weight child.
        fixedSpace -= spaceAfterLastNoWeight
    } else {
        // Measure the rest according to their weights in the remaining main axis space.
        val targetSpace =
            if (mainAxisMax != Constraints.Infinity) {
                mainAxisMax
            } else {
                mainAxisMin
            }
        val arrangementSpacingTotal = arrangementSpacingPx * (weightChildrenCount - 1)
        val remainingToTarget =
            (targetSpace - fixedSpace - arrangementSpacingTotal).fastCoerceAtLeast(0)

        val weightUnitSpace = remainingToTarget / totalWeight
        var remainder = remainingToTarget
        for (i in startIndex until endIndex) {
            val measurable = measurables[i]
            val itemWeight = measurable.rowColumnParentData.weight
            val weightedSize = (weightUnitSpace * itemWeight)
            remainder -= weightedSize.fastRoundToInt()
        }

        for (i in startIndex until endIndex) {
            if (placeables[i] == null) {
                val child = measurables[i]
                val parentData = child.rowColumnParentData
                val weight = parentData.weight
                val crossAxisDesiredSize =
                    if (crossAxisMax == Constraints.Infinity) null
                    else
                        parentData?.flowLayoutData?.let {
                            (it.fillCrossAxisFraction * crossAxisMax).fastRoundToInt()
                        }
                checkPrecondition(weight > 0) { "All weights <= 0 should have placeables" }
                // After the weightUnitSpace rounding, the total space going to be occupied
                // can be smaller or larger than remainingToTarget. Here we distribute the
                // loss or gain remainder evenly to the first children.
                val remainderUnit = remainder.sign
                remainder -= remainderUnit
                val weightedSize = (weightUnitSpace * weight)
                val childMainAxisSize = max(0, weightedSize.fastRoundToInt() + remainderUnit)
                val childConstraints: Constraints =
                    createConstraints(
                        mainAxisMin =
                            if (parentData.fill && childMainAxisSize != Constraints.Infinity) {
                                childMainAxisSize
                            } else {
                                0
                            },
                        crossAxisMin = crossAxisDesiredSize ?: 0,
                        mainAxisMax = childMainAxisSize,
                        crossAxisMax = crossAxisDesiredSize ?: crossAxisMax,
                        isPrioritizing = true
                    )
                val placeable = child.measure(childConstraints)
                val placeableMainAxisSize = placeable.mainAxisSize()
                val placeableCrossAxisSize = placeable.crossAxisSize()
                childrenMainAxisSize[i - startIndex] = placeableMainAxisSize
                weightedSpace += placeableMainAxisSize
                crossAxisSpace = max(crossAxisSpace, placeableCrossAxisSize)
                placeables[i] = placeable
            }
        }
        weightedSpace =
            (weightedSpace + arrangementSpacingTotal)
                .toInt()
                .fastCoerceIn(0, mainAxisMax - fixedSpace)
    }

    // we've done this check in weights as to avoid going through another loop
    if (anyAlignBy) {
        for (i in startIndex until endIndex) {
            val placeable = placeables[i]
            val parentData = placeable!!.rowColumnParentData
            val alignmentLinePosition =
                parentData.crossAxisAlignment?.calculateAlignmentLinePosition(placeable)
            alignmentLinePosition?.let {
                val placeableCrossAxisSize = placeable.crossAxisSize()
                beforeCrossAxisAlignmentLine =
                    max(
                        beforeCrossAxisAlignmentLine,
                        if (it != AlignmentLine.Unspecified) alignmentLinePosition else 0
                    )
                afterCrossAxisAlignmentLine =
                    max(
                        afterCrossAxisAlignmentLine,
                        placeableCrossAxisSize -
                            if (it != AlignmentLine.Unspecified) {
                                it
                            } else {
                                placeableCrossAxisSize
                            }
                    )
            }
        }
    }

    // Compute the Row or Column size and position the children.
    val mainAxisLayoutSize = max((fixedSpace + weightedSpace).fastCoerceAtLeast(0), mainAxisMin)
    val crossAxisLayoutSize =
        maxOf(
            crossAxisSpace,
            crossAxisMin,
            beforeCrossAxisAlignmentLine + afterCrossAxisAlignmentLine
        )
    val mainAxisPositions = IntArray(subSize)
    populateMainAxisPositions(
        mainAxisLayoutSize,
        childrenMainAxisSize,
        mainAxisPositions,
        measureScope
    )

    return placeHelper(
        placeables,
        measureScope,
        beforeCrossAxisAlignmentLine,
        mainAxisPositions,
        mainAxisLayoutSize,
        crossAxisLayoutSize,
        crossAxisOffset,
        currentLineIndex,
        startIndex,
        endIndex
    )
}
