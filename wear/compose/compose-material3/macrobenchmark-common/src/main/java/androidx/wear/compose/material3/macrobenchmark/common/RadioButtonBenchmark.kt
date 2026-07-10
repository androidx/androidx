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

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.waitForStable
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.SplitRadioButton
import androidx.wear.compose.material3.Text

object RadioButtonBenchmark : MacrobenchmarkScreen {
    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
            var selectedRadioIndex by remember { mutableIntStateOf(0) }
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RadioButton(
                    modifier = Modifier.fillMaxWidth(),
                    selected = selectedRadioIndex == 0,
                    onSelect = { selectedRadioIndex = 0 },
                    enabled = true,
                    label = { Text(RADIO_BUTTON_1, Modifier.fillMaxWidth()) },
                )
                RadioButton(
                    modifier = Modifier.fillMaxWidth(),
                    selected = selectedRadioIndex == 1,
                    onSelect = { selectedRadioIndex = 1 },
                    enabled = true,
                    label = { Text(RADIO_BUTTON_2, Modifier.fillMaxWidth()) },
                )
            }
        }

    override val exercise: MacrobenchmarkScope.() -> Unit
        get() = {
            // Find by text instead of object since there's 2 RadioButton objects
            val button1 =
                device.wait(Until.findObject(By.text(RADIO_BUTTON_1)), FIND_OBJECT_TIMEOUT_MS)
            val button2 =
                device.wait(Until.findObject(By.text(RADIO_BUTTON_2)), FIND_OBJECT_TIMEOUT_MS)
            repeat(3) {
                // button1 is already active, so press button 2 first
                button2.click()
                button2.waitForStable(requireStableScreenshot = false)
                button1.click()
                button1.waitForStable(requireStableScreenshot = false)
            }
        }
}

object SplitRadioButtonBenchmark : MacrobenchmarkScreen {
    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
            var selectedRadioIndex by remember { mutableIntStateOf(0) }
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val context1 = LocalContext.current
                val context2 = LocalContext.current
                SplitRadioButton(
                    modifier = Modifier.fillMaxWidth(),
                    selected = selectedRadioIndex == 0,
                    onSelectionClick = { selectedRadioIndex = 0 },
                    enabled = true,
                    selectionContentDescription = SPLIT_RADIO_BUTTON_1,
                    onContainerClick = {},
                    label = { Text("Sample 1", Modifier.fillMaxWidth()) },
                )
                SplitRadioButton(
                    modifier = Modifier.fillMaxWidth(),
                    selected = selectedRadioIndex == 1,
                    onSelectionClick = { selectedRadioIndex = 1 },
                    enabled = true,
                    selectionContentDescription = SPLIT_RADIO_BUTTON_2,
                    onContainerClick = {},
                    label = { Text("Sample 2", Modifier.fillMaxWidth()) },
                )
            }
        }

    override val exercise: MacrobenchmarkScope.() -> Unit
        get() = {
            val button1 =
                device.wait(Until.findObject(By.desc(SPLIT_RADIO_BUTTON_1)), FIND_OBJECT_TIMEOUT_MS)
            val button2 =
                device.wait(Until.findObject(By.desc(SPLIT_RADIO_BUTTON_2)), FIND_OBJECT_TIMEOUT_MS)
            repeat(3) {
                // button1 is already active, so press button 2 first
                button2.click()
                button2.waitForStable(requireStableScreenshot = false)
                button1.click()
                button1.waitForStable(requireStableScreenshot = false)
            }
        }
}

const val RADIO_BUTTON_1 = "Sample RadioButton (1)"
const val RADIO_BUTTON_2 = "Sample RadioButton (2)"
const val SPLIT_RADIO_BUTTON_1 = "Sample SplitRadioButton (1)"
const val SPLIT_RADIO_BUTTON_2 = "Sample SplitRadioButton (2)"
