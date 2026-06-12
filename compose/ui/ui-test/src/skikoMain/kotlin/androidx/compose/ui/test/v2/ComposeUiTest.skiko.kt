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

package androidx.compose.ui.test.v2

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.unit.Density
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest

/**
 * Sets up the test environment, runs the given [test][block] and then tears down the test
 * environment. Use the methods on [ComposeUiTest] in the test to find Compose content and make
 * assertions on it. If you need access to platform specific elements (such as the Activity on
 * Android), use one of the platform specific variants of this method, e.g.
 * [runAndroidComposeUiTest] on Android.
 *
 * Implementations of this method will launch a Compose host (such as an Activity on Android) for
 * you. If your test needs to launch its own host, use a platform specific variant that doesn't
 * launch anything for you (if available), e.g. [runEmptyComposeUiTest] on Android. Always make sure
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
 * Keeping a reference to the [ComposeUiTest] outside of this function is an error. Also avoid using
 * [androidx.compose.ui.test.junit4.ComposeTestRule] (e.g., createComposeRule) inside
 * [runComposeUiTest][block] or any of their respective variants. Since these APIs independently
 * manage the test environment, mixing them may lead to unexpected behavior.
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
 * @param block The suspendable test body.
 */
@ExperimentalTestApi
actual fun runComposeUiTest(
    effectContext: CoroutineContext,
    runTestContext: CoroutineContext,
    testTimeout: Duration,
    block: suspend ComposeUiTest.() -> Unit
): TestResult {
    return runSkikoComposeUiTest(
        effectContext = effectContext,
        runTestContext = runTestContext,
        testTimeout = testTimeout,
    ) {
        block()
    }
}

/**
 * Runs a Skiko-based Compose UI test within the specified configuration and test execution context.
 *
 * This implementation uses [kotlinx.coroutines.test.StandardTestDispatcher] by default for running
 * composition. This ensures that the test behavior is consistent with
 * [kotlinx.coroutines.test.runTest] and provides explicit control over coroutine execution order.
 * This means you may need to explicitly advance time or run current coroutines when testing complex
 * coroutine logic, as tasks are queued on the scheduler rather than running eagerly.
 *
 * @param size The dimensions of the test's virtual display, defaults to 1024x768 pixels.
 * @param density The screen density used in the test, defaults to a density of 1f.
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
 * @param block The suspendable test body.
 * @return A `TestResult` representing the outcome of the test execution.
 */
@OptIn(InternalTestApi::class, InternalComposeUiApi::class)
@ExperimentalTestApi
fun runSkikoComposeUiTest(
    size: Size = Size(1024.0f, 768.0f),
    density: Density = Density(1f),
    effectContext: CoroutineContext = EmptyCoroutineContext,
    runTestContext: CoroutineContext = EmptyCoroutineContext,
    testTimeout: Duration = Duration.INFINITE,
    block: suspend SkikoComposeUiTest.() -> Unit
): TestResult {
    return SkikoComposeUiTest(
        width = size.width.roundToInt(),
        height = size.height.roundToInt(),
        effectContext = effectContext,
        testTimeout = testTimeout,
        runTestContext = runTestContext,
        density = density,
        semanticsOwnerListener = null,
        windowInsets = null,
        useStandardTestDispatcherForComposition = true,
    ).runTest(block)
}

@InternalTestApi
@OptIn(ExperimentalTestApi::class, InternalComposeUiApi::class)
fun runInternalSkikoComposeUiTest(
    width: Int = 1024,
    height: Int = 768,
    density: Density = Density(1f),
    effectContext: CoroutineContext = EmptyCoroutineContext,
    runTestContext: CoroutineContext = EmptyCoroutineContext,
    testTimeout: Duration = Duration.INFINITE,
    semanticsOwnerListener: PlatformContext.SemanticsOwnerListener? = null,
    windowInsets: PlatformWindowInsets? = null,
    block: suspend SkikoComposeUiTest.() -> Unit,
): TestResult {
    return SkikoComposeUiTest(
        width = width,
        height = height,
        effectContext = effectContext,
        runTestContext = runTestContext,
        testTimeout = testTimeout,
        density = density,
        semanticsOwnerListener = semanticsOwnerListener,
        windowInsets = windowInsets,
        useStandardTestDispatcherForComposition = true,
    ).runTest(block)
}
