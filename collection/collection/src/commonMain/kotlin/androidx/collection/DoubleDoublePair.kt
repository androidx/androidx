/*
 * Copyright 2025 The Android Open Source Project
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
@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package androidx.collection

/**
 * Container to ease passing around a tuple of two [Double] values.
 *
 * @param first the first value in the pair
 * @param second the second value in the pair
 */
public class DoubleDoublePair public constructor(public val first: Double, public val second: Double) {
    /**
     * Returns the [first] component of the pair. For instance, the first component of
     * `DoubleDoublePair(3.1, 4.6)` is `3.1`.
     *
     * This method allows to use destructuring declarations when working with pairs, for example:
     * ```
     * val (first, second) = myPair
     * ```
     */
    public inline operator fun component1(): Double = first

    /**
     * Returns the [second] component of the pair. For instance, the second component of
     * `DoubleDoublePair(3.1, 4.6)` is `4.6`.
     *
     * This method allows to use destructuring declarations when working with pairs, for example:
     * ```
     * val (first, second) = myPair
     * ```
     */
    public inline operator fun component2(): Double = second

    /**
     * Checks the two values for equality.
     *
     * @param other the [DoubleDoublePair] to which this one is to be checked for equality
     * @return true if the underlying values of the [DoubleDoublePair] are both considered equal
     */
    override fun equals(other: Any?): Boolean {
        if (other !is DoubleDoublePair) {
            return false
        }
        return other.first == first && other.second == second
    }

    /**
     * Compute a hash code using the hash codes of the underlying values
     *
     * @return a hashcode of the [DoubleDoublePair]
     */
    override fun hashCode(): Int {
        return first.hashCode() xor second.hashCode()
    }

    override fun toString(): String {
        return "($first, $second)"
    }
}
