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
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

/**
 * Represents one node in a [androidx.ink.brush.BrushBehavior]'s expression graph. [Node] objects
 * are immutable and their inputs must be chosen at construction time; therefore, they can only ever
 * be assembled into an acyclic graph.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
@ExperimentalInkCustomBrushApi
@Suppress("NotCloseable") // Finalize is only used to free the native peer.
public abstract class Node
internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val nativePointer: Long,
    /** The ordered list of inputs that this node directly depends on. */
    public val inputs: List<ValueNode>,
) {

    // NOMUTANTS -- Not tested post garbage collection.
    protected fun finalize() {
        // Note that the instance becomes finalizable at the conclusion of the Object constructor,
        // which in Kotlin is always before any non-default field initialization has been done by a
        // derived class constructor.
        if (nativePointer == 0L) return
        NodeNative.free(nativePointer)
    }

    public companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun wrapNative(unownedNativePointer: Long, inputStack: ArrayDeque<ValueNode>): Node =
            when (NodeNative.getNodeType(unownedNativePointer)) {
                0 -> SourceNode.wrapNative(unownedNativePointer)
                1 -> ConstantNode.wrapNative(unownedNativePointer)
                2 -> NoiseNode.wrapNative(unownedNativePointer)
                3 -> ToolTypeFilterNode.wrapNative(unownedNativePointer, inputStack)
                4 -> DampingNode.wrapNative(unownedNativePointer, inputStack)
                5 -> ResponseNode.wrapNative(unownedNativePointer, inputStack)
                6 -> IntegralNode.wrapNative(unownedNativePointer, inputStack)
                7 -> BinaryOpNode.wrapNative(unownedNativePointer, inputStack)
                8 -> InterpolationNode.wrapNative(unownedNativePointer, inputStack)
                9 -> TargetNode.wrapNative(unownedNativePointer, inputStack)
                10 -> PolarTargetNode.wrapNative(unownedNativePointer, inputStack)
                else ->
                    throw IllegalArgumentException(
                        "Unknown node type: ${NodeNative.getNodeType(unownedNativePointer)}"
                    )
            }
    }
}

/**
 * Singleton wrapper for `BrushBehavior::Node` native methods.
 *
 * Note that even though Kotlin [Node] is an abstract class with several subtypes,
 * [Node.nativePointer] all wrap the _same_ native type (a specialization of `std::variant`).
 */
@OptIn(ExperimentalInkCustomBrushApi::class)
@UsedByNative
private object NodeNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative external fun free(nodeNativePointer: Long)

    @UsedByNative external fun getNodeType(nodeNativePointer: Long): Int
}
