/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.savedstate.serialization.utils

import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.modules.SerializersModule

@Serializable internal data class ContextualData(@Contextual val value: ContextualType)

internal data class ContextualType(val value1: String, val value2: String)

private object ContextualTypeSerializer : KSerializer<ContextualType> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("ContextualTypeSerializer") {
            element("value", String.serializer().descriptor)
        }

    override fun serialize(encoder: Encoder, value: ContextualType) {
        encoder.encodeStructure(descriptor) {
            val value1 = value.value1
            val value2 = value.value2
            encodeStringElement(descriptor, 0, "$value1#$value2")
        }
    }

    override fun deserialize(decoder: Decoder): ContextualType {
        lateinit var value: String
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (decodeElementIndex(descriptor)) {
                    0 -> value = decodeStringElement(descriptor, 0)
                    else -> break
                }
            }
        }
        val value1 = value.substringBefore("#")
        val value2 = value.substringAfter("#")
        return ContextualType(value1, value2)
    }
}

internal val contextualTestModule = SerializersModule {
    contextual(ContextualType::class, ContextualTypeSerializer)
}
