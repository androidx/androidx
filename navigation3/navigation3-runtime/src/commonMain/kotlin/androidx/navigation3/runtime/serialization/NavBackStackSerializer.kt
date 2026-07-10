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

import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.compose.serialization.serializers.SnapshotStateListSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

/**
 * A [KSerializer] for [NavBackStack].
 *
 * This serializer wraps a [KSerializer] for the element type [T], enabling serialization and
 * deserialization of [NavBackStack] instances. The serialization of individual elements is
 * delegated to the provided [elementSerializer].
 *
 * If your stack elements [T] are open polymorphic (e.g., a interface for different screens), the
 * provided [elementSerializer] must be correctly configured to handle this.
 *
 * @sample androidx.navigation3.runtime.samples.NavBackStackSerializer_withReflection
 * @param T The type of elements stored in the [NavBackStack].
 * @param elementSerializer The [KSerializer] used to serialize and deserialize individual elements.
 */
public class NavBackStackSerializer<T : NavKey>(private val elementSerializer: KSerializer<T>) :
    KSerializer<NavBackStack<T>> {

    private val delegate = SnapshotStateListSerializer(elementSerializer)

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        SerialDescriptor("androidx.navigation3.runtime.NavBackStack", delegate.descriptor)

    override fun serialize(encoder: Encoder, value: NavBackStack<T>) {
        encoder.encodeSerializableValue(serializer = delegate, value = value.base)
    }

    override fun deserialize(decoder: Decoder): NavBackStack<T> {
        return NavBackStack(base = decoder.decodeSerializableValue(deserializer = delegate))
    }
}

/**
 * Creates a [NavBackStackSerializer] for a polymorphic [NavKey] base type.
 *
 * This factory function is a convenience for creating a serializer for a [NavBackStack] whose
 * elements are polymorphic (e.g., different implementations of a `NavKey` interface).
 *
 * It retrieves a base serializer for the element type [T] (which is typically the base [NavKey]
 * interface).
 *
 * **Important:** For polymorphic serialization to work, you **must** provide a [SerializersModule]
 * (containing all concrete [NavKey] subtypes) to your `Encoder`/`Decoder` (e.g., via
 * [rememberSerializable]).
 *
 * `kotlinx.serialization`'s polymorphic dispatch relies on the module available during the
 * encoding/decoding process, not on the specific serializer retrieved by this function.
 *
 * @sample androidx.navigation3.runtime.samples.NavBackStackSerializer_withSerializersModule
 * @param T The reified element type, typically the base [NavKey] interface.
 * @return A new [NavBackStackSerializer] configured for the base polymorphic type.
 */
public inline fun <reified T : NavKey> NavBackStackSerializer(): NavBackStackSerializer<T> {
    return NavBackStackSerializer(elementSerializer = serializer<T>())
}
