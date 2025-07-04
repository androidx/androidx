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

package androidx.xr.compose.subspace

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SubspaceMeasurable
import androidx.xr.compose.subspace.layout.SubspaceMeasurePolicy
import androidx.xr.compose.subspace.layout.SubspaceMeasureResult
import androidx.xr.compose.subspace.layout.SubspaceMeasureScope
import androidx.xr.compose.subspace.layout.SubspacePlaceable
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.compose.unit.constrainDepth
import androidx.xr.compose.unit.constrainHeight
import androidx.xr.compose.unit.constrainWidth
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

/** Shared [SubspaceMeasurePolicy] between [Row] and [Column]. */
internal class RowColumnMeasurePolicy(
    private val orientation: LayoutOrientation,
    private val alignment: SpatialAlignment,
    private val curveRadius: Dp,
) : SubspaceMeasurePolicy {
    override fun SubspaceMeasureScope.measure(
        measurables: List<SubspaceMeasurable>,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        val resolvedMeasurables = measurables.map { ResolvedMeasurable(it) }

        measureRowColumnChildren(resolvedMeasurables, constraints)

        // contentSize() also sets the mainAxisPosition of each child.
        val contentSize = resolvedMeasurables.contentSize()
        val containerSize =
            IntVolumeSize(
                width = constraints.constrainWidth(contentSize.width),
                height = constraints.constrainHeight(contentSize.height),
                depth = constraints.constrainDepth(contentSize.depth),
            )

        // Each child will have its main-axis offset adjusted, based on extra space available and
        // the
        // provided alignment.
        val mainAxisOffset =
            if (isHorizontal) {
                // `mainAxisOffset` represents the left edge of the content in the container space.
                alignment.horizontalOffset(contentSize.width, containerSize.width) -
                    contentSize.width / 2
            } else {
                // `mainAxisOffset` represents the top edge of the content in the container space.
                alignment.verticalOffset(contentSize.height, containerSize.height) +
                    contentSize.height / 2
            }

        return layout(containerSize.width, containerSize.height, containerSize.depth) {
            resolvedMeasurables.forEach { resolvedMeasurable ->
                val placeable = resolvedMeasurable.placeable!!
                placeable.place(getPose(resolvedMeasurable, containerSize, mainAxisOffset))
            }
        }
    }

    /**
     * Measures the children of a Row or Column.
     *
     * This method will measure the children of a Row or Column in two passes. In the first pass,
     * children with no weight modifier will be measured. In the second pass, children with weight
     * modifiers will be measured.
     *
     * After measuring, the placeable of each child will be set.
     */
    private fun measureRowColumnChildren(
        resolvedMeasurables: List<ResolvedMeasurable>,
        constraints: VolumeConstraints,
    ) {
        // Space taken up on the main axis by children with no weight modifier.
        var fixedSpace = 0
        // The total amount of weight declared across all children.
        var totalWeight = 0f

        // We will measure non-weighted children in this pass.
        resolvedMeasurables.forEach { resolvedMeasurable ->
            if (resolvedMeasurable.weightInfo.weight > 0f) {
                // Children with weight will be measured after all others.
                totalWeight += resolvedMeasurable.weightInfo.weight
            } else {
                // Children without weight will be measured now.
                resolvedMeasurable.placeable =
                    resolvedMeasurable.measurable
                        .measure(constraints.plusMainAxis(-fixedSpace))
                        .also { fixedSpace += it.mainAxisSize }
            }
        }

        // Now we can measure the weighted children (if any).
        if (totalWeight > 0f) {
            // Amount of space this Row/Column wants to fill up.
            val targetSpace = constraints.mainAxisTargetSpace
            // Amount of space left (after the non-weighted children were measured).
            val remainingToTarget = (targetSpace - fixedSpace).coerceAtLeast(0)
            // Amount of space that would be given to a weighted child with `.weight(1f)`.
            val weightUnitSpace = remainingToTarget / totalWeight

            // First pass through the weighted children, we just want to see what the remaining
            // space is.
            // Due to rounding, we may over/underfill the container.
            //
            // For example, if we have a 200dp row with 7 children, each with a weight of 1f:
            // 200/7 ~= 28.57..., which gets rounded to 29. But 29*7 = 203, so we've overfilled our
            // 200dp row by 3dp.
            //
            // The fix is to track this remainder N, and adjust the first N children by 1dp or -1dp
            // so
            // that the row/column will be the exact size we need.
            var remainder = remainingToTarget
            resolvedMeasurables.forEach { resolvedMeasurable ->
                if (resolvedMeasurable.weightInfo.weight <= 0f) return@forEach
                val weightedSize = resolvedMeasurable.weightInfo.weight * weightUnitSpace
                remainder -= weightedSize.fastRoundToInt()
            }

            resolvedMeasurables.forEach { resolvedMeasurable ->
                if (resolvedMeasurable.weightInfo.weight <= 0f) return@forEach
                val childMainAxisSize = run {
                    val remainderUnit = remainder.sign
                    remainder -= remainderUnit
                    val weightedSize = resolvedMeasurable.weightInfo.weight * weightUnitSpace
                    weightedSize.fastRoundToInt() + remainderUnit
                }
                val childConstraints =
                    buildConstraints(
                        mainAxisMin =
                            if (resolvedMeasurable.weightInfo.fill) childMainAxisSize else 0,
                        mainAxisMax = childMainAxisSize,
                        crossAxisMin = constraints.crossAxisMin,
                        crossAxisMax = constraints.crossAxisMax,
                        minDepth = constraints.minDepth,
                        maxDepth = constraints.maxDepth,
                    )
                resolvedMeasurable.placeable =
                    resolvedMeasurable.measurable.measure(childConstraints)
            }
        }
    }

    private val isHorizontal = orientation == LayoutOrientation.Horizontal

    private val SubspacePlaceable.mainAxisSize
        get() = if (isHorizontal) measuredWidth else measuredHeight

    private val SubspacePlaceable.crossAxisSize
        get() = if (isHorizontal) measuredHeight else measuredWidth

    private fun VolumeConstraints.plusMainAxis(addToMainAxis: Int): VolumeConstraints {
        return VolumeConstraints(
            minWidth = 0,
            maxWidth = if (isHorizontal) maxWidth + addToMainAxis else maxWidth,
            minHeight = 0,
            maxHeight = if (isHorizontal) maxHeight else maxHeight + addToMainAxis,
            minDepth = 0,
            maxDepth = maxDepth,
        )
    }

    private fun buildConstraints(
        mainAxisMin: Int,
        mainAxisMax: Int,
        crossAxisMin: Int,
        crossAxisMax: Int,
        minDepth: Int,
        maxDepth: Int,
    ): VolumeConstraints {
        return VolumeConstraints(
            minWidth = if (isHorizontal) mainAxisMin else crossAxisMin,
            maxWidth = if (isHorizontal) mainAxisMax else crossAxisMax,
            minHeight = if (isHorizontal) crossAxisMin else mainAxisMin,
            maxHeight = if (isHorizontal) crossAxisMax else mainAxisMax,
            minDepth = minDepth,
            maxDepth = maxDepth,
        )
    }

    /**
     * Returns the total size of the content.
     *
     * The main axis size is the sum of all children's main-axis sizes. The cross axis size is the
     * max measured value of all children's cross-axis sizes. The depth size is the max measured
     * value of all children's depth sizes.
     *
     * This method will also set the mainAxisPosition of each child.
     */
    private fun List<ResolvedMeasurable>.contentSize(): IntVolumeSize {
        // Content's main-axis size is the sum of all children's main-axis sizes
        var mainAxisSize = 0
        val mainAxisMultiplier = if (isHorizontal) 1 else -1
        // Content's cross-axis and depth size are the max measured value of all children
        var crossAxisSize = 0
        var depthSize = 0
        this.forEach { resolvedMeasurable ->
            val placeable = resolvedMeasurable.placeable!!
            resolvedMeasurable.mainAxisPosition =
                ((mainAxisSize + placeable.mainAxisSize / 2.0f) * mainAxisMultiplier)
                    .fastRoundToInt()
            mainAxisSize += placeable.mainAxisSize
            crossAxisSize = maxOf(crossAxisSize, placeable.crossAxisSize)
            depthSize = maxOf(depthSize, placeable.measuredDepth)
        }
        return IntVolumeSize(
            width = if (isHorizontal) mainAxisSize else crossAxisSize,
            height = if (isHorizontal) crossAxisSize else mainAxisSize,
            depth = depthSize,
        )
    }

    /**
     * Returns the pose of the child in the container space.
     *
     * The pose is based on the child's main-axis position, cross-axis position, and depth offset,
     * taking into account the alignment of the child. The pose is also adjusted for the curve
     * radius, if it is not infinite.
     */
    private fun Density.getPose(
        resolvedMeasurable: ResolvedMeasurable,
        containerSize: IntVolumeSize,
        mainAxisOffset: Int,
    ): Pose {
        val mainAxisPosition = resolvedMeasurable.mainAxisPosition!! + mainAxisOffset

        // Set child's cross-axis position based on its desired size + the container's
        // size/alignment.
        val crossAxisSize = resolvedMeasurable.placeable!!.crossAxisSize
        val crossAxisPosition =
            if (isHorizontal) {
                resolvedMeasurable.verticalOffset(crossAxisSize, containerSize.height, alignment)
            } else {
                resolvedMeasurable.horizontalOffset(crossAxisSize, containerSize.width, alignment)
            }

        val depthPosition =
            resolvedMeasurable.depthOffset(
                resolvedMeasurable.placeable!!.measuredDepth,
                containerSize.depth,
                alignment,
            )

        var position =
            Vector3(
                x = if (isHorizontal) mainAxisPosition.toFloat() else crossAxisPosition.toFloat(),
                y = if (isHorizontal) crossAxisPosition.toFloat() else mainAxisPosition.toFloat(),
                z = depthPosition.toFloat(),
            )
        var orientation = Quaternion.Identity

        if (curveRadius != Dp.Infinity) {
            val pixelsCurveRadius = curveRadius.toPx()

            // NOTE: Orientation needs to be computed first, otherwise position
            // gets overwritten with the new position which will lead to an
            // incorrect orientation calculation.
            orientation = getOrientationTangentToCircle(position, pixelsCurveRadius)
            position = getPositionOnCircle(position, pixelsCurveRadius)
        }

        return Pose(position, orientation)
    }

    /**
     * The target space is the amount of space that the content should take up on the main axis.
     * This is based on the constraints of the container.
     */
    private val VolumeConstraints.mainAxisTargetSpace: Int
        get() {
            val mainAxisMax = if (isHorizontal) maxWidth else maxHeight
            return if (mainAxisMax != VolumeConstraints.INFINITY) {
                mainAxisMax
            } else {
                if (isHorizontal) minWidth else minHeight
            }
        }

    private val VolumeConstraints.crossAxisMin: Int
        get() = if (isHorizontal) minHeight else minWidth

    private val VolumeConstraints.crossAxisMax: Int
        get() = if (isHorizontal) maxHeight else maxWidth
}

// [radius], like [position], should be in pixels.
private fun getPositionOnCircle(position: Vector3, radius: Float): Vector3 {
    // NOTE: This method is hard coded to work with rows.  Needs to be made
    // slightly more general to work with columns.
    val arclength = position.x // Signed, negative means arc extends to left.
    val theta = arclength / radius
    val x = radius * sin(theta)
    val y = position.y
    val z = radius * (1.0f - cos(theta)) + position.z
    return Vector3(x.toInt().toFloat(), y.toInt().toFloat(), z.toInt().toFloat())
}

// [radius], like [position], should be in pixels.
private fun getOrientationTangentToCircle(position: Vector3, radius: Float): Quaternion {
    // NOTE: This method is hard coded to work with rows.  Needs to be made
    // slightly more general to work with columns.
    val arclength = position.x // Signed, negative means arc extends to left.
    val theta = arclength / radius

    // We need to rotate by negative theta (clockwise) around the Y axis.
    val qX = 0.0f
    val qY = sin(-theta * 0.5f)
    val qZ = 0.0f
    val qW = cos(-theta * 0.5f)

    return Quaternion(qX, qY, qZ, qW)
}

/**
 * A [SubspaceMeasurable] and all associated information computed in
 * [RowColumnMeasurePolicy.measure].
 */
private class ResolvedMeasurable(val measurable: SubspaceMeasurable) {
    /** Parameters set by the [RowScope.weight] and [ColumnScope.weight] modifiers. */
    val weightInfo: RowColumnParentData = RowColumnParentData().also { measurable.adjustParams(it) }

    /** Parameters set by the [RowScope.align] and [ColumnScope.align] modifiers. */
    val alignment: RowColumnSpatialAlignmentParentData =
        RowColumnSpatialAlignmentParentData().also { measurable.adjustParams(it) }

    /**
     * A measured placeable, only present once [SubspaceMeasurable.measure] is called on
     * [measurable].
     */
    var placeable: SubspacePlaceable? = null

    /** The main-axis position of this child in its parent; set after all children are measured. */
    var mainAxisPosition: Int? = null

    fun horizontalOffset(width: Int, space: Int, parentSpatialAlignment: SpatialAlignment): Int =
        alignment.horizontalSpatialAlignment?.offset(width, space)
            ?: parentSpatialAlignment.horizontalOffset(width, space)

    fun verticalOffset(height: Int, space: Int, parentSpatialAlignment: SpatialAlignment): Int =
        alignment.verticalSpatialAlignment?.offset(height, space)
            ?: parentSpatialAlignment.verticalOffset(height, space)

    fun depthOffset(depth: Int, space: Int, parentSpatialAlignment: SpatialAlignment): Int =
        alignment.depthSpatialAlignment?.offset(depth, space)
            ?: parentSpatialAlignment.depthOffset(depth, space)

    override fun toString(): String {
        return measurable.toString()
    }
}

/** [Row] is [Horizontal], [Column] is [Vertical]. */
internal enum class LayoutOrientation {
    Horizontal,
    Vertical,
}
