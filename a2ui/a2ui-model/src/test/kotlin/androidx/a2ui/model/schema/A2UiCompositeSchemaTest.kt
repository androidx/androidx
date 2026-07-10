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

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2UiCompositeSchemaTest {

    @Test
    fun toJsonSchema_withDefinitionNameAndSchemaId_returnsRefWithSchemaId() {
        val schema =
            object : A2uiCompositeSchema() {
                override val definitionName = DEFINITION_NAME
                override val schemaId = SCHEMA_ID
                override val description = null

                override fun getDefinition() = OBJECT_SCHEMA
            }

        val expected = A2uiRefSchema(ref = "$SCHEMA_ID#/\$defs/$DEFINITION_NAME").toJsonElement()
        assertThat(Json.parseToJsonElement(schema.toJsonSchema())).isEqualTo(expected)
    }

    @Test
    fun toJsonSchema_withDefinitionNameAndNullSchemaId_returnsRefWithoutSchemaId() {
        val schema =
            object : A2uiCompositeSchema() {
                override val definitionName = DEFINITION_NAME
                override val schemaId = null
                override val description = null

                override fun getDefinition() = OBJECT_SCHEMA
            }

        val expected = A2uiRefSchema(ref = "#/\$defs/$DEFINITION_NAME").toJsonElement()
        assertThat(Json.parseToJsonElement(schema.toJsonSchema())).isEqualTo(expected)
    }

    @Test
    fun toJsonSchema_withDefinitionNameAndDescription_returnsRefWithDescription() {
        val schema =
            object : A2uiCompositeSchema() {
                override val definitionName = DEFINITION_NAME
                override val schemaId = null
                override val description = DESCRIPTION

                override fun getDefinition() = OBJECT_SCHEMA
            }

        val expected =
            A2uiRefSchema(ref = "#/\$defs/$DEFINITION_NAME", description = DESCRIPTION)
                .toJsonElement()
        assertThat(Json.parseToJsonElement(schema.toJsonSchema())).isEqualTo(expected)
    }

    @Test
    fun toJsonSchema_withoutDefinitionName_delegatesToDefinition() {
        val schema =
            object : A2uiCompositeSchema() {
                override val definitionName = null
                override val schemaId = null
                override val description = null

                override fun getDefinition() = OBJECT_SCHEMA
            }

        val expected = OBJECT_SCHEMA.toJsonElement()
        assertThat(Json.parseToJsonElement(schema.toJsonSchema())).isEqualTo(expected)
    }

    @Test
    fun getDefinitionJsonSchema_returnsJsonSchemaOfDefinition() {
        val schema =
            object : A2uiCompositeSchema() {
                override val definitionName = DEFINITION_NAME
                override val schemaId = SCHEMA_ID
                override val description = DESCRIPTION

                override fun getDefinition() = OBJECT_SCHEMA
            }

        val expected = OBJECT_SCHEMA.toJsonElement()
        assertThat(Json.parseToJsonElement(schema.getDefinitionJsonSchema())).isEqualTo(expected)
    }

    private companion object {
        const val DEFINITION_NAME = "ChildDef"
        const val SCHEMA_ID = "mySchemaId"
        const val DESCRIPTION = "My schema description"
        val OBJECT_SCHEMA = A2uiObjectSchema.INSTANCE
    }
}
