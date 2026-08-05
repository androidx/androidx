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
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.runtime.LocalA2uiReadinessEvaluator
import androidx.a2ui.compose.runtime.observeA2uiComponentState
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.asReadinessEvaluator
import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.catalog.A2uiFunctionDefinition
import androidx.a2ui.model.catalog.A2uiFunctionReturnType
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiEventAction
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.a2ui.model.protocol.A2uiException.A2uiValidationException
import androidx.a2ui.model.protocol.A2uiExecutionContext
import androidx.a2ui.model.protocol.A2uiFunctionCallAction
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
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
    fun init_typeStub_defaultsToInstantReadiness() = runComposeUiTest {
        val originalComp =
            object : A2uiComponent {
                override val name = "SlowComponent"
                override val description = "Desc"
                override val properties = emptyList<A2uiProperty<*>>()

                @Composable
                override fun A2uiComponentScope.isReady(
                    properties: A2uiComponentProperties
                ): Boolean = false

                @Composable
                override fun A2uiComponentScope.Content(
                    properties: A2uiComponentProperties,
                    modifier: Modifier,
                ) {}
            }

        val catalog = A2uiCatalog(catalogId = TestCatalogId, components = listOf(originalComp))
        val stub =
            A2uiComponentStub.withType("SlowComponent") { _, modifier ->
                BasicText("Loaded", modifier)
            }

        val controller =
            A2uiTestController(
                catalog = catalog,
                initialComponents =
                    listOf(A2uiComponentPayload("root", "SlowComponent", emptyMap())),
                componentStubs = listOf(stub),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Loaded").assertIsDisplayed()
    }

    @Test
    fun init_typeStub_usesStubIsReady() = runComposeUiTest {
        val dynamicProp = A2uiProperty.dynamicString("url")
        val originalComp =
            object : A2uiComponent {
                override val name = "SlowComponent"
                override val description = "Desc"
                override val properties = listOf(dynamicProp)

                @Composable
                override fun A2uiComponentScope.Content(
                    properties: A2uiComponentProperties,
                    modifier: Modifier,
                ) {}
            }

        val catalog = A2uiCatalog(catalogId = TestCatalogId, components = listOf(originalComp))
        val stub =
            A2uiComponentStub.withType(
                type = "SlowComponent",
                isReady = { props -> props.bind(dynamicProp) != null },
            ) { _, modifier ->
                BasicText("Loaded", modifier)
            }

        val controller =
            A2uiTestController(
                catalog = catalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            "root",
                            "SlowComponent",
                            mapOf("url" to mapOf("path" to "/url")),
                        )
                    ),
                componentStubs = listOf(stub),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        controller.waitForIdle()
        onNodeWithText("Loaded").assertDoesNotExist()

        controller.updateData("/url", "http://example.com")
        controller.waitForIdle()

        onNodeWithText("Loaded").assertIsDisplayed()
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
    fun init_idStub_defaultsToInstantReadiness() = runComposeUiTest {
        val catalog = A2uiCatalog(catalogId = TestCatalogId, components = emptyList())
        val stub =
            A2uiComponentStub.withId("root") { _, modifier -> BasicText("IdLoaded", modifier) }

        val controller =
            A2uiTestController(
                catalog = catalog,
                initialComponents = listOf(A2uiComponentPayload("root", emptyMap())),
                componentStubs = listOf(stub),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("IdLoaded").assertIsDisplayed()
    }

    @Test
    fun init_idStub_usesStubIsReady() = runComposeUiTest {
        val dynamicProp = A2uiProperty.dynamicString("url")
        val catalog = A2uiCatalog(catalogId = TestCatalogId, components = emptyList())
        val stub =
            A2uiComponentStub.withId(
                id = "root",
                isReady = { props -> props.bind(dynamicProp) != null },
            ) { _, modifier ->
                BasicText("IdLoaded", modifier)
            }

        val controller =
            A2uiTestController(
                catalog = catalog,
                initialComponents =
                    listOf(A2uiComponentPayload("root", mapOf("url" to mapOf("path" to "/url")))),
                componentStubs = listOf(stub),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        controller.waitForIdle()
        onNodeWithText("IdLoaded").assertDoesNotExist()

        controller.updateData("/url", "http://example.com")
        controller.waitForIdle()

        onNodeWithText("IdLoaded").assertIsDisplayed()
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

    @Test
    fun surface_accessedBeforeStart_throwsException() = runComposeUiTest {
        val controller = A2uiTestController(catalog = testCatalog())

        val exception = assertFailsWith<IllegalStateException> { controller.surface }

        assertThat(exception.message).contains("Surface not created")
    }

    @Test
    fun start_initializesCorrectly() = runComposeUiTest {
        val controller = A2uiTestController(catalog = testCatalog())

        val surface = controller.start()

        assertThat(surface).isNotNull()
        assertThat(surface.id).isEqualTo(TestSurfaceId)
        assertThat(controller.surface).isSameInstanceAs(surface)
    }

    @Test
    fun start_withInitialData_populatesDataModel() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog(),
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            "root",
                            "TestComponent",
                            mapOf("text" to mapOf("path" to "/greeting")),
                        )
                    ),
                initialData = mapOf("greeting" to "Hello from DataModel"),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Hello from DataModel").assertIsDisplayed()
    }

    @Test
    fun start_withStubAndInitialComponent_usesInitialProperties() = runComposeUiTest {
        val textProp = A2uiProperty.string("label", required = true)

        // Define a stub for "root"
        val stub =
            A2uiComponentStub.withId(id = "root") { props, modifier ->
                val label = props[textProp] ?: "null"

                BasicText(text = "Stub: $label", modifier = modifier)
            }

        // Define an initial payload for the exact same ID "root".
        // It provides the real property "label" needed to pass validation.
        val initialPayload =
            A2uiComponentPayload(id = "root", properties = mapOf("label" to "Initial Label"))

        val controller =
            A2uiTestController(
                catalog = testCatalog(),
                initialComponents = listOf(initialPayload),
                componentStubs = listOf(stub),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        waitForIdle()

        onNodeWithText("Stub: Initial Label").assertIsDisplayed()
    }

    @Test
    fun start_idStubWithoutInitialComponent_defaultsToEmptyMap() = runComposeUiTest {
        val stub =
            A2uiComponentStub.withId("root") { _, modifier ->
                BasicText("Stub with default empty map", modifier)
            }

        val controller =
            A2uiTestController(
                catalog = A2uiCatalog(catalogId = TestCatalogId, components = emptyList()),
                componentStubs = listOf(stub),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Stub with default empty map").assertIsDisplayed()
    }

    @Test
    fun start_rootComponentWithStub_rendersStubbedContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog(),
                initialComponents = listOf(A2uiComponentPayload("root", mapOf("k" to "v"))),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("root") { props, modifier ->
                            BasicText("Stubbed: ${props[A2uiProperty.string("k")]}", modifier)
                        }
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Stubbed: v").assertIsDisplayed()
    }

    @Test
    fun start_unknownRootComponentType_throwsException() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog(),
                initialComponents = listOf(A2uiComponentPayload("root", "UnknownType", emptyMap())),
            )

        val exception = assertFailsWith<A2uiRuntimeException> { controller.start() }

        assertThat(exception.message)
            .contains("Component type 'UnknownType' is not registered in the test catalog.")
    }

    @Test
    fun start_withInvalidPayload_failsFastSynchronously() = runComposeUiTest {
        val labelProp = A2uiProperty.string("label", required = true)
        val requiredComp =
            object : A2uiComponent {
                override val name = "TestComponent"
                override val description = "Desc"
                override val properties = listOf(labelProp)

                @Composable
                override fun A2uiComponentScope.Content(
                    properties: A2uiComponentProperties,
                    modifier: Modifier,
                ) {}
            }
        val catalog = A2uiCatalog(TestCatalogId, listOf(requiredComp))

        // "label" is required, but we provide an empty map.
        val controller =
            A2uiTestController(
                catalog = catalog,
                initialComponents =
                    listOf(A2uiComponentPayload("root", "TestComponent", emptyMap())),
            )

        val exception = assertFailsWith<A2uiValidationException> { controller.start() }

        assertThat(exception.message).contains("Missing required property 'label'")
        assertThat(exception.context["path"]).isEqualTo("/components/root")
    }

    @Test
    fun start_capturesImmediateOutboundEvents() = runComposeUiTest {
        val controller = A2uiTestController(testCatalog())

        controller.start()

        controller.failComponent("root", A2uiRuntimeException("Immediate async error"))
        controller.waitForIdle()

        assertThat(controller.outboundErrors).hasSize(1)
        assertThat(controller.outboundErrors.first().message).isEqualTo("Immediate async error")
    }

    @Test
    fun start_multipleCalls_isIdempotent() = runComposeUiTest {
        val controller = A2uiTestController(testCatalog())

        controller.start()
        controller.start()
        controller.start()

        controller.failComponent("root", A2uiRuntimeException("Test error"))
        controller.waitForIdle()

        // If multiple collectors were spun up, the error would be added to the list multiple times.
        assertThat(controller.outboundErrors).hasSize(1)
    }

    @Test
    fun updateData_updatesDataModelAndRecomposes() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog(),
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            "root",
                            "TestComponent",
                            mapOf("text" to mapOf("path" to "/greeting")),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        // Initially data path is missing, should default to "default" fallback in TestComponent
        onNodeWithText("default").assertIsDisplayed()

        controller.updateData("/greeting", "Updated Greeting")
        controller.waitForIdle()

        onNodeWithText("Updated Greeting").assertIsDisplayed()
    }

    @Test
    fun getRawData_existingPath_returnsValue() = runComposeUiTest {
        val controller = A2uiTestController(catalog = testCatalog())
        controller.start()

        controller.updateData("/string_val", "Hello")
        controller.waitForIdle()

        assertThat(controller.getRawData("/string_val")).isEqualTo("Hello")
    }

    @Test
    fun getRawData_nonExistentPath_returnsNull() = runComposeUiTest {
        val controller = A2uiTestController(catalog = testCatalog())
        controller.start()

        assertThat(controller.getRawData("/non_existent")).isNull()
    }

    @Test
    fun getData_returnsTypedData() = runComposeUiTest {
        val controller = A2uiTestController(catalog = testCatalog())
        controller.start()

        controller.updateData("/string_val", "Hello")
        controller.updateData("/int_val", 42)
        controller.waitForIdle()

        assertThat(controller.getData<String>("/string_val")).isEqualTo("Hello")
        assertThat(controller.getData<Int>("/int_val")).isEqualTo(42)
    }

    @Test
    fun getData_nonExistentPath_returnsNull() = runComposeUiTest {
        val controller = A2uiTestController(catalog = testCatalog())
        controller.start()

        assertThat(controller.getData<Any>("/non_existent")).isNull()
    }

    @Test
    fun getData_wrongType_throwsClassCastException() = runComposeUiTest {
        val controller = A2uiTestController(catalog = testCatalog())
        controller.start()

        controller.updateData("/string_val", "Hello")
        controller.waitForIdle()

        val exception =
            assertFailsWith<ClassCastException> { controller.getData<Boolean>("/string_val") }
        assertThat(exception.message)
            .contains("Cannot cast value 'Hello' (String) at path '/string_val' to Boolean")
    }

    @Test
    fun updateComponent_knownType_updatesRegistry() = runComposeUiTest {
        val controller = A2uiTestController(catalog = testCatalog())
        val surface = controller.start()

        controller.updateComponent("root", "TestComponent", mapOf("text" to "Hello World"))
        controller.waitForIdle()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Hello World").assertIsDisplayed()
    }

    @Test
    fun updateComponent_beforeStart_queuesComponentUpdate() = runComposeUiTest {
        val controller = A2uiTestController(catalog = testCatalog())

        controller.updateComponent("root", "TestComponent", mapOf("text" to "Hello World"))
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Hello World").assertIsDisplayed()
    }

    @Test
    fun updateComponent_withoutType_knownType_updatesRegistry() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog(),
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TestComponent",
                            properties = mapOf("text" to "Initial Text"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }
        onNodeWithText("Initial Text").assertIsDisplayed()

        controller.updateComponent("root", mapOf("text" to "Updated Without Type"))
        controller.waitForIdle()

        onNodeWithText("Updated Without Type").assertIsDisplayed()
    }

    @Test
    fun updateComponent_withoutType_unknownId_throwsIllegalStateException() = runComposeUiTest {
        val controller = A2uiTestController(catalog = testCatalog())

        val exception =
            assertFailsWith<IllegalStateException> {
                controller.updateComponent("unknown_id", mapOf("text" to "Test"))
            }

        assertThat(exception.message)
            .contains("Cannot update component 'unknown_id': no type recorded.")
    }

    @Test
    fun updateComponent_stubbedId_withType_throwsIllegalArgumentException() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog(),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("root") { _, modifier ->
                            BasicText("Stubbed Root", modifier)
                        }
                    ),
            )

        val exception =
            assertFailsWith<IllegalArgumentException> {
                controller.updateComponent("root", "TestComponent", emptyMap())
            }

        assertThat(exception.message)
            .contains(
                "Component ID 'root' is registered as an ID stub. Do not specify a component type"
            )
    }

    @Test
    fun updateComponent_stubbedId_withoutType_updatesSuccessfully() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog(),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("root") { _, modifier ->
                            BasicText("Stubbed Root", modifier)
                        }
                    ),
            )
        val surface = controller.start()

        // Pushing properties without type for a stubbed ID successfully updates the ID stub
        controller.updateComponent("root", emptyMap())
        controller.waitForIdle()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Stubbed Root").assertIsDisplayed()
    }

    @Test
    fun initialComponents_idStubPayload_usesSyntheticType() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog(),
                initialComponents =
                    listOf(A2uiComponentPayload("root", mapOf("text" to "Hello Stub"))),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("root") { _, modifier ->
                            BasicText("Stubbed Root", modifier)
                        }
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Stubbed Root").assertIsDisplayed()
    }

    @Test
    fun initialComponents_idStubPayload_withType_throwsIllegalArgumentException() =
        runComposeUiTest {
            val controller =
                A2uiTestController(
                    catalog = testCatalog(),
                    initialComponents =
                        listOf(A2uiComponentPayload("root", "SomeType", mapOf("text" to "Test"))),
                    componentStubs =
                        listOf(
                            A2uiComponentStub.withId("root") { _, modifier ->
                                BasicText("Stubbed Root", modifier)
                            }
                        ),
                )

            val exception = assertFailsWith<IllegalArgumentException> { controller.start() }

            assertThat(exception.message)
                .contains(
                    "Component ID 'root' is registered as an ID stub. Do not specify a component type"
                )
        }

    @Test
    fun initialComponents_idStubPayload_unregisteredId_throwsIllegalArgumentException() =
        runComposeUiTest {
            val controller =
                A2uiTestController(
                    catalog = testCatalog(),
                    initialComponents =
                        listOf(A2uiComponentPayload("not_a_stub", mapOf("text" to "Test"))),
                )

            val exception = assertFailsWith<IllegalArgumentException> { controller.start() }

            assertThat(exception.message)
                .contains(
                    "A2uiComponentPayload(id, properties) without a type can only be used for ID stubs"
                )
        }

    @Test
    fun updateComponent_unknownType_throwsException() = runComposeUiTest {
        val controller = A2uiTestController(catalog = testCatalog())
        controller.start()

        val exception =
            assertFailsWith<A2uiRuntimeException> {
                controller.updateComponent("comp1", "UnknownType", emptyMap())
            }

        assertThat(exception.message).contains("Component type 'UnknownType' is not registered")
    }

    @Test
    fun updateComponent_withInvalidPayload_failsFastSynchronously() = runComposeUiTest {
        val controller = A2uiTestController(testCatalog())
        controller.start()

        // Provide wrong type for label (Int instead of String)
        val exception =
            assertFailsWith<A2uiValidationException> {
                controller.updateComponent("comp1", "TestComponent", mapOf("label" to 123))
            }

        assertThat(exception.message).contains("Expected a string")
        assertThat(exception.context["path"]).isEqualTo("/components/comp1/label")
    }

    @Test
    fun failComponent_reportsErrorToSurface() = runComposeUiTest {
        val controller = A2uiTestController(catalog = testCatalog())
        controller.start()

        val exception = A2uiValidationException("Mock Failure", "/")
        controller.failComponent("root", exception)
        controller.waitForIdle()

        val error = controller.outboundErrors.single()
        assertThat(error.message).isEqualTo("Mock Failure")
        assertThat(error.code).isEqualTo("VALIDATION_FAILED")
        assertThat(error.context["path"]).isEqualTo("/")
    }

    @Test
    fun dispatchAction_event_recordsDispatchedActionsAndOutboundEvents() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog(),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("root") { _, modifier ->
                            BasicText(
                                "Click Me",
                                modifier.clickable {
                                    dispatchAction(
                                        mapOf(
                                            "event" to
                                                mapOf(
                                                    "name" to "submit",
                                                    "context" to mapOf("k" to "v"),
                                                )
                                        )
                                    )
                                },
                            )
                        }
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Click Me").performClick()
        waitForIdle()
        controller.waitForIdle()

        assertThat(controller.dispatchedActions).hasSize(1)
        val action = controller.dispatchedActions.first() as A2uiEventAction
        assertThat(action.componentId).isEqualTo("root")
        assertThat(action.eventName).isEqualTo("submit")
        assertThat(action.context).isEqualTo(mapOf("k" to "v"))

        assertThat(controller.outboundEvents).hasSize(1)
        val event = controller.outboundEvents.first()
        assertThat(event.componentId).isEqualTo("root")
        assertThat(event.type).isEqualTo("submit")
        assertThat(event.context).isEqualTo(mapOf("k" to "v"))
    }

    @Test
    fun dispatchAction_functionCall_recordsDispatchedActionsButNoOutboundEvents() =
        runComposeUiTest {
            val controller =
                A2uiTestController(
                    catalog = testCatalog(),
                    componentStubs =
                        listOf(
                            A2uiComponentStub.withId("root") { _, modifier ->
                                BasicText(
                                    "Click Me",
                                    modifier.clickable {
                                        dispatchAction(
                                            mapOf(
                                                "functionCall" to
                                                    mapOf(
                                                        "call" to "testFunction",
                                                        "args" to mapOf("a" to 1),
                                                    )
                                            )
                                        )
                                    },
                                )
                            }
                        ),
                )
            val surface = controller.start()

            setContent { A2uiTestSurface(surface) }

            onNodeWithText("Click Me").performClick()
            waitForIdle()
            controller.waitForIdle()

            assertThat(controller.dispatchedActions).hasSize(1)
            val action = controller.dispatchedActions.first() as A2uiFunctionCallAction
            assertThat(action.componentId).isEqualTo("root")
            assertThat(action.functionName).isEqualTo("testFunction")
            assertThat(action.args).isEqualTo(mapOf("a" to 1))

            // Local function calls do not emit outbound network events
            assertThat(controller.outboundEvents).isEmpty()
        }

    @Test
    fun clearDispatchedActionsEventsAndErrors_clearsCollections() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog(),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("root") { _, modifier ->
                            BasicText(
                                "Click Me",
                                modifier.clickable {
                                    dispatchAction(mapOf("event" to mapOf("name" to "submit")))
                                },
                            )
                        }
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Click Me").performClick()
        controller.failComponent("child", A2uiRuntimeException("Crash"))
        waitForIdle()
        controller.waitForIdle()

        assertThat(controller.dispatchedActions).isNotEmpty()
        assertThat(controller.outboundEvents).isNotEmpty()
        assertThat(controller.outboundErrors).isNotEmpty()

        controller.clearDispatchedActions()
        controller.clearOutboundEvents()
        controller.clearOutboundErrors()

        assertThat(controller.dispatchedActions).isEmpty()
        assertThat(controller.outboundEvents).isEmpty()
        assertThat(controller.outboundErrors).isEmpty()
    }

    private fun testCatalog(): A2uiCatalog {
        val testProp = A2uiProperty.dynamicString("text")
        val labelProp = A2uiProperty.string("label")
        val testComp =
            object : A2uiComponent {
                override val name = "TestComponent"
                override val description = "Desc"
                override val properties = listOf(testProp, labelProp)

                @Composable
                override fun A2uiComponentScope.Content(
                    properties: A2uiComponentProperties,
                    modifier: Modifier,
                ) {
                    val text = properties.bind(testProp) ?: "default"
                    BasicText(text, modifier)
                }
            }
        return A2uiCatalog(TestCatalogId, listOf(testComp))
    }

    private class TestStubComponent(override val name: String) : A2uiComponent {
        override val description = "Test Component"
        override val properties = emptyList<A2uiProperty<*>>()

        @Composable
        override fun A2uiComponentScope.Content(
            properties: A2uiComponentProperties,
            modifier: Modifier,
        ) {}
    }

    @Composable
    private fun A2uiTestSurface(surface: A2uiSurfaceModel) {
        surface as? A2uiCoreSurfaceModel
            ?: throw IllegalArgumentException("A2uiTestSurface requires an A2uiCoreSurfaceModel.")

        val composeCatalog =
            surface.catalog as? A2uiCatalog
                ?: throw IllegalArgumentException("Catalog must implement A2uiCatalog.")

        val readinessEvaluator = remember(composeCatalog) { composeCatalog.asReadinessEvaluator() }

        CompositionLocalProvider(LocalA2uiReadinessEvaluator provides readinessEvaluator) {
            when (val rootState = observeA2uiComponentState(surface = surface)) {
                is A2uiComponentState.Success -> {
                    A2uiComponent(component = rootState.component)
                }
                is A2uiComponentState.Error -> {
                    throw rootState.exception
                }
                is A2uiComponentState.Loading -> {}
            }
        }
    }
}

private const val TestCatalogId = "test_catalog"
