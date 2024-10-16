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

import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.AndroidComposeUiTestEnvironment
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.compose.ui.test.waitUntilDoesNotExist
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.compose.ui.test.waitUntilNodeCount
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

actual fun createComposeRule(): ComposeContentTestRule =
    createAndroidComposeRule<ComponentActivity>()

@ExperimentalTestApi
actual fun createComposeRule(effectContext: CoroutineContext): ComposeContentTestRule =
    createAndroidComposeRule<ComponentActivity>(effectContext)

/**
 * Factory method to provide android specific implementation of [createComposeRule], for a given
 * activity class type [A].
 *
 * This method is useful for tests that require a custom Activity. This is usually the case for
 * tests where the compose content is set by that Activity, instead of via the test rule's
 * [setContent][ComposeContentTestRule.setContent]. Make sure that you add the provided activity
 * into your app's manifest file (usually in main/AndroidManifest.xml).
 *
 * This creates a test rule that is using [ActivityScenarioRule] as the activity launcher. If you
 * would like to use a different one you can create [AndroidComposeTestRule] directly and supply it
 * with your own launcher.
 *
 * If your test doesn't require a specific Activity, use [createComposeRule] instead.
 */
inline fun <reified A : ComponentActivity> createAndroidComposeRule():
    AndroidComposeTestRule<ActivityScenarioRule<A>, A> {
    // TODO(b/138993381): By launching custom activities we are losing control over what content is
    //  already there. This is issue in case the user already set some compose content and decides
    //  to set it again via our API. In such case we won't be able to dispose the old composition.
    //  Other option would be to provide a smaller interface that does not expose these methods.
    return createAndroidComposeRule(A::class.java)
}

/**
 * Factory method to provide android specific implementation of [createComposeRule], for a given
 * activity class type [A].
 *
 * This method is useful for tests that require a custom Activity. This is usually the case for
 * tests where the compose content is set by that Activity, instead of via the test rule's
 * [setContent][ComposeContentTestRule.setContent]. Make sure that you add the provided activity
 * into your app's manifest file (usually in main/AndroidManifest.xml).
 *
 * This creates a test rule that is using [ActivityScenarioRule] as the activity launcher. If you
 * would like to use a different one you can create [AndroidComposeTestRule] directly and supply it
 * with your own launcher.
 *
 * If your test doesn't require a specific Activity, use [createComposeRule] instead.
 *
 * @param effectContext The [CoroutineContext] used to run the composition. The context for
 *   `LaunchedEffect`s and `rememberCoroutineScope` will be derived from this context. If this
 *   context contains a [TestDispatcher] or [TestCoroutineScheduler] (in that order), it will be
 *   used for composition and the [MainTestClock].
 */
@ExperimentalTestApi
inline fun <reified A : ComponentActivity> createAndroidComposeRule(
    effectContext: CoroutineContext = EmptyCoroutineContext
): AndroidComposeTestRule<ActivityScenarioRule<A>, A> {
    // TODO(b/138993381): By launching custom activities we are losing control over what content is
    //  already there. This is issue in case the user already set some compose content and decides
    //  to set it again via our API. In such case we won't be able to dispose the old composition.
    //  Other option would be to provide a smaller interface that does not expose these methods.
    return createAndroidComposeRule(A::class.java, effectContext)
}

/**
 * Factory method to provide android specific implementation of [createComposeRule], for a given
 * [activityClass].
 *
 * This method is useful for tests that require a custom Activity. This is usually the case for
 * tests where the compose content is set by that Activity, instead of via the test rule's
 * [setContent][ComposeContentTestRule.setContent]. Make sure that you add the provided activity
 * into your app's manifest file (usually in main/AndroidManifest.xml).
 *
 * This creates a test rule that is using [ActivityScenarioRule] as the activity launcher. If you
 * would like to use a different one you can create [AndroidComposeTestRule] directly and supply it
 * with your own launcher.
 *
 * If your test doesn't require a specific Activity, use [createComposeRule] instead.
 */
fun <A : ComponentActivity> createAndroidComposeRule(
    activityClass: Class<A>
): AndroidComposeTestRule<ActivityScenarioRule<A>, A> =
    AndroidComposeTestRule(
        activityRule = ActivityScenarioRule(activityClass),
        activityProvider = ::getActivityFromTestRule
    )

/**
 * Factory method to provide android specific implementation of [createComposeRule], for a given
 * [activityClass].
 *
 * This method is useful for tests that require a custom Activity. This is usually the case for
 * tests where the compose content is set by that Activity, instead of via the test rule's
 * [setContent][ComposeContentTestRule.setContent]. Make sure that you add the provided activity
 * into your app's manifest file (usually in main/AndroidManifest.xml).
 *
 * This creates a test rule that is using [ActivityScenarioRule] as the activity launcher. If you
 * would like to use a different one you can create [AndroidComposeTestRule] directly and supply it
 * with your own launcher.
 *
 * If your test doesn't require a specific Activity, use [createComposeRule] instead.
 *
 * @param activityClass The activity type to use in the activity scenario
 * @param effectContext The [CoroutineContext] used to run the composition. The context for
 *   `LaunchedEffect`s and `rememberCoroutineScope` will be derived from this context. If this
 *   context contains a [TestDispatcher] or [TestCoroutineScheduler] (in that order), it will be
 *   used for composition and the [MainTestClock].
 */
@ExperimentalTestApi
fun <A : ComponentActivity> createAndroidComposeRule(
    activityClass: Class<A>,
    effectContext: CoroutineContext = EmptyCoroutineContext
): AndroidComposeTestRule<ActivityScenarioRule<A>, A> =
    AndroidComposeTestRule(
        activityRule = ActivityScenarioRule(activityClass),
        activityProvider = ::getActivityFromTestRule,
        effectContext = effectContext
    )

/**
 * Factory method to provide an implementation of [ComposeTestRule] that doesn't create a compose
 * host for you in which you can set content.
 *
 * This method is useful for tests that need to create their own compose host during the test. The
 * returned test rule will not create a host, and consequently does not provide a `setContent`
 * method. To set content in tests using this rule, use the appropriate `setContent` methods from
 * your compose host.
 *
 * A typical use case on Android is when the test needs to launch an Activity (the compose host)
 * after one or more dependencies have been injected.
 */
fun createEmptyComposeRule(): ComposeTestRule =
    AndroidComposeTestRule<TestRule, ComponentActivity>(
        activityRule = TestRule { base, _ -> base },
        activityProvider = {
            error(
                "createEmptyComposeRule() does not provide an Activity to set Compose content in." +
                    " Launch and use the Activity yourself, or use createAndroidComposeRule()."
            )
        }
    )

/**
 * Factory method to provide an implementation of [ComposeTestRule] that doesn't create a compose
 * host for you in which you can set content.
 *
 * This method is useful for tests that need to create their own compose host during the test. The
 * returned test rule will not create a host, and consequently does not provide a `setContent`
 * method. To set content in tests using this rule, use the appropriate `setContent` methods from
 * your compose host.
 *
 * A typical use case on Android is when the test needs to launch an Activity (the compose host)
 * after one or more dependencies have been injected.
 *
 * @param effectContext The [CoroutineContext] used to run the composition. The context for
 *   `LaunchedEffect`s and `rememberCoroutineScope` will be derived from this context. If this
 *   context contains a [TestDispatcher] or [TestCoroutineScheduler] (in that order), it will be
 *   used for composition and the [MainTestClock].
 */
@ExperimentalTestApi
fun createEmptyComposeRule(
    effectContext: CoroutineContext = EmptyCoroutineContext
): ComposeTestRule =
    AndroidComposeTestRule<TestRule, ComponentActivity>(
        activityRule = TestRule { base, _ -> base },
        effectContext = effectContext,
        activityProvider = {
            error(
                "createEmptyComposeRule() does not provide an Activity to set Compose content in." +
                    " Launch and use the Activity yourself, or use createAndroidComposeRule()."
            )
        }
    )

@OptIn(ExperimentalTestApi::class)
class AndroidComposeTestRule<R : TestRule, A : ComponentActivity>
private constructor(
    val activityRule: R,
    private val environment: AndroidComposeUiTestEnvironment<A>
) : ComposeContentTestRule {
    private val composeTest = environment.test

    /**
     * Android specific implementation of [ComposeContentTestRule], where compose content is hosted
     * by an Activity.
     *
     * The Activity is normally launched by the given [activityRule] before the test starts, but it
     * is possible to pass a test rule that chooses to launch an Activity on a later time. The
     * Activity is retrieved from the [activityRule] by means of the [activityProvider], which can
     * be thought of as a getter for the Activity on the [activityRule]. If you use an
     * [activityRule] that launches an Activity on a later time, you should make sure that the
     * Activity is launched by the time or while the [activityProvider] is called.
     *
     * The [AndroidComposeTestRule] wraps around the given [activityRule] to make sure the Activity
     * is launched _after_ the [AndroidComposeTestRule] has completed all necessary steps to control
     * and monitor the compose content.
     *
     * @param activityRule Test rule to use to launch the Activity.
     * @param activityProvider Function to retrieve the Activity from the given [activityRule].
     */
    constructor(
        activityRule: R,
        activityProvider: (R) -> A
    ) : this(
        activityRule = activityRule,
        effectContext = EmptyCoroutineContext,
        activityProvider = activityProvider,
    )

    /**
     * Android specific implementation of [ComposeContentTestRule], where compose content is hosted
     * by an Activity.
     *
     * The Activity is normally launched by the given [activityRule] before the test starts, but it
     * is possible to pass a test rule that chooses to launch an Activity on a later time. The
     * Activity is retrieved from the [activityRule] by means of the [activityProvider], which can
     * be thought of as a getter for the Activity on the [activityRule]. If you use an
     * [activityRule] that launches an Activity on a later time, you should make sure that the
     * Activity is launched by the time or while the [activityProvider] is called.
     *
     * The [AndroidComposeTestRule] wraps around the given [activityRule] to make sure the Activity
     * is launched _after_ the [AndroidComposeTestRule] has completed all necessary steps to control
     * and monitor the compose content.
     *
     * @param activityRule Test rule to use to launch the Activity.
     * @param effectContext The [CoroutineContext] used to run the composition. The context for
     *   `LaunchedEffect`s and `rememberCoroutineScope` will be derived from this context. If this
     *   context contains a [TestDispatcher] or [TestCoroutineScheduler] (in that order), it will be
     *   used for composition and the [MainTestClock].
     * @param activityProvider Function to retrieve the Activity from the given [activityRule].
     */
    @ExperimentalTestApi
    constructor(
        activityRule: R,
        effectContext: CoroutineContext = EmptyCoroutineContext,
        activityProvider: (R) -> A,
    ) : this(
        activityRule,
        AndroidComposeUiTestEnvironment(effectContext) { activityProvider(activityRule) },
    )

    /**
     * Provides the current activity.
     *
     * Avoid calling often as it can involve synchronization and can be slow.
     */
    val activity: A
        get() = checkNotNull(composeTest.activity) { "Host activity not found" }

    override fun apply(base: Statement, description: Description): Statement {
        val testStatement = activityRule.apply(base, description)
        return object : Statement() {
            override fun evaluate() {
                environment.runTest { testStatement.evaluate() }
            }
        }
    }

    @Deprecated(
        message = "Do not instantiate this Statement, use AndroidComposeTestRule instead",
        level = DeprecationLevel.ERROR
    )
    inner class AndroidComposeStatement(private val base: Statement) : Statement() {
        override fun evaluate() {
            base.evaluate()
        }
    }

    /*
     * WHEN THE NAME AND SHAPE OF THE NEW COMMON INTERFACES HAS BEEN DECIDED,
     * REPLACE ALL OVERRIDES BELOW WITH DELEGATION: ComposeTest by composeTest
     */

    override val density: Density
        get() = composeTest.density

    override val mainClock: MainTestClock
        get() = composeTest.mainClock

    /**
     * The [AccessibilityValidator] that will be used to run Android accessibility checks before
     * every action that is expected to change the UI.
     *
     * If no validator is set (`null`), no checks will be performed. You can either supply your own
     * validator directly, or have one configured for you with [enableAccessibilityChecks].
     *
     * The default value is `null`.
     *
     * This requires API 34+ (Android U), and currently does not work on Robolectric.
     *
     * @sample androidx.compose.ui.test.samples.accessibilityChecks_withAndroidComposeTestRule_sample
     */
    @get:RequiresApi(34)
    @set:RequiresApi(34)
    var accessibilityValidator: AccessibilityValidator?
        get() = composeTest.accessibilityValidator
        set(value) {
            composeTest.accessibilityValidator = value
        }

    override fun <T> runOnUiThread(action: () -> T): T = composeTest.runOnUiThread(action)

    override fun <T> runOnIdle(action: () -> T): T = composeTest.runOnIdle(action)

    override fun waitForIdle() = composeTest.waitForIdle()

    override suspend fun awaitIdle() = composeTest.awaitIdle()

    override fun waitUntil(timeoutMillis: Long, condition: () -> Boolean) =
        composeTest.waitUntil(conditionDescription = null, timeoutMillis, condition)

    override fun waitUntil(
        conditionDescription: String,
        timeoutMillis: Long,
        condition: () -> Boolean
    ) {
        composeTest.waitUntil(conditionDescription, timeoutMillis, condition)
    }

    @ExperimentalTestApi
    override fun waitUntilNodeCount(matcher: SemanticsMatcher, count: Int, timeoutMillis: Long) =
        composeTest.waitUntilNodeCount(matcher, count, timeoutMillis)

    @ExperimentalTestApi
    override fun waitUntilAtLeastOneExists(matcher: SemanticsMatcher, timeoutMillis: Long) =
        composeTest.waitUntilAtLeastOneExists(matcher, timeoutMillis)

    @ExperimentalTestApi
    override fun waitUntilExactlyOneExists(matcher: SemanticsMatcher, timeoutMillis: Long) =
        composeTest.waitUntilExactlyOneExists(matcher, timeoutMillis)

    @ExperimentalTestApi
    override fun waitUntilDoesNotExist(matcher: SemanticsMatcher, timeoutMillis: Long) =
        composeTest.waitUntilDoesNotExist(matcher, timeoutMillis)

    override fun registerIdlingResource(idlingResource: IdlingResource) =
        composeTest.registerIdlingResource(idlingResource)

    override fun unregisterIdlingResource(idlingResource: IdlingResource) =
        composeTest.unregisterIdlingResource(idlingResource)

    /**
     * Enables accessibility checks that will be run before every action that is expected to change
     * the UI.
     *
     * This will create and set an [accessibilityValidator] if there isn't one yet, or will do
     * nothing if an `accessibilityValidator` is already set.
     *
     * This requires API 34+ (Android U), and currently does not work on Robolectric.
     *
     * @sample androidx.compose.ui.test.samples.accessibilityChecks_withComposeTestRule_sample
     * @see disableAccessibilityChecks
     */
    @RequiresApi(34)
    override fun enableAccessibilityChecks() = composeTest.enableAccessibilityChecks()

    /**
     * Disables accessibility checks.
     *
     * This will set the [accessibilityValidator] back to `null`.
     *
     * @sample androidx.compose.ui.test.samples.accessibilityChecks_withAndroidComposeTestRule_sample
     * @see enableAccessibilityChecks
     */
    @RequiresApi(34)
    override fun disableAccessibilityChecks() = composeTest.disableAccessibilityChecks()

    override fun onNode(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean
    ): SemanticsNodeInteraction = composeTest.onNode(matcher, useUnmergedTree)

    override fun onAllNodes(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean
    ): SemanticsNodeInteractionCollection = composeTest.onAllNodes(matcher, useUnmergedTree)

    override fun setContent(composable: @Composable () -> Unit) = composeTest.setContent(composable)

    /**
     * Cancels AndroidComposeUiTestEnvironment's current Recomposer and creates a new one.
     *
     * Recreates the CoroutineContext associated with Compose being cancelled. This happens when an
     * app moves from a regular ("Full screen") view of the app to a "Pop up" view AND certain
     * properties in the manifest's android:configChanges are set to prevent a full tear down of the
     * app. This is a somewhat rare case (see [AndroidComposeUiTestEnvironment] for more details).
     */
    fun cancelAndRecreateRecomposer() {
        environment.cancelAndRecreateRecomposer()
    }
}

/**
 * An implementation of [ComposeTestRule] that will correctly reset itself in-between test retries.
 *
 * NOTE: In order to function properly this rule should be wrapped by your retry rule.
 *
 * This is necessary because there is currently no ability to reset the
 * [AndroidComposeUiTestEnvironment] in the standard [AndroidComposeTestRule].
 *
 * @param ruleProvider Function to create the underlying ComposeTestRule rule, defaults to [createEmptyComposeRule]
 */
class RetryableComposeTestRule constructor(
    private val ruleProvider: () -> ComposeTestRule = ::createEmptyComposeRule
) :
    ComposeTestRule {
    private var rule: ComposeTestRule = ruleProvider.invoke()

    override val density: Density
        get() = rule.density
    override val mainClock: MainTestClock
        get() = rule.mainClock

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    rule.apply(base, description).evaluate()
                } finally {
                    rule = ruleProvider.invoke()
                }
            }
        }
    }

    override suspend fun awaitIdle() = rule.awaitIdle()

    override fun onAllNodes(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean
    ): SemanticsNodeInteractionCollection = rule.onAllNodes(matcher, useUnmergedTree)

    override fun onNode(matcher: SemanticsMatcher, useUnmergedTree: Boolean): SemanticsNodeInteraction =
        rule.onNode(matcher, useUnmergedTree)

    override fun registerIdlingResource(idlingResource: IdlingResource) =
        rule.registerIdlingResource(idlingResource)

    override fun <T> runOnIdle(action: () -> T): T =
        rule.runOnIdle(action)

    override fun <T> runOnUiThread(action: () -> T): T =
        rule.runOnUiThread(action)

    override fun unregisterIdlingResource(idlingResource: IdlingResource) =
        rule.unregisterIdlingResource(idlingResource)

    override fun waitForIdle() = rule.waitForIdle()

    override fun waitUntil(timeoutMillis: Long, condition: () -> Boolean) =
        rule.waitUntil(timeoutMillis, condition)

}
    
private fun <A : ComponentActivity> getActivityFromTestRule(rule: ActivityScenarioRule<A>): A {
    var activity: A? = null
    rule.scenario.onActivity { activity = it }
    if (activity == null) {
        throw IllegalStateException("Activity was not set in the ActivityScenarioRule!")
    }
    return activity!!
}
