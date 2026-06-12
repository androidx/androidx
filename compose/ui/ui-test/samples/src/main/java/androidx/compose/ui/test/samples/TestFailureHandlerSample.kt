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

package androidx.compose.ui.test.samples

import androidx.annotation.Sampled
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.FailureArtifact
import androidx.compose.ui.test.TestFailureHandler
import androidx.compose.ui.test.TestFailurePolicy
import androidx.compose.ui.test.TestFailurePolicy.CaptureMode
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.platform.io.PlatformTestStorageRegistry

@Sampled
fun testFailureHandlerSample() {
    val customFailureHandler = TestFailureHandler { context ->
        val storage = PlatformTestStorageRegistry.getInstance()

        context.artifacts.forEach { artifact ->
            when (artifact.type) {
                FailureArtifact.Type.Screenshot -> {
                    // Example: Read the screenshot bytes to upload to a custom dashboard
                    // val inputStream = storage.openInputFile(artifact.fileName)
                }
                FailureArtifact.Type.UiHierarchy -> {
                    // Example: Get the URI to share or process further
                    // val uri = storage.getOutputFileUri(artifact.fileName)
                }
            }
        }
    }

    val testConfig =
        ComposeUiTestConfig(
            failurePolicy =
                TestFailurePolicy(
                    screenshotCaptureMode = CaptureMode.Enabled,
                    uiHierarchyCaptureMode = CaptureMode.Enabled,
                    failureHandlers = listOf(customFailureHandler),
                )
        )

    runComposeUiTest(config = testConfig) {
        setContent { /* Your Compose UI here */ }

        // If this assertion fails, the framework will:
        // 1. Take a screenshot
        // 2. Dump the UI hierarchy
        // 3. Call customFailureHandler.onTestFailed
        onNodeWithTag("non_existent_button").assertExists()
    }
}
