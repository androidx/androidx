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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiAllOfSchemaTest {

    @Test
    fun toJsonSchema_withoutDescription_returnsCorrectJsonObject() {
        val schemas = listOf(A2uiStringSchema(), A2uiBooleanSchema())
        val schema = A2uiAllOfSchema(schemas)
        val expected = buildJsonObject {
            put(
                ALL_OF_FIELD,
                buildJsonArray {
                    addJsonObject { put(TYPE_FIELD, STRING_TYPE) }
                    addJsonObject { put(TYPE_FIELD, BOOLEAN_TYPE) }
                },
            )
        }
        assertThat(Json.parseToJsonElement(schema.toJsonSchema())).isEqualTo(expected)
    }

    @Test
    fun toJsonSchema_withDescription_returnsCorrectJsonObject() {
        val schemas = listOf(A2uiStringSchema())
        val schema = A2uiAllOfSchema(schemas, TEST_DESCRIPTION_1)
        val expected = buildJsonObject {
            put(ALL_OF_FIELD, buildJsonArray { addJsonObject { put(TYPE_FIELD, STRING_TYPE) } })
            put(DESCRIPTION_FIELD, TEST_DESCRIPTION_1)
        }
        assertThat(Json.parseToJsonElement(schema.toJsonSchema())).isEqualTo(expected)
    }

    @Test
    fun equalsAndHashCode_behavesAccordingToContract() {
        val schemas1 = listOf(A2uiStringSchema())
        val schemas2 = listOf(A2uiBooleanSchema())
        EqualsTester()
            .addEqualityGroup(
                A2uiAllOfSchema(schemas1, TEST_DESCRIPTION_1),
                A2uiAllOfSchema(schemas1, TEST_DESCRIPTION_1),
            )
            .addEqualityGroup(A2uiAllOfSchema(schemas1, TEST_DESCRIPTION_2))
            .addEqualityGroup(A2uiAllOfSchema(schemas2, TEST_DESCRIPTION_1))
            .addEqualityGroup(A2uiAllOfSchema(schemas1, null))
            .testEquals()
    }

    @Test
    fun toString_withDescription_returnsExpectedFormat() {
        val schema = A2uiAllOfSchema(listOf(A2uiStringSchema()), TEST_DESCRIPTION_1)
        assertThat(schema.toString())
            .isEqualTo("AllOf(schemas=[String(description=null)], description=$TEST_DESCRIPTION_1)")
    }

    @Test
    fun toString_withoutDescription_returnsExpectedFormat() {
        val schema = A2uiAllOfSchema(listOf(A2uiStringSchema()), null)
        assertThat(schema.toString())
            .isEqualTo("AllOf(schemas=[String(description=null)], description=null)")
    }

    private companion object {
        private const val TEST_DESCRIPTION_1 = "testDescription1"
        private const val TEST_DESCRIPTION_2 = "testDescription2"
        private const val DESCRIPTION_FIELD = "description"
        private const val ALL_OF_FIELD = "allOf"
        private const val TYPE_FIELD = "type"
        private const val STRING_TYPE = "string"
        private const val BOOLEAN_TYPE = "boolean"
    }
}
