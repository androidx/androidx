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

import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

/**
 * Creates a [KSerializer] for a [SnapshotStateList] containing serializable elements of type [T].
 *
 * This inline function automatically infers the element type [T] and retrieves the corresponding
 * [KSerializer] for serializing and deserializing [SnapshotStateList] instances.
 *
 * @param T The type of elements stored in the [SnapshotStateList].
 * @return A [SnapshotStateListSerializer] for handling serialization and deserialization of a
 *   [SnapshotStateList] containing elements of type [T].
 */
public inline fun <reified T> SnapshotStateListSerializer(): SnapshotStateListSerializer<T> {
    return SnapshotStateListSerializer(serializer())
}

/**
 * A [KSerializer] for [SnapshotStateList].
 *
 * This serializer wraps a [KSerializer] for the element type [T], enabling serialization and
 * deserialization of [SnapshotStateList] instances. The serialization of individual elements is
 * delegated to the provided [elementSerializer].
 *
 * @param T The type of elements stored in the [SnapshotStateList].
 * @param elementSerializer The [KSerializer] used to serialize and deserialize individual elements.
 */
public class SnapshotStateListSerializer<T>(private val elementSerializer: KSerializer<T>) :
    KSerializer<SnapshotStateList<T>> {

    private val base = ListSerializer(elementSerializer)

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        SerialDescriptor("androidx.compose.runtime.SnapshotStateList", base.descriptor)

    override fun serialize(encoder: Encoder, value: SnapshotStateList<T>) {
        encoder.encodeSerializableValue(base, value)
    }

    override fun deserialize(decoder: Decoder): SnapshotStateList<T> {
        val deserialized = decoder.decodeSerializableValue(base)
        return SnapshotStateList<T>().apply { addAll(deserialized.toList()) }
    }
}
