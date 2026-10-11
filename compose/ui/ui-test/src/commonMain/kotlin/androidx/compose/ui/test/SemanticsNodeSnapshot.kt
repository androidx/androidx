/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastMap

/**
 * Everything that [printToString] prints about a [SemanticsNode] and its descendants, read from the
 * semantics tree when the snapshot is created.
 *
 * Once a node has left the tree, printing it loses most of this information, like its bounds, the
 * properties merged from its descendants, and its children and siblings. A snapshot still prints
 * the node as it was, for example to show the last known semantics of a node that went missing.
 *
 * @param node The node to read.
 * @param maxDepth Max depth of the descendants to read.
 */
internal class SemanticsNodeSnapshot(node: SemanticsNode, maxDepth: Int) {
    val id: Int = node.id

    /** The bounds of the node in the window, with no clipping applied. */
    val unclippedGlobalBounds: Rect = Rect(node.positionInWindow, node.size.toSize())

    /** The semantics properties of the node, as returned by [SemanticsNode.config]. */
    val config: SemanticsConfiguration = node.config

    /** Snapshots of the children of the node, or `null` if maxDepth was reached. */
    val children: List<SemanticsNodeSnapshot>? =
        if (maxDepth == 0) null
        else node.children.fastMap { SemanticsNodeSnapshot(it, maxDepth - 1) }

    /** The number of children of the node. */
    val childrenCount: Int = children?.size ?: node.children.size

    /** The number of siblings of the node. Only read if maxDepth was reached, else it is 0. */
    val siblingsCount: Int = if (children == null) (node.parent?.children?.size ?: 1) - 1 else 0
}
