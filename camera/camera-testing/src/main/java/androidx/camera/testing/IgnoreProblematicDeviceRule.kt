/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.camera.testing

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Test class to set the TestRule should not be run on the problematic devices.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class IgnoreProblematicDeviceRule : TestRule {
    private val api21Emulator = isEmulator && Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP

    private val isProblematicDevices = isPixel2Api26Emulator || api21Emulator

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                if (isProblematicDevices) {
                    throw AssumptionViolatedException(
                        "CameraDevice of the emulator may not be well prepared for camera" +
                            " related tests. Ignore the test: " + description.displayName +
                            ". To test on emulator devices, please remove the " +
                            "IgnoreProblematicDeviceRule from the test class."
                    )
                } else {
                    base.evaluate()
                }
            }
        }
    }

    companion object {
        // Sync from TestRequestBuilder.RequiresDeviceFilter
        private const val EMULATOR_HARDWARE_GOLDFISH = "goldfish"
        private const val EMULATOR_HARDWARE_RANCHU = "ranchu"
        private const val EMULATOR_HARDWARE_GCE = "gce_x86"
        private val emulatorHardwareNames: Set<String> = setOf(
            EMULATOR_HARDWARE_GOLDFISH,
            EMULATOR_HARDWARE_RANCHU,
            EMULATOR_HARDWARE_GCE
        )
        private var avdName: String = try {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            device.executeShellCommand("getprop ro.kernel.qemu.avd_name").filterNot {
                it == '_' || it == '-' || it == ' '
            }
        } catch (e: Exception) {
            Log.d("ProblematicDeviceRule", "Cannot get avd name", e)
            ""
        }

        val isEmulator = emulatorHardwareNames.contains(Build.HARDWARE.lowercase())
        val isPixel2Api26Emulator = isEmulator && avdName.contains(
            "Pixel2", ignoreCase = true
        ) && Build.VERSION.SDK_INT == Build.VERSION_CODES.O
    }
}