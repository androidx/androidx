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

package androidx.ui.framework.demos

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer

/**
 * This is a level 2 API. This component makes it easier to find/discover available properties.
 */
@Composable
fun SemanticProperties(
    label: String = "",
    visibility: Visibility = Visibility.Undefined,
    actions: Set<SemanticAction<out Any?>> = setOf(),
    @Children children: @Composable() () -> Unit
) {
    val propertySet = mutableSetOf<SemanticProperty<out Any>>()

    if (!label.isEmpty()) {
        propertySet.add(Label(label))
    }

    if (visibility != Visibility.Undefined) {
        propertySet.add(visibility)
    }

    Semantics(properties = propertySet, actions = actions) { children() }
}

/**
 * This is a component that emits a semantic node with a single action.
 *
 * Since this example does not have node merging implemented, we just creates an semantic action
 * using the supplied parameters and then invokes the supplied lambda.
 *
 * SemanticAction(params) { semanticAction->
 * ...
 * }
 *
 * For now the [Properties] component accepts a set of actions, but once this is finally
 * implemented, we will merge the nodes automatically.
 */
@Composable
fun <T> SemanticAction(
    phrase: String = "",
    defaultParam: T,
    types: Set<ActionType> = setOf(),
    action: (ActionParam<T>) -> Unit,
    @Children block: @Composable() (SemanticAction<T>) -> Unit
) {
    val semanticAction = SemanticAction<T>(phrase, defaultParam, types, action)
    block.invoke(semanticAction)
}
