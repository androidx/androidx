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

package androidx.serialization.runtime.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * An encoder receives field structure and values from a serializer.
 * <p>
 * Serializers provide the encoder with message data in units of whole fields. Each encode method
 * provides the backend with the field value, field ID, and information about the type of the
 * field, including a serializer for embedded messages. Units of whole fields makes encoders
 * agnostic of the order they receive fields in. This enables the encoder to intelligently omit
 * empty fields in their entirety and to define its own encoding direction. For example, the
 * proto encoder works from the end of its buffer towards the beginning.
 * <p>
 * While it is possible to encode fields with repeated values by repeatedly calling the encode
 * method for a single value of that type, this may result in an incorrect ordering in the
 * encoded message. Collections and scalar arrays should be encoded using the appropriate encode
 * method for the field type, such as
 * {@link #encodeRepeatedMessage(int, SerializerV1, Iterable)} for embedded messages.
 * <p>
 * Encoding for embedded messages, such as {@link #encodeMessage(int, SerializerV1, Object)}, is
 * likewise implemented in a unit of a whole field. This gives implementations flexibility in how
 * they invoke the serializer. The API is designed such that an implementation may store some
 * state on the stack and pass itself to the serializer, or at the implementor's discretion, it
 * could instantiate and pass a new encoder to the serializer.
 * <p>
 * Encoders include distinct methods for all Protocol Buffers integer types. This makes for
 * clearer serializer code and enables code shrinking tools such as R8 to more effectively remove
 * unused integer encodings. The int32 and int64 encodings are the default and the most common
 * integer types, and backends without multiple integer representations, such as the Parcel
 * backend may wish to implement {@link #encodeInt32(int, int)} and
 * {@link #encodeInt64(int, long)} and delegate the other int and long encoding methods to those
 * implementations.
 */
public interface EncoderV1 {
    /**
     * Encode an embedded message field.
     *
     * @param fieldId    the ID of the field.
     * @param serializer the serializer for the message class.
     * @param message    a message or null to omit the field.
     * @param <T>        the message class.
     */
    <T> void encodeMessage(
            int fieldId,
            @NonNull SerializerV1<T> serializer,
            @Nullable T message
    );

    /**
     * Encode a repeated embedded message field.
     *
     * @param fieldId    the ID of the field.
     * @param serializer the serializer for the message class.
     * @param messages   messages or null to omit the field.
     * @param <T>        the message class.
     */
    <T> void encodeRepeatedMessage(
            int fieldId,
            @NonNull SerializerV1<T> serializer,
            @Nullable Iterable<T> messages
    );

    /**
     * Encode an enum field.
     *
     * @param fieldId    the ID of the field.
     * @param serializer the serializer for the enum class.
     * @param value      an enum value or null to omit the field.
     * @param <T>        the enum class.
     */
    <T extends Enum<T>> void encodeEnum(
            int fieldId,
            @NonNull EnumSerializerV1<T> serializer,
            @Nullable T value
    );

    /**
     * Encode a repeated enum field.
     *
     * @param fieldId    the ID of the field.
     * @param serializer the serializer for the enum class.
     * @param values     enum values or null to omit the field.
     * @param <T>        the enum class.
     */
    <T extends Enum<T>> void encodeRepeatedEnum(
            int fieldId,
            @NonNull EnumSerializerV1<T> serializer,
            @Nullable Iterable<T> values
    );

    /**
     * Encode a bool scalar field.
     *
     * @param fieldId the ID of the field.
     * @param value   a field value.
     */
    void encodeBool(int fieldId, boolean value);

    /**
     * Encode a repeated bool scalar field.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedBool(int fieldId, @Nullable boolean[] values);

    /**
     * Encode a repeated bool scalar field.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedBool(int fieldId, @Nullable Iterable<Boolean> values);

    /**
     * Encode a bytes scalar field.
     *
     * @param fieldId the ID of the field.
     * @param value   a field value or null to omit the field.
     */
    void encodeBytes(int fieldId, @Nullable byte[] value);

    /**
     * Encode a repeated bytes scalar field.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedBytes(int fieldId, @Nullable Iterable<byte[]> values);

    /**
     * Encode a double scalar field.
     *
     * @param fieldId the ID of the field.
     * @param value   a field value.
     */
    void encodeDouble(int fieldId, double value);

    /**
     * Encode a repeated double scalar field.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedDouble(int fieldId, @Nullable double[] values);

    /**
     * Encode a repeated double scalar field.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedDouble(int fieldId, @Nullable Iterable<Double> values);

    /**
     * Encode a float scalar field.
     *
     * @param fieldId the ID of the field.
     * @param value   a field value.
     */
    void encodeFloat(int fieldId, float value);

    /**
     * Encode a repeated float scalar field.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedFloat(int fieldId, @Nullable float[] values);

    /**
     * Encode a repeated float scalar field.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedFloat(int fieldId, @Nullable Iterable<Float> values);

    /**
     * Encode an int32 scalar field.
     * <p>
     * With proto-based backends, int32 fields store 32-bit signed integers using a variable
     * length encoding with a compact representation of small positive values, but always uses 10
     * bytes for negative values.
     *
     * @param fieldId the ID of the field.
     * @param value   a field value.
     */
    void encodeInt32(int fieldId, int value);

    /**
     * Encode a repeated int32 scalar field.
     * <p>
     * With proto-based backends, int32 fields store 32-bit signed integers using a variable
     * length encoding with a compact representation of small positive values, but always uses 10
     * bytes for negative values.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedInt32(int fieldId, @Nullable int[] values);

    /**
     * Encode a repeated int32 scalar field.
     * <p>
     * With proto-based backends, int32 fields store 32-bit signed integers using a variable
     * length encoding with a compact representation of small positive values, but always uses 10
     * bytes for negative values.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedInt32(int fieldId, @Nullable Iterable<Integer> values);

    /**
     * Encode a sint32 scalar field.
     * <p>
     * With proto-based backends, sint32 fields store 32-bit signed integers using a variable
     * length encoding with a compact representation of values with small absolute values.
     *
     * @param fieldId the ID of the field.
     * @param value   a field value.
     */
    void encodeSInt32(int fieldId, int value);

    /**
     * Encode a repeated sint32 scalar field.
     * <p>
     * With proto-based backends, sint32 fields store 32-bit signed integers using a variable
     * length encoding with a compact representation of values with small absolute values.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedSInt32(int fieldId, @Nullable int[] values);

    /**
     * Encode a repeated sint32 scalar field.
     * <p>
     * With proto-based backends, sint32 fields store 32-bit signed integers using a variable
     * length encoding with a compact representation of values with small absolute values.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedSInt32(int fieldId, @Nullable Iterable<Integer> values);

    /**
     * Encode a uint32 scalar field.
     * <p>
     * With proto-based backends, uint32 fields store 32-bit unsigned integers using a variable
     * length encoding with a compact representation of small values.
     * <p>
     * Unsigned integers are represented as Java ints with MSB in the sign bit. This means unsigned
     * values greater than 2<sup>31</sup> are represented as negative signed values with the same
     * binary representation as the unsigned values.
     *
     * @param fieldId the ID of the field.
     * @param value   a field value.
     */
    void encodeUInt32(int fieldId, int value);

    /**
     * Encode a repeated uint32 scalar field.
     * <p>
     * With proto-based backends, uint32 fields store 32-bit unsigned integers using a variable
     * length encoding with a compact representation of small values.
     * <p>
     * Unsigned integers are represented as Java ints with MSB in the sign bit. This means unsigned
     * values greater than 2<sup>31</sup> are represented as negative signed values with the same
     * binary representation as the unsigned values.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedUInt32(int fieldId, @Nullable int[] values);

    /**
     * Encode a repeated uint32 scalar field.
     * <p>
     * With proto-based backends, uint32 fields store 32-bit unsigned integers using a variable
     * length encoding with a compact representation of small values.
     * <p>
     * Unsigned integers are represented as Java ints with MSB in the sign bit. This means unsigned
     * values greater than 2<sup>31</sup> are represented as negative signed values with the same
     * binary representation as the unsigned values.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedUInt32(int fieldId, @Nullable Iterable<Integer> values);

    /**
     * Encode a fixed32 or sfixed32 scalar field.
     * <p>
     * With proto-based backends, the unsigned fixed32 and the signed sfixed32 store 32-bit
     * integers as 4 little-endian bytes.
     * <p>
     * Unsigned integers are represented as Java ints with MSB in the sign bit. This means unsigned
     * values greater than 2<sup>31</sup> are represented as negative signed values with the same
     * binary representation as the unsigned values. This allows this method to be used
     * interchangeably for the signed sfixed32 and the unsigned fixed32.
     *
     * @param fieldId the ID of the field.
     * @param value   a field value.
     */
    void encodeFixed32(int fieldId, int value);

    /**
     * Encode a repeated fixed32 or sfixed32 scalar field.
     * <p>
     * With proto-based backends, the unsigned fixed32 and the signed sfixed32 store 32-bit
     * integers as 4 little-endian bytes.
     * <p>
     * Unsigned integers are represented as Java ints with MSB in the sign bit. This means unsigned
     * values greater than 2<sup>31</sup> are represented as negative signed values with the same
     * binary representation as the unsigned values. This allows this method to be used
     * interchangeably for the signed sfixed32 and the unsigned fixed32.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedFixed32(int fieldId, @Nullable int[] values);

    /**
     * Encode a repeated fixed32 or sfixed32 scalar field.
     * <p>
     * With proto-based backends, the unsigned fixed32 and the signed sfixed32 store 32-bit
     * integers as 4 little-endian bytes.
     * <p>
     * Unsigned integers are represented as Java ints with MSB in the sign bit. This means unsigned
     * values greater than 2<sup>31</sup> are represented as negative signed values with the same
     * binary representation as the unsigned values. This allows this method to be used
     * interchangeably for the signed sfixed32 and the unsigned fixed32.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedFixed32(int fieldId, @Nullable Iterable<Integer> values);

    /**
     * Encode an int64 scalar field.
     * <p>
     * With proto-based backends, int64 fields store 64-bit signed longs using a variable
     * length encoding with a compact representation of small positive values, but always uses 10
     * bytes for negative values.
     *
     * @param fieldId the ID of the field.
     * @param value   a field value.
     */
    void encodeInt64(int fieldId, long value);

    /**
     * Encode a repeated int64 scalar field.
     * <p>
     * With proto-based backends, int64 fields store 64-bit signed longs using a variable
     * length encoding with a compact representation of small positive values, but always uses 10
     * bytes for negative values.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedInt64(int fieldId, @Nullable long[] values);

    /**
     * Encode a repeated int64 scalar field.
     * <p>
     * With proto-based backends, int64 fields store 64-bit signed longs using a variable
     * length encoding with a compact representation of small positive values, but always uses 10
     * bytes for negative values.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedInt64(int fieldId, @Nullable Iterable<Long> values);

    /**
     * Encode a sint64 scalar field.
     * <p>
     * With proto-based backends, sint64 fields store 64-bit signed longs using a variable
     * length encoding with a compact representation of values with small absolute values.
     *
     * @param fieldId the ID of the field.
     * @param value   a field value.
     */
    void encodeSInt64(int fieldId, long value);

    /**
     * Encode a repeated sint64 scalar field.
     * <p>
     * With proto-based backends, sint64 fields store 64-bit signed longs using a variable
     * length encoding with a compact representation of values with small absolute values.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedSInt64(int fieldId, @Nullable long[] values);

    /**
     * Encode a repeated sint64 scalar field.
     * <p>
     * With proto-based backends, sint64 fields store 64-bit signed longs using a variable
     * length encoding with a compact representation of values with small absolute values.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedSInt64(int fieldId, @Nullable Iterable<Long> values);

    /**
     * Encode a uint64 scalar field.
     * <p>
     * With proto-based backends, uint64 fields store 64-bit unsigned longs using a variable
     * length encoding with a compact representation of small values.
     * <p>
     * Unsigned integers are represented as Java longs with MSB in the sign bit. This means
     * unsigned values greater than 2<sup>63</sup> are represented as negative signed values with
     * the same binary representation as the unsigned values.
     *
     * @param fieldId the ID of the field.
     * @param value   a field value.
     */
    void encodeUInt64(int fieldId, long value);

    /**
     * Encode a repeated uint64 scalar field.
     * <p>
     * With proto-based backends, uint64 fields store 64-bit unsigned longs using a variable
     * length encoding with a compact representation of small values.
     * <p>
     * Unsigned integers are represented as Java longs with MSB in the sign bit. This means
     * unsigned values greater than 2<sup>63</sup> are represented as negative signed values with
     * the same binary representation as the unsigned values.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedUInt64(int fieldId, @Nullable long[] values);

    /**
     * Encode a repeated uint64 scalar field.
     * <p>
     * With proto-based backends, uint64 fields store 64-bit unsigned longs using a variable
     * length encoding with a compact representation of small values.
     * <p>
     * Unsigned integers are represented as Java longs with MSB in the sign bit. This means
     * unsigned values greater than 2<sup>63</sup> are represented as negative signed values with
     * the same binary representation as the unsigned values.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedUInt64(int fieldId, @Nullable Iterable<Long> values);

    /**
     * Encode a fixed64 or sfixed64 scalar field.
     * <p>
     * With proto-based backends, the unsigned fixed64 and the signed sfixed64 store 64-bit
     * integers as 8 little-endian bytes.
     * <p>
     * Unsigned integers are represented as Java integers with MSB in the sign bit. This means
     * unsigned values greater than 2<sup>63</sup> are represented as negative signed values with
     * the same binary representation as the unsigned values. This allows this method to be used
     * interchangeably for the signed sfixed64 and the unsigned fixed64.
     *
     * @param fieldId the ID of the field.
     * @param value   a field value.
     */
    void encodeFixed64(int fieldId, long value);

    /**
     * Encode a repeated fixed64 or sfixed64 scalar field.
     * <p>
     * With proto-based backends, the unsigned fixed64 and the signed sfixed64 store 64-bit
     * integers as 8 little-endian bytes.
     * <p>
     * Unsigned integers are represented as Java integers with MSB in the sign bit. This means
     * unsigned values greater than 2<sup>63</sup> are represented as negative signed values with
     * the same binary representation as the unsigned values. This allows this method to be used
     * interchangeably for the signed sfixed64 and the unsigned fixed64.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedFixed64(int fieldId, @Nullable long[] values);

    /**
     * Encode a repeated fixed64 or sfixed64 scalar field.
     * <p>
     * With proto-based backends, the unsigned fixed64 and the signed sfixed64 store 64-bit
     * integers as 8 little-endian bytes.
     * <p>
     * Unsigned integers are represented as Java integers with MSB in the sign bit. This means
     * unsigned values greater than 2<sup>63</sup> are represented as negative signed values with
     * the same binary representation as the unsigned values. This allows this method to be used
     * interchangeably for the signed sfixed64 and the unsigned fixed64.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedFixed64(int fieldId, @Nullable Iterable<Long> values);

    /**
     * Encode a string scalar field.
     *
     * @param fieldId the ID of the field.
     * @param value   a field value or null to omit the field.
     */
    void encodeString(int fieldId, @Nullable String value);

    /**
     * Encode a repeated string field.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedString(int fieldId, @Nullable String[] values);

    /**
     * Encode a repeated string scalar field.
     *
     * @param fieldId the ID of the field.
     * @param values  field values or null to omit the field.
     */
    void encodeRepeatedString(int fieldId, @Nullable Iterable<String> values);
}
