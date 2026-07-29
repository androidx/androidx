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

package androidx.compose.ui.test

import androidx.compose.runtime.Immutable
import androidx.compose.ui.input.InputMode
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher

/**
 * Defines the configuration requirements for a Compose test environment.
 *
 * This configuration allows for fine-grained control over the test execution environment, including
 * the coroutine contexts used for composition and test execution, the overall test timeout, and the
 * initial input mode.
 *
 * @property effectContext The [CoroutineContext] used to run the composition. The context for
 *   `LaunchedEffect`s and `rememberCoroutineScope` will be derived from this context. If this
 *   context contains a [TestDispatcher] or [TestCoroutineScheduler] (in that order), it will be
 *   used for composition and the [androidx.compose.ui.test.MainTestClock]. Defaults to
 *   [EmptyCoroutineContext].
 * @property runTestContext The [CoroutineContext] used to create the context to run the test block.
 *   By default, test block will run using [kotlinx.coroutines.test.StandardTestDispatcher].
 *   [runTestContext] and [effectContext] must not share [TestCoroutineScheduler]. Defaults to
 *   [EmptyCoroutineContext].
 * @property testTimeout The [Duration] within which the test is expected to complete, otherwise a
 *   platform specific timeout exception will be thrown. Defaults to `60 seconds`.
 * @property inputMode The [InputMode] to be used for the test. This determines how input events
 *   (such as touch or keyboard) are injected and handled during the test execution. Defaults to
 *   [InputMode.Touch].
 * @property failurePolicy The [TestFailurePolicy] used to configure the failure handling pipeline,
 *   such as capture modes for diagnostic artifacts (screenshots, UI hierarchy) and custom failure
 *   handlers. Defaults to [TestFailurePolicy].
 */
@Immutable
public actual class ComposeUiTestConfig
public actual constructor(
    public actual val effectContext: CoroutineContext,
    public actual val runTestContext: CoroutineContext,
    public actual val testTimeout: Duration,
    public actual val inputMode: InputMode,
    public actual val failurePolicy: TestFailurePolicy,
) {
    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    public actual constructor(
        effectContext: CoroutineContext,
        runTestContext: CoroutineContext,
        testTimeout: Duration,
        inputMode: InputMode,
    ) : this(
        effectContext = effectContext,
        runTestContext = runTestContext,
        testTimeout = testTimeout,
        inputMode = inputMode,
        failurePolicy = TestFailurePolicy(),
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ComposeUiTestConfig) return false

        if (effectContext != other.effectContext) return false
        if (runTestContext != other.runTestContext) return false
        if (testTimeout != other.testTimeout) return false
        if (inputMode != other.inputMode) return false
        if (failurePolicy != other.failurePolicy) return false

        return true
    }

    override fun hashCode(): Int {
        var result = effectContext.hashCode()
        result = 31 * result + runTestContext.hashCode()
        result = 31 * result + testTimeout.hashCode()
        result = 31 * result + inputMode.hashCode()
        result = 31 * result + failurePolicy.hashCode()
        return result
    }
}

/**
 * Configuration for the failure handling pipeline in Compose UI tests.
 *
 * A [TestFailurePolicy] dictates what diagnostic artifacts the testing framework should capture
 * when a test fails (such as screenshots or UI tree dumps), and provides a mechanism to execute
 * custom [TestFailureHandler]s to process those artifacts or report the failure.
 *
 * By default, the capture modes are set to [CaptureMode.Unspecified]. This means the framework will
 * fall back to the suite-level runner configuration to determine if artifacts should be generated.
 * Setting a mode explicitly to [CaptureMode.Enabled] or [CaptureMode.Disabled] will override the
 * suite-level configuration for the specific test using this policy.
 *
 * On Android, when a mode is [CaptureMode.Unspecified], the framework falls back to reading
 * suite-level arguments from the `InstrumentationRegistry`. You can configure these globally in
 * your `build.gradle` via `testInstrumentationRunnerArguments`:
 * - `androidx.compose.ui.test.failure.isScreenshotCaptureEnabled` (true/false)
 * - `androidx.compose.ui.test.failure.isUiHierarchyCaptureEnabled` (true/false)
 *
 * @property screenshotCaptureMode Determines whether a visual screenshot of the screen/UI should be
 *   captured upon failure.
 * @property uiHierarchyCaptureMode Determines whether a text-based dump of the UI and semantics
 *   trees should be captured upon failure.
 * @property failureHandlers A list of custom [TestFailureHandler]s that will be invoked in sequence
 *   after the framework completes its standard artifact generation.
 */
@Immutable
public actual class TestFailurePolicy
public actual constructor(
    public actual val screenshotCaptureMode: CaptureMode,
    public actual val uiHierarchyCaptureMode: CaptureMode,
    public actual val failureHandlers: List<TestFailureHandler>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TestFailurePolicy) return false

        if (screenshotCaptureMode != other.screenshotCaptureMode) return false
        if (uiHierarchyCaptureMode != other.uiHierarchyCaptureMode) return false
        if (failureHandlers != other.failureHandlers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = screenshotCaptureMode.hashCode()
        result = 31 * result + uiHierarchyCaptureMode.hashCode()
        result = 31 * result + failureHandlers.hashCode()
        return result
    }

    /**
     * Represents a tri-state flag for failure artifact captures, allowing individual test
     * configurations to explicitly override or fall back to suite-level runner arguments.
     *
     * This is used within [TestFailurePolicy] to dictate whether the test framework should capture
     * diagnostic artifacts (like screenshots or UI hierarchy dumps) when a test fails.
     */
    @JvmInline
    public actual value class CaptureMode private actual constructor(private val value: Int) {
        public actual companion object {
            /** Fall back to the suite-level runner configuration. */
            public actual val Unspecified: CaptureMode = CaptureMode(0)
            /** Explicitly enable the capture for this test, overriding runner configuration. */
            public actual val Enabled: CaptureMode = CaptureMode(1)
            /** Explicitly disable the capture for this test, overriding runner configuration. */
            public actual val Disabled: CaptureMode = CaptureMode(2)
        }

        override fun toString(): String =
            when (this) {
                Unspecified -> "CaptureMode.Unspecified"
                Enabled -> "CaptureMode.Enabled"
                Disabled -> "CaptureMode.Disabled"
                else -> "CaptureMode(value=$value)"
            }
    }
}
