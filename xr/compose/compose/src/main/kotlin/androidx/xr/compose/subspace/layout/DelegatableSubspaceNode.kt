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

package androidx.xr.compose.subspace.layout

import androidx.xr.compose.subspace.node.SubspaceLayoutModifierNodeCoordinator
import androidx.xr.compose.subspace.node.SubspaceOwner

/**
 * Represents a [SubspaceModifier.Node] which can be a delegate of another [SubspaceModifier.Node].
 * Since [SubspaceModifier.Node] implements this interface, in practice any [SubspaceModifier.Node]
 * can be delegated.
 */
public interface DelegatableSubspaceNode {
    /**
     * A reference of the [SubspaceModifier.Node] that holds this node's position in the node
     * hierarchy. If the node is a delegate of another node, this will point to the root delegating
     * node that is actually part of the node tree. Otherwise, this will point to itself.
     */
    public val node: SubspaceModifier.Node
}

/**
 * Returns the [SubspaceLayoutModifierNodeCoordinator] associated with this
 * [DelegatableSubspaceNode].
 *
 * This is used to traverse the modifier node tree to find the correct [SubspaceLayoutCoordinates]
 * for a given [DelegatableSubspaceNode].
 */
internal fun DelegatableSubspaceNode.requireCoordinator(): SubspaceLayoutModifierNodeCoordinator =
    requireNotNull(node.coordinator) {
        "No SubspaceLayoutModifierNodeCoordinator available for $this. Ensure the node is attached."
    }

/**
 * This is used to access the root layout [SubspaceOwner] for a given [DelegatableSubspaceNode].
 *
 * Requires a [SubspaceOwner] for the node, throwing an error if the node is not attached.
 */
internal fun DelegatableSubspaceNode.requireOwner(): SubspaceOwner {
    return this.node.layoutNode?.owner
        ?: throw IllegalStateException("SubspaceModifier.Node is not attached.")
}
