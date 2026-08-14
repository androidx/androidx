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

package androidx.compose.material3.a2ui

import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.testing.A2uiComponentPayload
import androidx.a2ui.compose.ui.testing.A2uiComponentStub
import androidx.a2ui.compose.ui.testing.A2uiTestController
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
import androidx.a2ui.model.schema.A2uiSchema
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class A2uiSurfaceTest {

    private val testCatalog = A2uiCatalog(catalogId = "test_catalog", components = emptyList())

    @Test
    fun surface_nonCoreSurfaceModel_throwsIllegalArgumentException() = runComposeUiTest {
        val fakeSurface =
            object : A2uiSurfaceModel {
                override val id: String = "fake_surface"
            }

        val exception =
            assertFailsWith<IllegalArgumentException> { setContent { A2uiSurface(fakeSurface) } }

        assertThat(exception)
            .hasMessageThat()
            .contains("A2uiSurface requires an A2uiCoreSurfaceModel.")
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
                setContent { A2uiSurface(fakeCoreSurface) }
            }

        assertThat(exception).hasMessageThat().contains("A2uiSurface requires an A2uiCatalog.")
    }

    @Test
    fun successState_rendersRootComponent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(A2uiComponentPayload("root")),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("root") { _, modifier ->
                            Text("Initial Content", modifier = modifier)
                        }
                    ),
            )
        val surface = controller.start()

        setContent { A2uiSurface(surface) }

        onNodeWithText("Initial Content").assertIsDisplayed()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
    }

    @Test
    fun loadingState_default_displaysLoadingIndicator() = runComposeUiTest {
        val controller = A2uiTestController(catalog = testCatalog)
        val surface = controller.start()

        setContent { A2uiSurface(surface) }

        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun loadingState_customContent_displaysCustomContent() = runComposeUiTest {
        val controller = A2uiTestController(catalog = testCatalog)
        val surface = controller.start()

        setContent {
            A2uiSurface(surfaceModel = surface, loadingContent = { Text("Custom Loading...") })
        }

        onNodeWithText("Custom Loading...").assertIsDisplayed()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
    }

    @Test
    fun errorState_default_displaysErrorFallback() = runComposeUiTest {
        val controller = A2uiTestController(catalog = testCatalog)
        val surface = controller.start()

        setContent { A2uiSurface(surface) }

        controller.failComponent("root", A2uiRuntimeException("HallucinatedType"))
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("HallucinatedType").assertIsDisplayed()

        val error = controller.outboundErrors.single()
        assertThat(error.code).isEqualTo("RUNTIME_ERROR")
    }

    @Test
    fun errorState_customContent_displaysCustomContent() = runComposeUiTest {
        val controller = A2uiTestController(catalog = testCatalog)
        val surface = controller.start()

        setContent {
            A2uiSurface(
                surfaceModel = surface,
                errorContent = { exception -> Text("Custom Error: ${exception.message}") },
            )
        }

        controller.failComponent("root", A2uiRuntimeException("Test Exception"))
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Custom Error: Test Exception").assertIsDisplayed()
        onNodeWithText("Test Exception").assertDoesNotExist()
    }

    @Test
    fun errorState_exceptionUpdate_updatesErrorMessage() = runComposeUiTest {
        val controller = A2uiTestController(catalog = testCatalog)
        val surface = controller.start()

        setContent { A2uiSurface(surface) }

        controller.failComponent("root", A2uiRuntimeException("First Error"))
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("First Error").assertIsDisplayed()

        controller.failComponent("root", A2uiRuntimeException("Second Error"))
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("First Error").assertDoesNotExist()
        onNodeWithText("Second Error").assertIsDisplayed()
    }

    @Test
    fun transition_loadingToSuccess_updatesContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                componentStubs =
                    listOf(
                        A2uiComponentStub.withType("RootLayout") { _, modifier ->
                            Text("Content Ready", modifier = modifier)
                        }
                    ),
            )
        val surface = controller.start()

        setContent { A2uiSurface(surface) }

        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()

        controller.updateComponent(id = "root", type = "RootLayout", properties = emptyMap())
        controller.waitForIdle()
        waitForIdle()

        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
        onNodeWithText("Content Ready").assertIsDisplayed()
    }

    @Test
    fun transition_loadingToError_displaysError() = runComposeUiTest {
        val controller = A2uiTestController(catalog = testCatalog)
        val surface = controller.start()

        setContent { A2uiSurface(surface) }

        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()

        controller.failComponent("root", A2uiRuntimeException("Network Timeout"))
        controller.waitForIdle()
        waitForIdle()

        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
        onNodeWithText("Network Timeout").assertIsDisplayed()
    }

    @Test
    fun transition_successToLoading_displaysLoading() = runComposeUiTest {
        val titleProp = A2uiProperty.dynamicString("title")
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            properties = mapOf("title" to mapOf("path" to "/data/title")),
                        )
                    ),
                initialData = mapOf("data" to mapOf("title" to "Initial Title")),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId(
                            id = "root",
                            isReady = { props -> props.bind(titleProp) != null },
                        ) { props, modifier ->
                            Text("Ready: ${props.bind(titleProp)}", modifier = modifier)
                        }
                    ),
            )
        val surface = controller.start()

        setContent { A2uiSurface(surface) }

        onNodeWithText("Ready: Initial Title").assertIsDisplayed()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()

        controller.updateData("/data/title", null)
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Ready: Initial Title").assertDoesNotExist()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun transition_successToError_displaysError() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "RootLayout",
                            properties = emptyMap(),
                        )
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withType("RootLayout") { _, modifier ->
                            Text("I am root", modifier = modifier)
                        }
                    ),
            )
        val surface = controller.start()

        setContent { A2uiSurface(surface) }

        onNodeWithText("I am root").assertIsDisplayed()

        controller.failComponent("root", A2uiRuntimeException("Runtime Crash"))
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("I am root").assertDoesNotExist()
        onNodeWithText("Runtime Crash").assertIsDisplayed()
    }

    @Test
    fun transition_errorToSuccess_displaysSuccess() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("root") { _, modifier ->
                            Text("Recovered Content", modifier = modifier)
                        }
                    ),
            )
        val surface = controller.start()

        setContent { A2uiSurface(surface) }

        controller.failComponent("root", A2uiRuntimeException("Initial Failure"))
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Initial Failure").assertIsDisplayed()

        controller.updateComponent(id = "root", properties = emptyMap())
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Initial Failure").assertDoesNotExist()
        onNodeWithText("Recovered Content").assertIsDisplayed()
    }

    @Test
    fun transition_errorToLoading_displaysLoading() = runComposeUiTest {
        val titleProp = A2uiProperty.dynamicString("title")
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId(
                            id = "root",
                            isReady = { props -> props.bind(titleProp) != null },
                        ) { props, modifier ->
                            Text("Ready: ${props.bind(titleProp)}", modifier = modifier)
                        }
                    ),
            )
        val surface = controller.start()

        setContent { A2uiSurface(surface) }

        controller.failComponent("root", A2uiRuntimeException("Initial Failure"))
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Initial Failure").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties = mapOf("title" to mapOf("path" to "/data/title")),
        )
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Initial Failure").assertDoesNotExist()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun transition_customTransitionSpec_isInvoked() = runComposeUiTest {
        var transitionInvoked = false
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                componentStubs =
                    listOf(
                        A2uiComponentStub.withType("RootLayout") { _, modifier ->
                            Text("Content Ready", modifier = modifier)
                        }
                    ),
            )
        val surface = controller.start()

        setContent {
            A2uiSurface(
                surfaceModel = surface,
                transitionSpec = {
                    transitionInvoked = true
                    fadeIn() togetherWith fadeOut()
                },
            )
        }

        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()

        controller.updateComponent(id = "root", type = "RootLayout", properties = emptyMap())
        controller.waitForIdle()

        onNodeWithText("Content Ready").assertIsDisplayed()
        assertThat(transitionInvoked).isTrue()
    }

    @Test
    fun transition_propertyUpdate_doesNotTriggerAnimation() = runComposeUiTest {
        val labelProp = A2uiProperty.string("label")
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "RootLayout",
                            properties = mapOf("label" to "Initial"),
                        )
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withType("RootLayout") { props, modifier ->
                            Text(props[labelProp] ?: "", modifier = modifier)
                        }
                    ),
            )
        val surface = controller.start()

        setContent {
            A2uiSurface(
                surfaceModel = surface,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
            )
        }

        onNodeWithText("Initial").assertIsDisplayed()

        // Pause the animation clock to intercept any potential crossfade animation
        mainClock.autoAdvance = false

        // Update properties without changing the component type
        controller.updateComponent(
            id = "root",
            type = "RootLayout",
            properties = mapOf("label" to "Updated"),
        )
        controller.waitForIdle()

        // Advance by a single frame to allow composition of the new state
        mainClock.advanceTimeByFrame()

        // If a structural transition animation had been triggered, both the entering "Updated" node
        // and the exiting "Initial" node would exist simultaneously in the semantic tree during the
        // crossfade. However, we expect the content to update instantly as an animated transition
        // is not triggered for property updates.
        onNodeWithText("Initial").assertDoesNotExist()
        onNodeWithText("Updated").assertIsDisplayed()
    }

    @Test
    fun modifier_withTransitions_isAppliedToAnimatedContentRoot() = runComposeUiTest {
        val controller = A2uiTestController(catalog = testCatalog)
        val surface = controller.start()

        setContent {
            A2uiSurface(surfaceModel = surface, modifier = Modifier.testTag("surface_root"))
        }

        onNodeWithTag("surface_root").assertExists()
    }

    @Test
    fun modifier_withoutTransitions_isAppliedToBoxRoot() = runComposeUiTest {
        val controller = A2uiTestController(catalog = testCatalog)
        val surface = controller.start()

        setContent {
            A2uiSurface(
                surfaceModel = surface,
                modifier = Modifier.testTag("surface_root_box"),
                transitionSpec = null,
            )
        }

        onNodeWithTag("surface_root_box").assertExists()
    }

    @Test
    fun modifierParameterChanges_updatesRenderedModifier() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(A2uiComponentPayload("root")),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("root") { _, modifier ->
                            Text("I am root", modifier = modifier)
                        }
                    ),
            )
        val surface = controller.start()

        var currentTag by mutableStateOf("initial_tag")

        setContent { A2uiSurface(surfaceModel = surface, modifier = Modifier.testTag(currentTag)) }

        onNodeWithTag("initial_tag").assertIsDisplayed()
        onNodeWithTag("updated_tag").assertDoesNotExist()

        currentTag = "updated_tag"
        waitForIdle()

        onNodeWithTag("initial_tag").assertDoesNotExist()
        onNodeWithTag("updated_tag").assertIsDisplayed()
    }

    @Test
    fun surfaceParameterChanges_withSameComponentType_rendersNewSurfaceContent() =
        runComposeUiTest {
            val labelProp = A2uiProperty.string("label")
            val controller1 =
                A2uiTestController(
                    catalog = testCatalog,
                    initialComponents =
                        listOf(
                            A2uiComponentPayload(
                                id = "root",
                                properties = mapOf("label" to "Surface 1"),
                            )
                        ),
                    componentStubs =
                        listOf(
                            A2uiComponentStub.withId("root") { props, modifier ->
                                Text(props[labelProp] ?: "", modifier = modifier)
                            }
                        ),
                )
            val surface1 = controller1.start()

            val controller2 =
                A2uiTestController(
                    catalog = testCatalog,
                    initialComponents =
                        listOf(
                            A2uiComponentPayload(
                                id = "root",
                                properties = mapOf("label" to "Surface 2"),
                            )
                        ),
                    componentStubs =
                        listOf(
                            A2uiComponentStub.withId("root") { props, modifier ->
                                Text(props[labelProp] ?: "", modifier = modifier)
                            }
                        ),
                )
            val surface2 = controller2.start()

            var currentSurface by mutableStateOf(surface1)

            setContent { A2uiSurface(currentSurface) }

            onNodeWithText("Surface 1").assertIsDisplayed()
            onNodeWithText("Surface 2").assertDoesNotExist()

            currentSurface = surface2
            waitForIdle()

            onNodeWithText("Surface 1").assertDoesNotExist()
            onNodeWithText("Surface 2").assertIsDisplayed()
        }

    @Test
    fun rootComponentTypeChanges_recomposesWithNewType() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(id = "root", type = "TypeA", properties = emptyMap())
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withType("TypeA") { _, modifier ->
                            Text("I am Type A", modifier = modifier)
                        },
                        A2uiComponentStub.withType("TypeB") { _, modifier ->
                            Text("I am Type B", modifier = modifier)
                        },
                    ),
            )
        val surface = controller.start()

        setContent { A2uiSurface(surface) }

        onNodeWithText("I am Type A").assertIsDisplayed()
        onNodeWithText("I am Type B").assertDoesNotExist()

        controller.updateComponent(id = "root", type = "TypeB", properties = emptyMap())
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("I am Type A").assertDoesNotExist()
        onNodeWithText("I am Type B").assertIsDisplayed()
    }
}
