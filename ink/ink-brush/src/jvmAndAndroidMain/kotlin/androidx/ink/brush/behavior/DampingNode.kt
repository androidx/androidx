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

import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

/**
 * A [ValueNode] that damps changes in an input value, causing the output value to slowly follow
 * changes in the input value over a specified time or distance.
 */
public class DampingNode
private constructor(
    nativePointer: Long,
    /** The input node that produces the value to be modified by the damping. */
    public val input: ValueNode,
) : ValueNode(nativePointer, listOf(input)) {

    /**
     * Creates a [DampingNode] that damps changes in an input value, causing the output value to
     * slowly follow changes in the input value over a specified time or distance.
     *
     * If [dampingSource] is [ProgressDomain.DISTANCE_IN_CENTIMETERS] and the stroke input data does
     * not indicate the relationship between stroke units and physical units (e.g. as may be the
     * case for programmatically-generated inputs), then the output value will be null regardless of
     * the input.
     *
     * @param dampingSource the source of the damping
     * @param dampingGap the amount of damping to apply
     * @param input input node that produces the value to be modified by the damping
     */
    public constructor(
        dampingSource: ProgressDomain,
        dampingGap: Float,
        input: ValueNode,
    ) : this(DampingNodeNative.createDamping(dampingSource.value, dampingGap), input)

    internal companion object {
        internal fun wrapNative(
            unownedNativePointer: Long,
            inputStack: ArrayDeque<ValueNode>,
        ): DampingNode = DampingNode(unownedNativePointer, input = inputStack.removeLast())
    }

    /** The source of the damping. */
    public val dampingSource: ProgressDomain = DampingNodeNative.getDampingSource(nativePointer)

    /** The amount of damping to apply. */
    public val dampingGap: Float
        get() = DampingNodeNative.getDampingGap(nativePointer)

    override fun toString(): String =
        "DampingNode(${dampingSource.toSimpleString()}, $dampingGap, $input)"

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is DampingNode) return false
        if (other === this) return true
        return dampingSource == other.dampingSource &&
            dampingGap == other.dampingGap &&
            input == other.input
    }

    override fun hashCode(): Int {
        var result = dampingSource.hashCode()
        result = 31 * result + dampingGap.hashCode()
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
@UsedByNative
private object DampingNodeNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative external fun createDamping(dampingSource: Int, dampingGap: Float): Long

    fun getDampingSource(nativePointer: Long): ProgressDomain =
        ProgressDomain.fromInt(getDampingSourceInt(nativePointer))

    @UsedByNative private external fun getDampingSourceInt(nativePointer: Long): Int

    @UsedByNative external fun getDampingGap(nativePointer: Long): Float
}
