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

package androidx.xr.compose.subspace.layout

import androidx.annotation.FloatRange
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastRoundToInt
import androidx.xr.compose.subspace.node.SubspaceLayoutModifierNode
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.math.Pose

/**
 * Attempts to size the content to match a specified aspect ratio by trying to match one of the
 * incoming constraints in the following order: [VolumeConstraints.maxWidth],
 * [VolumeConstraints.maxHeight], [VolumeConstraints.minWidth], [VolumeConstraints.minHeight] if
 * [matchHeightConstraintsFirst] is `false` (which is the default), or
 * [VolumeConstraints.maxHeight], [VolumeConstraints.maxWidth], [VolumeConstraints.minHeight],
 * [VolumeConstraints.minWidth] if [matchHeightConstraintsFirst] is `true`. The size in the other
 * dimension is determined by the aspect ratio. The combinations will be tried in this order until
 * one non-empty is found to satisfy the constraints. If no valid size is obtained this way, it
 * means that there is no non-empty size satisfying both the constraints and the aspect ratio, so
 * the constraints will not be respected and the content will be sized such that the
 * [VolumeConstraints.maxWidth] or [VolumeConstraints.maxHeight] is matched (depending on
 * [matchHeightConstraintsFirst]). Note that this modifier constrains the ratio between the
 * content's width and height only. The depth dimension is not affected or constrained by this
 * aspect ratio modifier.
 *
 * Example usage:
 * ```kotlin
 * SpatialPanel(SubspaceModifier.width(100.dp).aspectRatio(16f / 9f)) {
 *     Text(text = "Inner Composable Content")
 * }
 * ```
 *
 * @param ratio the desired width/height positive ratio
 * @param matchHeightConstraintsFirst if true, height constraints will be matched before width
 *   constraints and used to calculate the resulting size according to [ratio]
 */
public fun SubspaceModifier.aspectRatio(
    @FloatRange(from = 0.0, fromInclusive = false) ratio: Float,
    matchHeightConstraintsFirst: Boolean = false,
): SubspaceModifier = this.then(AspectRatioElement(ratio, matchHeightConstraintsFirst))

private class AspectRatioElement(val aspectRatio: Float, val matchHeightConstraintsFirst: Boolean) :
    SubspaceModifierNodeElement<AspectRatioNode>() {
    init {
        require(aspectRatio.isFinite()) { "aspectRatio $aspectRatio must be finite" }
        require(aspectRatio > 0) { "aspectRatio $aspectRatio must be > 0" }
    }

    override fun create(): AspectRatioNode {
        return AspectRatioNode(aspectRatio, matchHeightConstraintsFirst)
    }

    override fun update(node: AspectRatioNode) {
        node.aspectRatio = aspectRatio
        node.matchHeightConstraintsFirst = matchHeightConstraintsFirst
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? AspectRatioElement ?: return false
        return aspectRatio == otherModifier.aspectRatio &&
            matchHeightConstraintsFirst == other.matchHeightConstraintsFirst
    }

    override fun hashCode(): Int =
        aspectRatio.hashCode() * 31 + matchHeightConstraintsFirst.hashCode()
}

private class AspectRatioNode(var aspectRatio: Float, var matchHeightConstraintsFirst: Boolean) :
    SubspaceLayoutModifierNode, SubspaceModifier.Node() {
    override fun SubspaceMeasureScope.measure(
        measurable: SubspaceMeasurable,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        val size = constraints.findSize()
        val wrappedConstraints =
            if (size != IntSize.Zero) {
                VolumeConstraints(
                    minWidth = size.width,
                    maxWidth = size.width,
                    minHeight = size.height,
                    maxHeight = size.height,
                    minDepth = constraints.minDepth,
                    maxDepth = constraints.maxDepth,
                )
            } else {
                constraints
            }
        val placeable = measurable.measure(wrappedConstraints)
        return layout(placeable.measuredWidth, placeable.measuredHeight, placeable.measuredDepth) {
            placeable.place(Pose.Identity)
        }
    }

    private fun VolumeConstraints.findSize(): IntSize {
        return if (!matchHeightConstraintsFirst) {
            tryMaxWidth(enforceConstraints = true)
                ?: tryMaxHeight(enforceConstraints = true)
                ?: tryMinWidth(enforceConstraints = true)
                ?: tryMinHeight(enforceConstraints = true)
                ?: tryMaxWidth(enforceConstraints = false)
                ?: tryMaxHeight(enforceConstraints = false)
                ?: tryMinWidth(enforceConstraints = false)
                ?: tryMinHeight(enforceConstraints = false)
                ?: IntSize.Zero
        } else {
            tryMaxHeight(enforceConstraints = true)
                ?: tryMaxWidth(enforceConstraints = true)
                ?: tryMinHeight(enforceConstraints = true)
                ?: tryMinWidth(enforceConstraints = true)
                ?: tryMaxHeight(enforceConstraints = false)
                ?: tryMaxWidth(enforceConstraints = false)
                ?: tryMinHeight(enforceConstraints = false)
                ?: tryMinWidth(enforceConstraints = false)
                ?: IntSize.Zero
        }
    }

    private fun VolumeConstraints.tryMaxWidth(enforceConstraints: Boolean): IntSize? {
        val maxWidth = this.maxWidth
        if (maxWidth != VolumeConstraints.INFINITY) {
            val height = (maxWidth / aspectRatio).fastRoundToInt()
            if (height > 0) {
                if (!enforceConstraints || isSatisfiedBy(maxWidth, height)) {
                    return IntSize(maxWidth, height)
                }
            }
        }
        return null
    }

    private fun VolumeConstraints.tryMaxHeight(enforceConstraints: Boolean): IntSize? {
        val maxHeight = this.maxHeight
        if (maxHeight != VolumeConstraints.INFINITY) {
            val width = (maxHeight * aspectRatio).fastRoundToInt()
            if (width > 0) {
                if (!enforceConstraints || isSatisfiedBy(width, maxHeight)) {
                    return IntSize(width, maxHeight)
                }
            }
        }
        return null
    }

    private fun VolumeConstraints.tryMinWidth(enforceConstraints: Boolean): IntSize? {
        val minWidth = this.minWidth
        val height = (minWidth / aspectRatio).fastRoundToInt()
        if (height > 0) {
            if (!enforceConstraints || isSatisfiedBy(minWidth, height)) {
                return IntSize(minWidth, height)
            }
        }
        return null
    }

    private fun VolumeConstraints.tryMinHeight(enforceConstraints: Boolean): IntSize? {
        val minHeight = this.minHeight
        val width = (minHeight * aspectRatio).fastRoundToInt()
        if (width > 0) {
            if (!enforceConstraints || isSatisfiedBy(width, minHeight)) {
                return IntSize(width, minHeight)
            }
        }
        return null
    }
}

/** Takes a size and returns whether it satisfies the current constraints. */
internal fun VolumeConstraints.isSatisfiedBy(width: Int, height: Int): Boolean {
    return width in minWidth..maxWidth && height in minHeight..maxHeight
}
