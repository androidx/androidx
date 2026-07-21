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

package androidx.navigation3.runtime.deeplink

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A [KSerializer] abstract implementation that encodes and decodes [T] as a String.
 *
 * This serializer can be used to serialize non-primitive arguments in navigation keys that are used
 * for deep linking.
 *
 * Compatible with [androidx.compose.runtime.saveable.rememberSerializable], meaning this
 * KSerializer can be used on navigation keys that are saved/restored via
 * [androidx.compose.runtime.saveable.rememberSerializable] or
 * [androidx.navigation3.runtime.rememberNavBackStack]
 *
 * @sample androidx.navigation3.runtime.samples.deeplink.deepLinkSerializerSample
 */
public abstract class DeepLinkSerializer<T : Any> : KSerializer<T> {
    final override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    /** Encode the given [value] as a [String] with the given [encoder]. */
    final override fun serialize(encoder: Encoder, value: T) {
        val stringValue = serialize(value)
        encoder.encodeString(stringValue)
    }

    /** Decodes the string with the given [decoder] into a [T] . */
    final override fun deserialize(decoder: Decoder): T {
        val stringValue = decoder.decodeString()
        return deserialize(stringValue)
    }

    /** The unique serial name that identifies the serializable class [T] */
    public abstract val serialName: String

    /**
     * Decode the given [value] as a [T]
     *
     * In general, the output of this function will be used as the input of [serialize], so their
     * input/output should be compatible.
     */
    public abstract fun deserialize(value: String): T

    /**
     * Encode the given [value] as a [String]
     *
     * In general, the output of this function will be used as the input of [deserialize], so their
     * input/output should be compatible.
     */
    public abstract fun serialize(value: T): String
}
