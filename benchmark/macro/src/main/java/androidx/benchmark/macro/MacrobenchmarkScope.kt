/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro

import android.content.Intent
import android.util.Log
import androidx.benchmark.macro.perfetto.executeShellScript
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.util.concurrent.TimeUnit

/**
 * Provides access to common operations in app automation, such as killing the app,
 * or navigating home.
 */
public class MacrobenchmarkScope(
    private val packageName: String,
    /**
     * Controls whether launches will automatically set [Intent.FLAG_ACTIVITY_CLEAR_TASK].
     *
     * Default to true, so Activity launches go through full creation lifecycle stages, instead of
     * just resume.
     */
    private val launchWithClearTask: Boolean
) {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.context
    private val device = UiDevice.getInstance(instrumentation)

    /**
     * Start an activity, by default the default launch of the package, and wait until
     * its launch completes.
     *
     * @throws IllegalStateException if unable to acquire intent for package.
     *
     * @param block Allows customization of the intent used to launch the activity.
     */
    public fun startActivityAndWait(
        block: (Intent) -> Unit = {}
    ) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: throw IllegalStateException("Unable to acquire intent for package $packageName")

        block(intent)
        startActivityAndWait(intent)
    }

    /**
     * Start an activity with the provided intent, and wait until its launch completes.
     *
     * @param intent Specifies which app/Activity should be launched.
     */
    public fun startActivityAndWait(intent: Intent) {
        // Must launch with new task, as we're not launching from an existing task
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (launchWithClearTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (securityException: SecurityException) {
            // Android 11 sets exported=false by default, which means we can't launch, but this
            // can also happen if "android:exported=false" is used directly.
            throw SecurityException(
                "Unable to launch Activity due to Security Exception. To launch an " +
                    "activity from a benchmark, you may need to set android:exported=true " +
                    "for the Activity in your application's manifest",
                securityException
            )
        }

        waitOnPackageLaunch()
    }

    /**
     * Wait on the current package to complete an Activity launch.
     *
     * Note that [timeoutInSeconds] is for full Activity launch, and UiAutomator detection of
     * Activity content. This is not just Activity launch time as would be reported by
     * [StartupTimingMetric] - it must include additional fixed time.
     *
     * As an example, when this timeout was 5 seconds, a 2 second activity launch would
     * frequently hit the timeout. This timeout should be conservatively large to encapsulate
     * any slow app / hardware combo.
     */
    internal fun waitOnPackageLaunch(timeoutInSeconds: Long = 30) {
        val timeoutInMilliseconds = TimeUnit.SECONDS.toMillis(timeoutInSeconds)

        // Note: if this wait starts during an activity launch, it will wait until the launch
        // completes. This is why it's safe to simply check package - even if launching from one
        // activity to another within the package, the launch has to fully complete.

        // Note though, that this wait does not wait for within-activity launch behavior to
        // complete. that must be done separately.
        val packageIsDisplayed = device.wait(
            Until.hasObject(By.pkg(packageName).depth(0)),
            timeoutInMilliseconds
        )
        if (!packageIsDisplayed) {
            throw IllegalStateException(
                "Unable to detect Activity of package $packageName after " +
                    "$timeoutInSeconds second timeout. Did it fail to launch?"
            )
        }
    }

    /**
     * Perform a home button click.
     *
     * Useful for resetting the test to a base condition in cases where the app isn't killed in
     * each iteration.
     */
    public fun pressHome(delayDurationMs: Long = 300) {
        device.pressHome()
        Thread.sleep(delayDurationMs)
    }

    /**
     * Force-stop the process being measured.
     */
    public fun killProcess() {
        Log.d(TAG, "Killing process $packageName")
        device.executeShellCommand("am force-stop $packageName")
    }

    /**
     * Drop Kernel's in-memory cache of disk pages.
     *
     * Enables measuring disk-based startup cost, without simply accessing cache of disk data
     * held in memory, such as during [cold startup](androidx.benchmark.macro.StartupMode.COLD).
     */
    public fun dropKernelPageCache() {
        val result = device.executeShellScript(
            "echo 3 > /proc/sys/vm/drop_caches && echo Success || echo Failure"
        ).trim()
        // User builds don't allow drop caches yet.
        if (result != "Success") {
            Log.w(TAG, "Failed to drop kernel page cache, result: '$result'")
        }
    }
}
