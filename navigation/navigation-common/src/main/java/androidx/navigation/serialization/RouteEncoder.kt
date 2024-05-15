/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.navigation.serialization

import androidx.navigation.NavType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * Encodes KClass of type T into a route filled with arguments
 */
@OptIn(ExperimentalSerializationApi::class)
internal class RouteEncoder<T : Any>(
    private val serializer: KSerializer<T>,
    private val typeMap: Map<String, NavType<Any?>>
) : AbstractEncoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()
    private val builder = RouteBuilder.Filled(serializer, typeMap)

    /**
     * Entry point to set up and start encoding [T].
     *
     * The default entry point is [encodeSerializableValue] but we need to override it to handle
     * primitive and non-primitive values by converting them directly to string (instead of the
     * default implementation which further serializes nested non-primitive values). So we
     * delegate to the default entry by directly calling [super.encodeSerializableValue].
     */
    @Suppress("UNCHECKED_CAST")
    fun encodeRouteWithArgs(value: Any): String {
        super.encodeSerializableValue(serializer, value as T)
        return builder.build()
    }

    /**
     * Can handle both primitives and non-primitives. This method is called in three possible
     * scenarios:
     * 1. nullable primitive type with non-null value
     * 2. nullable non-primitive type with non-null value
     * 3. non-nullable non-primitive type
     *
     * String literal "null" is considered non-null value.
     */
    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        if (value == "null") {
            builder.addNull(value)
        } else {
            builder.addArg(value)
        }
    }

    /**
     * Essentially called for every single argument.
     */
    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        builder.setElementIndex(index)
        return true
    }

    /**
     * Called for non-nullable primitives of non-null value.
     *
     * String literal "null" is considered non-null value.
     */
    override fun encodeValue(value: Any) {
        if (value == "null") {
            builder.addNull(value)
        } else {
            builder.addArg(value)
        }
    }

    /**
     * Called for primitive / non-primitives of null value
     */
    override fun encodeNull() {
        builder.addNull(null)
    }
}
