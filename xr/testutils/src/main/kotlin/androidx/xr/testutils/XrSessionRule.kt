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

package androidx.xr.testutils

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateResult
import androidx.xr.runtime.SessionCreateSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Exposes the active [Session] and [ComponentActivity] inside [XrSessionRule.onMainThread] and on
 * [XrSessionRule].
 */
public interface XrSessionScope<out T : ComponentActivity> {
    /** The launched [T] activity instance for the active test. */
    public val activity: T

    /** The initialized [Session] for the active test. */
    public val session: Session
}

/**
 * Creates an [XrSessionRule] that launches a default [ComponentActivity].
 *
 * Modeled after Compose's `createComposeRule()`. If your test requires a custom [ComponentActivity]
 * subclass or launch rule, use [createAndroidXrSessionRule] or construct [XrSessionRule] directly.
 */
public fun createXrSessionRule():
    XrSessionRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity> =
    createAndroidXrSessionRule()

/**
 * Creates an [XrSessionRule] that launches activity class [T] and exposes typed [T] via
 * [XrSessionRule.activity].
 *
 * Modeled after Compose's `createAndroidComposeRule<A>()`. If your test does not require a specific
 * activity subclass, use [createXrSessionRule] instead.
 */
public inline fun <reified T : ComponentActivity> createAndroidXrSessionRule():
    XrSessionRule<ActivityScenarioRule<T>, T> = createAndroidXrSessionRule(T::class.java)

/**
 * Creates an [XrSessionRule] for [activityClass], modeled after Compose's
 * `createAndroidComposeRule(Class<A>)`.
 */
public fun <T : ComponentActivity> createAndroidXrSessionRule(
    activityClass: Class<T>
): XrSessionRule<ActivityScenarioRule<T>, T> =
    createAndroidXrSessionRule(ActivityScenarioRule(activityClass))

/**
 * Creates an [XrSessionRule] backed by a caller-configured [activityRule] (for example, an
 * [ActivityScenarioRule] with a custom launch [android.content.Intent]).
 */
public fun <T : ComponentActivity> createAndroidXrSessionRule(
    activityRule: ActivityScenarioRule<T>
): XrSessionRule<ActivityScenarioRule<T>, T> =
    XrSessionRule(activityRule = activityRule, activityProvider = ::getActivityFromTestRule)

private fun <T : ComponentActivity> getActivityFromTestRule(rule: ActivityScenarioRule<T>): T {
    var activity: T? = null
    rule.scenario.onActivity { activity = it }
    return checkNotNull(activity) { "Activity was not set in the ActivityScenarioRule!" }
}

/**
 * Coordinates an [activityRule], creates an XR [Session], and runs instrumentation-thread test
 * helpers.
 *
 * * Modeled after Compose's `AndroidComposeTestRule<R, A>`: swap in a custom [T] via
 *   [createAndroidXrSessionRule], run main-thread XR work via [onMainThread], poll asynchronous
 *   conditions via [waitUntil], and capture stabilized 3D compositor bitmaps via [captureToBitmap].
 * * Domain-specific helpers (such as space mode transitions and `SpatialEnvironment` waits) belong
 *   in extension functions on [XrSessionScope] or [XrSessionRule] in those feature modules so
 *   `:xr:testutils` depends only on `:xr:runtime:runtime`.
 * * Destroying [activity] in [activityRule] triggers [Session] and runtime teardown at the end of
 *   each test.
 * * On devices without the XR spatial feature, skips tests via [Assume.assumeTrue] before launching
 *   [activityRule], keeping rule declaration order independent when paired with
 *   [XrScreenshotTestRule].
 *
 * @param activityRule test rule that launches and tears down [T] (typically an
 *   [ActivityScenarioRule])
 * @param activityProvider retrieves the launched [T] instance from [activityRule]
 */
public class XrSessionRule<R : TestRule, T : ComponentActivity>
internal constructor(
    public val activityRule: R,
    private val sessionFactory: suspend (T) -> SessionCreateResult,
    private val activityProvider: (R) -> T,
) : TestRule, XrSessionScope<T> {

    /**
     * Constructs an [XrSessionRule] wrapping [activityRule].
     *
     * @param activityRule test rule that launches and tears down [T] (typically an
     *   [ActivityScenarioRule])
     * @param activityProvider retrieves the launched [T] instance from [activityRule]
     */
    public constructor(
        activityRule: R,
        activityProvider: (R) -> T,
    ) : this(
        activityRule = activityRule,
        sessionFactory = { launchedActivity -> Session.create(context = launchedActivity) },
        activityProvider = activityProvider,
    )

    private val lock = Any()
    private var isTestRunning: Boolean = false
    private var activityOrNull: T? = null
    private var sessionOrNull: Session? = null

    /** The launched [T] activity instance for the active test. */
    override val activity: T
        get() =
            synchronized(lock) {
                checkNotNull(activityOrNull) {
                    "XrSessionRule.activity is only available while a test is running."
                }
            }

    /** The initialized [Session] for the active test. */
    override val session: Session
        get() =
            synchronized(lock) {
                checkNotNull(sessionOrNull) {
                    "XrSessionRule.session is only available while a test is running."
                }
            }

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                check(Looper.myLooper() != Looper.getMainLooper()) {
                    "XrSessionRule must run on the instrumentation thread, not the main thread."
                }
                Assume.assumeTrue("Requires an XR device", isXrDevice())
                synchronized(lock) {
                    check(!isTestRunning) {
                        "XrSessionRule cannot evaluate concurrent or nested tests."
                    }
                }

                val sessionStatement =
                    object : Statement() {
                        override fun evaluate() {
                            val launchedActivity = activityProvider(activityRule)
                            val sessionResult =
                                runBlocking(Dispatchers.Main.immediate) {
                                    sessionFactory(launchedActivity)
                                }
                            if (sessionResult !is SessionCreateSuccess) {
                                throw AssertionError(
                                    "Expected SessionCreateSuccess from Session.create, " +
                                        "but got $sessionResult"
                                )
                            }
                            val createdSession = sessionResult.session

                            synchronized(lock) {
                                activityOrNull = launchedActivity
                                sessionOrNull = createdSession
                                isTestRunning = true
                            }

                            try {
                                base.evaluate()
                            } finally {
                                synchronized(lock) {
                                    isTestRunning = false
                                    activityOrNull = null
                                    sessionOrNull = null
                                }
                            }
                        }
                    }

                activityRule.apply(sessionStatement, description).evaluate()
            }
        }

    /**
     * Runs [block] on the Android main thread with this [XrSessionScope] as receiver and blocks the
     * instrumentation thread until it completes.
     *
     * * Dispatches via `Dispatchers.Main.immediate` rather than blocking the main [Looper]
     *   synchronously, supporting both regular main-thread calls and `@MainThread suspend` XR APIs
     *   without deadlocking main-Looper callbacks.
     * * Exposes [activity] and [session] directly on the receiver scope.
     * * Must be called from the instrumentation thread while a test is running.
     *
     * @param block suspending main-thread lambda invoked on this [XrSessionScope]
     * @return result computed by [block]
     * @throws IllegalStateException if called on the main thread or when no test is running
     */
    public fun <V> onMainThread(block: suspend XrSessionScope<T>.() -> V): V {
        check(Looper.myLooper() != Looper.getMainLooper()) {
            "onMainThread must be called from the instrumentation thread, not the main thread."
        }
        synchronized(lock) {
            check(isTestRunning) {
                "XrSessionRule.onMainThread is only available while a test is running."
            }
        }
        return runBlocking(Dispatchers.Main.immediate) { this@XrSessionRule.block() }
    }

    /**
     * Captures the settled 3D compositor viewport as a software [Bitmap.Config.ARGB_8888] [Bitmap].
     *
     * * Waits for active window transitions on [activity] to settle and captures a stabilized
     *   compositor frame.
     * * Call [waitUntil] (or scene-inspection helpers such as
     *   [SpatialSceneHelper.waitForSpatialScene]) beforehand to await asynchronous scene or
     *   environment state.
     * * Must be called from the instrumentation thread while a test is running.
     *
     * @param cropBounds explicit pixel rectangle to crop from the compositor viewport, or `null`
     * @param padding extra padding in pixels preserved around cropped foreground bounds when
     *   [cropBounds] is `null` and [autoCrop] is `true`
     * @param autoCrop whether to crop out non-content background when [cropBounds] is `null`
     * @return software-backed [Bitmap.Config.ARGB_8888] [Bitmap] owned by the caller (call
     *   [Bitmap.recycle] if releasing eagerly)
     * @throws IllegalArgumentException if [padding] is negative
     * @throws IllegalStateException if called from the main thread or when no test is running
     * @throws AssertionError if the scene or compositor frame fails to settle within timeout
     */
    @JvmOverloads
    public fun captureToBitmap(
        cropBounds: Rect? = null,
        padding: Int = 0,
        autoCrop: Boolean = false,
    ): Bitmap {
        require(padding >= 0) { "padding must be non-negative, was $padding" }
        check(Looper.myLooper() != Looper.getMainLooper()) {
            "captureToBitmap must be called from the instrumentation thread, not the main thread."
        }
        val activeActivity = activity
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        SpatialSceneHelper.waitForSettledSpatialTask(
            activity = activeActivity,
            requireFullSpace = false,
            timeout = DEFAULT_WAIT_TIMEOUT,
        )
        return SpatialSceneHelper.captureSceneToBitmap(
            cropBounds = cropBounds,
            padding = padding,
            autoCrop = autoCrop,
        )
    }
}
