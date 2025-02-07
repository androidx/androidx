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
import androidx.xr.compose.subspace.node.SubspaceModifierElement
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3

/**
 * Offset the content by ([x] dp, [y] dp, [z] dp). The offsets can be positive as well as
 * non-positive.
 */
public fun SubspaceModifier.offset(x: Dp = 0.dp, y: Dp = 0.dp, z: Dp = 0.dp): SubspaceModifier =
    this then SubspaceOffsetElement(x = x, y = y, z = z)

private class SubspaceOffsetElement(public val x: Dp, public val y: Dp, public val z: Dp) :
    SubspaceModifierElement<OffsetNode>() {
    override fun create(): OffsetNode {
        return OffsetNode(x, y, z)
    }

    override fun update(node: OffsetNode) {
        node.x = x
        node.y = y
        node.z = z
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherElement = other as? OffsetNode ?: return false

        return x == otherElement.x && y == otherElement.y && z == otherElement.z
    }
}

private class OffsetNode(public var x: Dp, public var y: Dp, public var z: Dp) :
    SubspaceLayoutModifierNode, SubspaceModifier.Node() {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: VolumeConstraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.measuredWidth, placeable.measuredHeight, placeable.measuredDepth) {
            placeable.place(
                Pose(
                    Vector3(
                        x.roundToPx().toFloat(),
                        y.roundToPx().toFloat(),
                        z.roundToPx().toFloat()
                    )
                )
            )
        }
    }
}
