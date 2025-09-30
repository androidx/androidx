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

package androidx.navigation3.runtime.serialization

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

/**
 * A [KSerializer] for handling polymorphic [NavKey] hierarchies on Android.
 *
 * This serializer enables `kotlinx.serialization` to save and restore different disparate
 * implementations of [NavKey] without requiring developers to manually register them in a
 * [SerializersModule].
 *
 * ## How It Works
 * Instead of serializing the [NavKey] directly, this class serializes two pieces of information:
 * 1. The fully qualified class name of the concrete [NavKey] (e.g., "com.example.Home").
 * 2. The actual `NavKey` object, using its specific runtime serializer.
 *
 * During deserialization, it reads the class name first, uses reflection (`Class.forName`) to find
 * the corresponding [KSerializer] for that class, and then uses that serializer to deserialize the
 * object data.
 *
 * ## Platform Limitation
 * **This serializer is for Android only.** It relies on JVM reflection (`Class.forName`) which is
 * not available on other platforms in a multiplatform context.
 *
 * @param T The base [NavKey] type this serializer can handle.
 */
@OptIn(InternalSerializationApi::class)
public open class NavKeySerializer<T : NavKey> : KSerializer<T> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor(serialName = "androidx.navigation.runtime.NavKey") {
            element(elementName = "type", descriptor = serialDescriptor<String>())
            element(
                elementName = "value",
                descriptor = buildClassSerialDescriptor(serialName = "Any"),
            )
        }

    /**
     * Deserializes a concrete [NavKey] implementation.
     *
     * It first reads the class name (`type`), then uses that name to find the corresponding
     * [KSerializer] for the class using **reflection**. Finally, it uses that specific serializer
     * to read the actual object data (`value`).
     */
    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): T {
        return decoder.decodeStructure(descriptor) {
            val className = decodeStringElement(descriptor, decodeElementIndex(descriptor))
            val serializer = Class.forName(className).kotlin.serializer()
            decodeSerializableElement(descriptor, decodeElementIndex(descriptor), serializer) as T
        }
    }

    /**
     * Serializes a concrete [NavKey] implementation.
     *
     * It writes the object's fully qualified class name (`type`) first, then uses the object's
     * runtime serializer to write the object's data (`value`).
     */
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
