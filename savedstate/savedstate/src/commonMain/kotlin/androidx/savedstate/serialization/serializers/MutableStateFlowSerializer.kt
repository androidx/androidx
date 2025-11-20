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

@file:OptIn(InternalSerializationApi::class, ExperimentalTypeInference::class)

package androidx.savedstate.serialization.serializers

import kotlin.experimental.ExperimentalTypeInference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

/**
 * Creates a [KSerializer] for a [MutableStateFlow] containing a [Serializable] value of type [T].
 *
 * This inline function infers the state type [T] automatically and retrieves the appropriate
 * [KSerializer] for serialization and deserialization of [MutableStateFlow].
 *
 * @param T The type of the value stored in the [MutableStateFlow].
 * @return A [MutableStateFlowSerializer] for handling [MutableStateFlow] containing a
 *   [Serializable] type [T].
 */
public inline fun <reified T> MutableStateFlowSerializer(): MutableStateFlowSerializer<T> {
    return MutableStateFlowSerializer(serializer())
}

/**
 * A [KSerializer] for [MutableStateFlow].
 *
 * This class wraps a [KSerializer] for the inner value type [T], enabling serialization and
 * deserialization of [MutableStateFlow] instances. The inner value serialization is delegated to
 * the provided [valueSerializer].
 *
 * Note that the SavedState format uses this serializer automatically for a [MutableStateFlow] at
 * root and there is no need to explicitly specify it. For example:
 * ```
 * val msf = MutableStateFlow(123)
 * // No need to do `encodeToSavedState(MutableStateFlowSerializer(Int.serializer), msf)`
 * encodeToSavedState(msf)
 * ```
 *
 * However, when the [MutableStateFlow] is a property of a serializable class there is no automatic
 * fallback and a `Serializable` annotation is still needed if the property is intended to be
 * serialized with this serializer. For example:
 * ```
 * @Serializable
 * data class MyData(
 *     @Serializable(with = MutableStateFlowSerializer::class)
 *     val flow: MutableStateFlow<Int>
 * )
 * ```
 *
 * @param T The type of the value stored in the [MutableStateFlow].
 * @param valueSerializer The [KSerializer] used to serialize and deserialize the inner value.
 */
public class MutableStateFlowSerializer<T>(private val valueSerializer: KSerializer<T>) :
    KSerializer<MutableStateFlow<T>> {

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = run {
        val serialName = "kotlinx.coroutines.flow.MutableStateFlow"
        val kind = valueSerializer.descriptor.kind
        if (kind is PrimitiveKind) {
            PrimitiveSerialDescriptor(serialName, kind)
        } else {
            SerialDescriptor(serialName, valueSerializer.descriptor)
        }
    }

    override fun serialize(encoder: Encoder, value: MutableStateFlow<T>) {
        encoder.encodeSerializableValue(valueSerializer, value.value)
    }

    override fun deserialize(decoder: Decoder): MutableStateFlow<T> {
        return MutableStateFlow(decoder.decodeSerializableValue(valueSerializer))
    }
}
