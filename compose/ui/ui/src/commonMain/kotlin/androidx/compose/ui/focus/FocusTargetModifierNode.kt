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

import androidx.compose.ui.node.DelegatableNode
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
    fun requestFocus(): Boolean

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

/**
 * Create a [FocusTargetModifierNode] that can be delegated to in order to create a modifier that
 * makes a component focusable. Use a different instance of [FocusTargetModifierNode] for each
 * focusable component.
 */
@Deprecated(
    "Use the other overload with added parameters for focusability and onFocusChange",
    level = DeprecationLevel.HIDDEN
)
@JsName("funFocusTargetModifierNode")
fun FocusTargetModifierNode(): FocusTargetModifierNode = FocusTargetNode()

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
    onFocusChange: ((previous: FocusState, current: FocusState) -> Unit)? = null
): FocusTargetModifierNode =
    FocusTargetNode(focusability = focusability, onFocusChange = onFocusChange)
