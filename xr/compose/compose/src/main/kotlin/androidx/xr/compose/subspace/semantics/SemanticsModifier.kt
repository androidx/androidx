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

package androidx.xr.compose.subspace.semantics

import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.compose.subspace.node.SubspaceSemanticsModifierNode

/**
 * Add semantics key/value pairs to the layout node, for use in testing, accessibility, etc.
 *
 * **Mental Model (Picture Frame vs. Canvas):** When building a combined UI in Compose for XR, think
 * of a Subspace node (such as `SpatialPanel`) as a "Picture Frame" existing in 3D space, and the
 * standard 2D Compose UI elements inside it as the "Canvas".
 * - Use [SubspaceModifier.semantics] on the 3D container (the frame) to provide spatial properties
 *   (such as [testTag] and [contentDescription]) for 3D placement, anchoring, or testing.
 * - Use standard [androidx.compose.ui.semantics.semantics] modifiers on the 2D Compose composables
 *   (the canvas) for fine-grained user interactions and TalkBack accessibility.
 *
 * **Interop & Merging Guidance:** The 3D Subspace semantics tree and the 2D foundational semantics
 * tree operate as distinct hierarchies. Spatial containers do not support merging descendant
 * semantics (`mergeDescendants = true`).
 *
 * @sample androidx.xr.compose.samples.SubspaceSemanticsModifierSample
 * @param properties Builder block where the semantics properties are defined.
 */
@JvmName("semanticsSubspace")
public fun SubspaceModifier.semantics(
    properties: (SubspaceSemanticsPropertyReceiver.() -> Unit)
): SubspaceModifier = this then AppendedSemanticsElement(properties = properties)

private class AppendedSemanticsElement(
    private val properties: (SubspaceSemanticsPropertyReceiver.() -> Unit)
) : SubspaceModifierNodeElement<SemanticsModifierNode>() {

    override fun create(): SemanticsModifierNode {
        return SemanticsModifierNode(properties = properties)
    }

    override fun update(node: SemanticsModifierNode) {
        node.properties = properties
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppendedSemanticsElement) return false
        return properties === other.properties
    }

    override fun hashCode(): Int {
        return properties.hashCode()
    }
}

private class SemanticsModifierNode(
    public var properties: SubspaceSemanticsPropertyReceiver.() -> Unit
) : SubspaceModifier.Node(), SubspaceSemanticsModifierNode {
    override fun SubspaceSemanticsPropertyReceiver.applySemantics() {
        properties()
    }
}
