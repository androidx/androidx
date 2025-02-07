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

package androidx.savedstate.compose.serialization.serializers

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlin.experimental.ExperimentalTypeInference
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

/**
 * Creates a [KSerializer] for a [MutableState] containing a [Serializable] value of type [T].
 *
 * This inline function infers the state type [T] automatically and retrieves the appropriate
 * [KSerializer] for serialization and deserialization of [MutableState].
 *
 * @param T The type of the value stored in the [MutableState].
 * @return A [KSerializer] for handling [MutableState] containing a [Serializable] type [T].
 */
@Suppress("FunctionName")
public inline fun <reified T> MutableStateSerializer(): KSerializer<MutableState<T>> {
    return MutableStateSerializer(serializer())
}

/**
 * Creates a [KSerializer] for a [MutableState] containing a [Serializable] value of type [T].
 *
 * This function allows for explicit specification of the [KSerializer] for the state type [T]. It
 * provides serialization and deserialization capabilities for [MutableState] objects.
 *
 * @param T The type of the value stored in the [MutableState].
 * @param serializer The [KSerializer] for the [Serializable] type [T].
 * @return A [KSerializer] for handling [MutableState] containing a [Serializable] type [T].
 */
@Suppress("FunctionName")
public fun <T> MutableStateSerializer(serializer: KSerializer<T>): KSerializer<MutableState<T>> {
    return MutableStateSerializerImpl<T>(serializer)
}

/**
 * Internal implementation of [KSerializer] for [MutableState].
 *
 * This private class wraps a [KSerializer] for the inner value type [T], enabling serialization and
 * deserialization of [MutableState] instances. The inner value serialization is delegated to the
 * provided [valueSerializer].
 *
 * @param T The type of the value stored in the [MutableState].
 * @property valueSerializer The [KSerializer] used to serialize and deserialize the inner value.
 */
private class MutableStateSerializerImpl<T>(
    private val valueSerializer: KSerializer<T>,
) : KSerializer<MutableState<T>> {

    override val descriptor: SerialDescriptor = valueSerializer.descriptor

    override fun serialize(encoder: Encoder, value: MutableState<T>) {
        valueSerializer.serialize(encoder, value.value)
    }

    override fun deserialize(decoder: Decoder): MutableState<T> {
        return mutableStateOf(valueSerializer.deserialize(decoder))
    }
}
