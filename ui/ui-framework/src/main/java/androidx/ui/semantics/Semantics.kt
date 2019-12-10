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
import androidx.compose.ambient
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.core.DefaultTestTag
import androidx.ui.core.SemanticsComponentNode
import androidx.ui.core.TestTag
import androidx.ui.core.TestTagAmbient
import androidx.ui.core.semantics.SemanticsConfiguration
import androidx.ui.core.semantics.getOrNull

@Composable
fun Semantics(
    /**
     * If 'container' is true, this component will introduce a new
     * node in the semantics tree. Otherwise, the semantics will be
     * merged with the semantics of any ancestors.
     *
     * Whether descendants of this component can add their semantic information
     * to the [SemanticsNode] introduced by this configuration is controlled by
     * [explicitChildNodes].
     */
    container: Boolean = false,
    /**
     * Whether descendants of this component are allowed to add semantic
     * information to the [SemanticsNode] annotated by this composable.
     *
     * When set to false descendants are allowed to annotate [SemanticNode]s of
     * their parent with the semantic information they want to contribute to the
     * semantic tree.
     * When set to true the only way for descendants to contribute semantic
     * information to the semantic tree is to introduce new explicit
     * [SemanticNode]s to the tree.
     *
     * This setting is often used in combination with [isSemanticBoundary] to
     * create semantic boundaries that are either writable or not for children.
     */
    explicitChildNodes: Boolean = false,
    properties: (SemanticsPropertyReceiver.() -> Unit)? = null,
    children: @Composable() () -> Unit
) {
    val providedTestTag = +ambient(TestTagAmbient)
    // Memo ensures that we keep the same semantics node instance for this composable no matter
    // of changes we get. Thanks to this we can keep track of this composable in tests.
    val semanticsConfiguration = +memo { SemanticsConfiguration() }
    semanticsConfiguration.let {
        @Suppress("DEPRECATION")
        it.clear()
        properties?.invoke(it)
        // TODO(b/138173101): replace with the real thing
        it.testTag = it.getOrNull(SemanticsProperties.TestTag) ?: providedTestTag
    }

    SemanticsComponentNode(
        container = container,
        explicitChildNodes = explicitChildNodes,
        semanticsConfiguration = semanticsConfiguration
    ) {
        TestTag(tag = DefaultTestTag, children = children)
    }
}
