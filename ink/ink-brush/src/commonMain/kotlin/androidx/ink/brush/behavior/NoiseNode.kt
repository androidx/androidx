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

/** A [ValueNode] that produces a smooth random function. */
public class NoiseNode private constructor(nativeAlloc: () -> Long) :
    ValueNode(nativeAlloc, emptyList()) {

    /**
     * Creates a [NoiseNode] that produces a random variation.
     *
     * A new random value between 0 and 1 will be generated every [basePeriod] units along the
     * [varyOver] domain, with the output of this node shifting smoothly between successive random
     * values.
     *
     * If [varyOver] is [ProgressDomain.DISTANCE_IN_CENTIMETERS] and the stroke input data does not
     * indicate the relationship between stroke units and physical units (e.g. as may be the case
     * for programmatically-generated inputs), then the output value will be null.
     *
     * @param seed the seed for the random number generator
     * @param varyOver the source of the varying over which the random function is evaluated
     * @param basePeriod the base period of the random function
     */
    public constructor(
        seed: Int,
        varyOver: ProgressDomain,
        basePeriod: Float,
    ) : this({ NoiseNodeNative.create(seed, varyOver.value, basePeriod) })

    internal companion object {
        internal fun wrapNative(nativeAlloc: () -> Long) = NoiseNode(nativeAlloc)
    }

    /** The seed for the random number generator. */
    public val seed: Int
        get() = NoiseNodeNative.getSeed(nativePointer)

    /** The source of the varying over which the random function is evaluated. */
    public val varyOver: ProgressDomain =
        ProgressDomain.fromInt(NoiseNodeNative.getVaryOverInt(nativePointer))

    /** The base period of the random function. */
    public val basePeriod: Float
        get() = NoiseNodeNative.getBasePeriod(nativePointer)

    override fun toString(): String = "NoiseNode($seed, ${varyOver.toSimpleString()}, $basePeriod)"

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is NoiseNode) return false
        return seed == other.seed && varyOver == other.varyOver && basePeriod == other.basePeriod
    }

    override fun hashCode(): Int {
        var result = seed.hashCode()
        result = 31 * result + varyOver.hashCode()
        result = 31 * result + basePeriod.hashCode()
        return result
    }
}

/**
 * Singleton wrapper for `BrushBehavior::NoiseNode` native methods.
 *
 * Note that even though Kotlin [Node] is an abstract class with several subtypes,
 * [Node.nativePointer] all wrap the _same_ native type (a specialization of `std::variant`).
 */
expect internal object NoiseNodeNative {
    fun create(seed: Int, varyOver: Int, basePeriod: Float): Long

    fun getSeed(nativePointer: Long): Int

    fun getVaryOverInt(nativePointer: Long): Int

    fun getBasePeriod(nativePointer: Long): Float
}
