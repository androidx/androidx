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

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.compose.subspace.node.SubspaceSemanticsModifierNode

/**
 * Add semantics key/value pairs to the layout node, for use in testing, accessibility, etc.
 *
 * @param properties Builder block where the semantics properties are defined.
 */
@JvmName("semanticsSubspace")
public fun SubspaceModifier.semantics(
    properties: (SubspaceSemanticsPropertyReceiver.() -> Unit)
): SubspaceModifier = this then AppendedSemanticsElement(properties = properties)

/**
 * Add semantics key/value pairs to the layout node, for use in testing, accessibility, etc.
 *
 * @param properties Builder block where the semantics properties are defined.
 */
// TODO(b/518020831): Remove deprecated semantics modifier.
@Deprecated(
    message = "Replaced by semantics that takes SubspaceSemanticsPropertyReceiver",
    level = DeprecationLevel.HIDDEN,
)
public fun SubspaceModifier.semantics(
    properties: (SemanticsPropertyReceiver.() -> Unit)
): SubspaceModifier =
    this then
        AppendedSemanticsElement(
            properties = {
                val subspaceReceiver: SubspaceSemanticsPropertyReceiver = this
                val receiver: SemanticsPropertyReceiver =
                    object : SemanticsPropertyReceiver {
                        override operator fun <T> set(key: SemanticsPropertyKey<T>, value: T) {
                            subspaceReceiver.set(key = key, value = value)
                        }
                    }
                receiver.properties()
            }
        )

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
