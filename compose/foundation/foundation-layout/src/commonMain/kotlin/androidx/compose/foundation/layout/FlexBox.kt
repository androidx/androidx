/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.internal.JvmDefaultWithCompatibility
import androidx.compose.foundation.layout.internal.requirePrecondition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMaxBy
import androidx.compose.ui.util.fastSumBy
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.jvm.JvmInline
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * [FlexBox] provides a configurable layout system that is a superset of [Row], [Column], [FlowRow],
 * and [FlowColumn].
 *
 * ## Key Concepts
 *
 * ### Main Axis and Cross Axis
 * The **main axis** is the primary axis along which flex items are laid out, determined by
 * [FlexBoxConfigScope.direction]. The **cross axis** is perpendicular to it.
 * - [FlexDirection.Row]/[FlexDirection.RowReverse]: main axis is horizontal, cross axis is vertical
 * - [FlexDirection.Column]/[FlexDirection.ColumnReverse]: main axis is vertical, cross axis is
 *   horizontal
 *
 * ### Flexibility
 * Children can grow or shrink to fill available space using the [FlexBoxScope.flex] modifier:
 * - [FlexConfigScope.grow]: How much the item should grow relative to siblings
 * - [FlexConfigScope.shrink]: How much the item should shrink relative to siblings
 * - [FlexConfigScope.basis]: The initial size before flex distribution
 *
 * ### Alignment
 * FlexBox provides granular control over alignment:
 * - [FlexBoxConfigScope.justifyContent]: Distributes items along the main axis
 * - [FlexBoxConfigScope.alignItems]: Aligns items within a line along the cross axis
 * - [FlexBoxConfigScope.alignContent]: Aligns multiple lines along the cross axis (when wrapping)
 *
 * @sample androidx.compose.foundation.layout.samples.SimpleFlexBox
 * @param modifier The modifier to be applied to the FlexBox container.
 * @param config A [FlexBoxConfig] that configures the container's layout properties. Defaults to a
 *   horizontal row layout without wrapping, with items aligned to the start on both axes and no
 *   gaps between items.
 * @param content The content of the FlexBox, defined within a [FlexBoxScope].
 * @see FlexBoxConfig
 * @see FlexBoxScope
 * @see Row
 * @see Column
 * @see FlowRow
 * @see FlowColumn
 */
@Composable
@ExperimentalFlexBoxApi
inline fun FlexBox(
    modifier: Modifier = Modifier,
    config: FlexBoxConfig = FlexBoxConfig,
    content: @Composable FlexBoxScope.() -> Unit,
) {
    val currentConfig = rememberUpdatedState(config)
    Layout(
        modifier = modifier,
        content = { FlexBoxScopeInstance.content() },
        measurePolicy = flexMultiContentMeasurePolicy(flexBoxConfigState = currentConfig),
    )
}

/**
 * Creates a [MeasurePolicy] for FlexBox layout.
 *
 * This function is exposed to allow caching of measure policies for common configurations.
 */
@PublishedApi
@Composable
@ExperimentalFlexBoxApi
internal fun flexMultiContentMeasurePolicy(
    flexBoxConfigState: State<FlexBoxConfig>
): MeasurePolicy {
    return remember(flexBoxConfigState) {
        FlexBoxMeasurePolicy(flexBoxConfigState = flexBoxConfigState)
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
private class FlexBoxMeasurePolicy(private val flexBoxConfigState: State<FlexBoxConfig>) :
    MeasurePolicy {

    private val resolvedFlexBoxConfig = ResolvedFlexBoxConfig()

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        if (measurables.isEmpty()) {
            return layout(constraints.minWidth, constraints.minHeight) {}
        }
        val flexBoxConfig = resolveFlexBoxConfig(flexBoxConfigState.value, this, constraints)

        return measureFlexBox(
            flexBoxConfig,
            measurables,
            OrientationIndependentConstraints(
                constraints,
                if (flexBoxConfig.isHorizontal) {
                    LayoutOrientation.Horizontal
                } else {
                    LayoutOrientation.Vertical
                },
            ),
            flexBoxConfig.isHorizontal,
        )
    }

    // Main FlexBox layout algorithm.
    private fun MeasureScope.measureFlexBox(
        flexBoxConfig: ResolvedFlexBoxConfig,
        measurables: List<Measurable>,
        constraints: OrientationIndependentConstraints,
        isHorizontal: Boolean,
    ): MeasureResult {

        // Create all item states in single pass
        val items = ArrayList<ResolvedFlexItemInfo>(measurables.size)

        // Determine if we need upfront cross-axis calculation
        var needsUpfrontCrossAxisCalculation =
            flexBoxConfig.alignItems == FlexAlignItems.Stretch ||
                flexBoxConfig.alignItems == FlexAlignItems.Baseline ||
                flexBoxConfig.isWrapEnabled

        var needsSorting = false
        measurables.fastForEach { measurable ->
            val itemState = createFlexItem(measurable = measurable, isHorizontal, constraints)
            if (itemState.order != 0) needsSorting = true
            if (
                itemState.alignSelf == FlexAlignSelf.Baseline ||
                    itemState.alignSelf == FlexAlignSelf.Stretch
            ) {
                needsUpfrontCrossAxisCalculation = true
            }
            items.add(itemState)
        }

        if (needsSorting) {
            items.sortBy { it.order }
        }

        val mainAxisGap = flexBoxConfig.mainAxisGap()
        val crossAxisGap = flexBoxConfig.crossAxisGap()

        var totalLinesCrossSize = 0
        // Build lines
        val lines =
            buildFlexLines(
                flexBoxConfig = flexBoxConfig,
                items = items,
                constraints = constraints,
                mainAxisGap = mainAxisGap,
                crossAxisGap = crossAxisGap,
                needsUpfrontCrossAxisCalculation = needsUpfrontCrossAxisCalculation,
            ) {
                totalLinesCrossSize = it
            }

        // If we have single line and constraints are defined then the size of line is the
        // constraints instead of tallest item in the line.
        if (lines.size == 1) {
            val constrainedCrossSize = max(lines[0].crossAxisSize, constraints.crossAxisMin)
            lines[0].crossAxisSize = constrainedCrossSize
        }
        // handle `align-content: stretch`
        totalLinesCrossSize =
            applyAlignContentStretch(
                flexBoxConfig = flexBoxConfig,
                lines = lines,
                constraints = constraints,
                totalLinesCrossSize = totalLinesCrossSize,
                crossAxisGap = crossAxisGap,
            )

        // measure Items
        totalLinesCrossSize =
            measureFlexItems(
                lines = lines,
                items = items,
                flexBoxConfig = flexBoxConfig,
                totalLinesCrossSize = totalLinesCrossSize,
                needsUpfrontCrossAxisCalculation = needsUpfrontCrossAxisCalculation,
                constraints = constraints,
            )

        // calculate the cross position for each line.
        calculateLineCrossPositions(
            flexBoxConfig = flexBoxConfig,
            totalCrossAxisSpace = max(totalLinesCrossSize, constraints.crossAxisMin),
            lines = lines,
            totalLinesCrossSize = totalLinesCrossSize,
            crossAxisGap = crossAxisGap,
        )

        totalLinesCrossSize = lines.totalCrossAxisSize(isReverse = flexBoxConfig.isCrossAxisReverse)

        totalLinesCrossSize = max(totalLinesCrossSize, constraints.crossAxisMin)

        val maxLineMainSize =
            lines.fastMaxBy { it.mainAxisSize }?.mainAxisSize ?: constraints.mainAxisMin
        val mainAxisSize =
            max(maxLineMainSize, constraints.mainAxisMin)
                .fastCoerceIn(constraints.mainAxisMin, constraints.mainAxisMax)

        val layoutWidth: Int
        val layoutHeight: Int
        if (isHorizontal) {
            layoutWidth = mainAxisSize
            layoutHeight =
                totalLinesCrossSize.fastCoerceIn(constraints.crossAxisMin, constraints.crossAxisMax)
        } else {
            layoutWidth =
                totalLinesCrossSize.fastCoerceIn(constraints.crossAxisMin, constraints.crossAxisMax)
            layoutHeight = mainAxisSize
        }

        // final placement
        return layout(layoutWidth, layoutHeight) {
            placeFlexItems(
                lines = lines,
                items = items,
                layoutDirection = layoutDirection,
                flexBoxConfig = flexBoxConfig,
                layoutWidth = layoutWidth,
                layoutHeight = layoutHeight,
                mainAxisGap = mainAxisGap,
                isHorizontal = isHorizontal,
            )
        }
    }

    // Places all flex items in the layout.
    private fun Placeable.PlacementScope.placeFlexItems(
        lines: ArrayList<FlexLine>,
        items: ArrayList<ResolvedFlexItemInfo>,
        layoutDirection: LayoutDirection,
        flexBoxConfig: ResolvedFlexBoxConfig,
        layoutWidth: Int,
        layoutHeight: Int,
        mainAxisGap: Int,
        isHorizontal: Boolean,
    ) {
        lines.fastForEach { line ->
            // main-axis positions for this line
            positionItemsOnMainAxis(
                items = items,
                flexBoxConfig = flexBoxConfig,
                containerMainAxisSize = if (isHorizontal) layoutWidth else layoutHeight,
                line = line,
                mainAxisGap = mainAxisGap,
                isMainAxisReverse =
                    isMainAxisReversedForLayout(
                        flexBoxConfig = flexBoxConfig,
                        layoutDirection = layoutDirection,
                    ),
            )

            // Place each item
            items.fastForEachUntil(line.startIndex, line.endIndex) { item ->
                val x =
                    if (isHorizontal) {
                        item.mainPosition
                    } else {
                        item.crossPosition
                    }

                val y =
                    if (isHorizontal) {
                        item.crossPosition
                    } else {
                        item.mainPosition
                    }

                // Skip placing items that overflow the layout bounds.
                // Items are measured with clamped constraints to fit remaining space,
                // but if their position still exceeds bounds, they are not placed.
                if (x >= layoutWidth || y >= layoutHeight) return@fastForEachUntil

                item.placeable?.placeRelative(x = x, y = y)
            }
        }
    }

    private fun MeasureScope.createFlexItem(
        measurable: Measurable,
        isHorizontal: Boolean,
        constraints: OrientationIndependentConstraints,
    ): ResolvedFlexItemInfo {
        val node = measurable.parentData as? FlexBoxChildDataNode
        val resolvedItemInfo = ResolvedFlexItemInfo()
        if (node != null) {
            resolvedItemInfo.prepare(this, constraints)
            with(node.config) { resolvedItemInfo.configure() }
        }

        resolvedItemInfo.measurable = measurable
        // Calculate flex base size
        val minMainAxisSize = resolvedItemInfo.getMinMainAxisSize(isHorizontal)
        val maxContentSize = resolvedItemInfo.getMaxContentSize(isHorizontal)

        val flexBaseSize =
            when {
                resolvedItemInfo.basis.isDp -> resolvedItemInfo.basis.value.dp.roundToPx()
                resolvedItemInfo.basis.isPercent -> {
                    if (
                        constraints.mainAxisMax == Constraints.Infinity ||
                            resolvedItemInfo.basis.value.isNaN()
                    ) {
                        maxContentSize
                    } else {
                        (constraints.mainAxisMax * resolvedItemInfo.basis.value).toInt()
                    }
                }
                resolvedItemInfo.basis.isAuto -> maxContentSize
                else -> maxContentSize
            }

        resolvedItemInfo.flexBaseSize = flexBaseSize
        resolvedItemInfo.hypotheticalMainSize = flexBaseSize.fastCoerceAtLeast(minMainAxisSize)
        resolvedItemInfo.targetMainSize = resolvedItemInfo.hypotheticalMainSize

        return resolvedItemInfo
    }

    // Builds flex lines by distributing items according to wrap settings
    private inline fun buildFlexLines(
        flexBoxConfig: ResolvedFlexBoxConfig,
        items: ArrayList<ResolvedFlexItemInfo>,
        constraints: OrientationIndependentConstraints,
        mainAxisGap: Int,
        crossAxisGap: Int,
        needsUpfrontCrossAxisCalculation: Boolean,
        updateTotalCrossSize: (Int) -> Unit,
    ): ArrayList<FlexLine> {
        val lines = ArrayList<FlexLine>(8)

        var currentLine = FlexLine()
        var currentLineHypotheticalMainAxisSize = 0
        var currentCrossPosition = 0
        var totalLinesCrossSize = 0
        var lineStartIndex = 0

        var remainingCrossAxisSize = constraints.crossAxisMax

        items.fastForEachIndexed { index, item ->
            if (
                flexBoxConfig.isWrapEnabled &&
                    index > lineStartIndex &&
                    currentLineHypotheticalMainAxisSize + item.hypotheticalMainSize >
                        constraints.mainAxisMax
            ) {
                currentLine.startIndex = lineStartIndex
                currentLine.endIndex = index
                // it doesn't fit. process the completed line.
                processFlexLine(
                    line = currentLine,
                    items = items,
                    flexBoxConfig = flexBoxConfig,
                    // Subtract the trailing mainAxisGap here
                    currentLineHypotheticalMainAxisSize =
                        currentLineHypotheticalMainAxisSize - mainAxisGap,
                    needsUpfrontCrossAxisCalculation = needsUpfrontCrossAxisCalculation,
                    constraints = constraints,
                    remainingCrossAxisSize = remainingCrossAxisSize,
                )
                totalLinesCrossSize += currentLine.crossAxisSize
                currentLine.crossStart = currentCrossPosition
                currentCrossPosition += currentLine.crossAxisSize + crossAxisGap
                remainingCrossAxisSize =
                    (remainingCrossAxisSize - (currentLine.crossAxisSize + crossAxisGap))
                        .fastCoerceAtLeast(0)
                lines.add(currentLine)
                // start a new line with the current item
                currentLine = FlexLine()
                lineStartIndex = index
                currentLineHypotheticalMainAxisSize = item.hypotheticalMainSize + mainAxisGap
            } else {
                currentLineHypotheticalMainAxisSize += item.hypotheticalMainSize + mainAxisGap
            }
        }

        if (lineStartIndex < items.size) {
            currentLine.startIndex = lineStartIndex
            currentLine.endIndex = items.size
            processFlexLine(
                line = currentLine,
                items = items,
                flexBoxConfig = flexBoxConfig,
                // Subtract the trailing mainAxisGap here
                currentLineHypotheticalMainAxisSize =
                    currentLineHypotheticalMainAxisSize - mainAxisGap,
                needsUpfrontCrossAxisCalculation = needsUpfrontCrossAxisCalculation,
                constraints = constraints,
                remainingCrossAxisSize = remainingCrossAxisSize,
            )
            totalLinesCrossSize += currentLine.crossAxisSize
            currentLine.crossStart = currentCrossPosition
            lines.add(currentLine)
        }
        updateTotalCrossSize(totalLinesCrossSize)

        return lines
    }

    private fun processFlexLine(
        line: FlexLine,
        items: ArrayList<ResolvedFlexItemInfo>,
        flexBoxConfig: ResolvedFlexBoxConfig,
        currentLineHypotheticalMainAxisSize: Int,
        needsUpfrontCrossAxisCalculation: Boolean,
        constraints: OrientationIndependentConstraints,
        remainingCrossAxisSize: Int,
    ) {

        // resolve main-axis sizes (flex-grow/shrink).
        line.mainAxisSize =
            resolveFlexibleLengths(
                isHorizontal = flexBoxConfig.isHorizontal,
                items = items,
                flexBoxConfig = flexBoxConfig,
                startIndex = line.startIndex,
                endIndex = line.endIndex,
                hypotheticalLineSize = currentLineHypotheticalMainAxisSize,
                containerMainAxisSize = constraints.mainAxisMax,
            )

        // calculate the line's height.
        if (needsUpfrontCrossAxisCalculation) {
            calculateLineCrossAxisSize(
                flexBoxConfig = flexBoxConfig,
                line = line,
                items = items,
                constraints = constraints,
                remainingCrossAxisSize = remainingCrossAxisSize,
            )
        }
    }

    private fun resolveFlexibleLengths(
        isHorizontal: Boolean,
        items: ArrayList<ResolvedFlexItemInfo>,
        flexBoxConfig: ResolvedFlexBoxConfig,
        startIndex: Int,
        endIndex: Int,
        hypotheticalLineSize: Int,
        containerMainAxisSize: Int,
    ): Int {
        val itemCount = endIndex - startIndex
        val totalGap = if (itemCount > 0) (itemCount - 1) * flexBoxConfig.mainAxisGap() else 0

        if (containerMainAxisSize == Constraints.Infinity) {
            return items.fastSumBy(startIndex, endIndex) { it.targetMainSize } + totalGap
        }

        val initialFreeSpace = (containerMainAxisSize - hypotheticalLineSize).toDouble()
        val isGrowing = initialFreeSpace > 0

        var sumOfFrozenSizes = 0
        var sumOfBaseSizes = 0
        var sumOfGrowFactors = 0.0
        var sumOfScaledShrinkFactors = 0.0

        // initial freeze
        items.fastForEachUntil(startIndex, endIndex) { item ->
            val flexFactor = if (isGrowing) item.grow else item.shrink

            if (flexFactor == 0f || (!isGrowing && item.flexBaseSize < item.hypotheticalMainSize)) {
                item.isFrozen = true
                item.targetMainSize = item.hypotheticalMainSize
                sumOfFrozenSizes += item.targetMainSize
            } else {
                sumOfBaseSizes += item.flexBaseSize
                sumOfGrowFactors += item.grow.toDouble()
                sumOfScaledShrinkFactors += (item.shrink.toDouble() * item.flexBaseSize.toDouble())
            }
        }
        var lineMainAxisSize = sumOfFrozenSizes
        val remainingFreeSpace =
            (containerMainAxisSize - (sumOfFrozenSizes + sumOfBaseSizes)).toDouble()

        if (isGrowing) {
            if (sumOfGrowFactors > 0) {
                items.fastForEachUntil(startIndex, endIndex) { item ->
                    if (!item.isFrozen) {
                        val share = (item.grow / sumOfGrowFactors) * remainingFreeSpace
                        item.targetMainSize = item.flexBaseSize + share.toInt()
                        item.isFrozen = true
                        lineMainAxisSize += item.targetMainSize
                    }
                }
            }
        } else { // shrinking
            if (sumOfScaledShrinkFactors > 0) {
                items.fastForEachUntil(startIndex, endIndex) { item ->
                    if (!item.isFrozen) {
                        val scaledFactor = item.shrink * item.flexBaseSize
                        val share =
                            (scaledFactor / sumOfScaledShrinkFactors) * abs(remainingFreeSpace)
                        item.targetMainSize =
                            (item.flexBaseSize - share.toInt()).fastCoerceAtLeast(
                                item.getMinMainAxisSize(isHorizontal)
                            )
                        item.isFrozen = true
                        lineMainAxisSize += item.targetMainSize
                    }
                }
            }
        }

        return lineMainAxisSize + totalGap
    }

    // stretch to distribute extra cross-axis space to lines.
    private fun applyAlignContentStretch(
        flexBoxConfig: ResolvedFlexBoxConfig,
        lines: ArrayList<FlexLine>,
        constraints: OrientationIndependentConstraints,
        totalLinesCrossSize: Int,
        crossAxisGap: Int,
    ): Int {
        if (
            flexBoxConfig.alignContent != FlexAlignContent.Stretch ||
                constraints.crossAxisMin == Constraints.Infinity ||
                lines.isEmpty() ||
                lines.size == 1
        )
            return totalLinesCrossSize

        val totalSpacing = (lines.size - 1) * crossAxisGap
        val containerCrossAxisSize = constraints.crossAxisMin

        if ((totalLinesCrossSize + totalSpacing) >= containerCrossAxisSize)
            return totalLinesCrossSize
        var updatedTotalCrossAxisSize = totalLinesCrossSize

        val extraSpace =
            (containerCrossAxisSize - totalLinesCrossSize - totalSpacing).fastCoerceAtLeast(0)
        val spacePerLine = extraSpace / lines.size
        var currentY = 0
        lines.fastForEach { line ->
            line.crossStart = currentY
            line.crossAxisSize += spacePerLine
            currentY += line.crossAxisSize + crossAxisGap
            updatedTotalCrossAxisSize += spacePerLine
        }

        return updatedTotalCrossAxisSize
    }

    // Measures all flex items that haven't been measured yet.
    private fun measureFlexItems(
        lines: ArrayList<FlexLine>,
        items: ArrayList<ResolvedFlexItemInfo>,
        flexBoxConfig: ResolvedFlexBoxConfig,
        totalLinesCrossSize: Int,
        needsUpfrontCrossAxisCalculation: Boolean,
        constraints: OrientationIndependentConstraints,
    ): Int {
        var updatedTotalCrossSize = totalLinesCrossSize
        var remainingMainAxisSize: Int = constraints.mainAxisMax
        var remainingCrossAxisSize: Int = constraints.crossAxisMax

        lines.fastForEach { line ->
            var lineCrossAxisSize = if (needsUpfrontCrossAxisCalculation) line.crossAxisSize else 0
            items.fastForEachUntil(line.startIndex, line.endIndex) { item ->
                if (item.placeable != null) {
                    if (!needsUpfrontCrossAxisCalculation) {
                        lineCrossAxisSize = max(lineCrossAxisSize, item.crossAxisSize)
                    }
                    return@fastForEachUntil
                }

                val shouldStretch =
                    item.alignSelf == FlexAlignSelf.Stretch ||
                        (item.alignSelf == FlexAlignSelf.Auto &&
                            (flexBoxConfig.alignItems == FlexAlignItems.Stretch ||
                                flexBoxConfig.alignContent == FlexAlignContent.Stretch))

                val crossAxisSize =
                    measureItem(
                        item = item,
                        flexBoxConfig = flexBoxConfig,
                        lineCrossAxisSize = if (shouldStretch) line.crossAxisSize else 0,
                        shouldStretch = shouldStretch,
                        remainingMainAxisSize = remainingMainAxisSize,
                        remainingCrossAxisSize = remainingCrossAxisSize,
                    )
                if (!needsUpfrontCrossAxisCalculation) {
                    lineCrossAxisSize = max(lineCrossAxisSize, crossAxisSize)
                }

                // If we have single line and constraints are defined then the size of line is the
                // constraints instead of tallest item in the line.
                if (lines.size == 1) {
                    lineCrossAxisSize = max(lineCrossAxisSize, constraints.crossAxisMin)
                }
                remainingMainAxisSize =
                    (remainingMainAxisSize - (item.mainAxisSize + flexBoxConfig.mainAxisGap()))
                        .fastCoerceAtLeast(0)
            }
            if (!needsUpfrontCrossAxisCalculation) {
                line.crossAxisSize = lineCrossAxisSize
                updatedTotalCrossSize += lineCrossAxisSize
            }
            // reset main axis size for new line
            remainingMainAxisSize = constraints.mainAxisMax

            remainingCrossAxisSize =
                (remainingCrossAxisSize - line.crossAxisSize - flexBoxConfig.crossAxisGap())
                    .fastCoerceAtLeast(0)
        }
        return if (needsUpfrontCrossAxisCalculation) totalLinesCrossSize else updatedTotalCrossSize
    }

    // Calculates cross-axis positions for all lines based on align-content.
    private fun calculateLineCrossPositions(
        flexBoxConfig: ResolvedFlexBoxConfig,
        totalCrossAxisSpace: Int,
        lines: ArrayList<FlexLine>,
        totalLinesCrossSize: Int,
        crossAxisGap: Int,
    ) {
        if (lines.isEmpty() || lines.size == 1) return
        val totalGap = (lines.size - 1) * crossAxisGap
        val freeSpace = totalCrossAxisSpace.fastCoerceAtLeast(0) - totalLinesCrossSize - totalGap

        val spaceInBetweenLines =
            when (flexBoxConfig.alignContent) {
                FlexAlignContent.SpaceAround -> freeSpace / (lines.size)
                FlexAlignContent.SpaceBetween -> freeSpace / (lines.size - 1)
                else -> 0
            }

        var crossPosition =
            when (flexBoxConfig.alignContent) {
                FlexAlignContent.End -> if (flexBoxConfig.isCrossAxisReverse) 0 else freeSpace
                FlexAlignContent.Center -> freeSpace / 2
                FlexAlignContent.SpaceAround -> spaceInBetweenLines / 2
                else -> if (flexBoxConfig.isCrossAxisReverse) freeSpace else 0
            }

        val indices =
            if (flexBoxConfig.isCrossAxisReverse) lines.indices.reversed() else lines.indices
        for (index in indices) {
            val line = lines[index]
            line.crossStart = crossPosition
            crossPosition += line.crossAxisSize + spaceInBetweenLines + crossAxisGap
        }
    }

    private fun ArrayList<FlexLine>.totalCrossAxisSize(isReverse: Boolean): Int {
        if (isEmpty()) return 0
        val index = if (isReverse) 0 else lastIndex
        return this[index].crossStart + this[index].crossAxisSize
    }

    // Positions items within a line along the main axis based on justify-content.
    private fun positionItemsOnMainAxis(
        items: ArrayList<ResolvedFlexItemInfo>,
        flexBoxConfig: ResolvedFlexBoxConfig,
        containerMainAxisSize: Int,
        line: FlexLine,
        mainAxisGap: Int,
        isMainAxisReverse: Boolean,
    ) {
        val itemCount = line.endIndex - line.startIndex
        if (itemCount == 0) return

        val totalItemsGap = (itemCount - 1) * mainAxisGap
        val remainingSpace =
            (containerMainAxisSize - line.mainAxisSize - totalItemsGap).fastCoerceAtLeast(0)

        val spaceBetweenItems =
            when (flexBoxConfig.justifyContent) {
                FlexJustifyContent.SpaceAround -> remainingSpace / itemCount
                FlexJustifyContent.SpaceBetween ->
                    if (itemCount > 1) remainingSpace / (itemCount - 1) else 0
                FlexJustifyContent.SpaceEvenly -> (remainingSpace) / (itemCount + 1)
                else -> 0
            }

        var mainPosition =
            when (flexBoxConfig.justifyContent) {
                FlexJustifyContent.End -> if (isMainAxisReverse) 0 else remainingSpace
                FlexJustifyContent.Center -> (remainingSpace) / 2
                FlexJustifyContent.SpaceAround -> (spaceBetweenItems) / 2
                FlexJustifyContent.SpaceEvenly -> spaceBetweenItems
                else -> if (isMainAxisReverse) remainingSpace else 0
            }

        val indices =
            if (isMainAxisReverse) {
                (line.endIndex - 1) downTo line.startIndex
            } else {
                line.startIndex until line.endIndex
            }

        for (index in indices) {
            val item = items[index]
            item.mainPosition = mainPosition
            mainPosition += item.targetMainSize + spaceBetweenItems + mainAxisGap

            // cross-axis position based on alignSelf, falling back to alignItems
            val itemCrossAxisSize = item.crossAxisSize

            item.crossPosition =
                line.crossStart +
                    calculateItemCrossPosition(
                        flexConfig = item,
                        itemBaseline = item.baseline,
                        lineMaxAboveBaseline = line.maxAboveBaseline,
                        itemCrossAxisSize = itemCrossAxisSize,
                        lineCrossAxisSize = line.crossAxisSize,
                        containerAlignItems = flexBoxConfig.alignItems,
                    )
        }
    }

    private fun calculateItemCrossPosition(
        flexConfig: ResolvedFlexItemInfo,
        itemBaseline: Int,
        lineMaxAboveBaseline: Int,
        itemCrossAxisSize: Int,
        lineCrossAxisSize: Int,
        containerAlignItems: FlexAlignItems,
    ): Int {
        val effectiveAlignment =
            if (flexConfig.alignSelf != FlexAlignSelf.Auto) {
                flexConfig.alignSelf
            } else {
                // Convert AlignItems to equivalent AlignSelf
                when (containerAlignItems) {
                    FlexAlignItems.Start -> FlexAlignSelf.Start
                    FlexAlignItems.End -> FlexAlignSelf.End
                    FlexAlignItems.Center -> FlexAlignSelf.Center
                    FlexAlignItems.Stretch -> FlexAlignSelf.Stretch
                    FlexAlignItems.Baseline -> FlexAlignSelf.Baseline
                    else -> FlexAlignSelf.Start
                }
            }

        return when (effectiveAlignment) {
            FlexAlignSelf.Start -> 0
            FlexAlignSelf.End -> lineCrossAxisSize - itemCrossAxisSize
            FlexAlignSelf.Center -> (lineCrossAxisSize - itemCrossAxisSize) / 2
            FlexAlignSelf.Stretch -> 0
            FlexAlignSelf.Baseline ->
                if (itemBaseline != AlignmentLine.Unspecified) {
                    lineMaxAboveBaseline - itemBaseline
                } else {
                    0
                }
            else -> 0
        }
    }

    private fun isMainAxisReversedForLayout(
        flexBoxConfig: ResolvedFlexBoxConfig,
        layoutDirection: LayoutDirection,
    ): Boolean {
        val isMainAxisReverse =
            flexBoxConfig.direction == FlexDirection.RowReverse ||
                flexBoxConfig.direction == FlexDirection.ColumnReverse

        return when {
            !flexBoxConfig.isHorizontal -> isMainAxisReverse
            layoutDirection == LayoutDirection.Rtl -> !isMainAxisReverse // RTL flips row behavior
            else -> isMainAxisReverse
        }
    }

    // calculate cross axis size for line
    private fun calculateLineCrossAxisSize(
        items: ArrayList<ResolvedFlexItemInfo>,
        flexBoxConfig: ResolvedFlexBoxConfig,
        line: FlexLine,
        constraints: OrientationIndependentConstraints,
        remainingCrossAxisSize: Int,
    ) {

        var lineCrossAxisSize = 0
        var maxAboveBaseline = 0
        var maxBelowBaseline = 0
        val isHorizontal = flexBoxConfig.isHorizontal
        var remainingMainAxisSize: Int = constraints.mainAxisMax
        items.fastForEachUntil(line.startIndex, line.endIndex) { itemInfo ->
            val crossAxisSize =
                if (
                    itemInfo.hasBaseline ||
                        (flexBoxConfig.hasBaseline && itemInfo.alignSelf == FlexAlignSelf.Auto)
                ) {
                    measureItem(
                        itemInfo,
                        flexBoxConfig,
                        lineCrossAxisSize = 0,
                        shouldStretch = false,
                        remainingMainAxisSize = remainingMainAxisSize,
                        remainingCrossAxisSize = remainingCrossAxisSize,
                    )

                    val baseline =
                        itemInfo.getBaseline(itemInfo.placeable!!, fallback = flexBoxConfig)
                    itemInfo.baseline = baseline

                    maxAboveBaseline = max(maxAboveBaseline, baseline)
                    maxBelowBaseline = max(maxBelowBaseline, itemInfo.crossAxisSize - baseline)
                    remainingMainAxisSize =
                        (remainingMainAxisSize -
                                itemInfo.mainAxisSize -
                                flexBoxConfig.mainAxisGap())
                            .fastCoerceAtLeast(0)
                    // line cross Axis size
                    maxAboveBaseline + maxBelowBaseline
                } else {
                    itemInfo.crossAxisSize =
                        if (isHorizontal)
                            itemInfo.measurable?.maxIntrinsicHeight(width = itemInfo.targetMainSize)
                                ?: 0
                        else
                            itemInfo.measurable?.maxIntrinsicWidth(height = itemInfo.targetMainSize)
                                ?: 0
                    remainingMainAxisSize =
                        (remainingMainAxisSize -
                                itemInfo.targetMainSize -
                                flexBoxConfig.mainAxisGap())
                            .fastCoerceAtLeast(0)
                    itemInfo.crossAxisSize
                }
            lineCrossAxisSize = max(lineCrossAxisSize, crossAxisSize)
        }
        line.maxAboveBaseline = maxAboveBaseline
        line.crossAxisSize = lineCrossAxisSize
    }

    /**
     * Measures an item and updates its crossAxisSize. Returns the cross axis size contribution for
     * line height calculation.
     */
    private fun measureItem(
        item: ResolvedFlexItemInfo,
        flexBoxConfig: ResolvedFlexBoxConfig,
        lineCrossAxisSize: Int,
        shouldStretch: Boolean,
        remainingMainAxisSize: Int,
        remainingCrossAxisSize: Int,
    ): Int {
        val isHorizontal = flexBoxConfig.isHorizontal
        val clampedMainAxisSize =
            item.targetMainSize.fastCoerceAtMost(maximumValue = remainingMainAxisSize)
        val itemConstraints =
            if (shouldStretch && lineCrossAxisSize > 0) {
                Constraints.fixed(
                    width =
                        if (isHorizontal) clampedMainAxisSize
                        else
                            lineCrossAxisSize.fastCoerceAtMost(
                                maximumValue = remainingCrossAxisSize
                            ),
                    height =
                        if (isHorizontal)
                            lineCrossAxisSize.fastCoerceAtMost(
                                maximumValue = remainingCrossAxisSize
                            )
                        else clampedMainAxisSize,
                )
            } else {

                if (isHorizontal) {
                    Constraints.fitPrioritizingWidth(
                        minWidth = clampedMainAxisSize,
                        maxWidth = clampedMainAxisSize,
                        minHeight = 0,
                        maxHeight = remainingCrossAxisSize,
                    )
                } else {
                    Constraints.fitPrioritizingHeight(
                        minWidth = 0,
                        maxWidth = remainingCrossAxisSize,
                        minHeight = clampedMainAxisSize,
                        maxHeight = clampedMainAxisSize,
                    )
                }
            }

        item.placeable = item.measurable?.measure(itemConstraints)
        item.crossAxisSize =
            if (isHorizontal) {
                item.placeable?.height ?: 0
            } else {
                item.placeable?.width ?: 0
            }

        item.mainAxisSize =
            if (isHorizontal) {
                item.placeable?.width ?: 0
            } else {
                item.placeable?.height ?: 0
            }

        return item.crossAxisSize
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int {
        val resolvedFlexBoxConfig =
            resolveFlexBoxConfig(flexBoxConfigState.value, this, Constraints(maxHeight = height))

        return if (resolvedFlexBoxConfig.isHorizontal) {
            intrinsicMainAxisSize(resolvedFlexBoxConfig, measurables, height) { h ->
                minIntrinsicWidth(h)
            }
        } else {
            intrinsicCrossAxisSize(
                resolvedFlexBoxConfig,
                measurables,
                height,
                mainAxisSize = { h -> minIntrinsicHeight(h) },
            ) { w ->
                minIntrinsicWidth(w)
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        val resolvedFlexBoxConfig =
            resolveFlexBoxConfig(flexBoxConfigState.value, this, Constraints(maxWidth = width))

        return if (resolvedFlexBoxConfig.isHorizontal) {
            intrinsicCrossAxisSize(
                resolvedFlexBoxConfig,
                measurables,
                width,
                mainAxisSize = { w -> minIntrinsicWidth(w) },
            ) { h ->
                minIntrinsicHeight(h)
            }
        } else {
            intrinsicMainAxisSize(resolvedFlexBoxConfig, measurables, width) { w ->
                minIntrinsicHeight(w)
            }
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int {
        val resolvedFlexBoxConfig =
            resolveFlexBoxConfig(flexBoxConfigState.value, this, Constraints(maxHeight = height))

        return if (resolvedFlexBoxConfig.isHorizontal) {
            intrinsicMainAxisSize(resolvedFlexBoxConfig, measurables, height) { h ->
                maxIntrinsicWidth(h)
            }
        } else {
            intrinsicCrossAxisSize(
                resolvedFlexBoxConfig,
                measurables,
                height,
                mainAxisSize = { h -> maxIntrinsicHeight(h) },
            ) { w ->
                maxIntrinsicWidth(w)
            }
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        val resolvedFlexBoxConfig =
            resolveFlexBoxConfig(flexBoxConfigState.value, this, Constraints(maxWidth = width))

        return if (resolvedFlexBoxConfig.isHorizontal) {
            intrinsicCrossAxisSize(
                resolvedFlexBoxConfig,
                measurables,
                width,
                mainAxisSize = { w -> maxIntrinsicWidth(w) },
            ) { h ->
                maxIntrinsicHeight(h)
            }
        } else {
            intrinsicMainAxisSize(resolvedFlexBoxConfig, measurables, width) { w ->
                maxIntrinsicHeight(w)
            }
        }
    }

    /** Resolves and snapshots the container configuration. */
    private fun resolveFlexBoxConfig(
        flexBoxConfig: FlexBoxConfig,
        density: Density,
        constraints: Constraints,
    ): ResolvedFlexBoxConfig {
        resolvedFlexBoxConfig.prepare(density, constraints)
        with(flexBoxConfig) { resolvedFlexBoxConfig.configure() }
        return resolvedFlexBoxConfig
    }
}

private inline fun intrinsicMainAxisSize(
    flexBoxConfig: ResolvedFlexBoxConfig,
    measurables: List<IntrinsicMeasurable>,
    crossAxisAvailable: Int,
    mainAxisSize: IntrinsicMeasurable.(Int) -> Int,
): Int {
    if (measurables.isEmpty()) return 0

    val mainAxisGap = flexBoxConfig.mainAxisGap()

    return if (!flexBoxConfig.isWrapEnabled) {
        measurables.fastSumBy { it.mainAxisSize(crossAxisAvailable) } +
            (measurables.size - 1).coerceAtLeast(0) * mainAxisGap
    } else {
        var maxSize = 0
        measurables.fastForEach { maxSize = max(maxSize, it.mainAxisSize(crossAxisAvailable)) }
        maxSize
    }
}

private inline fun intrinsicCrossAxisSize(
    flexBoxConfig: ResolvedFlexBoxConfig,
    measurables: List<IntrinsicMeasurable>,
    mainAxisAvailable: Int,
    mainAxisSize: IntrinsicMeasurable.(Int) -> Int,
    crossAxisSize: IntrinsicMeasurable.(Int) -> Int,
): Int {
    if (measurables.isEmpty()) return 0

    val mainAxisGap = flexBoxConfig.mainAxisGap()
    val crossAxisGap = flexBoxConfig.crossAxisGap()

    var currentLineMainAxisSize = 0
    var currentLineCrossAxisSize = 0
    var totalCrossAxisSize = 0

    measurables.fastForEach { measurable ->
        val itemMainAxisSize = measurable.mainAxisSize(Constraints.Infinity)
        val itemCrossAxisSize = measurable.crossAxisSize(itemMainAxisSize)

        if (
            flexBoxConfig.isWrapEnabled &&
                currentLineMainAxisSize != 0 &&
                currentLineMainAxisSize + itemMainAxisSize > mainAxisAvailable
        ) {
            totalCrossAxisSize += currentLineCrossAxisSize + crossAxisGap
            currentLineMainAxisSize = itemMainAxisSize + mainAxisGap
            currentLineCrossAxisSize = itemCrossAxisSize
        } else {
            currentLineMainAxisSize += itemMainAxisSize + mainAxisGap
            currentLineCrossAxisSize = max(currentLineCrossAxisSize, itemCrossAxisSize)
        }
    }
    totalCrossAxisSize += currentLineCrossAxisSize
    return totalCrossAxisSize
}

/**
 * Receiver scope for the content of a [FlexBox]. Provides the [flex] modifier for configuring
 * individual flex item properties.
 */
@LayoutScopeMarker
@Immutable
@JvmDefaultWithCompatibility
@ExperimentalFlexBoxApi
interface FlexBoxScope {
    /**
     * Applies flex item properties using a [FlexConfig].
     *
     * @param flexConfig The flex configuration to apply.
     */
    @Stable fun Modifier.flex(flexConfig: FlexConfig): Modifier

    /**
     * Applies flex item properties using a configuration lambda.
     *
     * @param flexConfig A lambda that configures the flex properties.
     */
    @Stable
    fun Modifier.flex(flexConfig: FlexConfigScope.() -> Unit): Modifier =
        flex(FlexConfig(flexConfig))
}

@PublishedApi
@ExperimentalFlexBoxApi
internal object FlexBoxScopeInstance : FlexBoxScope {
    @Stable
    override fun Modifier.flex(flexConfig: FlexConfig): Modifier {
        return this.then(FlexBoxChildElement(flexConfig))
    }
}

/** ModifierNodeElement for flex item config. */
@OptIn(ExperimentalFlexBoxApi::class)
internal class FlexBoxChildElement(val config: FlexConfig) :
    ModifierNodeElement<FlexBoxChildDataNode>() {

    override fun create(): FlexBoxChildDataNode = FlexBoxChildDataNode(config)

    override fun update(node: FlexBoxChildDataNode) {
        node.config = config
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "flex"
        properties["config"] = config
    }

    override fun hashCode(): Int = config.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FlexBoxChildElement) return false
        return config == other.config
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
internal class FlexBoxChildDataNode(var config: FlexConfig) :
    ParentDataModifierNode, Modifier.Node() {

    override fun Density.modifyParentData(parentData: Any?): Any = this@FlexBoxChildDataNode
}

/**
 * Defines the direction of the main axis in a [FlexBox] container.
 *
 * The main axis determines the primary direction in which children are laid out. The cross axis is
 * always perpendicular to the main axis.
 *
 * @see FlexBoxConfigScope.direction
 */
@JvmInline
@ExperimentalFlexBoxApi
value class FlexDirection @PublishedApi internal constructor(private val bits: Int) {
    override fun toString() =
        when (bits) {
            0 -> "Row"
            1 -> "Column"
            2 -> "RowReverse"
            3 -> "ColumnReverse"
            else -> "INVALID"
        }

    companion object {
        /** Main axis is horizontal, items placed from start (left in LTR) to end. */
        inline val Row
            get() = FlexDirection(0)

        /** Main axis is vertical, items placed from top to bottom. */
        inline val Column
            get() = FlexDirection(1)

        /** Main axis is horizontal, items placed from end (right in LTR) to start. */
        inline val RowReverse
            get() = FlexDirection(2)

        /** Main axis is vertical, items placed from bottom to top. */
        inline val ColumnReverse
            get() = FlexDirection(3)
    }
}

/**
 * Defines whether flex items wrap onto multiple lines.
 *
 * @see FlexBoxConfigScope.wrap
 */
@JvmInline
@ExperimentalFlexBoxApi
value class FlexWrap @PublishedApi internal constructor(private val bits: Int) {
    override fun toString(): String =
        when (bits) {
            0 -> "NoWrap"
            1 -> "Wrap"
            2 -> "WrapReverse"
            else -> "INVALID"
        }

    companion object {

        /**
         * Items are laid out in a single line, which may overflow the container. Items will shrink
         * (if `shrink > 0`) to fit, but won't wrap.
         */
        inline val NoWrap
            get() = FlexWrap(0)

        /**
         * Items wrap onto multiple lines from top to bottom if the main axis is horizontal, or from
         * start to end if the main axis is vertical.
         */
        inline val Wrap
            get() = FlexWrap(1)

        /**
         * Items wrap onto multiple lines from bottom to top if the main axis is horizontal, or from
         * end to start if the main axis is vertical.
         */
        inline val WrapReverse
            get() = FlexWrap(2)
    }
}

/**
 * Defines the default cross-axis alignment for items within a [FlexBox]. Can be overridden per-item
 * using [FlexAlignSelf].
 *
 * @see FlexBoxConfigScope.alignItems
 * @see FlexAlignSelf
 */
@JvmInline
@ExperimentalFlexBoxApi
value class FlexAlignItems @PublishedApi internal constructor(private val bits: Int) {
    override fun toString(): String =
        when (bits) {
            0 -> "Start"
            1 -> "End"
            2 -> "Center"
            3 -> "Stretch"
            4 -> "Baseline"
            else -> "INVALID"
        }

    companion object {
        /** Items are aligned to the start of the cross axis. */
        inline val Start
            get() = FlexAlignItems(0)

        /** Items are aligned to the end of the cross axis. */
        inline val End
            get() = FlexAlignItems(1)

        /** Items are centered along the cross axis. */
        inline val Center
            get() = FlexAlignItems(2)

        /**
         * Items are stretched to fill the line's cross axis size. If the item has a fixed
         * cross-axis size, stretch has no effect.
         */
        inline val Stretch
            get() = FlexAlignItems(3)

        /**
         * Items are aligned based on their first baseline. Items without a baseline fall back to
         * [Start] alignment.
         */
        inline val Baseline
            get() = FlexAlignItems(4)
    }
}

/**
 * Defines the cross-axis alignment for a single flex item, overriding [FlexAlignItems].
 *
 * @see FlexConfigScope.alignSelf
 * @see FlexAlignItems
 */
@JvmInline
@ExperimentalFlexBoxApi
value class FlexAlignSelf @PublishedApi internal constructor(private val bits: Int) {
    override fun toString(): String =
        when (bits) {
            0 -> "Auto"
            1 -> "Start"
            2 -> "End"
            3 -> "Center"
            4 -> "Stretch"
            5 -> "Baseline"
            else -> "INVALID"
        }

    companion object {

        /** Inherits the alignment from the container's [FlexAlignItems]. */
        inline val Auto
            get() = FlexAlignSelf(0)

        /** The item is aligned to the start of the cross axis. */
        inline val Start
            get() = FlexAlignSelf(1)

        /** The item is aligned to the end of the cross axis. */
        inline val End
            get() = FlexAlignSelf(2)

        /** The item is centered along the cross axis. */
        inline val Center
            get() = FlexAlignSelf(3)

        /** The item is stretched to fill the line's cross axis size. */
        inline val Stretch
            get() = FlexAlignSelf(4)

        /** The item is aligned based on its first baseline. */
        inline val Baseline
            get() = FlexAlignSelf(5)
    }
}

/**
 * Defines how multiple lines are distributed along the cross axis. Only applies when there is more
 * than one line.
 *
 * @see FlexBoxConfigScope.alignContent
 */
@JvmInline
@ExperimentalFlexBoxApi
value class FlexAlignContent @PublishedApi internal constructor(private val bits: Int) {
    override fun toString(): String =
        when (bits) {
            0 -> "Start"
            1 -> "End"
            2 -> "Center"
            3 -> "Stretch"
            4 -> "SpaceBetween"
            5 -> "SpaceAround"
            else -> "INVALID"
        }

    companion object {
        /** Lines are aligned toward the start of the cross axis. */
        inline val Start
            get() = FlexAlignContent(0)

        /** Lines are aligned toward the end of the container. */
        inline val End
            get() = FlexAlignContent(1)

        /** Lines are centered along the cross axis. */
        inline val Center
            get() = FlexAlignContent(2)

        /** Lines are stretched to fill the available cross-axis space. */
        inline val Stretch
            get() = FlexAlignContent(3)

        /**
         * Lines are evenly distributed; first line at start, last at end. Equal space between
         * lines, no space at container edges.
         */
        inline val SpaceBetween
            get() = FlexAlignContent(4)

        /**
         * Lines are evenly distributed with equal space around each line. Space at edges is half
         * the space between lines.
         */
        inline val SpaceAround
            get() = FlexAlignContent(5)
    }
}

/**
 * Defines how items are distributed along the main axis.
 *
 * @see FlexBoxConfigScope.justifyContent
 */
@JvmInline
@ExperimentalFlexBoxApi
value class FlexJustifyContent @PublishedApi internal constructor(private val bits: Int) {
    override fun toString(): String =
        when (bits) {
            0 -> "Start"
            1 -> "End"
            2 -> "Center"
            3 -> "SpaceBetween"
            4 -> "SpaceAround"
            5 -> "SpaceEvenly"
            else -> "INVALID"
        }

    companion object {
        /** Items are aligned toward the start of the main axis. */
        inline val Start
            get() = FlexJustifyContent(0)

        /** Items are aligned toward the end of the main axis. */
        inline val End
            get() = FlexJustifyContent(1)

        /** Items are centered along the main axis. */
        inline val Center
            get() = FlexJustifyContent(2)

        /**
         * Items are evenly distributed; first item at start, last at end. Equal space between
         * items, no space at container edges.
         */
        inline val SpaceBetween
            get() = FlexJustifyContent(3)

        /**
         * Items are evenly distributed with equal space around each item. Space at edges is half
         * the space between items.
         */
        inline val SpaceAround
            get() = FlexJustifyContent(4)

        /** Items are evenly distributed with equal space between and at edges. */
        inline val SpaceEvenly
            get() = FlexJustifyContent(5)
    }
}

/**
 * Defines the initial main size of a flex item before free space distribution.
 *
 * @see FlexConfigScope.basis
 */
@JvmInline
@ExperimentalFlexBoxApi
value class FlexBasis
@PublishedApi
internal constructor(@PublishedApi internal val packedValue: Long) {
    companion object {
        private const val TypeShift = 32
        private const val TypeAuto = 0L
        private const val TypeDp = 1L
        private const val TypePercent = 2L

        val Auto = FlexBasis(TypeAuto shl TypeShift)

        fun Dp(value: Dp): FlexBasis {
            val valueBits = value.value.toBits().toLong() and 0xFFFFFFFFL
            return FlexBasis((TypeDp shl TypeShift) or valueBits)
        }

        fun Percent(@FloatRange(0.0, 1.0) value: Float): FlexBasis {
            val valueBits = value.toBits().toLong() and 0xFFFFFFFFL
            return FlexBasis((TypePercent shl TypeShift) or valueBits)
        }
    }

    internal val isAuto: Boolean
        get() = (packedValue ushr TypeShift) == TypeAuto

    internal val isDp: Boolean
        get() = (packedValue ushr TypeShift) == TypeDp

    internal val isPercent: Boolean
        get() = (packedValue ushr TypeShift) == TypePercent

    internal val value: Float
        get() = Float.fromBits(packedValue.toInt())

    override fun toString(): String =
        when {
            isAuto -> "FlexBasis.Auto"
            isDp -> "FlexBasis.Dp(${value}.dp)"
            isPercent -> "FlexBasis.Percent(${(value * 100).roundToInt()}%)"
            else -> "FlexBasis.Unknown"
        }
}

/**
 * Represents a configuration for a [FlexBox] container.
 *
 * FlexBoxConfig is implemented as a functional interface where the lambda is executed on
 * [FlexBoxConfigScope] during the layout phase. This means that reading state inside the lambda
 * will only trigger relayout, not recomposition.
 *
 * **Note**: Configuration properties are **not additive**. If a property is assigned multiple times
 * within the configuration block, the last assignment overrides previous values.
 *
 * ### Reusable Config
 * For better performance, define configs as top-level constants:
 * ```kotlin
 * private val RowWrap = FlexBoxConfig {
 *     direction = FlexDirection.Row
 *     wrap = FlexWrap.Wrap
 *     gap = 8.dp
 * }
 * ```
 *
 * @see FlexBoxConfigScope
 * @see FlexBox
 */
@Stable
@ExperimentalFlexBoxApi
fun interface FlexBoxConfig {
    /**
     * Configures the FlexBox container properties. Called during the layout phase, not during
     * composition.
     */
    fun FlexBoxConfigScope.configure()

    companion object : FlexBoxConfig {
        /** Default Config: Row direction, NoWrap, Start alignment. */
        override fun FlexBoxConfigScope.configure() {}
    }
}

/**
 * Scope for configuring [FlexBox] container properties.
 *
 * Properties are read during the layout/measure phase, not during composition. Changes to
 * state-backed properties trigger relayout, not recomposition.
 *
 * @see FlexBoxConfig
 */
@ExperimentalFlexBoxApi
sealed interface FlexBoxConfigScope : Density {

    /**
     * The layout constraints passed to this [FlexBox] from its parent.
     *
     * These constraints represent the minimum and maximum size limits that the parent has imposed
     * on this FlexBox. This can be useful for creating responsive layouts that adapt based on
     * available space.
     *
     * @see Constraints
     */
    val constraints: Constraints

    /** The direction of the main axis along which children are laid out. */
    var direction: FlexDirection

    /** Whether children should wrap to new lines when they exceed the main axis size. */
    var wrap: FlexWrap

    /** How children are distributed along the main axis. */
    var justifyContent: FlexJustifyContent

    /**
     * Default alignment for children along the cross axis within each line. Can be overridden
     * per-child using [FlexConfigScope.alignSelf].
     */
    var alignItems: FlexAlignItems

    /**
     * Aligns items to a specific baseline. Overrides [alignItems] when set.
     *
     * Example:
     * ```kotlin
     * FlexBox(
     *     flexBoxConfig = {
     *         alignItemsToBaseline(FirstBaseline)
     *         // or
     *         alignItemsToBaseline(LastBaseline)
     *         // or custom
     *         alignItemsToBaseline { measured ->
     *             measured[FirstBaseline] + 10
     *         }
     *     }
     * )
     * ```
     */
    fun alignItemsToBaseline(alignmentLine: AlignmentLine)

    /** Aligns items to a custom baseline. Overrides [alignItems] when set. */
    fun alignItemsToBaseline(alignmentLineBlock: (Measured) -> Int)

    /**
     * How multiple lines are aligned along the cross axis. Only applies when [wrap] is
     * [FlexWrap.Wrap] or [FlexWrap.WrapReverse] with multiple lines.
     */
    var alignContent: FlexAlignContent

    /** Vertical spacing between items (in Column) or lines (in Row with wrap). */
    var rowGap: Dp

    /** Horizontal spacing between columns of items. */
    var columnGap: Dp

    /**
     * Convenience function to set both [rowGap] and [columnGap] to the same value.
     *
     * @param value The gap size to apply to both row and column gaps.
     */
    fun gap(value: Dp)
}

@OptIn(ExperimentalFlexBoxApi::class)
internal class ResolvedFlexBoxConfig : FlexBoxConfigScope {

    private var _density: Density = DefaultDensity

    // Baseline alignment - null means use alignItems
    var baselineAlignmentLine: AlignmentLine? = null
        private set

    var baselineAlignmentBlock: ((Measured) -> Int)? = null
        private set

    override val density: Float
        get() = _density.density

    override val fontScale: Float
        get() = _density.fontScale

    override fun Dp.toSp(): TextUnit = with(_density) { this@toSp.toSp() }

    override fun TextUnit.toDp(): Dp = with(_density) { this@toDp.toDp() }

    override var constraints: Constraints = Constraints()
        private set

    override var direction: FlexDirection = FlexDirection.Row

    override var wrap: FlexWrap = FlexWrap.NoWrap

    override var justifyContent: FlexJustifyContent = FlexJustifyContent.Start

    override var alignItems: FlexAlignItems = FlexAlignItems.Start

    override var alignContent: FlexAlignContent = FlexAlignContent.Start

    override var rowGap: Dp = 0.dp

    override var columnGap: Dp = 0.dp

    override fun gap(value: Dp) {
        rowGap = value
        columnGap = value
    }

    override fun alignItemsToBaseline(alignmentLine: AlignmentLine) {
        alignItems = FlexAlignItems.Baseline
        baselineAlignmentLine = alignmentLine
        baselineAlignmentBlock = null
    }

    override fun alignItemsToBaseline(alignmentLineBlock: (Measured) -> Int) {
        alignItems = FlexAlignItems.Baseline
        baselineAlignmentLine = null
        baselineAlignmentBlock = alignmentLineBlock
    }

    fun getBaseline(placeable: Placeable): Int {
        return when {
            baselineAlignmentBlock != null -> baselineAlignmentBlock!!.invoke(placeable)
            baselineAlignmentLine != null -> {
                val value = placeable[baselineAlignmentLine!!]

                if (value != AlignmentLine.Unspecified) {
                    value
                } else {
                    if (baselineAlignmentLine!! is VerticalAlignmentLine) {
                        placeable.width
                    } else {
                        placeable.height
                    }
                }
            }
            else -> {
                val value = placeable[FirstBaseline]
                if (value != AlignmentLine.Unspecified) value else placeable.height
            }
        }
    }

    fun prepare(density: Density, constraints: Constraints) {
        this._density = density
        this.constraints = constraints
        // Reset to defaults
        direction = FlexDirection.Row
        wrap = FlexWrap.NoWrap
        justifyContent = FlexJustifyContent.Start
        alignItems = FlexAlignItems.Start
        alignContent = FlexAlignContent.Start
        rowGap = 0.dp
        columnGap = 0.dp
        baselineAlignmentLine = null
        baselineAlignmentBlock = null
    }

    inline val isHorizontal: Boolean
        get() = direction == FlexDirection.Row || direction == FlexDirection.RowReverse

    inline val isWrapEnabled: Boolean
        get() = wrap == FlexWrap.Wrap || wrap == FlexWrap.WrapReverse

    inline val isCrossAxisReverse: Boolean
        get() = wrap == FlexWrap.WrapReverse

    inline val hasBaseline: Boolean
        get() = alignItems == FlexAlignItems.Baseline

    /** Main axis gap (column gap for horizontal, row gap for vertical) */
    fun mainAxisGap(): Int = if (isHorizontal) columnGap.roundToPx() else rowGap.roundToPx()

    /** Cross axis gap (row gap for horizontal, column gap for vertical) */
    fun crossAxisGap(): Int = if (isHorizontal) rowGap.roundToPx() else columnGap.roundToPx()

    override fun toString(): String {
        return """
        FlexBoxConfig(
            direction = ${direction},
            wrap = ${wrap},
            justifyContent = ${justifyContent},
            alignItems = ${alignItems},
            alignContent = ${alignContent},
            rowGap = ${rowGap},
            columnGap = $columnGap
        )
    """
            .trimIndent()
    }
}

/**
 * FlexConfig represents a configuration for a flex item within a [FlexBox].
 *
 * FlexConfig is implemented as a functional interface where the lambda is executed on
 * [FlexConfigScope] during the layout phase.
 *
 * **Note**: Configuration properties are **not additive**. If a property is assigned multiple times
 * within the configuration block, the last assignment overrides previous values.
 *
 * Example:
 * ```
 * val growConfig = FlexConfig {
 *     grow = 1f
 *     alignSelf = AlignSelf.Center
 * }
 *
 * Box(modifier = Modifier.flex(growConfig))
 * ```
 *
 * @see FlexConfigScope
 * @see FlexBoxScope.flex
 */
@Stable
@ExperimentalFlexBoxApi
fun interface FlexConfig {

    /**
     * Configures the flex item properties. Called during the layout phase, not during composition.
     */
    fun FlexConfigScope.configure()
}

/**
 * Scope for configuring flex item properties. Properties are read during the layout/measure phase,
 * not during composition.
 *
 * @see FlexConfig
 */
@ExperimentalFlexBoxApi
sealed interface FlexConfigScope : Density {

    /**
     * The maximum size of the FlexBox container along the main axis.
     *
     * This corresponds to [Constraints.maxWidth] if the direction is [FlexDirection.Row] or
     * [FlexDirection.RowReverse], and [Constraints.maxHeight] if the direction is
     * [FlexDirection.Column] or [FlexDirection.ColumnReverse].
     */
    val flexBoxMainAxisMax: Int

    /**
     * The minimum size of the FlexBox container along the main axis.
     *
     * This corresponds to [Constraints.minWidth] if the direction is [FlexDirection.Row] or
     * [FlexDirection.RowReverse], and [Constraints.minHeight] if the direction is
     * [FlexDirection.Column] or [FlexDirection.ColumnReverse].
     */
    val flexBoxMainAxisMin: Int

    /**
     * The maximum size of the FlexBox container along the cross axis.
     *
     * This corresponds to [Constraints.maxHeight] if the direction is [FlexDirection.Row] or
     * [FlexDirection.RowReverse], and [Constraints.maxWidth] if the direction is
     * [FlexDirection.Column] or [FlexDirection.ColumnReverse].
     */
    val flexBoxCrossAxisMax: Int

    /**
     * The minimum size of the FlexBox container along the cross axis.
     *
     * This corresponds to [Constraints.minHeight] if the direction is [FlexDirection.Row] or
     * [FlexDirection.RowReverse], and [Constraints.minWidth] if the direction is
     * [FlexDirection.Column] or [FlexDirection.ColumnReverse].
     */
    val flexBoxCrossAxisMin: Int

    /** Overrides the container's [FlexBoxConfigScope.alignItems] for this specific item. */
    var alignSelf: FlexAlignSelf

    /** Aligns items to a specific baseline. Overrides [alignSelf] when set. */
    fun alignSelfToBaseline(alignmentLine: AlignmentLine)

    /** Aligns items to a custom baseline. Overrides [alignSelf] when set. */
    fun alignSelfToBaseline(alignmentLineBlock: (Measured) -> Int)

    /**
     * The visual order of this item relative to siblings. Items are sorted by [order] in ascending
     * order before layout, with lower values appearing first. Items with the same [order] maintain
     * their original declaration order.
     *
     * Default is `0`. Use negative values to move items before default-ordered items, or positive
     * values to move items after.
     */
    var order: Int

    /**
     * The flex grow factor.
     *
     * This value determines how much the item will grow relative to the rest of the flexible items
     * to absorb free space along the main axis.
     *
     * The value must be non-negative.
     *
     * **Note:** Items will grow even with explicit size constraints. Set `grow = 0f` to prevent
     * growth.
     */
    @get:FloatRange(from = 0.0) @setparam:FloatRange(from = 0.0) var grow: Float

    /**
     * The flex shrink factor.
     *
     * This value determines how much the item will shrink relative to the rest of the flexible
     * items when there is insufficient space along the main axis.
     *
     * The value must be non-negative.
     *
     * **Note:** Items will not shrink below their minimum intrinsic size. Items with explicit size
     * modifiers will not shrink at all.
     */
    @get:FloatRange(from = 0.0) @setparam:FloatRange(from = 0.0) var shrink: Float

    /** The initial main size of this item before flex distribution. */
    var basis: FlexBasis

    /** Sets the [basis] to a fixed Dp value. */
    fun basis(value: Dp)

    /**
     * Sets the [basis] to a percentage of the container's main axis size.
     *
     * @param value A value between 0.0 and 1.0 representing the percentage.
     */
    fun basis(@FloatRange(from = 0.0, to = 1.0) value: Float)
}

@OptIn(ExperimentalFlexBoxApi::class)
internal class ResolvedFlexItemInfo : FlexConfigScope {
    var baselineAlignmentLine: AlignmentLine? = null
        private set

    var baselineAlignmentBlock: ((Measured) -> Int)? = null
        private set

    private var _density: Density = DefaultDensity

    override val density: Float
        get() = _density.density

    override val fontScale: Float
        get() = _density.fontScale

    override fun Dp.toSp(): TextUnit = with(_density) { this@toSp.toSp() }

    override fun TextUnit.toDp(): Dp = with(_density) { this@toDp.toDp() }

    override var flexBoxMainAxisMax: Int = 0
        private set

    override var flexBoxMainAxisMin: Int = 0
        private set

    override var flexBoxCrossAxisMax: Int = 0
        private set

    override var flexBoxCrossAxisMin: Int = 0
        private set

    override var alignSelf: FlexAlignSelf = FlexAlignSelf.Auto

    override var order: Int = 0

    override var grow: Float = 0f
        set(value) {
            requirePrecondition(value >= 0f) { "Flex grow cannot be negative: $value" }
            field = value
        }

    override var shrink: Float = 1f
        set(value) {
            requirePrecondition(value >= 0f) { "Flex shrink cannot be negative: $value" }
            field = value
        }

    override var basis: FlexBasis = FlexBasis.Auto

    override fun basis(value: Dp) {
        basis = FlexBasis.Dp(value)
    }

    override fun basis(@FloatRange(from = 0.0, to = 1.0) value: Float) {
        basis = FlexBasis.Percent(value)
    }

    override fun alignSelfToBaseline(alignmentLine: AlignmentLine) {
        alignSelf = FlexAlignSelf.Baseline
        baselineAlignmentLine = alignmentLine
        baselineAlignmentBlock = null
    }

    override fun alignSelfToBaseline(alignmentLineBlock: (Measured) -> Int) {
        alignSelf = FlexAlignSelf.Baseline
        baselineAlignmentLine = null
        baselineAlignmentBlock = alignmentLineBlock
    }

    fun getBaseline(placeable: Placeable, fallback: ResolvedFlexBoxConfig): Int {
        return when {
            baselineAlignmentBlock != null -> baselineAlignmentBlock!!.invoke(placeable)
            baselineAlignmentLine != null -> {
                val value = placeable[baselineAlignmentLine!!]
                if (value != AlignmentLine.Unspecified) {
                    value
                } else {
                    if (baselineAlignmentLine!! is VerticalAlignmentLine) {
                        placeable.width
                    } else {
                        placeable.height
                    }
                }
            }
            alignSelf == FlexAlignSelf.Baseline -> fallback.getBaseline(placeable)
            fallback.hasBaseline && alignSelf == FlexAlignSelf.Auto ->
                fallback.getBaseline(placeable)
            else -> AlignmentLine.Unspecified
        }
    }

    val hasBaseline: Boolean
        get() = alignSelf == FlexAlignSelf.Baseline

    fun prepare(density: Density, constraints: OrientationIndependentConstraints) {
        this._density = density
        this.flexBoxMainAxisMax = constraints.mainAxisMax
        this.flexBoxMainAxisMin = constraints.mainAxisMin
        this.flexBoxCrossAxisMax = constraints.crossAxisMax
        this.flexBoxCrossAxisMin = constraints.crossAxisMin
    }

    // Measurable and measurement state
    var measurable: Measurable? = null
    private var _minMainAxisSize: Int = -1
    private var _maxContentSize: Int = -1

    // Returns the minimum main axis size (cached after first call).
    internal fun getMinMainAxisSize(isHorizontal: Boolean): Int {
        if (_minMainAxisSize == -1) {
            _minMainAxisSize =
                if (isHorizontal) {
                    measurable?.minIntrinsicWidth(Constraints.Infinity) ?: 0
                } else {
                    measurable?.minIntrinsicHeight(Constraints.Infinity) ?: 0
                }
        }
        return _minMainAxisSize
    }

    // Returns the max content size (cached after first call).
    internal fun getMaxContentSize(isHorizontal: Boolean): Int {
        if (_maxContentSize == -1) {
            _maxContentSize =
                if (isHorizontal) {
                    measurable?.maxIntrinsicWidth(Constraints.Infinity) ?: 0
                } else {
                    measurable?.maxIntrinsicHeight(Constraints.Infinity) ?: 0
                }
        }
        return _maxContentSize
    }

    // Flex algorithm state
    var flexBaseSize: Int = 0
    var hypotheticalMainSize: Int = 0
    var targetMainSize: Int = 0
    var isFrozen: Boolean = false

    // Position state
    var mainPosition: Int = 0
    var crossPosition: Int = 0

    // Measurement results
    var placeable: Placeable? = null
    var baseline: Int = AlignmentLine.Unspecified
    var mainAxisSize: Int = 0
    var crossAxisSize: Int = 0
}

internal val DefaultDensity =
    object : Density {
        override val density: Float = 1f
        override val fontScale: Float = 1f
    }

/** Holds state for a single flex line using indices */
private class FlexLine {
    var startIndex: Int = 0
    var endIndex: Int = 0
    var mainAxisSize: Int = 0
    var crossAxisSize: Int = 0
    var crossStart: Int = 0
    var maxAboveBaseline: Int = 0
}

/**
 * Iterates through a specific range of the [ArrayList] from [fromIndex] to [toIndex] (Exclusive)
 * and calls [action] for each item.
 *
 * This avoids creating a subList wrapper or an iterator.
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
private inline fun <T> ArrayList<T>.fastForEachUntil(
    fromIndex: Int,
    toIndex: Int,
    action: (T) -> Unit,
) {
    contract { callsInPlace(action) }
    if (fromIndex !in 0..size) {
        throw IndexOutOfBoundsException("fromIndex ($fromIndex) is out of bounds [0, $size]")
    }
    if (toIndex !in 0..size) {
        throw IndexOutOfBoundsException("toIndex ($toIndex) is out of bounds [0, $size]")
    }
    var index = fromIndex
    while (index < toIndex) {
        action(get(index))
        index++
    }
}

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
private inline fun <T> ArrayList<T>.fastSumBy(
    fromIndex: Int,
    toIndex: Int,
    selector: (T) -> Int,
): Int {
    contract { callsInPlace(selector) }
    var sum = 0
    fastForEachUntil(fromIndex, toIndex) { sum += selector(it) }
    return sum
}
