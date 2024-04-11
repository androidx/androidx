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

/**
 * Decoder to deserialize a bundle of argument value back into an object instance of that
 * destination.
 *
 * This decoder iterates through every class field (argument) in [T], retrieves the value
 * for that argument from the bundle, then use the retrieved values (or default values as fallback)
 * to re-create the object instance.
 *
 */
@OptIn(ExperimentalSerializationApi::class)
internal class RouteDecoder(
    bundle: Bundle,
    typeMap: Map<String, NavType<*>>
) : AbstractDecoder() {

    private val decoder = Decoder(bundle, typeMap)

    @Suppress("DEPRECATION") // deprecated in 1.6.3
    override val serializersModule: SerializersModule = EmptySerializersModule

    /**
     * The argument index returned here will be trigger deserializer to call [decodeValue] on that
     * argument.
     *
     * The decoder continually calls this method to process the next available argument until
     * this method returns [CompositeDecoder.DECODE_DONE], which indicates that there are
     * no more arguments to decode.
     *
     * This method should sequentially return the element index for every element that has its
     * value available within the [bundle]. For more details, see [Decoder.computeNextElementIndex].
     */
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return decoder.computeNextElementIndex(descriptor)
    }

    /**
     * Returns argument value from the [bundle] for the argument at the index returned from
     * [decodeElementIndex]
     */
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

    /**
     * Returns the next element index to call [decodeValue] on.
     *
     * [decodeValue] should only be called for arguments with values stores within [bundle].
     * Otherwise, we should let the deserializer fall back to default value. This is done by
     * skipping (not returning) the indices whose argument is not present in the bundle. In doing
     * so, the deserializer considers the skipped element un-processed and will use the
     * default value (if present) instead.
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun computeNextElementIndex(descriptor: SerialDescriptor): Int {
        var currentIndex = elementIndex
        while (true) {
            // proceed to next element
            currentIndex++
            // if we have reached the end, let decoder know there are not more arguments to decode
            if (currentIndex >= descriptor.elementsCount) return CompositeDecoder.DECODE_DONE
            val currentName = descriptor.getElementName(currentIndex)
            // Check if bundle has argument value. If so, we tell decoder to process
            // currentIndex. Otherwise, we skip this index and proceed to next index.
            if (bundle.containsKey(currentName)) {
                elementIndex = currentIndex
                elementName = currentName
                return elementIndex
            }
        }
    }

    /**
     * Retrieves argument value stored in the bundle
     */
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
