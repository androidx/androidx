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

package androidx.a2ui.compose.ui.testing

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.catalog.A2uiFunctionDefinition
import androidx.a2ui.model.catalog.A2uiFunctionReturnType
import androidx.a2ui.model.protocol.A2uiExecutionContext
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiTestControllerImplTest {

    @Test
    fun init_duplicateIdStubs_throwsException() {
        val catalog = A2uiCatalog(catalogId = TestCatalogId, components = emptyList())
        val stub1 = A2uiComponentStub.withId("my_id") { _, _ -> }
        val stub2 = A2uiComponentStub.withId("my_id") { _, _ -> }

        val exception =
            assertFailsWith<IllegalArgumentException> {
                A2uiTestController(catalog = catalog, componentStubs = listOf(stub1, stub2))
            }

        assertThat(exception).hasMessageThat().contains("Duplicate stub defined for ID: 'my_id'")
    }

    @Test
    fun init_duplicateTypeStubs_throwsException() {
        val catalog = A2uiCatalog(catalogId = TestCatalogId, components = emptyList())
        val stub1 = A2uiComponentStub.withType("MyType") { _, _ -> }
        val stub2 = A2uiComponentStub.withType("MyType") { _, _ -> }

        val exception =
            assertFailsWith<IllegalArgumentException> {
                A2uiTestController(catalog = catalog, componentStubs = listOf(stub1, stub2))
            }

        assertThat(exception).hasMessageThat().contains("Duplicate stub defined for Type: 'MyType'")
    }

    @Test
    fun init_typeStub_replacesExistingComponent() {
        val originalComponent = TestStubComponent("MyType")
        val catalog = A2uiCatalog(catalogId = TestCatalogId, components = listOf(originalComponent))
        val stub = A2uiComponentStub.withType("MyType") { _, _ -> }

        val controller =
            A2uiTestController(catalog = catalog, componentStubs = listOf(stub))
                as A2uiTestControllerImpl
        val testCatalog = controller.testCatalog

        assertThat(testCatalog.components.size).isEqualTo(1)
        val replacedComponent = testCatalog.components["MyType"]
        assertThat(replacedComponent).isNotNull()
        assertThat(replacedComponent).isNotSameInstanceAs(originalComponent)
        assertThat(replacedComponent).isNotInstanceOf(TestStubComponent::class.java)
        assertThat(replacedComponent?.name).isEqualTo("MyType")
        assertThat(replacedComponent?.description).isEqualTo("Test Component")
    }

    @Test
    fun init_typeStub_addsNewComponent() {
        val catalog = A2uiCatalog(catalogId = TestCatalogId, components = emptyList())
        val stub = A2uiComponentStub.withType("NewType") { _, _ -> }

        val controller =
            A2uiTestController(catalog = catalog, componentStubs = listOf(stub))
                as A2uiTestControllerImpl
        val testCatalog = controller.testCatalog

        assertThat(testCatalog.components.size).isEqualTo(1)
        val newComponent = testCatalog.components["NewType"]
        assertThat(newComponent).isNotNull()
        assertThat(newComponent?.name).isEqualTo("NewType")
        assertThat(newComponent?.description).isEqualTo("Stub for type NewType")
    }

    @Test
    fun init_idStub_createsSyntheticType() {
        val catalog = A2uiCatalog(catalogId = TestCatalogId, components = emptyList())
        val stub = A2uiComponentStub.withId("my_id") { _, _ -> }

        val controller =
            A2uiTestController(catalog = catalog, componentStubs = listOf(stub))
                as A2uiTestControllerImpl
        val testCatalog = controller.testCatalog

        assertThat(controller.idStubs).containsKey("my_id")

        val syntheticType = controller.syntheticTypesById["my_id"]
        assertThat(syntheticType).isEqualTo("__stub_my_id")

        assertThat(testCatalog.components.size).isEqualTo(1)
        val syntheticComp = testCatalog.components["__stub_my_id"]
        assertThat(syntheticComp).isNotNull()
        assertThat(syntheticComp?.name).isEqualTo("__stub_my_id")
        assertThat(syntheticComp?.description).isEqualTo("Stub for ID my_id")
    }

    @Test
    fun init_copiesFunctionsAndThemeSchemaFromOriginalCatalog() {
        val function =
            object : A2uiFunction {
                override val definition =
                    object : A2uiFunctionDefinition {
                        override val name = "MyFunc"
                        override val description = ""
                        override val argumentSchema = A2uiObjectSchema.INSTANCE
                        override val returnType = A2uiFunctionReturnType.VOID
                    }

                override fun execute(
                    args: Map<String, Any>,
                    executionContext: A2uiExecutionContext,
                ): Any? = null
            }
        val themeSchema = A2uiObjectSchema.INSTANCE
        val catalog =
            A2uiCatalog(
                catalogId = TestCatalogId,
                components = emptyList(),
                functions = listOf(function),
                themeSchema = themeSchema,
            )

        val controller = A2uiTestController(catalog = catalog) as A2uiTestControllerImpl

        assertThat(controller.testCatalog.functions).containsExactly(function)
        assertThat(controller.testCatalog.themeSchema).isEqualTo(themeSchema)
    }

    private class TestStubComponent(
        override val name: String,
        override val description: String = "Test Component",
        override val properties: List<A2uiProperty<*>> = emptyList(),
    ) : A2uiComponent {
        @Composable
        override fun A2uiComponentScope.Content(
            properties: A2uiComponentProperties,
            modifier: Modifier,
        ) {}
    }
}

private const val TestCatalogId = "TestCatalog"
