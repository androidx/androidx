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
 * {@link #encodeMessageCollection(int, SerializerV1, Iterable)} for embedded messages.
 * <p>
 * Encoding for embedded messages, such as {@link #encodeMessage(int, SerializerV1, Object)}, is
 * likewise implemented in a unit of a whole field. This gives implementations flexibility in how
 * they invoke the serializer. The API is designed such that an implementation may store some
 * state on the stack and pass itself to the serializer, or at the implementor's discretion, it
 * could instantiate and pass a new encoder to the serializer.
 */
public interface EncoderV1 {
    /**
     * Encode an embedded message field.
     *
     * @param fieldId    ID of the field.
     * @param serializer A serializer for the message class.
     * @param message    An instance of the message class or null to omit the field.
     * @param <T>        The message class.
     */
    <T> void encodeMessage(
            int fieldId,
            @NonNull SerializerV1<T> serializer,
            @Nullable T message
    );

    /**
     * Encode a repeated embedded message field.
     *
     * @param fieldId    ID of the field.
     * @param serializer A serializer for the message class.
     * @param messages   An iterable of message class instances or null to omit the field.
     * @param <T>        The message class.
     */
    <T> void encodeMessageCollection(
            int fieldId,
            @NonNull SerializerV1<T> serializer,
            @Nullable Iterable<T> messages
    );

    /**
     * Encode an enum field.
     *
     * @param fieldId    ID of the field.
     * @param serializer A serializer for the enum class.
     * @param value      An enum value or null to omit the field.
     * @param <T>        The enum class.
     */
    <T extends Enum<T>> void encodeEnum(
            int fieldId,
            @NonNull EnumSerializerV1<T> serializer,
            @Nullable T value
    );

    /**
     * Encode a repeated enum field.
     *
     * @param fieldId    ID of the field.
     * @param serializer A serializer for the enum class.
     * @param values     Enum values or null to omit the field.
     * @param <T>        The enum class.
     */
    <T extends Enum<T>> void encodeEnumCollection(
            int fieldId,
            @NonNull EnumSerializerV1<T> serializer,
            @Nullable Iterable<T> values
    );

    /**
     * Encode a boolean field.
     *
     * @param fieldId ID of the field.
     * @param value   Boolean field value.
     */
    void encodeBoolean(int fieldId, boolean value);

    /**
     * Encode a repeated boolean field.
     *
     * @param fieldId ID of the field.
     * @param values  Boolean field values or null to omit the field.
     */
    void encodeBooleanArray(int fieldId, @Nullable boolean[] values);

    /**
     * Encode a repeated boolean field.
     *
     * @param fieldId ID of the field.
     * @param values  Boolean field values or null to omit the field.
     */
    void encodeBooleanCollection(int fieldId, @Nullable Iterable<Boolean> values);

    /**
     * Encode a binary field.
     *
     * @param fieldId ID of the field.
     * @param value   Binary field value or null to omit the field.
     */
    void encodeByteArray(int fieldId, @Nullable byte[] value);

    /**
     * Encode a repeated binary field.
     *
     * @param fieldId ID of the field.
     * @param values  Binary field values or null to omit the field.
     */
    void encodeByteArrayCollection(int fieldId, @Nullable Iterable<byte[]> values);

    /**
     * Encode a double field.
     *
     * @param fieldId ID of the field.
     * @param value   Double field value.
     */
    void encodeDouble(int fieldId, double value);

    /**
     * Encode a repeated double field.
     *
     * @param fieldId ID of the field
     * @param values  Double field values or null to omit the field.
     */
    void encodeDoubleArray(int fieldId, @Nullable double[] values);

    /**
     * Encode a repeated double field.
     *
     * @param fieldId ID of the field
     * @param values  Double field values or null to omit the field.
     */
    void encodeDoubleCollection(int fieldId, @Nullable Iterable<Double> values);

    /**
     * Encode a float field.
     *
     * @param fieldId ID of the field.
     * @param value   Float field value.
     */
    void encodeFloat(int fieldId, float value);

    /**
     * Encode a repeated float field.
     *
     * @param fieldId ID of the field.
     * @param values  Float field values or null to omit the field.
     */
    void encodeFloatArray(int fieldId, @Nullable float[] values);

    /**
     * Encode a repeated float field.
     *
     * @param fieldId ID of the field.
     * @param values  Float field values or null to omit the field.
     */
    void encodeFloatCollection(int fieldId, @Nullable Iterable<Float> values);

    /**
     * Encode an integer field.
     *
     * @param fieldId  ID of the field.
     * @param encoding Encoding of the proto representation of the field.
     * @param value    Integer field value.
     */
    void encodeInt(int fieldId, @IntEncoding int encoding, int value);

    /**
     * Encode a repeated integer field.
     *
     * @param fieldId  ID of the field.
     * @param encoding Encoding of the proto representation of the field.
     * @param values   Integer field values or null to omit the field.
     */
    void encodeIntArray(int fieldId, @IntEncoding int encoding, @Nullable int[] values);

    /**
     * Encode a repeated integer field.
     *
     * @param fieldId  ID of the field.
     * @param encoding Encoding of the proto representation of the field.
     * @param values   Integer field values or null to omit the field.
     */
    void encodeIntCollection(
            int fieldId,
            @IntEncoding int encoding,
            @Nullable Iterable<Integer> values
    );

    /**
     * Encode a long field.
     *
     * @param fieldId  ID of the field.
     * @param encoding Encoding of the proto representation of the field.
     * @param value    Long field value.
     */
    void encodeLong(int fieldId, @IntEncoding int encoding, long value);

    /**
     * Encode a repeated long field.
     *
     * @param fieldId  ID of the field.
     * @param encoding Encoding of the proto representation of the field.
     * @param values   Long field values or null to omit the field.
     */
    void encodeLongArray(int fieldId, @IntEncoding int encoding, @Nullable long[] values);

    /**
     * Encode a repeated long field.
     *
     * @param fieldId  ID of the field.
     * @param encoding Encoding of the proto representation of the field.
     * @param values   Long field values or null to omit the field.
     */
    void encodeLongCollection(
            int fieldId,
            @IntEncoding int encoding,
            @Nullable Iterable<Long> values
    );

    /**
     * Encode a string field.
     *
     * @param fieldId ID of the field.
     * @param value   String field value or null to omit the field.
     */
    void encodeString(int fieldId, @Nullable String value);

    /**
     * Encode a repeated string field.
     *
     * @param fieldId ID of the field.
     * @param values  String field values or null to omit the field.
     */
    void encodeStringArray(int fieldId, @Nullable String[] values);

    /**
     * Encode a repeated string field.
     *
     * @param fieldId ID of the field.
     * @param values  String field values or null to omit the field.
     */
    void encodeStringCollection(int fieldId, @Nullable Iterable<String> values);
}
