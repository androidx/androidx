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

import androidx.xr.compose.subspace.layout.ParentLayoutParamsAdjustable
import androidx.xr.compose.subspace.layout.ParentLayoutParamsModifier
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement

internal class LayoutWeightElement(val weight: Float, val fill: Boolean) :
    SubspaceModifierNodeElement<LayoutWeightNode>() {
    override fun create(): LayoutWeightNode = LayoutWeightNode(weight = weight, fill = fill)

    override fun update(node: LayoutWeightNode) {
        node.weight = weight
        node.fill = fill
    }

    override fun hashCode(): Int {
        var result = weight.hashCode()
        result = 31 * result + fill.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? LayoutWeightElement ?: return false
        return weight == otherModifier.weight && fill == otherModifier.fill
    }
}

internal class LayoutWeightNode(var weight: Float, var fill: Boolean) :
    SubspaceModifier.Node(), ParentLayoutParamsModifier {
    override fun adjustParams(params: ParentLayoutParamsAdjustable) {
        if (params is RowColumnParentData) {
            params.weight = weight
            params.fill = fill
        }
    }
}

internal data class RowColumnParentData(var weight: Float = 0f, var fill: Boolean = true) :
    ParentLayoutParamsAdjustable

internal class RowColumnAlignElement(
    val horizontalSpatialAlignment: SpatialAlignment.Horizontal? = null,
    val verticalSpatialAlignment: SpatialAlignment.Vertical? = null,
    val depthSpatialAlignment: SpatialAlignment.Depth? = null,
) : SubspaceModifierNodeElement<RowColumnAlignNode>() {
    override fun create(): RowColumnAlignNode =
        RowColumnAlignNode(
            horizontalSpatialAlignment,
            verticalSpatialAlignment,
            depthSpatialAlignment,
        )

    override fun update(node: RowColumnAlignNode) {
        node.horizontalSpatialAlignment = horizontalSpatialAlignment
        node.verticalSpatialAlignment = verticalSpatialAlignment
        node.depthSpatialAlignment = depthSpatialAlignment
    }

    override fun hashCode(): Int {
        var result = horizontalSpatialAlignment.hashCode()
        result = 31 * result + verticalSpatialAlignment.hashCode()
        result = 31 * result + depthSpatialAlignment.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? RowColumnAlignElement ?: return false
        return horizontalSpatialAlignment == otherModifier.horizontalSpatialAlignment &&
            verticalSpatialAlignment == otherModifier.verticalSpatialAlignment &&
            depthSpatialAlignment == otherModifier.depthSpatialAlignment
    }
}

internal class RowColumnAlignNode(
    var horizontalSpatialAlignment: SpatialAlignment.Horizontal? = null,
    var verticalSpatialAlignment: SpatialAlignment.Vertical? = null,
    var depthSpatialAlignment: SpatialAlignment.Depth? = null,
) : SubspaceModifier.Node(), ParentLayoutParamsModifier {
    override fun adjustParams(params: ParentLayoutParamsAdjustable) {
        if (params !is RowColumnSpatialAlignmentParentData) return
        if (horizontalSpatialAlignment != null) {
            params.horizontalSpatialAlignment = horizontalSpatialAlignment
        }
        if (verticalSpatialAlignment != null) {
            params.verticalSpatialAlignment = verticalSpatialAlignment
        }
        if (depthSpatialAlignment != null) {
            params.depthSpatialAlignment = depthSpatialAlignment
        }
    }
}

internal data class RowColumnSpatialAlignmentParentData(
    var horizontalSpatialAlignment: SpatialAlignment.Horizontal? = null,
    var verticalSpatialAlignment: SpatialAlignment.Vertical? = null,
    var depthSpatialAlignment: SpatialAlignment.Depth? = null,
) : ParentLayoutParamsAdjustable
