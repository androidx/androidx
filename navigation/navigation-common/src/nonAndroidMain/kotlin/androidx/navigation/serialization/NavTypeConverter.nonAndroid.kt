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

import androidx.navigation.CollectionNavType
import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule

@OptIn(ExperimentalSerializationApi::class)
internal actual fun SerialDescriptor.parseEnum(serializer: KSerializer<*>?): NavType<*> {
    if (serializer == null) return UNKNOWN
    @Suppress("UNCHECKED_CAST")
    return EnumNavType(serializer as KSerializer<Any>, this, isNullable)
}

internal actual fun SerialDescriptor.parseNullableEnum(serializer: KSerializer<*>?): NavType<*> {
    if (serializer == null) return UNKNOWN
    @Suppress("UNCHECKED_CAST")
    return EnumNavType(serializer as KSerializer<Any>, this, true)
}

@OptIn(ExperimentalSerializationApi::class)
internal actual fun SerialDescriptor.parseEnumList(serializer: KSerializer<*>?): NavType<*> {
    if (serializer == null) return UNKNOWN
    @Suppress("UNCHECKED_CAST")
    return EnumListNavType(serializer as KSerializer<List<Any>>, getElementDescriptor(0))
}

/** NavType for enums on platforms without Android's Serializable/Parcelable support. */
@OptIn(ExperimentalSerializationApi::class)
private class EnumNavType(
    private val serializer: KSerializer<Any>,
    private val descriptor: SerialDescriptor,
    override val isNullableAllowed: Boolean,
) : NavType<Any?>(isNullableAllowed) {
    override val name: String
        get() = descriptor.serialName

    override fun put(bundle: SavedState, key: String, value: Any?) {
        bundle.write { if (value == null) putNull(key) else putString(key, value.toString()) }
    }

    override fun get(bundle: SavedState, key: String): Any? = bundle.read {
        if (contains(key) && !isNull(key)) parseValue(getString(key)) else null
    }

    override fun parseValue(value: String): Any = serializer.deserialize(EnumDecoder(value))

    override fun serializeAsValue(value: Any?): String = value?.toString() ?: "null"
}

@OptIn(ExperimentalSerializationApi::class)
private class EnumListNavType(
    private val serializer: KSerializer<List<Any>>,
    private val enumDescriptor: SerialDescriptor,
) : CollectionNavType<List<Any>?>(true) {
    override val name: String
        get() = "List<${enumDescriptor.serialName}>"

    override fun put(bundle: SavedState, key: String, value: List<Any>?) {
        bundle.write {
            if (value == null) putNull(key)
            else putStringArray(key, value.map(Any::toString).toTypedArray())
        }
    }

    override fun get(bundle: SavedState, key: String): List<Any>? = bundle.read {
        if (!contains(key) || isNull(key)) null
        else getStringArray(key).map { parseValue(it).single() }
    }

    override fun parseValue(value: String): List<Any> =
        serializer.deserialize(EnumListDecoder(value))

    override fun parseValue(value: String, previousValue: List<Any>?): List<Any> =
        previousValue.orEmpty() + parseValue(value)

    override fun valueEquals(value: List<Any>?, other: List<Any>?): Boolean = value == other

    override fun serializeAsValues(value: List<Any>?): List<String> =
        value?.map(Any::toString).orEmpty()

    override fun emptyCollection(): List<Any> = emptyList()
}

@OptIn(ExperimentalSerializationApi::class)
private open class EnumDecoder(private val value: String) : AbstractDecoder() {
    override val serializersModule = EmptySerializersModule()

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        for (index in 0 until enumDescriptor.elementsCount) {
            if (enumDescriptor.getElementName(index) == value) return index
        }
        throw IllegalArgumentException(
            "Unknown enum value '$value' for ${enumDescriptor.serialName}"
        )
    }

    override fun decodeNotNullMark(): Boolean = true

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int =
        CompositeDecoder.DECODE_DONE
}

@OptIn(ExperimentalSerializationApi::class)
private class EnumListDecoder(value: String) : EnumDecoder(value) {
    private var elementDecoded = false

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = 1

    override fun decodeSequentially(): Boolean = true

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int =
        if (elementDecoded) CompositeDecoder.DECODE_DONE else 0.also { elementDecoded = true }
}
