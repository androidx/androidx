/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.document

import androidx.annotation.RestrictTo

/** Typed handle to a float variable or expression ID in a RemoteCompose document. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RemoteFloatId(public val id: Int) {
    /** Converts this [RemoteFloatId] to a NaN-boxed [MaybeVariableFloat]. */
    public fun toMaybeVariableFloat(): MaybeVariableFloat = MaybeVariableFloat.variable(id)

    /** Converts this [RemoteFloatId] to a NaN-boxed [Float] for RPN expression arrays. */
    public fun toNanFloat(): Float = NanEncoding.asNan(id)

    /** Returns true if this ID refers to a built-in `RemoteContext` system variable (`1..41`). */
    public val isSystemVariable: Boolean
        get() = id in 1 until NanEncoding.START_ID
}

/** Creates a single-element RPN [FloatArray] containing [id] encoded as a NaN variable token. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun floatArrayOf(id: RemoteFloatId): FloatArray = kotlin.floatArrayOf(id.toNanFloat())

/** Typed handle to a 32-bit integer variable or expression ID in a RemoteCompose document. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RemoteIntId(public val id: Int) {
    /**
     * Encodes this integer variable ID as a `Long` (`id + 0x100000000L`) for operations that
     * distinguish literal integers from variable references via bit 32 (`Long`-encoded IDs).
     */
    public fun toLongEncodedId(): Long = NanEncoding.encodeLongId(id)
}

/** Typed handle to a 64-bit long constant or variable ID in a RemoteCompose document. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RemoteLongId(public val id: Int)

/** Typed handle to an ARGB color constant, theme color, or color expression ID. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RemoteColorId(public val id: Int)

/** Typed handle to a text constant, expression, or lookup ID in a RemoteCompose document. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RemoteStringId(public val id: Int)

/** Typed handle to a bitmap image resource ID in a RemoteCompose document. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RemoteBitmapId(public val id: Int)

/** Typed handle to a vector path resource or path expression ID in a RemoteCompose document. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RemotePathId(public val id: Int)

/** Typed handle to a shader resource ID in a RemoteCompose document. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RemoteShaderId(public val id: Int)

/** Typed handle to a font or bitmap-font resource ID in a RemoteCompose document. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RemoteFontId(public val id: Int)

/**
 * Typed handle to a list, array, or map collection ID (`NanEncoding.REGION_ARRAY` `0x200000+`) in a
 * RemoteCompose document.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RemoteArrayId(public val id: Int) {
    /** Converts this [RemoteArrayId] to a NaN-boxed [MaybeVariableFloat]. */
    public fun toMaybeVariableFloat(): MaybeVariableFloat = MaybeVariableFloat.variable(id)

    /**
     * Converts this [RemoteArrayId] to a NaN-boxed [Float] for RPN array operators (`A_DEREF`,
     * `A_LEN`, `A_SPLINE`, etc.).
     */
    public fun toNanFloat(): Float = NanEncoding.asNan(id)
}

/**
 * Pure-Kotlin NaN-boxing and `Long`-encoded ID utilities matching `Utils` and `NanMap` in
 * `remote-core`.
 *
 * Always compare NaN-boxed [Float] values via [Float.toRawBits] rather than `==`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object NanEncoding {
    /** Negative NaN prefix (`0xFF800000`) used when encoding NaN-boxed IDs and RPN tokens. */
    public const val NAN_PREFIX: Int = IdEncoding.NAN_PREFIX

    /**
     * IEEE-754 exponent mask (`0x7F800000`) used when decoding NaN-boxed IDs regardless of sign.
     */
    public const val NAN_EXPONENT: Int = IdEncoding.NAN_EXPONENT

    /** 22-bit payload mask (`0x003FFFFF`) matching `Utils.idFromNan`. */
    public const val ID_MASK: Int = IdEncoding.PAYLOAD_MASK

    /** Sentinel integer (`-1`) representing the absence of an ID. */
    public const val NO_ID: Int = IdEncoding.NO_ID

    /** 20-bit sub-ID mask (`0x000FFFFF`) within a `NanMap` region. */
    public const val REGION_SUB_ID_MASK: Int = IdEncoding.REGION_SUB_ID_MASK

    /** First non-system user-allocated variable/resource ID (`RemoteComposeState.START_ID`). */
    public const val START_ID: Int = IdEncoding.START_ID

    /** Region 0 (`0x000000..0x0FFFFF`): system variables (`1..35`) and standard resource IDs. */
    public const val REGION_SYSTEM: Int = IdEncoding.REGION_SYSTEM

    /** Region 1 (`0x100000..0x1FFFFF`): explicit `NanMap.TYPE_VARIABLE` namespace. */
    public const val REGION_VARIABLE: Int = IdEncoding.REGION_VARIABLE

    /** Region 2 (`0x200000..0x2FFFFF`): array/list/map IDs (`NanMap.TYPE_ARRAY`). */
    public const val REGION_ARRAY: Int = IdEncoding.REGION_ARRAY

    /** Region 3 (`0x300000..0x3FFFFF`): RPN operators (`0x310000+`) and path/command tags. */
    public const val REGION_OPERATION: Int = IdEncoding.REGION_OPERATION

    /** First user-allocated array/list/map ID (`NanMap.START_ARRAY = 0x20002A`). */
    public const val START_ARRAY_ID: Int = IdEncoding.START_ARRAY_ID

    /** Offset (`0x100000000L`) added to integer IDs when encoded in a 64-bit `Long` slot. */
    public const val LONG_ID_OFFSET: Long = IdEncoding.LONG_ID_OFFSET

    internal fun asNanBits(id: Int): Int {
        require(id in 1..ID_MASK) { "NaN ID out of 22-bit range (1..$ID_MASK): $id" }
        return id or NAN_PREFIX
    }

    internal fun isNanBits(rawBits: Int): Boolean =
        (rawBits and NAN_EXPONENT) == NAN_EXPONENT && (rawBits and ID_MASK) != 0

    internal fun idFromNanBits(rawBits: Int): Int =
        if (isNanBits(rawBits)) rawBits and ID_MASK else NO_ID

    /** Encodes a 22-bit [id] (`1..0x3FFFFF`) into a NaN-boxed [Float]. */
    @JvmStatic public fun asNan(id: Int): Float = Float.fromBits(asNanBits(id))

    /**
     * Returns true if [value] is an IEEE-754 NaN (either sign bit) with a non-zero 22-bit ID
     * payload (excluding infinities `0x7F800000` / `0xFF800000` and zero-payload quiet NaNs
     * `0x7FC00000` / `0xFFC00000`).
     */
    @JvmStatic public fun isNan(value: Float): Boolean = isNanBits(value.toRawBits())

    /** Extracts the 22-bit payload ID from a NaN-boxed [value], or `-1` if [value] is literal. */
    @JvmStatic public fun idFromNan(value: Float): Int = idFromNanBits(value.toRawBits())

    /** Returns true if [value] is a NaN-boxed array/collection ID in [REGION_ARRAY]. */
    @JvmStatic
    public fun isArrayNan(value: Float): Boolean {
        val id = idFromNan(value)
        return id != NO_ID && (id and 0x300000) == REGION_ARRAY
    }

    /** Encodes a 32-bit integer variable [id] into a `Long` (`id + 0x100000000L`). */
    @JvmStatic
    public fun encodeLongId(id: Int): Long {
        require(id >= 0) { "Integer variable ID must be non-negative: $id" }
        return id.toLong() + LONG_ID_OFFSET
    }

    /**
     * Returns true if [encoded] represents a `Long`-encoded non-negative 32-bit integer variable ID
     * (`0x100000000L..0x17FFFFFFFL`).
     */
    @JvmStatic
    public fun isLongId(encoded: Long): Boolean =
        encoded in LONG_ID_OFFSET..(LONG_ID_OFFSET + Int.MAX_VALUE)

    /** Extracts the 32-bit integer variable ID from a `Long`-encoded ID. */
    @JvmStatic
    public fun decodeLongId(encoded: Long): Int {
        require(encoded in LONG_ID_OFFSET..(LONG_ID_OFFSET + Int.MAX_VALUE)) {
            "Not a valid Long-encoded 32-bit variable ID: $encoded"
        }
        return (encoded - LONG_ID_OFFSET).toInt()
    }
}

/**
 * Returns true if this [MaybeVariableFloat] encodes a collection/array ID in `0x200000..0x2FFFFF`.
 */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val MaybeVariableFloat.isArrayVariable: Boolean
    get() = isVariable && (variableId and 0x300000) == NanEncoding.REGION_ARRAY

/** Returns true if this [MaybeVariableFloat] encodes a float RPN operator token. */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val MaybeVariableFloat.isRpnOperator: Boolean
    get() = FloatRpn.isOperator(floatValue)
