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

import java.util.Collection;

/**
 * A decoder provides the fields of an encoded message to a serializer.
 * <p>
 * Decoders provide access to the fields of an encoded message in the order in which they were
 * encoded. As the wire format is not self-describing, decoding a field requires a serializer to
 * know what type to decode the field into. A serializer is expected to iterate through field IDs
 * using {@link #nextFieldId()} and {@link #nextFieldId()}. The serializer looks up the field,
 * such as with a switch statement, and calls the appropriate decode method for the fields type
 * to obtain the value. Calling decode methods at other times may result in an
 * {@link IllegalStateException}.
 * <p>
 * While Serialization does not split embedded messages or distribute repeated field entries
 * throughout an encoded message, serializers and decoders must be prepared to decode messages
 * encoded by other encoders. To reduce the burden on generated code, all array and collection
 * decoding methods accept a nullable {@code mergeFrom} parameter. The decoder is responsible for
 * appropriately concatenating arrays or appending to existing collections. Similarly, the
 * {@link #decodeMessage(SerializerV1, Object)} method accepts a nullable message to merge into
 * the decoded message for non-repeated embedded message fields.
 * <p>
 * Collection decoding methods accept a {@link CollectionFactory} lambda. When possible, decoder
 * implementations pre-compute the number of items in a collection, and supply it to the factory
 * to be used as the initial capacity of the collection. This enables the decoder to decode
 * fields of any collection type, while avoiding intermediary allocations.
 * <p>
 * Decoders include distinct methods for all Protocol Buffers integer types. This makes for
 * clearer serializer code and enables code shrinking tools such as R8 to more effectively remove
 * unused integer encodings.
 */
public interface DecoderV1 {
    /**
     * Determine if this decoder has a next field available.
     *
     * @return True if another field is available to decode.
     */
    boolean hasNextField();

    /**
     * Get the next field ID in this decoder's current message, skipping the current field.
     *
     * @throws java.util.NoSuchElementException If no more fields are available.
     * @return Next available field ID.
     */
    int nextFieldId();

    /**
     * Decode an embedded message field.
     *
     * @param serializer the serializer for the message class.
     * @param mergeFrom  an optional message to merge fields.
     * @param <T>        the message class.
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field value as a new instance of the message class.
     */
    @NonNull
    <T> T decodeMessage(
            @NonNull SerializerV1<T> serializer,
            @Nullable T mergeFrom
    );

    /**
     * Decode a repeated embedded message field.
     *
     * @param serializer the serializer for the message class.
     * @param mergeFrom  an optional collection to concatenate.
     * @param factory    a factory for instantiating the collection.
     * @param <T>        the message class.
     * @param <C>        the collection class.
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field values as a collection of new instances of the message class.
     */
    @NonNull
    <T, C extends Collection<T>> C decodeRepeatedMessage(
            @NonNull SerializerV1<T> serializer,
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode an enum field.
     *
     * @param serializer the serializer for the enum class.
     * @param <T>        the enum class.
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field value as an enum value.
     */
    @NonNull
    <T extends Enum<T>> T decodeEnum(@NonNull EnumSerializerV1<T> serializer);

    /**
     * Decode a repeated enum field.
     *
     * @param serializer the enum serializer.
     * @param mergeFrom  an optional collection to concatenate.
     * @param factory    a factory for instantiating the collection.
     * @param <T>        the enum class.
     * @param <C>        the collection class.
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field values as a collection of enum value.
     */
    @NonNull <T extends Enum<T>, C extends Collection<T>> C decodeRepeatedEnum(
            @NonNull EnumSerializerV1<T> serializer,
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode a bool scalar field.
     *
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field value.
     */
    boolean decodeBool();

    /**
     * Decode a repeated bool scalar field.
     *
     * @param mergeFrom an optional array to concatenate.
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field values as an boolean array.
     */
    @NonNull
    boolean[] decodeRepeatedBool(@Nullable boolean[] mergeFrom);

    /**
     * Decode a repeated bool scalar field.
     *
     * @param mergeFrom an optional collection to concatenate.
     * @param factory   a factory for instantiating the collection.
     * @param <C>       the collection class.
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field values as a collection of boxed booleans.
     */
    @NonNull
    <C extends Collection<Boolean>> C decodeRepeatedBool(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode a bytes scalar field.
     *
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field value as a byte array.
     */
    @NonNull
    byte[] decodeBytes();

    /**
     * Decode a repeated bytes scalar field.
     *
     * @param mergeFrom an optional collection to concatenate.
     * @param factory   a factory for instantiating the collection.
     * @param <C>       the collection class.
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field values as a collection of byte arrays.
     */
    @NonNull
    <C extends Collection<byte[]>> C decodeRepeatedBytes(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode a double scalar field.
     *
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field value.
     */
    double decodeDouble();

    /**
     * Decode a repeated double scalar field.
     *
     * @param mergeFrom an optional array to concatenate.
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field values as a double array.
     */
    @NonNull
    double[] decodeRepeatedDouble(@Nullable double[] mergeFrom);

    /**
     * Decode a repeated double scalar field.
     *
     * @param mergeFrom an optional collection to concatenate.
     * @param factory   a factory for instantiating the collection.
     * @param <C>       The collection class.
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field values as a collection of boxed doubles.
     */
    @NonNull
    <C extends Collection<Double>> C decodeRepeatedDouble(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode a float scalar field.
     *
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field value.
     */
    float decodeFloat();

    /**
     * Decode a repeated float scalar field.
     *
     * @param mergeFrom an optional array to concatenate or null.
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field values as a float array.
     */
    @NonNull
    float[] decodeRepeatedFloat(@Nullable float[] mergeFrom);

    /**
     * Decode a repeated float scalar field.
     *
     * @param mergeFrom an optional collection to concatenate or null.
     * @param factory   a factory for instantiating the collection.
     * @param <C>       the collection class.
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field values as a collection of boxed floats.
     */
    @NonNull
    <C extends Collection<Float>> C decodeRepeatedFloat(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode an int32 scalar field.
     * <p>
     * With proto-based backends, int32 fields store 32-bit signed integers using a variable
     * length encoding with a compact representation of small positive values, but always uses 10
     * bytes for negative values.
     *
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field value.
     */
    int decodeInt32();

    /**
     * Decode a repeated int32 scalar field.
     * <p>
     * With proto-based backends, int32 fields store 32-bit signed integers using a variable
     * length encoding with a compact representation of small positive values, but always uses 10
     * bytes for negative values.
     *
     * @param mergeFrom an optional array to concatenate or null.
     * @return the field values as an int array.
     */
    @NonNull
    int[] decodeRepeatedInt32(@Nullable int[] mergeFrom);

    /**
     * Decode a repeated int32 scalar field.
     * <p>
     * With proto-based backends, int32 fields store 32-bit signed integers using a variable
     * length encoding with a compact representation of small positive values, but always uses 10
     * bytes for negative values.
     *
     * @param mergeFrom an optional collection to concatenate or null.
     * @param factory   a factory for instantiating the collection.
     * @param <C>       the collection class.
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field values as a collection of boxed integers.
     */
    @NonNull
    <C extends Collection<Integer>> C decodeRepeatedInt32(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode a sint32 scalar field.
     * <p>
     * With proto-based backends, sint32 fields store 32-bit signed integers using a variable
     * length encoding with a compact representation of values with small absolute values.
     *
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field value.
     */
    int decodeSInt32();

    /**
     * Decode a repeated sint32 scalar field.
     * <p>
     * With proto-based backends, sint32 fields store 32-bit signed integers using a variable
     * length encoding with a compact representation of values with small absolute values.
     *
     * @param mergeFrom an optional array to concatenate or null.
     * @return the field values as an int array.
     */
    @NonNull
    int[] decodeRepeatedSInt32(@Nullable int[] mergeFrom);

    /**
     * Decode a repeated sint32 scalar field.
     * <p>
     * With proto-based backends, sint32 fields store 32-bit signed integers using a variable
     * length encoding with a compact representation of values with small absolute values.
     *
     * @param mergeFrom an optional collection to concatenate or null.
     * @param factory   a factory for instantiating the collection.
     * @param <C>       the collection class.
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field values as a collection of boxed integers.
     */
    @NonNull
    <C extends Collection<Integer>> C decodeRepeatedSInt32(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode a uint32 scalar field.
     * <p>
     * With proto-based backends, uint32 fields store 32-bit unsigned integers using a variable
     * length encoding with a compact representation of small values.
     * <p>
     * Unsigned integers are represented as Java integers with MSB in the sign bit. This means
     * unsigned
     * values greater than 2<sup>31</sup> are represented as negative signed values with the same
     * binary representation as the unsigned values.
     *
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field value.
     */
    int decodeUInt32();

    /**
     * Decode a repeated uint32 scalar field.
     * <p>
     * With proto-based backends, uint32 fields store 32-bit unsigned integers using a variable
     * length encoding with a compact representation of small values.
     * <p>
     * Unsigned integers are represented as Java integers with MSB in the sign bit. This means
     * unsigned
     * values greater than 2<sup>31</sup> are represented as negative signed values with the same
     * binary representation as the unsigned values.
     *
     * @param mergeFrom an optional array to concatenate or null.
     * @return the field values as an int array.
     */
    @NonNull
    int[] decodeRepeatedUInt32(@Nullable int[] mergeFrom);

    /**
     * Decode a repeated uint32 scalar field.
     * <p>
     * With proto-based backends, uint32 fields store 32-bit unsigned integers using a variable
     * length encoding with a compact representation of small values.
     * <p>
     * Unsigned integers are represented as Java integers with MSB in the sign bit. This means
     * unsigned
     * values greater than 2<sup>31</sup> are represented as negative signed values with the same
     * binary representation as the unsigned values.
     *
     * @param mergeFrom an optional collection to concatenate or null.
     * @param factory   a factory for instantiating the collection.
     * @param <C>       the collection class.
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field values as a collection of boxed integers.
     */
    @NonNull
    <C extends Collection<Integer>> C decodeRepeatedUInt32(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode a fixed32 or sfixed32 scalar field.
     * <p>
     * With proto-based backends, the unsigned fixed32 and the signed sfixed32 store 32-bit
     * integers as 4 little-endian bytes.
     * <p>
     * Unsigned integers are represented as Java integers with MSB in the sign bit. This means
     * unsigned values greater than 2<sup>31</sup> are represented as negative signed values with
     * the same binary representation as the unsigned values. This allows this method to be used
     * interchangeably for the signed sfixed32 and the unsigned fixed32.
     *
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field value.
     */
    int decodeFixed32();

    /**
     * Decode a repeated fixed32 or sfixed32 scalar field.
     * <p>
     * With proto-based backends, the unsigned fixed32 and the signed sfixed32 store 32-bit
     * integers as 4 little-endian bytes.
     * <p>
     * Unsigned integers are represented as Java integers with MSB in the sign bit. This means
     * unsigned values greater than 2<sup>31</sup> are represented as negative signed values with
     * the same binary representation as the unsigned values. This allows this method to be used
     * interchangeably for the signed sfixed32 and the unsigned fixed32.
     *
     * @param mergeFrom an optional array to concatenate or null.
     * @return the field values as an int array.
     */
    @NonNull
    int[] decodeRepeatedFixed32(@Nullable int[] mergeFrom);

    /**
     * Decode a repeated fixed32 or sfixed32 scalar field.
     * <p>
     * With proto-based backends, the unsigned fixed32 and the signed sfixed32 store 32-bit
     * integers as 4 little-endian bytes.
     * <p>
     * Unsigned integers are represented as Java integers with MSB in the sign bit. This means
     * unsigned values greater than 2<sup>31</sup> are represented as negative signed values with
     * the same binary representation as the unsigned values. This allows this method to be used
     * interchangeably for the signed sfixed32 and the unsigned fixed32.
     *
     * @param mergeFrom an optional collection to concatenate or null.
     * @param factory   a factory for instantiating the collection.
     * @param <C>       the collection class.
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field values as a collection of boxed integers.
     */
    @NonNull
    <C extends Collection<Integer>> C decodeRepeatedFixed32(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode an int64 scalar field.
     * <p>
     * With proto-based backends, int64 fields store 64-bit signed longs using a variable
     * length encoding with a compact representation of small positive values, but always uses 10
     * bytes for negative values.
     *
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field value.
     */
    long decodeInt64();

    /**
     * Decode a repeated int64 scalar field.
     * <p>
     * With proto-based backends, int64 fields store 64-bit signed longs using a variable
     * length encoding with a compact representation of small positive values, but always uses 10
     * bytes for negative values.
     *
     * @param mergeFrom an optional array to concatenate or null.
     * @return the field values as a long array.
     */
    @NonNull
    long[] decodeRepeatedInt64(@Nullable long[] mergeFrom);

    /**
     * Decode a repeated int64 scalar field.
     * <p>
     * With proto-based backends, int64 fields store 64-bit signed longs using a variable
     * length encoding with a compact representation of small positive values, but always uses 10
     * bytes for negative values.
     *
     * @param mergeFrom an optional collection to concatenate or null.
     * @param factory   a factory for instantiating the collection.
     * @param <C>       the collection class.
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field values as a collection of boxed longs.
     */
    @NonNull
    <C extends Collection<Long>> C decodeRepeatedInt64(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode a sint64 scalar field.
     * <p>
     * With proto-based backends, sint64 fields store 64-bit signed longs using a variable
     * length encoding with a compact representation of values with small absolute values.
     *
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field value.
     */
    long decodeSInt64();

    /**
     * Decode a repeated sint64 scalar field.
     * <p>
     * With proto-based backends, sint64 fields store 64-bit signed longs using a variable
     * length encoding with a compact representation of values with small absolute values.
     *
     * @param mergeFrom an optional array to concatenate or null.
     * @return the field values as a long array.
     */
    @NonNull
    long[] decodeRepeatedSInt64(@Nullable long[] mergeFrom);

    /**
     * Decode a repeated sint64 scalar field.
     * <p>
     * With proto-based backends, sint64 fields store 64-bit signed longs using a variable
     * length encoding with a compact representation of values with small absolute values.
     *
     * @param mergeFrom an optional collection to concatenate or null.
     * @param factory   a factory for instantiating the collection.
     * @param <C>       the collection class.
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field values as a collection of boxed longs.
     */
    @NonNull
    <C extends Collection<Long>> C decodeRepeatedSInt64(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode a uint64 scalar field.
     * <p>
     * With proto-based backends, uint64 fields store 64-bit unsigned longs using a variable
     * length encoding with a compact representation of small values.
     * <p>
     * Unsigned integers are represented as Java longs with MSB in the sign bit. This means
     * unsigned values greater than 2<sup>63</sup> are represented as negative signed values with
     * the same binary representation as the unsigned values.
     *
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field value.
     */
    long decodeUInt64();

    /**
     * Decode a repeated uint64 scalar field.
     * <p>
     * With proto-based backends, sint64 fields store 64-bit unsigned longs using a variable
     * length encoding with a compact representation of small values.
     * <p>
     * Unsigned integers are represented as Java longs with MSB in the sign bit. This means
     * unsigned values greater than 2<sup>63</sup> are represented as negative signed values with
     * the same binary representation as the unsigned values.
     *
     * @param mergeFrom an optional array to concatenate or null.
     * @return the field values as an long array.
     */
    @NonNull
    long[] decodeRepeatedUInt64(@Nullable long[] mergeFrom);

    /**
     * Decode a repeated uint64 scalar field.
     * <p>
     * With proto-based backends, uint64 fields store 64-bit unsigned longs using a variable
     * length encoding with a compact representation of small values.
     * <p>
     * Unsigned integers are represented as Java longs with MSB in the sign bit. This means
     * unsigned values greater than 2<sup>63</sup> are represented as negative signed values with
     * the same binary representation as the unsigned values.
     *
     * @param mergeFrom an optional collection to concatenate or null.
     * @param factory   a factory for instantiating the collection.
     * @param <C>       the collection class.
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field values as a collection of boxed integers.
     */
    @NonNull
    <C extends Collection<Long>> C decodeRepeatedUInt64(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode a fixed64 or sfixed64 scalar field.
     * <p>
     * With proto-based backends, the unsigned fixed64 and the signed sfixed64 store 64-bit
     * integers as 8 little-endian bytes.
     * <p>
     * Unsigned integers are represented as Java integers with MSB in the sign bit. This means
     * unsigned values greater than 2<sup>63</sup> are represented as negative signed values with
     * the same binary representation as the unsigned values. This allows this method to be used
     * interchangeably for the signed sfixed64 and the unsigned fixed64.
     *
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field value.
     */
    long decodeFixed64();

    /**
     * Decode a repeated fixed64 or sfixed64 scalar field.
     * <p>
     * With proto-based backends, the unsigned fixed64 and the signed sfixed64 store 64-bit
     * integers as 8 little-endian bytes.
     * <p>
     * Unsigned integers are represented as Java integers with MSB in the sign bit. This means
     * unsigned values greater than 2<sup>63</sup> are represented as negative signed values with
     * the same binary representation as the unsigned values. This allows this method to be used
     * interchangeably for the signed sfixed64 and the unsigned fixed64.
     *
     * @param mergeFrom an optional array to concatenate or null.
     * @return the field values as an int array.
     */
    @NonNull
    long[] decodeRepeatedFixed64(@Nullable long[] mergeFrom);

    /**
     * Decode a repeated fixed64 or sfixed64 scalar field.
     * <p>
     * With proto-based backends, the unsigned fixed64 and the signed sfixed64 store 64-bit
     * integers as 8 little-endian bytes.
     * <p>
     * Unsigned integers are represented as Java integers with MSB in the sign bit. This means
     * unsigned values greater than 2<sup>63</sup> are represented as negative signed values with
     * the same binary representation as the unsigned values. This allows this method to be used
     * interchangeably for the signed sfixed64 and the unsigned fixed64.
     *
     * @param mergeFrom an optional collection to concatenate or null.
     * @param factory   a factory for instantiating the collection.
     * @param <C>       the collection class.
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field values as a collection of boxed integers.
     */
    @NonNull
    <C extends Collection<Long>> C decodeRepeatedFixed64(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode a string scalar field.
     *
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field value.
     */
    @NonNull
    String decodeString();

    /**
     * Decode a repeated string scalar field.
     *
     * @param mergeFrom an optional array to concatenate.
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field values as a string array.
     */
    @NonNull
    String[] decodeRepeatedString(@Nullable String[] mergeFrom);

    /**
     * Decode a repeated string scalar field.
     *
     * @param mergeFrom an optional collection to concatenate.
     * @param factory   a factory for instantiating the collection.
     * @param <C>       the collection class.
     * @throws IllegalStateException if this decoder is not currently positioned on a field.
     * @return the field values as a collection of strings.
     */
    @NonNull
    <C extends Collection<String>> C decodeRepeatedString(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );
}
