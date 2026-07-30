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
import androidx.a2ui.model.schema.A2uiCompositeSchema
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchema
import androidx.a2ui.model.schema.A2uiStringSchema
import androidx.a2ui.model.schema.commontypes.A2uiComponentCommonSchema
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class A2uiCoreCatalogTest {

    @Test
    fun toJsonSchema_withFullMetadata_serializesAllMetadataFields() {
        val catalog =
            createCatalog(title = TEST_CATALOG_TITLE, description = TEST_CATALOG_DESCRIPTION)

        val root = parseCatalogJsonSchema(catalog)

        assertThat(root[KEY_SCHEMA]?.jsonPrimitive?.content).isEqualTo(JSON_SCHEMA_DRAFT)
        assertThat(root[KEY_ID]?.jsonPrimitive?.content).isEqualTo(TEST_CATALOG_ID_1)
        assertThat(root[KEY_CATALOG_ID]?.jsonPrimitive?.content).isEqualTo(TEST_CATALOG_ID_1)
        assertThat(root[KEY_TITLE]?.jsonPrimitive?.content).isEqualTo(TEST_CATALOG_TITLE)
        assertThat(root[KEY_DESCRIPTION]?.jsonPrimitive?.content)
            .isEqualTo(TEST_CATALOG_DESCRIPTION)
    }

    @Test
    fun toJsonSchema_withNullTitle_omitsTitleKey() {
        val catalog = createCatalog(title = null, description = TEST_CATALOG_DESCRIPTION)

        val root = parseCatalogJsonSchema(catalog)

        assertThat(root[KEY_DESCRIPTION]?.jsonPrimitive?.content)
            .isEqualTo(TEST_CATALOG_DESCRIPTION)
        assertThat(root).doesNotContainKey(KEY_TITLE)
    }

    @Test
    fun toJsonSchema_withNullDescription_omitsDescriptionKey() {
        val catalog = createCatalog(title = TEST_CATALOG_TITLE, description = null)

        val root = parseCatalogJsonSchema(catalog)

        assertThat(root[KEY_TITLE]?.jsonPrimitive?.content).isEqualTo(TEST_CATALOG_TITLE)
        assertThat(root).doesNotContainKey(KEY_DESCRIPTION)
    }

    @Test
    fun toJsonSchema_withComponents_serializesComponentsMap() {
        val testComponent = createTestComponent(TEST_COMPONENT_NAME_1, TEST_COMPONENT_DESCRIPTION_1)
        val catalog = createCatalog(components = listOf(testComponent))

        val root = parseCatalogJsonSchema(catalog)

        val components = root[KEY_COMPONENTS] as JsonObject
        assertThat(components).containsKey(TEST_COMPONENT_NAME_1)
    }

    @Test
    fun toJsonSchema_withEmptyComponents_serializesEmptyComponentsMap() {
        val catalog = createCatalog(components = emptyList())

        val root = parseCatalogJsonSchema(catalog)

        val components = root[KEY_COMPONENTS] as JsonObject
        assertThat(components).isEmpty()
    }

    @Test
    fun toJsonSchema_withFunctions_serializesFunctionsMap() {
        val testFunction = createTestFunction(TEST_FUNCTION_NAME, TEST_FUNCTION_DESCRIPTION)
        val catalog = createCatalog(functions = listOf(testFunction))

        val root = parseCatalogJsonSchema(catalog)

        val functions = root[KEY_FUNCTIONS] as JsonObject
        assertThat(functions).containsKey(TEST_FUNCTION_NAME)
    }

    @Test
    fun toJsonSchema_withEmptyFunctions_serializesEmptyFunctionsMap() {
        val catalog = createCatalog(functions = emptyList())

        val root = parseCatalogJsonSchema(catalog)

        val functions = root[KEY_FUNCTIONS] as JsonObject
        assertThat(functions).isEmpty()
    }

    @Test
    fun toJsonSchema_withComponents_generatesAnyComponentDef() {
        val testComponent = createTestComponent(TEST_COMPONENT_NAME_1, TEST_COMPONENT_DESCRIPTION_1)
        val catalog = createCatalog(components = listOf(testComponent))

        val root = parseCatalogJsonSchema(catalog)
        val defs = root[KEY_DEFS] as JsonObject

        assertThat(defs).containsKey(KEY_ANY_COMPONENT)
        val anyComp = defs[KEY_ANY_COMPONENT] as JsonObject
        val oneOfArr = anyComp[KEY_ONE_OF] as JsonArray
        assertThat(oneOfArr).hasSize(1)
        val refObj = oneOfArr[0] as JsonObject
        assertThat(refObj[KEY_REF]?.jsonPrimitive?.content)
            .isEqualTo("#/components/$TEST_COMPONENT_NAME_1")

        val discriminatorObj = anyComp[KEY_DISCRIMINATOR] as JsonObject
        assertThat(discriminatorObj[KEY_PROPERTY_NAME]?.jsonPrimitive?.content)
            .isEqualTo(KEY_COMPONENT)
    }

    @Test
    fun toJsonSchema_withNoLocalReferences_containsOnlyEmptyStandardDefs() {
        val catalog = createCatalog(components = emptyList(), functions = emptyList())

        val root = parseCatalogJsonSchema(catalog)
        val defs = root[KEY_DEFS] as JsonObject

        assertThat(defs.keys).containsExactly(KEY_ANY_COMPONENT, KEY_ANY_FUNCTION)

        val anyComp = defs[KEY_ANY_COMPONENT] as JsonObject
        val anyCompOneOf = anyComp[KEY_ONE_OF] as JsonArray
        assertThat(anyCompOneOf).isEmpty()

        val anyFunc = defs[KEY_ANY_FUNCTION] as JsonObject
        val anyFuncOneOf = anyFunc[KEY_ONE_OF] as JsonArray
        assertThat(anyFuncOneOf).isEmpty()
    }

    @Test
    fun toJsonSchema_withFunctions_generatesAnyFunctionDefInDefs() {
        val testFunction = createTestFunction(TEST_FUNCTION_NAME, TEST_FUNCTION_DESCRIPTION)
        val catalog = createCatalog(functions = listOf(testFunction))

        val root = parseCatalogJsonSchema(catalog)
        val defs = root[KEY_DEFS] as JsonObject

        assertThat(defs).containsKey(KEY_ANY_FUNCTION)
        val anyFunc = defs[KEY_ANY_FUNCTION] as JsonObject
        val oneOfArr = anyFunc[KEY_ONE_OF] as JsonArray
        assertThat(oneOfArr).hasSize(1)
        val refObj = oneOfArr[0] as JsonObject
        assertThat(refObj[KEY_REF]?.jsonPrimitive?.content)
            .isEqualTo("#/functions/$TEST_FUNCTION_NAME")
    }

    @Test
    fun toJsonSchema_withLocalCompositeDefinitionInComponents_populatesDefs() {
        val testLocalComposite =
            object : A2uiCompositeSchema() {
                override val definitionName: String = TEST_DEF_NAME_1
                override val description: String? = null

                override fun getDefinition(): A2uiSchema =
                    A2uiObjectSchema(
                        properties = mapOf(TEST_PROPERTY_1 to A2uiStringSchema()),
                        required = setOf(TEST_PROPERTY_1),
                    )
            }

        val testComponent =
            object : A2uiCoreComponentDefinition {
                override val name: String = TEST_COMPONENT_NAME_2
                override val description: String = TEST_COMPONENT_DESCRIPTION_2
                override val propertySchema: A2uiSchema =
                    A2uiObjectSchema(properties = mapOf(TEST_PROPERTY_2 to testLocalComposite))
            }

        val catalog = createCatalog(components = listOf(testComponent))

        val root = parseCatalogJsonSchema(catalog)
        val defs = root[KEY_DEFS] as JsonObject

        val customDefObj = defs[TEST_DEF_NAME_1] as JsonObject
        assertThat(customDefObj[KEY_TYPE]?.jsonPrimitive?.content).isEqualTo(TYPE_OBJECT)

        val props = customDefObj[KEY_PROPERTIES] as JsonObject
        val propObj = props[TEST_PROPERTY_1] as JsonObject
        assertThat(propObj[KEY_TYPE]?.jsonPrimitive?.content).isEqualTo(TYPE_STRING)

        val requiredArr = customDefObj[KEY_REQUIRED] as JsonArray
        assertThat(requiredArr.map { it.jsonPrimitive.content }).containsExactly(TEST_PROPERTY_1)
    }

    @Test
    fun toJsonSchema_withLocalCompositeDefinitionInFunctions_populatesDefs() {
        val testLocalComposite =
            object : A2uiCompositeSchema() {
                override val definitionName: String = TEST_DEF_NAME_1
                override val description: String? = null

                override fun getDefinition(): A2uiSchema =
                    A2uiObjectSchema(
                        properties = mapOf(TEST_PROPERTY_1 to A2uiStringSchema()),
                        required = setOf(TEST_PROPERTY_1),
                    )
            }

        val testFunction =
            object : A2uiFunction {
                override val definition: A2uiFunctionDefinition =
                    object : A2uiFunctionDefinition {
                        override val name: String = TEST_FUNCTION_NAME
                        override val description: String = TEST_FUNCTION_DESCRIPTION
                        override val argumentSchema: A2uiSchema = testLocalComposite
                        override val returnType: A2uiFunctionReturnType =
                            A2uiFunctionReturnType.BOOLEAN
                    }

                override fun execute(
                    args: Map<String, Any>,
                    executionContext: A2uiExecutionContext,
                ): Any? = true
            }

        val catalog = createCatalog(functions = listOf(testFunction))

        val root = parseCatalogJsonSchema(catalog)
        val defs = root[KEY_DEFS] as JsonObject

        val customDefObj = defs[TEST_DEF_NAME_1] as JsonObject
        assertThat(customDefObj[KEY_TYPE]?.jsonPrimitive?.content).isEqualTo(TYPE_OBJECT)

        val props = customDefObj[KEY_PROPERTIES] as JsonObject
        val propObj = props[TEST_PROPERTY_1] as JsonObject
        assertThat(propObj[KEY_TYPE]?.jsonPrimitive?.content).isEqualTo(TYPE_STRING)

        val requiredArr = customDefObj[KEY_REQUIRED] as JsonArray
        assertThat(requiredArr.map { it.jsonPrimitive.content }).containsExactly(TEST_PROPERTY_1)
    }

    @Test
    fun toJsonSchema_withThemeSchema_populatesDefs() {
        val themeSchema =
            A2uiObjectSchema(
                properties = mapOf(TEST_PROPERTY_1 to A2uiStringSchema()),
                required = setOf(TEST_PROPERTY_1),
            )
        val catalog = createCatalog(themeSchema = themeSchema)

        val root = parseCatalogJsonSchema(catalog)
        val defs = root[KEY_DEFS] as JsonObject

        val themeObj = defs[KEY_THEME] as JsonObject
        assertThat(themeObj[KEY_TYPE]?.jsonPrimitive?.content).isEqualTo(TYPE_OBJECT)

        val props = themeObj[KEY_PROPERTIES] as JsonObject
        val propObj = props[TEST_PROPERTY_1] as JsonObject
        assertThat(propObj[KEY_TYPE]?.jsonPrimitive?.content).isEqualTo(TYPE_STRING)

        val requiredArr = themeObj[KEY_REQUIRED] as JsonArray
        assertThat(requiredArr.map { it.jsonPrimitive.content }).containsExactly(TEST_PROPERTY_1)
    }

    @Test
    fun toJsonSchema_withLocalCompositeDefinitionsFromComponentsFunctionsAndTheme_populatesDefs() {
        val compLocalComposite =
            object : A2uiCompositeSchema() {
                override val definitionName: String = TEST_DEF_NAME_1
                override val description: String? = null

                override fun getDefinition(): A2uiSchema =
                    A2uiObjectSchema(properties = mapOf(TEST_PROPERTY_1 to A2uiStringSchema()))
            }

        val funcLocalComposite =
            object : A2uiCompositeSchema() {
                override val definitionName: String = TEST_DEF_NAME_2
                override val description: String? = null

                override fun getDefinition(): A2uiSchema =
                    A2uiObjectSchema(properties = mapOf(TEST_PROPERTY_1 to A2uiStringSchema()))
            }

        val themeLocalComposite =
            object : A2uiCompositeSchema() {
                override val definitionName: String = TEST_DEF_NAME_3
                override val description: String? = null

                override fun getDefinition(): A2uiSchema =
                    A2uiObjectSchema(properties = mapOf(TEST_PROPERTY_1 to A2uiStringSchema()))
            }

        val testComponent =
            object : A2uiCoreComponentDefinition {
                override val name: String = TEST_COMPONENT_NAME_2
                override val description: String = TEST_COMPONENT_DESCRIPTION_2
                override val propertySchema: A2uiSchema =
                    A2uiObjectSchema(properties = mapOf(TEST_PROPERTY_2 to compLocalComposite))
            }

        val testFunction =
            object : A2uiFunction {
                override val definition: A2uiFunctionDefinition =
                    object : A2uiFunctionDefinition {
                        override val name: String = TEST_FUNCTION_NAME
                        override val description: String = TEST_FUNCTION_DESCRIPTION
                        override val argumentSchema: A2uiSchema = funcLocalComposite
                        override val returnType: A2uiFunctionReturnType =
                            A2uiFunctionReturnType.BOOLEAN
                    }

                override fun execute(
                    args: Map<String, Any>,
                    executionContext: A2uiExecutionContext,
                ): Any? = true
            }

        val catalog =
            createCatalog(
                components = listOf(testComponent),
                functions = listOf(testFunction),
                themeSchema = themeLocalComposite,
            )

        val root = parseCatalogJsonSchema(catalog)
        val defs = root[KEY_DEFS] as JsonObject

        assertThat(defs).containsKey(TEST_DEF_NAME_1)
        assertThat(defs).containsKey(TEST_DEF_NAME_2)
        assertThat(defs).containsKey(TEST_DEF_NAME_3)
    }

    @Test
    fun toJsonSchema_withExternalCompositeDefinitions_doesNotPopulateDefs() {
        val testComponent =
            object : A2uiCoreComponentDefinition {
                override val name: String = TEST_COMPONENT_NAME_2
                override val description: String = TEST_COMPONENT_DESCRIPTION_2
                override val propertySchema: A2uiSchema =
                    A2uiObjectSchema(
                        properties = mapOf("common" to A2uiComponentCommonSchema.DEFAULT_INSTANCE)
                    )
            }

        val catalog = createCatalog(components = listOf(testComponent))

        val root = parseCatalogJsonSchema(catalog)
        val defs = root[KEY_DEFS] as JsonObject

        assertThat(defs).doesNotContainKey("ComponentCommon")
    }

    @Test
    fun toJsonSchema_withNestedCompositeDefinitions_collectsBothParentAndChildDefsInDefs() {
        val innerComposite =
            object : A2uiCompositeSchema() {
                override val definitionName: String = TEST_DEF_NAME_1
                override val description: String? = null

                override fun getDefinition(): A2uiSchema =
                    A2uiObjectSchema(properties = mapOf(TEST_PROPERTY_1 to A2uiStringSchema()))
            }

        val outerComposite =
            object : A2uiCompositeSchema() {
                override val definitionName: String = TEST_DEF_NAME_2
                override val description: String? = null

                override fun getDefinition(): A2uiSchema =
                    A2uiObjectSchema(properties = mapOf(TEST_PROPERTY_2 to innerComposite))
            }

        val testComponent =
            object : A2uiCoreComponentDefinition {
                override val name: String = TEST_COMPONENT_NAME_2
                override val description: String = TEST_COMPONENT_DESCRIPTION_2
                override val propertySchema: A2uiSchema =
                    A2uiObjectSchema(properties = mapOf(TEST_PROPERTY_3 to outerComposite))
            }

        val catalog = createCatalog(components = listOf(testComponent))

        val root = parseCatalogJsonSchema(catalog)
        val defs = root[KEY_DEFS] as JsonObject

        assertThat(defs).containsKey(TEST_DEF_NAME_2)
        assertThat(defs).containsKey(TEST_DEF_NAME_1)
    }

    private fun createCatalog(
        id: String = TEST_CATALOG_ID_1,
        title: String? = null,
        description: String? = null,
        components: List<A2uiCoreComponentDefinition> = emptyList(),
        functions: List<A2uiFunction> = emptyList(),
        themeSchema: A2uiSchema? = null,
    ): A2uiCoreCatalog =
        object : A2uiCoreCatalog {
            override val id: String = id
            override val title: String? = title
            override val description: String? = description
            override val componentDefinitions: A2uiCoreComponentDefinitionCollection =
                A2uiCoreComponentDefinitionCollection(components)
            override val functions: A2uiFunctionCollection = A2uiFunctionCollection(functions)
            override val themeSchema: A2uiSchema? = themeSchema
        }

    private fun createTestComponent(
        name: String,
        description: String,
    ): A2uiCoreComponentDefinition =
        object : A2uiCoreComponentDefinition {
            override val name: String = name
            override val description: String = description
            override val propertySchema: A2uiSchema =
                A2uiObjectSchema(
                    properties = mapOf(TEST_PROPERTY_1 to A2uiStringSchema()),
                    required = setOf(TEST_PROPERTY_1),
                )
        }

    private fun createTestFunction(name: String, description: String): A2uiFunction =
        object : A2uiFunction {
            override val definition: A2uiFunctionDefinition =
                object : A2uiFunctionDefinition {
                    override val name: String = name
                    override val description: String = description
                    override val argumentSchema: A2uiSchema = A2uiStringSchema()
                    override val returnType: A2uiFunctionReturnType = A2uiFunctionReturnType.BOOLEAN
                }

            override fun execute(
                args: Map<String, Any>,
                executionContext: A2uiExecutionContext,
            ): Any? = true
        }

    private fun parseCatalogJsonSchema(catalog: A2uiCoreCatalog): JsonObject =
        Json.parseToJsonElement(catalog.toJsonSchema()) as JsonObject

    companion object {
        private const val KEY_SCHEMA = "\$schema"
        private const val KEY_ID = "\$id"
        private const val KEY_CATALOG_ID = "catalogId"
        private const val KEY_TITLE = "title"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_COMPONENTS = "components"
        private const val KEY_FUNCTIONS = "functions"
        private const val KEY_DEFS = "\$defs"
        private const val KEY_ANY_COMPONENT = "anyComponent"
        private const val KEY_ANY_FUNCTION = "anyFunction"
        private const val KEY_ONE_OF = "oneOf"
        private const val KEY_REF = "\$ref"
        private const val KEY_PROPERTIES = "properties"
        private const val KEY_DISCRIMINATOR = "discriminator"
        private const val KEY_PROPERTY_NAME = "propertyName"
        private const val KEY_COMPONENT = "component"
        private const val KEY_THEME = "theme"
        private const val KEY_TYPE = "type"
        private const val KEY_REQUIRED = "required"

        private const val TYPE_OBJECT = "object"
        private const val TYPE_STRING = "string"

        private const val TEST_CATALOG_ID_1 = "https://a2ui.org/test/catalog/v1.0"
        private const val TEST_CATALOG_ID_2 = "https://a2ui.org/test/catalog/v2.0"
        private const val TEST_CATALOG_TITLE = "Test Catalog"
        private const val TEST_CATALOG_DESCRIPTION = "Catalog for testing"

        private const val TEST_COMPONENT_NAME_1 = "TestComponent1"
        private const val TEST_COMPONENT_DESCRIPTION_1 = "Test component description 1"
        private const val TEST_COMPONENT_NAME_2 = "TestComponent2"
        private const val TEST_COMPONENT_DESCRIPTION_2 = "Test component description 2"

        private const val TEST_FUNCTION_NAME = "testFunction"
        private const val TEST_FUNCTION_DESCRIPTION = "Test function description"

        private const val TEST_PROPERTY_1 = "testProperty1"
        private const val TEST_PROPERTY_2 = "testProperty2"
        private const val TEST_PROPERTY_3 = "testProperty3"

        private const val TEST_DEF_NAME_1 = "TestDef1"
        private const val TEST_DEF_NAME_2 = "TestDef2"
        private const val TEST_DEF_NAME_3 = "TestDef3"

        private const val JSON_SCHEMA_DRAFT = "https://json-schema.org/draft/2020-12/schema"
    }
}
