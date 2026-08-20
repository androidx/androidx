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

import androidx.ink.nativeloader.InkInternalOnlyApi

/**
 * A [ValueNode] that damps changes in an input value, causing the output value to slowly follow
 * changes in the input value over a specified time or distance.
 */
@OptIn(InkInternalOnlyApi::class)
public class DampingNode
private constructor(
    nativeAlloc: () -> Long,
    /** The input node that produces the value to be modified by the damping. */
    public val input: ValueNode,
) : ValueNode(nativeAlloc, listOf(input)) {

    /**
     * Creates a [DampingNode] that damps changes in an input value, causing the output value to
     * slowly follow changes in the input value over a specified time or distance.
     *
     * If [dampingSource] is [ProgressDomain.DISTANCE_IN_CENTIMETERS] and the stroke input data does
     * not indicate the relationship between stroke units and physical units (e.g. as may be the
     * case for programmatically-generated inputs), then the output value will be null regardless of
     * the input.
     *
     * @param dampingSource The domain units over which damping is applied.
     * @param strength A scaling factor, in `dampingSource` units, for the damping. A smaller
     *   `strength` value results in less damping, so the output follows the input more closely.
     * @param input input node that produces the value to be modified by the damping
     */
    public constructor(
        dampingSource: ProgressDomain,
        strength: Float,
        input: ValueNode,
    ) : this({ DampingNodeNative.create(dampingSource.value, strength) }, input)

    internal companion object {
        internal fun wrapNative(nativeAlloc: () -> Long, inputStack: ArrayDeque<ValueNode>) =
            DampingNode(nativeAlloc, input = inputStack.removeLast())
    }

    /** The source of the damping. */
    public val dampingSource: ProgressDomain =
        ProgressDomain.fromInt(DampingNodeNative.getDampingSourceInt(nativePointer))

    /** The amount of damping to apply. */
    public val strength: Float
        get() = DampingNodeNative.getStrength(nativePointer)

    override fun toString(): String =
        "DampingNode(${dampingSource.toSimpleString()}, $strength, $input)"

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is DampingNode) return false
        if (other === this) return true
        return dampingSource == other.dampingSource &&
            strength == other.strength &&
            input == other.input
    }

    override fun hashCode(): Int {
        var result = dampingSource.hashCode()
        result = 31 * result + strength.hashCode()
        result = 31 * result + input.hashCode()
        return result
    }
}

/**
 * Singleton wrapper for `BrushBehavior::DampingNode` native methods.
 *
 * Note that even though Kotlin [Node] is an abstract class with several subtypes,
 * [Node.nativePointer] all wrap the _same_ native type (a specialization of `std::variant`).
 */
expect internal object DampingNodeNative {
    fun create(dampingSource: Int, strength: Float): Long

    fun getDampingSourceInt(nativePointer: Long): Int

    fun getStrength(nativePointer: Long): Float
}
