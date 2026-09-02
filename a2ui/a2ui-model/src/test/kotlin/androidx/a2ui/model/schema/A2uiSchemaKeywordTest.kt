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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiSchemaKeywordTest {
    private val schema1 = A2uiRefSchema("ref1")
    private val schema2 = A2uiRefSchema("ref2")

    @Test
    fun equalsAndHashCode_behavesAccordingToContract() {
        EqualsTester()
            .addEqualityGroup(
                A2uiSchemaKeyword.OneOf(listOf(schema1)),
                A2uiSchemaKeyword.OneOf(listOf(schema1)),
            )
            .addEqualityGroup(A2uiSchemaKeyword.OneOf(listOf(schema2)))
            .addEqualityGroup(
                A2uiSchemaKeyword.AllOf(listOf(schema1)),
                A2uiSchemaKeyword.AllOf(listOf(schema1)),
            )
            .addEqualityGroup(A2uiSchemaKeyword.AllOf(listOf(schema2)))
            .addEqualityGroup(
                A2uiSchemaKeyword.AnyOf(listOf(schema1)),
                A2uiSchemaKeyword.AnyOf(listOf(schema1)),
            )
            .addEqualityGroup(A2uiSchemaKeyword.AnyOf(listOf(schema2)))
            .addEqualityGroup(A2uiSchemaKeyword.Not(schema1), A2uiSchemaKeyword.Not(schema1))
            .addEqualityGroup(A2uiSchemaKeyword.Not(schema2))
            .addEqualityGroup(
                A2uiSchemaKeyword.Default("value1"),
                A2uiSchemaKeyword.Default("value1"),
            )
            .addEqualityGroup(A2uiSchemaKeyword.Default("value2"))
            .addEqualityGroup(
                A2uiSchemaKeyword.Enum(listOf("value1")),
                A2uiSchemaKeyword.Enum(listOf("value1")),
            )
            .addEqualityGroup(A2uiSchemaKeyword.Enum(listOf("value2")))
            .addEqualityGroup(A2uiSchemaKeyword.Const("value1"), A2uiSchemaKeyword.Const("value1"))
            .addEqualityGroup(A2uiSchemaKeyword.Const("value2"))
            .addEqualityGroup(
                A2uiSchemaKeyword.Format("date-time"),
                A2uiSchemaKeyword.Format("date-time"),
            )
            .addEqualityGroup(A2uiSchemaKeyword.Format("date"))
            .addEqualityGroup(
                A2uiSchemaKeyword.IfThen(ifSchema = schema1, thenSchema = schema2),
                A2uiSchemaKeyword.IfThen(ifSchema = schema1, thenSchema = schema2),
            )
            .addEqualityGroup(
                A2uiSchemaKeyword.IfThen(
                    ifSchema = schema1,
                    thenSchema = schema2,
                    elseSchema = schema1,
                )
            )
            .testEquals()
    }

    @Test
    fun addToJsonObject_format_serializesCorrectly() {
        val keyword = A2uiSchemaKeyword.Format("date-time")
        val jsonObject = buildJsonObject { keyword.addToJsonObject(this) }
        assertThat(jsonObject).isEqualTo(buildJsonObject { put("format", "date-time") })
    }

    @Test
    fun addToJsonObject_ifThen_serializesCorrectly() {
        val keyword =
            A2uiSchemaKeyword.IfThen(
                ifSchema = A2uiStringSchema(),
                thenSchema = A2uiAnySchema(keywords = listOf(A2uiSchemaKeyword.Format("date"))),
                elseSchema = A2uiNumberSchema(),
            )
        val jsonObject = buildJsonObject { keyword.addToJsonObject(this) }
        assertThat(jsonObject)
            .isEqualTo(
                buildJsonObject {
                    put("if", buildJsonObject { put("type", "string") })
                    put("then", buildJsonObject { put("format", "date") })
                    put("else", buildJsonObject { put("type", "number") })
                }
            )
    }
}
