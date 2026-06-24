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

package androidx.compose.ui.test.junit4.v2

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.AndroidComposeUiTestFlags
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.getActivityFromTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import org.junit.rules.TestRule

/**
 * Factory method to provide an implementation of [ComposeContentTestRule].
 *
 * This implementation uses [kotlinx.coroutines.test.StandardTestDispatcher] by default for running
 * composition. This ensures that the test behavior is consistent with
 * [kotlinx.coroutines.test.runTest] and provides explicit control over coroutine execution order.
 * This means you may need to explicitly advance time or run current coroutines when testing complex
 * coroutine logic, as tasks are queued on the scheduler rather than running eagerly.
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
 *   context contains a [TestDispatcher], it is used for composition and the [MainTestClock].
 *   Otherwise, a [kotlinx.coroutines.test.StandardTestDispatcher] is created and used. This new
 *   dispatcher will share the [TestCoroutineScheduler] from [effectContext] if one is present.
 */
actual fun createComposeRule(effectContext: CoroutineContext): ComposeContentTestRule =
    createAndroidComposeRule<ComponentActivity>(effectContext)

/**
 * Factory method to provide an implementation of [ComposeContentTestRule] configured via a
 * [ComposeUiTestConfig].
 *
 * This method is useful for tests in compose libraries where it is irrelevant where the compose
 * content is hosted (e.g. an Activity on Android). Such tests typically set compose content
 * themselves via [setContent][ComposeContentTestRule.setContent] and only instrument and assert
 * that content.
 *
 * For Android, this will use the default Activity (androidx.activity.ComponentActivity). You need
 * to add a reference to this activity into the manifest file of the corresponding tests (usually in
 * androidTest/AndroidManifest.xml). If your Android test requires a specific Activity to be
 * launched, see [createAndroidComposeRule].
 *
 * @param config The [ComposeUiTestConfig] is used to set up the test environment, providing control
 *   over the [CoroutineContext] used for composition, the test timeout, and other
 *   environment-specific settings.
 */
actual fun createComposeRule(config: ComposeUiTestConfig): ComposeContentTestRule {
    return createAndroidComposeRule<ComponentActivity>(config)
}

/**
 * Factory method to provide an implementation of [ComposeContentTestRule] configured via a
 * [ComposeUiTestConfig].
 *
 * This method is useful for tests in compose libraries where it is irrelevant where the compose
 * content is hosted (e.g. an Activity on Android). Such tests typically set compose content
 * themselves via [setContent][ComposeContentTestRule.setContent] and only instrument and assert
 * that content.
 *
 * For Android, this will use the default Activity (androidx.activity.ComponentActivity). You need
 * to add a reference to this activity into the manifest file of the corresponding tests (usually in
 * androidTest/AndroidManifest.xml). If your Android test requires a specific Activity to be
 * launched, see [createAndroidComposeRule].
 *
 * The default [ComposeUiTestConfig] sets the [InputMode][androidx.compose.ui.input.InputMode] to
 * [Touch][androidx.compose.ui.input.InputMode.Companion.Touch] for each test. To configure the test
 * to run with a different input mode (such as
 * [Keyboard][androidx.compose.ui.input.InputMode.Companion.Keyboard]) or customize other
 * environment settings, use the overload that accepts a [ComposeUiTestConfig].
 *
 * @see AndroidComposeUiTestFlags.isInputModeSetForDeviceTests
 */
@OptIn(ExperimentalTestApi::class)
@Suppress("DEPRECATION")
actual fun createComposeRule(): ComposeContentTestRule {
    return if (AndroidComposeUiTestFlags.isInputModeSetForDeviceTests) {
        // We set the timeout to INFINITE to retain the legacy behavior of not enforcing a timeout
        // for this overload. We are doing this to avoid breaking pre-existing tests with the
        // default 60-second timeout of ComposeUiTestConfig.
        createComposeRule(ComposeUiTestConfig(testTimeout = Duration.INFINITE))
    } else {
        createComposeRule(effectContext = EmptyCoroutineContext)
    }
}

/**
 * Factory method to provide android specific implementation of [createComposeRule], for a given
 * activity class type [A].
 *
 * This implementation uses [kotlinx.coroutines.test.StandardTestDispatcher] by default for running
 * composition. This ensures that the test behavior is consistent with
 * [kotlinx.coroutines.test.runTest] and provides explicit control over coroutine execution order.
 * This means you may need to explicitly advance time or run current coroutines when testing complex
 * coroutine logic, as tasks are queued on the scheduler rather than running eagerly.
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
 *   context contains a [TestDispatcher], it is used for composition and the [MainTestClock].
 *   Otherwise, a [kotlinx.coroutines.test.StandardTestDispatcher] is created and used. This new
 *   dispatcher will share the [TestCoroutineScheduler] from [effectContext] if one is present.
 */
inline fun <reified A : ComponentActivity> createAndroidComposeRule(
    effectContext: CoroutineContext = EmptyCoroutineContext
): AndroidComposeTestRule<ActivityScenarioRule<A>, A> {
    return createAndroidComposeRule(A::class.java, effectContext)
}

/**
 * Factory method to provide android specific implementation of [createComposeRule], configured via
 * a [ComposeUiTestConfig], for a given activity class type [A].
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
 * @param config The [ComposeUiTestConfig] used to set up the test environment, providing control
 *   over the [CoroutineContext] used for composition, the test timeout, and other
 *   environment-specific settings.
 */
inline fun <reified A : ComponentActivity> createAndroidComposeRule(
    config: ComposeUiTestConfig
): AndroidComposeTestRule<ActivityScenarioRule<A>, A> {
    return createAndroidComposeRule(A::class.java, config)
}

/**
 * Factory method to provide android specific implementation of [createComposeRule], configured via
 * a [ComposeUiTestConfig], for a given activity class type [A].
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
 * The default [ComposeUiTestConfig] sets the [InputMode][androidx.compose.ui.input.InputMode] to
 * [Touch][androidx.compose.ui.input.InputMode.Companion.Touch] for each test. To configure the test
 * to run with a different input mode (such as
 * [Keyboard][androidx.compose.ui.input.InputMode.Companion.Keyboard]) or customize other
 * environment settings, use the overload that accepts a [ComposeUiTestConfig].
 *
 * @see AndroidComposeUiTestFlags.isInputModeSetForDeviceTests
 */
@Suppress("DEPRECATION")
inline fun <reified A : ComponentActivity> createAndroidComposeRule():
    AndroidComposeTestRule<ActivityScenarioRule<A>, A> {
    return createAndroidComposeRule(A::class.java)
}

/**
 * Factory method to provide android specific implementation of [createComposeRule], for a given
 * [activityClass].
 *
 * This implementation uses [kotlinx.coroutines.test.StandardTestDispatcher] by default for running
 * composition. This ensures that the test behavior is consistent with
 * [kotlinx.coroutines.test.runTest] and provides explicit control over coroutine execution order.
 * This means you may need to explicitly advance time or run current coroutines when testing complex
 * coroutine logic, as tasks are queued on the scheduler rather than running eagerly.
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
 * @param activityClass The activity class to use in the activity scenario
 * @param effectContext The [CoroutineContext] used to run the composition. The context for
 *   `LaunchedEffect`s and `rememberCoroutineScope` will be derived from this context. If this
 *   context contains a [TestDispatcher], it is used for composition and the [MainTestClock].
 *   Otherwise, a [kotlinx.coroutines.test.StandardTestDispatcher] is created and used. This new
 *   dispatcher will share the [TestCoroutineScheduler] from [effectContext] if one is present.
 */
fun <A : ComponentActivity> createAndroidComposeRule(
    activityClass: Class<A>,
    effectContext: CoroutineContext = EmptyCoroutineContext,
): AndroidComposeTestRule<ActivityScenarioRule<A>, A> =
    AndroidComposeTestRule(
        activityRule = ActivityScenarioRule(activityClass),
        activityProvider = ::getActivityFromTestRule,
        config = ComposeUiTestConfig(effectContext = effectContext),
        enforceInputModeFromConfig = false,
    )

/**
 * Factory method to provide android specific implementation of [createComposeRule], configured via
 * a [ComposeUiTestConfig], for a given [activityClass].
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
 * @param activityClass The activity class to use in the activity scenario
 * @param config The [ComposeUiTestConfig] used to set up the test environment, providing control
 *   over the [CoroutineContext] used for composition, the test timeout, and other
 *   environment-specific settings.
 */
fun <A : ComponentActivity> createAndroidComposeRule(
    activityClass: Class<A>,
    config: ComposeUiTestConfig,
): AndroidComposeTestRule<ActivityScenarioRule<A>, A> =
    AndroidComposeTestRule(
        activityRule = ActivityScenarioRule(activityClass),
        activityProvider = ::getActivityFromTestRule,
        config = config,
    )

/**
 * Factory method to provide android specific implementation of [createComposeRule], configured via
 * a [ComposeUiTestConfig], for a given [activityClass].
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
 * The default [ComposeUiTestConfig] sets the [InputMode][androidx.compose.ui.input.InputMode] to
 * [Touch][androidx.compose.ui.input.InputMode.Companion.Touch] for each test. To configure the test
 * to run with a different input mode (such as
 * [Keyboard][androidx.compose.ui.input.InputMode.Companion.Keyboard]) or customize other
 * environment settings, use the overload that accepts a [ComposeUiTestConfig].
 *
 * @param activityClass The activity class to use in the activity scenario
 * @see AndroidComposeUiTestFlags.isInputModeSetForDeviceTests
 */
@OptIn(ExperimentalTestApi::class)
@Suppress("DEPRECATION")
fun <A : ComponentActivity> createAndroidComposeRule(
    activityClass: Class<A>
): AndroidComposeTestRule<ActivityScenarioRule<A>, A> =
    if (AndroidComposeUiTestFlags.isInputModeSetForDeviceTests) {
        // We set the timeout to INFINITE to retain the legacy behavior of not enforcing a timeout
        // for this overload. We are doing this to avoid breaking pre-existing tests with the
        // default 60-second timeout of ComposeUiTestConfig.
        createAndroidComposeRule(
            activityClass,
            ComposeUiTestConfig(testTimeout = Duration.INFINITE),
        )
    } else {
        createAndroidComposeRule(activityClass, effectContext = EmptyCoroutineContext)
    }

/**
 * Factory method to provide an implementation of [ComposeTestRule] that doesn't create a compose
 * host for you in which you can set content.
 *
 * This implementation uses [kotlinx.coroutines.test.StandardTestDispatcher] by default for running
 * composition. This ensures that the test behavior is consistent with
 * [kotlinx.coroutines.test.runTest] and provides explicit control over coroutine execution order.
 * This means you may need to explicitly advance time or run current coroutines when testing complex
 * coroutine logic, as tasks are queued on the scheduler rather than running eagerly.
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
 *   context contains a [TestDispatcher], it is used for composition and the [MainTestClock].
 *   Otherwise, a [kotlinx.coroutines.test.StandardTestDispatcher] is created and used. This new
 *   dispatcher will share the [TestCoroutineScheduler] from [effectContext] if one is present.
 */
fun createEmptyComposeRule(
    effectContext: CoroutineContext = EmptyCoroutineContext
): ComposeTestRule =
    AndroidComposeTestRule<TestRule, ComponentActivity>(
        activityRule = TestRule { base, _ -> base },
        config = ComposeUiTestConfig(effectContext = effectContext),
        activityProvider = {
            error(
                "createEmptyComposeRule() does not provide an Activity to set Compose content in." +
                    " Launch and use the Activity yourself, or use createAndroidComposeRule()."
            )
        },
        enforceInputModeFromConfig = false,
    )

/**
 * Factory method to provide an implementation of [ComposeTestRule], configured via an
 * [ComposeUiTestConfig], that doesn't create a compose host for you in which you can set content.
 *
 * This method is useful for tests that need to create their own compose host during the test. The
 * returned test rule will not create a host, and consequently does not provide a `setContent`
 * method. To set content in tests using this rule, use the appropriate `setContent` methods from
 * your compose host.
 *
 * A typical use case on Android is when the test needs to launch an Activity (the compose host)
 * after one or more dependencies have been injected.
 *
 * @param config The [ComposeUiTestConfig] used to set up the test environment, providing control
 *   over the [CoroutineContext] used for composition, the test timeout, and other
 *   environment-specific settings.
 */
fun createEmptyComposeRule(config: ComposeUiTestConfig): ComposeTestRule =
    AndroidComposeTestRule<TestRule, ComponentActivity>(
        activityRule = TestRule { base, _ -> base },
        config = config,
        activityProvider = {
            error(
                "createEmptyComposeRule() does not provide an Activity to set Compose content in." +
                    " Launch and use the Activity yourself, or use createAndroidComposeRule()."
            )
        },
    )

/**
 * Factory method to provide an implementation of [ComposeTestRule], configured via an
 * [ComposeUiTestConfig], that doesn't create a compose host for you in which you can set content.
 *
 * This method is useful for tests that need to create their own compose host during the test. The
 * returned test rule will not create a host, and consequently does not provide a `setContent`
 * method. To set content in tests using this rule, use the appropriate `setContent` methods from
 * your compose host.
 *
 * A typical use case on Android is when the test needs to launch an Activity (the compose host)
 * after one or more dependencies have been injected.
 *
 * The default [ComposeUiTestConfig] sets the [InputMode][androidx.compose.ui.input.InputMode] to
 * [Touch][androidx.compose.ui.input.InputMode.Companion.Touch] for each test. To configure the test
 * to run with a different input mode (such as
 * [Keyboard][androidx.compose.ui.input.InputMode.Companion.Keyboard]) or customize other
 * environment settings, use the overload that accepts a [ComposeUiTestConfig].
 *
 * @see AndroidComposeUiTestFlags.isInputModeSetForDeviceTests
 */
@Suppress("DEPRECATION")
@OptIn(ExperimentalTestApi::class)
fun createEmptyComposeRule(): ComposeTestRule =
    if (AndroidComposeUiTestFlags.isInputModeSetForDeviceTests) {
        // We set the timeout to INFINITE to retain the legacy behavior of not enforcing a timeout
        // for this overload. We are doing this to avoid breaking pre-existing tests with the
        // default 60-second timeout of ComposeUiTestConfig.
        createEmptyComposeRule(config = ComposeUiTestConfig(testTimeout = Duration.INFINITE))
    } else {
        createEmptyComposeRule(effectContext = EmptyCoroutineContext)
    }

/**
 * Factory method to provide an implementation of [AndroidComposeTestRule], where compose content is
 * hosted by an Activity.
 *
 * This implementation uses [kotlinx.coroutines.test.StandardTestDispatcher] by default for running
 * composition. This ensures that the test behavior is consistent with
 * [kotlinx.coroutines.test.runTest] and provides explicit control over coroutine execution order.
 * This means you may need to explicitly advance time or run current coroutines when testing complex
 * coroutine logic, as tasks are queued on the scheduler rather than running eagerly.
 *
 * The Activity is normally launched by the given [activityRule] before the test starts, but it is
 * possible to pass a test rule that chooses to launch an Activity on a later time. The Activity is
 * retrieved from the [activityRule] by means of the [activityProvider], which can be thought of as
 * a getter for the Activity on the [activityRule]. If you use an [activityRule] that launches an
 * Activity on a later time, you should make sure that the Activity is launched by the time or while
 * the [activityProvider] is called.
 *
 * The [AndroidComposeTestRule] wraps around the given [activityRule] to make sure the Activity is
 * launched _after_ the [AndroidComposeTestRule] has completed all necessary steps to control and
 * monitor the compose content.
 *
 * @param activityRule Test rule to use to launch the Activity.
 * @param effectContext The [CoroutineContext] used to run the composition. The context for
 *   `LaunchedEffect`s and `rememberCoroutineScope` will be derived from this context. If this
 *   context contains a [TestDispatcher] or [TestCoroutineScheduler] (in that order), it will be
 *   used for composition and the [MainTestClock].
 * @param activityProvider Function to retrieve the Activity from the given [activityRule].
 */
fun <R : TestRule, A : ComponentActivity> AndroidComposeTestRule(
    activityRule: R,
    effectContext: CoroutineContext = EmptyCoroutineContext,
    activityProvider: (R) -> A,
): AndroidComposeTestRule<R, A> {
    return AndroidComposeTestRule(
        activityRule = activityRule,
        config = ComposeUiTestConfig(effectContext = effectContext),
        activityProvider = activityProvider,
        enforceInputModeFromConfig = false,
    )
}

/**
 * Factory method to provide an implementation of [AndroidComposeTestRule], configured via an
 * [ComposeUiTestConfig], where compose content is hosted by an Activity.
 *
 * The Activity is normally launched by the given [activityRule] before the test starts, but it is
 * possible to pass a test rule that chooses to launch an Activity on a later time. The Activity is
 * retrieved from the [activityRule] by means of the [activityProvider], which can be thought of as
 * a getter for the Activity on the [activityRule]. If you use an [activityRule] that launches an
 * Activity on a later time, you should make sure that the Activity is launched by the time or while
 * the [activityProvider] is called.
 *
 * The [AndroidComposeTestRule] wraps around the given [activityRule] to make sure the Activity is
 * launched _after_ the [AndroidComposeTestRule] has completed all necessary steps to control and
 * monitor the compose content.
 *
 * @param activityRule Test rule to use to launch the Activity.
 * @param config The [ComposeUiTestConfig] used to set up the test environment, providing control
 *   over the [CoroutineContext] used for composition, the test timeout, and other
 *   environment-specific settings.
 * @param activityProvider Function to retrieve the Activity from the given [activityRule].
 */
fun <R : TestRule, A : ComponentActivity> AndroidComposeTestRule(
    activityRule: R,
    config: ComposeUiTestConfig,
    activityProvider: (R) -> A,
): AndroidComposeTestRule<R, A> {
    return AndroidComposeTestRule(
        activityRule = activityRule,
        config = config,
        activityProvider = activityProvider,
    )
}

/**
 * Factory method to provide an implementation of [AndroidComposeTestRule], configured via an
 * [ComposeUiTestConfig], where compose content is hosted by an Activity.
 *
 * The Activity is normally launched by the given [activityRule] before the test starts, but it is
 * possible to pass a test rule that chooses to launch an Activity on a later time. The Activity is
 * retrieved from the [activityRule] by means of the [activityProvider], which can be thought of as
 * a getter for the Activity on the [activityRule]. If you use an [activityRule] that launches an
 * Activity on a later time, you should make sure that the Activity is launched by the time or while
 * the [activityProvider] is called.
 *
 * The [AndroidComposeTestRule] wraps around the given [activityRule] to make sure the Activity is
 * launched _after_ the [AndroidComposeTestRule] has completed all necessary steps to control and
 * monitor the compose content.
 *
 * The default [ComposeUiTestConfig] sets the [InputMode][androidx.compose.ui.input.InputMode] to
 * [Touch][androidx.compose.ui.input.InputMode.Companion.Touch] for each test. To configure the test
 * to run with a different input mode (such as
 * [Keyboard][androidx.compose.ui.input.InputMode.Companion.Keyboard]) or customize other
 * environment settings, use the overload that accepts a [ComposeUiTestConfig].
 *
 * @param activityRule Test rule to use to launch the Activity.
 * @param activityProvider Function to retrieve the Activity from the given [activityRule].
 * @see AndroidComposeUiTestFlags.isInputModeSetForDeviceTests
 */
@OptIn(ExperimentalTestApi::class)
@Suppress("DEPRECATION")
fun <R : TestRule, A : ComponentActivity> AndroidComposeTestRule(
    activityRule: R,
    activityProvider: (R) -> A,
): AndroidComposeTestRule<R, A> {
    return if (AndroidComposeUiTestFlags.isInputModeSetForDeviceTests) {
        // We set the timeout to INFINITE to retain the legacy behavior of not enforcing a timeout
        // for this overload. We are doing this to avoid breaking pre-existing tests with the
        // default 60-second timeout of ComposeUiTestConfig.
        androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule(
            activityRule = activityRule,
            config = ComposeUiTestConfig(testTimeout = Duration.INFINITE),
            activityProvider = activityProvider,
        )
    } else {
        androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule(
            activityRule = activityRule,
            effectContext = EmptyCoroutineContext,
            activityProvider = activityProvider,
        )
    }
}
