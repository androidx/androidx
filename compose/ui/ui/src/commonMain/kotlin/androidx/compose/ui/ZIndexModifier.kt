/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints

/**
 * Creates a modifier that controls the drawing order for the children of the same layout parent.
 * A child with larger [zIndex] will be drawn on top of all the children with smaller [zIndex].
 * When children have the same [zIndex] the original order in which the parent placed the
 * children is used.
 *
 * Note that if there would be multiple [zIndex] modifiers applied for the same layout
 * the sum of their values will be used as the final zIndex. If no [zIndex] were applied for the
 * layout then the default zIndex is 0.
 *
 * @sample androidx.compose.ui.samples.ZIndexModifierSample
 */
@Stable
fun Modifier.zIndex(zIndex: Float): Modifier = this then ZIndexElement(zIndex = zIndex)

internal data class ZIndexElement(val zIndex: Float) : ModifierNodeElement<ZIndexNode>() {
    override fun create() = ZIndexNode(zIndex)
    override fun update(node: ZIndexNode) {
        node.zIndex = zIndex
    }
    override fun InspectorInfo.inspectableProperties() {
        name = "zIndex"
        properties["zIndex"] = zIndex
    }
}

internal class ZIndexNode(var zIndex: Float) : LayoutModifierNode, Modifier.Node() {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0, zIndex = zIndex)
        }
    }

    override fun toString(): String = "ZIndexModifier(zIndex=$zIndex)"
}
