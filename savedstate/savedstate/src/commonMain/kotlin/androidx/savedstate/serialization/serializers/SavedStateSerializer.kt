/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.savedstate.serialization.serializers

import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.savedstate.serialization.SavedStateDecoder
import androidx.savedstate.serialization.SavedStateEncoder
import androidx.savedstate.write
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A serializer for [SavedState].
 *
 * This serializer operates in two modes:
 * 1. **Optimized (Direct):** When used with [SavedStateEncoder]/[SavedStateDecoder], it writes
 *    directly to the underlying Bundle, bypassing intermediate object allocation.
 * 2. **Fallback (Wrapped):** When used with generic encoders (e.g. JSON), it converts the
 *    loosely-typed SavedState into strictly-typed wrapper classes to preserve type fidelity.
 */
@OptIn(ExperimentalSerializationApi::class)
public object SavedStateSerializer : KSerializer<SavedState> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("androidx.savedstate.SavedState")

    override fun serialize(encoder: Encoder, value: SavedState) {
        if (encoder is SavedStateEncoder) {
            // Optimization: Direct Bundle manipulation
            if (encoder.key == "") {
                encoder.savedState.write { putAll(value) }
            } else {
                encoder.savedState.write { putSavedState(encoder.key, value) }
            }
        } else {
            // Fallback: Convert to explicit wrapper map for generic formats (JSON, etc.)
            val genericMap = value.read { toMap() }
            val wrappedMap = SavedStateValueConverter.wrapMap(genericMap)
            encoder.encodeSerializableValue(FALLBACK_SERIALIZER, wrappedMap)
        }
    }

    override fun deserialize(decoder: Decoder): SavedState {
        if (decoder is SavedStateDecoder) {
            // Optimization: Direct Bundle manipulation
            return if (decoder.key == "") {
                decoder.savedState
            } else {
                decoder.savedState.read { getSavedState(decoder.key) }
            }
        } else {
            // Fallback: Decode wrapper map and unwrap to generic Map
            val wrappedMap = decoder.decodeSerializableValue(FALLBACK_SERIALIZER)
            val genericMap = SavedStateValueConverter.unwrapMap(wrappedMap)
            return savedState(genericMap)
        }
    }
}

private val FALLBACK_SERIALIZER = MapSerializer(String.serializer(), SerializableValue.serializer())

/**
 * Handles the mapping between loosely typed `Any?` (from Bundles) and strictly typed
 * [SerializableValue] wrappers (for KXS).
 */
@Suppress("UNCHECKED_CAST", "CAST_NEVER_SUCCEEDS")
private object SavedStateValueConverter {

    fun wrapMap(from: Map<String, Any?>): Map<String, SerializableValue> =
        buildMap(from.size) {
            for ((key, value) in from) {
                put(key, wrap(value))
            }
        }

    fun unwrapMap(from: Map<String, SerializableValue>): Map<String, Any?> =
        buildMap(from.size) {
            for ((key, value) in from) {
                put(key, unwrap(value))
            }
        }

    private fun wrap(value: Any?): SerializableValue =
        when (value) {
            null -> NullValue

            // Primitives
            is Boolean -> BooleanValue(value)
            is Char -> CharValue(value)
            is Double -> DoubleValue(value)
            is Float -> FloatValue(value)
            is Int -> IntValue(value)
            is Long -> LongValue(value)

            // Strings & CharSequences
            is String -> StringValue(value)
            is CharSequence -> CharSequenceValue(value.toString())

            // Primitive Arrays
            is BooleanArray -> BooleanArrayValue(value)
            is CharArray -> CharArrayValue(value)
            is DoubleArray -> DoubleArrayValue(value)
            is FloatArray -> FloatArrayValue(value)
            is IntArray -> IntArrayValue(value)
            is LongArray -> LongArrayValue(value)

            // Object Arrays
            is Array<*> -> wrapArray(value)

            // Lists
            is List<*> -> wrapList(value)

            // Nested SavedState
            is SavedState -> SavedStateValue(value)

            else ->
                throw IllegalArgumentException("Unsupported type in SavedState: ${value::class}")
        }

    private fun wrapArray(value: Array<*>): SerializableValue {
        if (value.isEmpty()) return StringArrayValue(emptyArray())

        return when (val first = value.first()) {
            is Int -> IntArrayValue(value as IntArray)
            is String -> StringArrayValue(value as Array<String>)
            is CharSequence -> CharSequenceListValue(value.map { it.toString() })
            is SavedState -> SavedStateListValue(value as List<SavedState>)
            else -> throw IllegalArgumentException("Unsupported Array type: ${first!!::class}")
        }
    }

    private fun wrapList(value: List<*>): SerializableValue {
        if (value.isEmpty()) return StringListValue(emptyList())

        return when (val first = value.first()) {
            is Int -> IntListValue(value as List<Int>)
            is String -> StringListValue(value as List<String>)
            is CharSequence ->
                CharSequenceListValue((value as List<CharSequence>).map { it.toString() })
            is SavedState -> SavedStateListValue(value as List<SavedState>)
            else -> throw IllegalArgumentException("Unsupported List type: ${first!!::class}")
        }
    }

    private fun unwrap(value: SerializableValue): Any? =
        when (value) {
            is BooleanValue -> value.value
            is CharValue -> value.value
            is DoubleValue -> value.value
            is FloatValue -> value.value
            is IntValue -> value.value
            is LongValue -> value.value
            NullValue -> null

            is StringValue -> value.value
            is CharSequenceValue -> value.value

            is BooleanArrayValue -> value.value
            is CharArrayValue -> value.value
            is DoubleArrayValue -> value.value
            is FloatArrayValue -> value.value
            is IntArrayValue -> value.value
            is LongArrayValue -> value.value

            is StringArrayValue -> value.value
            is CharSequenceArrayValue -> value.value
            is SavedStateArrayValue -> value.value

            is IntListValue -> value.value
            is StringListValue -> value.value
            is CharSequenceListValue -> value.value
            is SavedStateListValue -> value.value

            is SavedStateValue -> value.value
        }
}

@Serializable private sealed interface SerializableValue

// Primitives
@Serializable private data class BooleanValue(val value: Boolean) : SerializableValue

@Serializable private data class CharValue(val value: Char) : SerializableValue

@Serializable private data class DoubleValue(val value: Double) : SerializableValue

@Serializable private data class FloatValue(val value: Float) : SerializableValue

@Serializable private data class IntValue(val value: Int) : SerializableValue

@Serializable private data class LongValue(val value: Long) : SerializableValue

@Serializable private data object NullValue : SerializableValue

// Strings & CharSequences
@Serializable private data class StringValue(val value: String) : SerializableValue

@Serializable private data class CharSequenceValue(val value: String) : SerializableValue

// Primitive Arrays
@Serializable private data class BooleanArrayValue(val value: BooleanArray) : SerializableValue

@Serializable private data class CharArrayValue(val value: CharArray) : SerializableValue

@Serializable private data class DoubleArrayValue(val value: DoubleArray) : SerializableValue

@Serializable private data class FloatArrayValue(val value: FloatArray) : SerializableValue

@Serializable private data class IntArrayValue(val value: IntArray) : SerializableValue

@Serializable private data class LongArrayValue(val value: LongArray) : SerializableValue

// Object Arrays
@Serializable private data class StringArrayValue(val value: Array<String>) : SerializableValue

@Serializable
private data class CharSequenceArrayValue(val value: Array<String>) : SerializableValue

@Serializable
private data class SavedStateArrayValue(
    val value: Array<@Serializable(with = SavedStateSerializer::class) SavedState>
) : SerializableValue

// Lists
@Serializable private data class IntListValue(val value: List<Int>) : SerializableValue

@Serializable private data class StringListValue(val value: List<String>) : SerializableValue

@Serializable private data class CharSequenceListValue(val value: List<String>) : SerializableValue

@Serializable
private data class SavedStateListValue(
    val value: List<@Serializable(with = SavedStateSerializer::class) SavedState>
) : SerializableValue

@Serializable
private data class SavedStateValue(
    @Serializable(with = SavedStateSerializer::class) val value: SavedState
) : SerializableValue
