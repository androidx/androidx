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

package androidx.xr.glimmer.list

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusTargetModifierNode
import androidx.compose.ui.focus.Focusability
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints

/** Read the auto focus value from [behaviour] and apply it to children after the layout pass. */
internal fun Modifier.autoFocus(behaviour: GlimmerListAutoFocusBehaviour): Modifier =
    this then GlimmerListAutoFocusNodeElement(behaviour)

private class GlimmerListAutoFocusNodeElement(
    private val behaviour: GlimmerListAutoFocusBehaviour
) : ModifierNodeElement<GlimmerListAutoFocusNode>() {

    override fun create(): GlimmerListAutoFocusNode {
        return GlimmerListAutoFocusNode(behaviour)
    }

    override fun update(node: GlimmerListAutoFocusNode) {
        node.update(behaviour)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "autoFocus"
        properties["behaviour"] = behaviour
    }

    override fun hashCode(): Int {
        return behaviour.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GlimmerListAutoFocusNodeElement) return false

        return behaviour == other.behaviour
    }
}

private class GlimmerListAutoFocusNode(private var behaviour: GlimmerListAutoFocusBehaviour) :
    DelegatingNode(), LayoutModifierNode {

    private val focusTargetModifierNode =
        delegate(FocusTargetModifierNode(focusability = Focusability.Never))

    fun update(behaviour: GlimmerListAutoFocusBehaviour) {
        this.behaviour = behaviour
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
            notifyAutoFocus()
        }
    }

    private fun notifyAutoFocus() {
        // A list should only request the focus for its child if the focus already belongs to the
        // list. Otherwise, the list will "steal" the focus from other elements.
        if (focusTargetModifierNode.focusState.hasFocus) {
            // The focus should only be requested once all of the node's children have been laid
            // out. We don't have a dedicated callback for `onAfterLayout` or `onPreDraw` yet. So
            // far, we've been using the `onGloballyPositioned` callback for that purpose. If a
            // better callback is introduced, we should replace it.
            behaviour.onAfterLayout(this)
        }
    }
}
