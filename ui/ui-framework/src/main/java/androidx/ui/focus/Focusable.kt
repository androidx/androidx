/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.focus

import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.compose.remember
import androidx.ui.core.FocusNode
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.Ref
import androidx.ui.core.focus.initializeFocusState
import androidx.ui.focus.FocusDetailedState.Inactive

private val focusNotCreated = "Focus node could not be created."

/**
 * This composable can be used to create components that are Focusable. A component that is focused
 * receives any invoked actions. Some examples of actions are 'paste' (receiving the
 * contents of the clipboard), or receiving text from the keyboard.
 *
 * [Focusable] components have access to the current focus state. The children of a [Focusable]
 * have access to this focus state during composition.
 *
 * [focusOperator] : This object is returned in the receiver scope of the components
 * passed as [children]. You should not specify this parameter unless you want to hoist the
 * focusOperator so that you can control the focusable from outside the scope of its children.
 *
 * [children]: This is a composable block called with [focusOperator] in its receiver scope.
 * Children can use [FocusOperator.focusState] for conditional composition.
 *
 */
@Composable
fun Focusable(
    focusOperator: FocusOperator = remember { FocusOperator() },
    children: @Composable() (FocusOperator) -> Unit
) {
    // TODO (b/144897112): Remove manual recomposition.
    Recompose { recompose ->

        val focusNodeRef = Ref<FocusNode>()
        FocusNode(recompose = recompose, ref = focusNodeRef) {

            val focusNode = (focusNodeRef.value ?: error(focusNotCreated))

            focusOperator.focusNode = focusNode

            // Set the focusNode coordinates when the composable is positioned. Also, if this is
            // the focus root and the host view is in focus, request focus for this node.
            OnChildPositioned(
                onPositioned = {
                    focusNode.layoutCoordinates = it
                    if (focusNode.focusState == Inactive) {
                        focusNode.initializeFocusState()
                    }
                }, children = {
                    children(focusOperator)
                })
        }
    }
}

/**
 * The [FocusOperator] is returned in the receiver scope of the children of a [Focusable]. It
 * access to focus APIs pertaining to the [Focusable].
 */
class FocusOperator {
    /**
     * The [FocusNode] associated with this [FocusOperator].
     *
     * @throws UninitializedPropertyAccessException if this [FocusOperator] has no associated
     * [FocusNode].
     */
    internal lateinit var focusNode: FocusNode

    /**
     * A more detailed focus state of the [Focusable] associated with this [FocusOperator]. For a
     * smaller subset of states, use [focusState].
     */
    val focusDetailedState: FocusDetailedState get() = focusNode.focusState

    /**
     * The current focus state of the [Focusable] associated with this [FocusOperator]. For more
     * detailed focus state information, use [focusDetailedState].
     */
    val focusState: FocusState get() = focusDetailedState.focusState()

    /**
     * Request focus for the [Focusable] associated with this [FocusOperator].
     */
    fun requestFocus() = focusNode.requestFocus()
}