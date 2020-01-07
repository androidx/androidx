/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.serialization.schema

import java.util.Locale

/** Scalar values representable in Serialization. */
enum class Scalar : Type {
    /** Boolean scalar. */
    BOOL,

    /** Byte array scalar. */
    BYTES,

    /** 64-bit floating point scalar. */
    DOUBLE,

    /** 32-bit floating point scalar. */
    FLOAT,

    /** 32-bit signed varint scalar. */
    INT32,

    /** 32-bit signed varint scalar with zig-zag encoding. */
    SINT32,

    /** 32-bit unsigned varint scalar. */
    UINT32,

    /** 32-bit unsigned fixed-width scalar */
    FIXED32,

    /** 32-bit signed fixed-width scalar. */
    SFIXED32,

    /** 64-bit signed varint scalar. */
    INT64,

    /** 64-bit signed varint scalar with zig-zag encoding. */
    SINT64,

    /** 64-bit unsigned varint scalar. */
    UINT64,

    /** 64-bit unsigned fixed-width scalar. */
    FIXED64,

    /** 65-bit signed fixed-width scalar. */
    SFIXED64,

    /** Unicode string scalar. */
    STRING;

    /** The protobuf keyword for this type. */
    val keyword: String = name.toLowerCase(Locale.ROOT)

    override val typeKind: Type.Kind = Type.Kind.SCALAR
}
