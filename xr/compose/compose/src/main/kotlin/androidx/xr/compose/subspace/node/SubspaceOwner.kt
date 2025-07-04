/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.subspace.node

/**
 * Owner interface that defines the connection to the underlying element system.
 *
 * On Android, this connects to Android [elements][androidx.xr.subspace.Element] and all layout,
 * draw, input, and accessibility is hooked through them.
 *
 * See [androidx.compose.ui.node.Owner]
 */
internal interface SubspaceOwner {
    /** The root layout node in the component tree. */
    public val root: SubspaceLayoutNode

    /**
     * Called by [SubspaceLayoutNode] when the node is attached to this owner's element system.
     *
     * This is used by [SubspaceOwner] to track which nodes are associated with it. It is only
     * called by a [node] that just got attached to this owner.
     */
    public fun onAttach(node: SubspaceLayoutNode)

    /**
     * Called by [SubspaceLayoutNode] when it is detached from the element system (for example
     * during [SubspaceLayoutNode.removeAt]).
     *
     * @param node the node that is being detached from this owner's element system.
     */
    public fun onDetach(node: SubspaceLayoutNode)

    /**
     * Called by [SubspaceLayoutNode] when it needs to be re-laid out due to a change that doesn't
     * trigger a recomposition.
     */
    public fun requestRelayout()
}
