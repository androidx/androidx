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

/** A [ValueNode] that integrates an input value over time or distance. */
public class IntegralNode
private constructor(
    nativePointer: Long,
    /** The input node that produces the value to be integrated. */
    public val input: ValueNode,
) : ValueNode(nativePointer, listOf(input)) {

    /**
     * Creates an [IntegralNode] that integrates over an input value.
     *
     * If [integrateOver] is [ProgressDomain.DISTANCE_IN_CENTIMETERS] and the stroke input data does
     * not indicate the relationship between stroke units and physical units (e.g. as may be the
     * case for programmatically-generated inputs), then the output value will be null regardless of
     * the input.
     *
     * @param integrateOver the metric to integrate the input over
     * @param integralValueRangeStart the start of the range of values that the integral can produce
     * @param integralValueRangeEnd the end of the range of values that the integral can produce
     * @param integralOutOfRangeBehavior the behavior to use if the integral produces a value
     *   outside the specified range
     * @param input input node that produces the value to be integrated
     */
    public constructor(
        integrateOver: ProgressDomain,
        integralValueRangeStart: Float,
        integralValueRangeEnd: Float,
        integralOutOfRangeBehavior: OutOfRange,
        input: ValueNode,
    ) : this(
        IntegralNodeNative.createIntegral(
            integrateOver.value,
            integralValueRangeStart,
            integralValueRangeEnd,
            integralOutOfRangeBehavior.value,
        ),
        input,
    )

    internal companion object {
        internal fun wrapNative(
            unownedNativePointer: Long,
            inputStack: ArrayDeque<ValueNode>,
        ): IntegralNode = IntegralNode(unownedNativePointer, input = inputStack.removeLast())
    }

    /** The metric to integrate the input over. */
    public val integrateOver: ProgressDomain = IntegralNodeNative.getIntegrateOver(nativePointer)

    /** The start of the range of values that the integral can produce. */
    public val integralValueRangeStart: Float
        get() = IntegralNodeNative.getIntegralValueRangeStart(nativePointer)

    /** The end of the range of values that the integral can produce. */
    public val integralValueRangeEnd: Float
        get() = IntegralNodeNative.getIntegralValueRangeEnd(nativePointer)

    /** The behavior to use if the integral produces a value outside the specified range. */
    public val integralOutOfRangeBehavior: OutOfRange =
        IntegralNodeNative.getIntegralOutOfRangeBehavior(nativePointer)

    override fun toString(): String =
        "IntegralNode(${integrateOver.toSimpleString()}, $integralValueRangeStart, $integralValueRangeEnd, ${integralOutOfRangeBehavior.toSimpleString()}, $input)"

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is IntegralNode) return false
        if (other === this) return true
        return integrateOver == other.integrateOver &&
            integralValueRangeStart == other.integralValueRangeStart &&
            integralValueRangeEnd == other.integralValueRangeEnd &&
            integralOutOfRangeBehavior == other.integralOutOfRangeBehavior &&
            input == other.input
    }

    override fun hashCode(): Int {
        var result = integrateOver.hashCode()
        result = 31 * result + integralValueRangeStart.hashCode()
        result = 31 * result + integralValueRangeEnd.hashCode()
        result = 31 * result + integralOutOfRangeBehavior.hashCode()
        result = 31 * result + input.hashCode()
        return result
    }
}

/**
 * Singleton wrapper for `BrushBehavior::IntegralNode` native methods.
 *
 * Note that even though Kotlin [Node] is an abstract class with several subtypes,
 * [Node.nativePointer] all wrap the _same_ native type (a specialization of `std::variant`).
 */
@UsedByNative
private object IntegralNodeNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative
    external fun createIntegral(
        integrateOver: Int,
        integralValueRangeStart: Float,
        integralValueRangeEnd: Float,
        integralOutOfRangeBehavior: Int,
    ): Long

    fun getIntegrateOver(nativePointer: Long): ProgressDomain =
        ProgressDomain.fromInt(getIntegrateOverInt(nativePointer))

    @UsedByNative private external fun getIntegrateOverInt(nativePointer: Long): Int

    @UsedByNative external fun getIntegralValueRangeStart(nativePointer: Long): Float

    @UsedByNative external fun getIntegralValueRangeEnd(nativePointer: Long): Float

    fun getIntegralOutOfRangeBehavior(nativePointer: Long): OutOfRange =
        OutOfRange.fromInt(getIntegralOutOfRangeBehaviorInt(nativePointer))

    @UsedByNative private external fun getIntegralOutOfRangeBehaviorInt(nativePointer: Long): Int
}
