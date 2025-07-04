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

package androidx.savedstate.compose.serialization.serializers

import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

/**
 * Creates a [KSerializer] for a [SnapshotStateMap] containing serializable keys of type [K] and
 * serializable values of type [V].
 *
 * This inline function automatically infers the key type [K] and value type [V], and retrieves the
 * corresponding [KSerializer] for serializing and deserializing [SnapshotStateMap] instances.
 *
 * @param K The type of keys stored in the [SnapshotStateMap].
 * @param V The type of values stored in the [SnapshotStateMap].
 * @return A [SnapshotStateMapSerializer] for handling serialization and deserialization of a
 *   [SnapshotStateMap] containing keys of type [K] and values of type [V].
 */
public inline fun <reified K, reified V> SnapshotStateMapSerializer():
    SnapshotStateMapSerializer<K, V> {
    return SnapshotStateMapSerializer(serializer(), serializer())
}

/**
 * A [KSerializer] for [SnapshotStateMap].
 *
 * This serializer wraps [KSerializer] instances for the key type [K] and value type [V], enabling
 * serialization and deserialization of [SnapshotStateMap] instances. The serialization of
 * individual keys and values is delegated to the provided [keySerializer] and [valueSerializer].
 *
 * @param K The type of keys stored in the [SnapshotStateMap].
 * @param V The type of values stored in the [SnapshotStateMap].
 * @param keySerializer The [KSerializer] used to serialize and deserialize individual keys.
 * @param valueSerializer The [KSerializer] used to serialize and deserialize individual values.
 */
public class SnapshotStateMapSerializer<K, V>(
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>,
) : KSerializer<SnapshotStateMap<K, V>> {

    private val base = MapSerializer(keySerializer, valueSerializer)

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        SerialDescriptor("androidx.compose.runtime.SnapshotStateMap", base.descriptor)

    override fun serialize(encoder: Encoder, value: SnapshotStateMap<K, V>) {
        encoder.encodeSerializableValue(base, value)
    }

    override fun deserialize(decoder: Decoder): SnapshotStateMap<K, V> {
        val deserialized = decoder.decodeSerializableValue(base)
        return SnapshotStateMap<K, V>().apply { putAll(deserialized) }
    }
}
