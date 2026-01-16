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

package androidx.xr.glimmer.stack

import androidx.compose.ui.focus.FocusProperties
import androidx.compose.ui.focus.FocusPropertiesModifierNode
import androidx.compose.ui.focus.FocusTargetModifierNode
import androidx.compose.ui.focus.Focusability
import androidx.compose.ui.focus.requestFocusForChildInRootBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import kotlin.math.roundToInt

/** Directs the initial focus and focus re-enter to the current top item of the stack. */
internal class StackInitialFocusElement(private val stackState: StackState) :
    ModifierNodeElement<StackInitialFocusNode>() {

    override fun create(): StackInitialFocusNode = StackInitialFocusNode(stackState)

    override fun update(node: StackInitialFocusNode) {
        node.update(stackState)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StackInitialFocusElement) return false
        if (stackState != other.stackState) return false
        return true
    }

    override fun hashCode(): Int = stackState.hashCode()

    override fun InspectorInfo.inspectableProperties() {
        name = "stackInitialFocus"
        properties["stackState"] = stackState
    }
}

internal class StackInitialFocusNode(private var stackState: StackState) :
    DelegatingNode(), FocusPropertiesModifierNode {

    /**
     * This focus target is used to apply the focus properties to and to receive stack-level focus
     * change events.
     */
    private val focusTargetModifierNode =
        delegate(
            FocusTargetModifierNode(
                focusability = Focusability.Never,
                onFocusChange = { _, current ->
                    this@StackInitialFocusNode.stackState.onTopLevelFocusChanged(current)
                },
            )
        )

    override fun applyFocusProperties(focusProperties: FocusProperties) {
        focusProperties.onEnter = {
            val coordinates = requireLayoutCoordinates()
            val offset = coordinates.localToRoot(Offset.Zero)
            val left = offset.x.roundToInt()
            val top = offset.y.roundToInt()
            val size = coordinates.size
            requestFocusForChildInRootBounds(
                left = left,
                top = top,
                right = left + size.width,
                bottom = top + size.height,
            )
        }
    }

    fun update(stackState: StackState) {
        this.stackState = stackState
    }
}
