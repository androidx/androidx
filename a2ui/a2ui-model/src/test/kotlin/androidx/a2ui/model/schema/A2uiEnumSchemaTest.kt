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

package androidx.a2ui.model.schema

import com.google.common.testing.EqualsTester
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiEnumSchemaTest {
    @Test
    fun toJsonSchema_withDescription_returnsCorrectJsonObject() {
        val schema = A2uiEnumSchema(listOf("A", "B"), TEST_DESCRIPTION_1)
        val expected = buildJsonObject {
            put(
                ENUM_FIELD,
                buildJsonArray {
                    add(JsonPrimitive("A"))
                    add(JsonPrimitive("B"))
                },
            )
            put(DESCRIPTION_FIELD, TEST_DESCRIPTION_1)
        }
        assertThat(Json.parseToJsonElement(schema.toJsonSchema())).isEqualTo(expected)
    }

    @Test
    fun toJsonSchema_withoutDescription_returnsCorrectJsonObject() {
        val noDesc = A2uiEnumSchema(listOf("X"))
        val expectedNoDesc = buildJsonObject {
            put(ENUM_FIELD, buildJsonArray { add(JsonPrimitive("X")) })
        }
        assertThat(Json.parseToJsonElement(noDesc.toJsonSchema())).isEqualTo(expectedNoDesc)
    }

    @Test
    fun toJsonSchema_noEnumOptions_includesEmptyEnumArray() {
        val noDesc = A2uiEnumSchema(emptyList())
        val expectedNoDesc = buildJsonObject { put(ENUM_FIELD, buildJsonArray {}) }
        assertThat(Json.parseToJsonElement(noDesc.toJsonSchema())).isEqualTo(expectedNoDesc)
    }

    @Test
    fun equalsAndHashCode_behavesAccordingToContract() {
        EqualsTester()
            .addEqualityGroup(
                A2uiEnumSchema(listOf("A", "B"), TEST_DESCRIPTION_1),
                A2uiEnumSchema(listOf("A", "B"), TEST_DESCRIPTION_1),
            )
            .addEqualityGroup(A2uiEnumSchema(listOf("A"), TEST_DESCRIPTION_1))
            .addEqualityGroup(A2uiEnumSchema(listOf("A", "B"), TEST_DESCRIPTION_2))
            .addEqualityGroup(A2uiEnumSchema(emptyList(), null))
            .testEquals()
    }

    @Test
    fun toString_withDescription_returnsExpectedFormat() {
        val schema = A2uiEnumSchema(listOf("A", "B"), TEST_DESCRIPTION_1)
        assertThat(schema.toString())
            .isEqualTo("Enum(enumValues=[A, B], description=$TEST_DESCRIPTION_1)")
    }

    @Test
    fun toString_withoutDescription_returnsExpectedFormat() {
        val schema = A2uiEnumSchema(listOf("A", "B"), null)
        assertThat(schema.toString()).isEqualTo("Enum(enumValues=[A, B], description=null)")
    }

    @Test
    fun toJsonSchema_withNumbers_returnsCorrectJsonObject() {
        val schema = A2uiEnumSchema(listOf(1, 2.5))
        val expected = buildJsonObject {
            put(
                ENUM_FIELD,
                buildJsonArray {
                    add(JsonPrimitive(1))
                    add(JsonPrimitive(2.5))
                },
            )
        }
        assertThat(Json.parseToJsonElement(schema.toJsonSchema())).isEqualTo(expected)
    }

    @Test
    fun toJsonSchema_withBooleans_returnsCorrectJsonObject() {
        val schema = A2uiEnumSchema(listOf(true, false))
        val expected = buildJsonObject {
            put(
                ENUM_FIELD,
                buildJsonArray {
                    add(JsonPrimitive(true))
                    add(JsonPrimitive(false))
                },
            )
        }
        assertThat(Json.parseToJsonElement(schema.toJsonSchema())).isEqualTo(expected)
    }

    @Test
    fun toJsonSchema_withLists_returnsCorrectJsonObject() {
        val schema = A2uiEnumSchema(listOf(listOf("a"), listOf(1)))
        val expected = buildJsonObject {
            put(
                ENUM_FIELD,
                buildJsonArray {
                    add(buildJsonArray { add(JsonPrimitive("a")) })
                    add(buildJsonArray { add(JsonPrimitive(1)) })
                },
            )
        }
        assertThat(Json.parseToJsonElement(schema.toJsonSchema())).isEqualTo(expected)
    }

    @Test
    fun toJsonSchema_withMaps_returnsCorrectJsonObject() {
        val schema = A2uiEnumSchema(listOf(mapOf("key" to 1), mapOf("another" to true)))
        val expected = buildJsonObject {
            put(
                ENUM_FIELD,
                buildJsonArray {
                    add(buildJsonObject { put("key", 1) })
                    add(buildJsonObject { put("another", true) })
                },
            )
        }
        assertThat(Json.parseToJsonElement(schema.toJsonSchema())).isEqualTo(expected)
    }

    @Test
    fun toJsonSchema_withUnsupportedType_throwsException() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                A2uiEnumSchema(listOf(object {})).toJsonSchema()
            }
        assertThat(exception).hasMessageThat().contains("Unsupported type")
    }

    private companion object {
        const val TEST_DESCRIPTION_1 = "testDescription1"
        const val TEST_DESCRIPTION_2 = "testDescription2"
        const val DESCRIPTION_FIELD = "description"
        const val ENUM_FIELD = "enum"
    }
}
