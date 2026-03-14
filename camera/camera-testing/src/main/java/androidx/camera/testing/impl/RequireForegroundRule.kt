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

package androidx.camera.testing.impl

import android.annotation.SuppressLint
import android.app.Instrumentation
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.RemoteException
import android.util.Log
import androidx.camera.core.Logger
import androidx.camera.testing.impl.activity.ForegroundTestActivity
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.IOException
import org.junit.Assume.assumeTrue
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A JUnit [TestRule] that ensures a stable, focused foreground environment for UI-heavy tests.
 *
 * This rule automates the "clean room" setup required for Activity and Fragment tests:
 * 1. **Device Preparation**: Wakes the screen, dismisses the keyguard, and enables "stay on."
 * 2. **Orientation Lock**: Freezes the device in natural orientation to prevent unexpected
 *    recreations.
 * 3. **Foreground Verification**: Verifies that the app can actually obtain window focus. If a
 *    system dialog or notification shade is blocking the foreground, the rule throws a
 *    [ForegroundOccupiedError] (or skips the test in non-lab environments).
 *
 * ### Why use this instead of @Before/@After?
 * Standard `@After` blocks execute while the test Activity might still be visible or active. If you
 * shut down a singleton dependency (like `ProcessCameraProvider`) in `@After`, the Activity might
 * crash while trying to access that provider during its own destruction.
 *
 * This rule provides [withCleanup] and [deferCleanup] to ensure that teardown happens **only
 * after** the device has successfully returned to the Home screen and the Activity is gone.
 *
 * ### Call Sequence Diagram
 * 1. **Rule Setup**: [preTestCheck] runs (Kotlin suspend or Java Runnable).
 * 2. **Device Prep**: Screen wakes -> Orientation locked -> Navigates to Home.
 * 3. **Focus Check**: Launches a hidden activity to verify foreground focus is obtainable.
 * 4. **Test Body**: Your actual test code (and any `@Before` methods) executes.
 * 5. **Rule Reset**: Device returns Home and rotation is unfrozen.
 * 6. **Cleanup Tasks**: All tasks registered via [withCleanup] are executed in **LIFO** (Last-In,
 *    First-Out) order.
 *
 * ### Example: Kotlin (Suspend + Chaining)
 *
 * ```kotlin
 * @get:Rule
 * val foregroundRule = RequireForegroundRule {
 *     // Runs before UI prep. Good for initialization/assumptions.
 *     cameraProvider = ProcessCameraProvider.getInstance(context).await()
 * }.withCleanup {
 *     // Runs after the test is done and app is backgrounded.
 *     cameraProvider.shutdownAsync().await()
 * }
 * ```
 *
 * ### Example: Java (Runnable)
 *
 * ```java
 * @Rule
 * public RequireForegroundRule foregroundRule = new RequireForegroundRule(() -> {
 *     // Standard setup logic
 * }).withCleanup(() -> {
 *     // Standard cleanup logic
 * });
 * ```
 *
 * @param preTestCheck A suspending lambda or [Runnable] executed *before* heavy UI operations. If
 *   an assumption fails here, the test is skipped immediately.
 * @warning **JUnit Lifecycle Awareness**: If this rule skips a test (e.g., due to an assumption
 *   failure in [preTestCheck] or a occupied foreground), JUnit may **not** execute the class's
 *   `@Before` or `@After` methods. Always prefer registering cleanup tasks via [withCleanup] inside
 *   the rule to ensure they are handled correctly regardless of the test outcome.
 */
public class RequireForegroundRule(private val preTestCheck: suspend () -> Unit) : TestRule {

    /** Constructor for Java callers or standard non-suspending [Runnable] blocks. */
    public constructor(runnable: Runnable) : this(suspend { runnable.run() })

    /** Default constructor for when no pre-test check is needed. */
    public constructor() : this(suspend {})

    // Registry for delayed cleanup tasks
    private val cleanupTasks = mutableListOf<Runnable>()

    /**
     * Registers a synchronous cleanup task to be executed after the test finishes, the app is
     * safely backgrounded, and the UI rotation is unfrozen.
     *
     * This allows tests to keep their initialization and teardown logic logically grouped together
     * in the [@Before] block, avoiding lifecycle crashes caused by tearing down dependencies while
     * an Activity is still active or being recreated.
     *
     * Multiple tasks can be registered. They will be executed in reverse order (Last-In, First-Out)
     * during the Rule's final teardown phase.
     *
     * @param task The synchronous cleanup task to run.
     */
    public fun deferCleanup(task: Runnable) {
        cleanupTasks.add(task)
    }

    public fun withCleanup(task: Runnable): RequireForegroundRule =
        withCleanup(suspend { task.run() })

    /**
     * Registers a suspending cleanup task to be executed after the test finishes, the app is safely
     * backgrounded, and the UI rotation is unfrozen.
     *
     * This is a Kotlin-friendly convenience method that automatically executes the suspending task
     * within a `runBlocking` block during the Rule's teardown phase.
     *
     * @param task The suspending cleanup task to run.
     */
    public fun deferCleanup(task: suspend () -> Unit) {
        cleanupTasks.add(Runnable { kotlinx.coroutines.runBlocking { task() } })
    }

    /** Registers a task and returns 'this' for a builder-style flow. */
    public fun withCleanup(task: suspend () -> Unit): RequireForegroundRule {
        deferCleanup(task)
        return this
    }

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                var testError: Throwable? = null

                try {
                    // Run early assumptions BEFORE heavy UI operations.
                    kotlinx.coroutines.runBlocking { preTestCheck() }

                    val instrumentation = InstrumentationRegistry.getInstrumentation()
                    val device = UiDevice.getInstance(instrumentation)

                    device.setOrientationNatural()
                    device.waitForIdle(INITIAL_IDLE_TIMEOUT_MS)
                    prepareDeviceUI(instrumentation)

                    try {
                        base.evaluate()
                    } catch (t: Throwable) {
                        // Capture the test failure so it isn't swallowed
                        testError = t
                    } finally {
                        try {
                            backToHome(instrumentation)
                            device.unfreezeRotation()
                            device.waitForIdle(UI_STABILIZATION_TIMEOUT_MS)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to gracefully clean up device state", e)
                        }
                    }
                } catch (t: Throwable) {
                    // Capture failures from preTestCheck or prepareDeviceUI if test hasn't run
                    if (testError == null) testError = t
                } finally {
                    // Deferred Cleanup Tasks
                    val cleanupErrors = mutableListOf<Throwable>()
                    cleanupTasks.reversed().forEach { task ->
                        try {
                            task.run()
                        } catch (t: Throwable) {
                            cleanupErrors.add(t)
                        }
                    }
                    cleanupTasks.clear()

                    // Final Exception Arbitration
                    if (testError != null) {
                        // If the test failed, it is the primary exception.
                        // Attach all cleanup errors to it.
                        cleanupErrors.forEach { testError.addSuppressed(it) }
                        throw testError
                    } else if (cleanupErrors.isNotEmpty()) {
                        // If test passed but cleanup failed, throw the first cleanup error.
                        val firstCleanupError = cleanupErrors.first()
                        cleanupErrors.drop(1).forEach { firstCleanupError.addSuppressed(it) }
                        throw firstCleanupError
                    }
                }
            }
        }

    public companion object {
        private const val TAG = "RequireForegroundRule"
        private const val INITIAL_IDLE_TIMEOUT_MS: Long = 2000
        private const val UI_STABILIZATION_TIMEOUT_MS: Long = 3000
        private const val DISMISS_LOCK_SCREEN_CODE = 82
        private const val ADB_SHELL_DISMISS_KEYGUARD_API23_AND_ABOVE = "wm dismiss-keyguard"
        private const val ADB_SHELL_SCREEN_ALWAYS_ON = "svc power stayon true"

        /** The display foreground of the device is occupied that cannot execute UI related test. */
        public class ForegroundOccupiedError(message: String) : Exception(message)

        /**
         * Navigates to the home screen using an Intent.
         *
         * This is preferred over UiDevice.pressHome() because it is less likely to trigger system
         * selection dialogs on certain devices. Please see b/489937303#comment2
         */
        @JvmStatic
        public fun backToHome(instrumentation: Instrumentation) {
            try {
                val intent =
                    Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                instrumentation.context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.e(TAG, "Home screen not found, falling back to keyevent: ${e.message}")
                try {
                    UiDevice.getInstance(instrumentation).executeShellCommand("input keyevent 3")
                } catch (ioException: IOException) {
                    Log.e(TAG, "Failed to execute home keyevent", ioException)
                }
            } catch (_: SecurityException) {
                Log.e(TAG, "Permission denied to start home activity")
            }
        }

        /**
         * Try to clear the UI and check if there is any dialog or lock screen on the top of the
         * window that might cause the activity related test fail.
         */
        @JvmStatic
        @Throws(ForegroundOccupiedError::class)
        public fun prepareDeviceUI(instrumentation: Instrumentation) {
            clearDeviceUI(instrumentation)

            var activityRef: ForegroundTestActivity? = null
            try {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val startIntent =
                    Intent(Intent.ACTION_MAIN).apply {
                        setClassName(context.packageName, ForegroundTestActivity::class.java.name)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                activityRef =
                    instrumentation.startActivitySync(startIntent) as ForegroundTestActivity
                instrumentation.waitForIdleSync()

                IdlingRegistry.getInstance().register(activityRef.viewReadyIdlingResource)
                Espresso.onIdle()
                return
            } catch (e: Exception) {
                Logger.d(TAG, "Fail to get foreground", e)
            } finally {
                if (activityRef != null) {
                    IdlingRegistry.getInstance().unregister(activityRef.viewReadyIdlingResource)
                    instrumentation.runOnMainSync { activityRef.finish() }
                    instrumentation.waitForIdleSync()
                }
            }

            // Throw AssumptionViolatedException to skip the test if not in the CameraX lab
            // environment. The loggable tag will be set when running the CameraX daily testing.
            assumeTrue(Log.isLoggable("MH", Log.DEBUG))
            throw ForegroundOccupiedError("CameraX_fail_to_start_foreground, model: ${Build.MODEL}")
        }

        @JvmStatic
        @SuppressLint("MissingPermission", "ObsoleteSdkInt")
        @Suppress("DEPRECATION")
        public fun clearDeviceUI(instrumentation: Instrumentation) {
            val device = UiDevice.getInstance(instrumentation)

            try {
                device.wakeUp()
            } catch (_: RemoteException) {}

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                device.pressKeyCode(DISMISS_LOCK_SCREEN_CODE)
            } else {
                try {
                    device.executeShellCommand(ADB_SHELL_DISMISS_KEYGUARD_API23_AND_ABOVE)
                } catch (_: IOException) {}
            }

            try {
                device.executeShellCommand(ADB_SHELL_SCREEN_ALWAYS_ON)
            } catch (_: IOException) {}

            backToHome(instrumentation)

            try {
                device.waitForIdle(UI_STABILIZATION_TIMEOUT_MS)
            } catch (e: IllegalStateException) {
                Logger.d(TAG, "Fail to waitForIdle", e)
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                instrumentation.targetContext.sendBroadcast(
                    Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
                )
            }
        }
    }
}
