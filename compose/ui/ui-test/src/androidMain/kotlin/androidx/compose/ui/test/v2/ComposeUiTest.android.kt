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

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.getActivity
import androidx.test.core.app.ActivityScenario
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
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
 * This API differs from the deprecated API by using
 * [kotlinx.coroutines.test.StandardTestDispatcher] by default for running composition, instead of
 * [kotlinx.coroutines.test.UnconfinedTestDispatcher]. This ensures that the test behavior is
 * consistent with [kotlinx.coroutines.test.runTest] and provides explicit control over coroutine
 * execution order. This means you may need to explicitly advance time or run current coroutines
 * when testing complex coroutine logic, as tasks are queued on the scheduler rather than running
 * eagerly.
 *
 * Keeping a reference to the [ComposeUiTest] outside of this function is an error. Also avoid using
 * [ComposeTestRule] (e.g., createComposeRule) inside [runComposeUiTest][block] or any of their
 * respective variants. Since these APIs independently manage the test environment, mixing them may
 * lead to unexpected behavior.
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
@Suppress("RedundantUnitReturnType")
@ExperimentalTestApi
actual fun runComposeUiTest(
    effectContext: CoroutineContext,
    runTestContext: CoroutineContext,
    testTimeout: Duration,
    block: suspend ComposeUiTest.() -> Unit,
): TestResult {
    return runAndroidComposeUiTest(
        ComponentActivity::class.java,
        effectContext,
        runTestContext,
        testTimeout,
        block,
    )
}

/**
 * Variant of [runComposeUiTest] that allows you to specify which Activity should be launched. Be
 * aware that if the Activity [sets content][androidx.activity.compose.setContent] during its
 * launch, you cannot use [setContent][ComposeUiTest.setContent] on the ComposeUiTest anymore as
 * this would override the content and can lead to subtle bugs.
 *
 * This API differs from the deprecated API by using
 * [kotlinx.coroutines.test.StandardTestDispatcher] by default for running composition, instead of
 * [kotlinx.coroutines.test.UnconfinedTestDispatcher]. This ensures that the test behavior is
 * consistent with [kotlinx.coroutines.test.runTest] and provides explicit control over coroutine
 * execution order. This means you may need to explicitly advance time or run current coroutines
 * when testing complex coroutine logic, as tasks are queued on the scheduler rather than running
 * eagerly.
 *
 * Avoid using [ComposeTestRule] (e.g., createComposeRule) inside [runAndroidComposeUiTest][block]
 * or any of their respective variants. Since these APIs independently manage the test environment,
 * mixing them may lead to unexpected behavior.
 *
 * @param A The Activity type to be launched, which typically (but not necessarily) hosts the
 *   Compose content
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
@Suppress("RedundantUnitReturnType")
@ExperimentalTestApi
inline fun <reified A : ComponentActivity> runAndroidComposeUiTest(
    effectContext: CoroutineContext = EmptyCoroutineContext,
    runTestContext: CoroutineContext = EmptyCoroutineContext,
    testTimeout: Duration = 60.seconds,
    noinline block: suspend AndroidComposeUiTest<A>.() -> Unit,
): TestResult {
    return runAndroidComposeUiTest(A::class.java, effectContext, runTestContext, testTimeout, block)
}

/**
 * Variant of [runComposeUiTest] that allows you to specify which Activity should be launched. Be
 * aware that if the Activity [sets content][androidx.activity.compose.setContent] during its
 * launch, you cannot use [setContent][ComposeUiTest.setContent] on the ComposeUiTest anymore as
 * this would override the content and can lead to subtle bugs.
 *
 * This API differs from the deprecated API by using
 * [kotlinx.coroutines.test.StandardTestDispatcher] by default for running composition, instead of
 * [kotlinx.coroutines.test.UnconfinedTestDispatcher]. This ensures that the test behavior is
 * consistent with [kotlinx.coroutines.test.runTest] and provides explicit control over coroutine
 * execution order. This means you may need to explicitly advance time or run current coroutines
 * when testing complex coroutine logic, as tasks are queued on the scheduler rather than running
 * eagerly.
 *
 * Avoid using [ComposeTestRule] (e.g., createComposeRule) inside [runAndroidComposeUiTest][block]
 * or any of their respective variants. Since these APIs independently manage the test environment,
 * mixing them may lead to unexpected behavior.
 *
 * @param A The Activity type to be launched, which typically (but not necessarily) hosts the
 *   Compose content
 * @param activityClass The [Class] of the Activity type to be launched, corresponding to [A].
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
@Suppress("RedundantUnitReturnType")
@ExperimentalTestApi
fun <A : ComponentActivity> runAndroidComposeUiTest(
    activityClass: Class<A>,
    effectContext: CoroutineContext = EmptyCoroutineContext,
    runTestContext: CoroutineContext = EmptyCoroutineContext,
    testTimeout: Duration = 60.seconds,
    block: suspend AndroidComposeUiTest<A>.() -> Unit,
): TestResult {
    // Don't start the scenario now, wait until we're inside runTest { },
    // in case the Activity's onCreate/Start/Resume calls setContent
    var scenario: ActivityScenario<A>? = null
    val environment =
        AndroidComposeUiTestEnvironment(
            effectContext = effectContext,
            runTestContext = runTestContext,
            testTimeout = testTimeout,
        ) {
            requireNotNull(scenario) {
                    "ActivityScenario has not yet been launched, or has already finished. Make sure that " +
                        "any call to ComposeUiTest.setContent() and AndroidComposeUiTest.getActivity() " +
                        "is made within the lambda passed to AndroidComposeUiTestEnvironment.runTest()"
                }
                .getActivity()
        }
    try {
        return environment.runTest {
            scenario = ActivityScenario.launch(activityClass)
            var blockException: Throwable? = null
            try {
                // Run the test
                block()
            } catch (t: Throwable) {
                blockException = t
            }

            // Throw the aggregate exception. May be from the test body or from the cleanup.
            blockException?.let { throw it }
        }
    } finally {
        // Close the scenario outside runTest to avoid getting stuck.
        //
        // ActivityScenario.close() calls Instrumentation.waitForIdleSync(), which would time out
        // if there is an infinite self-invalidating measure, layout, or draw loop. If the
        // Compose content was set through the test's setContent method, it will remove the
        // AndroidComposeView from the view hierarchy which breaks this loop, which is why we
        // call close() outside the runTest lambda. This will not help if the content is not set
        // through the test's setContent method though, in which case we'll still time out here.
        scenario?.close()
    }
}

/**
 * Variant of [runComposeUiTest] that does not launch an Activity to host Compose content in and
 * thus acts as an "empty shell". Use this if you need to have control over the timing and method of
 * launching the Activity, for example when you want to launch it with a custom Intent, or if you
 * have a complex test setup.
 *
 * This API differs from the deprecated API by using
 * [kotlinx.coroutines.test.StandardTestDispatcher] by default for running composition, instead of
 * [kotlinx.coroutines.test.UnconfinedTestDispatcher]. This ensures that the test behavior is
 * consistent with [kotlinx.coroutines.test.runTest] and provides explicit control over coroutine
 * execution order. This means you may need to explicitly advance time or run current coroutines
 * when testing complex coroutine logic, as tasks are queued on the scheduler rather than running
 * eagerly.
 *
 * When using this method, calling [ComposeUiTest.setContent] will throw an IllegalStateException.
 * Instead, you'll have to set the content in the Activity that you have launched yourself, either
 * directly on the Activity or on an [androidx.compose.ui.platform.AbstractComposeView]. You will
 * need to do this from within the [test lambda][block], or the test framework will not be able to
 * find the content.
 *
 * Avoid using [ComposeTestRule] (e.g., createComposeRule) inside [runEmptyComposeUiTest][block] or
 * any of their respective variants. Since these APIs independently manage the test environment,
 * mixing them may lead to unexpected behavior.
 */
@Suppress("RedundantUnitReturnType")
@ExperimentalTestApi
fun runEmptyComposeUiTest(block: ComposeUiTest.() -> Unit): TestResult {
    return AndroidComposeUiTestEnvironment {
            error(
                "runEmptyComposeUiTest {} does not provide an Activity to set Compose content in. " +
                    "Launch and use the Activity yourself within the lambda passed to " +
                    "runEmptyComposeUiTest {}, or use runAndroidComposeUiTest {}"
            )
        }
        .runTest(block)
}

/**
 * Creates an [AndroidComposeUiTestEnvironment] that retrieves the
 * [host Activity][AndroidComposeUiTest.activity] by delegating to the given [activityProvider]. Use
 * this if you need to launch an Activity in a way that is not compatible with any of the existing
 * [runComposeUiTest], [runAndroidComposeUiTest], or [runEmptyComposeUiTest] methods.
 *
 * This API differs from the deprecated API by using
 * [kotlinx.coroutines.test.StandardTestDispatcher] by default for running composition, instead of
 * [kotlinx.coroutines.test.UnconfinedTestDispatcher]. This ensures that the test behavior is
 * consistent with [kotlinx.coroutines.test.runTest] and provides explicit control over coroutine
 * execution order. This means you may need to explicitly advance time or run current coroutines
 * when testing complex coroutine logic, as tasks are queued on the scheduler rather than running
 * eagerly.
 *
 * Valid use cases include, but are not limited to, creating your own JUnit test rule that
 * implements [AndroidComposeUiTest] by delegating to
 * [androidx.compose.ui.test.AndroidComposeUiTestEnvironment.test]. See
 * [AndroidComposeTestRule][androidx.compose.ui.test.junit4.AndroidComposeTestRule] for a reference
 * implementation.
 *
 * The [activityProvider] is called every time [activity][AndroidComposeUiTest.activity] is called,
 * which in turn is called when [setContent][ComposeUiTest.setContent] is called.
 *
 * The most common implementation of an [activityProvider] retrieves the activity from a backing
 * [ActivityScenario] (that the caller launches _within_ the lambda passed to [runTest]), but one is
 * not limited to this pattern.
 *
 * @param activityProvider A lambda that should return the current Activity instance of type [A], if
 *   it is available. If it is not available, it should return `null`.
 * @param A The Activity type to be interacted with, which typically (but not necessarily) is the
 *   activity that was launched and hosts the Compose content.
 * @param effectContext The [CoroutineContext] used to run the composition. The context for
 *   `LaunchedEffect`s and `rememberCoroutineScope` will be derived from this context. If this
 *   context contains a [TestDispatcher], it is used for composition and the [MainTestClock].
 *   Otherwise, a [kotlinx.coroutines.test.StandardTestDispatcher] is created and used. This new
 *   dispatcher will share the [TestCoroutineScheduler] from [effectContext] if one is present.
 * @param runTestContext The [CoroutineContext] used to create the context to run the test. By
 *   default it will run using [kotlinx.coroutines.test.StandardTestDispatcher]. [runTestContext]
 *   and [effectContext] must not share [TestCoroutineScheduler].
 * @param testTimeout The [Duration] within which the test is expected to complete, otherwise a
 *   platform specific timeout exception will be thrown.
 */
@ExperimentalTestApi
inline fun <A : ComponentActivity> AndroidComposeUiTestEnvironment(
    effectContext: CoroutineContext = EmptyCoroutineContext,
    runTestContext: CoroutineContext = EmptyCoroutineContext,
    testTimeout: Duration = 60.seconds,
    crossinline activityProvider: () -> A?,
): androidx.compose.ui.test.AndroidComposeUiTestEnvironment<A> {
    return object :
        androidx.compose.ui.test.AndroidComposeUiTestEnvironment<A>(
            effectContext = effectContext,
            runTestContext = runTestContext,
            testTimeout = testTimeout,
        ) {
        override val activity: A?
            get() = activityProvider.invoke()
    }
}
