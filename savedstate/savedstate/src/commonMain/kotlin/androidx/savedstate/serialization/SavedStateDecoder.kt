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

package androidx.savedstate.serialization

import androidx.savedstate.SavedState
import androidx.savedstate.read
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

/**
 * Decode a serializable object from a [SavedState] with the default deserializer.
 *
 * **Format not stable:** The internal structure of the given [SavedState] is subject to change in
 * future releases for optimization. While it is guaranteed to be compatible with
 * [encodeToSavedState], direct manipulation of its encoded format using keys is not recommended.
 *
 * @sample androidx.savedstate.decode
 * @param savedState The [SavedState] to decode from.
 * @param configuration The [SavedStateConfiguration] to use. Defaults to
 *   [SavedStateConfiguration.DEFAULT].
 * @return The decoded object.
 * @throws SerializationException in case of any decoding-specific error.
 * @throws IllegalArgumentException if the decoded input is not a valid instance of [T].
 * @see encodeToSavedState
 */
@Deprecated(
    message =
        "Use the new 'decodeFromSavedState' overload that supports both nullable and non-nullable types.",
    level = DeprecationLevel.HIDDEN,
)
public inline fun <reified T : Any> decodeFromSavedState(
    savedState: SavedState,
    configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
): T = decodeFromSavedState(configuration.serializersModule.serializer(), savedState, configuration)

/**
 * Decode a serializable object from a [SavedState] with the default deserializer.
 *
 * **Format not stable:** The internal structure of the given [SavedState] is subject to change in
 * future releases for optimization. While it is guaranteed to be compatible with
 * [encodeToSavedState], direct manipulation of its encoded format using keys is not recommended.
 *
 * @sample androidx.savedstate.decode
 * @param savedState The [SavedState] to decode from.
 * @param configuration The [SavedStateConfiguration] to use. Defaults to
 *   [SavedStateConfiguration.DEFAULT].
 * @return The decoded object.
 * @throws SerializationException in case of any decoding-specific error.
 * @throws IllegalArgumentException if the decoded input is not a valid instance of [T].
 * @see encodeToSavedState
 */
@JvmName("decodeFromSavedStateNullable")
public inline fun <reified T> decodeFromSavedState(
    savedState: SavedState,
    configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
): T = decodeFromSavedState(configuration.serializersModule.serializer(), savedState, configuration)

/**
 * Decodes and deserializes the given [SavedState] to the value of type [T] using the given
 * [deserializer].
 *
 * **Format not stable:** The internal structure of the given [SavedState] is subject to change in
 * future releases for optimization. While it is guaranteed to be compatible with
 * [decodeFromSavedState], direct manipulation of its encoded format using keys is not recommended.
 *
 * @sample androidx.savedstate.decodeWithExplicitSerializerAndConfig
 * @param deserializer The deserializer to use.
 * @param savedState The [SavedState] to decode from.
 * @param configuration The [SavedStateConfiguration] to use. Defaults to
 *   [SavedStateConfiguration.DEFAULT].
 * @return The deserialized object.
 * @throws SerializationException in case of any decoding-specific error.
 * @throws IllegalArgumentException if the decoded input is not a valid instance of [T].
 * @see encodeToSavedState
 */
@Deprecated(
    message =
        "Use the new 'decodeFromSavedState' overload that supports both nullable and non-nullable types.",
    level = DeprecationLevel.HIDDEN,
)
@JvmOverloads
public fun <T : Any> decodeFromSavedState(
    deserializer: DeserializationStrategy<T>,
    savedState: SavedState,
    configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
): T {
    return SavedStateDecoder(savedState, configuration).decodeSerializableValue(deserializer)
}

/**
 * Decodes and deserializes the given [SavedState] to the value of type [T] using the given
 * [deserializer].
 *
 * **Format not stable:** The internal structure of the given [SavedState] is subject to change in
 * future releases for optimization. While it is guaranteed to be compatible with
 * [decodeFromSavedState], direct manipulation of its encoded format using keys is not recommended.
 *
 * @sample androidx.savedstate.decodeWithExplicitSerializerAndConfig
 * @param deserializer The deserializer to use.
 * @param savedState The [SavedState] to decode from.
 * @param configuration The [SavedStateConfiguration] to use. Defaults to
 *   [SavedStateConfiguration.DEFAULT].
 * @return The deserialized object.
 * @throws SerializationException in case of any decoding-specific error.
 * @throws IllegalArgumentException if the decoded input is not a valid instance of [T].
 * @see encodeToSavedState
 */
@JvmOverloads
@JvmName("decodeFromSavedStateNullable")
public fun <T> decodeFromSavedState(
    deserializer: DeserializationStrategy<T>,
    savedState: SavedState,
    configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
): T {
    return SavedStateDecoder(savedState, configuration).decodeSerializableValue(deserializer)
}

/**
 * A [kotlinx.serialization.encoding.Decoder] that can decode a serializable object from a
 * [SavedState]. The instance should not be reused after decoding.
 *
 * @property savedState The [SavedState] to decode from.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class SavedStateDecoder(
    internal val savedState: SavedState,
    private val configuration: SavedStateConfiguration,
) : AbstractDecoder() {
    internal var key: String = ""
        private set

    private var index = 0

    override val serializersModule: SerializersModule = configuration.serializersModule

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        val size =
            if (descriptor.kind == StructureKind.LIST || descriptor.kind == StructureKind.MAP) {
                // Use the number of elements encoded for collections.
                savedState.read { size() }
            } else {
                // We may skip elements when encoding so if we used `size()`
                // here we may miss some fields.
                descriptor.elementsCount
            }
        fun hasDefaultValueDefined(index: Int) = descriptor.isElementOptional(index)
        fun presentInEncoding(index: Int) =
            savedState.read {
                val key = descriptor.getElementName(index)
                contains(key)
            }
        // Skip elements omitted from encoding (those assigned with its default values).
        while (index < size && hasDefaultValueDefined(index) && !presentInEncoding(index)) {
            index++
        }
        if (index < size) {
            key = descriptor.getElementName(index)
            return index++
        } else {
            return CompositeDecoder.DECODE_DONE
        }
    }

    override fun decodeBoolean(): Boolean = savedState.read { getBoolean(key) }

    override fun decodeByte(): Byte = savedState.read { getInt(key).toByte() }

    override fun decodeShort(): Short = savedState.read { getInt(key).toShort() }

    override fun decodeInt(): Int = savedState.read { getInt(key) }

    override fun decodeLong(): Long = savedState.read { getLong(key) }

    override fun decodeFloat(): Float = savedState.read { getFloat(key) }

    override fun decodeDouble(): Double = savedState.read { getDouble(key) }

    override fun decodeChar(): Char = savedState.read { getChar(key) }

    override fun decodeString(): String = savedState.read { getString(key) }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = savedState.read { getInt(key) }

    private fun decodeIntList(): List<Int> {
        return savedState.read { getIntList(key) }
    }

    private fun decodeStringList(): List<String> {
        return savedState.read { getStringList(key) }
    }

    private fun decodeBooleanArray(): BooleanArray {
        return savedState.read { getBooleanArray(key) }
    }

    private fun decodeCharArray(): CharArray {
        return savedState.read { getCharArray(key) }
    }

    private fun decodeDoubleArray(): DoubleArray {
        return savedState.read { getDoubleArray(key) }
    }

    private fun decodeFloatArray(): FloatArray {
        return savedState.read { getFloatArray(key) }
    }

    private fun decodeIntArray(): IntArray {
        return savedState.read { getIntArray(key) }
    }

    private fun decodeLongArray(): LongArray {
        return savedState.read { getLongArray(key) }
    }

    private fun decodeStringArray(): Array<String> {
        return savedState.read { getStringArray(key) }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        if (key == "") {
            this
        } else {
            SavedStateDecoder(
                savedState = savedState.read { getSavedState(key) },
                configuration = configuration,
            )
        }

    // We don't encode NotNullMark so this will actually read either a `null` from
    // `encodeNull()` or a value from other encode functions.
    override fun decodeNotNullMark(): Boolean = savedState.read { !isNull(key) }

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        return decodeFormatSpecificTypes(deserializer)
            ?: super.decodeSerializableValue(deserializer)
    }

    /** @return `T` if `T` has a special representation in `SavedState`, `null` otherwise. */
    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    private fun <T> decodeFormatSpecificTypes(deserializer: DeserializationStrategy<T>): T? {
        return decodeFormatSpecificTypesOnPlatform(deserializer)
            ?: when (deserializer.descriptor) {
                intListDescriptor -> decodeIntList()
                stringListDescriptor -> decodeStringList()
                booleanArrayDescriptor -> decodeBooleanArray()
                charArrayDescriptor -> decodeCharArray()
                doubleArrayDescriptor -> decodeDoubleArray()
                floatArrayDescriptor -> decodeFloatArray()
                intArrayDescriptor -> decodeIntArray()
                longArrayDescriptor -> decodeLongArray()
                stringArrayDescriptor -> decodeStringArray()
                else -> null
            }
                as T?
    }
}

/** @return `T` if `T` has an internal representation in `SavedState`, `null` otherwise. */
internal expect fun <T> SavedStateDecoder.decodeFormatSpecificTypesOnPlatform(
    strategy: DeserializationStrategy<T>
): T?
