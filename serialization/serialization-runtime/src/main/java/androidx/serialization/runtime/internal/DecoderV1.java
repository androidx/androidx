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

import java.nio.ByteBuffer;
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
     * @param serializer A serializer for the message class.
     * @param mergeFrom  An optional message to merge fields.
     * @param <T>        The message class.
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return A decoded instance of the message.
     */
    @NonNull
    <T> T decodeMessage(
            @NonNull SerializerV1<T> serializer,
            @Nullable T mergeFrom
    );

    /**
     * Decode a repeated embedded message field.
     *
     * @param serializer A serializer for the message class.
     * @param mergeFrom  An optional collection to concatenate.
     * @param factory    Factory for instantiating the collection.
     * @param <T>        The message class.
     * @param <C>        The collection class.
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return A collection containing decoded messages.
     */
    @NonNull
    <T, C extends Collection<T>> C decodeMessageCollection(
            @NonNull SerializerV1<T> serializer,
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode an enum field.
     *
     * @param serializer An enum serializer.
     * @param <T>        The enum class.
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return A decoded enum value.
     */
    @NonNull
    <T extends Enum<T>> T decodeEnum(@NonNull EnumSerializerV1<T> serializer);

    /**
     * Decode a repeated enum field.
     *
     * @param serializer An enum serializer.
     * @param mergeFrom  An optional collection to concatenate.
     * @param factory    Factory for instantiating the collection.
     * @param <T>        The enum class.
     * @param <C>        The collection class.
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return A collection containing decoded enum values.
     */
    @NonNull
    <T extends Enum<T>, C extends Collection<T>> C decodeEnumCollection(
            @NonNull EnumSerializerV1<T> serializer,
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode a boolean field.
     *
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return Boolean field value.
     */
    boolean decodeBoolean();

    /**
     * Decode a repeated boolean field.
     *
     * @param mergeFrom An optional array to concatenate.
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return An array of boolean field values.
     */
    @NonNull
    boolean[] decodeBooleanArray(@Nullable boolean[] mergeFrom);

    /**
     * Decode a repeated boolean field.
     *
     * @param mergeFrom An optional collection to concatenate.
     * @param factory   Factory for instantiating the collection.
     * @param <C>       The collection class.
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return A collection of boolean field values.
     */
    @NonNull
    <C extends Collection<Boolean>> C decodeBoolCollection(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode a binary field.
     *
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return Binary field value.
     */
    @NonNull
    byte[] decodeByteArray();

    /**
     * Decode a binary field.
     *
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return Binary field value.
     */
    @NonNull
    ByteBuffer decodeByteBuffer();

    /**
     * Decode a repeated binary field.
     *
     * @param mergeFrom An optional collection to concatenate.
     * @param factory   Factory for instantiating the collection.
     * @param <C>       The collection class.
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return A collection of binary field values.
     */
    @NonNull
    <C extends Collection<byte[]>> C decodeByteArrayCollection(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode a repeated binary field.
     *
     * @param mergeFrom An optional collection to concatenate.
     * @param factory   Factory for instantiating the collection.
     * @param <C>       The collection class.
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return A collection of binary field values.
     */
    @NonNull
    <C extends Collection<ByteBuffer>> C decodeByteBufferCollection(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode a double field.
     *
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return Double field value.
     */
    double decodeDouble();

    /**
     * Decode a repeated double field.
     *
     * @param mergeFrom An optional array to concatenate.
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return An array of double field values.
     */
    @NonNull
    double[] decodeDoubleArray(@Nullable double[] mergeFrom);

    /**
     * Decode a repeated double field.
     *
     * @param mergeFrom An optional collection to concatenate.
     * @param factory   Factory for instantiating the collection.
     * @param <C>       The collection class.
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return A collection of double field values.
     */
    @NonNull
    <C extends Collection<Double>> C decodeDoubleCollection(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode a float field.
     *
     * @return Float field value.
     */
    float decodeFloat();

    /**
     * Decode a repeated float field.
     *
     * @param mergeFrom An optional array to concatenate or null.
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return An array of float field values.
     */
    @NonNull
    float[] decodeFloatArray(@Nullable float[] mergeFrom);

    /**
     * Decode a repeated float field.
     *
     * @param mergeFrom An optional collection to concatenate or null.
     * @param factory   Factory for instantiating the collection.
     * @param <C>       The collection class.
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return A collection of float field values.
     */
    @NonNull
    <C extends Collection<Float>> C decodeFloatCollection(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode an integer field.
     *
     * @param encoding Encoding of the proto representation of the field.
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return Integer field value.
     */
    int decodeInt(@IntEncoding int encoding);

    /**
     * Decode a repeated integer field.
     *
     * @param encoding  Encoding of the proto representation of the field.
     * @param mergeFrom An optional array to concatenate.
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return An array of integer field values.
     */
    @NonNull
    int[] decodeIntArray(@IntEncoding int encoding, @Nullable int[] mergeFrom);

    /**
     * Decode a repeated integer field.
     *
     * @param encoding  Encoding of the proto representation of the field.
     * @param mergeFrom An optional collection to concatenate.
     * @param factory   Factory for instantiating the collection.
     * @param <C>       The collection class.
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return A collection of integer field values.
     */
    @NonNull
    <C extends Collection<Integer>> C decodeIntCollection(
            @IntEncoding int encoding,
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode a long field.
     *
     * @param encoding Encoding of the proto representation of the field.
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return Long field value.
     */
    long decodeLong(@IntEncoding int encoding);

    /**
     * Decode a repeated long field.
     *
     * @param encoding  Encoding of the proto representation of the field.
     * @param mergeFrom An optional array to concatenate.
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return An array of long field values.
     */
    @NonNull
    long[] decodeLongArray(@IntEncoding int encoding, @Nullable long[] mergeFrom);

    /**
     * Decodes a repeated long field.
     *
     * @param encoding  Encoding of the proto representation of the field.
     * @param mergeFrom An optional collection to concatenate.
     * @param factory   Factory for instantiating the collection.
     * @param <C>       The collection class.
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return A collection of long field values.
     */
    @NonNull
    <C extends Collection<Long>> C decodeLongCollection(
            @IntEncoding int encoding,
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );

    /**
     * Decode a string field.
     *
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return String field value.
     */
    @NonNull
    String decodeString();

    /**
     * Decode a repeated string field.
     *
     * @param mergeFrom An optional array to concatenate.
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return An array of string field values.
     */
    @NonNull
    String[] decodeStringArray(@Nullable String[] mergeFrom);

    /**
     * Decode a repeated string field.
     *
     * @param mergeFrom An optional collection to concatenate.
     * @param factory   Factory for instantiating the collection.
     * @param <C>       The collection class.
     * @throws IllegalStateException If iteration is not currently positioned on a field.
     * @return A collection of string field values.
     */
    @NonNull
    <C extends Collection<String>> C decodeStringCollection(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    );
}
