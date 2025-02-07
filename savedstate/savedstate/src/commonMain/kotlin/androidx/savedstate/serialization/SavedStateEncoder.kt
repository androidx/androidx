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
import androidx.savedstate.savedState
import androidx.savedstate.write
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.serializer

/**
 * Encode a serializable object to a [SavedState] with an explicit serializer, which can be a custom
 * or third-party one.
 *
 * @sample androidx.savedstate.encodeWithExplicitSerializer
 * @param serializer The serializer to use.
 * @param value The serializable object to encode.
 * @return The encoded [SavedState].
 * @throws SerializationException if [value] cannot be serialized.
 */
public fun <T : Any> encodeToSavedState(
    serializer: SerializationStrategy<T>,
    value: T
): SavedState =
    savedState().apply {
        SavedStateEncoder(this, SavedStateConfig.DEFAULT).encodeSerializableValue(serializer, value)
    }

/**
 * Encode a serializable object to a [SavedState] with an explicit serializer, which can be a custom
 * or third-party one.
 *
 * @sample androidx.savedstate.encodeWithExplicitSerializerAndConfig
 * @param serializer The serializer to use.
 * @param value The serializable object to encode.
 * @param config The [SavedStateConfig] to use.
 * @return The encoded [SavedState].
 * @throws SerializationException if [value] cannot be serialized.
 */
public fun <T : Any> encodeToSavedState(
    serializer: SerializationStrategy<T>,
    value: T,
    config: SavedStateConfig,
): SavedState =
    savedState().apply {
        SavedStateEncoder(this, config).encodeSerializableValue(serializer, value)
    }

/**
 * Encode a serializable object to a [SavedState] with the default serializer.
 *
 * @sample androidx.savedstate.encode
 * @param value The serializable object to encode.
 * @param config The [SavedStateConfig] to use.
 * @return The encoded [SavedState].
 * @throws SerializationException if [value] cannot be serialized.
 */
public inline fun <reified T : Any> encodeToSavedState(
    value: T,
    config: SavedStateConfig = SavedStateConfig.DEFAULT,
): SavedState {
    return encodeToSavedState(
        serializer = config.serializersModule.serializer<T>(),
        config = config,
        value = value
    )
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
    private val config: SavedStateConfig
) : AbstractEncoder() {
    internal var key: String = ""
        private set

    override val serializersModule = config.serializersModule

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean =
        false

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        // The key will be property names for classes by default and can be modified with
        // `@SerialName`. The key for collections will be decimal integer Strings ("0",
        // "1", "2", ...).
        key = descriptor.getElementName(index)
        return true
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
            this
        } else {
            SavedStateEncoder(
                savedState =
                    savedState().also { child -> savedState.write { putSavedState(key, child) } },
                config = config
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
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
            else -> super.encodeSerializableValue(serializer, value)
        }
    }
}
