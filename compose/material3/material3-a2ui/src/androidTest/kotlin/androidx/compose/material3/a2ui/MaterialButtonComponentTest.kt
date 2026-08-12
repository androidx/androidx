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

import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.LocalA2uiReadinessEvaluator
import androidx.a2ui.compose.runtime.observeA2uiComponentState
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.asReadinessEvaluator
import androidx.a2ui.compose.ui.testing.A2uiComponentPayload
import androidx.a2ui.compose.ui.testing.A2uiComponentStub
import androidx.a2ui.compose.ui.testing.A2uiTestController
import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MaterialButtonComponentTest {

    private val testCatalog =
        A2uiCatalog(catalogId = "test_catalog", components = listOf(MaterialButtonComponent))

    @Test
    fun defaultVariant_rendersChildAndDispatchesActionOnClick() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "btn_text",
                                    "variant" to "default",
                                    "action" to
                                        mapOf(
                                            "event" to
                                                mapOf(
                                                    "name" to "click",
                                                    "context" to
                                                        mapOf(
                                                            "username" to
                                                                mapOf("path" to "/user/name")
                                                        ),
                                                )
                                        ),
                                ),
                        ),
                        A2uiComponentPayload(id = "btn_text"),
                    ),
                initialData = mapOf("user" to mapOf("name" to "Test User")),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("btn_text") { _, modifier ->
                            Text("Default Button", modifier = modifier)
                        }
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Default Button").assertIsDisplayed().performClick()
        waitForIdle()
        controller.waitForIdle()

        assertThat(controller.outboundEvents).hasSize(1)

        val action = controller.outboundEvents.single()
        assertThat(action.type).isEqualTo("click")
        assertThat(action.context["username"]).isEqualTo("Test User")
    }

    @Test
    fun primaryVariant_rendersChildAndDispatchesActionOnClick() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "btn_text",
                                    "variant" to "primary",
                                    "action" to
                                        mapOf(
                                            "event" to
                                                mapOf(
                                                    "name" to "click",
                                                    "context" to
                                                        mapOf(
                                                            "username" to
                                                                mapOf("path" to "/user/name")
                                                        ),
                                                )
                                        ),
                                ),
                        ),
                        A2uiComponentPayload(id = "btn_text"),
                    ),
                initialData = mapOf("user" to mapOf("name" to "Test User")),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("btn_text") { _, modifier ->
                            Text("Primary Button", modifier = modifier)
                        }
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Primary Button").assertIsDisplayed().performClick()
        waitForIdle()
        controller.waitForIdle()

        assertThat(controller.outboundEvents).hasSize(1)

        val action = controller.outboundEvents.single()
        assertThat(action.type).isEqualTo("click")
        assertThat(action.context["username"]).isEqualTo("Test User")
    }

    @Test
    fun borderlessVariant_rendersChildAndDispatchesActionOnClick() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "btn_text",
                                    "variant" to "borderless",
                                    "action" to
                                        mapOf(
                                            "event" to
                                                mapOf(
                                                    "name" to "click",
                                                    "context" to
                                                        mapOf(
                                                            "username" to
                                                                mapOf("path" to "/user/name")
                                                        ),
                                                )
                                        ),
                                ),
                        ),
                        A2uiComponentPayload(id = "btn_text"),
                    ),
                initialData = mapOf("user" to mapOf("name" to "Test User")),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("btn_text") { _, modifier ->
                            Text("Borderless Button", modifier = modifier)
                        }
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Borderless Button").assertIsDisplayed().performClick()
        waitForIdle()
        controller.waitForIdle()

        assertThat(controller.outboundEvents).hasSize(1)

        val action = controller.outboundEvents.single()
        assertThat(action.type).isEqualTo("click")
        assertThat(action.context["username"]).isEqualTo("Test User")
    }

    @Test
    fun childFails_rendersErrorState() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "failing_child",
                                    "action" to mapOf("event" to mapOf("name" to "click")),
                                ),
                        ),
                        A2uiComponentPayload(id = "failing_child"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("failing_child") { _, modifier ->
                            Text("Child Text", modifier = modifier)
                        }
                    ),
            )

        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        controller.failComponent("failing_child", A2uiRuntimeException("Failed to load child"))
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Child Text").assertDoesNotExist()
        onNodeWithText("Error").assertIsDisplayed()
    }

    @Test
    fun childTransitionsFromLoadingToSuccess_displaysChild() = runComposeUiTest {
        val isReadyState = mutableStateOf(false)
        val stub =
            A2uiComponentStub.withId(id = "delayed_text_id", isReady = { isReadyState.value }) {
                _,
                modifier ->
                Text("Submit", modifier = modifier)
            }

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "delayed_text_id",
                                    "action" to mapOf("event" to mapOf("name" to "click")),
                                ),
                        ),
                        A2uiComponentPayload(id = "delayed_text_id"),
                    ),
                componentStubs = listOf(stub),
            )

        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Submit").assertDoesNotExist()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()

        isReadyState.value = true
        controller.waitForIdle()
        waitForIdle()

        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
        onNodeWithText("Submit").assertIsDisplayed()
    }

    @Test
    fun childTransitionsFromLoadingToError_displaysErrorState() = runComposeUiTest {
        val isReadyState = mutableStateOf(false)
        val stub =
            A2uiComponentStub.withId("delayed_child", isReady = { isReadyState.value }) {
                _,
                modifier ->
                Text("Child", modifier = modifier)
            }
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "delayed_child",
                                    "action" to mapOf("event" to mapOf("name" to "click")),
                                ),
                        ),
                        A2uiComponentPayload(id = "delayed_child"),
                    ),
                componentStubs = listOf(stub),
            )

        val surface = controller.start()
        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Child").assertDoesNotExist()
        onNodeWithText("Error").assertDoesNotExist()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()

        controller.failComponent("delayed_child", A2uiRuntimeException("Failed to load"))
        controller.waitForIdle()
        waitForIdle()

        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
        onNodeWithText("Child").assertDoesNotExist()
        onNodeWithText("Error").assertIsDisplayed()
    }

    @Test
    fun childTransitionsFromErrorToSuccess_displaysRecoveredChild() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "child_id",
                                    "action" to mapOf("event" to mapOf("name" to "click")),
                                ),
                        ),
                        A2uiComponentPayload(id = "child_id"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child_id") { _, modifier ->
                            Text("Recovered Child", modifier = modifier)
                        }
                    ),
            )

        val surface = controller.start()
        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        controller.failComponent("child_id", A2uiRuntimeException("Initial Error"))
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Recovered Child").assertDoesNotExist()
        onNodeWithText("Error").assertIsDisplayed()

        controller.updateComponent(id = "child_id", properties = emptyMap())
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Error").assertDoesNotExist()
        onNodeWithText("Recovered Child").assertIsDisplayed()
    }

    @Test
    fun childTransitionsFromSuccessToLoading_hidesChild() = runComposeUiTest {
        val isReadyState = mutableStateOf(true)
        val stub =
            A2uiComponentStub.withId("child_id", isReady = { isReadyState.value }) { _, modifier ->
                Text("Dynamic Child", modifier = modifier)
            }
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "child_id",
                                    "action" to mapOf("event" to mapOf("name" to "click")),
                                ),
                        ),
                        A2uiComponentPayload(id = "child_id"),
                    ),
                componentStubs = listOf(stub),
            )

        val surface = controller.start()
        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Dynamic Child").assertIsDisplayed()
        onNodeWithText("Error").assertDoesNotExist()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()

        isReadyState.value = false
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Dynamic Child").assertDoesNotExist()
        onNodeWithText("Error").assertDoesNotExist()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun childTransitionsFromSuccessToError_displaysErrorState() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "child_id",
                                    "action" to mapOf("event" to mapOf("name" to "click")),
                                ),
                        ),
                        A2uiComponentPayload(id = "child_id"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child_id") { _, modifier ->
                            Text("Healthy Child", modifier = modifier)
                        }
                    ),
            )

        val surface = controller.start()
        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Healthy Child").assertIsDisplayed()
        onNodeWithText("Error").assertDoesNotExist()

        controller.failComponent("child_id", A2uiRuntimeException("Runtime Failure"))
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Healthy Child").assertDoesNotExist()
        onNodeWithText("Error").assertIsDisplayed()
    }

    @Test
    fun childIdChanges_rendersNewChildComponent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "child_1",
                                    "action" to mapOf("event" to mapOf("name" to "click")),
                                ),
                        ),
                        A2uiComponentPayload(id = "child_1"),
                        A2uiComponentPayload(id = "child_2"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child_1") { _, modifier ->
                            Text("Child One", modifier = modifier)
                        },
                        A2uiComponentStub.withId("child_2") { _, modifier ->
                            Text("Child Two", modifier = modifier)
                        },
                    ),
            )

        val surface = controller.start()
        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Child One").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties =
                mapOf("child" to "child_2", "action" to mapOf("event" to mapOf("name" to "click"))),
        )
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Child One").assertDoesNotExist()
        onNodeWithText("Child Two").assertIsDisplayed()
    }

    @Test
    fun childTypeChanges_switchesToNewComponentType() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "child_1",
                                    "action" to mapOf("event" to mapOf("name" to "click")),
                                ),
                        ),
                        A2uiComponentPayload(
                            id = "child_1",
                            type = "Progress",
                            properties = emptyMap(),
                        ),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withType("Progress") { _, modifier ->
                            CircularProgressIndicator(modifier = modifier.testTag("progress"))
                        },
                        A2uiComponentStub.withType("Text") { _, modifier ->
                            Text("Text Content", modifier = modifier)
                        },
                    ),
            )

        val surface = controller.start()
        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag("progress").assertIsDisplayed()

        controller.updateComponent(id = "child_1", type = "Text", properties = emptyMap())
        controller.waitForIdle()
        waitForIdle()

        onNodeWithTag("progress").assertDoesNotExist()
        onNodeWithText("Text Content").assertIsDisplayed()
    }

    @Test
    fun buttonVariantChanges_retainsChildContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "btn_text",
                                    "variant" to "default",
                                    "action" to mapOf("event" to mapOf("name" to "click")),
                                ),
                        ),
                        A2uiComponentPayload(id = "btn_text"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("btn_text") { _, modifier ->
                            Text("Variant Text", modifier = modifier)
                        }
                    ),
            )

        val surface = controller.start()
        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Variant Text").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties =
                mapOf(
                    "child" to "btn_text",
                    "variant" to "primary",
                    "action" to mapOf("event" to mapOf("name" to "click")),
                ),
        )
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Variant Text").assertIsDisplayed()
    }

    @Test
    fun actionChanges_dispatchesUpdatedActionOnClick() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "btn_text",
                                    "action" to mapOf("event" to mapOf("name" to "action_one")),
                                ),
                        ),
                        A2uiComponentPayload(id = "btn_text"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("btn_text") { _, modifier ->
                            Text("Action Text", modifier = modifier)
                        }
                    ),
            )

        val surface = controller.start()
        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Action Text").performClick()
        waitForIdle()
        controller.waitForIdle()

        assertThat(controller.outboundEvents.single().type).isEqualTo("action_one")

        controller.clearOutboundEvents()

        controller.updateComponent(
            id = "root",
            properties =
                mapOf(
                    "child" to "btn_text",
                    "action" to mapOf("event" to mapOf("name" to "action_two")),
                ),
        )
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Action Text").performClick()
        waitForIdle()
        controller.waitForIdle()

        assertThat(controller.outboundEvents.single().type).isEqualTo("action_two")
    }

    @Test
    fun parentModifier_isAppliedInAllChildStates() = runComposeUiTest {
        val isReadyState = mutableStateOf(false)
        val stub =
            A2uiComponentStub.withId("stub_child", isReady = { isReadyState.value }) { _, modifier
                ->
                Text("Ready Content", modifier = modifier)
            }
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "stub_child",
                                    "action" to mapOf("event" to mapOf("name" to "click")),
                                ),
                        ),
                        A2uiComponentPayload(id = "stub_child"),
                    ),
                componentStubs = listOf(stub),
            )

        val surface = controller.start()
        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface, modifier = Modifier.testTag("button_tag"))
            }
        }

        // Test tag applied in Loading state
        onNode(hasTestTag("button_tag")).assertIsDisplayed()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()

        // Transition to Success state
        isReadyState.value = true
        controller.waitForIdle()
        waitForIdle()
        onNode(hasTestTag("button_tag")).assertIsDisplayed()

        // Transition to Error state
        controller.failComponent("stub_child", A2uiRuntimeException("Failure"))
        controller.waitForIdle()
        waitForIdle()
        onNode(hasTestTag("button_tag")).assertIsDisplayed()
    }

    @Test
    fun accessibility_hasCorrectButtonRole() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "btn_text",
                                    "action" to mapOf("event" to mapOf("name" to "click")),
                                ),
                        ),
                        A2uiComponentPayload(id = "btn_text"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("btn_text") { _, modifier ->
                            Text("Accessible Button", modifier = modifier)
                        }
                    ),
            )

        val surface = controller.start()
        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNode(hasText("Accessible Button") and hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Composable
    private fun A2uiTestSurface(surface: A2uiSurfaceModel, modifier: Modifier = Modifier) {
        surface as? A2uiCoreSurfaceModel
            ?: throw IllegalArgumentException("A2uiTestSurface requires an A2uiCoreSurfaceModel.")

        val composeCatalog =
            surface.catalog as? A2uiCatalog
                ?: throw IllegalArgumentException("Catalog must implement A2uiCatalog.")

        val readinessEvaluator = remember(composeCatalog) { composeCatalog.asReadinessEvaluator() }

        CompositionLocalProvider(LocalA2uiReadinessEvaluator provides readinessEvaluator) {
            when (val rootState = observeA2uiComponentState(surface = surface)) {
                is A2uiComponentState.Success -> {
                    A2uiComponent(component = rootState.component, modifier = modifier)
                }
                is A2uiComponentState.Error -> {
                    throw rootState.exception
                }
                is A2uiComponentState.Loading -> {}
            }
        }
    }
}
