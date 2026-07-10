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

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidatePlacement
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.unit.Constraints

/**
 * A [Modifier] that controls the visibility of the Layout it is applied to. When `visible` is
 * `false`, the element will not be placed and thus will not be drawn or be interactable, but it
 * will still be measured and take up space. It will also be invisible to accessibility services.
 *
 * This is similar to the `View.INVISIBLE` visibility in the classic Android View system.
 *
 * Note that [Modifier.visible] is not suitable for managing the visibility of a composable involved
 * in a
 * [androidx.compose.animation.SharedTransitionScope.sharedElement]/[androidx.compose.animation.SharedTransitionScope.sharedBounds]
 * transition because it prevents placement.
 *
 * @sample androidx.compose.foundation.layout.samples.VisibleModifierSample
 * @param visible `true` to make the component visible, `false` to hide it.
 */
@Stable fun Modifier.visible(visible: Boolean): Modifier = this.then(VisibilityElement(visible))

private class VisibilityElement(private val visible: Boolean) :
    ModifierNodeElement<VisibilityNode>() {
    override fun create(): VisibilityNode {
        return VisibilityNode(visible)
    }

    override fun update(node: VisibilityNode) {
        if (node.visible != visible) {
            node.visible = visible
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "Visible"
        properties["Visible"] = visible
    }

    override fun hashCode(): Int {
        return visible.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        val otherModifier = other as? VisibilityElement ?: return false
        return this.visible == otherModifier.visible
    }
}

private class VisibilityNode(visible: Boolean) :
    LayoutModifierNode, SemanticsModifierNode, Modifier.Node() {

    var visible: Boolean = visible
        set(value) {
            if (field != value) {
                field = value
                if (!value) {
                    invalidatePlacement()
                    invalidateSemantics()
                }
            }
        }

    val Visible = SemanticsPropertyKey<Boolean>("Visible")

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)

        return layout(placeable.width, placeable.height) {
            if (visible) {
                placeable.place(0, 0)
            } else {
                // skip the placement
            }
        }
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        this[Visible] = visible
        if (!visible) {
            focused = false
        }
    }
}
