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

package androidx.a2ui.compose.ui.testing.samples

import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.testing.A2uiComponentPayload
import androidx.a2ui.compose.ui.testing.A2uiComponentStub
import androidx.a2ui.compose.ui.testing.A2uiTestController
import androidx.a2ui.compose.ui.testing.A2uiTestSurface
import androidx.a2ui.compose.ui.testing.getData
import androidx.annotation.Sampled
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.google.common.truth.Truth.assertThat

@Sampled
fun A2uiTestControllerSample() = runComposeUiTest {
    val titleProp = A2uiProperty.string("title")
    val rootStub =
        A2uiComponentStub.withId("root") { props, modifier ->
            val title = props[titleProp] ?: "Initial Title"
            BasicText(title, modifier = modifier.testTag("root_tag"))
        }

    // Initialize the controller with catalog and stubs
    val controller =
        A2uiTestController(
            catalog = A2uiCatalog("test_catalog", emptyList()),
            initialComponents = listOf(A2uiComponentPayload("root")),
            componentStubs = listOf(rootStub),
        )

    // Start message processing and initialize a surface model
    val surface = controller.start()

    setContent { A2uiTestSurface(surface = surface) }

    onNodeWithText("Initial Title").assertIsDisplayed()

    // Simulate an agent pushing an incremental property update to the component
    controller.updateComponent(id = "root", properties = mapOf("title" to "Updated Title"))
    // Suspend until the update components message is fully processed
    controller.waitForIdle()

    onNodeWithText("Updated Title").assertIsDisplayed()
}

@Sampled
fun A2uiTestControllerDataSample() = runComposeUiTest {
    val controller =
        A2uiTestController(
            catalog = A2uiCatalog("test_catalog", emptyList()),
            initialData = mapOf("user" to mapOf("name" to "Alice")),
        )
    controller.start()

    // Read typed data from the controller's underlying data model
    val initialName: String? = controller.getData("/user/name")
    assertThat(initialName).isEqualTo("Alice")

    // Simulate the agent sending a data layer update
    controller.updateData("/user/name", "Bob")
    controller.waitForIdle()

    val updatedName: String? = controller.getData("/user/name")
    assertThat(updatedName).isEqualTo("Bob")
}

@Sampled
fun A2uiTestControllerActionsAndEventsSample() = runComposeUiTest {
    val buttonStub =
        A2uiComponentStub.withId("button") { _, modifier ->
            BasicText(
                "Submit",
                modifier.testTag("submit_button").clickable {
                    dispatchAction(
                        mapOf(
                            "event" to
                                mapOf("name" to "on_submit", "context" to emptyMap<String, Any?>())
                        )
                    )
                },
            )
        }

    val controller =
        A2uiTestController(
            catalog = A2uiCatalog("test_catalog", emptyList()),
            initialComponents = listOf(A2uiComponentPayload("button")),
            componentStubs = listOf(buttonStub),
        )
    val surface = controller.start()

    setContent { A2uiTestSurface(surface = surface) }

    // Perform a click on the rendered button
    onNodeWithTag("submit_button").performClick()
    waitForIdle()
    controller.waitForIdle()

    // Assert that the dispatched event was recorded by the controller
    assertThat(controller.dispatchedActions).hasSize(1)
    assertThat(controller.outboundEvents).hasSize(1)
    assertThat(controller.outboundEvents.first().type).isEqualTo("on_submit")
}
