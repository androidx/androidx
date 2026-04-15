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

/** A [ValueNode] that produces a smooth random function. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
@ExperimentalInkCustomBrushApi
public class NoiseNode private constructor(nativePointer: Long) :
    ValueNode(nativePointer, emptyList()) {

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
    ) : this(NoiseNodeNative.createNoise(seed, varyOver.value, basePeriod))

    internal companion object {
        internal fun wrapNative(unownedNativePointer: Long): NoiseNode =
            NoiseNode(unownedNativePointer)
    }

    /** The seed for the random number generator. */
    public val seed: Int
        get() = NoiseNodeNative.getNoiseSeed(nativePointer)

    /** The source of the varying over which the random function is evaluated. */
    public val varyOver: ProgressDomain = NoiseNodeNative.getNoiseVaryOver(nativePointer)

    /** The base period of the random function. */
    public val basePeriod: Float
        get() = NoiseNodeNative.getNoiseBasePeriod(nativePointer)

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
@OptIn(ExperimentalInkCustomBrushApi::class)
@UsedByNative
private object NoiseNodeNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative external fun createNoise(seed: Int, varyOver: Int, basePeriod: Float): Long

    @UsedByNative external fun getNoiseSeed(nativePointer: Long): Int

    fun getNoiseVaryOver(nativePointer: Long): ProgressDomain =
        ProgressDomain.fromInt(getNoiseVaryOverInt(nativePointer))

    @UsedByNative private external fun getNoiseVaryOverInt(nativePointer: Long): Int

    @UsedByNative external fun getNoiseBasePeriod(nativePointer: Long): Float
}
