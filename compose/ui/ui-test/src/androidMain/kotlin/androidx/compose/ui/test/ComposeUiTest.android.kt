/*
 * Copyright 2023 The Android Open Source Project
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

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.node.RootForTest.UncaughtExceptionHandler
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.platform.WindowRecomposerPolicy
import androidx.compose.ui.test.ComposeRootRegistry.OnRegistrationChangedListener
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import java.io.PrintStream
import java.io.PrintWriter
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

@ExperimentalTestApi
@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message = "Replaced with same function, but with suspend block, runTextContext, testTimeout",
)
@JvmName("runComposeUiTest")
fun runComposeUiTestNonSuspendingLambda(
    effectContext: CoroutineContext = EmptyCoroutineContext,
    block: ComposeUiTest.() -> Unit,
) {
    runAndroidComposeUiTest(
        activityClass = ComponentActivity::class.java,
        effectContext = effectContext,
        runTestContext = EmptyCoroutineContext,
        testTimeout = Duration.INFINITE,
    ) {
        block()
    }
}

@ExperimentalTestApi
@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message = "Replaced with same function, but with suspend block, runTextContext, testTimeout",
)
@JvmName("runAndroidComposeUiTest")
fun <A : ComponentActivity> runAndroidComposeUiTestNonSuspendingLambda(
    activityClass: Class<A>,
    effectContext: CoroutineContext = EmptyCoroutineContext,
    block: AndroidComposeUiTest<A>.() -> Unit,
) {
    runAndroidComposeUiTest(
        activityClass = activityClass,
        effectContext = effectContext,
        runTestContext = EmptyCoroutineContext,
        testTimeout = Duration.INFINITE,
    ) {
        block()
    }
}

@ExperimentalTestApi
@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message = "Replaced with same function, but with suspend block, runTextContext, testTimeout",
)
@JvmName("runAndroidComposeUiTest")
inline fun <reified A : ComponentActivity> runAndroidComposeUiTestNonSuspendingLambda(
    effectContext: CoroutineContext = EmptyCoroutineContext,
    noinline block: AndroidComposeUiTest<A>.() -> Unit,
) {
    runAndroidComposeUiTest(
        activityClass = A::class.java,
        effectContext = effectContext,
        runTestContext = EmptyCoroutineContext,
        testTimeout = Duration.INFINITE,
    ) {
        block()
    }
}

@ExperimentalTestApi
@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message = "Replaced with same function, but with runTextContext and testTimeout",
)
@JvmName("AndroidComposeUiTestEnvironment")
inline fun <A : ComponentActivity> AndroidComposeUiTestEnvironmentNoSuspendingLambda(
    effectContext: CoroutineContext = EmptyCoroutineContext,
    crossinline activityProvider: () -> A?,
): AndroidComposeUiTestEnvironment<A> {
    return AndroidComposeUiTestEnvironment(
        effectContext,
        EmptyCoroutineContext,
        Duration.INFINITE,
        activityProvider,
    )
}

/**
 * @param effectContext The [CoroutineContext] used to run the composition. The context for
 *   `LaunchedEffect`s and `rememberCoroutineScope` will be derived from this context. If this
 *   context contains a [TestDispatcher] or [TestCoroutineScheduler] (in that order), it will be
 *   used for composition and the [MainTestClock].
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
 * @param A The Activity type to be launched, which typically (but not necessarily) hosts the
 *   Compose content
 * @param effectContext The [CoroutineContext] used to run the composition. The context for
 *   `LaunchedEffect`s and `rememberCoroutineScope` will be derived from this context. If this
 *   context contains a [TestDispatcher] or [TestCoroutineScheduler] (in that order), it will be
 *   used for composition and the [MainTestClock].
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
 * @param A The Activity type to be launched, which typically (but not necessarily) hosts the
 *   Compose content
 * @param activityClass The [Class] of the Activity type to be launched, corresponding to [A].
 * @param effectContext The [CoroutineContext] used to run the composition. The context for
 *   `LaunchedEffect`s and `rememberCoroutineScope` will be derived from this context. If this
 *   context contains a [TestDispatcher] or [TestCoroutineScheduler] (in that order), it will be
 *   used for composition and the [MainTestClock].
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
 * When using this method, calling [ComposeUiTest.setContent] will throw an IllegalStateException.
 * Instead, you'll have to set the content in the Activity that you have launched yourself, either
 * directly on the Activity or on an [androidx.compose.ui.platform.AbstractComposeView]. You will
 * need to do this from within the [test lambda][block], or the test framework will not be able to
 * find the content.
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
 * Variant of [ComposeUiTest] for when you want to have access the current [activity] of type [A].
 * The activity might not always be available, for example if the test navigates to another
 * activity. In such cases, [activity] will return `null`.
 *
 * An instance of [AndroidComposeUiTest] can be obtained by calling [runAndroidComposeUiTest], the
 * argument to which will have it as the receiver scope.
 *
 * Note that any Compose content can be found and tested, regardless if it is hosted by [activity]
 * or not. What is important, is that the content is set _during_ the lambda passed to
 * [runAndroidComposeUiTest] (not before, and not after), and that the activity that is actually
 * hosting the Compose content is in resumed state.
 *
 * @param A The Activity type to be interacted with, which typically (but not necessarily) is the
 *   activity that was launched and hosts the Compose content
 */
@ExperimentalTestApi
sealed interface AndroidComposeUiTest<A : ComponentActivity> : ComposeUiTest {
    /**
     * Returns the current activity of type [A] used in this [ComposeUiTest]. If no such activity is
     * available, for example if you've navigated to a different activity and the original host has
     * now been destroyed, this will return `null`.
     *
     * Note that you should never hold on to a reference to the Activity, always use [activity] to
     * interact with the Activity.
     */
    val activity: A?
}

/**
 * Creates an [AndroidComposeUiTestEnvironment] that retrieves the
 * [host Activity][AndroidComposeUiTest.activity] by delegating to the given [activityProvider]. Use
 * this if you need to launch an Activity in a way that is not compatible with any of the existing
 * [runComposeUiTest], [runAndroidComposeUiTest], or [runEmptyComposeUiTest] methods.
 *
 * Valid use cases include, but are not limited to, creating your own JUnit test rule that
 * implements [AndroidComposeUiTest] by delegating to [AndroidComposeUiTestEnvironment.test]. See
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
 *   context contains a [TestDispatcher] or [TestCoroutineScheduler] (in that order), it will be
 *   used for composition and the [MainTestClock].
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
): AndroidComposeUiTestEnvironment<A> {
    return object : AndroidComposeUiTestEnvironment<A>(effectContext, runTestContext, testTimeout) {
        override val activity: A?
            get() = activityProvider.invoke()
    }
}

/**
 * A test environment that can [run tests][runTest] using the [test receiver scope][test]. Note that
 * some of the properties and methods on [test] will only work during the call to [runTest], as they
 * require that the environment has been set up.
 *
 * If the [effectContext] contains a [TestDispatcher], that dispatcher will be used to run
 * composition on and its [TestCoroutineScheduler] will be used to construct the [MainTestClock]. If
 * the `effectContext` does not contain a `TestDispatcher`, an [UnconfinedTestDispatcher] will be
 * created, using the `TestCoroutineScheduler` from the `effectContext` if present.
 *
 * @param A The Activity type to be interacted with, which typically (but not necessarily) is the
 *   activity that was launched and hosts the Compose content.
 * @param effectContext The [CoroutineContext] used to run the composition. The context for
 *   `LaunchedEffect`s and `rememberCoroutineScope` will be derived from this context. If this
 *   context contains a [TestDispatcher] or [TestCoroutineScheduler] (in that order), it will be
 *   used for composition and the [MainTestClock].
 * @param runTestContext The [CoroutineContext] used to create the context to run the test. By
 *   default it will run using [kotlinx.coroutines.test.StandardTestDispatcher]. [runTestContext]
 *   and [effectContext] must not share [TestCoroutineScheduler].
 * @param testTimeout The [Duration] within which the test is expected to complete, otherwise a
 *   platform specific timeout exception will be thrown.
 */
@ExperimentalTestApi
@OptIn(ExperimentalCoroutinesApi::class)
abstract class AndroidComposeUiTestEnvironment<A : ComponentActivity>(
    private val effectContext: CoroutineContext = EmptyCoroutineContext,
    private val runTestContext: CoroutineContext = EmptyCoroutineContext,
    private val testTimeout: Duration = 60.seconds,
) {

    @Suppress("unused")
    constructor(
        effectContext: CoroutineContext = EmptyCoroutineContext
    ) : this(effectContext, EmptyCoroutineContext, 60.seconds)

    /**
     * Returns the current host activity of type [A]. If no such activity is available, for example
     * if you've navigated to a different activity and the original host has now been destroyed,
     * this will return `null`.
     */
    protected abstract val activity: A?

    private val idlingResourceRegistry = IdlingResourceRegistry()

    internal val composeRootRegistry = ComposeRootRegistry()

    private val mainClockImpl: MainTestClockImpl
    private lateinit var composeIdlingResource: ComposeIdlingResource
    private var idlingStrategy: IdlingStrategy = EspressoLink(idlingResourceRegistry)

    private lateinit var recomposer: Recomposer

    private val customTestDispatcher: TestDispatcher? =
        effectContext[ContinuationInterceptor] as? TestDispatcher

    /**
     * We can only accept a TestDispatcher here because we need to access its scheduler. Use the
     * TestDispatcher if it is provided in the effectContext Otherwise, use the
     * TestCoroutineScheduler if it is provided
     */
    private val compositionCoroutineDispatcher: TestDispatcher =
        customTestDispatcher ?: UnconfinedTestDispatcher(effectContext[TestCoroutineScheduler])

    /**
     * This flag is set to `false` when a custom `TestDispatcher` (including
     * `UnconfinedTestDispatcher`) is provided to the `ComposeTestRule`.
     */
    private val isDefaultTestDispatcherUsed: Boolean
        get() = customTestDispatcher == null

    /**
     * This enables a compatibility layer to support the `StandardTestDispatcher` behavior for
     * tests.
     */
    private val isStandardTestDispatcherSupportEnabled: Boolean =
        !isDefaultTestDispatcherUsed && ComposeUiTestFlags.isStandardTestDispatcherSupportEnabled

    private val frameClockCoroutineScope = TestScope(compositionCoroutineDispatcher)
    private lateinit var recomposerCoroutineScope: CoroutineScope
    private val coroutineExceptionHandler =
        UncaughtExceptionHandler(effectContext[CoroutineExceptionHandler])

    private val frameClock: TestMonotonicFrameClock
    private val recomposerContinuationInterceptor: ApplyingContinuationInterceptor
    private val infiniteAnimationPolicy: InfiniteAnimationPolicy
    private val combinedRunTestCoroutineContext: CoroutineContext

    private var pendingThrowable: Throwable? = null

    init {
        frameClock =
            TestMonotonicFrameClock(
                frameClockCoroutineScope,
                // This callback will get run at the same time, relative to frame callbacks and
                // coroutine resumptions, as the Choreographer's perform traversal frame, where it
                // runs
                // layout and draw passes. We use it to run layout passes manually when executing
                // frames
                // during a waitForIdle, during which the Choreographer isn't in control.
                onPerformTraversals = {
                    composeRootRegistry.getRegisteredComposeRoots().forEach {
                        it.measureAndLayoutForTest()
                    }
                },
            )
        // The applying interceptor needs to be the outermost wrapper since TestMonotonicFrameClock
        // will not delegate if the dispatcher dispatch is not needed at the time of intercept.
        recomposerContinuationInterceptor =
            ApplyingContinuationInterceptor(frameClock.continuationInterceptor)

        mainClockImpl =
            MainTestClockImpl(
                scheduler = compositionCoroutineDispatcher.scheduler,
                frameClock = frameClock,
                isStandardTestDispatcherSupportEnabled = isStandardTestDispatcherSupportEnabled,
            )

        infiniteAnimationPolicy =
            object : InfiniteAnimationPolicy {
                override suspend fun <R> onInfiniteOperation(block: suspend () -> R): R {
                    if (mainClockImpl.autoAdvance) {
                        throw CancellationException("Infinite animations are disabled on tests")
                    }
                    return block()
                }
            }

        createRecomposer()

        @OptIn(kotlin.ExperimentalStdlibApi::class)
        val testDispatcher =
            runTestContext[CoroutineDispatcher] as? TestDispatcher ?: StandardTestDispatcher()

        combinedRunTestCoroutineContext =
            recomposer.effectCoroutineContext
                .minusKey(CoroutineExceptionHandler.Key)
                .minusKey(Job.Key)
                .minusKey(TestCoroutineScheduler.Key)
                .plus(runTestContext)
                .plus(testDispatcher)
    }

    private fun createRecomposer() {
        recomposerCoroutineScope =
            CoroutineScope(
                effectContext +
                    recomposerContinuationInterceptor +
                    frameClock +
                    infiniteAnimationPolicy +
                    coroutineExceptionHandler +
                    Job()
            )
        recomposer = Recomposer(recomposerCoroutineScope.coroutineContext)

        composeIdlingResource =
            ComposeIdlingResource(
                composeRootRegistry,
                mainClockImpl,
                recomposer,
                isStandardTestDispatcherSupportEnabled,
            )
    }

    /**
     * Recreates the CoroutineContext associated with Compose being cancelled. This happens when an
     * app moves from a regular ("Full screen") view of the app to a "Pop up" view AND certain
     * properties in the manifest's android:configChanges are set to prevent a full tear down of the
     * app. This is a somewhat rare case (see issuetracker.google.com/issues/309326720 for more
     * details).
     *
     * It does this by canceling the existing Recomposer and creates a new Recomposer (including a
     * new recomposer coroutine scope for that new Recomposer) and a new ComposeIdlingResource.
     *
     * To see full test:
     * click_viewAddedAndRemovedWithRecomposerCancelledAndRecreated_clickStillWorks
     */
    fun cancelAndRecreateRecomposer() {
        recomposer.cancel()
        createRecomposer()
    }

    internal val testReceiverScope = AndroidComposeUiTestImpl()
    private val testOwner = AndroidTestOwner()
    private val testContext = TestContext(testOwner)

    /**
     * The receiver scope of the test passed to [runTest]. Note that some of the properties and
     * methods will only work during the call to [runTest], as they require that the environment has
     * been set up.
     */
    val test: AndroidComposeUiTest<A> = testReceiverScope

    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "Replace with the same function, but with suspend block",
    )
    @JvmName("runTest") // for binary compatibility
    fun <R> runTestNonSuspendingLambda(block: AndroidComposeUiTest<A>.() -> R?): R? {
        var result: R? = null
        runTest { result = block() }
        return result
    }

    /**
     * Runs the given [block], setting up all test hooks before running the test and tearing them
     * down after running the test.
     */
    fun <R> runTest(block: suspend AndroidComposeUiTest<A>.() -> R): TestResult =
        runCatching {
                kotlinx.coroutines.test.runTest(
                    context = combinedRunTestCoroutineContext,
                    timeout = testTimeout,
                ) {
                    if (HasRobolectricFingerprint) {
                        idlingStrategy =
                            RobolectricIdlingStrategy(composeRootRegistry, composeIdlingResource)
                    }
                    // Need to await quiescence before registering our ComposeIdlingResource because
                    // the
                    // host activity might still be launching. If it is going to set compose
                    // content,
                    // we want that to happen before we install our hooks to avoid a race.
                    idlingStrategy.runUntilIdle()

                    composeRootRegistry.withRegistry {
                        idlingResourceRegistry.withRegistry {
                            idlingStrategy.withStrategy {
                                withTestCoroutines {
                                    withWindowRecomposer {
                                        withComposeIdlingResource { testReceiverScope.block() }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .onFailure { throwable ->
                if (
                    throwable.javaClass.name == "kotlinx.coroutines.test.UncompletedCoroutinesError"
                ) {
                    throw AndroidComposeUiTestTimeoutException(
                            "runTest did not complete within the testTimeout of $testTimeout",
                            throwable,
                        )
                        .also { it.addSuppressed(throwable) }
                } else {
                    throw throwable
                }
            }
            .getOrNull() ?: error("runTest failed with an unhandled exception")

    private fun waitForIdle(atLeastOneRootExpected: Boolean) {
        // First wait until we have a compose root (in case an Activity is being started)
        composeRootRegistry.waitForComposeRoots(atLeastOneRootExpected)
        // Then await composition(s)
        idlingStrategy.runUntilIdle()
        throwPendingException()
        // Then wait for the next frame to ensure any scheduled drawing has completed
        waitForNextChoreographerFrame()
        // Check if a coroutine threw an uncaught exception
        coroutineExceptionHandler.throwUncaught()
    }

    private fun waitForNextChoreographerFrame() {
        val view =
            composeRootRegistry
                .getRegisteredComposeRoots()
                .map { it.view.rootView }
                .firstOrNull { it.isAttachedToWindow }
        if (view != null) {
            var frameHit = false
            // The animation callback is called before draw, so post a message from the callback
            // that will be executed after draw happened
            view.postOnAnimation { view.post { frameHit = true } }
            while (!frameHit) {
                idlingStrategy.runUntilIdle()
                throwPendingException()
            }
        }
    }

    private inline fun <R> withWindowRecomposer(block: () -> R): R {
        val rootRegistrationListener =
            object : OnRegistrationChangedListener {
                val uncaughtExceptionHandler =
                    object : UncaughtExceptionHandler {
                        override fun onUncaughtException(t: Throwable) {
                            pendingThrowable =
                                pendingThrowable?.apply {
                                    addCascadingErrorHeaderIfAbsent()
                                    addSuppressed(t)
                                } ?: t
                        }
                    }

                override fun onRegistrationChanged(
                    composeRoot: ViewRootForTest,
                    registered: Boolean,
                ) {
                    composeRoot.setUncaughtExceptionHandler(
                        if (registered) {
                            uncaughtExceptionHandler
                        } else {
                            null
                        }
                    )
                }
            }

        @OptIn(InternalComposeUiApi::class)
        return WindowRecomposerPolicy.withFactory({ recomposer }) {
            try {
                // Install an uncaught exception handler into every Composition in the test
                composeRootRegistry.addOnRegistrationChangedListener(rootRegistrationListener)

                // Start the recomposer undispatched. If this would be dispatched, setContent could
                // be called before the dispatch, leading to an unexpected recomposition when
                // runRecomposeAndApplyChanges() is called.
                val coroutineStart =
                    if (isStandardTestDispatcherSupportEnabled) {
                        CoroutineStart.UNDISPATCHED
                    } else {
                        CoroutineStart.DEFAULT
                    }
                recomposerCoroutineScope.launch(start = coroutineStart) {
                    recomposer.runRecomposeAndApplyChanges()
                }
                block()
            } finally {
                // Stop the recomposer:
                recomposer.cancel()
                // Cancel our scope to ensure there are no active coroutines when
                // cleanupTestCoroutines is called in the CleanupCoroutinesStatement
                recomposerCoroutineScope.cancel()
                // Remove the exception handler installer
                composeRootRegistry.removeOnRegistrationChangedListener(rootRegistrationListener)

                throwPendingException()
            }
        }
    }

    private inline fun <R> withTestCoroutines(block: () -> R): R {
        try {
            return block()
        } finally {
            // runTest {} as the last step -
            // to replace deprecated TestCoroutineScope.cleanupTestCoroutines
            frameClockCoroutineScope.runTest {}
            frameClockCoroutineScope.cancel()
            coroutineExceptionHandler.throwUncaught()
        }
    }

    private inline fun <R> withComposeIdlingResource(block: () -> R): R {
        try {
            test.registerIdlingResource(composeIdlingResource)
            return block()
        } finally {
            test.unregisterIdlingResource(composeIdlingResource)
        }
    }

    internal inner class AndroidComposeUiTestImpl : AndroidComposeUiTest<A> {

        override val activity: A?
            get() = this@AndroidComposeUiTestEnvironment.activity

        override val density: Density by lazy {
            Density(ApplicationProvider.getApplicationContext())
        }

        override val mainClock: MainTestClock
            get() = mainClockImpl

        override fun setComposeAccessibilityValidator(validator: ComposeAccessibilityValidator?) {
            testContext.platform.composeAccessibilityValidator = validator
        }

        override fun <T> runOnUiThread(action: () -> T): T {
            return androidx.compose.ui.test.runOnUiThread(action)
        }

        override fun <T> runOnIdle(action: () -> T): T {
            // Method below make sure that compose is idle.
            waitForIdle()
            // Execute the action on ui thread in a blocking way.
            return runOnUiThread(action)
        }

        override fun waitForIdle() {
            waitForIdle(atLeastOneRootExpected = true)
            throwPendingException()
        }

        override suspend fun awaitIdle() {
            // First wait until we have a compose root (in case an Activity is being started)
            composeRootRegistry.awaitComposeRoots()
            // Switch to a thread where we're allowed to call synchronization methods
            withContext(idlingStrategy.synchronizationContext) {
                // Then await composition(s)
                idlingStrategy.runUntilIdle()
                throwPendingException()
                // Then wait for the next frame to ensure any scheduled drawing has completed
                waitForNextChoreographerFrame()
                throwPendingException()
            }
            // Check if a coroutine threw an uncaught exception
            coroutineExceptionHandler.throwUncaught()
        }

        override fun waitUntil(
            conditionDescription: String?,
            timeoutMillis: Long,
            condition: () -> Boolean,
        ) {
            val startTime = System.nanoTime()

            // With a StandardTestDispatcher, it could be that tasks are due which can satisfy the
            // condition, so run all pending tasks before checking the condition.
            if (isStandardTestDispatcherSupportEnabled) {
                mainClockImpl.runCurrent()
            }

            while (!condition()) {
                if (mainClock.autoAdvance) {
                    mainClock.advanceTimeByFrame()
                }
                // Let Android run measure, draw and in general any other async operations.
                Thread.sleep(10)
                throwPendingException()
                if (System.nanoTime() - startTime > timeoutMillis * NanoSecondsPerMilliSecond) {
                    throw ComposeTimeoutException(
                        buildWaitUntilTimeoutMessage(timeoutMillis, conditionDescription)
                    )
                }
            }
        }

        override fun registerIdlingResource(idlingResource: IdlingResource) {
            idlingResourceRegistry.registerIdlingResource(idlingResource)
        }

        override fun unregisterIdlingResource(idlingResource: IdlingResource) {
            idlingResourceRegistry.unregisterIdlingResource(idlingResource)
        }

        override fun onNode(
            matcher: SemanticsMatcher,
            useUnmergedTree: Boolean,
        ): SemanticsNodeInteraction {
            return SemanticsNodeInteraction(testContext, useUnmergedTree, matcher)
        }

        override fun onAllNodes(
            matcher: SemanticsMatcher,
            useUnmergedTree: Boolean,
        ): SemanticsNodeInteractionCollection {
            return SemanticsNodeInteractionCollection(testContext, useUnmergedTree, matcher)
        }

        override fun setContent(composable: @Composable () -> Unit) {
            // We always make sure we have the latest activity when setting a content
            val currentActivity =
                checkNotNull(activity) { "Cannot set content, host activity not found" }
            // Check if the current activity hasn't already called setContent itself
            val root = currentActivity.findViewById<ViewGroup>(android.R.id.content)
            check(root == null || root.childCount == 0) {
                "$currentActivity has already set content. If you have populated the Activity " +
                    "with a ComposeView, make sure to call setContent on that ComposeView " +
                    "instead of on the test rule; and make sure that that call to " +
                    "`setContent {}` is done after the ComposeTestRule has run"
            }

            runOnUiThread { currentActivity.setContent(recomposer, composable) }

            // Synchronizing from the UI thread when we can't leads to a dead lock
            if (idlingStrategy.canSynchronizeOnUiThread || !isOnUiThread()) {
                waitForIdle()
            }
        }
    }

    private fun throwPendingException() {
        pendingThrowable?.let {
            pendingThrowable = null
            throw it
        }
    }

    private inner class AndroidTestOwner : TestOwner {
        override val mainClock: MainTestClock
            get() = mainClockImpl

        override fun <T> runOnUiThread(action: () -> T): T = testReceiverScope.runOnUiThread(action)

        override fun getRoots(atLeastOneRootExpected: Boolean): Set<RootForTest> {
            waitForIdle(atLeastOneRootExpected)
            return composeRootRegistry.getRegisteredComposeRoots()
        }

        override fun runCurrent() {
            mainClockImpl.runCurrent()
        }
    }

    private companion object {
        val TAG = "ComposeUiTest"
    }
}

internal fun <A : ComponentActivity> ActivityScenario<A>.getActivity(): A? {
    var activity: A? = null
    onActivity { activity = it }
    return activity
}

private fun Throwable.addCascadingErrorHeaderIfAbsent() {
    if (suppressed.none { it is BeginningOfCascadingComposeErrors }) {
        addSuppressed(BeginningOfCascadingComposeErrors())
    }
}

internal class BeginningOfCascadingComposeErrors() : RuntimeException(MESSAGE) {
    override fun getStackTrace() = emptyArray<StackTraceElement>()

    override fun fillInStackTrace() = this

    override fun printStackTrace() {}

    override fun printStackTrace(s: PrintStream) {}

    override fun printStackTrace(s: PrintWriter) {}

    companion object {
        private const val MESSAGE =
            """Beginning of cascading composition errors.

               The following suppressed exceptions occurred when the test continued composition
               or teardown after the first exception was thrown. Exceptions that follow this
               message are likely not actionable or relevant for troubleshooting purposes.
               They have been included for information purposes.
               """
    }
}

@ExperimentalTestApi
actual sealed interface ComposeUiTest : SemanticsNodeInteractionsProvider {
    actual val density: Density
    actual val mainClock: MainTestClock

    /**
     * Sets the [ComposeAccessibilityValidator] to perform the accessibility checks with. Providing
     * `null` means disabling the accessibility checks
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun setComposeAccessibilityValidator(validator: ComposeAccessibilityValidator?)

    actual fun <T> runOnUiThread(action: () -> T): T

    actual fun <T> runOnIdle(action: () -> T): T

    actual fun waitForIdle()

    actual suspend fun awaitIdle()

    actual fun waitUntil(
        conditionDescription: String?,
        timeoutMillis: Long,
        condition: () -> Boolean,
    )

    /** Registers an [IdlingResource] in this test. */
    fun registerIdlingResource(idlingResource: IdlingResource)

    /** Unregisters an [IdlingResource] from this test. */
    fun unregisterIdlingResource(idlingResource: IdlingResource)

    actual fun setContent(composable: @Composable () -> Unit)
}

/**
 * A validator that is used to run accessibility checks before every action through
 * [tryPerformAccessibilityChecks]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ComposeAccessibilityValidator {
    fun check(view: View)
}

internal class AndroidComposeUiTestTimeoutException(message: String, cause: Throwable?) :
    Exception(message, cause)
