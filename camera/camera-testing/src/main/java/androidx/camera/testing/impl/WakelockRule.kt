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

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.camera.core.Logger
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.TimeUnit
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] that keeps the device awake for headless tests without launching an Activity.
 *
 * It wakes the screen and configures stay-on via [RequireForegroundRule.clearDeviceUI], and
 * acquires a [PowerManager.PARTIAL_WAKE_LOCK] (via shell permission identity on API 29+ or standard
 * power manager) to prevent the CPU/device from sleeping during test execution.
 */
public class WakelockRule(private val timeoutMs: Long = TimeUnit.MINUTES.toMillis(5)) : TestRule {
    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                val context = instrumentation.targetContext
                RequireForegroundRule.clearDeviceUI(instrumentation)

                var wakeLock: PowerManager.WakeLock? = null
                val hasAdoptedShell = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

                if (hasAdoptedShell) {
                    try {
                        instrumentation.uiAutomation.adoptShellPermissionIdentity(
                            Manifest.permission.WAKE_LOCK
                        )
                    } catch (e: Exception) {
                        Logger.w(TAG, "Failed to adopt shell permission identity for WAKE_LOCK", e)
                    }
                }

                try {
                    val powerManager =
                        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    wakeLock =
                        powerManager?.newWakeLock(
                            PowerManager.PARTIAL_WAKE_LOCK,
                            "CameraX:HeadlessWakeLock",
                        )
                    wakeLock?.acquire(timeoutMs)
                } catch (e: Exception) {
                    Logger.w(TAG, "Failed to acquire PARTIAL_WAKE_LOCK", e)
                }

                try {
                    base.evaluate()
                } finally {
                    try {
                        if (wakeLock?.isHeld == true) {
                            wakeLock.release()
                        }
                    } catch (e: Exception) {
                        Logger.w(TAG, "Failed to release wakeLock", e)
                    } finally {
                        if (hasAdoptedShell) {
                            try {
                                instrumentation.uiAutomation.dropShellPermissionIdentity()
                            } catch (e: Exception) {
                                Logger.w(TAG, "Failed to drop shell permission identity", e)
                            }
                        }
                    }
                }
            }
        }

    private companion object {
        private const val TAG = "WakelockRule"
    }
}
