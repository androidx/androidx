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
package androidx.ui.semantics

import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.SemanticsComponentNode
import androidx.ui.core.semantics.SemanticsConfiguration
import androidx.ui.core.semantics.SemanticsNode

@Composable
fun Semantics(
    /**
     * If 'container' is true, this component will introduce a new
     * node in the semantics tree. Otherwise, the semantics will be
     * merged with the semantics of any ancestors (the root of the tree is a container).
     */
    container: Boolean = false,
    /**
     * Whether the semantic information provided by the owning component and
     * all of its descendants should be treated as one logical entity.
     *
     * If set to true, the descendants of the owning component's
     * [SemanticsNode] will merge their semantic information into the
     * [SemanticsNode] representing the owning component.
     *
     * Setting this to true requires that [container] is also true.
     */
    mergeAllDescendants: Boolean = false,
    properties: (SemanticsPropertyReceiver.() -> Unit)? = null,
    children: @Composable() () -> Unit
) {
    require(!mergeAllDescendants || container) {
        "Attempting to set mergeAllDescendants to true on a configuration" +
                " that is not a semantic boundary (container must be true)"
    }
    val semanticsConfiguration = SemanticsConfiguration().also {
        it.isSemanticBoundary = container
        it.isMergingSemanticsOfDescendants = mergeAllDescendants

        properties?.invoke(it)
    }

    val id = remember { SemanticsNode.generateNewId() }

    SemanticsComponentNode(
        id = id,
        localSemanticsConfiguration = semanticsConfiguration
    ) {
        children()
    }
}
