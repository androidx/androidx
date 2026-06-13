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

package androidx.a2ui.core.schema

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

private const val TEST_DESCRIPTION_1 = "testDescription1"
private const val TEST_DESCRIPTION_2 = "testDescription2"
private const val TEST_PROPERTY_1 = "prop1"
private const val TEST_PROPERTY_2 = "prop2"

private const val TYPE_FIELD = "type"
private const val PROPERTIES_FIELD = "properties"
private const val REQUIRED_FIELD = "required"
private const val DESCRIPTION_FIELD = "description"

private const val STRING_TYPE = "string"
private const val NUMBER_TYPE = "number"
private const val BOOLEAN_TYPE = "boolean"
private const val OBJECT_TYPE = "object"

@RunWith(JUnit4::class)
class A2uiObjectSchemaTest {
    @Test
    fun toJsonSchema_withoutDescriptionAndRequired_returnsCorrectJsonObject() {
        val schema =
            A2uiObjectSchema(
                properties =
                    mapOf(
                        TEST_PROPERTY_1 to A2uiStringSchema(),
                        TEST_PROPERTY_2 to A2uiNumberSchema(),
                    )
            )
        val expected = buildJsonObject {
            put(TYPE_FIELD, OBJECT_TYPE)
            put(
                PROPERTIES_FIELD,
                buildJsonObject {
                    put(TEST_PROPERTY_1, buildJsonObject { put(TYPE_FIELD, STRING_TYPE) })
                    put(TEST_PROPERTY_2, buildJsonObject { put(TYPE_FIELD, NUMBER_TYPE) })
                },
            )
        }
        assertThat(Json.parseToJsonElement(schema.toJsonSchema())).isEqualTo(expected)
    }

    @Test
    fun toJsonSchema_withDescriptionAndRequired_returnsCorrectJsonObject() {
        val schema =
            A2uiObjectSchema(
                properties = mapOf(TEST_PROPERTY_1 to A2uiBooleanSchema()),
                required = setOf(TEST_PROPERTY_1),
                description = TEST_DESCRIPTION_1,
            )
        val expected = buildJsonObject {
            put(TYPE_FIELD, OBJECT_TYPE)
            put(
                PROPERTIES_FIELD,
                buildJsonObject {
                    put(TEST_PROPERTY_1, buildJsonObject { put(TYPE_FIELD, BOOLEAN_TYPE) })
                },
            )
            put(REQUIRED_FIELD, buildJsonArray { add(JsonPrimitive(TEST_PROPERTY_1)) })
            put(DESCRIPTION_FIELD, TEST_DESCRIPTION_1)
        }
        assertThat(Json.parseToJsonElement(schema.toJsonSchema())).isEqualTo(expected)
    }

    @Test
    fun toJsonSchema_withEmptyRequired_doesNotIncludeRequiredField() {
        val schema =
            A2uiObjectSchema(
                properties = mapOf(TEST_PROPERTY_1 to A2uiBooleanSchema()),
                required = emptySet(),
            )
        val expected = buildJsonObject {
            put(TYPE_FIELD, OBJECT_TYPE)
            put(
                PROPERTIES_FIELD,
                buildJsonObject {
                    put(TEST_PROPERTY_1, buildJsonObject { put(TYPE_FIELD, BOOLEAN_TYPE) })
                },
            )
        }
        assertThat(Json.parseToJsonElement(schema.toJsonSchema())).isEqualTo(expected)
    }

    @Test
    fun toJsonSchema_withEmptyProperties_includesEmptyProperties() {
        val schema = A2uiObjectSchema(properties = emptyMap())
        val expected = buildJsonObject {
            put(TYPE_FIELD, OBJECT_TYPE)
            put(PROPERTIES_FIELD, buildJsonObject {})
        }
        assertThat(Json.parseToJsonElement(schema.toJsonSchema())).isEqualTo(expected)
    }

    @Test
    fun init_withMissingRequiredProperties_throwsIllegalArgumentException() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                A2uiObjectSchema(properties = emptyMap(), required = setOf(TEST_PROPERTY_1))
            }
        assertThat(exception).hasMessageThat().contains("Missing keys: [$TEST_PROPERTY_1]")
    }

    @Test
    fun equalsAndHashCode_behavesAccordingToContract() {
        EqualsTester()
            .addEqualityGroup(
                A2uiObjectSchema(
                    properties = mapOf(TEST_PROPERTY_1 to A2uiStringSchema()),
                    required = setOf(TEST_PROPERTY_1),
                    description = TEST_DESCRIPTION_1,
                ),
                A2uiObjectSchema(
                    properties = mapOf(TEST_PROPERTY_1 to A2uiStringSchema()),
                    required = setOf(TEST_PROPERTY_1),
                    description = TEST_DESCRIPTION_1,
                ),
            )
            .addEqualityGroup(
                A2uiObjectSchema(
                    properties = mapOf(TEST_PROPERTY_1 to A2uiStringSchema()),
                    required = emptySet(),
                    description = TEST_DESCRIPTION_1,
                )
            )
            .addEqualityGroup(
                A2uiObjectSchema(
                    properties = mapOf(TEST_PROPERTY_1 to A2uiStringSchema()),
                    required = setOf(TEST_PROPERTY_1),
                    description = TEST_DESCRIPTION_2,
                )
            )
            .addEqualityGroup(
                A2uiObjectSchema(properties = mapOf(), required = emptySet(), description = null)
            )
            .testEquals()
    }

    @Test
    fun toString_withDescription_returnsExpectedFormat() {
        val schema =
            A2uiObjectSchema(
                properties = mapOf(),
                required = emptySet(),
                description = TEST_DESCRIPTION_1,
            )
        assertThat(schema.toString())
            .isEqualTo(
                "Object(properties={}, required=[], isAdditionalPropertiesAllowed=true, additionalPropertiesSchema=null, description=$TEST_DESCRIPTION_1)"
            )
    }

    @Test
    fun toString_withoutDescription_returnsExpectedFormat() {
        val schema =
            A2uiObjectSchema(properties = mapOf(), required = emptySet(), description = null)
        assertThat(schema.toString())
            .isEqualTo(
                "Object(properties={}, required=[], isAdditionalPropertiesAllowed=true, additionalPropertiesSchema=null, description=null)"
            )
    }
}
