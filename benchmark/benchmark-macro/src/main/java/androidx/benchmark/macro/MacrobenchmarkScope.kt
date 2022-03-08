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
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.benchmark.DeviceInfo
import androidx.benchmark.Shell
import androidx.benchmark.macro.perfetto.forceTrace
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.tracing.trace

/**
 * Provides access to common operations in app automation, such as killing the app,
 * or navigating home.
 */
public class MacrobenchmarkScope(
    /**
     * Package name of the app being tested.
     */
    val packageName: String,
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

    /**
     * Current Macrobenchmark measurement iteration, or null if measurement is not yet enabled.
     *
     * Non-measurement iterations can occur due to warmup a [CompilationMode], or prior to the first
     * iteration for [StartupMode.WARM] or [StartupMode.HOT], to create the Process or Activity
     * ahead of time.
     */
    @get:Suppress("AutoBoxing") // low frequency, non-perf-relevant part of test
    var iteration: Int? = null
        internal set

    /**
     * Get the [UiDevice] instance, to use in reading target app UI state, or interacting with the
     * UI via touches, scrolls, or other inputs.
     *
     * Convenience for `UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())`
     */
    val device: UiDevice = UiDevice.getInstance(instrumentation)

    /**
     * Start an activity, by default the default launch of the package, and wait until
     * its launch completes.
     *
     * This call will ignore any parcelable extras on the intent, as the start is performed by
     * converting the Intent to a URI, and starting via `am start` shell command.
     *
     * @throws IllegalStateException if unable to acquire intent for package.
     *
     * @param block Allows customization of the intent used to launch the activity.
     */
    @JvmOverloads
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
     * This call will ignore any parcelable extras on the intent, as the start is performed by
     * converting the Intent to a URI, and starting via `am start` shell command.
     *
     * @param intent Specifies which app/Activity should be launched.
     */
    public fun startActivityAndWait(intent: Intent): Unit = forceTrace("startActivityAndWait") {
        // Must launch with new task, as we're not launching from an existing task
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (launchWithClearTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        // Note: intent.toUri(0) produces a String that can't be parsed by `am start-activity`.
        // intent.toUri(Intent.URI_ANDROID_APP_SCHEME) also works though.
        startActivityImpl(intent.toUri(Intent.URI_INTENT_SCHEME))
    }

    private fun startActivityImpl(uri: String) {
        val cmd = "am start -W \"$uri\""
        Log.d(TAG, "Starting activity with command: $cmd")

        // executeShellScript used to access stderr, and avoid need to escape special chars like `;`
        val result = Shell.executeScriptWithStderr(cmd)

        // Check for errors
        result.stdout
            .split("\n")
            .map { it.trim() }
            .forEach {
                if (it.startsWith("Error:")) {
                    throw IllegalStateException(it)
                }
            }
        if (result.stderr.contains("java.lang.SecurityException")) {
            throw SecurityException(result.stderr)
        }
        if (result.stderr.isNotEmpty()) {
            throw IllegalStateException(result.stderr)
        }

        // `am start -W` doesn't wait for renderthread pre API 29, we stick a conservative
        // extra wait in to ensure the launch has fully rendered.

        // On newer platform versions, `start -W` does not necessarily wait for the process to be
        // alive. So we need to check for that irrespective of `launchWithClearTask`.
        // b/218668335
        if (Build.VERSION.SDK_INT < 29 || !Shell.isPackageAlive(packageName)) {
            trace("sleeping to ensure am start completed") {
                Thread.sleep(250) // conservative number, determined empirically
                // Wait for up to an additional 1 second.
                var retryCount = 0
                while (!Shell.isPackageAlive(packageName) && retryCount < 10) {
                    retryCount += 1
                    Thread.sleep(100) // Wait for a little longer until package is alive
                }
            }
        }
    }

    /**
     * Perform a home button click.
     *
     * Useful for resetting the test to a base condition in cases where the app isn't killed in
     * each iteration.
     */
    @JvmOverloads
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
     * Drop caches via setprop added in API 31
     *
     * Feature for dropping caches without root added in 31: https://r.android.com/1584525
     * Passing 3 will cause caches to be dropped, and prop will go back to 0 when it's done
     */
    @RequiresApi(31)
    private fun dropKernelPageCacheSetProp() {
        val result = Shell.executeScriptWithStderr("setprop perf.drop_caches 3")
        check(result.stdout.isEmpty() && result.stderr.isEmpty()) {
            "Failed to trigger drop cache via setprop: $result"
        }
        // Polling duration is very conservative, on Pixel 4L finishes in ~150ms
        repeat(50) {
            Thread.sleep(50)
            when (val getPropResult = Shell.executeCommand("getprop perf.drop_caches").trim()) {
                "0" -> return // completed!
                "3" -> {} // not completed, continue
                else -> throw IllegalStateException(
                    "Unable to drop caches: Failed to read drop cache via getprop: $getPropResult"
                )
            }
        }
        throw IllegalStateException(
            "Unable to drop caches: Did not observe perf.drop_caches reset automatically"
        )
    }

    /**
     * Drop Kernel's in-memory cache of disk pages.
     *
     * Enables measuring disk-based startup cost, without simply accessing cache of disk data
     * held in memory, such as during [cold startup](androidx.benchmark.macro.StartupMode.COLD).
     *
     * @Throws IllegalStateException if dropping the cache fails on a API 31+ or rooted device,
     * where it is expecte to work.
     */
    public fun dropKernelPageCache() {
        if (Build.VERSION.SDK_INT >= 31) {
            dropKernelPageCacheSetProp()
        } else {
            val result = Shell.executeScript(
                "echo 3 > /proc/sys/vm/drop_caches && echo Success || echo Failure"
            ).trim()
            // Older user builds don't allow drop caches, should investigate workaround
            if (result != "Success") {
                if (DeviceInfo.isRooted && !Shell.isSessionRooted()) {
                    throw IllegalStateException("Failed to drop caches - run `adb root`")
                }
                Log.w(TAG, "Failed to drop kernel page cache, result: '$result'")
            }
        }
    }
}
