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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assume
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** Default per-item timeout applied to suspending cleanups during [XrSessionRule] teardown. */
private val DEFAULT_CLEANUP_TIMEOUT = 10.seconds

/**
 * Provides the active [Session], [ComponentActivity], and LIFO cleanup registration inside
 * [XrSessionRule.onMainThread] and directly on [XrSessionRule].
 */
public interface XrSessionScope<out T : ComponentActivity> {
    /** The launched [T] activity instance for the active test. */
    public val activity: T

    /** The initialized [Session] for the active test. */
    public val session: Session

    /**
     * Registers [resource] to close on the Android main thread during LIFO teardown and returns it.
     *
     * * Register dependencies before the objects that use them (matching nested `use {}` blocks) so
     *   dependents clean up first.
     * * Must only be called while a test is running.
     *
     * @param resource [AutoCloseable] resource to close during teardown
     * @return [resource] for inline assignment
     * @throws IllegalStateException if called when no test is running
     */
    public fun <C : AutoCloseable> manage(resource: C): C

    /**
     * Registers [action] to run on the Android main thread during LIFO teardown.
     *
     * * Cleanups registered via [manage] and [onCleanup] share a single LIFO stack and run in
     *   strict reverse registration order.
     * * If [key] is non-null and a cleanup with the same [key] has already been registered for the
     *   current test, this call is a no-op.
     * * Must only be called while a test is running.
     *
     * @param key optional deduplication key scoped to the current test
     * @param action suspending cleanup lambda run on the main thread during teardown
     * @throws IllegalStateException if called when no test is running
     */
    public fun onCleanup(key: Any? = null, action: suspend () -> Unit)
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
 * Coordinates an [activityRule], initializes an XR [Session], and runs serial
 * instrumentation-thread test helpers.
 *
 * * Modeled after Compose's `AndroidComposeTestRule<R, A>`: swap in a custom [T] via
 *   [createAndroidXrSessionRule], run main-thread XR work via [onMainThread], register LIFO
 *   cleanups via [manage] and [onCleanup], poll asynchronous conditions via [waitUntil], and
 *   capture stabilized 3D compositor bitmaps via [captureToBitmap].
 * * Domain-specific helpers (such as SceneCore's `scene`, space mode transitions, `Entity`
 *   management, and `SpatialEnvironment` waits) belong in extension functions on [XrSessionScope]
 *   or [XrSessionRule] in those feature modules so `:xr:testutils` stays layered strictly on
 *   `:xr:runtime:runtime`.
 * * On devices without the XR spatial feature, skips tests via [Assume.assumeTrue] before launching
 *   [activityRule], keeping rule declaration order independent when paired with
 *   [XrScreenshotTestRule].
 *
 * @param activityRule test rule that launches and tears down [T] (typically an
 *   [ActivityScenarioRule])
 * @param cleanupTimeout maximum duration each suspending cleanup item may take before cancellation
 * @param activityProvider retrieves the launched [T] instance from [activityRule]
 */
public class XrSessionRule<R : TestRule, T : ComponentActivity>
internal constructor(
    public val activityRule: R,
    private val cleanupTimeout: Duration,
    private val sessionFactory: suspend (T) -> SessionCreateResult,
    private val activityProvider: (R) -> T,
) : TestRule, XrSessionScope<T> {

    /**
     * Constructs an [XrSessionRule] wrapping [activityRule].
     *
     * @param activityRule test rule that launches and tears down [T] (typically an
     *   [ActivityScenarioRule])
     * @param cleanupTimeout maximum duration each suspending cleanup item may take before
     *   cancellation
     * @param activityProvider retrieves the launched [T] instance from [activityRule]
     * @throws IllegalArgumentException if [cleanupTimeout] is not positive
     */
    public constructor(
        activityRule: R,
        cleanupTimeout: Duration = DEFAULT_CLEANUP_TIMEOUT,
        activityProvider: (R) -> T,
    ) : this(
        activityRule = activityRule,
        cleanupTimeout = cleanupTimeout,
        sessionFactory = { launchedActivity -> Session.create(context = launchedActivity) },
        activityProvider = activityProvider,
    )

    init {
        require(cleanupTimeout.isPositive()) {
            "cleanupTimeout must be positive, was $cleanupTimeout"
        }
    }

    /**
     * Wraps an exception or timeout thrown by an individual [manage] or [onCleanup] item during
     * [XrSessionRule] teardown.
     */
    public class CleanupException internal constructor(message: String, cause: Throwable) :
        RuntimeException(message, cause)

    private class CleanupItem(val label: String, val action: suspend () -> Unit)

    private val lock = Any()
    private var isTestRunning: Boolean = false
    private var activityOrNull: T? = null
    private var sessionOrNull: Session? = null
    private val cleanups = ArrayDeque<CleanupItem>()
    private val registeredKeys = LinkedHashSet<Any>()

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
                    cleanups.clear()
                    registeredKeys.clear()
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

                            var primaryFailure: Throwable? = null
                            val cleanupFailures = mutableListOf<Throwable>()
                            try {
                                base.evaluate()
                            } catch (t: Throwable) {
                                primaryFailure = t
                            } finally {
                                try {
                                    tearDown(cleanupFailures)
                                } catch (t: Throwable) {
                                    cleanupFailures.add(t)
                                } finally {
                                    synchronized(lock) {
                                        isTestRunning = false
                                        activityOrNull = null
                                        sessionOrNull = null
                                        cleanups.clear()
                                        registeredKeys.clear()
                                    }
                                }
                            }

                            if (primaryFailure != null) {
                                for (failure in cleanupFailures) {
                                    if (primaryFailure !== failure) {
                                        primaryFailure.addSuppressed(failure)
                                    }
                                }
                                throw primaryFailure
                            }
                            if (cleanupFailures.isNotEmpty()) {
                                val first = cleanupFailures.first()
                                for (failure in cleanupFailures.drop(1)) {
                                    first.addSuppressed(failure)
                                }
                                throw first
                            }
                        }
                    }

                activityRule.apply(sessionStatement, description).evaluate()
            }
        }

    override fun <C : AutoCloseable> manage(resource: C): C {
        synchronized(lock) {
            check(isTestRunning) {
                "XrSessionRule.manage is only available while a test is running."
            }
            cleanups.addLast(CleanupItem(label = "close $resource") { resource.close() })
        }
        return resource
    }

    override fun onCleanup(key: Any?, action: suspend () -> Unit) {
        synchronized(lock) {
            check(isTestRunning) {
                "XrSessionRule.onCleanup is only available while a test is running."
            }
            if (key != null && !registeredKeys.add(key)) {
                return
            }
            val label = if (key != null) "onCleanup ($key)" else "onCleanup #${cleanups.size}"
            cleanups.addLast(CleanupItem(label = label, action = action))
        }
    }

    /**
     * Runs [block] on the Android main thread with this [XrSessionScope] as receiver and blocks the
     * instrumentation thread until it completes.
     *
     * * Dispatches via `Dispatchers.Main.immediate` rather than blocking the main [Looper]
     *   synchronously, supporting both regular main-thread calls and `@MainThread suspend` XR APIs
     *   without deadlocking main-Looper callbacks.
     * * Exposes [activity], [session], [manage], and [onCleanup] directly on the receiver scope.
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
     * * Waits for active window transitions on [activity] to settle, captures a stabilized
     *   compositor frame, and registers the returned [Bitmap] with [manage] for recycling on
     *   teardown.
     * * Call [waitUntil] (or scene-inspection helpers such as
     *   [SpatialSceneHelper.waitForSpatialScene]) beforehand to await asynchronous scene or
     *   environment state.
     * * Must be called from the instrumentation thread while a test is running.
     *
     * @param cropBounds explicit pixel rectangle to crop from the compositor viewport, or `null`
     * @param padding extra padding in pixels preserved around cropped foreground bounds when
     *   [cropBounds] is `null` and [autoCrop] is `true`
     * @param autoCrop whether to crop out non-content background when [cropBounds] is `null`
     * @return software-backed [Bitmap.Config.ARGB_8888] [Bitmap] owned and auto-recycled by this
     *   rule
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
        val bitmap =
            SpatialSceneHelper.captureSceneToBitmap(
                cropBounds = cropBounds,
                padding = padding,
                autoCrop = autoCrop,
            )
        manage(
            object : AutoCloseable {
                override fun close() {
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }

                override fun toString(): String =
                    "captured Bitmap (${bitmap.width}x${bitmap.height})"
            }
        )
        return bitmap
    }

    private fun tearDown(cleanupFailures: MutableList<Throwable>) {
        runBlocking(Dispatchers.Main.immediate) {
            while (true) {
                val item = synchronized(lock) { cleanups.removeLastOrNull() } ?: break
                try {
                    withTimeout(cleanupTimeout) { item.action() }
                } catch (t: Throwable) {
                    val detail = t.message?.let { ": $it" } ?: ""
                    cleanupFailures.add(
                        CleanupException("Cleanup '${item.label}' failed$detail", t)
                    )
                }
            }
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
}
