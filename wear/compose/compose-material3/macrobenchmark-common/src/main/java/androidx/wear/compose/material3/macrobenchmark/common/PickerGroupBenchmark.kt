/*
 * Copyright 2025 The Android Open Source Project
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

import android.os.SystemClock
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.wear.compose.material3.PickerGroup
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberPickerState

object PickerGroupBenchmark : MacrobenchmarkScreen {

    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
            var selectedPickerIndex by remember { mutableIntStateOf(0) }
            val pickerStateHour = rememberPickerState(initialNumberOfOptions = 24)
            val pickerStateMinute = rememberPickerState(initialNumberOfOptions = 60)
            val pickerStateSeconds = rememberPickerState(initialNumberOfOptions = 60)

            // PickerGroup with 3 columns with autocenter enabled
            PickerGroup(
                selectedPickerState =
                    when (selectedPickerIndex) {
                        0 -> pickerStateHour
                        1 -> pickerStateMinute
                        else -> pickerStateSeconds
                    },
                autoCenter = true,
            ) {
                PickerGroupItem(
                    pickerState = pickerStateHour,
                    selected = selectedPickerIndex == 0,
                    onSelected = { selectedPickerIndex = 0 },
                    contentDescription = { HOURS },
                    option = { optionIndex, _ -> Text(text = "%02d".format(optionIndex)) },
                    modifier = Modifier.fillMaxWidth(fraction = 0.33f),
                )

                PickerGroupItem(
                    pickerState = pickerStateMinute,
                    selected = selectedPickerIndex == 1,
                    onSelected = { selectedPickerIndex = 1 },
                    contentDescription = { MINUTES },
                    option = { optionIndex, _ -> Text(text = "%02d".format(optionIndex)) },
                    modifier = Modifier.fillMaxWidth(fraction = 0.33f),
                )

                PickerGroupItem(
                    pickerState = pickerStateSeconds,
                    selected = selectedPickerIndex == 2,
                    onSelected = { selectedPickerIndex = 2 },
                    contentDescription = { SECONDS },
                    option = { optionIndex, _ -> Text(text = "%02d".format(optionIndex)) },
                    modifier = Modifier.fillMaxWidth(fraction = 0.33f),
                )
            }
        }

    override val exercise: MacrobenchmarkScope.() -> Unit
        get() = {
            // Switch between PickerGroup columns
            repeat(10) {
                // Select hours picker
                device.wait(Until.findObject(By.desc(HOURS)), FIND_OBJECT_TIMEOUT_MS).click()
                SystemClock.sleep(250)

                // Select minutes picker
                device.wait(Until.findObject(By.desc(MINUTES)), FIND_OBJECT_TIMEOUT_MS).click()
                SystemClock.sleep(250)

                // Select seconds picker
                device.wait(Until.findObject(By.desc(SECONDS)), FIND_OBJECT_TIMEOUT_MS).click()
                SystemClock.sleep(250)

                // Select minutes picker again
                device.wait(Until.findObject(By.desc(MINUTES)), FIND_OBJECT_TIMEOUT_MS).click()
                SystemClock.sleep(250)
            }
        }

    private const val HOURS = "HOURS"
    private const val MINUTES = "MINUTES"
    private const val SECONDS = "SECONDS"
}
