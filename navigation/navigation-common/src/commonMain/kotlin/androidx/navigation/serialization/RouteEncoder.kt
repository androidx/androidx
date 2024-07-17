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

import androidx.annotation.RestrictTo
import androidx.navigation.CollectionNavType
import androidx.navigation.NavType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/** Encodes KClass of type T into a route filled with arguments */
@OptIn(ExperimentalSerializationApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RouteEncoder<T : Any>(
    private val serializer: KSerializer<T>,
    private val typeMap: Map<String, NavType<Any?>>
) : AbstractEncoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()
    private val map: MutableMap<String, List<String>> = mutableMapOf()
    private var elementIndex: Int = -1

    /**
     * Entry point to set up and start encoding [T].
     *
     * The default entry point is [encodeSerializableValue] but we need to override it to handle
     * primitive and non-primitive values by converting them directly to string (instead of the
     * default implementation which further serializes nested non-primitive values). So we delegate
     * to the default entry by directly calling [super.encodeSerializableValue].
     */
    @Suppress("UNCHECKED_CAST")
    fun encodeToArgMap(value: Any): Map<String, List<String>> {
        super.encodeSerializableValue(serializer, value as T)
        return map.toMap()
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
        internalEncodeValue(value)
    }

    /** Essentially called for every single argument. */
    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        elementIndex = index
        return true
    }

    /**
     * Called for non-nullable primitives of non-null value.
     *
     * String literal "null" is considered non-null value.
     */
    override fun encodeValue(value: Any) {
        internalEncodeValue(value)
    }

    /** Called for primitive / non-primitives of null value */
    override fun encodeNull() {
        internalEncodeValue(null)
    }

    private fun internalEncodeValue(value: Any?) {
        val argName = serializer.descriptor.getElementName(elementIndex)
        val navType = typeMap[argName]
        checkNotNull(navType) {
            "Cannot find NavType for argument $argName. Please provide NavType through typeMap."
        }
        val parsedValue =
            if (navType is CollectionNavType) {
                navType.serializeAsValues(value)
            } else {
                listOf(navType.serializeAsValue(value))
            }
        map[argName] = parsedValue
    }
}
