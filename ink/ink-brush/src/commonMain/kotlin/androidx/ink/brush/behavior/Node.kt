/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.brush.behavior

import androidx.annotation.RestrictTo
import androidx.ink.nativeloader.NativePointer

/**
 * Represents one node in a [androidx.ink.brush.BrushBehavior]'s expression graph. [Node] objects
 * are immutable and their inputs must be chosen at construction time; therefore, they can only ever
 * be assembled into an acyclic graph.
 */
public abstract class Node
internal constructor(
    nativeAlloc: () -> Long,
    /** The ordered list of inputs that this node directly depends on. */
    public val inputs: List<ValueNode>,
) {

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val nativePointer: Long by NativePointer(nativeAlloc, NodeNative::free)

    public companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun wrapNative(
            nativeAlloc: () -> Long,
            nodeType: Int,
            inputStack: ArrayDeque<ValueNode>,
        ): Node =
            when (nodeType) {
                0 -> SourceNode.wrapNative(nativeAlloc)
                1 -> ConstantNode.wrapNative(nativeAlloc)
                2 -> NoiseNode.wrapNative(nativeAlloc)
                3 -> ToolTypeFilterNode.wrapNative(nativeAlloc, inputStack)
                4 -> DampingNode.wrapNative(nativeAlloc, inputStack)
                5 -> ResponseNode.wrapNative(nativeAlloc, inputStack)
                6 -> IntegralNode.wrapNative(nativeAlloc, inputStack)
                7 -> BinaryOpNode.wrapNative(nativeAlloc, inputStack)
                8 -> InterpolationNode.wrapNative(nativeAlloc, inputStack)
                9 -> TargetNode.wrapNative(nativeAlloc, inputStack)
                10 -> PolarTargetNode.wrapNative(nativeAlloc, inputStack)
                else -> throw IllegalArgumentException("Unknown node type: ${nodeType}")
            }
    }
}

/**
 * Singleton wrapper for `BrushBehavior::Node` native methods.
 *
 * Note that even though Kotlin [Node] is an abstract class with several subtypes,
 * [Node.nativePointer] all wrap the _same_ native type (a specialization of `std::variant`).
 */
expect internal object NodeNative {
    fun free(nodeNativePointer: Long)
}
