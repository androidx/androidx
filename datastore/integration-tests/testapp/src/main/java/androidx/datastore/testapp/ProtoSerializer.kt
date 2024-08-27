/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.datastore.testapp

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.ExtensionRegistryLite
import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.MessageLite
import java.io.InputStream
import java.io.OutputStream

/** Serializer for using DataStore with protos. */
internal class ProtoSerializer<T : MessageLite>(
    /** The default proto of this type, obtained via {@code T.getDefaultInstance()} */
    override val defaultValue: T,
    /**
     * Set the extensionRegistryLite to use when deserializing T. If no extension registry is
     * necessary, use {@code ExtensionRegistryLite.getEmptyRegistry()}.
     */
    private val extensionRegistryLite: ExtensionRegistryLite
) : Serializer<T> {

    @Suppress("UNCHECKED_CAST")
    override suspend fun readFrom(input: InputStream): T {
        try {
            return defaultValue.parserForType.parseFrom(input, extensionRegistryLite) as T
        } catch (invalidProtocolBufferException: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", invalidProtocolBufferException)
        }
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        t.writeTo(output)
    }
}
