/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.serialization

/**
 * The encoding used in the proto representation of a field.
 *
 * Protocol Buffers supports [several encodings][1] for integral fields which may allow you to
 * reduce the average encoded size of a field based on knowledge of what values it is likely to
 * contain. Supply an encoding from this enumeration to [Field.protoEncoding] or
 * [Field.mapKeyProtoEncoding] to select a field's encoding.
 *
 * Use the [DEFAULT] value for non-integral fields, as all other fields types only have one
 * encoding, or to request the compiler to select a variable length encoding with a signedness
 * matching the signedness of an integral field.
 *
 * Signed integral fields may optionally use an unsigned encoding. This may be desirable when
 * integrating with other protobuf implementations. Unsigned numbers are represented as signed
 * numbers by setting the sign bit to their most significant bit. This means that smaller values
 * with zero in their most significant bit are represented as positive values and larger values are
 * represented as negative values with the same binary representation as their unsigned
 * counterpart.
 *
 * [1]: https://developers.google.com/protocol-buffers/docs/proto3#scalar
 */
enum class ProtoEncoding {
    /**
     * Default varint encoding for integral fields, or no specific encoding for other types.
     */
    DEFAULT,

    /**
     * Signed variable length encoding, `int32` and `int64` in proto.
     *
     * This encoding represents smaller positive values in fewer bytes, but always uses 10 bytes
     * for negative values. Consider [ZIG_ZAG_VARINT] for fields that often contain negative
     * values. It stores 7 bits of data per encoded byte. For fields that commonly contain ints
     * greater than 2^28 or longs greater than 2^56, consider [SIGNED_FIXED] instead.
     *
     * It is used by default for [Int] and [Long] fields and supports nullable fields using the
     * [`Int32Value`][1] and [`Int64Value`][2] wrapper messages.
     *
     * [1]: https://developers.google.com/protocol-buffers/docs/reference/google.protobuf#google.protobuf.Int32Value
     * [2]: https://developers.google.com/protocol-buffers/docs/reference/google.protobuf#google.protobuf.Int64Value
     */
    SIGNED_VARINT,

    /**
     * Unsigned variable length encoding, `uint32` and `uint64` in proto.
     *
     * This is an unsigned variant of [SIGNED_VARINT] which represents smaller values in fewer
     * bytes. It stores 7 bits of data per encoded byte. Consider [UNSIGNED_FIXED] instead for
     * fields that commonly contains ints greater than 2^28 or longs greater than 2^56.
     *
     * It is used by default for [UInt] and [ULong] fields and supports nullable fields using the
     * [`UInt32Value`][1] and [`UInt64Value][2] wrapper messages.
     *
     * [1]: https://developers.google.com/protocol-buffers/docs/reference/google.protobuf#google.protobuf.UInt32Value
     * [2]: https://developers.google.com/protocol-buffers/docs/reference/google.protobuf#google.protobuf.UInt64Value
     */
    UNSIGNED_VARINT,

    /**
     * Signed variable length encoding with compact negative values, `sint32` and `sint64` in proto.
     *
     * This is a variant of [SIGNED_VARINT] which stores the sign bit in the least significant
     * bit of the encoded representation to represent smaller absolute values in fewer bytes.
     *
     * It does support nullable fields.
     */
    ZIG_ZAG_VARINT,

    /**
     * Signed fixed width encoding, `sfixed32` and `sfixed64` in proto.
     *
     * This encoding always uses 4 bytes for ints and 8 bytes for longs. It is more compact than
     * the variable length encodings for fields which commonly contain ints greater than 2^28 or
     * longs greater than 2^56. Consider [SIGNED_VARINT] instead for fields which usually
     * contain smaller positive values or [ZIG_ZAG_VARINT] for fields which often contain
     * negative values with small absolute values.
     *
     * It does not support nullable fields.
     */
    SIGNED_FIXED,

    /**
     * Unsigned fixed width encoding, `fixed32` and `fixed64` in proto.
     *
     * This encoding always uses 4 bytes for ints and 8 bytes for longs. It is more compact than
     * the variable length encodings for fields which commonly contain ints greater than 2^28 or
     * longs greater than 2^56. Consider [UNSIGNED_VARINT] instead for fields which do not
     * usually contain large values.
     *
     * It does not support nullable fields.
     */
    UNSIGNED_FIXED
}
