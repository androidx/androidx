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

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.xr.compose.subspace.node.SubspaceLayoutNode

/**
 * The receiver scope of a layout's measure lambda. The return value of the measure lambda is
 * [SubspaceMeasureResult], which should be returned by [layout]
 *
 * Based on [androidx.compose.ui.layout.MeasureScope].
 */
public interface SubspaceMeasureScope : Density {
    /**
     * The [LayoutDirection] of the `Layout` or `LayoutModifier` using the measure scope to measure
     * their children.
     */
    public val layoutDirection: LayoutDirection

    /**
     * Sets the size and alignment lines of the measured layout, as well as the positioning block
     * that defines the children positioning logic. The [placementBlock] is a lambda used for
     * positioning children. [SubspacePlaceable.placeAt] should be called on children inside
     * placementBlock.
     *
     * @param width the measured width of the layout, in pixels
     * @param height the measured height of the layout, in pixels
     * @param depth the measured depth of the layout, in pixels
     * @param placementBlock block defining the children positioning of the current layout
     */
    public fun layout(
        width: Int,
        height: Int,
        depth: Int,
        placementBlock: SubspacePlaceable.SubspacePlacementScope.() -> Unit,
    ): SubspaceMeasureResult {
        return object : SubspaceMeasureResult {
            override val width = width
            override val height = height
            override val depth = depth

            override fun placeChildren(placementScope: SubspacePlaceable.SubspacePlacementScope) {
                placementScope.placementBlock()
            }
        }
    }
}

internal class LayoutSubspaceMeasureScope(private val layoutNode: SubspaceLayoutNode) :
    SubspaceMeasureScope {
    override val density: Float
        get() = layoutNode.density.density

    override val fontScale: Float
        get() = layoutNode.density.fontScale

    override val layoutDirection: LayoutDirection
        get() = layoutNode.layoutDirection
}
