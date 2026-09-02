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
import androidx.a2ui.compose.runtime.A2uiComponentReference
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.engine.catalog.A2uiCoreCatalog
import androidx.a2ui.engine.catalog.toInlineCatalog
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
                isInline = false,
            )

        assertThat(catalog.id).isEqualTo(TestCatalogId)
        assertThat(catalog.components).containsExactly(component1, component2)
        assertThat(catalog.functions).containsExactly(function1, function2)
        assertThat(catalog.themeSchema).isEqualTo(themeSchema)
        assertThat(catalog.isInline).isFalse()
    }

    @Test
    fun factory_isInlineTrue_createsInlineCatalog() {
        val catalog =
            A2uiCatalog(catalogId = "inline_catalog", components = emptyList(), isInline = true)

        assertThat(catalog.isInline).isTrue()
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
        assertThat(catalog.isInline).isFalse()
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
        val testFunction = StubFunction("TestFunc")
        val basicCatalog = createTestBasicCatalog(functions = listOf(testFunction))

        val catalog = A2uiCatalog(basicCatalog)

        assertThat(catalog.id).isEqualTo(A2uiBasicCatalogV1.CatalogId)
        assertThat(catalog.themeSchema).isEqualTo(A2uiBasicCatalogV1.ThemeSchema)
        assertThat(catalog.components["Text"]).isSameInstanceAs(basicCatalog.text)
        assertThat(catalog.components["Image"]).isSameInstanceAs(basicCatalog.image)
        assertThat(catalog.components["Icon"]).isSameInstanceAs(basicCatalog.icon)
        assertThat(catalog.components["Video"]).isSameInstanceAs(basicCatalog.video)
        assertThat(catalog.components["AudioPlayer"]).isSameInstanceAs(basicCatalog.audioPlayer)
        assertThat(catalog.components["Card"]).isSameInstanceAs(basicCatalog.card)
        assertThat(catalog.components["Row"]).isSameInstanceAs(basicCatalog.row)
        assertThat(catalog.components["Column"]).isSameInstanceAs(basicCatalog.column)
        assertThat(catalog.components["List"]).isSameInstanceAs(basicCatalog.list)
        assertThat(catalog.components["Tabs"]).isSameInstanceAs(basicCatalog.tabs)
        assertThat(catalog.components["Divider"]).isSameInstanceAs(basicCatalog.divider)
        assertThat(catalog.components["Button"]).isSameInstanceAs(basicCatalog.button)
        assertThat(catalog.components["CheckBox"]).isSameInstanceAs(basicCatalog.checkBox)
        assertThat(catalog.components["Slider"]).isSameInstanceAs(basicCatalog.slider)
        assertThat(catalog.components["DateTimeInput"]).isSameInstanceAs(basicCatalog.dateTimeInput)
        assertThat(catalog.functions["TestFunc"]).isSameInstanceAs(testFunction)
        assertThat(catalog.isInline).isFalse()
    }

    @Test
    fun factory_fromBasicCatalog_withIsInlineTrue_createsInlineCatalog() {
        val basicCatalog = createTestBasicCatalog()

        val catalog = A2uiCatalog(basicCatalog, isInline = true)

        assertThat(catalog.id).isEqualTo(A2uiBasicCatalogV1.CatalogId)
        assertThat(catalog.isInline).isTrue()
    }

    @Test
    fun toJsonSchemaString_returnsSerializedJsonSchemaString() {
        val catalog =
            A2uiCatalog(catalogId = TestCatalogId, components = listOf(StubComponent("Component1")))

        val jsonString = catalog.toJsonSchemaString()

        assertThat(jsonString).contains("\"catalogId\":\"$TestCatalogId\"")
        assertThat(jsonString).contains("\"Component1\"")
    }

    @Test
    fun toJsonSchemaMap_returnsSerializedJsonSchemaMap() {
        val catalog =
            A2uiCatalog(catalogId = TestCatalogId, components = listOf(StubComponent("Component1")))

        val schemaMap = catalog.toJsonSchemaMap()

        assertThat(schemaMap["catalogId"]).isEqualTo(TestCatalogId)
        @Suppress("UNCHECKED_CAST") val components = schemaMap["components"] as Map<String, Any?>
        assertThat(components).containsKey("Component1")
    }

    @Test
    fun toInlineCatalog_sharesSameInstanceAndCache() {
        val catalog =
            A2uiCatalog(catalogId = "inline_catalog", components = emptyList(), isInline = true)
        val coreCatalog = catalog as A2uiCoreCatalog

        val inlineCatalog = coreCatalog.toInlineCatalog()

        assertThat(inlineCatalog).isSameInstanceAs(catalog)
        assertThat(inlineCatalog.toJsonSchemaMap()).isSameInstanceAs(catalog.toJsonSchemaMap())
        assertThat(inlineCatalog.toJsonSchemaString())
            .isSameInstanceAs(catalog.toJsonSchemaString())
    }

    @Test
    fun toJsonSchema_caching_returnsSameInstancesOnSubsequentCalls() {
        val prop = A2uiProperty.string("stringProp", required = true)
        val testComponent =
            object : A2uiComponent {
                override val name = "TestComponent"
                override val description = "Test description"
                override val properties = listOf(prop)

                @Composable
                override fun A2uiComponentScope.Content(
                    properties: A2uiComponentProperties,
                    modifier: Modifier,
                ) {}
            }
        val catalog = A2uiCatalog(catalogId = TestCatalogId, components = listOf(testComponent))

        val map1 = catalog.toJsonSchemaMap()
        val str1 = catalog.toJsonSchemaString()

        val map2 = catalog.toJsonSchemaMap()
        val str2 = catalog.toJsonSchemaString()

        assertThat(map1).isSameInstanceAs(map2)
        assertThat(str1).isSameInstanceAs(str2)
        assertThat(map1["components"]).isNotNull()
        assertThat(str1).contains("TestComponent")
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

    private companion object {
        fun createTestBasicCatalog(
            text: A2uiBasicCatalogV1.Text = createStubText(),
            image: A2uiBasicCatalogV1.Image = createStubImage(),
            icon: A2uiBasicCatalogV1.Icon = createStubIcon(),
            video: A2uiBasicCatalogV1.Video = createStubVideo(),
            audioPlayer: A2uiBasicCatalogV1.AudioPlayer = createStubAudioPlayer(),
            card: A2uiBasicCatalogV1.Card = createStubCard(),
            row: A2uiBasicCatalogV1.Row = createStubRow(),
            column: A2uiBasicCatalogV1.Column = createStubColumn(),
            list: A2uiBasicCatalogV1.List = createStubList(),
            tabs: A2uiBasicCatalogV1.Tabs = createStubTabs(),
            divider: A2uiBasicCatalogV1.Divider = createStubDivider(),
            button: A2uiBasicCatalogV1.Button = createStubButton(),
            checkBox: A2uiBasicCatalogV1.CheckBox = createStubCheckBox(),
            slider: A2uiBasicCatalogV1.Slider = createStubSlider(),
            dateTimeInput: A2uiBasicCatalogV1.DateTimeInput = createStubDateTimeInput(),
            functions: List<A2uiFunction> = emptyList(),
        ) =
            A2uiBasicCatalogV1(
                text = text,
                image = image,
                icon = icon,
                video = video,
                audioPlayer = audioPlayer,
                card = card,
                row = row,
                column = column,
                list = list,
                tabs = tabs,
                divider = divider,
                button = button,
                checkBox = checkBox,
                slider = slider,
                dateTimeInput = dateTimeInput,
                functions = functions,
            )

        fun createStubText() =
            object : A2uiBasicCatalogV1.Text {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    text: String,
                    variant: A2uiBasicCatalogV1.Text.Variant,
                    modifier: Modifier,
                ) {}
            }

        fun createStubImage() =
            object : A2uiBasicCatalogV1.Image {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    url: String,
                    description: String?,
                    fit: A2uiBasicCatalogV1.Image.Fit,
                    variant: A2uiBasicCatalogV1.Image.Variant,
                    modifier: Modifier,
                ) {}
            }

        fun createStubIcon() =
            object : A2uiBasicCatalogV1.Icon {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    source: A2uiBasicCatalogV1.Icon.Source,
                    accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
                    modifier: Modifier,
                ) {}
            }

        fun createStubVideo() =
            object : A2uiBasicCatalogV1.Video {
                @Composable
                override fun A2uiComponentScope.TypedContent(url: String, modifier: Modifier) {}
            }

        fun createStubAudioPlayer() =
            object : A2uiBasicCatalogV1.AudioPlayer {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    url: String,
                    description: String?,
                    modifier: Modifier,
                ) {}
            }

        fun createStubCard() =
            object : A2uiBasicCatalogV1.Card {
                @Composable
                override fun A2uiComponentScope.TypedContent(childId: String, modifier: Modifier) {}
            }

        fun createStubRow() =
            object : A2uiBasicCatalogV1.Row {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    children: List<A2uiComponentReference>,
                    justify: A2uiBasicCatalogV1.Row.Justify,
                    align: A2uiBasicCatalogV1.Row.Align,
                    modifier: Modifier,
                ) {}
            }

        fun createStubColumn() =
            object : A2uiBasicCatalogV1.Column {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    children: List<A2uiComponentReference>,
                    justify: A2uiBasicCatalogV1.Column.Justify,
                    align: A2uiBasicCatalogV1.Column.Align,
                    modifier: Modifier,
                ) {}
            }

        fun createStubList() =
            object : A2uiBasicCatalogV1.List {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    children: List<A2uiComponentReference>,
                    direction: A2uiBasicCatalogV1.List.Direction,
                    align: A2uiBasicCatalogV1.List.Align,
                    modifier: Modifier,
                ) {}
            }

        fun createStubTabs() =
            object : A2uiBasicCatalogV1.Tabs {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    tabs: List<A2uiBasicCatalogV1.Tabs.Tab>,
                    modifier: Modifier,
                ) {}
            }

        fun createStubDivider() =
            object : A2uiBasicCatalogV1.Divider {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    axis: A2uiBasicCatalogV1.Divider.Axis,
                    modifier: Modifier,
                ) {}
            }

        fun createStubButton() =
            object : A2uiBasicCatalogV1.Button {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    childId: String,
                    variant: A2uiBasicCatalogV1.Button.Variant,
                    action: Map<String, Any?>,
                    modifier: Modifier,
                ) {}
            }

        fun createStubCheckBox() =
            object : A2uiBasicCatalogV1.CheckBox {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    label: String,
                    value: Boolean,
                    onValueChange: (Boolean) -> Unit,
                    enabled: Boolean,
                    modifier: Modifier,
                ) {}
            }

        fun createStubSlider() =
            object : A2uiBasicCatalogV1.Slider {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    label: String?,
                    min: Float,
                    max: Float,
                    value: Float,
                    onValueChange: (Float) -> Unit,
                    enabled: Boolean,
                    modifier: Modifier,
                ) {}
            }

        fun createStubDateTimeInput() =
            object : A2uiBasicCatalogV1.DateTimeInput {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    value: Long?,
                    onValueChange: ((Long?) -> Unit)?,
                    enableDate: Boolean,
                    enableTime: Boolean,
                    min: Long?,
                    max: Long?,
                    label: String?,
                    modifier: Modifier,
                ) {}
            }
    }
}

private const val TestCatalogId = "test_catalog"
