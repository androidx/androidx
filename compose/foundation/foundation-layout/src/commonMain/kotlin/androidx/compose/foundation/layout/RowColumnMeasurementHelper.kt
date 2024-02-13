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

import androidx.collection.IntList
import androidx.collection.MutableIntObjectMap
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
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
    val crossAxisSize: SizeMode,
    val crossAxisAlignment: CrossAxisAlignment,
    val listWrapper: RowColumnMeasurablesWrapper
) {

    val mainAxisSpacing = if (orientation == LayoutOrientation.Horizontal) {
        requireNotNull(horizontalArrangement) { "null horizontalArrangement in Row/FlowRow" }
        horizontalArrangement.spacing
    } else {
        requireNotNull(verticalArrangement) { "null verticalArrangement in Column/FlowColumn" }
        verticalArrangement.spacing
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
            mainAxisSpacing.roundToPx().toLong()
        }

        var totalWeight = 0f
        var fixedSpace = 0L
        var crossAxisSpace = 0
        var weightChildrenCount = 0

        var anyAlignBy = false
        val subSize = listWrapper.subSize(startIndex, endIndex)

        // First measure children with zero weight.
        var spaceAfterLastNoWeight = 0
        listWrapper.forEachIndexed(startIndex, endIndex) { i, child, placeableCache ->
            val parentData = child.rowColumnParentData
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
                val placeable = placeableCache ?: child.measure(
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
                listWrapper.setPlaceable(i, placeable)
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
            var remainder = remainingToTarget
            listWrapper.forEachIndexed(startIndex, endIndex) { _, measurable, _ ->
                remainder -=
                    (weightUnitSpace * measurable.rowColumnParentData.weight).fastRoundToInt()
            }

            listWrapper.forEachIndexed(startIndex, endIndex) { i, child, placeableCache ->
                if (placeableCache == null) {
                    val parentData = child.rowColumnParentData
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
                    listWrapper.setPlaceable(i, placeable)
                }
            }
            weightedSpace = (weightedSpace + arrangementSpacingTotal)
                .coerceIn(0, constraints.mainAxisMax - fixedSpace)
                .toInt()
        }

        var beforeCrossAxisAlignmentLine = 0
        var afterCrossAxisAlignmentLine = 0
        if (anyAlignBy) {
            listWrapper.forEachIndexed(startIndex, endIndex) { _, _, placeable ->
                val parentData = placeable!!.rowColumnParentData
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
        val childrenMainAxisSize = IntArray(subSize)
        listWrapper.forEachIndexed(startIndex, endIndex) { i, _, placeable ->
            childrenMainAxisSize[i - startIndex] = placeable!!.mainAxisSize()
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
            listWrapper.forEachIndexed(
                measureResult.startIndex, measureResult.endIndex
            ) { i, _, placeable ->
                placeable!!
                val mainAxisPositions = measureResult.mainAxisPositions
                val crossAxisPosition = getCrossAxisPosition(
                    placeable,
                    placeable.rowColumnParentData,
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

/**
 * The wrapper allows setting a range for processing its contents,
 * defined by startIndex and endIndex
 * and also considers the ellipsis handling as well.
 */
internal class RowColumnMeasurablesWrapper(
    private val measurables: List<Measurable>,
    private val placeables: MutableIntObjectMap<Placeable?> = mutableIntObjectMapOf(),
    private val ellipsis: Measurable? = null,
    private val ellipsisOnLineContent: Boolean? = null,
) {

    private val contentStart: Int = 0
    private val contentEnd: Int = measurables.size
    private var ellipsisPlaceable: Placeable? = null

    /**
     * Breaks the loop into multiple lines, taking into considering the ellipsis
     */
    inline fun forEachLine(
        breakLineIndices: IntList,
        action: (lineNo: Int, startIndex: Int, endIndex: Int) -> Unit
    ) {
        var start = contentStart
        var lineNo = 0
        breakLineIndices.forEach { end ->
            require(end in (start + 1)..contentEnd) {
                "For each line, contentStartIndex must be less than or equal to contentEndIndex "
            }
            action(lineNo, start, end)
            start = end
            lineNo++
        }
        val end = start
        if (end == contentEnd && ellipsis != null && ellipsisOnLineContent == false) {
            // create a new line where no content is included, but just used for the ellipsis
            action(lineNo, end, end)
        }
    }

    /**
     * ForEach loop that iterates through the range of the
     * current [contentStart] and [contentEnd] to iterate through.
     * If the [contentEnd] aligns with our [contentEnd], we also provide the ellipsis measurable
     * as the last for each return.
     */
    inline fun forEachIndexed(
        contentStart: Int,
        contentEnd: Int,
        action: (index: Int, measurable: Measurable, placeable: Placeable?) -> Unit
    ) {
        require(contentStart <= contentEnd && contentEnd <= measurables.size) {
            "contentStartIndex must be less than or equal to contentEndIndex " +
                "and contentEndIndex must be less than or equal to list size"
        }
        var i = contentStart
        while (i < contentEnd) {
            action(i, measurables[i], placeables[i])
            i++
        }
        val hasEllipsisOnContentLine =
            ellipsis != null &&
                contentEnd == this.contentEnd &&
                ellipsisOnLineContent == true
        val isEllipsisLineOnly =
            ellipsis != null &&
                contentStart == contentEnd &&
                contentEnd == this.contentEnd
        if (hasEllipsisOnContentLine || isEllipsisLineOnly) {
            ellipsis?.let { action(i, it, ellipsisPlaceable) }
        }
    }

    fun subSize(
        contentStart: Int,
        contentEnd: Int
    ): Int {
        require(contentStart <= contentEnd && contentEnd <= measurables.size) {
            "contentStartIndex must be less than or equal to contentEndIndex " +
                "and contentEndIndex must be less than or equal to list size"
        }
        val subSize = contentEnd - contentStart

        val hasEllipsisOnContentLine =
            ellipsis != null &&
                contentEnd == this.contentEnd &&
                ellipsisOnLineContent == true
        // lines with only ellipsis have content starts that
        // have reached the end of the list allowed.
        val isEllipsisLineOnly =
            ellipsis != null &&
                contentStart == contentEnd &&
                contentEnd == this.contentEnd

        return if ((hasEllipsisOnContentLine || isEllipsisLineOnly)) {
            subSize + 1
        } else {
            subSize
        }
    }

    /**
     * setPlaceable based on the index provided by [forEachIndexed]
     * If the index goes over the content end, the ellipsis placeable is set
     */
    fun setPlaceable(i: Int, placeable: Placeable) {
        if (i < contentEnd) { placeables[i] = placeable } else ellipsisPlaceable = placeable
    }
}
