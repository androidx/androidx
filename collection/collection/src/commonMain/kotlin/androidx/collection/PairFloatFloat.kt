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

package androidx.collection

/**
 * Container to ease passing around a tuple of two [Float] values.
 */
@kotlin.jvm.JvmInline
public value class PairFloatFloat internal constructor(internal val packedValue: Long) {

    /**
     * Constructs a [PairFloatFloat] with two [Float] values.
     *
     * @param first the first value in the pair
     * @param second the second value in the pair
     */
    public constructor(first: Float, second: Float) : this(packFloats(first, second))

    /**
     * The first value in the pair.
     */
    public val first: Float
        get() {
            return unpackFloat1(packedValue)
        }

    /**
     * The second value in the pair.
     */
    public val second: Float
        get() {
            return unpackFloat2(packedValue)
        }

    /**
     * Returns the [first] component of the pair. For instance, the first component
     * of `PairFloatFloat(3f, 4f)` is `3f`.
     *
     * This method allows to use destructuring declarations when working with pairs,
     * for example:
     * ```
     * val (first, second) = myPair
     * ```
     */
    public operator fun component1(): Float = first

    /**
     * Returns the [second] component of the pair. For instance, the second component
     * of `PairFloatFloat(3f, 4f)` is `4f`.
     *
     * This method allows to use destructuring declarations when working with pairs,
     * for example:
     * ```
     * val (first, second) = myPair
     * ```
     */
    public operator fun component2(): Float = second

    override fun toString(): String {
        return "PairFloatFloat{" + first + " " + second + "}";
    }
}
