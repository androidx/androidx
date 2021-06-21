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

import android.app.Instrumentation
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.profileinstaller.ProfileInstallReceiver
import androidx.profileinstaller.ProfileInstaller
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.AssumptionViolatedException

/**
 * Type of compilation to use for a Macrobenchmark.
 *
 * For example, [SpeedProfile] will run a configurable number of profiling iterations to generate
 * a profile, and use that to compile the target app.
 */
public sealed class CompilationMode(
    // for modes other than [None], is argument passed `cmd package compile`
    private val compileArgument: String?
) {
    internal fun compileArgument(): String {
        if (compileArgument == null) {
            throw UnsupportedOperationException("No compileArgument for mode $this")
        }
        return compileArgument
    }

    /**
     * No pre-compilation - entire app will be allowed to Just-In-Time compile as it runs.
     */
    public object None : CompilationMode(null) {
        public override fun toString(): String = "None"
    }

    /**
     * Partial pre-compilation, based on configurable number of profiling iterations.
     */
    public class SpeedProfile(
        public val warmupIterations: Int = 3
    ) : CompilationMode("speed-profile") {
        public override fun toString(): String = "SpeedProfile(iterations=$warmupIterations)"
    }

    /**
     * Partial pre-compilation based on bundled baseline profile.
     *
     * Note: this mode is only supported for APKs that have the profileinstaller library
     * included, and have been built by AGP 7.0+ to package the baseline profile in the APK.
     */
    public object BaselineProfile : CompilationMode("speed-profile") {
        public override fun toString(): String = "BaselineProfile"
    }

    /**
     * Full ahead-of-time compilation.
     */
    public object Speed : CompilationMode("speed") {
        public override fun toString(): String = "Speed"
    }

    /**
     * No JIT / pre-compilation, all app code runs on the interpreter.
     *
     * Note: this mode will only be supported on rooted devices with jit disabled. For this reason,
     * it's only available for internal benchmarking.
     *
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public object Interpreted : CompilationMode(null) {
        public override fun toString(): String = "Interpreted"
    }
}

/**
 * Compiles the application with the given mode.
 *
 * For more information: https://source.android.com/devices/tech/dalvik/jit-compiler
 */
internal fun CompilationMode.compile(packageName: String, block: () -> Unit) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val device = instrumentation.device()
    // Clear profile between runs.
    Log.d(TAG, "Clearing profiles for $packageName")
    device.executeShellCommand("cmd package compile --reset $packageName")

    if (this == CompilationMode.None || this == CompilationMode.Interpreted) {
        return // nothing to do
    }
    if (this == CompilationMode.BaselineProfile) {
        // For baseline profiles, if the profileinstaller library is included in the APK, then we
        // triggering this broadcast will cause the baseline profile to get installed
        // synchronously, instead of waiting for the
        val action = ProfileInstallReceiver.ACTION_INSTALL_PROFILE
        val result = device.executeShellCommand("am broadcast -a $action -p $packageName")
            .substringAfter("Broadcast completed: result=")
            .trim()
            .toIntOrNull()
        when (result) {
            null,
            // 0 is returned by the platform by default, and also if no broadcast receiver
            // receives the broadcast.
            0 -> {
                throw RuntimeException(
                    "The baseline profile install broadcast was not received. This most likely " +
                        "means that the profileinstaller library is not in the target APK. It " +
                        "must be in order to use CompilationMode.BaselineProfile."
                )
            }
            ProfileInstaller.RESULT_INSTALL_SUCCESS,
            ProfileInstaller.RESULT_ALREADY_INSTALLED -> {
                // success!
            }
            ProfileInstaller.RESULT_UNSUPPORTED_ART_VERSION -> {
                throw RuntimeException("Baseline profiles aren't supported on this device version")
            }
            ProfileInstaller.RESULT_BASELINE_PROFILE_NOT_FOUND -> {
                throw RuntimeException("No baseline profile was found in the target apk.")
            }
            ProfileInstaller.RESULT_NOT_WRITABLE,
            ProfileInstaller.RESULT_DESIRED_FORMAT_UNSUPPORTED,
            ProfileInstaller.RESULT_IO_EXCEPTION,
            ProfileInstaller.RESULT_PARSE_EXCEPTION -> {
                throw RuntimeException("Baseline Profile wasn't successfully installed")
            }
            else -> {
                throw RuntimeException(
                    "unrecognized ProfileInstaller result code: $result"
                )
            }
        }
    }
    if (this is CompilationMode.SpeedProfile) {
        repeat(this.warmupIterations) {
            block()
        }
        // For speed profile compilation, ART team recommended to wait for 5 secs when app
        // is in the foreground, dump the profile, wait for another 5 secs before
        // speed-profile compilation.
        Thread.sleep(5000)
        val response = device.executeShellCommand("killall -s SIGUSR1 $packageName")
        if (response.isNotBlank()) {
            Log.d(TAG, "Received dump profile response $response")
            throw RuntimeException("Failed to dump profile for $packageName ($response)")
        }
        Thread.sleep(5000)
    }

    Log.d(TAG, "Compiling $packageName ($this)")
    val response = device.executeShellCommand(
        "cmd package compile -f -m ${compileArgument()} $packageName"
    )
    if (!response.contains("Success")) {
        Log.d(TAG, "Received compile cmd response: $response")
        throw RuntimeException("Failed to compile $packageName ($response)")
    }
}

/**
 * Returns true if the CompilationMode can be run with the device's current VM settings.
 *
 * Used by jetpack-internal benchmarks to skip CompilationModes that would self-suppress.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun CompilationMode.isSupportedWithVmSettings(): Boolean {
    val device = InstrumentationRegistry.getInstrumentation().device()
    val getProp = device.executeShellCommand("getprop dalvik.vm.extra-opts")
    val vmRunningInterpretedOnly = getProp.contains("-Xusejit:false")

    // true if requires interpreted, false otherwise
    val interpreted = this == CompilationMode.Interpreted
    return vmRunningInterpretedOnly == interpreted
}

internal fun CompilationMode.assumeSupportedWithVmSettings() {
    if (!isSupportedWithVmSettings()) {
        throw AssumptionViolatedException(
            when {
                DeviceInfo.isRooted && this == CompilationMode.Interpreted ->
                    """
                        To run benchmarks with CompilationMode $this,
                        you must disable jit on your device with the following command:
                        `adb shell setprop dalvik.vm.extra-opts -Xusejit:false; adb shell stop; adb shell start`                         
                    """.trimIndent()
                DeviceInfo.isRooted && this != CompilationMode.Interpreted ->
                    """
                        To run benchmarks with CompilationMode $this,
                        you must enable jit on your device with the following command:
                        `adb shell setprop dalvik.vm.extra-opts \"\"; adb shell stop; adb shell start` 
                    """.trimIndent()
                else ->
                    "You must toggle usejit on the VM to use CompilationMode $this, this requires" +
                        "rooting your device."
            }
        )
    }
}

internal fun Instrumentation.device(): UiDevice {
    return UiDevice.getInstance(this)
}
