/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.navigation3.runtime

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * An [AbstractDecoder] that decodes arguments from a flat map of strings (extracted from a deep
 * link) into a navigation key.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class DeepLinkDecoder(private val arguments: Map<String, List<String>>) :
    AbstractDecoder() {

    private var currentName: String = ""
    private var currentIndex = 0

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        // Iterate through properties of a class
        while (currentIndex < descriptor.elementsCount) {
            val name = descriptor.getElementName(currentIndex)
            val elementDescriptor = descriptor.getElementDescriptor(currentIndex)
            val kind = elementDescriptor.kind

            // Check for unsupported collections
            if (kind == StructureKind.MAP) {
                TODO("Map decoding is not supported yet")
            }

            // If it's a primitive, an enum, or a list, we check if it's in the map
            // If it's not in the map, we skip it and let it fallback to the default value (if any)
            if (
                (kind is PrimitiveKind || kind == SerialKind.ENUM || kind == StructureKind.LIST) &&
                    !arguments.containsKey(name)
            ) {
                currentIndex++
                continue
            }

            // For nested structures (classes), we always return the index.
            // This recurses into the child decoder, which will then look for its own fields in the
            // map.
            currentName = name
            return currentIndex++
        }
        return CompositeDecoder.DECODE_DONE
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return when (descriptor.kind) {
            StructureKind.CLASS,
            StructureKind.OBJECT -> {
                DeepLinkDecoder(arguments).apply { currentName = this@DeepLinkDecoder.currentName }
            }
            StructureKind.LIST -> {
                // getElementDescriptor(0) returns the type param of the collection
                val elementDescriptor = descriptor.getElementDescriptor(0)
                if (elementDescriptor.kind !is PrimitiveKind) {
                    throw SerializationException("Only collections of primitives are supported")
                }
                val values = arguments[currentName] ?: emptyList()
                ListDecoder(values)
            }
            StructureKind.MAP -> TODO()
            else -> throw SerializationException("Unsupported structure kind: ${descriptor.kind}")
        }
    }

    override fun decodeString(): String {
        val value = arguments[currentName]?.firstOrNull()
        requireNotNull(value) { "Missing value for field $currentName" }
        return arguments[currentName]?.first()!!
    }

    override fun decodeInt(): Int = decodeString().toInt()

    override fun decodeBoolean(): Boolean = decodeString().toBoolean()

    override fun decodeLong(): Long = decodeString().toLong()

    override fun decodeFloat(): Float = decodeString().toFloat()

    override fun decodeDouble(): Double = decodeString().toDouble()

    override fun decodeChar(): Char = decodeString().first()

    override fun decodeByte(): Byte = decodeString().toByte()

    override fun decodeShort(): Short = decodeString().toShort()

    override fun decodeNull(): Nothing? = null

    override fun decodeNotNullMark(): Boolean = arguments.containsKey(currentName)

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val value = decodeString()
        val index = enumDescriptor.getElementIndex(value)
        if (index == CompositeDecoder.UNKNOWN_NAME) {
            throw SerializationException(
                "Unknown enum value: $value for enum ${enumDescriptor.serialName}"
            )
        }
        return index
    }
}

/**
 * A decoder for handling collections of primitives (Lists, Sets, Arrays). The descriptor's
 * [SerialDescriptor.kind] is [StructureKind.LIST]. The elementCount is expected to be the size of
 * the list of elements passed in. The decoder iterates through every index and sequentially
 * retrieves the element from [values].
 */
@OptIn(ExperimentalSerializationApi::class)
internal class ListDecoder(private val values: List<String>) : AbstractDecoder() {
    private var currentIndex = -1

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        currentIndex++
        if (currentIndex >= values.size) return CompositeDecoder.DECODE_DONE
        return currentIndex
    }

    override fun decodeNotNullMark(): Boolean {
        if (currentIndex < 0 || currentIndex >= values.size) return false
        return values[currentIndex] != "null"
    }

    override fun decodeString(): String {
        return values[currentIndex]
    }

    override fun decodeInt(): Int = decodeString().toInt()

    override fun decodeBoolean(): Boolean = decodeString().toBoolean()

    override fun decodeLong(): Long = decodeString().toLong()

    override fun decodeFloat(): Float = decodeString().toFloat()

    override fun decodeDouble(): Double = decodeString().toDouble()

    override fun decodeChar(): Char = decodeString().first()

    override fun decodeByte(): Byte = decodeString().toByte()

    override fun decodeShort(): Short = decodeString().toShort()
}
