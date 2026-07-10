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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiArraySchemaTest {
    @Test
    fun toJsonSchema_withDescription_returnsCorrectJsonObject() {
        val schema = A2uiArraySchema(A2uiStringSchema(), TEST_DESCRIPTION_1)
        val expected = buildJsonObject {
            put(TYPE_FIELD, ARRAY_TYPE)
            put(ITEMS_FIELD, buildJsonObject { put(TYPE_FIELD, STRING_TYPE) })
            put(DESCRIPTION_FIELD, TEST_DESCRIPTION_1)
        }
        assertThat(Json.parseToJsonElement(schema.toJsonSchema())).isEqualTo(expected)
    }

    @Test
    fun toJsonSchema_withoutDescription_returnsCorrectJsonObject() {
        val noDesc = A2uiArraySchema(A2uiStringSchema())
        val expectedNoDesc = buildJsonObject {
            put(TYPE_FIELD, ARRAY_TYPE)
            put(ITEMS_FIELD, buildJsonObject { put(TYPE_FIELD, STRING_TYPE) })
        }
        assertThat(Json.parseToJsonElement(noDesc.toJsonSchema())).isEqualTo(expectedNoDesc)
    }

    @Test
    fun equalsAndHashCode_behavesAccordingToContract() {
        EqualsTester()
            .addEqualityGroup(
                A2uiArraySchema(A2uiStringSchema(), TEST_DESCRIPTION_1),
                A2uiArraySchema(A2uiStringSchema(), TEST_DESCRIPTION_1),
            )
            .addEqualityGroup(A2uiArraySchema(A2uiNumberSchema(), TEST_DESCRIPTION_1))
            .addEqualityGroup(A2uiArraySchema(A2uiStringSchema(), TEST_DESCRIPTION_2))
            .addEqualityGroup(A2uiArraySchema(A2uiNumberSchema(), null))
            .testEquals()
    }

    @Test
    fun toString_withDescription_returnsExpectedFormat() {
        val schema = A2uiArraySchema(A2uiStringSchema(), TEST_DESCRIPTION_1)
        assertThat(schema.toString())
            .isEqualTo("Array(items=String(description=null), description=$TEST_DESCRIPTION_1)")
    }

    @Test
    fun toString_withoutDescription_returnsExpectedFormat() {
        val schema = A2uiArraySchema(A2uiStringSchema(), null)
        assertThat(schema.toString())
            .isEqualTo("Array(items=String(description=null), description=null)")
    }

    private companion object {
        const val TEST_DESCRIPTION_1 = "testDescription1"
        const val TEST_DESCRIPTION_2 = "testDescription2"
        const val TYPE_FIELD = "type"
        const val DESCRIPTION_FIELD = "description"
        const val ITEMS_FIELD = "items"
        const val ARRAY_TYPE = "array"
        const val STRING_TYPE = "string"
    }
}
