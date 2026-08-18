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

import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.engine.catalog.A2uiCoreCatalog
import androidx.a2ui.engine.catalog.A2uiCoreComponentDefinitionCollection
import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.engine.platform.A2uiCoreComponentRegistry
import androidx.a2ui.engine.platform.A2uiCoreDataModel
import androidx.a2ui.model.catalog.A2uiFunctionCollection
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.a2ui.model.protocol.A2uiException.A2uiValidationException
import androidx.a2ui.model.schema.A2uiSchema
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class A2uiTestSurfaceTest {

    @Test
    fun surface_nonCoreSurfaceModel_throwsIllegalArgumentException() = runComposeUiTest {
        val fakeSurface =
            object : A2uiSurfaceModel {
                override val id: String = "fake_surface"
            }

        val exception =
            assertFailsWith<IllegalArgumentException> {
                setContent { A2uiTestSurface(fakeSurface) }
            }

        assertThat(exception)
            .hasMessageThat()
            .contains("A2uiTestSurface requires an A2uiCoreSurfaceModel")
    }

    @Test
    fun surface_nonComposeCatalog_throwsIllegalArgumentException() = runComposeUiTest {
        val fakeCatalog =
            object : A2uiCoreCatalog {
                override val id: String = "fake_catalog"
                override val componentDefinitions = A2uiCoreComponentDefinitionCollection()
                override val functions = A2uiFunctionCollection()
                override val themeSchema: A2uiSchema? = null
            }
        val fakeDataModel =
            object : A2uiCoreDataModel {
                override fun update(path: A2uiDataPath, value: Any?) {}

                override fun get(path: A2uiDataPath): Any? = null

                override fun close() {}
            }
        val fakeComponentRegistry =
            object : A2uiCoreComponentRegistry {
                override fun update(components: List<A2uiComponentPayload>) {}

                override fun reportError(id: String, exception: A2uiException) {}

                override fun close() {}
            }
        val fakeCoreSurface =
            A2uiCoreSurfaceModel(
                id = "fake_surface",
                catalog = fakeCatalog,
                dataModel = fakeDataModel,
                componentRegistry = fakeComponentRegistry,
                onDispatchAction = {},
                onDispatchError = {},
            )

        val exception =
            assertFailsWith<IllegalArgumentException> {
                setContent { A2uiTestSurface(fakeCoreSurface) }
            }

        assertThat(exception).hasMessageThat().contains("Catalog must implement A2uiCatalog")
    }

    @Test
    fun successState_rendersComponentAndAppliesModifier() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = A2uiCatalog("test_catalog", emptyList()),
                initialComponents = listOf(A2uiComponentPayload("root")),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("root") { _, modifier ->
                            BasicText("I am root", modifier = modifier)
                        }
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface = surface, modifier = Modifier.testTag("root_tag")) }

        onNode(hasText("I am root") and hasTestTag("root_tag")).assertIsDisplayed()
    }

    @Test
    fun loadingState_default_rendersNothing() = runComposeUiTest {
        // Not providing a root component to trigger the loading UI of the surface.
        val controller = A2uiTestController(catalog = A2uiCatalog("test_catalog", emptyList()))
        val surface = controller.start()

        setContent { A2uiTestSurface(surface = surface, modifier = Modifier.testTag("root_tag")) }

        // By default, when the root component is loading, the A2uiTestSurface emits nothing,
        // hence the root component and its modifier should not exist in the semantics tree.
        onNodeWithTag("root_tag").assertDoesNotExist()
    }

    @Test
    fun loadingState_customLoadingLambda_invokesLambdaAndAppliesModifier() = runComposeUiTest {
        val controller = A2uiTestController(catalog = A2uiCatalog("test_catalog", emptyList()))
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                modifier = Modifier.testTag("passed_modifier"),
                onLoading = { modifier -> BasicText("Custom Loading", modifier = modifier) },
            )
        }

        onNode(hasText("Custom Loading") and hasTestTag("passed_modifier")).assertIsDisplayed()
    }

    @Test
    fun readinessEvaluator_componentNotReadyInitially_showsLoadingUntilReady() = runComposeUiTest {
        val titleProp = A2uiProperty.dynamicString("title")
        val controller =
            A2uiTestController(
                catalog = A2uiCatalog("test_catalog", emptyList()),
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            "root",
                            mapOf("title" to mapOf("path" to "/data/title")),
                        )
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId(
                            id = "root",
                            // The component is only ready when its data binding is resolved
                            isReady = { props -> props.bind(titleProp) != null },
                        ) { props, modifier ->
                            BasicText("Ready: ${props.bind(titleProp)}", modifier = modifier)
                        }
                    ),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Waiting for data...", modifier = modifier) },
            )
        }

        onNodeWithText("Waiting for data...").assertIsDisplayed()
        onNodeWithText("Ready: Hello World").assertDoesNotExist()

        controller.updateData("/data/title", "Hello World")
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Waiting for data...").assertDoesNotExist()
        onNodeWithText("Ready: Hello World").assertIsDisplayed()
    }

    @Test
    fun errorState_default_throwsAssertionError() = runComposeUiTest {
        val controller = A2uiTestController(catalog = A2uiCatalog("test_catalog", emptyList()))
        val surface = controller.start()

        val expectedException = A2uiRuntimeException("Simulated crash")
        controller.failComponent("root", expectedException)
        controller.waitForIdle()

        val exception = assertFailsWith<AssertionError> { setContent { A2uiTestSurface(surface) } }

        assertThat(exception).hasMessageThat().contains("A2UI test surface failed to render")
        assertThat(exception).hasMessageThat().contains("Simulated crash")
        assertThat(exception.cause).isSameInstanceAs(expectedException)
    }

    @Test
    fun errorState_customErrorLambda_invokesLambdaAndAppliesModifier() = runComposeUiTest {
        val controller = A2uiTestController(catalog = A2uiCatalog("test_catalog", emptyList()))
        val surface = controller.start()

        val expectedException = A2uiRuntimeException("Simulated crash")
        controller.failComponent("root", expectedException)
        controller.waitForIdle()

        setContent {
            A2uiTestSurface(
                surface = surface,
                modifier = Modifier.testTag("passed_modifier"),
                onError = { exception, modifier ->
                    BasicText("Custom Error: ${exception.message}", modifier = modifier)
                },
            )
        }

        onNode(hasText("Custom Error: Simulated crash") and hasTestTag("passed_modifier"))
            .assertIsDisplayed()
    }

    @Test
    fun transition_loadingToSuccess_updatesContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = A2uiCatalog("test_catalog", emptyList()),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("root") { _, modifier ->
                            BasicText("I am root", modifier = modifier)
                        }
                    ),
            )
        // Start without the root component to force the initial Loading state
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading...", modifier = modifier) },
            )
        }

        onNodeWithText("Loading...").assertIsDisplayed()
        onNodeWithText("I am root").assertDoesNotExist()

        // Simulate the agent streaming the root component payload
        controller.updateComponent("root", emptyMap())
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Loading...").assertDoesNotExist()
        onNodeWithText("I am root").assertIsDisplayed()
    }

    @Test
    fun transition_loadingToError_displaysError() = runComposeUiTest {
        val controller = A2uiTestController(catalog = A2uiCatalog("test_catalog", emptyList()))
        // Start without the root component to force the initial Loading state
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { mod -> BasicText("Loading...", modifier = mod) },
                onError = { exception, mod ->
                    BasicText("Error: ${exception.message}", modifier = mod)
                },
            )
        }

        onNodeWithText("Loading...").assertIsDisplayed()

        // Simulate an invalid root component payload arriving over the network
        controller.failComponent("root", A2uiValidationException("Missing required property", "/"))
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Loading...").assertDoesNotExist()
        onNodeWithText("Error: Missing required property").assertIsDisplayed()
    }

    @Test
    fun transition_successToLoading_displaysLoading() = runComposeUiTest {
        val titleProp = A2uiProperty.dynamicString("title")
        val controller =
            A2uiTestController(
                catalog = A2uiCatalog("test_catalog", emptyList()),
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            "root",
                            mapOf("title" to mapOf("path" to "/data/title")),
                        )
                    ),
                initialData = mapOf("data" to mapOf("title" to "Initial Title")),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId(
                            id = "root",
                            isReady = { props -> props.bind(titleProp) != null },
                        ) { props, modifier ->
                            BasicText("Ready: ${props.bind(titleProp)}", modifier = modifier)
                        }
                    ),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { mod -> BasicText("Loading...", modifier = mod) },
            )
        }

        onNodeWithText("Ready: Initial Title").assertIsDisplayed()

        // Simulate agent removing the data the component depends on
        controller.updateData("/data/title", null)
        controller.waitForIdle()

        onNodeWithText("Ready: Initial Title").assertDoesNotExist()
        onNodeWithText("Loading...").assertIsDisplayed()
    }

    @Test
    fun transition_successToError_displaysError() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = A2uiCatalog("test_catalog", emptyList()),
                initialComponents = listOf(A2uiComponentPayload("root")),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("root") { _, modifier ->
                            BasicText("I am root", modifier = modifier)
                        }
                    ),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onError = { exception, modifier ->
                    BasicText("Error: ${exception.message}", modifier = modifier)
                },
            )
        }

        onNodeWithText("I am root").assertIsDisplayed()

        // Dispatch a runtime agent hallucination/error to the root component
        controller.failComponent("root", A2uiRuntimeException("Late failure"))
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("I am root").assertDoesNotExist()
        onNodeWithText("Error: Late failure").assertIsDisplayed()
    }

    @Test
    fun transition_errorToSuccess_displaysSuccess() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = A2uiCatalog("test_catalog", emptyList()),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("root") { _, modifier ->
                            BasicText("Recovered Root", modifier = modifier)
                        }
                    ),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onError = { exception, modifier ->
                    BasicText("Error: ${exception.message}", modifier = modifier)
                },
            )
        }

        // Force an initial error state (e.g. LLM hallucinates a bad payload)
        controller.failComponent("root", A2uiRuntimeException("Initial failure"))
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Error: Initial failure").assertIsDisplayed()

        // Agent self-corrects by sending a valid component payload
        controller.updateComponent("root", emptyMap())
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Error: Initial failure").assertDoesNotExist()
        onNodeWithText("Recovered Root").assertIsDisplayed()
    }

    @Test
    fun transition_errorToLoading_displaysLoading() = runComposeUiTest {
        val titleProp = A2uiProperty.dynamicString("title")
        val controller =
            A2uiTestController(
                catalog = A2uiCatalog("test_catalog", emptyList()),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId(
                            id = "root",
                            isReady = { props -> props.bind(titleProp) != null },
                        ) { props, modifier ->
                            BasicText("Ready: ${props.bind(titleProp)}", modifier = modifier)
                        }
                    ),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading...", modifier = modifier) },
                onError = { exception, modifier ->
                    BasicText("Error: ${exception.message}", modifier = modifier)
                },
            )
        }

        // Force an initial error state (e.g. LLM hallucinates a bad payload)
        controller.failComponent("root", A2uiRuntimeException("Initial failure"))
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Error: Initial failure").assertIsDisplayed()

        // Agent attempts to self-correct by sending a valid component payload,
        // but it is not yet ready because it depends on missing data.
        controller.updateComponent("root", mapOf("title" to mapOf("path" to "/data/title")))
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Error: Initial failure").assertDoesNotExist()
        onNodeWithText("Loading...").assertIsDisplayed()
    }

    @Test
    fun surfaceParameterChanges_recomposesWithNewSurface() = runComposeUiTest {
        val controller1 =
            A2uiTestController(
                catalog = A2uiCatalog("test_catalog_1", emptyList()),
                initialComponents = listOf(A2uiComponentPayload("root")),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("root") { _, modifier ->
                            BasicText("Surface 1", modifier = modifier)
                        }
                    ),
            )
        val surface1 = controller1.start()

        val controller2 =
            A2uiTestController(
                catalog = A2uiCatalog("test_catalog_2", emptyList()),
                initialComponents = listOf(A2uiComponentPayload("root")),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("root") { _, modifier ->
                            BasicText("Surface 2", modifier = modifier)
                        }
                    ),
            )
        val surface2 = controller2.start()

        var currentSurface by mutableStateOf(surface1)

        setContent { A2uiTestSurface(surface = currentSurface) }

        onNodeWithText("Surface 1").assertIsDisplayed()

        currentSurface = surface2
        waitForIdle()

        onNodeWithText("Surface 1").assertDoesNotExist()
        onNodeWithText("Surface 2").assertIsDisplayed()
    }

    @Test
    fun modifierParameterChanges_updatesRenderedModifier() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = A2uiCatalog("test_catalog", emptyList()),
                initialComponents = listOf(A2uiComponentPayload("root")),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("root") { _, modifier ->
                            BasicText("I am root", modifier = modifier)
                        }
                    ),
            )
        val surface = controller.start()

        var currentTag by mutableStateOf("initial_tag")

        setContent { A2uiTestSurface(surface = surface, modifier = Modifier.testTag(currentTag)) }

        onNodeWithTag("initial_tag").assertIsDisplayed()
        onNodeWithTag("updated_tag").assertDoesNotExist()

        currentTag = "updated_tag"
        waitForIdle()

        onNodeWithTag("initial_tag").assertDoesNotExist()
        onNodeWithTag("updated_tag").assertIsDisplayed()
    }

    @Test
    fun rootComponentTypeChanges_recomposesWithNewType() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = A2uiCatalog("test_catalog", emptyList()),
                initialComponents = listOf(A2uiComponentPayload("root", "TypeA", emptyMap())),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withType("TypeA") { _, modifier ->
                            BasicText("I am Type A", modifier)
                        },
                        A2uiComponentStub.withType("TypeB") { _, modifier ->
                            BasicText("I am Type B", modifier)
                        },
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("I am Type A").assertIsDisplayed()
        onNodeWithText("I am Type B").assertDoesNotExist()

        controller.updateComponent("root", "TypeB", emptyMap())
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("I am Type A").assertDoesNotExist()
        onNodeWithText("I am Type B").assertIsDisplayed()
    }
}
