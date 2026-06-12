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

package androidx.compose.ui.test.failure

import android.util.Log
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.AndroidComposeUiTestTimeoutException
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.FailureArtifact
import androidx.compose.ui.test.FailureContext
import androidx.compose.ui.test.TestFailurePolicy.CaptureMode
import androidx.compose.ui.util.fastForEach
import kotlin.time.Duration

private const val TAG = "ComposeUiTest"

private fun CaptureMode.resolve(fallback: Boolean): Boolean =
    when (this) {
        CaptureMode.Enabled -> true
        CaptureMode.Disabled -> false
        CaptureMode.Unspecified -> fallback
        else -> fallback
    }

/**
 * Executes the test failure pipeline when a Compose UI test fails.
 *
 * This runner resolves the active TestFailurePolicy (evaluating local test configurations against
 * suite-level fallbacks), invokes the standard artifact capturers ([ScreenshotHandler] and
 * [UiHierarchyHandler]), and dispatches the resulting [FailureContext] to all registered
 * TestFailureHandlers.
 *
 * The pipeline guarantees that the root test failure (e.g., an `AssertionError` or timeout) is
 * never masked. Any secondary exceptions thrown during artifact IO or custom handler execution are
 * caught, logged, and attached as suppressed exceptions to the root [Throwable].
 */
@Suppress("VisibleForTests")
internal class FailurePipelineRunner(
    private val config: ComposeUiTestConfig,
    private val screenshotHandler: ScreenshotHandler = AndroidScreenshotHandler(),
    private val uiHierarchyHandler: UiHierarchyHandler = AndroidUiHierarchyHandler(),
) {
    fun runPipeline(throwable: Throwable, composeRoots: Set<ViewRootForTest>): Nothing {
        val error = throwable.wrapIfCoroutineTimeout(config.testTimeout)
        val artifacts = mutableListOf<FailureArtifact>()

        val policy = config.failurePolicy
        val fallbackArgs = AndroidTestConfigFallbacks.arguments

        val isScreenshotEnabled =
            policy.screenshotCaptureMode.resolve(fallbackArgs.isScreenshotEnabled)
        val isUiHierarchyEnabled =
            policy.uiHierarchyCaptureMode.resolve(fallbackArgs.isHierarchyEnabled)
        val failureHandlers = policy.failureHandlers

        val timeNs = System.nanoTime()
        if (isScreenshotEnabled) {
            val fileName = "${timeNs}_screenshot.png"
            executeSafely(error, "Failed to capture screenshot") {
                screenshotHandler.export(fileName)
                artifacts.add(FailureArtifact(FailureArtifact.Type.Screenshot, fileName))
            }
        }

        if (isUiHierarchyEnabled) {
            val fileName = "${timeNs}_ui.txt"
            executeSafely(error, "Failed to dump UI hierarchy") {
                uiHierarchyHandler.export(fileName, composeRoots)
                artifacts.add(FailureArtifact(FailureArtifact.Type.UiHierarchy, fileName))
            }
        }

        val context = FailureContext(error = error, artifacts = artifacts)
        failureHandlers.fastForEach { handler ->
            val handlerName = handler.javaClass.simpleName.ifEmpty { handler.javaClass.name }
            executeSafely(error, "Custom failure handler '$handlerName' threw an exception") {
                handler.onTestFailed(context)
            }
        }

        throw error
    }

    private inline fun executeSafely(error: Throwable, errorMessage: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            Log.e(TAG, errorMessage, t)
            error.addSuppressed(t)
        }
    }

    private fun Throwable.wrapIfCoroutineTimeout(timeout: Duration): Throwable {
        return if (this.javaClass.name == "kotlinx.coroutines.test.UncompletedCoroutinesError") {
            AndroidComposeUiTestTimeoutException(
                "runTest did not complete within the testTimeout of $timeout",
                this,
            )
        } else {
            this
        }
    }
}
