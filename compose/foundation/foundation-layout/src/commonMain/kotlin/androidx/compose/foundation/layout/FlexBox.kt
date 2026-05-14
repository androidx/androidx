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
import androidx.compose.ui.layout.LastBaseline
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
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.fastSumBy
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.jvm.JvmInline
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A layout that aligns its children in a single direction (the main axis) and allows them to wrap
 * onto multiple lines. [FlexBox] provides a highly configurable layout system, serving as a
 * flexible superset of [Row], [Column], [FlowRow], and [FlowColumn].
 *
 * The layout behavior of the container is controlled by the [config] parameter, which dictates the
 * flex direction, wrapping behavior, alignment, and spacing. Individual children can further
 * control their own flexibility (grow, shrink, and base size) and alignment using the
 * [FlexBoxScope.flex] modifier.
 *
 * Understanding FlexBox requires familiarity with its axes:
 * - **Main Axis**: The primary direction along which items are laid out, determined by the
 *   [FlexBoxConfigScope.direction]. Items are placed starting from the **`main-start`** edge and
 *   flowing toward the **`main-end`** edge. Defaults to [FlexDirection.Row].
 *         - For [FlexDirection.Row]: `main-start` is the layout's start edge (left in LTR, right in
 *           RTL) and `main-end` is the end edge (right in LTR, left in RTL).
 *         - For [FlexDirection.RowReverse]: `main-start` is the layout's end edge (right in LTR,
 *           left in
 *     * RTL) and `main-end` is the start edge (left in LTR, right in RTL).
 *         - For [FlexDirection.Column]: `main-start` is the top edge and `main-end` is the bottom edge.
 *         - For [FlexDirection.ColumnReverse]: `main-start` is the bottom edge and `main-end` is
 *           the top edge.
 * - **Cross Axis**: The axis perpendicular to the main axis. Wrapped lines are added, and items are
 *   aligned within their lines, starting from the **`cross-start`** edge and flowing toward the
 *   **`cross-end`** edge.
 *         - For horizontal directions ([FlexDirection.Row] and [FlexDirection.RowReverse]):
 *           `cross-start` is the top edge and `cross-end` is the bottom edge.
 *         - For vertical directions ([FlexDirection.Column] and [FlexDirection.ColumnReverse]):
 *           `cross-start` is the layout's start edge and `cross-end` is the end edge.
 *
 * Children can dictate how they share available space using the [FlexBoxScope.flex] modifier:
 * - [FlexConfigScope.grow]: Defines how much of the remaining positive free space the item should
 *   consume relative to its siblings. Defaults to 0f (no growth).
 * - [FlexConfigScope.shrink]: Defines how much the item should shrink when the combined sizes of
 *   the items exceed the container's main axis size. Defaults to 1f.
 * - [FlexConfigScope.basis]: Sets the initial main axis size of the item before any free space
 *   distribution (grow or shrink) is calculated. Defaults to [FlexBasis.Auto].
 *
 * [FlexBox] provides granular control over the placement of items and lines:
 * - [FlexBoxConfigScope.wrap]: Controls whether items are forced onto a single line or allowed to
 *   wrap onto multiple lines when they exceed the available space. Defaults to [FlexWrap.NoWrap].
 * - [FlexBoxConfigScope.justifyContent]: Distributes items along the main axis (for example,
 *   spacing them evenly). Defaults to [FlexJustifyContent.Start].
 * - [FlexBoxConfigScope.alignItems]: Aligns items within a specific line along the cross axis (for
 *   example, centering them vertically within a Row). Defaults to [FlexAlignItems.Start].
 * - [FlexConfigScope.alignSelf]: Allows an individual item to override the container's
 *   [FlexBoxConfigScope.alignItems]. Defaults to [FlexAlignSelf.Auto].
 * - [FlexBoxConfigScope.alignContent]:Distributes multiple wrapped lines along the cross axis. This
 *   only applies when wrapping is enabled. Defaults to [FlexAlignContent.Start].
 *
 * By default, children are placed in a horizontal row without wrapping. If wrapping is disabled
 * ([FlexWrap.NoWrap]), children will shrink to fit the container if they have a shrink factor > 0.
 * If children cannot shrink enough due to their minimum intrinsic sizes, they will visually
 * overflow the container's bounds along the main axis. You can explicitly apply
 * [Modifier.clipToBounds][androidx.compose.ui.draw.clipToBounds] on the FlexBox if you wish to hide
 * overflowing content.
 *
 * @sample androidx.compose.foundation.layout.samples.SimpleFlexBox
 * @param modifier The modifier to be applied to the FlexBox container.
 * @param config A [FlexBoxConfig] that configures the container's layout properties. Defaults to a
 *   horizontal row layout without wrapping, with items aligned to the start on both axes and no
 *   gaps between items.
 * @param content The content of the FlexBox, defined within a [FlexBoxScope].
 * @see FlexBoxConfig
 * @see FlexBoxScope
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
            flexBoxConfig.needUpfrontCrossAxisCalculation(constraints)

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

        val flexBaseSize =
            when {
                resolvedItemInfo.basis.isDp -> resolvedItemInfo.basis.value.dp.roundToPx()
                resolvedItemInfo.basis.isPercent -> {
                    if (
                        constraints.mainAxisMax == Constraints.Infinity ||
                            resolvedItemInfo.basis.value.isNaN()
                    ) {
                        resolvedItemInfo.getMaxContentSize(isHorizontal)
                    } else {
                        (constraints.mainAxisMax * resolvedItemInfo.basis.value).toInt()
                    }
                }
                resolvedItemInfo.basis.isAuto -> resolvedItemInfo.getMaxContentSize(isHorizontal)
                else -> resolvedItemInfo.getMaxContentSize(isHorizontal)
            }

        resolvedItemInfo.flexBaseSize = flexBaseSize
        resolvedItemInfo.hypotheticalMainSize = flexBaseSize.fastCoerceAtLeast(minMainAxisSize)
        resolvedItemInfo.targetMainSize = resolvedItemInfo.hypotheticalMainSize

        return resolvedItemInfo
    }

    private fun ResolvedFlexBoxConfig.needUpfrontCrossAxisCalculation(
        constraints: OrientationIndependentConstraints
    ) =
        (alignItems == FlexAlignItems.Stretch) ||
            (alignItems == FlexAlignItems.Baseline) ||
            (isWrapEnabled &&
                alignContent == FlexAlignContent.Stretch &&
                constraints.crossAxisMax != Constraints.Infinity)

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
            var lineMainAxisSize = totalGap
            items.fastForEachUntil(startIndex, endIndex) { item ->
                item.targetMainSize = item.hypotheticalMainSize
                lineMainAxisSize += item.targetMainSize
            }
            return lineMainAxisSize
        }

        val isGrowing = hypotheticalLineSize < containerMainAxisSize

        var unfrozenCount = 0
        var sumGrow = 0f
        var sumScaledShrink = 0f
        var sumFlexBaseSize = 0
        var sumFrozenTargetSize = 0
        var sumFactors = 0f

        // Initial Pass
        items.fastForEachUntil(startIndex, endIndex) { item ->
            val flexFactor = if (isGrowing) item.grow else item.shrink

            if (flexFactor == 0f || (!isGrowing && item.flexBaseSize < item.hypotheticalMainSize)) {
                item.targetMainSize = item.hypotheticalMainSize
                item.isFrozen = true
                sumFrozenTargetSize += item.targetMainSize
            } else {
                item.isFrozen = false
                unfrozenCount++
                sumFlexBaseSize += item.flexBaseSize
                sumFactors += flexFactor
                if (isGrowing) {
                    sumGrow += item.grow
                } else {
                    sumScaledShrink += (item.shrink * item.flexBaseSize)
                }
            }
        }

        val initialFreeSpace =
            (containerMainAxisSize - totalGap - sumFrozenTargetSize - sumFlexBaseSize).toFloat()

        var allocatedUnfrozenSize = 0

        if (isGrowing) {
            // For growth, there isn't any upper bound so we only need single pass
            val sumSizes = totalGap + sumFrozenTargetSize + sumFlexBaseSize
            var freeSpace = (containerMainAxisSize - sumSizes).toFloat()

            if (sumFactors < 1f) {
                val fractionalSpace = initialFreeSpace * sumFactors
                if (abs(fractionalSpace) < abs(freeSpace)) {
                    freeSpace = fractionalSpace
                }
            }

            var currentFreeSpace = freeSpace
            var currentSumGrow = sumGrow

            items.fastForEachUntil(startIndex, endIndex) { item ->
                if (!item.isFrozen) {
                    val share = if (currentSumGrow > 0f) item.grow / currentSumGrow else 0f
                    val spaceToAllocate = (currentFreeSpace * share).fastRoundToInt()

                    currentFreeSpace -= spaceToAllocate
                    currentSumGrow -= item.grow

                    item.targetMainSize = item.flexBaseSize + spaceToAllocate
                    allocatedUnfrozenSize += item.targetMainSize
                }
            }
        } else {
            var needsRedistribution = true
            var loopCount = 0

            while (needsRedistribution && loopCount < itemCount) {
                needsRedistribution = false
                loopCount++
                allocatedUnfrozenSize = 0

                if (unfrozenCount == 0) break

                val sumSizes = totalGap + sumFrozenTargetSize + sumFlexBaseSize
                var freeSpace = (containerMainAxisSize - sumSizes).toFloat()

                if (sumFactors < 1f) {
                    val fractionalSpace = initialFreeSpace * sumFactors
                    if (abs(fractionalSpace) < abs(freeSpace)) {
                        freeSpace = fractionalSpace
                    }
                }

                var currentFreeSpace = abs(freeSpace)
                var currentSumScaledShrink = sumScaledShrink

                items.fastForEachUntil(startIndex, endIndex) { item ->
                    if (!item.isFrozen) {
                        val scaledShrink = item.shrink * item.flexBaseSize
                        val share =
                            if (currentSumScaledShrink > 0f) scaledShrink / currentSumScaledShrink
                            else 0f
                        val spaceToTake = (currentFreeSpace * share).fastRoundToInt()

                        val targetSize = item.flexBaseSize - spaceToTake
                        val minSize = item.getMinMainAxisSize(isHorizontal)

                        if (targetSize < minSize) {
                            item.isFrozen = true
                            needsRedistribution = true

                            val actualSpaceTaken = item.flexBaseSize - minSize
                            currentFreeSpace -= actualSpaceTaken
                            currentSumScaledShrink -= scaledShrink

                            item.targetMainSize = minSize
                            unfrozenCount--
                            sumFrozenTargetSize += minSize
                            sumFlexBaseSize -= item.flexBaseSize
                            sumScaledShrink -= scaledShrink
                            sumFactors -= item.shrink
                        } else {
                            currentFreeSpace -= spaceToTake
                            currentSumScaledShrink -= scaledShrink

                            item.targetMainSize = targetSize
                            allocatedUnfrozenSize += item.targetMainSize
                        }
                    }
                }
            }
        }

        return totalGap + sumFrozenTargetSize + allocatedUnfrozenSize
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
            }
            if (!needsUpfrontCrossAxisCalculation) {
                line.crossAxisSize = lineCrossAxisSize
                updatedTotalCrossSize += lineCrossAxisSize
            }

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
        val freeSpace = totalCrossAxisSpace - totalLinesCrossSize - totalGap

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

        val remainingSpace = containerMainAxisSize - line.mainAxisSize

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
        remainingCrossAxisSize: Int,
    ) {

        var lineCrossAxisSize = 0
        var maxAboveBaseline = 0
        var maxBelowBaseline = 0
        val isHorizontal = flexBoxConfig.isHorizontal
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
                        remainingCrossAxisSize = remainingCrossAxisSize,
                    )

                    val baseline =
                        itemInfo.getBaseline(itemInfo.placeable!!, fallback = flexBoxConfig)
                    itemInfo.baseline = baseline

                    maxAboveBaseline = max(maxAboveBaseline, baseline)
                    maxBelowBaseline = max(maxBelowBaseline, itemInfo.crossAxisSize - baseline)
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
        remainingCrossAxisSize: Int,
    ): Int {
        val isHorizontal = flexBoxConfig.isHorizontal

        val itemConstraints =
            if (shouldStretch && lineCrossAxisSize > 0) {
                Constraints.fixed(
                    width =
                        if (isHorizontal) item.targetMainSize
                        else
                            lineCrossAxisSize.fastCoerceAtMost(
                                maximumValue = remainingCrossAxisSize
                            ),
                    height =
                        if (isHorizontal)
                            lineCrossAxisSize.fastCoerceAtMost(
                                maximumValue = remainingCrossAxisSize
                            )
                        else item.targetMainSize,
                )
            } else {

                if (isHorizontal) {
                    Constraints.fitPrioritizingWidth(
                        minWidth = item.targetMainSize,
                        maxWidth = item.targetMainSize,
                        minHeight = 0,
                        maxHeight = remainingCrossAxisSize,
                    )
                } else {
                    Constraints.fitPrioritizingHeight(
                        minWidth = 0,
                        maxWidth = remainingCrossAxisSize,
                        minHeight = item.targetMainSize,
                        maxHeight = item.targetMainSize,
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
        if (measurables.isEmpty()) return 0
        val config =
            resolveFlexBoxConfig(flexBoxConfigState.value, this, Constraints(maxHeight = height))
        return if (config.isHorizontal) {
            // Main axis. Min = narrowest without clipping.
            // Wrap: widest child (each child could be its own line).
            // NoWrap: sum of all children (they must all fit on one line).
            val gap = config.mainAxisGap()
            if (config.isWrapEnabled) {
                var maxSize = 0
                measurables.fastForEach { maxSize = max(maxSize, it.minIntrinsicWidth(height)) }
                maxSize
            } else {
                measurables.fastSumBy { it.minIntrinsicWidth(height) } +
                    (measurables.size - 1).coerceAtLeast(0) * gap
            }
        } else {
            // Cross axis. Simulate line breaks along the vertical main axis,
            // then take the widest line.
            intrinsicCrossAxisSize(
                config = config,
                measurables = measurables,
                mainAxisAvailable = height,
                mainAxisSize = { it.minIntrinsicHeight(Constraints.Infinity) },
                crossAxisSize = { measurable, mainSize -> measurable.minIntrinsicWidth(mainSize) },
            )
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        if (measurables.isEmpty()) return 0
        val config =
            resolveFlexBoxConfig(flexBoxConfigState.value, this, Constraints(maxWidth = width))
        return if (config.isHorizontal) {
            // Cross axis. Simulate line breaks along the horizontal main axis,
            // then sum the tallest-child-per-line heights + cross gaps.
            intrinsicCrossAxisSize(
                config = config,
                measurables = measurables,
                mainAxisAvailable = width,
                mainAxisSize = { it.minIntrinsicWidth(Constraints.Infinity) },
                crossAxisSize = { measurable, mainSize -> measurable.minIntrinsicHeight(mainSize) },
            )
        } else {
            // Main axis.
            val gap = config.mainAxisGap()
            if (config.isWrapEnabled) {
                var maxSize = 0
                measurables.fastForEach { maxSize = max(maxSize, it.minIntrinsicHeight(width)) }
                maxSize
            } else {
                measurables.fastSumBy { it.minIntrinsicHeight(width) } +
                    (measurables.size - 1).coerceAtLeast(0) * gap
            }
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int {
        if (measurables.isEmpty()) return 0
        val config =
            resolveFlexBoxConfig(flexBoxConfigState.value, this, Constraints(maxHeight = height))
        return if (config.isHorizontal) {
            // Main axis. Max = preferred unwrapped size, regardless of wrap setting.
            val gap = config.mainAxisGap()
            measurables.fastSumBy { it.maxIntrinsicWidth(height) } +
                (measurables.size - 1).coerceAtLeast(0) * gap
        } else {
            // Cross axis.
            intrinsicCrossAxisSize(
                config = config,
                measurables = measurables,
                mainAxisAvailable = height,
                mainAxisSize = { it.maxIntrinsicHeight(Constraints.Infinity) },
                crossAxisSize = { measurable, mainSize -> measurable.maxIntrinsicWidth(mainSize) },
            )
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        if (measurables.isEmpty()) return 0
        val config =
            resolveFlexBoxConfig(flexBoxConfigState.value, this, Constraints(maxWidth = width))
        return if (config.isHorizontal) {
            // Cross axis.
            intrinsicCrossAxisSize(
                config = config,
                measurables = measurables,
                mainAxisAvailable = width,
                mainAxisSize = { it.maxIntrinsicWidth(Constraints.Infinity) },
                crossAxisSize = { measurable, mainSize -> measurable.maxIntrinsicHeight(mainSize) },
            )
        } else {
            // Main axis. Max = preferred unwrapped size.
            val gap = config.mainAxisGap()
            measurables.fastSumBy { it.maxIntrinsicHeight(width) } +
                (measurables.size - 1).coerceAtLeast(0) * gap
        }
    }

    /**
     * Simulates line-breaking along the main axis and sums up per-line cross-axis sizes to compute
     * the total cross-axis intrinsic size.
     *
     * This is an approximation: child cross sizes are queried at their unconstrained main-axis size
     * rather than their post-flex-resolution size, since the full flex algorithm cannot run during
     * intrinsic measurement.
     */
    private inline fun intrinsicCrossAxisSize(
        config: ResolvedFlexBoxConfig,
        measurables: List<IntrinsicMeasurable>,
        mainAxisAvailable: Int,
        mainAxisSize: (IntrinsicMeasurable) -> Int,
        crossAxisSize: (IntrinsicMeasurable, Int) -> Int,
    ): Int {
        val mainAxisGap = config.mainAxisGap()
        val crossAxisGap = config.crossAxisGap()

        var currentLineMainAxisSize = 0
        var currentLineCrossAxisSize = 0
        var totalCrossAxisSize = 0
        var isFirstItemInLine = true

        measurables.fastForEach { measurable ->
            val itemMainAxisSize = mainAxisSize(measurable)
            val itemCrossAxisSize = crossAxisSize(measurable, itemMainAxisSize)

            // Would adding this item (plus the gap before it) overflow the line?
            val projectedLineSize =
                if (isFirstItemInLine) {
                    itemMainAxisSize
                } else {
                    currentLineMainAxisSize + mainAxisGap + itemMainAxisSize
                }

            if (
                config.isWrapEnabled && !isFirstItemInLine && projectedLineSize > mainAxisAvailable
            ) {
                // Finalize current line and start a new one.
                totalCrossAxisSize += currentLineCrossAxisSize + crossAxisGap
                currentLineMainAxisSize = itemMainAxisSize
                currentLineCrossAxisSize = itemCrossAxisSize
                // remains false for the next iteration, which will be the second item of this new
                // line
            } else {
                currentLineMainAxisSize = projectedLineSize
                currentLineCrossAxisSize = max(currentLineCrossAxisSize, itemCrossAxisSize)
                isFirstItemInLine = false
            }
        }

        // Add the last line (no trailing crossAxisGap).
        totalCrossAxisSize += currentLineCrossAxisSize
        return totalCrossAxisSize
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

/**
 * Scope for the content of a [FlexBox]. Provides the [flex] modifier for configuring individual
 * flex item properties.
 *
 * @sample androidx.compose.foundation.layout.samples.FlexBoxScopeSample
 * @see FlexBox
 * @see FlexConfig
 */
@LayoutScopeMarker
@Immutable
@JvmDefaultWithCompatibility
@ExperimentalFlexBoxApi
interface FlexBoxScope {
    /**
     * Configures the flex properties of this element within the [FlexBox] using the provided
     * [FlexConfig].
     *
     * @sample androidx.compose.foundation.layout.samples.FlexModifierWithConfigSample
     * @param flexConfig The flex configuration to apply.
     * @see FlexConfig
     */
    @Stable fun Modifier.flex(flexConfig: FlexConfig): Modifier

    /**
     * Configures the flex properties of this element within the [FlexBox] using a configuration
     * lambda.
     *
     * This modifier allows you to specify how an individual item should share available space
     * (grow, shrink, basis) and how it aligns itself along the cross axis (alignSelf).
     *
     * @sample androidx.compose.foundation.layout.samples.FlexModifierWithLambdaSample
     * @param flexConfig A lambda that configures the flex properties within a [FlexConfigScope].
     * @see FlexConfigScope
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
 * The main axis determines the primary direction in which children are laid out. It establishes the
 * `main-start` and `main-end` edges of the container. The cross axis is always perpendicular to the
 * main axis.
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
        /**
         * The main axis is horizontal. Items are placed starting from the `main-start` edge and
         * flowing toward the `main-end` edge.
         *
         * In a Left-To-Right (LTR) layout direction, `main-start` corresponds to the start (left)
         * edge of the container. In a Right-To-Left (RTL) layout direction, `main-start`
         * corresponds to the end (right).
         */
        inline val Row
            get() = FlexDirection(0)

        /**
         * The main axis is vertical. Items are placed starting from the `main-start` edge (the top
         * of the container) and flowing toward the `main-end` edge (the bottom).
         */
        inline val Column
            get() = FlexDirection(1)

        /**
         * The main axis is horizontal, but the placement direction is reversed. The `main-start`
         * and `main-end` edges are swapped.
         *
         * In a Left-To-Right (LTR) layout direction, `main-start` becomes the right edge of the
         * container, and items flow leftward. In a Right-To-Left (RTL) layout direction,
         * `main-start` becomes the left edge.
         */
        inline val RowReverse
            get() = FlexDirection(2)

        /**
         * The main axis is vertical, but the placement direction is reversed. The `main-start` edge
         * becomes the bottom of the container, and items flow toward the `main-end` edge at the
         * top.
         */
        inline val ColumnReverse
            get() = FlexDirection(3)
    }
}

/**
 * Defines whether flex items are forced onto a single line or can wrap onto multiple lines.
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
         * Items are laid out in a single line. Items will shrink to fit the container if their
         * [FlexConfigScope.shrink] factor allows it. If they cannot shrink enough to fit the main
         * axis (for example, due to their minimum intrinsic sizes), they will visually overflow on
         * main axis of the container.
         */
        inline val NoWrap
            get() = FlexWrap(0)

        /**
         * Items wrap onto multiple lines if they exceed the main axis size. New lines are added
         * along the cross axis, starting from the `cross-start` edge and flowing toward the
         * `cross-end` edge. (For example, top-to-bottom in a [FlexDirection.Row]).
         */
        inline val Wrap
            get() = FlexWrap(1)

        /**
         * Items wrap onto multiple lines if they exceed the main axis size. New lines are added in
         * the reverse direction along the cross axis, starting from the `cross-end` edge and
         * flowing toward the `cross-start` edge. (For example, bottom-to-top in a
         * [FlexDirection.Row]).
         */
        inline val WrapReverse
            get() = FlexWrap(2)
    }
}

/**
 * Defines the default alignment for items along the cross axis within their respective lines. This
 * controls how items are positioned perpendicular to the main axis. This can be overridden for an
 * individual item using [FlexConfigScope.alignSelf].
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
        /** Items are aligned toward the cross-start edge of their line. */
        inline val Start
            get() = FlexAlignItems(0)

        /** Items are aligned toward the cross-end edge of their line. */
        inline val End
            get() = FlexAlignItems(1)

        /** Items are centered along the cross axis within their line. */
        inline val Center
            get() = FlexAlignItems(2)

        /** Items are stretched to fill the cross axis size of their line. */
        inline val Stretch
            get() = FlexAlignItems(3)

        /**
         * Items are aligned such that their baselines match along the cross axis. Items without a
         * baseline fall back to [Start] alignment.
         */
        inline val Baseline
            get() = FlexAlignItems(4)
    }
}

/**
 * Defines the cross-axis alignment for a single flex item, overriding the container's
 * [FlexAlignItems].
 *
 * This controls how an individual item is positioned perpendicular to the main axis within its
 * respective line.
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

        /**
         * Inherits the alignment from the container's [FlexBoxConfigScope.alignItems]. This is the
         * default value.
         */
        inline val Auto
            get() = FlexAlignSelf(0)

        /** The item is aligned toward the `cross-start` edge of its line. */
        inline val Start
            get() = FlexAlignSelf(1)

        /** The item is aligned toward the `cross-end` edge of its line. */
        inline val End
            get() = FlexAlignSelf(2)

        /** The item is centered along the cross axis within its line. */
        inline val Center
            get() = FlexAlignSelf(3)

        /** The item is stretched to fill the cross axis size of its line. */
        inline val Stretch
            get() = FlexAlignSelf(4)

        /**
         * The item is aligned such that its baseline matches the baseline of other baseline-aligned
         * items in the line. Items without a baseline fall back to [Start] alignment.
         */
        inline val Baseline
            get() = FlexAlignSelf(5)
    }
}

/**
 * Defines how multiple lines are distributed along the cross axis. This only applies when wrapping
 * is enabled ([FlexWrap.Wrap] or [FlexWrap.WrapReverse]), the container has extra cross-axis space,
 * and there is more than one line of items.
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
        /**
         * Place lines such that they are as close as possible to the `cross-start` edge of the
         * container.
         */
        inline val Start
            get() = FlexAlignContent(0)

        /**
         * Place lines such that they are as close as possible to the `cross-end` edge of the
         * container.
         */
        inline val End
            get() = FlexAlignContent(1)

        /**
         * Place lines such that they are as close as possible to the middle of the container's
         * cross axis.
         */
        inline val Center
            get() = FlexAlignContent(2)

        /**
         * Distribute remaining free space evenly among all lines, increasing their cross-axis size
         * to fill the available space.
         */
        inline val Stretch
            get() = FlexAlignContent(3)

        /**
         * Place lines such that they are spaced evenly across the cross axis, without free space
         * before the first line or after the last line.
         */
        inline val SpaceBetween
            get() = FlexAlignContent(4)

        /**
         * Place lines such that they are spaced evenly across the cross axis, including free space
         * before the first line and after the last line, but half the amount of space existing
         * otherwise between two consecutive lines.
         */
        inline val SpaceAround
            get() = FlexAlignContent(5)
    }
}

/**
 * Defines the arrangement of items along the main axis of their respective lines. This controls how
 * free space is distributed between and around items after their main axis sizes have been
 * resolved.
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
        /**
         * Place items such that they are as close as possible to the `main-start` edge of their
         * line.
         */
        inline val Start
            get() = FlexJustifyContent(0)

        /**
         * Place items such that they are as close as possible to the `main-end` edge of their line.
         */
        inline val End
            get() = FlexJustifyContent(1)

        /**
         * Place items such that they are as close as possible to the middle of the main axis within
         * their line.
         */
        inline val Center
            get() = FlexJustifyContent(2)

        /**
         * Place items such that they are spaced evenly across the main axis, without free space
         * before the first item or after the last item.
         */
        inline val SpaceBetween
            get() = FlexJustifyContent(3)

        /**
         * Place items such that they are spaced evenly across the main axis, including free space
         * before the first item and after the last item, but half the amount of space existing
         * otherwise between two consecutive items.
         */
        inline val SpaceAround
            get() = FlexJustifyContent(4)

        /**
         * Place items such that they are spaced evenly across the main axis, including free space
         * before the first item and after the last item.
         */
        inline val SpaceEvenly
            get() = FlexJustifyContent(5)
    }
}

/**
 * Defines the initial main size of a flex item before free space distribution.
 * - [Auto]: Uses the item's explicitly set size, or falls back to its natural content size.
 * - [Dp]: Uses a fixed exact size in [androidx.compose.ui.unit.Dp].
 * - [Percent]: Uses a fraction of the container's main axis size.
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

        /**
         * Use the item's maximum intrinsic size as the basis.
         *
         * If the item has an explicitly set size modifier along the main axis (for example,
         * `Modifier.width` in a [FlexDirection.Row]), that exact size will be used as the basis.
         * Otherwise, it falls back to measuring the item's preferred natural content size without
         * constraints.
         *
         * This is the default value.
         */
        val Auto = FlexBasis(TypeAuto shl TypeShift)

        /**
         * Use a fixed size in [androidx.compose.ui.unit.Dp] as the basis.
         *
         * @sample androidx.compose.foundation.layout.samples.FlexBasisDpSample
         * @param value The basis size in Dp.
         */
        fun Dp(value: Dp): FlexBasis {
            val valueBits = value.value.toBits().toLong() and 0xFFFFFFFFL
            return FlexBasis((TypeDp shl TypeShift) or valueBits)
        }

        /**
         * Use a fraction of the container's main axis size as the basis.
         *
         * @sample androidx.compose.foundation.layout.samples.FlexBasisPercentSample
         * @param value A value between 0.0 and 1.0 representing the percentage.
         */
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
 * This configuration is defined via a lambda that operates on a [FlexBoxConfigScope]. Because this
 * configuration block is executed during the layout phase rather than the composition phase,
 * reading state variables inside the block will only trigger a layout pass, completely avoiding
 * costly recompositions.
 *
 * Configuration properties are applied sequentially. If a property is configured multiple times
 * within the block, the final call takes precedence.
 *
 * **Reusability and Responsiveness**
 *
 * Configurations can be extracted, saved, and reused across multiple [FlexBox] containers:
 *
 * @sample androidx.compose.foundation.layout.samples.FlexBoxConfigReusableSample
 *
 * Furthermore, because the [FlexBoxConfigScope] provides direct access to the incoming
 * [Constraints][androidx.compose.ui.unit.Constraints], you can easily create responsive
 * configurations that dynamically adapt their direction, wrapping, or gaps based on the available
 * screen space:
 *
 * @sample androidx.compose.foundation.layout.samples.FlexBoxConfigResponsiveSample
 * @see FlexBoxConfigScope
 * @see FlexBox
 */
@Stable
@ExperimentalFlexBoxApi
fun interface FlexBoxConfig {
    /**
     * Applies the configuration to the given [FlexBoxConfigScope]. This method is invoked by the
     * layout system during the measurement phase, not during composition.
     */
    fun FlexBoxConfigScope.configure()

    /**
     * Merges this config with another. Configs further "to the right" will override properties to
     * the left of them, on a per-property basis.
     *
     * @sample androidx.compose.foundation.layout.samples.FlexBoxConfigCombineSample
     * @param other the config to merge into the receiver.
     */
    infix fun then(other: FlexBoxConfig): FlexBoxConfig =
        when {
            (other === Companion) -> this
            other is CombinedFlexBoxConfig -> CombinedFlexBoxConfig(this, *other.configs)
            else -> CombinedFlexBoxConfig(this, other)
        }

    companion object : FlexBoxConfig {

        /**
         * A default configuration that lays out items in a horizontal row without wrapping, with
         *
         * items aligned to the start on both axes and no gaps.
         */
        override fun FlexBoxConfigScope.configure() {}

        /** Identity elision: merging the identity with any config yields that config. */
        override fun then(other: FlexBoxConfig): FlexBoxConfig = other
    }
}

/**
 * Receiver scope for configuring [FlexBox] container properties.
 *
 * This scope is provided by [FlexBoxConfig]. All configuration functions are called during the
 * layout/measure phase, not during composition. Changes to state-backed values read within this
 * scope will trigger a relayout, entirely skipping recomposition.
 *
 * @see FlexBoxConfig
 */
@ExperimentalFlexBoxApi
sealed interface FlexBoxConfigScope : Density {

    /**
     * The layout constraints passed to this [FlexBox] from its parent.
     *
     * Use this for creating responsive layouts that dynamically adapt their properties (like
     * direction, wrapping, or gaps) based on the available incoming space.
     *
     * @sample androidx.compose.foundation.layout.samples.FlexBoxConstraintsSample
     * @see Constraints
     */
    val constraints: Constraints

    /**
     * Sets the direction of the main axis along which children are laid out.
     *
     * The main axis determines the primary direction of item placement:
     * - [FlexDirection.Row]: Items placed horizontally, `main-start` to `main-end` (end to start in
     *   RTL).
     * - [FlexDirection.RowReverse]: Items placed horizontally, end to start (start to end in RTL).
     * - [FlexDirection.Column]: Items placed vertically, top to bottom.
     * - [FlexDirection.ColumnReverse]: Items placed vertically, bottom to top.
     *
     * @sample androidx.compose.foundation.layout.samples.FlexBoxDirectionSample
     * @param value The flex direction. Default is [FlexDirection.Row].
     * @see FlexDirection
     */
    fun direction(value: FlexDirection)

    /**
     * Sets whether children are forced onto a single line or can wrap onto multiple lines.
     * - [FlexWrap.NoWrap]: All items stay on one line. Items may visually overflow on main axis if
     *   they cannot shrink enough.
     * - [FlexWrap.Wrap]: Items wrap to new lines toward the `cross-end` edge.
     * - [FlexWrap.WrapReverse]: Items wrap to new lines toward the `cross-start` edge.
     *
     * @sample androidx.compose.foundation.layout.samples.FlexBoxWrapSample
     * @param value The wrap behavior. Default is [FlexWrap.NoWrap].
     * @see FlexWrap
     */
    fun wrap(value: FlexWrap)

    /**
     * Sets how children are distributed along the main axis.
     *
     * This controls the spacing and positioning of items within each line after their main axis
     * sizes have been resolved.
     * - [FlexJustifyContent.Start]: Items packed toward the `main-start` edge.
     * - [FlexJustifyContent.End]: Items packed toward the `main-end` edge.
     * - [FlexJustifyContent.Center]: Items centered along the main axis.
     * - [FlexJustifyContent.SpaceBetween]: Items evenly distributed; first at start, last at end.
     * - [FlexJustifyContent.SpaceAround]: Items evenly distributed with half-size space at edges.
     * - [FlexJustifyContent.SpaceEvenly]: Items evenly distributed with equal space everywhere.
     *
     * @sample androidx.compose.foundation.layout.samples.FlexBoxJustifyContentSample
     * @param value The justify content value. Default is [FlexJustifyContent.Start].
     * @see FlexJustifyContent
     */
    fun justifyContent(value: FlexJustifyContent)

    /**
     * Sets the default alignment for children along the cross axis within each line.
     *
     * This controls how items are positioned perpendicular to the main axis. Individual items can
     * override this default alignment using [FlexConfigScope.alignSelf].
     * - [FlexAlignItems.Start]: Items aligned to the `cross-start` edge within the line.
     * - [FlexAlignItems.End]: Items aligned to the `cross-end` edge within the line.
     * - [FlexAlignItems.Center]: Items centered along the cross axis within the line.
     * - [FlexAlignItems.Stretch]: Items stretched to fill the line's cross-axis size.
     * - [FlexAlignItems.Baseline]: Items aligned by their baseline within the line.
     *
     * @sample androidx.compose.foundation.layout.samples.FlexBoxAlignItemsSample
     * @param value The align items value. Default is [FlexAlignItems.Start].
     * @see FlexAlignItems
     * @see FlexConfigScope.alignSelf
     */
    fun alignItems(value: FlexAlignItems)

    /**
     * Aligns all items to a specific baseline.
     *
     * This is equivalent to calling `alignItems(FlexAlignItems.Baseline)` but allows specifying
     * exactly which alignment line to use (e.g., [FirstBaseline] or [LastBaseline]).
     *
     * @sample androidx.compose.foundation.layout.samples.FlexBoxAlignItemsBaselineSample
     * @param alignmentLine The alignment line to use.
     * @see AlignmentLine
     */
    fun alignItems(alignmentLine: AlignmentLine)

    /**
     * Aligns all items to a custom baseline computed from each measured item.
     *
     * Use this when you need custom baseline calculation logic. This functions similarly to
     * [RowScope.alignBy] and [ColumnScope.alignBy].
     *
     * @sample androidx.compose.foundation.layout.samples.FlexBoxAlignItemsCustomBaselineSample
     * @param alignmentLineBlock A function that computes the baseline position from a [Measured]
     *   item.
     * @see Measured
     */
    fun alignItems(alignmentLineBlock: (Measured) -> Int)

    /**
     * Sets how multiple lines are distributed along the cross axis.
     *
     * This only applies when [wrap] is [FlexWrap.Wrap] or [FlexWrap.WrapReverse] and there are
     * multiple lines of items.
     * - [FlexAlignContent.Start]: Lines packed toward the `cross-start` edge.
     * - [FlexAlignContent.End]: Lines packed toward the `cross-end` edge.
     * - [FlexAlignContent.Center]: Lines centered along the cross axis.
     * - [FlexAlignContent.Stretch]: Lines stretched to fill available cross-axis space.
     * - [FlexAlignContent.SpaceBetween]: Lines evenly distributed; first at start, last at end.
     * - [FlexAlignContent.SpaceAround]: Lines evenly distributed with half-size space at edges.
     *
     * @sample androidx.compose.foundation.layout.samples.FlexBoxAlignContentSample
     * @param value The align content value. Default is [FlexAlignContent.Start].
     * @see FlexAlignContent
     * @see wrap
     */
    fun alignContent(value: FlexAlignContent)

    /**
     * Sets the vertical spacing between items or lines.
     *
     * Regardless of the flex [direction], this always applies spacing along the vertical axis
     * (Y-axis). In a horizontal layout with wrapping, this represents the space between wrapped
     * lines. In a vertical layout, this represents the space between the items themselves.
     *
     * @sample androidx.compose.foundation.layout.samples.FlexBoxRowGapSample
     * @param value The vertical gap size. Default is `0.dp`.
     * @see columnGap
     * @see gap
     */
    fun rowGap(value: Dp)

    /**
     * Sets the horizontal spacing between items or columns.
     *
     * Regardless of the flex [direction], this always applies spacing along the horizontal axis
     * (X-axis). In a horizontal layout, this represents the space between the items themselves. In
     * a vertical layout with wrapping, this represents the space between wrapped columns.
     *
     * @sample androidx.compose.foundation.layout.samples.FlexBoxColumnGapSample
     * @param value The horizontal gap size. Default is `0.dp`.
     * @see rowGap
     * @see gap
     */
    fun columnGap(value: Dp)

    /**
     * Sets both [rowGap] and [columnGap] to the same value.
     *
     * This is a convenience function for uniform spacing across both axes.
     *
     * @sample androidx.compose.foundation.layout.samples.FlexBoxGapSample
     * @param all The gap size to apply to both row and column gaps.
     * @see rowGap
     * @see columnGap
     */
    fun gap(all: Dp)

    /**
     * Sets [rowGap] and [columnGap] to different values.
     *
     * @sample androidx.compose.foundation.layout.samples.FlexBoxGapDifferentSample
     * @param row The vertical spacing (Y-axis).
     * @param column The horizontal spacing (X-axis).
     * @see rowGap
     * @see columnGap
     */
    fun gap(row: Dp, column: Dp)
}

@OptIn(ExperimentalFlexBoxApi::class)
internal class ResolvedFlexBoxConfig : FlexBoxConfigScope {

    private var _density: Density = DefaultDensity

    // Baseline alignment - null means use alignItems
    var baselineAlignmentLine: AlignmentLine? = null
        private set

    var baselineAlignmentBlock: AlignmentLineProviderBlock? = null
        private set

    override val density: Float
        get() = _density.density

    override val fontScale: Float
        get() = _density.fontScale

    override fun Dp.toSp(): TextUnit = with(_density) { this@toSp.toSp() }

    override fun TextUnit.toDp(): Dp = with(_density) { this@toDp.toDp() }

    override var constraints: Constraints = Constraints()
        private set

    internal var direction: FlexDirection = FlexDirection.Row

    internal var wrap: FlexWrap = FlexWrap.NoWrap

    internal var justifyContent: FlexJustifyContent = FlexJustifyContent.Start

    internal var alignItems: FlexAlignItems = FlexAlignItems.Start

    internal var alignContent: FlexAlignContent = FlexAlignContent.Start

    internal var rowGap: Dp = 0.dp

    internal var columnGap: Dp = 0.dp

    override fun direction(value: FlexDirection) {
        this.direction = value
    }

    override fun wrap(value: FlexWrap) {
        this.wrap = value
    }

    override fun justifyContent(value: FlexJustifyContent) {
        this.justifyContent = value
    }

    override fun alignItems(value: FlexAlignItems) {
        this.alignItems = value
    }

    override fun gap(all: Dp) {
        rowGap = all
        columnGap = all
    }

    override fun alignItems(alignmentLine: AlignmentLine) {
        alignItems = FlexAlignItems.Baseline
        baselineAlignmentLine = alignmentLine
        baselineAlignmentBlock = null
    }

    override fun alignItems(alignmentLineBlock: (Measured) -> Int) {
        alignItems = FlexAlignItems.Baseline
        baselineAlignmentLine = null
        baselineAlignmentBlock = AlignmentLineProviderBlock { alignmentLineBlock(it) }
    }

    override fun alignContent(value: FlexAlignContent) {
        this.alignContent = value
    }

    override fun rowGap(value: Dp) {
        this.rowGap = value
    }

    override fun columnGap(value: Dp) {
        this.columnGap = value
    }

    override fun gap(row: Dp, column: Dp) {
        this.rowGap = row
        this.columnGap = column
    }

    internal fun getBaseline(placeable: Placeable): Int {
        return when {
            baselineAlignmentBlock != null ->
                baselineAlignmentBlock!!.calculateAlignmentLinePosition(placeable)
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
 * Represents a configuration for a flex item within a [FlexBox].
 *
 * This configuration is defined via a lambda that operates on a [FlexConfigScope]. Because this
 * configuration block is executed during the layout phase rather than the composition phase,
 * reading state variables inside the block will only trigger a layout pass, completely avoiding
 * costly recompositions.
 *
 * Configuration properties are applied sequentially. If a property (such as
 * [grow][FlexConfigScope.grow] or [shrink][FlexConfigScope.shrink]) is assigned multiple times
 * within the configuration block, the final call takes precedence.
 *
 * @sample androidx.compose.foundation.layout.samples.FlexConfigSample
 * @see FlexConfigScope
 * @see FlexBoxScope.flex
 */
@Stable
@ExperimentalFlexBoxApi
fun interface FlexConfig {

    /**
     * Applies the configuration to the given [FlexConfigScope].This method is invoked by the layout
     * system during the measurement phase, not during composition.
     */
    fun FlexConfigScope.configure()

    /**
     * Merges this config with another. Configs further "to the right" will override properties to
     * the left of them, on a per-property basis.
     *
     * @sample androidx.compose.foundation.layout.samples.FlexConfigCombineSample
     * @param other the config to merge into the receiver.
     */
    infix fun then(other: FlexConfig): FlexConfig =
        when {
            (other === Companion) -> this
            other is CombinedFlexConfig -> CombinedFlexConfig(this, *other.configs)
            else -> CombinedFlexConfig(this, other)
        }

    companion object : FlexConfig {
        override fun FlexConfigScope.configure() {}

        /** Merging the identity with any config yields that config. */
        override fun then(other: FlexConfig): FlexConfig = other
    }
}

/**
 * Scope for configuring flex item properties within a [FlexBox].
 *
 * All configuration functions are called during the layout/measure phase, not during composition.
 *
 * @sample androidx.compose.foundation.layout.samples.FlexConfigScopeSample
 * @see FlexConfig
 */
@ExperimentalFlexBoxApi
sealed interface FlexConfigScope : Density {

    /**
     * The maximum size of the FlexBox container along the main axis. Corresponds to
     * [Constraints.maxWidth] for [FlexDirection.Row]/[FlexDirection.RowReverse], or
     * [Constraints.maxHeight] for [FlexDirection.Column]/[FlexDirection.ColumnReverse]. Use this
     * for responsive item sizing based on the container's available space.
     */
    val flexBoxMainAxisMax: Int

    /**
     * The minimum size of the FlexBox container along the main axis. Corresponds to
     * [Constraints.minWidth] for [FlexDirection.Row]/[FlexDirection.RowReverse], or
     * [Constraints.minHeight] for [FlexDirection.Column]/[FlexDirection.ColumnReverse].
     */
    val flexBoxMainAxisMin: Int

    /**
     * The maximum size of the FlexBox container along the cross axis. Corresponds to
     * [Constraints.maxHeight] for [FlexDirection.Row]/[FlexDirection.RowReverse], or
     * [Constraints.maxWidth] for [FlexDirection.Column]/[FlexDirection.ColumnReverse].
     */
    val flexBoxCrossAxisMax: Int

    /**
     * The minimum size of the FlexBox container along the cross axis. Corresponds to
     * [Constraints.minHeight] for [FlexDirection.Row]/[FlexDirection.RowReverse], or
     * [Constraints.minWidth] for [FlexDirection.Column]/[FlexDirection.ColumnReverse].
     */
    val flexBoxCrossAxisMin: Int

    /**
     * Overrides the container's [FlexBoxConfigScope.alignItems] for this specific item.
     *
     * This controls how the individual item is positioned perpendicular to the main axis within its
     * respective line.
     * - [FlexAlignSelf.Auto]: Inherits the container's alignment (default).
     * - [FlexAlignSelf.Start]: Aligns to the `cross-start` edge of its line.
     * - [FlexAlignSelf.End]: Aligns to the `cross-end` edge of its line.
     * - [FlexAlignSelf.Center]: Centers along the cross axis within its line.
     * - [FlexAlignSelf.Stretch]: Stretches to fill the line's cross-axis size.
     * - [FlexAlignSelf.Baseline]: Aligns by baseline within its line.
     *
     * @sample androidx.compose.foundation.layout.samples.FlexAlignSelfSample
     * @param value The alignment for this item. Default is [FlexAlignSelf.Auto].
     * @see FlexAlignSelf
     * @see FlexBoxConfigScope.alignItems
     */
    fun alignSelf(value: FlexAlignSelf)

    /**
     * Aligns this item to a specific baseline within its line, overriding the container's
     * alignment.
     *
     * @sample androidx.compose.foundation.layout.samples.FlexAlignSelfBaselineSample
     * @param alignmentLine The alignment line to use (e.g., [FirstBaseline], [LastBaseline]).
     * @see AlignmentLine
     */
    fun alignSelf(alignmentLine: AlignmentLine)

    /**
     * Aligns this item to a custom baseline computed from the measured item within its line,
     * overriding the container's alignment.
     *
     * @sample androidx.compose.foundation.layout.samples.FlexAlignSelfCustomBaselineSample
     * @param alignmentLineBlock A function that computes the baseline from a [Measured] item.
     */
    fun alignSelf(alignmentLineBlock: (Measured) -> Int)

    /**
     * ◦ Sets the visual order of this item relative to its siblings.
     *
     * Items are sorted by their order value in ascending order before layout. Lower values are
     * placed first, starting from the main-start edge of the container. Note that in reverse
     * directions (like [FlexDirection.RowReverse]), the main-start edge is visually flipped (e.g.,
     * to the right side of the container).
     *
     * The sorting is stable; items with the same order maintain the exact sequence in which they
     * were emitted in the composition. By default, all items have an order of 0. You can use
     * negative values to move items before default-ordered items, or positive values to move them
     * after.
     *
     * @sample androidx.compose.foundation.layout.samples.FlexOrderSample
     * @param value The order value. Default is 0.
     */
    fun order(value: Int)

    /**
     * Sets the flex grow factor, determining how much of the remaining positive free space this
     * item should consume relative to its siblings.
     *
     * When the sum of all item base sizes is less than the container's main axis size, the leftover
     * space is distributed among items proportional to their growth factors. An item with a grow
     * factor of 0f (the default) will not grow beyond its base size.
     *
     * @sample androidx.compose.foundation.layout.samples.FlexGrowSample
     * @param value The growth factor. Must be non-negative. Default is 0f.
     * @throws IllegalArgumentException if [value] is negative.
     * @see shrink
     * @see basis
     */
    fun grow(@FloatRange(from = 0.0) value: Float)

    /**
     * ◦ Sets the flex shrink factor, determining how much this item should shrink relative to its
     * siblings when there is not enough space.
     *
     * When the sum of all item base sizes exceeds the container's main axis size, items will shrink
     * proportionally based on their shrink factor multiplied by their base size. An item with a
     * shrink factor of 0f will not shrink.
     *
     * **Note:** Items will never shrink below their minimum intrinsic size. If the total minimum
     * size of all items exceeds the container's size, the items will overflow visually on main
     * axis. Use [Modifier.clipToBounds][androidx.compose.ui.draw.clipToBounds] on the container if
     * you need to hide the overflow.
     *
     * @sample androidx.compose.foundation.layout.samples.FlexShrinkSample
     * @param value The shrink factor. Must be non-negative. Default is 1f.
     * @throws IllegalArgumentException if [value] is negative.
     * @see grow
     * @see basis
     */
    fun shrink(@FloatRange(from = 0.0) value: Float)

    /**
     * Sets the initial main axis size of this item before any free space distribution (grow or
     * shrink) is calculated.
     *
     * The basis determines the starting size before [grow] and [shrink] are applied:
     * - [FlexBasis.Auto]: Uses the item's explicitly set size, or falls back to its natural content
     *   size.
     * - [FlexBasis.Dp]: Uses a fixed exact size in dp.
     * - [FlexBasis.Percent]: Uses a fraction of the container's main axis size.
     *
     * @sample androidx.compose.foundation.layout.samples.FlexBasisSample
     * @param value The basis value. Default is [FlexBasis.Auto].
     * @see FlexBasis
     */
    fun basis(value: FlexBasis)

    /**
     * Sets the basis to a fixed Dp value. This is a convenience function equivalent to
     * `basis(FlexBasis.Dp(value))`.
     *
     * @param value The basis size in Dp.
     * @see FlexBasis.Dp
     */
    fun basis(value: Dp)

    /**
     * ◦ Sets the basis to a fraction of the container's main axis size.This is a convenience
     * function equivalent to `basis(FlexBasis.Percent(value))`.
     *
     * @param value A value between 0.0 and 1.0 representing the fraction of the container's size.
     * @see FlexBasis.Percent
     */
    fun basis(@FloatRange(from = 0.0, to = 1.0) value: Float)
}

@OptIn(ExperimentalFlexBoxApi::class)
internal class ResolvedFlexItemInfo : FlexConfigScope {
    var baselineAlignmentLine: AlignmentLine? = null
        private set

    var baselineAlignmentBlock: AlignmentLineProviderBlock? = null
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

    override fun alignSelf(value: FlexAlignSelf) {
        this.alignSelf = value
    }

    override fun alignSelf(alignmentLine: AlignmentLine) {
        this.alignSelf = FlexAlignSelf.Baseline
        this.baselineAlignmentLine = alignmentLine
        this.baselineAlignmentBlock = null
    }

    override fun alignSelf(alignmentLineBlock: (Measured) -> Int) {
        this.alignSelf = FlexAlignSelf.Baseline
        this.baselineAlignmentLine = null
        this.baselineAlignmentBlock = AlignmentLineProviderBlock { alignmentLineBlock(it) }
    }

    override fun order(value: Int) {
        this.order = value
    }

    override fun grow(value: Float) {
        requirePrecondition(value >= 0f) { "Flex grow cannot be negative: $value" }
        this.grow = value
    }

    override fun shrink(value: Float) {
        requirePrecondition(value >= 0f) { "Flex shrink cannot be negative: $value" }
        this.shrink = value
    }

    override fun basis(value: FlexBasis) {
        this.basis = value
    }

    internal var alignSelf: FlexAlignSelf = FlexAlignSelf.Auto

    internal var order: Int = 0

    internal var grow: Float = 0f

    internal var shrink: Float = 1f

    internal var basis: FlexBasis = FlexBasis.Auto

    override fun basis(value: Dp) {
        basis = FlexBasis.Dp(value)
    }

    override fun basis(@FloatRange(from = 0.0, to = 1.0) value: Float) {
        basis = FlexBasis.Percent(value)
    }

    fun getBaseline(placeable: Placeable, fallback: ResolvedFlexBoxConfig): Int {
        return when {
            baselineAlignmentBlock != null ->
                baselineAlignmentBlock!!.calculateAlignmentLinePosition(placeable)
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
 * Combine two [FlexBoxConfig] objects together. Configs further "to the right" will override
 * properties to the left of them, on a per-property basis.
 */
@ExperimentalFlexBoxApi
fun FlexBoxConfig(first: FlexBoxConfig, second: FlexBoxConfig): FlexBoxConfig = first then second

/**
 * Combine three [FlexBoxConfig] objects together. Configs further "to the right" will override
 * properties to the left of them, on a per-property basis.
 */
@ExperimentalFlexBoxApi
fun FlexBoxConfig(
    first: FlexBoxConfig,
    second: FlexBoxConfig,
    third: FlexBoxConfig,
): FlexBoxConfig =
    when {
        first === FlexBoxConfig -> FlexBoxConfig(second, third)
        second === FlexBoxConfig -> FlexBoxConfig(first, third)
        third === FlexBoxConfig -> FlexBoxConfig(first, second)
        first is CombinedFlexBoxConfig &&
            second is CombinedFlexBoxConfig &&
            third is CombinedFlexBoxConfig ->
            FlexBoxConfig(*first.configs, *second.configs, *third.configs)
        first is CombinedFlexBoxConfig && second is CombinedFlexBoxConfig ->
            FlexBoxConfig(*first.configs, *second.configs, third)
        first is CombinedFlexBoxConfig && third is CombinedFlexBoxConfig ->
            FlexBoxConfig(*first.configs, second, *third.configs)
        second is CombinedFlexBoxConfig && third is CombinedFlexBoxConfig ->
            FlexBoxConfig(first, *second.configs, *third.configs)
        first is CombinedFlexBoxConfig -> FlexBoxConfig(*first.configs, second, third)
        second is CombinedFlexBoxConfig -> FlexBoxConfig(first, *second.configs, third)
        third is CombinedFlexBoxConfig -> FlexBoxConfig(first, second, *third.configs)
        else -> CombinedFlexBoxConfig(first, second, third)
    }

/**
 * Combine multiple [FlexBoxConfig] objects together. Configs further "to the right" will override
 * properties to the left of them, on a per-property basis.
 *
 * @sample androidx.compose.foundation.layout.samples.FlexBoxConfigCombineSample
 */
@ExperimentalFlexBoxApi
fun FlexBoxConfig(vararg configs: FlexBoxConfig): FlexBoxConfig =
    if (configs.isEmpty()) {
        FlexBoxConfig
    } else if (configs.any { it === FlexBoxConfig }) {
        val count = configs.count { it !== FlexBoxConfig }
        when (count) {
            0 -> FlexBoxConfig
            1 -> configs.first { it !== FlexBoxConfig }
            else -> {
                val filtered = arrayOfNulls<FlexBoxConfig>(count)
                var cursor = 0
                configs.forEach { config ->
                    if (config !== FlexBoxConfig) {
                        filtered[cursor++] = config
                    }
                }
                @Suppress("UNCHECKED_CAST")
                CombinedFlexBoxConfig(*(filtered as Array<FlexBoxConfig>))
            }
        }
    } else {
        CombinedFlexBoxConfig(*configs)
    }

/**
 * Internal representation for a composition of two or more [FlexBoxConfig] objects.
 *
 * This class holds a **flat** array of configs. The [FlexBoxConfig] factory functions ensure that
 * [CombinedFlexBoxConfig] instances are never nested — if a factory receives a
 * [CombinedFlexBoxConfig] as input, its [configs] array is spread into the new result. This
 * guarantees that [configure] is always a single-pass flat iteration regardless of how many
 * composition steps produced this instance.
 *
 * @property configs the flattened array of configs to apply in order. Later entries override
 *   earlier entries on a per-property basis.
 */
@ExperimentalFlexBoxApi
internal class CombinedFlexBoxConfig(vararg val configs: FlexBoxConfig) : FlexBoxConfig {
    override fun FlexBoxConfigScope.configure() {
        configs.forEach { config -> with(config) { configure() } }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CombinedFlexBoxConfig) return false
        return configs.contentEquals(other.configs)
    }

    override fun hashCode(): Int = configs.contentHashCode()
}

/**
 * Combine two [FlexConfig] objects together. Configs further "to the right" will override
 * properties to the left of them, on a per-property basis.
 */
@ExperimentalFlexBoxApi
fun FlexConfig(first: FlexConfig, second: FlexConfig): FlexConfig = first then second

/**
 * Combine three [FlexConfig] objects together. Configs further "to the right" will override
 * properties to the left of them, on a per-property basis.
 */
@ExperimentalFlexBoxApi
fun FlexConfig(first: FlexConfig, second: FlexConfig, third: FlexConfig): FlexConfig =
    when {
        first === FlexConfig -> FlexConfig(second, third)
        second === FlexConfig -> FlexConfig(first, third)
        third === FlexConfig -> FlexConfig(first, second)
        first is CombinedFlexConfig &&
            second is CombinedFlexConfig &&
            third is CombinedFlexConfig ->
            FlexConfig(*first.configs, *second.configs, *third.configs)
        first is CombinedFlexConfig && second is CombinedFlexConfig ->
            FlexConfig(*first.configs, *second.configs, third)
        first is CombinedFlexConfig && third is CombinedFlexConfig ->
            FlexConfig(*first.configs, second, *third.configs)
        second is CombinedFlexConfig && third is CombinedFlexConfig ->
            FlexConfig(first, *second.configs, *third.configs)
        first is CombinedFlexConfig -> FlexConfig(*first.configs, second, third)
        second is CombinedFlexConfig -> FlexConfig(first, *second.configs, third)
        third is CombinedFlexConfig -> FlexConfig(first, second, *third.configs)
        else -> CombinedFlexConfig(first, second, third)
    }

/**
 * Combine multiple [FlexConfig] objects together. Configs further "to the right" will override
 * properties to the left of them, on a per-property basis.
 *
 * @sample androidx.compose.foundation.layout.samples.FlexConfigCombineSample
 */
@ExperimentalFlexBoxApi
fun FlexConfig(vararg configs: FlexConfig): FlexConfig =
    if (configs.isEmpty()) {
        FlexConfig
    } else if (configs.any { it === FlexConfig }) {
        val count = configs.count { it !== FlexConfig }
        when (count) {
            0 -> FlexConfig
            1 -> configs.first { it !== FlexConfig }
            else -> {
                val filtered = arrayOfNulls<FlexConfig>(count)
                var cursor = 0
                configs.forEach { config ->
                    if (config !== FlexConfig) {
                        filtered[cursor++] = config
                    }
                }
                @Suppress("UNCHECKED_CAST") CombinedFlexConfig(*(filtered as Array<FlexConfig>))
            }
        }
    } else {
        CombinedFlexConfig(*configs)
    }

@OptIn(ExperimentalFlexBoxApi::class)
internal class CombinedFlexConfig(vararg val configs: FlexConfig) : FlexConfig {
    override fun FlexConfigScope.configure() {
        configs.forEach { config -> with(config) { configure() } }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CombinedFlexConfig) return false
        return configs.contentEquals(other.configs)
    }

    override fun hashCode(): Int = configs.contentHashCode()
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
