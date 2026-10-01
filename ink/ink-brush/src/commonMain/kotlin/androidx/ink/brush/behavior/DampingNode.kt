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
 * A [ValueNode] that applies damping to an input value, smoothing out rapid changes by causing the
 * output value to asymptotically decay towards the input value over a specified time or distance.
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
     * Damping smooths changes in the input value using standard
     * [exponential decay](https://en.wikipedia.org/wiki/Exponential_decay), where [strength] acts
     * as the decay constant, measured in [dampOver] units. The greater the damping strength, the
     * longer it takes for the output value to asymptotically approach the input value. For example,
     * if [dampOver] is set to [ProgressDomain.TIME_IN_SECONDS] and [strength] is 0.5, then it will
     * take about half a second for the output value to decay most of the way (see the documentation
     * of the [strength] property) towards the input value; if [strength] is 0.25 then it will take
     * only a quarter second. If [strength] is zero, then there is no damping at all, and the output
     * value will instantly snap to the input value whenever that value changes.
     *
     * If [dampOver] is [ProgressDomain.DISTANCE_IN_CENTIMETERS] and the stroke input data does not
     * indicate the relationship between stroke units and physical units (e.g. as may be the case
     * for programmatically-generated inputs), then the output value will be null regardless of the
     * input.
     *
     * @param dampOver The domain units over which damping is applied.
     * @param strength The amount of damping to apply, measured in [dampOver] units. Must be finite
     *   and non-negative. Smaller values result in less damping (i.e. faster transitions, following
     *   the input more closely), while larger values result in more damping (i.e. slower
     *   transitions and heavier smoothing).
     * @param input The input node that produces the value to be modified by the damping.
     */
    public constructor(
        dampOver: ProgressDomain,
        strength: Float,
        input: ValueNode,
    ) : this({ DampingNodeNative.create(dampOver.value, strength) }, input)

    internal companion object {
        internal fun wrapNative(nativeAlloc: () -> Long, inputStack: ArrayDeque<ValueNode>) =
            DampingNode(nativeAlloc, input = inputStack.removeLast())
    }

    /** The domain units over which damping is applied (whether distance or time). */
    public val dampOver: ProgressDomain =
        ProgressDomain.fromInt(DampingNodeNative.getDampOverInt(nativePointer))

    /**
     * The strength of the damping to apply, measured in [dampOver] units.
     *
     * This represents the amount of distance or time it will take for the output value of this node
     * to exponentially decay most of the way (see note below) towards the input value.
     *
     * This value will always be finite and non-negative.
     *
     * *Note:* This value is used as the decay constant for a standard
     * [exponential decay](https://en.wikipedia.org/wiki/Exponential_decay) function, so "most of
     * the way" above means specifically 1 - 1/e, about 63%. This may seem like an arbitrary amount,
     * but using a standard natural-base exponential decay function has nicer mathematical
     * properties than using another base, and tends to feel more intuitive when tuning the
     * [strength] value by hand.
     */
    public val strength: Float
        get() = DampingNodeNative.getStrength(nativePointer)

    override fun toString(): String = "DampingNode(${dampOver.toSimpleString()}, $strength, $input)"

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is DampingNode) return false
        if (other === this) return true
        return dampOver == other.dampOver && strength == other.strength && input == other.input
    }

    override fun hashCode(): Int {
        var result = dampOver.hashCode()
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
    fun create(dampOver: Int, strength: Float): Long

    fun getDampOverInt(nativePointer: Long): Int

    fun getStrength(nativePointer: Long): Float
}
