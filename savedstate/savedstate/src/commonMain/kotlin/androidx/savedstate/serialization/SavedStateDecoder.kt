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

@file:Suppress("FacadeClassJvmName") // Cannot be updated, the Kt name has been released

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

    override val serializersModule
        get() = configuration.serializersModule

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        // We flatten single structured object at root to prevent encoding to a
        // SavedState containing only one SavedState inside. For example, a
        // `Pair(3, 5)` would become `{"first" = 3, "second" = 5}` instead of
        // `{{"first" = 3, "second" = 5}}`, which is more consistent but less
        // efficient.
        return if (key == "") {
            this
        } else {
            SavedStateDecoder(
                savedState = savedState.read { getSavedState(key) },
                configuration = configuration,
            )
        }
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        // Get iteration boundary. For collections, it's the saved size.
        // For classes, it's all schema fields, as optional ones might be missing.
        val elementCount =
            if (descriptor.kind == StructureKind.LIST || descriptor.kind == StructureKind.MAP) {
                savedState.read { size() }
            } else {
                descriptor.elementsCount
            }

        // Find the next element present in the saved state, skipping
        // omitted optional fields. 'index' is a class property.
        while (index < elementCount) {
            val elementName = descriptor.getElementName(index)

            // Skip optional fields that aren't in the saved state
            // (they will use their default value).
            if (descriptor.isElementOptional(index) && savedState.read { elementName !in this }) {
                index++
                continue
            }

            // This element is present or non-optional.
            // Set 'key' so subsequent decode* calls know what to read from the state.
            key = elementName

            // Return current index; increment 'index' for the next call.
            return index++
        }

        // All elements processed.
        return CompositeDecoder.DECODE_DONE
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

    // We don't encode NotNullMark so this will actually read either a `null` from
    // `encodeNull()` or a value from other encode functions.
    override fun decodeNotNullMark(): Boolean = savedState.read { !isNull(key) }

    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        // First, try any platform-specific types
        val platformDecoded = decodeFormatSpecificTypesOnPlatform(deserializer)
        if (platformDecoded != null) {
            // Platform decoder handled it, we're done.
            return platformDecoded as T
        }

        // If platform decoding didn't handle it, try our known fast-path types.
        return when (deserializer.descriptor) {
            intListDescriptor -> savedState.read { getIntList(key) }
            stringListDescriptor -> savedState.read { getStringList(key) }
            booleanArrayDescriptor -> savedState.read { getBooleanArray(key) }
            charArrayDescriptor -> savedState.read { getCharArray(key) }
            doubleArrayDescriptor -> savedState.read { getDoubleArray(key) }
            floatArrayDescriptor -> savedState.read { getFloatArray(key) }
            intArrayDescriptor -> savedState.read { getIntArray(key) }
            longArrayDescriptor -> savedState.read { getLongArray(key) }
            stringArrayDescriptor -> savedState.read { getStringArray(key) }
            else -> {
                // This isn't a type we can specially handle.
                // Fall back to the default deserialization behavior.
                super.decodeSerializableValue(deserializer)
            }
        }
            as T
    }
}

/** @return `T` if `T` has an internal representation in `SavedState`, `null` otherwise. */
internal expect fun <T> SavedStateDecoder.decodeFormatSpecificTypesOnPlatform(
    strategy: DeserializationStrategy<T>
): T?
