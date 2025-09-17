/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.test.junit4

import androidx.compose.runtime.Composable
import androidx.compose.ui.UiComposable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.unit.Density
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import org.junit.rules.TestRule

/**
 * A [TestRule] that allows you to test and control composables, either in isolation or in
 * applications. Most of the functionality in this interface provides some form of test
 * synchronization: the test will block until the app or composable is idle, to ensure the tests are
 * deterministic.
 *
 * For example, if you would perform a click on the center of the screen while a button is animating
 * from left to right over the screen, without synchronization the test would sometimes click when
 * the button is in the middle of the screen (button is clicked), and sometimes when the button is
 * past the middle of the screen (button is not clicked). With synchronization, the app would not be
 * idle until the animation is over, so the test will always click when the button is past the
 * middle of the screen (and not click it). If you actually do want to click the button when it's in
 * the middle of the animation, you can do so by controlling the [clock][mainClock]. You'll have to
 * disable [automatic advancing][MainTestClock.autoAdvance], and manually advance the clock by the
 * time necessary to position the button in the middle of the screen.
 *
 * An instance of [ComposeTestRule] can be created with [createComposeRule], which will also create
 * a host for the compose content for you (see [ComposeContentTestRule]). If you need to specify
 * which particular Activity is started on Android, you can use [createAndroidComposeRule].
 *
 * If you don't want any Activity to be started automatically by the test rule on Android, you can
 * use [createEmptyComposeRule]. In such a case, you will have to set content using one of Compose
 * UI's setters (like [ComponentActivity.setContent][androidx.compose.ui.platform .setContent]).
 */
@JvmDefaultWithCompatibility
interface ComposeTestRule : TestRule, SemanticsNodeInteractionsProvider {
    /**
     * Current device screen's density. Note that it is technically possible for a Compose hierarchy
     * to define a different density for a certain subtree.
     */
    val density: Density

    /** Clock that drives frames and recompositions in compose tests. */
    val mainClock: MainTestClock

    /**
     * Runs the given [action] on the UI thread.
     *
     * This method is blocking until the action is complete.
     */
    fun <T> runOnUiThread(action: () -> T): T

    /**
     * Executes the given [action] in the same way as [runOnUiThread] but [waits][waitForIdle] until
     * the app is idle before executing the action. This is the recommended way of doing your
     * assertions on shared variables.
     *
     * This method blocks until the action is complete.
     */
    fun <T> runOnIdle(action: () -> T): T

    /**
     * Waits for the UI to become idle. Quiescence is reached when there are no more pending changes
     * (e.g. pending recompositions or a pending draw call) and all [IdlingResource]s are idle.
     *
     * If [auto advancement][MainTestClock.autoAdvance] is enabled on the [mainClock], this method
     * will advance the clock to process any pending composition, invalidation and animation. If
     * auto advancement is not enabled, the clock will not be advanced which means that the Compose
     * UI appears to be frozen. This is ideal for testing animations in a deterministic way. This
     * method will always wait for all [IdlingResource]s to become idle.
     *
     * Note that some processes are driven by the host operating system and will therefore still
     * execute when auto advancement is disabled. For example, Android's measure, layout and draw
     * passes can still happen if required by the View system.
     */
    fun waitForIdle()

    /**
     * Suspends until the UI is idle. Quiescence is reached when there are no more pending changes
     * (e.g. pending recompositions or a pending draw call) and all [IdlingResource]s are idle.
     *
     * If [auto advancement][MainTestClock.autoAdvance] is enabled on the [mainClock], this method
     * will advance the clock to process any pending composition, invalidation and animation. If
     * auto advancement is not enabled, the clock will not be advanced which means that the Compose
     * UI appears to be frozen. This is ideal for testing animations in a deterministic way. This
     * method will always wait for all [IdlingResource]s to become idle.
     *
     * Note that some processes are driven by the host operating system and will therefore still
     * execute when auto advancement is disabled. For example, Android's measure, layout and draw
     * passes can still happen if required by the View system.
     */
    suspend fun awaitIdle()

    /**
     * Blocks until the given [condition] is satisfied.
     *
     * If [auto advancement][MainTestClock.autoAdvance] is enabled on the [mainClock], this method
     * will actively advance the clock to process any pending composition, invalidation and
     * animation. If auto advancement is not enabled, the clock will not be advanced actively which
     * means that the Compose UI appears to be frozen. It is still valid to use this method in this
     * way, if the condition will be satisfied by something not driven by our clock.
     *
     * Compared to [MainTestClock.advanceTimeUntil], [waitUntil] sleeps after every iteration to
     * yield to other processes. This gives [waitUntil] a better integration with the host, but it
     * is less preferred from a performance viewpoint. Therefore, we recommend that you try using
     * [MainTestClock.advanceTimeUntil] before resorting to [waitUntil].
     *
     * @param timeoutMillis The time after which this method throws an exception if the given
     *   condition is not satisfied. This observes wall clock time, not
     *   [test clock time][mainClock].
     * @param condition Condition that must be satisfied in order for this method to successfully
     *   finish.
     * @throws androidx.compose.ui.test.ComposeTimeoutException If the condition is not satisfied
     *   after [timeoutMillis] (in wall clock time).
     */
    fun waitUntil(timeoutMillis: Long = 1_000, condition: () -> Boolean)

    /**
     * Blocks until the given [condition] is satisfied.
     *
     * If [auto advancement][MainTestClock.autoAdvance] is enabled on the [mainClock], this method
     * will actively advance the clock to process any pending composition, invalidation and
     * animation. If auto advancement is not enabled, the clock will not be advanced actively which
     * means that the Compose UI appears to be frozen. It is still valid to use this method in this
     * way, if the condition will be satisfied by something not driven by our clock.
     *
     * Compared to [MainTestClock.advanceTimeUntil], [waitUntil] sleeps after every iteration to
     * yield to other processes. This gives [waitUntil] a better integration with the host, but it
     * is less preferred from a performance viewpoint. Therefore, we recommend that you try using
     * [MainTestClock.advanceTimeUntil] before resorting to [waitUntil].
     *
     * @param conditionDescription An optional human-readable description of [condition] that will
     *   be included in the timeout exception if thrown.
     * @param timeoutMillis The time after which this method throws an exception if the given
     *   condition is not satisfied. This observes wall clock time, not
     *   [test clock time][mainClock].
     * @param condition Condition that must be satisfied in order for this method to successfully
     *   finish.
     * @throws androidx.compose.ui.test.ComposeTimeoutException If the condition is not satisfied
     *   after [timeoutMillis] (in wall clock time).
     */
    fun waitUntil(
        conditionDescription: String,
        timeoutMillis: Long = 1_000,
        condition: () -> Boolean,
    ) {
        waitUntil(timeoutMillis, condition)
    }

    /**
     * Blocks until the number of nodes matching the given [matcher] is equal to the given [count].
     *
     * @param matcher The matcher that will be used to filter nodes.
     * @param count The number of nodes that are expected to
     * @param timeoutMillis The time after which this method throws an exception if the number of
     *   nodes that match the [matcher] is not [count]. This observes wall clock time, not frame
     *   time.
     * @throws androidx.compose.ui.test.ComposeTimeoutException If the number of nodes that match
     *   the [matcher] is not [count] after [timeoutMillis] (in wall clock time).
     * @see ComposeTestRule.waitUntil
     */
    @ExperimentalTestApi
    fun waitUntilNodeCount(matcher: SemanticsMatcher, count: Int, timeoutMillis: Long = 1_000L)

    /**
     * Blocks until at least one node matches the given [matcher].
     *
     * @param matcher The matcher that will be used to filter nodes.
     * @param timeoutMillis The time after which this method throws an exception if no nodes match
     *   the given [matcher]. This observes wall clock time, not frame time.
     * @throws androidx.compose.ui.test.ComposeTimeoutException If no nodes match the given
     *   [matcher] after [timeoutMillis] (in wall clock time).
     * @see ComposeTestRule.waitUntil
     */
    @ExperimentalTestApi
    fun waitUntilAtLeastOneExists(matcher: SemanticsMatcher, timeoutMillis: Long = 1_000L)

    /**
     * Blocks until exactly one node matches the given [matcher].
     *
     * @param matcher The matcher that will be used to filter nodes.
     * @param timeoutMillis The time after which this method throws an exception if exactly one node
     *   does not match the given [matcher]. This observes wall clock time, not frame time.
     * @throws androidx.compose.ui.test.ComposeTimeoutException If exactly one node does not match
     *   the given [matcher] after [timeoutMillis] (in wall clock time).
     * @see ComposeTestRule.waitUntil
     */
    @ExperimentalTestApi
    fun waitUntilExactlyOneExists(matcher: SemanticsMatcher, timeoutMillis: Long = 1_000L)

    /**
     * Blocks until no nodes match the given [matcher].
     *
     * @param matcher The matcher that will be used to filter nodes.
     * @param timeoutMillis The time after which this method throws an exception if any nodes match
     *   the given [matcher]. This observes wall clock time, not frame time.
     * @throws androidx.compose.ui.test.ComposeTimeoutException If any nodes match the given
     *   [matcher] after [timeoutMillis] (in wall clock time).
     * @see ComposeTestRule.waitUntil
     */
    @ExperimentalTestApi
    fun waitUntilDoesNotExist(matcher: SemanticsMatcher, timeoutMillis: Long = 1_000L)

    /** Registers an [IdlingResource] in this test. */
    fun registerIdlingResource(idlingResource: IdlingResource)

    /** Unregisters an [IdlingResource] from this test. */
    fun unregisterIdlingResource(idlingResource: IdlingResource)
}

/**
 * A [ComposeTestRule] that allows you to set content without the necessity to provide a host for
 * the content. The host, such as an Activity, will be created by the test rule.
 *
 * An instance of [ComposeContentTestRule] can be created with [createComposeRule]. If you need to
 * specify which particular Activity is started on Android, you can use [createAndroidComposeRule].
 *
 * If you don't want any host to be started automatically by the test rule on Android, you can use
 * [createEmptyComposeRule]. In such a case, you will have to create a host in your test and set the
 * content using one of Compose UI's setters (like
 * [ComponentActivity .setContent][androidx.activity.compose.setContent]).
 */
@JvmDefaultWithCompatibility
interface ComposeContentTestRule : ComposeTestRule {
    /**
     * Sets the given composable as a content of the current screen.
     *
     * Use this in your tests to setup the UI content to be tested. This should be called exactly
     * once per test.
     *
     * @throws IllegalStateException if called more than once per test.
     */
    fun setContent(composable: @Composable @UiComposable () -> Unit)
}

/**
 * Factory method to provide an implementation of [ComposeContentTestRule].
 *
 * This method is useful for tests in compose libraries where it is irrelevant where the compose
 * content is hosted (e.g. an Activity on Android). Such tests typically set compose content
 * themselves via [setContent][ComposeContentTestRule.setContent] and only instrument and assert
 * that content.
 *
 * For Android this will use the default Activity (android.app.Activity). You need to add a
 * reference to this activity into the manifest file of the corresponding tests (usually in
 * androidTest/AndroidManifest.xml). If your Android test requires a specific Activity to be
 * launched, see [createAndroidComposeRule].
 */
expect fun createComposeRule(): ComposeContentTestRule

/**
 * Factory method to provide an implementation of [ComposeContentTestRule].
 *
 * This method is useful for tests in compose libraries where it is irrelevant where the compose
 * content is hosted (e.g. an Activity on Android). Such tests typically set compose content
 * themselves via [setContent][ComposeContentTestRule.setContent] and only instrument and assert
 * that content.
 *
 * For Android this will use the default Activity (android.app.Activity). You need to add a
 * reference to this activity into the manifest file of the corresponding tests (usually in
 * androidTest/AndroidManifest.xml). If your Android test requires a specific Activity to be
 * launched, see [createAndroidComposeRule].
 *
 * @param effectContext The [CoroutineContext] used to run the composition. The context for
 *   `LaunchedEffect`s and `rememberCoroutineScope` will be derived from this context. If this
 *   context contains a [TestDispatcher] or [TestCoroutineScheduler] (in that order), it will be
 *   used for composition and the [MainTestClock].
 */
@ExperimentalTestApi
expect fun createComposeRule(
    effectContext: CoroutineContext = EmptyCoroutineContext
): ComposeContentTestRule
