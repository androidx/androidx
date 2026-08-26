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

package androidx.a2ui.engine.catalog

import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.catalog.A2uiFunctionCollection
import androidx.a2ui.model.catalog.A2uiFunctionDefinition
import androidx.a2ui.model.catalog.A2uiFunctionReturnType
import androidx.a2ui.model.protocol.A2uiExecutionContext
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchema
import androidx.a2ui.model.schema.A2uiStringSchema
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test

class A2uiCoreCatalogSerializerTest {

    @Test
    fun jsonSchemaMap_returnsEquivalentMapRepresentation() {
        val testComponent = createTestComponent(TEST_COMPONENT_NAME_1, TEST_COMPONENT_DESCRIPTION_1)
        val catalog =
            createCatalog(
                title = TEST_CATALOG_TITLE,
                description = TEST_CATALOG_DESCRIPTION,
                components = listOf(testComponent),
            )

        val serializer = A2uiCoreCatalogSerializer(catalog)
        val schemaMap = serializer.jsonSchemaMap

        assertThat(schemaMap[KEY_SCHEMA]).isEqualTo(JSON_SCHEMA_DRAFT)
        assertThat(schemaMap[KEY_ID]).isEqualTo(TEST_CATALOG_ID_1)
        assertThat(schemaMap[KEY_CATALOG_ID]).isEqualTo(TEST_CATALOG_ID_1)
        assertThat(schemaMap[KEY_TITLE]).isEqualTo(TEST_CATALOG_TITLE)
        assertThat(schemaMap[KEY_DESCRIPTION]).isEqualTo(TEST_CATALOG_DESCRIPTION)

        @Suppress("UNCHECKED_CAST") val components = schemaMap[KEY_COMPONENTS] as Map<String, Any?>
        assertThat(components).containsKey(TEST_COMPONENT_NAME_1)
    }

    @Test
    fun jsonSchemaString_returnsSerializedJsonSchemaString() {
        val testComponent = createTestComponent(TEST_COMPONENT_NAME_1, TEST_COMPONENT_DESCRIPTION_1)
        val catalog =
            createCatalog(
                title = TEST_CATALOG_TITLE,
                description = TEST_CATALOG_DESCRIPTION,
                components = listOf(testComponent),
            )

        val serializer = A2uiCoreCatalogSerializer(catalog)
        val jsonString = serializer.jsonSchemaString

        assertThat(jsonString).contains("\"$KEY_CATALOG_ID\":\"$TEST_CATALOG_ID_1\"")
        assertThat(jsonString).contains("\"$KEY_TITLE\":\"$TEST_CATALOG_TITLE\"")
    }

    @Test
    fun caching_returnsCachedInstancesOnSubsequentCalls() {
        var componentDefinitionAccessCount = 0
        val testComponent =
            object : A2uiCoreComponentDefinition {
                override val name: String = TEST_COMPONENT_NAME_1
                override val description: String = TEST_COMPONENT_DESCRIPTION_1
                override val propertySchema: A2uiSchema
                    get() {
                        componentDefinitionAccessCount++
                        return A2uiObjectSchema()
                    }
            }
        val catalog =
            createCatalog(
                id = TEST_CATALOG_ID_1,
                components = listOf(testComponent),
                isInline = true,
            )

        val serializer = A2uiCoreCatalogSerializer(catalog)

        assertThat(componentDefinitionAccessCount).isEqualTo(0)

        val map1 = serializer.jsonSchemaMap
        val str1 = serializer.jsonSchemaString

        // 1 access during component.toSchema(), and 1 access during collectLocalDefinitions()
        assertThat(componentDefinitionAccessCount).isEqualTo(2)

        val map2 = serializer.jsonSchemaMap
        val str2 = serializer.jsonSchemaString

        // Access count remains 2 because cached properties on serializer are returned
        assertThat(componentDefinitionAccessCount).isEqualTo(2)
        assertThat(map1).isSameInstanceAs(map2)
        assertThat(str1).isSameInstanceAs(str2)
    }

    @Test
    fun serializer_withFunctions_serializesFunctionsCorrectly() {
        val testFunction =
            object : A2uiFunction {
                override val definition =
                    object : A2uiFunctionDefinition {
                        override val name = "testFunc"
                        override val description = "Test func description"
                        override val argumentSchema = A2uiStringSchema()
                        override val returnType = A2uiFunctionReturnType.BOOLEAN
                    }

                override fun execute(
                    args: Map<String, Any>,
                    executionContext: A2uiExecutionContext,
                ): Any = true
            }
        val catalog = createCatalog(functions = listOf(testFunction))
        val serializer = A2uiCoreCatalogSerializer(catalog)

        val root = Json.parseToJsonElement(serializer.jsonSchemaString) as JsonObject
        val functions = root[KEY_FUNCTIONS] as JsonObject
        assertThat(functions).containsKey("testFunc")
    }

    private fun createCatalog(
        id: String = TEST_CATALOG_ID_1,
        title: String? = null,
        description: String? = null,
        components: List<A2uiCoreComponentDefinition> = emptyList(),
        functions: List<A2uiFunction> = emptyList(),
        themeSchema: A2uiSchema? = null,
        isInline: Boolean = false,
    ): A2uiCoreCatalog =
        object : A2uiCoreCatalog {
            override val id: String = id
            override val title: String? = title
            override val description: String? = description
            override val componentDefinitions: A2uiCoreComponentDefinitionCollection =
                A2uiCoreComponentDefinitionCollection(components)
            override val functions: A2uiFunctionCollection = A2uiFunctionCollection(functions)
            override val themeSchema: A2uiSchema? = themeSchema
            override val isInline: Boolean = isInline
        }

    private fun createTestComponent(
        name: String,
        description: String,
    ): A2uiCoreComponentDefinition =
        object : A2uiCoreComponentDefinition {
            override val name: String = name
            override val description: String = description
            override val propertySchema: A2uiSchema = A2uiObjectSchema()
        }

    companion object {
        private const val JSON_SCHEMA_DRAFT = "https://json-schema.org/draft/2020-12/schema"
        private const val KEY_SCHEMA = "\$schema"
        private const val KEY_ID = "\$id"
        private const val KEY_CATALOG_ID = "catalogId"
        private const val KEY_TITLE = "title"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_COMPONENTS = "components"
        private const val KEY_FUNCTIONS = "functions"

        private const val TEST_CATALOG_ID_1 = "test_catalog_1"
        private const val TEST_CATALOG_TITLE = "Test Catalog Title"
        private const val TEST_CATALOG_DESCRIPTION = "Test Catalog Description"
        private const val TEST_COMPONENT_NAME_1 = "TestComponent1"
        private const val TEST_COMPONENT_DESCRIPTION_1 = "Test Component Description 1"
    }
}
