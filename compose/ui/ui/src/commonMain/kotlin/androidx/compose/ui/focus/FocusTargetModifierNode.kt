/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.focus

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.unit.toSize
import kotlin.js.JsName

/**
 * This modifier node can be delegated to in order to create a modifier that makes a component
 * focusable.
 */
sealed interface FocusTargetModifierNode : DelegatableNode {
    /**
     * The [FocusState] associated with this [FocusTargetModifierNode]. When you delegate to a
     * [FocusTargetModifierNode], instead of implementing [FocusEventModifierNode], you can get the
     * state by accessing this variable.
     */
    val focusState: FocusState

    /**
     * Request focus for this node.
     *
     * @return true if focus was successfully requested
     */
    @Deprecated(
        message = "Use the version accepting FocusDirection",
        replaceWith = ReplaceWith("this.requestFocus()"),
        level = DeprecationLevel.HIDDEN,
    )
    fun requestFocus(): Boolean

    /**
     * Request focus for this node.
     *
     * @param focusDirection The direction from which the focus is being requested
     * @return true if focus was successfully requested
     */
    fun requestFocus(focusDirection: FocusDirection = FocusDirection.Enter): Boolean

    /**
     * The [Focusability] for this node.
     *
     * Note that parent [FocusPropertiesModifierNode]s that set [FocusProperties.canFocus] take
     * priority over this property.
     *
     * If the current focus state would be affected by a new focusability, focus will be invalidated
     * as needed.
     */
    var focusability: Focusability
}

// Before aosp/3296711 we would calculate semantics configuration lazily. The focusable
// implementation used to call invalidateSemantics() and then change focus state. However, now that
// we are calculating semantics configuration eagerly, the old implementation of focusable would
// end up calculating semantics configuration before the local copy of focus state is updated.
// To fix this, we added an extra invalidateSemantics() call for the deprecated
// [FocusTargetModifierNode].
private object InvalidateSemantics {
    fun onDispatchEventsCompleted(focusTargetNode: FocusTargetNode) {
        (focusTargetNode.node as? SemanticsModifierNode)?.invalidateSemantics()
    }
}

/**
 * Create a [FocusTargetModifierNode] that can be delegated to in order to create a modifier that
 * makes a component focusable. Use a different instance of [FocusTargetModifierNode] for each
 * focusable component.
 */
@Deprecated(
    "Use the other overload with added parameters for focusability and onFocusChange",
    level = DeprecationLevel.HIDDEN,
)
@JsName("funFocusTargetModifierNode")
fun FocusTargetModifierNode(): FocusTargetModifierNode =
    FocusTargetNode(onDispatchEventsCompleted = InvalidateSemantics::onDispatchEventsCompleted)

/**
 * Create a [FocusTargetModifierNode] that can be delegated to in order to create a modifier that
 * makes a component focusable. Use a different instance of [FocusTargetModifierNode] for each
 * focusable component.
 *
 * @param focusability the [Focusability] that configures focusability for this node
 * @param onFocusChange a callback invoked when the [FocusTargetModifierNode.focusState] changes,
 *   providing the previous state that it changed from, and the current focus state. Note that this
 *   will be invoked if the node is losing focus due to being detached from the hierarchy, but
 *   before the node is marked as detached (node.isAttached will still be true).
 */
@JsName("funFocusTargetModifierNode2")
fun FocusTargetModifierNode(
    focusability: Focusability = Focusability.Always,
    onFocusChange: ((previous: FocusState, current: FocusState) -> Unit)? = null,
): FocusTargetModifierNode =
    FocusTargetNode(focusability = focusability, onFocusChange = onFocusChange)

/**
 * Calculates the rectangular area in this node's coordinates that corresponds to the focus area of
 * the focused node under this [FocusTargetModifierNode], including itself.
 *
 * This function returns `null` when;
 * - This node is not focused and there is no focused descendant.
 * - This node is detached from the composition hierarchy.
 */
fun FocusTargetModifierNode.getFocusedRect(): Rect? {
    if (!node.isAttached) return null
    // Reading focusState includes traversal and computation. We shouldn't do it twice.
    val currentFocusState = focusState
    // If there is nothing focused under this node, then we have no focus area.
    if (!currentFocusState.hasFocus) return null
    // Special case where the node itself is focused, not a descendant
    if (currentFocusState.isFocused)
        return Rect(Offset.Zero, requireLayoutCoordinates().size.toSize())

    // The focused item is guaranteed to be under this node since we have focus.
    val focusedChildCoordinates =
        requireOwner().focusOwner.activeFocusTargetNode?.requireLayoutCoordinates() ?: return null
    return requireLayoutCoordinates().localBoundingBoxOf(focusedChildCoordinates, false)
}
