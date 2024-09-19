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

import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import androidx.compose.ui.platform.WindowRecomposerPolicy
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

@ExperimentalTestApi
actual fun runComposeUiTest(effectContext: CoroutineContext, block: ComposeUiTest.() -> Unit) {
    runAndroidComposeUiTest(ComponentActivity::class.java, effectContext, block)
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
 * @param block The test function.
 */
@ExperimentalTestApi
inline fun <reified A : ComponentActivity> runAndroidComposeUiTest(
    effectContext: CoroutineContext = EmptyCoroutineContext,
    noinline block: AndroidComposeUiTest<A>.() -> Unit
) {
    runAndroidComposeUiTest(A::class.java, effectContext, block)
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
 * @param block The test function.
 */
@ExperimentalTestApi
fun <A : ComponentActivity> runAndroidComposeUiTest(
    activityClass: Class<A>,
    effectContext: CoroutineContext = EmptyCoroutineContext,
    block: AndroidComposeUiTest<A>.() -> Unit
) {
    // Don't start the scenario now, wait until we're inside runTest { },
    // in case the Activity's onCreate/Start/Resume calls setContent
    var scenario: ActivityScenario<A>? = null
    val environment =
        AndroidComposeUiTestEnvironment(effectContext) {
            requireNotNull(scenario) {
                    "ActivityScenario has not yet been launched, or has already finished. Make sure that " +
                        "any call to ComposeUiTest.setContent() and AndroidComposeUiTest.getActivity() " +
                        "is made within the lambda passed to AndroidComposeUiTestEnvironment.runTest()"
                }
                .getActivity()
        }
    try {
        environment.runTest {
            scenario = ActivityScenario.launch(activityClass)
            block()
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
@ExperimentalTestApi
fun runEmptyComposeUiTest(block: ComposeUiTest.() -> Unit) {
    AndroidComposeUiTestEnvironment {
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
 */
@ExperimentalTestApi
inline fun <A : ComponentActivity> AndroidComposeUiTestEnvironment(
    effectContext: CoroutineContext = EmptyCoroutineContext,
    crossinline activityProvider: () -> A?
): AndroidComposeUiTestEnvironment<A> {
    return object : AndroidComposeUiTestEnvironment<A>(effectContext) {
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
 */
@ExperimentalTestApi
@OptIn(ExperimentalCoroutinesApi::class)
abstract class AndroidComposeUiTestEnvironment<A : ComponentActivity>(
    private val effectContext: CoroutineContext = EmptyCoroutineContext
) {
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
    // We can only accept a TestDispatcher here because we need to access its scheduler.
    private val testCoroutineDispatcher =
        // Use the TestDispatcher if it is provided in the effectContext
        effectContext[ContinuationInterceptor] as? TestDispatcher
            ?:
            // Otherwise, use the TestCoroutineScheduler if it is provided
            UnconfinedTestDispatcher(effectContext[TestCoroutineScheduler])
    private val testCoroutineScope = TestScope(testCoroutineDispatcher)
    private lateinit var recomposerCoroutineScope: CoroutineScope
    private val coroutineExceptionHandler = UncaughtExceptionHandler()

    private val frameClock: TestMonotonicFrameClock
    private val recomposerContinuationInterceptor: ApplyingContinuationInterceptor
    private val infiniteAnimationPolicy: InfiniteAnimationPolicy

    init {
        frameClock =
            TestMonotonicFrameClock(
                testCoroutineScope,
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
                }
            )
        // The applying interceptor needs to be the outermost wrapper since TestMonotonicFrameClock
        // will not delegate if the dispatcher dispatch is not needed at the time of intercept.
        recomposerContinuationInterceptor =
            ApplyingContinuationInterceptor(frameClock.continuationInterceptor)

        mainClockImpl = MainTestClockImpl(testCoroutineDispatcher.scheduler, frameClock)

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
            ComposeIdlingResource(composeRootRegistry, mainClockImpl, recomposer)
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

    /**
     * Runs the given [block], setting up all test hooks before running the test and tearing them
     * down after running the test.
     */
    fun <R> runTest(block: AndroidComposeUiTest<A>.() -> R): R {
        if (Build.FINGERPRINT.lowercase() == "robolectric") {
            idlingStrategy = RobolectricIdlingStrategy(composeRootRegistry, composeIdlingResource)
        }
        // Need to await quiescence before registering our ComposeIdlingResource because the host
        // activity might still be launching. If it is going to set compose content, we want that
        // to happen before we install our hooks to avoid a race.
        idlingStrategy.runUntilIdle()
        return composeRootRegistry.withRegistry {
            idlingResourceRegistry.withRegistry {
                idlingStrategy.withStrategy {
                    withTestCoroutines {
                        withWindowRecomposer {
                            withComposeIdlingResource {
                                testReceiverScope.withDisposableContent(block)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun waitForIdle(atLeastOneRootExpected: Boolean) {
        // First wait until we have a compose root (in case an Activity is being started)
        composeRootRegistry.waitForComposeRoots(atLeastOneRootExpected)
        // Then await composition(s)
        idlingStrategy.runUntilIdle()
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
            }
        }
    }

    private fun <R> withWindowRecomposer(block: () -> R): R {
        @OptIn(InternalComposeUiApi::class)
        return WindowRecomposerPolicy.withFactory({ recomposer }) {
            try {
                // Start the recomposer:
                recomposerCoroutineScope.launch { recomposer.runRecomposeAndApplyChanges() }
                block()
            } finally {
                // Stop the recomposer:
                recomposer.cancel()
                // Cancel our scope to ensure there are no active coroutines when
                // cleanupTestCoroutines is called in the CleanupCoroutinesStatement
                recomposerCoroutineScope.cancel()
            }
        }
    }

    private fun <R> withTestCoroutines(block: () -> R): R {
        try {
            return block()
        } finally {
            // runTest {} as the last step -
            // to replace deprecated TestCoroutineScope.cleanupTestCoroutines
            testCoroutineScope.runTest {}
            testCoroutineScope.cancel()
            coroutineExceptionHandler.throwUncaught()
        }
    }

    private fun <R> withComposeIdlingResource(block: () -> R): R {
        try {
            test.registerIdlingResource(composeIdlingResource)
            return block()
        } finally {
            test.unregisterIdlingResource(composeIdlingResource)
        }
    }

    internal inner class AndroidComposeUiTestImpl : AndroidComposeUiTest<A> {
        private var disposeContentHook: (() -> Unit)? = null

        override val activity: A?
            get() = this@AndroidComposeUiTestEnvironment.activity

        override val density: Density by lazy {
            Density(ApplicationProvider.getApplicationContext())
        }

        override val mainClock: MainTestClock
            get() = mainClockImpl

        override var accessibilityValidator: AccessibilityValidator?
            get() = testContext.platform.accessibilityValidator
            set(value) {
                testContext.platform.accessibilityValidator = value
            }

        override fun <T> runOnUiThread(action: () -> T): T {
            return testOwner.runOnUiThread(action)
        }

        override fun <T> runOnIdle(action: () -> T): T {
            // Method below make sure that compose is idle.
            waitForIdle()
            // Execute the action on ui thread in a blocking way.
            return runOnUiThread(action)
        }

        override fun waitForIdle() {
            waitForIdle(atLeastOneRootExpected = true)
        }

        override suspend fun awaitIdle() {
            // First wait until we have a compose root (in case an Activity is being started)
            composeRootRegistry.awaitComposeRoots()
            // Switch to a thread where we're allowed to call synchronization methods
            withContext(idlingStrategy.synchronizationContext) {
                // Then await composition(s)
                idlingStrategy.runUntilIdle()
                // Then wait for the next frame to ensure any scheduled drawing has completed
                waitForNextChoreographerFrame()
            }
            // Check if a coroutine threw an uncaught exception
            coroutineExceptionHandler.throwUncaught()
        }

        override fun waitUntil(
            conditionDescription: String?,
            timeoutMillis: Long,
            condition: () -> Boolean
        ) {
            val startTime = System.nanoTime()
            while (!condition()) {
                if (mainClockImpl.autoAdvance) {
                    mainClock.advanceTimeByFrame()
                }
                // Let Android run measure, draw and in general any other async operations.
                Thread.sleep(10)
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

        override fun enableAccessibilityChecks() {
            accessibilityValidator = AccessibilityValidator().setRunChecksFromRootView(true)
        }

        override fun disableAccessibilityChecks() {
            accessibilityValidator = null
        }

        override fun onNode(
            matcher: SemanticsMatcher,
            useUnmergedTree: Boolean
        ): SemanticsNodeInteraction {
            return SemanticsNodeInteraction(testContext, useUnmergedTree, matcher)
        }

        override fun onAllNodes(
            matcher: SemanticsMatcher,
            useUnmergedTree: Boolean
        ): SemanticsNodeInteractionCollection {
            return SemanticsNodeInteractionCollection(testContext, useUnmergedTree, matcher)
        }

        override fun setContent(composable: @Composable () -> Unit) {
            check(disposeContentHook == null) { "Cannot call setContent twice per test!" }

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

            runOnUiThread {
                currentActivity.setContent(recomposer, composable)
                disposeContentHook = {
                    // Removing a default ComposeView from the view hierarchy will
                    // dispose its composition.
                    activity?.let { it.setContentView(View(it)) }
                }
            }

            // Synchronizing from the UI thread when we can't leads to a dead lock
            if (idlingStrategy.canSynchronizeOnUiThread || !isOnUiThread()) {
                waitForIdle()
            }
        }

        fun <R> withDisposableContent(block: AndroidComposeUiTest<A>.() -> R): R {
            try {
                return block.invoke(this)
            } finally {
                // Dispose the content. The content is disposed by replacing the activity's content
                // with an empty View, breaking potential infinite loops. Just cancelling the
                // Recomposer is not enough, as the infinite loop might not involve recomposition.
                // For example, when there is a layout or draw lambda that keeps invalidating
                // itself. Note that this won't have any effect if the content is not set with
                // ComposeUiTest.setContent, but directly with ComponentActivity.setContent, which
                // would be the typical case when testing an Activity that sets Compose content.
                disposeContentHook?.let {
                    disposeContentHook = null
                    runOnUiThread {
                        // NOTE: currently, calling dispose after an exception that happened during
                        // composition is not a safe call. Compose runtime should fix this, and then
                        // this call will be okay. At the moment, however, calling this could
                        // itself produce an exception which will then obscure the original
                        // exception. To fix this, we will just wrap this call in a try/catch of
                        // its own
                        try {
                            it.invoke()
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                }
            }
        }
    }

    private inner class AndroidTestOwner : TestOwner {
        override val mainClock: MainTestClock
            get() = mainClockImpl

        override fun <T> runOnUiThread(action: () -> T): T {
            return androidx.compose.ui.test.runOnUiThread(action)
        }

        override fun getRoots(atLeastOneRootExpected: Boolean): Set<RootForTest> {
            waitForIdle(atLeastOneRootExpected)
            return composeRootRegistry.getRegisteredComposeRoots()
        }
    }
}

internal fun <A : ComponentActivity> ActivityScenario<A>.getActivity(): A? {
    var activity: A? = null
    onActivity { activity = it }
    return activity
}

@ExperimentalTestApi
actual sealed interface ComposeUiTest : SemanticsNodeInteractionsProvider {
    actual val density: Density
    actual val mainClock: MainTestClock

    /**
     * The [AccessibilityValidator] that will be used to run Android accessibility checks before
     * every action that is expected to change the UI.
     *
     * If no validator is set (`null`), no checks will be performed. You can either supply your own
     * validator directly, or have one configured for you with [enableAccessibilityChecks].
     *
     * The default value is `null`.
     *
     * @sample androidx.compose.ui.test.samples.accessibilityChecks_withAndroidComposeUiTest_sample
     */
    var accessibilityValidator: AccessibilityValidator?

    actual fun <T> runOnUiThread(action: () -> T): T

    actual fun <T> runOnIdle(action: () -> T): T

    actual fun waitForIdle()

    actual suspend fun awaitIdle()

    actual fun waitUntil(
        conditionDescription: String?,
        timeoutMillis: Long,
        condition: () -> Boolean
    )

    actual fun registerIdlingResource(idlingResource: IdlingResource)

    actual fun unregisterIdlingResource(idlingResource: IdlingResource)

    actual fun setContent(composable: @Composable () -> Unit)

    actual fun enableAccessibilityChecks()

    actual fun disableAccessibilityChecks()
}
