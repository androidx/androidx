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
 * Container to ease passing around a tuple of two [Int] values.
 */
@kotlin.jvm.JvmInline
public value class PairIntInt internal constructor(internal val packedValue: Long) {

    /**
     * Constructs a [PairIntInt] with two [Int] values.
     *
     * @param first the first value in the pair
     * @param second the second value in the pair
     */
    public constructor(first: Int, second: Int) : this(packInts(first, second))

    /**
     * The first value in the pair.
     */
    public val first: Int
        get() {
            return unpackInt1(packedValue)
        }

    /**
     * The second value in the pair.
     */
    public val second: Int
        get() {
            return unpackInt2(packedValue)
        }

    /**
     * Returns the [first] component of the pair. For instance, the first component
     * of `PairIntInt(3, 4)` is `3`.
     *
     * This method allows to use destructuring declarations when working with pairs,
     * for example:
     * ```
     * val (first, second) = myPair
     * ```
     */
    public operator fun component1(): Int = first

    /**
     * Returns the [second] component of the pair. For instance, the second component
     * of `PairIntInt(3, 4)` is `4`.
     *
     * This method allows to use destructuring declarations when working with pairs,
     * for example:
     * ```
     * val (first, second) = myPair
     * ```
     */
    public operator fun component2(): Int = second

    override fun toString(): String {
        return "PairIntInt{" + first + " " + second + "}";
    }
}
