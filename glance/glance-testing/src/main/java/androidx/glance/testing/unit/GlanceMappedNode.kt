/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.glance.testing.unit

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.glance.Emittable
import androidx.glance.EmittableLazyItemWithChildren
import androidx.glance.EmittableWithChildren
import androidx.glance.testing.GlanceNode

/**
 * Hold a Glance composable node in a form that enables performing assertion on it.
 *
 * <p>[MappedNode]s are not rendered representations, but they map 1:1 to the composable nodes. They
 * enable faster testing of the logic of composing Glance composable tree as part of unit tests.
 */
class MappedNode internal constructor(
    @get:RestrictTo(Scope.LIBRARY_GROUP) val emittable: Emittable
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MappedNode) return false
        return emittable == other.emittable
    }

    override fun hashCode(): Int {
        val result = emittable.hashCode()
        return 31 * result
    }

    override fun toString(): String {
        return ("MappedNode{emittable='$emittable}'")
    }
}

/**
 * An implementation of [GlanceNode] node that uses [MappedNode] to perform assertions during
 * testing.
 */
class GlanceMappedNode(private val mappedNode: MappedNode) : GlanceNode<MappedNode>(mappedNode) {

    @RestrictTo(Scope.LIBRARY_GROUP)
    constructor(emittable: Emittable) : this(MappedNode(emittable))

    @RestrictTo(Scope.LIBRARY_GROUP)
    override fun children(): List<GlanceNode<MappedNode>> {
        val emittable = mappedNode.emittable
        if (emittable is EmittableWithChildren) {
            return emittable.toMappedNodes()
        }
        return emptyList()
    }

    private fun EmittableWithChildren.toMappedNodes(): List<GlanceMappedNode> {
        val mappedNodes = mutableListOf<GlanceMappedNode>()
        children.forEach { child ->
            if (child is EmittableLazyItemWithChildren) {
                mappedNodes.addAll(child.toMappedNodes())
            } else {
                mappedNodes.add(GlanceMappedNode(child))
            }
        }
        return mappedNodes.toList()
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    override fun toDebugString(): String {
        // TODO(b/201779038): map to a more readable format.
        return mappedNode.emittable.toString()
    }
}
