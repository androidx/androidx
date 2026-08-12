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

import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.testing.A2uiComponentPayload
import androidx.a2ui.compose.ui.testing.A2uiComponentStub
import androidx.a2ui.compose.ui.testing.A2uiTestController
import androidx.a2ui.compose.ui.testing.A2uiTestSurface
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.annotation.Sampled
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest

@Sampled
fun A2uiTestSurfaceSample() = runComposeUiTest {
    val controller =
        A2uiTestController(
            catalog = A2uiCatalog("test_catalog", emptyList()),
            initialComponents = listOf(A2uiComponentPayload("root")),
            componentStubs =
                listOf(
                    A2uiComponentStub.withId("root") { _, modifier ->
                        BasicText("Success Root", modifier = modifier.testTag("root_tag"))
                    }
                ),
        )
    val surface = controller.start()

    setContent {
        // Mount a test A2UI surface for a given surface model with custom loading and error UIs
        A2uiTestSurface(
            surface = surface,
            onLoading = { modifier ->
                BasicText("Loading...", modifier = modifier.testTag("loading_tag"))
            },
            onError = { exception, modifier ->
                BasicText("Error: ${exception.message}", modifier = modifier.testTag("error_tag"))
            },
        )
    }

    onNodeWithTag("root_tag").assertIsDisplayed()
    onNodeWithText("Success Root").assertIsDisplayed()

    // Simulate an error arriving for the root component
    controller.failComponent("root", A2uiRuntimeException("Simulated crash"))
    controller.waitForIdle()

    // Verify the surface transitions to the onError callback
    onNodeWithTag("error_tag").assertIsDisplayed()
    onNodeWithText("Error: Simulated crash").assertIsDisplayed()
}
