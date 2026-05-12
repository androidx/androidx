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

import androidx.kruth.assertThat
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer

class DeepLinkDecoderTest {

    @Test
    fun testDecodeSimpleKey() {
        val arguments = mapOf("name" to listOf("john"), "age" to listOf("30"))
        val decoder = DeepLinkDecoder(arguments)
        val result = decoder.decodeSerializableValue(serializer<SimpleKey>())

        assertThat(result).isEqualTo(SimpleKey("john", 30))
    }

    @Test
    fun testDecodeMissingRequiredFieldThrows() {
        val arguments =
            mapOf(
                "name" to listOf("john")
                // age is missing
            )
        val decoder = DeepLinkDecoder(arguments)

        assertFailsWith<SerializationException> {
            decoder.decodeSerializableValue(serializer<SimpleKey>())
        }
    }

    @Test
    fun testDecodeDefaultValues() {
        val arguments = emptyMap<String, List<String>>()
        val decoder = DeepLinkDecoder(arguments)
        val result = decoder.decodeSerializableValue(serializer<DefaultKey>())

        // Should fall back to defaults defined in the data class
        assertThat(result).isEqualTo(DefaultKey("default", 0))
    }

    @Test
    fun testDecodeNullableValues_present() {
        val arguments = mapOf("name" to listOf("john"), "age" to listOf("30"))
        val decoder = DeepLinkDecoder(arguments)
        val result = decoder.decodeSerializableValue(serializer<NullableKey>())

        assertThat(result).isEqualTo(NullableKey("john", 30))
    }

    @Test
    fun testDecodeNullableValues_missingThrows() {
        val arguments = emptyMap<String, List<String>>()
        val decoder = DeepLinkDecoder(arguments)

        // In our current implementation, missing keys are skipped.
        // Since NullableKey does not provide defaults, skipping them causes the framework to throw.
        assertFailsWith<SerializationException> {
            decoder.decodeSerializableValue(serializer<NullableKey>())
        }
    }

    @Test
    fun testDecodeNestedObject() {
        val arguments =
            mapOf("name" to listOf("john"), "age" to listOf("30"), "flag" to listOf("true"))
        val decoder = DeepLinkDecoder(arguments)
        val result = decoder.decodeSerializableValue(serializer<NestedKey>())

        // Flat map should populate the nested SimpleKey object
        assertThat(result).isEqualTo(NestedKey(SimpleKey("john", 30), true))
    }

    @Test
    fun testDecodeInvalidPrimitiveFormatThrows() {
        val arguments =
            mapOf(
                "name" to listOf("john"),
                "age" to listOf("notAnInt"), // Invalid Int
            )
        val decoder = DeepLinkDecoder(arguments)

        assertFailsWith<IllegalArgumentException> {
            decoder.decodeSerializableValue(serializer<SimpleKey>())
        }
    }

    @Test
    fun testDecodeEnum() {
        val arguments = mapOf("direction" to listOf("NORTH"))
        val decoder = DeepLinkDecoder(arguments)
        val result = decoder.decodeSerializableValue(serializer<EnumKey>())

        assertThat(result).isEqualTo(EnumKey(DirectionEnum.NORTH))
    }

    @Test
    fun testDecodeNestedEnum() {
        val arguments = mapOf("direction" to listOf("NORTH"), "flag" to listOf("true"))
        val decoder = DeepLinkDecoder(arguments)
        val result = decoder.decodeSerializableValue(serializer<NestedEnumKey>())

        assertThat(result).isEqualTo(NestedEnumKey(EnumKey(DirectionEnum.NORTH), true))
    }

    @Test
    fun testDecodeDefaultEnum() {
        val arguments = emptyMap<String, List<String>>()
        val decoder = DeepLinkDecoder(arguments)
        val result = decoder.decodeSerializableValue(serializer<DefaultEnumKey>())

        assertThat(result).isEqualTo(DefaultEnumKey(DirectionEnum.NORTH))
    }

    @Test
    fun testDecodeInvalidEnumThrows() {
        val arguments = mapOf("direction" to listOf("UP"))
        val decoder = DeepLinkDecoder(arguments)

        assertFailsWith<SerializationException> {
            decoder.decodeSerializableValue(serializer<EnumKey>())
        }
    }

    @Test
    fun testDecodeList() {
        val arguments = mapOf("list" to listOf("1", "2", "3"))
        val decoder = DeepLinkDecoder(arguments)
        val result = decoder.decodeSerializableValue(serializer<ListKey>())

        assertThat(result).isEqualTo(ListKey(listOf(1, 2, 3)))
    }

    @Test
    fun testDecodeSet() {
        val arguments = mapOf("set" to listOf("a", "b", "a"))
        val decoder = DeepLinkDecoder(arguments)
        val result = decoder.decodeSerializableValue(serializer<SetKey>())

        assertThat(result).isEqualTo(SetKey(setOf("a", "b")))
    }

    @Test
    fun testDecodeArray() {
        val arguments = mapOf("array" to listOf("true", "false", "true"))
        val decoder = DeepLinkDecoder(arguments)
        val result = decoder.decodeSerializableValue(serializer<ArrayKey>())

        assertThat(result.array.contentEquals(arrayOf(true, false, true))).isTrue()
    }

    @Test
    fun testDecodeNullableList() {
        val arguments = mapOf("list" to listOf("1", "null", "3"))
        val decoder = DeepLinkDecoder(arguments)
        val result = decoder.decodeSerializableValue(serializer<NullableListKey>())

        assertThat(result).isEqualTo(NullableListKey(listOf(1, null, 3)))
    }

    @Test
    fun testDecodeEmptyList() {
        val arguments = emptyMap<String, List<String>>()
        val decoder = DeepLinkDecoder(arguments)
        val result = decoder.decodeSerializableValue(serializer<DefaultListKey>())

        assertThat(result).isEqualTo(DefaultListKey(emptyList()))
    }

    @Test
    fun testDecodeNonEmptyDefaultList() {
        val arguments = emptyMap<String, List<String>>()
        val decoder = DeepLinkDecoder(arguments)
        val result = decoder.decodeSerializableValue(serializer<NonEmptyDefaultListKey>())

        assertThat(result).isEqualTo(NonEmptyDefaultListKey(listOf(1, 2, 3)))
    }

    @Test
    fun testDecodeNonPrimitiveListThrows() {
        val arguments = mapOf("list" to listOf("john", "12"))
        val decoder = DeepLinkDecoder(arguments)

        assertFailsWith<SerializationException> {
            decoder.decodeSerializableValue(serializer<NonPrimitiveListKey>())
        }
    }

    @Serializable data class SimpleKey(val name: String, val age: Int)

    @Serializable data class DefaultKey(val name: String = "default", val age: Int = 0)

    @Serializable data class NullableKey(val name: String?, val age: Int?)

    @Serializable data class NestedKey(val user: SimpleKey, val flag: Boolean)

    @Serializable
    enum class DirectionEnum {
        NORTH,
        SOUTH,
    }

    @Serializable data class EnumKey(val direction: DirectionEnum)

    @Serializable data class DefaultEnumKey(val direction: DirectionEnum = DirectionEnum.NORTH)

    @Serializable data class NestedEnumKey(val direction: EnumKey, val flag: Boolean)

    @Serializable data class ListKey(val list: List<Int>)

    @Serializable data class SetKey(val set: Set<String>)

    @Serializable data class ArrayKey(val array: Array<Boolean>)

    @Serializable data class NullableListKey(val list: List<Int?>)

    @Serializable data class DefaultListKey(val list: List<Int> = emptyList())

    @Serializable data class NonPrimitiveListKey(val list: List<SimpleKey>)

    @Serializable data class NonEmptyDefaultListKey(val list: List<Int> = listOf(1, 2, 3))
}
