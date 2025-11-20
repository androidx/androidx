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

package androidx.xr.compose.subspace

import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastRoundToInt
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SubspaceMeasurable
import androidx.xr.compose.subspace.layout.SubspaceMeasureResult
import androidx.xr.compose.subspace.layout.SubspaceMeasureScope
import androidx.xr.compose.subspace.layout.SubspacePlaceable
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.compose.unit.constrainDepth
import androidx.xr.compose.unit.constrainHeight
import androidx.xr.compose.unit.constrainWidth
import androidx.xr.runtime.math.Pose
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * Base class for measure policies of [SpatialRow] and [SpatialColumn]. It encapsulates the common
 * logic for measuring and placing children along a main axis, handling weights, arrangements, and
 * basic alignment.
 *
 * Subclasses must define axis-specific details like main/cross axis sizes and how final poses are
 * computed.
 */
internal abstract class SpatialRowColumnMeasurePolicy {

    /** Returns the size of the placeable along the main layout axis. */
    abstract val SubspacePlaceable.mainAxisSize: Int
    /** Returns the size of the placeable along the cross layout axis. */
    abstract val SubspacePlaceable.crossAxisSize: Int
    /**
     * The target space is the amount of space that the content should take up on the main axis.
     * This is based on the constraints of the container.
     */
    abstract val VolumeConstraints.mainAxisTargetSpace: Int
    /** Returns the minimum constraint for the main axis. */
    abstract val VolumeConstraints.mainAxisMin: Int
    /** Returns the minimum constraint for the cross axis. */
    abstract val VolumeConstraints.crossAxisMin: Int
    /** Returns the maximum constraint for the cross axis. */
    abstract val VolumeConstraints.crossAxisMax: Int

    /**
     * Arranges the children's positions along the main axis based on the total main axis size and
     * the specific arrangement logic provided by concrete implementations (e.g., SpatialRow's
     * horizontal arrangement).
     *
     * @param mainAxisLayoutSize The total size available or occupied along the main axis.
     * @param childrenMainAxisSize An array containing the main axis sizes of each child.
     * @param mainAxisPositions An output array to be populated with the calculated main axis
     *   position for each child.
     * @param subspaceMeasureScope The scope providing layout direction and arrangement helpers.
     */
    abstract fun arrangeMainAxisPositions(
        mainAxisLayoutSize: Int,
        childrenMainAxisSize: IntArray,
        mainAxisPositions: IntArray,
        subspaceMeasureScope: SubspaceMeasureScope,
    )

    /**
     * Calculates the offset required to position the block of content (all children) along the main
     * axis within the container, according to the specified alignment.
     *
     * @param contentSize The total size of the content block.
     * @param containerSize The total size of the container.
     * @return The offset along the main axis.
     */
    abstract fun getMainAxisOffset(contentSize: IntVolumeSize, containerSize: IntVolumeSize): Int

    /**
     * Creates [VolumeConstraints] for a child, given specific constraints for main axis, cross
     * axis, and depth.
     */
    abstract fun buildConstraints(
        mainAxisMin: Int,
        mainAxisMax: Int,
        crossAxisMin: Int,
        crossAxisMax: Int,
        minDepth: Int,
        maxDepth: Int,
    ): VolumeConstraints

    /**
     * Modifies the given [VolumeConstraints] by adding a value to the main axis constraints. This
     * is typically used when measuring children without weight, to subtract the space already
     * occupied by fixed-size children.
     */
    abstract fun VolumeConstraints.plusMainAxis(addToMainAxis: Int): VolumeConstraints

    /**
     * Core measurement logic for children. It resolves measurables, determines content size, and
     * then places children using [placeHelper].
     */
    internal fun measure(
        measurables: List<SubspaceMeasurable>,
        constraints: VolumeConstraints,
        arrangementSpacingInt: Int,
        mainAxisMultiplier: MainAxisMultiplier,
        subspaceMeasureScope: SubspaceMeasureScope,
    ): SubspaceMeasureResult {
        val resolvedMeasurables = measurables.fastMap { ResolvedMeasurable(it) }

        val contentSize =
            measureAndArrangeChildren(
                resolvedMeasurables = resolvedMeasurables,
                constraints = constraints,
                arrangementSpacingInt = arrangementSpacingInt,
                subspaceMeasureScope = subspaceMeasureScope,
                mainAxisMultiplier = mainAxisMultiplier,
            )
        val containerSize =
            IntVolumeSize(
                width = constraints.constrainWidth(contentSize.width),
                height = constraints.constrainHeight(contentSize.height),
                depth = constraints.constrainDepth(contentSize.depth),
            )

        return placeHelper(contentSize, containerSize, resolvedMeasurables, subspaceMeasureScope)
    }

    /**
     * Data class to hold the state and results of measuring children. This is passed around and
     * updated during the different measurement passes.
     */
    private data class ChildrenMeasureState(
        // Space occupied by fixed-size (non-weighted) children, including their spacing.
        var fixedSpace: Int = 0,
        // Total weight sum of all weighted children.
        var totalWeight: Float = 0f,
        // Count of children that have a weight modifier.
        var totalWeightedChildren: Int = 0,
        // Arrangement spacing to be applied after the last measured non-weighted child,
        // if followed by weighted children.
        var spaceAfterLastNoWeight: Int = 0,
        // Maximum cross-axis size encountered among all children.
        var crossAxisSize: Int = 0,
        // Maximum depth encountered among all children.
        var depthSize: Int = 0,
        // Accumulated main-axis space for weighted children, *excluding* their arrangement spacing.
        var weightedChildrenSpace: Int = 0,
        // Total main-axis space for weighted children, *including* their arrangement spacing.
        var totalWeightedSpace: Int = 0,
    )

    /**
     * Measures the children of a Row or Column, handling both weighted and non-weighted items, and
     * then arranges them.
     *
     * This function orchestrates the measurement in two main passes:
     * 1. Non-weighted children are measured to determine fixed space.
     * 2. Weighted children are measured, distributing remaining space according to their weights.
     *    Finally, it arranges all children based on the specified arrangement.
     *
     * @param resolvedMeasurables The list of [ResolvedMeasurable] children.
     * @param constraints The [VolumeConstraints] for the parent layout.
     * @param arrangementSpacingInt The spacing to apply between children, in pixels.
     * @param subspaceMeasureScope The scope providing layout context.
     * @param mainAxisMultiplier Multiplier for main axis positioning.
     * @return The total [IntVolumeSize] occupied by the content.
     */
    private fun measureAndArrangeChildren(
        resolvedMeasurables: List<ResolvedMeasurable>,
        constraints: VolumeConstraints,
        arrangementSpacingInt: Int,
        subspaceMeasureScope: SubspaceMeasureScope,
        mainAxisMultiplier: MainAxisMultiplier,
    ): IntVolumeSize {
        val childrenMainAxisSize = IntArray(resolvedMeasurables.size)
        val measureState = ChildrenMeasureState()

        measureNonWeightedChildrenPass(
            resolvedMeasurables,
            constraints,
            arrangementSpacingInt,
            childrenMainAxisSize,
            measureState,
        )

        measureWeightedChildrenPass(
            resolvedMeasurables,
            constraints,
            arrangementSpacingInt.toLong(),
            childrenMainAxisSize,
            measureState,
        )

        val mainAxisSize =
            (measureState.fixedSpace + measureState.totalWeightedSpace).coerceAtLeast(0)
        val mainAxisLayoutSize = max(mainAxisSize, constraints.mainAxisMin)

        val mainAxisPositions = IntArray(resolvedMeasurables.size)
        arrangeMainAxisPositions(
            mainAxisLayoutSize,
            childrenMainAxisSize,
            mainAxisPositions,
            subspaceMeasureScope,
        )

        resolvedMeasurables.populateMainAxisPositions(mainAxisPositions, mainAxisMultiplier)

        return contentSize(mainAxisSize, measureState.crossAxisSize, measureState.depthSize)
    }

    /**
     * Performs the first measurement pass for non-weighted children. It measures children that do
     * not have a weight modifier, calculates the space they occupy (`fixedSpace`), and updates the
     * overall `crossAxisSize` and `depthSize`. It also gathers information about weighted children
     * (`totalWeight`, `totalWeightedChildren`).
     */
    private fun measureNonWeightedChildrenPass(
        resolvedMeasurables: List<ResolvedMeasurable>,
        constraints: VolumeConstraints,
        arrangementSpacingInt: Int,
        childrenMainAxisSize: IntArray, // Output
        measureState: ChildrenMeasureState, // Input/Output
    ) {
        resolvedMeasurables.fastForEachIndexed { index, resolvedMeasurable ->
            if (resolvedMeasurable.weightInfo.weight > 0f) {
                measureState.totalWeight += resolvedMeasurable.weightInfo.weight
                measureState.totalWeightedChildren++
            } else {
                val remainingMainAxisSpace =
                    constraints.mainAxisTargetSpace - measureState.fixedSpace
                val childConstraints = constraints.plusMainAxis(-measureState.fixedSpace)

                resolvedMeasurable.placeable =
                    resolvedMeasurable.measurable.measure(childConstraints).also { placeable ->
                        childrenMainAxisSize[index] = placeable.mainAxisSize
                        measureState.spaceAfterLastNoWeight =
                            min(
                                arrangementSpacingInt,
                                (remainingMainAxisSpace - placeable.mainAxisSize).coerceAtLeast(0),
                            )
                        measureState.fixedSpace +=
                            placeable.mainAxisSize + measureState.spaceAfterLastNoWeight
                        measureState.crossAxisSize =
                            maxOf(measureState.crossAxisSize, placeable.crossAxisSize)
                        measureState.depthSize =
                            maxOf(measureState.depthSize, placeable.measuredDepth)
                    }
            }
        }
    }

    /**
     * Calculates the total rounding remainder for weighted children. Due to float-to-int conversion
     * when assigning space based on weights, the sum of rounded individual sizes might not exactly
     * match the total intended space. This function calculates that difference (the remainder).
     *
     * @param resolvedMeasurables List of all resolved measurables.
     * @param totalSpaceForWeightedChildren The total main axis space allocated for all weighted
     *   children (excluding spacing).
     * @param weightUnitSpace The amount of space allocated per unit of weight.
     * @return The total rounding remainder. This value will be distributed among weighted children.
     */
    private fun calculateWeightedSpaceRoundingRemainder(
        resolvedMeasurables: List<ResolvedMeasurable>,
        totalSpaceForWeightedChildren: Long,
        weightUnitSpace: Float,
    ): Long {
        var remainder = totalSpaceForWeightedChildren
        resolvedMeasurables.fastForEach {
            if (it.weightInfo.weight > 0f) {
                remainder -= (it.weightInfo.weight * weightUnitSpace).fastRoundToInt()
            }
        }
        return remainder
    }

    /**
     * Measures a single weighted child, assigns it space considering rounding errors, and updates
     * the overall measure state.
     *
     * @param resolvedMeasurable The specific weighted child to measure.
     * @param index The index of this child in the `childrenMainAxisSize` array.
     * @param remainder Mutable reference to the remaining rounding error to be distributed. This
     *   function will consume one unit (1 or -1) of this remainder if it's non-zero.
     * @param weightUnitSpace Space allocated per unit of weight.
     * @param constraints Parent's volume constraints.
     * @param childrenMainAxisSize Output array to store the measured main axis size of this child.
     * @param measureState Mutable state object to update with this child's measurements
     *   (`weightedChildrenSpace`, `crossAxisSize`, `depthSize`).
     */
    private fun measureAndRecordSingleWeightedChild(
        resolvedMeasurable: ResolvedMeasurable,
        index: Int,
        remainder: MutableLong,
        weightUnitSpace: Float,
        constraints: VolumeConstraints,
        childrenMainAxisSize: IntArray, // Output
        measureState: ChildrenMeasureState, // Input/Output
    ) {
        val childAssignedMainAxisSize = run {
            val remainderUnit = remainder.value.sign
            remainder.value -= remainderUnit
            val baseWeightedSize = resolvedMeasurable.weightInfo.weight * weightUnitSpace
            baseWeightedSize.fastRoundToInt() + remainderUnit
        }

        val childConstraints =
            buildConstraints(
                mainAxisMin =
                    if (resolvedMeasurable.weightInfo.fill) childAssignedMainAxisSize else 0,
                mainAxisMax = childAssignedMainAxisSize,
                crossAxisMin = constraints.crossAxisMin,
                crossAxisMax = constraints.crossAxisMax,
                minDepth = constraints.minDepth,
                maxDepth = constraints.maxDepth,
            )

        resolvedMeasurable.placeable =
            resolvedMeasurable.measurable.measure(childConstraints).also { placeable ->
                childrenMainAxisSize[index] = placeable.mainAxisSize
                measureState.weightedChildrenSpace += placeable.mainAxisSize
                measureState.crossAxisSize =
                    maxOf(measureState.crossAxisSize, placeable.crossAxisSize)
                measureState.depthSize = maxOf(measureState.depthSize, placeable.measuredDepth)
            }
    }

    /** Wrapper class for a mutable Long to pass by reference. */
    private class MutableLong(var value: Long)

    /**
     * Performs the second measurement pass for weighted children. Distributes remaining main-axis
     * space among children with weight modifiers, measures them, and updates `totalWeightedSpace`,
     * `crossAxisSize`, and `depthSize`.
     */
    private fun measureWeightedChildrenPass(
        resolvedMeasurables: List<ResolvedMeasurable>,
        constraints: VolumeConstraints,
        arrangementSpacingPx: Long,
        childrenMainAxisSize: IntArray, // Input/Output
        measureState: ChildrenMeasureState, // Input/Output
    ) {
        if (measureState.totalWeightedChildren == 0) {
            measureState.fixedSpace -= measureState.spaceAfterLastNoWeight
            return // totalWeightedSpace remains 0
        }

        val weightedArrangementSpacingTotal =
            if (measureState.totalWeightedChildren > 0)
                arrangementSpacingPx * (measureState.totalWeightedChildren - 1)
            else 0

        val spaceAvailableForWeightedChildrenAndTheirSpacing =
            (constraints.mainAxisTargetSpace - measureState.fixedSpace).coerceAtLeast(0)

        val totalSpaceForWeightedChildrenContent = // Space for content only, excluding their
            // arrangement spacing
            (spaceAvailableForWeightedChildrenAndTheirSpacing - weightedArrangementSpacingTotal)
                .coerceAtLeast(0)

        val weightUnitSpace =
            if (measureState.totalWeight > 0)
                totalSpaceForWeightedChildrenContent / measureState.totalWeight
            else 0f

        val remainder =
            MutableLong(
                calculateWeightedSpaceRoundingRemainder(
                    resolvedMeasurables,
                    totalSpaceForWeightedChildrenContent,
                    weightUnitSpace,
                )
            )

        resolvedMeasurables.fastForEachIndexed { index, child ->
            if (child.weightInfo.weight > 0f) {
                measureAndRecordSingleWeightedChild(
                    resolvedMeasurable = child,
                    index = index,
                    remainder = remainder,
                    weightUnitSpace = weightUnitSpace,
                    constraints = constraints, // Pass parent constraints
                    childrenMainAxisSize = childrenMainAxisSize,
                    measureState = measureState,
                )
            }
        }

        measureState.totalWeightedSpace =
            (measureState.weightedChildrenSpace + weightedArrangementSpacingTotal)
                .toInt()
                .coerceIn(0, spaceAvailableForWeightedChildrenAndTheirSpacing)
    }

    /** Populates the `mainAxisPosition` for each [ResolvedMeasurable]. */
    private fun List<ResolvedMeasurable>.populateMainAxisPositions(
        mainAxisPositions: IntArray,
        mainAxisMultiplier: MainAxisMultiplier,
    ) {
        this.fastForEachIndexed { index, resolvedMeasurable ->
            resolvedMeasurable.mainAxisPosition =
                mainAxisPositions[index] * mainAxisMultiplier.value
        }
    }

    /** Helper function to perform layout after content size and main axis offset are known. */
    private fun placeHelper(
        contentSize: IntVolumeSize,
        containerSize: IntVolumeSize,
        resolvedMeasurables: List<ResolvedMeasurable>,
        subspaceMeasureScope: SubspaceMeasureScope,
    ): SubspaceMeasureResult {
        val mainAxisOffset = getMainAxisOffset(contentSize, containerSize)

        return with(subspaceMeasureScope) {
            layout(containerSize.width, containerSize.height, containerSize.depth) {
                resolvedMeasurables.fastForEach { resolvedMeasurable ->
                    val placeable =
                        checkNotNull(resolvedMeasurable.placeable) {
                            "Placeable cannot be null during placement. Measurement pass might have failed."
                        }
                    placeable.place(getPose(resolvedMeasurable, containerSize, mainAxisOffset))
                }
            }
        }
    }

    /** Returns the total size of the content (sum of children sizes and spacing). */
    abstract fun contentSize(
        mainAxisLayoutSize: Int,
        crossAxisSize: Int,
        depthSize: Int,
    ): IntVolumeSize

    /**
     * Returns the pose of the child in the container space.
     *
     * The pose is based on the child's main-axis position, cross-axis position, and depth offset,
     * taking into account the alignment of the child. For [SpatialRow], this may also adjust for
     * curvature if a `curveRadius` is specified.
     */
    abstract fun Density.getPose(
        resolvedMeasurable: ResolvedMeasurable,
        containerSize: IntVolumeSize,
        mainAxisOffset: Int,
    ): Pose
}

/**
 * A [SubspaceMeasurable] and its associated layout data computed during the measure pass of
 * [SpatialRowColumnMeasurePolicy]. This includes weight, alignment overrides, the resulting
 * [SubspacePlaceable], and its calculated main axis position.
 */
internal class ResolvedMeasurable(val measurable: SubspaceMeasurable) {
    /** Parent data related to layout weight, extracted from modifiers. */
    val weightInfo: RowColumnParentData = RowColumnParentData().also { measurable.adjustParams(it) }

    /** Parent data for overriding spatial alignment, extracted from modifiers. */
    val alignment: RowColumnSpatialAlignmentParentData =
        RowColumnSpatialAlignmentParentData().also { measurable.adjustParams(it) }

    /** The [SubspacePlaceable] resulting from measuring the [measurable]. Null until measured. */
    var placeable: SubspacePlaceable? = null

    /**
     * The calculated position of this child along the main axis of the parent. Null until
     * positioned.
     */
    var mainAxisPosition: Int? = null

    /**
     * Calculates the horizontal offset, considering local alignment override or falling back to
     * parent alignment.
     */
    fun horizontalOffset(width: Int, space: Int, parentSpatialAlignment: SpatialAlignment): Int =
        alignment.horizontalSpatialAlignment?.offset(width, space)
            ?: parentSpatialAlignment.horizontalOffset(width, space)

    /**
     * Calculates the vertical offset, considering local alignment override or falling back to
     * parent alignment.
     */
    fun verticalOffset(height: Int, space: Int, parentSpatialAlignment: SpatialAlignment): Int =
        alignment.verticalSpatialAlignment?.offset(height, space)
            ?: parentSpatialAlignment.verticalOffset(height, space)

    /**
     * Calculates the depth offset, considering local alignment override or falling back to parent
     * alignment.
     */
    fun depthOffset(depth: Int, space: Int, parentSpatialAlignment: SpatialAlignment): Int =
        alignment.depthSpatialAlignment?.offset(depth, space)
            ?: parentSpatialAlignment.depthOffset(depth, space)

    override fun toString(): String {
        return measurable.toString()
    }
}

/**
 * Specifies the direction multiplier for the main axis. [HorizontalAxisMultiplier] is typically
 * used for horizontal layouts (e.g., SpatialRow), and [VerticalAxisMultiplier] for vertical layouts
 * (e.g., SpatialColumn).
 */
internal enum class MainAxisMultiplier(val value: Int) {
    HorizontalAxisMultiplier(1),
    VerticalAxisMultiplier(-1),
}
