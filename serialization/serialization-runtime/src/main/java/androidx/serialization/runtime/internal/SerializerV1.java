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
 * Serializes a message class.
 * <p>
 * Serializers are expected to be stateless singletons. The annotation processor generates
 * serializers in the same package as the message class or union annotation with a name derived by
 * replacing the {@code .} of nested classes with {@code _} and adding a {@code Serializer}
 * suffix. In Java, serializers are final classes which expose a singleton instance on a static
 * final {@code INSTANCE} field. In Kotlin, they are implemented as an object declaration.
 * <p>
 * The annotation processor may add static convenience methods to a serializer implementation
 * that obtain an encoder or decoder from a backend and use the singleton instance to encode or
 * decode messages.
 *
 * @param <T> The message class this serializer serializes.
 */
public interface SerializerV1<T> {
    /**
     * Encodes the fields of a message instance to an encoder.
     *
     * @param encoder An encoder which receives the fields of the message.
     * @param message The message to encode.
     */
    void encode(@NonNull EncoderV1 encoder, @NonNull T message);

    /**
     * Decodes an instantiates a new message instance from a decoder.
     * <p>
     * To merge an encoded message with an existing decoded message, supply the instance of the
     * message to the mergeFrom parameter. The new message's fields will default to the values of
     * the existing message's fields. Singular fields will be replaced with any values present in
     * the encoded message, and collection or array fields will be concatenated with encoded
     * values. This functionality is required for inter-operation with other encoders which may
     * split embedded messages into multiple chunks or distribute repeated field entries
     * throughout the body of an encoded message.
     *
     * @param decoder A decoder to read from.
     * @param mergeFrom An optional message instance to merge with the encoded message.
     * @return A new instance of the message class.
     */
    @NonNull
    T decode(@NonNull DecoderV1 decoder, @Nullable T mergeFrom);
}
