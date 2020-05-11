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

package androidx.ui.core.focus

import androidx.compose.Composable

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
 * Children can use FocusOperator.focusState for conditional composition.
 *
 */
@Suppress("UNUSED_PARAMETER")
@Deprecated(
    message = "Focusable is deprecated. Use androidx.ui.core.focus.FocusModifier instead.",
    level = DeprecationLevel.ERROR
)
@Composable
fun Focusable(focusOperator: Any, children: @Composable (Any) -> Unit) {
}

/**
 * The [FocusOperator] is returned in the receiver scope of the children of a [Focusable]. It
 * access to focus APIs pertaining to the [Focusable].
 *
 * TODO(b/154633015): Deprecated in Dev11. Delete for Dev12.
 */
@Deprecated(
    message = "FocusOperator is used along with a Focusable. Focusable is deprecated in favor of " +
            "androidx.ui.core.focus.FocusModifier.",
    level = DeprecationLevel.ERROR
)
class FocusOperator