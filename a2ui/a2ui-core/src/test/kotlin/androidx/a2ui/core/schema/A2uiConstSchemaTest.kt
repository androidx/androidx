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
private const val DESCRIPTION_FIELD = "description"
private const val CONST_FIELD = "const"
private const val VALUE_1 = "value1"
private const val VALUE_2 = "value2"

@RunWith(JUnit4::class)
class A2uiConstSchemaTest {

    @Test
    fun toJsonSchema_withoutDescription_returnsCorrectJsonObject() {
        val schema = A2uiConstSchema(VALUE_1)
        val expected = buildJsonObject { put(CONST_FIELD, VALUE_1) }
        assertThat(Json.parseToJsonElement(schema.toJsonSchema())).isEqualTo(expected)
    }

    @Test
    fun toJsonSchema_withDescription_returnsCorrectJsonObject() {
        val schema = A2uiConstSchema(VALUE_1, TEST_DESCRIPTION_1)
        val expected = buildJsonObject {
            put(CONST_FIELD, VALUE_1)
            put(DESCRIPTION_FIELD, TEST_DESCRIPTION_1)
        }
        assertThat(Json.parseToJsonElement(schema.toJsonSchema())).isEqualTo(expected)
    }

    @Test
    fun equalsAndHashCode_behavesAccordingToContract() {
        EqualsTester()
            .addEqualityGroup(
                A2uiConstSchema(VALUE_1, TEST_DESCRIPTION_1),
                A2uiConstSchema(VALUE_1, TEST_DESCRIPTION_1),
            )
            .addEqualityGroup(A2uiConstSchema(VALUE_1, TEST_DESCRIPTION_2))
            .addEqualityGroup(A2uiConstSchema(VALUE_2, TEST_DESCRIPTION_1))
            .addEqualityGroup(A2uiConstSchema(VALUE_1, null))
            .testEquals()
    }

    @Test
    fun toString_withDescription_returnsExpectedFormat() {
        val schema = A2uiConstSchema(VALUE_1, TEST_DESCRIPTION_1)
        assertThat(schema.toString())
            .isEqualTo("Const(value=$VALUE_1, description=$TEST_DESCRIPTION_1)")
    }

    @Test
    fun toString_withoutDescription_returnsExpectedFormat() {
        val schema = A2uiConstSchema(VALUE_1, null)
        assertThat(schema.toString()).isEqualTo("Const(value=$VALUE_1, description=null)")
    }

    @Test
    fun toJsonSchema_withNumber_returnsCorrectJsonObject() {
        val schema = A2uiConstSchema(123)
        val expected = buildJsonObject { put(CONST_FIELD, 123) }
        assertThat(Json.parseToJsonElement(schema.toJsonSchema())).isEqualTo(expected)
    }

    @Test
    fun toJsonSchema_withBoolean_returnsCorrectJsonObject() {
        val schema = A2uiConstSchema(true)
        val expected = buildJsonObject { put(CONST_FIELD, true) }
        assertThat(Json.parseToJsonElement(schema.toJsonSchema())).isEqualTo(expected)
    }

    @Test
    fun toJsonSchema_withList_returnsCorrectJsonObject() {
        val schema = A2uiConstSchema(listOf("a", 1))
        val expected = buildJsonObject {
            put(
                CONST_FIELD,
                buildJsonArray {
                    add(JsonPrimitive("a"))
                    add(JsonPrimitive(1))
                },
            )
        }
        assertThat(Json.parseToJsonElement(schema.toJsonSchema())).isEqualTo(expected)
    }

    @Test
    fun toJsonSchema_withMap_returnsCorrectJsonObject() {
        val schema = A2uiConstSchema(mapOf("key" to "value"))
        val expected = buildJsonObject { put(CONST_FIELD, buildJsonObject { put("key", "value") }) }
        assertThat(Json.parseToJsonElement(schema.toJsonSchema())).isEqualTo(expected)
    }

    @Test
    fun toJsonSchema_withUnsupportedType_throwsException() {
        val exception =
            assertFailsWith<IllegalArgumentException> { A2uiConstSchema(object {}).toJsonSchema() }
        assertThat(exception).hasMessageThat().contains("Unsupported type")
    }
}
