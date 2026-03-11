/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.test.v2

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MainTestClock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestResult

/**
 * Sets up the test environment, runs the given [test][block] and then tears down the test
 * environment. Use the methods on [ComposeUiTest] in the test to find Compose content and make
 * assertions on it. If you need access to platform specific elements (such as the Activity on
 * Android), use one of the platform specific variants of this method, e.g.
 * `runAndroidComposeUiTest` on Android.
 *
 * Implementations of this method will launch a Compose host (such as an Activity on Android) for
 * you. If your test needs to launch its own host, use a platform specific variant that doesn't
 * launch anything for you (if available), e.g. `runEmptyComposeUiTest` on Android. Always make sure
 * that the Compose content is set during execution of the [test lambda][block] so the test
 * framework is aware of the content. Whether you need to launch the host from within the test
 * lambda as well depends on the platform.
 *
 * This implementation uses [kotlinx.coroutines.test.StandardTestDispatcher] by default for running
 * composition. This ensures that the test behavior is consistent with
 * [kotlinx.coroutines.test.runTest] and provides explicit control over coroutine execution order.
 * This means you may need to explicitly advance time or run current coroutines when testing complex
 * coroutine logic, as tasks are queued on the scheduler rather than running eagerly.
 *
 * Keeping a reference to the [ComposeUiTest] outside of this function is an error.
 *
 * @sample androidx.compose.ui.test.samples.RunComposeUiTestSample
 * @param effectContext The [CoroutineContext] used to run the composition. The context for
 *   `LaunchedEffect`s and `rememberCoroutineScope` will be derived from this context. If this
 *   context contains a [TestDispatcher], it is used for composition and the [MainTestClock].
 *   Otherwise, a [kotlinx.coroutines.test.StandardTestDispatcher] is created and used. This new
 *   dispatcher will share the [TestCoroutineScheduler] from [effectContext] if one is present.
 * @param runTestContext The [CoroutineContext] used to create the context to run the test [block].
 *   By default [block] will run using [kotlinx.coroutines.test.StandardTestDispatcher].
 *   [runTestContext] and [effectContext] must not share [TestCoroutineScheduler].
 * @param testTimeout The [Duration] within which the test is expected to complete, otherwise a
 *   platform specific timeout exception will be thrown.
 * @param block The test function.
 */
@ExperimentalTestApi
expect fun runComposeUiTest(
    effectContext: CoroutineContext = EmptyCoroutineContext,
    runTestContext: CoroutineContext = EmptyCoroutineContext,
    testTimeout: Duration = 60.seconds,
    block: suspend ComposeUiTest.() -> Unit,
): TestResult
