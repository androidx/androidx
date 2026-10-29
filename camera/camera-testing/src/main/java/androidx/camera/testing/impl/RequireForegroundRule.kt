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
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.AssumptionViolatedException
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
 * 3. **Foreground Verification**: Verifies that the app can actually obtain window focus before the
 *    test starts, and monitors mid-test activity lifecycle transitions for unsolicited foreign
 *    packages stealing window focus. If a system dialog, notification shade, or third-party/OEM app
 *    blocks or steals the foreground, the rule throws a [ForegroundOccupiedError] in lab
 *    environments (or skips the test via [AssumptionViolatedException] outside the lab).
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
 * 4. **Test Body**: Your actual test code (and any `@Before` methods) executes while monitoring for
 *    mid-test foreground loss to foreign packages.
 * 5. **Rule Reset**: Any foreign foreground occupier is force-stopped (in lab environments), the
 *    device returns Home, and rotation is unfrozen.
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
                    val device = runBlocking { getUiDevice(instrumentation) }

                    device.setOrientationNatural()
                    device.waitForIdle(INITIAL_IDLE_TIMEOUT_MS)
                    prepareDeviceUI(instrumentation)

                    val testPackage = instrumentation.context.packageName
                    val targetPackage = instrumentation.targetContext.packageName
                    val launcherPackage =
                        try {
                            device.launcherPackageName
                        } catch (_: Throwable) {
                            null
                        }
                    val midTestOccupier = AtomicReference<String?>(null)
                    val resumeGeneration = AtomicLong(0)
                    val pendingForegroundCheck = AtomicReference<Future<*>?>(null)
                    val foregroundCheckExecutor =
                        Executors.newSingleThreadScheduledExecutor { runnable ->
                            Thread(runnable, "RequireForegroundRule-Monitor").apply {
                                isDaemon = true
                            }
                        }
                    val lifecycleMonitor = ActivityLifecycleMonitorRegistry.getInstance()
                    val lifecycleCallback = ActivityLifecycleCallback { activity, stage ->
                        onActivityLifecycleChanged(
                            stage = stage,
                            isFinishing = activity.isFinishing,
                            isChangingConfigurations = activity.isChangingConfigurations,
                            midTestOccupier = midTestOccupier,
                            resumeGeneration = resumeGeneration,
                            pendingCheck = pendingForegroundCheck,
                            scheduleDelayedCheck = { task ->
                                try {
                                    // Query rootInActiveWindow off the main thread after a brief
                                    // settle window so:
                                    // 1. UiAutomation accessibility IPC never blocks or
                                    //    self-deadlocks the Main Looper during activity pause/stop
                                    //    transitions, and
                                    // 2. ActivityTaskManagerService has transferred window focus to
                                    //    the incoming window (including PAUSED-only dialogs).
                                    foregroundCheckExecutor.schedule(
                                        task,
                                        FOREGROUND_CHECK_DELAY_MS,
                                        TimeUnit.MILLISECONDS,
                                    )
                                } catch (_: RejectedExecutionException) {
                                    null
                                }
                            },
                            activePackageProvider = {
                                try {
                                    instrumentation.uiAutomation.rootInActiveWindow
                                        ?.packageName
                                        ?.toString()
                                } catch (_: Throwable) {
                                    null
                                }
                            },
                            testPackage = testPackage,
                            targetPackage = targetPackage,
                            launcherPackage = launcherPackage,
                        )
                    }
                    lifecycleMonitor.addLifecycleCallback(lifecycleCallback)

                    try {
                        base.evaluate()
                    } catch (t: Throwable) {
                        // Capture the test failure so it isn't swallowed, mapping mid-test
                        // foreground theft by a foreign package to ForegroundOccupiedError / skip.
                        testError =
                            try {
                                handlePotentialForegroundTheft(
                                    testError = t,
                                    instrumentation = instrumentation,
                                    device = device,
                                    launcherPackage = launcherPackage,
                                    recordedOccupierPackage = midTestOccupier.get(),
                                )
                            } catch (mappingError: Throwable) {
                                t.also { it.addSuppressed(mappingError) }
                            }
                    } finally {
                        lifecycleMonitor.removeLifecycleCallback(lifecycleCallback)
                        pendingForegroundCheck.getAndSet(null)?.cancel(false)
                        foregroundCheckExecutor.shutdownNow()
                        try {
                            backToHome(instrumentation)
                            device.unfreezeRotation()
                            device.waitForIdle(UI_STABILIZATION_TIMEOUT_MS)
                        } catch (t: Throwable) {
                            Log.w(TAG, "Failed to gracefully clean up device state", t)
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
        private const val MH_TAG = "MH"
        private const val INITIAL_IDLE_TIMEOUT_MS: Long = 2000
        private const val UI_STABILIZATION_TIMEOUT_MS: Long = 3000
        private const val FOREGROUND_CHECK_DELAY_MS: Long = 200
        private const val DISMISS_LOCK_SCREEN_CODE = 82
        private const val ADB_SHELL_DISMISS_KEYGUARD_API23_AND_ABOVE = "wm dismiss-keyguard"
        private const val ADB_SHELL_SCREEN_ALWAYS_ON = "svc power stayon true"
        private const val ADB_SHELL_DISABLE_CAMERA_SENSOR_PRIVACY_API31_AND_ABOVE =
            "cmd sensor_privacy disable 0 camera"
        private const val ADB_SHELL_DISABLE_MICROPHONE_SENSOR_PRIVACY_API31_AND_ABOVE =
            "cmd sensor_privacy disable 0 microphone"
        private const val ADB_SHELL_FORCE_STOP_HEYTAP_MARKET = "am force-stop com.heytap.market"
        private const val XIAOMI_OP_BACKGROUND_START_ACTIVITY = 10021
        private val SAFE_PACKAGE_NAME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$")
        private val SYSTEM_ALLOWED_FOREGROUND_PACKAGES =
            setOf("android", "com.android.systemui", "com.android.settings")
        private val NEVER_FORCE_STOP_PACKAGES =
            setOf("com.google.android.gms", "com.google.android.gsf", "com.android.vending")

        /** The display foreground of the device is occupied that cannot execute UI related test. */
        public class ForegroundOccupiedError
        @JvmOverloads
        public constructor(message: String, cause: Throwable? = null) : Exception(message, cause)

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
                    runBlocking { getUiDevice(instrumentation) }
                        .executeShellCommand("input keyevent 3")
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

                var retryCount = 0
                while (retryCount < 3) {
                    try {
                        activityRef =
                            instrumentation.startActivitySync(startIntent) as ForegroundTestActivity
                        break
                    } catch (e: RuntimeException) {
                        Logger.w(
                            TAG,
                            "Failed to launch ForegroundTestActivity on attempt $retryCount",
                            e,
                        )
                        retryCount++
                        if (retryCount >= 3) throw e
                        clearDeviceUI(instrumentation)
                    }
                }
                instrumentation.waitForIdleSync()

                IdlingRegistry.getInstance().register(activityRef!!.viewReadyIdlingResource)
                Espresso.onIdle()
                return
            } catch (e: Exception) {
                Logger.d(TAG, "Fail to get foreground", e)
            } finally {
                if (activityRef != null) {
                    IdlingRegistry.getInstance().unregister(activityRef!!.viewReadyIdlingResource)
                    instrumentation.runOnMainSync { activityRef?.finish() }
                    instrumentation.waitForIdleSync()
                }
            }

            // Throw AssumptionViolatedException to skip the test if not in the CameraX lab
            // environment. The loggable tag will be set when running the CameraX daily testing.
            assumeTrue(Log.isLoggable(MH_TAG, Log.DEBUG))
            throw ForegroundOccupiedError("CameraX_fail_to_start_foreground, model: ${Build.MODEL}")
        }

        @JvmStatic
        @SuppressLint("MissingPermission", "ObsoleteSdkInt")
        @Suppress("DEPRECATION")
        public fun clearDeviceUI(instrumentation: Instrumentation) {
            val device = runBlocking { getUiDevice(instrumentation) }

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

            // MIUI/HyperOS custom op OP_BACKGROUND_START_ACTIVITY (10021): allow background
            // activity starts.
            if (isXiaomiDevice()) {
                for (pkg in
                    setOf(
                        instrumentation.context.packageName,
                        instrumentation.targetContext.packageName,
                    )) {
                    try {
                        device.executeShellCommand(
                            "appops set $pkg $XIAOMI_OP_BACKGROUND_START_ACTIVITY allow"
                        )
                    } catch (_: IOException) {}
                }
            }

            // ColorOS/OxygenOS/Realme UI pre-installed HeyTap App Market can launch unsolicited
            // exported activities (e.g. com.heytap.market/.activity.TranslateActivity) mid-test
            // and steal window focus from the test Activity. Force-stop it in lab environments.
            if (isOplusDevice() && Log.isLoggable(MH_TAG, Log.DEBUG)) {
                try {
                    device.executeShellCommand(ADB_SHELL_FORCE_STOP_HEYTAP_MARKET)
                } catch (_: IOException) {}
            }

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
            } else {
                try {
                    device.executeShellCommand(
                        ADB_SHELL_DISABLE_CAMERA_SENSOR_PRIVACY_API31_AND_ABOVE
                    )
                } catch (_: IOException) {}
                try {
                    device.executeShellCommand(
                        ADB_SHELL_DISABLE_MICROPHONE_SENSOR_PRIVACY_API31_AND_ABOVE
                    )
                } catch (_: IOException) {}
                try {
                    val currentUser = device.executeShellCommand("am get-current-user").trim()
                    if (currentUser.isNotEmpty() && currentUser != "0") {
                        device.executeShellCommand("cmd sensor_privacy disable $currentUser camera")
                        device.executeShellCommand(
                            "cmd sensor_privacy disable $currentUser microphone"
                        )
                    }
                } catch (_: IOException) {}
            }
        }

        internal suspend fun getUiDevice(instrumentation: Instrumentation): UiDevice {
            var lastException: Exception? = null
            for (i in 0 until 3) {
                try {
                    // Try to trigger connection by accessing uiAutomation
                    instrumentation.uiAutomation
                    return UiDevice.getInstance(instrumentation)
                } catch (e: IllegalStateException) {
                    lastException = e
                    Logger.w(TAG, "Failed to get UiDevice instance, attempt ${i + 1}", e)
                    if (i < 2) {
                        delay(500)
                    }
                }
            }
            throw IllegalStateException("Failed to get UiDevice after retries", lastException)
        }

        internal fun onActivityLifecycleChanged(
            stage: Stage,
            isFinishing: Boolean,
            isChangingConfigurations: Boolean,
            midTestOccupier: AtomicReference<String?>,
            resumeGeneration: AtomicLong,
            pendingCheck: AtomicReference<Future<*>?>,
            scheduleDelayedCheck: (Runnable) -> Future<*>?,
            activePackageProvider: () -> String?,
            testPackage: String,
            targetPackage: String,
            launcherPackage: String?,
        ) {
            if (stage == Stage.RESUMED) {
                resumeGeneration.incrementAndGet()
                pendingCheck.getAndSet(null)?.cancel(false)
                midTestOccupier.set(null)
                return
            }
            if (
                (stage == Stage.PAUSED || stage == Stage.STOPPED) &&
                    !isFinishing &&
                    !isChangingConfigurations &&
                    midTestOccupier.get() == null
            ) {
                val generationAtPause = resumeGeneration.get()
                val checkTask = Runnable {
                    if (
                        resumeGeneration.get() != generationAtPause || midTestOccupier.get() != null
                    ) {
                        return@Runnable
                    }
                    val activePkg = activePackageProvider()
                    if (
                        resumeGeneration.get() == generationAtPause &&
                            isForeignForegroundPackage(
                                currentPackage = activePkg,
                                testPackage = testPackage,
                                targetPackage = targetPackage,
                                launcherPackage = launcherPackage,
                            )
                    ) {
                        midTestOccupier.compareAndSet(null, activePkg)
                        if (resumeGeneration.get() != generationAtPause) {
                            midTestOccupier.compareAndSet(activePkg, null)
                        }
                    }
                }
                pendingCheck.getAndSet(scheduleDelayedCheck(checkTask))?.cancel(false)
            }
        }

        private fun handlePotentialForegroundTheft(
            testError: Throwable,
            instrumentation: Instrumentation,
            device: UiDevice,
            launcherPackage: String?,
            recordedOccupierPackage: String?,
        ): Throwable {
            if (isNonRemappable(testError)) {
                return testError
            }
            val currentPackage =
                recordedOccupierPackage
                    ?: try {
                        device.currentPackageName
                    } catch (t: Throwable) {
                        Logger.d(TAG, "Failed to query currentPackageName after test failure", t)
                        null
                    }
            return mapFailureOnForegroundLoss(
                testError = testError,
                currentPackage = currentPackage,
                testPackage = instrumentation.context.packageName,
                targetPackage = instrumentation.targetContext.packageName,
                launcherPackage = launcherPackage,
                isLabEnvironment = Log.isLoggable(MH_TAG, Log.DEBUG),
                onForceStopPackage = { pkg -> device.executeShellCommand("am force-stop $pkg") },
            )
        }

        internal fun mapFailureOnForegroundLoss(
            testError: Throwable,
            currentPackage: String?,
            testPackage: String,
            targetPackage: String,
            launcherPackage: String?,
            isLabEnvironment: Boolean,
            onForceStopPackage: (String) -> Unit,
        ): Throwable {
            if (isNonRemappable(testError)) {
                return testError
            }
            if (
                !isForeignForegroundPackage(
                    currentPackage = currentPackage,
                    testPackage = testPackage,
                    targetPackage = targetPackage,
                    launcherPackage = launcherPackage,
                )
            ) {
                return testError
            }
            val occupierPkg = currentPackage!!
            Logger.w(
                TAG,
                "Test failed while foreign package '$occupierPkg' occupied the foreground",
                testError,
            )
            if (
                isLabEnvironment &&
                    isSafePackageName(occupierPkg) &&
                    occupierPkg !in NEVER_FORCE_STOP_PACKAGES
            ) {
                try {
                    onForceStopPackage(occupierPkg)
                } catch (e: Exception) {
                    Logger.w(TAG, "Failed to force-stop foreground occupier $occupierPkg", e)
                }
            }
            val message =
                "CameraX_fail_to_start_foreground (occupied by $occupierPkg), model: ${Build.MODEL}"
            return if (isLabEnvironment) {
                ForegroundOccupiedError(message, testError)
            } else {
                AssumptionViolatedException(message, testError)
            }
        }

        @Suppress("DEPRECATION")
        internal fun isNonRemappable(testError: Throwable): Boolean =
            testError is ForegroundOccupiedError ||
                testError is AssumptionViolatedException ||
                testError is org.junit.internal.AssumptionViolatedException ||
                testError is VirtualMachineError

        internal fun isForeignForegroundPackage(
            currentPackage: String?,
            testPackage: String,
            targetPackage: String,
            launcherPackage: String?,
        ): Boolean {
            if (currentPackage.isNullOrBlank()) {
                return false
            }
            if (
                currentPackage == testPackage ||
                    currentPackage == targetPackage ||
                    currentPackage == launcherPackage ||
                    currentPackage in SYSTEM_ALLOWED_FOREGROUND_PACKAGES
            ) {
                return false
            }
            if (
                currentPackage.startsWith("androidx.camera.") ||
                    currentPackage.startsWith("androidx.test.")
            ) {
                return false
            }
            if (
                currentPackage.contains("launcher", ignoreCase = true) ||
                    currentPackage.endsWith(".home", ignoreCase = true) ||
                    currentPackage.contains(".home.", ignoreCase = true) ||
                    currentPackage.contains("permissioncontroller", ignoreCase = true) ||
                    currentPackage.contains("packageinstaller", ignoreCase = true)
            ) {
                return false
            }
            return true
        }

        internal fun isSafePackageName(packageName: String): Boolean =
            SAFE_PACKAGE_NAME_REGEX.matches(packageName)

        internal fun isOplusDevice(
            manufacturer: String = Build.MANUFACTURER,
            brand: String = Build.BRAND,
        ): Boolean =
            manufacturer.equals("OPPO", ignoreCase = true) ||
                manufacturer.equals("OnePlus", ignoreCase = true) ||
                manufacturer.equals("Realme", ignoreCase = true) ||
                brand.equals("OPPO", ignoreCase = true) ||
                brand.equals("OnePlus", ignoreCase = true) ||
                brand.equals("Realme", ignoreCase = true)

        private fun isXiaomiDevice(): Boolean =
            Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
                Build.BRAND.equals("Xiaomi", ignoreCase = true) ||
                Build.BRAND.equals("Redmi", ignoreCase = true) ||
                Build.BRAND.equals("Poco", ignoreCase = true)
    }
}
