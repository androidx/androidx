/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.annotation.RequiresApi
import androidx.benchmark.Shell
import androidx.profileinstaller.ProfileInstallReceiver
import androidx.profileinstaller.ProfileInstaller

internal object ProfileInstallBroadcast {
    private val receiverName = ProfileInstallReceiver::class.java.name

    /**
     * Returns null on success, error string on suppress-able error, or throws if profileinstaller
     * not up to date.
     *
     * Returned error strings aren't thrown, to let the calling function decide strictness.
     */
    fun installProfile(packageName: String): String? {
        Log.d(TAG, "Profile Installer - Install profile")
        // For baseline profiles, we trigger this broadcast to force the baseline profile to be
        // installed synchronously
        val action = ProfileInstallReceiver.ACTION_INSTALL_PROFILE
        // Use an explicit broadcast given the app was force-stopped.
        when (val result = Shell.amBroadcast("-a $action $packageName/$receiverName")) {
            null,
            // 0 is returned by the platform by default, and also if no broadcast receiver
            // receives the broadcast.
            0 -> {
                return "The baseline profile install broadcast was not received. " +
                    "This most likely means that the profileinstaller library is missing " +
                    "from the target apk."
            }
            ProfileInstaller.RESULT_INSTALL_SUCCESS -> {
                return null // success!
            }
            ProfileInstaller.RESULT_ALREADY_INSTALLED -> {
                throw RuntimeException(
                    "Unable to install baseline profile. This most likely means that the " +
                        "latest version of the profileinstaller library is not being used. " +
                        "Please use the latest profileinstaller library version " +
                        "in the target app."
                )
            }
            ProfileInstaller.RESULT_UNSUPPORTED_ART_VERSION -> {
                val sdkInt = Build.VERSION.SDK_INT
                throw RuntimeException(
                    if (sdkInt <= 23) {
                        "Baseline profiles aren't supported on this device version," +
                            " as all apps are fully ahead-of-time compiled."
                    } else {
                        "The device SDK version ($sdkInt) isn't supported" +
                            " by the target app's copy of profileinstaller." +
                            if (sdkInt in 31..33) {
                                " Please use profileinstaller `1.2.1`" +
                                    " or newer for API 31-33 support"
                            } else if (sdkInt >= 34) {
                                " Please use profileinstaller `1.4.0`" +
                                    " or newer for API 34+ support"
                            } else {
                                ""
                            }
                    }
                )
            }
            ProfileInstaller.RESULT_BASELINE_PROFILE_NOT_FOUND -> {
                return "No baseline profile was found in the target apk."
            }
            ProfileInstaller.RESULT_NOT_WRITABLE,
            ProfileInstaller.RESULT_DESIRED_FORMAT_UNSUPPORTED,
            ProfileInstaller.RESULT_IO_EXCEPTION,
            ProfileInstaller.RESULT_PARSE_EXCEPTION -> {
                throw RuntimeException("Baseline Profile wasn't successfully installed")
            }
            else -> {
                throw RuntimeException("unrecognized ProfileInstaller result code: $result")
            }
        }
    }

    /**
     * Uses skip files for avoiding interference from ProfileInstaller when using
     * [CompilationMode.None].
     *
     * Operation name is one of `WRITE_SKIP_FILE` or `DELETE_SKIP_FILE`.
     *
     * Returned error strings aren't thrown, to let the calling function decide strictness.
     */
    fun skipFileOperation(
        packageName: String,
        @Suppress("SameParameterValue") operation: String
    ): String? {
        Log.d(TAG, "Profile Installer - Skip File Operation: $operation")
        // Redefining constants here, because these are only defined in the latest alpha for
        // ProfileInstaller.
        // Use an explicit broadcast given the app was force-stopped.
        val action = "androidx.profileinstaller.action.SKIP_FILE"
        val operationKey = "EXTRA_SKIP_FILE_OPERATION"
        val extras = "$operationKey $operation"
        val result = Shell.amBroadcast("-a $action -e $extras $packageName/$receiverName")
        return when {
            result == null || result == 0 -> {
                // 0 is returned by the platform by default, and also if no broadcast receiver
                // receives the broadcast.

                "The baseline profile skip file broadcast was not received. " +
                    "This most likely means that the `androidx.profileinstaller` library " +
                    "used by the target apk is old. Please use `1.2.0-alpha03` or newer. " +
                    "For more information refer to the release notes at " +
                    "https://developer.android.com/jetpack/androidx/releases/profileinstaller."
            }
            operation == "WRITE_SKIP_FILE" && result == 10 -> { // RESULT_INSTALL_SKIP_FILE_SUCCESS
                null // success!
            }
            operation == "DELETE_SKIP_FILE" && result == 11 -> { // RESULT_DELETE_SKIP_FILE_SUCCESS
                null // success!
            }
            else -> {
                throw RuntimeException("unrecognized ProfileInstaller result code: $result")
            }
        }
    }

    /**
     * Save any in-memory profile data in the target app to disk, so it can be used for compilation.
     *
     * Returned error strings aren't thrown, to let the calling function decide strictness.
     */
    @RequiresApi(24)
    fun saveProfile(packageName: String): String? {
        Log.d(TAG, "Profile Installer - Save Profile")
        val action = "androidx.profileinstaller.action.SAVE_PROFILE"
        return when (val result = Shell.amBroadcast("-a $action $packageName/$receiverName")) {
            null,
            0 -> {
                // 0 is returned by the platform by default, and also if no broadcast receiver
                // receives the broadcast. This can be because the package name specified is
                // incorrect or an old version of profile installer was used.

                "The save profile broadcast event was not received. This can be because the " +
                    "specified package name is incorrect or the `androidx.profileinstaller`" +
                    " library used by the target apk is old. Please use version `1.3.1` or " +
                    "newer. For more information refer to the release notes at " +
                    "https://developer.android.com/jetpack/androidx/releases/profileinstaller."
            }
            12 -> { // RESULT_SAVE_PROFILE_SIGNALLED
                // While this is observed to be fast for simple/sample apps,
                // this can take up significantly longer on large apps
                // especially on low end devices (see b/316082056)
                @Suppress("BanThreadSleep") Thread.sleep(1000)
                null // success!
            }
            else -> {
                // We don't bother supporting RESULT_SAVE_PROFILE_SKIPPED here,
                // since we already perform SDK_INT checks and use @RequiresApi(24)
                throw RuntimeException("unrecognized ProfileInstaller result code: $result")
            }
        }
    }

    private fun benchmarkOperation(
        packageName: String,
        @Suppress("SameParameterValue") operation: String
    ): String? {
        Log.d(TAG, "Profile Installer - Benchmark Operation: $operation")
        // Redefining constants here, because these are only defined in the latest alpha for
        // ProfileInstaller.
        // Use an explicit broadcast given the app was force-stopped.
        val action = "androidx.profileinstaller.action.BENCHMARK_OPERATION"
        val operationKey = "EXTRA_BENCHMARK_OPERATION"
        val broadcastArguments = "-a $action -e $operationKey $operation $packageName/$receiverName"
        return when (val result = Shell.amBroadcast(broadcastArguments)) {
            null,
            0,
            16 /* BENCHMARK_OPERATION_UNKNOWN */ -> {
                // 0 is returned by the platform by default, and also if no broadcast receiver
                // receives the broadcast.

                // NOTE: may need to update this over time for different versions,
                // based on operation string
                "The $operation broadcast was not received. " +
                    "This most likely means that the `androidx.profileinstaller` library " +
                    "used by the target apk is old. Please use `1.3.0-alpha02` or newer. " +
                    "For more information refer to the release notes at " +
                    "https://developer.android.com/jetpack/androidx/releases/profileinstaller. " +
                    "If you are already using androidx.profileinstaller library and still seeing " +
                    "error, verify: 1) androidx.profileinstaller.ProfileInstallReceiver appears " +
                    "unobfuscated in your APK's AndroidManifest and dex, and 2) the following " +
                    "command executes successfully (should print 14): " +
                    "adb shell am broadcast $broadcastArguments"
            }
            15 -> { // RESULT_BENCHMARK_OPERATION_FAILURE
                "The $operation broadcast failed."
            }
            14 -> { // RESULT_BENCHMARK_OPERATION_SUCCESS
                null // success!
            }
            else -> {
                throw RuntimeException("unrecognized ProfileInstaller result code: $result")
            }
        }
    }

    fun dropShaderCache(packageName: String): String? =
        benchmarkOperation(packageName, "DROP_SHADER_CACHE")
}
