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

package androidx.benchmark.macro

import android.app.Instrumentation
import android.util.Log
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

private const val TAG = "MacroBenchmarks"

// SELinux enforcement
private const val PERMISSIVE = "Permissive"
private const val ENFORCING = "Enforcing"

// Compile modes
const val SPEED = "speed"
const val SPEED_PROFILE = "speed-profile"
const val QUICKEN = "quicken"
const val VERIFY = "verify"

// All modes
private val COMPILE_MODES = listOf(SPEED, SPEED_PROFILE, QUICKEN, VERIFY)

/**
 * Kills an app.
 *
 * @param instrumentation The [Instrumentation] context.
 * @param packageName The target package name.
 */
fun killProcess(instrumentation: Instrumentation, packageName: String) {
    val device = instrumentation.device()
    val command = "am force-stop $packageName"
    device.executeShellCommand(command)
}

/**
 * Drops the app's cache directory.
 *
 *@param instrumentation The [Instrumentation] context.
 */
fun dropCaches(instrumentation: Instrumentation) {
    val outputDirectory = instrumentation.context.cacheDir
    val script = createTempFile("drop_cache_script", ".sh", outputDirectory)
    script.setWritable(true)
    script.setExecutable(/* executable */true, /* owner only */false)
    val command = "echo 3 > /proc/sys/vm/drop_caches && echo Success || echo Failure"
    script.writeText(command)
    val device = instrumentation.device()
    val result = device.executeShellCommand(script.toString())
    Log.d(TAG, "drop caches output was $result")
}

/**
 * Compiles the application with the specified filter.
 */
fun compilationFilter(
    instrumentation: Instrumentation,
    packageName: String,
    mode: String,
    profileSaveTimeout: Long = 5000
) = runBlocking {
    check(mode in COMPILE_MODES) {
        "Invalid compilation mode. Must be one of ${COMPILE_MODES.joinToString(",")}"
    }
    val device = instrumentation.device()
    if (mode == SPEED_PROFILE) {
        // For speed profile compilation, ART team recommended to wait for 5 secs when app
        // is in the foreground, dump the profile, wait for another 5 secs before
        // speed-profile compilation.
        delay(profileSaveTimeout)
        val response = device.executeShellCommand("killall -s SIGUSR1 $packageName")
        if (response.isNotBlank()) {
            Log.d(TAG, "Received dump profile response $response")
        } else {
            throw RuntimeException("Failed to dump profile for $packageName")
        }
        delay(profileSaveTimeout)
    }
    val response = device.executeShellCommand("cmd package compile -f -m $mode $packageName")
    if (!response.contains("Success")) {
        Log.d(TAG, "Received compile cmd response: $response")
        throw RuntimeException("Failed to compile $packageName")
    }
}

/**
 * Presses the home button.
 */
fun pressHome(instrumentation: Instrumentation, delayDurationMs: Long = 300) = runBlocking {
    instrumentation.device().pressHome()
    // Sleep for statsd to update the metrics.
    delay(delayDurationMs)
}

/**
 * Gets the SE Linux policy.
 */
fun getLinuxPolicy(instrumentation: Instrumentation): Int {
    return when (instrumentation.device().executeShellCommand("getenforce")) {
        PERMISSIVE -> 0
        else -> 1
    }
}

/**
 * Overrides the SE Linux policy.
 */
fun setLinuxPolicy(instrumentation: Instrumentation, policy: Int) {
    check(policy == 0 || policy == 1) {
        "Policy can only be one of `0` or `1`"
    }
    instrumentation.device().executeShellCommand("setenforce $policy")
}

fun Instrumentation.device(): UiDevice {
    return UiDevice.getInstance(this)
}
