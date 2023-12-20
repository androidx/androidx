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
@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package androidx.collection

import kotlin.jvm.JvmField
import kotlin.jvm.JvmInline

/**
 * Container to ease passing around a tuple of two [Int] values.
 *
 * *Note*: This class is optimized by using a value class, a Kotlin language featured
 * not available from Java code. Java developers can get the same functionality by
 * using [Pair] or by constructing a custom implementation using Int parameters
 * directly (see [LongLongPair] for an example).
 */
@JvmInline
public value class IntIntPair internal constructor(
    @PublishedApi @JvmField internal val packedValue: Long
) {
    /**
     * Constructs a [IntIntPair] with two [Int] values.
     *
     * @param first the first value in the pair
     * @param second the second value in the pair
     */
    public constructor(first: Int, second: Int) : this(packInts(first, second))

    /**
     * The first value in the pair.
     */
    public val first: Int
        get() = (packedValue shr 32).toInt()

    /**
     * The second value in the pair.
     */
    public val second: Int
        get() = (packedValue and 0xFFFFFFFF).toInt()

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
    // NOTE: Unpack the value directly because using `first` forces an invokestatic
    public inline operator fun component1(): Int = (packedValue shr 32).toInt()

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
    // NOTE: Unpack the value directly because using `second` forces an invokestatic
    public inline operator fun component2(): Int = (packedValue and 0xFFFFFFFF).toInt()

    override fun toString(): String = "($first, $second)"
}
