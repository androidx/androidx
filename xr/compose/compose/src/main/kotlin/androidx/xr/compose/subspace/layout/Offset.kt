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

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.xr.compose.subspace.node.SubspaceLayoutModifierNode
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.compose.subspace.node.invalidatePlacement
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3

/**
 * Offset the content by ([x] dp, [y] dp, [z] dp). The offsets can be positive as well as
 * non-positive.
 *
 * This modifier will automatically adjust the horizontal offset according to the layout direction:
 * when the layout direction is LTR, positive [x] offsets will move the content to the right and
 * when the layout direction is RTL, positive [x] offsets will move the content to the left. For a
 * modifier that offsets without considering layout direction, see [absoluteOffset].
 *
 * @see absoluteOffset
 */
public fun SubspaceModifier.offset(x: Dp = 0.dp, y: Dp = 0.dp, z: Dp = 0.dp): SubspaceModifier =
    this then SubspaceOffsetElement(x = x, y = y, z = z, rtlAware = true)

/**
 * Offset the content by ([x] dp, [y] dp, [z] dp) without considering layout direction. The offsets
 * can be positive as well as non-positive.
 *
 * This modifier will not consider layout direction when calculating the position of the content: a
 * positive [x] offset will always move the content to the right. For a modifier that considers the
 * layout direction when applying the offset, see [offset].
 *
 * @see offset
 */
public fun SubspaceModifier.absoluteOffset(
    x: Dp = 0.dp,
    y: Dp = 0.dp,
    z: Dp = 0.dp,
): SubspaceModifier = this then SubspaceOffsetElement(x = x, y = y, z = z, rtlAware = false)

private class SubspaceOffsetElement(val x: Dp, val y: Dp, val z: Dp, val rtlAware: Boolean) :
    SubspaceModifierNodeElement<OffsetNode>() {
    override fun create(): OffsetNode {
        return OffsetNode(x, y, z, rtlAware)
    }

    override fun update(node: OffsetNode) {
        node.update(x, y, z, rtlAware)
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        result = 31 * result + rtlAware.hashCode()

        return result
    }

    /*
     * TODO(b/475896820): Add unit tests for hashCode and equals for all *Element classes in
     *  SubspaceModifier APIs
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherElement = other as? OffsetNode ?: return false

        return x == otherElement.x &&
            y == otherElement.y &&
            z == otherElement.z &&
            rtlAware == otherElement.rtlAware
    }
}

private class OffsetNode(var x: Dp, var y: Dp, var z: Dp, var rtlAware: Boolean) :
    SubspaceLayoutModifierNode, SubspaceModifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    fun update(x: Dp, y: Dp, z: Dp, rtlAware: Boolean) {
        if (this.x != x || this.y != y || this.z != z || this.rtlAware != rtlAware)
            invalidatePlacement()
        this.x = x
        this.y = y
        this.z = z
        this.rtlAware = rtlAware
    }

    override fun SubspaceMeasureScope.measure(
        measurable: SubspaceMeasurable,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        val subspacePlaceable = measurable.measure(constraints)
        return layout(
            subspacePlaceable.measuredWidth,
            subspacePlaceable.measuredHeight,
            subspacePlaceable.measuredDepth,
        ) {
            val pose =
                Pose(
                    Vector3(
                        x.roundToPx().toFloat(),
                        y.roundToPx().toFloat(),
                        z.roundToPx().toFloat(),
                    )
                )
            if (rtlAware) {
                subspacePlaceable.placeRelative(pose)
            } else {
                subspacePlaceable.place(pose)
            }
        }
    }
}
