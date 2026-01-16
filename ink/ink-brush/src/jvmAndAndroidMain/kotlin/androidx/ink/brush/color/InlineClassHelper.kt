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
@file:Suppress("NOTHING_TO_INLINE")

package androidx.ink.brush.color

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

internal inline fun floatFromBits(bits: Int): Float = java.lang.Float.intBitsToFloat(bits)

internal inline fun packFloats(val1: Float, val2: Float): Long {
    val v1 = val1.toRawBits().toLong()
    val v2 = val2.toRawBits().toLong()
    return (v1 shl 32) or (v2 and 0xFFFFFFFF)
}

/** Unpacks the first Float value in [packFloats] from its returned Long. */
internal inline fun unpackFloat1(value: Long): Float {
    return floatFromBits((value shr 32).toInt())
}

/** Unpacks the first absolute Float value in [packFloats] from its returned Long. */
internal inline fun unpackAbsFloat1(value: Long): Float {
    return floatFromBits(((value shr 32) and 0x7FFFFFFF).toInt())
}

/** Unpacks the second Float value in [packFloats] from its returned Long. */
internal inline fun unpackFloat2(value: Long): Float {
    return floatFromBits((value and 0xFFFFFFFF).toInt())
}

/** Unpacks the second absolute Float value in [packFloats] from its returned Long. */
internal inline fun unpackAbsFloat2(value: Long): Float {
    return floatFromBits((value and 0x7FFFFFFF).toInt())
}

/** Packs two Int values into one Long value for use in inline classes. */
internal inline fun packInts(val1: Int, val2: Int): Long {
    return (val1.toLong() shl 32) or (val2.toLong() and 0xFFFFFFFF)
}

/** Unpacks the first Int value in [packInts] from its returned ULong. */
internal inline fun unpackInt1(value: Long): Int {
    return (value shr 32).toInt()
}

/** Unpacks the second Int value in [packInts] from its returned ULong. */
internal inline fun unpackInt2(value: Long): Int {
    return (value and 0xFFFFFFFF).toInt()
}

// This function is technically identical to Float.fromBits(). However,
// since they are declared as top-level functions, they do not incur the
// cost of a static fetch through the Companion class. Using these
// top-level functions, the generated arm64 code after dex2oat is exactly
// a single `fmov`

// This function exists so we do *not* inline the throw. It keeps
// the call site much smaller and since it's the slow path anyway,
// we don't mind the extra function call
internal fun throwIllegalArgumentException(message: String) {
    throw IllegalArgumentException(message)
}

// Like Kotlin's require() but without the .toString() call
@Suppress("BanInlineOptIn") // same opt-in as using Kotlin's require()
@OptIn(ExperimentalContracts::class)
internal inline fun requirePrecondition(value: Boolean, lazyMessage: () -> String) {
    contract { returns() implies value }
    if (!value) {
        throwIllegalArgumentException(lazyMessage())
    }
}
