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
import androidx.savedstate.savedState
import androidx.savedstate.write
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.serializer

/**
 * Serializes the [value] of type [T] into an equivalent [SavedState] using [KSerializer] retrieved
 * from the reified type parameter.
 *
 * **Format not stable:** The internal structure of the returned [SavedState] is subject to change
 * in future releases for optimization. While it is guaranteed to be compatible with
 * [decodeFromSavedState], direct manipulation of its encoded format using keys is not recommended.
 *
 * @sample androidx.savedstate.encode
 * @param value The serializable object to encode.
 * @param configuration The [SavedStateConfiguration] to use. Defaults to
 *   [SavedStateConfiguration.DEFAULT].
 * @return The encoded [SavedState].
 * @throws SerializationException in case of any encoding-specific error.
 * @see decodeFromSavedState
 */
@Deprecated(
    message =
        "Use the new 'encodeToSavedState' overload that supports both nullable and non-nullable types.",
    level = DeprecationLevel.HIDDEN,
)
public inline fun <reified T : Any> encodeToSavedState(
    value: T,
    configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
): SavedState =
    encodeToSavedState(configuration.serializersModule.serializer(), value, configuration)

/**
 * Serializes the [value] of type [T] into an equivalent [SavedState] using [KSerializer] retrieved
 * from the reified type parameter.
 *
 * **Format not stable:** The internal structure of the returned [SavedState] is subject to change
 * in future releases for optimization. While it is guaranteed to be compatible with
 * [decodeFromSavedState], direct manipulation of its encoded format using keys is not recommended.
 *
 * @sample androidx.savedstate.encode
 * @param value The serializable object to encode.
 * @param configuration The [SavedStateConfiguration] to use. Defaults to
 *   [SavedStateConfiguration.DEFAULT].
 * @return The encoded [SavedState].
 * @throws SerializationException in case of any encoding-specific error.
 * @see decodeFromSavedState
 */
@JvmName("encodeToSavedStateNullable")
public inline fun <reified T> encodeToSavedState(
    value: T,
    configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
): SavedState =
    encodeToSavedState(configuration.serializersModule.serializer(), value, configuration)

/**
 * Serializes and encodes the given [value] to [SavedState] using the given [serializer].
 *
 * **Format not stable:** The internal structure of the returned [SavedState] is subject to change
 * in future releases for optimization. While it is guaranteed to be compatible with
 * [decodeFromSavedState], direct manipulation of its encoded format using keys is not recommended.
 *
 * @sample androidx.savedstate.encodeWithExplicitSerializerAndConfig
 * @param serializer The serializer to use.
 * @param value The serializable object to encode.
 * @param configuration The [SavedStateConfiguration] to use. Defaults to
 *   [SavedStateConfiguration.DEFAULT].
 * @return The encoded [SavedState].
 * @throws SerializationException in case of any encoding-specific error.
 * @see decodeFromSavedState
 */
@Deprecated(
    message =
        "Use the new 'encodeToSavedState' overload that supports both nullable and non-nullable types.",
    level = DeprecationLevel.HIDDEN,
)
@JvmOverloads
public fun <T : Any> encodeToSavedState(
    serializer: SerializationStrategy<T>,
    value: T,
    configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
): SavedState {
    val result = savedState()
    SavedStateEncoder(result, configuration).encodeSerializableValue(serializer, value)
    return result
}

/**
 * Serializes and encodes the given [value] to [SavedState] using the given [serializer].
 *
 * **Format not stable:** The internal structure of the returned [SavedState] is subject to change
 * in future releases for optimization. While it is guaranteed to be compatible with
 * [decodeFromSavedState], direct manipulation of its encoded format using keys is not recommended.
 *
 * @sample androidx.savedstate.encodeWithExplicitSerializerAndConfig
 * @param serializer The serializer to use.
 * @param value The serializable object to encode.
 * @param configuration The [SavedStateConfiguration] to use. Defaults to
 *   [SavedStateConfiguration.DEFAULT].
 * @return The encoded [SavedState].
 * @throws SerializationException in case of any encoding-specific error.
 * @see decodeFromSavedState
 */
@JvmOverloads
@JvmName("encodeToSavedStateNullable")
public fun <T> encodeToSavedState(
    serializer: SerializationStrategy<T>,
    value: T,
    configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
): SavedState {
    val result = savedState()
    SavedStateEncoder(result, configuration).encodeSerializableValue(serializer, value)
    return result
}

/**
 * A [kotlinx.serialization.encoding.Encoder] that can encode a serializable object to a
 * [SavedState]. The instance should not be reused after encoding.
 *
 * @property savedState The [SavedState] to encode to. Has to be empty before encoding.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class SavedStateEncoder(
    internal val savedState: SavedState,
    private val configuration: SavedStateConfiguration,
) : AbstractEncoder() {

    internal var key: String = ""
        private set

    override val serializersModule = configuration.serializersModule

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean {
        return configuration.encodeDefaults
    }

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        // The key will be property names for classes by default and can be modified with
        // `@SerialName`. The key for collections will be decimal integer Strings ("0",
        // "1", "2", ...).
        key = descriptor.getElementName(index)
        checkDiscriminatorCollisions(savedState, key)

        return true
    }

    private fun checkDiscriminatorCollisions(savedState: SavedState, elementName: String) {
        if (configuration.classDiscriminatorMode == ClassDiscriminatorMode.ALL_OBJECTS) {
            val hasClassDiscriminator = savedState.read { contains(CLASS_DISCRIMINATOR_KEY) }
            val hasConflictingElementName = elementName == CLASS_DISCRIMINATOR_KEY
            if (hasClassDiscriminator && hasConflictingElementName) {
                val classDiscriminator = savedState.read { getString(CLASS_DISCRIMINATOR_KEY) }
                throw IllegalArgumentException(
                    "SavedStateEncoder for $classDiscriminator has property '$elementName' that " +
                        "conflicts with the class discriminator. You can rename a property with " +
                        "@SerialName annotation."
                )
            }
        }
    }

    override fun encodeBoolean(value: Boolean) {
        savedState.write { putBoolean(key, value) }
    }

    override fun encodeByte(value: Byte) {
        savedState.write { putInt(key, value.toInt()) }
    }

    override fun encodeShort(value: Short) {
        savedState.write { putInt(key, value.toInt()) }
    }

    override fun encodeInt(value: Int) {
        savedState.write { putInt(key, value) }
    }

    override fun encodeLong(value: Long) {
        savedState.write { putLong(key, value) }
    }

    override fun encodeFloat(value: Float) {
        savedState.write { putFloat(key, value) }
    }

    override fun encodeDouble(value: Double) {
        savedState.write { putDouble(key, value) }
    }

    override fun encodeChar(value: Char) {
        savedState.write { putChar(key, value) }
    }

    override fun encodeString(value: String) {
        savedState.write { putString(key, value) }
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        savedState.write { putInt(key, index) }
    }

    override fun encodeNull() {
        savedState.write { putNull(key) }
    }

    private fun encodeIntList(value: List<Int>) {
        savedState.write { putIntList(key, value) }
    }

    private fun encodeStringList(value: List<String>) {
        savedState.write { putStringList(key, value) }
    }

    private fun encodeBooleanArray(value: BooleanArray) {
        savedState.write { putBooleanArray(key, value) }
    }

    private fun encodeCharArray(value: CharArray) {
        savedState.write { putCharArray(key, value) }
    }

    private fun encodeDoubleArray(value: DoubleArray) {
        savedState.write { putDoubleArray(key, value) }
    }

    private fun encodeFloatArray(value: FloatArray) {
        savedState.write { putFloatArray(key, value) }
    }

    private fun encodeIntArray(value: IntArray) {
        savedState.write { putIntArray(key, value) }
    }

    private fun encodeLongArray(value: LongArray) {
        savedState.write { putLongArray(key, value) }
    }

    private fun encodeStringArray(value: Array<String>) {
        savedState.write { putStringArray(key, value) }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        // We flatten single structured object at root to prevent encoding to a
        // SavedState containing only one SavedState inside. For example, a
        // `Pair(3, 5)` would become `{"first" = 3, "second" = 5}` instead of
        // `{{"first" = 3, "second" = 5}}`, which is more consistent but less
        // efficient.
        return if (key == "") {
            putClassDiscriminatorIfRequired(configuration, descriptor, savedState)
            this
        } else {
            val childState = savedState()
            savedState.write { putSavedState(key, childState) } // Link child to parent.
            putClassDiscriminatorIfRequired(configuration, descriptor, childState)
            SavedStateEncoder(childState, configuration)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun putClassDiscriminatorIfRequired(
        configuration: SavedStateConfiguration,
        descriptor: SerialDescriptor,
        savedState: SavedState,
    ) {
        // POLYMORPHIC is handled by kotlinx.serialization.PolymorphicSerializer.
        if (configuration.classDiscriminatorMode != ClassDiscriminatorMode.ALL_OBJECTS) {
            return
        }

        if (savedState.read { contains(CLASS_DISCRIMINATOR_KEY) }) {
            return
        }

        if (descriptor.kind == StructureKind.CLASS || descriptor.kind == StructureKind.OBJECT) {
            savedState.write { putString(CLASS_DISCRIMINATOR_KEY, descriptor.serialName) }
        }
    }

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        val encoded = encodeFormatSpecificTypes(serializer, value)
        if (!encoded) {
            super.encodeSerializableValue(serializer, value)
        }
    }

    /**
     * @return `true` if [value] was encoded with SavedState's special representation, `false`
     *   otherwise.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> encodeFormatSpecificTypes(
        serializer: SerializationStrategy<T>,
        value: T,
    ): Boolean {
        val encoded = encodeFormatSpecificTypesOnPlatform(serializer, value)
        if (!encoded) {
            when (serializer.descriptor) {
                intListDescriptor -> encodeIntList(value as List<Int>)
                stringListDescriptor -> encodeStringList(value as List<String>)
                booleanArrayDescriptor -> encodeBooleanArray(value as BooleanArray)
                charArrayDescriptor -> encodeCharArray(value as CharArray)
                doubleArrayDescriptor -> encodeDoubleArray(value as DoubleArray)
                floatArrayDescriptor -> encodeFloatArray(value as FloatArray)
                intArrayDescriptor -> encodeIntArray(value as IntArray)
                longArrayDescriptor -> encodeLongArray(value as LongArray)
                stringArrayDescriptor -> encodeStringArray(value as Array<String>)
                else -> return false
            }
        }
        return true
    }
}

/**
 * @return `true` if [value] was encoded with SavedState's special representation, `false`
 *   otherwise.
 */
internal expect fun <T> SavedStateEncoder.encodeFormatSpecificTypesOnPlatform(
    strategy: SerializationStrategy<T>,
    value: T,
): Boolean
