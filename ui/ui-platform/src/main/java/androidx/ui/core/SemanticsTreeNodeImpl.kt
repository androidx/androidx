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

package androidx.ui.core

import androidx.ui.core.semantics.SemanticsNode

/**
 * Finds all [SemanticsNode]s under the given [rootNode].
 */
internal fun findAllSemanticNodesIn(rootNode: SemanticsNode): List<SemanticsNode> {
    // TODO(pavlis): Write some unit tests for this
    val nodes = mutableListOf<SemanticsNode>()
    findAllSemanticNodesInternal(currentNode = rootNode, nodes = nodes)
    return nodes
}

private fun findAllSemanticNodesInternal(
    currentNode: SemanticsNode,
    nodes: MutableList<SemanticsNode>
) {
    nodes.add(currentNode)

    for (child in currentNode.children) {
        findAllSemanticNodesInternal(child, nodes)
    }
}
