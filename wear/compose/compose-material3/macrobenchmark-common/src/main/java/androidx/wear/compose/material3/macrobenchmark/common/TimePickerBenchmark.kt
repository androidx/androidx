/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3.macrobenchmark.common

import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.test.uiautomator.By
import androidx.wear.compose.material3.TimePicker
import androidx.wear.compose.material3.TimePickerType
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.O)
object TimePickerBenchmark : MacrobenchmarkScreen {
    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
            TimePicker(
                onTimePicked = {},
                timePickerType = TimePickerType.HoursMinutesAmPm12H,
                initialTime = LocalTime.of(11, 30),
            )
        }

    override val exercise: MacrobenchmarkScope.() -> Unit
        get() = {
            val startY = device.displayHeight / 2
            val endY = device.displayHeight * 9 / 10 // scroll down

            // Find hour/minute/AmPm by searching for clickable items with content description
            val testObjects =
                device.findObjects(By.clickable(true)).filter { it ->
                    it.contentDescription != null
                }
            assert(testObjects.size == 3)

            repeat(20) {
                for (obj in testObjects) {
                    val x = obj.visibleBounds.centerX()
                    device.swipe(x, startY, x, endY, 10)
                    device.waitForIdle()
                    SystemClock.sleep(500)
                }
            }
        }
}

@RequiresApi(Build.VERSION_CODES.O)
object TimePickerScrollMinutesBenchmark : MacrobenchmarkScreen {
    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
            TimePicker(
                onTimePicked = {},
                timePickerType = TimePickerType.HoursMinutesAmPm12H,
                initialTime = LocalTime.of(11, 30),
            )
        }

    override val exercise: MacrobenchmarkScope.() -> Unit
        get() = {
            val startY = device.displayHeight / 2
            val endY = device.displayHeight * 9 / 10 // scroll down
            val x = device.displayWidth / 2

            repeat(20) {
                device.swipe(x, startY, x, endY, 10)
                device.waitForIdle()
                SystemClock.sleep(500)
            }
        }
}
