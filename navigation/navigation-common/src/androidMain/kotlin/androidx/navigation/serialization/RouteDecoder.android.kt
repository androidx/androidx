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

import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
internal class RouteDecoder(
    bundle: Bundle,
    typeMap: Map<String, NavType<*>>
) : AbstractDecoder() {

    private val decoder = Decoder(bundle, typeMap)

    @Suppress("DEPRECATION") // deprecated in 1.6.3
    override val serializersModule: SerializersModule = EmptySerializersModule

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return decoder.incrementElement(descriptor)
    }

    override fun decodeValue(): Any = decoder.decodeValue()

    override fun decodeNull(): Nothing? = null

    // we want to know if it is not null, so its !isNull
    override fun decodeNotNullMark(): Boolean = !decoder.isCurrentElementNull()

    // value from decodeValue() rather than decodeInt, decodeBoolean etc.. needs to be casted
    @Suppress("UNCHECKED_CAST")
    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?
    ): T = decoder.decodeValue() as T
}

private class Decoder(
    private val bundle: Bundle,
    private val typeMap: Map<String, NavType<*>>
) {
    private var elementIndex: Int = -1
    private var elementName: String = ""

    @OptIn(ExperimentalSerializationApi::class)
    fun incrementElement(descriptor: SerialDescriptor): Int {
        if (++elementIndex >= descriptor.elementsCount) return CompositeDecoder.DECODE_DONE
        elementName = descriptor.getElementName(elementIndex)
        return elementIndex
    }

    fun decodeValue(): Any {
        val navType = typeMap[elementName]
        val arg = navType?.get(bundle, elementName)
        checkNotNull(arg) {
            "Unexpected null value for non-nullable argument $elementName"
        }
        return arg
    }

    fun isCurrentElementNull(): Boolean {
        val navType = typeMap[elementName]
        return navType?.isNullableAllowed == true && navType[bundle, elementName] == null
    }
}
