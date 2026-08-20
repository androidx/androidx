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

package androidx.a2ui.compose.ui

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.engine.catalog.A2uiCoreCatalog
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.catalog.A2uiFunctionDefinition
import androidx.a2ui.model.catalog.A2uiFunctionReturnType
import androidx.a2ui.model.protocol.A2uiExecutionContext
import androidx.a2ui.model.schema.A2uiAnySchema
import androidx.a2ui.model.schema.A2uiNumberSchema
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiStringSchema
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiCatalogTest {

    @Test
    fun factory_setsPropertiesCorrectly() {
        val component1 = StubComponent("Component1")
        val component2 = StubComponent("Component2")
        val function1 = StubFunction("Function1")
        val function2 = StubFunction("Function2")
        val themeSchema = A2uiObjectSchema(description = "Test Theme Schema")

        val catalog =
            A2uiCatalog(
                catalogId = TestCatalogId,
                components = listOf(component1, component2),
                functions = listOf(function1, function2),
                themeSchema = themeSchema,
            )

        assertThat(catalog.id).isEqualTo(TestCatalogId)
        assertThat(catalog.components).containsExactly(component1, component2)
        assertThat(catalog.functions).containsExactly(function1, function2)
        assertThat(catalog.themeSchema).isEqualTo(themeSchema)
    }

    @Test
    fun factory_emptyLists_createsCatalogSuccessfully() {
        val catalog =
            A2uiCatalog(
                catalogId = "empty_catalog",
                components = emptyList(),
                functions = emptyList(),
                themeSchema = null,
            )

        assertThat(catalog.components).isEmpty()
        assertThat(catalog.functions).isEmpty()
        assertThat(catalog.themeSchema).isNull()
    }

    @Test
    fun factory_duplicateComponents_throws() {
        val component1 = StubComponent("DuplicateComponent")
        val component2 = StubComponent("DuplicateComponent")

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                A2uiCatalog(catalogId = TestCatalogId, components = listOf(component1, component2))
            }

        assertThat(exception)
            .hasMessageThat()
            .contains("Duplicate component registered for name 'DuplicateComponent'")
    }

    @Test
    fun factory_duplicateFunctions_throws() {
        val function1 = StubFunction("DuplicateFunction")
        val function2 = StubFunction("DuplicateFunction")

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                A2uiCatalog(
                    catalogId = TestCatalogId,
                    components = listOf(),
                    functions = listOf(function1, function2),
                )
            }

        assertThat(exception)
            .hasMessageThat()
            .contains("Duplicate function registered for name 'DuplicateFunction'")
    }

    @Test
    fun factory_fromBasicCatalog_createsCatalogSuccessfully() {
        val testText =
            object : A2uiBasicCatalogV1.Text {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    text: String,
                    variant: A2uiBasicCatalogV1.Text.Variant,
                    modifier: Modifier,
                ) {}
            }
        val testCard =
            object : A2uiBasicCatalogV1.Card {
                @Composable
                override fun A2uiComponentScope.TypedContent(childId: String, modifier: Modifier) {}
            }
        val testFunction = StubFunction("TestFunc")
        val basicCatalog =
            A2uiBasicCatalogV1(text = testText, card = testCard, functions = listOf(testFunction))

        val catalog = A2uiCatalog(basicCatalog)

        assertThat(catalog.id).isEqualTo(A2uiBasicCatalogV1.CatalogId)
        assertThat(catalog.themeSchema).isEqualTo(A2uiBasicCatalogV1.ThemeSchema)
        assertThat(catalog.components["Text"]).isSameInstanceAs(testText)
        assertThat(catalog.components["Card"]).isSameInstanceAs(testCard)
        assertThat(catalog.functions["TestFunc"]).isSameInstanceAs(testFunction)
    }

    @Test
    fun components_lookupByName_returnsCorrectComponent() {
        val component1 = StubComponent("Component1")
        val component2 = StubComponent("Component2")

        val catalog =
            A2uiCatalog(catalogId = TestCatalogId, components = listOf(component1, component2))

        assertThat(catalog.components["Component1"]).isEqualTo(component1)
        assertThat(catalog.components["Component2"]).isEqualTo(component2)
        assertThat(catalog.components["UnknownComp"]).isNull()
    }

    @Test
    fun functions_lookupByName_returnsCorrectFunction() {
        val function1 = StubFunction("Function1")
        val function2 = StubFunction("Function2")

        val catalog =
            A2uiCatalog(
                catalogId = TestCatalogId,
                components = listOf(),
                functions = listOf(function1, function2),
            )

        assertThat(catalog.functions["Function1"]).isEqualTo(function1)
        assertThat(catalog.functions["Function2"]).isEqualTo(function2)
        assertThat(catalog.functions["UnknownFunction"]).isNull()
    }

    @Test
    fun getComponentDefinition_returnsCorrectComponentDefinition() {
        val component1 = StubComponent(name = "Component1", description = "Description1")
        val component2 = StubComponent(name = "Component2", description = "Description2")

        val catalog =
            A2uiCatalog(catalogId = TestCatalogId, components = listOf(component1, component2))
        val coreCatalog = catalog as A2uiCoreCatalog

        val definition1 = coreCatalog.componentDefinitions["Component1"]
        assertThat(definition1?.name).isEqualTo("Component1")
        assertThat(definition1?.description).isEqualTo("Description1")
        val definition2 = coreCatalog.componentDefinitions["Component2"]
        assertThat(definition2?.name).isEqualTo("Component2")
        assertThat(definition2?.description).isEqualTo("Description2")
        assertThat(coreCatalog.componentDefinitions["UnknownComponent"]).isNull()
    }

    @Test
    fun getComponentDefinition_generatesCorrectPropertySchemaForComponent() {
        val prop1 = A2uiProperty.string("stringProp", required = true)
        val prop2 = A2uiProperty.number("numberProp", required = false)
        val component =
            object : A2uiComponent {
                override val name = "TestComponent"
                override val description = "Test description"
                override val properties = listOf(prop1, prop2)

                @Composable
                override fun A2uiComponentScope.Content(
                    properties: A2uiComponentProperties,
                    modifier: Modifier,
                ) {}
            }

        val catalog = A2uiCatalog(TestCatalogId, listOf(component))
        val coreCatalog = catalog as A2uiCoreCatalog
        val definition = coreCatalog.componentDefinitions["TestComponent"]

        assertThat(definition).isNotNull()
        assertThat(definition?.name).isEqualTo("TestComponent")
        val schema = definition?.propertySchema as A2uiObjectSchema
        assertThat(schema.properties.keys).containsExactly("stringProp", "numberProp")
        assertThat(schema.properties["stringProp"]).isInstanceOf(A2uiStringSchema::class.java)
        assertThat(schema.properties["numberProp"]).isInstanceOf(A2uiNumberSchema::class.java)
        assertThat(schema.required).containsExactly("stringProp")
    }

    private class StubComponent(
        override val name: String,
        override val description: String = "Stub component $name",
        override val properties: List<A2uiProperty<*>> = emptyList(),
    ) : A2uiComponent {
        @Composable
        override fun A2uiComponentScope.Content(
            properties: A2uiComponentProperties,
            modifier: Modifier,
        ) {}
    }

    private class StubFunction(name: String) : A2uiFunction {
        override val definition =
            object : A2uiFunctionDefinition {
                override val name = name
                override val description = "Stub function $name"
                override val argumentSchema = A2uiAnySchema()
                override val returnType = A2uiFunctionReturnType.ANY
            }

        override fun execute(args: Map<String, Any>, executionContext: A2uiExecutionContext): Any? =
            null
    }
}

private const val TestCatalogId = "test_catalog"
