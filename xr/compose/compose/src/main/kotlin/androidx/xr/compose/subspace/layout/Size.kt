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

package androidx.xr.compose.subspace.layout

import androidx.annotation.FloatRange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.xr.compose.subspace.node.SubspaceLayoutModifierNode
import androidx.xr.compose.subspace.node.SubspaceModifierElement
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.compose.unit.constrain
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3

/** Declare the preferred size of the content to be exactly [width] dp along the x dimension. */
public fun SubspaceModifier.width(width: Dp): SubspaceModifier =
    this.then(SizeElement(minWidth = width, maxWidth = width, enforceIncoming = true))

/** Declare the preferred size of the content to be exactly [height] dp along the y dimension. */
public fun SubspaceModifier.height(height: Dp): SubspaceModifier =
    this.then(SizeElement(minHeight = height, maxHeight = height, enforceIncoming = true))

/**
 * Declare the preferred size of the content to be exactly [depth] dp along the z dimension. Panels
 * have 0 depth and ignore this modifier.
 */
public fun SubspaceModifier.depth(depth: Dp): SubspaceModifier =
    this.then(SizeElement(minDepth = depth, maxDepth = depth, enforceIncoming = true))

/**
 * Declare the preferred size of the content to be exactly a [size] dp cube. When applied to a
 * Panel, the preferred size will be a [size] dp square instead.
 */
public fun SubspaceModifier.size(size: Dp): SubspaceModifier =
    this.then(
        SizeElement(
            minWidth = size,
            maxWidth = size,
            minHeight = size,
            maxHeight = size,
            minDepth = size,
            maxDepth = size,
            enforceIncoming = true,
        )
    )

/**
 * Declare the preferred size of the content to be exactly [size] in each of the three dimensions.
 * Panels have 0 depth and ignore the z-component of this modifier.
 */
public fun SubspaceModifier.size(size: DpVolumeSize): SubspaceModifier =
    this.then(
        SizeElement(
            minWidth = size.width,
            maxWidth = size.width,
            minHeight = size.height,
            maxHeight = size.height,
            minDepth = size.depth,
            maxDepth = size.depth,
            enforceIncoming = true,
        )
    )

/** Declare the size of the content to be exactly [width] dp along the x dimension. */
public fun SubspaceModifier.requiredWidth(width: Dp): SubspaceModifier =
    this.then(SizeElement(minWidth = width, maxWidth = width, enforceIncoming = false))

/** Declare the size of the content to be exactly [height] dp along the y dimension. */
public fun SubspaceModifier.requiredHeight(height: Dp): SubspaceModifier =
    this.then(SizeElement(minHeight = height, maxHeight = height, enforceIncoming = false))

/**
 * Declare the size of the content to be exactly [depth] dp along the z dimension. Panels have 0
 * depth and ignore this modifier.
 */
public fun SubspaceModifier.requiredDepth(depth: Dp): SubspaceModifier =
    this.then(SizeElement(minDepth = depth, maxDepth = depth, enforceIncoming = false))

/**
 * Declare the size of the content to be exactly a [size] dp cube. When applied to a Panel, the size
 * will be a [size] dp square instead.
 */
public fun SubspaceModifier.requiredSize(size: Dp): SubspaceModifier =
    this.then(
        SizeElement(
            minWidth = size,
            maxWidth = size,
            minHeight = size,
            maxHeight = size,
            minDepth = size,
            maxDepth = size,
            enforceIncoming = false,
        )
    )

/**
 * Declare the size of the content to be exactly [size] in each of the three dimensions. Panels have
 * 0 depth and ignore the z-component of this modifier.
 */
public fun SubspaceModifier.requiredSize(size: DpVolumeSize): SubspaceModifier =
    this.then(
        SizeElement(
            minWidth = size.width,
            maxWidth = size.width,
            minHeight = size.height,
            maxHeight = size.height,
            minDepth = size.depth,
            maxDepth = size.depth,
            enforceIncoming = false,
        )
    )

/**
 * Have the content fill (possibly only partially) the [VolumeConstraints.maxWidth] of the incoming
 * measurement constraints, by setting the [minimum width][VolumeConstraints.minWidth] and the
 * [maximum width][VolumeConstraints.maxWidth] to be equal to the
 * [maximum width][VolumeConstraints.maxWidth] multiplied by [fraction]. Note that, by default, the
 * [fraction] is 1, so the modifier will make the content fill the whole available width. If the
 * incoming maximum width is [VolumeConstraints.Infinity] this modifier will have no effect.
 *
 * @param fraction The fraction of the maximum width to use, between `0` and `1`, inclusive.
 */
public fun SubspaceModifier.fillMaxWidth(
    @FloatRange(from = 0.0, to = 1.0) fraction: Float = 1f
): SubspaceModifier =
    this.then(if (fraction == 1f) FillWholeMaxWidth else FillElement.width(fraction))

private val FillWholeMaxWidth = FillElement.width(1f)

/**
 * Have the content fill (possibly only partially) the [VolumeConstraints.maxHeight] of the incoming
 * measurement constraints, by setting the [minimum height][VolumeConstraints.minHeight] and the
 * [maximum height][VolumeConstraints.maxHeight] to be equal to the
 * [maximum height][VolumeConstraints.maxHeight] multiplied by [fraction]. Note that, by default,
 * the [fraction] is 1, so the modifier will make the content fill the whole available height. If
 * the incoming maximum height is [VolumeConstraints.Infinity] this modifier will have no effect.
 *
 * @param fraction The fraction of the maximum height to use, between `0` and `1`, inclusive.
 */
public fun SubspaceModifier.fillMaxHeight(
    @FloatRange(from = 0.0, to = 1.0) fraction: Float = 1f
): SubspaceModifier =
    this.then(if (fraction == 1f) FillWholeMaxHeight else FillElement.height(fraction))

private val FillWholeMaxHeight = FillElement.height(1f)

/**
 * Have the content fill (possibly only partially) the [VolumeConstraints.maxDepth] of the incoming
 * measurement constraints, by setting the [minimum depth][VolumeConstraints.minDepth] and the
 * [maximum depth][VolumeConstraints.maxDepth] to be equal to the
 * [maximum depth][VolumeConstraints.maxDepth] multiplied by [fraction]. Note that, by default, the
 * [fraction] is 1, so the modifier will make the content fill the whole available depth. If the
 * incoming maximum depth is [VolumeConstraints.Infinity] this modifier will have no effect.
 *
 * @param fraction The fraction of the maximum height to use, between `0` and `1`, inclusive.
 */
public fun SubspaceModifier.fillMaxDepth(
    @FloatRange(from = 0.0, to = 1.0) fraction: Float = 1f
): SubspaceModifier =
    this.then(if (fraction == 1f) FillWholeMaxDepth else FillElement.depth(fraction))

private val FillWholeMaxDepth = FillElement.depth(1f)

/**
 * Have the content fill (possibly only partially) the [VolumeConstraints.maxWidth],
 * [VolumeConstraints.maxHeight], and [VolumeConstraints.maxDepth] of the incoming measurement
 * constraints. See [SubspaceModifier.fillMaxWidth], [SubspaceModifier.fillMaxHeight], and
 * [SubspaceModifier.fillMaxDepth] for details. Note that, by default, the [fraction] is 1, so the
 * modifier will make the content fill the whole available space. If the incoming maximum width or
 * height or depth is [VolumeConstraints.Infinity] this modifier will have no effect in that
 * dimension.
 *
 * @param fraction The fraction of the maximum size to use, between `0` and `1`, inclusive.
 */
public fun SubspaceModifier.fillMaxSize(
    @FloatRange(from = 0.0, to = 1.0) fraction: Float = 1f
): SubspaceModifier =
    this.then(if (fraction == 1f) FillWholeMaxSize else FillElement.size(fraction))

private val FillWholeMaxSize = FillElement.size(1f)

private class FillElement(
    private val direction: Direction,
    private val fraction: Float,
    private val inspectorName: String,
) : SubspaceModifierElement<FillNode>() {
    override fun create(): FillNode = FillNode(direction = direction, fraction = fraction)

    override fun update(node: FillNode) {
        node.direction = direction
        node.fraction = fraction
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FillElement) return false

        if (direction != other.direction) return false
        if (fraction != other.fraction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = direction.hashCode()
        result = 31 * result + fraction.hashCode()
        return result
    }

    @Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
    public companion object {
        public fun width(fraction: Float) =
            FillElement(
                direction = Direction.X,
                fraction = fraction,
                inspectorName = "fillMaxWidth"
            )

        public fun height(fraction: Float) =
            FillElement(
                direction = Direction.Y,
                fraction = fraction,
                inspectorName = "fillMaxHeight"
            )

        public fun depth(fraction: Float) =
            FillElement(
                direction = Direction.Z,
                fraction = fraction,
                inspectorName = "fillMaxDepth"
            )

        public fun size(fraction: Float) =
            FillElement(
                direction = Direction.AllThree,
                fraction = fraction,
                inspectorName = "fillMaxSize",
            )
    }
}

private class FillNode(public var direction: Direction, public var fraction: Float) :
    SubspaceLayoutModifierNode, SubspaceModifier.Node() {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: VolumeConstraints,
    ): MeasureResult {
        val minWidth: Int
        val maxWidth: Int
        if (
            constraints.hasBoundedWidth &&
                (direction == Direction.X || direction == Direction.AllThree)
        ) {
            val width =
                (constraints.maxWidth * fraction)
                    .fastRoundToInt()
                    .coerceIn(constraints.minWidth, constraints.maxWidth)
            minWidth = width
            maxWidth = width
        } else {
            minWidth = constraints.minWidth
            maxWidth = constraints.maxWidth
        }
        val minHeight: Int
        val maxHeight: Int
        if (
            constraints.hasBoundedHeight &&
                (direction == Direction.Y || direction == Direction.AllThree)
        ) {
            val height =
                (constraints.maxHeight * fraction)
                    .fastRoundToInt()
                    .coerceIn(constraints.minHeight, constraints.maxHeight)
            minHeight = height
            maxHeight = height
        } else {
            minHeight = constraints.minHeight
            maxHeight = constraints.maxHeight
        }
        val minDepth: Int
        val maxDepth: Int
        if (
            constraints.hasBoundedDepth &&
                (direction == Direction.Z || direction == Direction.AllThree)
        ) {
            val depth =
                (constraints.maxDepth * fraction)
                    .fastRoundToInt()
                    .coerceIn(constraints.minDepth, constraints.maxDepth)
            minDepth = depth
            maxDepth = depth
        } else {
            minDepth = constraints.minDepth
            maxDepth = constraints.maxDepth
        }
        val placeable =
            measurable.measure(
                VolumeConstraints(minWidth, maxWidth, minHeight, maxHeight, minDepth, maxDepth)
            )

        return layout(placeable.measuredWidth, placeable.measuredHeight, placeable.measuredDepth) {
            placeable.place(Pose(translation = Vector3.Zero, rotation = Quaternion.Identity))
        }
    }
}

private class SizeElement(
    private val minWidth: Dp = Dp.Unspecified,
    private val maxWidth: Dp = Dp.Unspecified,
    private val minHeight: Dp = Dp.Unspecified,
    private val maxHeight: Dp = Dp.Unspecified,
    private val minDepth: Dp = Dp.Unspecified,
    private val maxDepth: Dp = Dp.Unspecified,
    private val enforceIncoming: Boolean,
) : SubspaceModifierElement<SizeNode>() {
    override fun create(): SizeNode =
        SizeNode(
            minWidth = minWidth,
            minHeight = minHeight,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            minDepth = minDepth,
            maxDepth = maxDepth,
            enforceIncoming = enforceIncoming,
        )

    override fun hashCode(): Int {
        var result = minWidth.hashCode()
        result = 31 * result + minHeight.hashCode()
        result = 31 * result + maxWidth.hashCode()
        result = 31 * result + maxHeight.hashCode()
        result = 31 * result + minDepth.hashCode()
        result = 31 * result + maxDepth.hashCode()
        result = 31 * result + enforceIncoming.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SizeElement) return false

        if (minWidth != other.minWidth) return false
        if (minHeight != other.minHeight) return false
        if (maxWidth != other.maxWidth) return false
        if (maxHeight != other.maxHeight) return false
        if (minDepth != other.minDepth) return false
        if (maxDepth != other.maxDepth) return false
        if (enforceIncoming != other.enforceIncoming) return false

        return true
    }

    override fun update(node: SizeNode) {
        node.minWidth = minWidth
        node.minHeight = minHeight
        node.maxWidth = maxWidth
        node.maxHeight = maxHeight
        node.minDepth = minDepth
        node.maxDepth = maxDepth
        node.enforceIncoming = enforceIncoming
    }
}

private class SizeNode(
    public var minWidth: Dp = Dp.Unspecified,
    public var maxWidth: Dp = Dp.Unspecified,
    public var minHeight: Dp = Dp.Unspecified,
    public var maxHeight: Dp = Dp.Unspecified,
    public var minDepth: Dp = Dp.Unspecified,
    public var maxDepth: Dp = Dp.Unspecified,
    public var enforceIncoming: Boolean,
) : SubspaceLayoutModifierNode, SubspaceModifier.Node() {

    private val MeasureScope.targetConstraints: VolumeConstraints
        get() {
            val maxWidth =
                if (maxWidth != Dp.Unspecified) {
                    maxWidth.roundToPx().coerceAtLeast(0)
                } else {
                    VolumeConstraints.INFINITY
                }
            val maxHeight =
                if (maxHeight != Dp.Unspecified) {
                    maxHeight.roundToPx().coerceAtLeast(0)
                } else {
                    VolumeConstraints.INFINITY
                }
            val maxDepth =
                if (maxDepth != Dp.Unspecified) {
                    maxDepth.roundToPx().coerceAtLeast(0)
                } else {
                    VolumeConstraints.INFINITY
                }
            val minWidth =
                if (minWidth != Dp.Unspecified) {
                    minWidth.roundToPx().coerceAtMost(maxWidth).coerceAtLeast(0).let {
                        if (it != VolumeConstraints.INFINITY) it else 0
                    }
                } else {
                    0
                }
            val minHeight =
                if (minHeight != Dp.Unspecified) {
                    minHeight.roundToPx().coerceAtMost(maxHeight).coerceAtLeast(0).let {
                        if (it != VolumeConstraints.INFINITY) it else 0
                    }
                } else {
                    0
                }
            val minDepth =
                if (minDepth != Dp.Unspecified) {
                    minDepth.roundToPx().coerceAtMost(maxDepth).coerceAtLeast(0).let {
                        if (it != VolumeConstraints.INFINITY) it else 0
                    }
                } else {
                    0
                }
            return VolumeConstraints(
                minWidth = minWidth,
                minHeight = minHeight,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
                minDepth = minDepth,
                maxDepth = maxDepth,
            )
        }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: VolumeConstraints,
    ): MeasureResult {
        val wrappedConstraints =
            targetConstraints.let {
                if (enforceIncoming) {
                    constraints.constrain(targetConstraints)
                } else {
                    val resolvedMinWidth =
                        if (minWidth != Dp.Unspecified) {
                            targetConstraints.minWidth
                        } else {
                            constraints.minWidth.coerceAtMost(targetConstraints.maxWidth)
                        }
                    val resolvedMaxWidth =
                        if (maxWidth != Dp.Unspecified) {
                            targetConstraints.maxWidth
                        } else {
                            constraints.maxWidth.coerceAtLeast(targetConstraints.minWidth)
                        }
                    val resolvedMinHeight =
                        if (minHeight != Dp.Unspecified) {
                            targetConstraints.minHeight
                        } else {
                            constraints.minHeight.coerceAtMost(targetConstraints.maxHeight)
                        }
                    val resolvedMaxHeight =
                        if (maxHeight != Dp.Unspecified) {
                            targetConstraints.maxHeight
                        } else {
                            constraints.maxHeight.coerceAtLeast(targetConstraints.minHeight)
                        }
                    val resolvedMinDepth =
                        if (minDepth != Dp.Unspecified) {
                            targetConstraints.minDepth
                        } else {
                            constraints.minDepth.coerceAtMost(targetConstraints.maxDepth)
                        }
                    val resolvedMaxDepth =
                        if (maxDepth != Dp.Unspecified) {
                            targetConstraints.maxDepth
                        } else {
                            constraints.maxDepth.coerceAtLeast(targetConstraints.minDepth)
                        }
                    VolumeConstraints(
                        resolvedMinWidth,
                        resolvedMaxWidth,
                        resolvedMinHeight,
                        resolvedMaxHeight,
                        resolvedMinDepth,
                        resolvedMaxDepth,
                    )
                }
            }

        val placeable = measurable.measure(wrappedConstraints)
        return layout(placeable.measuredWidth, placeable.measuredHeight, placeable.measuredDepth) {
            placeable.place(Pose())
        }
    }
}

internal enum class Direction {
    X,
    Y,
    Z,
    AllThree,
}
