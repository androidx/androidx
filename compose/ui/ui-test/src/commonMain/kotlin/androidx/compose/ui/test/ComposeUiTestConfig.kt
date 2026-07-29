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
import kotlin.jvm.JvmInline
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
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
 *   platform specific timeout exception will be thrown. Defaults to 60 seconds.
 * @property inputMode The [InputMode] to be used for the test. This determines how input events
 *   (such as touch or keyboard) are injected and handled during the test execution. Defaults to
 *   [InputMode.Touch].
 * @property failurePolicy The [TestFailurePolicy] used to configure the failure handling pipeline,
 *   such as capture modes for diagnostic artifacts (screenshots, UI hierarchy) and custom failure
 *   handlers. Defaults to [TestFailurePolicy].
 */
@Immutable
public expect class ComposeUiTestConfig(
    effectContext: CoroutineContext = EmptyCoroutineContext,
    runTestContext: CoroutineContext = EmptyCoroutineContext,
    testTimeout: Duration = 60.seconds,
    inputMode: InputMode = InputMode.Touch,
    failurePolicy: TestFailurePolicy = TestFailurePolicy(),
) {
    public val effectContext: CoroutineContext
    public val runTestContext: CoroutineContext
    public val testTimeout: Duration
    public val inputMode: InputMode
    public val failurePolicy: TestFailurePolicy

    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(
        effectContext: CoroutineContext = EmptyCoroutineContext,
        runTestContext: CoroutineContext = EmptyCoroutineContext,
        testTimeout: Duration = 60.seconds,
        inputMode: InputMode = InputMode.Touch,
    )
}

/**
 * Configuration for the failure handling pipeline in Compose UI tests.
 *
 * A [TestFailurePolicy] dictates what diagnostic artifacts the testing framework should capture
 * when a test fails (such as screenshots or UI tree dumps), and provides a mechanism to execute
 * custom [TestFailureHandler]s to process those artifacts or report the failure.
 *
 * By default, the capture modes are set to [CaptureMode.Unspecified]. This means the framework will
 * fall back to the suite-level runner configuration (e.g., Instrumentation arguments on Android) to
 * determine if artifacts should be generated. Setting a mode explicitly to [CaptureMode.Enabled] or
 * [CaptureMode.Disabled] will override the suite-level configuration for the specific test using
 * this policy.
 *
 * @property screenshotCaptureMode Determines whether a visual screenshot of the screen/UI should be
 *   captured upon failure.
 * @property uiHierarchyCaptureMode Determines whether a text-based dump of the UI and semantics
 *   trees should be captured upon failure.
 * @property failureHandlers A list of custom [TestFailureHandler]s that will be invoked in sequence
 *   after the framework completes its artifact generation.
 */
@Immutable
public expect class TestFailurePolicy(
    screenshotCaptureMode: CaptureMode = CaptureMode.Unspecified,
    uiHierarchyCaptureMode: CaptureMode = CaptureMode.Unspecified,
    failureHandlers: List<TestFailureHandler> = emptyList(),
) {
    public val screenshotCaptureMode: CaptureMode
    public val uiHierarchyCaptureMode: CaptureMode
    public val failureHandlers: List<TestFailureHandler>

    /**
     * Represents a tri-state flag for failure artifact captures, allowing individual test
     * configurations to explicitly override or fall back to suite-level runner arguments.
     *
     * This is used within [TestFailurePolicy] to dictate whether the test framework should capture
     * diagnostic artifacts (like screenshots or UI hierarchy dumps) when a test fails.
     */
    @JvmInline
    public value class CaptureMode private constructor(private val value: Int) {
        public companion object {
            /** Fall back to the suite-level runner configuration. */
            public val Unspecified: CaptureMode
            /** Explicitly enable the capture for this test, overriding runner configuration. */
            public val Enabled: CaptureMode
            /** Explicitly disable the capture for this test, overriding runner configuration. */
            public val Disabled: CaptureMode
        }
    }
}

/**
 * Represents a diagnostic artifact produced by the test failure pipeline when a Compose UI test
 * fails.
 *
 * Artifacts capture the state of the UI at the moment of failure, such as visual screenshots or
 * structural UI hierarchy dumps. Custom handlers implementing [TestFailureHandler] receive a list
 * of these artifacts within the [FailureContext].
 *
 * The [fileName] serves as an identifier to access the underlying file. The exact location and
 * lifecycle of this file depend on the specific platform's test storage mechanisms.
 *
 * **Android Platform:** On Android, the framework writes these artifacts to the
 * `PlatformTestStorage` provided by AndroidX Test. You can use the [fileName] to retrieve the
 * file's URI or read its bytes directly via the registry:
 *
 * @sample androidx.compose.ui.test.samples.failureArtifactStorageUsageSample
 * @property type The classification of the artifact, determining what kind of diagnostic data it
 *   contains (e.g., [Type.Screenshot] or [Type.UiHierarchy]).
 * @property fileName The platform-specific name of the generated artifact file.
 */
public class FailureArtifact(public val type: Type, public val fileName: String) {
    /** Defines the category of a [FailureArtifact]. */
    @JvmInline
    public value class Type private constructor(private val value: Int) {
        public companion object {
            public val Screenshot: Type = Type(0)
            public val UiHierarchy: Type = Type(1)
        }

        override fun toString(): String =
            when (this) {
                Screenshot -> "Type.Screenshot"
                UiHierarchy -> "Type.UiHierarchy"
                else -> "Unknown"
            }
    }
}

/**
 * A contextual object provided to [TestFailureHandler]s when a Compose UI test fails.
 *
 * This context encapsulates the environment of the failure, providing both the root exception that
 * triggered the failure and any diagnostic artifacts (such as screenshots or UI hierarchy dumps)
 * that were captured by the testing framework prior to invoking the custom handlers.
 *
 * @property error The original [Throwable] (e.g., `AssertionError` or timeout exception) that
 *   caused the test to fail.
 * @property artifacts A list of diagnostic [FailureArtifact] objects generated by the failure
 *   pipeline. Handlers can use this list to locate and process the generated diagnostic files on
 *   the host platform.
 */
public class FailureContext(
    public val error: Throwable,
    public val artifacts: List<FailureArtifact> = emptyList(),
)

/**
 * Handles Compose UI test failures for custom diagnostics or artifact processing.
 *
 * Implementations are registered in a [TestFailurePolicy] within a [ComposeUiTestConfig] and
 * execute in the order provided. If a handler throws an exception, the framework catches and
 * attaches it as a suppressed exception to [FailureContext.error], ensuring the original test
 * failure is never masked.
 *
 * Handlers execute synchronously on the test thread in a post-mortem state where the Compose
 * hierarchy, coroutine scopes, and UI registries have already been torn down. As a result,
 * interactive testing APIs like [ComposeUiTest.waitForIdle] or [onNodeWithTag] cannot be called
 * within a handler.
 *
 * Because handlers execute after the test timeout has elapsed, blocking calls such as file IO or
 * network operations can delay or stall the test runner indefinitely. Handlers that perform heavy
 * IO should enforce their own tight timeouts or dispatch work to background threads.
 *
 * @sample androidx.compose.ui.test.samples.testFailureHandlerSample
 * @see TestFailurePolicy
 * @see FailureContext
 */
public fun interface TestFailureHandler {
    /**
     * Invoked synchronously on the test thread when a Compose UI test fails.
     *
     * @param context The [FailureContext] containing the root [Throwable] and any generated
     *   [FailureArtifact]s.
     */
    public fun onTestFailed(context: FailureContext)
}
