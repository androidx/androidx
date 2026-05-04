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

import androidx.ink.brush.InputToolType
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative
import java.util.Collections.unmodifiableSet

/**
 * A [ValueNode] for filtering out a branch of a behavior graph unless this stroke's tool type is in
 * the specified set.
 */
public class ToolTypeFilterNode
private constructor(
    nativePointer: Long,
    /** The input node that produces the value filtered if the tool type is not enabled. */
    public val input: ValueNode,
) : ValueNode(nativePointer, listOf(input)) {

    /**
     * Creates a [ToolTypeFilterNode] that filters out a branch of a behavior graph unless this
     * stroke's tool type is in the specified set.
     *
     * @param enabledToolTypes the set of tool types that should be enabled
     * @param input input node that produces the value filtered if the tool type is not enabled
     */
    public constructor(
        // The [enabledToolTypes] val below is a defensive copy of this parameter.
        enabledToolTypes: Set<InputToolType>,
        input: ValueNode,
    ) : this(
        ToolTypeFilterNodeNative.createToolTypeFilter(
            mouseEnabled = enabledToolTypes.contains(InputToolType.MOUSE),
            touchEnabled = enabledToolTypes.contains(InputToolType.TOUCH),
            stylusEnabled = enabledToolTypes.contains(InputToolType.STYLUS),
            unknownEnabled = enabledToolTypes.contains(InputToolType.UNKNOWN),
        ),
        input,
    )

    internal companion object {
        internal fun wrapNative(
            unownedNativePointer: Long,
            inputStack: ArrayDeque<ValueNode>,
        ): ToolTypeFilterNode =
            ToolTypeFilterNode(unownedNativePointer, input = inputStack.removeLast())
    }

    /** The set of tool types that should be enabled. */
    public val enabledToolTypes: Set<InputToolType> =
        unmodifiableSet(
            mutableSetOf<InputToolType>().apply {
                if (ToolTypeFilterNodeNative.getToolTypeFilterMouseEnabled(nativePointer)) {
                    add(InputToolType.MOUSE)
                }
                if (ToolTypeFilterNodeNative.getToolTypeFilterTouchEnabled(nativePointer)) {
                    add(InputToolType.TOUCH)
                }
                if (ToolTypeFilterNodeNative.getToolTypeFilterStylusEnabled(nativePointer)) {
                    add(InputToolType.STYLUS)
                }
                if (ToolTypeFilterNodeNative.getToolTypeFilterUnknownEnabled(nativePointer)) {
                    add(InputToolType.UNKNOWN)
                }
            }
        )

    override fun toString(): String = "ToolTypeFilterNode($enabledToolTypes, $input)"

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is ToolTypeFilterNode) return false
        if (other === this) return true
        return enabledToolTypes == other.enabledToolTypes && input == other.input
    }

    override fun hashCode(): Int {
        var result = enabledToolTypes.hashCode()
        result = 31 * result + input.hashCode()
        return result
    }
}

/**
 * Singleton wrapper for `BrushBehavior::TargetNode` native methods.
 *
 * Note that even though Kotlin [Node] is an abstract class with several subtypes,
 * [Node.nativePointer] all wrap the _same_ native type (a specialization of `std::variant`).
 */
@UsedByNative
private object ToolTypeFilterNodeNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative
    external fun createToolTypeFilter(
        mouseEnabled: Boolean,
        touchEnabled: Boolean,
        stylusEnabled: Boolean,
        unknownEnabled: Boolean,
    ): Long

    @UsedByNative external fun getToolTypeFilterMouseEnabled(nativePointer: Long): Boolean

    @UsedByNative external fun getToolTypeFilterTouchEnabled(nativePointer: Long): Boolean

    @UsedByNative external fun getToolTypeFilterStylusEnabled(nativePointer: Long): Boolean

    @UsedByNative external fun getToolTypeFilterUnknownEnabled(nativePointer: Long): Boolean
}
