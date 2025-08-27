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

package androidx.navigation3.runtime

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.savedstate.compose.serialization.serializers.SnapshotStateListSerializer
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer

@Suppress("FunctionName") // Factory function.
public actual inline fun <reified T : Any> NavBackStackSerializer(
    configuration: SavedStateConfiguration
): KSerializer<SnapshotStateList<T>> {
    val elementSerializer =
        if (configuration == SavedStateConfiguration.DEFAULT) {
            // DEFAULT uses a reflective polymorphic strategy. This is self-contained and
            // does not require registering subtypes or passing a SerializersModule when
            // encoding/decoding. It uses reflection to resolve `.serializer()` for each type.
            ReflectivePolymorphicSerializer()
        } else {
            // If a custom configuration is provided, we use its serializersModule to resolve
            // the element serializer. In this path, you MUST also pass the same configuration
            // (or at least its serializersModule) to your encode/decode calls. This is because
            // polymorphic dispatch looks up subtypes in the Encoder/Decoder’s module, not in
            // the KSerializer instance itself.
            configuration.serializersModule.serializer<T>()
        }

    return SnapshotStateListSerializer(elementSerializer)
}

/**
 * A [KSerializer] that enables polymorphic serialization for navigation back stack entries on
 * Android using reflection, without requiring subtypes to be pre-registered.
 *
 * ## The Problem This Solves
 * Standard `kotlinx.serialization` polymorphism requires registering all possible subtypes in a
 * `SerializersModule`. For navigation, where screen destinations are often defined across different
 * modules, this is impractical and creates tight coupling.
 *
 * ## How It Works
 * This serializer circumvents the registration requirement by storing the fully-qualified class
 * name of the object alongside its serialized data. During deserialization, it uses reflection
 * (`Class.forName`) to find the class and its default serializer.
 *
 * This is an internal implementation detail and should not be used directly.
 */
@OptIn(InternalSerializationApi::class)
@PublishedApi
internal class ReflectivePolymorphicSerializer<T : Any> : KSerializer<T> {

    override val descriptor =
        buildClassSerialDescriptor("PolymorphicData") {
            element(elementName = "type", serialDescriptor<String>())
            element(elementName = "payload", buildClassSerialDescriptor("Any"))
        }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): T {
        return decoder.decodeStructure(descriptor) {
            val className = decodeStringElement(descriptor, decodeElementIndex(descriptor))
            val classRef = Class.forName(className).kotlin
            val serializer = classRef.serializer()

            decodeSerializableElement(descriptor, decodeElementIndex(descriptor), serializer) as T
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeStructure(descriptor) {
            val className = value::class.java.name
            encodeStringElement(descriptor, index = 0, className)
            val serializer = value::class.serializer() as KSerializer<T>
            encodeSerializableElement(descriptor, index = 1, serializer, value)
        }
    }
}
