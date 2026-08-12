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
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.annotation.Sampled
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest

@Sampled
fun A2uiComponentStubWithIdSample() = runComposeUiTest {
    // Define a stub for a specific component ID to isolate it in tests
    val buttonStub =
        A2uiComponentStub.withId("submit_button") { _, modifier ->
            BasicText("Stubbed Submit Button", modifier = modifier.testTag("button_tag"))
        }

    val controller =
        A2uiTestController(
            catalog = A2uiCatalog("test_catalog", emptyList()),
            initialComponents = listOf(A2uiComponentPayload("submit_button")),
            componentStubs = listOf(buttonStub),
        )
    val surface = controller.start()

    setContent { A2uiTestSurface(surface = surface) }

    // Verify the surface renders the stubbed ID content
    onNodeWithTag("button_tag").assertIsDisplayed()
    onNodeWithText("Stubbed Submit Button").assertIsDisplayed()
}

@Sampled
fun A2uiComponentStubWithTypeSample() = runComposeUiTest {
    val urlProp = A2uiProperty.string("url")
    // Define a stub to override all components of type "Image"
    val imageStub =
        A2uiComponentStub.withType("Image") { properties, modifier ->
            val url = properties[urlProp] ?: "no-url"
            BasicText("Stubbed Image: $url", modifier = modifier.testTag("image_tag"))
        }

    val controller =
        A2uiTestController(
            catalog = A2uiCatalog("test_catalog", emptyList()),
            initialComponents =
                listOf(
                    A2uiComponentPayload(
                        id = "header_image",
                        type = "Image",
                        properties = mapOf("url" to "https://example.com/photo.png"),
                    )
                ),
            componentStubs = listOf(imageStub),
        )
    val surface = controller.start()

    setContent { A2uiTestSurface(surface = surface) }

    onNodeWithTag("image_tag").assertIsDisplayed()
    onNodeWithText("Stubbed Image: https://example.com/photo.png").assertIsDisplayed()
}

@Sampled
fun A2uiComponentPayloadForIdStubSample() = runComposeUiTest {
    val textProp = A2uiProperty.string("text")
    val stub =
        A2uiComponentStub.withId("title_stub") { props, modifier ->
            val title = props[textProp] ?: ""
            BasicText(title, modifier = modifier.testTag("title_tag"))
        }

    // Use the ID-only A2uiComponentPayload helper specifically designed for ID stubs
    val initialPayload =
        A2uiComponentPayload(id = "title_stub", properties = mapOf("text" to "Welcome to A2UI"))

    val controller =
        A2uiTestController(
            catalog = A2uiCatalog("test_catalog", emptyList()),
            initialComponents = listOf(initialPayload),
            componentStubs = listOf(stub),
        )
    val surface = controller.start()

    setContent { A2uiTestSurface(surface = surface) }

    onNodeWithTag("title_tag").assertIsDisplayed()
    onNodeWithText("Welcome to A2UI").assertIsDisplayed()
}
