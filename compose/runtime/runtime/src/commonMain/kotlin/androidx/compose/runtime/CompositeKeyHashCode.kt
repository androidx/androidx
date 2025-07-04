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

@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package androidx.compose.runtime

/**
 * The return type of [currentCompositeKeyHashCode]. On most platforms this is a [Long] but may be a
 * different type if the platform target does not support [Long] efficiently (such as JavaScript).
 *
 * A `CompositeKeyHashCode` is a hash that correlates to a location in a Composition. Hashes are
 * stable, meaning that if the same composition hierarchy is recomposed or recreated, it will have
 * the same hashes. Hashes are very likely, but not guaranteed, to be unique.
 *
 * If you need to convert this value to an Int, it is strongly recommended to use [hashCode] instead
 * of [toInt][Number.toInt]. Truncating this value instead of hashing it can greatly impact a
 * value's effectiveness as a hash.
 *
 * @see currentCompositeKeyHashCode
 */
public expect class CompositeKeyHashCode

/**
 * Converts a [CompositeKeyHashCode] to a 64-bit Long. This may be higher precision than the
 * underlying type.
 */
public expect fun CompositeKeyHashCode.toLong(): Long

/**
 * Returns a String representation of a [CompositeKeyHashCode] with the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string
 *   conversion.
 */
public expect fun CompositeKeyHashCode.toString(radix: Int): String

internal expect fun CompositeKeyHashCode(initial: Int): CompositeKeyHashCode

/**
 * This function implements hashing arithmetic used by the Composer.
 *
 * Defines the top-down incremental hash. This hash is computed by taking the receiver hash,
 * combining it with the incoming [segment], and applying a [shift]. The order of these operations
 * and the application of these operations are arbitrary, but must be consistent with the other
 * functions in this file.
 *
 * The standard implementation of this is `(this rol shift) xor segment`.
 */
internal expect fun CompositeKeyHashCode.compoundWith(
    segment: Int,
    shift: Int,
): CompositeKeyHashCode

/**
 * This function implements hashing arithmetic used by the Composer.
 *
 * Performs the inverse operation of [compoundWith]. As in, the following equality always holds
 * true:
 * ```
 * key.compoundWith(segment, shift).unCompoundWith(segment, shift) == key
 * ```
 *
 * With the standard implementation of [compoundWith], this function should be implemented as `(this
 * xor segment) ror shift`
 */
internal expect fun CompositeKeyHashCode.unCompoundWith(
    segment: Int,
    shift: Int,
): CompositeKeyHashCode

/**
 * This function implements hashing arithmetic used by the Composer.
 *
 * Implements [compoundWith] as a bottom-up hash. The sequence of the receiver and [segment]
 * argument are reversed in this role to build the hash from a child location upwards in the
 * composition hierarchy towards a known parent.
 *
 * The caller is responsible for specifying the [shift] value such that each segment has the
 * aggregated shift amount it would have by building the hash from top-down (which is the product of
 * its incremental shift amount and the distance of the compounded element from the child). [shift]
 * must be less than [CompositeKeyHashSizeBits].
 *
 * This implementation must be consistent with [compoundWith] such that building both hashes in
 * opposite directions always returns the same value. Given the standard implementation of
 * [compoundWith], this function should be implemented as: `this xor (segment rol shift)`
 */
internal expect fun CompositeKeyHashCode.bottomUpCompoundWith(
    segment: CompositeKeyHashCode,
    shift: Int,
): CompositeKeyHashCode

/**
 * This function implements hashing arithmetic used by the Composer.
 *
 * Implements [compoundWith] as a bottom-up hash. The sequence of the receiver and [segment]
 * argument are reversed in this role to build the hash from a child location upwards in the
 * composition hierarchy towards a known parent.
 *
 * The caller is responsible for specifying the [shift] value such that each segment has the
 * aggregated shift amount it would have by building the hash from top-down (which is the product of
 * its incremental shift amount and the distance of the compounded element from the child). [shift]
 * must be less than [CompositeKeyHashSizeBits].
 *
 * This implementation must be consistent with [compoundWith] such that building both hashes in
 * opposite directions always returns the same value. Given the standard implementation of
 * [compoundWith], this function should be implemented as: `this xor (segment rol shift)`
 */
internal expect fun CompositeKeyHashCode.bottomUpCompoundWith(
    segment: Int,
    shift: Int,
): CompositeKeyHashCode

/**
 * The number of bits available in a [CompositeKeyHashCode]. For most platforms, this is 64 since
 * the [CompositeKeyHashCode] is backed by a Long.
 */
internal expect val CompositeKeyHashSizeBits: Int

/**
 * An empty [CompositeKeyHashCode], equivalent to hashing no content. This is not necessarily the
 * same as the hash at the root of a composition. This is an effective default value in the absence
 * of a hash from [currentCompositeKeyHashCode].
 */
public expect val EmptyCompositeKeyHashCode: CompositeKeyHashCode
