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

package androidx.compose.material3.a2ui.catalog

import android.view.KeyEvent
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.compose.ui.testing.A2uiComponentPayload
import androidx.a2ui.compose.ui.testing.A2uiComponentStub
import androidx.a2ui.compose.ui.testing.A2uiTestController
import androidx.a2ui.compose.ui.testing.A2uiTestSurface
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.a2ui.MaterialA2uiDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MaterialA2uiBasicCatalogV1ModalTest {

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components =
                listOf(
                    MaterialA2uiBasicCatalogV1Defaults.modal,
                    MaterialA2uiBasicCatalogV1Defaults.button,
                    MaterialA2uiBasicCatalogV1Defaults.text,
                ),
        )

    // =======================================================
    // Category 1: Trigger Interactions & Setup
    // =======================================================

    @Test
    fun initialState_showsTrigger_hidesContent() = runComposeUiTest {
        val modalPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Modal",
                properties = mapOf("trigger" to "trigger_btn", "content" to "modal_content"),
            )
        val triggerPayload =
            A2uiComponentPayload(
                id = "trigger_btn",
                type = "Button",
                properties =
                    mapOf(
                        "child" to "trigger_label",
                        "action" to mapOf("event" to mapOf("name" to "trigger_click")),
                    ),
            )
        val triggerLabelPayload =
            A2uiComponentPayload(
                id = "trigger_label",
                type = "Text",
                properties = mapOf("text" to "Open Modal"),
            )
        val contentPayload =
            A2uiComponentPayload(
                id = "modal_content",
                type = "Text",
                properties = mapOf("text" to "Dialog Body Content"),
            )

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(modalPayload, triggerPayload, triggerLabelPayload, contentPayload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Open Modal").assertIsDisplayed()
        onNodeWithText("Dialog Body Content").assertDoesNotExist()
    }

    @Test
    fun trigger_tap_opensModalAndDisplaysContent() = runComposeUiTest {
        val modalPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Modal",
                properties = mapOf("trigger" to "trigger_text", "content" to "modal_content"),
            )
        val triggerPayload =
            A2uiComponentPayload(
                id = "trigger_text",
                type = "Text",
                properties = mapOf("text" to "Click to open"),
            )
        val contentPayload =
            A2uiComponentPayload(
                id = "modal_content",
                type = "Text",
                properties = mapOf("text" to "Dialog Content Here"),
            )

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(modalPayload, triggerPayload, contentPayload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Click to open").assertIsDisplayed()
        onNodeWithText("Dialog Content Here").assertDoesNotExist()

        onNodeWithText("Click to open").performClick()

        onNodeWithText("Dialog Content Here").assertIsDisplayed()
    }

    @Test
    fun trigger_physicalTouchOnButton_interceptsClickAndDoesNotDispatchButtonAction() =
        runComposeUiTest {
            val modalPayload =
                A2uiComponentPayload(
                    id = "root",
                    type = "Modal",
                    properties = mapOf("trigger" to "trigger_btn", "content" to "modal_content"),
                )
            val triggerPayload =
                A2uiComponentPayload(
                    id = "trigger_btn",
                    type = "Button",
                    properties =
                        mapOf(
                            "child" to "trigger_label",
                            "action" to
                                mapOf("event" to mapOf("name" to "button_action_should_not_fire")),
                        ),
                )
            val triggerLabelPayload =
                A2uiComponentPayload(
                    id = "trigger_label",
                    type = "Text",
                    properties = mapOf("text" to "Open Dialog Button"),
                )
            val contentPayload =
                A2uiComponentPayload(
                    id = "modal_content",
                    type = "Text",
                    properties = mapOf("text" to "Inner Content"),
                )

            val controller =
                A2uiTestController(
                    catalog = testCatalog,
                    initialComponents =
                        listOf(modalPayload, triggerPayload, triggerLabelPayload, contentPayload),
                )
            val surface = controller.start()

            setContent { MaterialTheme { A2uiTestSurface(surface) } }

            onNodeWithText("Open Dialog Button").performTouchInput { click() }
            waitForIdle()
            controller.waitForIdle()

            onNodeWithText("Inner Content").assertIsDisplayed()

            assertThat(controller.dispatchedActions).isEmpty()
        }

    @Test
    fun trigger_accessibilityClickOnButton_interceptsClickAndDoesNotDispatchButtonAction() =
        runComposeUiTest {
            val modalPayload =
                A2uiComponentPayload(
                    id = "root",
                    type = "Modal",
                    properties = mapOf("trigger" to "trigger_btn", "content" to "modal_content"),
                )
            val triggerPayload =
                A2uiComponentPayload(
                    id = "trigger_btn",
                    type = "Button",
                    properties =
                        mapOf(
                            "child" to "trigger_label",
                            "action" to
                                mapOf("event" to mapOf("name" to "button_action_should_not_fire")),
                        ),
                )
            val triggerLabelPayload =
                A2uiComponentPayload(
                    id = "trigger_label",
                    type = "Text",
                    properties = mapOf("text" to "Open Dialog Button"),
                )
            val contentPayload =
                A2uiComponentPayload(
                    id = "modal_content",
                    type = "Text",
                    properties = mapOf("text" to "Inner Content"),
                )

            val controller =
                A2uiTestController(
                    catalog = testCatalog,
                    initialComponents =
                        listOf(modalPayload, triggerPayload, triggerLabelPayload, contentPayload),
                )
            val surface = controller.start()

            setContent { MaterialTheme { A2uiTestSurface(surface) } }

            onNodeWithText("Open Dialog Button").performClick()
            waitForIdle()
            controller.waitForIdle()

            onNodeWithText("Inner Content").assertIsDisplayed()

            assertThat(controller.dispatchedActions).isEmpty()
        }

    @Test
    fun trigger_loadingState_doesNotOpenModal() = runComposeUiTest {
        val modalPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Modal",
                properties = mapOf("trigger" to "loading_trigger", "content" to "modal_content"),
            )
        val contentPayload =
            A2uiComponentPayload(
                id = "modal_content",
                type = "Text",
                properties = mapOf("text" to "Modal Body"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(modalPayload, contentPayload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).performClick()

        onNodeWithText("Modal Body").assertDoesNotExist()
    }

    @Test
    fun trigger_errorState_doesNotOpenModal() = runComposeUiTest {
        val modalPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Modal",
                properties = mapOf("trigger" to "failing_trigger", "content" to "modal_content"),
            )
        val contentPayload =
            A2uiComponentPayload(
                id = "modal_content",
                type = "Text",
                properties = mapOf("text" to "Modal Body"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(modalPayload, contentPayload),
            )
        val surface = controller.start()

        controller.failComponent("failing_trigger", A2uiRuntimeException("Trigger failure"))

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Error").performClick()

        onNodeWithText("Modal Body").assertDoesNotExist()
    }

    // =======================================================
    // Category 2: Closing / Dismissing Modal
    // =======================================================

    @Test
    fun childAction_tap_closesModalAndDispatchesAction() = runComposeUiTest {
        val modalPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Modal",
                properties = mapOf("trigger" to "trigger_text", "content" to "modal_button"),
            )
        val triggerPayload =
            A2uiComponentPayload(
                id = "trigger_text",
                type = "Text",
                properties = mapOf("text" to "Open"),
            )
        val buttonPayload =
            A2uiComponentPayload(
                id = "modal_button",
                type = "Button",
                properties =
                    mapOf(
                        "child" to "button_text",
                        "action" to mapOf("event" to mapOf("name" to "submit")),
                    ),
            )
        val buttonTextPayload =
            A2uiComponentPayload(
                id = "button_text",
                type = "Text",
                properties = mapOf("text" to "Confirm"),
            )

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(modalPayload, triggerPayload, buttonPayload, buttonTextPayload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Open").performClick()

        onNodeWithText("Confirm").assertIsDisplayed()

        onNodeWithText("Confirm").performClick()

        onNodeWithText("Confirm").assertDoesNotExist()
        onNodeWithText("Open").assertIsDisplayed()
        controller.waitForIdle()

        assertThat(controller.dispatchedActions).hasSize(1)
    }

    @Test
    fun staticContent_tap_doesNotCloseModal() = runComposeUiTest {
        val modalPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Modal",
                properties = mapOf("trigger" to "trigger_text", "content" to "static_text"),
            )
        val triggerPayload =
            A2uiComponentPayload(
                id = "trigger_text",
                type = "Text",
                properties = mapOf("text" to "Open"),
            )
        val staticTextPayload =
            A2uiComponentPayload(
                id = "static_text",
                type = "Text",
                properties = mapOf("text" to "Non Actionable Text"),
            )

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(modalPayload, triggerPayload, staticTextPayload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Open").performClick()

        onNodeWithText("Non Actionable Text").assertIsDisplayed()

        // Tapping static content without an action handler does NOT close the modal
        onNodeWithText("Non Actionable Text").performClick()

        onNodeWithText("Non Actionable Text").assertIsDisplayed()
        assertThat(controller.dispatchedActions).isEmpty()
    }

    @Test
    fun backButton_press_closesModal() = runComposeUiTest {
        val modalPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Modal",
                properties = mapOf("trigger" to "trigger_text", "content" to "modal_content"),
            )
        val triggerPayload =
            A2uiComponentPayload(
                id = "trigger_text",
                type = "Text",
                properties = mapOf("text" to "Open Modal"),
            )
        val contentPayload =
            A2uiComponentPayload(
                id = "modal_content",
                type = "Text",
                properties = mapOf("text" to "Dismissable Content"),
            )

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(modalPayload, triggerPayload, contentPayload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Open Modal").performClick()

        onNodeWithText("Dismissable Content").assertIsDisplayed()

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)

        onNodeWithText("Dismissable Content").assertDoesNotExist()
        onNodeWithText("Open Modal").assertIsDisplayed()
    }

    @Test
    fun reopenedModal_showsContentAgain() = runComposeUiTest {
        val modalPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Modal",
                properties = mapOf("trigger" to "trigger_text", "content" to "modal_button"),
            )
        val triggerPayload =
            A2uiComponentPayload(
                id = "trigger_text",
                type = "Text",
                properties = mapOf("text" to "Open"),
            )
        val buttonPayload =
            A2uiComponentPayload(
                id = "modal_button",
                type = "Button",
                properties =
                    mapOf(
                        "child" to "button_text",
                        "action" to mapOf("event" to mapOf("name" to "dismiss")),
                    ),
            )
        val buttonTextPayload =
            A2uiComponentPayload(
                id = "button_text",
                type = "Text",
                properties = mapOf("text" to "Dismiss"),
            )

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(modalPayload, triggerPayload, buttonPayload, buttonTextPayload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        // Open
        onNodeWithText("Open").performClick()
        onNodeWithText("Dismiss").assertIsDisplayed()

        // Close via child button action
        onNodeWithText("Dismiss").performClick()
        onNodeWithText("Dismiss").assertDoesNotExist()

        // Re-open
        onNodeWithText("Open").performClick()
        onNodeWithText("Dismiss").assertIsDisplayed()
    }

    @Test
    fun openModal_survivesStateRestoration() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        val modalPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Modal",
                properties = mapOf("trigger" to "trigger_text", "content" to "modal_content"),
            )
        val triggerPayload =
            A2uiComponentPayload(
                id = "trigger_text",
                type = "Text",
                properties = mapOf("text" to "Open"),
            )
        val contentPayload =
            A2uiComponentPayload(
                id = "modal_content",
                type = "Text",
                properties = mapOf("text" to "Persistent Content"),
            )

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(modalPayload, triggerPayload, contentPayload),
            )
        val surface = controller.start()

        restorationTester.setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Open").performClick()
        onNodeWithText("Persistent Content").assertIsDisplayed()

        restorationTester.emulateSaveAndRestore()

        onNodeWithText("Persistent Content").assertIsDisplayed()
    }

    // =======================================================
    // Category 3: Trigger Lifecycle & Dynamic Property Changes
    // =======================================================

    @Test
    fun triggerTransitionsFromLoadingToSuccess_displaysTrigger() = runComposeUiTest {
        val isReadyState = mutableStateOf(false)
        val stub =
            A2uiComponentStub.withId("delayed_trigger", isReady = { isReadyState.value }) {
                _,
                modifier ->
                Text("Ready Trigger", modifier = modifier)
            }
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Modal",
                            properties =
                                mapOf("trigger" to "delayed_trigger", "content" to "modal_content"),
                        ),
                        A2uiComponentPayload(id = "delayed_trigger"),
                        A2uiComponentPayload(
                            id = "modal_content",
                            type = "Text",
                            properties = mapOf("text" to "Modal Body"),
                        ),
                    ),
                componentStubs = listOf(stub),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Ready Trigger").assertDoesNotExist()
        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()

        isReadyState.value = true

        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertDoesNotExist()
        onNodeWithText("Ready Trigger").assertIsDisplayed()
    }

    @Test
    fun triggerTransitionsFromSuccessToError_displaysErrorState() = runComposeUiTest {
        val modalPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Modal",
                properties = mapOf("trigger" to "trigger_text", "content" to "modal_content"),
            )
        val triggerPayload =
            A2uiComponentPayload(
                id = "trigger_text",
                type = "Text",
                properties = mapOf("text" to "Working Trigger"),
            )
        val contentPayload =
            A2uiComponentPayload(
                id = "modal_content",
                type = "Text",
                properties = mapOf("text" to "Modal Body"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(modalPayload, triggerPayload, contentPayload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Working Trigger").assertIsDisplayed()

        controller.failComponent("trigger_text", A2uiRuntimeException("Trigger failure"))

        onNodeWithText("Working Trigger").assertDoesNotExist()
        onNodeWithText("Error").assertIsDisplayed()
    }

    @Test
    fun triggerIdChanges_rendersNewTriggerComponent() = runComposeUiTest {
        val modalPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Modal",
                properties = mapOf("trigger" to "trigger_1", "content" to "modal_content"),
            )
        val trigger1Payload =
            A2uiComponentPayload(
                id = "trigger_1",
                type = "Text",
                properties = mapOf("text" to "First Trigger"),
            )
        val trigger2Payload =
            A2uiComponentPayload(
                id = "trigger_2",
                type = "Text",
                properties = mapOf("text" to "Second Trigger"),
            )
        val contentPayload =
            A2uiComponentPayload(
                id = "modal_content",
                type = "Text",
                properties = mapOf("text" to "Modal Body"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(modalPayload, trigger1Payload, trigger2Payload, contentPayload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("First Trigger").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties = mapOf("trigger" to "trigger_2", "content" to "modal_content"),
        )
        controller.waitForIdle()

        onNodeWithText("First Trigger").assertDoesNotExist()
        onNodeWithText("Second Trigger").assertIsDisplayed()
    }

    @Test
    fun passedModifier_appliesToTriggerContainer() = runComposeUiTest {
        val modalPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Modal",
                properties = mapOf("trigger" to "trigger_text", "content" to "modal_content"),
            )
        val triggerPayload =
            A2uiComponentPayload(
                id = "trigger_text",
                type = "Text",
                properties = mapOf("text" to "Trigger Text"),
            )
        val contentPayload =
            A2uiComponentPayload(
                id = "modal_content",
                type = "Text",
                properties = mapOf("text" to "Content Text"),
            )

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(modalPayload, triggerPayload, contentPayload),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface, modifier = Modifier.testTag("modal_tag"))
            }
        }

        onNode(hasTestTag("modal_tag")).assertIsDisplayed()
        onNode(hasText("Trigger Text")).assertIsDisplayed()
    }

    // =======================================================
    // Category 4: Content Lifecycle & Dynamic Transitions inside Open Modal
    // =======================================================

    @Test
    fun dynamicContentUpdate_reflectsInsideOpenModal() = runComposeUiTest {
        val modalPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Modal",
                properties = mapOf("trigger" to "trigger_text", "content" to "modal_content"),
            )
        val triggerPayload =
            A2uiComponentPayload(
                id = "trigger_text",
                type = "Text",
                properties = mapOf("text" to "Open Details"),
            )
        val contentPayload =
            A2uiComponentPayload(
                id = "modal_content",
                type = "Text",
                properties = mapOf("text" to mapOf("path" to "/order/status")),
            )

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(modalPayload, triggerPayload, contentPayload),
                initialData = mapOf("order" to mapOf("status" to "Processing")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Open Details").performClick()

        onNodeWithText("Processing").assertIsDisplayed()

        controller.updateData("/order/status", "Shipped")
        controller.waitForIdle()

        onNodeWithText("Shipped").assertIsDisplayed()
    }

    @Test
    fun contentIdChanges_rendersNewContentComponentInsideOpenModal() = runComposeUiTest {
        val modalPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Modal",
                properties = mapOf("trigger" to "trigger_text", "content" to "content_1"),
            )
        val triggerPayload =
            A2uiComponentPayload(
                id = "trigger_text",
                type = "Text",
                properties = mapOf("text" to "Open"),
            )
        val content1Payload =
            A2uiComponentPayload(
                id = "content_1",
                type = "Text",
                properties = mapOf("text" to "First Content"),
            )
        val content2Payload =
            A2uiComponentPayload(
                id = "content_2",
                type = "Text",
                properties = mapOf("text" to "Second Content"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(modalPayload, triggerPayload, content1Payload, content2Payload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Open").performClick()

        onNodeWithText("First Content").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties = mapOf("trigger" to "trigger_text", "content" to "content_2"),
        )
        controller.waitForIdle()

        onNodeWithText("First Content").assertDoesNotExist()
        onNodeWithText("Second Content").assertIsDisplayed()
    }

    @Test
    fun contentErrorState_displaysErrorFallbackInsideModal() = runComposeUiTest {
        val modalPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Modal",
                properties = mapOf("trigger" to "trigger_text", "content" to "failing_content"),
            )
        val triggerPayload =
            A2uiComponentPayload(
                id = "trigger_text",
                type = "Text",
                properties = mapOf("text" to "Open Failing Modal"),
            )

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(modalPayload, triggerPayload),
            )
        val surface = controller.start()

        controller.failComponent("failing_content", A2uiRuntimeException("Content failed"))

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Open Failing Modal").performClick()

        onNodeWithText("Error").assertIsDisplayed()
    }

    @Test
    fun contentTransitionsFromLoadingToSuccess_displaysContentInsideModal() = runComposeUiTest {
        val isReadyState = mutableStateOf(false)
        val stub =
            A2uiComponentStub.withId("delayed_content", isReady = { isReadyState.value }) {
                _,
                modifier ->
                Text("Delayed Content Resolved", modifier = modifier)
            }
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Modal",
                            properties =
                                mapOf("trigger" to "trigger_text", "content" to "delayed_content"),
                        ),
                        A2uiComponentPayload(
                            id = "trigger_text",
                            type = "Text",
                            properties = mapOf("text" to "Open Delayed"),
                        ),
                        A2uiComponentPayload(id = "delayed_content"),
                    ),
                componentStubs = listOf(stub),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Open Delayed").performClick()

        onNodeWithText("Delayed Content Resolved").assertDoesNotExist()
        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()

        isReadyState.value = true

        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertDoesNotExist()
        onNodeWithText("Delayed Content Resolved").assertIsDisplayed()
    }

    @Test
    fun contentTransitionsFromLoadingToError_displaysErrorFallbackInsideModal() = runComposeUiTest {
        val isReadyState = mutableStateOf(false)
        val stub =
            A2uiComponentStub.withId("failing_content", isReady = { isReadyState.value }) {
                _,
                modifier ->
                Text("Resolved Content", modifier = modifier)
            }
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Modal",
                            properties =
                                mapOf("trigger" to "trigger_text", "content" to "failing_content"),
                        ),
                        A2uiComponentPayload(
                            id = "trigger_text",
                            type = "Text",
                            properties = mapOf("text" to "Open Modal"),
                        ),
                        A2uiComponentPayload(id = "failing_content"),
                    ),
                componentStubs = listOf(stub),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Open Modal").performClick()

        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()

        controller.failComponent("failing_content", A2uiRuntimeException("Content error"))

        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertDoesNotExist()
        onNodeWithText("Error").assertIsDisplayed()
    }

    @Test
    fun contentTransitionsFromErrorToLoading_displaysLoadingIndicatorInsideModal() =
        runComposeUiTest {
            val isReadyState = mutableStateOf(true)
            val stub =
                A2uiComponentStub.withId("reloadable_content", isReady = { isReadyState.value }) {
                    _,
                    modifier ->
                    Text("Content Loaded", modifier = modifier)
                }
            val controller =
                A2uiTestController(
                    catalog = testCatalog,
                    initialComponents =
                        listOf(
                            A2uiComponentPayload(
                                id = "root",
                                type = "Modal",
                                properties =
                                    mapOf(
                                        "trigger" to "trigger_text",
                                        "content" to "reloadable_content",
                                    ),
                            ),
                            A2uiComponentPayload(
                                id = "trigger_text",
                                type = "Text",
                                properties = mapOf("text" to "Open Modal"),
                            ),
                            A2uiComponentPayload(id = "reloadable_content"),
                        ),
                    componentStubs = listOf(stub),
                )
            val surface = controller.start()

            setContent { MaterialTheme { A2uiTestSurface(surface) } }

            onNodeWithText("Open Modal").performClick()
            onNodeWithText("Content Loaded").assertIsDisplayed()

            controller.failComponent("reloadable_content", A2uiRuntimeException("Error occurred"))
            controller.waitForIdle()

            onNodeWithText("Error").assertIsDisplayed()

            isReadyState.value = false
            controller.updateComponent(
                id = "reloadable_content",
                properties = mapOf("text" to "Recovering"),
            )
            controller.waitForIdle()

            onNodeWithText("Error").assertDoesNotExist()
            onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()
        }

    @Test
    fun contentTransitionsFromSuccessToLoading_hidesContentInsideModal() = runComposeUiTest {
        val isReadyState = mutableStateOf(true)
        val stub =
            A2uiComponentStub.withId("stub_content", isReady = { isReadyState.value }) { _, modifier
                ->
                Text("Ready Modal Content", modifier = modifier)
            }
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Modal",
                            properties =
                                mapOf("trigger" to "trigger_text", "content" to "stub_content"),
                        ),
                        A2uiComponentPayload(
                            id = "trigger_text",
                            type = "Text",
                            properties = mapOf("text" to "Open Modal"),
                        ),
                        A2uiComponentPayload(id = "stub_content"),
                    ),
                componentStubs = listOf(stub),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Open Modal").performClick()

        onNodeWithText("Ready Modal Content").assertIsDisplayed()
        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertDoesNotExist()

        isReadyState.value = false

        onNodeWithText("Ready Modal Content").assertDoesNotExist()
        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun contentTransitionsFromSuccessToError_displaysErrorStateInsideModal() = runComposeUiTest {
        val modalPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Modal",
                properties = mapOf("trigger" to "trigger_text", "content" to "modal_content"),
            )
        val triggerPayload =
            A2uiComponentPayload(
                id = "trigger_text",
                type = "Text",
                properties = mapOf("text" to "Open Modal"),
            )
        val contentPayload =
            A2uiComponentPayload(
                id = "modal_content",
                type = "Text",
                properties = mapOf("text" to "Working Content"),
            )

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(modalPayload, triggerPayload, contentPayload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Open Modal").performClick()

        onNodeWithText("Working Content").assertIsDisplayed()

        controller.failComponent("modal_content", A2uiRuntimeException("Content failure"))

        onNodeWithText("Working Content").assertDoesNotExist()
        onNodeWithText("Error").assertIsDisplayed()
    }

    // =======================================================
    // Category 5: Accessibility
    // =======================================================

    @Test
    fun accessibility_withLabelAndDescription_setsContentDescription() = runComposeUiTest {
        val modalPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Modal",
                properties =
                    mapOf(
                        "trigger" to "trigger_text",
                        "content" to "modal_content",
                        "accessibility" to
                            mapOf(
                                "label" to "Modal Label",
                                "description" to "Modal Description",
                            ),
                    ),
            )
        val triggerPayload =
            A2uiComponentPayload(
                id = "trigger_text",
                type = "Text",
                properties = mapOf("text" to "Open Modal"),
            )
        val contentPayload =
            A2uiComponentPayload(
                id = "modal_content",
                type = "Text",
                properties = mapOf("text" to "Modal Content"),
            )

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(modalPayload, triggerPayload, contentPayload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        // Content description is not present before the dialog is opened
        onNodeWithContentDescription("Modal Label - Modal Description").assertDoesNotExist()

        // Open modal
        onNodeWithText("Open Modal").performClick()

        // Content description is present on the dialog surface
        onNodeWithContentDescription("Modal Label - Modal Description").assertIsDisplayed()

        // Dismiss modal via Back key
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        waitForIdle()

        // Content description is removed when the dialog is dismissed
        onNodeWithContentDescription("Modal Label - Modal Description").assertDoesNotExist()
        onNodeWithText("Open Modal").assertIsDisplayed()
    }

    @Test
    fun accessibility_childrenMaintainIndependentAccessibilityProperties() = runComposeUiTest {
        val triggerStub =
            A2uiComponentStub.withId("stub_trigger") { _, modifier ->
                Text(
                    "Open Trigger",
                    modifier =
                        modifier.a2uiAccessibility(
                            attributes =
                                A2uiBasicCatalogV1.AccessibilityAttributes(
                                    label = "Trigger Label",
                                    description = "Trigger Description",
                                ),
                            isClickable = false,
                        ),
                )
            }
        val contentStub =
            A2uiComponentStub.withId("stub_content") { _, modifier ->
                Text(
                    "Modal Content",
                    modifier =
                        modifier.a2uiAccessibility(
                            attributes =
                                A2uiBasicCatalogV1.AccessibilityAttributes(
                                    label = "Content Label",
                                    description = "Content Description",
                                ),
                            isClickable = false,
                        ),
                )
            }
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Modal",
                            properties =
                                mapOf(
                                    "trigger" to "stub_trigger",
                                    "content" to "stub_content",
                                    "accessibility" to
                                        mapOf(
                                            "label" to "Modal Label",
                                            "description" to "Modal Description",
                                        ),
                                ),
                        ),
                        A2uiComponentPayload(id = "stub_trigger"),
                        A2uiComponentPayload(id = "stub_content"),
                    ),
                componentStubs = listOf(triggerStub, contentStub),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        // Trigger maintains its own accessibility properties before opening
        onNodeWithContentDescription("Trigger Label - Trigger Description").assertIsDisplayed()
        onNodeWithContentDescription("Modal Label - Modal Description").assertDoesNotExist()

        // Open modal
        onNodeWithContentDescription("Trigger Label - Trigger Description").performClick()

        // Both modal dialog and content maintain independent accessibility properties
        onNodeWithContentDescription("Modal Label - Modal Description").assertIsDisplayed()
        onNodeWithContentDescription("Content Label - Content Description").assertIsDisplayed()
    }
}
