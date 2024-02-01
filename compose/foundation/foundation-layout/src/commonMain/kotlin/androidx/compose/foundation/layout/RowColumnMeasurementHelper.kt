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

import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastRoundToInt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * This is a data class that holds the determined width, height of a row,
 * and information on how to retrieve main axis and cross axis positions.
 */
internal class RowColumnMeasureHelperResult(
    val crossAxisSize: Int,
    val mainAxisSize: Int,
    val startIndex: Int,
    val endIndex: Int,
    val beforeCrossAxisAlignmentLine: Int,
    val mainAxisPositions: IntArray,
)

/**
 * RowColumnMeasurementHelper
 * Measures the row and column without placing, useful for reusing row/column logic
 */
internal class RowColumnMeasurementHelper(
    val orientation: LayoutOrientation,
    val horizontalArrangement: Arrangement.Horizontal?,
    val verticalArrangement: Arrangement.Vertical?,
    val arrangementSpacing: Dp,
    val crossAxisSize: SizeMode,
    val crossAxisAlignment: CrossAxisAlignment,
    val measurables: List<Measurable>,
    val placeables: Array<Placeable?>
) {

    private val rowColumnParentData = Array(measurables.size) {
        measurables[it].rowColumnParentData
    }

    fun Placeable.mainAxisSize() =
        if (orientation == LayoutOrientation.Horizontal) width else height

    fun Placeable.crossAxisSize() =
        if (orientation == LayoutOrientation.Horizontal) height else width

    /**
     * Measures the row and column without placing, useful for reusing row/column logic
     *
     * @param measureScope The measure scope to retrieve density
     * @param constraints The desired constraints for the startIndex and endIndex
     * can hold null items if not measured.
     * @param startIndex The startIndex (inclusive) when examining measurables, placeable
     * and parentData
     * @param endIndex The ending index (exclusive) when examinning measurable, placeable
     * and parentData
     */
    fun measureWithoutPlacing(
        measureScope: MeasureScope,
        constraints: Constraints,
        startIndex: Int,
        endIndex: Int
    ): RowColumnMeasureHelperResult {
        @Suppress("NAME_SHADOWING")
        val constraints = OrientationIndependentConstraints(constraints, orientation)
        val arrangementSpacingPx = with(measureScope) {
            arrangementSpacing.roundToPx().toLong()
        }

        var totalWeight = 0f
        var fixedSpace = 0L
        var crossAxisSpace = 0
        var weightChildrenCount = 0

        var anyAlignBy = false
        val subSize = endIndex - startIndex

        // First measure children with zero weight.
        var spaceAfterLastNoWeight = 0
        for (i in startIndex until endIndex) {
            val child = measurables[i]
            val parentData = rowColumnParentData[i]
            val weight = parentData.weight

            if (weight > 0f) {
                totalWeight += weight
                ++weightChildrenCount
            } else {
                val mainAxisMax = constraints.mainAxisMax
                val crossAxisMax = constraints.crossAxisMax
                val crossAxisDesiredSize = if (crossAxisMax == Constraints.Infinity) null else
                    parentData?.flowLayoutData?.let {
                        (it.fillCrossAxisFraction * crossAxisMax).fastRoundToInt()
                    }
                val placeable = placeables[i] ?: child.measure(
                    // Ask for preferred main axis size.
                    constraints.copy(
                        mainAxisMin = 0,
                        mainAxisMax = if (mainAxisMax == Constraints.Infinity) {
                            Constraints.Infinity
                        } else {
                            (mainAxisMax - fixedSpace).coerceAtLeast(0).toInt()
                        },
                        crossAxisMin = crossAxisDesiredSize ?: 0,
                        crossAxisMax = crossAxisDesiredSize ?: constraints.crossAxisMax
                    ).toBoxConstraints(orientation)
                )
                spaceAfterLastNoWeight = min(
                    arrangementSpacingPx.toInt(),
                    (mainAxisMax - fixedSpace - placeable.mainAxisSize())
                        .coerceAtLeast(0).toInt()
                )
                fixedSpace += placeable.mainAxisSize() + spaceAfterLastNoWeight
                crossAxisSpace = max(crossAxisSpace, placeable.crossAxisSize())
                anyAlignBy = anyAlignBy || parentData.isRelative
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
                if (totalWeight > 0f && constraints.mainAxisMax != Constraints.Infinity) {
                    constraints.mainAxisMax
                } else {
                    constraints.mainAxisMin
                }
            val arrangementSpacingTotal = arrangementSpacingPx * (weightChildrenCount - 1)
            val remainingToTarget =
                (targetSpace - fixedSpace - arrangementSpacingTotal).coerceAtLeast(0)

            val weightUnitSpace = if (totalWeight > 0) remainingToTarget / totalWeight else 0f
            var remainder = remainingToTarget - (startIndex until endIndex).sumOf {
                (weightUnitSpace * rowColumnParentData[it].weight).fastRoundToInt()
            }

            for (i in startIndex until endIndex) {
                if (placeables[i] == null) {
                    val child = measurables[i]
                    val parentData = rowColumnParentData[i]
                    val weight = parentData.weight
                    val crossAxisMax = constraints.crossAxisMax
                    val crossAxisDesiredSize = if (crossAxisMax == Constraints.Infinity) null else
                        parentData?.flowLayoutData?.let {
                            (it.fillCrossAxisFraction * crossAxisMax).fastRoundToInt()
                        }
                    check(weight > 0) { "All weights <= 0 should have placeables" }
                    // After the weightUnitSpace rounding, the total space going to be occupied
                    // can be smaller or larger than remainingToTarget. Here we distribute the
                    // loss or gain remainder evenly to the first children.
                    val remainderUnit = remainder.sign
                    remainder -= remainderUnit
                    val childMainAxisSize = max(
                        0,
                        (weightUnitSpace * weight).fastRoundToInt() + remainderUnit
                    )
                    val restrictedConstraints = Constraints.restrictedConstraints(
                        minWidth = if (parentData.fill &&
                            childMainAxisSize != Constraints.Infinity
                        ) {
                            childMainAxisSize
                        } else {
                            0
                        },
                        maxWidth = childMainAxisSize,
                        minHeight = crossAxisDesiredSize ?: 0,
                        maxHeight = crossAxisDesiredSize ?: constraints.crossAxisMax
                    )
                    val childConstraints = if (orientation == LayoutOrientation.Horizontal) {
                        restrictedConstraints
                    } else {
                        Constraints(
                            minHeight = restrictedConstraints.minWidth,
                            maxHeight = restrictedConstraints.maxWidth,
                            minWidth = restrictedConstraints.minHeight,
                            maxWidth = restrictedConstraints.maxHeight,
                        )
                    }
                    val placeable = child.measure(childConstraints)
                    weightedSpace += placeable.mainAxisSize()
                    crossAxisSpace = max(crossAxisSpace, placeable.crossAxisSize())
                    anyAlignBy = anyAlignBy || parentData.isRelative
                    placeables[i] = placeable
                }
            }
            weightedSpace = (weightedSpace + arrangementSpacingTotal)
                .coerceIn(0, constraints.mainAxisMax - fixedSpace)
                .toInt()
        }

        var beforeCrossAxisAlignmentLine = 0
        var afterCrossAxisAlignmentLine = 0
        if (anyAlignBy) {
            for (i in startIndex until endIndex) {
                val placeable = placeables[i]!!
                val parentData = rowColumnParentData[i]
                val alignmentLinePosition = parentData.crossAxisAlignment
                    ?.calculateAlignmentLinePosition(placeable)
                if (alignmentLinePosition != null) {
                    beforeCrossAxisAlignmentLine = max(
                        beforeCrossAxisAlignmentLine,
                        alignmentLinePosition.let {
                            if (it != AlignmentLine.Unspecified) it else 0
                        }
                    )
                    afterCrossAxisAlignmentLine = max(
                        afterCrossAxisAlignmentLine,
                        placeable.crossAxisSize() -
                            (
                                alignmentLinePosition.let {
                                    if (it != AlignmentLine.Unspecified) {
                                        it
                                    } else {
                                        placeable.crossAxisSize()
                                    }
                                }
                                )
                    )
                }
            }
        }

        // Compute the Row or Column size and position the children.
        val mainAxisLayoutSize = max(
            (fixedSpace + weightedSpace).coerceAtLeast(0).toInt(),
            constraints.mainAxisMin
        )
        val crossAxisLayoutSize = if (constraints.crossAxisMax != Constraints.Infinity &&
            crossAxisSize == SizeMode.Expand
        ) {
            constraints.crossAxisMax
        } else {
            max(
                crossAxisSpace,
                max(
                    constraints.crossAxisMin,
                    beforeCrossAxisAlignmentLine + afterCrossAxisAlignmentLine
                )
            )
        }
        val mainAxisPositions = IntArray(subSize) { 0 }
        val childrenMainAxisSize = IntArray(subSize) { index ->
            placeables[index + startIndex]!!.mainAxisSize()
        }

        return RowColumnMeasureHelperResult(
            mainAxisSize = mainAxisLayoutSize,
            crossAxisSize = crossAxisLayoutSize,
            startIndex = startIndex,
            endIndex = endIndex,
            beforeCrossAxisAlignmentLine = beforeCrossAxisAlignmentLine,
            mainAxisPositions = mainAxisPositions(
                mainAxisLayoutSize,
                childrenMainAxisSize,
                mainAxisPositions,
                measureScope
            ))
    }

    private fun mainAxisPositions(
        mainAxisLayoutSize: Int,
        childrenMainAxisSize: IntArray,
        mainAxisPositions: IntArray,
        measureScope: MeasureScope
    ): IntArray {
        if (orientation == LayoutOrientation.Vertical) {
            with(requireNotNull(verticalArrangement) { "null verticalArrangement in Column" }) {
                measureScope.arrange(
                    mainAxisLayoutSize,
                    childrenMainAxisSize,
                    mainAxisPositions
                )
            }
        } else {
            with(requireNotNull(horizontalArrangement) { "null horizontalArrangement in Row" }) {
                measureScope.arrange(
                    mainAxisLayoutSize,
                    childrenMainAxisSize,
                    measureScope.layoutDirection,
                    mainAxisPositions
                )
            }
        }
        return mainAxisPositions
    }

    private fun getCrossAxisPosition(
        placeable: Placeable,
        parentData: RowColumnParentData?,
        crossAxisLayoutSize: Int,
        layoutDirection: LayoutDirection,
        beforeCrossAxisAlignmentLine: Int
    ): Int {
        val childCrossAlignment = parentData?.crossAxisAlignment ?: crossAxisAlignment
        return childCrossAlignment.align(
            size = crossAxisLayoutSize - placeable.crossAxisSize(),
            layoutDirection = if (orientation == LayoutOrientation.Horizontal) {
                LayoutDirection.Ltr
            } else {
                layoutDirection
            },
            placeable = placeable,
            beforeCrossAxisAlignmentLine = beforeCrossAxisAlignmentLine
        )
    }
    fun placeHelper(
        placeableScope: Placeable.PlacementScope,
        measureResult: RowColumnMeasureHelperResult,
        crossAxisOffset: Int,
        layoutDirection: LayoutDirection,
    ) {
        with(placeableScope) {
            for (i in measureResult.startIndex until measureResult.endIndex) {
                val placeable = placeables[i]
                placeable!!
                val mainAxisPositions = measureResult.mainAxisPositions
                val crossAxisPosition = getCrossAxisPosition(
                    placeable,
                    (measurables[i].parentData as? RowColumnParentData),
                    measureResult.crossAxisSize,
                    layoutDirection,
                    measureResult.beforeCrossAxisAlignmentLine
                ) + crossAxisOffset
                if (orientation == LayoutOrientation.Horizontal) {
                    placeable.place(
                        mainAxisPositions[i - measureResult.startIndex],
                        crossAxisPosition
                    )
                } else {
                    placeable.place(
                        crossAxisPosition,
                        mainAxisPositions[i - measureResult.startIndex]
                    )
                }
            }
        }
    }
}
