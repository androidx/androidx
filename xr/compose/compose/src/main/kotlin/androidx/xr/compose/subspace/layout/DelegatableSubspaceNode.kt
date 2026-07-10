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
 *
 * This interface is conceptually similar to [androidx.compose.ui.node.DelegatableNode] from 2D
 * Compose, but it intentionally exposes a minimal subset of functionality to meet current Jetpack
 * XR Compose requirements. This approach aims to reduce complexity and avoid prematurely adopting
 * concepts from 2D Compose that may not apply or may have different implications in a 3D context.
 *
 * For example, callbacks like `onDensityChange` or `onLayoutDirectionChange` found in the 2D
 * version are not currently included.
 *
 * This design choice follows guidance to tailor the API to the specific needs of XR and avoid
 * unnecessary inherited complexity (see b/483999028 for API review context).
 *
 * If additional capabilities from 2D Compose are needed in the future, they can potentially be
 * added to this interface. To maintain backward compatibility, such additions should provide
 * default implementations, avoiding breaking changes for external implementations. In the meantime,
 * users requiring more advanced node access can often utilize extension functions like
 * [requireCoordinator] and [requireOwner], or directly access properties on
 * [SubspaceModifier.Node].
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
