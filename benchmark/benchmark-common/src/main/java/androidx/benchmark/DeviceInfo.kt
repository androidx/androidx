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

package androidx.benchmark

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object DeviceInfo {
    val isEmulator = Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.startsWith("unknown") ||
        Build.FINGERPRINT.contains("emulator") ||
        Build.MODEL.contains("google_sdk") ||
        Build.MODEL.contains("sdk_gphone64") ||
        Build.MODEL.contains("Emulator") ||
        Build.MODEL.contains("Android SDK built for x86") ||
        Build.MODEL.contains("Android SDK built for arm64") ||
        Build.MANUFACTURER.contains("Genymotion") ||
        Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
        "google_sdk" == Build.PRODUCT

    val isEngBuild = Build.FINGERPRINT.contains(":eng/")

    val isRooted =
        arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        ).any { File(it).exists() }

    /**
     * Battery percentage required to avoid low battery warning.
     *
     * This number is supposed to be a conservative cutoff for when low-battery-triggered power
     * savings modes (such as disabling cores) may be enabled. It's possible that
     * [BatteryManager.EXTRA_BATTERY_LOW] is a better source of truth for this, but we want to be
     * conservative in case the device loses power slowly while benchmarks run.
     */
    const val MINIMUM_BATTERY_PERCENT = 25

    val initialBatteryPercent: Int

    /**
     * String summarizing device hardware and software, for bug reporting purposes.
     */
    val deviceSummaryString: String

    /**
     * General errors about device configuration, applicable to all types of benchmark.
     *
     * These errors indicate no performance tests should be performed on this device, in it's
     * current conditions.
     */
    val errors: List<ConfigurationError>

    init {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        initialBatteryPercent = context.registerReceiver(null, filter)?.run {
            val level = if (getBooleanExtra(BatteryManager.EXTRA_PRESENT, true)) {
                getIntExtra(BatteryManager.EXTRA_LEVEL, 100)
            } else {
                // If the device has no battery consider it full for this check.
                100
            }
            val scale = getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            level * 100 / scale
        } ?: 100

        deviceSummaryString = "DeviceInfo(Brand=${Build.BRAND}" +
            ", Model=${Build.MODEL}" +
            ", SDK=${Build.VERSION.SDK_INT}" +
            ", BuildFp=${Build.FINGERPRINT})"

        errors = listOfNotNull(
            conditionalError(
                hasError = isEngBuild,
                id = "ENG-BUILD",
                summary = "Running on Eng Build",
                message = """
                    Benchmark is running on device flashed with a '-eng' build. Eng builds
                    of the platform drastically reduce performance to enable testing
                    changes quickly. For this reason they should not be used for
                    benchmarking. Use a '-user' or '-userdebug' system image.
                """.trimIndent()
            ),
            conditionalError(
                hasError = isEmulator,
                id = "EMULATOR",
                summary = "Running on Emulator",
                message = """
                    Benchmark is running on an emulator, which is not representative of
                    real user devices. Use a physical device to benchmark. Emulator
                    benchmark improvements might not carry over to a real user's
                    experience (or even regress real device performance).
                """.trimIndent()
            ),
            conditionalError(
                hasError = initialBatteryPercent < MINIMUM_BATTERY_PERCENT,
                id = "LOW-BATTERY",
                summary = "Device has low battery ($initialBatteryPercent)",
                message = """
                    When battery is low, devices will often reduce performance (e.g. disabling big
                    cores) to save remaining battery. This occurs even when they are plugged in.
                    Wait for your battery to charge to at least $MINIMUM_BATTERY_PERCENT%.
                    Currently at $initialBatteryPercent%.
                """.trimIndent()
            )
        )
    }
}