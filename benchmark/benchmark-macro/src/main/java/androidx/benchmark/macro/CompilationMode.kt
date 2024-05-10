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

import android.os.Build
import android.util.Log
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.benchmark.Arguments
import androidx.benchmark.DeviceInfo
import androidx.benchmark.Shell
import androidx.benchmark.inMemoryTrace
import androidx.benchmark.macro.CompilationMode.Full
import androidx.benchmark.macro.CompilationMode.Ignore
import androidx.benchmark.macro.CompilationMode.None
import androidx.benchmark.macro.CompilationMode.Partial
import androidx.profileinstaller.ProfileInstallReceiver
import java.lang.StringBuilder
import org.junit.AssumptionViolatedException

/**
 * Type of compilation to use for a Macrobenchmark.
 *
 * This compilation mode controls pre-compilation that occurs before running the setup / measure
 * blocks of the benchmark.
 *
 * On Android N+ (API 24+), there are different levels of compilation supported:
 * * [Partial] - the default configuration of [Partial] will partially pre-compile your application,
 *   if a Baseline Profile is included in your app. This represents the most realistic fresh-install
 *   experience on an end-user's device. You can additionally or instead use
 *   [Partial.warmupIterations] to use Profile Guided Optimization, using the benchmark content to
 *   guide pre-compilation. This can mimic an application's performance after background dexopt has
 *   partially compiled the app during device idle time, after the app has been used (generally
 *   after a day or more of usage after update/install).
 * * [Full] - the app is fully pre-compiled. This is generally not representative of real user
 *   experience, as apps are not fully pre-compiled on user devices more recent than Android N (API
 *   24). `Full` can be used to show unrealistic but potentially more stable performance by removing
 *   the noise/inconsistency from just-in-time compilation within benchmark runs. Note that `Full`
 *   compilation will often be slower than [Partial] compilation, as the increased code size creates
 *   more cost for disk loading during startup, and increases pressure in the instruction cache.
 * * [None] - the app isn't pre-compiled at all, bypassing the default compilation that should
 *   generally be done at install time, e.g. by the Play Store. This will illustrate worst case
 *   performance, and will show you performance of your app if you do not enable baseline profiles,
 *   useful for judging the performance impact of the baseline profiles included in your
 *   application.
 * * [Ignore] - the state of compilation will be ignored. The intended use-case is for a developer
 *   to customize the compilation state for an app; and then tell Macrobenchmark to leave it
 *   unchanged.
 *
 * On Android M (API 23), only [Full] is supported, as all apps are always fully compiled.
 *
 * To understand more how these modes work, you can see comments for each class, and also see the
 * [Android Runtime compilation modes](https://source.android.com/devices/tech/dalvik/configure#compilation_options)
 * (which are passed by benchmark into
 * [`cmd compile`](https://source.android.com/devices/tech/dalvik/jit-compiler#force-compilation-of-a-specific-package)
 * to compile the target app).
 */
sealed class CompilationMode {
    internal fun resetAndCompile(
        scope: MacrobenchmarkScope,
        allowCompilationSkipping: Boolean = true,
        warmupBlock: () -> Unit,
    ) {
        val packageName = scope.packageName
        if (Build.VERSION.SDK_INT >= 24) {
            if (Arguments.enableCompilation || !allowCompilationSkipping) {
                Log.d(TAG, "Clearing ART profiles for $packageName")
                // The compilation mode chooses whether a reset is required or not.
                // Currently the only compilation mode that does not perform a reset is
                // CompilationMode.Ignore.
                if (shouldReset()) {
                    // Package reset enabled
                    Log.d(TAG, "Resetting profiles for $packageName")
                    // It's not possible to reset the compilation profile on `user` builds.
                    // The flag `enablePackageReset` can be set to `true` on `userdebug` builds in
                    // order to speed-up the profile reset. When set to false, reset is performed
                    // uninstalling and reinstalling the app.
                    if (Build.VERSION.SDK_INT >= 34) {
                        // Starting API 34, --reset restores the state of the compiled code based
                        // on prior install state. This means, e.g. if AGP version 8.3+ installs a
                        // DM alongside the APK, reset != clear.
                        // Use --verify to replace the contents of the odex file with that of an
                        // empty file.
                        cmdPackageCompile(packageName, "verify")
                        // This does not clear the state of the `cur` and `ref` profiles.
                        // To do that we also need to call `pm art clear-app-profiles <package>`.
                        // pm art clear-app-profiles returns a "Profiles cleared"
                        // to stdout upon success. Otherwise it includes an Error: <error reason>.
                        val output =
                            Shell.executeScriptCaptureStdout(
                                "pm art clear-app-profiles $packageName"
                            )

                        check(output.trim() == "Profiles cleared") {
                            compileResetErrorString(packageName, output, DeviceInfo.isEmulator)
                        }
                    } else if (Shell.isSessionRooted()) {
                        // cmd package compile --reset returns a "Success" or a "Failure" to stdout.
                        // Rather than rely on exit codes which are not always correct, we
                        // specifically look for the work "Success" in stdout to make sure reset
                        // actually happened.
                        val output =
                            Shell.executeScriptCaptureStdout(
                                "cmd package compile --reset $packageName"
                            )

                        check(output.trim() == "Success" || output.contains("PERFORMED")) {
                            compileResetErrorString(packageName, output, DeviceInfo.isEmulator)
                        }
                    } else {
                        // User builds pre-U. Kick off a full uninstall-reinstall
                        Log.d(TAG, "Reinstalling $packageName")
                        reinstallPackage(packageName)
                    }
                }
                // Write skip file to stop profile installer from interfering with the benchmark
                writeProfileInstallerSkipFile(scope)
                compileImpl(scope, warmupBlock)
            } else {
                Log.d(TAG, "Compilation is disabled, skipping compilation of $packageName")
            }
        }
    }

    /**
     * A more expensive alternative to `compile --reset` which doesn't preserve app data, but does
     * work on older APIs without root.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun reinstallPackage(packageName: String) {
        inMemoryTrace("reinstallPackage") {
            val copiedApkPaths = copiedApkPaths(packageName)
            try {
                // Uninstall package
                // This is what effectively clears the ART profiles
                uninstallPackage(packageName)
                // Install the APK from /data/local/tmp
                installPackageFromPaths(packageName = packageName, copiedApkPaths = copiedApkPaths)
            } finally {
                // Cleanup the temporary APK
                Log.d(TAG, "Deleting $copiedApkPaths")
                Shell.rm(copiedApkPaths)
            }
        }
    }

    /**
     * Copies the APKs obtained from the current install location, into `/data/local/tmp` and
     * returns a `<space>` delimited list of paths that can be used to reinstall the app package
     * after uninstall.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun copiedApkPaths(packageName: String): String {
        // Copy APKs to /data/local/temp
        val apkPaths = Shell.pmPath(packageName)

        val tempApkPaths: List<String> =
            apkPaths.mapIndexed { index, apkPath ->
                val tempApkPath =
                    "/data/local/tmp/$packageName-$index-${System.currentTimeMillis()}.apk"
                Log.d(TAG, "Copying APK $apkPath to $tempApkPath")
                Shell.cp(from = apkPath, to = tempApkPath)
                tempApkPath
            }
        return tempApkPaths.joinToString(" ")
    }

    /** Uninstalls an app package by using `pm uninstall` under the hood. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun uninstallPackage(packageName: String) {
        Log.d(TAG, "Uninstalling $packageName")
        val output = Shell.executeScriptCaptureStdout("pm uninstall $packageName")
        check(output.trim() == "Success") { "Unable to uninstall $packageName ($output)" }
    }

    /**
     * Installs the app using a set of APKs that were previously copied and staged into
     * `/data/local/tmp` from a pre-existing install session.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun installPackageFromPaths(packageName: String, copiedApkPaths: String) {
        Log.d(TAG, "Installing $packageName")
        val builder = StringBuilder("pm install")
        // Provide a `-t` argument to `pm install` to ensure test packages are
        // correctly installed. (b/231294733)
        builder.append(" -t")
        if (Build.VERSION.SDK_INT >= 30) {
            // Use --skip-verification to disable Play protect.
            // This option was introduced in Android R (30)
            // b/308100444 has additional context.
            builder.append(" --skip-verification")
        }
        builder.append(" $copiedApkPaths")
        val output = Shell.executeScriptCaptureStdout(builder.toString())

        check(output.trim() == "Success" || output.contains("PERFORMED")) {
            "Unable to install $packageName (out=$output)"
        }
    }

    /**
     * Writes a skip file via a [ProfileInstallReceiver] broadcast, so profile installation does not
     * interfere with benchmarks.
     */
    private fun writeProfileInstallerSkipFile(scope: MacrobenchmarkScope) {
        val packageName = scope.packageName
        val result = ProfileInstallBroadcast.skipFileOperation(packageName, "WRITE_SKIP_FILE")
        if (result != null) {
            Log.w(
                TAG,
                """
                    $packageName should use the latest version of `androidx.profileinstaller`
                    for stable benchmarks. ($result)"
                """
                    .trimIndent()
            )
        }
        Log.d(TAG, "Killing process $packageName")
        scope.killProcess()
    }

    @RequiresApi(24)
    internal abstract fun compileImpl(
        scope: MacrobenchmarkScope,
        warmupBlock: () -> Unit,
    )

    @RequiresApi(24) internal abstract fun shouldReset(): Boolean

    /**
     * No pre-compilation - a compilation profile reset is performed and the entire app will be
     * allowed to Just-In-Time compile as it runs.
     *
     * Note that later iterations may perform differently, as app code is jitted.
     */
    // Leaving possibility for future configuration (such as interpreted = true)
    @Suppress("CanSealedSubClassBeObject")
    @RequiresApi(24)
    class None : CompilationMode() {
        override fun toString(): String = "None"

        override fun compileImpl(scope: MacrobenchmarkScope, warmupBlock: () -> Unit) {
            // nothing to do!
        }

        override fun shouldReset(): Boolean = true
    }

    /**
     * This compilation mode doesn't perform any reset or compilation, leaving the user the choice
     * to implement these steps.
     */
    // Leaving possibility for future configuration
    @ExperimentalMacrobenchmarkApi
    @Suppress("CanSealedSubClassBeObject")
    class Ignore : CompilationMode() {
        override fun toString(): String = "Ignore"

        override fun compileImpl(scope: MacrobenchmarkScope, warmupBlock: () -> Unit) {
            // Do nothing.
        }

        override fun shouldReset(): Boolean = false
    }

    /**
     * Partial ahead-of-time app compilation.
     *
     * The default parameters for this mimic the default state of an app partially pre-compiled by
     * the installer - such as via Google Play.
     *
     * Either [baselineProfileMode] must be set to non-[BaselineProfileMode.Disable], or
     * [warmupIterations] must be set to a non-`0` value.
     *
     * Note: `[baselineProfileMode] = [BaselineProfileMode.Require]` is only supported for APKs that
     * have the ProfileInstaller library included, and have been built by AGP 7.0+ to package the
     * baseline profile in the APK.
     */
    @RequiresApi(24)
    class Partial
    @JvmOverloads
    constructor(
        /**
         * Controls whether a Baseline Profile should be used to partially pre compile the app.
         *
         * Defaults to [BaselineProfileMode.Require]
         *
         * @see BaselineProfileMode
         */
        val baselineProfileMode: BaselineProfileMode = BaselineProfileMode.Require,

        /**
         * If greater than 0, your macrobenchmark will run an extra [warmupIterations] times before
         * compilation, to prepare
         */
        @IntRange(from = 0) val warmupIterations: Int = 0
    ) : CompilationMode() {
        init {
            require(warmupIterations >= 0) {
                "warmupIterations must be non-negative, was $warmupIterations"
            }
            require(baselineProfileMode != BaselineProfileMode.Disable || warmupIterations > 0) {
                "Must set baselineProfileMode != Ignore, or warmup iterations > 0 to define" +
                    " which portion of the app to pre-compile."
            }
        }

        override fun toString(): String {
            return if (
                baselineProfileMode == BaselineProfileMode.Require && warmupIterations == 0
            ) {
                "BaselineProfile"
            } else if (baselineProfileMode == BaselineProfileMode.Disable && warmupIterations > 0) {
                "WarmupProfile(iterations=$warmupIterations)"
            } else {
                "Partial(baselineProfile=$baselineProfileMode,iterations=$warmupIterations)"
            }
        }

        override fun compileImpl(scope: MacrobenchmarkScope, warmupBlock: () -> Unit) {
            val packageName = scope.packageName
            if (baselineProfileMode != BaselineProfileMode.Disable) {
                // Ignores the presence of a skip file.
                val installErrorString = ProfileInstallBroadcast.installProfile(packageName)
                if (installErrorString == null) {
                    // baseline profile install success, kill process before compiling
                    Log.d(TAG, "Killing process $packageName")
                    scope.killProcess()
                    cmdPackageCompile(packageName, "speed-profile")
                } else {
                    if (baselineProfileMode == BaselineProfileMode.Require) {
                        throw RuntimeException(installErrorString)
                    } else {
                        Log.d(TAG, installErrorString)
                    }
                }
            }
            if (warmupIterations > 0) {
                scope.flushArtProfiles = true
                check(!scope.hasFlushedArtProfiles)
                try {
                    repeat(this.warmupIterations) { warmupBlock() }
                    scope.killProcessAndFlushArtProfiles()
                    check(scope.hasFlushedArtProfiles) {
                        "Process $packageName never flushed profiles in any process - check that" +
                            " you launched the process, and that you only killed it with" +
                            " scope.killProcess, which will save profiles."
                    }
                    cmdPackageCompile(packageName, "speed-profile")
                } finally {
                    scope.flushArtProfiles = false
                }
            }
        }

        override fun shouldReset(): Boolean = true
    }

    /**
     * Full ahead-of-time compilation.
     *
     * Equates to `cmd package compile -f -m speed <package>` on API 24+.
     *
     * On Android M (API 23), this is the only supported compilation mode, as all apps are fully
     * compiled ahead-of-time.
     */
    @Suppress("CanSealedSubClassBeObject") // Leaving possibility for future configuration
    class Full : CompilationMode() {
        override fun toString(): String = "Full"

        override fun compileImpl(scope: MacrobenchmarkScope, warmupBlock: () -> Unit) {
            if (Build.VERSION.SDK_INT >= 24) {
                cmdPackageCompile(scope.packageName, "speed")
            }
            // Noop on older versions: apps are fully compiled at install time on API 23 and below
        }

        override fun shouldReset(): Boolean = true
    }

    /**
     * No JIT / pre-compilation, all app code runs on the interpreter.
     *
     * Note: this mode will only be supported on rooted devices with jit disabled. For this reason,
     * it's only available for internal benchmarking.
     *
     * TODO: migrate this to an internal-only flag on [None] instead
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    object Interpreted : CompilationMode() {
        override fun toString(): String = "Interpreted"

        override fun compileImpl(scope: MacrobenchmarkScope, warmupBlock: () -> Unit) {
            // Nothing to do - handled externally
        }

        override fun shouldReset(): Boolean = true
    }

    companion object {

        /**
         * Represents the default compilation mode for the platform, on an end user's device.
         *
         * This is a post-store-install app configuration for this device's SDK
         * version - [`Partial(BaselineProfileMode.UseIfAvailable)`][Partial] on API 24+, and [Full]
         * prior to API 24 (where all apps are fully AOT compiled).
         *
         * On API 24+, Baseline Profile pre-compilation is used if possible, but no error will be
         * thrown if installation fails.
         *
         * Generally, it is preferable to explicitly pass a compilation mode, such as
         * [`Partial(BaselineProfileMode.Required)`][Partial] to avoid ambiguity, and e.g. validate
         * an app's BaselineProfile can be correctly used.
         */
        @JvmField
        val DEFAULT: CompilationMode =
            if (Build.VERSION.SDK_INT >= 24) {
                Partial(
                    baselineProfileMode = BaselineProfileMode.UseIfAvailable,
                    warmupIterations = 0
                )
            } else {
                // API 23 is always fully compiled
                Full()
            }

        @RequiresApi(24)
        internal fun cmdPackageCompile(packageName: String, compileArgument: String) {
            val stdout =
                Shell.executeScriptCaptureStdout(
                    "cmd package compile -f -m $compileArgument $packageName"
                )
            check(stdout.trim() == "Success" || stdout.contains("PERFORMED")) {
                "Failed to compile (out=$stdout)"
            }
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // enable testing
        fun compileResetErrorString(
            packageName: String,
            output: String,
            isEmulator: Boolean
        ): String {
            return "Unable to reset compilation of $packageName (out=$output)." +
                if (output.contains("could not be compiled") && isEmulator) {
                    " Try updating your emulator -" +
                        " see https://issuetracker.google.com/issue?id=251540646"
                } else {
                    ""
                }
        }
    }
}

/**
 * Returns true if the CompilationMode can be run with the device's current VM settings.
 *
 * Used by jetpack-internal benchmarks to skip CompilationModes that would self-suppress.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun CompilationMode.isSupportedWithVmSettings(): Boolean {
    // Only check for supportedVmSettings when CompilationMode.Interpreted is being requested.
    // More context: b/248085179
    val interpreted = this == CompilationMode.Interpreted
    return if (interpreted) {
        val getProp = Shell.getprop("dalvik.vm.extra-opts")
        val vmRunningInterpretedOnly = getProp.contains("-Xusejit:false")
        // true if requires interpreted, false otherwise
        vmRunningInterpretedOnly
    } else {
        true
    }
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
                    """
                        .trimIndent()
                DeviceInfo.isRooted && this != CompilationMode.Interpreted ->
                    """
                        To run benchmarks with CompilationMode $this,
                        you must enable jit on your device with the following command:
                        `adb shell setprop dalvik.vm.extra-opts \"\"; adb shell stop; adb shell start` 
                    """
                        .trimIndent()
                else ->
                    "You must toggle usejit on the VM to use CompilationMode $this, this requires" +
                        "rooting your device."
            }
        )
    }
}
