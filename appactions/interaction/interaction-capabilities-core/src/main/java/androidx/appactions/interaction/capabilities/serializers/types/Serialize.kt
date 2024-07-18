/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.serializers.types

import androidx.appactions.builtintypes.types.Thing
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException
import androidx.appactions.interaction.protobuf.Struct

/**
 * Converts the specified [Thing] (or subtype) to a JSON-LD conforming [Struct].
 *
 * Takes care of dynamically invoking the correct serializer based on the runtime type of the
 * [instance]. For example,
 * ```kt
 * val person =
 *   Person.Builder()
 *     .setName("Jane")
 *     .setEmail("jane@gmail.com")
 *     .build()
 * // {
 * //   "@type": "Person",
 * //   "name": "Jane",
 * //   "email": "jane@gmail.com"
 * // }
 * val struct = serialize(person)
 * ```
 *
 * @throws StructConversionException if some internal error occurs during serialization.
 */
fun serialize(instance: Thing): Struct {
    val serializer =
        builtInTypeSerializerRegistry.getSerializer(instance)
            ?: throw StructConversionException(
                "Could not unambiguously determine the serializer for instance: $instance")
    return serializer.serialize(instance)
}

/**
 * Converts a JSON-LD conforming [Struct] to a [Thing] (or subtype).
 *
 * Takes care of dynamically invoking the correct serializer based on the "@type" field in the
 * [struct]. For example,
 * ```kt
 * // {
 * //   "@type": "Person",
 * //   "name": "Jane",
 * //   "email": "jane@gmail.com"
 * // }
 * val struct: Struct
 * val thing = deserialize(struct)
 *
 * print(thing.name) // Jane
 * if (thing is Person) {
 *   println(thing.email) // jane@gmail.com
 * }
 * ```
 *
 * @throws StructConversionException If the [struct] refers to some unknown type.
 */
fun deserialize(@Suppress("UNUSED_PARAMETER") struct: Struct): Thing = TODO()
