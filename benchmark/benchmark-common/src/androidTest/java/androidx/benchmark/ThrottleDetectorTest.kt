/*
 * Copyright 2018 The Android Open Source Project
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

import android.content.Context
import android.os.PowerManager
import android.os.Process
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.concurrent.thread
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 29)
@LargeTest
@RunWith(AndroidJUnit4::class)
class ThrottleDetectorTest {
    /**
     * This test can be used to evaluate correlation between CPU perf and
     * `PowerManager.currentThermalStatus`
     *
     * Wrapping an API 30 Pixel 2 in a jacket, and running the test, I observed the following:
     *
     * - 5 minutes of "Measured 55.XXXX, throttle state 0" (extremely low variance)
     * - 2 minutes of "Measured 55.YYYY, throttle state 1" (higher variance, within 1ms)
     * - Several min of "Measured ZZ.ZZ, throttle state 1" (much higher variance, between 55 - 72ms)
     *    along with logcat messages like the following:
     * `02-10 19:26:48.894   913   963 I android.hardware.thermal@2.0-service.pixel: back_therm: 40`
     *
     * It's unclear how much of this measurement variation is due to unplugging big CPUs vs freq
     * changes, but either way, we can observe significant variation with just
     * "THERMAL_STATUS_LIGHT (1)", so we disallow that for microbenchmarks.
     */
    @Ignore // this is only for local experimentation with thermal throttling values
    @Test
    fun powerManagerCurrentThermalStatus() {
        var end = false
        try {
            // is it getting hot in here, or is it just me?
            repeat(16) {
                thread {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                    while (!end) {
                        // Use more of the CPU to get more heat
                        ThrottleDetector.measureWorkNs()
                    }
                }
            }

            // while device slowly heats up, check thermal state
            repeat(1000) {
                val time = ThrottleDetector.measureWorkNs()
                val state = (InstrumentationRegistry
                    .getInstrumentation()
                    .context
                    .getSystemService(Context.POWER_SERVICE) as PowerManager)
                    .currentThermalStatus
                Log.d("ThrottleDetection", "Measured $time, throttle state $state")
            }
        } finally {
            end = true
        }
    }
}
